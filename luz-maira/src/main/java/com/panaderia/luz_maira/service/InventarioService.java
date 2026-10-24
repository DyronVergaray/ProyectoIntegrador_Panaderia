package com.panaderia.luz_maira.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.InventarioDAO;
import com.panaderia.luz_maira.model.MovimientoForm;
import com.panaderia.luz_maira.model.SolicitudForm;

/**
 * Reglas del almacén: cada movimiento del registro del día actualiza el stock del producto base,
 * y recibir una solicitud a un proveedor suma lo recibido al stock.
 */
@Service
public class InventarioService {

    private static final Logger log = LoggerFactory.getLogger(InventarioService.class);

    // Cada unidad con su "familia" y su valor en la unidad más pequeña de la familia (kg y g se pueden convertir)
    private record Unidad(String familia, int factor) { }

    private static final Map<String, Unidad> UNIDADES = Map.of(
            "Kilogramo", new Unidad("masa", 1000), "Gramo", new Unidad("masa", 1),
            "Litro", new Unidad("volumen", 1), "Unidad", new Unidad("cuenta", 1));

    private final InventarioDAO dao;

    public InventarioService(InventarioDAO dao) {
        this.dao = dao;
    }

    /** Convierte una cantidad entre unidades de la misma familia (kg <-> g); null si no son compatibles. */
    static BigDecimal convertir(BigDecimal cantidad, String de, String a) {
        Unidad origen = UNIDADES.get(de);
        Unidad destino = UNIDADES.get(a);
        if (origen == null || destino == null || !origen.familia().equals(destino.familia())) {
            return null;
        }
        return cantidad.multiply(BigDecimal.valueOf(origen.factor()))
                .divide(BigDecimal.valueOf(destino.factor()), 2, RoundingMode.HALF_UP);
    }

    // ---------- Registro del día ----------

    /** Anota una entrada o salida y actualiza el stock. Una salida no puede superar lo que hay. */
    @Transactional
    public void registrarMovimiento(MovimientoForm f) {
        InventarioDAO.Materia materia = dao.materia(f.idMateria());
        if (materia == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto base no existe");
        }
        boolean ingreso = f.movimiento().equals("Ingreso");
        BigDecimal cambio = BigDecimal.valueOf(f.cantidad());
        if (!dao.ajustarStock(f.idMateria(), ingreso ? cambio : cambio.negate())) {
            throw error("No hay stock suficiente para esa salida");
        }
        String motivo = f.descripcion() == null || f.descripcion().isBlank() ? "" : ": " + f.descripcion().trim();
        dao.insertarMovimiento(dao.registroDeHoy(), f.idMateria(), materia.idMedicion(), f.cantidad(), f.movimiento() + motivo);
        log.info("Registro del día: {} de {} {} (producto base {})", f.movimiento(), f.cantidad(), materia.unidad(), f.idMateria());
    }

    // ---------- Solicitudes a proveedores ----------

    @Transactional
    public int crearSolicitud(SolicitudForm f) {
        if (!dao.existeProveedor(f.idProveedor())) {
            throw error("El proveedor no existe");
        }
        InventarioDAO.Materia materia = dao.materia(f.idMateria());
        if (materia == null) {
            throw error("El producto base no existe");
        }
        Integer idMedicion = dao.idMedicion(f.unidad());
        if (idMedicion == null) {
            throw error("La unidad de medida no existe");
        }
        if (convertir(f.cantidad(), f.unidad(), materia.unidad()) == null) {
            throw error("La unidad no es compatible con la del producto base (" + materia.unidad() + ")");
        }
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(f.fechaEstimada());
        } catch (DateTimeParseException e) {
            throw error("La fecha estimada no es válida");
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw error("La fecha estimada de recepción no puede estar en el pasado");
        }
        int id = dao.insertarSolicitud(f.idMateria(), f.idProveedor(), idMedicion, f.cantidad(), f.precio(), Date.valueOf(fecha));
        log.info("Solicitud {} creada: producto base {}, {} {}", id, f.idMateria(), f.cantidad(), f.unidad());
        return id;
    }

    /** Confirma la llegada: la solicitud pasa a Recibido, el stock sube y queda un Ingreso en el registro del día. */
    @Transactional
    public void recibirSolicitud(int idSolicitud) {
        InventarioDAO.SolicitudGuardada s = dao.solicitud(idSolicitud);
        if (s == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La solicitud no existe");
        }
        if (!s.estado().equals("En camino")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esa solicitud ya fue recibida");
        }
        InventarioDAO.Materia materia = dao.materia(s.idMateria());
        BigDecimal enUnidadDelProducto = s.cantidad() == null || materia == null ? null : convertir(s.cantidad(), s.unidad(), materia.unidad());
        if (enUnidadDelProducto == null) {
            throw error("No se pudo convertir la cantidad de la solicitud a la unidad del producto base");
        }
        dao.marcarRecibida(idSolicitud);
        dao.ajustarStock(s.idMateria(), enUnidadDelProducto);
        // Lista_Materia solo guarda cantidades enteras: si la cantidad recibida tiene decimales, el stock sube igual pero no se anota la línea
        if (s.cantidad().stripTrailingZeros().scale() <= 0 && s.cantidad().compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) <= 0) {
            dao.insertarMovimiento(dao.registroDeHoy(), s.idMateria(), s.idMedicion(), s.cantidad().intValueExact(),
                    "Ingreso: recepción de proveedor");
        }
        log.info("Solicitud {} recibida: +{} {} al producto base {}", idSolicitud, enUnidadDelProducto, materia.unidad(), s.idMateria());
    }

    private ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}

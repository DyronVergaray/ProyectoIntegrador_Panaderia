package com.panaderia.luz_maira.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.PromocionDAO;
import com.panaderia.luz_maira.model.Promocion;
import com.panaderia.luz_maira.model.PromocionForm;

/** Reglas de negocio de las promociones: validación de datos del administrador y consulta de las vigentes. */
@Service
public class PromocionService {

    private static final Logger log = LoggerFactory.getLogger(PromocionService.class);
    private final PromocionDAO dao;

    public PromocionService(PromocionDAO dao) {
        this.dao = dao;
    }

    public List<Promocion> todas() {
        return dao.todas();
    }

    public List<Promocion> vigentes() {
        return dao.vigentes();
    }

    @Transactional
    public int crear(PromocionForm f) {
        Date[] fechas = validar(f);
        int id = dao.crear(f, fechas[0], fechas[1], activa(f));
        log.info("Promoción {} creada: {}", id, f.nombre());
        return id;
    }

    @Transactional
    public void actualizar(int id, PromocionForm f) {
        existente(id);
        Date[] fechas = validar(f);
        dao.actualizar(id, f, fechas[0], fechas[1], activa(f));
        log.info("Promoción {} actualizada", id);
    }

    @Transactional
    public void eliminar(int id) {
        existente(id);
        dao.eliminar(id);
        log.info("Promoción {} eliminada", id);
    }

    private boolean activa(PromocionForm f) {
        return f.activa() == null || f.activa();
    }

    private void existente(int id) {
        if (!dao.existePromocion(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La promoción no existe");
        }
    }

    /** Revisa tipo, productos y fechas; devuelve {inicio, fin} (cada una puede ser null). */
    private Date[] validar(PromocionForm f) {
        if (f.tipo().equals(Promocion.MONTO)) {
            if (f.montoMinimo() == null) {
                throw error("Ingresa el monto de compra");
            }
        } else if (f.tipo().equals(Promocion.CANTIDAD)) {
            if (f.idProductoCondicion() == null || f.cantidadCondicion() == null) {
                throw error("Elige el producto a comprar y la cantidad");
            }
            if (!dao.productoExiste(f.idProductoCondicion())) {
                throw error("El producto a comprar no existe");
            }
        } else {
            throw error("El tipo de promoción no es válido");
        }
        if (!dao.productoExiste(f.idProducto())) {
            throw error("El producto de regalo no existe");
        }
        Date inicio = fecha(f.fechaInicio(), "La fecha de inicio no es válida");
        Date fin = fecha(f.fechaFin(), "La fecha de fin no es válida");
        if (inicio != null && fin != null && fin.before(inicio)) {
            throw error("La fecha de fin no puede ser anterior a la de inicio");
        }
        return new Date[] { inicio, fin };
    }

    private Date fecha(String texto, String mensaje) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Date.valueOf(LocalDate.parse(texto.trim()));
        } catch (DateTimeParseException e) {
            throw error(mensaje);
        }
    }

    private ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}

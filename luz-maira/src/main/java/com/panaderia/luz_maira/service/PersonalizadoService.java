package com.panaderia.luz_maira.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.PersonalizadoDAO;
import com.panaderia.luz_maira.model.Pedidos;
import com.panaderia.luz_maira.model.Personalizado;
import com.panaderia.luz_maira.model.PersonalizadoForm;
import com.panaderia.luz_maira.security.UsuarioAutenticado;

/**
 * Pedidos personalizados: el cliente envía una solicitud (MongoDB), el administrador la acepta con un precio
 * (se crea el pedido en SQL Server) o la rechaza.
 * No usa @Transactional porque cruza dos bases de datos: el orden de los pasos al aceptar evita inconsistencias
 * (ver aceptar).
 */
@Service
public class PersonalizadoService {

    private static final Logger log = LoggerFactory.getLogger(PersonalizadoService.class);
    /** Días mínimos de anticipación: una torta a medida no se prepara de un día para otro. */
    public static final int ANTICIPACION_DIAS = 2;

    private final PersonalizadoDAO dao;
    private final PedidoService pedidos;

    public PersonalizadoService(PersonalizadoDAO dao, PedidoService pedidos) {
        this.dao = dao;
        this.pedidos = pedidos;
    }

    public Personalizado solicitar(UsuarioAutenticado usuario, PersonalizadoForm.Solicitud f) {
        LocalDate fecha;
        try {
            fecha = LocalDate.parse(f.fechaEntrega().trim());
            LocalTime.parse(f.horaEntrega().trim());
        } catch (DateTimeParseException e) {
            throw error(HttpStatus.BAD_REQUEST, "La fecha u hora de entrega no es válida");
        }
        if (fecha.isBefore(LocalDate.now().plusDays(ANTICIPACION_DIAS))) {
            throw error(HttpStatus.BAD_REQUEST, "Los pedidos personalizados se piden con al menos " + ANTICIPACION_DIAS + " días de anticipación");
        }
        boolean delivery = f.modalidad().equals("Delivery");
        if (delivery && (f.direccion() == null || f.direccion().isBlank())) {
            throw error(HttpStatus.BAD_REQUEST, "Ingresa la dirección para el delivery");
        }
        Personalizado nueva = new Personalizado(null, usuario.getId(), usuario.getNombre(), usuario.getUsername(),
                f.tipoProducto().trim(), f.descripcion().trim(), limpio(f.sabor()), f.porciones(), limpio(f.decoracion()),
                limpio(f.dedicatoria()), limpio(f.alergias()), f.modalidad(), delivery ? f.direccion().trim() : null,
                fecha.toString(), f.horaEntrega().trim(), Personalizado.SOLICITADA, null, null, null,
                LocalDateTime.now().withNano(0).toString());
        Personalizado guardada = dao.crear(nueva);
        log.info("Solicitud personalizada {} creada por {}", guardada.id(), usuario.getUsername());
        return guardada;
    }

    public List<Personalizado> deCliente(UsuarioAutenticado usuario) {
        return dao.deUsuario(usuario.getId());
    }

    public List<Personalizado> todas(String estado) {
        return dao.todas(estado);
    }

    /** El cliente puede cancelar su solicitud mientras nadie la haya respondido. */
    public void cancelar(UsuarioAutenticado usuario, String id) {
        Personalizado s = existente(id);
        if (s.idUsuario() != usuario.getId()) {
            throw error(HttpStatus.NOT_FOUND, "La solicitud no existe");
        }
        if (!dao.cambiarEstado(id, Personalizado.SOLICITADA, Personalizado.CANCELADA, null, null)) {
            throw error(HttpStatus.CONFLICT, "La solicitud ya fue respondida y no se puede cancelar");
        }
    }

    /**
     * Acepta la solicitud con el precio acordado y crea el pedido en SQL Server.
     * Pasos: 1) marcar "Aceptada" en MongoDB solo si seguía "Solicitada" (evita aceptarla dos veces),
     * 2) crear el pedido en SQL, 3) guardar el número de pedido en la solicitud.
     * Si el paso 2 falla, se devuelve la solicitud a "Solicitada" para que no quede aceptada sin pedido.
     */
    public Pedidos.Confirmacion aceptar(String id, PersonalizadoForm.Aceptar f) {
        Personalizado s = existente(id);
        if (!dao.cambiarEstado(id, Personalizado.SOLICITADA, Personalizado.ACEPTADA, f.precio().doubleValue(), limpio(f.nota()))) {
            throw error(HttpStatus.CONFLICT, "La solicitud ya fue respondida");
        }
        try {
            String direccion = s.modalidad().equals("Delivery") ? s.direccion() : null;
            Pedidos.Confirmacion pedido = pedidos.crearPersonalizado(s.idUsuario(), f.precio(),
                    LocalDate.parse(s.fechaEntrega()), LocalTime.parse(s.horaEntrega()), direccion);
            dao.asignarPedido(id, pedido.idPedido());
            log.info("Solicitud {} aceptada: pedido {} por S/ {}", id, pedido.idPedido(), f.precio());
            return pedido;
        } catch (RuntimeException e) {
            dao.cambiarEstado(id, Personalizado.ACEPTADA, Personalizado.SOLICITADA, null, null);
            throw e;
        }
    }

    public void rechazar(String id, PersonalizadoForm.Rechazar f) {
        existente(id);
        if (!dao.cambiarEstado(id, Personalizado.SOLICITADA, Personalizado.RECHAZADA, null, f.nota().trim())) {
            throw error(HttpStatus.CONFLICT, "La solicitud ya fue respondida");
        }
    }

    /** Características del pedido personalizado para mostrarlas en el detalle del pedido (vacío si no es personalizado). */
    public List<Pedidos.Campo> especificaciones(int idPedido) {
        Personalizado s = dao.porIdPedido(idPedido);
        List<Pedidos.Campo> campos = new ArrayList<>();
        if (s == null) {
            return campos;
        }
        campos.add(new Pedidos.Campo("Tipo de producto", s.tipoProducto()));
        campos.add(new Pedidos.Campo("Descripción", s.descripcion()));
        agregar(campos, "Sabor", s.sabor());
        campos.add(new Pedidos.Campo("Porciones", String.valueOf(s.porciones())));
        agregar(campos, "Decoración", s.decoracion());
        agregar(campos, "Dedicatoria", s.dedicatoria());
        agregar(campos, "Alergias / restricciones", s.alergias());
        campos.add(new Pedidos.Campo("Entrega", s.modalidad().equals("Delivery") ? "Delivery" : "Recojo en tienda"));
        agregar(campos, "Nota de la panadería", s.nota());
        return campos;
    }

    private void agregar(List<Pedidos.Campo> campos, String etiqueta, String valor) {
        if (valor != null && !valor.isBlank()) {
            campos.add(new Pedidos.Campo(etiqueta, valor));
        }
    }

    private Personalizado existente(String id) {
        Personalizado s = dao.porId(id);
        if (s == null) {
            throw error(HttpStatus.NOT_FOUND, "La solicitud no existe");
        }
        return s;
    }

    private String limpio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}

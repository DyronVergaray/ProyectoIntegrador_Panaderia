package com.panaderia.luz_maira.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.EntregaDAO;
import com.panaderia.luz_maira.model.AsignacionForm;
import com.panaderia.luz_maira.model.Logistica;

/**
 * Flujo de los pedidos:
 *  - Virtual:               Pendiente --(despachador confirma con el código del cliente)--> Entregado
 *  - Delivery/Personalizado: Pendiente --(almacenero asigna conductor + vehículo)--> asignado
 *                            --(entregador confirma con el código del cliente)--> Entregado
 * El estado guardado en la BD sigue siendo Pendiente / Entregado; "asignado" es tener conductor y vehículo.
 */
@Service
public class EntregaService {

    private static final Logger log = LoggerFactory.getLogger(EntregaService.class);

    private final EntregaDAO dao;

    public EntregaService(EntregaDAO dao) {
        this.dao = dao;
    }

    // ---------- Listados (cada pantalla muestra los estados con sus propios nombres) ----------

    public List<Logistica.PedidoOperativo> paraDespacho() {
        return dao.paraDespacho().stream()
                .map(f -> aPedido(f, f.estado().equals("Pendiente") ? "Por despachar" : "Despachado")).toList();
    }

    public List<Logistica.PedidoOperativo> paraAsignacion() {
        return dao.paraAsignacion().stream()
                .map(f -> aPedido(f, f.estado().equals("Entregado") ? "Entregado" : f.asignado() ? "Asignado" : "Por asignar")).toList();
    }

    public List<Logistica.PedidoOperativo> paraEntrega() {
        return dao.paraEntrega().stream()
                .map(f -> aPedido(f, f.estado().equals("Entregado") ? "Entregado" : "Por entregar")).toList();
    }

    public Logistica.Recursos recursosDisponibles() {
        return new Logistica.Recursos(dao.conductoresDisponibles(), dao.vehiculosDisponibles());
    }

    private Logistica.PedidoOperativo aPedido(EntregaDAO.Fila f, String estado) {
        return new Logistica.PedidoOperativo(f.id(), f.cliente(), f.tipo(), f.fechaSolicitud(), f.fechaEntrega(),
                f.horaEntrega(), f.direccion(), estado, f.conductor(), f.vehiculo());
    }

    // ---------- Acciones ----------

    /** El almacenero asigna un pedido Delivery / Personalizado a un conductor y un vehículo disponibles. */
    @Transactional
    public void asignar(AsignacionForm f) {
        EntregaDAO.Ficha ficha = ficha(f.idPedido());
        if (!esReparto(ficha)) {
            throw error("Solo los pedidos Delivery o Personalizados se asignan a un conductor");
        }
        if (!ficha.estado().equals("Pendiente")) {
            throw error("Ese pedido ya no está pendiente");
        }
        if (ficha.asignado()) {
            throw error("Ese pedido ya tiene conductor y vehículo asignados");
        }
        // Si otra persona asignó el mismo conductor o vehículo hace un instante, se cancela todo (rollback)
        if (!dao.ocuparConductor(f.idConductor())) {
            throw error("El conductor ya no está disponible. Elige otro.");
        }
        if (!dao.ocuparVehiculo(f.idVehiculo())) {
            throw error("El vehículo ya no está disponible. Elige otro.");
        }
        dao.asignar(f.idPedido(), f.idConductor(), f.idVehiculo());
        log.info("Pedido {} asignado: conductor={}, vehículo={}", f.idPedido(), f.idConductor(), f.idVehiculo());
    }

    /** El despachador entrega un pedido Virtual cuando el cliente muestra su código. */
    @Transactional
    public void confirmarDespacho(int idPedido, String codigo) {
        EntregaDAO.Ficha ficha = ficha(idPedido);
        if (!ficha.tipo().equals("Virtual")) {
            throw error("Este pedido no es Virtual");
        }
        validarPendienteYCodigo(idPedido, ficha, codigo);
        dao.marcarEntregado(idPedido);
        log.info("Pedido virtual {} despachado", idPedido);
    }

    /** El entregador cierra un Delivery / Personalizado cuando el cliente muestra su código. */
    @Transactional
    public void confirmarEntrega(int idPedido, String codigo) {
        EntregaDAO.Ficha ficha = ficha(idPedido);
        if (!esReparto(ficha)) {
            throw error("Este pedido no es Delivery ni Personalizado");
        }
        if (!ficha.asignado()) {
            throw error("El almacén aún no asignó conductor y vehículo a este pedido");
        }
        validarPendienteYCodigo(idPedido, ficha, codigo);
        dao.marcarEntregado(idPedido);
        dao.liberarRecursos(idPedido);
        log.info("Pedido {} entregado; conductor y vehículo quedan disponibles", idPedido);
    }

    private void validarPendienteYCodigo(int idPedido, EntregaDAO.Ficha ficha, String codigo) {
        if (!ficha.estado().equals("Pendiente")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido ya fue entregado o cancelado");
        }
        if (ficha.codigo() == null || !ficha.codigo().equals(codigo)) {
            log.warn("Código incorrecto al confirmar el pedido {}", idPedido);
            throw error("El código no coincide con el del pedido");
        }
    }

    private EntregaDAO.Ficha ficha(int idPedido) {
        EntregaDAO.Ficha ficha = dao.ficha(idPedido);
        if (ficha == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El pedido no existe");
        }
        return ficha;
    }

    private boolean esReparto(EntregaDAO.Ficha ficha) {
        return ficha.tipo().equals("Delivery") || ficha.tipo().equals("Personalizado");
    }

    private ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}

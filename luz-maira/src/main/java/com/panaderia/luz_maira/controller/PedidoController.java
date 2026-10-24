package com.panaderia.luz_maira.controller;

import java.util.List;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.dao.PedidoDAO;
import com.panaderia.luz_maira.model.PedidoForm;
import com.panaderia.luz_maira.model.Pedidos;
import com.panaderia.luz_maira.model.TipoPedido;
import com.panaderia.luz_maira.security.UsuarioAutenticado;
import com.panaderia.luz_maira.service.PedidoService;

import jakarta.validation.Valid;

/** /api/tipos-pedido y /api/pedidos (consolidar, historial, listado y detalle). Permisos en SecurityConfig. */
@RestController
@RequestMapping("/api")
public class PedidoController {

    private final PedidoDAO dao;
    private final PedidoService servicio;

    public PedidoController(PedidoDAO dao, PedidoService servicio) {
        this.dao = dao;
        this.servicio = servicio;
    }

    @GetMapping("/tipos-pedido")
    public List<TipoPedido> tipos() {
        return dao.tipos().stream().filter(t -> !t.nombre().equals("Presencial")).toList();
    }

    /** Consolida el pedido del cliente (o la venta de la cajera) y devuelve el código de 3 dígitos. */
    @PostMapping("/pedidos")
    public ResponseEntity<Pedidos.Confirmacion> crear(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                      @Valid @RequestBody PedidoForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(usuario, form));
    }

    /** Historial del cliente que inició sesión. */
    @GetMapping("/pedidos/mios")
    public List<Pedidos.Resumen> mios(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return dao.listar(null, usuario.getId());
    }

    /** Todos los pedidos (cajera y administrador). */
    @GetMapping("/pedidos")
    public List<Pedidos.Resumen> pedidos(@RequestParam(required = false) String tipo) {
        return dao.listar(tipo, null);
    }

    @GetMapping("/pedidos/{id}")
    public Pedidos.Detalle detalle(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable int id) {
        // Un cliente solo puede ver sus propios pedidos
        if (usuario.getRol().equals("Cliente") && !dao.esDelCliente(id, usuario.getId())) {
            throw new EmptyResultDataAccessException(1);
        }
        Pedidos.Detalle detalle = dao.detalle(id);
        // El código lo muestra el cliente al recibir su pedido: despachador, entregador y almacenero no lo ven, para que confirmarlo tenga sentido
        if (usuario.getRol().equals("Despachador") || usuario.getRol().equals("Entregador") || usuario.getRol().equals("Almacenero")) {
            return new Pedidos.Detalle(detalle.id(), detalle.cliente(), detalle.telefono(), detalle.email(), detalle.direccion(),
                    detalle.tipo(), null, detalle.fechaSolicitud(), detalle.estado(), detalle.items(), detalle.total());
        }
        return detalle;
    }
}

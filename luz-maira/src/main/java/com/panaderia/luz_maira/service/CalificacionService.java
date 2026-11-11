package com.panaderia.luz_maira.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.CalificacionDAO;
import com.panaderia.luz_maira.dao.PedidoDAO;
import com.panaderia.luz_maira.model.Calificacion;
import com.panaderia.luz_maira.model.CalificacionForm;
import com.panaderia.luz_maira.security.UsuarioAutenticado;

/** Calificaciones: el pedido vive en SQL Server (se verifica dueño y estado) y la calificación en MongoDB. */
@Service
public class CalificacionService {

    private final CalificacionDAO dao;
    private final PedidoDAO pedidos;

    public CalificacionService(CalificacionDAO dao, PedidoDAO pedidos) {
        this.dao = dao;
        this.pedidos = pedidos;
    }

    public Calificacion calificar(UsuarioAutenticado usuario, CalificacionForm f) {
        if (!pedidos.esDelCliente(f.idPedido(), usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El pedido no existe");
        }
        if (!"Entregado".equals(pedidos.estadoDe(f.idPedido()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo puedes calificar pedidos que ya fueron entregados");
        }
        if (dao.existe(f.idPedido())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido ya fue calificado");
        }
        String comentario = f.comentario() == null || f.comentario().isBlank() ? null : f.comentario().trim();
        try {
            return dao.crear(new Calificacion(null, f.idPedido(), usuario.getId(), f.compra(), f.entrega(), f.atencion(),
                    f.calidad(), f.recomienda(), comentario, LocalDateTime.now().withNano(0).toString()));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este pedido ya fue calificado");
        }
    }

    public List<Calificacion> deCliente(UsuarioAutenticado usuario) {
        return dao.deUsuario(usuario.getId());
    }
}

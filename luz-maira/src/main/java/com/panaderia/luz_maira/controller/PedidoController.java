package com.panaderia.luz_maira.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.dao.PedidoDAO;
import com.panaderia.luz_maira.model.Pedidos;
import com.panaderia.luz_maira.model.TipoPedido;

/** /api/tipos-pedido y /api/pedidos (listado con filtros y detalle). */
@RestController
@RequestMapping("/api")
public class PedidoController {

    private final PedidoDAO dao;

    public PedidoController(PedidoDAO dao) {
        this.dao = dao;
    }

    @GetMapping("/tipos-pedido")
    public List<TipoPedido> tipos() {
        return dao.tipos();
    }

    @GetMapping("/pedidos")
    public List<Pedidos.Resumen> pedidos(@RequestParam(required = false) String tipo,
                                         @RequestParam(required = false) Integer clienteId) {
        return dao.listar(tipo, clienteId);
    }

    @GetMapping("/pedidos/{id}")
    public Pedidos.Detalle detalle(@PathVariable int id) {
        return dao.detalle(id);
    }
}

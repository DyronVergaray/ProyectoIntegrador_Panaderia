package com.panaderia.luz_maira.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.model.AsignacionForm;
import com.panaderia.luz_maira.model.CodigoForm;
import com.panaderia.luz_maira.model.Logistica;
import com.panaderia.luz_maira.service.EntregaService;

import jakarta.validation.Valid;

/**
 * Pantallas operativas de pedidos. Permisos en SecurityConfig:
 *  /api/despacho      -> Despachador (pedidos Virtuales)
 *  /api/asignaciones  -> Almacenero y Administrador (asignar conductor y vehículo)
 *  /api/entregas      -> Entregador (Delivery y Personalizados)
 */
@RestController
@RequestMapping("/api")
public class EntregaController {

    private final EntregaService servicio;

    public EntregaController(EntregaService servicio) {
        this.servicio = servicio;
    }

    // ----- Despachador -----

    @GetMapping("/despacho")
    public List<Logistica.PedidoOperativo> despacho() {
        return servicio.paraDespacho();
    }

    @PostMapping("/despacho/{id}/confirmar")
    public Map<String, String> confirmarDespacho(@PathVariable int id, @Valid @RequestBody CodigoForm form) {
        servicio.confirmarDespacho(id, form.codigo());
        return Map.of("mensaje", "Pedido despachado correctamente");
    }

    // ----- Almacenero -----

    @GetMapping("/asignaciones")
    public List<Logistica.PedidoOperativo> asignaciones() {
        return servicio.paraAsignacion();
    }

    @GetMapping("/asignaciones/recursos")
    public Logistica.Recursos recursos() {
        return servicio.recursosDisponibles();
    }

    @PostMapping("/asignaciones")
    public Map<String, String> asignar(@Valid @RequestBody AsignacionForm form) {
        servicio.asignar(form);
        return Map.of("mensaje", "Delivery asignado correctamente");
    }

    // ----- Entregador -----

    @GetMapping("/entregas")
    public List<Logistica.PedidoOperativo> entregas() {
        return servicio.paraEntrega();
    }

    @PostMapping("/entregas/{id}/confirmar")
    public Map<String, String> confirmarEntrega(@PathVariable int id, @Valid @RequestBody CodigoForm form) {
        servicio.confirmarEntrega(id, form.codigo());
        return Map.of("mensaje", "Pedido entregado correctamente");
    }
}

package com.panaderia.luz_maira.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.dao.InventarioDAO;
import com.panaderia.luz_maira.model.Inventario;
import com.panaderia.luz_maira.model.MovimientoForm;
import com.panaderia.luz_maira.model.SolicitudForm;
import com.panaderia.luz_maira.service.InventarioService;

import jakarta.validation.Valid;

/** /api/movimientos (registro del día) y /api/solicitudes (pedidos de productos base a proveedores). Permisos en SecurityConfig. */
@RestController
@RequestMapping("/api")
public class InventarioController {

    private final InventarioDAO dao;
    private final InventarioService servicio;

    public InventarioController(InventarioDAO dao, InventarioService servicio) {
        this.dao = dao;
        this.servicio = servicio;
    }

    @GetMapping("/movimientos")
    public List<Inventario.Movimiento> movimientos() {
        return dao.movimientos();
    }

    @PostMapping("/movimientos")
    public ResponseEntity<Map<String, String>> registrar(@Valid @RequestBody MovimientoForm form) {
        servicio.registrarMovimiento(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("mensaje", "Movimiento registrado correctamente"));
    }

    @GetMapping("/solicitudes")
    public List<Inventario.Solicitud> solicitudes() {
        return dao.solicitudes();
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody SolicitudForm form) {
        int id = servicio.crearSolicitud(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "mensaje", "Solicitud enviada correctamente"));
    }

    @PostMapping("/solicitudes/{id}/recibir")
    public Map<String, String> recibir(@PathVariable int id) {
        servicio.recibirSolicitud(id);
        return Map.of("mensaje", "Recepción confirmada: el stock se actualizó");
    }
}

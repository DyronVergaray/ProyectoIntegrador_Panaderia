package com.panaderia.luz_maira.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.model.Promocion;
import com.panaderia.luz_maira.model.PromocionForm;
import com.panaderia.luz_maira.service.PromocionService;

import jakarta.validation.Valid;

/**
 * /api/promociones. GET /api/promociones es público (alimenta el slider) y solo trae las vigentes.
 * El resto (/todas, crear, editar, eliminar) es solo del Administrador: ver SecurityConfig.
 */
@RestController
@RequestMapping("/api/promociones")
public class PromocionController {

    private final PromocionService servicio;

    public PromocionController(PromocionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Promocion> vigentes() {
        return servicio.vigentes();
    }

    @GetMapping("/todas")
    public List<Promocion> todas() {
        return servicio.todas();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody PromocionForm form) {
        int id = servicio.crear(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "mensaje", "Promoción registrada correctamente"));
    }

    @PutMapping("/{id}")
    public Map<String, String> actualizar(@PathVariable int id, @Valid @RequestBody PromocionForm form) {
        servicio.actualizar(id, form);
        return Map.of("mensaje", "Promoción actualizada correctamente");
    }

    @DeleteMapping("/{id}")
    public Map<String, String> eliminar(@PathVariable int id) {
        servicio.eliminar(id);
        return Map.of("mensaje", "Promoción eliminada");
    }
}

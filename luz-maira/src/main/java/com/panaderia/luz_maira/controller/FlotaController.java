package com.panaderia.luz_maira.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.model.Flota;
import com.panaderia.luz_maira.model.FlotaForm;
import com.panaderia.luz_maira.model.UsuarioForm;
import com.panaderia.luz_maira.service.FlotaService;

import jakarta.validation.Valid;

/** /api/entregadores y /api/vehiculos (solo Administrador; ver SecurityConfig). */
@RestController
@RequestMapping("/api")
public class FlotaController {

    private final FlotaService servicio;

    public FlotaController(FlotaService servicio) {
        this.servicio = servicio;
    }

    // ----- Entregadores -----

    @GetMapping("/entregadores")
    public List<Flota.Entregador> entregadores() {
        return servicio.entregadores();
    }

    /** Crea al entregador junto con su cuenta (correo y contraseña propios). El rol lo fija el servidor. */
    @PostMapping("/entregadores")
    public ResponseEntity<Map<String, Object>> crearEntregador(@Valid @RequestBody UsuarioForm form) {
        int id = servicio.crearEntregador(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "mensaje", "Entregador registrado con sus credenciales"));
    }

    @PutMapping("/entregadores/{id}")
    public Map<String, String> actualizarEntregador(@PathVariable int id, @Valid @RequestBody FlotaForm.Entregador form) {
        servicio.actualizarEntregador(id, form);
        return Map.of("mensaje", "Entregador actualizado correctamente");
    }

    @PostMapping("/entregadores/{id}/acceso")
    public Map<String, String> crearAcceso(@PathVariable int id, @Valid @RequestBody FlotaForm.Acceso form) {
        servicio.crearAcceso(id, form);
        return Map.of("mensaje", "Acceso creado correctamente");
    }

    @PutMapping("/entregadores/{id}/clave")
    public Map<String, String> cambiarClave(@PathVariable int id, @Valid @RequestBody FlotaForm.Acceso form) {
        servicio.cambiarClave(id, form.password());
        return Map.of("mensaje", "Contraseña cambiada correctamente");
    }

    // ----- Vehículos -----

    @GetMapping("/vehiculos")
    public List<Flota.VehiculoFicha> vehiculos() {
        return servicio.vehiculos();
    }

    @PostMapping("/vehiculos")
    public ResponseEntity<Map<String, Object>> crearVehiculo(@Valid @RequestBody FlotaForm.Vehiculo form) {
        int id = servicio.crearVehiculo(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "mensaje", "Vehículo registrado correctamente"));
    }

    @PutMapping("/vehiculos/{id}")
    public Map<String, String> actualizarVehiculo(@PathVariable int id, @Valid @RequestBody FlotaForm.Vehiculo form) {
        servicio.actualizarVehiculo(id, form);
        return Map.of("mensaje", "Vehículo actualizado correctamente");
    }
}

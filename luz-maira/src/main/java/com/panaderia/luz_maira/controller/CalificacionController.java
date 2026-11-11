package com.panaderia.luz_maira.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.model.Calificacion;
import com.panaderia.luz_maira.model.CalificacionForm;
import com.panaderia.luz_maira.security.UsuarioAutenticado;
import com.panaderia.luz_maira.service.CalificacionService;

import jakarta.validation.Valid;

/** /api/calificaciones (solo Cliente): calificar un pedido entregado y consultar las calificaciones propias. */
@RestController
@RequestMapping("/api/calificaciones")
public class CalificacionController {

    private final CalificacionService servicio;

    public CalificacionController(CalificacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<Calificacion> calificar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                  @Valid @RequestBody CalificacionForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.calificar(usuario, form));
    }

    @GetMapping("/mias")
    public List<Calificacion> mias(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.deCliente(usuario);
    }
}

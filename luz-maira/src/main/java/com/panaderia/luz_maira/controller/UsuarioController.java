package com.panaderia.luz_maira.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.dao.UsuarioDAO;
import com.panaderia.luz_maira.model.Rol;
import com.panaderia.luz_maira.model.UsuarioForm;
import com.panaderia.luz_maira.model.UsuarioResumen;
import com.panaderia.luz_maira.service.UsuarioService;

import jakarta.validation.Valid;

/** /api/roles y /api/usuarios (solo Administrador; ver SecurityConfig). */
@RestController
@RequestMapping("/api")
public class UsuarioController {

    private final UsuarioDAO dao;
    private final UsuarioService servicio;

    public UsuarioController(UsuarioDAO dao, UsuarioService servicio) {
        this.dao = dao;
        this.servicio = servicio;
    }

    @GetMapping("/roles")
    public List<Rol> roles() {
        return dao.roles();
    }

    @GetMapping("/usuarios")
    public List<UsuarioResumen> usuarios() {
        return dao.listar();
    }

    @PostMapping("/usuarios")
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody UsuarioForm form) {
        int id = servicio.crear(form, form.idRol());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "mensaje", "Usuario creado correctamente"));
    }
}

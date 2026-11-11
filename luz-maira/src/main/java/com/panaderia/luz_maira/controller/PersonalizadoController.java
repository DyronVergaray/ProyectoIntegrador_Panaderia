package com.panaderia.luz_maira.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.model.Pedidos;
import com.panaderia.luz_maira.model.Personalizado;
import com.panaderia.luz_maira.model.PersonalizadoForm;
import com.panaderia.luz_maira.security.UsuarioAutenticado;
import com.panaderia.luz_maira.service.PersonalizadoService;

import jakarta.validation.Valid;

/**
 * /api/personalizados. Cliente: enviar, ver las suyas y cancelar. Administrador: listar, aceptar y rechazar.
 * Los permisos están en SecurityConfig.
 */
@RestController
@RequestMapping("/api/personalizados")
public class PersonalizadoController {

    private final PersonalizadoService servicio;

    public PersonalizadoController(PersonalizadoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<Personalizado> solicitar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                                   @Valid @RequestBody PersonalizadoForm.Solicitud form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.solicitar(usuario, form));
    }

    @GetMapping("/mias")
    public List<Personalizado> mias(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servicio.deCliente(usuario);
    }

    @PutMapping("/{id}/cancelar")
    public Map<String, String> cancelar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable String id) {
        servicio.cancelar(usuario, id);
        return Map.of("mensaje", "Solicitud cancelada");
    }

    @GetMapping
    public List<Personalizado> todas(@RequestParam(required = false) String estado) {
        return servicio.todas(estado);
    }

    @PutMapping("/{id}/aceptar")
    public Map<String, Object> aceptar(@PathVariable String id, @Valid @RequestBody PersonalizadoForm.Aceptar form) {
        Pedidos.Confirmacion pedido = servicio.aceptar(id, form);
        return Map.of("idPedido", pedido.idPedido(), "mensaje", "Solicitud aceptada. Se creó el pedido N.º " + pedido.idPedido());
    }

    @PutMapping("/{id}/rechazar")
    public Map<String, String> rechazar(@PathVariable String id, @Valid @RequestBody PersonalizadoForm.Rechazar form) {
        servicio.rechazar(id, form);
        return Map.of("mensaje", "Solicitud rechazada");
    }
}

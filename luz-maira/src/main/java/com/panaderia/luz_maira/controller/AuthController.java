package com.panaderia.luz_maira.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.model.LoginForm;
import com.panaderia.luz_maira.model.UsuarioForm;
import com.panaderia.luz_maira.security.DbUserDetailsService;
import com.panaderia.luz_maira.security.UsuarioAutenticado;
import com.panaderia.luz_maira.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/** /api/login, /api/logout, /api/me y /api/registro (cuenta de cliente). */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final DbUserDetailsService usuarios;
    private final PasswordEncoder codificador;
    private final UsuarioService servicio;
    private final HttpSessionSecurityContextRepository repositorio = new HttpSessionSecurityContextRepository();

    public AuthController(DbUserDetailsService usuarios, PasswordEncoder codificador, UsuarioService servicio) {
        this.usuarios = usuarios;
        this.codificador = codificador;
        this.servicio = servicio;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginForm form, HttpServletRequest req, HttpServletResponse res) {
        UsuarioAutenticado usuario;
        try {
            usuario = (UsuarioAutenticado) usuarios.loadUserByUsername(form.email().trim().toLowerCase());
        } catch (UsernameNotFoundException e) {
            throw credencialesInvalidas();
        }
        if (!codificador.matches(form.password(), usuario.getPassword())) {
            throw credencialesInvalidas();
        }

        if (req.getSession(false) != null) {
            req.changeSessionId();      // evita reutilizar una sesión anterior
        }
        usuario.eraseCredentials();
        SecurityContext contexto = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities()));
        SecurityContextHolder.getContextHolderStrategy().setContext(contexto);
        repositorio.saveContext(contexto, req, res);      // guarda el inicio de sesión en la sesión HTTP
        return datos(usuario);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest req) {
        SecurityContextHolder.clearContext();
        HttpSession sesion = req.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        return Map.of("mensaje", "Sesión cerrada");
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return datos(usuario);
    }

    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registro(@Valid @RequestBody UsuarioForm form) {
        int id = servicio.registrarCliente(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "mensaje", "Cuenta creada correctamente"));
    }

    private Map<String, Object> datos(UsuarioAutenticado u) {
        return Map.of("id", u.getId(), "nombre", u.getNombre(), "rol", u.getRol(), "panel", u.getPanel());
    }

    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
    }
}

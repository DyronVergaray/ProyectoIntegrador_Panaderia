package com.panaderia.luz_maira.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.UsuarioDAO;
import com.panaderia.luz_maira.model.UsuarioForm;

/** Crea usuarios: valida, cifra la contraseña y guarda credenciales + usuario en una transacción. */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private final UsuarioDAO dao;
    private final PasswordEncoder codificador;

    public UsuarioService(UsuarioDAO dao, PasswordEncoder codificador) {
        this.dao = dao;
        this.codificador = codificador;
    }

    /** Alta hecha por el administrador: el rol lo elige él. */
    @Transactional
    public int crear(UsuarioForm f, Integer idRol) {
        if (idRol == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Elige un rol");
        }
        String email = f.email().trim().toLowerCase();
        if (!dao.existeRol(idRol)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol seleccionado no existe");
        }
        if (dao.existeEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        int idCredenciales = dao.insertarCredenciales(email, codificador.encode(f.password()));
        int id = dao.insertarUsuario(f, idRol, idCredenciales);
        log.info("Usuario creado: id={}, correo={}, idRol={}", id, email, idRol);
        return id;
    }

    /** Registro público: siempre crea un Cliente (el rol nunca lo decide quien se registra). */
    @Transactional
    public int registrarCliente(UsuarioForm f) {
        return crear(f, dao.idRolPorNombre("Cliente"));
    }
}

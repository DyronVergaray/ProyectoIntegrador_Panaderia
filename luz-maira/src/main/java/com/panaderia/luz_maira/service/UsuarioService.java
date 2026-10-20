package com.panaderia.luz_maira.service;

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

    private final UsuarioDAO dao;
    private final PasswordEncoder codificador;

    public UsuarioService(UsuarioDAO dao, PasswordEncoder codificador) {
        this.dao = dao;
        this.codificador = codificador;
    }

    @Transactional
    public int crear(UsuarioForm f) {
        String email = f.email().trim().toLowerCase();
        if (!dao.existeRol(f.idRol())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol seleccionado no existe");
        }
        if (dao.existeEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        int idCredenciales = dao.insertarCredenciales(email, codificador.encode(f.password()));
        return dao.insertarUsuario(f, idCredenciales);
    }
}

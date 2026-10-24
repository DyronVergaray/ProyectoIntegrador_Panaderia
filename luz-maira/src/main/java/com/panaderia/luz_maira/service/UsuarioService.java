package com.panaderia.luz_maira.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.FlotaDAO;
import com.panaderia.luz_maira.dao.UsuarioDAO;
import com.panaderia.luz_maira.model.UsuarioForm;

/**
 * Crea usuarios: valida, cifra la contraseña y guarda credenciales + usuario en una transacción.
 * Si el rol es Entregador, también crea su ficha de Conductor enlazada a la cuenta (cada entregador tiene sus credenciales).
 */
@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);
    private final UsuarioDAO dao;
    private final FlotaDAO flota;
    private final PasswordEncoder codificador;

    public UsuarioService(UsuarioDAO dao, FlotaDAO flota, PasswordEncoder codificador) {
        this.dao = dao;
        this.flota = flota;
        this.codificador = codificador;
    }

    /** Alta hecha por el administrador: el rol lo elige él. */
    @Transactional
    public int crear(UsuarioForm f, Integer idRol) {
        int id = crearCuenta(f, idRol);
        if ("Entregador".equals(dao.nombreRol(idRol))) {
            flota.crearConductor(id, f.primerNombre(), f.primerApellido(), f.segundoApellido(), f.telefono());
        }
        return id;
    }

    /** Crea solo la cuenta (credenciales + usuario), sin ficha de conductor. */
    @Transactional
    public int crearCuenta(UsuarioForm f, Integer idRol) {
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

    /** Contraseña nueva para una cuenta existente. */
    @Transactional
    public void cambiarClave(int idUsuario, String clave) {
        dao.actualizarClave(idUsuario, codificador.encode(clave));
        log.info("Contraseña cambiada por el administrador: idUsuario={}", idUsuario);
    }

    /** Registro público: siempre crea un Cliente (el rol nunca lo decide quien se registra). */
    @Transactional
    public int registrarCliente(UsuarioForm f) {
        return crear(f, dao.idRolPorNombre("Cliente"));
    }
}

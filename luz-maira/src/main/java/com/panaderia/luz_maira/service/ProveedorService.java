package com.panaderia.luz_maira.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.ProveedorDAO;
import com.panaderia.luz_maira.model.ProveedorForm;

/** Crea y edita proveedores: el RUC no se puede repetir. */
@Service
public class ProveedorService {

    private static final Logger log = LoggerFactory.getLogger(ProveedorService.class);

    private final ProveedorDAO dao;

    public ProveedorService(ProveedorDAO dao) {
        this.dao = dao;
    }

    public int crear(ProveedorForm f) {
        validarRuc(f.ruc(), 0);
        int id = dao.crear(f.nombre(), f.ruc(), f.direccion(), f.descripcion());
        log.info("Proveedor creado: id={}, nombre='{}'", id, f.nombre().trim());
        return id;
    }

    public void actualizar(int id, ProveedorForm f) {
        validarRuc(f.ruc(), id);
        if (dao.actualizar(id, f.nombre(), f.ruc(), f.direccion(), f.descripcion()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El proveedor no existe");
        }
        log.info("Proveedor actualizado: id={}, nombre='{}'", id, f.nombre().trim());
    }

    private void validarRuc(String ruc, int idExcluido) {
        if (dao.existeRuc(ruc, idExcluido)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un proveedor con ese RUC");
        }
    }
}

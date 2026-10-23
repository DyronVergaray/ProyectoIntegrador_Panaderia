package com.panaderia.luz_maira.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.MateriaDAO;
import com.panaderia.luz_maira.model.MateriaForm;

/** Crea y edita productos base (materias primas): valida la unidad de medida y que el nombre no se repita. */
@Service
public class MateriaService {

    private static final Logger log = LoggerFactory.getLogger(MateriaService.class);

    private final MateriaDAO dao;

    public MateriaService(MateriaDAO dao) {
        this.dao = dao;
    }

    public int crear(MateriaForm f) {
        int idMedicion = validar(f, 0);
        int id = dao.crear(f.nombre(), idMedicion, f.stock(), f.stockMinimo());
        log.info("Producto base creado: id={}, nombre='{}'", id, f.nombre().trim());
        return id;
    }

    public void actualizar(int id, MateriaForm f) {
        int idMedicion = validar(f, id);
        if (dao.actualizar(id, f.nombre(), idMedicion, f.stock(), f.stockMinimo()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto base no existe");
        }
        log.info("Producto base actualizado: id={}, nombre='{}', stock={}", id, f.nombre().trim(), f.stock());
    }

    /** Devuelve el id de la unidad de medida; lanza 400 (unidad inválida) o 409 (nombre repetido). */
    private int validar(MateriaForm f, int idExcluido) {
        Integer idMedicion = dao.idMedicion(f.unidad());
        if (idMedicion == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La unidad de medida no existe");
        }
        if (dao.existeNombre(f.nombre(), idExcluido)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un producto base con ese nombre");
        }
        return idMedicion;
    }
}

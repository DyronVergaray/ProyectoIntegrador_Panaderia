package com.panaderia.luz_maira.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.dao.MateriaDAO;
import com.panaderia.luz_maira.model.MateriaPrima;

/** /api/materias: materias primas con su stock y stock mínimo (almacenero y administrador). */
@RestController
@RequestMapping("/api")
public class MateriaController {

    private final MateriaDAO dao;

    public MateriaController(MateriaDAO dao) {
        this.dao = dao;
    }

    @GetMapping("/materias")
    public List<MateriaPrima> materias() {
        return dao.listar();
    }
}

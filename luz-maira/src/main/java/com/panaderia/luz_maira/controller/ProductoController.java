package com.panaderia.luz_maira.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.ProductoDAO;
import com.panaderia.luz_maira.model.Categoria;
import com.panaderia.luz_maira.model.Producto;
import com.panaderia.luz_maira.model.ProductoForm;

import jakarta.validation.Valid;

/** /api/categorias, /api/productos, /api/productos/favoritos */
@RestController
@RequestMapping("/api")
public class ProductoController {

    private final ProductoDAO dao;

    public ProductoController(ProductoDAO dao) {
        this.dao = dao;
    }

    @GetMapping("/categorias")
    public List<Categoria> categorias() {
        return dao.categorias();
    }

    @GetMapping("/productos")
    public List<Producto> productos(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) List<Integer> categorias) {
        return dao.buscar(q, categorias);
    }

    @GetMapping("/productos/favoritos")
    public List<Producto> favoritos() {
        return dao.favoritos(4);
    }

    @PostMapping("/productos")
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody ProductoForm form) {
        int id = dao.crear(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "mensaje", "Producto creado correctamente"));
    }

    @PutMapping("/productos/{id}")
    public Map<String, String> actualizar(@PathVariable int id, @Valid @RequestBody ProductoForm form) {
        if (dao.actualizar(id, form) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe");
        }
        return Map.of("mensaje", "Producto actualizado correctamente");
    }
}

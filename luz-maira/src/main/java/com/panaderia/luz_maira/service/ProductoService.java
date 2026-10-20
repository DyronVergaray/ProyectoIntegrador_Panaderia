package com.panaderia.luz_maira.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.ProductoDAO;
import com.panaderia.luz_maira.model.ProductoForm;

/** Crea y edita productos aplicando la regla de stock: los panes NO controlan stock. */
@Service
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);
    private static final String CATEGORIA_PANES = "Panes";

    private final ProductoDAO dao;

    public ProductoService(ProductoDAO dao) {
        this.dao = dao;
    }

    private record Stock(Integer actual, Integer minimo) { }

    public int crear(ProductoForm f) {
        Stock stock = validarStock(f);
        int id = dao.crear(f.nombre(), f.idCategoria(), f.descripcion(), f.precio(), stock.actual(), stock.minimo());
        log.info("Producto creado: id={}, nombre='{}'", id, f.nombre().trim());
        return id;
    }

    public void actualizar(int id, ProductoForm f) {
        Stock stock = validarStock(f);
        if (dao.actualizar(id, f.nombre(), f.idCategoria(), f.descripcion(), f.precio(), stock.actual(), stock.minimo()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe");
        }
        log.info("Producto actualizado: id={}, nombre='{}', stock={}", id, f.nombre().trim(), stock.actual());
    }

    /** Panes: el stock queda vacío (no se controla). Resto de categorías: stock y mínimo son obligatorios. */
    private Stock validarStock(ProductoForm f) {
        String categoria = dao.nombreCategoria(f.idCategoria());
        if (categoria == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no existe");
        }
        if (CATEGORIA_PANES.equals(categoria)) {
            return new Stock(null, null);
        }
        if (f.stock() == null || f.stockMinimo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa el stock y el stock mínimo del producto");
        }
        return new Stock(f.stock(), f.stockMinimo());
    }
}

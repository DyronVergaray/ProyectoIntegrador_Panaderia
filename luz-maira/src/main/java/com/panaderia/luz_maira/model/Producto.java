package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

/**
 * imagen: ruta pública (ej. /img/productos/3f2a....jpg) o null si aún no tiene foto.
 * stock / stockMinimo: null en los panes, que NO controlan stock (se hornean a diario).
 */
public record Producto(int id, String nombre, String descripcion, BigDecimal precio,
                       int idCategoria, String categoria, String imagen,
                       Integer stock, Integer stockMinimo) { }

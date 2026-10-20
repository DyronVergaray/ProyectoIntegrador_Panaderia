package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

/** imagen: ruta pública (ej. /img/productos/3-1699.jpg) o null si el producto aún no tiene foto. */
public record Producto(int id, String nombre, String descripcion, BigDecimal precio,
                       int idCategoria, String categoria, String imagen) { }

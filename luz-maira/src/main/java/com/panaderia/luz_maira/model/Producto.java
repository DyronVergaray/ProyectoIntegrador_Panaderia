package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

public record Producto(int id, String nombre, String descripcion, BigDecimal precio,
                       int idCategoria, String categoria) { }

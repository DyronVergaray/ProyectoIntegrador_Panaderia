package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

/** Materia prima del almacén. bajo = el stock llegó al mínimo (o está por debajo). */
public record MateriaPrima(int id, String nombre, String unidad, BigDecimal stock,
                           BigDecimal stockMinimo, boolean bajo) { }

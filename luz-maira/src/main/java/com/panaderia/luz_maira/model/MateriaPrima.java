package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

/**
 * Materia prima del almacén ("producto base"). bajo = el stock llegó al mínimo (o está por debajo).
 * imagen: ruta pública (ej. /img/materias/3f2a....jpg) o null si aún no tiene foto.
 */
public record MateriaPrima(int id, String nombre, String unidad, BigDecimal stock,
                           BigDecimal stockMinimo, boolean bajo, String imagen) { }

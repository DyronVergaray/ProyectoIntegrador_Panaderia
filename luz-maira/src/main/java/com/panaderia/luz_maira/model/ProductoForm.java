package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos que envía el administrador al crear o editar un producto (stock y mínimo no aplican a los panes). */
public record ProductoForm(
        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres") String nombre,
        @NotNull(message = "Elige una categoría") Integer idCategoria,
        @Size(max = 1000, message = "La descripción admite máximo 1000 caracteres") String descripcion,
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        @Digits(integer = 8, fraction = 2, message = "El precio admite máximo 2 decimales") BigDecimal precio,
        @Min(value = 0, message = "El stock no puede ser negativo") Integer stock,
        @Min(value = 0, message = "El stock mínimo no puede ser negativo") Integer stockMinimo) { }

package com.panaderia.luz_maira.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entrada o salida de un producto base. La cantidad es entera (Lista_Materia.cantidad_materia es INT)
 * y va en la unidad del producto base; para medios kilos, por ejemplo, se registra en gramos.
 */
public record MovimientoForm(
        @NotNull(message = "Elige el producto base") Integer idMateria,
        @NotBlank(message = "Elige el tipo de movimiento")
        @Pattern(regexp = "Ingreso|Salida", message = "El movimiento debe ser Ingreso o Salida") String movimiento,
        @NotNull(message = "Ingresa la cantidad")
        @Min(value = 1, message = "La cantidad mínima es 1")
        @Max(value = 1000000, message = "La cantidad es demasiado grande") Integer cantidad,
        @Size(max = 130, message = "El motivo admite máximo 130 caracteres") String descripcion) { }

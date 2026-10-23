package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos que envía el almacenero al crear o editar un producto base. La foto se sube aparte. */
public record MateriaForm(
        @NotBlank(message = "El nombre del producto base es obligatorio")
        @Size(max = 50, message = "El nombre admite máximo 50 caracteres") String nombre,
        @NotBlank(message = "Elige la unidad de medida") String unidad,
        @NotNull(message = "Ingresa la cantidad (stock)")
        @DecimalMin(value = "0", message = "La cantidad no puede ser negativa")
        @Digits(integer = 10, fraction = 2, message = "La cantidad admite máximo 2 decimales") BigDecimal stock,
        @NotNull(message = "Ingresa el stock mínimo")
        @DecimalMin(value = "0", message = "El stock mínimo no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "El stock mínimo admite máximo 2 decimales") BigDecimal stockMinimo) { }

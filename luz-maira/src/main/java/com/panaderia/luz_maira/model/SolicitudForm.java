package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Nueva solicitud de un producto base a un proveedor. La fecha de solicitud (hoy) y el estado "En camino" los pone el servidor. */
public record SolicitudForm(
        @NotNull(message = "Elige el proveedor") Integer idProveedor,
        @NotNull(message = "Elige el producto base") Integer idMateria,
        @NotNull(message = "Ingresa la cantidad")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "La cantidad admite máximo 2 decimales") BigDecimal cantidad,
        @NotBlank(message = "Elige la unidad de medida") String unidad,
        @NotNull(message = "Ingresa el precio de la solicitud")
        @DecimalMin(value = "0", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio admite máximo 2 decimales") BigDecimal precio,
        @NotBlank(message = "Elige la fecha estimada de recepción") String fechaEstimada) { }

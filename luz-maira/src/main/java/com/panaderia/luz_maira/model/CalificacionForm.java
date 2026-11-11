package com.panaderia.luz_maira.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Lo que envía el cliente al calificar un pedido entregado. */
public record CalificacionForm(
        @NotNull(message = "Falta el pedido") Integer idPedido,
        @NotNull(message = "Califica tu experiencia de compra") @Min(value = 1, message = "Las estrellas van de 1 a 5") @Max(value = 5, message = "Las estrellas van de 1 a 5") Integer compra,
        @NotNull(message = "Califica la entrega") @Min(value = 1, message = "Las estrellas van de 1 a 5") @Max(value = 5, message = "Las estrellas van de 1 a 5") Integer entrega,
        @NotNull(message = "Califica la atención") @Min(value = 1, message = "Las estrellas van de 1 a 5") @Max(value = 5, message = "Las estrellas van de 1 a 5") Integer atencion,
        @NotNull(message = "Califica la calidad de los productos") @Min(value = 1, message = "Las estrellas van de 1 a 5") @Max(value = 5, message = "Las estrellas van de 1 a 5") Integer calidad,
        @NotNull(message = "Indica si nos recomendarías") Boolean recomienda,
        @Size(max = 500, message = "El comentario admite máximo 500 caracteres") String comentario) { }

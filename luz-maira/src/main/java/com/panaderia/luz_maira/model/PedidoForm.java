package com.panaderia.luz_maira.model;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Pedido que envía el cliente (o la cajera) al consolidar.
 * No lleva precios ni código: el servidor toma los precios de la BD y genera el código de 3 dígitos.
 * fecha/hora (yyyy-MM-dd / HH:mm) solo se usan en pedidos Personalizados; direccion, en Delivery.
 */
public record PedidoForm(
        Integer idTipo,
        String fecha,
        String hora,
        @Size(max = 50, message = "La dirección admite máximo 50 caracteres") String direccion,
        @NotEmpty(message = "Agrega al menos un producto al pedido")
        @Size(max = 60, message = "El pedido tiene demasiados productos") List<@Valid Linea> items) {

    public record Linea(
            @NotNull(message = "Falta el producto") Integer idProducto,
            @NotNull(message = "Falta la cantidad") @Min(value = 1, message = "La cantidad mínima es 1")
            @Max(value = 50, message = "La cantidad máxima por producto es 50") Integer cantidad) { }
}

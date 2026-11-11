package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos que envía el administrador para crear o editar una promoción.
 * tipo MONTO usa montoMinimo; tipo CANTIDAD usa idProductoCondicion y cantidadCondicion (lo valida PromocionService).
 * fechaInicio/fechaFin: yyyy-MM-dd (opcionales). */
public record PromocionForm(
        @NotBlank(message = "Ingresa el nombre de la promoción") @Size(max = 80, message = "El nombre admite máximo 80 caracteres") String nombre,
        @Size(max = 200, message = "La descripción admite máximo 200 caracteres") String descripcion,
        @NotBlank(message = "Elige el tipo de promoción") String tipo,
        @DecimalMin(value = "0.10", message = "El monto de compra debe ser mayor a 0") BigDecimal montoMinimo,
        Integer idProductoCondicion,
        @Min(value = 1, message = "La cantidad a comprar mínima es 1") @Max(value = 500, message = "La cantidad a comprar máxima es 500") Integer cantidadCondicion,
        @NotNull(message = "Elige el producto de regalo") Integer idProducto,
        @NotNull(message = "Ingresa la cantidad de regalo") @Min(value = 1, message = "La cantidad de regalo mínima es 1")
        @Max(value = 50, message = "La cantidad de regalo máxima es 50") Integer cantidadRegalo,
        @NotNull(message = "Ingresa las veces máximas por pedido") @Min(value = 1, message = "Las veces mínimas son 1")
        @Max(value = 20, message = "Las veces máximas son 20") Integer maxVeces,
        String fechaInicio,
        String fechaFin,
        Boolean activa) { }

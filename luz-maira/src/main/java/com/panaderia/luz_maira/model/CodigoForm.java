package com.panaderia.luz_maira.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Código de 3 dígitos que el cliente muestra al recibir su pedido. */
public record CodigoForm(
        @NotBlank(message = "Ingresa el código del cliente")
        @Pattern(regexp = "\\d{3}", message = "El código tiene 3 dígitos") String codigo) { }

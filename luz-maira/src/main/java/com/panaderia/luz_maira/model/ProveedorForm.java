package com.panaderia.luz_maira.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos que envía el almacenero al crear o editar un proveedor. La foto se sube aparte. */
public record ProveedorForm(
        @NotBlank(message = "El nombre del proveedor es obligatorio")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres") String nombre,
        @NotBlank(message = "El RUC es obligatorio")
        @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos") String ruc,
        @Size(max = 255, message = "La dirección admite máximo 255 caracteres") String direccion,
        @Size(max = 150, message = "La descripción admite máximo 150 caracteres") String descripcion) { }

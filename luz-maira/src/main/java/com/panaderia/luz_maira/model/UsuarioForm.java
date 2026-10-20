package com.panaderia.luz_maira.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos que envía el administrador al crear un usuario con rol. */
public record UsuarioForm(
        @NotBlank(message = "El primer nombre es obligatorio")
        @Size(max = 20, message = "El primer nombre admite máximo 20 caracteres") String primerNombre,
        @NotBlank(message = "El primer apellido es obligatorio")
        @Size(max = 20, message = "El primer apellido admite máximo 20 caracteres") String primerApellido,
        @Size(max = 20, message = "El segundo apellido admite máximo 20 caracteres") String segundoApellido,
        @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener exactamente 9 dígitos") String telefono,
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Ingresa un correo válido")
        @Size(max = 100, message = "El correo admite máximo 100 caracteres") String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres") String password,
        @NotNull(message = "Elige un rol") Integer idRol) { }

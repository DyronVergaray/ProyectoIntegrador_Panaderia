package com.panaderia.luz_maira.model;

import jakarta.validation.constraints.NotBlank;

public record LoginForm(
        @NotBlank(message = "Ingresa tu correo") String email,
        @NotBlank(message = "Ingresa tu contraseña") String password) { }

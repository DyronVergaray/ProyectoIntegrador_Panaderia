package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos que envía el administrador para gestionar entregadores y vehículos. */
public final class FlotaForm {

    private FlotaForm() { }

    /** Edición de los datos de un entregador y de su disponibilidad (Disponible / No disponible). */
    public record Entregador(
            @NotBlank(message = "El primer nombre es obligatorio")
            @Size(max = 20, message = "El primer nombre admite máximo 20 caracteres") String primerNombre,
            @NotBlank(message = "El primer apellido es obligatorio")
            @Size(max = 20, message = "El primer apellido admite máximo 20 caracteres") String primerApellido,
            @Size(max = 20, message = "El segundo apellido admite máximo 20 caracteres") String segundoApellido,
            @Pattern(regexp = "\\d{9}", message = "El teléfono debe tener exactamente 9 dígitos") String telefono,
            String disponibilidad) { }

    /** Correo y contraseña propios de un entregador (alta de acceso o cambio de contraseña). */
    public record Acceso(
            @Email(message = "Ingresa un correo válido")
            @Size(max = 100, message = "El correo admite máximo 100 caracteres") String email,
            @NotBlank(message = "La contraseña es obligatoria")
            @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres") String password) { }

    public record Vehiculo(
            @NotBlank(message = "La placa es obligatoria")
            @Size(max = 10, message = "La placa admite máximo 10 caracteres") String placa,
            @Size(max = 50, message = "La marca admite máximo 50 caracteres") String marca,
            @Size(max = 50, message = "El modelo admite máximo 50 caracteres") String modelo,
            @DecimalMin(value = "0.0", message = "La capacidad no puede ser negativa") BigDecimal capacidad,
            String estado) { }
}

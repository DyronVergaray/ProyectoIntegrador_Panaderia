package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos del formulario de pedido personalizado y de las decisiones del administrador. */
public final class PersonalizadoForm {

    private PersonalizadoForm() { }

    /** Lo que llena el cliente. fechaEntrega: yyyy-MM-dd, horaEntrega: HH:mm. */
    public record Solicitud(
            @NotBlank(message = "Elige el tipo de producto") @Size(max = 40, message = "El tipo de producto admite máximo 40 caracteres") String tipoProducto,
            @NotBlank(message = "Describe lo que necesitas") @Size(max = 500, message = "La descripción admite máximo 500 caracteres") String descripcion,
            @Size(max = 60, message = "El sabor admite máximo 60 caracteres") String sabor,
            @NotNull(message = "Ingresa las porciones o unidades") @Min(value = 1, message = "Mínimo 1 porción") @Max(value = 500, message = "Máximo 500 porciones") Integer porciones,
            @Size(max = 300, message = "La decoración admite máximo 300 caracteres") String decoracion,
            @Size(max = 80, message = "La dedicatoria admite máximo 80 caracteres") String dedicatoria,
            @Size(max = 200, message = "Las alergias admiten máximo 200 caracteres") String alergias,
            @NotBlank(message = "Elige cómo recibirás el pedido") @Pattern(regexp = "Recojo|Delivery", message = "Modalidad no válida") String modalidad,
            @Size(max = 50, message = "La dirección admite máximo 50 caracteres") String direccion,
            @NotBlank(message = "Elige la fecha de entrega") String fechaEntrega,
            @NotBlank(message = "Elige la hora de entrega") String horaEntrega) { }

    /** El administrador acepta la solicitud y fija el precio final. */
    public record Aceptar(
            @NotNull(message = "Ingresa el precio del pedido") @DecimalMin(value = "1.00", message = "El precio mínimo es S/ 1.00")
            @DecimalMax(value = "99999.99", message = "El precio es demasiado alto") BigDecimal precio,
            @Size(max = 300, message = "La nota admite máximo 300 caracteres") String nota) { }

    /** El administrador rechaza la solicitud y explica el motivo al cliente. */
    public record Rechazar(
            @NotBlank(message = "Escribe el motivo del rechazo") @Size(max = 300, message = "El motivo admite máximo 300 caracteres") String nota) { }
}

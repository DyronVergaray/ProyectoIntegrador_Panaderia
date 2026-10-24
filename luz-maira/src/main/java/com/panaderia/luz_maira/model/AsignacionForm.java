package com.panaderia.luz_maira.model;

import jakarta.validation.constraints.NotNull;

/** Asignación de un pedido Delivery / Personalizado a un conductor y un vehículo. */
public record AsignacionForm(
        @NotNull(message = "Elige el pedido") Integer idPedido,
        @NotNull(message = "Elige el conductor") Integer idConductor,
        @NotNull(message = "Elige el vehículo") Integer idVehiculo) { }

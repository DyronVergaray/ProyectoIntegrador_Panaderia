package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

/** Modelos de lectura de la flota que ve el administrador: entregadores (conductores) y vehículos. */
public final class Flota {

    private Flota() { }

    /** email es null si el conductor todavía no tiene cuenta para iniciar sesión (el front lo muestra como "Sin acceso"). */
    public record Entregador(int id, String primerNombre, String primerApellido, String segundoApellido,
                             String telefono, String disponibilidad, String email) {
    }

    public record VehiculoFicha(int id, String placa, String marca, String modelo, BigDecimal capacidad, String estado) { }
}

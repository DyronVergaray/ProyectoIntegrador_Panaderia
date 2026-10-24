package com.panaderia.luz_maira.model;

import java.util.List;

/** Modelos de lectura del reparto: pedidos por despachar / asignar / entregar, conductores y vehículos. */
public final class Logistica {

    private Logistica() { }

    /**
     * Pedido visto por el personal operativo. estado ya viene con el nombre de cada pantalla:
     * Por despachar / Despachado, Por asignar / Asignado / Entregado, Por entregar / Entregado.
     * conductor y vehiculo (placa) son null mientras el pedido no esté asignado.
     */
    public record PedidoOperativo(int id, String cliente, String tipo, String fechaSolicitud, String fechaEntrega,
                                  String horaEntrega, String direccion, String estado, String conductor, String vehiculo) { }

    public record Conductor(int id, String nombre, String telefono) { }

    public record Vehiculo(int id, String placa, String descripcion) { }

    /** Conductores y vehículos que están disponibles para asignar. */
    public record Recursos(List<Conductor> conductores, List<Vehiculo> vehiculos) { }
}

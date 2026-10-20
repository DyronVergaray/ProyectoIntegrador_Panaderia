package com.panaderia.luz_maira.model;

import java.math.BigDecimal;
import java.util.List;

/** Respuesta completa del dashboard (todo ya calculado con los filtros aplicados). */
public record Dashboard(Rango rango, Kpis kpis, List<PuntoVenta> ventasPorDia,
                        List<ProductoVenta> productos, List<EstadoCantidad> estados,
                        List<Materia> materias, Alerta alerta) {

    public record Rango(String desde, String hasta, int dias) { }

    public record Kpis(BigDecimal ventas, Double variacionVentas, int pedidos, int entregados, int clientes) { }

    public record PuntoVenta(String fecha, BigDecimal ventas) { }

    public record ProductoVenta(int id, String nombre, long unidades, BigDecimal ventas,
                                BigDecimal ingresoProm, Double variacion) { }

    public record EstadoCantidad(String estado, int pedidos) { }

    public record Materia(String nombre, String unidad, BigDecimal consumo, double porcentaje) { }

    public record Alerta(String materia, double diasDeStock) { }
}

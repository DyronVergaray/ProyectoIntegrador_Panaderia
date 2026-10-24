package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

/** Modelos de lectura del almacén: registro del día (movimientos de productos base) y solicitudes a proveedores. */
public final class Inventario {

    private Inventario() { }

    /** descripcion empieza con "Ingreso" o "Salida" (así se distinguen en Lista_Materia, que no tiene columna de tipo). */
    public record Movimiento(String fecha, String materia, int cantidad, String unidad, String descripcion) { }

    public record Solicitud(int id, String proveedor, String materia, BigDecimal cantidad, String unidad, BigDecimal precio,
                            String fechaSolicitud, String fechaEstimada, String estado) { }
}

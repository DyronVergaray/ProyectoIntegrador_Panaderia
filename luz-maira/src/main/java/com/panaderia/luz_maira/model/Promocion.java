package com.panaderia.luz_maira.model;

import java.math.BigDecimal;

/**
 * Promoción administrable. Dos tipos de condición:
 *  - MONTO    : por cada S/ montoMinimo de compra...
 *  - CANTIDAD : por cada cantidadCondicion unidades que compres de idProductoCondicion...
 * ...llévate cantidadRegalo unidades de idProducto (gratis), hasta maxVeces veces por pedido.
 * Modelo de lectura (viaja al navegador como JSON).
 */
public record Promocion(int id, String nombre, String descripcion, String tipo, BigDecimal montoMinimo,
                        Integer idProductoCondicion, String productoCondicion, Integer cantidadCondicion,
                        int idProducto, String producto, int cantidadRegalo, int maxVeces,
                        String fechaInicio, String fechaFin, boolean activa, boolean vigente) {

    public static final String MONTO = "MONTO";
    public static final String CANTIDAD = "CANTIDAD";

    /** Promoción ya aplicada a un pedido (copia de los textos: no cambia si luego se edita la promoción). */
    public record Aplicada(String nombre, String producto, int veces, int cantidad) { }
}

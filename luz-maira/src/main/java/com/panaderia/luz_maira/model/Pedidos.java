package com.panaderia.luz_maira.model;

import java.math.BigDecimal;
import java.util.List;

/** Modelos de lectura de pedidos (listado y detalle). */
public final class Pedidos {

    private Pedidos() { }

    public record Resumen(int id, String cliente, String tipo, String fechaSolicitud,
                          String fechaEstimada, String estado, BigDecimal total) { }

    /** Respuesta al consolidar un pedido: el código de 3 dígitos se genera en el servidor. */
    public record Confirmacion(int idPedido, String codigo, BigDecimal total) { }

    public record Item(String producto, int cantidad, BigDecimal precio, BigDecimal subtotal) { }

    public record Detalle(int id, String cliente, String telefono, String email, String direccion,
                          String tipo, String codigo, String fechaSolicitud, String estado,
                          List<Item> items, BigDecimal total) { }
}

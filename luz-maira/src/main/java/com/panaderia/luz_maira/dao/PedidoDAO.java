package com.panaderia.luz_maira.dao;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.Pedidos;
import com.panaderia.luz_maira.model.TipoPedido;

/** Tablas: Pedido_Gestion, Carrito, Carrito_Objetos, Codigo y Tipo_Pedido. */
@Repository
public class PedidoDAO extends BaseDAO {

    public PedidoDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public List<TipoPedido> tipos() {
        return jdbc.query("SELECT id_tipo, nombre_tipo FROM Tipo_Pedido ORDER BY id_tipo",
                (rs, n) -> new TipoPedido(rs.getInt(1), rs.getString(2)));
    }

    /** Lista pedidos; tipo y clienteId son filtros opcionales. */
    public List<Pedidos.Resumen> listar(String tipo, Integer clienteId) {
        Clausula donde = new Clausula();
        if (tipo != null && !tipo.isBlank()) {
            donde.y("tp.nombre_tipo = ?", tipo);
        }
        if (clienteId != null) {
            donde.y("c.id_usuario = ?", clienteId);
        }
        String sql = "SELECT TOP 300 pg.id_pedido, u.primer_nombre + ' ' + u.primer_apellido, tp.nombre_tipo, "
                + "CONVERT(VARCHAR(10), pg.pedido_fecha, 23), CONVERT(VARCHAR(10), pg.fecha_asignada, 23), "
                + "pg.estado_pedido, c.precio_total "
                + "FROM Pedido_Gestion pg JOIN Carrito c ON c.id_carrito = pg.id_carrito "
                + "JOIN Usuario u ON u.id_usuario = c.id_usuario "
                + "JOIN Tipo_Pedido tp ON tp.id_tipo = pg.id_tipo"
                + donde.sql() + " ORDER BY pg.pedido_fecha DESC, pg.id_pedido DESC";
        return jdbc.query(sql, (rs, n) -> new Pedidos.Resumen(rs.getInt(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getBigDecimal(7)), donde.args());
    }

    /** Detalle completo; lanza EmptyResultDataAccessException (404) si no existe. */
    public Pedidos.Detalle detalle(int id) {
        String sql = "SELECT pg.id_pedido, u.primer_nombre + ' ' + u.primer_apellido, u.telefono, ce.email, pg.direccion, "
                + "tp.nombre_tipo, cd.codigo, CONVERT(VARCHAR(10), pg.pedido_fecha, 23), pg.estado_pedido, "
                + "c.id_carrito, c.precio_total "
                + "FROM Pedido_Gestion pg JOIN Carrito c ON c.id_carrito = pg.id_carrito "
                + "JOIN Usuario u ON u.id_usuario = c.id_usuario "
                + "JOIN Credenciales_usuario ce ON ce.id_credenciales = u.id_credenciales "
                + "JOIN Tipo_Pedido tp ON tp.id_tipo = pg.id_tipo "
                + "LEFT JOIN Codigo cd ON cd.id_pedido = pg.id_pedido WHERE pg.id_pedido = ?";

        return jdbc.queryForObject(sql, (rs, n) -> {
            List<Pedidos.Item> items = jdbc.query(
                    "SELECT p.nombre_producto, co.cantidad, co.precio FROM Carrito_Objetos co "
                            + "JOIN Producto p ON p.id_producto = co.id_producto WHERE co.id_carrito = ? "
                            + "ORDER BY co.id_objeto_carrito",
                    (r, i) -> new Pedidos.Item(r.getString(1), r.getInt(2), r.getBigDecimal(3),
                            r.getBigDecimal(3).multiply(BigDecimal.valueOf(r.getInt(2)))),
                    rs.getInt(10));
            return new Pedidos.Detalle(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                    items, rs.getBigDecimal(11));
        }, id);
    }
}

package com.panaderia.luz_maira.dao;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // ---------- Escritura (se usa dentro de la transacción de PedidoService) ----------

    public TipoPedido tipoPorId(Integer id) {
        if (id == null) {
            return null;
        }
        return jdbc.query("SELECT id_tipo, nombre_tipo FROM Tipo_Pedido WHERE id_tipo = ?",
                (rs, n) -> new TipoPedido(rs.getInt(1), rs.getString(2)), id).stream().findFirst().orElse(null);
    }

    public TipoPedido tipoPorNombre(String nombre) {
        return jdbc.query("SELECT id_tipo, nombre_tipo FROM Tipo_Pedido WHERE nombre_tipo = ?",
                (rs, n) -> new TipoPedido(rs.getInt(1), rs.getString(2)), nombre).stream().findFirst().orElse(null);
    }

    /** Precios vigentes (el precio siempre se toma de la BD, nunca del navegador). */
    public Map<Integer, BigDecimal> preciosDe(Collection<Integer> idsProducto) {
        Clausula donde = new Clausula().en("id_producto", List.copyOf(idsProducto));
        Map<Integer, BigDecimal> precios = new HashMap<>();
        jdbc.query("SELECT id_producto, precio_unitario FROM Producto" + donde.sql(),
                rs -> { precios.put(rs.getInt(1), rs.getBigDecimal(2)); }, donde.args());
        return precios;
    }

    public int insertarCarrito(int idUsuario, BigDecimal total) {
        return insertar("INSERT INTO Carrito (id_usuario, estado, precio_total) VALUES (?, 'Confirmado', ?)", idUsuario, total);
    }

    public void insertarItem(int idCarrito, int idProducto, int cantidad, BigDecimal precio) {
        jdbc.update("INSERT INTO Carrito_Objetos (id_producto, id_carrito, cantidad, precio) VALUES (?, ?, ?, ?)",
                idProducto, idCarrito, cantidad, precio);
    }

    public int insertarPedido(int idCarrito, int idTipo, Date fecha, Time hora, String estado, String direccion) {
        return insertar("INSERT INTO Pedido_Gestion (id_carrito, id_tipo, pedido_fecha, fecha_asignada, hora_asignada, "
                        + "estado_pedido, direccion) VALUES (?, ?, CAST(GETDATE() AS DATE), ?, ?, ?, ?)",
                idCarrito, idTipo, fecha, hora, estado, direccion);
    }

    public void insertarCodigo(int idPedido, String codigo) {
        jdbc.update("INSERT INTO Codigo (id_pedido, codigo) VALUES (?, ?)", idPedido, codigo);
    }

    /** ¿Algún pedido pendiente ya usa este código? (así no se repite entre pedidos por entregar). */
    public boolean codigoEnUso(String codigo) {
        return existe("SELECT COUNT(*) FROM Codigo cd JOIN Pedido_Gestion pg ON pg.id_pedido = cd.id_pedido "
                + "WHERE cd.codigo = ? AND pg.estado_pedido = 'Pendiente'", codigo);
    }

    public boolean esDelCliente(int idPedido, int idUsuario) {
        return existe("SELECT COUNT(*) FROM Pedido_Gestion pg JOIN Carrito c ON c.id_carrito = pg.id_carrito "
                + "WHERE pg.id_pedido = ? AND c.id_usuario = ?", idPedido, idUsuario);
    }
}

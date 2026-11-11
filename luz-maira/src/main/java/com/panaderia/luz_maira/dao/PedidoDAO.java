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
import com.panaderia.luz_maira.model.Promocion;
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

    private static final String SELECT_RESUMEN = "SELECT %s pg.id_pedido, u.primer_nombre + ' ' + u.primer_apellido, tp.nombre_tipo, "
            + "CONVERT(VARCHAR(10), pg.pedido_fecha, 23), CONVERT(VARCHAR(10), pg.fecha_asignada, 23), "
            + "pg.estado_pedido, c.precio_total, cd.codigo "
            + "FROM Pedido_Gestion pg JOIN Carrito c ON c.id_carrito = pg.id_carrito "
            + "JOIN Usuario u ON u.id_usuario = c.id_usuario "
            + "JOIN Credenciales_usuario ce ON ce.id_credenciales = u.id_credenciales "
            + "JOIN Tipo_Pedido tp ON tp.id_tipo = pg.id_tipo "
            + "LEFT JOIN Codigo cd ON cd.id_pedido = pg.id_pedido";

    private List<Pedidos.Resumen> consultar(String top, Clausula donde, String orden) {
        return jdbc.query(String.format(SELECT_RESUMEN, top) + donde.sql() + " ORDER BY " + orden,
                (rs, n) -> new Pedidos.Resumen(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getBigDecimal(7), rs.getString(8)), donde.args());
    }

    /** Lista pedidos (más recientes primero); tipo y clienteId son filtros opcionales. */
    public List<Pedidos.Resumen> listar(String tipo, Integer clienteId) {
        Clausula donde = new Clausula();
        if (tipo != null && !tipo.isBlank()) {
            donde.y("tp.nombre_tipo = ?", tipo);
        }
        if (clienteId != null) {
            donde.y("c.id_usuario = ?", clienteId);
        }
        return consultar("TOP 300", donde, "pg.pedido_fecha DESC, pg.id_pedido DESC");
    }

    /**
     * Historial completo del administrador. Todos los filtros son opcionales:
     * texto (N.º de pedido, código, nombre del cliente o correo), tipo, estado y rango de fechas (inclusive).
     * orden: "fecha" o "codigo"; ascendente o descendente.
     */
    public List<Pedidos.Resumen> historial(String texto, String tipo, String estado, java.time.LocalDate desde,
                                           java.time.LocalDate hasta, String orden, boolean ascendente) {
        Clausula donde = new Clausula();
        if (texto != null && !texto.isBlank()) {
            String t = texto.trim();
            String patron = "%" + t.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]") + "%";
            // El N.º de pedido solo se compara cuando el texto es un número (evita errores de conversión)
            String id = t.matches("\\d{1,9}") ? t : "-1";
            donde.y("(pg.id_pedido = ? OR cd.codigo LIKE ? OR ce.email LIKE ? OR "
                    + "(u.primer_nombre + ' ' + u.primer_apellido + ' ' + ISNULL(u.segundo_apellido, '')) LIKE ?)",
                    Integer.parseInt(id), patron, patron, patron);
        }
        if (tipo != null && !tipo.isBlank()) {
            donde.y("tp.nombre_tipo = ?", tipo);
        }
        if (estado != null && !estado.isBlank()) {
            donde.y("pg.estado_pedido = ?", estado);
        }
        if (desde != null) {
            donde.y("pg.pedido_fecha >= ?", Date.valueOf(desde));
        }
        if (hasta != null) {
            donde.y("pg.pedido_fecha < ?", Date.valueOf(hasta.plusDays(1)));
        }
        // La columna de orden sale de una lista fija (nunca del texto que envía el usuario)
        String sentido = ascendente ? " ASC" : " DESC";
        String ordenSql = "codigo".equals(orden)
                ? "cd.codigo" + sentido + ", pg.id_pedido" + sentido
                : "pg.pedido_fecha" + sentido + ", pg.id_pedido" + sentido;
        return consultar("TOP 1000", donde, ordenSql);
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
            List<Promocion.Aplicada> promociones = jdbc.query(
                    "SELECT nombre_promocion, producto, veces, cantidad FROM Pedido_Promocion WHERE id_pedido = ? ORDER BY id_pedido_promocion",
                    (r, i) -> new Promocion.Aplicada(r.getString(1), r.getString(2), r.getInt(3), r.getInt(4)), rs.getInt(1));
            return new Pedidos.Detalle(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                    items, rs.getBigDecimal(11), promociones, List.of());
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

    /** Datos del producto al momento de comprar (el precio y el stock siempre se toman de la BD). */
    public record ProductoPedido(String nombre, BigDecimal precio, Integer stock) { }

    public Map<Integer, ProductoPedido> productosParaPedido(Collection<Integer> idsProducto) {
        Clausula donde = new Clausula().en("id_producto", List.copyOf(idsProducto));
        Map<Integer, ProductoPedido> productos = new HashMap<>();
        // Los productos eliminados (categoría "Producto eliminado") no se pueden comprar aunque alguien tenga el carrito abierto
        jdbc.query("SELECT id_producto, nombre_producto, precio_unitario, stock FROM Producto" + donde.sql()
                        + " AND id_categoriaProducto NOT IN (SELECT id_categoriaProducto FROM Categoria_Producto WHERE nombre_categoria = 'Producto eliminado')",
                rs -> {
                    int stock = rs.getInt(4);
                    boolean sinControl = rs.wasNull();      // los panes tienen stock NULL: no se controla
                    productos.put(rs.getInt(1), new ProductoPedido(rs.getString(2), rs.getBigDecimal(3), sinControl ? null : stock));
                }, donde.args());
        return productos;
    }

    /** Descuenta stock de forma segura: solo si todavía alcanza (evita que dos compras dejen stock negativo). */
    public boolean descontarStock(int idProducto, int cantidad) {
        return jdbc.update("UPDATE Producto SET stock = stock - ? WHERE id_producto = ? AND stock >= ?",
                cantidad, idProducto, cantidad) == 1;
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

    /** Estado actual de un pedido (Pendiente, Entregado...). */
    public String estadoDe(int idPedido) {
        List<String> estados = jdbc.queryForList("SELECT estado_pedido FROM Pedido_Gestion WHERE id_pedido = ?", String.class, idPedido);
        return estados.isEmpty() ? null : estados.get(0);
    }

    /** Id del producto base "Pedido personalizado" (ver database/12_producto_personalizado.sql); null si no existe. */
    public Integer idProductoPersonalizado() {
        List<Integer> ids = jdbc.queryForList("SELECT TOP 1 id_producto FROM Producto WHERE nombre_producto = 'Pedido personalizado'", Integer.class);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public boolean esDelCliente(int idPedido, int idUsuario) {
        return existe("SELECT COUNT(*) FROM Pedido_Gestion pg JOIN Carrito c ON c.id_carrito = pg.id_carrito "
                + "WHERE pg.id_pedido = ? AND c.id_usuario = ?", idPedido, idUsuario);
    }
}

package com.panaderia.luz_maira.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.TipoImagen;
import com.panaderia.luz_maira.model.Categoria;
import com.panaderia.luz_maira.model.Producto;

/** Tablas: Producto y Categoria_Producto. */
@Repository
public class ProductoDAO extends BaseDAO {

    /** Categoría a la que pasan los productos "eliminados" (baja lógica: el producto sigue en la base de datos). */
    public static final String CATEGORIA_ELIMINADA = "Producto eliminado";

    /** Carpeta pública desde la que se sirven las fotos (ver WebConfig). */
    public static final String RUTA_IMAGENES = TipoImagen.PRODUCTOS.rutaPublica();

    private static final String SELECT =
            "SELECT p.id_producto, p.nombre_producto, CAST(p.descripcion AS VARCHAR(MAX)), p.precio_unitario, "
            + "c.id_categoriaProducto, c.nombre_categoria, p.imagen, p.stock, p.stock_minimo "
            + "FROM Producto p JOIN Categoria_Producto c ON c.id_categoriaProducto = p.id_categoriaProducto";

    private static final RowMapper<Producto> MAPA = (rs, n) -> {
        String imagen = rs.getString(7);
        Integer stock = enteroONulo(rs, 8);
        Integer minimo = enteroONulo(rs, 9);
        return new Producto(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getInt(5),
                rs.getString(6), imagen == null ? null : RUTA_IMAGENES + imagen, stock, minimo);
    };

    public ProductoDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    private static Integer enteroONulo(ResultSet rs, int columna) throws SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    /** puedeVerEliminados = administrador y almacenero: solo ellos ven la categoría "Producto eliminado". */
    public List<Categoria> categorias(boolean puedeVerEliminados) {
        String filtro = puedeVerEliminados ? "" : " WHERE nombre_categoria <> ?";
        return jdbc.query("SELECT id_categoriaProducto, nombre_categoria FROM Categoria_Producto" + filtro + " ORDER BY id_categoriaProducto",
                (rs, n) -> new Categoria(rs.getInt(1), rs.getString(2)), puedeVerEliminados ? new Object[0] : new Object[] { CATEGORIA_ELIMINADA });
    }

    /** Nombre de la categoría (null si no existe). */
    public String nombreCategoria(int idCategoria) {
        return jdbc.query("SELECT nombre_categoria FROM Categoria_Producto WHERE id_categoriaProducto = ?",
                (rs, n) -> rs.getString(1), idCategoria).stream().findFirst().orElse(null);
    }

    /** Id de la categoría "Producto eliminado" (null si aún no se ejecutó el script 09). */
    public Integer idCategoriaEliminada() {
        return jdbc.query("SELECT id_categoriaProducto FROM Categoria_Producto WHERE nombre_categoria = ?",
                (rs, n) -> rs.getInt(1), CATEGORIA_ELIMINADA).stream().findFirst().orElse(null);
    }

    /** Categoría actual del producto (null si el producto no existe). */
    public String categoriaDe(int idProducto) {
        return jdbc.query("SELECT c.nombre_categoria FROM Producto p JOIN Categoria_Producto c "
                + "ON c.id_categoriaProducto = p.id_categoriaProducto WHERE p.id_producto = ?",
                (rs, n) -> rs.getString(1), idProducto).stream().findFirst().orElse(null);
    }

    /** Baja lógica: solo cambia la categoría del producto. */
    public int cambiarCategoria(int idProducto, int idCategoria) {
        return jdbc.update("UPDATE Producto SET id_categoriaProducto = ? WHERE id_producto = ?", idCategoria, idProducto);
    }

    /**
     * Los productos eliminados solo se ven si quien consulta puede verlos (administrador o almacenero) Y eligió
     * explícitamente esa categoría en el filtro. En cualquier otro caso (catálogo, alertas de stock, etc.) se ocultan.
     */
    private void ocultarEliminados(Clausula donde, List<Integer> categorias, boolean puedeVerEliminados) {
        if (!puedeVerEliminados || categorias == null || categorias.isEmpty()) {
            donde.y("c.nombre_categoria <> ?", CATEGORIA_ELIMINADA);
        }
    }

    private Clausula filtro(String texto, List<Integer> categorias, boolean puedeVerEliminados) {
        Clausula donde = new Clausula();
        ocultarEliminados(donde, categorias, puedeVerEliminados);
        if (texto != null && !texto.isBlank()) {
            String patron = "%" + texto.trim() + "%";
            donde.y("(p.nombre_producto LIKE ? OR CAST(p.descripcion AS VARCHAR(MAX)) LIKE ?)", patron, patron);
        }
        return donde.en("c.id_categoriaProducto", categorias);
    }

    private List<Producto> consultar(Clausula donde) {
        return jdbc.query(SELECT + donde.sql() + " ORDER BY c.nombre_categoria, p.nombre_producto", MAPA, donde.args());
    }

    /**
     * Búsqueda por texto y categorías. soloDisponibles = oculta los productos agotados (stock 0);
     * los panes nunca se ocultan porque no controlan stock.
     */
    /*
     * Además aplica los filtros de estado de stock del administrador:
     * pocoStock = stock menor o igual a su stock mínimo; desabastecidos = stock en 0.
     * Si se piden las dos, se muestran los que cumplan alguna. Los panes (stock NULL) nunca cumplen ninguna.
     */
    public List<Producto> buscar(String texto, List<Integer> categorias, boolean soloDisponibles,
                                 boolean pocoStock, boolean desabastecidos, boolean puedeVerEliminados) {
        Clausula donde = filtro(texto, categorias, puedeVerEliminados);
        if (soloDisponibles) {
            donde.y("(p.stock IS NULL OR p.stock > 0)");
        }
        return consultar(estadoDelStock(donde, pocoStock, desabastecidos));
    }

    /** Agrega a la consulta los filtros de poco stock / desabastecidos (si no se pide ninguno, no cambia nada). */
    private Clausula estadoDelStock(Clausula donde, boolean pocoStock, boolean desabastecidos) {
        if (pocoStock && desabastecidos) {
            donde.y("(p.stock <= p.stock_minimo OR p.stock = 0)");
        } else if (pocoStock) {
            donde.y("p.stock <= p.stock_minimo");
        } else if (desabastecidos) {
            donde.y("p.stock = 0");
        }
        return donde;
    }

    /** Vista del almacenero: solo los productos que controlan stock (todos menos los panes), con los mismos filtros de estado. */
    public List<Producto> conStock(String texto, List<Integer> categorias, boolean pocoStock, boolean desabastecidos) {
        return consultar(estadoDelStock(filtro(texto, categorias, true).y("p.stock IS NOT NULL"), pocoStock, desabastecidos));
    }

    /** Los más vendidos de los últimos 7 días (sin agotados). Si no hay ventas, ordena por nombre. */
    public List<Producto> favoritos(int limite) {
        String sql = "SELECT TOP (" + limite + ") p.id_producto, p.nombre_producto, CAST(p.descripcion AS VARCHAR(MAX)), "
                + "p.precio_unitario, c.id_categoriaProducto, c.nombre_categoria, p.imagen, p.stock, p.stock_minimo "
                + "FROM Producto p JOIN Categoria_Producto c ON c.id_categoriaProducto = p.id_categoriaProducto "
                + "LEFT JOIN (SELECT co.id_producto, SUM(co.cantidad) AS unidades "
                + "           FROM Carrito_Objetos co JOIN Pedido_Gestion pg ON pg.id_carrito = co.id_carrito "
                + "           WHERE pg.pedido_fecha >= DATEADD(DAY, -7, CAST(GETDATE() AS DATE)) "
                + "             AND pg.estado_pedido <> 'Cancelado' GROUP BY co.id_producto) v ON v.id_producto = p.id_producto "
                + "WHERE (p.stock IS NULL OR p.stock > 0) AND c.nombre_categoria <> ? "
                + "ORDER BY ISNULL(v.unidades, 0) DESC, p.nombre_producto";
        return jdbc.query(sql, MAPA, CATEGORIA_ELIMINADA);
    }

    public int crear(String nombre, int idCategoria, String descripcion, java.math.BigDecimal precio, Integer stock, Integer stockMinimo) {
        return insertar("INSERT INTO Producto (id_categoriaProducto, nombre_producto, descripcion, precio_unitario, stock, stock_minimo) "
                + "VALUES (?, ?, ?, ?, ?, ?)", idCategoria, nombre.trim(), vacioANulo(descripcion), precio, stock, stockMinimo);
    }

    public int actualizar(int id, String nombre, int idCategoria, String descripcion, java.math.BigDecimal precio, Integer stock, Integer stockMinimo) {
        return jdbc.update("UPDATE Producto SET id_categoriaProducto = ?, nombre_producto = ?, descripcion = ?, "
                        + "precio_unitario = ?, stock = ?, stock_minimo = ? WHERE id_producto = ?",
                idCategoria, nombre.trim(), vacioANulo(descripcion), precio, stock, stockMinimo, id);
    }

    /** Nombre del archivo de imagen actual (null si no tiene). Lanza 404 si el producto no existe. */
    public String nombreImagen(int id) {
        return jdbc.queryForObject("SELECT imagen FROM Producto WHERE id_producto = ?", String.class, id);
    }

    public void actualizarImagen(int id, String nombreArchivo) {
        jdbc.update("UPDATE Producto SET imagen = ? WHERE id_producto = ?", nombreArchivo, id);
    }
}

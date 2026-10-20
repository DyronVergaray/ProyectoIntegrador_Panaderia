package com.panaderia.luz_maira.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.Categoria;
import com.panaderia.luz_maira.model.Producto;
import com.panaderia.luz_maira.model.ProductoForm;

/** Tablas: Producto y Categoria_Producto. */
@Repository
public class ProductoDAO extends BaseDAO {

    private static final String SELECT =
            "SELECT p.id_producto, p.nombre_producto, CAST(p.descripcion AS VARCHAR(MAX)), p.precio_unitario, "
            + "c.id_categoriaProducto, c.nombre_categoria, p.imagen "
            + "FROM Producto p JOIN Categoria_Producto c ON c.id_categoriaProducto = p.id_categoriaProducto";

    /** Carpeta pública desde la que se sirven las fotos (ver WebConfig). */
    public static final String RUTA_IMAGENES = "/img/productos/";

    private static final RowMapper<Producto> MAPA = (rs, n) -> new Producto(
            rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getInt(5), rs.getString(6),
            rs.getString(7) == null ? null : RUTA_IMAGENES + rs.getString(7));

    public ProductoDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public List<Categoria> categorias() {
        return jdbc.query("SELECT id_categoriaProducto, nombre_categoria FROM Categoria_Producto ORDER BY id_categoriaProducto",
                (rs, n) -> new Categoria(rs.getInt(1), rs.getString(2)));
    }

    /** Búsqueda por texto y por categorías (ambos opcionales). */
    public List<Producto> buscar(String texto, List<Integer> categorias) {
        Clausula donde = new Clausula();
        if (texto != null && !texto.isBlank()) {
            String patron = "%" + texto.trim() + "%";
            donde.y("(p.nombre_producto LIKE ? OR CAST(p.descripcion AS VARCHAR(MAX)) LIKE ?)", patron, patron);
        }
        donde.en("c.id_categoriaProducto", categorias);
        return jdbc.query(SELECT + donde.sql() + " ORDER BY c.nombre_categoria, p.nombre_producto", MAPA, donde.args());
    }

    /** Los más vendidos de los últimos 7 días (si no hay ventas, devuelve productos por nombre). */
    public List<Producto> favoritos(int limite) {
        String sql = "SELECT TOP (" + limite + ") p.id_producto, p.nombre_producto, CAST(p.descripcion AS VARCHAR(MAX)), "
                + "p.precio_unitario, c.id_categoriaProducto, c.nombre_categoria, p.imagen "
                + "FROM Producto p JOIN Categoria_Producto c ON c.id_categoriaProducto = p.id_categoriaProducto "
                + "LEFT JOIN (SELECT co.id_producto, SUM(co.cantidad) AS unidades "
                + "           FROM Carrito_Objetos co JOIN Pedido_Gestion pg ON pg.id_carrito = co.id_carrito "
                + "           WHERE pg.pedido_fecha >= DATEADD(DAY, -7, CAST(GETDATE() AS DATE)) "
                + "             AND pg.estado_pedido <> 'Cancelado' GROUP BY co.id_producto) v ON v.id_producto = p.id_producto "
                + "ORDER BY ISNULL(v.unidades, 0) DESC, p.nombre_producto";
        return jdbc.query(sql, MAPA);
    }

    public int crear(ProductoForm f) {
        return insertar("INSERT INTO Producto (id_categoriaProducto, nombre_producto, descripcion, precio_unitario) VALUES (?, ?, ?, ?)",
                f.idCategoria(), f.nombre().trim(), vacioANulo(f.descripcion()), f.precio());
    }

    public int actualizar(int id, ProductoForm f) {
        return jdbc.update("UPDATE Producto SET id_categoriaProducto = ?, nombre_producto = ?, descripcion = ?, "
                        + "precio_unitario = ? WHERE id_producto = ?",
                f.idCategoria(), f.nombre().trim(), vacioANulo(f.descripcion()), f.precio(), id);
    }

    /** Nombre del archivo de imagen actual (null si no tiene). Lanza 404 si el producto no existe. */
    public String nombreImagen(int id) {
        return jdbc.queryForObject("SELECT imagen FROM Producto WHERE id_producto = ?", String.class, id);
    }

    public void actualizarImagen(int id, String nombreArchivo) {
        jdbc.update("UPDATE Producto SET imagen = ? WHERE id_producto = ?", nombreArchivo, id);
    }
}

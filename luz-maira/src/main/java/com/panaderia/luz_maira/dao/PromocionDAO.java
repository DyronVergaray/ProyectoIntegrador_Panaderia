package com.panaderia.luz_maira.dao;

import java.sql.Date;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.Promocion;
import com.panaderia.luz_maira.model.PromocionForm;

/** Tablas: Promocion (reglas del administrador) y Pedido_Promocion (promociones aplicadas a cada pedido). */
@Repository
public class PromocionDAO extends BaseDAO {

    // "vigente" = activa y dentro de sus fechas (si las tiene)
    private static final String SELECT = "SELECT pr.id_promocion, pr.nombre, pr.descripcion, pr.tipo, pr.monto_minimo, "
            + "pr.id_producto_condicion, pc.nombre_producto, pr.cantidad_condicion, pr.id_producto, p.nombre_producto, "
            + "pr.cantidad_regalo, pr.max_veces, CONVERT(VARCHAR(10), pr.fecha_inicio, 23), "
            + "CONVERT(VARCHAR(10), pr.fecha_fin, 23), pr.activa, "
            + "CASE WHEN pr.activa = 1 AND (pr.fecha_inicio IS NULL OR pr.fecha_inicio <= CAST(GETDATE() AS DATE)) "
            + "AND (pr.fecha_fin IS NULL OR pr.fecha_fin >= CAST(GETDATE() AS DATE)) THEN 1 ELSE 0 END "
            + "FROM Promocion pr JOIN Producto p ON p.id_producto = pr.id_producto "
            + "LEFT JOIN Producto pc ON pc.id_producto = pr.id_producto_condicion";

    public PromocionDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    private static Integer entero(java.sql.ResultSet rs, int columna) throws java.sql.SQLException {
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    private static Promocion fila(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new Promocion(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getBigDecimal(5),
                entero(rs, 6), rs.getString(7), entero(rs, 8), rs.getInt(9), rs.getString(10),
                rs.getInt(11), rs.getInt(12), rs.getString(13), rs.getString(14),
                rs.getBoolean(15), rs.getInt(16) == 1);
    }

    public List<Promocion> todas() {
        return jdbc.query(SELECT + " ORDER BY pr.id_promocion DESC", PromocionDAO::fila);
    }

    /** Solo las que se pueden aplicar hoy (las que ve el público y las que usa el pedido). */
    public List<Promocion> vigentes() {
        return todas().stream().filter(Promocion::vigente).sorted((a, b) -> Integer.compare(a.id(), b.id())).toList();
    }

    public boolean existePromocion(int id) {
        return existe("SELECT COUNT(*) FROM Promocion WHERE id_promocion = ?", id);
    }

    /** Solo guarda los datos de la condición que corresponde al tipo (MONTO o CANTIDAD); los demás quedan en NULL. */
    public int crear(PromocionForm f, Date inicio, Date fin, boolean activa) {
        boolean porMonto = f.tipo().equals(Promocion.MONTO);
        return insertar("INSERT INTO Promocion (nombre, descripcion, tipo, monto_minimo, id_producto_condicion, cantidad_condicion, "
                + "id_producto, cantidad_regalo, max_veces, fecha_inicio, fecha_fin, activa) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                f.nombre().trim(), vacioANulo(f.descripcion()), f.tipo(), porMonto ? f.montoMinimo() : null,
                porMonto ? null : f.idProductoCondicion(), porMonto ? null : f.cantidadCondicion(),
                f.idProducto(), f.cantidadRegalo(), f.maxVeces(), inicio, fin, activa);
    }

    public void actualizar(int id, PromocionForm f, Date inicio, Date fin, boolean activa) {
        boolean porMonto = f.tipo().equals(Promocion.MONTO);
        jdbc.update("UPDATE Promocion SET nombre = ?, descripcion = ?, tipo = ?, monto_minimo = ?, id_producto_condicion = ?, "
                + "cantidad_condicion = ?, id_producto = ?, cantidad_regalo = ?, max_veces = ?, fecha_inicio = ?, fecha_fin = ?, "
                + "activa = ? WHERE id_promocion = ?",
                f.nombre().trim(), vacioANulo(f.descripcion()), f.tipo(), porMonto ? f.montoMinimo() : null,
                porMonto ? null : f.idProductoCondicion(), porMonto ? null : f.cantidadCondicion(),
                f.idProducto(), f.cantidadRegalo(), f.maxVeces(), inicio, fin, activa, id);
    }

    /** Eliminar no afecta a los pedidos ya hechos: Pedido_Promocion guarda su propia copia de los textos. */
    public void eliminar(int id) {
        jdbc.update("DELETE FROM Promocion WHERE id_promocion = ?", id);
    }

    public boolean productoExiste(int idProducto) {
        return existe("SELECT COUNT(*) FROM Producto WHERE id_producto = ?", idProducto);
    }

    // ---------- Promociones aplicadas a pedidos ----------

    public void registrarAplicada(int idPedido, Promocion.Aplicada a) {
        jdbc.update("INSERT INTO Pedido_Promocion (id_pedido, nombre_promocion, producto, veces, cantidad) VALUES (?, ?, ?, ?, ?)",
                idPedido, a.nombre(), a.producto(), a.veces(), a.cantidad());
    }
}

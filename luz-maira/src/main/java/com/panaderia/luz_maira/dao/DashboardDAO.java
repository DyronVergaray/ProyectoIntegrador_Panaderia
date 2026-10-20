package com.panaderia.luz_maira.dao;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.Dashboard;
import com.panaderia.luz_maira.model.FiltroDashboard;

/**
 * Consultas del dashboard. Cada consulta decide qué partes del filtro aplica:
 * un gráfico NO se filtra a sí mismo (como en Power BI), así sus barras siguen visibles
 * y solo se resalta la seleccionada.
 */
@Repository
public class DashboardDAO extends BaseDAO {

    private static final String BASE = " FROM Pedido_Gestion pg "
            + "JOIN Carrito c ON c.id_carrito = pg.id_carrito "
            + "JOIN Carrito_Objetos co ON co.id_carrito = c.id_carrito "
            + "JOIN Producto p ON p.id_producto = co.id_producto";

    /** Datos crudos de materias primas (el servicio calcula porcentajes y alertas). */
    public record MateriaBruta(String nombre, String unidad, BigDecimal consumo, BigDecimal stock) { }

    public DashboardDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    /** WHERE común: siempre el periodo; el resto según los booleanos. */
    private Clausula donde(FiltroDashboard f, boolean dias, boolean productos, boolean estados, boolean sinCancelados) {
        Clausula c = new Clausula().y("pg.pedido_fecha BETWEEN ? AND ?", Date.valueOf(f.desde()), Date.valueOf(f.hasta()));
        if (dias) {
            c.en("pg.pedido_fecha", f.dias().stream().map(Date::valueOf).toList());
        }
        if (productos) {
            c.en("p.id_producto", f.productos());
        }
        if (estados) {
            c.en("pg.estado_pedido", f.estados());
        }
        if (sinCancelados) {
            c.y("pg.estado_pedido <> 'Cancelado'");
        }
        return c;
    }

    /** Ventas por día (no aplica el filtro de días). */
    public Map<LocalDate, BigDecimal> ventasPorDia(FiltroDashboard f) {
        Clausula w = donde(f, false, true, true, true);
        Map<LocalDate, BigDecimal> mapa = new LinkedHashMap<>();
        jdbc.query("SELECT pg.pedido_fecha, SUM(co.cantidad * co.precio)" + BASE + w.sql()
                + " GROUP BY pg.pedido_fecha ORDER BY pg.pedido_fecha",
                rs -> {
                    mapa.put(rs.getDate(1).toLocalDate(), rs.getBigDecimal(2));
                }, w.args());
        return mapa;
    }

    /** Indicadores generales (aplica todos los filtros). Orden: ventas, pedidos, entregados, clientes. */
    public Object[] indicadores(FiltroDashboard f) {
        Clausula w = donde(f, true, true, true, false);
        return jdbc.queryForObject("SELECT ISNULL(SUM(CASE WHEN pg.estado_pedido <> 'Cancelado' THEN co.cantidad * co.precio ELSE 0 END), 0), "
                + "COUNT(DISTINCT pg.id_pedido), "
                + "COUNT(DISTINCT CASE WHEN pg.estado_pedido = 'Entregado' THEN pg.id_pedido END), "
                + "COUNT(DISTINCT c.id_usuario)" + BASE + w.sql(),
                (rs, n) -> new Object[]{rs.getBigDecimal(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)}, w.args());
    }

    /** Ventas por producto (no aplica el filtro de productos). Ordenado por unidades. */
    public List<Dashboard.ProductoVenta> productos(FiltroDashboard f) {
        Clausula w = donde(f, true, false, true, true);
        return jdbc.query("SELECT p.id_producto, p.nombre_producto, SUM(co.cantidad), SUM(co.cantidad * co.precio)" + BASE + w.sql()
                + " GROUP BY p.id_producto, p.nombre_producto ORDER BY SUM(co.cantidad) DESC",
                (rs, n) -> {
                    long unidades = rs.getLong(3);
                    BigDecimal ventas = rs.getBigDecimal(4);
                    BigDecimal promedio = unidades == 0 ? BigDecimal.ZERO
                            : ventas.divide(BigDecimal.valueOf(unidades), 2, java.math.RoundingMode.HALF_UP);
                    return new Dashboard.ProductoVenta(rs.getInt(1), rs.getString(2), unidades, ventas, promedio, null);
                }, w.args());
    }

    /** Pedidos por estado (no aplica el filtro de estados). */
    public List<Dashboard.EstadoCantidad> estados(FiltroDashboard f) {
        Clausula w = donde(f, true, true, false, false);
        return jdbc.query("SELECT pg.estado_pedido, COUNT(DISTINCT pg.id_pedido)" + BASE + w.sql()
                + " GROUP BY pg.estado_pedido",
                (rs, n) -> new Dashboard.EstadoCantidad(rs.getString(1), rs.getInt(2)), w.args());
    }

    /** Consumo de materias primas en el periodo (solo responde al periodo y a los días marcados). */
    public List<MateriaBruta> materias(FiltroDashboard f) {
        Clausula w = new Clausula().y("rd.fecha BETWEEN ? AND ?", Date.valueOf(f.desde()), Date.valueOf(f.hasta()));
        w.en("rd.fecha", f.dias().stream().map(Date::valueOf).toList());
        return jdbc.query("SELECT m.nombre_materia, u.nombre_medicion, SUM(lm.cantidad_materia), "
                + "MAX(TRY_CAST(m.stock AS DECIMAL(12, 2))) "
                + "FROM Lista_Materia lm "
                + "JOIN Registro_Diario rd ON rd.id_RegistroDiario = lm.id_RegistroDiario "
                + "JOIN Materia_P m ON m.id_materia_P = lm.id_materia_P "
                + "JOIN Medicion u ON u.id_medicion = m.id_medicion" + w.sql()
                + " GROUP BY m.nombre_materia, u.nombre_medicion",
                (rs, n) -> new MateriaBruta(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getBigDecimal(4)),
                w.args());
    }
}

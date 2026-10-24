package com.panaderia.luz_maira.dao;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.Inventario;

/**
 * Tablas: Registro_Diario, Lista_Materia (movimientos), Proveedor_solicitudes, Materia_P y Medicion.
 * El stock de Materia_P es VARCHAR en el modelo original: se lee con TRY_CAST y se escribe como texto.
 */
@Repository
public class InventarioDAO extends BaseDAO {

    /** Producto base: su unidad de medida (id y nombre). */
    public record Materia(int idMedicion, String unidad) { }

    /** Solicitud tal como está guardada (para recibirla). */
    public record SolicitudGuardada(int idMateria, int idMedicion, String unidad, BigDecimal cantidad, String estado) { }

    public InventarioDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    // ---------- Registro del día ----------

    public List<Inventario.Movimiento> movimientos() {
        return jdbc.query("SELECT TOP 200 CONVERT(VARCHAR(10), rd.fecha, 23), m.nombre_materia, lm.cantidad_materia, "
                + "u.nombre_medicion, lm.descripcion "
                + "FROM Lista_Materia lm JOIN Registro_Diario rd ON rd.id_RegistroDiario = lm.id_RegistroDiario "
                + "JOIN Materia_P m ON m.id_materia_P = lm.id_materia_P "
                + "JOIN Medicion u ON u.id_medicion = lm.id_medicion "
                + "ORDER BY rd.fecha DESC, lm.cod_lista DESC",
                (rs, n) -> new Inventario.Movimiento(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5)));
    }

    /** Unidad del producto base; null si no existe. */
    public Materia materia(int idMateria) {
        return jdbc.query("SELECT m.id_medicion, u.nombre_medicion FROM Materia_P m JOIN Medicion u ON u.id_medicion = m.id_medicion "
                + "WHERE m.id_materia_P = ?", (rs, n) -> new Materia(rs.getInt(1), rs.getString(2)), idMateria)
                .stream().findFirst().orElse(null);
    }

    /** Id del registro diario de hoy (lo crea si es el primer movimiento del día). */
    public int registroDeHoy() {
        return jdbc.query("SELECT id_RegistroDiario FROM Registro_Diario WHERE fecha = CAST(GETDATE() AS DATE)",
                (rs, n) -> rs.getInt(1)).stream().findFirst()
                .orElseGet(() -> insertar("INSERT INTO Registro_Diario (fecha) VALUES (CAST(GETDATE() AS DATE))"));
    }

    public void insertarMovimiento(int idRegistro, int idMateria, int idMedicion, int cantidad, String descripcion) {
        jdbc.update("INSERT INTO Lista_Materia (id_RegistroDiario, id_materia_P, id_medicion, cantidad_materia, descripcion) "
                + "VALUES (?, ?, ?, ?, ?)", idRegistro, idMateria, idMedicion, cantidad, descripcion);
    }

    /**
     * Suma (o resta, si es negativo) al stock del producto base. Devuelve false cuando una salida dejaría
     * el stock por debajo de 0 (así no se registra una salida de algo que no hay).
     */
    public boolean ajustarStock(int idMateria, BigDecimal cambio) {
        return jdbc.update("UPDATE Materia_P SET stock = CONVERT(VARCHAR(50), CAST(ISNULL(TRY_CAST(stock AS DECIMAL(12, 2)), 0) + ? AS DECIMAL(12, 2))) "
                + "WHERE id_materia_P = ? AND ISNULL(TRY_CAST(stock AS DECIMAL(12, 2)), 0) + ? >= 0",
                cambio, idMateria, cambio) == 1;
    }

    // ---------- Solicitudes a proveedores ----------

    public List<Inventario.Solicitud> solicitudes() {
        return jdbc.query("SELECT s.id_solicitud, pr.nombre_proveedor, m.nombre_materia, TRY_CAST(s.cantidad_producto_soli AS DECIMAL(12, 2)), "
                + "u.nombre_medicion, s.precio_solicitud, CONVERT(VARCHAR(10), s.fecha_solicitado, 23), "
                + "CONVERT(VARCHAR(10), s.fecha_estimada_entrega, 23), s.estado_solicitud "
                + "FROM Proveedor_solicitudes s JOIN Proveedor_identidad pr ON pr.id_proveedor = s.id_proveedor "
                + "JOIN Materia_P m ON m.id_materia_P = s.id_materia_P JOIN Medicion u ON u.id_medicion = s.id_medicion "
                + "ORDER BY CASE WHEN s.estado_solicitud = 'En camino' THEN 0 ELSE 1 END, s.fecha_solicitado DESC, s.id_solicitud DESC",
                (rs, n) -> new Inventario.Solicitud(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4), rs.getString(5),
                        rs.getBigDecimal(6), rs.getString(7), rs.getString(8), rs.getString(9)));
    }

    public boolean existeProveedor(int idProveedor) {
        return existe("SELECT COUNT(*) FROM Proveedor_identidad WHERE id_proveedor = ?", idProveedor);
    }

    /** Id de la unidad de medida por su nombre (Kilogramo, Gramo, Litro, Unidad); null si no existe. */
    public Integer idMedicion(String nombreUnidad) {
        return jdbc.query("SELECT id_medicion FROM Medicion WHERE nombre_medicion = ?",
                (rs, n) -> rs.getInt(1), nombreUnidad).stream().findFirst().orElse(null);
    }

    public int insertarSolicitud(int idMateria, int idProveedor, int idMedicion, BigDecimal cantidad, BigDecimal precio, Date fechaEstimada) {
        return insertar("INSERT INTO Proveedor_solicitudes (id_materia_P, id_proveedor, id_medicion, cantidad_producto_soli, "
                        + "precio_solicitud, fecha_solicitado, fecha_estimada_entrega, estado_solicitud) "
                        + "VALUES (?, ?, ?, ?, ?, CAST(GETDATE() AS DATE), ?, 'En camino')",
                idMateria, idProveedor, idMedicion, cantidad.stripTrailingZeros().toPlainString(), precio, fechaEstimada);
    }

    /** La solicitud guardada; null si no existe. */
    public SolicitudGuardada solicitud(int idSolicitud) {
        return jdbc.query("SELECT s.id_materia_P, s.id_medicion, u.nombre_medicion, TRY_CAST(s.cantidad_producto_soli AS DECIMAL(12, 2)), "
                        + "s.estado_solicitud FROM Proveedor_solicitudes s JOIN Medicion u ON u.id_medicion = s.id_medicion "
                        + "WHERE s.id_solicitud = ?",
                (rs, n) -> new SolicitudGuardada(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getBigDecimal(4), rs.getString(5)),
                idSolicitud).stream().findFirst().orElse(null);
    }

    /** Pasa la solicitud de En camino a Recibido; false si ya estaba recibida. */
    public boolean marcarRecibida(int idSolicitud) {
        return jdbc.update("UPDATE Proveedor_solicitudes SET estado_solicitud = 'Recibido' WHERE id_solicitud = ? AND estado_solicitud = 'En camino'",
                idSolicitud) == 1;
    }
}

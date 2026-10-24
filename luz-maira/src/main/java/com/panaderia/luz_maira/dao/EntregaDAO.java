package com.panaderia.luz_maira.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.Logistica;

/**
 * Tablas: Pedido_Gestion, Codigo, Pedido_Conductor, Pedido_Vehiculo, Conductor y Vehiculo.
 * Un pedido Delivery / Personalizado está "asignado" cuando tiene una fila en Pedido_Conductor;
 * así el estado del pedido sigue siendo Pendiente o Entregado y el dashboard no cambia.
 */
@Repository
public class EntregaDAO extends BaseDAO {

    /** Pedido tal como sale de la BD (el servicio le pone el nombre de estado de cada pantalla). */
    public record Fila(int id, String cliente, String tipo, String fechaSolicitud, String fechaEntrega, String horaEntrega,
                       String direccion, String estado, boolean asignado, String conductor, String vehiculo) { }

    /** Lo mínimo para validar una confirmación o una asignación. */
    public record Ficha(String tipo, String estado, boolean asignado, String codigo) { }

    private static final String SELECT = "SELECT TOP 200 pg.id_pedido, u.primer_nombre + ' ' + u.primer_apellido, tp.nombre_tipo, "
            + "CONVERT(VARCHAR(10), pg.pedido_fecha, 23), CONVERT(VARCHAR(10), pg.fecha_asignada, 23), "
            + "CONVERT(VARCHAR(5), pg.hora_asignada, 108), pg.direccion, pg.estado_pedido, pc.id_pedido, "
            + "cd.primer_nombre + ' ' + cd.primer_apellido, v.placa "
            + "FROM Pedido_Gestion pg JOIN Carrito c ON c.id_carrito = pg.id_carrito "
            + "JOIN Usuario u ON u.id_usuario = c.id_usuario JOIN Tipo_Pedido tp ON tp.id_tipo = pg.id_tipo "
            + "LEFT JOIN Pedido_Conductor pc ON pc.id_pedido = pg.id_pedido "
            + "LEFT JOIN Conductor cd ON cd.id_Conductor = pc.id_conductor "
            + "LEFT JOIN Pedido_Vehiculo pv ON pv.id_pedido = pg.id_pedido "
            + "LEFT JOIN Vehiculo v ON v.id_vehiculo = pv.id_vehiculo ";

    // Los pendientes primero (el que toca antes, arriba) y después los entregados de la última semana
    private static final String ORDEN = " ORDER BY CASE WHEN pg.estado_pedido = 'Pendiente' THEN 0 ELSE 1 END, "
            + "pg.fecha_asignada, pg.hora_asignada, pg.id_pedido";
    private static final String ENTREGADO_RECIENTE = "(pg.estado_pedido = 'Entregado' "
            + "AND pg.fecha_asignada >= DATEADD(DAY, -7, CAST(GETDATE() AS DATE)))";
    private static final String TIPOS_REPARTO = "tp.nombre_tipo IN ('Delivery', 'Personalizado')";

    public EntregaDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    private List<Fila> consultar(String donde) {
        return jdbc.query(SELECT + "WHERE " + donde + ORDEN, (rs, n) -> new Fila(rs.getInt(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                rs.getObject(9) != null, rs.getString(10), rs.getString(11)));
    }

    /** Pedidos Virtuales (el cliente los retira en tienda): por despachar y despachados esta semana. */
    public List<Fila> paraDespacho() {
        return consultar("tp.nombre_tipo = 'Virtual' AND (pg.estado_pedido = 'Pendiente' OR " + ENTREGADO_RECIENTE + ")");
    }

    /** Delivery y Personalizados: por asignar, asignados y entregados esta semana (pantalla del almacenero). */
    public List<Fila> paraAsignacion() {
        return consultar(TIPOS_REPARTO + " AND (pg.estado_pedido = 'Pendiente' OR " + ENTREGADO_RECIENTE + ")");
    }

    /**
     * Pantalla del entregador: SOLO los pedidos asignados a él (cd.id_Conductor = su ficha de conductor):
     * los que faltan entregar y los que entregó esta semana.
     */
    public List<Fila> paraEntrega(int idConductor) {
        return jdbc.query(SELECT + "WHERE " + TIPOS_REPARTO + " AND pc.id_conductor = ? AND (pg.estado_pedido = 'Pendiente' OR "
                + ENTREGADO_RECIENTE + ")" + ORDEN, (rs, n) -> new Fila(rs.getInt(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                rs.getObject(9) != null, rs.getString(10), rs.getString(11)), idConductor);
    }

    /** true si el pedido está asignado a ese conductor. */
    public boolean esDelConductor(int idPedido, int idConductor) {
        return existe("SELECT COUNT(*) FROM Pedido_Conductor WHERE id_pedido = ? AND id_conductor = ?", idPedido, idConductor);
    }

    /** Datos del pedido para validar; null si no existe. */
    public Ficha ficha(int idPedido) {
        return jdbc.query("SELECT tp.nombre_tipo, pg.estado_pedido, "
                        + "(SELECT COUNT(*) FROM Pedido_Conductor WHERE id_pedido = pg.id_pedido), cd.codigo "
                        + "FROM Pedido_Gestion pg JOIN Tipo_Pedido tp ON tp.id_tipo = pg.id_tipo "
                        + "LEFT JOIN Codigo cd ON cd.id_pedido = pg.id_pedido WHERE pg.id_pedido = ?",
                (rs, n) -> new Ficha(rs.getString(1), rs.getString(2), rs.getInt(3) > 0, rs.getString(4)), idPedido)
                .stream().findFirst().orElse(null);
    }

    public List<Logistica.Conductor> conductoresDisponibles() {
        return jdbc.query("SELECT id_Conductor, primer_nombre + ' ' + primer_apellido, telefono FROM Conductor "
                + "WHERE disponibilidad = 'Disponible' ORDER BY primer_nombre, primer_apellido",
                (rs, n) -> new Logistica.Conductor(rs.getInt(1), rs.getString(2), rs.getString(3)));
    }

    public List<Logistica.Vehiculo> vehiculosDisponibles() {
        return jdbc.query("SELECT id_vehiculo, placa, LTRIM(RTRIM(ISNULL(marca, '') + ' ' + ISNULL(modelo, ''))) FROM Vehiculo "
                + "WHERE estado_vehiculo = 'Disponible' ORDER BY placa",
                (rs, n) -> new Logistica.Vehiculo(rs.getInt(1), rs.getString(2), rs.getString(3)));
    }

    // ---------- Escritura (dentro de la transacción de EntregaService) ----------

    /** Marca el conductor como ocupado solo si seguía disponible (evita que dos personas asignen el mismo). */
    public boolean ocuparConductor(int idConductor) {
        return jdbc.update("UPDATE Conductor SET disponibilidad = 'Ocupado' WHERE id_Conductor = ? AND disponibilidad = 'Disponible'",
                idConductor) == 1;
    }

    public boolean ocuparVehiculo(int idVehiculo) {
        return jdbc.update("UPDATE Vehiculo SET estado_vehiculo = 'En ruta' WHERE id_vehiculo = ? AND estado_vehiculo = 'Disponible'",
                idVehiculo) == 1;
    }

    public void asignar(int idPedido, int idConductor, int idVehiculo) {
        jdbc.update("INSERT INTO Pedido_Conductor (id_pedido, id_conductor) VALUES (?, ?)", idPedido, idConductor);
        jdbc.update("INSERT INTO Pedido_Vehiculo (id_pedido, id_vehiculo) VALUES (?, ?)", idPedido, idVehiculo);
    }

    /** Pasa el pedido de Pendiente a Entregado; false si ya no estaba pendiente. */
    public boolean marcarEntregado(int idPedido) {
        return jdbc.update("UPDATE Pedido_Gestion SET estado_pedido = 'Entregado' WHERE id_pedido = ? AND estado_pedido = 'Pendiente'",
                idPedido) == 1;
    }

    /** Deja libres al conductor y al vehículo que llevaron el pedido. */
    public void liberarRecursos(int idPedido) {
        jdbc.update("UPDATE Conductor SET disponibilidad = 'Disponible' "
                + "WHERE id_Conductor IN (SELECT id_conductor FROM Pedido_Conductor WHERE id_pedido = ?)", idPedido);
        jdbc.update("UPDATE Vehiculo SET estado_vehiculo = 'Disponible' "
                + "WHERE id_vehiculo IN (SELECT id_vehiculo FROM Pedido_Vehiculo WHERE id_pedido = ?)", idPedido);
    }
}

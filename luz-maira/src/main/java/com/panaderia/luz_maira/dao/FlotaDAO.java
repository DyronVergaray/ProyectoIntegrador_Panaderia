package com.panaderia.luz_maira.dao;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.Flota;
import com.panaderia.luz_maira.model.FlotaForm;

/**
 * Tablas: Conductor (entregadores), Vehiculo, y Usuario / Credenciales_usuario para saber con qué correo entra cada uno.
 * Conductor.id_usuario enlaza al entregador con su cuenta (ver database/08_entregadores_credenciales.sql).
 */
@Repository
public class FlotaDAO extends BaseDAO {

    public FlotaDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    private static final String SELECT_ENTREGADOR = "SELECT c.id_Conductor, c.primer_nombre, c.primer_apellido, c.segundo_apellido, "
            + "c.telefono, c.disponibilidad, cr.email FROM Conductor c "
            + "LEFT JOIN Usuario u ON u.id_usuario = c.id_usuario "
            + "LEFT JOIN Credenciales_usuario cr ON cr.id_credenciales = u.id_credenciales ";

    private static final RowMapper<Flota.Entregador> MAPA_ENTREGADOR = (rs, n) -> new Flota.Entregador(rs.getInt(1),
            rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7));

    private static final RowMapper<Flota.VehiculoFicha> MAPA_VEHICULO = (rs, n) -> new Flota.VehiculoFicha(rs.getInt(1),
            rs.getString(2), rs.getString(3), rs.getString(4), rs.getBigDecimal(5), rs.getString(6));

    // ---------- Entregadores ----------

    public List<Flota.Entregador> entregadores() {
        return jdbc.query(SELECT_ENTREGADOR + "ORDER BY c.primer_nombre, c.primer_apellido", MAPA_ENTREGADOR);
    }

    /** null si no existe. */
    public Flota.Entregador entregador(int id) {
        return jdbc.query(SELECT_ENTREGADOR + "WHERE c.id_Conductor = ?", MAPA_ENTREGADOR, id).stream().findFirst().orElse(null);
    }

    /** id del usuario enlazado al conductor (null si aún no tiene cuenta). */
    public Integer idUsuarioDe(int idConductor) {
        return jdbc.query("SELECT id_usuario FROM Conductor WHERE id_Conductor = ?", (rs, n) -> (Integer) rs.getObject(1), idConductor)
                .stream().findFirst().orElse(null);
    }

    /** id del conductor que corresponde a la cuenta que inició sesión (null si no está enlazada). */
    public Integer idConductorDeUsuario(int idUsuario) {
        return jdbc.query("SELECT id_Conductor FROM Conductor WHERE id_usuario = ?", (rs, n) -> rs.getInt(1), idUsuario)
                .stream().findFirst().orElse(null);
    }

    public int crearConductor(Integer idUsuario, String nombre, String apellido, String apellido2, String telefono) {
        return insertar("INSERT INTO Conductor (primer_nombre, primer_apellido, segundo_apellido, telefono, disponibilidad, id_usuario) "
                + "VALUES (?, ?, ?, ?, 'Disponible', ?)", nombre.trim(), apellido.trim(), vacioANulo(apellido2), vacioANulo(telefono), idUsuario);
    }

    public void actualizarConductor(int id, FlotaForm.Entregador f, String disponibilidad) {
        jdbc.update("UPDATE Conductor SET primer_nombre = ?, primer_apellido = ?, segundo_apellido = ?, telefono = ?, disponibilidad = ? "
                        + "WHERE id_Conductor = ?", f.primerNombre().trim(), f.primerApellido().trim(), vacioANulo(f.segundoApellido()),
                vacioANulo(f.telefono()), disponibilidad, id);
    }

    /** Mantiene iguales el nombre y teléfono del conductor y los de su cuenta de usuario. */
    public void actualizarUsuario(int idUsuario, FlotaForm.Entregador f) {
        jdbc.update("UPDATE Usuario SET primer_nombre = ?, primer_apellido = ?, segundo_apellido = ?, telefono = ? WHERE id_usuario = ?",
                f.primerNombre().trim(), f.primerApellido().trim(), vacioANulo(f.segundoApellido()), vacioANulo(f.telefono()), idUsuario);
    }

    public void enlazar(int idConductor, int idUsuario) {
        jdbc.update("UPDATE Conductor SET id_usuario = ? WHERE id_Conductor = ?", idUsuario, idConductor);
    }

    // ---------- Vehículos ----------

    public List<Flota.VehiculoFicha> vehiculos() {
        return jdbc.query("SELECT id_vehiculo, placa, marca, modelo, capacidad_carga, estado_vehiculo FROM Vehiculo ORDER BY placa", MAPA_VEHICULO);
    }

    public Flota.VehiculoFicha vehiculo(int id) {
        return jdbc.query("SELECT id_vehiculo, placa, marca, modelo, capacidad_carga, estado_vehiculo FROM Vehiculo WHERE id_vehiculo = ?",
                MAPA_VEHICULO, id).stream().findFirst().orElse(null);
    }

    public boolean existePlaca(String placa, int idExcluido) {
        return existe("SELECT COUNT(*) FROM Vehiculo WHERE UPPER(placa) = UPPER(?) AND id_vehiculo <> ?", placa.trim(), idExcluido);
    }

    public int crearVehiculo(String placa, String marca, String modelo, BigDecimal capacidad) {
        return insertar("INSERT INTO Vehiculo (placa, marca, modelo, capacidad_carga, estado_vehiculo) VALUES (?, ?, ?, ?, 'Disponible')",
                placa.trim().toUpperCase(), vacioANulo(marca), vacioANulo(modelo), capacidad);
    }

    public void actualizarVehiculo(int id, FlotaForm.Vehiculo f, String estado) {
        jdbc.update("UPDATE Vehiculo SET placa = ?, marca = ?, modelo = ?, capacidad_carga = ?, estado_vehiculo = ? WHERE id_vehiculo = ?",
                f.placa().trim().toUpperCase(), vacioANulo(f.marca()), vacioANulo(f.modelo()), f.capacidad(), estado, id);
    }
}

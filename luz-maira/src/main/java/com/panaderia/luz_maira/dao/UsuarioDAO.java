package com.panaderia.luz_maira.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.Rol;
import com.panaderia.luz_maira.model.UsuarioForm;
import com.panaderia.luz_maira.model.UsuarioResumen;

/** Tablas: Usuario, Credenciales_usuario y Roles. */
@Repository
public class UsuarioDAO extends BaseDAO {

    public UsuarioDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public List<Rol> roles() {
        return jdbc.query("SELECT id_rol, nombre_rol FROM Roles ORDER BY id_rol",
                (rs, n) -> new Rol(rs.getInt(1), rs.getString(2)));
    }

    public List<UsuarioResumen> listar() {
        String sql = "SELECT u.id_usuario, "
                + "u.primer_nombre + ' ' + u.primer_apellido + ISNULL(' ' + u.segundo_apellido, ''), "
                + "c.email, u.telefono, r.nombre_rol "
                + "FROM Usuario u JOIN Roles r ON r.id_rol = u.id_rol "
                + "JOIN Credenciales_usuario c ON c.id_credenciales = u.id_credenciales "
                + "ORDER BY u.id_usuario DESC";
        return jdbc.query(sql, (rs, n) -> new UsuarioResumen(
                rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
    }

    /** id del rol por su nombre (null si no existe). */
    public Integer idRolPorNombre(String nombre) {
        List<Integer> ids = jdbc.query("SELECT id_rol FROM Roles WHERE nombre_rol = ?", (rs, n) -> rs.getInt(1), nombre);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public boolean existeEmail(String email) {
        return existe("SELECT COUNT(*) FROM Credenciales_usuario WHERE email = ?", email);
    }

    public boolean existeRol(int idRol) {
        return existe("SELECT COUNT(*) FROM Roles WHERE id_rol = ?", idRol);
    }

    public int insertarCredenciales(String email, String claveCifrada) {
        return insertar("INSERT INTO Credenciales_usuario ([contraseña], email) VALUES (?, ?)", claveCifrada, email);
    }

    public int insertarUsuario(UsuarioForm f, int idRol, int idCredenciales) {
        return insertar("INSERT INTO Usuario (id_rol, id_credenciales, primer_nombre, primer_apellido, segundo_apellido, telefono) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                idRol, idCredenciales, f.primerNombre().trim(), f.primerApellido().trim(),
                vacioANulo(f.segundoApellido()), f.telefono());
    }

    /** Cambia la contraseña (ya cifrada) de la cuenta de un usuario. */
    public void actualizarClave(int idUsuario, String claveCifrada) {
        jdbc.update("UPDATE Credenciales_usuario SET [contraseña] = ? "
                + "WHERE id_credenciales = (SELECT id_credenciales FROM Usuario WHERE id_usuario = ?)", claveCifrada, idUsuario);
    }

    /** Nombre del rol (null si no existe). */
    public String nombreRol(int idRol) {
        return jdbc.query("SELECT nombre_rol FROM Roles WHERE id_rol = ?", (rs, n) -> rs.getString(1), idRol)
                .stream().findFirst().orElse(null);
    }
}

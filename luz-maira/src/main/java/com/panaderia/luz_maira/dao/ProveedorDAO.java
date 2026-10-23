package com.panaderia.luz_maira.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.TipoImagen;
import com.panaderia.luz_maira.model.Proveedor;

/** Tabla: Proveedor_identidad. */
@Repository
public class ProveedorDAO extends BaseDAO {

    private static final RowMapper<Proveedor> MAPA = (rs, n) -> {
        String imagen = rs.getString(6);
        return new Proveedor(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                imagen == null ? null : TipoImagen.PROVEEDORES.rutaPublica() + imagen);
    };

    public ProveedorDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public List<Proveedor> listar() {
        return jdbc.query("SELECT id_proveedor, nombre_proveedor, ruc_proveedor, direccion_proveedor, descripcion, imagen "
                + "FROM Proveedor_identidad ORDER BY nombre_proveedor", MAPA);
    }

    /** ¿Ya existe otro proveedor con ese RUC? (idExcluido = el que se está editando, 0 si es nuevo) */
    public boolean existeRuc(String ruc, int idExcluido) {
        return existe("SELECT COUNT(*) FROM Proveedor_identidad WHERE ruc_proveedor = ? AND id_proveedor <> ?", ruc, idExcluido);
    }

    public int crear(String nombre, String ruc, String direccion, String descripcion) {
        return insertar("INSERT INTO Proveedor_identidad (nombre_proveedor, ruc_proveedor, direccion_proveedor, descripcion) VALUES (?, ?, ?, ?)",
                nombre.trim(), ruc, vacioANulo(direccion), vacioANulo(descripcion));
    }

    public int actualizar(int id, String nombre, String ruc, String direccion, String descripcion) {
        return jdbc.update("UPDATE Proveedor_identidad SET nombre_proveedor = ?, ruc_proveedor = ?, direccion_proveedor = ?, "
                        + "descripcion = ? WHERE id_proveedor = ?",
                nombre.trim(), ruc, vacioANulo(direccion), vacioANulo(descripcion), id);
    }

    /** Nombre del archivo de imagen actual (null si no tiene). Lanza 404 si el proveedor no existe. */
    public String nombreImagen(int id) {
        return jdbc.queryForObject("SELECT imagen FROM Proveedor_identidad WHERE id_proveedor = ?", String.class, id);
    }

    public void actualizarImagen(int id, String nombreArchivo) {
        jdbc.update("UPDATE Proveedor_identidad SET imagen = ? WHERE id_proveedor = ?", nombreArchivo, id);
    }
}

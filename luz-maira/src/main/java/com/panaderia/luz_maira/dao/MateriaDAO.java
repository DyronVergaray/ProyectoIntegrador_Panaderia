package com.panaderia.luz_maira.dao;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.TipoImagen;
import com.panaderia.luz_maira.model.MateriaPrima;

/** Tablas: Materia_P y Medicion (materias primas del almacén, llamadas "productos base" en la pantalla). */
@Repository
public class MateriaDAO extends BaseDAO {

    private static final RowMapper<MateriaPrima> MAPA = (rs, n) -> {
        BigDecimal stock = rs.getBigDecimal(4);
        BigDecimal minimo = rs.getBigDecimal(5);
        boolean bajo = stock != null && minimo != null && stock.compareTo(minimo) <= 0;
        String imagen = rs.getString(6);
        return new MateriaPrima(rs.getInt(1), rs.getString(2), rs.getString(3), stock, minimo, bajo,
                imagen == null ? null : TipoImagen.MATERIAS.rutaPublica() + imagen);
    };

    public MateriaDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public List<MateriaPrima> listar() {
        return jdbc.query("SELECT m.id_materia_P, m.nombre_materia, u.nombre_medicion, "
                + "TRY_CAST(m.stock AS DECIMAL(12, 2)), m.stock_minimo, m.imagen "
                + "FROM Materia_P m JOIN Medicion u ON u.id_medicion = m.id_medicion ORDER BY m.nombre_materia", MAPA);
    }

    /** Id de la unidad de medida (Kilogramo, Gramo, Litro, Unidad) o null si no existe. */
    public Integer idMedicion(String nombreUnidad) {
        return jdbc.query("SELECT id_medicion FROM Medicion WHERE nombre_medicion = ?",
                (rs, n) -> rs.getInt(1), nombreUnidad).stream().findFirst().orElse(null);
    }

    /** ¿Ya hay otro producto base con ese nombre? (idExcluido = el que se está editando, 0 si es nuevo) */
    public boolean existeNombre(String nombre, int idExcluido) {
        return existe("SELECT COUNT(*) FROM Materia_P WHERE nombre_materia = ? AND id_materia_P <> ?", nombre.trim(), idExcluido);
    }

    // La columna stock de Materia_P es VARCHAR en el modelo original, por eso se guarda como texto
    private static String texto(BigDecimal numero) {
        return numero.stripTrailingZeros().toPlainString();
    }

    public int crear(String nombre, int idMedicion, BigDecimal stock, BigDecimal stockMinimo) {
        return insertar("INSERT INTO Materia_P (id_medicion, nombre_materia, stock, stock_minimo) VALUES (?, ?, ?, ?)",
                idMedicion, nombre.trim(), texto(stock), stockMinimo);
    }

    public int actualizar(int id, String nombre, int idMedicion, BigDecimal stock, BigDecimal stockMinimo) {
        return jdbc.update("UPDATE Materia_P SET id_medicion = ?, nombre_materia = ?, stock = ?, stock_minimo = ? WHERE id_materia_P = ?",
                idMedicion, nombre.trim(), texto(stock), stockMinimo, id);
    }

    /** Nombre del archivo de imagen actual (null si no tiene). Lanza 404 si el producto base no existe. */
    public String nombreImagen(int id) {
        return jdbc.queryForObject("SELECT imagen FROM Materia_P WHERE id_materia_P = ?", String.class, id);
    }

    public void actualizarImagen(int id, String nombreArchivo) {
        jdbc.update("UPDATE Materia_P SET imagen = ? WHERE id_materia_P = ?", nombreArchivo, id);
    }
}

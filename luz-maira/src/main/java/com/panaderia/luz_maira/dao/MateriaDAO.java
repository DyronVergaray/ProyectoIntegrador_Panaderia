package com.panaderia.luz_maira.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.panaderia.luz_maira.model.MateriaPrima;

/** Tablas: Materia_P y Medicion (materias primas del almacén). */
@Repository
public class MateriaDAO extends BaseDAO {

    public MateriaDAO(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public List<MateriaPrima> listar() {
        return jdbc.query("SELECT m.id_materia_P, m.nombre_materia, u.nombre_medicion, "
                + "TRY_CAST(m.stock AS DECIMAL(12, 2)), m.stock_minimo "
                + "FROM Materia_P m JOIN Medicion u ON u.id_medicion = m.id_medicion ORDER BY m.nombre_materia",
                (rs, n) -> {
                    java.math.BigDecimal stock = rs.getBigDecimal(4);
                    java.math.BigDecimal minimo = rs.getBigDecimal(5);
                    boolean bajo = stock != null && minimo != null && stock.compareTo(minimo) <= 0;
                    return new MateriaPrima(rs.getInt(1), rs.getString(2), rs.getString(3), stock, minimo, bajo);
                });
    }
}

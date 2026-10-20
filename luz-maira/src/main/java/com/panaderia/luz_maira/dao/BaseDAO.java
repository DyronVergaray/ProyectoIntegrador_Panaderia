package com.panaderia.luz_maira.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/** Código común de los DAO: acceso a JdbcTemplate e INSERT que devuelve el id generado. */
public abstract class BaseDAO {

    protected final JdbcTemplate jdbc;

    protected BaseDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    protected int insertar(String sql, Object... args) {
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, llave);
        return llave.getKey().intValue();
    }

    protected boolean existe(String sql, Object... args) {
        Integer cantidad = jdbc.queryForObject(sql, Integer.class, args);
        return cantidad != null && cantidad > 0;
    }

    protected static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}

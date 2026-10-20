package com.panaderia.luz_maira.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Arma un WHERE dinámico con parámetros (evita concatenar valores y repetir código). */
public class Clausula {

    private final StringBuilder sql = new StringBuilder();
    private final List<Object> args = new ArrayList<>();

    /** Agrega una condición (la primera abre el WHERE; las siguientes usan AND). */
    public Clausula y(String condicion, Object... valores) {
        sql.append(sql.length() == 0 ? " WHERE " : " AND ").append(condicion);
        args.addAll(Arrays.asList(valores));
        return this;
    }

    /** Agrega "columna IN (?, ?, ...)" solo si la lista trae valores. */
    public Clausula en(String columna, List<?> valores) {
        if (valores == null || valores.isEmpty()) {
            return this;
        }
        String marcas = String.join(",", Collections.nCopies(valores.size(), "?"));
        return y(columna + " IN (" + marcas + ")", valores.toArray());
    }

    public String sql() {
        return sql.toString();
    }

    public Object[] args() {
        return args.toArray();
    }
}

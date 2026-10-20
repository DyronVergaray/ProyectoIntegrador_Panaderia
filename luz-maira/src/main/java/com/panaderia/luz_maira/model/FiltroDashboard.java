package com.panaderia.luz_maira.model;

import java.time.LocalDate;
import java.util.List;

/** Selección actual del dashboard: periodo + lo que se marcó en los gráficos. */
public record FiltroDashboard(LocalDate desde, LocalDate hasta, List<LocalDate> dias,
                              List<Integer> productos, List<String> estados) {

    /** Mismo filtro pero en otro periodo y sin días marcados (para comparar con el periodo anterior). */
    public FiltroDashboard conPeriodo(LocalDate nuevoDesde, LocalDate nuevoHasta) {
        return new FiltroDashboard(nuevoDesde, nuevoHasta, List.of(), productos, estados);
    }
}

package com.panaderia.luz_maira.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.panaderia.luz_maira.dao.DashboardDAO;
import com.panaderia.luz_maira.dao.DashboardDAO.MateriaBruta;
import com.panaderia.luz_maira.model.Dashboard;
import com.panaderia.luz_maira.model.FiltroDashboard;

/** Junta las consultas del dashboard y calcula variaciones, porcentajes y alertas. */
@Service
public class DashboardService {

    private static final int PRODUCTOS_VISIBLES = 5;
    private static final double DIAS_MINIMOS_STOCK = 3;

    private final DashboardDAO dao;

    public DashboardService(DashboardDAO dao) {
        this.dao = dao;
    }

    public Dashboard obtener(String rango, List<String> dias, List<Integer> productos, List<String> estados) {
        return obtener(rango, dias, productos, estados, false);
    }

    /** completo = true entrega todos los productos y materias (para el reporte de Excel). */
    public Dashboard obtener(String rango, List<String> dias, List<Integer> productos, List<String> estados, boolean completo) {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = switch (rango) {
            case "7" -> hoy.minusDays(6);
            case "mes" -> hoy.withDayOfMonth(1);
            default -> hoy.minusDays(29);
        };
        int totalDias = (int) ChronoUnit.DAYS.between(desde, hoy) + 1;

        // Solo se aceptan días marcados que estén dentro del periodo
        List<LocalDate> diasMarcados = (dias == null ? List.<String>of() : dias).stream()
                .map(LocalDate::parse).filter(d -> !d.isBefore(desde) && !d.isAfter(hoy)).toList();
        FiltroDashboard filtro = new FiltroDashboard(desde, hoy, diasMarcados,
                productos == null ? List.of() : productos, estados == null ? List.of() : estados);

        // Periodo anterior de igual duración (solo se compara si no hay días marcados)
        boolean comparar = diasMarcados.isEmpty();
        FiltroDashboard anterior = filtro.conPeriodo(desde.minusDays(totalDias), desde.minusDays(1));

        return new Dashboard(
                new Dashboard.Rango(desde.toString(), hoy.toString(), totalDias),
                indicadores(filtro, anterior, comparar),
                ventasPorDia(filtro),
                productos(filtro, anterior, comparar, completo),
                dao.estados(filtro),
                materias(filtro, completo),
                alerta(filtro, diasMarcados.isEmpty() ? totalDias : diasMarcados.size()));
    }

    // ---------- Indicadores ----------
    private Dashboard.Kpis indicadores(FiltroDashboard f, FiltroDashboard anterior, boolean comparar) {
        Object[] actual = dao.indicadores(f);
        BigDecimal ventas = (BigDecimal) actual[0];
        Double variacion = null;
        if (comparar) {
            variacion = variacion(ventas, (BigDecimal) dao.indicadores(anterior)[0]);
        }
        return new Dashboard.Kpis(ventas, variacion, (int) actual[1], (int) actual[2], (int) actual[3]);
    }

    // ---------- Ventas por día (rellena con 0 los días sin ventas) ----------
    private List<Dashboard.PuntoVenta> ventasPorDia(FiltroDashboard f) {
        Map<LocalDate, BigDecimal> reales = dao.ventasPorDia(f);
        List<Dashboard.PuntoVenta> puntos = new ArrayList<>();
        for (LocalDate d = f.desde(); !d.isAfter(f.hasta()); d = d.plusDays(1)) {
            puntos.add(new Dashboard.PuntoVenta(d.toString(), reales.getOrDefault(d, BigDecimal.ZERO)));
        }
        return puntos;
    }

    // ---------- Productos: los más vendidos + los seleccionados, con su variación ----------
    private List<Dashboard.ProductoVenta> productos(FiltroDashboard f, FiltroDashboard anterior, boolean comparar, boolean completo) {
        List<Dashboard.ProductoVenta> todos = dao.productos(f);
        Map<Integer, BigDecimal> ventasAnteriores = new HashMap<>();
        if (comparar) {
            dao.productos(anterior).forEach(p -> ventasAnteriores.put(p.id(), p.ventas()));
        }
        List<Dashboard.ProductoVenta> resultado = new ArrayList<>();
        for (int i = 0; i < todos.size(); i++) {
            Dashboard.ProductoVenta p = todos.get(i);
            if (completo || i < PRODUCTOS_VISIBLES || f.productos().contains(p.id())) {
                Double var = comparar ? variacion(p.ventas(), ventasAnteriores.get(p.id())) : null;
                resultado.add(new Dashboard.ProductoVenta(p.id(), p.nombre(), p.unidades(), p.ventas(), p.ingresoProm(), var));
            }
        }
        return resultado;
    }

    // ---------- Materias primas ----------
    private List<Dashboard.Materia> materias(FiltroDashboard f, boolean completo) {
        return dao.materias(f).stream()
                .map(m -> new Dashboard.Materia(m.nombre(), m.unidad(), m.consumo(), porcentajeConsumido(m)))
                .sorted(Comparator.comparingDouble(Dashboard.Materia::porcentaje).reversed())
                .limit(completo ? Long.MAX_VALUE : 3).toList();
    }

    /** Consumo del periodo frente al inventario (consumo + stock actual). */
    private double porcentajeConsumido(MateriaBruta m) {
        if (m.stock() == null) {
            return 0;
        }
        BigDecimal total = m.consumo().add(m.stock());
        return total.signum() == 0 ? 0 : redondear(m.consumo().doubleValue() / total.doubleValue() * 100);
    }

    /**
     * Avisa de la materia prima en riesgo: primero las que llegaron a su stock mínimo y,
     * si no hay, la que se acabaría antes (menos de 3 días de stock).
     */
    private Dashboard.Alerta alerta(FiltroDashboard f, int dias) {
        Dashboard.Alerta peor = null;
        for (MateriaBruta m : dao.materias(f)) {
            if (m.stock() == null) {
                continue;
            }
            boolean bajoMinimo = m.stockMinimo() != null && m.stock().compareTo(m.stockMinimo()) <= 0;
            double promedioDiario = m.consumo().doubleValue() / Math.max(dias, 1);
            double diasDeStock = promedioDiario <= 0 ? 999 : redondear(m.stock().doubleValue() / promedioDiario);
            if (!bajoMinimo && diasDeStock >= DIAS_MINIMOS_STOCK) {
                continue;
            }
            boolean mejor = peor == null || (bajoMinimo && !peor.bajoMinimo())
                    || (bajoMinimo == peor.bajoMinimo() && diasDeStock < peor.diasDeStock());
            if (mejor) {
                peor = new Dashboard.Alerta(m.nombre(), diasDeStock, bajoMinimo);
            }
        }
        return peor;
    }

    // ---------- Utilidades ----------
    private Double variacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) {
            return null;
        }
        return redondear(actual.subtract(anterior).doubleValue() / anterior.doubleValue() * 100);
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}

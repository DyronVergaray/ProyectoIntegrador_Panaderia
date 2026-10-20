package com.panaderia.luz_maira.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.model.Dashboard;

/**
 * Convierte el dashboard (con los filtros que el administrador tenga marcados) en un archivo Excel (.xlsx)
 * usando Apache POI: una hoja de resumen y una hoja por cada gráfico/tabla.
 */
@Service
public class ReporteExcelService {

    private static final Logger log = LoggerFactory.getLogger(ReporteExcelService.class);
    private final DashboardService dashboard;

    public ReporteExcelService(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    public byte[] generar(String rango, List<String> dias, List<Integer> productos, List<String> estados) {
        Dashboard datos = dashboard.obtener(rango, dias, productos, estados, true);
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Estilos e = new Estilos(libro);
            hojaResumen(libro, e, datos, dias, productos, estados);
            hojaVentas(libro, e, datos);
            hojaProductos(libro, e, datos);
            hojaEstados(libro, e, datos);
            hojaMaterias(libro, e, datos);
            libro.setForceFormulaRecalculation(true);      // Excel calcula los totales al abrir
            libro.write(salida);
            log.info("Reporte Excel generado: periodo {} a {}, {} bytes", datos.rango().desde(), datos.rango().hasta(), salida.size());
            return salida.toByteArray();
        } catch (IOException ex) {
            log.error("No se pudo generar el reporte Excel", ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el reporte de Excel");
        }
    }

    // ---------- Hojas ----------
    private void hojaResumen(XSSFWorkbook libro, Estilos e, Dashboard d, List<String> dias, List<Integer> productos, List<String> estados) {
        Sheet hoja = libro.createSheet("Resumen");
        anchos(hoja, 34, 44);
        texto(hoja, 0, 0, "Reporte del negocio - Panadería Luz Mayra", e.titulo);
        texto(hoja, 1, 0, "Generado el " + LocalDate.now(), e.normal);

        // Filtros aplicados (lo que estaba marcado en el dashboard)
        Map<Integer, String> nombres = new HashMap<>();
        d.productos().forEach(p -> nombres.put(p.id(), p.nombre()));
        String productosMarcados = productos == null || productos.isEmpty() ? "Todos"
                : String.join(", ", productos.stream().map(id -> nombres.getOrDefault(id, "Producto " + id)).toList());
        cabecera(hoja, 3, e, "Filtro", "Valor");
        fila(hoja, 4, e, "Periodo", d.rango().desde() + " a " + d.rango().hasta() + " (" + d.rango().dias() + " días)");
        fila(hoja, 5, e, "Días marcados", dias == null || dias.isEmpty() ? "Todos" : String.join(", ", dias));
        fila(hoja, 6, e, "Productos marcados", productosMarcados);
        fila(hoja, 7, e, "Estados marcados", estados == null || estados.isEmpty() ? "Todos" : String.join(", ", estados));

        // Indicadores
        Dashboard.Kpis k = d.kpis();
        cabecera(hoja, 9, e, "Indicador", "Valor");
        texto(hoja, 10, 0, "Ventas totales", e.normal);
        numero(hoja, 10, 1, k.ventas().doubleValue(), e.moneda);
        texto(hoja, 11, 0, "Variación vs. periodo anterior", e.normal);
        if (k.variacionVentas() == null) {
            texto(hoja, 11, 1, "Sin comparación", e.normal);
        } else {
            numero(hoja, 11, 1, k.variacionVentas(), e.porcentaje);
        }
        texto(hoja, 12, 0, "Pedidos en la selección", e.normal);
        numero(hoja, 12, 1, k.pedidos(), e.entero);
        texto(hoja, 13, 0, "Pedidos entregados", e.normal);
        numero(hoja, 13, 1, k.entregados(), e.entero);
        texto(hoja, 14, 0, "Clientes atendidos", e.normal);
        numero(hoja, 14, 1, k.clientes(), e.entero);
        if (d.alerta() != null) {
            texto(hoja, 16, 0, "ALERTA DE INVENTARIO", e.cabecera);
            texto(hoja, 16, 1, "Reponer " + d.alerta().materia() + (d.alerta().bajoMinimo()
                    ? " (por debajo del stock mínimo)" : " (stock para " + d.alerta().diasDeStock() + " días)"), e.normal);
        }
    }

    private void hojaVentas(XSSFWorkbook libro, Estilos e, Dashboard d) {
        Sheet hoja = libro.createSheet("Ventas por día");
        anchos(hoja, 16, 18);
        cabecera(hoja, 0, e, "Fecha", "Ventas (S/)");
        int fila = 1;
        for (Dashboard.PuntoVenta p : d.ventasPorDia()) {
            Row r = hoja.createRow(fila++);
            Cell fecha = r.createCell(0);
            fecha.setCellValue(LocalDate.parse(p.fecha()));
            fecha.setCellStyle(e.fecha);
            numero(hoja, fila - 1, 1, p.ventas().doubleValue(), e.moneda);
        }
        texto(hoja, fila, 0, "Total", e.cabecera);
        Cell total = hoja.getRow(fila).createCell(1);
        total.setCellFormula("SUM(B2:B" + fila + ")");
        total.setCellStyle(e.moneda);
        hoja.createFreezePane(0, 1);
    }

    private void hojaProductos(XSSFWorkbook libro, Estilos e, Dashboard d) {
        Sheet hoja = libro.createSheet("Productos");
        anchos(hoja, 34, 14, 16, 18, 14);
        cabecera(hoja, 0, e, "Producto", "Unidades", "Ventas (S/)", "Ingreso promedio (S/)", "Variación %");
        int fila = 1;
        for (Dashboard.ProductoVenta p : d.productos()) {
            Row r = hoja.createRow(fila++);
            r.createCell(0).setCellValue(p.nombre());
            numero(hoja, fila - 1, 1, p.unidades(), e.entero);
            numero(hoja, fila - 1, 2, p.ventas().doubleValue(), e.moneda);
            numero(hoja, fila - 1, 3, p.ingresoProm().doubleValue(), e.moneda);
            if (p.variacion() == null) {
                texto(hoja, fila - 1, 4, "—", e.normal);
            } else {
                numero(hoja, fila - 1, 4, p.variacion(), e.porcentaje);
            }
        }
        hoja.createFreezePane(0, 1);
    }

    private void hojaEstados(XSSFWorkbook libro, Estilos e, Dashboard d) {
        Sheet hoja = libro.createSheet("Estado de pedidos");
        anchos(hoja, 22, 14);
        cabecera(hoja, 0, e, "Estado", "Pedidos");
        int fila = 1;
        for (Dashboard.EstadoCantidad s : d.estados()) {
            Row r = hoja.createRow(fila++);
            r.createCell(0).setCellValue(s.estado());
            numero(hoja, fila - 1, 1, s.pedidos(), e.entero);
        }
    }

    private void hojaMaterias(XSSFWorkbook libro, Estilos e, Dashboard d) {
        Sheet hoja = libro.createSheet("Materias primas");
        anchos(hoja, 26, 16, 18, 22);
        cabecera(hoja, 0, e, "Materia prima", "Unidad", "Consumo", "% del inventario");
        int fila = 1;
        for (Dashboard.Materia m : d.materias()) {
            Row r = hoja.createRow(fila++);
            r.createCell(0).setCellValue(m.nombre());
            r.createCell(1).setCellValue(m.unidad());
            numero(hoja, fila - 1, 2, m.consumo().doubleValue(), e.entero);
            numero(hoja, fila - 1, 3, m.porcentaje(), e.porcentaje);
        }
    }

    // ---------- Utilidades de celdas ----------
    private void anchos(Sheet hoja, int... caracteres) {
        for (int i = 0; i < caracteres.length; i++) {
            hoja.setColumnWidth(i, caracteres[i] * 256);
        }
    }

    private void cabecera(Sheet hoja, int fila, Estilos e, String... titulos) {
        Row r = hoja.createRow(fila);
        for (int i = 0; i < titulos.length; i++) {
            Cell c = r.createCell(i);
            c.setCellValue(titulos[i]);
            c.setCellStyle(e.cabecera);
        }
    }

    private void fila(Sheet hoja, int fila, Estilos e, String a, String b) {
        texto(hoja, fila, 0, a, e.normal);
        texto(hoja, fila, 1, b, e.normal);
    }

    private void texto(Sheet hoja, int fila, int columna, String valor, CellStyle estilo) {
        Row r = hoja.getRow(fila) != null ? hoja.getRow(fila) : hoja.createRow(fila);
        Cell c = r.createCell(columna);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    private void numero(Sheet hoja, int fila, int columna, double valor, CellStyle estilo) {
        Row r = hoja.getRow(fila) != null ? hoja.getRow(fila) : hoja.createRow(fila);
        Cell c = r.createCell(columna);
        c.setCellValue(valor);
        c.setCellStyle(estilo);
    }

    /** Estilos del libro (colores de la marca: café para encabezados). */
    private static class Estilos {
        final CellStyle titulo;
        final CellStyle cabecera;
        final CellStyle normal;
        final CellStyle moneda;
        final CellStyle entero;
        final CellStyle porcentaje;
        final CellStyle fecha;

        Estilos(XSSFWorkbook libro) {
            Font negrita = libro.createFont();
            negrita.setBold(true);
            negrita.setFontHeightInPoints((short) 14);
            Font blanca = libro.createFont();
            blanca.setBold(true);
            blanca.setColor(IndexedColors.WHITE.getIndex());

            titulo = libro.createCellStyle();
            titulo.setFont(negrita);

            XSSFCellStyle encabezado = libro.createCellStyle();
            encabezado.setFont(blanca);
            encabezado.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 0x5B, (byte) 0x3A, (byte) 0x25}, null));
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            encabezado.setAlignment(HorizontalAlignment.CENTER);
            cabecera = encabezado;

            normal = libro.createCellStyle();
            moneda = formato(libro, "\"S/\" #,##0.00");
            entero = formato(libro, "#,##0");
            porcentaje = formato(libro, "0.0\"%\"");
            fecha = formato(libro, "dd/mm/yyyy");
        }

        private static CellStyle formato(XSSFWorkbook libro, String patron) {
            CellStyle estilo = libro.createCellStyle();
            estilo.setDataFormat(libro.createDataFormat().getFormat(patron));
            return estilo;
        }
    }
}

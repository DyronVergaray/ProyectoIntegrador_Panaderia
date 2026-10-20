package com.panaderia.luz_maira.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.model.Dashboard;
import com.panaderia.luz_maira.service.DashboardService;
import com.panaderia.luz_maira.service.ReporteExcelService;

/**
 * /api/dashboard?rango=30&dias=2026-10-01,2026-10-02&productos=3,5&estados=Entregado
 * rango: 7 | 30 | mes. Los demás parámetros son las selecciones hechas en los gráficos.
 */
@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService servicio;
    private final ReporteExcelService reporte;

    public DashboardController(DashboardService servicio, ReporteExcelService reporte) {
        this.servicio = servicio;
        this.reporte = reporte;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@RequestParam(defaultValue = "30") String rango,
                               @RequestParam(required = false) List<String> dias,
                               @RequestParam(required = false) List<Integer> productos,
                               @RequestParam(required = false) List<String> estados) {
        return servicio.obtener(rango, dias, productos, estados);
    }

    /** Mismo dashboard (con los mismos filtros) convertido en un archivo Excel. */
    @GetMapping("/dashboard/excel")
    public ResponseEntity<byte[]> excel(@RequestParam(defaultValue = "30") String rango,
                                        @RequestParam(required = false) List<String> dias,
                                        @RequestParam(required = false) List<Integer> productos,
                                        @RequestParam(required = false) List<String> estados) {
        byte[] archivo = reporte.generar(rango, dias, productos, estados);
        String nombre = "reporte-panaderia-" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(archivo);
    }
}

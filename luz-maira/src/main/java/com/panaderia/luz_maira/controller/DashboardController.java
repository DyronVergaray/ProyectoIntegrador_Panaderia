package com.panaderia.luz_maira.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.panaderia.luz_maira.model.Dashboard;
import com.panaderia.luz_maira.service.DashboardService;

/**
 * /api/dashboard?rango=30&dias=2026-10-01,2026-10-02&productos=3,5&estados=Entregado
 * rango: 7 | 30 | mes. Los demás parámetros son las selecciones hechas en los gráficos.
 */
@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService servicio;

    public DashboardController(DashboardService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/dashboard")
    public Dashboard dashboard(@RequestParam(defaultValue = "30") String rango,
                               @RequestParam(required = false) List<String> dias,
                               @RequestParam(required = false) List<Integer> productos,
                               @RequestParam(required = false) List<String> estados) {
        return servicio.obtener(rango, dias, productos, estados);
    }
}

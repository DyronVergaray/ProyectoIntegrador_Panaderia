package com.panaderia.luz_maira.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.panaderia.luz_maira.model.TipoImagen;
import com.panaderia.luz_maira.dao.ProveedorDAO;
import com.panaderia.luz_maira.model.Proveedor;
import com.panaderia.luz_maira.model.ProveedorForm;
import com.panaderia.luz_maira.service.ImagenService;
import com.panaderia.luz_maira.service.ProveedorService;

import jakarta.validation.Valid;

/** /api/proveedores: listado, alta, edición y foto de los proveedores. Permisos en SecurityConfig. */
@RestController
@RequestMapping("/api")
public class ProveedorController {

    private final ProveedorDAO dao;
    private final ProveedorService servicio;
    private final ImagenService imagenes;

    public ProveedorController(ProveedorDAO dao, ProveedorService servicio, ImagenService imagenes) {
        this.dao = dao;
        this.servicio = servicio;
        this.imagenes = imagenes;
    }

    @GetMapping("/proveedores")
    public List<Proveedor> proveedores() {
        return dao.listar();
    }

    @PostMapping("/proveedores")
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody ProveedorForm form) {
        int id = servicio.crear(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "mensaje", "Proveedor creado correctamente"));
    }

    @PutMapping("/proveedores/{id}")
    public Map<String, String> actualizar(@PathVariable int id, @Valid @RequestBody ProveedorForm form) {
        servicio.actualizar(id, form);
        return Map.of("mensaje", "Proveedor actualizado correctamente");
    }

    /** Sube o reemplaza la foto del proveedor (JPG, PNG o WEBP de hasta 2 MB). */
    @PostMapping(path = "/proveedores/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> subirImagen(@PathVariable int id, @RequestParam("archivo") MultipartFile archivo) {
        String anterior = dao.nombreImagen(id);          // lanza 404 si el proveedor no existe
        String nuevo = imagenes.guardar(TipoImagen.PROVEEDORES, id, archivo);
        dao.actualizarImagen(id, nuevo);
        imagenes.borrar(TipoImagen.PROVEEDORES, anterior);
        return Map.of("imagen", TipoImagen.PROVEEDORES.rutaPublica() + nuevo, "mensaje", "Imagen actualizada correctamente");
    }
}

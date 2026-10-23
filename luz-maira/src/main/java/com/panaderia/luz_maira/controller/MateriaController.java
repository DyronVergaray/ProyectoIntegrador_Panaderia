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
import com.panaderia.luz_maira.dao.MateriaDAO;
import com.panaderia.luz_maira.model.MateriaForm;
import com.panaderia.luz_maira.model.MateriaPrima;
import com.panaderia.luz_maira.service.ImagenService;
import com.panaderia.luz_maira.service.MateriaService;

import jakarta.validation.Valid;

/** /api/materias: productos base (materias primas) con su stock, stock mínimo y foto. Permisos en SecurityConfig. */
@RestController
@RequestMapping("/api")
public class MateriaController {

    private final MateriaDAO dao;
    private final MateriaService servicio;
    private final ImagenService imagenes;

    public MateriaController(MateriaDAO dao, MateriaService servicio, ImagenService imagenes) {
        this.dao = dao;
        this.servicio = servicio;
        this.imagenes = imagenes;
    }

    @GetMapping("/materias")
    public List<MateriaPrima> materias() {
        return dao.listar();
    }

    @PostMapping("/materias")
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody MateriaForm form) {
        int id = servicio.crear(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "mensaje", "Producto base creado correctamente"));
    }

    @PutMapping("/materias/{id}")
    public Map<String, String> actualizar(@PathVariable int id, @Valid @RequestBody MateriaForm form) {
        servicio.actualizar(id, form);
        return Map.of("mensaje", "Producto base actualizado correctamente");
    }

    /** Sube o reemplaza la foto del producto base (JPG, PNG o WEBP de hasta 2 MB). */
    @PostMapping(path = "/materias/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> subirImagen(@PathVariable int id, @RequestParam("archivo") MultipartFile archivo) {
        String anterior = dao.nombreImagen(id);          // lanza 404 si el producto base no existe
        String nuevo = imagenes.guardar(TipoImagen.MATERIAS, id, archivo);
        dao.actualizarImagen(id, nuevo);
        imagenes.borrar(TipoImagen.MATERIAS, anterior);
        return Map.of("imagen", TipoImagen.MATERIAS.rutaPublica() + nuevo, "mensaje", "Imagen actualizada correctamente");
    }
}

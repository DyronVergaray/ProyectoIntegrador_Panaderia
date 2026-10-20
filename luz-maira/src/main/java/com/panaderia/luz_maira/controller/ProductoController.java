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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.ProductoDAO;
import com.panaderia.luz_maira.model.Categoria;
import com.panaderia.luz_maira.model.Producto;
import com.panaderia.luz_maira.model.ProductoForm;
import com.panaderia.luz_maira.service.ImagenService;

import jakarta.validation.Valid;

/** /api/categorias, /api/productos, /api/productos/favoritos */
@RestController
@RequestMapping("/api")
public class ProductoController {

    private final ProductoDAO dao;
    private final ImagenService imagenes;

    public ProductoController(ProductoDAO dao, ImagenService imagenes) {
        this.dao = dao;
        this.imagenes = imagenes;
    }

    @GetMapping("/categorias")
    public List<Categoria> categorias() {
        return dao.categorias();
    }

    @GetMapping("/productos")
    public List<Producto> productos(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) List<Integer> categorias) {
        return dao.buscar(q, categorias);
    }

    @GetMapping("/productos/favoritos")
    public List<Producto> favoritos() {
        return dao.favoritos(4);
    }

    @PostMapping("/productos")
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody ProductoForm form) {
        int id = dao.crear(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "mensaje", "Producto creado correctamente"));
    }

    @PutMapping("/productos/{id}")
    public Map<String, String> actualizar(@PathVariable int id, @Valid @RequestBody ProductoForm form) {
        if (dao.actualizar(id, form) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe");
        }
        return Map.of("mensaje", "Producto actualizado correctamente");
    }

    /** Sube o reemplaza la foto del producto (JPG, PNG o WEBP de hasta 2 MB). */
    @PostMapping(path = "/productos/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> subirImagen(@PathVariable int id, @RequestParam("archivo") MultipartFile archivo) {
        String anterior = dao.nombreImagen(id);          // lanza 404 si el producto no existe
        String nuevo = imagenes.guardar(id, archivo);
        dao.actualizarImagen(id, nuevo);
        imagenes.borrar(anterior);
        return Map.of("imagen", ProductoDAO.RUTA_IMAGENES + nuevo, "mensaje", "Imagen actualizada correctamente");
    }
}

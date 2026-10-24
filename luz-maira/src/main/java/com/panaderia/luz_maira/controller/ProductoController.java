package com.panaderia.luz_maira.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.panaderia.luz_maira.dao.ProductoDAO;
import com.panaderia.luz_maira.model.Categoria;
import com.panaderia.luz_maira.model.Producto;
import com.panaderia.luz_maira.model.ProductoForm;
import com.panaderia.luz_maira.security.UsuarioAutenticado;
import com.panaderia.luz_maira.service.ImagenService;
import com.panaderia.luz_maira.service.ProductoService;

import jakarta.validation.Valid;

/** /api/categorias, /api/productos, /api/productos/favoritos y /api/stock. Permisos en SecurityConfig. */
@RestController
@RequestMapping("/api")
public class ProductoController {

    private final ProductoDAO dao;
    private final ProductoService servicio;
    private final ImagenService imagenes;

    public ProductoController(ProductoDAO dao, ProductoService servicio, ImagenService imagenes) {
        this.dao = dao;
        this.servicio = servicio;
        this.imagenes = imagenes;
    }

    @GetMapping("/categorias")
    public List<Categoria> categorias() {
        return dao.categorias();
    }

    /**
     * Catálogo (público): nadie ve los agotados excepto el administrador, que los necesita para editarlos.
     * Solo el administrador puede filtrar por estado del stock:
     *   pocoStock = stock menor o igual a su stock mínimo (incluye los agotados)
     *   desabastecidos = stock en 0
     * Si marca las dos, salen los productos que cumplan cualquiera de ellas.
     */
    @GetMapping("/productos")
    public List<Producto> productos(@AuthenticationPrincipal UsuarioAutenticado usuario,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false) List<Integer> categorias,
                                    @RequestParam(defaultValue = "false") boolean pocoStock,
                                    @RequestParam(defaultValue = "false") boolean desabastecidos) {
        boolean esAdmin = usuario != null && usuario.getRol().equals("Administrador");
        return dao.buscar(q, categorias, !esAdmin, esAdmin && pocoStock, esAdmin && desabastecidos);
    }

    @GetMapping("/productos/favoritos")
    public List<Producto> favoritos() {
        return dao.favoritos(4);
    }

    /**
     * Productos que controlan stock (todos menos los panes), para el almacenero y el administrador.
     * Mismos filtros que /api/productos: pocoStock (stock <= mínimo) y desabastecidos (stock = 0).
     */
    @GetMapping("/stock")
    public List<Producto> stock(@RequestParam(required = false) String q,
                                @RequestParam(required = false) List<Integer> categorias,
                                @RequestParam(defaultValue = "false") boolean pocoStock,
                                @RequestParam(defaultValue = "false") boolean desabastecidos) {
        return dao.conStock(q, categorias, pocoStock, desabastecidos);
    }

    @PostMapping("/productos")
    public ResponseEntity<Map<String, Object>> crear(@Valid @RequestBody ProductoForm form) {
        int id = servicio.crear(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id, "mensaje", "Producto creado correctamente"));
    }

    @PutMapping("/productos/{id}")
    public Map<String, String> actualizar(@PathVariable int id, @Valid @RequestBody ProductoForm form) {
        servicio.actualizar(id, form);
        return Map.of("mensaje", "Producto actualizado correctamente");
    }

    /** Sube o reemplaza la foto del producto (JPG, PNG o WEBP de hasta 2 MB). */
    @PostMapping(path = "/productos/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> subirImagen(@PathVariable int id, @RequestParam("archivo") MultipartFile archivo) {
        String anterior = dao.nombreImagen(id);          // lanza 404 si el producto no existe
        String nuevo = imagenes.guardar(TipoImagen.PRODUCTOS, id, archivo);
        dao.actualizarImagen(id, nuevo);
        imagenes.borrar(TipoImagen.PRODUCTOS, anterior);
        return Map.of("imagen", TipoImagen.PRODUCTOS.rutaPublica() + nuevo, "mensaje", "Imagen actualizada correctamente");
    }
}

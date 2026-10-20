package com.panaderia.luz_maira.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Guarda las fotos de productos en una carpeta del disco (app.imagenes.carpeta) y valida que sean imágenes reales. */
@Service
public class ImagenService {

    private static final Logger log = LoggerFactory.getLogger(ImagenService.class);
    private static final Map<String, String> EXTENSIONES = Map.of("image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
    private static final long MAXIMO_BYTES = 2L * 1024 * 1024;

    private final Path carpeta;

    public ImagenService(@Value("${app.imagenes.carpeta:imagenes-productos}") String ruta) throws IOException {
        this.carpeta = Path.of(ruta).toAbsolutePath().normalize();
        Files.createDirectories(carpeta);
        log.info("Las imágenes de productos se guardan en: {}", carpeta);
    }

    public Path carpeta() {
        return carpeta;
    }

    /** Guarda la imagen y devuelve el nombre del archivo (el nombre lo genera el servidor, nunca el usuario). */
    public String guardar(int idProducto, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw error("Elige una imagen");
        }
        String extension = EXTENSIONES.get(archivo.getContentType());
        if (extension == null) {
            log.warn("Imagen rechazada para el producto {}: tipo {}", idProducto, archivo.getContentType());
            throw error("Solo se permiten imágenes JPG, PNG o WEBP");
        }
        if (archivo.getSize() > MAXIMO_BYTES) {
            throw error("La imagen supera el tamaño máximo permitido (2 MB)");
        }
        try {
            if (!esImagenReal(archivo.getBytes(), extension)) {
                throw error("El archivo no es una imagen válida");
            }
            // UUID: nunca se repite el nombre, aunque dos personas suban el mismo archivo a la vez
            String nombre = UUID.randomUUID() + extension;
            archivo.transferTo(carpeta.resolve(nombre));
            log.info("Imagen guardada para el producto {}: {}", idProducto, nombre);
            return nombre;
        } catch (IOException e) {
            log.error("No se pudo guardar la imagen del producto {}", idProducto, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen");
        }
    }

    /** Elimina una imagen anterior (si existe y está dentro de la carpeta). */
    public void borrar(String nombre) {
        if (nombre == null) {
            return;
        }
        try {
            Path archivo = carpeta.resolve(nombre).normalize();
            if (archivo.startsWith(carpeta)) {
                Files.deleteIfExists(archivo);
            }
        } catch (IOException e) {
            // Si no se puede borrar, no pasa nada: solo queda un archivo sin usar
        }
    }

    /** Revisa los primeros bytes del archivo (firma) para no confiar solo en el tipo que declara el navegador. */
    private boolean esImagenReal(byte[] b, String extension) {
        if (b.length < 12) {
            return false;
        }
        return switch (extension) {
            case ".jpg" -> (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case ".png" -> (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            default -> b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
        };
    }

    private ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}

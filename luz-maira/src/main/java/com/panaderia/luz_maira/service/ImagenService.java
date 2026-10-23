package com.panaderia.luz_maira.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.model.TipoImagen;

/**
 * Guarda las fotos en el disco y valida que sean imágenes reales (JPG, PNG o WEBP de hasta 2 MB).
 * Sirve para los tres tipos de foto del sistema: productos, productos base (materias primas) y proveedores.
 * Cada tipo tiene su propia carpeta y su propia ruta pública (ver WebConfig).
 */
@Service
public class ImagenService {

    private static final Logger log = LoggerFactory.getLogger(ImagenService.class);
    private static final Map<String, String> EXTENSIONES = Map.of("image/jpeg", ".jpg", "image/png", ".png", "image/webp", ".webp");
    private static final long MAXIMO_BYTES = 2L * 1024 * 1024;

    private final Map<TipoImagen, Path> carpetas = new EnumMap<>(TipoImagen.class);

    /**
     * app.imagenes.carpeta es la carpeta de las fotos de PRODUCTOS (como siempre).
     * Las de productos base y proveedores se guardan en carpetas "hermanas" (mismo lugar, otro nombre),
     * por ejemplo .../img/productos, .../img/materias y .../img/proveedores.
     */
    public ImagenService(@Value("${app.imagenes.carpeta:imagenes-productos}") String ruta) throws IOException {
        Path configurada = Path.of(ruta);
        // Ruta relativa: se cuenta desde la raíz del proyecto (donde está el pom.xml), no desde la carpeta
        // en la que el IDE lance la aplicación. Ruta absoluta: se usa tal cual.
        Path productos = (configurada.isAbsolute() ? configurada : raizProyecto().resolve(configurada)).toAbsolutePath().normalize();
        carpetas.put(TipoImagen.PRODUCTOS, productos);
        carpetas.put(TipoImagen.MATERIAS, productos.resolveSibling(TipoImagen.MATERIAS.carpeta()));
        carpetas.put(TipoImagen.PROVEEDORES, productos.resolveSibling(TipoImagen.PROVEEDORES.carpeta()));
        for (Map.Entry<TipoImagen, Path> e : carpetas.entrySet()) {
            Files.createDirectories(e.getValue());
            log.info("Las imágenes de {} se guardan en: {}", e.getKey().carpeta(), e.getValue());
        }
    }

    /** En desarrollo las clases viven en <proyecto>/target/classes; de ahí se obtiene la raíz del proyecto. */
    private static Path raizProyecto() {
        try {
            Path clases = Path.of(ImagenService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isDirectory(clases) && clases.endsWith(Path.of("target", "classes"))) {
                return clases.getParent().getParent();
            }
        } catch (Exception e) {
            log.debug("No se pudo ubicar la raíz del proyecto; se usa la carpeta de ejecución", e);
        }
        return Path.of("").toAbsolutePath();       // ejecutando desde un .jar: carpeta actual
    }

    public Path carpeta(TipoImagen tipo) {
        return carpetas.get(tipo);
    }

    /** Guarda la imagen y devuelve el nombre del archivo (el nombre lo genera el servidor, nunca el usuario). */
    public String guardar(TipoImagen tipo, int id, MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw error("Elige una imagen");
        }
        if (!EXTENSIONES.containsKey(archivo.getContentType())) {
            log.warn("Imagen rechazada ({} {}): tipo {}", tipo.carpeta(), id, archivo.getContentType());
            throw error("Solo se permiten imágenes JPG, PNG o WEBP");
        }
        if (archivo.getSize() > MAXIMO_BYTES) {
            throw error("La imagen supera el tamaño máximo permitido (2 MB)");
        }
        try {
            byte[] bytes = archivo.getBytes();
            // El formato se decide por el CONTENIDO real del archivo, no por su extensión: muchas imágenes descargadas
            // de internet se llaman .jpg o .png pero en realidad son WEBP (o al revés) y no por eso son inválidas.
            String extension = extensionReal(bytes);
            if (extension == null) {
                log.warn("Imagen rechazada ({} {}): declarada como {}, pero sus primeros bytes son {}",
                        tipo.carpeta(), id, archivo.getContentType(), primerosBytes(bytes));
                throw error("El archivo no es una imagen JPG, PNG o WEBP real (puede ser otro formato, como AVIF o GIF, con la extensión cambiada). "
                        + "Ábrela en Paint o en un editor y vuelve a guardarla como JPG o PNG.");
            }
            // UUID: nunca se repite el nombre, aunque dos personas suban el mismo archivo a la vez
            String nombre = UUID.randomUUID() + extension;
            Files.write(carpetas.get(tipo).resolve(nombre), bytes);
            log.info("Imagen guardada ({} {}): {}", tipo.carpeta(), id, nombre);
            return nombre;
        } catch (IOException e) {
            log.error("No se pudo guardar la imagen ({} {})", tipo.carpeta(), id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen");
        }
    }

    /** Elimina una imagen anterior (si existe y está dentro de la carpeta de ese tipo). */
    public void borrar(TipoImagen tipo, String nombre) {
        if (nombre == null) {
            return;
        }
        try {
            Path carpeta = carpetas.get(tipo);
            Path archivo = carpeta.resolve(nombre).normalize();
            if (archivo.startsWith(carpeta)) {
                Files.deleteIfExists(archivo);
            }
        } catch (IOException e) {
            // Si no se puede borrar, no pasa nada: solo queda un archivo sin usar
        }
    }

    /** Mira los primeros bytes del archivo (su "firma") y devuelve la extensión real (.jpg, .png, .webp) o null si no es ninguna. */
    private String extensionReal(byte[] b) {
        if (b.length < 12) {
            return null;
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return ".png";
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return ".webp";
        }
        return null;
    }

    /** Los primeros 12 bytes en hexadecimal, para el registro de errores. */
    private static String primerosBytes(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(12, b.length); i++) {
            sb.append(String.format("%02X ", b[i]));
        }
        return sb.toString().trim();
    }

    private ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}

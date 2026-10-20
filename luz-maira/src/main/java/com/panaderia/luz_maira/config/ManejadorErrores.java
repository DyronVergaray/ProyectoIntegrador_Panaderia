package com.panaderia.luz_maira.config;

import java.time.format.DateTimeParseException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

/** Convierte los errores en JSON { "error": "mensaje" } para que el front los muestre. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    private ResponseEntity<Map<String, String>> error(int codigo, String mensaje) {
        return ResponseEntity.status(codigo).body(Map.of("error", mensaje));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getDefaultMessage()).orElse("Datos inválidos");
        return error(400, mensaje);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> estado(ResponseStatusException e) {
        return error(e.getStatusCode().value(), e.getReason());
    }

    @ExceptionHandler({DateTimeParseException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, String>> parametro(Exception e) {
        return error(400, "Algún parámetro de la solicitud no es válido");
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    ResponseEntity<Map<String, String>> noEncontrado(EmptyResultDataAccessException e) {
        return error(404, "No se encontró el registro solicitado");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMessage());
        return error(409, "Los datos no son válidos o ya existen");
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Map<String, String>> baseDeDatos(DataAccessException e) {
        log.error("Error de base de datos", e);
        return error(503, "No se pudo acceder a la base de datos. Revisa que SQL Server esté encendido y los datos de conexión.");
    }
}

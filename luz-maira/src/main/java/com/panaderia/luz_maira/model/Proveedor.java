package com.panaderia.luz_maira.model;

/** imagen: ruta pública (ej. /img/proveedores/3f2a....png) o null si aún no tiene foto. direccion y descripcion son opcionales. */
public record Proveedor(int id, String nombre, String ruc, String direccion, String descripcion, String imagen) { }

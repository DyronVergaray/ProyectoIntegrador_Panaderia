package com.panaderia.luz_maira.model;

/** Tipos de foto del sistema. "carpeta" es el nombre de la subcarpeta en disco y también de la ruta pública: /img/<carpeta>/archivo.jpg */
public enum TipoImagen {
    PRODUCTOS("productos"), MATERIAS("materias"), PROVEEDORES("proveedores");

    private final String carpeta;

    TipoImagen(String carpeta) {
        this.carpeta = carpeta;
    }

    public String carpeta() {
        return carpeta;
    }

    /** Ruta pública desde la que el navegador pide las fotos de este tipo (ver WebConfig). */
    public String rutaPublica() {
        return "/img/" + carpeta + "/";
    }
}

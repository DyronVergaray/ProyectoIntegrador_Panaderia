package com.panaderia.luz_maira.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.panaderia.luz_maira.service.ImagenService;

/** Publica la carpeta de fotos en la ruta /img/productos/... */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ImagenService imagenes;

    public WebConfig(ImagenService imagenes) {
        this.imagenes = imagenes;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler("/img/productos/**")
                .addResourceLocations(imagenes.carpeta().toUri().toString())
                .setCachePeriod(86400);     // el nombre del archivo cambia cada vez que se sube otra foto
    }
}

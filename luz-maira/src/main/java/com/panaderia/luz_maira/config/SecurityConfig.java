package com.panaderia.luz_maira.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Permisos por rol. Roles de la tabla Roles: Cliente, Cajero, Almacenero, Despachador, Entregador, Administrador.
 * - Páginas HTML: cada rol solo entra a las suyas (si no, vuelve a index.html).
 * - API (/api/...): responde 401 si no hay sesión y 403 si el rol no corresponde.
 */
@Configuration
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())   // la API usa JSON y sesión del mismo origen; ver README
            .authorizeHttpRequests(auth -> auth
                // ----- Público -----
                .requestMatchers("/", "/index.html", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/login", "/api/registro", "/api/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categorias", "/api/productos/favoritos").permitAll()

                // ----- API por rol -----
                .requestMatchers(HttpMethod.GET, "/api/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/productos").hasAnyRole("Cliente", "Cajero", "Administrador")
                .requestMatchers("/api/productos/**").hasRole("Administrador")          // crear, editar, imagen
                .requestMatchers("/api/tipos-pedido").hasAnyRole("Cliente", "Cajero")
                .requestMatchers(HttpMethod.POST, "/api/pedidos").hasAnyRole("Cliente", "Cajero")
                .requestMatchers(HttpMethod.GET, "/api/pedidos/mios").hasRole("Cliente")
                .requestMatchers(HttpMethod.GET, "/api/pedidos").hasAnyRole("Cajero", "Administrador")
                .requestMatchers(HttpMethod.GET, "/api/pedidos/*").hasAnyRole("Cliente", "Cajero", "Administrador")
                .requestMatchers(HttpMethod.GET, "/api/stock", "/api/materias").hasAnyRole("Almacenero", "Administrador")
                .requestMatchers("/api/usuarios/**", "/api/roles", "/api/dashboard/**").hasRole("Administrador")
                .requestMatchers("/api/**").denyAll()

                // ----- Páginas por rol -----
                .requestMatchers("/catalogo.html").hasRole("Cliente")
                .requestMatchers("/historial.html").hasAnyRole("Cliente", "Cajero")
                .requestMatchers("/cajera.html").hasRole("Cajero")
                .requestMatchers("/almacen-*.html").hasAnyRole("Almacenero", "Administrador")
                .requestMatchers("/despachador.html").hasRole("Despachador")
                .requestMatchers("/entregador.html").hasRole("Entregador")
                .requestMatchers("/admin-*.html").hasRole("Administrador")
                .anyRequest().authenticated())
            .exceptionHandling(errores -> errores
                // Sin sesión: la API responde 401; las páginas llevan al inicio con el panel de login abierto
                .authenticationEntryPoint((req, res, ex) -> responder(req, res, 401, "Debes iniciar sesión", "/index.html?login=1"))
                // Con sesión pero sin permiso
                .accessDeniedHandler((req, res, ex) -> responder(req, res, 403, "No tienes permiso para esta acción", "/index.html?sinpermiso=1")));
        return http.build();
    }

    private void responder(HttpServletRequest req, HttpServletResponse res, int codigo, String mensaje, String redireccion) throws IOException {
        if (codigo == 403) {
            log.warn("Acceso denegado (403): {} {}", req.getMethod(), req.getRequestURI());
        }
        if (req.getRequestURI().startsWith("/api/")) {
            res.setStatus(codigo);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"error\":\"" + mensaje + "\"}");
        } else {
            res.sendRedirect(redireccion);
        }
    }

    /** Las contraseñas se guardan con BCrypt (nunca en texto plano). */
    @Bean
    PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder();
    }
}

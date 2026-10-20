package com.panaderia.luz_maira.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad.
 * POR AHORA TODAS LAS RUTAS SON PÚBLICAS: el inicio de sesión se activará más adelante.
 * Cuando llegue ese momento, solo hay que reemplazar el permitAll() por reglas por rol.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** Las contraseñas se guardan con BCrypt (nunca en texto plano). */
    @Bean
    PasswordEncoder codificadorDeClaves() {
        return new BCryptPasswordEncoder();
    }
}

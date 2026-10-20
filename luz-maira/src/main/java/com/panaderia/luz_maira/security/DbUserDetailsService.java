package com.panaderia.luz_maira.security;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Busca al usuario por correo en Usuario + Credenciales_usuario + Roles. */
@Service
public class DbUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbc;

    public DbUserDetailsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        List<UsuarioAutenticado> usuarios = jdbc.query(
                "SELECT u.id_usuario, u.primer_nombre + ' ' + u.primer_apellido, r.nombre_rol, c.email, c.[contraseña] "
                        + "FROM Usuario u JOIN Roles r ON r.id_rol = u.id_rol "
                        + "JOIN Credenciales_usuario c ON c.id_credenciales = u.id_credenciales WHERE c.email = ?",
                (rs, n) -> new UsuarioAutenticado(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)),
                email);
        if (usuarios.isEmpty()) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        return usuarios.get(0);
    }
}

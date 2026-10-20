package com.panaderia.luz_maira.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Usuario que inició sesión: guarda su id, nombre y rol (nombre de la tabla Roles). */
public class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    /** Página de inicio de cada rol. */
    private static final Map<String, String> PANEL = Map.of(
            "Cliente", "/catalogo.html", "Cajero", "/cajera.html", "Almacenero", "/almacen-productos.html",
            "Despachador", "/despachador.html", "Entregador", "/entregador.html", "Administrador", "/admin-dashboard.html");

    private final int id;
    private final String nombre;
    private final String rol;
    private final String email;
    private String clave;

    public UsuarioAutenticado(int id, String nombre, String rol, String email, String clave) {
        this.id = id;
        this.nombre = nombre;
        this.rol = rol;
        this.email = email;
        this.clave = clave;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getRol() { return rol; }
    public String getPanel() { return PANEL.getOrDefault(rol, "/index.html"); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Override public String getPassword() { return clave; }
    @Override public String getUsername() { return email; }
    @Override public void eraseCredentials() { clave = null; }
}

package com.panaderia.luz_maira.service;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.FlotaDAO;
import com.panaderia.luz_maira.dao.UsuarioDAO;
import com.panaderia.luz_maira.model.Flota;
import com.panaderia.luz_maira.model.FlotaForm;
import com.panaderia.luz_maira.model.UsuarioForm;

/**
 * Administración de la flota de reparto: entregadores (cada uno con su propia cuenta) y vehículos.
 * El estado "Ocupado" (conductor) y "En ruta" (vehículo) lo manejan las asignaciones de pedidos,
 * por eso el administrador no puede cambiarlo a mano mientras están en uso.
 */
@Service
public class FlotaService {

    private static final Logger log = LoggerFactory.getLogger(FlotaService.class);
    private static final Set<String> ESTADOS_ENTREGADOR = Set.of("Disponible", "No disponible");
    private static final Set<String> ESTADOS_VEHICULO = Set.of("Disponible", "Mantenimiento");

    private final FlotaDAO dao;
    private final UsuarioDAO usuarios;
    private final UsuarioService cuentas;

    public FlotaService(FlotaDAO dao, UsuarioDAO usuarios, UsuarioService cuentas) {
        this.dao = dao;
        this.usuarios = usuarios;
        this.cuentas = cuentas;
    }

    // ---------- Entregadores ----------

    public List<Flota.Entregador> entregadores() {
        return dao.entregadores();
    }

    /** Alta de un entregador con su correo y contraseña propios (el rol siempre es Entregador). */
    @Transactional
    public int crearEntregador(UsuarioForm f) {
        return cuentas.crear(f, usuarios.idRolPorNombre("Entregador"));
    }

    @Transactional
    public void actualizarEntregador(int id, FlotaForm.Entregador f) {
        Flota.Entregador actual = entregador(id);
        String estado = estadoNuevo(actual.disponibilidad(), f.disponibilidad(), "Ocupado", ESTADOS_ENTREGADOR,
                "El entregador está en una entrega: su estado cambia solo cuando confirme el pedido");
        dao.actualizarConductor(id, f, estado);
        Integer idUsuario = dao.idUsuarioDe(id);
        if (idUsuario != null) {
            dao.actualizarUsuario(idUsuario, f);
        }
        log.info("Entregador actualizado: id={}, estado={}", id, estado);
    }

    /** Para conductores antiguos que aún no tienen cuenta: les crea correo y contraseña propios. */
    @Transactional
    public void crearAcceso(int id, FlotaForm.Acceso a) {
        Flota.Entregador e = entregador(id);
        if (e.email() != null) {
            throw error(HttpStatus.CONFLICT, "Este entregador ya tiene acceso. Usa 'Cambiar contraseña'.");
        }
        if (a.email() == null || a.email().isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "El correo es obligatorio");
        }
        UsuarioForm datos = new UsuarioForm(e.primerNombre(), e.primerApellido(), e.segundoApellido(), e.telefono(),
                a.email(), a.password(), null);
        int idUsuario = cuentas.crearCuenta(datos, usuarios.idRolPorNombre("Entregador"));
        dao.enlazar(id, idUsuario);
        log.info("Acceso creado para el entregador {}", id);
    }

    @Transactional
    public void cambiarClave(int id, String clave) {
        entregador(id);
        Integer idUsuario = dao.idUsuarioDe(id);
        if (idUsuario == null) {
            throw error(HttpStatus.BAD_REQUEST, "Este entregador aún no tiene acceso. Créale uno primero.");
        }
        cuentas.cambiarClave(idUsuario, clave);
    }

    // ---------- Vehículos ----------

    public List<Flota.VehiculoFicha> vehiculos() {
        return dao.vehiculos();
    }

    @Transactional
    public int crearVehiculo(FlotaForm.Vehiculo f) {
        validarPlaca(f.placa(), 0);
        int id = dao.crearVehiculo(f.placa(), f.marca(), f.modelo(), f.capacidad());
        log.info("Vehículo creado: id={}, placa={}", id, f.placa());
        return id;
    }

    @Transactional
    public void actualizarVehiculo(int id, FlotaForm.Vehiculo f) {
        Flota.VehiculoFicha actual = dao.vehiculo(id);
        if (actual == null) {
            throw error(HttpStatus.NOT_FOUND, "El vehículo no existe");
        }
        validarPlaca(f.placa(), id);
        String estado = estadoNuevo(actual.estado(), f.estado(), "En ruta", ESTADOS_VEHICULO,
                "El vehículo está en ruta: su estado cambia solo cuando se confirme la entrega");
        dao.actualizarVehiculo(id, f, estado);
        log.info("Vehículo actualizado: id={}, estado={}", id, estado);
    }

    // ---------- Apoyo ----------

    private Flota.Entregador entregador(int id) {
        Flota.Entregador e = dao.entregador(id);
        if (e == null) {
            throw error(HttpStatus.NOT_FOUND, "El entregador no existe");
        }
        return e;
    }

    private void validarPlaca(String placa, int idExcluido) {
        if (dao.existePlaca(placa, idExcluido)) {
            throw error(HttpStatus.CONFLICT, "Ya existe un vehículo con esa placa");
        }
    }

    /** Decide el estado que se guarda: respeta el "en uso" y solo admite los estados permitidos. */
    private String estadoNuevo(String actual, String pedido, String enUso, Set<String> permitidos, String mensajeEnUso) {
        if (pedido == null || pedido.isBlank()) {
            return actual == null ? "Disponible" : actual;
        }
        if (enUso.equals(actual)) {
            if (!enUso.equals(pedido)) {
                throw error(HttpStatus.CONFLICT, mensajeEnUso);
            }
            return actual;
        }
        if (!permitidos.contains(pedido)) {
            throw error(HttpStatus.BAD_REQUEST, "Estado no válido: " + pedido);
        }
        return pedido;
    }

    private ResponseStatusException error(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }
}

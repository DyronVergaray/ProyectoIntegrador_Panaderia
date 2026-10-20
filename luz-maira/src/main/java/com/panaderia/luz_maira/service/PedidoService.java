package com.panaderia.luz_maira.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.PedidoDAO;
import com.panaderia.luz_maira.model.PedidoForm;
import com.panaderia.luz_maira.model.Pedidos;
import com.panaderia.luz_maira.model.TipoPedido;
import com.panaderia.luz_maira.security.UsuarioAutenticado;

/** Consolida un pedido: guarda carrito, productos, pedido y genera el código aleatorio de 3 dígitos. */
@Service
public class PedidoService {

    private static final int INTENTOS_CODIGO = 100;
    private final PedidoDAO dao;
    private final SecureRandom azar = new SecureRandom();

    public PedidoService(PedidoDAO dao) {
        this.dao = dao;
    }

    @Transactional
    public Pedidos.Confirmacion crear(UsuarioAutenticado usuario, PedidoForm f) {
        boolean enCaja = usuario.getRol().equals("Cajero");

        // La cajera siempre registra ventas "Presencial"; el cliente elige entre Virtual, Delivery y Personalizado
        TipoPedido tipo = enCaja ? dao.tipoPorNombre("Presencial") : dao.tipoPorId(f.idTipo());
        if (tipo == null || (!enCaja && tipo.nombre().equals("Presencial"))) {
            throw error("Tipo de pedido no válido. Si falta 'Presencial', ejecuta database/03_roles_codigos_imagenes.sql");
        }

        // Fecha y hora de entrega según el tipo
        LocalDate fecha = LocalDate.now();
        LocalTime hora = LocalTime.now().withSecond(0).withNano(0);
        String direccion = null;
        if (tipo.nombre().equals("Delivery")) {
            direccion = textoObligatorio(f.direccion(), "Ingresa la dirección para el delivery");
        } else if (tipo.nombre().equals("Personalizado")) {
            try {
                fecha = LocalDate.parse(textoObligatorio(f.fecha(), "Elige la fecha del pedido personalizado"));
                hora = LocalTime.parse(textoObligatorio(f.hora(), "Elige la hora del pedido personalizado"));
            } catch (DateTimeParseException e) {
                throw error("La fecha u hora del pedido no es válida");
            }
            if (fecha.isBefore(LocalDate.now())) {
                throw error("La fecha del pedido no puede estar en el pasado");
            }
        }

        // Une productos repetidos y toma los precios de la base de datos
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        f.items().forEach(i -> cantidades.merge(i.idProducto(), i.cantidad(), Integer::sum));
        Map<Integer, BigDecimal> precios = dao.preciosDe(cantidades.keySet());
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> e : cantidades.entrySet()) {
            BigDecimal precio = precios.get(e.getKey());
            if (precio == null) {
                throw error("Uno de los productos ya no existe");
            }
            total = total.add(precio.multiply(BigDecimal.valueOf(e.getValue())));
        }

        int idCarrito = dao.insertarCarrito(usuario.getId(), total);
        cantidades.forEach((idProducto, cantidad) -> dao.insertarItem(idCarrito, idProducto, cantidad, precios.get(idProducto)));
        int idPedido = dao.insertarPedido(idCarrito, tipo.id(), Date.valueOf(fecha), Time.valueOf(hora),
                enCaja ? "Entregado" : "Pendiente", direccion);

        String codigo = generarCodigo();
        dao.insertarCodigo(idPedido, codigo);
        return new Pedidos.Confirmacion(idPedido, codigo, total);
    }

    /** Código aleatorio de 3 dígitos (100 a 999) que no esté en uso por otro pedido pendiente. */
    private String generarCodigo() {
        for (int i = 0; i < INTENTOS_CODIGO; i++) {
            String codigo = String.valueOf(100 + azar.nextInt(900));
            if (!dao.codigoEnUso(codigo)) {
                return codigo;
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No hay códigos disponibles en este momento. Intenta de nuevo.");
    }

    private String textoObligatorio(String texto, String mensaje) {
        if (texto == null || texto.isBlank()) {
            throw error(mensaje);
        }
        return texto.trim();
    }

    private ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}

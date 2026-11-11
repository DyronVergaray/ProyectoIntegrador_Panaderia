package com.panaderia.luz_maira.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.panaderia.luz_maira.dao.PedidoDAO;
import com.panaderia.luz_maira.dao.PromocionDAO;
import com.panaderia.luz_maira.model.PedidoForm;
import com.panaderia.luz_maira.model.Pedidos;
import com.panaderia.luz_maira.model.Promocion;
import com.panaderia.luz_maira.model.TipoPedido;
import com.panaderia.luz_maira.security.UsuarioAutenticado;

/** Consolida un pedido: guarda carrito, productos, pedido y genera el código aleatorio de 3 dígitos. */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);
    private static final int INTENTOS_CODIGO = 100;
    private final PedidoDAO dao;
    private final PromocionDAO promociones;
    private final SecureRandom azar = new SecureRandom();

    public PedidoService(PedidoDAO dao, PromocionDAO promociones) {
        this.dao = dao;
        this.promociones = promociones;
    }

    /** Premio ya calculado de una promoción: qué producto se regala y cuántas unidades. */
    private record Premio(Promocion promocion, int veces, int idProducto, PedidoDAO.ProductoPedido producto, int cantidad) { }

    @Transactional
    public Pedidos.Confirmacion crear(UsuarioAutenticado usuario, PedidoForm f) {
        boolean enCaja = usuario.getRol().equals("Cajero");

        // La cajera siempre registra ventas "Presencial"; el cliente elige entre Virtual, Delivery y Personalizado
        TipoPedido tipo = enCaja ? dao.tipoPorNombre("Presencial") : dao.tipoPorId(f.idTipo());
        if (tipo == null || (!enCaja && (tipo.nombre().equals("Presencial") || tipo.nombre().equals("Personalizado")))) {
            throw error("Tipo de pedido no válido. Si falta 'Presencial', ejecuta database/03_roles_codigos_imagenes.sql");
        }

        // Fecha y hora de entrega según el tipo
        LocalDate fecha = LocalDate.now();
        LocalTime hora = LocalTime.now().withSecond(0).withNano(0);
        String direccion = null;
        if (tipo.nombre().equals("Delivery")) {
            direccion = textoObligatorio(f.direccion(), "Ingresa la dirección para el delivery");
        }

        // Une productos repetidos y toma precios y stock de la base de datos
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        f.items().forEach(i -> cantidades.merge(i.idProducto(), i.cantidad(), Integer::sum));
        Map<Integer, PedidoDAO.ProductoPedido> productos = dao.productosParaPedido(cantidades.keySet());
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> e : cantidades.entrySet()) {
            PedidoDAO.ProductoPedido producto = productos.get(e.getKey());
            if (producto == null) {
                throw error("Uno de los productos ya no existe");
            }
            // Los panes tienen stock null: no se verifica. El resto no puede superar lo disponible.
            if (producto.stock() != null && e.getValue() > producto.stock()) {
                log.warn("Stock insuficiente de '{}': pedidos={}, disponible={}", producto.nombre(), e.getValue(), producto.stock());
                throw error(producto.stock() == 0 ? producto.nombre() + " está agotado"
                        : "No hay stock suficiente de " + producto.nombre() + " (quedan " + producto.stock() + ")");
            }
            total = total.add(producto.precio().multiply(BigDecimal.valueOf(e.getValue())));
        }

        // Promociones: solo si el cliente o la cajera marcaron la casilla. El premio se calcula aquí, nunca en el navegador.
        List<Premio> premios = Boolean.TRUE.equals(f.aplicarPromociones()) ? calcularPremios(total, cantidades) : List.of();

        int idCarrito = dao.insertarCarrito(usuario.getId(), total);
        for (Map.Entry<Integer, Integer> e : cantidades.entrySet()) {
            PedidoDAO.ProductoPedido producto = productos.get(e.getKey());
            dao.insertarItem(idCarrito, e.getKey(), e.getValue(), producto.precio());
            // Descuento atómico: si otra compra se llevó el stock mientras tanto, se cancela todo (rollback)
            if (producto.stock() != null && !dao.descontarStock(e.getKey(), e.getValue())) {
                log.warn("El stock de '{}' cambió durante la compra", producto.nombre());
                throw error("El stock de " + producto.nombre() + " acaba de cambiar. Intenta de nuevo.");
            }
        }
        // Los regalos se guardan como productos a precio S/ 0.00 (y descuentan stock como cualquier otro)
        for (Premio premio : premios) {
            dao.insertarItem(idCarrito, premio.idProducto(), premio.cantidad(), BigDecimal.ZERO);
            if (premio.producto().stock() != null && !dao.descontarStock(premio.idProducto(), premio.cantidad())) {
                throw error("El stock de " + premio.producto().nombre() + " acaba de cambiar. Intenta de nuevo.");
            }
        }
        int idPedido = dao.insertarPedido(idCarrito, tipo.id(), Date.valueOf(fecha), Time.valueOf(hora),
                enCaja ? "Entregado" : "Pendiente", direccion);

        List<Promocion.Aplicada> aplicadas = new ArrayList<>();
        for (Premio premio : premios) {
            Promocion.Aplicada aplicada = new Promocion.Aplicada(premio.promocion().nombre(), premio.producto().nombre(),
                    premio.veces(), premio.cantidad());
            promociones.registrarAplicada(idPedido, aplicada);
            aplicadas.add(aplicada);
        }

        String codigo = generarCodigo();
        dao.insertarCodigo(idPedido, codigo);
        log.info("Pedido {} registrado por {} ({}): tipo={}, total={}, código={}",
                idPedido, usuario.getUsername(), usuario.getRol(), tipo.nombre(), total, codigo);
        return new Pedidos.Confirmacion(idPedido, codigo, total, aplicadas);
    }

    /**
     * Revisa cada promoción vigente contra la compra (total en S/ o unidades de un producto). Por cada bloque completo
     * (hasta maxVeces) se regalan cantidadRegalo unidades. Si el producto de regalo tiene stock limitado, se
     * regala lo que quede después de lo que el cliente ya compró; si no queda nada, esa promoción no se aplica.
     */
    private List<Premio> calcularPremios(BigDecimal total, Map<Integer, Integer> cantidades) {
        List<Promocion> vigentes = promociones.vigentes();
        if (vigentes.isEmpty()) {
            return List.of();
        }
        List<Premio> premios = new ArrayList<>();
        Map<Integer, Integer> regalado = new LinkedHashMap<>();     // unidades ya regaladas por producto en este pedido
        Map<Integer, PedidoDAO.ProductoPedido> regalos = dao.productosParaPedido(
                vigentes.stream().map(Promocion::idProducto).toList());
        for (Promocion promo : vigentes) {
            // Bloques completos que cumple la compra: por monto (total / monto) o por unidades compradas del producto
            int bloques = promo.tipo().equals(Promocion.MONTO)
                    ? total.divideToIntegralValue(promo.montoMinimo()).setScale(0, RoundingMode.DOWN).intValue()
                    : cantidades.getOrDefault(promo.idProductoCondicion(), 0) / promo.cantidadCondicion();
            int veces = Math.min(bloques, promo.maxVeces());
            PedidoDAO.ProductoPedido producto = regalos.get(promo.idProducto());
            if (veces < 1 || producto == null) {
                continue;
            }
            int cantidad = veces * promo.cantidadRegalo();
            if (producto.stock() != null) {
                int ocupado = cantidades.getOrDefault(promo.idProducto(), 0) + regalado.getOrDefault(promo.idProducto(), 0);
                cantidad = Math.min(cantidad, producto.stock() - ocupado);
            }
            if (cantidad < 1) {
                log.info("Promoción '{}' sin aplicar: no queda stock de {}", promo.nombre(), producto.nombre());
                continue;
            }
            regalado.merge(promo.idProducto(), cantidad, Integer::sum);
            premios.add(new Premio(promo, veces, promo.idProducto(), producto, cantidad));
        }
        return premios;
    }

    /**
     * Crea el pedido SQL de una solicitud personalizada aceptada por el administrador.
     * El carrito lleva un solo producto base ("Pedido personalizado") con el precio acordado. Queda "Pendiente"
     * y sigue el flujo normal: código de 3 dígitos, entrega e historial del cliente.
     */
    @Transactional
    public Pedidos.Confirmacion crearPersonalizado(int idCliente, BigDecimal precio, LocalDate fecha, LocalTime hora, String direccion) {
        TipoPedido tipo = dao.tipoPorNombre("Personalizado");
        Integer idProducto = dao.idProductoPersonalizado();
        if (tipo == null || idProducto == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falta el producto base de pedidos personalizados. Ejecuta database/12_producto_personalizado.sql");
        }
        int idCarrito = dao.insertarCarrito(idCliente, precio);
        dao.insertarItem(idCarrito, idProducto, 1, precio);
        int idPedido = dao.insertarPedido(idCarrito, tipo.id(), Date.valueOf(fecha), Time.valueOf(hora), "Pendiente", direccion);
        String codigo = generarCodigo();
        dao.insertarCodigo(idPedido, codigo);
        log.info("Pedido personalizado {} creado para el cliente {}: total={}, código={}", idPedido, idCliente, precio, codigo);
        return new Pedidos.Confirmacion(idPedido, codigo, precio, List.of());
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

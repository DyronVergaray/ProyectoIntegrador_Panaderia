package com.panaderia.luz_maira.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Solicitud de pedido personalizado (documento de MongoDB, colección "solicitudes_personalizadas").
 * Es un documento y no una fila de SQL porque cada solicitud trae características distintas
 * (torta, bocaditos, cupcakes...) y el formulario puede crecer sin tocar el esquema.
 * estado: Solicitada -> Aceptada (con precio y idPedido) | Rechazada | Cancelada (por el cliente).
 */
@Document("solicitudes_personalizadas")
public record Personalizado(@Id String id, int idUsuario, String cliente, String email,
                            String tipoProducto, String descripcion, String sabor, int porciones,
                            String decoracion, String dedicatoria, String alergias,
                            String modalidad, String direccion, String fechaEntrega, String horaEntrega,
                            String estado, Double precio, String nota, Integer idPedido, String fechaCreacion) {

    public static final String SOLICITADA = "Solicitada";
    public static final String ACEPTADA = "Aceptada";
    public static final String RECHAZADA = "Rechazada";
    public static final String CANCELADA = "Cancelada";
}

package com.panaderia.luz_maira.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Calificación de un pedido entregado (documento de MongoDB, colección "calificaciones").
 * Cuatro aspectos de 1 a 5 estrellas, si nos recomendaría y un comentario libre. Una por pedido (índice único en idPedido).
 */
@Document("calificaciones")
public record Calificacion(@Id String id, int idPedido, int idUsuario, int compra, int entrega, int atencion,
                           int calidad, boolean recomienda, String comentario, String fecha) { }

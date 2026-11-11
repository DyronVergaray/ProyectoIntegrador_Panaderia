/* =====================================================================
   Panadería Luz Mayra - MongoDB (base "luzmayra")
   Opcional: la aplicación crea las colecciones sola al guardar el primer documento.
   Este script solo deja listos los ÍNDICES. Ejecutar con:  mongosh luzmayra database/mongodb_inicial.js

   Colecciones:
     solicitudes_personalizadas : formulario del pedido personalizado (estado: Solicitada / Aceptada / Rechazada / Cancelada)
     calificaciones             : calificación del cliente a un pedido Entregado (una sola por pedido)
   ===================================================================== */
db = db.getSiblingDB("luzmayra");

db.solicitudes_personalizadas.createIndex({ idUsuario: 1, fechaCreacion: -1 });
db.solicitudes_personalizadas.createIndex({ estado: 1 });
db.solicitudes_personalizadas.createIndex({ idPedido: 1 }, { sparse: true });

// unique: un pedido solo puede calificarse una vez
db.calificaciones.createIndex({ idPedido: 1 }, { unique: true });
db.calificaciones.createIndex({ idUsuario: 1 });

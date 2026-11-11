/* =====================================================================
   Panadería Luz Mayra - Producto base "Pedido personalizado"
   Ejecutar DESPUÉS de 11 (y después de 09). Se puede ejecutar varias veces.

   Los pedidos personalizados se piden con un formulario (las características se guardan en MongoDB).
   Cuando el administrador acepta una solicitud y le pone precio, se crea un pedido normal en SQL Server:
   su carrito lleva UN producto, "Pedido personalizado", con el precio acordado. Así el pedido sigue el flujo
   de siempre (código de 3 dígitos, entrega, historial, dashboard).
   El producto vive en la categoría "Producto eliminado" para que NO salga en el catálogo del cliente ni de la cajera.
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

IF NOT EXISTS (SELECT 1 FROM Categoria_Producto WHERE nombre_categoria = 'Producto eliminado')
    INSERT INTO Categoria_Producto (nombre_categoria) VALUES ('Producto eliminado');
GO

IF NOT EXISTS (SELECT 1 FROM Producto WHERE nombre_producto = 'Pedido personalizado')
    INSERT INTO Producto (id_categoriaProducto, nombre_producto, descripcion, precio_unitario)
    SELECT id_categoriaProducto, 'Pedido personalizado', 'Producto base de los pedidos hechos a medida (no se vende en el catálogo)', 0
    FROM Categoria_Producto WHERE nombre_categoria = 'Producto eliminado';
GO

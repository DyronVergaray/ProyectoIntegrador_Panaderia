/* =====================================================================
   Panadería Luz Mayra - Categoría "Producto eliminado" (baja lógica de productos)
   Ejecutar DESPUÉS de 08. Se puede ejecutar varias veces. No cambia la estructura de ninguna tabla.
   El botón "Eliminar producto" del administrador solo pasa el producto a esta categoría;
   así se conserva el historial de pedidos y se puede restaurar. Solo la ven el administrador y el almacenero.
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

IF NOT EXISTS (SELECT 1 FROM Categoria_Producto WHERE nombre_categoria = 'Producto eliminado')
    INSERT INTO Categoria_Producto (nombre_categoria) VALUES ('Producto eliminado');
GO

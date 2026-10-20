/* =====================================================================
   Panadería Luz Mayra - Ajustes para roles, códigos de pedido e imágenes
   Ejecutar DESPUÉS de 01_schema.sql y 02_datos_demo.sql. Se puede ejecutar varias veces.
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

/* ---------- 1. Imagen de cada producto ----------
   Guarda solo el nombre del archivo (ej. 3-1699999999.jpg); la foto vive en la carpeta imagenes-productos/ */
IF COL_LENGTH('Producto', 'imagen') IS NULL
    ALTER TABLE Producto ADD imagen VARCHAR(255) NULL;
GO

/* ---------- 2. Tipo de pedido para las ventas hechas en caja ---------- */
IF NOT EXISTS (SELECT 1 FROM Tipo_Pedido WHERE nombre_tipo = 'Presencial')
    INSERT INTO Tipo_Pedido (nombre_tipo) VALUES ('Presencial');
GO

/* ---------- 3. Códigos de pedido de 3 dígitos ----------
   Los pedidos nuevos reciben su código aleatorio desde la aplicación.
   Esto convierte los códigos de letras que pudieran existir de la demo anterior. */
UPDATE Codigo
SET codigo = CAST(100 + ABS(CHECKSUM(NEWID())) % 900 AS VARCHAR(3))
WHERE codigo IS NULL OR codigo NOT LIKE '[0-9][0-9][0-9]';
GO

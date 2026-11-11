/* =====================================================================
   Panadería Luz Mayra - Tipos de promoción (por monto o por cantidad de un producto)
   Ejecutar DESPUÉS de 10. Se puede ejecutar varias veces.

   tipo = 'MONTO'    : "por cada S/ monto_minimo de compra, llévate cantidad_regalo de id_producto"
   tipo = 'CANTIDAD' : "por cada cantidad_condicion unidades que compres de id_producto_condicion,
                        llévate cantidad_regalo de id_producto"
   Las promociones que ya existían quedan como 'MONTO'.
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

IF COL_LENGTH('Promocion', 'tipo') IS NULL
    ALTER TABLE Promocion ADD tipo VARCHAR(10) NOT NULL CONSTRAINT DF_Promocion_tipo DEFAULT 'MONTO';
GO
IF COL_LENGTH('Promocion', 'id_producto_condicion') IS NULL
    ALTER TABLE Promocion ADD id_producto_condicion INT NULL;
GO
IF COL_LENGTH('Promocion', 'cantidad_condicion') IS NULL
    ALTER TABLE Promocion ADD cantidad_condicion INT NULL;
GO

-- monto_minimo solo se usa en las promociones por monto
ALTER TABLE Promocion ALTER COLUMN monto_minimo DECIMAL(10,2) NULL;
GO

IF OBJECT_ID('CK_Promocion_valores') IS NOT NULL
    ALTER TABLE Promocion DROP CONSTRAINT CK_Promocion_valores;
GO
IF OBJECT_ID('CK_Promocion_tipo') IS NOT NULL
    ALTER TABLE Promocion DROP CONSTRAINT CK_Promocion_tipo;
GO
ALTER TABLE Promocion ADD CONSTRAINT CK_Promocion_tipo CHECK (
    cantidad_regalo > 0 AND max_veces > 0 AND (
        (tipo = 'MONTO'    AND monto_minimo > 0) OR
        (tipo = 'CANTIDAD' AND id_producto_condicion IS NOT NULL AND cantidad_condicion > 0)));
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Promocion_ProductoCondicion')
    ALTER TABLE Promocion ADD CONSTRAINT FK_Promocion_ProductoCondicion
        FOREIGN KEY (id_producto_condicion) REFERENCES Producto(id_producto);
GO

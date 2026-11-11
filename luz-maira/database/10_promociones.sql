/* =====================================================================
   Panadería Luz Mayra - Promociones administrables
   Ejecutar DESPUÉS de 09. Se puede ejecutar varias veces.

   Promocion          : las crea/edita/elimina el administrador. Regla:
                        "por cada S/ monto_minimo de compra, el cliente se lleva cantidad_regalo
                         unidades de id_producto (gratis), hasta max_veces veces por pedido".
   Pedido_Promocion   : qué promociones se aplicaron a cada pedido. Guarda copia de los textos
                        (nombre, producto) para que el historial no cambie si luego se edita o elimina la promoción.
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

IF OBJECT_ID('Promocion') IS NULL
CREATE TABLE Promocion (
    id_promocion    INT IDENTITY(1,1) NOT NULL,
    nombre          VARCHAR(80)   NOT NULL,
    descripcion     VARCHAR(200)  NULL,          -- texto que se muestra en el slider
    monto_minimo    DECIMAL(10,2) NOT NULL,      -- cada bloque de este monto da un premio
    id_producto     INT           NOT NULL,      -- producto que se regala
    cantidad_regalo INT           NOT NULL,      -- unidades por bloque
    max_veces       INT           NOT NULL CONSTRAINT DF_Promocion_max DEFAULT 1,  -- máximo de bloques por pedido
    fecha_inicio    DATE          NULL,
    fecha_fin       DATE          NULL,
    activa          BIT           NOT NULL CONSTRAINT DF_Promocion_activa DEFAULT 1,
    CONSTRAINT PK_Promocion PRIMARY KEY (id_promocion),
    CONSTRAINT FK_Promocion_Producto FOREIGN KEY (id_producto) REFERENCES Producto(id_producto),
    CONSTRAINT CK_Promocion_valores CHECK (monto_minimo > 0 AND cantidad_regalo > 0 AND max_veces > 0)
);
GO

IF OBJECT_ID('Pedido_Promocion') IS NULL
CREATE TABLE Pedido_Promocion (
    id_pedido_promocion INT IDENTITY(1,1) NOT NULL,
    id_pedido           INT           NOT NULL,
    nombre_promocion    VARCHAR(80)   NOT NULL,
    producto            VARCHAR(150)  NOT NULL,
    veces               INT           NOT NULL,  -- cuántos bloques se cumplieron
    cantidad            INT           NOT NULL,  -- unidades regaladas en total
    CONSTRAINT PK_Pedido_Promocion PRIMARY KEY (id_pedido_promocion),
    CONSTRAINT FK_PedPromo_Pedido FOREIGN KEY (id_pedido) REFERENCES Pedido_Gestion(id_pedido)
);
GO

-- Promoción de ejemplo (la que antes estaba fija en la portada): por cada S/ 100, 6 cupcakes
IF NOT EXISTS (SELECT 1 FROM Promocion) AND EXISTS (SELECT 1 FROM Producto WHERE nombre_producto LIKE '%upcake%')
    INSERT INTO Promocion (nombre, descripcion, monto_minimo, id_producto, cantidad_regalo, max_veces)
    SELECT TOP 1 'Cupcakes gratis', 'Por cada S/ 100 llévate 6 cupcakes', 100, id_producto, 6, 5
    FROM Producto WHERE nombre_producto LIKE '%upcake%' ORDER BY id_producto;
GO

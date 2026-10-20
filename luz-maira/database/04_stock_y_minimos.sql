/* =====================================================================
   Panadería Luz Mayra - Stock de productos y stock mínimo de materias primas
   Ejecutar DESPUÉS de 03_roles_codigos_imagenes.sql. Se puede ejecutar varias veces.
   Regla: los PANES no controlan stock (se hornean a diario): su stock queda en NULL.
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

/* ---------- 1. Columnas nuevas ---------- */
IF COL_LENGTH('Producto', 'stock') IS NULL        ALTER TABLE Producto ADD stock INT NULL;
IF COL_LENGTH('Producto', 'stock_minimo') IS NULL ALTER TABLE Producto ADD stock_minimo INT NULL;
IF COL_LENGTH('Materia_P', 'stock_minimo') IS NULL ALTER TABLE Materia_P ADD stock_minimo DECIMAL(12,2) NULL;
GO

/* ---------- 2. Pan Chapla es un pan: pasa a la categoría Panes ---------- */
UPDATE p
SET id_categoriaProducto = (SELECT id_categoriaProducto FROM Categoria_Producto WHERE nombre_categoria = 'Panes')
FROM Producto p
WHERE p.nombre_producto = 'Pan Chapla';

/* ---------- 3. Los panes no controlan stock ---------- */
UPDATE p
SET stock = NULL, stock_minimo = NULL
FROM Producto p
JOIN Categoria_Producto c ON c.id_categoriaProducto = p.id_categoriaProducto
WHERE c.nombre_categoria = 'Panes';

/* ---------- 4. Stock inicial del resto de productos (solo si aún no tienen) ----------
   Para la demostración: Flan de Vainilla queda AGOTADO (no aparece en el catálogo)
   y Gelatina de Fresa queda con STOCK BAJO (por debajo del mínimo). */
UPDATE p
SET stock = CASE p.nombre_producto WHEN 'Flan de Vainilla' THEN 0 WHEN 'Gelatina de Fresa' THEN 6 ELSE 40 END,
    stock_minimo = 10
FROM Producto p
JOIN Categoria_Producto c ON c.id_categoriaProducto = p.id_categoriaProducto
WHERE c.nombre_categoria <> 'Panes' AND p.stock IS NULL;
GO

/* ---------- 5. Stock mínimo de las materias primas (la levadura queda por debajo para mostrar la alerta) ---------- */
UPDATE Materia_P
SET stock_minimo = CASE nombre_materia
        WHEN 'Harina' THEN 30 WHEN 'Huevos' THEN 100 WHEN 'Mantequilla' THEN 8
        WHEN 'Azúcar' THEN 10 WHEN 'Levadura' THEN 500 WHEN 'Leche' THEN 10 ELSE 10 END
WHERE stock_minimo IS NULL;
GO

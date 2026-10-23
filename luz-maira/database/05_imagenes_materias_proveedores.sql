/* =====================================================================
   Panadería Luz Mayra - Imagen de productos base (materias primas) y de proveedores
   Ejecutar DESPUÉS de 04_stock_y_minimos.sql. Se puede ejecutar varias veces.
   Solo agrega columnas opcionales (NULL) y los 6 proveedores que antes estaban escritos a mano en la página.
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

/* ---------- 1. Columnas nuevas (guardan solo el nombre del archivo, igual que Producto.imagen) ---------- */
IF COL_LENGTH('Materia_P', 'imagen') IS NULL
    ALTER TABLE Materia_P ADD imagen VARCHAR(255) NULL;
IF COL_LENGTH('Proveedor_identidad', 'imagen') IS NULL
    ALTER TABLE Proveedor_identidad ADD imagen VARCHAR(255) NULL;
-- La página de proveedores ya mostraba una descripción corta de cada uno; ahora se guarda en la BD
IF COL_LENGTH('Proveedor_identidad', 'descripcion') IS NULL
    ALTER TABLE Proveedor_identidad ADD descripcion VARCHAR(150) NULL;
GO

/* ---------- 2. Proveedores de demostración ----------
   OJO: los RUC son de EJEMPLO (la columna es obligatoria) y la dirección queda vacía.
   Edítalos desde la página de Proveedores si quieres los reales. */
INSERT INTO Proveedor_identidad (nombre_proveedor, ruc_proveedor, direccion_proveedor, descripcion)
SELECT v.nombre, v.ruc, v.direccion, v.descripcion
FROM (VALUES
    ('Alicorp',             '20100000001', NULL, 'Proveedor industrial masivo de harinas'),
    ('Puratos Perú',        '20100000002', NULL, 'Especialista en levaduras, masa madre, etc'),
    ('Casa Grande',         '20100000003', NULL, 'Proveedor Azucarero'),
    ('La Calera',           '20100000004', NULL, 'Distribuidor Avícola'),
    ('Gloria',              '20100000005', NULL, 'Distribuidor insumos lácteos'),
    ('Machu Picchu Foods',  '20100000006', NULL, 'Proveedor Chocolatero')
) v(nombre, ruc, direccion, descripcion)
WHERE NOT EXISTS (SELECT 1 FROM Proveedor_identidad p WHERE p.nombre_proveedor = v.nombre);
GO

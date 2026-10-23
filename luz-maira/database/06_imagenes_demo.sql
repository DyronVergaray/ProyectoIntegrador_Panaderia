/* =====================================================================
   Panadería Luz Mayra - Fotos de demostración (productos, productos base y proveedores)
   Ejecutar DESPUÉS de 05_imagenes_materias_proveedores.sql. Se puede ejecutar varias veces.
   Asigna a cada registro el archivo de imagen que ya está en el repositorio:
       src/main/resources/static/img/productos/     img/materias/     img/proveedores/
   La relación se hace por NOMBRE; si un registro tiene otro nombre en tu base, se avisa al final.
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

/* ---------- 1. Productos ---------- */
UPDATE p SET p.imagen = v.imagen
FROM Producto p
JOIN (VALUES
    (N'Alfajor de maicena', 'be3736f9-bc0d-435d-9af2-baf895f15f0b.jpg'),
    (N'Baguette de Masa Madre', '2628a5f3-624b-4435-aa56-c8fe3793d16d.jpg'),
    (N'Chicha morada 1 L', '7ca4f376-7bb2-4be2-aa83-5e8a9316389e.png'),
    (N'Concha Rústica', '2d5705ae-ea28-40a5-9cb3-a9df6d4884be.jpg'),
    (N'Croissant de Almendras', '5f48d2d3-ed3f-49b3-adcd-c10fc2f5ca1c.jpg'),
    (N'Cupcake', '3ebf9656-f412-4313-bf58-882386eec133.png'),
    (N'Empanada de pollo', 'dead048f-5c3c-4200-9f5a-5d8e5a0fb4b1.jpg'),
    (N'Flan de Vainilla', '4f6e2a31-a7ce-4394-a5dc-35ed63395553.webp'),
    (N'Gelatina de Fresa', 'dba537f5-a7ac-48d7-b70d-8a62c86bec04.jpg'),
    (N'Inca Kola 500 ml', '02b3ddbd-fc07-407c-a5df-2366b9198423.png'),
    (N'Pan Chapla', '0c347fcd-df3e-44bb-a911-efeb779a78c8.webp'),
    (N'Pan ciabatta', 'be9ae126-3f8a-480d-8f7e-6cde66afacf6.png'),
    (N'Pan de ajo', 'd5048c0c-5e2f-456e-84c4-e7b81b502b57.png'),
    (N'Pan de Mantequilla', '6e48055a-0972-4b08-9608-fd3f3aab508a.jpg'),
    (N'Pan francés', 'b077cd2d-f4da-42be-8ad4-f1cf7234941b.jpg'),
    (N'Pan integral', '13743674-0ccd-407b-bddd-6ed4618bcaf7.png'),
    (N'Panetón Clásico', '51506a6b-faed-4835-bf8a-ee8a65aecca9.png'),
    (N'Rebanada de torta de Chocolate', '691e45dc-9ac3-48e0-90c9-e780b740dcd3.jpg'),
    (N'Rebanada de torta de Tres Leches', 'a25bba2a-978d-4752-b093-0b58f66132df.jpg'),
    (N'Tequeños x6', '470f7e94-94e0-4182-98e5-c558528c74a4.jpg'),
    (N'Torta de chocolate', '8e92ae9d-b2c9-4f24-9469-2542ae0ef03d.jpg'),
    (N'Torta tres leches', '008d2a0c-f327-440c-9f0d-d171134e70d2.jpg')
) v(nombre, imagen) ON p.nombre_producto = v.nombre;
PRINT 'Productos actualizados: ' + CAST(@@ROWCOUNT AS VARCHAR(10)) + ' de 22';
GO

/* ---------- 2. Productos base (materias primas) ---------- */
UPDATE m SET m.imagen = v.imagen
FROM Materia_P m
JOIN (VALUES
    (N'Azúcar', 'b5ef01e6-a2c8-47ce-8270-9099db4bf7af.jpg'),
    (N'Harina', '3c1e7bbc-e8c6-463e-8751-3fd6c2fa26e0.jpg'),
    (N'Huevos', 'e1048311-727a-477e-ba59-c68f4925b188.jpg'),
    (N'Leche', '04705cb3-a519-4b65-b5de-5170976ba503.webp'),
    (N'Levadura', '4b70486f-b529-44ef-ad71-9f1f4535907c.jpg'),
    (N'Mantequilla', '51d01f9c-7ccc-471f-b570-6a9e26e770ad.webp')
) v(nombre, imagen) ON m.nombre_materia = v.nombre;
PRINT 'Productos base actualizados: ' + CAST(@@ROWCOUNT AS VARCHAR(10)) + ' de 6';
GO

/* ---------- 3. Proveedores ---------- */
UPDATE pr SET pr.imagen = v.imagen
FROM Proveedor_identidad pr
JOIN (VALUES
    (N'Alicorp', '779f0d3d-7195-432d-b1b4-4ab7bf2cd66e.png'),
    (N'Casa Grande', 'faa7d16b-5fe7-445d-b1d4-013c391ce121.jpg'),
    (N'Gloria', '6a477d6c-f40f-4250-99db-fd9969b8aa6e.png'),
    (N'La Calera', '8ca8ffdf-e24e-4fcd-bee0-bf871ed20970.png'),
    (N'Machu Picchu Foods', 'ad4a9dd6-b163-4058-9e8e-3dd731ed0b97.jpg'),
    (N'Puratos Perú', '7f749cd5-ebd8-4bc1-b98f-acdd2da5ef65.jpg')
) v(nombre, imagen) ON pr.nombre_proveedor = v.nombre;
PRINT 'Proveedores actualizados: ' + CAST(@@ROWCOUNT AS VARCHAR(10)) + ' de 6';
GO

/* ---------- 4. Revisión: registros que siguen sin foto (si salen filas, su nombre no coincide o es un registro nuevo) ---------- */
SELECT 'Producto' AS tabla, nombre_producto AS sin_foto FROM Producto WHERE imagen IS NULL
UNION ALL SELECT 'Materia_P', nombre_materia FROM Materia_P WHERE imagen IS NULL
UNION ALL SELECT 'Proveedor_identidad', nombre_proveedor FROM Proveedor_identidad WHERE imagen IS NULL;
GO

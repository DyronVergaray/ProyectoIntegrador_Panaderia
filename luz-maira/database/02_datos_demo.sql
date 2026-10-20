/* =====================================================================
   Panadería Luz Mayra - Datos de demostración y ajustes (SQL Server)
   Ejecutar DESPUÉS de 01_schema.sql. Se puede ejecutar más de una vez:
   los datos de catálogo no se duplican y los pedidos solo se crean si no existen.
   Los pedidos se generan con fechas relativas a HOY (últimos 60 días).
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

/* ---------- 1. AJUSTE DE ESTRUCTURA ----------
   La contraseña pasa a VARCHAR(100) para guardar hashes BCrypt (60 caracteres). */
ALTER TABLE Credenciales_usuario ALTER COLUMN [contraseña] VARCHAR(100) NOT NULL;
GO

/* ---------- 2. CATÁLOGOS ---------- */
IF NOT EXISTS (SELECT 1 FROM Roles WHERE nombre_rol = 'Despachador') INSERT INTO Roles (nombre_rol) VALUES ('Despachador');
IF NOT EXISTS (SELECT 1 FROM Roles WHERE nombre_rol = 'Entregador')  INSERT INTO Roles (nombre_rol) VALUES ('Entregador');

UPDATE Tipo_Pedido SET nombre_tipo = 'Virtual' WHERE nombre_tipo = 'Recojo en tienda';
IF NOT EXISTS (SELECT 1 FROM Tipo_Pedido WHERE nombre_tipo = 'Personalizado') INSERT INTO Tipo_Pedido (nombre_tipo) VALUES ('Personalizado');

INSERT INTO Categoria_Producto (nombre_categoria)
SELECT v.n FROM (VALUES ('Postres'), ('Gaseosas'), ('Snacks'), ('Productos diarios'), ('Panetones')) v(n)
WHERE NOT EXISTS (SELECT 1 FROM Categoria_Producto c WHERE c.nombre_categoria = v.n);

INSERT INTO Producto (id_categoriaProducto, nombre_producto, descripcion, precio_unitario)
SELECT c.id_categoriaProducto, v.nombre, v.descr, v.precio
FROM (VALUES
    ('Panes',             'Pan de Mantequilla',       'Pan tradicional esponjoso',         2.00),
    ('Panes',             'Croissant de Almendras',   'Hojaldre relleno de crema',         4.50),
    ('Pasteles',          'Pastel Tres Leches',       'Esponjoso bañado en tres leches',  18.00),
    ('Panes',             'Concha Rústica',           'Toque clásico de vainilla',         3.00),
    ('Panes',             'Baguette de Masa Madre',   'Corteza crujiente y natural',      12.00),
    ('Pasteles',          'Pastel de Chocolate Real', 'Para los amantes del cacao',       22.00),
    ('Postres',           'Flan de Vainilla',         'Cremoso con caramelo casero',       6.50),
    ('Postres',           'Gelatina de Fresa',        'Postre fresco de la casa',          3.50),
    ('Gaseosas',          'Inca Kola 500 ml',         'Gaseosa bien fría',                 3.50),
    ('Snacks',            'Tequeños x6',              'Rellenos de queso, listos para freír', 9.00),
    ('Snacks',            'Sándwich de Pollo',        'Pan ciabatta con pollo deshilachado', 7.50),
    ('Productos diarios', 'Pan Chapla',               'El pan de todos los días',          0.50),
    ('Panetones',         'Panetón Clásico',          'Con frutas confitadas',            25.00)
) v(cat, nombre, descr, precio)
JOIN Categoria_Producto c ON c.nombre_categoria = v.cat
WHERE NOT EXISTS (SELECT 1 FROM Producto p WHERE p.nombre_producto = v.nombre);
GO

/* ---------- 3. USUARIOS DE PRUEBA (contraseñas con BCrypt) ----------*/
UPDATE Credenciales_usuario SET [contraseña] = '$2b$10$YZyGBvdmw9LtWs66P2oTXOh0DGEs.zhUHWsnGP2BWGstMpiMj5dE2' WHERE email = 'admin@luzmayra.com';
UPDATE Credenciales_usuario SET [contraseña] = '$2b$10$lNZY8VNe7HoClsDe0u1mYeIvx/r8XPev7woh79zjkWY5IMNRummLW' WHERE email = 'cajero@luzmayra.com';
UPDATE Credenciales_usuario SET [contraseña] = '$2b$10$UC.ZnrzXf0b2fwhFgEEhZ.V3IN0w3i1cR13KfPbrxCLs9QVrfPm3m' WHERE email = 'cliente@luzmayra.com';

CREATE TABLE #nuevos (n INT IDENTITY(1,1), rol VARCHAR(20), nombre VARCHAR(20), ape1 VARCHAR(20), ape2 VARCHAR(20),
                      tel CHAR(9), email VARCHAR(100), clave VARCHAR(100));
INSERT INTO #nuevos (rol, nombre, ape1, ape2, tel, email, clave) VALUES
  ('Almacenero','Rosa','Huamán','Díaz','955111222','almacen@luzmayra.com','$2b$10$7LY2Hun25NBEQ/b.FzFRc.jGSxL6gbx95.H1XQNGFSiXriGstRlJa'),
  ('Despachador','Pedro','Vilca','Cruz','955333444','despacho@luzmayra.com','$2b$10$WbABxgK1uFm/qZeMZcKVQOT4qsmjjemArI53DHYQe89RKy3UKOCaa'),
  ('Entregador','Miguel','Ñahui','Soto','955555666','entrega@luzmayra.com','$2b$10$4krQowmhWZ1ivxEYKP1HueBogImTUNqhTg/XoL7LM5pPEFdtCGb7S'),
  ('Cliente','Lucía','Ramos','Vega','970100200','lucia.ramos@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.'),
  ('Cliente','Jorge','Salas','Pinto','971101210','jorge.salas@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.'),
  ('Cliente','Marta','Quispe','León','972102220','marta.quispe@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.'),
  ('Cliente','Carlos','Mena','Díaz','973103230','carlos.mena@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.'),
  ('Cliente','Elena','Torres','Ríos','974104240','elena.torres@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.'),
  ('Cliente','Luis','Paredes','Soto','975105250','luis.paredes@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.'),
  ('Cliente','Ana','Castillo','Mejía','976106260','ana.castillo@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.'),
  ('Cliente','Diego','Flores','Arce','977107270','diego.flores@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.'),
  ('Cliente','Sofía','Navarro','Lazo','978108280','sofia.navarro@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.'),
  ('Cliente','Raúl','Campos','Ibáñez','979109290','raul.campos@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.'),
  ('Cliente','Karen','Poma','Gil','980110300','karen.poma@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.'),
  ('Cliente','Iván','Rojas','Luna','981111310','iván.rojas@correo.com','$2b$10$3n/fACr0QQqHe7f2U7O4Kucd3GNrRpP4BWg9gVfoGAQ1Ejy./7Ao.');

DECLARE @n INT = 1, @max INT = (SELECT COUNT(*) FROM #nuevos), @cred INT;
WHILE @n <= @max
BEGIN
    IF NOT EXISTS (SELECT 1 FROM Credenciales_usuario cu JOIN #nuevos x ON x.email = cu.email WHERE x.n = @n)
    BEGIN
        INSERT INTO Credenciales_usuario ([contraseña], email) SELECT clave, email FROM #nuevos WHERE n = @n;
        SET @cred = SCOPE_IDENTITY();
        INSERT INTO Usuario (id_rol, id_credenciales, primer_nombre, primer_apellido, segundo_apellido, telefono)
        SELECT r.id_rol, @cred, x.nombre, x.ape1, x.ape2, x.tel
        FROM #nuevos x JOIN Roles r ON r.nombre_rol = x.rol WHERE x.n = @n;
    END
    SET @n += 1;
END
DROP TABLE #nuevos;
GO

/* ---------- 4. PEDIDOS DE DEMOSTRACIÓN (últimos 60 días) ---------- */
IF NOT EXISTS (SELECT 1 FROM Pedido_Gestion)
BEGIN
    DECLARE @rolCliente INT = (SELECT id_rol FROM Roles WHERE nombre_rol = 'Cliente');
    DECLARE @tVirtual INT = (SELECT id_tipo FROM Tipo_Pedido WHERE nombre_tipo = 'Virtual');
    DECLARE @tDelivery INT = (SELECT id_tipo FROM Tipo_Pedido WHERE nombre_tipo = 'Delivery');
    DECLARE @tPersonal INT = (SELECT id_tipo FROM Tipo_Pedido WHERE nombre_tipo = 'Personalizado');
    DECLARE @hoy DATE = CAST(GETDATE() AS DATE);
    DECLARE @atras INT = 59, @cantidad INT, @k INT, @cliente INT, @carrito INT, @lineas INT;
    DECLARE @tipo INT, @fecha DATE, @asignada DATE, @estado VARCHAR(20), @azar INT, @pedido INT, @direccion VARCHAR(50);

    WHILE @atras >= 0
    BEGIN
        SET @fecha = DATEADD(DAY, -@atras, @hoy);
        SET @cantidad = 6 + ABS(CHECKSUM(NEWID())) % 11;
        SET @k = 0;
        WHILE @k < @cantidad
        BEGIN
            SELECT TOP 1 @cliente = u.id_usuario FROM Usuario u WHERE u.id_rol = @rolCliente ORDER BY NEWID();

            INSERT INTO Carrito (id_usuario, estado, precio_total) VALUES (@cliente, 'Confirmado', 0);
            SET @carrito = SCOPE_IDENTITY();

            SET @lineas = 1 + ABS(CHECKSUM(NEWID())) % 4;
            INSERT INTO Carrito_Objetos (id_producto, id_carrito, cantidad, precio)
            SELECT TOP (@lineas) p.id_producto, @carrito, 1 + ABS(CHECKSUM(NEWID())) % 5, p.precio_unitario
            FROM Producto p ORDER BY NEWID();

            UPDATE Carrito SET precio_total = (SELECT SUM(cantidad * precio) FROM Carrito_Objetos WHERE id_carrito = @carrito)
            WHERE id_carrito = @carrito;

            SET @azar = ABS(CHECKSUM(NEWID())) % 100;
            SET @tipo = CASE WHEN @azar < 60 THEN @tVirtual WHEN @azar < 85 THEN @tDelivery ELSE @tPersonal END;
            SET @asignada = CASE WHEN @tipo = @tPersonal THEN DATEADD(DAY, 3 + ABS(CHECKSUM(NEWID())) % 8, @fecha) ELSE @fecha END;
            SET @direccion = CASE WHEN @tipo = @tDelivery
                THEN CHOOSE(1 + ABS(CHECKSUM(NEWID())) % 4, 'Av. Los Olivos 123', 'Jr. Las Flores 456', 'Av. Túpac Amaru 789', 'Calle Los Pinos 321')
                ELSE NULL END;

            SET @azar = ABS(CHECKSUM(NEWID())) % 100;
            SET @estado = CASE
                WHEN @atras > 2 THEN CASE WHEN @azar < 94 THEN 'Entregado' ELSE 'Cancelado' END
                ELSE CASE WHEN @azar < 55 THEN 'Entregado' WHEN @azar < 95 THEN 'Pendiente' ELSE 'Cancelado' END END;

            INSERT INTO Pedido_Gestion (id_carrito, id_tipo, pedido_fecha, fecha_asignada, hora_asignada, tiempo_estimado, estado_pedido, direccion)
            VALUES (@carrito, @tipo, @fecha, @asignada,
                    DATEADD(MINUTE, ABS(CHECKSUM(NEWID())) % 840, CAST('06:00' AS TIME)),
                    15 + ABS(CHECKSUM(NEWID())) % 46, @estado, @direccion);
            SET @pedido = SCOPE_IDENTITY();

            INSERT INTO Codigo (id_pedido, codigo)
            VALUES (@pedido, CAST(100 + ABS(CHECKSUM(NEWID())) % 900 AS VARCHAR(3)));

            SET @k += 1;
        END
        SET @atras -= 1;
    END
END
GO

/* ---------- 5. MATERIAS PRIMAS Y CONSUMO DIARIO (últimos 60 días) ----------
   La levadura queda con poco stock a propósito para que el dashboard muestre la alerta. */
IF NOT EXISTS (SELECT 1 FROM Materia_P)
BEGIN
    INSERT INTO Materia_P (id_medicion, nombre_materia, stock)
    SELECT m.id_medicion, v.n, v.s
    FROM (VALUES ('Harina', 'Kilogramo', '120'), ('Huevos', 'Unidad', '400'), ('Mantequilla', 'Kilogramo', '30'),
                 ('Azúcar', 'Kilogramo', '45'), ('Levadura', 'Gramo', '450'), ('Leche', 'Litro', '40')) v(n, u, s)
    JOIN Medicion m ON m.nombre_medicion = v.u;

    DECLARE @dia INT = 59, @registro INT;
    WHILE @dia >= 0
    BEGIN
        INSERT INTO Registro_Diario (fecha) VALUES (DATEADD(DAY, -@dia, CAST(GETDATE() AS DATE)));
        SET @registro = SCOPE_IDENTITY();

        INSERT INTO Lista_Materia (id_RegistroDiario, id_materia_P, id_medicion, cantidad_materia, descripcion)
        SELECT @registro, mp.id_materia_P, mp.id_medicion,
               CASE mp.nombre_materia
                    WHEN 'Harina'      THEN 18  + ABS(CHECKSUM(NEWID())) % 23
                    WHEN 'Huevos'      THEN 40  + ABS(CHECKSUM(NEWID())) % 51
                    WHEN 'Mantequilla' THEN 3   + ABS(CHECKSUM(NEWID())) % 6
                    WHEN 'Azúcar'      THEN 4   + ABS(CHECKSUM(NEWID())) % 8
                    WHEN 'Levadura'    THEN 200 + ABS(CHECKSUM(NEWID())) % 151
                    ELSE 5 + ABS(CHECKSUM(NEWID())) % 11 END,
               'Consumo en producción diaria'
        FROM Materia_P mp;
        SET @dia -= 1;
    END
END
GO

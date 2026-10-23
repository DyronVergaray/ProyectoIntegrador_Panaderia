/* =====================================================================
   Panadería Luz Mayra - Script de base de datos (SQL Server)
   Basado en el Modelo Físico Normalizado (draw.io)
   Orden: 1) Base de datos  2) Tablas  3) Llaves foráneas  4) Datos iniciales
   ===================================================================== */

/* ---------- 1. BASE DE DATOS ---------- */
IF DB_ID('PanaderiaLuzMayra') IS NULL
    CREATE DATABASE PanaderiaLuzMayra;
GO
USE PanaderiaLuzMayra;
GO

/* ---------- 2. TABLAS ---------- */

-- Seguridad y usuarios
CREATE TABLE Roles (
    id_rol      INT IDENTITY(1,1) NOT NULL,
    nombre_rol  VARCHAR(20) NOT NULL,
    CONSTRAINT PK_Roles PRIMARY KEY (id_rol),
    CONSTRAINT UQ_Roles_nombre UNIQUE (nombre_rol)
);

CREATE TABLE Credenciales_usuario (
    id_credenciales INT IDENTITY(1,1) NOT NULL,
    [contraseña]    VARCHAR(32) NOT NULL,      -- hash MD5 en hexadecimal (32 caracteres)
    email           VARCHAR(100) NOT NULL,
    CONSTRAINT PK_Credenciales PRIMARY KEY (id_credenciales),
    CONSTRAINT UQ_Credenciales_email UNIQUE (email)
);

CREATE TABLE Usuario (
    id_usuario       INT IDENTITY(1,1) NOT NULL,
    id_rol           INT NOT NULL,
    id_credenciales  INT NOT NULL,
    primer_nombre    VARCHAR(20),
    primer_apellido  VARCHAR(20),
    segundo_apellido VARCHAR(20),
    telefono         CHAR(9),
    CONSTRAINT PK_Usuario PRIMARY KEY (id_usuario),
    CONSTRAINT UQ_Usuario_credenciales UNIQUE (id_credenciales)
);

-- Catálogo y carrito
CREATE TABLE Categoria_Producto (
    id_categoriaProducto INT IDENTITY(1,1) NOT NULL,
    nombre_categoria     VARCHAR(30),
    CONSTRAINT PK_Categoria_Producto PRIMARY KEY (id_categoriaProducto)
);

CREATE TABLE Producto (
    id_producto          INT IDENTITY(1,1) NOT NULL,
    id_categoriaProducto INT NOT NULL,
    nombre_producto      VARCHAR(150) NOT NULL,
    descripcion          TEXT,
    precio_unitario      DECIMAL(10,2) NOT NULL,
    CONSTRAINT PK_Producto PRIMARY KEY (id_producto)
);

CREATE TABLE Carrito (
    id_carrito   INT IDENTITY(1,1) NOT NULL,
    id_usuario   INT NOT NULL,
    estado       VARCHAR(20),                 -- Activo / Confirmado
    precio_total DECIMAL(10,2) NOT NULL,
    CONSTRAINT PK_Carrito PRIMARY KEY (id_carrito)
);

CREATE TABLE Carrito_Objetos (
    id_objeto_carrito INT IDENTITY(1,1) NOT NULL,
    id_producto       INT NOT NULL,
    id_carrito        INT NOT NULL,
    cantidad          INT,
    precio            DECIMAL(10,2) NOT NULL,  -- precio unitario al momento de agregar
    CONSTRAINT PK_Carrito_Objetos PRIMARY KEY (id_objeto_carrito)
);

-- Pedidos y entrega
CREATE TABLE Tipo_Pedido (
    id_tipo     INT IDENTITY(1,1) NOT NULL,
    nombre_tipo VARCHAR(30),
    CONSTRAINT PK_Tipo_Pedido PRIMARY KEY (id_tipo)
);

CREATE TABLE Pedido_Gestion (
    id_pedido       INT IDENTITY(1,1) NOT NULL,
    id_carrito      INT NOT NULL,
    id_tipo         INT NOT NULL,
    pedido_fecha    DATE NOT NULL,
    fecha_asignada  DATE NOT NULL,
    hora_asignada   TIME NOT NULL,
    tiempo_estimado INT,
    estado_pedido   VARCHAR(20),
    direccion       VARCHAR(50),
    CONSTRAINT PK_Pedido_Gestion PRIMARY KEY (id_pedido)
);

CREATE TABLE Codigo (
    id_codigo INT IDENTITY(1,1) NOT NULL,
    id_pedido INT NOT NULL,
    codigo    CHAR(3),
    CONSTRAINT PK_Codigo PRIMARY KEY (id_codigo)
);

CREATE TABLE Conductor (
    id_Conductor     INT IDENTITY(1,1) NOT NULL,
    primer_nombre    VARCHAR(20),
    primer_apellido  VARCHAR(20),
    segundo_apellido VARCHAR(20),
    telefono         CHAR(9),
    disponibilidad   VARCHAR(20),
    CONSTRAINT PK_Conductor PRIMARY KEY (id_Conductor)
);

CREATE TABLE Pedido_Conductor (
    num_asig_cond INT IDENTITY(1,1) NOT NULL,
    id_pedido     INT NOT NULL,
    id_conductor  INT NOT NULL,
    CONSTRAINT PK_Pedido_Conductor PRIMARY KEY (num_asig_cond)
);

CREATE TABLE Vehiculo (
    id_vehiculo     INT IDENTITY(1,1) NOT NULL,
    placa           VARCHAR(10) NOT NULL,
    marca           VARCHAR(50),
    modelo          VARCHAR(50),
    capacidad_carga DECIMAL(10,2),
    estado_vehiculo VARCHAR(20),
    CONSTRAINT PK_Vehiculo PRIMARY KEY (id_vehiculo)
);

CREATE TABLE Pedido_Vehiculo (
    num_asig_vehi INT IDENTITY(1,1) NOT NULL,
    id_pedido     INT NOT NULL,
    id_vehiculo   INT NOT NULL,
    CONSTRAINT PK_Pedido_Vehiculo PRIMARY KEY (num_asig_vehi)
);

-- Inventario de materia prima
CREATE TABLE Medicion (
    id_medicion     INT IDENTITY(1,1) NOT NULL,
    nombre_medicion VARCHAR(20),
    CONSTRAINT PK_Medicion PRIMARY KEY (id_medicion)
);

CREATE TABLE Materia_P (
    id_materia_P   INT IDENTITY(1,1) NOT NULL,
    id_medicion    INT NOT NULL,
    nombre_materia VARCHAR(50),
    stock          VARCHAR(50),
    CONSTRAINT PK_Materia_P PRIMARY KEY (id_materia_P)
);

CREATE TABLE Materia_P_Planeacion (
    cod_planeacion  INT IDENTITY(1,1) NOT NULL,
    id_pedido       INT NOT NULL,
    id_materia_P    INT NOT NULL,
    id_medicion     INT NOT NULL,
    cantidad_materia INT,
    CONSTRAINT PK_Materia_P_Planeacion PRIMARY KEY (cod_planeacion)
);

CREATE TABLE Registro_Diario (
    id_RegistroDiario INT IDENTITY(1,1) NOT NULL,
    fecha             DATE,
    CONSTRAINT PK_Registro_Diario PRIMARY KEY (id_RegistroDiario)
);

CREATE TABLE Lista_Materia (
    cod_lista         INT IDENTITY(1,1) NOT NULL,
    id_RegistroDiario INT NOT NULL,
    id_materia_P      INT NOT NULL,
    id_medicion       INT NOT NULL,
    cantidad_materia  INT,
    descripcion       VARCHAR(150),
    CONSTRAINT PK_Lista_Materia PRIMARY KEY (cod_lista)
);

-- Proveedores
CREATE TABLE Proveedor_identidad (
    id_proveedor        INT IDENTITY(1,1) NOT NULL,
    nombre_proveedor    VARCHAR(150),
    direccion_proveedor VARCHAR(255),
    ruc_proveedor       VARCHAR(20) NOT NULL,
    CONSTRAINT PK_Proveedor_identidad PRIMARY KEY (id_proveedor)
);

CREATE TABLE Proveedor_solicitudes (
    id_solicitud           INT IDENTITY(1,1) NOT NULL,
    id_materia_P           INT NOT NULL,
    id_proveedor           INT NOT NULL,
    id_medicion            INT NOT NULL,
    cantidad_producto_soli VARCHAR(50),
    precio_solicitud       DECIMAL(10,2) NOT NULL,
    fecha_solicitado       DATE NOT NULL,
    fecha_estimada_entrega DATE,
    estado_solicitud       VARCHAR(50) NOT NULL,
    CONSTRAINT PK_Proveedor_solicitudes PRIMARY KEY (id_solicitud)
);
GO

/* ---------- 3. LLAVES FORÁNEAS ---------- */
ALTER TABLE Usuario ADD
    CONSTRAINT FK_Usuario_Roles        FOREIGN KEY (id_rol)          REFERENCES Roles(id_rol),
    CONSTRAINT FK_Usuario_Credenciales FOREIGN KEY (id_credenciales) REFERENCES Credenciales_usuario(id_credenciales);

ALTER TABLE Producto ADD
    CONSTRAINT FK_Producto_Categoria FOREIGN KEY (id_categoriaProducto) REFERENCES Categoria_Producto(id_categoriaProducto);

ALTER TABLE Carrito ADD
    CONSTRAINT FK_Carrito_Usuario FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario);

ALTER TABLE Carrito_Objetos ADD
    CONSTRAINT FK_CarritoObj_Producto FOREIGN KEY (id_producto) REFERENCES Producto(id_producto),
    CONSTRAINT FK_CarritoObj_Carrito  FOREIGN KEY (id_carrito)  REFERENCES Carrito(id_carrito);

ALTER TABLE Pedido_Gestion ADD
    CONSTRAINT FK_Pedido_Carrito FOREIGN KEY (id_carrito) REFERENCES Carrito(id_carrito),
    CONSTRAINT FK_Pedido_Tipo    FOREIGN KEY (id_tipo)    REFERENCES Tipo_Pedido(id_tipo);

ALTER TABLE Codigo ADD
    CONSTRAINT FK_Codigo_Pedido FOREIGN KEY (id_pedido) REFERENCES Pedido_Gestion(id_pedido);

ALTER TABLE Pedido_Conductor ADD
    CONSTRAINT FK_PedCond_Pedido    FOREIGN KEY (id_pedido)    REFERENCES Pedido_Gestion(id_pedido),
    CONSTRAINT FK_PedCond_Conductor FOREIGN KEY (id_conductor) REFERENCES Conductor(id_Conductor);

ALTER TABLE Pedido_Vehiculo ADD
    CONSTRAINT FK_PedVehi_Pedido   FOREIGN KEY (id_pedido)   REFERENCES Pedido_Gestion(id_pedido),
    CONSTRAINT FK_PedVehi_Vehiculo FOREIGN KEY (id_vehiculo) REFERENCES Vehiculo(id_vehiculo);

ALTER TABLE Materia_P ADD
    CONSTRAINT FK_MateriaP_Medicion FOREIGN KEY (id_medicion) REFERENCES Medicion(id_medicion);

ALTER TABLE Materia_P_Planeacion ADD
    CONSTRAINT FK_Planeacion_Pedido   FOREIGN KEY (id_pedido)    REFERENCES Pedido_Gestion(id_pedido),
    CONSTRAINT FK_Planeacion_Materia  FOREIGN KEY (id_materia_P) REFERENCES Materia_P(id_materia_P),
    CONSTRAINT FK_Planeacion_Medicion FOREIGN KEY (id_medicion)  REFERENCES Medicion(id_medicion);

ALTER TABLE Lista_Materia ADD
    CONSTRAINT FK_Lista_Registro  FOREIGN KEY (id_RegistroDiario) REFERENCES Registro_Diario(id_RegistroDiario),
    CONSTRAINT FK_Lista_Materia   FOREIGN KEY (id_materia_P)      REFERENCES Materia_P(id_materia_P),
    CONSTRAINT FK_Lista_Medicion  FOREIGN KEY (id_medicion)       REFERENCES Medicion(id_medicion);

ALTER TABLE Proveedor_solicitudes ADD
    CONSTRAINT FK_Solicitud_Materia   FOREIGN KEY (id_materia_P) REFERENCES Materia_P(id_materia_P),
    CONSTRAINT FK_Solicitud_Proveedor FOREIGN KEY (id_proveedor) REFERENCES Proveedor_identidad(id_proveedor),
    CONSTRAINT FK_Solicitud_Medicion  FOREIGN KEY (id_medicion)  REFERENCES Medicion(id_medicion);
GO

/* ---------- 4. DATOS INICIALES ---------- */

-- Roles (el sistema depende de estos cuatro nombres exactos)
INSERT INTO Roles (nombre_rol) VALUES ('Cliente'), ('Cajero'), ('Almacenero'), ('Administrador');

-- Tipos de pedido (id 2 = Delivery, exige dirección)
INSERT INTO Tipo_Pedido (nombre_tipo) VALUES ('Recojo en tienda'), ('Delivery');

-- Unidades de medida
INSERT INTO Medicion (nombre_medicion) VALUES ('Kilogramo'), ('Gramo'), ('Litro'), ('Unidad');

-- Catálogo de ejemplo
INSERT INTO Categoria_Producto (nombre_categoria) VALUES ('Panes'), ('Pasteles'), ('Bocaditos'), ('Bebidas');

INSERT INTO Producto (id_categoriaProducto, nombre_producto, descripcion, precio_unitario) VALUES
(1, 'Pan francés',             'Pan crocante recién horneado.',            1.50),
(1, 'Pan ciabatta',            'Pan rústico de corteza dorada.',           2.00),
(1, 'Pan integral',            'Pan de molde integral con semillas.',      8.50),
(2, 'Torta de chocolate',      'Torta húmeda con cobertura de fudge.',     45.00),
(2, 'Torta tres leches',       'Bizcocho bañado en tres leches.',          42.00),
(3, 'Empanada de pollo',       'Masa hojaldrada rellena de pollo.',        4.50),
(3, 'Alfajor de maicena',      'Alfajor relleno de manjar blanco.',        2.50),
(4, 'Chicha morada 1 L',       'Chicha morada casera.',                    7.00);

-- Usuarios de prueba
DECLARE @cred INT;

INSERT INTO Credenciales_usuario ([contraseña], email)
VALUES (LOWER(CONVERT(VARCHAR(32), HASHBYTES('MD5', 'Admin123'), 2)), 'admin@luzmayra.com');
SET @cred = SCOPE_IDENTITY();
INSERT INTO Usuario (id_rol, id_credenciales, primer_nombre, primer_apellido, segundo_apellido, telefono)
VALUES (4, @cred, 'Admin', 'Principal', 'Sistema', '999999999');

INSERT INTO Credenciales_usuario ([contraseña], email)
VALUES (LOWER(CONVERT(VARCHAR(32), HASHBYTES('MD5', 'Cajero123'), 2)), 'cajero@luzmayra.com');
SET @cred = SCOPE_IDENTITY();
INSERT INTO Usuario (id_rol, id_credenciales, primer_nombre, primer_apellido, segundo_apellido, telefono)
VALUES (2, @cred, 'Carla', 'Quispe', 'Rojas', '988888888');

INSERT INTO Credenciales_usuario ([contraseña], email)
VALUES (LOWER(CONVERT(VARCHAR(32), HASHBYTES('MD5', 'Cliente123'), 2)), 'cliente@luzmayra.com');
SET @cred = SCOPE_IDENTITY();
INSERT INTO Usuario (id_rol, id_credenciales, primer_nombre, primer_apellido, segundo_apellido, telefono)
VALUES (1, @cred, 'Luis', 'Paredes', 'Soto', '977777777');
GO

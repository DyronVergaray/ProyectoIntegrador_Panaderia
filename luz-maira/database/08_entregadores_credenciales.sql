/* =====================================================================
   Panadería Luz Mayra - Cada entregador con sus propias credenciales
   Ejecutar DESPUÉS de 07_conductores_vehiculos_demo.sql. Se puede ejecutar varias veces.

   CAMBIO DE ESTRUCTURA (único): agrega la columna Conductor.id_usuario (NULL permitido),
   que enlaza al conductor con su cuenta en la tabla Usuario (rol Entregador).
   Así cada entregador entra con su correo y contraseña y solo ve los pedidos que le asignaron.
   - Los conductores sin cuenta (por ejemplo los de 07) siguen funcionando: el administrador
     les crea el acceso desde "Entregadores y vehículos".
   - Un usuario solo puede estar enlazado a UN conductor (índice único filtrado).
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

IF COL_LENGTH('Conductor', 'id_usuario') IS NULL
    ALTER TABLE Conductor ADD id_usuario INT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'FK_Conductor_Usuario')
    ALTER TABLE Conductor ADD CONSTRAINT FK_Conductor_Usuario FOREIGN KEY (id_usuario) REFERENCES Usuario(id_usuario);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Conductor_Usuario')
    CREATE UNIQUE INDEX UX_Conductor_Usuario ON Conductor(id_usuario) WHERE id_usuario IS NOT NULL;
GO

/* Los usuarios que ya tienen rol Entregador (p. ej. entrega@luzmayra.com) pasan a ser conductores enlazados */
INSERT INTO Conductor (primer_nombre, primer_apellido, segundo_apellido, telefono, disponibilidad, id_usuario)
SELECT u.primer_nombre, u.primer_apellido, u.segundo_apellido, u.telefono, 'Disponible', u.id_usuario
FROM Usuario u JOIN Roles r ON r.id_rol = u.id_rol
WHERE r.nombre_rol = 'Entregador'
  AND NOT EXISTS (SELECT 1 FROM Conductor c WHERE c.id_usuario = u.id_usuario);
GO

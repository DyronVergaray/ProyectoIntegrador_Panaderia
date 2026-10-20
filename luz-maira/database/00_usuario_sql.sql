/* =====================================================================
   Crea el usuario SQL que usa la aplicación (el mismo de application.properties).
   Ejecutar PRIMERO, conectado como administrador (sa o Windows), cuando ya exista la base de datos
   (después de 01_schema.sql). Cambia 'pan' y la clave si usas otros datos.
   ===================================================================== */
USE master;
GO
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = 'pan')
    CREATE LOGIN pan WITH PASSWORD = 'pan', CHECK_POLICY = OFF;
GO
USE PanaderiaLuzMayra;
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = 'pan')
    CREATE USER pan FOR LOGIN pan;
ALTER ROLE db_owner ADD MEMBER pan;
GO

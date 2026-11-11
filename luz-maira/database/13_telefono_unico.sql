/* =====================================================================
   13_telefono_unico.sql
   Restricción: dos usuarios no pueden tener el mismo teléfono.
   (La aplicación ya lo valida; este índice lo garantiza también en la BD.)
   Ejecutar en la base PanaderiaLuzMayra.
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

/* 1) PRIMERO revisa si ya hay teléfonos repetidos. Si devuelve filas,
      cambia el teléfono de los repetidos antes de crear el índice. */
SELECT telefono, COUNT(*) AS veces
FROM Usuario
WHERE telefono IS NOT NULL
GROUP BY telefono
HAVING COUNT(*) > 1;
GO

/* 2) Índice único (los NULL se permiten y no cuentan como repetidos). */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_Usuario_telefono')
    CREATE UNIQUE INDEX UX_Usuario_telefono ON Usuario(telefono) WHERE telefono IS NOT NULL;
GO

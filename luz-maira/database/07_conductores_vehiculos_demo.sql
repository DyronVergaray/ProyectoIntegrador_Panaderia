/* =====================================================================
   Panadería Luz Mayra - Conductores y vehículos de demostración
   Ejecutar DESPUÉS de 06_imagenes_demo.sql. Se puede ejecutar varias veces.
   La pantalla "Delivery" del almacenero asigna cada pedido Delivery / Personalizado a un conductor y un vehículo
   que estén 'Disponible'. Sin estos datos no hay a quién asignar.
   OJO: nombres, teléfonos y placas son de EJEMPLO. Cámbialos por los reales si los tienes.
   No agrega columnas ni tablas: usa las tablas Conductor y Vehiculo del modelo original.
   ===================================================================== */
USE PanaderiaLuzMayra;
GO

INSERT INTO Conductor (primer_nombre, primer_apellido, segundo_apellido, telefono, disponibilidad)
SELECT v.nombre, v.ape1, v.ape2, v.tel, 'Disponible'
FROM (VALUES
    ('Marco',  'Salazar', 'Huanca', '966111222'),
    ('Julio',  'Mamani',  'Cruz',   '966333444'),
    ('Renzo',  'Ccori',   'Vera',   '966555666')
) v(nombre, ape1, ape2, tel)
WHERE NOT EXISTS (SELECT 1 FROM Conductor c WHERE c.primer_nombre = v.nombre AND c.primer_apellido = v.ape1);
GO

INSERT INTO Vehiculo (placa, marca, modelo, capacidad_carga, estado_vehiculo)
SELECT v.placa, v.marca, v.modelo, v.capacidad, 'Disponible'
FROM (VALUES
    ('A1B-234', 'Honda',  'Moto de reparto', 40.00),
    ('C5D-678', 'Yamaha', 'Moto de reparto', 40.00),
    ('E9F-012', 'Suzuki', 'Furgoneta',      400.00)
) v(placa, marca, modelo, capacidad)
WHERE NOT EXISTS (SELECT 1 FROM Vehiculo x WHERE x.placa = v.placa);
GO

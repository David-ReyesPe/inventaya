-- InventaYa - datos ficticios de prueba (catalogo de productos).
-- Requiere que servicio-inventario ya haya ejecutado la migracion Flyway V1 (tablas creadas).
-- Es idempotente: se puede ejecutar varias veces sin duplicar datos.
-- Las ventas NO se insertan por SQL: deben pasar por la API para generar los eventos Kafka
-- y el descuento de stock (use scripts/insertar_datos_prueba.sh o insertar_datos_prueba.ps1).

insert into productos (id, nombre, precio, stock, umbral_minimo) values
 ('00000000-0000-4000-8000-000000000001', 'Arroz 500g',          3200, 80, 15),
 ('00000000-0000-4000-8000-000000000002', 'Aceite Vegetal 1L',   8500, 60, 15),
 ('00000000-0000-4000-8000-000000000003', 'Leche Entera 1L',     4200, 90, 20),
 ('00000000-0000-4000-8000-000000000004', 'Azucar 1kg',          4800, 70, 15),
 ('00000000-0000-4000-8000-000000000005', 'Pan Tajado',          5200, 12, 10),
 ('00000000-0000-4000-8000-000000000006', 'Huevos AA x30',      18500, 40, 10),
 ('00000000-0000-4000-8000-000000000007', 'Cafe Molido 250g',    7900, 55, 12),
 ('00000000-0000-4000-8000-000000000008', 'Gaseosa 1.5L',        5600, 100, 25),
 ('00000000-0000-4000-8000-000000000009', 'Detergente 1kg',     11200, 9, 10),
 ('00000000-0000-4000-8000-000000000010', 'Papel Higienico x4',  7400, 65, 15)
on conflict (id) do nothing;
-- Pan Tajado (12 / umbral 10) y Detergente (9 / umbral 10) quedan cerca o bajo el umbral
-- a proposito, para probar el dashboard de stock bajo.

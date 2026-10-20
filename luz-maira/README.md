# Panadería Luz Mayra — Spring Boot + SQL Server

Arquitectura: **Vista (HTML/CSS/JS) → Controller → Service → DAO (JdbcTemplate) → SQL Server**.

## Cómo ponerlo a funcionar
1. En SQL Server ejecuta, en orden: `database/01_schema.sql` y `database/02_datos_demo.sql`
   (crea las tablas y genera productos, usuarios, 60 días de pedidos y consumo de materias primas).
2. Pon tu clave de SQL Server en la variable de entorno `DB_PASS` (o edita `application.properties`).
   Opcionales: `DB_URL`, `DB_USER`. El puerto 1433 debe estar habilitado (SQL Server Configuration Manager → TCP/IP).
3. Ejecuta `LuzMairaApplication` (o `./mvnw spring-boot:run`) y abre **http://localhost:8080**.
   No abras los HTML con Live Server ni con doble clic: el front y la API deben salir del mismo puerto.

## API
| Ruta | Descripción |
|---|---|
| `GET /api/categorias` | Categorías |
| `GET /api/productos?q=&categorias=1,2` | Productos filtrados |
| `GET /api/productos/favoritos` | 4 más vendidos de los últimos 7 días |
| `POST /api/productos` · `PUT /api/productos/{id}` | Crear / editar producto |
| `GET /api/roles` · `GET /api/usuarios` · `POST /api/usuarios` | Roles y usuarios (clave con BCrypt) |
| `GET /api/tipos-pedido` · `GET /api/pedidos?tipo=&clienteId=` · `GET /api/pedidos/{id}` | Pedidos y su detalle |
| `GET /api/dashboard?rango=7\|30\|mes&dias=&productos=&estados=` | Dashboard con filtros cruzados |

## Autenticación
Aún no está activa: `SecurityConfig` deja todas las rutas públicas. Cuando se active, el historial dejará de usar
`CLIENTE_DEMO_ID` (js/config.js) y tomará el usuario de la sesión.

## Front: dónde está cada cosa (sin código repetido)
- `js/ui.js`: `h()` para crear elementos, cabecera, barra lateral, tablas, tarjetas, ventanas y formularios.
  Cada HTML solo trae su contenido propio; la cabecera y la barra lateral se inyectan según los `data-*` del `<body>`.
- `js/api.js`: único lugar con `fetch`. `js/catalogo.js`: listado de productos. `js/carrito.js`: panel "Tu Pedido".
- `js/paginas/*.js`: un script por pantalla (tienda, admin-productos, usuarios, pedidos, dashboard, inicio, maqueta).

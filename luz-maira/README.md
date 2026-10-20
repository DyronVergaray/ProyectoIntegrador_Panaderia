# Panadería Luz Mayra — Spring Boot + SQL Server

Arquitectura: **Vista (HTML/CSS/JS) → Controller → Service → DAO (JdbcTemplate) → SQL Server**.

## Cómo ponerlo a funcionar
1. En SQL Server ejecuta, en orden: `01_schema.sql`, `02_datos_demo.sql`, `03_roles_codigos_imagenes.sql`
   (tablas, datos de demostración, y la columna de imagen + tipo "Presencial" + códigos de 3 dígitos).
   Si tu usuario SQL no puede iniciar sesión, ejecuta antes `00_usuario_sql.sql` (crea el usuario `pan`).
   **Sin el script 03 la aplicación falla** (falta la columna `imagen`).
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
| `POST /api/productos` · `PUT /api/productos/{id}` · `POST /api/productos/{id}/imagen` | Crear / editar producto y subir su foto |
| `POST /api/login` · `GET /api/me` · `POST /api/logout` · `POST /api/registro` | Sesión y registro de clientes |
| `POST /api/pedidos` · `GET /api/pedidos/mios` | Consolidar pedido (devuelve el código) · historial del cliente |
| `GET /api/roles` · `GET /api/usuarios` · `POST /api/usuarios` | Roles y usuarios (clave con BCrypt) |
| `GET /api/tipos-pedido` · `GET /api/pedidos?tipo=` · `GET /api/pedidos/{id}` | Pedidos y su detalle |
| `GET /api/dashboard?rango=7\|30\|mes&dias=&productos=&estados=` | Dashboard con filtros cruzados |

## Roles y sesión
El inicio de sesión usa la sesión HTTP de Spring Security (`POST /api/login`, `GET /api/me`, `POST /api/logout`).
`SecurityConfig` define qué puede hacer cada rol, tanto en las páginas como en la API:

| Rol | Páginas | Acciones |
|---|---|---|
| Cliente | catalogo, historial | Ver productos, consolidar pedidos, ver solo su historial |
| Cajero | cajera, historial | Registrar ventas en caja (tipo Presencial), ver todos los pedidos |
| Almacenero | almacen-* | Pantallas de almacén (maquetas) |
| Despachador | despachador | Pantalla de despacho (maqueta) |
| Entregador | entregador | Pantalla de entrega (maqueta) |
| Administrador | admin-*, almacen-* | Productos (crear, editar, imagen), usuarios, pedidos personalizados, dashboard |

Sin sesión, las páginas llevan a `index.html` con el panel de login abierto; la API responde 401. Con sesión pero sin
permiso, 403 (la página vuelve al inicio con un aviso). CSRF está desactivado porque la API solo recibe JSON desde el
mismo origen; antes de publicar en internet conviene activarlo.

## Código del pedido
Al consolidar, el servidor genera un código aleatorio de 3 dígitos (100–999) que no se repite entre pedidos
pendientes. Se guarda en `Codigo` y se muestra al cliente y en el recibo de la cajera.

## Imágenes de productos
Admin → Productos → Editar / Añadir → campo "Imagen" (JPG, PNG o WEBP, máx. 2 MB). El archivo se guarda en la carpeta
`imagenes-productos/` (propiedad `app.imagenes.carpeta`) y en la BD solo queda su nombre (columna `Producto.imagen`).
Se sirven desde `/img/productos/...`. Los productos sin foto muestran el cuadro rayado.

## Front: dónde está cada cosa (sin código repetido)
- `js/ui.js`: `h()` para crear elementos, cabecera, barra lateral, tablas, tarjetas, ventanas y formularios.
  Cada HTML solo trae su contenido propio; la cabecera y la barra lateral se inyectan según los `data-*` del `<body>`.
- `js/api.js`: único lugar con `fetch`. `js/catalogo.js`: listado de productos. `js/carrito.js`: panel "Tu Pedido".
- `js/paginas/*.js`: un script por pantalla (tienda, admin-productos, usuarios, pedidos, dashboard, inicio, maqueta).

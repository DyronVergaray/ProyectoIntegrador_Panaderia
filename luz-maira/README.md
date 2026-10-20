# Panadería Luz Mayra — Spring Boot + SQL Server

Arquitectura: **Vista (HTML/CSS/JS) → Controller → Service → DAO (JdbcTemplate) → SQL Server**.

## Cómo ponerlo a funcionar
1. En SQL Server ejecuta, en orden: `01_schema.sql`, `02_datos_demo.sql`, `03_roles_codigos_imagenes.sql` y
   `04_stock_y_minimos.sql` (tablas, datos de demostración, columna de imagen + tipo "Presencial" + códigos de
   3 dígitos, y stock / stock mínimo).
   Si tu usuario SQL no puede iniciar sesión, ejecuta antes `00_usuario_sql.sql` (crea el usuario `pan`).
   **Sin los scripts 03 y 04 la aplicación falla** (faltan las columnas `imagen`, `stock` y `stock_minimo`).
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
| Almacenero | almacen-* | Productos base y stock de productos (desde la BD); el resto del almacén son maquetas |
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
Admin → Productos → Editar / Añadir → zona "Imagen del producto": se **arrastra y suelta** la foto o se hace clic para
buscarla en las carpetas (JPG, PNG o WEBP, máx. 2 MB). Se guarda en `src/main/resources/static/img/productos/`
(propiedad `app.imagenes.carpeta`; si publicas el .jar usa una ruta absoluta) con un nombre único (UUID), así que no
hay conflictos de nombre. En la BD solo queda el nombre del archivo (`Producto.imagen`). Sin foto, se ve el cuadro rayado.

## Stock
- `Producto.stock` y `Producto.stock_minimo`; `Materia_P.stock_minimo`.
- **Los panes (categoría "Panes") no controlan stock**: su stock es NULL y nunca se verifica.
- Productos agotados (stock 0): no aparecen en el catálogo del cliente ni de la cajera (el admin sí los ve para editarlos).
- No se puede pedir más de lo disponible: se valida en pantalla y otra vez en el servidor, y al consolidar el pedido
  se descuenta el stock en la misma transacción.
- El almacenero ve en "Stock de productos" todos los productos con stock (agotados, bajos y disponibles).

## Reporte en Excel (Apache POI)
Dashboard → "Exportar a Excel": descarga un .xlsx con lo que esté marcado en los gráficos (periodo, días, productos,
estados). Hojas: Resumen, Ventas por día, Productos, Estado de pedidos y Materias primas.

## Registro de errores (Logback)
Configurado en `src/main/resources/logback-spring.xml`. Genera en la carpeta `logs/`:
`luz-maira.log` (actividad: inicios de sesión, pedidos, productos, reportes) y `errores.log` (solo WARN y ERROR, con la
traza completa). Rotan por día/tamaño y se borran a los 30 días. Los errores inesperados de la API quedan ahí y el
usuario ve un mensaje sencillo.

## Librerías
Spring Boot (web, jdbc, security, validation, mongodb), mssql-jdbc, Apache POI (Excel), Logback (logs).
Front: SweetAlert2 (avisos), guardada en `js/lib/` para que funcione sin internet.

## Front: dónde está cada cosa (sin código repetido)
- `js/ui.js`: `h()` para crear elementos, cabecera, barra lateral, tablas, tarjetas, ventanas y formularios.
  Cada HTML solo trae su contenido propio; la cabecera y la barra lateral se inyectan según los `data-*` del `<body>`.
- `js/api.js`: único lugar con `fetch`. `js/catalogo.js`: listado de productos. `js/carrito.js`: panel "Tu Pedido".
- `js/paginas/*.js`: un script por pantalla (tienda, admin-productos, usuarios, pedidos, dashboard, inicio, maqueta).

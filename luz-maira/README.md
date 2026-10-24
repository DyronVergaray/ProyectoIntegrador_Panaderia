# Panadería Luz Mayra — Spring Boot + SQL Server

Arquitectura: **Vista (HTML/CSS/JS) → Controller → Service → DAO (JdbcTemplate) → SQL Server**.

## Cómo ponerlo a funcionar
1. En SQL Server ejecuta, en orden: `01_schema.sql`, `02_datos_demo.sql`, `03_roles_codigos_imagenes.sql`,
   `04_stock_y_minimos.sql`, `05_imagenes_materias_proveedores.sql`, `06_imagenes_demo.sql` y `07_conductores_vehiculos_demo.sql` (tablas, datos de demostración, columna de imagen
   + tipo "Presencial" + códigos de 3 dígitos, stock / stock mínimo, y fotos de productos base y proveedores).
   Si tu usuario SQL no puede iniciar sesión, ejecuta antes `00_usuario_sql.sql` (crea el usuario `pan`).
   **Sin los scripts 03, 04 y 05 la aplicación falla** (faltan las columnas `imagen`, `stock`, `stock_minimo` y `descripcion`).
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
(propiedad `app.imagenes.carpeta`; la ruta relativa se cuenta desde la raíz del proyecto, no desde la carpeta que abra el IDE; si publicas el .jar usa una ruta absoluta) con un nombre único (UUID), así que no
hay conflictos de nombre. En la BD solo queda el nombre del archivo (`Producto.imagen`). Sin foto, se ve el cuadro rayado.

Lo mismo funciona en **Almacén → Productos Base** (`Materia_P.imagen`, carpeta `img/materias/`) y **Almacén → Proveedores**
(`Proveedor_identidad.imagen`, carpeta `img/proveedores/`): botón "+ Nuevo..." o "✎ Editar" en cada tarjeta. Las tres carpetas
quedan una al lado de la otra (la de productos es la que marca `app.imagenes.carpeta`; las otras dos se crean solas junto a ella).

## Filtros de stock (Admin → Productos)
En la barra lateral, bloque "Estado del stock": **Productos con poco stock** (stock menor o igual a su stock mínimo, incluye los
agotados) y **Desabastecidos** (stock en 0). Se pueden combinar con la búsqueda y las categorías; si se marcan los dos salen los
que cumplan cualquiera. Los panes no controlan stock, así que nunca aparecen en estos filtros. Solo el administrador puede usarlos
(`GET /api/productos?pocoStock=true&desabastecidos=true`).

## Despacho, delivery y almacén (pantallas operativas)
Todas leen y guardan en la BD (ya no son maquetas). Ejecuta también `07_conductores_vehiculos_demo.sql` (conductores y vehículos de ejemplo).

| Rol | Pantalla | Qué hace |
|---|---|---|
| Despachador | `despachador.html` | Pedidos **Virtuales**: elige el pedido, pide al cliente su código de 3 dígitos y confirma → el pedido pasa a Entregado. |
| Almacenero / Admin | Almacén → **Asignación-Delivery** | Asigna cada pedido **Delivery / Personalizado** a un conductor y un vehículo disponibles (quedan Ocupado / En ruta). |
| Entregador | `entregador.html` | Ve los Delivery / Personalizados ya asignados y confirma con el código del cliente → Entregado, y el conductor y el vehículo quedan disponibles. |
| Almacenero / Admin | Almacén → **Registro del día** | Anota ingresos y salidas de productos base; cada registro actualiza el stock (la salida no puede superar lo que hay). |
| Almacenero / Admin | Almacén → **Solicitud Proveedores** | Crea solicitudes a un proveedor (quedan "En camino"); **Confirmar Recepción** las pasa a "Recibido", suma el stock y deja un Ingreso en el registro del día. |

Reglas: el estado del pedido en la BD sigue siendo Pendiente / Entregado (el dashboard no cambia); "asignado" significa tener fila en
`Pedido_Conductor` y `Pedido_Vehiculo`. El código del pedido **no** se muestra al despachador, entregador ni almacenero (solo al cliente,
la cajera y el admin). En `Lista_Materia` el tipo de movimiento va al inicio de la descripción ("Ingreso: ..." / "Salida: ..."); el
consumo del dashboard solo cuenta las salidas. Las cantidades del registro del día son enteras (la columna es INT): para medios kilos se registra en gramos.

## Portada pública
- El **catálogo completo** se ve sin iniciar sesión, con búsqueda y filtro por categorías (el admin es el único que ve productos agotados).
- Al intentar agregar un producto (o abrir el carrito) sin sesión sale un aviso de SweetAlert2 y, al aceptarlo, se abre el panel de perfil para iniciar sesión o registrarse. Un cliente con sesión pasa a su catálogo de pedidos.
- Incluye el logo (`img/Logo.png`, también favicon), la foto de portada (`img/caradepan.jpg`) y el mapa de Google con la ubicación de la panadería.

## Boleta de la cajera
Al imprimir solo se manda el recibo (`@media print` en `estilos.css`): una sola hoja, sin páginas vacías.

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

## Usuarios de prueba
admin@luzmayra.com / Admin123 · cajero@luzmayra.com / Cajero123 · cliente@luzmayra.com / Cliente123 ·
almacen@luzmayra.com / Almacen123 · despacho@luzmayra.com / Despacho123 · entrega@luzmayra.com / Entrega123

## Front: dónde está cada cosa (sin código repetido)
- `js/ui.js`: `h()` para crear elementos, cabecera, barra lateral, tablas, tarjetas, ventanas y formularios.
  Cada HTML solo trae su contenido propio; la cabecera y la barra lateral se inyectan según los `data-*` del `<body>`.
- `js/api.js`: único lugar con `fetch`. `js/catalogo.js`: listado de productos. `js/carrito.js`: panel "Tu Pedido".
- `js/paginas/*.js`: un script por pantalla (tienda, admin-productos, usuarios, pedidos, dashboard, inicio, maqueta).

/* ===== Configuración compartida del front ===== */

// Menú de la cabecera según la pantalla (nombre, enlace)
const MENUS = {
    cliente: [["Inicio", "index.html"], ["Ver Historial", "historial.html"]],
    historial: [["Inicio", "index.html"], ["Ver Catálogo Completo", "catalogo.html"]],
    cajera: [["Inicio", "index.html"], ["Ver Historial", "historial.html"]],
    almacen: [["Productos Base", "almacen-productos.html"], ["Registro del día", "almacen-registro.html"],
        ["Solicitud Proveedores", "almacen-solicitudes.html"], ["Proveedores", "almacen-proveedores.html"],
        ["Asignación-Delivery", "almacen-asignacion.html"]],
    admin: [["Crear Usuarios", "admin-usuarios.html"], ["Productos", "admin-productos.html"],
        ["Pedidos Personalizados", "admin-pedidos.html"], ["Dashboard", "admin-dashboard.html"]],
    ninguno: []
};

// Color de cada estado de pedido (los no listados salen en naranja)
const ESTADO_COLOR = {
    Entregado: "verde", Despachado: "verde", Recibido: "verde", Asignado: "verde", Cancelado: "rojo"
};

// Descripción de cada rol en las tarjetas de "Crear Nuevo Usuario"
const DESCRIPCION_ROL = {
    Administrador: "Puede crear nuevos productos, ver el dashboard y crear nuevos usuarios",
    Cajero: "Registra los pedidos de manera presencial",
    Almacenero: "Gestiona el inventario, los proveedores y las solicitudes de abastecimiento",
    Despachador: "Despacha los pedidos virtuales",
    Entregador: "Entrega los pedidos delivery y personalizados"
};

// TEMPORAL: mientras no exista el inicio de sesión, el historial muestra los pedidos de este cliente.
// Cuando se active la autenticación se reemplaza por el usuario de la sesión.
const CLIENTE_DEMO_ID = 3;

/* ===== Configuración compartida del front ===== */

// Menú de la cabecera según la pantalla (nombre, enlace)
const MENUS = {
    cliente: [["Inicio", "index.html"], ["Ver Historial", "historial.html"]],
    historial: [["Inicio", "index.html"], ["Ver Catálogo Completo", "catalogo.html"]],
    cajera: [["Inicio", "index.html"], ["Ver Historial", "historial.html"]],
    almacen: [["Productos Base", "almacen-productos.html"], ["Stock de productos", "almacen-stock.html"], ["Registro del día", "almacen-registro.html"],
        ["Solicitud Proveedores", "almacen-solicitudes.html"], ["Proveedores", "almacen-proveedores.html"],
        ["Asignación-Delivery", "almacen-asignacion.html"]],
    admin: [["Crear Usuarios", "admin-usuarios.html"], ["Productos", "admin-productos.html"],
        ["Pedidos Personalizados", "admin-pedidos.html"], ["Dashboard", "admin-dashboard.html"], ["Almacén", "almacen-productos.html"]],
    ninguno: []
};

// Color de cada estado de pedido (los no listados salen en naranja)
const ESTADO_COLOR = {
    Entregado: "verde", Despachado: "verde", Recibido: "verde", Asignado: "verde", Disponible: "verde",
    Cancelado: "rojo", Agotado: "rojo"
};

// Menú lateral del administrador (también aparece al entrar a las pantallas del almacén)
const ENLACES_ADMIN = [["Dashboard", "admin-dashboard.html"], ["Productos", "admin-productos.html"],
    ["Pedidos Personalizados", "admin-pedidos.html"], ["Crear Usuarios", "admin-usuarios.html"], ["Almacén", "almacen-productos.html"]];

// Descripción de cada rol en las tarjetas de "Crear Nuevo Usuario"
const DESCRIPCION_ROL = {
    Administrador: "Puede crear nuevos productos, ver el dashboard y crear nuevos usuarios",
    Cajero: "Registra los pedidos de manera presencial",
    Almacenero: "Gestiona el inventario, los proveedores y las solicitudes de abastecimiento",
    Despachador: "Despacha los pedidos virtuales",
    Entregador: "Entrega los pedidos delivery y personalizados"
};

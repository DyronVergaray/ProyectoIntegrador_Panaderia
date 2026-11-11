/* ===== Historial del cliente y pedidos personalizados del admin (misma tabla y mismo detalle) ===== */
(async function () {
    await UI.listo;
    const modo = document.body.dataset.modo;            // "historial" o "admin"
    const esCliente = UI.usuario.rol === "Cliente";     // el cliente ve solo lo suyo; cajera y admin ven todos
    const contenedor = document.getElementById("tabla-pedidos");
    let pedidos = [];
    let calificados = new Map();      // N.º de pedido -> calificación que el cliente ya dio (MongoDB)

    async function cargar() {
        try {
            const ruta = modo === "admin" ? "/api/pedidos?tipo=Personalizado" : (esCliente ? "/api/pedidos/mios" : "/api/pedidos");
            pedidos = await api(ruta);
            if (esCliente && modo === "historial") {
                try {
                    calificados = new Map((await api("/api/calificaciones/mias")).map((c) => [c.idPedido, c]));
                } catch (e) {
                    console.log("No se pudieron leer las calificaciones: " + e.message);      // sin MongoDB el historial sigue funcionando
                }
            }
            const pendientes = pedidos.filter((p) => p.estado === "Pendiente").length;
            const entregados = pedidos.filter((p) => p.estado === "Entregado").length;
            UI.actualizarResumen(esCliente
                ? [pendientes + " solicitudes pendientes", entregados + " solicitudes recibidas"]
                : [pendientes + " pedidos por recibir", entregados + " pedidos entregados"]);
            dibujar();
        } catch (e) {
            UI.estado(contenedor, e.message);
        }
    }

    // Solo los pedidos Entregados se pueden calificar, y una sola vez
    function calificacion(p) {
        if (p.estado !== "Entregado") {
            return h("span", { class: "sin-calificar", title: "Podrás calificar cuando recibas tu pedido", texto: "—" });
        }
        if (calificados.has(p.id)) {
            const c = calificados.get(p.id);
            const promedio = (c.compra + c.entrega + c.atencion + c.calidad) / 4;
            return h("span", { class: "calificado", title: "Tu calificación: " + promedio.toFixed(1) + " de 5" },
                h("span", { class: "estrella activa", texto: "★" }), h("strong", { texto: promedio.toFixed(1) }), h("small", { texto: "Calificado" }));
        }
        return h("button", { class: "pastilla-chica pc-calificar", onclick: () => Calificar.abrir(p.id, cargar) }, "★ Calificar");
    }

    function dibujar() {
        const texto = UI.filtro.q.toLowerCase();
        const vistos = pedidos.filter((p) => !texto || [p.id, p.cliente, p.tipo, p.estado].join(" ").toLowerCase().includes(texto));
        if (!vistos.length) {
            UI.estado(contenedor, "No hay pedidos para mostrar.");
            return;
        }
        const info = (p) => UI.botonInfo(() => verDetalle(p.id));
        const cabeceras = esCliente
            ? ["ID", "Tipo", "Fecha de solicitud", "Fecha estimada de recepción", "Estado", "Info", "Calificación"]
            : ["ID", "Cliente", "Tipo", "Fecha de solicitud", "Fecha estimada de recepción", "Estado", "Info"];
        const filas = vistos.map((p) => esCliente
            ? [String(p.id), p.tipo, UI.fecha(p.fechaSolicitud), UI.fecha(p.fechaEstimada), UI.chip(p.estado), info(p), calificacion(p)]
            : [String(p.id), p.cliente, p.tipo, UI.fecha(p.fechaSolicitud), UI.fecha(p.fechaEstimada), UI.chip(p.estado), info(p)]);
        contenedor.replaceChildren(UI.tabla(cabeceras, filas, vistos.length + (esCliente ? " solicitudes" : " pedidos")));
    }

    // El detalle (productos, promoción aplicada y características del pedido personalizado) es el mismo en todo el sistema
    const verDetalle = (id) => UI.verPedido(id);

    UI.alFiltrar = dibujar;
    cargar();
})();

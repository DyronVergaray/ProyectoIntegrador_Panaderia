/* ===== Historial del cliente y pedidos personalizados del admin (misma tabla y mismo detalle) ===== */
(async function () {
    await UI.listo;
    const modo = document.body.dataset.modo;            // "historial" o "admin"
    const contenedor = document.getElementById("tabla-pedidos");
    const detalle = UI.modal({ id: "modal-detalle", titulo: "Detalle del Pedido", contenido: [] });
    let pedidos = [];

    async function cargar() {
        try {
            const filtro = modo === "historial" ? "?clienteId=" + CLIENTE_DEMO_ID : "?tipo=Personalizado";
            pedidos = await api("/api/pedidos" + filtro);
            const pendientes = pedidos.filter((p) => p.estado === "Pendiente").length;
            const entregados = pedidos.filter((p) => p.estado === "Entregado").length;
            UI.actualizarResumen(modo === "historial"
                ? [pendientes + " solicitudes pendientes", entregados + " solicitudes recibidas"]
                : [pendientes + " pedidos por recibir", entregados + " pedidos entregados"]);
            dibujar();
        } catch (e) {
            UI.estado(contenedor, e.message);
        }
    }

    function dibujar() {
        const texto = UI.filtro.q.toLowerCase();
        const vistos = pedidos.filter((p) => !texto || [p.id, p.cliente, p.tipo, p.estado].join(" ").toLowerCase().includes(texto));
        if (!vistos.length) {
            UI.estado(contenedor, "No hay pedidos para mostrar.");
            return;
        }
        const segunda = modo === "historial" ? "Tipo" : "Cliente";
        contenedor.replaceChildren(UI.tabla(
            ["ID", segunda, "Fecha de solicitud", "Fecha estimada de recepción", "Estado", "Info"],
            vistos.map((p) => [String(p.id), modo === "historial" ? p.tipo : p.cliente, UI.fecha(p.fechaSolicitud),
                UI.fecha(p.fechaEstimada), UI.chip(p.estado), UI.botonInfo(() => verDetalle(p.id))]),
            vistos.length + (modo === "historial" ? " solicitudes" : " pedidos")));
    }

    function dato(etiqueta, valor) {
        return h("p", {}, h("span", { texto: etiqueta }), valor);
    }

    async function verDetalle(id) {
        try {
            const d = await api("/api/pedidos/" + id);
            detalle.ponerTitulo("Detalle del Pedido #" + d.id);
            detalle.poner([
                h("div", { class: "datos-detalle" },
                    h("div", {}, h("span", { class: "etiqueta", texto: "DATOS DEL CLIENTE" }),
                        dato("Nombre:", d.cliente), dato("Teléfono:", d.telefono || "—"), dato("Email:", d.email),
                        d.direccion ? dato("Dirección:", d.direccion) : null,
                        dato("Tipo de Pedido:", h("span", { class: "chip chip-sencillo", texto: d.tipo }))),
                    h("div", { class: "caja-codigo" }, h("small", { texto: "CÓDIGO DE RETIRO" }),
                        h("strong", { texto: d.codigo || "—" }), h("em", { texto: "Presentar código en caja al retirar" }))),
                h("span", { class: "etiqueta", texto: "PRODUCTOS DEL PEDIDO" }),
                UI.tabla(["Producto", ">Cant.", ">Precio Unit.", ">Subtotal"],
                    d.items.map((i) => [i.producto, "× " + i.cantidad, UI.soles(i.precio), UI.soles(i.subtotal)])),
                h("div", { class: "totales" }, h("p", {}, "Total: ", h("span", { class: "grande", texto: UI.soles(d.total) }))),
                h("div", { class: "modal-pie" }, h("span", { texto: "Fecha de solicitud: " + UI.fecha(d.fechaSolicitud) }),
                    h("span", {}, "Estado: ", UI.chip(d.estado)))
            ]);
            detalle.abrir();
        } catch (e) {
            UI.toast(e.message, false);
        }
    }

    UI.alFiltrar = dibujar;
    cargar();
})();

/* ===== Historial del cliente y pedidos personalizados del admin (misma tabla y mismo detalle) ===== */
(async function () {
    await UI.listo;
    const modo = document.body.dataset.modo;            // "historial" o "admin"
    const esCliente = UI.usuario.rol === "Cliente";     // el cliente ve solo lo suyo; cajera y admin ven todos
    const contenedor = document.getElementById("tabla-pedidos");
    const detalle = UI.modal({ id: "modal-detalle", titulo: "Detalle del Pedido", contenido: [] });
    let pedidos = [];

    async function cargar() {
        try {
            const ruta = modo === "admin" ? "/api/pedidos?tipo=Personalizado" : (esCliente ? "/api/pedidos/mios" : "/api/pedidos");
            pedidos = await api(ruta);
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

    function dibujar() {
        const texto = UI.filtro.q.toLowerCase();
        const vistos = pedidos.filter((p) => !texto || [p.id, p.cliente, p.tipo, p.estado].join(" ").toLowerCase().includes(texto));
        if (!vistos.length) {
            UI.estado(contenedor, "No hay pedidos para mostrar.");
            return;
        }
        const info = (p) => UI.botonInfo(() => verDetalle(p.id));
        const cabeceras = esCliente
            ? ["ID", "Tipo", "Fecha de solicitud", "Fecha estimada de recepción", "Estado", "Info"]
            : ["ID", "Cliente", "Tipo", "Fecha de solicitud", "Fecha estimada de recepción", "Estado", "Info"];
        const filas = vistos.map((p) => esCliente
            ? [String(p.id), p.tipo, UI.fecha(p.fechaSolicitud), UI.fecha(p.fechaEstimada), UI.chip(p.estado), info(p)]
            : [String(p.id), p.cliente, p.tipo, UI.fecha(p.fechaSolicitud), UI.fecha(p.fechaEstimada), UI.chip(p.estado), info(p)]);
        contenedor.replaceChildren(UI.tabla(cabeceras, filas, vistos.length + (esCliente ? " solicitudes" : " pedidos")));
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

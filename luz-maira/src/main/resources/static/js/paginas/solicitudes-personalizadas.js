/* ===== Admin: solicitudes de pedidos personalizados (vienen de MongoDB) =====
   El administrador revisa el detalle, acepta con un precio (se crea el pedido en SQL Server) o rechaza con un motivo. */
(async function () {
    await UI.listo;
    const tabla = document.getElementById("tabla-solicitudes");
    const pestanas = document.getElementById("pestanas");
    const ESTADOS = ["Solicitada", "Aceptada", "Rechazada", "Cancelada", "Todas"];
    let estadoActivo = "Solicitada";
    let todas = [];
    let solicitudes = [];          // las del estado elegido
    let actual = null;          // solicitud que se está aceptando o rechazando

    const detalle = UI.modal({ id: "modal-solicitud", etiqueta: "PEDIDO PERSONALIZADO", titulo: "Solicitud personalizada", ancho: "doble", contenido: [], botones: [{ texto: "Cerrar", clase: "btn-claro", onclick: (cerrar) => cerrar() }] });

    const formAceptar = UI.modalForm({
        id: "modal-aceptar", titulo: "Aceptar solicitud", boton: "Aceptar y crear pedido",
        campos: [
            { id: "precio", etiqueta: "Precio final (S/)", tipo: "number", requerido: true, min: 1, step: "0.10" },
            { id: "nota", etiqueta: "Nota para el cliente (opcional)", maxlength: 300, placeholder: "Ej. Incluye velas y caja" }
        ],
        alGuardar: async (d) => {
            const precio = Number(d.precio);
            if (!(precio >= 1)) {
                throw new Error("El precio mínimo es S/ 1.00");
            }
            const r = await api("/api/personalizados/" + actual.id + "/aceptar", "PUT", { precio: precio, nota: d.nota });
            UI.toast(r.mensaje, true);
            await cargar();
        }
    });

    const formRechazar = UI.modalForm({
        id: "modal-rechazar", titulo: "Rechazar solicitud", boton: "Rechazar",
        campos: [{ id: "nota", etiqueta: "Motivo (lo verá el cliente)", requerido: true, maxlength: 300, placeholder: "Ej. No alcanzamos a prepararlo en esa fecha" }],
        alGuardar: async (d) => {
            const r = await api("/api/personalizados/" + actual.id + "/rechazar", "PUT", { nota: d.nota });
            UI.toast(r.mensaje, true);
            await cargar();
        }
    });

    // Una "ficha" = etiqueta pequeña + valor; si el cliente no llenó el dato se muestra "Sin especificar"
    function ficha(etiqueta, valor, ancha) {
        return h("div", { class: "ficha" + (ancha ? " ancha" : "") }, h("span", { class: "etiqueta", texto: etiqueta }),
            valor ? h("p", { texto: valor }) : h("p", { class: "sin-dato", texto: "Sin especificar" }));
    }

    function ver(s) {
        const entrega = UI.fecha(s.fechaEntrega) + " a las " + s.horaEntrega;
        detalle.ponerTitulo("Solicitud de " + s.cliente);
        detalle.poner([
            h("div", { class: "ficha-cabecera" },
                h("span", { texto: s.email || "" }), UI.chip(s.estado)),
            h("div", { class: "fichas" },
                ficha("Producto", s.tipoProducto), ficha("Porciones", String(s.porciones)),
                ficha("Sabor", s.sabor), ficha("Entrega", entrega),
                ficha("Modalidad", s.modalidad === "Delivery" ? "Delivery" : "Recojo en tienda"),
                ficha("Dirección", s.modalidad === "Delivery" ? s.direccion : "No aplica"),
                ficha("Descripción", s.descripcion, true), ficha("Decoración", s.decoracion, true),
                ficha("Dedicatoria", s.dedicatoria), ficha("Alergias o restricciones", s.alergias)),
            s.estado === "Solicitada" ? null : h("div", { class: "ficha-respuesta" },
                s.precio ? h("p", {}, h("strong", { texto: "Precio final: " + UI.soles(s.precio) })) : null,
                s.idPedido ? h("p", { texto: "Pedido N.º " + s.idPedido }) : null,
                s.nota ? h("p", { texto: (s.estado === "Rechazada" ? "Motivo: " : "Nota: ") + s.nota }) : null),
            h("div", { class: "modal-pie" }, h("span", { texto: "Solicitada el " + UI.fecha(s.fechaCreacion.substring(0, 10)) }))
        ]);
        detalle.abrir();
    }

    function acciones(s) {
        const ojo = h("button", { class: "pastilla-chica pc-claro", onclick: () => ver(s) }, "👁 Ver");
        if (s.estado !== "Solicitada") {
            return h("div", { class: "acciones" }, ojo);
        }
        return h("div", { class: "acciones" }, ojo,
            h("button", { class: "pastilla-chica pc-ok", onclick: () => { actual = s; formAceptar.abrir({}, "Aceptar · " + s.cliente); } }, "✔ Aceptar"),
            h("button", { class: "pastilla-chica pc-no", onclick: () => { actual = s; formRechazar.abrir({}, "Rechazar · " + s.cliente); } }, "✖ Rechazar"));
    }

    // Pestañas con el número de solicitudes de cada estado
    function dibujarPestanas() {
        pestanas.replaceChildren(...ESTADOS.map((e) => {
            const n = e === "Todas" ? todas.length : todas.filter((s) => s.estado === e).length;
            return h("button", { class: "pestana" + (e === estadoActivo ? " activa" : ""), role: "tab", "aria-selected": String(e === estadoActivo),
                onclick: () => { estadoActivo = e; dibujar(); } }, e === "Solicitada" ? "Por revisar" : e, h("span", { class: "contador", texto: String(n) }));
        }));
    }

    function dibujar() {
        dibujarPestanas();
        solicitudes = estadoActivo === "Todas" ? todas : todas.filter((s) => s.estado === estadoActivo);
        if (!solicitudes.length) {
            UI.estado(tabla, "No hay solicitudes en este estado.");
            return;
        }
        tabla.replaceChildren(UI.tabla(["Cliente", "Producto", ">Porciones", "Entrega", "Estado", ">Precio", ""],
            solicitudes.map((s) => [s.cliente, s.tipoProducto, String(s.porciones), UI.fecha(s.fechaEntrega) + " " + s.horaEntrega,
                UI.chip(s.estado), s.precio ? UI.soles(s.precio) : "—", acciones(s)]),
            solicitudes.length + (solicitudes.length === 1 ? " solicitud" : " solicitudes")));
    }

    async function cargar() {
        try {
            todas = await api("/api/personalizados");
            dibujar();
        } catch (e) {
            UI.estado(tabla, e.message);
        }
    }

    cargar();
})();

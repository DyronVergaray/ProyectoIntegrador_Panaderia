/* ===== Cliente: pedido personalizado (formulario -> solicitud en MongoDB) y seguimiento de sus solicitudes =====
   Flujo: el cliente envía la solicitud; la panadería la acepta con un precio (se crea el pedido) o la rechaza. */
(async function () {
    await UI.listo;
    const ANTICIPACION_DIAS = 2;        // mismo valor que PersonalizadoService en el servidor
    const EMOJIS = { "Torta": "🎂", "Cupcakes": "🧁", "Bocaditos / salados": "🥟", "Pan (pedido grande)": "🥖", "Otro": "✨" };
    const lista = document.getElementById("tabla-solicitudes");
    const mensaje = document.getElementById("p-mensaje");
    const campo = (id) => document.getElementById(id);
    const elegido = (nombre) => { const r = document.querySelector('input[name="' + nombre + '"]:checked'); return r ? r.value : ""; };

    campo("p-fecha").min = UI.hoy(ANTICIPACION_DIAS);

    // La dirección solo se pide si elige Delivery
    document.querySelectorAll('input[name="p-modalidad"]').forEach((r) => {
        r.onchange = () => { campo("campo-direccion").hidden = elegido("p-modalidad") !== "Delivery"; };
    });

    campo("form-personalizado").onsubmit = async (evento) => {
        evento.preventDefault();
        const cuerpo = {
            tipoProducto: elegido("p-tipo"), descripcion: campo("p-descripcion").value.trim(),
            sabor: campo("p-sabor").value.trim(), porciones: parseInt(campo("p-porciones").value, 10),
            decoracion: campo("p-decoracion").value.trim(), dedicatoria: campo("p-dedicatoria").value.trim(),
            alergias: campo("p-alergias").value.trim(), modalidad: elegido("p-modalidad"),
            direccion: campo("p-direccion").value.trim(), fechaEntrega: campo("p-fecha").value, horaEntrega: campo("p-hora").value
        };
        const boton = campo("p-enviar");
        try {
            if (cuerpo.tipoProducto === "") { throw new Error("Elige el tipo de producto"); }
            if (cuerpo.descripcion === "") { throw new Error("Describe lo que necesitas"); }
            if (!(cuerpo.porciones >= 1)) { throw new Error("Ingresa las porciones o unidades"); }
            if (cuerpo.fechaEntrega === "" || cuerpo.horaEntrega === "") { throw new Error("Elige la fecha y la hora de entrega"); }
            if (cuerpo.fechaEntrega < UI.hoy(ANTICIPACION_DIAS)) { throw new Error("Pide con al menos " + ANTICIPACION_DIAS + " días de anticipación"); }
            if (cuerpo.modalidad === "Delivery" && cuerpo.direccion === "") { throw new Error("Ingresa la dirección para el delivery"); }
            boton.disabled = true;
            await api("/api/personalizados", "POST", cuerpo);
            mensaje.innerText = "";
            campo("form-personalizado").reset();
            campo("campo-direccion").hidden = true;
            UI.alerta("success", "Solicitud enviada", "La panadería la revisará y te responderá con el precio. Mira el estado a la derecha.");
            await cargar();
        } catch (e) {
            mensaje.innerText = e.message;
            mensaje.className = "mensaje";
        } finally {
            boton.disabled = false;
        }
    };

    async function cancelar(s) {
        if (!(await UI.confirmar("¿Cancelar esta solicitud?", "La panadería ya no la revisará.", "Sí, cancelar"))) {
            return;
        }
        try {
            const r = await api("/api/personalizados/" + s.id + "/cancelar", "PUT");
            UI.toast(r.mensaje, true);
            await cargar();
        } catch (e) {
            UI.toast(e.message, false);
        }
    }

    // Recuadro con la respuesta de la panadería según el estado
    function respuesta(s) {
        if (s.estado === "Aceptada") {
            return h("div", { class: "sol-respuesta ok" }, h("strong", { texto: "Precio final: " + UI.soles(s.precio) }),
                h("span", { texto: "Pedido N.º " + s.idPedido + " — míralo en tu historial." + (s.nota ? " " + s.nota : "") }));
        }
        if (s.estado === "Rechazada") {
            return h("div", { class: "sol-respuesta no" }, h("strong", { texto: "No pudimos aceptarla" }), h("span", { texto: s.nota || "" }));
        }
        if (s.estado === "Solicitada") {
            return h("div", { class: "sol-respuesta" }, h("span", { texto: "En revisión: te responderemos pronto." }));
        }
        return null;
    }

    function tarjeta(s) {
        return h("article", { class: "sol-tarjeta" },
            h("div", { class: "sol-cabecera" }, h("strong", { texto: (EMOJIS[s.tipoProducto] || "✨") + " " + s.tipoProducto }), UI.chip(s.estado)),
            h("p", { class: "sol-datos", texto: s.porciones + " porciones · " + UI.fecha(s.fechaEntrega) + " " + s.horaEntrega + " · " + (s.modalidad === "Delivery" ? "Delivery" : "Recojo") }),
            respuesta(s),
            s.estado === "Solicitada" ? h("button", { class: "pastilla-chica pc-no", onclick: () => cancelar(s) }, "✖ Cancelar solicitud") : null);
    }

    async function cargar() {
        try {
            const solicitudes = await api("/api/personalizados/mias");
            if (!solicitudes.length) {
                UI.estado(lista, "Aún no enviaste solicitudes.");
                return;
            }
            lista.replaceChildren(...solicitudes.map(tarjeta));
        } catch (e) {
            UI.estado(lista, e.message);
        }
    }

    cargar();
})();

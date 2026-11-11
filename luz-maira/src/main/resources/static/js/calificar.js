/* ===== Calificar un pedido entregado (solo cliente). Se guarda en MongoDB (colección "calificaciones") =====
   Responsabilidad: ventana con estrellas (1 a 5) para compra, entrega, atención y calidad, "¿nos recomendarías?" y comentario.
   Uso: Calificar.abrir(pedidoId, alGuardar) */

const Calificar = {
    ASPECTOS: [["compra", "Experiencia de compra"], ["entrega", "Entrega"], ["atencion", "Atención"], ["calidad", "Calidad de los productos"]],

    TEXTOS: ["Elige una calificación", "Muy malo", "Malo", "Regular", "Bueno", "Excelente"],

    // Fila de 5 estrellas con un texto que dice qué significa la elegida (valor 0 = sin elegir)
    estrellas(nombre) {
        let valor = 0;
        const texto = h("span", { class: "estrellas-texto", texto: this.TEXTOS[0] });
        const botones = [1, 2, 3, 4, 5].map((n) => h("button", { type: "button", class: "estrella", "aria-label": n + " de 5 estrellas para " + nombre,
            onclick: () => { valor = n; pintar(valor); },
            onmouseenter: () => pintar(n), onmouseleave: () => pintar(valor) }, "★"));
        const pintar = (hasta) => {
            botones.forEach((b, i) => b.classList.toggle("activa", i < hasta));
            texto.innerText = this.TEXTOS[valor];
            texto.classList.toggle("elegido", valor > 0);
        };
        return { el: h("div", { class: "estrellas-grupo" }, h("div", { class: "estrellas", role: "group", "aria-label": nombre }, botones), texto), valor: () => valor };
    },

    abrir(idPedido, alGuardar) {
        const filas = this.ASPECTOS.map(([id, texto]) => ({ id: id, texto: texto, control: this.estrellas(texto) }));
        let recomienda = null;       // true / false / null (sin elegir)
        const botonSi = h("button", { type: "button", class: "opcion-reco", onclick: () => elegir(true) }, "👍 Sí");
        const botonNo = h("button", { type: "button", class: "opcion-reco", onclick: () => elegir(false) }, "👎 No");
        const elegir = (valor) => {
            recomienda = valor;
            botonSi.classList.toggle("activa", valor === true);
            botonNo.classList.toggle("activa", valor === false);
        };
        const comentario = h("textarea", { maxlength: "500", rows: "3", placeholder: "Cuéntanos más (opcional)", "aria-label": "Comentario" });
        const mensaje = h("div", { class: "mensaje", role: "alert" });

        const ventana = UI.modal({
            id: "modal-calificar", etiqueta: "PEDIDO N.º " + idPedido, titulo: "Califica tu experiencia", ancho: "doble",
            contenido: [
                ...filas.map((f) => h("div", { class: "fila-estrellas" }, h("span", { texto: f.texto }), f.control.el)),
                h("div", { class: "fila-estrellas reco" }, h("span", { texto: "¿Nos recomendarías?" }), h("div", { class: "recomienda" }, botonSi, botonNo)),
                h("label", { class: "campo" }, h("span", { texto: "Comentarios" }), comentario), mensaje],
            botones: [{ texto: "Cancelar", clase: "btn-claro", onclick: (cerrar) => { cerrar(); ventana.el.remove(); } },
                { texto: "Enviar calificación", onclick: async (cerrar) => {
                    try {
                        const cuerpo = { idPedido: idPedido, comentario: comentario.value.trim() };
                        for (const f of filas) {
                            cuerpo[f.id] = f.control.valor();
                            if (cuerpo[f.id] === 0) {
                                throw new Error("Pon estrellas en: " + f.texto);
                            }
                        }
                        if (recomienda === null) {
                            throw new Error("Indica si nos recomendarías");
                        }
                        cuerpo.recomienda = recomienda;
                        await api("/api/calificaciones", "POST", cuerpo);
                        cerrar();
                        ventana.el.remove();
                        UI.toast("¡Gracias por tu calificación!", true);
                        if (alGuardar) { await alGuardar(); }
                    } catch (e) {
                        mensaje.innerText = e.message;
                        mensaje.className = "mensaje";
                    }
                } }]
        });
        ventana.abrir();
    }
};

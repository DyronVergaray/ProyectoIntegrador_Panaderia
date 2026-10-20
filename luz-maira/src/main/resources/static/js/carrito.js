/* ===== Panel "Tu Pedido" (compartido por cliente y cajera). El pedido vive en el navegador hasta confirmarlo. ===== */

const Carrito = {
    modo: "cliente",
    items: [],

    montar(modo) {
        this.modo = modo;
        this.lineas = h("div", { id: "lineas-pedido" });
        this.total = h("span", { class: "grande", texto: UI.soles(0) });
        const filaTotal = h("div", { class: "fila-total" }, h("strong", { texto: "Total:" }), this.total);
        const confirmar = h("button", { class: "btn", onclick: () => this.confirmar() }, "Confirmar Compra");
        const panel = document.getElementById("panel");

        if (modo === "cajera") {
            this.dinero = h("input", { id: "dinero", class: "campo-simple", type: "number", min: "0", step: "0.10", placeholder: "Dinero recibido (S/)", "aria-label": "Dinero recibido", style: "letter-spacing:0" });
            this.vuelto = h("span", { class: "precio", texto: UI.soles(0) });
            this.dinero.oninput = () => this.actualizarVuelto();
            panel.append(h("h2", { texto: "Tu Pedido" }), this.lineas, filaTotal,
                h("label", { class: "etiqueta", for: "dinero", style: "margin-top:.8rem" }, "DINERO (RECIBIDO)"), this.dinero,
                h("div", { class: "vuelto" }, h("span", { texto: "Vuelto:" }), this.vuelto), confirmar);
        } else {
            this.codigo = h("input", { class: "campo-simple", id: "codigo-secreto", type: "text", maxlength: "3", placeholder: "3 letras", "aria-label": "Código secreto" });
            this.tipos = h("div", { id: "tipos-entrega" });
            panel.append(h("h2", { texto: "Tu Pedido" }), this.lineas, filaTotal, confirmar,
                h("label", { class: "etiqueta", for: "codigo-secreto", style: "text-transform:none;font-size:.72rem" }, "Ingresa tu código secreto"),
                this.codigo, this.tipos);
            this.cargarTipos();
        }
        this.dibujar();
    },

    // Tipos de entrega desde la base de datos (Virtual, Delivery, Personalizado)
    async cargarTipos() {
        const nombres = { Virtual: "Virtual (Recojo en tienda)" };
        try {
            const tipos = await api("/api/tipos-pedido");
            this.tipos.replaceChildren(...tipos.map((t, i) => h("label", { class: "check" },
                h("input", { type: "radio", name: "entrega", value: t.id, checked: i === 0 }),
                h("span", { texto: nombres[t.nombre] || t.nombre }))));
        } catch (e) {
            UI.estado(this.tipos, e.message);
        }
    },

    agregar(producto) {
        const existente = this.items.find((i) => i.id === producto.id);
        if (existente) {
            existente.cantidad++;
        } else {
            this.items.push({ id: producto.id, nombre: producto.nombre, precio: Number(producto.precio), cantidad: 1 });
        }
        this.dibujar();
    },

    cambiar(id, diferencia) {
        const item = this.items.find((i) => i.id === id);
        item.cantidad += diferencia;
        this.items = this.items.filter((i) => i.cantidad > 0);
        this.dibujar();
    },

    suma() {
        return Math.round(this.items.reduce((s, i) => s + i.precio * i.cantidad, 0) * 100) / 100;
    },

    dibujar() {
        if (!this.items.length) {
            this.lineas.replaceChildren(h("p", { class: "sin-items", texto: "Aún no agregaste productos. Usa el botón + de cada producto." }));
        } else {
            this.lineas.replaceChildren(...this.items.map((i) => h("div", { class: "linea-pedido" },
                h("span", { class: "cantidad-control" },
                    h("button", { class: "mini", "aria-label": "Quitar uno", onclick: () => this.cambiar(i.id, -1) }, "−"),
                    h("span", { class: "cantidad", texto: i.cantidad + "x" }),
                    h("button", { class: "mini", "aria-label": "Agregar uno", onclick: () => this.cambiar(i.id, 1) }, "+")),
                h("div", {}, h("strong", { texto: i.nombre }), h("small", { texto: i.cantidad + " × " + UI.soles(i.precio) })),
                h("span", { class: "monto", texto: UI.soles(i.precio * i.cantidad) }))));
        }
        this.total.innerText = UI.soles(this.suma());
        if (this.dinero) {
            this.actualizarVuelto();
        }
    },

    actualizarVuelto() {
        const recibido = parseFloat(this.dinero.value);
        const vuelto = isNaN(recibido) ? 0 : Math.max(0, Math.round((recibido - this.suma()) * 100) / 100);
        this.vuelto.innerText = UI.soles(vuelto);
    },

    confirmar() {
        try {
            if (!this.items.length) {
                throw new Error("Agrega al menos un producto al pedido");
            }
            if (this.modo === "cajera") {
                const recibido = parseFloat(this.dinero.value);
                if (isNaN(recibido)) {
                    throw new Error("Ingresa el dinero recibido");
                }
                if (recibido < this.suma()) {
                    throw new Error("El dinero recibido es menor al total");
                }
                this.mostrarRecibo(recibido);
            } else {
                if (!/^[A-Za-z]{3}$/.test(this.codigo.value.trim())) {
                    throw new Error("El código secreto debe tener exactamente 3 letras");
                }
                UI.toast("Pedido listo. Se guardará en la base de datos cuando activemos el inicio de sesión.");
            }
        } catch (e) {
            UI.toast(e.message, false);
        }
    },

    mostrarRecibo(recibido) {
        const total = this.suma();
        const ahora = new Date();
        const fecha = ahora.getDate() + " " + MESES[ahora.getMonth()] + " " + ahora.getFullYear() + ", " + UI.hora();
        const recibo = UI.modal({
            id: "modal-recibo", ancho: "angosto",
            contenido: h("div", { class: "recibo" },
                h("span", { class: "logo", texto: "LUZ MAYRA" }), h("h2", { texto: "Panaderia Luz Mayra" }),
                h("small", { texto: "PANADERÍA & PASTELERÍA" }),
                h("div", { class: "recibo-titulo" }, h("strong", { texto: "RECIBO DE COMPRA" }), "Ticket #" + String(Date.now()).slice(-5) + " · " + fecha),
                this.items.map((i) => h("div", { class: "linea-pedido" }, h("span", { class: "cantidad", texto: i.cantidad + "x" }),
                    h("div", {}, h("strong", { texto: i.nombre }), h("small", { texto: i.cantidad + " × " + UI.soles(i.precio) })),
                    h("span", { class: "monto", texto: UI.soles(i.precio * i.cantidad) }))),
                h("div", { class: "fila-total" }, h("span", { texto: "Dinero Recibido:" }), h("strong", { texto: UI.soles(recibido) })),
                h("div", { class: "fila-total", style: "border:0;padding-top:.3rem" }, h("strong", { texto: "Total:" }), h("span", { class: "grande", texto: UI.soles(total) })),
                h("div", { class: "fila-total", style: "border:0;padding-top:.3rem" }, h("span", { texto: "Vuelto:" }),
                    h("strong", { class: "precio", texto: UI.soles(Math.round((recibido - total) * 100) / 100) }))),
            botones: [{ texto: "Imprimir", onclick: () => window.print() },
                { texto: "Cerrar", clase: "btn-claro", onclick: (cerrar) => { cerrar(); recibo.el.remove(); this.items = []; this.dinero.value = ""; this.dibujar(); } }]
        });
        recibo.abrir();
    }
};

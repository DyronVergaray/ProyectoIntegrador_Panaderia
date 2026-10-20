/* ===== Panel "Tu Pedido" (compartido por cliente y cajera). El pedido vive en el navegador hasta confirmarlo. ===== */

const Carrito = {
    modo: "cliente",
    items: [],
    tipos: [],

    montar(modo) {
        this.modo = modo;
        this.lineas = h("div", { id: "lineas-pedido" });
        this.total = h("span", { class: "grande", texto: UI.soles(0) });
        const filaTotal = h("div", { class: "fila-total" }, h("strong", { texto: "Total:" }), this.total);
        const confirmar = h("button", { class: "btn", id: "btn-confirmar", onclick: () => this.confirmar() }, "Confirmar Compra");
        const panel = document.getElementById("panel");

        if (modo === "cajera") {
            this.dinero = h("input", { id: "dinero", class: "campo-simple", type: "number", min: "0", step: "0.10", placeholder: "Dinero recibido (S/)", "aria-label": "Dinero recibido", style: "letter-spacing:0" });
            this.vuelto = h("span", { class: "precio", texto: UI.soles(0) });
            this.dinero.oninput = () => this.actualizarVuelto();
            panel.append(h("h2", { texto: "Tu Pedido" }), this.lineas, filaTotal,
                h("label", { class: "etiqueta", for: "dinero", style: "margin-top:.8rem" }, "DINERO (RECIBIDO)"), this.dinero,
                h("div", { class: "vuelto" }, h("span", { texto: "Vuelto:" }), this.vuelto), confirmar);
        } else {
            this.tiposCont = h("div", { id: "tipos-entrega" });
            this.extra = h("div", { id: "datos-entrega" });
            panel.append(h("h2", { texto: "Tu Pedido" }), this.lineas, filaTotal,
                h("span", { class: "etiqueta", style: "margin-top:.8rem", texto: "¿CÓMO LO RECIBES?" }), this.tiposCont, this.extra, confirmar,
                h("p", { class: "aviso", texto: "Al confirmar recibirás un código de 3 dígitos para retirar tu pedido." }));
            this.cargarTipos();
        }
        this.dibujar();
    },

    // ---------- Tipo de entrega (Virtual, Delivery, Personalizado) desde la base de datos ----------
    async cargarTipos() {
        const nombres = { Virtual: "Virtual (Recojo en tienda)" };
        try {
            this.tipos = await api("/api/tipos-pedido");
            this.tiposCont.replaceChildren(...this.tipos.map((t, i) => h("label", { class: "check" },
                h("input", { type: "radio", name: "entrega", value: t.id, checked: i === 0, onchange: () => this.actualizarExtra() }),
                h("span", { texto: nombres[t.nombre] || t.nombre }))));
            this.actualizarExtra();
        } catch (e) {
            UI.estado(this.tiposCont, e.message);
        }
    },

    tipoActual() {
        const marcado = this.tiposCont.querySelector("input:checked");
        return marcado ? this.tipos.find((t) => t.id === parseInt(marcado.value, 10)) : null;
    },

    // Campos extra según el tipo: Delivery pide dirección; Personalizado pide fecha y hora
    actualizarExtra() {
        const tipo = this.tipoActual();
        this.extra.replaceChildren();
        if (!tipo) {
            return;
        }
        if (tipo.nombre === "Delivery") {
            this.direccion = h("input", { class: "campo-simple", type: "text", maxlength: "50", placeholder: "Dirección de entrega", "aria-label": "Dirección de entrega", style: "letter-spacing:0" });
            this.extra.append(this.direccion);
        } else if (tipo.nombre === "Personalizado") {
            const hoy = new Date();
            const minimo = hoy.getFullYear() + "-" + String(hoy.getMonth() + 1).padStart(2, "0") + "-" + String(hoy.getDate()).padStart(2, "0");
            this.fecha = h("input", { class: "campo-simple", type: "date", min: minimo, "aria-label": "Fecha del pedido", style: "letter-spacing:0" });
            this.hora = h("input", { class: "campo-simple", type: "time", "aria-label": "Hora del pedido", style: "letter-spacing:0" });
            this.extra.append(this.fecha, this.hora);
        }
    },

    // ---------- Productos del pedido ----------
    // Cada vez que se agrega un producto sale un aviso (SweetAlert2). Los productos con stock no pueden
    // superar lo disponible; los panes (stock null) no tienen ese límite.
    agregar(producto) {
        const existente = this.items.find((i) => i.id === producto.id);
        const actual = existente ? existente.cantidad : 0;
        if (!this.puedeSubir(producto.nombre, producto.stock, actual)) {
            return;
        }
        if (existente) {
            existente.cantidad++;
        } else {
            this.items.push({ id: producto.id, nombre: producto.nombre, precio: Number(producto.precio), cantidad: 1, stock: producto.stock });
        }
        this.dibujar();
        UI.alerta("success", producto.nombre + " agregado", "Cantidad en tu pedido: " + (actual + 1));
    },

    cambiar(id, diferencia) {
        const item = this.items.find((i) => i.id === id);
        if (diferencia > 0) {
            if (!this.puedeSubir(item.nombre, item.stock, item.cantidad)) {
                return;
            }
            UI.alerta("success", item.nombre + " agregado", "Cantidad en tu pedido: " + (item.cantidad + 1));
        }
        item.cantidad += diferencia;
        this.items = this.items.filter((i) => i.cantidad > 0);
        this.dibujar();
    },

    puedeSubir(nombre, stock, cantidadActual) {
        if (stock !== null && stock !== undefined && cantidadActual + 1 > stock) {
            UI.alerta("warning", "Stock máximo alcanzado", "Solo quedan " + stock + " unidades de " + nombre);
            return false;
        }
        if (cantidadActual + 1 > 50) {
            UI.alerta("warning", "Cantidad máxima", "Puedes pedir hasta 50 unidades de cada producto");
            return false;
        }
        return true;
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

    // ---------- Consolidar el pedido: el servidor guarda todo y genera el código de 3 dígitos ----------
    async confirmar() {
        const boton = document.getElementById("btn-confirmar");
        try {
            if (!this.items.length) {
                throw new Error("Agrega al menos un producto al pedido");
            }
            const cuerpo = { items: this.items.map((i) => ({ idProducto: i.id, cantidad: i.cantidad })) };
            let recibido = 0;

            if (this.modo === "cajera") {
                recibido = parseFloat(this.dinero.value);
                if (isNaN(recibido)) {
                    throw new Error("Ingresa el dinero recibido");
                }
                if (recibido < this.suma()) {
                    throw new Error("El dinero recibido es menor al total");
                }
            } else {
                const tipo = this.tipoActual();
                if (!tipo) {
                    throw new Error("Elige cómo quieres recibir tu pedido");
                }
                cuerpo.idTipo = tipo.id;
                if (tipo.nombre === "Delivery") {
                    cuerpo.direccion = this.direccion.value.trim();
                    if (cuerpo.direccion === "") {
                        throw new Error("Ingresa la dirección para el delivery");
                    }
                }
                if (tipo.nombre === "Personalizado") {
                    cuerpo.fecha = this.fecha.value;
                    cuerpo.hora = this.hora.value;
                    if (cuerpo.fecha === "" || cuerpo.hora === "") {
                        throw new Error("Elige la fecha y la hora de tu pedido personalizado");
                    }
                }
            }

            boton.disabled = true;
            const respuesta = await api("/api/pedidos", "POST", cuerpo);
            if (this.modo === "cajera") {
                this.mostrarRecibo(respuesta, recibido);
            } else {
                this.mostrarCodigo(respuesta);
                this.items = [];
                this.dibujar();
            }
        } catch (e) {
            UI.toast(e.message, false);
        } finally {
            boton.disabled = false;
        }
    },

    // Cliente: muestra el código aleatorio que generó el servidor
    mostrarCodigo(pedido) {
        const ventana = UI.modal({
            id: "modal-codigo", etiqueta: "PEDIDO REGISTRADO", titulo: "¡Gracias por tu compra!", ancho: "angosto",
            contenido: [
                h("div", { class: "caja-codigo" }, h("small", { texto: "TU CÓDIGO DE RETIRO" }),
                    h("strong", { texto: pedido.codigo }), h("em", { texto: "Preséntalo al retirar o recibir tu pedido" })),
                h("p", { class: "aviso", texto: "Pedido N.º " + pedido.idPedido + " · Total " + UI.soles(pedido.total) + ". Puedes volver a ver tu código en Ver Historial." })],
            botones: [{ texto: "Entendido", onclick: (cerrar) => { cerrar(); ventana.el.remove(); } }]
        });
        ventana.abrir();
    },

    // Cajera: recibo de la venta ya guardada
    mostrarRecibo(pedido, recibido) {
        const total = Number(pedido.total);
        const ahora = new Date();
        const fecha = ahora.getDate() + " " + MESES[ahora.getMonth()] + " " + ahora.getFullYear() + ", " + UI.hora();
        const filaSimple = (izquierda, derecha, clase) => h("div", { class: "fila-total", style: "border:0;padding-top:.3rem" }, h("span", { texto: izquierda }), h("strong", { class: clase, texto: derecha }));
        const recibo = UI.modal({
            id: "modal-recibo", ancho: "angosto",
            contenido: h("div", { class: "recibo" },
                h("span", { class: "logo", texto: "LUZ MAYRA" }), h("h2", { texto: "Panaderia Luz Mayra" }),
                h("small", { texto: "PANADERÍA & PASTELERÍA" }),
                h("div", { class: "recibo-titulo" }, h("strong", { texto: "RECIBO DE COMPRA" }),
                    "Ticket #" + String(pedido.idPedido).padStart(5, "0") + " · Código " + pedido.codigo + " · " + fecha),
                this.items.map((i) => h("div", { class: "linea-pedido" }, h("span", { class: "cantidad", texto: i.cantidad + "x" }),
                    h("div", {}, h("strong", { texto: i.nombre }), h("small", { texto: i.cantidad + " × " + UI.soles(i.precio) })),
                    h("span", { class: "monto", texto: UI.soles(i.precio * i.cantidad) }))),
                h("div", { class: "fila-total" }, h("span", { texto: "Dinero Recibido:" }), h("strong", { texto: UI.soles(recibido) })),
                filaSimple("Total:", UI.soles(total), "precio"),
                filaSimple("Vuelto:", UI.soles(Math.round((recibido - total) * 100) / 100), "precio")),
            botones: [{ texto: "Imprimir", onclick: () => window.print() },
                { texto: "Cerrar", clase: "btn-claro", onclick: (cerrar) => { cerrar(); recibo.el.remove(); this.items = []; this.dinero.value = ""; this.dibujar(); } }]
        });
        recibo.abrir();
    }
};

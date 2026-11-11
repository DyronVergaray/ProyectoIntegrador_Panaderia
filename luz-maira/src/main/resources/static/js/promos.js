/* ===== Promociones: slider bajo el menú (inicio, cliente y cajera) y cálculo de vista previa para el carrito =====
   Responsabilidad: leer las promociones vigentes (GET /api/promociones, público) y mostrarlas.
   Ojo: el premio real lo calcula el servidor al confirmar el pedido; aquí solo se muestra una vista previa.
   Tipos de promoción: MONTO (por cada S/ X de compra) y CANTIDAD (por cada N unidades de un producto). */

const Promos = {
    lista: [],
    indice: 0,              // posición actual del carril (puede llegar a lista.length: es la copia de la primera)
    temporizador: null,

    // Condición en palabras: "Por cada S/ 100.00 de compra" o "Por cada 3 Pan francés que compres"
    condicion(p) {
        return p.tipo === "CANTIDAD"
            ? "Por cada " + p.cantidadCondicion + " " + p.productoCondicion + " que compres"
            : "Por cada " + UI.soles(p.montoMinimo) + " de compra";
    },

    // Texto de la promoción: la descripción que escribió el administrador o una frase generada con sus datos
    texto(p) {
        return p.descripcion || (this.condicion(p) + " llévate " + p.cantidadRegalo + " " + p.producto);
    },

    // Lo que ganaría esta compra (misma regla que el servidor: bloques completos hasta maxVeces).
    // items = productos del carrito [{ id, cantidad }]
    premio(p, total, items) {
        let bloques;
        if (p.tipo === "CANTIDAD") {
            const linea = items.filter((i) => i.id === p.idProductoCondicion).reduce((suma, i) => suma + i.cantidad, 0);
            bloques = Math.floor(linea / p.cantidadCondicion);
        } else {
            bloques = Math.floor(Math.round(total * 100) / Math.round(p.montoMinimo * 100));
        }
        const veces = Math.min(bloques, p.maxVeces);
        return veces >= 1 ? { veces: veces, cantidad: veces * p.cantidadRegalo } : null;
    },

    async cargar() {
        try {
            this.lista = await api("/api/promociones", "GET", null, true);
        } catch (e) {
            console.log(e.message);
            this.lista = [];
        }
        return this.lista;
    },

    diapositiva(p, i, total) {
        return h("div", { class: "promo-slide", role: "group", "aria-label": (i + 1) + " de " + total },
            h("strong", { texto: "🎁 " + p.nombre + ": " }), h("span", { texto: this.texto(p) }));
    },

    // Inserta el slider justo debajo del elemento indicado (la cabecera con el menú). Sin promociones no se muestra nada.
    // Con varias promociones, el carril se desplaza solo hacia la izquierda mostrando la siguiente.
    async montar(cabecera) {
        await this.cargar();
        if (!this.lista.length || !cabecera) {
            return;
        }
        const n = this.lista.length;
        const varias = n > 1;
        const diapositivas = this.lista.map((p, i) => this.diapositiva(p, i, n));
        if (varias) {
            diapositivas.push(this.diapositiva(this.lista[0], 0, n));     // copia de la primera: permite el giro continuo hacia la izquierda
        }
        this.carril = h("div", { class: "promo-carril", "aria-live": "off" }, diapositivas);
        const puntos = this.lista.map((p, i) => h("button", { class: "promo-punto" + (i === 0 ? " activo" : ""), "aria-label": "Ver promoción " + (i + 1), onclick: () => { this.saltar(i); this.iniciar(); } }));
        this.el = h("section", { class: "promo-slider", "aria-label": "Promociones", "aria-roledescription": "carrusel" },
            varias ? h("button", { class: "promo-flecha", "aria-label": "Promoción anterior", onclick: () => { this.anterior(); this.iniciar(); } }, "‹") : null,
            h("div", { class: "promo-pista" }, this.carril),
            varias ? h("button", { class: "promo-flecha", "aria-label": "Promoción siguiente", onclick: () => { this.siguiente(); this.iniciar(); } }, "›") : null,
            varias ? h("div", { class: "promo-puntos" }, puntos) : null);
        cabecera.after(this.el);
        this.indice = 0;
        if (varias) {
            this.carril.addEventListener("transitionend", () => {
                // Al llegar a la copia de la primera, vuelve a la real sin animación (no se nota el salto)
                if (this.indice >= n) {
                    this.mover(0, false);
                }
            });
            this.el.onmouseenter = () => clearInterval(this.temporizador);
            this.el.onmouseleave = () => this.iniciar();
            this.iniciar();
        }
    },

    iniciar() {
        clearInterval(this.temporizador);
        this.temporizador = setInterval(() => this.siguiente(), 4000);
    },

    // Desplaza el carril a la posición i (con o sin animación) y marca el punto que corresponde
    mover(i, animar) {
        const n = this.lista.length;
        this.carril.style.transition = animar ? "" : "none";
        this.indice = i;
        this.carril.style.transform = "translateX(" + (-100 * i) + "%)";
        if (!animar) {
            this.carril.getBoundingClientRect();        // fuerza el repintado antes de reactivar la animación
            this.carril.style.transition = "";
        }
        this.el.querySelectorAll(".promo-punto").forEach((d, k) => d.classList.toggle("activo", k === i % n));
    },

    siguiente() {
        // Si todavía está en la copia de la primera (la animación no terminó), vuelve primero a la real sin animar:
        // así el índice nunca pasa de la última diapositiva (evita el carril vacío al pulsar varias veces seguidas)
        if (this.indice >= this.lista.length) {
            this.mover(0, false);
        }
        this.mover(this.indice + 1, true);
    },

    anterior() {
        if (this.indice <= 0) {
            this.mover(this.lista.length, false);       // salta a la copia del final y retrocede desde ahí
        }
        this.mover(this.indice - 1, true);
    },

    saltar(i) {
        this.mover(i, true);
    }
};

/* ===== Componentes compartidos: se escriben una vez y se inyectan en todas las pantallas ===== */

// ---------- Crear elementos del DOM (los textos se asignan con innerText: sin riesgo de inyección) ----------
function h(etiqueta, props, ...hijos) {
    const el = document.createElement(etiqueta);
    const p = props || {};
    for (const clave of Object.keys(p)) {
        const valor = p[clave];
        if (valor === null || valor === undefined || valor === false) {
            continue;
        }
        if (clave === "class") {
            el.className = valor;
        } else if (clave === "texto") {
            el.innerText = valor;
        } else if (clave === "value") {
            el.value = valor;
        } else if (clave.startsWith("on")) {
            el[clave] = valor;
        } else {
            el.setAttribute(clave, valor === true ? "" : valor);
        }
    }
    for (const hijo of hijos.flat(Infinity)) {
        if (hijo === null || hijo === undefined || hijo === false) {
            continue;
        }
        el.append(hijo instanceof Node ? hijo : document.createTextNode(String(hijo)));
    }
    return el;
}

const MESES = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];
const ICONO_BUSCAR = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="9" r="6"/><path d="M14 14l4 4"/></svg>';

const UI = {
    filtro: { q: "", categorias: [] },   // lo que el usuario escribió/marcó en la barra lateral
    alFiltrar: null,                      // cada pantalla define qué hacer cuando cambia el filtro
    categorias: [],
    listo: null,

    // ---------- Formato ----------
    soles(numero) {
        return "S/ " + (Math.round(Number(numero) * 100) / 100).toFixed(2);
    },

    fecha(iso) {
        const partes = iso.split("-");
        return parseInt(partes[2], 10) + " " + MESES[parseInt(partes[1], 10) - 1] + " " + partes[0];
    },

    hora() {
        return new Date().toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" });
    },

    // ---------- Mensajes ----------
    toast(texto, ok) {
        console.log(texto);
        let aviso = document.getElementById("toast");
        if (!aviso) {
            aviso = h("div", { id: "toast", class: "toast", role: "status" });
            document.body.append(aviso);
        }
        aviso.innerText = texto;
        aviso.className = "toast" + (ok === false ? " error" : "");
        aviso.hidden = false;
        clearTimeout(UI.temporizador);
        UI.temporizador = setTimeout(() => { aviso.hidden = true; }, 3800);
    },

    estado(contenedor, texto) {
        console.log(texto);
        contenedor.replaceChildren(h("p", { class: "vacio", texto: texto }));
    },

    // ---------- Piezas pequeñas ----------
    chip(estado) {
        return h("span", { class: "chip chip-" + (ESTADO_COLOR[estado] || "naranja") }, h("i"), estado);
    },

    botonInfo(alClic, oscuro) {
        return h("button", { class: "info" + (oscuro ? " oscuro" : ""), "aria-label": "Ver información", onclick: alClic }, "i");
    },

    // Tabla: las cabeceras que empiezan con ">" se alinean a la derecha
    tabla(cabeceras, filas, pie) {
        const derecha = cabeceras.map((c) => c.startsWith(">"));
        return h("div", { class: "tabla-caja" },
            h("div", { class: "tabla-scroll" }, h("table", {},
                h("thead", {}, h("tr", {}, cabeceras.map((c, i) => h("th", { class: derecha[i] ? "num" : null, texto: c.replace(/^>/, "") })))),
                h("tbody", {}, filas.map((fila) => h("tr", { "data-filtrable": true },
                    fila.map((c, i) => h("td", { class: derecha[i] ? "num" : null }, c))))))),
            pie ? h("div", { class: "tabla-pie" }, h("span", { texto: pie }), h("span", { texto: "Actualizado hoy, " + UI.hora() })) : null);
    },

    tarjetaProducto(p, modo, alAccion) {
        const accion = modo === "admin"
            ? h("button", { class: "pastilla-chica", onclick: () => alAccion(p) }, "✎ Editar")
            : h("button", { class: "mas", "aria-label": "Agregar " + p.nombre + " al pedido", onclick: () => alAccion(p) }, "+");
        return h("article", { class: "tarjeta" },
            h("div", { class: "foto", role: "img", "aria-label": "Foto de " + p.nombre, texto: p.nombre }),
            h("div", { class: "tarjeta-cuerpo" },
                h("h3", { texto: p.nombre }),
                h("p", { texto: p.descripcion || "" }),
                h("div", { class: "fila" }, h("span", { class: "precio", texto: UI.soles(p.precio) }), accion)));
    },

    // ---------- Ventanas (modales) ----------
    modal({ id, etiqueta, titulo, contenido, ancho, botones }) {
        const cerrar = () => { el.hidden = true; };
        const encabezado = h("h2", { texto: titulo || "" });
        const cuerpo = h("div", { class: "modal-contenido" }, contenido);
        const caja = h("div", { class: "modal-caja " + (ancho || ""), role: "dialog", "aria-modal": "true" },
            h("button", { class: "cerrar", "aria-label": "Cerrar", onclick: cerrar }, "×"),
            etiqueta ? h("div", { class: "modal-etiqueta", texto: etiqueta }) : null,
            titulo ? encabezado : null,
            cuerpo,
            botones ? h("div", { class: "modal-botones" }, botones.map((b) =>
                h("button", { class: "btn " + (b.clase || ""), onclick: () => b.onclick(cerrar) }, b.texto))) : null);
        const el = h("div", { class: "modal", id: id, hidden: true, onclick: (e) => { if (e.target === el) { cerrar(); } } }, caja);
        document.body.append(el);
        return {
            el: el, cerrar: cerrar,
            abrir() { el.hidden = false; },
            poner(nodos) { cuerpo.replaceChildren(...[].concat(nodos)); },
            ponerTitulo(texto) { encabezado.innerText = texto; }
        };
    },

    // Formulario en ventana. campos: { id, etiqueta, tipo, opciones:[{valor,texto}], placeholder, requerido, maxlength }
    modalForm({ id, titulo, etiqueta, campos, boton, alGuardar }) {
        const mensaje = h("div", { class: "mensaje", role: "alert" });
        const controles = {};
        const filas = campos.map((c) => {
            const control = c.tipo === "select"
                ? h("select", {}, (c.opciones || []).map((o) => h("option", { value: o.valor, texto: o.texto })))
                : h("input", { type: c.tipo || "text", placeholder: c.placeholder, maxlength: c.maxlength, min: c.min, step: c.step });
            controles[c.id] = control;
            return h("label", { class: "campo" }, h("span", { texto: c.etiqueta }), control);
        });

        const guardar = async (cerrar) => {
            try {
                const datos = {};
                for (const c of campos) {
                    const crudo = controles[c.id].value;
                    datos[c.id] = c.tipo === "password" ? crudo : crudo.trim();
                    if (c.requerido && datos[c.id] === "") {
                        throw new Error("Completa el campo: " + c.etiqueta);
                    }
                }
                await alGuardar(datos);
                cerrar();
            } catch (e) {
                console.log(e.message);
                mensaje.innerText = e.message;
                mensaje.className = "mensaje";
            }
        };

        const modal = UI.modal({
            id: id, etiqueta: etiqueta, titulo: titulo, ancho: "angosto", contenido: [filas, mensaje],
            botones: [{ texto: "Cancelar", clase: "btn-claro", onclick: (cerrar) => cerrar() }, { texto: boton, onclick: guardar }]
        });
        return Object.assign(modal, {
            // Abre el formulario, con valores iniciales opcionales ({campo: valor})
            abrir(valores, nuevoTitulo) {
                for (const c of campos) {
                    controles[c.id].value = valores && valores[c.id] !== undefined ? valores[c.id] : "";
                }
                mensaje.innerText = "";
                if (nuevoTitulo) {
                    modal.ponerTitulo(nuevoTitulo);
                }
                modal.el.hidden = false;
            },
            ponerOpciones(campo, opciones) {
                controles[campo].replaceChildren(...opciones.map((o) => h("option", { value: o.valor, texto: o.texto })));
            }
        });
    },

    // ---------- Estructura de las pantallas internas ----------
    cabecera(items) {
        const actual = location.pathname.split("/").pop() || "index.html";
        return h("header", { class: "cabecera" },
            h("a", { class: "marca", href: "index.html" },
                h("span", { class: "logo", texto: "LUZ MAYRA" }),
                h("span", {}, h("strong", { texto: "Panaderia Luz Mayra" }), h("small", { texto: "PANADERÍA & PASTELERÍA" }))),
            h("nav", { class: "cabecera-nav", "aria-label": "Navegación" },
                items.map(([texto, enlace]) => h("a", { class: "pastilla" + (enlace === actual ? " activa" : ""), href: enlace, texto: texto }))),
            h("a", { class: "pastilla", href: "index.html", texto: "Cerrar Sesión" }));
    },

    bloqueBuscador() {
        const campo = h("input", { type: "search", placeholder: "Buscar...", "aria-label": "Buscar" });
        const lupa = new DOMParser().parseFromString(ICONO_BUSCAR, "image/svg+xml").documentElement;
        let espera = null;
        campo.oninput = () => {
            clearTimeout(espera);
            espera = setTimeout(() => { UI.filtro.q = campo.value.trim(); UI.disparar(); }, 250);
        };
        return h("div", { class: "lateral-bloque" }, h("span", { class: "etiqueta", texto: "BÚSQUEDA" }),
            h("label", { class: "buscador" }, document.importNode(lupa, true), campo));
    },

    async bloqueCategorias() {
        const bloque = h("div", { class: "lateral-bloque" }, h("h3", { class: "lateral-titulo", texto: "Categorías" }));
        try {
            UI.categorias = await api("/api/categorias");
        } catch (e) {
            bloque.append(h("p", { class: "vacio", texto: e.message }));
            return bloque;
        }
        const preseleccion = parseInt(new URLSearchParams(location.search).get("categoria"), 10);
        if (!isNaN(preseleccion)) {
            UI.filtro.categorias = [preseleccion];
        }
        UI.categorias.forEach((c) => {
            const marca = h("input", { type: "checkbox", checked: UI.filtro.categorias.includes(c.id) });
            marca.onchange = () => {
                UI.filtro.categorias = UI.filtro.categorias.filter((id) => id !== c.id);
                if (marca.checked) {
                    UI.filtro.categorias.push(c.id);
                }
                UI.disparar();
            };
            bloque.append(h("label", { class: "check" }, marca, h("span", { texto: c.nombre })));
        });
        return bloque;
    },

    bloqueResumen(lineas) {
        return h("div", { class: "resumen", id: "resumen" }, h("h3", { texto: "Resumen" }), lineas.map((l) => h("p", { texto: l })));
    },

    actualizarResumen(lineas) {
        const bloque = document.getElementById("resumen");
        if (bloque) {
            bloque.replaceChildren(h("h3", { texto: "Resumen" }), ...lineas.map((l) => h("p", { texto: l })));
        }
    },

    // Avisa a la pantalla que cambió el filtro (o filtra el contenido estático por texto)
    disparar() {
        (UI.alFiltrar || UI.filtrarLocal)(UI.filtro);
    },

    filtrarLocal(filtro) {
        const texto = filtro.q.toLowerCase();
        document.querySelectorAll("[data-filtrable]").forEach((el) => {
            el.hidden = texto !== "" && !el.innerText.toLowerCase().includes(texto);
        });
    },

    // Arma cabecera + barra lateral + panel alrededor del <main id="principal"> según los data-* del <body>
    async montarPagina() {
        const datos = document.body.dataset;
        const principal = document.getElementById("principal");
        if (!datos.menu || !principal) {
            return;
        }
        const partes = (datos.lateral || "").split(",").filter(Boolean);
        const lateral = h("aside", { class: "lateral", id: "lateral" });
        if (partes.includes("buscador")) {
            lateral.append(UI.bloqueBuscador());
        }
        if (partes.includes("categorias")) {
            lateral.append(await UI.bloqueCategorias());
        }
        if (partes.includes("resumen")) {
            lateral.append(UI.bloqueResumen(JSON.parse(datos.resumen || "[]")));
        }
        const panel = datos.panel ? h("aside", { class: "panel-pedido", id: "panel", "aria-label": "Tu pedido" }) : null;
        const app = h("div", { class: "app " + (panel ? "con-panel " : "") + (datos.appclase || "") }, lateral, principal, panel);
        document.body.prepend(UI.cabecera(MENUS[datos.menu] || []), app);
    }
};

// La tecla Escape cierra cualquier ventana abierta
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        document.querySelectorAll(".modal").forEach((m) => { m.hidden = true; });
    }
});

UI.listo = UI.montarPagina();

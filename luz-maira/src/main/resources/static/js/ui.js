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
    // Lo que el usuario escribió/marcó en la barra lateral (pocoStock y desabastecidos: solo en Productos del administrador)
    filtro: { q: "", categorias: [], pocoStock: false, desabastecidos: false },
    alFiltrar: null,                      // cada pantalla define qué hacer cuando cambia el filtro
    categorias: [],
    listo: null,

    // ---------- Formato ----------
    soles(numero) {
        return "S/ " + (Math.round(Number(numero) * 100) / 100).toFixed(2);
    },

    // Fecha de hoy como yyyy-MM-dd (para el atributo min de los campos de fecha)
    hoy(diasMas) {
        const d = new Date();
        d.setDate(d.getDate() + (diasMas || 0));
        return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
    },

    // Cantidad con su unidad en singular o plural: "1 Kilogramo", "25 Kilogramos"
    cantidad(valor, unidad) {
        const PLURAL = { Kilogramo: "Kilogramos", Gramo: "Gramos", Litro: "Litros", Unidad: "Unidades" };
        const numero = Number(valor);
        return numero + " " + (numero === 1 ? unidad : (PLURAL[unidad] || unidad));
    },

    fecha(iso) {
        const partes = iso.split("-");
        return parseInt(partes[2], 10) + " " + MESES[parseInt(partes[1], 10) - 1] + " " + partes[0];
    },

    hora() {
        return new Date().toLocaleTimeString("es-PE", { hour: "2-digit", minute: "2-digit" });
    },

    // ---------- Sesión ----------
    usuario: null,                        // { id, nombre, rol, panel } del usuario que inició sesión

    async cerrarSesion() {
        try {
            await api("/api/logout", "POST", null, true);
        } catch (e) {
            console.log(e.message);
        }
        location.href = "index.html";
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

    // Aviso emergente con SweetAlert2 (si no se cargó la librería, usa el aviso simple)
    alerta(icono, titulo, texto) {
        console.log(titulo + (texto ? " - " + texto : ""));
        if (typeof Swal === "undefined") {
            UI.toast(titulo, icono === "success");
            return;
        }
        Swal.fire({
            toast: true, position: "top-end", icon: icono, title: titulo, text: texto,
            showConfirmButton: false, timer: 2000, timerProgressBar: true, confirmButtonColor: "#d0814f"
        });
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

    // Foto de una tarjeta (productos, productos base y proveedores). Se muestra completa (contain) sobre una versión
    // borrosa de sí misma, así ninguna se recorta. Si todavía no tiene foto, muestra el nombre sobre el fondo de pan.
    foto(url, nombre) {
        return url
            ? h("div", { class: "foto con-imagen", style: "--imagen:url('" + url + "')" }, h("img", { src: url, alt: "Foto de " + nombre, loading: "lazy" }))
            : h("div", { class: "foto", role: "img", "aria-label": "Foto de " + nombre, texto: nombre });
    },

    // Tarjeta de producto: todas miden lo mismo. Si la descripción es larga se desplaza hacia abajo
    // dentro de su recuadro y el precio y el botón quedan siempre a la misma altura.
    tarjetaProducto(p, modo, alAccion) {
        const accion = modo === "admin"
            ? h("button", { class: "pastilla-chica", onclick: () => alAccion(p) }, "✎ Editar")
            : h("button", { class: "mas", "aria-label": "Agregar " + p.nombre + " al pedido", onclick: () => alAccion(p) }, "+");
        return h("article", { class: "tarjeta" },
            UI.foto(p.imagen, p.nombre),
            h("div", { class: "tarjeta-cuerpo" },
                h("h3", { texto: p.nombre, title: p.nombre }),
                h("p", { class: "tarjeta-desc", tabindex: "0", texto: p.descripcion || "" }),
                h("div", { class: "stock-espacio" }, UI.lineaStock(p, modo)),
                h("div", { class: "fila" }, h("span", { class: "precio", texto: UI.soles(p.precio) }), accion)));
    },

    // Estado del stock: los panes (stock null) no controlan stock y no muestran nada a los clientes
    lineaStock(p, modo) {
        if (p.stock === null || p.stock === undefined) {
            return modo === "admin" ? h("small", { class: "stock-linea", texto: "Sin control de stock (pan)" }) : null;
        }
        if (p.stock === 0) {
            return h("small", { class: "stock-linea bajo", texto: "Agotado" });
        }
        const bajo = p.stockMinimo !== null && p.stock <= p.stockMinimo;
        if (modo === "admin") {
            return h("small", { class: "stock-linea" + (bajo ? " bajo" : ""), texto: "Stock: " + p.stock + " (mín. " + p.stockMinimo + ")" });
        }
        return bajo ? h("small", { class: "stock-linea bajo", texto: "¡Quedan " + p.stock + "!" }) : null;
    },

    // Hace elegibles algunas filas de una tabla ya dibujada (de a una). puedeElegir(i) dice si la fila i se puede elegir;
    // alElegir(i) recibe la posición de la fila elegida, o -1 si se quitó la selección.
    seleccionarFilas(contenedor, puedeElegir, alElegir) {
        const filas = [...contenedor.querySelectorAll("tbody tr")];
        filas.forEach((fila, i) => {
            if (!puedeElegir(i)) {
                return;
            }
            fila.classList.add("fila-selec");
            fila.onclick = () => {
                const yaElegida = fila.classList.contains("sel");
                filas.forEach((f) => f.classList.remove("sel"));
                if (!yaElegida) {
                    fila.classList.add("sel");
                }
                alElegir(yaElegida ? -1 : i);
            };
        });
    },

    // Ventana con el detalle de un pedido (cliente, productos y total). El código de retiro solo aparece
    // para quien puede verlo (cliente, cajera y administrador): el servidor no lo envía al personal operativo.
    async verPedido(id) {
        const dato = (etiqueta, valor) => h("p", {}, h("span", { texto: etiqueta }), valor);
        try {
            const d = await api("/api/pedidos/" + id);
            if (!UI.ventanaPedido) {
                UI.ventanaPedido = UI.modal({ id: "modal-detalle", titulo: "Detalle del Pedido", contenido: [] });
            }
            UI.ventanaPedido.ponerTitulo("Detalle del Pedido #" + d.id);
            UI.ventanaPedido.poner([
                h("div", { class: d.codigo ? "datos-detalle" : "" },
                    h("div", {}, h("span", { class: "etiqueta", texto: "DATOS DEL CLIENTE" }),
                        dato("Nombre:", d.cliente), dato("Teléfono:", d.telefono || "—"), dato("Email:", d.email),
                        d.direccion ? dato("Dirección:", d.direccion) : null,
                        dato("Tipo de Pedido:", h("span", { class: "chip chip-sencillo", texto: d.tipo }))),
                    d.codigo ? h("div", { class: "caja-codigo" }, h("small", { texto: "CÓDIGO DE RETIRO" }),
                        h("strong", { texto: d.codigo }), h("em", { texto: "Presentar código en caja al retirar" })) : null),
                h("span", { class: "etiqueta", texto: "PRODUCTOS DEL PEDIDO" }),
                UI.tabla(["Producto", ">Cant.", ">Precio Unit.", ">Subtotal"],
                    d.items.map((i) => [i.producto, "× " + i.cantidad, UI.soles(i.precio), UI.soles(i.subtotal)])),
                h("div", { class: "totales" }, h("p", {}, "Total: ", h("span", { class: "grande", texto: UI.soles(d.total) }))),
                h("div", { class: "modal-pie" }, h("span", { texto: "Fecha de solicitud: " + UI.fecha(d.fechaSolicitud) }))
            ]);
            UI.ventanaPedido.abrir();
        } catch (e) {
            UI.toast(e.message, false);
        }
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

    // Zona para arrastrar y soltar una imagen, o buscarla en las carpetas del equipo (clic o Enter)
    zonaImagen({ accept, maxBytes }) {
        const permitidos = accept.split(",");
        let archivo = null;
        let urlActual = null;          // foto que ya tiene el producto (al editar)
        let urlTemporal = null;        // vista previa del archivo recién elegido

        const entrada = h("input", { type: "file", accept: accept, hidden: true, "aria-label": "Elegir imagen" });
        const miniatura = h("img", { class: "zona-miniatura", alt: "Vista previa de la imagen", hidden: true });
        const texto = h("span", { class: "zona-texto" }, "Arrastra una imagen aquí o ", h("u", { texto: "busca en tus carpetas" }));
        const aviso = h("small", { class: "zona-aviso", texto: "JPG, PNG o WEBP · máximo " + Math.round(maxBytes / 1024 / 1024) + " MB" });
        const mensaje = h("small", { class: "zona-error", role: "alert" });
        const quitar = h("button", { class: "zona-quitar", type: "button", hidden: true }, "Quitar imagen");
        const zona = h("div", { class: "zona-imagen", tabindex: "0", role: "button", "aria-label": "Arrastrar o elegir una imagen" },
            miniatura, texto, aviso, quitar, entrada);

        function mostrar(url) {
            miniatura.hidden = url === null;
            if (url !== null) {
                miniatura.src = url;
            }
            texto.hidden = url !== null;
            aviso.hidden = url !== null;
        }

        function elegir(nuevo) {
            mensaje.innerText = "";
            if (!nuevo) {
                return;
            }
            if (!permitidos.includes(nuevo.type)) {
                mensaje.innerText = "Solo se permiten imágenes JPG, PNG o WEBP";
                return;
            }
            if (nuevo.size > maxBytes) {
                mensaje.innerText = "La imagen supera el tamaño máximo permitido";
                return;
            }
            if (urlTemporal) {
                URL.revokeObjectURL(urlTemporal);
            }
            archivo = nuevo;
            urlTemporal = URL.createObjectURL(nuevo);
            mostrar(urlTemporal);
            quitar.hidden = false;
        }

        zona.onclick = () => entrada.click();
        zona.onkeydown = (e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); entrada.click(); } };
        entrada.onclick = (e) => e.stopPropagation();
        entrada.onchange = () => elegir(entrada.files[0]);
        zona.ondragover = (e) => { e.preventDefault(); zona.classList.add("arrastrando"); };
        zona.ondragleave = () => zona.classList.remove("arrastrando");
        zona.ondrop = (e) => {
            e.preventDefault();
            zona.classList.remove("arrastrando");
            elegir(e.dataTransfer.files[0]);
        };
        quitar.onclick = (e) => {
            e.stopPropagation();
            archivo = null;
            entrada.value = "";
            mensaje.innerText = "";
            mostrar(urlActual);
            quitar.hidden = true;
        };

        return {
            el: h("div", {}, zona, mensaje),
            archivo: () => archivo,
            // Reinicia la zona; urlFoto = foto actual del producto (o null)
            reiniciar(urlFoto) {
                archivo = null;
                entrada.value = "";
                mensaje.innerText = "";
                urlActual = urlFoto || null;
                mostrar(urlActual);
                quitar.hidden = true;
            }
        };
    },

    // Formulario en ventana.
    // campos: { id, etiqueta, tipo (text|number|select|password|imagen...), opciones:[{valor,texto}], placeholder, requerido, maxlength, accept }
    // alCambiar(controles, etiquetas): se llama cuando cambia un campo y al abrir (sirve para mostrar u ocultar campos).
    modalForm({ id, titulo, etiqueta, campos, boton, alGuardar, alCambiar }) {
        const mensaje = h("div", { class: "mensaje", role: "alert" });
        const controles = {};
        const etiquetas = {};
        const zonas = {};
        const filas = campos.map((c) => {
            if (c.tipo === "imagen") {
                zonas[c.id] = UI.zonaImagen({ accept: c.accept, maxBytes: 2 * 1024 * 1024 });
                etiquetas[c.id] = h("div", { class: "campo" }, h("span", { texto: c.etiqueta }), zonas[c.id].el);
                return etiquetas[c.id];
            }
            const control = c.tipo === "select"
                ? h("select", {}, (c.opciones || []).map((o) => h("option", { value: o.valor, texto: o.texto })))
                : h("input", { type: c.tipo || "text", placeholder: c.placeholder, maxlength: c.maxlength, min: c.min, step: c.step });
            control.onchange = () => { if (alCambiar) { alCambiar(controles, etiquetas); } };
            controles[c.id] = control;
            etiquetas[c.id] = h("label", { class: "campo" }, h("span", { texto: c.etiqueta }), control);
            return etiquetas[c.id];
        });

        const guardar = async (cerrar) => {
            try {
                const datos = {};
                for (const c of campos) {
                    if (c.tipo === "imagen") {
                        datos[c.id] = zonas[c.id].archivo();
                        continue;
                    }
                    const crudo = controles[c.id].value;
                    datos[c.id] = c.tipo === "password" ? crudo : crudo.trim();
                    if (c.requerido && datos[c.id] === "" && !etiquetas[c.id].hidden) {
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
            // Abre el formulario, con valores iniciales opcionales ({campo: valor}); para "imagen", el valor es la URL actual
            abrir(valores, nuevoTitulo) {
                for (const c of campos) {
                    const valor = valores && valores[c.id] !== undefined ? valores[c.id] : "";
                    if (c.tipo === "imagen") {
                        zonas[c.id].reiniciar(valor || null);
                    } else {
                        controles[c.id].value = valor;
                    }
                }
                mensaje.innerText = "";
                if (nuevoTitulo) {
                    modal.ponerTitulo(nuevoTitulo);
                }
                if (alCambiar) {
                    alCambiar(controles, etiquetas);
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
                h("img", { class: "logo", src: "img/Logo.png", alt: "Logo de Panadería Luz Mayra", width: "44", height: "44" }),
                h("span", {}, h("strong", { texto: "Panaderia Luz Mayra" }), h("small", { texto: "PANADERÍA & PASTELERÍA" }))),
            h("nav", { class: "cabecera-nav", "aria-label": "Navegación" },
                items.map(([texto, enlace]) => h("a", {
                    class: "pastilla" + (enlace === actual ? " activa" : "") + (texto.startsWith("←") ? " pastilla-volver" : ""),
                    href: enlace, texto: texto }))),
            h("div", { class: "cabecera-sesion" },
                UI.usuario ? h("span", { class: "cabecera-usuario", texto: UI.usuario.nombre + " · " + UI.usuario.rol }) : null,
                h("a", { class: "pastilla", href: "index.html", onclick: (e) => { e.preventDefault(); UI.cerrarSesion(); } }, "Cerrar Sesión")));
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

    // Filtros por estado del stock (solo Productos del administrador). Se pueden marcar los dos a la vez:
    // salen los productos que cumplan cualquiera de las dos condiciones.
    bloqueStock() {
        const bloque = h("div", { class: "lateral-bloque" }, h("h3", { class: "lateral-titulo", texto: "Estado del stock" }));
        [["pocoStock", "Productos con poco stock"], ["desabastecidos", "Desabastecidos"]].forEach(([clave, texto]) => {
            const marca = h("input", { type: "checkbox" });
            marca.onchange = () => {
                UI.filtro[clave] = marca.checked;
                UI.disparar();
            };
            bloque.append(h("label", { class: "check" }, marca, h("span", { texto: texto })));
        });
        return bloque;
    },

    // Menú lateral del administrador (se muestra también cuando entra a las pantallas del almacén)
    bloqueAdmin() {
        const enAlmacen = location.pathname.includes("almacen-");
        return h("div", { class: "lateral-bloque menu-admin" }, h("h3", { texto: "ADMINISTRACIÓN" }),
            ENLACES_ADMIN.map(([texto, enlace]) => h("a", {
                href: enlace, texto: texto,
                class: (enAlmacen ? enlace.startsWith("almacen-") : enlace === location.pathname.split("/").pop()) ? "activo" : null
            })));
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
        try {
            UI.usuario = await api("/api/me");
        } catch (e) {
            return;                       // sin sesión: api() ya llevó al inicio
        }
        const partes = (datos.lateral || "").split(",").filter(Boolean);
        const lateral = h("aside", { class: "lateral", id: "lateral" });
        // El administrador que entra al almacén conserva su menú y un botón para volver a su panel
        const adminEnAlmacen = UI.usuario.rol === "Administrador" && datos.menu === "almacen";
        if (adminEnAlmacen) {
            lateral.append(UI.bloqueAdmin());
        }
        if (partes.includes("buscador")) {
            lateral.append(UI.bloqueBuscador());
        }
        if (partes.includes("categorias")) {
            lateral.append(await UI.bloqueCategorias());
        }
        if (partes.includes("stock")) {
            lateral.append(UI.bloqueStock());
        }
        if (partes.includes("resumen")) {
            lateral.append(UI.bloqueResumen(JSON.parse(datos.resumen || "[]")));
        }
        const panel = datos.panel ? h("aside", { class: "panel-pedido", id: "panel", "aria-label": "Tu pedido" }) : null;
        const app = h("div", { class: "app " + (panel ? "con-panel " : "") + (datos.appclase || "") }, lateral, principal, panel);
        const menu = adminEnAlmacen ? [["← Panel Admin", "admin-dashboard.html"]].concat(MENUS.almacen) : (MENUS[datos.menu] || []);
        document.body.prepend(UI.cabecera(menu), app);
    }
};

// La tecla Escape cierra cualquier ventana abierta
document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        document.querySelectorAll(".modal").forEach((m) => { m.hidden = true; });
    }
});

UI.listo = UI.montarPagina();

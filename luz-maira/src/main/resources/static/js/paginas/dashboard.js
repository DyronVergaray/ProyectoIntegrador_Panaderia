/* ===== Dashboard interactivo (estilo Power BI): al marcar una barra, todo lo demás se filtra ===== */
(async function () {
    await UI.listo;

    const estado = { rango: "30", dias: [], productos: [], estados: [] };   // selección actual
    const ESTADOS = ["Entregado", "Pendiente", "Cancelado"];
    const COLORES = { Entregado: "var(--ambar)", Pendiente: "#f08a4b", Cancelado: "var(--rosa)" };
    const UNIDADES = { Kilogramo: "kg", Gramo: "g", Litro: "L", Unidad: "u" };
    const VISTAS = [["resumen", "Resumen"], ["ventas", "Ventas"], ["pedidos", "Pedidos"], ["produccion", "Producción"], ["inventario", "Inventario"]];
    let datos = null;
    let vista = "resumen";
    const el = (id) => document.getElementById(id);

    // ---------- Selección (clic = solo esa; clic de nuevo = quitar; Ctrl/Mayús + clic = sumar) ----------
    function alternar(lista, valor, evento) {
        const sumar = evento.ctrlKey || evento.metaKey || evento.shiftKey;
        const pos = lista.indexOf(valor);
        if (sumar) {
            if (pos >= 0) { lista.splice(pos, 1); } else { lista.push(valor); }
        } else if (pos >= 0 && lista.length === 1) {
            lista.length = 0;
        } else {
            lista.length = 0;
            lista.push(valor);
        }
        cargar();
    }

    // Clase según haya selección: la elegida se resalta y el resto se atenúa
    function claseSeleccion(lista, valor) {
        return lista.length === 0 ? "" : (lista.includes(valor) ? " sel" : " atenuada");
    }

    // ---------- Carga ----------
    // Parámetros con la selección actual (sirven para cargar el dashboard y para exportarlo a Excel)
    function consultaActual() {
        const consulta = new URLSearchParams({ rango: estado.rango });
        if (estado.dias.length) { consulta.set("dias", estado.dias.join(",")); }
        if (estado.productos.length) { consulta.set("productos", estado.productos.join(",")); }
        if (estado.estados.length) { consulta.set("estados", estado.estados.join(",")); }
        return consulta.toString();
    }

    async function cargar() {
        try {
            el("principal").classList.add("carga");
            datos = await api("/api/dashboard?" + consultaActual());
            dibujar();
        } catch (e) {
            el("dash-sub").innerText = e.message;
            UI.toast(e.message, false);
        } finally {
            el("principal").classList.remove("carga");
        }
    }

    function dibujar() {
        dibujarIndicadores();
        dibujarTendencia();
        dibujarRanking();
        dibujarEstados();
        dibujarRendimiento();
        dibujarMaterias();
        dibujarFiltros();
        dibujarRango();
        aplicarVista();
    }

    // ---------- Indicadores ----------
    function dibujarIndicadores() {
        const k = datos.kpis;
        el("dash-sub").innerText = "Del " + UI.fecha(datos.rango.desde) + " al " + UI.fecha(datos.rango.hasta) + " · " + datos.rango.dias + " días";
        el("kpi-ventas").innerText = UI.soles(k.ventas);
        el("kpi-ventas-sub").innerText = k.variacionVentas == null
            ? (estado.dias.length ? estado.dias.length + " día(s) seleccionado(s)" : "Sin datos del periodo anterior")
            : (k.variacionVentas >= 0 ? "+" : "") + k.variacionVentas + "% vs. periodo anterior";
        el("kpi-pedidos").innerText = k.entregados;
        const porcentaje = k.pedidos === 0 ? 0 : Math.round(k.entregados / k.pedidos * 100);
        el("kpi-pedidos-sub").innerText = porcentaje + "% del total (" + k.pedidos + " pedidos)";
        el("kpi-clientes").innerText = k.clientes;
        el("kpi-clientes-sub").innerText = "clientes distintos en la selección";
    }

    // ---------- Tendencia de ventas (barras) ----------
    function dibujarTendencia() {
        const puntos = datos.ventasPorDia;
        const maximo = Math.max(1, ...puntos.map((p) => Number(p.ventas)));
        const salto = Math.ceil(puntos.length / 12);
        el("barras").replaceChildren(...puntos.map((p) => h("button", {
            class: "barra" + claseSeleccion(estado.dias, p.fecha),
            style: "height:" + Math.max(4, Math.round(Number(p.ventas) / maximo * 100)) + "%",
            title: UI.fecha(p.fecha) + ": " + UI.soles(p.ventas),
            "aria-pressed": estado.dias.includes(p.fecha),
            "aria-label": UI.fecha(p.fecha) + ", " + UI.soles(p.ventas),
            onclick: (e) => alternar(estado.dias, p.fecha, e)
        })));
        el("barras-dias").replaceChildren(...puntos.map((p, i) => h("span", { texto: i % salto === 0 ? p.fecha.slice(8) : "" })));
    }

    // ---------- Más vendidos ----------
    function dibujarRanking() {
        const maximo = Math.max(1, ...datos.productos.map((p) => p.unidades));
        if (!datos.productos.length) {
            UI.estado(el("ranking"), "Sin ventas con esta selección.");
            return;
        }
        el("ranking").replaceChildren(...datos.productos.map((p, i) => h("button", {
            class: "rank-item" + claseSeleccion(estado.productos, p.id), "aria-pressed": estado.productos.includes(p.id),
            onclick: (e) => alternar(estado.productos, p.id, e)
        }, h("p", {}, h("span", { texto: (i + 1) + ". " + p.nombre }), h("strong", { texto: p.unidades })),
            h("div", { class: "pista" }, h("i", { style: "width:" + Math.round(p.unidades / maximo * 100) + "%" })))));
    }

    // ---------- Estado de pedidos (dona + leyenda seleccionable) ----------
    function dibujarEstados() {
        const cantidades = {};
        ESTADOS.forEach((e) => { cantidades[e] = 0; });
        datos.estados.forEach((e) => { cantidades[e.estado] = e.pedidos; });
        const total = ESTADOS.reduce((s, e) => s + cantidades[e], 0);

        let acumulado = 0;
        const tramos = ESTADOS.map((e) => {
            const desde = acumulado;
            acumulado += total === 0 ? 0 : cantidades[e] / total * 100;
            return COLORES[e] + " " + desde + "% " + acumulado + "%";
        });
        el("dona").style.background = total === 0 ? "rgba(255,255,255,.2)" : "conic-gradient(" + tramos.join(", ") + ")";
        el("dona-texto").innerText = (total === 0 ? 0 : Math.round(cantidades.Entregado / total * 100)) + "%";

        el("leyenda").replaceChildren(...ESTADOS.map((e) => h("li", {}, h("button", {
            class: claseSeleccion(estado.estados, e).trim(), "aria-pressed": estado.estados.includes(e),
            onclick: (ev) => alternar(estado.estados, e, ev)
        }, h("span", { style: "color:" + COLORES[e] }, "● "), cantidades[e] + " " + e.toLowerCase() + (cantidades[e] === 1 ? "" : "s")))));
        el("nota-estados").innerText = total + " pedidos en la selección";
    }

    // ---------- Rendimiento por producto ----------
    function dibujarRendimiento() {
        if (!datos.productos.length) {
            UI.estado(el("rendimiento"), "Sin ventas con esta selección.");
            return;
        }
        const filas = datos.productos.map((p) => {
            const variacion = p.variacion == null ? "—" : (p.variacion >= 0 ? "+" : "") + p.variacion + "%";
            return h("tr", { class: "fila-selec" + claseSeleccion(estado.productos, p.id), onclick: (e) => alternar(estado.productos, p.id, e) },
                h("td", { texto: p.nombre }), h("td", { texto: UI.soles(p.ventas) }), h("td", { texto: p.unidades }),
                h("td", { texto: UI.soles(p.ingresoProm) }),
                h("td", {}, h("span", { class: p.variacion == null ? "" : (p.variacion >= 0 ? "variacion-sube" : "variacion-baja"), texto: variacion })));
        });
        el("rendimiento").replaceChildren(h("table", {},
            h("thead", {}, h("tr", {}, ["Producto", "Ventas", "Unidades", "Ingreso prom.", "Variación"].map((t) => h("th", { texto: t })))),
            h("tbody", {}, filas)));
    }

    // ---------- Materias primas y alerta ----------
    function dibujarMaterias() {
        el("materias").replaceChildren(...(datos.materias.length ? datos.materias.map((m) => h("div", { class: "materia" },
            h("span", { texto: m.nombre }),
            h("span", { texto: m.consumo + " " + (UNIDADES[m.unidad] || m.unidad) + " · " + m.porcentaje + "%" })))
            : [h("p", { class: "vacio", texto: "Sin consumo registrado en la selección." })]));

        const alerta = datos.alerta;
        el("alerta").hidden = alerta === null;
        if (alerta) {
            el("alerta").replaceChildren(h("span", { texto: "⚠" }), h("div", {},
                h("strong", { texto: "Reponer " + alerta.materia.toLowerCase() + " hoy" }),
                alerta.bajoMinimo ? "El stock llegó al mínimo permitido." : "Stock para " + alerta.diasDeStock + " días al ritmo actual.", h("br"),
                h("a", { href: "almacen-solicitudes.html", texto: "Crear solicitud →" })));
        }
    }

    // ---------- Filtros activos (se pueden quitar uno por uno) ----------
    function dibujarFiltros() {
        const nombresProducto = {};
        datos.productos.forEach((p) => { nombresProducto[p.id] = p.nombre; });
        const chips = [];
        estado.dias.forEach((d) => chips.push(chipFiltro("Día " + UI.fecha(d), estado.dias, d)));
        estado.productos.forEach((id) => chips.push(chipFiltro(nombresProducto[id] || "Producto " + id, estado.productos, id)));
        estado.estados.forEach((e) => chips.push(chipFiltro(e, estado.estados, e)));
        const contenedor = el("filtros-activos");
        contenedor.hidden = chips.length === 0;
        if (chips.length) {
            chips.push(h("button", { class: "filtro-limpiar", onclick: limpiar }, "Limpiar filtros"));
        }
        contenedor.replaceChildren(...chips);
    }

    function chipFiltro(texto, lista, valor) {
        return h("button", { class: "filtro-chip", title: "Quitar este filtro", onclick: () => { lista.splice(lista.indexOf(valor), 1); cargar(); } }, texto + " ✕");
    }

    function limpiar() {
        estado.dias.length = 0;
        estado.productos.length = 0;
        estado.estados.length = 0;
        cargar();
    }

    // ---------- Periodo (7 días / 30 días / este mes) ----------
    function dibujarRango() {
        el("rango").replaceChildren(...[["7", "7 días"], ["30", "30 días"], ["mes", "Este mes"]].map(([valor, texto]) =>
            h("button", { class: estado.rango === valor ? "activo" : null, "aria-pressed": estado.rango === valor,
                onclick: () => { estado.rango = valor; estado.dias.length = 0; cargar(); } }, texto)));
    }

    // ---------- Vistas del menú lateral ----------
    function aplicarVista() {
        document.querySelectorAll("[data-vistas]").forEach((panel) => {
            panel.hidden = vista !== "resumen" && !panel.dataset.vistas.split(" ").includes(vista);
        });
        document.querySelectorAll("#lateral .menu-admin button").forEach((b) => {
            b.className = b.dataset.vista === vista ? "activo" : "";
        });
    }

    el("lateral").append(h("div", { class: "menu-admin" }, h("h3", { texto: "ADMINISTRACIÓN" }),
        VISTAS.map(([clave, texto]) => h("button", { "data-vista": clave, onclick: () => { vista = clave; aplicarVista(); } }, texto)),
        h("a", { href: "admin-productos.html", texto: "Productos" }),
        h("a", { href: "almacen-productos.html", texto: "Almacén" })));

    // ---------- Exportar a Excel (lo genera el servidor con Apache POI, con los filtros marcados) ----------
    async function exportarExcel() {
        const botones = [el("btn-excel"), el("btn-reporte")];
        try {
            botones.forEach((b) => { b.disabled = true; });
            await descargar("/api/dashboard/excel?" + consultaActual(), "reporte-panaderia.xlsx");
            UI.alerta("success", "Reporte de Excel descargado");
        } catch (e) {
            UI.toast(e.message, false);
        } finally {
            botones.forEach((b) => { b.disabled = false; });
        }
    }
    el("btn-excel").onclick = exportarExcel;
    el("btn-reporte").onclick = exportarExcel;

    cargar();
})();

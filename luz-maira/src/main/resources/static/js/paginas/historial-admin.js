/* ===== Admin: historial de todos los pedidos =====
   Los filtros se envían al servidor (GET /api/pedidos?q=&tipo=&estado=&desde=&hasta=&orden=&dir=),
   que es quien busca y ordena; aquí solo se dibuja la tabla. Al hacer clic en un pedido se abre su detalle. */
(async function () {
    await UI.listo;
    const contenedor = document.getElementById("tabla-pedidos");
    const resumen = document.getElementById("resumen-historial");

    const TIPOS = ["Presencial", "Virtual", "Delivery", "Personalizado"];
    const ESTADOS = ["Pendiente", "Entregado"];

    // Crea un campo con etiqueta; devuelve el control para leer su valor
    function campo(etiqueta, control) {
        return h("label", { class: "campo" }, h("span", { texto: etiqueta }), control);
    }
    function lista(opciones) {
        return h("select", {}, opciones.map((o) => h("option", { value: o[0], texto: o[1] })));
    }

    const texto = h("input", { type: "search", placeholder: "N.º, código, cliente o correo" });
    const tipo = lista([["", "Todos los tipos"]].concat(TIPOS.map((t) => [t, t])));
    const estado = lista([["", "Todos los estados"]].concat(ESTADOS.map((e) => [e, e])));
    const desde = h("input", { type: "date" });
    const hasta = h("input", { type: "date" });
    const orden = lista([["fecha-desc", "Fecha: más recientes primero"], ["fecha-asc", "Fecha: más antiguos primero"],
        ["codigo-asc", "Código: menor a mayor"], ["codigo-desc", "Código: mayor a menor"]]);
    const limpiar = h("button", { class: "btn btn-borde", type: "button", texto: "Limpiar filtros" });

    document.getElementById("filtros-historial").append(
        campo("Buscar", texto), campo("Tipo de pedido", tipo), campo("Estado", estado),
        campo("Desde", desde), campo("Hasta", hasta), campo("Ordenar por", orden), limpiar);

    let pedidos = [];
    let espera = null;                  // retraso al escribir para no consultar en cada tecla
    let consultaActual = 0;             // evita que una respuesta lenta pise a una más reciente

    async function cargar() {
        const numero = ++consultaActual;
        const [campoOrden, sentido] = orden.value.split("-");
        const consulta = new URLSearchParams({ orden: campoOrden, dir: sentido });
        const filtros = { q: texto.value.trim(), tipo: tipo.value, estado: estado.value, desde: desde.value, hasta: hasta.value };
        Object.keys(filtros).forEach((k) => { if (filtros[k]) { consulta.set(k, filtros[k]); } });
        try {
            const respuesta = await api("/api/pedidos?" + consulta.toString());
            if (numero !== consultaActual) {
                return;
            }
            pedidos = respuesta;
            dibujar(Object.values(filtros).some(Boolean));
        } catch (e) {
            UI.estado(contenedor, e.message);
        }
    }

    function dibujar(hayFiltros) {
        if (!pedidos.length) {
            resumen.textContent = "";
            UI.estado(contenedor, hayFiltros ? "Ningún pedido coincide con los filtros elegidos." : "Aún no hay pedidos registrados.");
            return;
        }
        const total = pedidos.reduce((suma, p) => suma + Number(p.total), 0);
        resumen.textContent = pedidos.length + (pedidos.length === 1 ? " pedido" : " pedidos") + " · Total: " + UI.soles(total)
            + (pedidos.length >= 1000 ? " · se muestran los primeros 1000; afina los filtros" : "");
        const info = (p) => UI.botonInfo((e) => { e.stopPropagation(); UI.verPedido(p.id); });
        contenedor.replaceChildren(UI.tabla(
            ["N.º", "Código", "Cliente", "Tipo", "Fecha de solicitud", "Fecha estimada", "Estado", ">Total", "Info"],
            pedidos.map((p) => [String(p.id), p.codigo || "—", p.cliente, p.tipo, UI.fecha(p.fechaSolicitud),
                p.fechaEstimada ? UI.fecha(p.fechaEstimada) : "—", UI.chip(p.estado), UI.soles(p.total), info(p)]),
            pedidos.length + " pedidos"));
    }

    texto.oninput = () => { clearTimeout(espera); espera = setTimeout(cargar, 300); };
    [tipo, estado, desde, hasta, orden].forEach((c) => { c.onchange = cargar; });
    limpiar.onclick = () => {
        texto.value = ""; tipo.value = ""; estado.value = ""; desde.value = ""; hasta.value = ""; orden.value = "fecha-desc";
        cargar();
    };

    cargar();
})();

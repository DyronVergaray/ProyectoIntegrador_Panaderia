/* ===== Admin: registrar, editar y eliminar promociones =====
   Dos tipos de condición:
     - Por monto:    "por cada S/ [monto] de compra..."
     - Por cantidad: "por cada [N] unidades de [producto] que compre..."
   ...llévate [cantidad] [producto] gratis, hasta [veces] veces por pedido, entre las fechas indicadas (opcionales)
   y mientras esté activa. */
(async function () {
    await UI.listo;
    const tabla = document.getElementById("tabla-promociones");
    let promociones = [];
    let idEditando = null;      // null = nueva promoción

    const formulario = UI.modalForm({
        id: "modal-promocion", titulo: "Promoción", boton: "Guardar",
        campos: [
            { id: "nombre", etiqueta: "Nombre", requerido: true, maxlength: 80, placeholder: "Cupcakes gratis" },
            { id: "descripcion", etiqueta: "Texto del slider (opcional)", maxlength: 200, placeholder: "Por cada S/ 100 llévate 6 cupcakes" },
            { id: "tipo", etiqueta: "La promoción se gana…", tipo: "select", requerido: true,
                opciones: [{ valor: "MONTO", texto: "Por monto de compra (S/)" }, { valor: "CANTIDAD", texto: "Por cantidad comprada de un producto" }] },
            { id: "montoMinimo", etiqueta: "Por cada S/ de compra", tipo: "number", requerido: true, min: 0.1, step: "0.10" },
            { id: "idProductoCondicion", etiqueta: "Producto que debe comprar", tipo: "select", requerido: true },
            { id: "cantidadCondicion", etiqueta: "Por cada ¿cuántas unidades compradas?", tipo: "number", requerido: true, min: 1, step: "1" },
            { id: "idProducto", etiqueta: "Producto de regalo", tipo: "select", requerido: true },
            { id: "cantidadRegalo", etiqueta: "Unidades de regalo por cada bloque", tipo: "number", requerido: true, min: 1, step: "1" },
            { id: "maxVeces", etiqueta: "Máximo de veces por pedido", tipo: "number", requerido: true, min: 1, step: "1" },
            { id: "fechaInicio", etiqueta: "Válida desde (opcional)", tipo: "date" },
            { id: "fechaFin", etiqueta: "Válida hasta (opcional)", tipo: "date" },
            { id: "activa", etiqueta: "Estado", tipo: "select", requerido: true, opciones: [{ valor: "1", texto: "Activa" }, { valor: "0", texto: "Pausada" }] }
        ],
        // Muestra solo los campos del tipo elegido (los ocultos no se exigen al guardar)
        alCambiar: (controles, etiquetas) => {
            const porMonto = controles.tipo.value !== "CANTIDAD";
            etiquetas.montoMinimo.hidden = !porMonto;
            etiquetas.idProductoCondicion.hidden = porMonto;
            etiquetas.cantidadCondicion.hidden = porMonto;
        },
        alGuardar: async (d) => {
            const porMonto = d.tipo !== "CANTIDAD";
            const cuerpo = {
                nombre: d.nombre, descripcion: d.descripcion, tipo: d.tipo, idProducto: Number(d.idProducto),
                montoMinimo: porMonto ? Number(d.montoMinimo) : null,
                idProductoCondicion: porMonto ? null : Number(d.idProductoCondicion),
                cantidadCondicion: porMonto ? null : parseInt(d.cantidadCondicion, 10),
                cantidadRegalo: parseInt(d.cantidadRegalo, 10), maxVeces: parseInt(d.maxVeces, 10),
                fechaInicio: d.fechaInicio || null, fechaFin: d.fechaFin || null, activa: d.activa === "1"
            };
            if (porMonto && !(cuerpo.montoMinimo > 0)) {
                throw new Error("El monto de compra debe ser mayor a 0");
            }
            if (!porMonto && !(cuerpo.cantidadCondicion >= 1)) {
                throw new Error("La cantidad a comprar debe ser al menos 1");
            }
            if (!(cuerpo.cantidadRegalo >= 1) || !(cuerpo.maxVeces >= 1)) {
                throw new Error("La cantidad de regalo y las veces máximas deben ser al menos 1");
            }
            if (cuerpo.fechaInicio && cuerpo.fechaFin && cuerpo.fechaFin < cuerpo.fechaInicio) {
                throw new Error("La fecha final no puede ser anterior a la inicial");
            }
            const r = idEditando === null
                ? await api("/api/promociones", "POST", cuerpo)
                : await api("/api/promociones/" + idEditando, "PUT", cuerpo);
            UI.toast(r.mensaje, true);
            await cargar();
        }
    });

    // Productos que se pueden regalar (los eliminados no aparecen en el listado)
    async function cargarProductos(idActual, idCondicion) {
        const productos = await api("/api/productos");
        const opciones = productos.map((p) => ({ valor: String(p.id), texto: p.nombre }));
        // Si el producto de la promoción ya fue eliminado, se conserva en la lista para no perderlo al editar
        for (const [id, nombre] of [[idActual, (promociones.find((p) => p.idProducto === idActual) || {}).producto],
                                    [idCondicion, (promociones.find((p) => p.idProductoCondicion === idCondicion) || {}).productoCondicion]]) {
            if (id && nombre && !opciones.some((o) => o.valor === String(id))) {
                opciones.unshift({ valor: String(id), texto: nombre + " (eliminado)" });
            }
        }
        opciones.unshift({ valor: "", texto: "Elige un producto…" });
        formulario.ponerOpciones("idProducto", opciones);
        formulario.ponerOpciones("idProductoCondicion", opciones);
    }

    async function nueva() {
        idEditando = null;
        try {
            await cargarProductos(null, null);
            formulario.abrir({ tipo: "MONTO", montoMinimo: "100", cantidadCondicion: "3", cantidadRegalo: "1", maxVeces: "1", activa: "1" }, "Nueva promoción");
        } catch (e) {
            UI.toast(e.message, false);
        }
    }

    async function editar(p) {
        idEditando = p.id;
        try {
            await cargarProductos(p.idProducto, p.idProductoCondicion);
            formulario.abrir({ nombre: p.nombre, descripcion: p.descripcion || "", tipo: p.tipo, montoMinimo: p.montoMinimo === null ? "" : p.montoMinimo,
                idProductoCondicion: p.idProductoCondicion ? String(p.idProductoCondicion) : "", cantidadCondicion: p.cantidadCondicion || "", idProducto: String(p.idProducto),
                cantidadRegalo: p.cantidadRegalo, maxVeces: p.maxVeces, fechaInicio: p.fechaInicio || "", fechaFin: p.fechaFin || "",
                activa: p.activa ? "1" : "0" }, "Editar promoción");
        } catch (e) {
            UI.toast(e.message, false);
        }
    }

    async function eliminar(p) {
        if (!(await UI.confirmar("¿Eliminar la promoción «" + p.nombre + "»?", "Dejará de mostrarse y de aplicarse. Los pedidos que ya la usaron conservan su registro.", "Eliminar"))) {
            return;
        }
        try {
            const r = await api("/api/promociones/" + p.id, "DELETE");
            UI.toast(r.mensaje, true);
            await cargar();
        } catch (e) {
            UI.toast(e.message, false);
        }
    }

    function vigencia(p) {
        if (!p.fechaInicio && !p.fechaFin) {
            return "Sin límite";
        }
        return (p.fechaInicio ? UI.fecha(p.fechaInicio) : "…") + " → " + (p.fechaFin ? UI.fecha(p.fechaFin) : "…");
    }

    function estado(p) {
        if (!p.activa) {
            return UI.chip("Pausada");
        }
        return UI.chip(p.vigente ? "Vigente" : "Fuera de fechas");
    }

    function dibujar() {
        if (!promociones.length) {
            UI.estado(tabla, "Aún no hay promociones registradas.");
            return;
        }
        tabla.replaceChildren(UI.tabla(["Promoción", "Condición", "Regalo", "Vigencia", "Estado", ""],
            promociones.map((p) => [
                h("span", {}, h("strong", { texto: p.nombre }), h("br"), h("small", { texto: Promos.texto(p) })),
                Promos.condicion(p) + " (máx. " + p.maxVeces + (p.maxVeces === 1 ? " vez" : " veces") + ")",
                p.cantidadRegalo + " × " + p.producto, vigencia(p), estado(p),
                h("div", { class: "fila" },
                    h("button", { class: "pastilla-chica", onclick: () => editar(p) }, "✎ Editar"),
                    h("button", { class: "pastilla-chica", onclick: () => eliminar(p) }, "🗑 Eliminar"))]),
            promociones.length + (promociones.length === 1 ? " promoción" : " promociones")));
    }

    async function cargar() {
        try {
            promociones = await api("/api/promociones/todas");
            dibujar();
        } catch (e) {
            UI.estado(tabla, e.message);
        }
    }

    document.getElementById("btn-nueva-promocion").onclick = nueva;
    cargar();
})();

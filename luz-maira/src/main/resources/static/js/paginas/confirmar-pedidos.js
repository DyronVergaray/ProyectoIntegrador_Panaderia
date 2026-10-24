/* ===== Despachador y Entregador: lista de pedidos reales y confirmación con el código del cliente =====
   La misma pantalla sirve para los dos roles; el <body> dice cuál es:
     data-accion="Despacho" data-api="/api/despacho"  -> pedidos Virtuales (el cliente los retira en tienda)
     data-accion="Entrega"  data-api="/api/entregas"  -> pedidos Delivery y Personalizados ya asignados a un conductor */
(async function () {
    await UI.listo;
    const datos = document.body.dataset;
    const ruta = datos.api;
    const esEntrega = datos.accion === "Entrega";
    const T = esEntrega
        ? { pendiente: "Por entregar", hecho: "Entregado", verbo: "Entrega", resumen: [" por entregar", " entregados"] }
        : { pendiente: "Por despachar", hecho: "Despachado", verbo: "Despacho", resumen: [" por despachar", " despachados"] };
    const contenedor = document.getElementById("tabla-pedidos");
    let pedidos = [];
    let vistos = [];            // pedidos que se están mostrando (después del buscador)
    let elegido = null;         // pedido pendiente seleccionado

    const formulario = UI.modalForm({
        id: "modal-codigo", titulo: "Confirmar " + T.verbo, boton: "Confirmar",
        campos: [{ id: "codigo", etiqueta: "Código que muestra el cliente (3 dígitos)", placeholder: "123", maxlength: 3, requerido: true }],
        alGuardar: async (valores) => {
            if (!/^\d{3}$/.test(valores.codigo)) {
                throw new Error("El código tiene 3 dígitos");
            }
            const respuesta = await api(ruta + "/" + elegido.id + "/confirmar", "POST", { codigo: valores.codigo });
            UI.alerta("success", respuesta.mensaje);
            await cargar();
        }
    });

    async function cargar() {
        try {
            pedidos = await api(ruta);
            const porHacer = pedidos.filter((p) => p.estado === T.pendiente).length;
            UI.actualizarResumen([porHacer + T.resumen[0], (pedidos.length - porHacer) + T.resumen[1] + " (últimos 7 días)"]);
            dibujar();
        } catch (e) {
            UI.estado(contenedor, e.message);
        }
    }

    function dibujar() {
        elegido = null;
        const texto = UI.filtro.q.toLowerCase();
        vistos = pedidos.filter((p) => !texto || [p.id, p.cliente, p.tipo, p.estado, p.direccion, p.conductor, p.vehiculo]
            .join(" ").toLowerCase().includes(texto));
        if (!vistos.length) {
            UI.estado(contenedor, pedidos.length ? "No hay pedidos que coincidan con tu búsqueda." : "No hay pedidos para mostrar.");
            return;
        }
        // El botón "i" abre el detalle sin elegir la fila
        const info = (p) => UI.botonInfo((e) => { e.stopPropagation(); UI.verPedido(p.id); }, p.estado === T.pendiente);
        const entrega = (p) => UI.fecha(p.fechaEntrega) + " " + p.horaEntrega;
        const cabeceras = esEntrega
            ? ["Pedido", "Cliente", "Tipo", "Entrega", "Dirección", "Conductor · Vehículo", "Estado", "Ver información"]
            : ["Pedido", "Cliente", "Fecha de solicitud", "Estado", "Ver información"];
        const filas = vistos.map((p) => esEntrega
            ? [h("strong", { texto: "#" + p.id }), p.cliente, p.tipo, entrega(p), p.direccion || "—",
                p.conductor ? p.conductor + " · " + p.vehiculo : "—", UI.chip(p.estado), info(p)]
            : [h("strong", { texto: "#" + p.id }), p.cliente, UI.fecha(p.fechaSolicitud), UI.chip(p.estado), info(p)]);
        contenedor.replaceChildren(UI.tabla(cabeceras, filas, vistos.length + " pedidos"));
        UI.seleccionarFilas(contenedor, (i) => vistos[i].estado === T.pendiente, (i) => { elegido = i < 0 ? null : vistos[i]; });
    }

    document.getElementById("btn-confirmar").onclick = () => {
        if (!elegido) {
            UI.toast("Selecciona primero un pedido pendiente de la lista", false);
            return;
        }
        formulario.abrir({}, "Confirmar " + T.verbo + " · pedido #" + elegido.id);
    };

    UI.alFiltrar = dibujar;
    cargar();
})();

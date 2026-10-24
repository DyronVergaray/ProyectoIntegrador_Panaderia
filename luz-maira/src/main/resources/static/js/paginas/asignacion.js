/* ===== Almacén: asignar los pedidos Delivery y Personalizados a un conductor y un vehículo ===== */
(async function () {
    await UI.listo;
    const contenedor = document.getElementById("tabla-asignaciones");
    let pedidos = [];
    let vistos = [];
    let elegido = null;         // pedido "Por asignar" seleccionado en la tabla

    const formulario = UI.modalForm({
        id: "modal-asignar", titulo: "Asignar Delivery", boton: "Asignar",
        campos: [
            { id: "pedido", etiqueta: "Pedido", tipo: "select", requerido: true },
            { id: "conductor", etiqueta: "Conductor", tipo: "select", requerido: true },
            { id: "vehiculo", etiqueta: "Vehículo", tipo: "select", requerido: true }
        ],
        alGuardar: async (valores) => {
            const respuesta = await api("/api/asignaciones", "POST", {
                idPedido: parseInt(valores.pedido, 10), idConductor: parseInt(valores.conductor, 10), idVehiculo: parseInt(valores.vehiculo, 10)
            });
            UI.alerta("success", respuesta.mensaje);
            await cargar();
        }
    });

    async function cargar() {
        try {
            pedidos = await api("/api/asignaciones");
            dibujar();
        } catch (e) {
            UI.estado(contenedor, e.message);
        }
    }

    function dibujar() {
        elegido = null;
        const texto = UI.filtro.q.toLowerCase();
        vistos = pedidos.filter((p) => !texto || [p.id, p.cliente, p.tipo, p.estado, p.conductor, p.vehiculo]
            .join(" ").toLowerCase().includes(texto));
        if (!vistos.length) {
            UI.estado(contenedor, pedidos.length ? "No hay pedidos que coincidan con tu búsqueda." : "No hay pedidos Delivery o Personalizados.");
            return;
        }
        const filas = vistos.map((p) => [String(p.id), p.cliente, p.tipo, UI.fecha(p.fechaSolicitud),
            UI.fecha(p.fechaEntrega) + " " + p.horaEntrega, p.conductor ? p.conductor + " · " + p.vehiculo : "—", UI.chip(p.estado)]);
        contenedor.replaceChildren(UI.tabla(["ID", "Cliente", "Tipo", "Fecha de solicitud", "Fecha de entrega", "Asignado a", "Estado"],
            filas, vistos.length + " pedidos"));
        UI.seleccionarFilas(contenedor, (i) => vistos[i].estado === "Por asignar", (i) => { elegido = i < 0 ? null : vistos[i]; });
    }

    document.getElementById("btn-asignar").onclick = async () => {
        const porAsignar = pedidos.filter((p) => p.estado === "Por asignar");
        if (!porAsignar.length) {
            UI.toast("No hay pedidos por asignar", false);
            return;
        }
        try {
            const recursos = await api("/api/asignaciones/recursos");
            if (!recursos.conductores.length || !recursos.vehiculos.length) {
                UI.toast(!recursos.conductores.length ? "No hay conductores disponibles" : "No hay vehículos disponibles", false);
                return;
            }
            formulario.ponerOpciones("pedido", porAsignar.map((p) => ({ valor: p.id, texto: "#" + p.id + " · " + p.tipo + " · " + p.cliente })));
            formulario.ponerOpciones("conductor", recursos.conductores.map((c) => ({ valor: c.id, texto: c.nombre })));
            formulario.ponerOpciones("vehiculo", recursos.vehiculos.map((v) => ({ valor: v.id, texto: v.placa + (v.descripcion ? " · " + v.descripcion : "") })));
            formulario.abrir({ pedido: elegido ? elegido.id : porAsignar[0].id, conductor: recursos.conductores[0].id,
                vehiculo: recursos.vehiculos[0].id });
        } catch (e) {
            UI.toast(e.message, false);
        }
    };

    UI.alFiltrar = dibujar;
    cargar();
})();

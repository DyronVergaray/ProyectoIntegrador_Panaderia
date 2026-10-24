/* ===== Almacén: solicitudes de productos base a los proveedores y confirmación de su recepción ===== */
(async function () {
    await UI.listo;
    const contenedor = document.getElementById("tabla-solicitudes");
    const UNIDADES = ["Kilogramo", "Gramo", "Litro", "Unidad"].map((u) => ({ valor: u, texto: u }));
    let solicitudes = [];
    let vistas = [];
    let elegida = null;         // solicitud "En camino" seleccionada
    let materias = [];
    let ultimaMateria = "";     // para poner la unidad del producto base solo cuando cambia el producto

    const formulario = UI.modalForm({
        id: "modal-solicitud", titulo: "Nueva solicitud", boton: "Enviar solicitud",
        campos: [
            { id: "proveedor", etiqueta: "Proveedor", tipo: "select", requerido: true },
            { id: "materia", etiqueta: "Producto base", tipo: "select", requerido: true },
            { id: "cantidad", etiqueta: "Cantidad", tipo: "number", min: "0.01", step: "0.01", requerido: true },
            { id: "unidad", etiqueta: "Unidad de medida", tipo: "select", opciones: UNIDADES },
            { id: "precio", etiqueta: "Precio de la solicitud (S/)", tipo: "number", min: "0", step: "0.01", requerido: true },
            { id: "entrega", etiqueta: "Fecha estimada de recepción", tipo: "date", min: UI.hoy(), requerido: true }
        ],
        // Al cambiar de producto base, la unidad sugerida es la suya
        alCambiar: (controles) => {
            if (controles.materia.value !== ultimaMateria) {
                ultimaMateria = controles.materia.value;
                const materia = materias.find((m) => String(m.id) === ultimaMateria);
                if (materia) {
                    controles.unidad.value = materia.unidad;
                }
            }
        },
        alGuardar: async (valores) => {
            const cantidad = parseFloat(valores.cantidad);
            const precio = parseFloat(valores.precio);
            if (isNaN(cantidad) || cantidad <= 0 || isNaN(precio) || precio < 0) {
                throw new Error("Ingresa una cantidad mayor a 0 y un precio desde 0");
            }
            const respuesta = await api("/api/solicitudes", "POST", {
                idProveedor: parseInt(valores.proveedor, 10), idMateria: parseInt(valores.materia, 10),
                cantidad: Math.round(cantidad * 100) / 100, unidad: valores.unidad,
                precio: Math.round(precio * 100) / 100, fechaEstimada: valores.entrega
            });
            UI.alerta("success", respuesta.mensaje);
            await cargar();
        }
    });

    // Ventana de confirmación de recepción
    const recepcion = UI.modal({
        id: "modal-recepcion", etiqueta: "CONFIRMAR RECEPCIÓN", titulo: "¿Llegó este pedido?", contenido: [],
        botones: [
            { texto: "Cancelar", clase: "btn-claro", onclick: (cerrar) => cerrar() },
            { texto: "Confirmar Recepción", onclick: async (cerrar) => {
                try {
                    const respuesta = await api("/api/solicitudes/" + elegida.id + "/recibir", "POST");
                    cerrar();
                    UI.alerta("success", respuesta.mensaje);
                    await cargar();
                } catch (e) {
                    UI.toast(e.message, false);
                }
            } }
        ]
    });

    async function cargar() {
        try {
            solicitudes = await api("/api/solicitudes");
            const enCamino = solicitudes.filter((s) => s.estado === "En camino").length;
            UI.actualizarResumen([enCamino + " solicitudes en camino", (solicitudes.length - enCamino) + " solicitudes recibidas"]);
            dibujar();
        } catch (e) {
            UI.estado(contenedor, e.message);
        }
    }

    function dibujar() {
        elegida = null;
        const texto = UI.filtro.q.toLowerCase();
        vistas = solicitudes.filter((s) => !texto || [s.proveedor, s.materia, s.estado].join(" ").toLowerCase().includes(texto));
        if (!vistas.length) {
            UI.estado(contenedor, solicitudes.length ? "No hay solicitudes que coincidan con tu búsqueda." : "Aún no hay solicitudes a proveedores.");
            return;
        }
        const filas = vistas.map((s) => [s.proveedor, s.materia, h("strong", { texto: s.cantidad === null ? "—" : UI.cantidad(s.cantidad, s.unidad) }),
            UI.soles(s.precio), UI.fecha(s.fechaSolicitud), s.fechaEstimada ? UI.fecha(s.fechaEstimada) : "—", UI.chip(s.estado)]);
        contenedor.replaceChildren(UI.tabla(["Proveedor", "Producto base solicitado", "Cantidad", "Precio", "Fecha de solicitud",
            "Fecha estimada de recepción", "Estado"], filas, vistas.length + " solicitudes"));
        UI.seleccionarFilas(contenedor, (i) => vistas[i].estado === "En camino", (i) => { elegida = i < 0 ? null : vistas[i]; });
    }

    document.getElementById("btn-nueva").onclick = async () => {
        try {
            const proveedores = await api("/api/proveedores");
            materias = await api("/api/materias");
            if (!proveedores.length || !materias.length) {
                UI.toast(!proveedores.length ? "Primero registra un proveedor" : "Primero crea un producto base", false);
                return;
            }
            formulario.ponerOpciones("proveedor", proveedores.map((p) => ({ valor: p.id, texto: p.nombre })));
            formulario.ponerOpciones("materia", materias.map((m) => ({ valor: m.id, texto: m.nombre })));
            ultimaMateria = String(materias[0].id);
            formulario.abrir({ proveedor: proveedores[0].id, materia: materias[0].id, unidad: materias[0].unidad, entrega: UI.hoy(7) });
        } catch (e) {
            UI.toast(e.message, false);
        }
    };

    document.getElementById("btn-recibir").onclick = () => {
        if (!elegida) {
            UI.toast("Selecciona primero una solicitud que esté En camino", false);
            return;
        }
        recepcion.poner([h("p", {}, "Se sumarán ", h("strong", { texto: UI.cantidad(elegida.cantidad, elegida.unidad) }), " de ",
            h("strong", { texto: elegida.materia }), " al stock de productos base y quedará anotado en el registro del día.")]);
        recepcion.abrir();
    };

    UI.alFiltrar = dibujar;
    cargar();
})();

/* ===== Almacén: registro del día (entradas y salidas de productos base). Cada registro actualiza el stock ===== */
(async function () {
    await UI.listo;
    const contenedor = document.getElementById("tabla-registro");
    let movimientos = [];
    let materias = [];

    const formulario = UI.modalForm({
        id: "modal-registro", titulo: "Nuevo registro", boton: "Guardar",
        campos: [
            { id: "materia", etiqueta: "Producto base", tipo: "select", requerido: true },
            { id: "movimiento", etiqueta: "Movimiento", tipo: "select", opciones: [{ valor: "Ingreso", texto: "Ingreso" }, { valor: "Salida", texto: "Salida" }] },
            { id: "cantidad", etiqueta: "Cantidad (en la unidad del producto base)", tipo: "number", min: "1", step: "1", requerido: true },
            { id: "descripcion", etiqueta: "Descripción", placeholder: "Motivo del movimiento", maxlength: 130 }
        ],
        alGuardar: async (valores) => {
            const cantidad = Number(valores.cantidad);
            if (!Number.isInteger(cantidad) || cantidad < 1) {
                throw new Error("La cantidad debe ser un número entero desde 1 (para medios kilos, registra en gramos)");
            }
            const respuesta = await api("/api/movimientos", "POST", {
                idMateria: parseInt(valores.materia, 10), movimiento: valores.movimiento, cantidad: cantidad, descripcion: valores.descripcion
            });
            UI.alerta("success", respuesta.mensaje);
            await cargar();
        }
    });

    async function cargar() {
        try {
            movimientos = await api("/api/movimientos");
            dibujar();
        } catch (e) {
            UI.estado(contenedor, e.message);
        }
    }

    function dibujar() {
        const texto = UI.filtro.q.toLowerCase();
        const vistos = movimientos.filter((m) => !texto || [m.materia, m.descripcion, m.fecha].join(" ").toLowerCase().includes(texto));
        if (!vistos.length) {
            UI.estado(contenedor, movimientos.length ? "No hay registros que coincidan con tu búsqueda." : "Aún no hay registros.");
            return;
        }
        contenedor.replaceChildren(UI.tabla(["Fecha", "Producto base", "Cantidad", "Descripción"],
            vistos.map((m) => [UI.fecha(m.fecha), m.materia, UI.cantidad(m.cantidad, m.unidad), m.descripcion || "—"]),
            vistos.length + " registros (los más recientes)"));
    }

    document.getElementById("btn-nuevo").onclick = async () => {
        try {
            materias = await api("/api/materias");
            if (!materias.length) {
                UI.toast("Primero crea un producto base", false);
                return;
            }
            formulario.ponerOpciones("materia", materias.map((m) => ({ valor: m.id, texto: m.nombre + " (" + m.unidad + ")" })));
            formulario.abrir({ materia: materias[0].id, movimiento: "Salida" });
        } catch (e) {
            UI.toast(e.message, false);
        }
    };

    UI.alFiltrar = dibujar;
    cargar();
})();

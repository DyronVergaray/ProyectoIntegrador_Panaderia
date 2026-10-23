/* ===== Almacén: productos base (materias primas). Lista, alta y edición con foto, todo desde la BD ===== */
(async function () {
    await UI.listo;
    const grilla = document.getElementById("grilla-materias");
    const PLURAL = { Kilogramo: "Kilogramos", Gramo: "Gramos", Litro: "Litros", Unidad: "Unidades" };
    const UNIDADES = Object.keys(PLURAL).map((u) => ({ valor: u, texto: u }));
    let materias = [];          // última lista recibida del servidor (el buscador filtra sobre ella)
    let idEditando = null;      // null = se está creando uno nuevo

    const formulario = UI.modalForm({
        id: "modal-materia", titulo: "Producto base", boton: "Guardar",
        campos: [
            { id: "nombre", etiqueta: "Nombre", placeholder: "Ej. Harina", requerido: true, maxlength: 50 },
            { id: "unidad", etiqueta: "Unidad de medida", tipo: "select", opciones: UNIDADES },
            { id: "stock", etiqueta: "Cantidad (stock actual)", tipo: "number", min: "0", step: "0.01", requerido: true },
            { id: "stockMinimo", etiqueta: "Stock mínimo (alerta)", tipo: "number", min: "0", step: "0.01", requerido: true },
            { id: "imagen", etiqueta: "Imagen del producto base", tipo: "imagen", accept: "image/jpeg,image/png,image/webp" }
        ],
        alGuardar: async (datos) => {
            const stock = parseFloat(datos.stock);
            const minimo = parseFloat(datos.stockMinimo);
            if (isNaN(stock) || stock < 0 || isNaN(minimo) || minimo < 0) {
                throw new Error("Ingresa la cantidad y el stock mínimo (números desde 0)");
            }
            const cuerpo = { nombre: datos.nombre, unidad: datos.unidad, stock: stock, stockMinimo: minimo };
            const respuesta = idEditando === null
                ? await api("/api/materias", "POST", cuerpo)
                : await api("/api/materias/" + idEditando, "PUT", cuerpo);
            idEditando = idEditando === null ? respuesta.id : idEditando;   // si falla la imagen, reintentar edita el mismo registro
            if (datos.imagen) {
                try {
                    const archivo = new FormData();
                    archivo.append("archivo", datos.imagen);
                    await api("/api/materias/" + idEditando + "/imagen", "POST", archivo);
                } catch (e) {
                    await cargar();
                    throw new Error("El producto base se guardó, pero la imagen no se pudo subir: " + e.message);
                }
            }
            UI.alerta("success", respuesta.mensaje);
            await cargar();
        }
    });

    function editar(m) {
        idEditando = m.id;
        formulario.abrir({ nombre: m.nombre, unidad: m.unidad,
            stock: m.stock === null ? "" : Number(m.stock),
            stockMinimo: m.stockMinimo === null ? "" : Number(m.stockMinimo),
            imagen: m.imagen }, "Editar producto base");
    }

    document.getElementById("btn-nuevo").onclick = () => {
        idEditando = null;
        formulario.abrir({ unidad: UNIDADES[0].valor }, "Nuevo producto base");
    };

    function tarjeta(m) {
        const unidad = Number(m.stock) === 1 ? m.unidad : (PLURAL[m.unidad] || m.unidad);
        const cantidad = m.stock === null ? "Sin dato" : Number(m.stock) + " " + unidad;
        return h("article", { class: "tarjeta" },
            UI.foto(m.imagen, m.nombre),
            h("div", { class: "tarjeta-cuerpo" }, h("h3", { texto: m.nombre, title: m.nombre }),
                h("span", { class: "dato-destacado", texto: "Cantidad total: " + cantidad }),
                h("small", { class: "stock-linea" + (m.bajo ? " bajo" : ""),
                    texto: m.stockMinimo === null ? "Sin stock mínimo" : "Mínimo: " + Number(m.stockMinimo) + (m.bajo ? " · ¡Stock bajo!" : "") }),
                h("div", { class: "fila" }, h("span"),
                    h("button", { class: "pastilla-chica", onclick: () => editar(m) }, "✎ Editar"))));
    }

    // Dibuja la lista aplicando lo que se escribió en el buscador (por nombre)
    function dibujar() {
        const texto = UI.filtro.q.toLowerCase();
        const visibles = materias.filter((m) => m.nombre.toLowerCase().includes(texto));
        if (!visibles.length) {
            UI.estado(grilla, materias.length ? "No hay productos base que coincidan con tu búsqueda." : "Aún no hay productos base registrados.");
            return;
        }
        grilla.replaceChildren(...visibles.map(tarjeta));
    }

    async function cargar() {
        try {
            materias = await api("/api/materias");
            dibujar();
        } catch (e) {
            UI.estado(grilla, e.message);
        }
    }

    UI.alFiltrar = dibujar;
    cargar();
})();

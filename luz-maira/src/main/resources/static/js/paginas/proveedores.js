/* ===== Almacén: proveedores. Lista, alta y edición con foto, todo desde la BD ===== */
(async function () {
    await UI.listo;
    const grilla = document.getElementById("grilla-proveedores");
    let proveedores = [];       // última lista recibida del servidor (el buscador filtra sobre ella)
    let idEditando = null;      // null = se está creando uno nuevo

    const formulario = UI.modalForm({
        id: "modal-proveedor", titulo: "Proveedor", boton: "Guardar",
        campos: [
            { id: "nombre", etiqueta: "Nombre", placeholder: "Razón social", requerido: true, maxlength: 150 },
            { id: "ruc", etiqueta: "RUC", placeholder: "11 dígitos", requerido: true, maxlength: 11 },
            { id: "direccion", etiqueta: "Dirección", maxlength: 255 },
            { id: "descripcion", etiqueta: "Descripción", placeholder: "Qué nos provee", maxlength: 150 },
            { id: "imagen", etiqueta: "Imagen del proveedor", tipo: "imagen", accept: "image/jpeg,image/png,image/webp" }
        ],
        alGuardar: async (datos) => {
            if (!/^\d{11}$/.test(datos.ruc)) {
                throw new Error("El RUC debe tener 11 dígitos");
            }
            const cuerpo = { nombre: datos.nombre, ruc: datos.ruc, direccion: datos.direccion, descripcion: datos.descripcion };
            const respuesta = idEditando === null
                ? await api("/api/proveedores", "POST", cuerpo)
                : await api("/api/proveedores/" + idEditando, "PUT", cuerpo);
            idEditando = idEditando === null ? respuesta.id : idEditando;   // si falla la imagen, reintentar edita el mismo registro
            if (datos.imagen) {
                try {
                    const archivo = new FormData();
                    archivo.append("archivo", datos.imagen);
                    await api("/api/proveedores/" + idEditando + "/imagen", "POST", archivo);
                } catch (e) {
                    await cargar();
                    throw new Error("El proveedor se guardó, pero la imagen no se pudo subir: " + e.message);
                }
            }
            UI.alerta("success", respuesta.mensaje);
            await cargar();
        }
    });

    function editar(p) {
        idEditando = p.id;
        formulario.abrir({ nombre: p.nombre, ruc: p.ruc, direccion: p.direccion || "",
            descripcion: p.descripcion || "", imagen: p.imagen }, "Editar proveedor");
    }

    document.getElementById("btn-nuevo").onclick = () => {
        idEditando = null;
        formulario.abrir({}, "Nuevo proveedor");
    };

    function tarjeta(p) {
        return h("article", { class: "tarjeta" },
            UI.foto(p.imagen, p.nombre),
            h("div", { class: "tarjeta-cuerpo" }, h("h3", { texto: p.nombre, title: p.nombre }),
                p.descripcion ? h("span", { class: "dato-destacado", texto: p.descripcion }) : null,
                h("small", { class: "stock-linea", texto: "RUC " + p.ruc + (p.direccion ? " · " + p.direccion : "") }),
                h("div", { class: "fila" }, h("span"),
                    h("button", { class: "pastilla-chica", onclick: () => editar(p) }, "✎ Editar"))));
    }

    // Dibuja la lista aplicando lo que se escribió en el buscador (nombre, descripción o RUC)
    function dibujar() {
        const texto = UI.filtro.q.toLowerCase();
        const visibles = proveedores.filter((p) =>
            (p.nombre + " " + (p.descripcion || "") + " " + p.ruc).toLowerCase().includes(texto));
        if (!visibles.length) {
            UI.estado(grilla, proveedores.length ? "No hay proveedores que coincidan con tu búsqueda." : "Aún no hay proveedores registrados.");
            return;
        }
        grilla.replaceChildren(...visibles.map(tarjeta));
    }

    async function cargar() {
        try {
            proveedores = await api("/api/proveedores");
            dibujar();
        } catch (e) {
            UI.estado(grilla, e.message);
        }
    }

    UI.alFiltrar = dibujar;
    cargar();
})();

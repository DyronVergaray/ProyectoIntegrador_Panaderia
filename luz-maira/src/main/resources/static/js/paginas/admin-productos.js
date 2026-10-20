/* ===== Admin: gestión de productos (filtros, añadir y editar) ===== */
(async function () {
    await UI.listo;
    const grilla = document.getElementById("grilla");
    let idEditando = null;

    const formulario = UI.modalForm({
        id: "modal-producto", titulo: "Producto", boton: "Guardar",
        campos: [
            { id: "nombre", etiqueta: "Nombre", requerido: true, maxlength: 150, placeholder: "Nombre del producto" },
            { id: "idCategoria", etiqueta: "Categoría", tipo: "select", requerido: true },
            { id: "descripcion", etiqueta: "Descripción", maxlength: 1000, placeholder: "Descripción corta" },
            { id: "precio", etiqueta: "Precio (S/)", tipo: "number", min: "0.01", step: "0.01", requerido: true },
            { id: "imagen", etiqueta: "Imagen (JPG, PNG o WEBP, máx. 2 MB)", tipo: "file", accept: "image/jpeg,image/png,image/webp" }
        ],
        alGuardar: async (datos) => {
            const precio = parseFloat(datos.precio);
            if (isNaN(precio) || precio <= 0) {
                throw new Error("El precio debe ser mayor a 0");
            }
            if (datos.imagen && datos.imagen.size > 2 * 1024 * 1024) {
                throw new Error("La imagen supera el tamaño máximo permitido (2 MB)");
            }
            const cuerpo = {
                nombre: datos.nombre, idCategoria: parseInt(datos.idCategoria, 10),
                descripcion: datos.descripcion, precio: Math.round(precio * 100) / 100
            };
            const respuesta = idEditando === null
                ? await api("/api/productos", "POST", cuerpo)
                : await api("/api/productos/" + idEditando, "PUT", cuerpo);
            idEditando = idEditando === null ? respuesta.id : idEditando;   // si falla la imagen, reintentar edita el mismo producto
            if (datos.imagen) {
                try {
                    const archivo = new FormData();
                    archivo.append("archivo", datos.imagen);
                    await api("/api/productos/" + idEditando + "/imagen", "POST", archivo);
                } catch (e) {
                    UI.disparar();
                    throw new Error("El producto se guardó, pero la imagen no se pudo subir: " + e.message);
                }
            }
            UI.toast(respuesta.mensaje, true);
            UI.disparar();
        }
    });
    formulario.ponerOpciones("idCategoria", UI.categorias.map((c) => ({ valor: c.id, texto: c.nombre })));

    function editar(producto) {
        idEditando = producto.id;
        formulario.abrir({ nombre: producto.nombre, idCategoria: producto.idCategoria,
            descripcion: producto.descripcion || "", precio: producto.precio }, "Editar producto");
    }

    document.getElementById("btn-nuevo").onclick = () => {
        idEditando = null;
        formulario.abrir({ idCategoria: UI.categorias.length ? UI.categorias[0].id : "" }, "Añadir producto");
    };

    UI.alFiltrar = () => Catalogo.cargar(grilla, "admin", editar);
    UI.disparar();
})();

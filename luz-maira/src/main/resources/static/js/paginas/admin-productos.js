/* ===== Admin: gestión de productos (filtros, añadir y editar) ===== */
(async function () {
    await UI.listo;
    const grilla = document.getElementById("grilla");
    let idEditando = null;

    // Los panes no controlan stock: al elegir esa categoría se ocultan los campos de stock
    const esPan = (idCategoria) => {
        const categoria = UI.categorias.find((c) => c.id === idCategoria);
        return categoria !== undefined && categoria.nombre === "Panes";
    };

    const formulario = UI.modalForm({
        id: "modal-producto", titulo: "Producto", boton: "Guardar",
        campos: [
            { id: "nombre", etiqueta: "Nombre", requerido: true, maxlength: 150, placeholder: "Nombre del producto" },
            { id: "idCategoria", etiqueta: "Categoría", tipo: "select", requerido: true },
            { id: "descripcion", etiqueta: "Descripción", maxlength: 1000, placeholder: "Descripción corta" },
            { id: "precio", etiqueta: "Precio (S/)", tipo: "number", min: "0.01", step: "0.01", requerido: true },
            { id: "stock", etiqueta: "Stock actual (unidades)", tipo: "number", min: "0", step: "1" },
            { id: "stockMinimo", etiqueta: "Stock mínimo (alerta)", tipo: "number", min: "0", step: "1" },
            { id: "imagen", etiqueta: "Imagen del producto", tipo: "imagen", accept: "image/jpeg,image/png,image/webp" }
        ],
        alCambiar: (controles, etiquetas) => {
            const pan = esPan(parseInt(controles.idCategoria.value, 10));
            etiquetas.stock.hidden = pan;
            etiquetas.stockMinimo.hidden = pan;
        },
        alGuardar: async (datos) => {
            const precio = parseFloat(datos.precio);
            if (isNaN(precio) || precio <= 0) {
                throw new Error("El precio debe ser mayor a 0");
            }
            const idCategoria = parseInt(datos.idCategoria, 10);
            const cuerpo = {
                nombre: datos.nombre, idCategoria: idCategoria,
                descripcion: datos.descripcion, precio: Math.round(precio * 100) / 100
            };
            if (!esPan(idCategoria)) {
                const stock = parseInt(datos.stock, 10);
                const minimo = parseInt(datos.stockMinimo, 10);
                if (isNaN(stock) || stock < 0 || isNaN(minimo) || minimo < 0) {
                    throw new Error("Ingresa el stock y el stock mínimo (números desde 0)");
                }
                cuerpo.stock = stock;
                cuerpo.stockMinimo = minimo;
            }
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
            UI.alerta("success", respuesta.mensaje);
            UI.disparar();
        }
    });
    formulario.ponerOpciones("idCategoria", UI.categorias.map((c) => ({ valor: c.id, texto: c.nombre })));

    function editar(producto) {
        idEditando = producto.id;
        formulario.abrir({ nombre: producto.nombre, idCategoria: producto.idCategoria,
            descripcion: producto.descripcion || "", precio: producto.precio,
            stock: producto.stock === null ? "" : producto.stock,
            stockMinimo: producto.stockMinimo === null ? "" : producto.stockMinimo,
            imagen: producto.imagen }, "Editar producto");
    }

    document.getElementById("btn-nuevo").onclick = () => {
        idEditando = null;
        formulario.abrir({ idCategoria: UI.categorias.length ? UI.categorias[0].id : "" }, "Añadir producto");
    };

    UI.alFiltrar = () => Catalogo.cargar(grilla, "admin", editar);
    UI.disparar();
})();

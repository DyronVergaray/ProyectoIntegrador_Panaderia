/* ===== Admin: gestión de productos (filtros, añadir, editar, eliminar y restaurar) =====
   "Eliminar" no borra: pasa el producto a la categoría "Producto eliminado" (solo la ven admin y almacenero).
   El botón Eliminar solo aparece al editar un producto ya creado; si el producto está eliminado, el mismo botón
   pasa a decir "Devolver producto" y lo regresa al catálogo con la categoría que se elija. */
(async function () {
    await UI.listo;
    const grilla = document.getElementById("grilla");
    let idEditando = null;
    const ELIMINADO = "Producto eliminado";
    // Categorías que se pueden elegir en el formulario ("Producto eliminado" solo se asigna con el botón Eliminar)
    const categoriasElegibles = () => UI.categorias.filter((c) => c.nombre !== ELIMINADO);
    // Un solo botón con dos funciones: "Eliminar" (producto activo) o "Devolver" (producto eliminado)
    const botonEliminar = h("button", { class: "btn btn-peligro", type: "button", texto: "Eliminar", hidden: true });
    let productoEliminado = false;

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
    formulario.ponerOpciones("idCategoria", categoriasElegibles().map((c) => ({ valor: c.id, texto: c.nombre })));
    formulario.el.querySelector(".modal-botones").prepend(botonEliminar);   // a la izquierda de Cancelar / Guardar

    // El botón Guardar del formulario (el último de la ventana): se oculta cuando el producto está eliminado
    const botonGuardar = formulario.el.querySelector(".modal-botones .btn:last-child");

    botonEliminar.onclick = async () => {
        if (productoEliminado) {
            botonGuardar.click();       // guardar con la categoría elegida = devolver el producto al catálogo
            return;
        }
        if (!(await UI.confirmar("¿Eliminar este producto?", "Dejará de verse en el catálogo. Podrás restaurarlo desde la categoría 'Producto eliminado'.", "Sí, eliminar"))) {
            return;
        }
        try {
            const r = await api("/api/productos/" + idEditando, "DELETE");
            formulario.cerrar();
            UI.alerta("success", r.mensaje);
            UI.disparar();
        } catch (e) {
            UI.alerta("error", e.message);
        }
    };

    function editar(producto) {
        idEditando = producto.id;
        const eliminado = producto.categoria === ELIMINADO;
        // Un producto eliminado se restaura eligiendo una categoría; uno activo se puede eliminar
        const opciones = categoriasElegibles().map((c) => ({ valor: c.id, texto: c.nombre }));
        formulario.ponerOpciones("idCategoria", eliminado ? [{ valor: "", texto: "— Elige la categoría para restaurarlo —" }].concat(opciones) : opciones);
        productoEliminado = eliminado;
        botonEliminar.hidden = false;       // solo se abre con productos ya creados
        botonEliminar.textContent = eliminado ? "Devolver" : "Eliminar";
        botonEliminar.classList.toggle("btn-devolver", eliminado);
        botonGuardar.hidden = eliminado;
        formulario.abrir({ nombre: producto.nombre, idCategoria: eliminado ? "" : producto.idCategoria,
            descripcion: producto.descripcion || "", precio: producto.precio,
            stock: producto.stock === null ? "" : producto.stock,
            stockMinimo: producto.stockMinimo === null ? "" : producto.stockMinimo,
            imagen: producto.imagen }, eliminado ? "Devolver producto" : "Editar producto");
    }

    document.getElementById("btn-nuevo").onclick = () => {
        idEditando = null;
        botonEliminar.hidden = true;        // un producto nuevo no se puede eliminar
        botonGuardar.hidden = false;
        productoEliminado = false;
        formulario.ponerOpciones("idCategoria", categoriasElegibles().map((c) => ({ valor: c.id, texto: c.nombre })));
        formulario.abrir({ idCategoria: categoriasElegibles().length ? categoriasElegibles()[0].id : "" }, "Añadir producto");
    };

    UI.alFiltrar = () => Catalogo.cargar(grilla, "admin", editar);
    UI.disparar();
})();

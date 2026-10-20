/* ===== Pantallas de ejemplo (almacén, despacho y entrega): ventanas con el mismo componente, sin guardar datos ===== */
(async function () {
    await UI.listo;
    const accion = document.body.dataset.accion || "";       // "Despacho" o "Entrega"
    const unidades = ["Kilogramo", "Gramo", "Litro", "Unidad"].map((u) => ({ valor: u, texto: u }));
    const opciones = (lista) => lista.map((t) => ({ valor: t, texto: t }));
    const ejemplo = async () => { UI.toast("Pantalla de ejemplo: todavía no guarda datos."); };
    const productosBase = opciones(["Agua", "Masa Madre", "Azúcar", "Huevos", "Levadura", "Chocolate"]);

    // Formularios de cada ventana (clave = valor del atributo data-abrir)
    const FORMULARIOS = {
        base: { titulo: "Nuevo Producto Base", boton: "Guardar", campos: [
            { id: "nombre", etiqueta: "Nombre", placeholder: "Ej. Harina", requerido: true },
            { id: "unidad", etiqueta: "Unidad de medida", tipo: "select", opciones: unidades },
            { id: "cantidad", etiqueta: "Cantidad inicial", tipo: "number", min: "0" }] },
        proveedor: { titulo: "Proveedor", boton: "Guardar", campos: [
            { id: "nombre", etiqueta: "Nombre", placeholder: "Razón social", requerido: true },
            { id: "ruc", etiqueta: "RUC", placeholder: "11 dígitos", maxlength: 11 },
            { id: "direccion", etiqueta: "Dirección" }, { id: "descripcion", etiqueta: "Descripción" }] },
        solicitud: { titulo: "Nueva solicitud", boton: "Enviar solicitud", campos: [
            { id: "proveedor", etiqueta: "Proveedor", tipo: "select", opciones: opciones(["Alicorp", "Puratos Perú", "Casa Grande", "La Calera", "Gloria", "Machu Picchu Foods"]) },
            { id: "producto", etiqueta: "Producto base", tipo: "select", opciones: productosBase },
            { id: "cantidad", etiqueta: "Cantidad", tipo: "number", min: "0" },
            { id: "unidad", etiqueta: "Unidad de medida", tipo: "select", opciones: unidades },
            { id: "entrega", etiqueta: "Fecha estimada de recepción", tipo: "date" }] },
        asignar: { titulo: "Asignar Delivery", boton: "Asignar", campos: [
            { id: "pedido", etiqueta: "Pedido", tipo: "select", opciones: opciones(["190", "213", "543"]) },
            { id: "conductor", etiqueta: "Conductor", tipo: "select", opciones: opciones(["Seleccionar conductor"]) },
            { id: "vehiculo", etiqueta: "Vehículo", tipo: "select", opciones: opciones(["Seleccionar vehículo"]) }] },
        registro: { titulo: "Nuevo registro", boton: "Guardar", campos: [
            { id: "producto", etiqueta: "Producto base", tipo: "select", opciones: productosBase },
            { id: "movimiento", etiqueta: "Movimiento", tipo: "select", opciones: opciones(["Ingreso", "Salida"]) },
            { id: "cantidad", etiqueta: "Cantidad", tipo: "number", min: "0" },
            { id: "unidad", etiqueta: "Unidad de medida", tipo: "select", opciones: unidades },
            { id: "descripcion", etiqueta: "Descripción", placeholder: "Motivo del movimiento" }] },
        codigo: { titulo: "Confirmar " + accion, boton: "Confirmar", campos: [
            { id: "codigo", etiqueta: "Ingrese el código", placeholder: "AU4", maxlength: 3, requerido: true }] }
    };

    // Ventana con la lista de productos de un pedido
    function ventanaProductos(codigo) {
        const modal = UI.modal({
            id: "modal-info", etiqueta: "DETALLE DEL PEDIDO", titulo: "Productos del pedido " + codigo,
            contenido: [
                UI.tabla(["Producto", ">Cantidad", ">Precio Unit.", ">Subtotal"], [
                    ["Pan de Mantequilla", "5", "S/ 4.00", "S/ 20.00"], ["Croissant de Almendras", "3", "S/ 4.50", "S/ 13.50"],
                    ["Pastel Tres Leches", "2", "S/ 18.00", "S/ 36.00"], ["Concha Rústica", "4", "S/ 1.00", "S/ 4.00"]]),
                h("div", { class: "modal-pie" }, UI.chip("Por " + (accion === "Entrega" ? "entregar" : "despachar")),
                    h("span", {}, "Total del Pedido: ", h("strong", { class: "precio", style: "font-size:1.3rem", texto: "S/ 73.50" })))],
            botones: [{ texto: "Cerrar", clase: "btn-claro", onclick: (cerrar) => { cerrar(); modal.el.remove(); } },
                { texto: "Confirmar " + accion, onclick: async (cerrar) => { await ejemplo(); cerrar(); modal.el.remove(); } }]
        });
        modal.abrir();
    }

    const creadas = {};
    function abrir(clave, boton) {
        const codigo = (boton && boton.dataset.codigo) || document.body.dataset.primero || "";
        if (clave === "info") {
            ventanaProductos(codigo);
            return;
        }
        if (!creadas[clave]) {
            creadas[clave] = UI.modalForm({ id: "modal-" + clave, ...FORMULARIOS[clave], alGuardar: ejemplo });
        }
        creadas[clave].abrir(null, clave === "codigo" ? "Confirmar " + accion + " · pedido " + codigo : undefined);
    }

    document.querySelectorAll("[data-abrir]").forEach((boton) => {
        boton.onclick = () => abrir(boton.dataset.abrir, boton);
    });
})();

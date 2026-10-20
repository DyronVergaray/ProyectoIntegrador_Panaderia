/* ===== Catálogo del cliente y de la cajera: productos de la BD + panel "Tu Pedido" ===== */
(async function () {
    await UI.listo;
    const modo = document.body.dataset.modo;            // "cliente" o "cajera"
    const grilla = document.getElementById("grilla");

    Carrito.montar(modo);
    UI.alFiltrar = () => Catalogo.cargar(grilla, "cliente", (producto) => Carrito.agregar(producto));
    UI.disparar();
})();

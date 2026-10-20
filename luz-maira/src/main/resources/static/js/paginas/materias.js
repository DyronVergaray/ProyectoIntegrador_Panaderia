/* ===== Almacén: productos base (materias primas) con su stock y stock mínimo, desde la BD ===== */
(async function () {
    await UI.listo;
    const grilla = document.getElementById("grilla-materias");
    const PLURAL = { Kilogramo: "Kilogramos", Gramo: "Gramos", Litro: "Litros", Unidad: "Unidades" };

    try {
        const materias = await api("/api/materias");
        if (!materias.length) {
            UI.estado(grilla, "Aún no hay productos base registrados.");
            return;
        }
        grilla.replaceChildren(...materias.map((m) => {
            const unidad = Number(m.stock) === 1 ? m.unidad : (PLURAL[m.unidad] || m.unidad);
            const cantidad = m.stock === null ? "Sin dato" : Number(m.stock) + " " + unidad;
            return h("article", { class: "tarjeta", "data-filtrable": true },
                h("div", { class: "foto", role: "img", "aria-label": "Imagen de " + m.nombre, texto: m.nombre }),
                h("div", { class: "tarjeta-cuerpo" }, h("h3", { texto: m.nombre }),
                    h("span", { class: "dato-destacado", texto: "Cantidad total: " + cantidad }),
                    h("small", { class: "stock-linea" + (m.bajo ? " bajo" : ""),
                        texto: m.stockMinimo === null ? "Sin stock mínimo" : "Mínimo: " + Number(m.stockMinimo) + (m.bajo ? " · ¡Stock bajo!" : "") })));
        }));
    } catch (e) {
        UI.estado(grilla, e.message);
    }
})();

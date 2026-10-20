/* ===== Almacén: productos que controlan stock (todos menos los panes) ===== */
(async function () {
    await UI.listo;
    const contenedor = document.getElementById("tabla-stock");
    const resumen = document.getElementById("resumen-stock");

    // Estado de cada producto según su stock y su mínimo
    function estadoDe(p) {
        if (p.stock === 0) {
            return "Agotado";
        }
        return p.stock <= p.stockMinimo ? "Stock bajo" : "Disponible";
    }

    async function cargar() {
        try {
            const consulta = new URLSearchParams();
            if (UI.filtro.q) {
                consulta.set("q", UI.filtro.q);
            }
            if (UI.filtro.categorias.length) {
                consulta.set("categorias", UI.filtro.categorias.join(","));
            }
            const lista = await api("/api/stock?" + consulta.toString());
            const agotados = lista.filter((p) => p.stock === 0).length;
            const bajos = lista.filter((p) => p.stock > 0 && p.stock <= p.stockMinimo).length;
            resumen.innerText = lista.length + " productos · " + bajos + " con stock bajo · " + agotados + " agotados";
            if (!lista.length) {
                UI.estado(contenedor, "No hay productos que coincidan con tu búsqueda.");
                return;
            }
            contenedor.replaceChildren(UI.tabla(["Producto", "Categoría", ">Stock", ">Mínimo", "Estado"],
                lista.map((p) => [p.nombre, p.categoria, String(p.stock), String(p.stockMinimo), UI.chip(estadoDe(p))]),
                lista.length + " productos"));
        } catch (e) {
            UI.estado(contenedor, e.message);
        }
    }

    UI.alFiltrar = cargar;
    cargar();
})();

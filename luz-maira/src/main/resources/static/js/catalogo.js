/* ===== Listado de productos (lo usan: catálogo del cliente, cajera, admin y favoritos de la pantalla principal) ===== */

const Catalogo = {
    // Pide los productos según el filtro de la barra lateral y los dibuja con el mismo componente
    async cargar(contenedor, modo, alAccion) {
        try {
            const consulta = new URLSearchParams();
            if (UI.filtro.q) {
                consulta.set("q", UI.filtro.q);
            }
            if (UI.filtro.categorias.length) {
                consulta.set("categorias", UI.filtro.categorias.join(","));
            }
            // Filtros de estado del stock (solo los marca el administrador; para los demás el servidor los ignora)
            if (UI.filtro.pocoStock) {
                consulta.set("pocoStock", "true");
            }
            if (UI.filtro.desabastecidos) {
                consulta.set("desabastecidos", "true");
            }
            const lista = await api("/api/productos?" + consulta.toString());
            this.dibujar(contenedor, lista, modo, alAccion, "No hay productos que coincidan con tu búsqueda.");
            return lista;
        } catch (e) {
            UI.estado(contenedor, "No se pudieron cargar los productos: " + e.message);
            return [];
        }
    },

    async favoritos(contenedor, modo, alAccion) {
        try {
            this.dibujar(contenedor, await api("/api/productos/favoritos"), modo, alAccion, "Aún no hay productos para mostrar.");
        } catch (e) {
            UI.estado(contenedor, "No se pudieron cargar los productos: " + e.message);
        }
    },

    dibujar(contenedor, lista, modo, alAccion, textoVacio) {
        if (!lista.length) {
            UI.estado(contenedor, textoVacio);
            return;
        }
        contenedor.replaceChildren(...lista.map((p) => UI.tarjetaProducto(p, modo, alAccion)));
    }
};

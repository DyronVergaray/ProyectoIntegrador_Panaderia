/* ===== Pantalla principal: favoritos y categorías desde la BD, cajón de inicio de sesión y menú ===== */

// ---------- Cajón de inicio de sesión (el login real se activará más adelante) ----------
function abrirLogin() {
    mostrarLogin();
    document.getElementById("login-fondo").hidden = false;
    document.getElementById("login").classList.add("abierto");
}

function cerrarLogin() {
    document.getElementById("login-fondo").hidden = true;
    document.getElementById("login").classList.remove("abierto");
}

function mostrarRegistro() {
    document.getElementById("vista-login").hidden = true;
    document.getElementById("vista-registro").hidden = false;
}

function mostrarLogin() {
    document.getElementById("vista-registro").hidden = true;
    document.getElementById("vista-login").hidden = false;
}

function alternarMenu() {
    document.getElementById("nav-principal").classList.toggle("abierto");
}

document.addEventListener("keydown", function (evento) {
    if (evento.key === "Escape") {
        cerrarLogin();
    }
});

// ---------- Datos de la base de datos ----------
// Favoritos: mismo componente de tarjeta que el catálogo; el botón + pide iniciar sesión
Catalogo.favoritos(document.getElementById("favoritos"), "cliente", abrirLogin);

(async function () {
    const contenedor = document.getElementById("categorias-lista");
    try {
        const categorias = await api("/api/categorias");
        contenedor.replaceChildren(...categorias.map((c) =>
            h("a", { class: "categoria-chip", href: "catalogo.html?categoria=" + c.id, texto: c.nombre })));
    } catch (e) {
        UI.estado(contenedor, e.message);
    }
})();

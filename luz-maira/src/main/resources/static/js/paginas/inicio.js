/* ===== Pantalla principal: favoritos y categorías desde la BD, inicio de sesión, registro y menú ===== */

let usuarioActual = null;        // { id, nombre, rol, panel } si hay sesión; null si no

function mostrarMensaje(id, texto, ok) {
    console.log(texto);
    const elemento = document.getElementById(id);
    elemento.innerText = texto;
    elemento.className = ok ? "mensaje ok" : "mensaje";
}

// ---------- Cajón de sesión ----------
function abrirLogin() {
    mostrarLogin();
    document.getElementById("login-fondo").hidden = false;
    document.getElementById("login").classList.add("abierto");
}

function cerrarLogin() {
    document.getElementById("login-fondo").hidden = true;
    document.getElementById("login").classList.remove("abierto");
}

// Muestra la vista que corresponde: iniciar sesión, o el resumen si ya hay sesión
function mostrarLogin() {
    document.getElementById("vista-registro").hidden = true;
    document.getElementById("vista-login").hidden = usuarioActual !== null;
    document.getElementById("vista-sesion").hidden = usuarioActual === null;
}

function mostrarRegistro() {
    document.getElementById("vista-login").hidden = true;
    document.getElementById("vista-registro").hidden = false;
}

// ---------- Acciones ----------
async function iniciarSesion() {
    try {
        const email = document.getElementById("login-email").value.trim();
        const clave = document.getElementById("login-clave").value;
        if (email === "") {
            throw new Error("Ingresa tu correo");
        }
        if (clave === "") {
            throw new Error("Ingresa tu contraseña");
        }
        const sesion = await api("/api/login", "POST", { email: email, password: clave });
        console.log("Bienvenido " + sesion.nombre + " (" + sesion.rol + ")");
        location.href = sesion.panel;
    } catch (e) {
        mostrarMensaje("mensaje-login", e.message);
    }
}

async function registrarse() {
    try {
        const datos = {
            primerNombre: document.getElementById("reg-nombre").value.trim(),
            primerApellido: document.getElementById("reg-apellido1").value.trim(),
            segundoApellido: document.getElementById("reg-apellido2").value.trim(),
            telefono: document.getElementById("reg-telefono").value.trim(),
            email: document.getElementById("reg-email").value.trim(),
            password: document.getElementById("reg-clave").value
        };
        if (datos.primerNombre === "" || datos.primerApellido === "") {
            throw new Error("Ingresa tu nombre y tu primer apellido");
        }
        if (!/^\d{9}$/.test(datos.telefono)) {
            throw new Error("El teléfono debe tener 9 dígitos");
        }
        if (datos.email === "") {
            throw new Error("Ingresa tu correo");
        }
        if (datos.password.length < 6) {
            throw new Error("La contraseña debe tener al menos 6 caracteres");
        }
        await api("/api/registro", "POST", datos);
        const sesion = await api("/api/login", "POST", { email: datos.email, password: datos.password });
        location.href = sesion.panel;
    } catch (e) {
        mostrarMensaje("mensaje-registro", e.message);
    }
}

async function cerrarSesion() {
    await UI.cerrarSesion();
}

function irAMiPanel() {
    location.href = usuarioActual.panel;
}

// Cada vez que alguien intenta agregar un producto (o abre el carrito) desde la portada:
//  - sin sesión: aviso de SweetAlert2 y, al cerrarlo, se abre el panel de perfil (iniciar sesión / registrarse)
//  - cliente: lo lleva a su catálogo, donde arma el pedido
function irAlCatalogo() {
    if (usuarioActual === null) {
        console.log("Para hacer un pedido hay que iniciar sesión");
        if (typeof Swal === "undefined") {
            UI.toast("Por favor, regístrese o inicie sesión para hacer su pedido", false);
            abrirLogin();
            return;
        }
        Swal.fire({
            icon: "info", title: "Inicia sesión para continuar",
            text: "Por favor, regístrese o inicie sesión para hacer su pedido.",
            confirmButtonText: "Entendido", confirmButtonColor: "#d0814f"
        }).then(() => abrirLogin());
    } else if (usuarioActual.rol === "Cliente") {
        location.href = "catalogo.html";
    } else {
        UI.alerta("info", "Los pedidos desde el catálogo son solo para clientes");
    }
}

function alternarMenu() {
    document.getElementById("nav-principal").classList.toggle("abierto");
}

document.addEventListener("keydown", function (evento) {
    if (evento.key === "Escape") {
        cerrarLogin();
    }
    if (evento.key === "Enter" && document.getElementById("login").classList.contains("abierto")) {
        if (!document.getElementById("vista-login").hidden) {
            iniciarSesion();
        } else if (!document.getElementById("vista-registro").hidden) {
            registrarse();
        }
    }
});

// ---------- Datos de la base de datos ----------
const grillaCatalogo = document.getElementById("grilla-catalogo");
let categoriasPortada = [];

// Catálogo completo (público): respeta la búsqueda y las categorías elegidas
async function cargarCatalogo() {
    const lista = await Catalogo.cargar(grillaCatalogo, "cliente", irAlCatalogo);
    document.getElementById("cuenta-catalogo").innerText = lista.length + (lista.length === 1 ? " producto" : " productos");
    dibujarFiltros();
}

// Categorías: sirven como filtro del catálogo de abajo
function elegirCategoria(id) {
    const pos = UI.filtro.categorias.indexOf(id);
    if (pos >= 0) {
        UI.filtro.categorias.splice(pos, 1);
    } else {
        UI.filtro.categorias.push(id);
    }
    cargarCatalogo();
    document.getElementById("catalogo").scrollIntoView();
}

function limpiarFiltros() {
    UI.filtro.categorias.length = 0;
    UI.filtro.q = "";
    document.getElementById("buscar-catalogo").value = "";
    cargarCatalogo();
}

function dibujarFiltros() {
    document.querySelectorAll("#categorias-lista .categoria-chip[data-id]").forEach((chip) => {
        chip.setAttribute("aria-pressed", UI.filtro.categorias.includes(parseInt(chip.dataset.id, 10)));
    });
    const contenedor = document.getElementById("filtros-catalogo");
    const pastillas = UI.filtro.categorias.map((id) => {
        const categoria = categoriasPortada.find((c) => c.id === id);
        return h("button", { class: "filtro-chip", title: "Quitar este filtro", onclick: () => elegirCategoria(id) }, (categoria ? categoria.nombre : "Categoría") + " ✕");
    });
    if (pastillas.length || UI.filtro.q !== "") {
        pastillas.push(h("button", { class: "filtro-limpiar", onclick: limpiarFiltros }, "Limpiar filtros"));
    }
    contenedor.hidden = pastillas.length === 0;
    contenedor.replaceChildren(...pastillas);
}

(function () {
    const campo = document.getElementById("buscar-catalogo");
    let espera = null;
    campo.oninput = () => {
        clearTimeout(espera);
        espera = setTimeout(() => { UI.filtro.q = campo.value.trim(); cargarCatalogo(); }, 250);
    };
})();

// Favoritos: mismo componente de tarjeta que el catálogo
Catalogo.favoritos(document.getElementById("favoritos"), "cliente", irAlCatalogo);

(async function () {
    const contenedor = document.getElementById("categorias-lista");
    try {
        categoriasPortada = await api("/api/categorias");
        contenedor.replaceChildren(...categoriasPortada.map((c) =>
            h("button", { class: "categoria-chip", "data-id": c.id, "aria-pressed": false, onclick: () => elegirCategoria(c.id) }, c.nombre)));
    } catch (e) {
        UI.estado(contenedor, e.message);
    }
    cargarCatalogo();
})();

// ---------- ¿Hay sesión iniciada? ----------
(async function () {
    try {
        usuarioActual = await api("/api/me", "GET", null, true);
        document.getElementById("sesion-saludo").innerText = "Hola, " + usuarioActual.nombre;
        document.getElementById("sesion-rol").innerText = usuarioActual.rol;
        document.getElementById("btn-sesion").setAttribute("aria-label", "Mi cuenta: " + usuarioActual.nombre);
    } catch (e) {
        usuarioActual = null;
    }

    // Avisos que llegan desde la protección de páginas (SecurityConfig)
    const parametros = new URLSearchParams(location.search);
    if (parametros.get("login") === "1" && usuarioActual === null) {
        abrirLogin();
    }
    if (parametros.get("sinpermiso") === "1") {
        UI.toast("No tienes permiso para entrar a esa página con tu rol.", false);
    }
    if (parametros.has("login") || parametros.has("sinpermiso")) {
        history.replaceState(null, "", "index.html");
    }
})();

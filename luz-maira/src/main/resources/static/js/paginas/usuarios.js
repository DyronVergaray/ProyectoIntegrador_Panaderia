/* ===== Admin: crear usuarios con rol y ver los registrados ===== */
(async function () {
    await UI.listo;
    const tarjetas = document.getElementById("tarjetas-rol");
    const tabla = document.getElementById("tabla-usuarios");
    let rolActual = null;

    const formulario = UI.modalForm({
        id: "modal-usuario", titulo: "Nuevo usuario", boton: "Crear usuario",
        campos: [
            { id: "primerNombre", etiqueta: "Primer nombre", requerido: true, maxlength: 20 },
            { id: "primerApellido", etiqueta: "Primer apellido", requerido: true, maxlength: 20 },
            { id: "segundoApellido", etiqueta: "Segundo apellido (opcional)", maxlength: 20 },
            { id: "telefono", etiqueta: "Teléfono (9 dígitos)", tipo: "tel", requerido: true, maxlength: 9 },
            { id: "email", etiqueta: "Correo electrónico", tipo: "email", requerido: true, maxlength: 100 },
            { id: "password", etiqueta: "Contraseña (mínimo 6 caracteres)", tipo: "password", requerido: true, maxlength: 50 }
        ],
        alGuardar: async (datos) => {
            if (!/^\d{9}$/.test(datos.telefono)) {
                throw new Error("El teléfono debe tener exactamente 9 dígitos");
            }
            if (datos.password.length < 6) {
                throw new Error("La contraseña debe tener al menos 6 caracteres");
            }
            const respuesta = await api("/api/usuarios", "POST", { ...datos, idRol: rolActual.id });
            UI.toast(respuesta.mensaje + " (" + rolActual.nombre + ")", true);
            await cargarUsuarios();
        }
    });

    async function cargarRoles() {
        try {
            const roles = await api("/api/roles");
            tarjetas.replaceChildren(...roles.filter((r) => r.nombre !== "Cliente").map((r) => h("article", { class: "tarjeta-rol" },
                h("div", { class: "foto", role: "img", "aria-label": "Imagen del rol " + r.nombre, texto: r.nombre }),
                h("div", { class: "tarjeta-cuerpo" }, h("h3", { texto: r.nombre }), h("p", { texto: DESCRIPCION_ROL[r.nombre] || "" }),
                    h("button", { class: "pastilla-chica", onclick: () => { rolActual = r; formulario.abrir(null, "Nuevo usuario · " + r.nombre); } }, "+ Crear")))));
        } catch (e) {
            UI.estado(tarjetas, e.message);
        }
    }

    async function cargarUsuarios() {
        try {
            const usuarios = await api("/api/usuarios");
            tabla.replaceChildren(UI.tabla(["N.º", "Nombre", "Correo", "Teléfono", "Rol"],
                usuarios.map((u) => [String(u.id), u.nombreCompleto, u.email, u.telefono || "", u.rol]), usuarios.length + " usuarios"));
        } catch (e) {
            UI.estado(tabla, e.message);
        }
    }

    cargarRoles();
    cargarUsuarios();
})();

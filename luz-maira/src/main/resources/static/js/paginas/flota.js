/* ===== Admin: entregadores (cada uno con su propia cuenta) y vehículos ===== */
(async function () {
    await UI.listo;
    const tablaEntregadores = document.getElementById("tabla-entregadores");
    const tablaVehiculos = document.getElementById("tabla-vehiculos");
    let entregadores = [];
    let vehiculos = [];
    let idEditando = null;      // id del entregador o vehículo que se está editando (null = alta)

    const soloNumeros = /^\d{9}$/;

    // El estado "Ocupado" / "En ruta" lo pone el sistema al asignar un pedido: solo se muestra si ya lo tiene
    function opcionesEstado(actual, libres, enUso) {
        const lista = libres.map((e) => ({ valor: e, texto: e }));
        return actual === enUso ? [{ valor: enUso, texto: enUso + " (en una entrega)" }].concat(lista) : lista;
    }

    // ---------- Entregadores ----------
    const formNuevo = UI.modalForm({
        id: "modal-entregador-nuevo", titulo: "Nuevo entregador", boton: "Registrar",
        campos: [
            { id: "primerNombre", etiqueta: "Primer nombre", requerido: true, maxlength: 20 },
            { id: "primerApellido", etiqueta: "Primer apellido", requerido: true, maxlength: 20 },
            { id: "segundoApellido", etiqueta: "Segundo apellido (opcional)", maxlength: 20 },
            { id: "telefono", etiqueta: "Teléfono (9 dígitos)", tipo: "tel", requerido: true, maxlength: 9 },
            { id: "email", etiqueta: "Correo (su usuario para entrar)", tipo: "email", requerido: true, maxlength: 100 },
            { id: "password", etiqueta: "Contraseña (mínimo 6 caracteres)", tipo: "password", requerido: true, maxlength: 50 }
        ],
        alGuardar: async (datos) => {
            if (!soloNumeros.test(datos.telefono)) {
                throw new Error("El teléfono debe tener exactamente 9 dígitos");
            }
            if (datos.password.length < 6) {
                throw new Error("La contraseña debe tener al menos 6 caracteres");
            }
            const r = await api("/api/entregadores", "POST", datos);
            UI.toast(r.mensaje, true);
            await cargar();
        }
    });

    const formEditar = UI.modalForm({
        id: "modal-entregador-editar", titulo: "Editar entregador", boton: "Guardar",
        campos: [
            { id: "primerNombre", etiqueta: "Primer nombre", requerido: true, maxlength: 20 },
            { id: "primerApellido", etiqueta: "Primer apellido", requerido: true, maxlength: 20 },
            { id: "segundoApellido", etiqueta: "Segundo apellido (opcional)", maxlength: 20 },
            { id: "telefono", etiqueta: "Teléfono (9 dígitos)", tipo: "tel", requerido: true, maxlength: 9 },
            { id: "disponibilidad", etiqueta: "Estado", tipo: "select", requerido: true }
        ],
        alGuardar: async (datos) => {
            if (!soloNumeros.test(datos.telefono)) {
                throw new Error("El teléfono debe tener exactamente 9 dígitos");
            }
            const r = await api("/api/entregadores/" + idEditando, "PUT", datos);
            UI.toast(r.mensaje, true);
            await cargar();
        }
    });

    const formAcceso = UI.modalForm({
        id: "modal-entregador-acceso", titulo: "Crear acceso", boton: "Crear acceso",
        campos: [
            { id: "email", etiqueta: "Correo (su usuario para entrar)", tipo: "email", requerido: true, maxlength: 100 },
            { id: "password", etiqueta: "Contraseña (mínimo 6 caracteres)", tipo: "password", requerido: true, maxlength: 50 }
        ],
        alGuardar: async (datos) => {
            if (datos.password.length < 6) {
                throw new Error("La contraseña debe tener al menos 6 caracteres");
            }
            const r = await api("/api/entregadores/" + idEditando + "/acceso", "POST", datos);
            UI.toast(r.mensaje, true);
            await cargar();
        }
    });

    const formClave = UI.modalForm({
        id: "modal-entregador-clave", titulo: "Cambiar contraseña", boton: "Cambiar",
        campos: [{ id: "password", etiqueta: "Nueva contraseña (mínimo 6 caracteres)", tipo: "password", requerido: true, maxlength: 50 }],
        alGuardar: async (datos) => {
            if (datos.password.length < 6) {
                throw new Error("La contraseña debe tener al menos 6 caracteres");
            }
            const r = await api("/api/entregadores/" + idEditando + "/clave", "PUT", datos);
            UI.toast(r.mensaje, true);
        }
    });

    function nombreDe(e) {
        return [e.primerNombre, e.primerApellido, e.segundoApellido].filter(Boolean).join(" ");
    }

    function accionesEntregador(e) {
        const editar = h("button", { class: "pastilla-chica", onclick: () => {
            idEditando = e.id;
            formEditar.ponerOpciones("disponibilidad", opcionesEstado(e.disponibilidad, ["Disponible", "No disponible"], "Ocupado"));
            formEditar.abrir({ primerNombre: e.primerNombre, primerApellido: e.primerApellido, segundoApellido: e.segundoApellido || "",
                telefono: e.telefono || "", disponibilidad: e.disponibilidad }, "Editar entregador");
        } }, "✎ Editar");
        const acceso = e.email
            ? h("button", { class: "pastilla-chica", onclick: () => { idEditando = e.id; formClave.abrir({}, "Cambiar contraseña · " + e.primerNombre); } }, "🔑 Contraseña")
            : h("button", { class: "pastilla-chica", onclick: () => { idEditando = e.id; formAcceso.abrir({}, "Crear acceso · " + e.primerNombre); } }, "+ Crear acceso");
        return h("div", { class: "fila" }, editar, acceso);
    }

    function dibujarEntregadores() {
        if (!entregadores.length) {
            UI.estado(tablaEntregadores, "Aún no hay entregadores registrados.");
            return;
        }
        tablaEntregadores.replaceChildren(UI.tabla(["N.º", "Entregador", "Teléfono", "Correo de acceso", "Estado", ""],
            entregadores.map((e) => [String(e.id), nombreDe(e), e.telefono || "—", e.email || "Sin acceso",
                UI.chip(e.disponibilidad || "Disponible"), accionesEntregador(e)]), entregadores.length + " entregadores"));
    }

    // ---------- Vehículos ----------
    const formVehiculo = UI.modalForm({
        id: "modal-vehiculo", titulo: "Vehículo", boton: "Guardar",
        campos: [
            { id: "placa", etiqueta: "Placa", requerido: true, maxlength: 10 },
            { id: "marca", etiqueta: "Marca", maxlength: 50 },
            { id: "modelo", etiqueta: "Modelo / tipo", placeholder: "Moto de reparto, furgoneta…", maxlength: 50 },
            { id: "capacidad", etiqueta: "Capacidad de carga (kg)", tipo: "number", min: 0, step: "0.01" },
            { id: "estado", etiqueta: "Estado", tipo: "select", requerido: true }
        ],
        alGuardar: async (datos) => {
            const cuerpo = { placa: datos.placa, marca: datos.marca, modelo: datos.modelo,
                capacidad: datos.capacidad === "" ? null : Number(datos.capacidad), estado: datos.estado };
            const r = idEditando === null
                ? await api("/api/vehiculos", "POST", cuerpo)
                : await api("/api/vehiculos/" + idEditando, "PUT", cuerpo);
            UI.toast(r.mensaje, true);
            await cargar();
        }
    });

    function editarVehiculo(v) {
        idEditando = v.id;
        formVehiculo.ponerOpciones("estado", opcionesEstado(v.estado, ["Disponible", "Mantenimiento"], "En ruta"));
        formVehiculo.abrir({ placa: v.placa, marca: v.marca || "", modelo: v.modelo || "",
            capacidad: v.capacidad === null ? "" : v.capacidad, estado: v.estado }, "Editar vehículo");
    }

    function dibujarVehiculos() {
        if (!vehiculos.length) {
            UI.estado(tablaVehiculos, "Aún no hay vehículos registrados.");
            return;
        }
        tablaVehiculos.replaceChildren(UI.tabla(["Placa", "Marca", "Modelo", ">Capacidad (kg)", "Estado", ""],
            vehiculos.map((v) => [v.placa, v.marca || "—", v.modelo || "—", v.capacidad === null ? "—" : String(v.capacidad),
                UI.chip(v.estado || "Disponible"),
                h("button", { class: "pastilla-chica", onclick: () => editarVehiculo(v) }, "✎ Editar")]), vehiculos.length + " vehículos"));
    }

    document.getElementById("btn-nuevo-entregador").onclick = () => formNuevo.abrir({}, "Nuevo entregador");
    document.getElementById("btn-nuevo-vehiculo").onclick = () => {
        idEditando = null;
        formVehiculo.ponerOpciones("estado", opcionesEstado("", ["Disponible", "Mantenimiento"], "En ruta"));
        formVehiculo.abrir({ estado: "Disponible" }, "Nuevo vehículo");
    };

    async function cargar() {
        try {
            entregadores = await api("/api/entregadores");
            dibujarEntregadores();
        } catch (e) {
            UI.estado(tablaEntregadores, e.message);
        }
        try {
            vehiculos = await api("/api/vehiculos");
            dibujarVehiculos();
        } catch (e) {
            UI.estado(tablaVehiculos, e.message);
        }
    }

    cargar();
})();

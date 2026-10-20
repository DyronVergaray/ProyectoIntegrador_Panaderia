/* ===== Comunicación con el backend (única función que usa fetch) ===== */

// datos puede ser un objeto (se envía como JSON) o un FormData (archivos).
// silencioso = true evita redirigir al inicio cuando no hay sesión.
async function api(ruta, metodo, datos, silencioso) {
    const opciones = { method: metodo || "GET", headers: {} };
    if (datos instanceof FormData) {
        opciones.body = datos;
    } else if (datos) {
        opciones.headers["Content-Type"] = "application/json";
        opciones.body = JSON.stringify(datos);
    }

    let respuesta;
    try {
        respuesta = await fetch(ruta, opciones);
    } catch (e) {
        console.log(e.message);
        throw new Error("No se pudo conectar con el servidor. Abre la página desde http://localhost:8080");
    }

    let cuerpo = null;
    try {
        cuerpo = await respuesta.json();
    } catch (e) {
        cuerpo = null;
    }

    if (!respuesta.ok) {
        // Sesión vencida: vuelve al inicio con el panel de inicio de sesión abierto
        if (respuesta.status === 401 && !silencioso && ruta !== "/api/login") {
            location.href = "index.html?login=1";
        }
        throw new Error((cuerpo && cuerpo.error) || "Error del servidor (" + respuesta.status + ")");
    }
    return cuerpo;
}

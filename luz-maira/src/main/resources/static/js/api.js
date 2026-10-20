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

// Descarga un archivo del servidor (ej. el reporte de Excel). Usa el nombre que sugiere el servidor.
async function descargar(ruta, nombrePorDefecto) {
    let respuesta;
    try {
        respuesta = await fetch(ruta);
    } catch (e) {
        console.log(e.message);
        throw new Error("No se pudo conectar con el servidor. Abre la página desde http://localhost:8080");
    }
    if (!respuesta.ok) {
        let cuerpo = null;
        try {
            cuerpo = await respuesta.json();
        } catch (e) {
            cuerpo = null;
        }
        if (respuesta.status === 401) {
            location.href = "index.html?login=1";
        }
        throw new Error((cuerpo && cuerpo.error) || "No se pudo descargar el archivo");
    }
    const cabecera = respuesta.headers.get("Content-Disposition") || "";
    const coincidencia = cabecera.match(/filename="?([^";]+)"?/);
    const enlace = document.createElement("a");
    enlace.href = URL.createObjectURL(await respuesta.blob());
    enlace.download = coincidencia ? coincidencia[1] : nombrePorDefecto;
    document.body.append(enlace);
    enlace.click();
    enlace.remove();
    URL.revokeObjectURL(enlace.href);
}

/* ===== Comunicación con el backend (única función que usa fetch) ===== */

async function api(ruta, metodo, datos) {
    const opciones = { method: metodo || "GET", headers: { "Content-Type": "application/json" } };
    if (datos) {
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
        throw new Error((cuerpo && cuerpo.error) || "Error del servidor (" + respuesta.status + ")");
    }
    return cuerpo;
}

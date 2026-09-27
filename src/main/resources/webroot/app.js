// Cliente asincronico. Ninguna accion recarga la pagina: pido el servicio con
// fetch y solo actualizo el area de resultado o la de error.

const resultadoTexto = document.getElementById("resultado-texto");
const cajaError = document.getElementById("error");
const errorTexto = document.getElementById("error-texto");
const botones = document.querySelectorAll("button");

function mostrarResultado(texto) {
    resultadoTexto.textContent = texto;
    cajaError.hidden = true;
}

function mostrarError(texto) {
    errorTexto.textContent = texto;
    cajaError.hidden = false;
    // si no, se queda el "Esperando..." de cuando empezo la peticion
    resultadoTexto.textContent = "Sin resultado.";
}

// Bloqueo los botones mientras espero para que se note el estado de carga
function cargando(activo) {
    botones.forEach((boton) => {
        boton.disabled = activo;
    });
    if (activo) {
        resultadoTexto.textContent = "Esperando la respuesta del servidor...";
    }
}

// Pide un servicio y devuelve el texto de la respuesta, o null si algo fallo.
// Separo tres casos: falla de red, respuesta HTTP de error, y respuesta buena.
async function pedirServicio(url) {
    cargando(true);
    try {
        let respuesta;
        try {
            respuesta = await fetch(url);
        } catch (fallaDeRed) {
            // Aca no hubo respuesta HTTP: el servidor esta caido o no hay red
            mostrarError("No se pudo contactar al servidor. Verifica que este corriendo.");
            return null;
        }

        // Aca si hubo respuesta, pero puede ser 400, 404, 500...
        if (!respuesta.ok) {
            mostrarError("El servidor respondio con estado " + respuesta.status + ".");
            return null;
        }

        // Las lambdas del servidor devuelven texto, no JSON
        return await respuesta.text();
    } finally {
        // Pase lo que pase, vuelvo a habilitar los botones
        cargando(false);
    }
}

document.getElementById("btn-saludo").addEventListener("click", async (evento) => {
    evento.preventDefault();
    const nombre = document.getElementById("nombre").value.trim();
    if (nombre === "") {
        mostrarError("Escribe un nombre antes de pedir el saludo.");
        return;
    }
    const mensaje = await pedirServicio("/hello?name=" + encodeURIComponent(nombre));
    if (mensaje !== null) {
        mostrarResultado(mensaje);
    }
});

document.getElementById("btn-pi").addEventListener("click", async (evento) => {
    evento.preventDefault();
    const pi = await pedirServicio("/pi");
    if (pi !== null) {
        mostrarResultado("Pi según el servidor: " + pi);
    }
});

document.getElementById("btn-config").addEventListener("click", async (evento) => {
    evento.preventDefault();
    const config = await pedirServicio("/config");
    if (config !== null) {
        mostrarResultado(config);
    }
});

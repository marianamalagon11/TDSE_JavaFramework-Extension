package co.edu.escuelaing.httpserver.httpserver2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

// levanta el servidor de verdad en un puerto libre y le pega por http
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServidorTest {

    private static int port;
    private static Thread hilo;
    private static final HttpClient cliente = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @BeforeAll
    static void arrancarServidor() throws Exception {
        try (ServerSocket libre = new ServerSocket(0)) {
            port = libre.getLocalPort();
        }

        WebFramework.staticfiles("/webroot");

        WebFramework.get("/hello", (req, resp) -> {
            String name = req.getValue("name");
            if (name == null || name.isBlank()) {
                name = "world";
            }
            String lang = req.getValue("language");
            return "Hello " + name + (lang == null ? "" : " (" + lang + ")");
        });
        WebFramework.get("/pi", (req, resp) -> String.valueOf(Math.PI));
        WebFramework.get("/falla", (req, resp) -> {
            throw new RuntimeException("error de prueba");
        });
        WebFramework.get("/apagar", (req, resp) -> {
            WebFramework.stop();
            return "adios";
        });

        hilo = new Thread(() -> {
            try {
                WebFramework.start(port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        hilo.start();

        // espero a que el puerto acepte conexiones
        for (int i = 0; i < 50; i++) {
            try (Socket s = new Socket("localhost", port)) {
                return;
            } catch (IOException e) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("el servidor no arranco");
    }

    @AfterAll
    static void apagarSiSigueVivo() throws Exception {
        if (hilo.isAlive()) {
            try {
                raw("GET /apagar HTTP/1.1\r\n\r\n");
            } catch (IOException e) {
                // ya estaba apagado
            }
            hilo.join(5000);
        }
    }

    private static HttpResponse<String> get(String ruta) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        return cliente.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // manda texto crudo por un socket, sirve para peticiones mal formadas
    private static String raw(String peticion) throws IOException {
        try (Socket s = new Socket("localhost", port)) {
            s.setSoTimeout(10000);
            s.getOutputStream().write(peticion.getBytes(StandardCharsets.UTF_8));
            s.getOutputStream().flush();
            return new String(s.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @Order(1)
    void rutaDinamicaConVariosParametros() throws Exception {
        HttpResponse<String> respuesta = get("/hello?name=Pedro&language=en");

        assertEquals(200, respuesta.statusCode());
        assertEquals("Hello Pedro (en)", respuesta.body());
    }

    @Test
    @Order(2)
    void parametroFaltanteNoRompeNada() throws Exception {
        assertEquals("Hello world", get("/hello").body());
        assertEquals("Hello world", get("/hello?name=").body());
    }

    @Test
    @Order(3)
    void decodificaLosParametros() throws Exception {
        assertEquals("Hello Ana Maria", get("/hello?name=Ana%20Maria").body());
    }

    @Test
    @Order(4)
    void segundaRutaDinamica() throws Exception {
        HttpResponse<String> respuesta = get("/pi");

        assertEquals(200, respuesta.statusCode());
        assertEquals(String.valueOf(Math.PI), respuesta.body());
    }

    @Test
    @Order(5)
    void sirveArchivosEstaticos() throws Exception {
        HttpResponse<String> html = get("/index.html");
        assertEquals(200, html.statusCode());
        assertTrue(html.headers().firstValue("Content-Type").get().startsWith("text/html"));

        assertTrue(get("/styles.css").headers().firstValue("Content-Type").get().startsWith("text/css"));
        assertTrue(get("/app.js").headers().firstValue("Content-Type").get().startsWith("application/javascript"));
    }

    @Test
    @Order(6)
    void laImagenLlegaIgualPorHttp() throws Exception {
        byte[] original;
        try (InputStream in = getClass().getResourceAsStream("/webroot/images/logoU.png")) {
            original = in.readAllBytes();
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/images/logoU.png")).GET().build();
        HttpResponse<byte[]> respuesta = cliente.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertEquals(200, respuesta.statusCode());
        assertEquals("image/png", respuesta.headers().firstValue("Content-Type").get());
        assertArrayEquals(original, respuesta.body());
    }

    @Test
    @Order(7)
    void recursoInexistenteDevuelve404() throws Exception {
        HttpResponse<String> respuesta = get("/unknown");

        assertEquals(404, respuesta.statusCode());
        assertEquals("404 Not Found", respuesta.body());
        assertTrue(respuesta.headers().firstValue("Content-Type").get().startsWith("text/plain"));
        assertEquals(404, get("/images/nada.png").statusCode());
    }

    @Test
    @Order(8)
    void peticionesMalFormadasDevuelven400() throws Exception {
        assertTrue(raw("HOLA\r\n\r\n").startsWith("HTTP/1.1 400"));
        assertTrue(raw("GET pi HTTP/1.1\r\n\r\n").startsWith("HTTP/1.1 400"));
        assertTrue(raw("GET /pi FOO\r\n\r\n").startsWith("HTTP/1.1 400"));
        assertTrue(raw("GET /hello?name=%zz HTTP/1.1\r\n\r\n").startsWith("HTTP/1.1 400"));
        assertTrue(raw("POST /hello HTTP/1.1\r\n\r\n").startsWith("HTTP/1.1 400"));

        // despues de tanto error el servidor sigue vivo
        assertEquals(200, get("/pi").statusCode());
    }

    @Test
    @Order(9)
    void siLaLambdaFallaResponde500YSigueVivo() throws Exception {
        assertEquals(500, get("/falla").statusCode());
        assertEquals(200, get("/pi").statusCode());
    }

    @Test
    @Order(10)
    void clienteCalladoNoBloqueaParaSiempre() throws Exception {
        // este cliente se conecta y no manda nada, el servidor lo descarta a los 5 segundos
        try (Socket callado = new Socket("localhost", port)) {
            assertTimeout(Duration.ofSeconds(15), () -> {
                assertEquals(200, get("/pi").statusCode());
            });
        }
    }

    @Test
    @Order(11)
    void apagadoGradual() throws Exception {
        HttpResponse<String> respuesta = get("/apagar");

        // la respuesta llega completa aunque el servidor ya se este apagando
        assertEquals(200, respuesta.statusCode());
        assertEquals("adios", respuesta.body());

        hilo.join(5000);
        assertFalse(hilo.isAlive());
        assertThrows(ConnectException.class, () -> new Socket("localhost", port).close());
    }
}

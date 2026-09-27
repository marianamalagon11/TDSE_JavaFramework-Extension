package co.edu.escuelaing.httpserver.httpserver2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestTest {

    @Test
    void leeVariosParametros() {
        Request request = new Request("/hello", Map.of("name", "Pedro", "language", "en"));

        assertEquals("/hello", request.getPath());
        assertEquals("Pedro", request.getValue("name"));
        assertEquals("en", request.getValue("language"));
    }

    @Test
    void parametroAusenteDevuelveNull() {
        Request request = new Request("/hello", Map.of("name", "Pedro"));

        assertNull(request.getValue("language"));
    }

    @Test
    void responseTieneValoresPorDefecto() {
        Response response = new Response();

        assertEquals(200, response.getStatus());
        assertEquals("text/html; charset=utf-8", response.getContentType());
    }
}

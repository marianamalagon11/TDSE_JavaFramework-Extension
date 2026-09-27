package co.edu.escuelaing.httpserver.httpserver2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import org.junit.jupiter.api.Test;

class RouterTest {

    @Test
    void encuentraUnaRutaRegistrada() throws Exception {
        WebService servicio = (req, resp) -> "hola";
        Router.register("/router-test", servicio);

        WebService encontrado = Router.find("/router-test");

        assertSame(servicio, encontrado);
        assertEquals("hola", encontrado.invoke(new Request("/router-test", Map.of()), new Response()));
    }

    @Test
    void rutaNoRegistradaDevuelveNull() {
        assertNull(Router.find("/ruta-que-nadie-registro"));
    }
}

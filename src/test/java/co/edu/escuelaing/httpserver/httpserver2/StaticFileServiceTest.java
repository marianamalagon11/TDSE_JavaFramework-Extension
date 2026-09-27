package co.edu.escuelaing.httpserver.httpserver2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.junit.jupiter.api.Test;

class StaticFileServiceTest {

    @Test
    void sirveElHtml() throws Exception {
        StaticFileService.StaticFile archivo = StaticFileService.load("/index.html");

        assertNotNull(archivo);
        assertEquals("text/html; charset=utf-8", archivo.contentType());
        assertTrue(archivo.content().length > 0);
    }

    @Test
    void laRaizSirveElIndex() throws Exception {
        StaticFileService.StaticFile raiz = StaticFileService.load("/");
        StaticFileService.StaticFile index = StaticFileService.load("/index.html");

        assertArrayEquals(index.content(), raiz.content());
    }

    @Test
    void deduceElTipoDeContenidoPorExtension() throws Exception {
        assertEquals("text/css; charset=utf-8", StaticFileService.load("/styles.css").contentType());
        assertEquals("application/javascript; charset=utf-8", StaticFileService.load("/app.js").contentType());
        assertEquals("image/png", StaticFileService.load("/images/logoU.png").contentType());
        assertEquals("image/jpeg", StaticFileService.load("/images/fotoU.jpeg").contentType());
    }

    @Test
    void laImagenLlegaIntactaComoBytes() throws Exception {
        byte[] original;
        try (InputStream in = getClass().getResourceAsStream("/webroot/images/logoU.png")) {
            original = in.readAllBytes();
        }

        StaticFileService.StaticFile archivo = StaticFileService.load("/images/logoU.png");

        assertArrayEquals(original, archivo.content());
        // los primeros bytes de un png
        assertEquals((byte) 0x89, archivo.content()[0]);
        assertEquals((byte) 0x50, archivo.content()[1]);
    }

    @Test
    void archivoInexistenteDevuelveNull() throws Exception {
        assertNull(StaticFileService.load("/no-existe.html"));
        assertNull(StaticFileService.load("/images/nada.png"));
    }

    @Test
    void unaCarpetaNoSeSirve() throws Exception {
        assertNull(StaticFileService.load("/images"));
    }

    @Test
    void noPermiteSalirseDeLaCarpeta() throws Exception {
        assertNull(StaticFileService.load("/../pom.xml"));
        assertNull(StaticFileService.load("/images/../index.html"));
    }
}

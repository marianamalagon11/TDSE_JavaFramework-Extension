package co.edu.escuelaing.httpserver.httpserver2;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class StaticFileService {

    // carpeta dentro de resources donde estan los archivos
    private static String location = "/webroot";

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "application/javascript; charset=utf-8",
            "json", "application/json; charset=utf-8",
            "txt", "text/plain; charset=utf-8",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "gif", "image/gif",
            "svg", "image/svg+xml");

    public static void setLocation(String path) {
        location = path;
    }

    // devuelve el archivo o null si no existe
    public static StaticFile load(String requestPath) throws IOException {
        if (requestPath.equals("/")) {
            requestPath = "/index.html";
        }

        // para que nadie pueda salirse de la carpeta con ../
        if (requestPath.contains("..")) {
            return null;
        }

        // sin extension seria una carpeta, no un archivo
        String fileName = requestPath.substring(requestPath.lastIndexOf('/') + 1);
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }

        try (InputStream in = StaticFileService.class.getResourceAsStream(location + requestPath)) {
            if (in == null) {
                return null;
            }
            String extension = fileName.substring(dot + 1).toLowerCase();
            String contentType = CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
            // leo bytes y no texto para que las imagenes no se dañen
            return new StaticFile(in.readAllBytes(), contentType);
        }
    }

    public record StaticFile(byte[] content, String contentType) {
    }
}

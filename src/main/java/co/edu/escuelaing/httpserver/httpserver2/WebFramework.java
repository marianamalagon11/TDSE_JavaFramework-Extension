package co.edu.escuelaing.httpserver.httpserver2;

import java.io.IOException;

public class WebFramework {

    public static void get(String route, WebService ws) {
        Router.register(route, ws);
    }

    public static void staticfiles(String path) {
        StaticFileService.setLocation(path);
    }

    // el puerto sale de la variable PORT, si no existe uso 8080
    public static void start() throws IOException {
        start(readPort());
    }

    private static int readPort() {
        String portValue = System.getenv("PORT");
        if (portValue == null || portValue.isBlank()) {
            return 8080;
        }
        try {
            return Integer.parseInt(portValue.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("PORT no es un numero valido: " + portValue);
        }
    }

    public static void start(int port) throws IOException {
        HttpServer2.start(port);
    }

    public static void stop() {
        HttpServer2.stop();
    }
}

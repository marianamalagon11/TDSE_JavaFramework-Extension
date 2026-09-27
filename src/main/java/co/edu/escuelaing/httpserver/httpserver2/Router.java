package co.edu.escuelaing.httpserver.httpserver2;

import java.util.HashMap;
import java.util.Map;

public class Router {

    // ruta -> lambda que la atiende
    private static final Map<String, WebService> routes = new HashMap<>();

    public static void register(String path, WebService service) {
        routes.put(path, service);
    }

    // devuelve null si la ruta no esta registrada
    public static WebService find(String path) {
        return routes.get(path);
    }
}

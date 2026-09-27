package co.edu.escuelaing.app;

import static co.edu.escuelaing.httpserver.httpserver2.WebFramework.*;

public class Application {

    public static void main(String[] args) throws Exception {

        // configuracion que viene de variables de entorno
        String greetingPrefix = System.getenv().getOrDefault("GREETING_PREFIX", "Hello");
        String appEnv = System.getenv().getOrDefault("APP_ENV", "development");

        System.out.println("APP_ENV=" + appEnv);

        staticfiles("/webroot");

        get("/hello", (req, resp) -> {
            String name = req.getValue("name");
            if (name == null || name.isBlank()) {
                name = "world";
            }
            // en texto plano para que el navegador no interprete el nombre como html
            resp.setContentType("text/plain; charset=utf-8");
            return greetingPrefix + " " + name;
        });

        get("/pi", (req, resp) -> String.valueOf(Math.PI));

        // sirve para mostrar en el README que las variables se leyeron
        get("/config", (req, resp) -> {
            resp.setContentType("text/plain; charset=utf-8");
            return "APP_ENV=" + appEnv + "\nGREETING_PREFIX=" + greetingPrefix;
        });

        // solo en desarrollo, en la nube nadie debe poder apagar el servidor
        if (appEnv.equals("development")) {
            get("/shutdown", (req, resp) -> {
                stop();
                resp.setContentType("text/plain; charset=utf-8");
                return "Server will stop after this response.";
            });
        }

        start();
    }
}

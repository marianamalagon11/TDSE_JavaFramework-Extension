package co.edu.escuelaing.httpserver.httpserver2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HttpServer2 {

    private static final String TEXT = "text/plain; charset=utf-8";

    // stop() cierra el ServerSocket para sacar al accept() del bloqueo
    private static volatile boolean running = false;
    private static ServerSocket serverSocket;
    private static ExecutorService requestExecutor;

    public static void start(int port) throws IOException {
        running = true;
        // un thread virtual por peticion, asi cada conexion se atiende en paralelo
        requestExecutor = Executors.newVirtualThreadPerTaskExecutor();

        try (ServerSocket socket = new ServerSocket(port)) {
            serverSocket = socket;
            System.out.println("Servidor escuchando en el puerto " + port);

            while (running) {
                try {
                    Socket clientSocket = socket.accept();
                    requestExecutor.execute(() -> attendConnection(clientSocket));
                } catch (IOException e) {
                    // el stop() cierra el socket a proposito, eso no es un error real
                    if (running) {
                        System.err.println("Error aceptando una conexion: " + e.getMessage());
                    }
                }
            }
        }

        // deja de aceptar peticiones nuevas y espera a que terminen las que ya estaban en curso
        requestExecutor.shutdown();
        try {
            requestExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Servidor detenido.");
    }

    private static void attendConnection(Socket clientSocket) {
        try (clientSocket) {
            // si el cliente se queda callado no me quedo esperando para siempre
            clientSocket.setSoTimeout(5000);
            handleRequest(clientSocket);
        } catch (Exception e) {
            // cualquier error de una peticion no debe tumbar el servidor
            System.err.println("Error atendiendo una conexion: " + e.getMessage());
        }
    }

    public static void stop() {
        running = false;
        try {
            // cierra el ServerSocket de inmediato para que accept() deje de bloquear
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando el servidor: " + e.getMessage());
        }
    }

    private static void handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        OutputStream out = clientSocket.getOutputStream();

        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            return;
        }

        // leo los headers hasta la linea vacia aunque no los use todavia
        String headerLine;
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
        }

        System.out.println("Peticion recibida: " + requestLine);

        String[] parts = requestLine.split(" ");
        if (parts.length != 3 || !parts[1].startsWith("/") || !parts[2].startsWith("HTTP/")) {
            sendResponse(out, 400, TEXT, "400 Bad Request");
            return;
        }

        String method = parts[0];
        String fullPath = parts[1];

        if (!method.equals("GET")) {
            sendResponse(out, 400, TEXT, "400 Bad Request: solo se soporta GET");
            return;
        }

        Request request;
        try {
            request = parseRequestTarget(fullPath);
        } catch (IllegalArgumentException e) {
            // por ejemplo un %zz que no se puede decodificar
            sendResponse(out, 400, TEXT, "400 Bad Request: parametros mal codificados");
            return;
        }
        Response response = new Response();

        // primero busco una ruta dinamica registrada
        WebService service = Router.find(request.getPath());
        if (service != null) {
            try {
                String body = service.invoke(request, response);
                sendResponse(out, response.getStatus(), response.getContentType(),
                        body == null ? "" : body);
            } catch (Exception e) {
                // si el lambda revienta respondo 500 y el servidor sigue vivo
                System.err.println("Error en el handler de " + request.getPath() + ": " + e.getMessage());
                sendResponse(out, 500, TEXT, "500 Internal Server Error");
            }
            return;
        }

        // si no es ruta dinamica pruebo con un archivo estatico
        StaticFileService.StaticFile file = StaticFileService.load(request.getPath());
        if (file != null) {
            sendResponse(out, 200, file.contentType(), file.content());
            return;
        }

        sendResponse(out, 404, TEXT, "404 Not Found");
    }

    // separa "/hello?name=mari" en el path y los parametros
    private static Request parseRequestTarget(String fullPath) {
        String path;
        Map<String, String> queryParams = new HashMap<>();

        int qIndex = fullPath.indexOf('?');
        if (qIndex >= 0) {
            path = fullPath.substring(0, qIndex);
            parseQueryString(fullPath.substring(qIndex + 1), queryParams);
        } else {
            path = fullPath;
        }

        return new Request(path, queryParams);
    }

    private static void parseQueryString(String queryString, Map<String, String> queryParams) {
        for (String pair : queryString.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int eqIndex = pair.indexOf('=');
            if (eqIndex >= 0) {
                String key = URLDecoder.decode(pair.substring(0, eqIndex), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(eqIndex + 1), StandardCharsets.UTF_8);
                queryParams.put(key, value);
            } else {
                queryParams.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
    }

    private static void sendResponse(OutputStream out, int status, String contentType, String body)
            throws IOException {
        sendResponse(out, status, contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendResponse(OutputStream out, int status, String contentType, byte[] body)
            throws IOException {
        String statusText = switch (status) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "OK";
        };

        // Content-Length va en bytes, no en caracteres
        String headers = "HTTP/1.1 " + status + " " + statusText + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";

        out.write(headers.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }
}

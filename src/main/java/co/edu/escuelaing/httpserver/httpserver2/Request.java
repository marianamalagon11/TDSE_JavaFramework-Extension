package co.edu.escuelaing.httpserver.httpserver2;

import java.util.Map;


//Representa la peticion HTTP que le llega a un WebService: la ruta pedida
//y los parametros de query string ya parseados.
 
public class Request {

    private final String path;
    private final Map<String, String> queryParams;

    public Request(String path, Map<String, String> queryParams) {
        this.path = path;
        this.queryParams = queryParams;
    }

    public String getPath() {
        return path;
    }

    // Devuelve el valor del parametro, o null si no vino en la peticion
    public String getValue(String name) {
        return queryParams.get(name);
    }
}

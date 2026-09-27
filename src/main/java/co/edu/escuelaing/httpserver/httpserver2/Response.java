package co.edu.escuelaing.httpserver.httpserver2;


//Representa la respuesta HTTP que un WebService puede ajustar antes de
//devolver el cuerpo (por ejemplo, cambiar el status o el content-type).
//Si el lambda no toca nada, queda 200 OK con text/html.
 
public class Response {

    private int status = 200;
    private String contentType = "text/html; charset=utf-8";

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}

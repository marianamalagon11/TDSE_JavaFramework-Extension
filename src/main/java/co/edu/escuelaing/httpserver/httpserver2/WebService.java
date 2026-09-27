package co.edu.escuelaing.httpserver.httpserver2;


//Contrato que cumple cada lambda registrada con WebFramework.get(...).
//Recibe la peticion y una respuesta que puede ajustar, y devuelve el
//texto que se manda como cuerpo del HTTP response.
 
@FunctionalInterface
public interface WebService {

    String invoke(Request request, Response response) throws Exception;
}

# TDSE Framework Extension: concurrencia, apagado gradual y despliegue

Extensión de mi propio framework web en Java (sin Spring), tomado del repositorio [TDSE_JavaFramework](https://github.com/marianamalagon11/TDSE_JavaFramework). El objetivo de este repositorio es que el framework soporte manejo concurrente de peticiones, apagado gradual, puerto configurable por variable de entorno, ejecución en Docker y despliegue en EC2.

> Este README se va completando a medida que avanza el trabajo. Las secciones de Docker y de la nube se agregan cuando esos pasos estén verificados.

## Estado del avance

| Requisito | Estado |
|---|---|
| Framework base importado | Hecho |
| Manejo concurrente de peticiones | Hecho (threads virtuales de Java 21) |
| Apagado gradual | Hecho (cierra el socket de inmediato y espera las peticiones en curso) |
| Puerto por variable de entorno | Ya existía en el framework base (`PORT`) |
| Ejecución en Docker | Dockerfile heredado, pendiente de verificar con la extensión |
| Despliegue en EC2 | Pendiente |

## Estado actual del framework

El framework base es un servidor HTTP escrito en Java sin librerías externas. El desarrollador registra servicios GET con lambdas y no toca el ciclo de conexión.

- Sirve archivos estáticos desde `src/main/resources/webroot`.
- Registra servicios GET con lambdas: `get("/hello", (req, resp) -> ...)`.
- Extrae valores del query string con `req.getValue("name")`.
- Resuelve cada petición en este orden: ruta dinámica, archivo estático, 404.
- Responde 400, 404 o 500 ante errores sin caerse.
- Lee `PORT`, `APP_ENV` y `GREETING_PREFIX` desde variables de entorno.
- Solo soporta el método GET.

### Arquitectura

```
Application                      registra rutas y configuración
    |
WebFramework                     API pública: get(), staticfiles(), start(), stop()
    |
    +--> Router                  guarda la tabla ruta -> lambda
    |
HttpServer2                      acepta conexiones, interpreta la petición, envía la respuesta
    |
    +--> Request / Response      datos HTTP que ve la lambda
    |
    +--> StaticFileService       entrega archivos cuando no hay ruta dinámica
```

### Variables de entorno

| Variable | Propósito | Valor por defecto |
|---|---|---|
| `PORT` | Puerto en el que escucha el servidor | `8080` |
| `GREETING_PREFIX` | Texto con el que empieza la respuesta de `/hello` | `Hello` |
| `APP_ENV` | Ambiente. La ruta `/shutdown` solo existe si vale `development` | `development` |

## Cambios de esta extensión

El único archivo de código modificado es [HttpServer2.java](src/main/java/co/edu/escuelaing/httpserver/httpserver2/HttpServer2.java).

### Antes: servidor secuencial

El mismo hilo aceptaba la conexión y la procesaba completa antes de aceptar la siguiente. Una conexión lenta, o un cliente que no enviaba nada, bloqueaba a todos los demás hasta 5 segundos (el timeout de lectura).

```java
while (running) {
    try (Socket clientSocket = serverSocket.accept()) {
        clientSocket.setSoTimeout(5000);
        handleRequest(clientSocket);
    }
}
```

### Ahora: un thread virtual por conexión

El ciclo principal solo acepta conexiones y las despacha a un `ExecutorService` de threads virtuales (Java 21). Cada petición se procesa en paralelo y el ciclo vuelve de inmediato a `accept()`.

```java
requestExecutor = Executors.newVirtualThreadPerTaskExecutor();

while (running) {
    Socket clientSocket = socket.accept();
    requestExecutor.execute(() -> attendConnection(clientSocket));
}
```

### Apagado gradual

`stop()` ahora hace dos cosas:

1. Pone `running = false` (la variable es `volatile`, porque `stop()` corre en el thread virtual de `/shutdown` y el ciclo principal la lee desde otro thread).
2. Cierra el `ServerSocket` de inmediato. Así el `accept()` bloqueado lanza una excepción y el ciclo termina sin esperar a una conexión nueva.

Al salir del ciclo, `start()` llama a `requestExecutor.shutdown()` (no acepta tareas nuevas, pero no corta las que están corriendo) y luego a `awaitTermination(30, SECONDS)`. Solo después imprime "Servidor detenido.". Así ninguna petición queda a medias, incluida la de `/shutdown`, que alcanza a enviar su respuesta completa.

## Cómo compilar y ejecutar

Requisitos: Java 21 y Maven 3.9 o superior.

Compilar, correr las pruebas y empaquetar:

```bash
mvn clean package
```

Ejecutar el JAR generado:

```bash
PORT=8085 java -jar target/httpServer2-1.0-SNAPSHOT.jar
```

En PowerShell:

```powershell
$env:PORT="8085"; java -jar target/httpServer2-1.0-SNAPSHOT.jar
```

Luego abrir `http://localhost:8085`.

## Evidencia de progreso

### Commits

| Commit | Descripción |
|---|---|
| [`105d2de`](https://github.com/marianamalagon11/TDSE_JavaFramework-Extension/commit/105d2de) | Import base framework from TDSE_JavaFramework |

Los commits de la extensión se agregan aquí a medida que se hacen.

### Pruebas automatizadas

Las 23 pruebas JUnit del framework pasan con el servidor concurrente (0 fallos). La clase `ServidorTest`, que levanta el servidor real y le pega por HTTP, pasó de tardar unos 6 segundos con el servidor secuencial a 0.87 segundos. La causa es la prueba `clienteCalladoNoBloqueaParaSiempre`: un cliente se conecta y no envía nada, y antes eso bloqueaba la siguiente petición hasta el timeout de 5 segundos. Ahora esa conexión espera en su propio thread virtual y la petición siguiente se responde de inmediato.

### Compilación con Maven

`mvn clean package` termina con `BUILD SUCCESS` y genera el JAR ejecutable con la extensión aplicada:

![Build exitoso](images/02-mvn-package.png)

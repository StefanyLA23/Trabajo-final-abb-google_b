import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Un solo servidor local entrega los tres archivos web y la API JSON del ABB. */
public final class ServidorWeb implements AutoCloseable {
    private final HttpServer servidor;
    private final ExecutorService ejecutor;
    private final ServicioABBWeb servicio;
    private final Path carpetaWeb;

    public ServidorWeb(int puerto, Path proyecto) throws IOException {
        carpetaWeb = proyecto.resolve("web").toRealPath();
        servicio = new ServicioABBWeb(proyecto.resolve("terminos.csv"));
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", puerto), 0);
        ejecutor = Executors.newFixedThreadPool(4);
        servidor.setExecutor(ejecutor);
        servidor.createContext("/", this::atender);
    }

    public void iniciar() { servidor.start(); }
    public int getPuerto() { return servidor.getAddress().getPort(); }

    public static void main(String[] args) {
        try {
            ServidorWeb servidor = new ServidorWeb(8080, Path.of("."));
            Runtime.getRuntime().addShutdownHook(new Thread(servidor::close));
            servidor.iniciar();
            System.out.println("Google ABB disponible en http://localhost:8080");
            System.out.println("Servidor limitado a 127.0.0.1. Para detener: Ctrl+C.");
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor. Ejecuta desde la carpeta del proyecto y verifica el puerto 8080.");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private void atender(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            try {
                validarOrigen(intercambio);
                String ruta = intercambio.getRequestURI().getPath();
                if (ruta.startsWith("/api/")) {
                    Map<String, Object> respuesta = servicio.atender(intercambio.getRequestMethod(), ruta,
                            parametros(intercambio.getRequestURI().getRawQuery()), cuerpo(intercambio));
                    int codigo = Boolean.TRUE.equals(respuesta.get("creado")) ? 201 : 200;
                    responderJSON(intercambio, codigo, respuesta);
                } else {
                    archivo(intercambio, ruta);
                }
            } catch (ErrorHTTP e) {
                if (e.getCodigo() == 405) intercambio.getResponseHeaders().set("Allow", permitidos(intercambio.getRequestURI().getPath()));
                responderJSON(intercambio, e.getCodigo(), Map.of("error", e.getMessage()));
            } catch (IllegalArgumentException e) {
                responderJSON(intercambio, 400, Map.of("error", e.getMessage()));
            } catch (Exception e) {
                System.err.println("Error al atender petición: " + e.getMessage());
                responderJSON(intercambio, 500, Map.of("error", "No se pudo completar la operación. El servidor sigue disponible."));
            }
        }
    }

    private String permitidos(String ruta) {
        if (ruta.equals("/api/termino")) return "GET, PUT, DELETE";
        if (ruta.equals("/api/terminos")) return "GET, POST";
        if (ruta.equals("/api/arbol")) return "GET, DELETE";
        if (ruta.equals("/api/buscar") || ruta.equals("/api/restaurar")) return "POST";
        return ruta.startsWith("/api/") ? "GET" : "GET, HEAD";
    }

    /** Evita uso desde páginas de otro origen; no habilita CORS ni sirve fuera de loopback. */
    private void validarOrigen(HttpExchange intercambio) {
        String host = intercambio.getRequestHeaders().getFirst("Host");
        String local = "localhost:" + getPuerto();
        String numerico = "127.0.0.1:" + getPuerto();
        if (!local.equalsIgnoreCase(host) && !numerico.equals(host)) throw new ErrorHTTP(403, "Host no permitido.");
        String origen = intercambio.getRequestHeaders().getFirst("Origin");
        if (origen != null && !origen.equals("http://" + local) && !origen.equals("http://" + numerico)) {
            throw new ErrorHTTP(403, "Origen no permitido.");
        }
    }

    private Map<String, Object> cuerpo(HttpExchange intercambio) throws IOException {
        byte[] bytes = intercambio.getRequestBody().readNBytes(65537);
        if (bytes.length > 65536) throw new ErrorHTTP(413, "La petición supera los 64 KiB permitidos.");
        if (bytes.length == 0) return Map.of();
        String tipo = intercambio.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
            throw new ErrorHTTP(415, "Envía el cuerpo como application/json.");
        }
        try {
            String texto = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            return Json.leerObjeto(texto);
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("El JSON debe utilizar UTF-8 válido.");
        }
    }

    private Map<String, String> parametros(String consulta) {
        Map<String, String> resultado = new LinkedHashMap<>();
        if (consulta == null || consulta.isEmpty()) return resultado;
        if (consulta.length() > 4096) throw new ErrorHTTP(414, "La consulta es demasiado larga.");
        for (String par : consulta.split("&")) {
            String[] partes = par.split("=", 2);
            String clave = URLDecoder.decode(partes[0], StandardCharsets.UTF_8);
            String valor = partes.length < 2 ? "" : URLDecoder.decode(partes[1], StandardCharsets.UTF_8);
            if (resultado.containsKey(clave)) throw new IllegalArgumentException("Parámetro repetido: " + clave);
            if (valor.length() > 200) throw new IllegalArgumentException("Cada parámetro admite hasta 200 caracteres.");
            resultado.put(clave, valor);
        }
        return resultado;
    }

    /** Lista cerrada: nunca transforma una ruta del navegador en un archivo arbitrario. */
    private void archivo(HttpExchange intercambio, String ruta) throws IOException {
        if (!Set.of("GET", "HEAD").contains(intercambio.getRequestMethod())) throw new ErrorHTTP(405, "Método no permitido.");
        String nombre;
        String tipo;
        switch (ruta) {
            case "/": case "/index.html": nombre = "index.html"; tipo = "text/html"; break;
            case "/styles.css": nombre = "styles.css"; tipo = "text/css"; break;
            case "/app.js": nombre = "app.js"; tipo = "text/javascript"; break;
            default: throw new ErrorHTTP(404, "Archivo no disponible.");
        }
        Path real = carpetaWeb.resolve(nombre).toRealPath();
        if (!real.startsWith(carpetaWeb) || !Files.isRegularFile(real)) throw new ErrorHTTP(404, "Archivo no disponible.");
        byte[] contenido = Files.readAllBytes(real);
        cabeceras(intercambio, tipo + "; charset=utf-8");
        if (intercambio.getRequestMethod().equals("HEAD")) {
            intercambio.getResponseHeaders().set("Content-Length", String.valueOf(contenido.length));
            intercambio.sendResponseHeaders(200, -1);
        } else {
            intercambio.sendResponseHeaders(200, contenido.length);
            intercambio.getResponseBody().write(contenido);
        }
    }

    private void responderJSON(HttpExchange intercambio, int codigo, Map<String, Object> respuesta) throws IOException {
        byte[] bytes = Json.escribir(respuesta).getBytes(StandardCharsets.UTF_8);
        cabeceras(intercambio, "application/json; charset=utf-8");
        intercambio.sendResponseHeaders(codigo, bytes.length);
        intercambio.getResponseBody().write(bytes);
    }

    private void cabeceras(HttpExchange intercambio, String tipo) {
        intercambio.getResponseHeaders().set("Content-Type", tipo);
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        intercambio.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        intercambio.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
    }

    @Override
    public void close() {
        servidor.stop(0);
        ejecutor.shutdownNow();
    }
}

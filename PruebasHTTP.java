import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Pruebas HTTP reales en un puerto efímero; no alteran la sesión del puerto 8080. */
public class PruebasHTTP {
    private static final HttpClient CLIENTE = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static String base;
    private static int aprobadas;
    private static int fallidas;
    private static int verificaciones;

    private interface Prueba { void ejecutar() throws Exception; }

    public static void main(String[] args) throws Exception {
        try (ServidorWeb servidor = new ServidorWeb(0, Path.of("."))) {
            servidor.iniciar();
            base = "http://127.0.0.1:" + servidor.getPuerto();
            ejecutar("HTML entregado y cinco secciones", PruebasHTTP::html);
            ejecutar("CSS y JavaScript locales con MIME correcto", PruebasHTTP::recursos);
            ejecutar("HEAD y cabeceras de respuesta", PruebasHTTP::cabeceras);
            ejecutar("Carga de 30 registros y estadísticas", PruebasHTTP::estado);
            ejecutar("Consulta pura sin incrementar", PruebasHTTP::consultaPura);
            ejecutar("Búsqueda registra popularidad y fecha", PruebasHTTP::buscar);
            ejecutar("Búsquedas inexistentes devuelven 404", PruebasHTTP::ausentes);
            ejecutar("Inserción y normalización", PruebasHTTP::insertar);
            ejecutar("Duplicado no reemplaza ni suma contador entrante", PruebasHTTP::duplicado);
            ejecutar("Actualización conserva la clave", PruebasHTTP::actualizar);
            ejecutar("Actualización inválida no modifica parcialmente", PruebasHTTP::actualizacionAtomica);
            ejecutar("Eliminar hoja actualiza la estructura", PruebasHTTP::eliminarHoja);
            ejecutar("Eliminar nodo con un hijo", PruebasHTTP::eliminarUnHijo);
            ejecutar("Eliminar dos hijos conserva sucesor completo", PruebasHTTP::eliminarDosHijos);
            ejecutar("Eliminar raíz con dos hijos", PruebasHTTP::eliminarRaiz);
            ejecutar("Eliminar inexistente devuelve 404", PruebasHTTP::eliminarAusente);
            ejecutar("Autocompletado con poda sin popularidad", PruebasHTTP::autocompletar);
            ejecutar("Top 5 por popularidad y empates", PruebasHTTP::top);
            ejecutar("Inorden", () -> recorrido("inorden", "a,b,c,d,e,f,g"));
            ejecutar("Preorden", () -> recorrido("preorden", "d,b,a,c,f,e,g"));
            ejecutar("Postorden", () -> recorrido("postorden", "a,c,b,e,g,f,d"));
            ejecutar("Por niveles FIFO", () -> recorrido("niveles", "d,b,f,a,c,e,g"));
            ejecutar("Mínimo y máximo", PruebasHTTP::extremos);
            ejecutar("Nivel existente e inexistente", PruebasHTTP::nivel);
            ejecutar("Vecinos estrictos y límites", PruebasHTTP::vecinos);
            ejecutar("Rango inclusivo con poda", PruebasHTTP::rango);
            ejecutar("Rango inválido devuelve 400", PruebasHTTP::rangoInvalido);
            ejecutar("JSON de estructura conserva enlaces reales", PruebasHTTP::estructura);
            ejecutar("Camino de búsqueda proviene de Java", PruebasHTTP::camino);
            ejecutar("Fotografía inmutable y separada del ABB", PruebasHTTP::fotografia);
            ejecutar("Restaurar dos veces no acumula popularidad", PruebasHTTP::restaurar);
            ejecutar("Vaciar exige confirmación explícita", PruebasHTTP::confirmaciones);
            ejecutar("Consultas sobre árbol vacío", PruebasHTTP::vacio);
            ejecutar("Restauración después de vaciar", PruebasHTTP::restaurarVacio);
            ejecutar("Experimento real y aislado de la sesión", PruebasHTTP::experimento);
            ejecutar("Datos inválidos rechazados en Java", PruebasHTTP::datosInvalidos);
            ejecutar("Clave inmutable también en la API", PruebasHTTP::claveInmutable);
            ejecutar("JSON malformado, duplicados y profundidad", PruebasHTTP::jsonInvalido);
            ejecutar("JSON escapa Unicode, comillas y controles", PruebasHTTP::jsonEscapes);
            ejecutar("Valores con HTML permanecen como datos", PruebasHTTP::contenidoHTML);
            ejecutar("Tipos de contenido y tamaño limitados", PruebasHTTP::limites);
            ejecutar("UTF-8 inválido rechazado", PruebasHTTP::utf8Invalido);
            ejecutar("Métodos incorrectos devuelven 405", PruebasHTTP::metodos);
            ejecutar("Rutas internas no se sirven", PruebasHTTP::rutasInternas);
            ejecutar("Orígenes externos rechazados", PruebasHTTP::origen);
            ejecutar("Parámetros repetidos y vacíos rechazados", PruebasHTTP::parametros);
            ejecutar("Búsquedas concurrentes no pierden incrementos", PruebasHTTP::concurrencia);
            ejecutar("CSV y 57 pruebas originales intactos", PruebasHTTP::integridad);
            System.out.println("\nRESULTADO HTTP: " + aprobadas + " aprobadas, " + fallidas
                    + " fallidas; " + verificaciones + " verificaciones.");
        }
        if (fallidas != 0) throw new AssertionError("Hay pruebas HTTP fallidas.");
    }

    private static void ejecutar(String nombre, Prueba prueba) {
        try {
            codigo(200, pedir("POST", "/api/restaurar", "{\"confirmacion\":\"RESTAURAR\"}"));
            prueba.ejecutar();
            aprobadas++;
            System.out.println("OK - " + nombre);
        } catch (Exception | AssertionError e) {
            fallidas++;
            System.out.println("FALLO - " + nombre + ": " + e);
        }
    }

    private static void comprobar(boolean condicion, String mensaje) {
        verificaciones++;
        if (!condicion) throw new AssertionError(mensaje);
    }

    private static void igual(Object esperado, Object real) {
        comprobar(esperado == null ? real == null : esperado.equals(real), "Esperado: " + esperado + "; obtenido: " + real);
    }

    private static void codigo(int esperado, HttpResponse<String> respuesta) {
        igual(esperado, respuesta.statusCode());
    }

    private static Map<?, ?> mapa(Object dato) {
        comprobar(dato instanceof Map<?, ?>, "Se esperaba objeto JSON");
        return (Map<?, ?>) dato;
    }

    private static List<?> lista(Object dato) {
        comprobar(dato instanceof List<?>, "Se esperaba lista JSON");
        return (List<?>) dato;
    }

    private static Map<?, ?> json(HttpResponse<String> respuesta) {
        return mapa(Json.leer(respuesta.body()));
    }

    private static HttpResponse<String> pedir(String metodo, String ruta, String cuerpo) throws IOException, InterruptedException {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(base + ruta)).timeout(Duration.ofSeconds(8));
        if (cuerpo != null) peticion.header("Content-Type", "application/json; charset=utf-8");
        peticion.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));
        return CLIENTE.send(peticion.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static Map<?, ?> obtener(String ruta) throws Exception {
        HttpResponse<String> respuesta = pedir("GET", ruta, null);
        codigo(200, respuesta);
        return json(respuesta);
    }

    private static String clave(String termino) {
        return "/api/termino?clave=" + URLEncoder.encode(termino, StandardCharsets.UTF_8);
    }

    private static String registro(String termino, int busquedas, String categoria, String fecha) {
        return Json.escribir(Map.of("termino", termino, "busquedas", busquedas, "categoria", categoria, "fecha", fecha));
    }

    private static void agregar(String termino) throws Exception {
        codigo(201, pedir("POST", "/api/terminos", registro(termino, 10, "prueba", "2024-02-29")));
    }

    private static void preparar(String... claves) throws Exception {
        codigo(200, pedir("DELETE", "/api/arbol", "{\"confirmacion\":\"VACIAR\"}"));
        for (String termino : claves) agregar(termino);
    }

    private static String secuencia(Object datos) {
        List<String> claves = new ArrayList<>();
        for (Object dato : lista(datos)) claves.add((String) mapa(dato).get("termino"));
        return String.join(",", claves);
    }

    private static void html() throws Exception {
        HttpResponse<String> respuesta = pedir("GET", "/", null);
        codigo(200, respuesta);
        comprobar(respuesta.headers().firstValue("Content-Type").orElse("").startsWith("text/html"), "MIME HTML incorrecto");
        comprobar(respuesta.body().contains("Google ABB — Índice de autocompletado"), "Título incorrecto");
        for (String seccion : List.of("inicio", "administracion", "arbol", "consultas", "datos")) {
            comprobar(respuesta.body().contains("id=\"view-" + seccion + "\""), "Falta sección " + seccion);
        }
        comprobar(respuesta.body().contains("<svg id=\"tree-svg\""), "Falta SVG");
    }

    private static void recursos() throws Exception {
        for (String[] recurso : new String[][] {{"/styles.css", "text/css"}, {"/app.js", "text/javascript"}}) {
            HttpResponse<String> respuesta = pedir("GET", recurso[0], null);
            codigo(200, respuesta);
            comprobar(respuesta.headers().firstValue("Content-Type").orElse("").startsWith(recurso[1]), "MIME incorrecto");
            comprobar(respuesta.body().length() > 1000, "Recurso vacío");
            comprobar(!respuesta.body().contains("https://"), "Dependencia externa inesperada");
        }
    }

    private static void cabeceras() throws Exception {
        HttpResponse<String> respuesta = pedir("HEAD", "/styles.css", null);
        codigo(200, respuesta);
        igual("", respuesta.body());
        igual("nosniff", respuesta.headers().firstValue("X-Content-Type-Options").orElse(""));
        igual("no-store", respuesta.headers().firstValue("Cache-Control").orElse(""));
        comprobar(respuesta.headers().firstValue("Content-Security-Policy").orElse("").contains("script-src 'self'"), "Falta política de contenido");
    }

    private static void estado() throws Exception {
        Map<?, ?> estado = obtener("/api/estado");
        igual(30L, estado.get("total")); igual(9L, estado.get("altura"));
        igual(8L, estado.get("hojas")); igual(22L, estado.get("internos"));
        igual(30L, estado.get("registrosCSV")); igual("original", estado.get("carga"));
        igual(30, lista(obtener("/api/terminos").get("resultados")).size());
    }

    private static void consultaPura() throws Exception {
        Map<?, ?> primero = mapa(obtener(clave(" JAVA ")).get("termino"));
        igual("java", primero.get("termino")); igual(4200L, primero.get("busquedas"));
        igual("2026-10-24", primero.get("fecha"));
        igual(primero, obtener(clave("java")).get("termino"));
    }

    private static void buscar() throws Exception {
        HttpResponse<String> respuesta = pedir("POST", "/api/buscar", "{\"termino\":\" JAVA \"}");
        codigo(200, respuesta);
        Map<?, ?> datos = json(respuesta);
        igual(4201L, mapa(datos.get("termino")).get("busquedas"));
        igual(LocalDate.now().toString(), mapa(datos.get("termino")).get("fecha"));
        igual(List.of("medellin", "aviones", "java"), datos.get("camino"));
        igual(3L, datos.get("comparaciones"));
    }

    private static void ausentes() throws Exception {
        codigo(404, pedir("GET", clave("no existe"), null));
        codigo(404, pedir("POST", "/api/buscar", "{\"termino\":\"no existe\"}"));
        codigo(404, pedir("PUT", clave("no existe"), "{\"busquedas\":1,\"categoria\":\"otra\",\"fecha\":\"2024-02-29\"}"));
    }

    private static void insertar() throws Exception {
        HttpResponse<String> respuesta = pedir("POST", "/api/terminos", registro(" NUEVO TÉRMINO ", 123, "ciencia", "2024-02-29"));
        codigo(201, respuesta);
        igual(Boolean.TRUE, json(respuesta).get("creado"));
        igual("nuevo término", mapa(json(respuesta).get("termino")).get("termino"));
        igual(31L, obtener("/api/estado").get("total"));
        igual(123L, mapa(obtener(clave("nuevo término")).get("termino")).get("busquedas"));
    }

    private static void duplicado() throws Exception {
        HttpResponse<String> respuesta = pedir("POST", "/api/terminos", registro(" JAVA ", 99999, "otra", "2020-01-01"));
        codigo(200, respuesta);
        igual(Boolean.TRUE, json(respuesta).get("duplicado"));
        Map<?, ?> termino = mapa(json(respuesta).get("termino"));
        igual(4201L, termino.get("busquedas")); igual("tecnologia", termino.get("categoria"));
        igual(LocalDate.now().toString(), termino.get("fecha"));
        igual(30L, obtener("/api/estado").get("total"));
    }

    private static void actualizar() throws Exception {
        HttpResponse<String> respuesta = pedir("PUT", clave("java"), "{\"busquedas\":43,\"categoria\":\"programacion\",\"fecha\":\"2024-02-29\"}");
        codigo(200, respuesta);
        Map<?, ?> termino = mapa(json(respuesta).get("termino"));
        igual("java", termino.get("termino")); igual(43L, termino.get("busquedas"));
        igual("programacion", termino.get("categoria")); igual("2024-02-29", termino.get("fecha"));
    }

    private static void actualizacionAtomica() throws Exception {
        Object antes = obtener(clave("java")).get("termino");
        codigo(400, pedir("PUT", clave("java"), "{\"busquedas\":123,\"categoria\":\"otra\",\"fecha\":\"2026-02-30\"}"));
        igual(antes, obtener(clave("java")).get("termino"));
    }

    private static void eliminarHoja() throws Exception {
        preparar("d", "b", "f");
        codigo(200, pedir("DELETE", clave("b"), null));
        Map<?, ?> raiz = mapa(obtener("/api/arbol").get("raiz"));
        igual(null, raiz.get("izquierdo")); igual("f", mapa(raiz.get("derecho")).get("termino"));
        igual(2L, obtener("/api/estado").get("total"));
    }

    private static void eliminarUnHijo() throws Exception {
        preparar("m", "f", "t", "d");
        codigo(200, pedir("DELETE", clave("f"), null));
        igual("d", mapa(mapa(obtener("/api/arbol").get("raiz")).get("izquierdo")).get("termino"));
    }

    private static void eliminarDosHijos() throws Exception {
        preparar("m", "f", "t", "b", "h", "g", "i");
        codigo(200, pedir("PUT", clave("g"), "{\"busquedas\":123,\"categoria\":\"sucesor\",\"fecha\":\"2024-02-29\"}"));
        codigo(200, pedir("DELETE", clave("f"), null));
        Map<?, ?> nodo = mapa(mapa(obtener("/api/arbol").get("raiz")).get("izquierdo"));
        igual("g", nodo.get("termino")); igual(123L, nodo.get("busquedas")); igual("sucesor", nodo.get("categoria"));
        igual(6, validarEstructura(obtener("/api/arbol").get("raiz"), null, null));
    }

    private static void eliminarRaiz() throws Exception {
        codigo(200, pedir("DELETE", clave("medellin"), null));
        igual("metro medellin", mapa(obtener("/api/arbol").get("raiz")).get("termino"));
        igual(29, validarEstructura(obtener("/api/arbol").get("raiz"), null, null));
    }

    private static void eliminarAusente() throws Exception {
        Object antes = obtener("/api/arbol").get("raiz");
        codigo(404, pedir("DELETE", clave("no existe"), null));
        igual(antes, obtener("/api/arbol").get("raiz"));
    }

    private static void autocompletar() throws Exception {
        Map<?, ?> respuesta = obtener("/api/autocompletar?prefijo=JA");
        igual("java", secuencia(respuesta.get("resultados"))); igual(7L, respuesta.get("visitas"));
        igual(4200L, mapa(obtener(clave("java")).get("termino")).get("busquedas"));
        igual(0, lista(obtener("/api/autocompletar?prefijo=zzz").get("resultados")).size());
    }

    private static void top() throws Exception {
        igual("pico y placa medellin,dolares a pesos,noticias colombia,vuelos baratos,python", secuencia(obtener("/api/top").get("resultados")));
        preparar("g", "d", "a", "f", "b", "e", "c");
        igual("a,b,c,d,e", secuencia(obtener("/api/top").get("resultados")));
    }

    private static void recorrido(String orden, String esperado) throws Exception {
        preparar("d", "b", "f", "a", "c", "e", "g");
        igual(esperado, secuencia(obtener("/api/recorrido?orden=" + orden).get("resultados")));
    }

    private static void extremos() throws Exception {
        Map<?, ?> datos = obtener("/api/extremos");
        igual("algebra lineal", mapa(datos.get("minimo")).get("termino"));
        igual("vuelos baratos", mapa(datos.get("maximo")).get("termino"));
    }

    private static void nivel() throws Exception {
        igual(0L, obtener("/api/nivel?termino=medellin").get("nivel"));
        igual(2L, obtener("/api/nivel?termino=java").get("nivel"));
        codigo(404, pedir("GET", "/api/nivel?termino=zzz", null));
    }

    private static void vecinos() throws Exception {
        Map<?, ?> datos = obtener("/api/vecinos?termino=medellin");
        igual("java", mapa(datos.get("predecesor")).get("termino"));
        igual("metro medellin", mapa(datos.get("sucesor")).get("termino"));
        igual(null, obtener("/api/vecinos?termino=0").get("predecesor"));
        igual(null, obtener("/api/vecinos?termino=zzzz").get("sucesor"));
    }

    private static void rango() throws Exception {
        Map<?, ?> datos = obtener("/api/rango?desde=java&hasta=python");
        igual(9, lista(datos.get("resultados")).size()); igual(10L, datos.get("visitas"));
        comprobar(secuencia(datos.get("resultados")).startsWith("java,medellin,"), "Límite inferior omitido");
        comprobar(secuencia(datos.get("resultados")).endsWith(",python"), "Límite superior omitido");
    }

    private static void rangoInvalido() throws Exception { codigo(400, pedir("GET", "/api/rango?desde=z&hasta=a", null)); }

    private static int validarEstructura(Object objeto, String menor, String mayor) {
        if (objeto == null) return 0;
        Map<?, ?> nodo = mapa(objeto);
        String clave = (String) nodo.get("termino");
        comprobar(menor == null || clave.compareTo(menor) > 0, "Límite inferior roto");
        comprobar(mayor == null || clave.compareTo(mayor) < 0, "Límite superior roto");
        comprobar(nodo.containsKey("busquedas") && nodo.containsKey("categoria") && nodo.containsKey("fecha"), "Faltan metadatos");
        return 1 + validarEstructura(nodo.get("izquierdo"), menor, clave) + validarEstructura(nodo.get("derecho"), clave, mayor);
    }

    private static void estructura() throws Exception {
        Map<?, ?> raiz = mapa(obtener("/api/arbol").get("raiz"));
        igual("medellin", raiz.get("termino"));
        igual("aviones", mapa(raiz.get("izquierdo")).get("termino"));
        igual("python", mapa(raiz.get("derecho")).get("termino"));
        igual(30, validarEstructura(raiz, null, null));
    }

    private static void camino() throws Exception {
        Map<?, ?> datos = obtener("/api/camino?termino=java");
        igual(List.of("medellin", "aviones", "java"), datos.get("camino"));
        igual(3L, datos.get("comparaciones")); igual(Boolean.TRUE, datos.get("encontrado"));
        Map<?, ?> ausente = obtener("/api/camino?termino=zzzz");
        igual(Boolean.FALSE, ausente.get("encontrado")); comprobar(!lista(ausente.get("camino")).isEmpty(), "Falta camino fallido");
        igual(4200L, mapa(obtener(clave("java")).get("termino")).get("busquedas"));
    }

    private static void fotografia() {
        ArbolBinarioBusqueda arbol = new ArbolBinarioBusqueda();
        arbol.insertar(new TerminoBusqueda("java", 10, "otra", "2024-02-29"));
        VistaNodoABB copia = arbol.obtenerEstructura();
        arbol.buscar("java").setNumBusquedas(50);
        arbol.eliminar("java");
        igual("java", copia.getTermino()); igual(10, copia.getBusquedas());
        igual("otra", copia.getCategoria()); igual("2024-02-29", copia.getFecha());
        igual(null, copia.getIzquierdo()); igual(null, arbol.obtenerEstructura());
    }

    private static void restaurar() throws Exception {
        codigo(200, pedir("POST", "/api/buscar", "{\"termino\":\"java\"}"));
        agregar("prueba");
        for (int i = 0; i < 2; i++) {
            codigo(200, pedir("POST", "/api/restaurar", "{\"confirmacion\":\"RESTAURAR\"}"));
            igual(30L, obtener("/api/estado").get("total"));
            igual(4200L, mapa(obtener(clave("java")).get("termino")).get("busquedas"));
            codigo(404, pedir("GET", clave("prueba"), null));
        }
    }

    private static void confirmaciones() throws Exception {
        codigo(400, pedir("DELETE", "/api/arbol", "{}"));
        codigo(400, pedir("POST", "/api/restaurar", "{\"confirmacion\":\"no\"}"));
        igual(30L, obtener("/api/estado").get("total"));
    }

    private static void vacio() throws Exception {
        preparar();
        Map<?, ?> datos = obtener("/api/estado");
        igual(0L, datos.get("total")); igual(-1L, datos.get("altura"));
        igual(0L, datos.get("hojas")); igual(0L, datos.get("internos"));
        igual(null, obtener("/api/arbol").get("raiz"));
        igual(null, obtener("/api/extremos").get("minimo"));
        igual(null, obtener("/api/vecinos?termino=java").get("sucesor"));
        for (String ruta : List.of("/api/top", "/api/terminos", "/api/autocompletar?prefijo=ja", "/api/rango?desde=a&hasta=z",
                "/api/recorrido?orden=inorden", "/api/recorrido?orden=preorden", "/api/recorrido?orden=postorden", "/api/recorrido?orden=niveles")) {
            igual(0, lista(obtener(ruta).get("resultados")).size());
        }
        codigo(404, pedir("POST", "/api/buscar", "{\"termino\":\"java\"}"));
    }

    private static void restaurarVacio() throws Exception {
        preparar();
        codigo(200, pedir("POST", "/api/restaurar", "{\"confirmacion\":\"RESTAURAR\"}"));
        estado();
    }

    private static void experimento() throws Exception {
        agregar("nuevo");
        Object antes = obtener("/api/arbol").get("raiz");
        List<?> filas = lista(obtener("/api/experimento").get("resultados"));
        igual(2, filas.size());
        Map<?, ?> original = mapa(filas.get(0)); Map<?, ?> ordenado = mapa(filas.get(1));
        igual(9L, original.get("altura")); igual(130L, original.get("insercion")); igual(160L, original.get("totalBusqueda"));
        igual(29L, ordenado.get("altura")); igual(435L, ordenado.get("insercion")); igual(465L, ordenado.get("totalBusqueda"));
        igual(antes, obtener("/api/arbol").get("raiz"));
    }

    private static void datosInvalidos() throws Exception {
        for (String cuerpo : List.of(registro(" ", 1, "otra", "2024-02-29"), registro("a", -1, "otra", "2024-02-29"),
                registro("a", 1, "", "2024-02-29"), registro("a", 1, "otra", "2026-02-30"),
                "{\"termino\":\"a\",\"busquedas\":1.5,\"categoria\":\"otra\",\"fecha\":\"2024-02-29\"}",
                "{\"termino\":\"a\",\"busquedas\":2147483648,\"categoria\":\"otra\",\"fecha\":\"2024-02-29\"}",
                registro("a".repeat(201), 1, "otra", "2024-02-29"))) {
            codigo(400, pedir("POST", "/api/terminos", cuerpo));
        }
        igual(30L, obtener("/api/estado").get("total"));
    }

    private static void claveInmutable() throws Exception {
        codigo(400, pedir("PUT", clave("java"), registro("otra clave", 10, "otra", "2024-02-29")));
        igual("java", mapa(obtener(clave("java")).get("termino")).get("termino"));
    }

    private static void jsonInvalido() throws Exception {
        for (String cuerpo : List.of("", "{", "[]", "null", "{\"a\":1,}", "{\"a\":01}", "{\"a\":1,\"a\":2}",
                "{\"a\":NaN}", "{\"a\":1e}", "{\"a\":\"\\uD800\"}", "[".repeat(34) + "0" + "]".repeat(34))) {
            codigo(400, pedir("POST", "/api/terminos", cuerpo));
        }
    }

    private static void jsonEscapes() {
        String texto = "Tildes áéíóú, comillas \" y barra \\, salto\n, emoji 🌳 y <script> &";
        String escrito = Json.escribir(Map.of("dato", texto, "lista", List.of(1, true, "x")));
        igual(texto, mapa(Json.leer(escrito)).get("dato"));
        comprobar(!escrito.contains("<script>"), "No escapó HTML en JSON");
        igual("á", Json.leer("\"\\u00e1\""));
        igual("🌳", Json.leer("\"\\ud83c\\udf33\""));
        igual(null, Json.leer("null"));
    }

    private static void contenidoHTML() throws Exception {
        String clave = "<img src=x onerror=alert(1)>";
        HttpResponse<String> respuesta = pedir("POST", "/api/terminos", registro(clave, 0, "otra", "2024-02-29"));
        codigo(201, respuesta);
        comprobar(!respuesta.body().contains("<img"), "Respuesta JSON sin escape");
        igual(clave, mapa(json(respuesta).get("termino")).get("termino"));
        String js = Files.readString(Path.of("web/app.js"));
        comprobar(!js.contains("innerHTML") && !js.contains("insertAdjacentHTML") && !js.contains("eval("), "Inserción de HTML dinámico insegura");
    }

    private static void limites() throws Exception {
        HttpRequest incorrecta = HttpRequest.newBuilder(URI.create(base + "/api/terminos"))
                .header("Content-Type", "text/plain").POST(HttpRequest.BodyPublishers.ofString("{}" )).build();
        codigo(415, CLIENTE.send(incorrecta, HttpResponse.BodyHandlers.ofString()));
        codigo(413, pedir("POST", "/api/terminos", "a".repeat(65537)));
    }

    private static void utf8Invalido() throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(base + "/api/terminos"))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofByteArray(new byte[] {(byte) 0xc3, 0x28})).build();
        codigo(400, CLIENTE.send(peticion, HttpResponse.BodyHandlers.ofString()));
    }

    private static void metodos() throws Exception {
        for (String[] caso : new String[][] {{"POST", "/api/estado"}, {"GET", "/api/buscar"}, {"PUT", "/api/arbol"},
                {"PATCH", clave("java")}, {"GET", "/api/restaurar"}, {"POST", "/styles.css"}}) {
            HttpResponse<String> respuesta = pedir(caso[0], caso[1], null);
            codigo(405, respuesta);
            comprobar(respuesta.headers().firstValue("Allow").isPresent(), "Falta cabecera Allow");
        }
    }

    private static void rutasInternas() throws Exception {
        for (String ruta : List.of("/README.md", "/terminos.csv", "/Main.java", "/.git/config", "/web/../README.md", "/%2e%2e/README.md", "/api/desconocido")) {
            codigo(404, pedir("GET", ruta, null));
        }
    }

    private static void origen() throws Exception {
        HttpRequest externa = HttpRequest.newBuilder(URI.create(base + "/api/restaurar"))
                .header("Origin", "https://ejemplo.com").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"confirmacion\":\"RESTAURAR\"}")).build();
        codigo(403, CLIENTE.send(externa, HttpResponse.BodyHandlers.ofString()));
        HttpRequest local = HttpRequest.newBuilder(URI.create(base + "/api/estado")).header("Origin", base).GET().build();
        codigo(200, CLIENTE.send(local, HttpResponse.BodyHandlers.ofString()));
    }

    private static void parametros() throws Exception {
        for (String ruta : List.of("/api/autocompletar?prefijo=", "/api/termino", "/api/termino?clave=", "/api/nivel?termino=", "/api/termino?clave=a&clave=b", "/api/recorrido?orden=otro")) {
            codigo(400, pedir("GET", ruta, null));
        }
    }

    private static void concurrencia() throws Exception {
        List<CompletableFuture<HttpResponse<String>>> peticiones = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(base + "/api/buscar"))
                    .header("Content-Type", "application/json").timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString("{\"termino\":\"java\"}")).build();
            peticiones.add(CLIENTE.sendAsync(peticion, HttpResponse.BodyHandlers.ofString()));
        }
        for (CompletableFuture<HttpResponse<String>> respuesta : peticiones) codigo(200, respuesta.join());
        igual(4240L, mapa(obtener(clave("java")).get("termino")).get("busquedas"));
        igual(30, validarEstructura(obtener("/api/arbol").get("raiz"), null, null));
    }

    private static void integridad() throws Exception {
        for (String[] archivo : new String[][] {
                {"terminos.csv", "6015F70B3EA846EFD32AC4234C3D9221FD5A48CAB6DC5C1204F82C25A8E1F843"},
                {"PruebasABB.java", "A442891831D5AA4C1F86671B158631BD3002C9559D8A1138797AD531E831B94C"}}) {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(archivo[0])));
            StringBuilder hexadecimal = new StringBuilder();
            for (byte valor : hash) hexadecimal.append(String.format(java.util.Locale.ROOT, "%02X", valor & 0xff));
            igual(archivo[1], hexadecimal.toString());
        }
    }
}

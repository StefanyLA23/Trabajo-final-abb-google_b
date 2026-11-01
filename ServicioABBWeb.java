import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Adaptador HTTP del ABB. Un monitor protege el árbol y sus métricas frente a concurrencia. */
public final class ServicioABBWeb {
    private ArbolBinarioBusqueda arbol;
    private final Path csv;
    private int registrosCSV;
    private long revision;
    private String carga;

    public ServicioABBWeb(Path csv) throws IOException {
        this.csv = csv;
        restaurar();
    }

    /** Una petición completa toma una fotografía coherente, sin entregar entidades mutables. */
    public synchronized Map<String, Object> atender(String metodo, String ruta,
            Map<String, String> parametros, Map<String, Object> cuerpo) throws IOException {
        switch (ruta) {
            case "/api/estado":
                exigirMetodo(metodo, "GET");
                return estado();
            case "/api/arbol":
                if (metodo.equals("DELETE")) {
                    confirmar(cuerpo, "VACIAR");
                    arbol = new ArbolBinarioBusqueda();
                    cambio("vacío");
                    return objeto("mensaje", "Árbol vaciado. Puedes restaurar los datos originales.", "estado", estado());
                }
                exigirMetodo(metodo, "GET");
                return objeto("raiz", vista(arbol.obtenerEstructura()), "estado", estado());
            case "/api/terminos":
                if (metodo.equals("POST")) {
                    campos(cuerpo, Set.of("termino", "busquedas", "categoria", "fecha"));
                    TerminoBusqueda nuevo = new TerminoBusqueda(texto(cuerpo, "termino"), entero(cuerpo, "busquedas"),
                            texto(cuerpo, "categoria"), texto(cuerpo, "fecha"));
                    boolean duplicado = arbol.buscar(nuevo.getTermino()) != null;
                    arbol.insertar(nuevo);
                    cambio("modificado");
                    return objeto("creado", !duplicado, "duplicado", duplicado,
                            "mensaje", duplicado ? "Ya existía: popularidad +1 y fecha actualizada; sin duplicar el término."
                                    : "Término insertado correctamente.",
                            "termino", termino(arbol.buscar(nuevo.getTermino())), "estado", estado());
                }
                exigirMetodo(metodo, "GET");
                return objeto("resultados", terminos(arbol.inOrden()));
            case "/api/termino":
                return operarTermino(metodo, parametro(parametros, "clave"), cuerpo);
            case "/api/buscar":
                exigirMetodo(metodo, "POST");
                campos(cuerpo, Set.of("termino"));
                TerminoBusqueda encontrado = arbol.registrarBusqueda(texto(cuerpo, "termino"));
                if (encontrado == null) throw new ErrorHTTP(404, "No encontramos ese término en el índice.");
                int comparaciones = arbol.getComparacionesUltimaBusqueda();
                cambio("modificado");
                return objeto("termino", termino(encontrado), "comparaciones", comparaciones,
                        "camino", arbol.obtenerCaminoBusqueda(encontrado.getTermino()), "estado", estado());
            case "/api/autocompletar":
                exigirMetodo(metodo, "GET");
                List<TerminoBusqueda> sugerencias = arbol.autocompletar(parametro(parametros, "prefijo"));
                return objeto("resultados", terminos(sugerencias), "visitas", arbol.getVisitasUltimoAutocompletado());
            case "/api/camino":
                exigirMetodo(metodo, "GET");
                String clave = parametro(parametros, "termino");
                TerminoBusqueda resultado = arbol.buscar(clave);
                return objeto("encontrado", resultado != null, "termino", termino(resultado),
                        "comparaciones", arbol.getComparacionesUltimaBusqueda(), "camino", arbol.obtenerCaminoBusqueda(clave));
            case "/api/recorrido":
                exigirMetodo(metodo, "GET");
                return recorrido(parametro(parametros, "orden"));
            case "/api/extremos":
                exigirMetodo(metodo, "GET");
                return objeto("minimo", termino(arbol.obtenerMinimo()), "maximo", termino(arbol.obtenerMaximo()));
            case "/api/nivel":
                exigirMetodo(metodo, "GET");
                int nivel = arbol.obtenerNivel(parametro(parametros, "termino"));
                if (nivel < 0) throw new ErrorHTTP(404, "El término no existe en el árbol.");
                return objeto("nivel", nivel);
            case "/api/vecinos":
                exigirMetodo(metodo, "GET");
                String referencia = parametro(parametros, "termino");
                return objeto("predecesor", termino(arbol.predecesor(referencia)),
                        "sucesor", termino(arbol.sucesor(referencia)));
            case "/api/rango":
                exigirMetodo(metodo, "GET");
                List<TerminoBusqueda> rango = arbol.buscarPorRango(parametro(parametros, "desde"), parametro(parametros, "hasta"));
                return objeto("resultados", terminos(rango), "visitas", arbol.getVisitasUltimoRango());
            case "/api/top":
                exigirMetodo(metodo, "GET");
                return objeto("resultados", terminos(arbol.top5()));
            case "/api/restaurar":
                exigirMetodo(metodo, "POST");
                confirmar(cuerpo, "RESTAURAR");
                restaurar();
                return objeto("mensaje", "Datos originales restaurados en un árbol nuevo, sin acumular popularidad.", "estado", estado());
            case "/api/experimento":
                exigirMetodo(metodo, "GET");
                List<Object> mediciones = new ArrayList<>();
                for (ExperimentoABB.Resultado medicion : ExperimentoABB.ejecutar(csv.toString())) {
                    mediciones.add(objeto("orden", medicion.getOrden(), "nodos", medicion.getNodos(),
                            "altura", medicion.getAltura(), "insercion", medicion.getInsercion(),
                            "totalBusqueda", medicion.getTotalBusqueda(), "mediaBusqueda", medicion.getMediaBusqueda(),
                            "maximoBusqueda", medicion.getMaximoBusqueda(), "ausente", medicion.getAusente(),
                            "visitasPrefijo", medicion.getVisitasPrefijo(), "visitasRango", medicion.getVisitasRango(),
                            "valido", medicion.isValido()));
                }
                return objeto("resultados", mediciones);
            default:
                throw new ErrorHTTP(404, "Esta ruta no existe.");
        }
    }

    private Map<String, Object> operarTermino(String metodo, String clave, Map<String, Object> cuerpo) {
        if (!Set.of("GET", "PUT", "DELETE").contains(metodo)) throw new ErrorHTTP(405, "Método HTTP no permitido.");
        TerminoBusqueda existente = arbol.buscar(clave);
        if (existente == null) throw new ErrorHTTP(404, "El término no existe en el árbol.");
        if (metodo.equals("GET")) return objeto("termino", termino(existente));
        if (metodo.equals("DELETE")) {
            arbol.eliminar(clave);
            cambio("modificado");
            return objeto("mensaje", "Término eliminado. El árbol conserva su orden.", "estado", estado());
        }
        campos(cuerpo, Set.of("busquedas", "categoria", "fecha"));
        arbol.actualizarTermino(clave, entero(cuerpo, "busquedas"), texto(cuerpo, "categoria"), texto(cuerpo, "fecha"));
        cambio("modificado");
        return objeto("mensaje", "Término actualizado. La clave permanece intacta.", "termino", termino(existente), "estado", estado());
    }

    private Map<String, Object> recorrido(String orden) {
        List<TerminoBusqueda> resultado;
        switch (orden) {
            case "inorden": resultado = arbol.inOrden(); break;
            case "preorden": resultado = arbol.preOrden(); break;
            case "postorden": resultado = arbol.postOrden(); break;
            case "niveles": resultado = arbol.porNiveles(); break;
            default: throw new IllegalArgumentException("Recorrido inválido.");
        }
        return objeto("orden", orden, "resultados", terminos(resultado));
    }

    /** Construye primero el reemplazo; un error de lectura deja intacto el árbol actual. */
    private void restaurar() throws IOException {
        List<TerminoBusqueda> registros = LectorCSV.leer(csv.toString());
        ArbolBinarioBusqueda nuevo = new ArbolBinarioBusqueda();
        for (TerminoBusqueda registro : registros) nuevo.insertar(registro);
        arbol = nuevo;
        registrosCSV = registros.size();
        cambio("original");
    }

    private void cambio(String carga) { revision++; this.carga = carga; }

    private Map<String, Object> estado() {
        return objeto("total", arbol.contarTotalNodos(), "altura", arbol.obtenerAltura(),
                "hojas", arbol.contarHojas(), "internos", arbol.getNodosInternos(),
                "registrosCSV", registrosCSV, "carga", carga, "revision", revision,
                "columnas", List.of("termino", "busquedas", "categoria", "fecha"));
    }

    private static Map<String, Object> termino(TerminoBusqueda dato) {
        if (dato == null) return null;
        return objeto("termino", dato.getTermino(), "busquedas", dato.getNumBusquedas(),
                "categoria", dato.getCategoria(), "fecha", dato.getFechaUltBusqueda());
    }

    private static List<Object> terminos(List<TerminoBusqueda> datos) {
        List<Object> resultados = new ArrayList<>();
        for (TerminoBusqueda dato : datos) resultados.add(termino(dato));
        return resultados;
    }

    private static Map<String, Object> vista(VistaNodoABB nodo) {
        if (nodo == null) return null;
        return objeto("termino", nodo.getTermino(), "busquedas", nodo.getBusquedas(),
                "categoria", nodo.getCategoria(), "fecha", nodo.getFecha(),
                "izquierdo", vista(nodo.getIzquierdo()), "derecho", vista(nodo.getDerecho()));
    }

    static Map<String, Object> objeto(Object... pares) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) resultado.put((String) pares[i], pares[i + 1]);
        return resultado;
    }

    private static void exigirMetodo(String real, String esperado) {
        if (!real.equals(esperado)) throw new ErrorHTTP(405, "Método HTTP no permitido. Usa " + esperado + ".");
    }

    private static String parametro(Map<String, String> parametros, String nombre) {
        String valor = parametros.get(nombre);
        if (valor == null) throw new IllegalArgumentException("Falta el parámetro " + nombre + ".");
        return valor;
    }

    private static String texto(Map<String, Object> cuerpo, String nombre) {
        Object valor = cuerpo.get(nombre);
        if (!(valor instanceof String)) throw new IllegalArgumentException("El campo " + nombre + " debe ser texto.");
        if (((String) valor).length() > 200) throw new IllegalArgumentException("El campo " + nombre + " admite hasta 200 caracteres.");
        for (char caracter : ((String) valor).toCharArray()) {
            if (Character.isISOControl(caracter)) throw new IllegalArgumentException("El campo " + nombre + " contiene caracteres de control.");
        }
        return (String) valor;
    }

    private static int entero(Map<String, Object> cuerpo, String nombre) {
        Object valor = cuerpo.get(nombre);
        if (!(valor instanceof Long) || (Long) valor < 0 || (Long) valor > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("El campo " + nombre + " debe ser un entero entre 0 y " + Integer.MAX_VALUE + ".");
        }
        return ((Long) valor).intValue();
    }

    private static void campos(Map<String, Object> cuerpo, Set<String> permitidos) {
        if (!permitidos.equals(cuerpo.keySet())) throw new IllegalArgumentException("Envía exactamente estos campos: " + permitidos + ".");
    }

    private static void confirmar(Map<String, Object> cuerpo, String accion) {
        campos(cuerpo, Set.of("confirmacion"));
        if (!accion.equals(cuerpo.get("confirmacion"))) throw new IllegalArgumentException("Es necesario confirmar esta operación.");
    }
}

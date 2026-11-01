import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/** ABB sin balanceo: claves izquierdas menores y derechas mayores que la del nodo. */
public class ArbolBinarioBusqueda {
    private NodoABB raiz;
    private long comparacionesInsercion;
    private int comparacionesUltimaBusqueda;
    private int visitasUltimoAutocompletado;
    private int visitasUltimoRango;

    public ArbolBinarioBusqueda() {
        raiz = null;
    }

    public boolean estaVacio() {
        return raiz == null;
    }

    /** Inserta conservando el contador inicial; un duplicado suma uno al registro existente. */
    public void insertar(TerminoBusqueda dato) {
        if (dato == null) throw new IllegalArgumentException("El término no puede ser nulo.");
        raiz = insertarRecursivo(raiz, dato);
    }

    private NodoABB insertarRecursivo(NodoABB nodo, TerminoBusqueda dato) {
        if (nodo == null) return new NodoABB(dato);
        comparacionesInsercion++;
        int comparacion = dato.getTermino().compareTo(nodo.getDato().getTermino());
        if (comparacion < 0) {
            nodo.setIzq(insertarRecursivo(nodo.getIzq(), dato));
        } else if (comparacion > 0) {
            nodo.setDer(insertarRecursivo(nodo.getDer(), dato));
        } else {
            nodo.getDato().incrementarNumBusquedas();
        }
        return nodo;
    }

    /** Consulta exacta sin modificar la popularidad. Retorna null si no existe. */
    public TerminoBusqueda buscar(String termino) {
        String clave = TerminoBusqueda.normalizarTermino(termino);
        comparacionesUltimaBusqueda = 0;
        return buscarRecursivo(raiz, clave);
    }

    private TerminoBusqueda buscarRecursivo(NodoABB nodo, String termino) {
        if (nodo == null) return null;
        comparacionesUltimaBusqueda++;
        int comparacion = termino.compareTo(nodo.getDato().getTermino());
        if (comparacion == 0) return nodo.getDato();
        if (comparacion < 0) return buscarRecursivo(nodo.getIzq(), termino);
        return buscarRecursivo(nodo.getDer(), termino);
    }

    /** Búsqueda de usuario: si existe, aumenta su popularidad y actualiza la fecha. */
    public TerminoBusqueda registrarBusqueda(String termino) {
        TerminoBusqueda resultado = buscar(termino);
        if (resultado != null) resultado.incrementarNumBusquedas();
        return resultado;
    }

    /** Valida todos los datos antes de actualizar, manteniendo intacta la clave. */
    public boolean actualizarTermino(String termino, int busquedas, String categoria, String fecha) {
        TerminoBusqueda datosValidados = new TerminoBusqueda(termino, busquedas, categoria, fecha);
        TerminoBusqueda encontrado = buscar(datosValidados.getTermino());
        if (encontrado == null) return false;
        encontrado.setNumBusquedas(datosValidados.getNumBusquedas());
        encontrado.setCategoria(datosValidados.getCategoria());
        encontrado.setFecha(datosValidados.getFechaUltBusqueda());
        return true;
    }

    /** Elimina una clave, incluida la raíz. Retorna false si no existe. */
    public boolean eliminar(String termino) {
        String clave = TerminoBusqueda.normalizarTermino(termino);
        if (buscar(clave) == null) return false;
        raiz = eliminarRecursivo(raiz, clave);
        return true;
    }

    private NodoABB eliminarRecursivo(NodoABB nodo, String termino) {
        if (nodo == null) return null;
        int comparacion = termino.compareTo(nodo.getDato().getTermino());
        if (comparacion < 0) {
            nodo.setIzq(eliminarRecursivo(nodo.getIzq(), termino));
        } else if (comparacion > 0) {
            nodo.setDer(eliminarRecursivo(nodo.getDer(), termino));
        } else {
            // Hoja o un hijo: el hijo (que puede ser null) ocupa su lugar.
            if (nodo.getIzq() == null) return nodo.getDer();
            if (nodo.getDer() == null) return nodo.getIzq();
            // Dos hijos: se traslada el registro completo del sucesor inorden.
            TerminoBusqueda sucesor = minimoRecursivo(nodo.getDer());
            nodo.setDato(sucesor);
            nodo.setDer(eliminarRecursivo(nodo.getDer(), sucesor.getTermino()));
        }
        return nodo;
    }

    /** Inorden: izquierdo, nodo, derecho. Retorna los términos en orden alfabético. */
    public List<TerminoBusqueda> inOrden() {
        List<TerminoBusqueda> resultados = new ArrayList<>();
        inOrdenRecursivo(raiz, resultados);
        return resultados;
    }

    private void inOrdenRecursivo(NodoABB nodo, List<TerminoBusqueda> resultados) {
        if (nodo == null) return;
        inOrdenRecursivo(nodo.getIzq(), resultados);
        resultados.add(nodo.getDato());
        inOrdenRecursivo(nodo.getDer(), resultados);
    }

    /** Preorden: nodo, izquierdo, derecho. */
    public List<TerminoBusqueda> preOrden() {
        List<TerminoBusqueda> resultados = new ArrayList<>();
        preOrdenRecursivo(raiz, resultados);
        return resultados;
    }

    private void preOrdenRecursivo(NodoABB nodo, List<TerminoBusqueda> resultados) {
        if (nodo == null) return;
        resultados.add(nodo.getDato());
        preOrdenRecursivo(nodo.getIzq(), resultados);
        preOrdenRecursivo(nodo.getDer(), resultados);
    }

    /** Postorden: izquierdo, derecho, nodo. */
    public List<TerminoBusqueda> postOrden() {
        List<TerminoBusqueda> resultados = new ArrayList<>();
        postOrdenRecursivo(raiz, resultados);
        return resultados;
    }

    private void postOrdenRecursivo(NodoABB nodo, List<TerminoBusqueda> resultados) {
        if (nodo == null) return;
        postOrdenRecursivo(nodo.getIzq(), resultados);
        postOrdenRecursivo(nodo.getDer(), resultados);
        resultados.add(nodo.getDato());
    }

    /** Único recorrido iterativo del ABB: una cola FIFO visita nivel por nivel. */
    public List<TerminoBusqueda> porNiveles() {
        List<TerminoBusqueda> resultados = new ArrayList<>();
        Queue<NodoABB> cola = new ArrayDeque<>();
        if (raiz != null) cola.add(raiz);
        while (!cola.isEmpty()) {
            NodoABB nodo = cola.remove();
            resultados.add(nodo.getDato());
            if (nodo.getIzq() != null) cola.add(nodo.getIzq());
            if (nodo.getDer() != null) cola.add(nodo.getDer());
        }
        return resultados;
    }

    public TerminoBusqueda obtenerMinimo() { return minimoRecursivo(raiz); }

    private TerminoBusqueda minimoRecursivo(NodoABB nodo) {
        if (nodo == null) return null;
        if (nodo.getIzq() == null) return nodo.getDato();
        return minimoRecursivo(nodo.getIzq());
    }

    public TerminoBusqueda obtenerMaximo() { return maximoRecursivo(raiz); }

    private TerminoBusqueda maximoRecursivo(NodoABB nodo) {
        if (nodo == null) return null;
        if (nodo.getDer() == null) return nodo.getDato();
        return maximoRecursivo(nodo.getDer());
    }

    /** Menor clave estrictamente mayor a la consultada, aunque esta no exista. */
    public TerminoBusqueda sucesor(String termino) {
        return sucesorRecursivo(raiz, TerminoBusqueda.normalizarTermino(termino));
    }

    private TerminoBusqueda sucesorRecursivo(NodoABB nodo, String termino) {
        if (nodo == null) return null;
        if (termino.compareTo(nodo.getDato().getTermino()) < 0) {
            TerminoBusqueda candidato = sucesorRecursivo(nodo.getIzq(), termino);
            return candidato == null ? nodo.getDato() : candidato;
        }
        return sucesorRecursivo(nodo.getDer(), termino);
    }

    /** Mayor clave estrictamente menor a la consultada; null si no hay anterior. */
    public TerminoBusqueda predecesor(String termino) {
        return predecesorRecursivo(raiz, TerminoBusqueda.normalizarTermino(termino));
    }

    private TerminoBusqueda predecesorRecursivo(NodoABB nodo, String termino) {
        if (nodo == null) return null;
        if (termino.compareTo(nodo.getDato().getTermino()) > 0) {
            TerminoBusqueda candidato = predecesorRecursivo(nodo.getDer(), termino);
            return candidato == null ? nodo.getDato() : candidato;
        }
        return predecesorRecursivo(nodo.getIzq(), termino);
    }

    /** Rango inclusivo [desde, hasta], ordenado, sin entrar en ramas fuera de límites. */
    public List<TerminoBusqueda> buscarPorRango(String desde, String hasta) {
        String inicio = TerminoBusqueda.normalizarTermino(desde);
        String fin = TerminoBusqueda.normalizarTermino(hasta);
        if (inicio.compareTo(fin) > 0) {
            throw new IllegalArgumentException("El inicio del rango debe ser menor o igual al final.");
        }
        visitasUltimoRango = 0;
        List<TerminoBusqueda> resultados = new ArrayList<>();
        rangoRecursivo(raiz, inicio, fin, resultados);
        return resultados;
    }

    private void rangoRecursivo(NodoABB nodo, String desde, String hasta, List<TerminoBusqueda> resultados) {
        if (nodo == null) return;
        visitasUltimoRango++;
        String clave = nodo.getDato().getTermino();
        int contraInicio = clave.compareTo(desde);
        int contraFin = clave.compareTo(hasta);
        if (contraInicio > 0) rangoRecursivo(nodo.getIzq(), desde, hasta, resultados);
        if (contraInicio >= 0 && contraFin <= 0) resultados.add(nodo.getDato());
        if (contraFin < 0) rangoRecursivo(nodo.getDer(), desde, hasta, resultados);
    }

    /** Autocompleta en orden alfabético y poda las ramas incompatibles con el prefijo. */
    public List<TerminoBusqueda> autocompletar(String prefijo) {
        String inicio = TerminoBusqueda.normalizarTermino(prefijo);
        visitasUltimoAutocompletado = 0;
        List<TerminoBusqueda> resultados = new ArrayList<>();
        autocompletarRecursivo(raiz, inicio, resultados);
        return resultados;
    }

    private void autocompletarRecursivo(NodoABB nodo, String prefijo, List<TerminoBusqueda> resultados) {
        if (nodo == null) return;
        visitasUltimoAutocompletado++;
        String clave = nodo.getDato().getTermino();
        if (clave.startsWith(prefijo)) {
            autocompletarRecursivo(nodo.getIzq(), prefijo, resultados);
            resultados.add(nodo.getDato());
            autocompletarRecursivo(nodo.getDer(), prefijo, resultados);
        } else if (clave.compareTo(prefijo) < 0) {
            // La clave y todo su subárbol izquierdo son menores que el prefijo.
            autocompletarRecursivo(nodo.getDer(), prefijo, resultados);
        } else {
            // La clave ya superó el bloque del prefijo; el derecho tampoco coincide.
            autocompletarRecursivo(nodo.getIzq(), prefijo, resultados);
        }
    }

    /** Visita todos los nodos y mantiene cinco candidatos; empata por clave ascendente. */
    public List<TerminoBusqueda> top5() {
        List<TerminoBusqueda> mejores = new ArrayList<>();
        top5Recursivo(raiz, mejores);
        return mejores;
    }

    private void top5Recursivo(NodoABB nodo, List<TerminoBusqueda> mejores) {
        if (nodo == null) return;
        TerminoBusqueda dato = nodo.getDato();
        int posicion = 0;
        while (posicion < mejores.size() && compararPopularidad(mejores.get(posicion), dato) <= 0) {
            posicion++;
        }
        mejores.add(posicion, dato);
        if (mejores.size() > 5) mejores.remove(5);
        top5Recursivo(nodo.getIzq(), mejores);
        top5Recursivo(nodo.getDer(), mejores);
    }

    private int compararPopularidad(TerminoBusqueda primero, TerminoBusqueda segundo) {
        int comparacion = Integer.compare(segundo.getNumBusquedas(), primero.getNumBusquedas());
        if (comparacion == 0) return primero.getTermino().compareTo(segundo.getTermino());
        return comparacion;
    }

    /** Dibujo en preorden: sangría para la profundidad y etiquetas para los hijos. */
    public String visualizarEstructura() {
        if (estaVacio()) return "Árbol vacío.\n";
        StringBuilder texto = new StringBuilder();
        estructuraRecursiva(raiz, "", "RAÍZ", texto);
        return texto.toString();
    }

    private void estructuraRecursiva(NodoABB nodo, String sangria, String lado, StringBuilder texto) {
        if (nodo == null) return;
        texto.append(sangria).append(lado).append(": ").append(nodo.getDato()).append('\n');
        estructuraRecursiva(nodo.getIzq(), sangria + "    ", "IZQ", texto);
        estructuraRecursiva(nodo.getDer(), sangria + "    ", "DER", texto);
    }

    public int contarTotalNodos() { return contarTotalRecursivo(raiz); }

    private int contarTotalRecursivo(NodoABB nodo) {
        if (nodo == null) return 0;
        return 1 + contarTotalRecursivo(nodo.getIzq()) + contarTotalRecursivo(nodo.getDer());
    }

    public int contarHojas() { return contarHojasRecursivo(raiz); }

    private int contarHojasRecursivo(NodoABB nodo) {
        if (nodo == null) return 0;
        if (nodo.getIzq() == null && nodo.getDer() == null) return 1;
        return contarHojasRecursivo(nodo.getIzq()) + contarHojasRecursivo(nodo.getDer());
    }

    public int contarInternos() { return getNodosInternos(); }
    public int getNodosInternos() { return internosRecursivo(raiz); }

    private int internosRecursivo(NodoABB nodo) {
        if (nodo == null || (nodo.getIzq() == null && nodo.getDer() == null)) return 0;
        return 1 + internosRecursivo(nodo.getIzq()) + internosRecursivo(nodo.getDer());
    }

    /** Altura medida en aristas: vacío = -1, hoja = 0. */
    public int obtenerAltura() { return calcularAlturaRecursiva(raiz); }

    private int calcularAlturaRecursiva(NodoABB nodo) {
        if (nodo == null) return -1;
        return 1 + Math.max(calcularAlturaRecursiva(nodo.getIzq()), calcularAlturaRecursiva(nodo.getDer()));
    }

    /** La raíz tiene nivel cero; una clave ausente tiene nivel -1. */
    public int obtenerNivel(String termino) {
        return nivelRecursivo(raiz, TerminoBusqueda.normalizarTermino(termino), 0);
    }

    private int nivelRecursivo(NodoABB nodo, String termino, int nivel) {
        if (nodo == null) return -1;
        int comparacion = termino.compareTo(nodo.getDato().getTermino());
        if (comparacion == 0) return nivel;
        if (comparacion < 0) return nivelRecursivo(nodo.getIzq(), termino, nivel + 1);
        return nivelRecursivo(nodo.getDer(), termino, nivel + 1);
    }

    /** Verifica límites estrictos de todos los subárboles, no solo de los hijos directos. */
    public boolean esABBValido() { return validarRecursivo(raiz, null, null); }

    private boolean validarRecursivo(NodoABB nodo, String menor, String mayor) {
        if (nodo == null) return true;
        String clave = nodo.getDato().getTermino();
        if (menor != null && clave.compareTo(menor) <= 0) return false;
        if (mayor != null && clave.compareTo(mayor) >= 0) return false;
        return validarRecursivo(nodo.getIzq(), menor, clave) && validarRecursivo(nodo.getDer(), clave, mayor);
    }

    /** Copia recursiva de las relaciones reales, sin exponer NodoABB ni datos modificables. */
    public VistaNodoABB obtenerEstructura() {
        return copiarEstructuraRecursiva(raiz);
    }

    private VistaNodoABB copiarEstructuraRecursiva(NodoABB nodo) {
        if (nodo == null) return null;
        return new VistaNodoABB(nodo.getDato(), copiarEstructuraRecursiva(nodo.getIzq()),
                copiarEstructuraRecursiva(nodo.getDer()));
    }

    /** Devuelve las claves visitadas por una búsqueda; no registra popularidad. */
    public List<String> obtenerCaminoBusqueda(String termino) {
        List<String> camino = new ArrayList<>();
        caminoRecursivo(raiz, TerminoBusqueda.normalizarTermino(termino), camino);
        return camino;
    }

    private void caminoRecursivo(NodoABB nodo, String clave, List<String> camino) {
        if (nodo == null) return;
        camino.add(nodo.getDato().getTermino());
        int comparacion = clave.compareTo(nodo.getDato().getTermino());
        if (comparacion < 0) caminoRecursivo(nodo.getIzq(), clave, camino);
        else if (comparacion > 0) caminoRecursivo(nodo.getDer(), clave, camino);
    }

    // Instrumentación educativa: una comparación de búsqueda/inserción por nodo no nulo.
    public long getComparacionesInsercion() { return comparacionesInsercion; }
    public int getComparacionesUltimaBusqueda() { return comparacionesUltimaBusqueda; }
    public int getVisitasUltimoAutocompletado() { return visitasUltimoAutocompletado; }
    public int getVisitasUltimoRango() { return visitasUltimoRango; }
}

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Experimento reproducible: mismo conjunto de registros, dos órdenes de inserción. */
public class ExperimentoABB {
    public static void main(String[] args) throws IOException {
        System.out.println("Orden | Nodos | Altura | Comparaciones inserción | Búsquedas exitosas: total/media/máximo | Ausente zzzzz");
        for (Resultado resultado : ejecutar("terminos.csv")) resultado.imprimir();
    }

    /** Reutilizable por consola y web; mide árboles independientes del árbol de la aplicación. */
    public static List<Resultado> ejecutar(String rutaCSV) throws IOException {
        List<TerminoBusqueda> originales = LectorCSV.leer(rutaCSV);
        List<TerminoBusqueda> alfabeticos = new ArrayList<>(originales);
        ordenarAlfabeticamente(alfabeticos);
        List<Resultado> resultados = new ArrayList<>();
        resultados.add(medir("CSV original", originales));
        resultados.add(medir("Alfabético", alfabeticos));
        return resultados;
    }

    // Ordenación por inserción de una lista, independiente de los recorridos del ABB.
    private static void ordenarAlfabeticamente(List<TerminoBusqueda> registros) {
        for (int i = 1; i < registros.size(); i++) {
            TerminoBusqueda actual = registros.get(i);
            int j = i - 1;
            while (j >= 0 && registros.get(j).getTermino().compareTo(actual.getTermino()) > 0) {
                registros.set(j + 1, registros.get(j));
                j--;
            }
            registros.set(j + 1, actual);
        }
    }

    private static Resultado medir(String nombre, List<TerminoBusqueda> registros) {
        ArbolBinarioBusqueda arbol = new ArbolBinarioBusqueda();
        for (TerminoBusqueda registro : registros) arbol.insertar(registro);
        int total = 0;
        int maximo = 0;
        for (TerminoBusqueda registro : registros) {
            arbol.buscar(registro.getTermino());
            int comparaciones = arbol.getComparacionesUltimaBusqueda();
            total += comparaciones;
            maximo = Math.max(maximo, comparaciones);
        }
        arbol.buscar("zzzzz");
        int ausente = arbol.getComparacionesUltimaBusqueda();
        double media = registros.isEmpty() ? 0 : (double) total / registros.size();
        arbol.autocompletar("ja");
        int visitasPrefijo = arbol.getVisitasUltimoAutocompletado();
        arbol.buscarPorRango("java", "python");
        return new Resultado(nombre, arbol.contarTotalNodos(), arbol.obtenerAltura(),
                arbol.getComparacionesInsercion(), total, media, maximo, ausente,
                visitasPrefijo, arbol.getVisitasUltimoRango(), arbol.esABBValido());
    }

    /** Solo valores medidos: no contiene referencias a los árboles del experimento. */
    public static final class Resultado {
        private final String orden;
        private final int nodos;
        private final int altura;
        private final long insercion;
        private final int totalBusqueda;
        private final double mediaBusqueda;
        private final int maximoBusqueda;
        private final int ausente;
        private final int visitasPrefijo;
        private final int visitasRango;
        private final boolean valido;

        private Resultado(String orden, int nodos, int altura, long insercion, int totalBusqueda,
                          double mediaBusqueda, int maximoBusqueda, int ausente,
                          int visitasPrefijo, int visitasRango, boolean valido) {
            this.orden = orden;
            this.nodos = nodos;
            this.altura = altura;
            this.insercion = insercion;
            this.totalBusqueda = totalBusqueda;
            this.mediaBusqueda = mediaBusqueda;
            this.maximoBusqueda = maximoBusqueda;
            this.ausente = ausente;
            this.visitasPrefijo = visitasPrefijo;
            this.visitasRango = visitasRango;
            this.valido = valido;
        }

        public String getOrden() { return orden; }
        public int getNodos() { return nodos; }
        public int getAltura() { return altura; }
        public long getInsercion() { return insercion; }
        public int getTotalBusqueda() { return totalBusqueda; }
        public double getMediaBusqueda() { return mediaBusqueda; }
        public int getMaximoBusqueda() { return maximoBusqueda; }
        public int getAusente() { return ausente; }
        public int getVisitasPrefijo() { return visitasPrefijo; }
        public int getVisitasRango() { return visitasRango; }
        public boolean isValido() { return valido; }

        private void imprimir() {
            System.out.printf(Locale.ROOT, "%s | %d | %d | %d | %d / %.2f / %d | %d%n",
                    orden, nodos, altura, insercion, totalBusqueda, mediaBusqueda, maximoBusqueda, ausente);
            System.out.println("  Prefijo ja: " + visitasPrefijo + " nodos visitados.");
            System.out.println("  Rango [java, python]: " + visitasRango + " nodos visitados.");
            System.out.println("  Orden ABB válido: " + valido);
        }
    }
}

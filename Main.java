import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Presentación y lectura de consola. La lógica de consultas permanece en el ABB. */
public class Main {
    public static void main(String[] args) {
        ArbolBinarioBusqueda arbol = new ArbolBinarioBusqueda();
        try {
            System.out.println("Carga inicial: " + cargarDatosPrueba(arbol) + " registros.");
        } catch (IOException e) {
            System.out.println("No se pudo cargar terminos.csv: " + e.getMessage());
            System.out.println("Se iniciará con un árbol vacío. Ejecute desde la carpeta del proyecto.");
        }

        try (Scanner sc = new Scanner(System.in)) {
            int opcion;
            do {
                mostrarMenu();
                opcion = leerEntero(sc, "Elige una opción (0-13): ", 0, 13);
                try {
                    switch (opcion) {
                        case 1:
                            insertar(sc, arbol);
                            break;
                        case 2:
                            mostrarTermino(arbol.registrarBusqueda(leerTexto(sc, "Término a buscar: ")),
                                    "Término no encontrado.");
                            break;
                        case 3:
                            mostrarRecorridos(sc, arbol);
                            break;
                        case 4:
                            mostrarLista(arbol.autocompletar(leerTexto(sc, "Prefijo: ")));
                            System.out.println("Nodos visitados: " + arbol.getVisitasUltimoAutocompletado());
                            break;
                        case 5:
                            System.out.println("Mínimo:");
                            mostrarTermino(arbol.obtenerMinimo(), "Árbol vacío.");
                            System.out.println("Máximo:");
                            mostrarTermino(arbol.obtenerMaximo(), "Árbol vacío.");
                            break;
                        case 6:
                            actualizar(sc, arbol);
                            break;
                        case 7:
                            System.out.println("Total: " + arbol.contarTotalNodos());
                            System.out.println("Hojas: " + arbol.contarHojas());
                            System.out.println("Internos: " + arbol.getNodosInternos());
                            System.out.println("Altura (aristas): " + arbol.obtenerAltura());
                            break;
                        case 8:
                            int nivel = arbol.obtenerNivel(leerTexto(sc, "Término: "));
                            System.out.println(nivel < 0 ? "Término no encontrado." : "Nivel: " + nivel);
                            break;
                        case 9:
                            boolean eliminado = arbol.eliminar(leerTexto(sc, "Término a eliminar: "));
                            System.out.println(eliminado ? "Término eliminado." : "Término no encontrado.");
                            break;
                        case 10:
                            String clave = leerTexto(sc, "Término de referencia: ");
                            System.out.println("Predecesor:");
                            mostrarTermino(arbol.predecesor(clave), "No hay predecesor.");
                            System.out.println("Sucesor:");
                            mostrarTermino(arbol.sucesor(clave), "No hay sucesor.");
                            break;
                        case 11:
                            String desde = leerTexto(sc, "Desde (incluido): ");
                            String hasta = leerTexto(sc, "Hasta (incluido): ");
                            mostrarLista(arbol.buscarPorRango(desde, hasta));
                            System.out.println("Nodos visitados: " + arbol.getVisitasUltimoRango());
                            break;
                        case 12:
                            System.out.print(arbol.visualizarEstructura());
                            break;
                        case 13:
                            System.out.println("Top 5 por búsquedas (empates en orden alfabético):");
                            mostrarLista(arbol.top5());
                            break;
                        case 0:
                            System.out.println("Saliendo del programa...");
                            break;
                        default:
                            break;
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println("Operación rechazada: " + e.getMessage());
                }
            } while (opcion != 0);
        } catch (NoSuchElementException e) {
            System.out.println("\nFin de la entrada. Programa finalizado.");
        }
    }

    private static void mostrarMenu() {
        System.out.println("\n=== GOOGLE SEARCH - ÍNDICE DE AUTOCOMPLETADO ===");
        System.out.println("1. Insertar término (duplicado: +1 búsqueda y fecha actual)");
        System.out.println("2. Buscar término y registrar una búsqueda");
        System.out.println("3. Recorridos: inorden, preorden, postorden y niveles");
        System.out.println("4. Autocompletar por prefijo");
        System.out.println("5. Mínimo y máximo alfabético");
        System.out.println("6. Actualizar contador, categoría y fecha");
        System.out.println("7. Conteos y altura");
        System.out.println("8. Nivel de un término");
        System.out.println("9. Eliminar término");
        System.out.println("10. Predecesor y sucesor alfabético");
        System.out.println("11. Buscar por rango alfabético inclusivo");
        System.out.println("12. Ver estructura del ABB");
        System.out.println("13. Top 5 más buscados");
        System.out.println("0. Salir");
    }

    private static void insertar(Scanner sc, ArbolBinarioBusqueda arbol) {
        String termino = leerTexto(sc, "Término: ");
        int busquedas = leerEntero(sc, "Número inicial de búsquedas: ", 0, Integer.MAX_VALUE);
        String categoria = leerTexto(sc, "Categoría: ");
        String fecha = leerTexto(sc, "Fecha (AAAA-MM-DD): ");
        TerminoBusqueda nuevo = new TerminoBusqueda(termino, busquedas, categoria, fecha);
        boolean duplicado = arbol.buscar(termino) != null;
        arbol.insertar(nuevo);
        System.out.println(duplicado ? "Ya existía: contador incrementado y fecha actualizada."
                : "Término insertado correctamente.");
        System.out.println(arbol.buscar(termino));
    }

    private static void actualizar(Scanner sc, ArbolBinarioBusqueda arbol) {
        String termino = leerTexto(sc, "Término a actualizar (la clave se conserva): ");
        int busquedas = leerEntero(sc, "Nuevo número de búsquedas: ", 0, Integer.MAX_VALUE);
        String categoria = leerTexto(sc, "Nueva categoría: ");
        String fecha = leerTexto(sc, "Nueva fecha (AAAA-MM-DD): ");
        boolean actualizado = arbol.actualizarTermino(termino, busquedas, categoria, fecha);
        System.out.println(actualizado ? "Término actualizado." : "Término no encontrado.");
    }

    /** Un único submenú, con retorno explícito y sin caída accidental al siguiente case. */
    private static void mostrarRecorridos(Scanner sc, ArbolBinarioBusqueda arbol) {
        int opcion;
        do {
            System.out.println("\n1. Inorden  2. Preorden  3. Postorden  4. Por niveles  0. Volver");
            opcion = leerEntero(sc, "Recorrido (0-4): ", 0, 4);
            switch (opcion) {
                case 1: mostrarLista(arbol.inOrden()); break;
                case 2: mostrarLista(arbol.preOrden()); break;
                case 3: mostrarLista(arbol.postOrden()); break;
                case 4: mostrarLista(arbol.porNiveles()); break;
                default: break;
            }
        } while (opcion != 0);
    }

    /** Siempre lee líneas completas: no queda un salto pendiente tras leer un entero. */
    private static int leerEntero(Scanner sc, String mensaje, int minimo, int maximo) {
        while (true) {
            String texto = leerLinea(sc, mensaje).trim();
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) return valor;
            } catch (NumberFormatException e) {
                // Se pide otra línea, incluso si el número es demasiado grande.
            }
            System.out.println("Entrada inválida. Ingresa un entero entre " + minimo + " y " + maximo + ".");
        }
    }

    private static String leerTexto(Scanner sc, String mensaje) {
        while (true) {
            String texto = leerLinea(sc, mensaje).trim();
            if (!texto.isEmpty()) return texto;
            System.out.println("El texto no puede estar vacío.");
        }
    }

    private static String leerLinea(Scanner sc, String mensaje) {
        System.out.print(mensaje);
        if (!sc.hasNextLine()) throw new NoSuchElementException("Fin de la entrada");
        return sc.nextLine();
    }

    private static void mostrarTermino(TerminoBusqueda termino, String mensajeAusente) {
        System.out.println(termino == null ? mensajeAusente : termino);
    }

    private static void mostrarLista(List<TerminoBusqueda> resultados) {
        if (resultados.isEmpty()) System.out.println("Sin resultados (árbol vacío o sin coincidencias).");
        for (TerminoBusqueda termino : resultados) System.out.println(termino);
    }

    public static int cargarDatosPrueba(ArbolBinarioBusqueda arbol) throws IOException {
        return LectorCSV.cargarEnArbol("terminos.csv", arbol);
    }
}

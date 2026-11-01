import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Pruebas sin JUnit ni dependencias. Cada caso se ejecuta aunque falle otro. */
public class PruebasABB {
    private static int aprobadas;
    private static int fallidas;
    private static int verificaciones;
    private static final String FECHA = "2026-10-01";

    // Esta interfaz solo permite agrupar y reportar los casos de prueba por nombre.
    private interface Prueba {
        void ejecutar() throws Exception;
    }

    public static void main(String[] args) {
        ejecutar("Carga de 30 registros, orden y atributos", PruebasABB::cargaCSV);
        ejecutar("CSV original sin cambios (SHA-256)", PruebasABB::integridadCSV);
        ejecutar("Inserción nueva y normalización", PruebasABB::insercionNueva);
        ejecutar("Duplicado: contador +1, fecha y sin reemplazo", PruebasABB::duplicado);
        ejecutar("Búsqueda exacta sin efectos secundarios", PruebasABB::busquedaExistente);
        ejecutar("Búsqueda inexistente", PruebasABB::busquedaInexistente);
        ejecutar("Registro de búsqueda: popularidad y fecha", PruebasABB::registroBusqueda);
        ejecutar("Registrar búsqueda inexistente", PruebasABB::registroInexistente);
        ejecutar("Actualización válida sin cambiar la clave", PruebasABB::actualizacion);
        ejecutar("Actualización de clave ausente", PruebasABB::actualizacionAusente);
        ejecutar("Actualización inválida es atómica", PruebasABB::actualizacionAtomica);
        ejecutar("Eliminar hoja", PruebasABB::eliminarHoja);
        ejecutar("Eliminar nodo con hijo izquierdo", PruebasABB::eliminarHijoIzquierdo);
        ejecutar("Eliminar nodo con hijo derecho", PruebasABB::eliminarHijoDerecho);
        ejecutar("Dos hijos: sucesor profundo con hijo derecho", PruebasABB::eliminarDosHijos);
        ejecutar("Eliminar raíz única", PruebasABB::eliminarRaizUnica);
        ejecutar("Eliminar raíz con un hijo", PruebasABB::eliminarRaizUnHijo);
        ejecutar("Eliminar raíz con dos hijos y sucesor inmediato", PruebasABB::eliminarRaizDosHijos);
        ejecutar("Eliminar ausente no modifica el árbol", PruebasABB::eliminarAusente);
        ejecutar("Orden ABB tras 120 inserciones y 120 eliminaciones", PruebasABB::ordenTrasMutaciones);
        ejecutar("Recorrido inorden", PruebasABB::inorden);
        ejecutar("Recorrido preorden", PruebasABB::preorden);
        ejecutar("Recorrido postorden", PruebasABB::postorden);
        ejecutar("Recorrido FIFO por niveles", PruebasABB::niveles);
        ejecutar("Mínimo y máximo", PruebasABB::extremos);
        ejecutar("Sucesor, límites y clave ausente", PruebasABB::sucesores);
        ejecutar("Predecesor, límites y clave ausente", PruebasABB::predecesores);
        ejecutar("Rango inclusivo y normalización", PruebasABB::rango);
        ejecutar("Rango sin coincidencias", PruebasABB::rangoVacio);
        ejecutar("Rango con poda observable", PruebasABB::podaRango);
        ejecutar("Rango invertido rechazado", PruebasABB::rangoInvertido);
        ejecutar("Autocompletado ordenado y normalizado", PruebasABB::prefijo);
        ejecutar("Autocompletado con poda observable", PruebasABB::podaPrefijo);
        ejecutar("Prefijo inexistente", PruebasABB::prefijoAusente);
        ejecutar("Prefijos y rangos contra filtrado de referencia", PruebasABB::consultasContraReferencia);
        ejecutar("Top 5 original por popularidad", PruebasABB::topCSV);
        ejecutar("Top 5 con desempate alfabético", PruebasABB::topEmpates);
        ejecutar("Top 5 con menos de cinco nodos y cambios", PruebasABB::topPequeno);
        ejecutar("Conteos, altura y nivel", PruebasABB::estadisticas);
        ejecutar("Visualización textual", PruebasABB::estructura);
        ejecutar("Árbol vacío: todas las consultas", PruebasABB::vacio);
        ejecutar("Clave inmutable y atributos privados", PruebasABB::encapsulacion);
        ejecutar("Términos nulos o vacíos rechazados", PruebasABB::terminosInvalidos);
        ejecutar("Contadores negativos y desbordamiento", PruebasABB::contadoresInvalidos);
        ejecutar("Fechas inexistentes y formato estricto", PruebasABB::fechasInvalidas);
        ejecutar("Categorías vacías y datos nulos", PruebasABB::otrosDatosInvalidos);
        ejecutar("Representación incluye los cuatro atributos", PruebasABB::representacionCompleta);
        ejecutar("CSV inválido no carga parcialmente", PruebasABB::csvInvalido);
        ejecutar("CSV con BOM, espacios y líneas vacías", PruebasABB::csvBOM);
        ejecutar("CSV faltante informa IOException", PruebasABB::csvAusente);
        ejecutar("Menú: inserción captura categoría y fecha", PruebasABB::menuInsercion);
        ejecutar("Menú: enteros inválidos y recuperación", PruebasABB::menuNumerosInvalidos);
        ejecutar("Menú: cuatro recorridos y break del caso 3", PruebasABB::menuRecorridos);
        ejecutar("Menú: fechas y rangos inválidos no cierran", PruebasABB::menuDatosInvalidos);
        ejecutar("Menú: fin de entrada sin excepción", PruebasABB::menuFinEntrada);
        ejecutar("Menú: opciones 2 y 4 a 13", PruebasABB::menuOperaciones);
        ejecutar("Instrumentación de comparaciones", PruebasABB::comparaciones);
        System.out.println("\nRESULTADO: " + aprobadas + " aprobadas, " + fallidas
                + " fallidas; " + verificaciones + " verificaciones.");
        if (fallidas != 0) throw new AssertionError("Hay pruebas fallidas.");
    }

    private static void ejecutar(String nombre, Prueba prueba) {
        try {
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
        comprobar(esperado == null ? real == null : esperado.equals(real),
                "Esperado: " + esperado + "; obtenido: " + real);
    }

    private static void rechaza(Prueba operacion) throws Exception {
        try {
            operacion.ejecutar();
        } catch (IllegalArgumentException e) {
            comprobar(true, "Entrada inválida rechazada");
            return;
        }
        comprobar(false, "Se esperaba IllegalArgumentException");
    }

    private static TerminoBusqueda dato(String clave) {
        return new TerminoBusqueda(clave, 10, "tecnologia", FECHA);
    }

    private static ArbolBinarioBusqueda arbol(String... claves) {
        ArbolBinarioBusqueda arbol = new ArbolBinarioBusqueda();
        for (String clave : claves) arbol.insertar(dato(clave));
        return arbol;
    }

    private static ArbolBinarioBusqueda balanceado() {
        return arbol("d", "b", "f", "a", "c", "e", "g");
    }

    private static List<String> claves(List<TerminoBusqueda> datos) {
        List<String> claves = new ArrayList<>();
        for (TerminoBusqueda dato : datos) claves.add(dato.getTermino());
        return claves;
    }

    private static void secuencia(List<TerminoBusqueda> datos, String... esperadas) {
        igual(Arrays.asList(esperadas), claves(datos));
    }

    private static void ordenValido(ArbolBinarioBusqueda arbol) {
        comprobar(arbol.esABBValido(), "Se violó la propiedad del ABB");
        List<String> orden = claves(arbol.inOrden());
        igual(orden.size(), arbol.contarTotalNodos());
        for (int i = 1; i < orden.size(); i++) {
            comprobar(orden.get(i - 1).compareTo(orden.get(i)) < 0, "Inorden no es estrictamente creciente");
        }
    }

    private static void cargaCSV() throws IOException {
        List<TerminoBusqueda> registros = LectorCSV.leer("terminos.csv");
        igual(30, registros.size());
        igual("medellin", registros.get(0).getTermino());
        igual("sabaneta parques", registros.get(29).getTermino());
        ArbolBinarioBusqueda arbol = new ArbolBinarioBusqueda();
        igual(30, LectorCSV.cargarEnArbol("terminos.csv", arbol));
        igual(30, arbol.contarTotalNodos());
        igual("medellin", arbol.preOrden().get(0).getTermino());
        for (TerminoBusqueda original : registros) {
            TerminoBusqueda cargado = arbol.buscar(original.getTermino());
            igual(original.getNumBusquedas(), cargado.getNumBusquedas());
            igual(original.getCategoria(), cargado.getCategoria());
            igual(original.getFechaUltBusqueda(), cargado.getFechaUltBusqueda());
        }
        ordenValido(arbol);
    }

    private static void integridadCSV() throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Paths.get("terminos.csv")));
        StringBuilder hexadecimal = new StringBuilder();
        for (byte valor : hash) hexadecimal.append(String.format("%02X", valor & 0xff));
        igual("6015F70B3EA846EFD32AC4234C3D9221FD5A48CAB6DC5C1204F82C25A8E1F843", hexadecimal.toString());
    }

    private static void insercionNueva() {
        ArbolBinarioBusqueda arbol = arbol("  JAVA  ");
        igual("java", arbol.obtenerMinimo().getTermino());
        igual(10, arbol.buscar(" JAVA ").getNumBusquedas());
        igual(0, arbol.obtenerNivel(" java "));
        ordenValido(arbol);
    }

    private static void duplicado() {
        ArbolBinarioBusqueda arbol = arbol("java");
        TerminoBusqueda original = arbol.buscar("java");
        arbol.insertar(new TerminoBusqueda(" JAVA ", 999, "otra", "2020-01-01"));
        igual(1, arbol.contarTotalNodos());
        comprobar(original == arbol.buscar("java"), "El duplicado reemplazó el objeto");
        igual(11, original.getNumBusquedas());
        igual("tecnologia", original.getCategoria());
        igual(LocalDate.now().toString(), original.getFechaUltBusqueda());
        ordenValido(arbol);
    }

    private static void busquedaExistente() {
        ArbolBinarioBusqueda arbol = balanceado();
        igual("c", arbol.buscar(" C ").getTermino());
        igual(10, arbol.buscar("c").getNumBusquedas());
        igual(FECHA, arbol.buscar("c").getFechaUltBusqueda());
    }

    private static void busquedaInexistente() { igual(null, balanceado().buscar("zzz")); }

    private static void registroBusqueda() {
        ArbolBinarioBusqueda arbol = arbol("java");
        TerminoBusqueda resultado = arbol.registrarBusqueda(" JAVA ");
        igual(11, resultado.getNumBusquedas());
        igual(LocalDate.now().toString(), resultado.getFechaUltBusqueda());
    }

    private static void registroInexistente() {
        ArbolBinarioBusqueda arbol = balanceado();
        igual(null, arbol.registrarBusqueda("ausente"));
        igual(7, arbol.contarTotalNodos());
    }

    private static void actualizacion() {
        ArbolBinarioBusqueda arbol = balanceado();
        comprobar(arbol.actualizarTermino(" B ", 99, " nueva ", "2024-02-29"), "No actualizó");
        TerminoBusqueda actualizado = arbol.buscar("b");
        igual("b", actualizado.getTermino());
        igual(99, actualizado.getNumBusquedas());
        igual("nueva", actualizado.getCategoria());
        igual("2024-02-29", actualizado.getFechaUltBusqueda());
        ordenValido(arbol);
    }

    private static void actualizacionAusente() {
        ArbolBinarioBusqueda arbol = balanceado();
        comprobar(!arbol.actualizarTermino("x", 1, "otra", FECHA), "Actualizó una clave ausente");
        igual(7, arbol.contarTotalNodos());
    }

    private static void actualizacionAtomica() throws Exception {
        ArbolBinarioBusqueda arbol = arbol("java");
        String antes = arbol.buscar("java").toString();
        rechaza(() -> arbol.actualizarTermino("java", 100, "otra", "2026-02-30"));
        igual(antes, arbol.buscar("java").toString());
        rechaza(() -> arbol.actualizarTermino("java", -1, "otra", FECHA));
        rechaza(() -> arbol.actualizarTermino("java", 100, " ", FECHA));
        igual(antes, arbol.buscar("java").toString());
    }

    private static void eliminarHoja() {
        ArbolBinarioBusqueda arbol = balanceado();
        comprobar(arbol.eliminar(" A "), "No eliminó la hoja");
        igual(null, arbol.buscar("a"));
        secuencia(arbol.inOrden(), "b", "c", "d", "e", "f", "g");
        ordenValido(arbol);
    }

    private static void eliminarHijoIzquierdo() {
        ArbolBinarioBusqueda arbol = arbol("m", "f", "d");
        comprobar(arbol.eliminar("f"), "No eliminó el nodo");
        secuencia(arbol.preOrden(), "m", "d");
        ordenValido(arbol);
    }

    private static void eliminarHijoDerecho() {
        ArbolBinarioBusqueda arbol = arbol("m", "f", "h");
        comprobar(arbol.eliminar("f"), "No eliminó el nodo");
        secuencia(arbol.preOrden(), "m", "h");
        ordenValido(arbol);
    }

    private static void eliminarDosHijos() {
        ArbolBinarioBusqueda arbol = arbol("m", "f", "b", "k", "h", "i", "z");
        arbol.actualizarTermino("h", 123, "sucesor", "2024-02-29");
        comprobar(arbol.eliminar("f"), "No eliminó dos hijos");
        secuencia(arbol.preOrden(), "m", "h", "b", "k", "i", "z");
        igual(123, arbol.buscar("h").getNumBusquedas());
        igual("sucesor", arbol.buscar("h").getCategoria());
        igual("2024-02-29", arbol.buscar("h").getFechaUltBusqueda());
        igual(null, arbol.buscar("f"));
        ordenValido(arbol);
    }

    private static void eliminarRaizUnica() {
        ArbolBinarioBusqueda arbol = arbol("m");
        comprobar(arbol.eliminar("m"), "No eliminó raíz");
        comprobar(arbol.estaVacio(), "No quedó vacío");
        igual(-1, arbol.obtenerAltura());
        ordenValido(arbol);
    }

    private static void eliminarRaizUnHijo() {
        ArbolBinarioBusqueda izquierda = arbol("m", "a");
        comprobar(izquierda.eliminar("m"), "No eliminó raíz");
        secuencia(izquierda.preOrden(), "a");
        ordenValido(izquierda);
        ArbolBinarioBusqueda derecha = arbol("m", "z");
        comprobar(derecha.eliminar("m"), "No eliminó raíz");
        secuencia(derecha.preOrden(), "z");
        ordenValido(derecha);
    }

    private static void eliminarRaizDosHijos() {
        ArbolBinarioBusqueda arbol = arbol("m", "a", "z", "zz");
        comprobar(arbol.eliminar("m"), "No eliminó raíz");
        secuencia(arbol.preOrden(), "z", "a", "zz");
        ordenValido(arbol);
        ArbolBinarioBusqueda profundo = balanceado();
        comprobar(profundo.eliminar("d"), "No eliminó raíz con sucesor profundo");
        secuencia(profundo.preOrden(), "e", "b", "a", "c", "f", "g");
        ordenValido(profundo);
    }

    private static void eliminarAusente() {
        ArbolBinarioBusqueda arbol = balanceado();
        String antes = arbol.visualizarEstructura();
        comprobar(!arbol.eliminar("x"), "Eliminó ausente");
        igual(antes, arbol.visualizarEstructura());
    }

    private static void ordenTrasMutaciones() {
        List<String> esperadas = new ArrayList<>();
        for (int i = 0; i < 120; i++) esperadas.add(String.format("k%03d", i));
        List<String> mezcla = new ArrayList<>(esperadas);
        Collections.shuffle(mezcla, new Random(42));
        ArbolBinarioBusqueda arbol = new ArbolBinarioBusqueda();
        List<String> insertadas = new ArrayList<>();
        for (String clave : mezcla) {
            arbol.insertar(dato(clave));
            insertadas.add(clave);
            Collections.sort(insertadas);
            igual(insertadas, claves(arbol.inOrden()));
            ordenValido(arbol);
        }
        Collections.shuffle(mezcla, new Random(73));
        for (String clave : mezcla) {
            comprobar(arbol.eliminar(clave), "No eliminó " + clave);
            esperadas.remove(clave);
            igual(esperadas, claves(arbol.inOrden()));
            ordenValido(arbol);
        }
        comprobar(arbol.estaVacio(), "Quedaron datos");
    }

    private static void inorden() { secuencia(balanceado().inOrden(), "a", "b", "c", "d", "e", "f", "g"); }
    private static void preorden() { secuencia(balanceado().preOrden(), "d", "b", "a", "c", "f", "e", "g"); }
    private static void postorden() { secuencia(balanceado().postOrden(), "a", "c", "b", "e", "g", "f", "d"); }
    private static void niveles() { secuencia(balanceado().porNiveles(), "d", "b", "f", "a", "c", "e", "g"); }

    private static void extremos() {
        igual("a", balanceado().obtenerMinimo().getTermino());
        igual("g", balanceado().obtenerMaximo().getTermino());
    }

    private static void sucesores() {
        ArbolBinarioBusqueda arbol = balanceado();
        igual("e", arbol.sucesor(" D ").getTermino());
        igual("d", arbol.sucesor("c").getTermino());
        igual("b", arbol.sucesor("aa").getTermino());
        igual("a", arbol.sucesor("0").getTermino());
        igual(null, arbol.sucesor("g"));
        igual(null, arbol.sucesor("z"));
    }

    private static void predecesores() {
        ArbolBinarioBusqueda arbol = balanceado();
        igual("c", arbol.predecesor(" D ").getTermino());
        igual("d", arbol.predecesor("e").getTermino());
        igual("a", arbol.predecesor("aa").getTermino());
        igual("g", arbol.predecesor("z").getTermino());
        igual(null, arbol.predecesor("a"));
        igual(null, arbol.predecesor("0"));
    }

    private static void rango() {
        ArbolBinarioBusqueda arbol = balanceado();
        secuencia(arbol.buscarPorRango(" B ", " F "), "b", "c", "d", "e", "f");
        secuencia(arbol.buscarPorRango("d", "d"), "d");
        secuencia(arbol.buscarPorRango("bb", "ff"), "c", "d", "e", "f");
    }

    private static void rangoVacio() {
        secuencia(balanceado().buscarPorRango("x", "z"));
        secuencia(balanceado().buscarPorRango("bb", "bc"));
    }

    private static void podaRango() {
        ArbolBinarioBusqueda arbol = balanceado();
        secuencia(arbol.buscarPorRango("a", "a"), "a");
        igual(3, arbol.getVisitasUltimoRango());
        comprobar(arbol.getVisitasUltimoRango() < arbol.contarTotalNodos(), "No podó");
    }

    private static void rangoInvertido() throws Exception {
        rechaza(() -> balanceado().buscarPorRango("z", "a"));
    }

    private static void prefijo() {
        ArbolBinarioBusqueda arbol = arbol("m", "java", "z", "javascript", "jabon", "ja", "jazz", "a");
        secuencia(arbol.autocompletar(" JA "), "ja", "jabon", "java", "javascript", "jazz");
        secuencia(arbol.autocompletar("java"), "java", "javascript");
    }

    private static void podaPrefijo() {
        ArbolBinarioBusqueda arbol = balanceado();
        secuencia(arbol.autocompletar("a"), "a");
        igual(3, arbol.getVisitasUltimoAutocompletado());
        comprobar(arbol.getVisitasUltimoAutocompletado() < arbol.contarTotalNodos(), "No podó");
    }

    private static void prefijoAusente() {
        ArbolBinarioBusqueda arbol = balanceado();
        secuencia(arbol.autocompletar("zz"));
        igual(3, arbol.getVisitasUltimoAutocompletado());
    }

    private static void consultasContraReferencia() throws IOException {
        ArbolBinarioBusqueda arbol = new ArbolBinarioBusqueda();
        LectorCSV.cargarEnArbol("terminos.csv", arbol);
        List<TerminoBusqueda> ordenados = arbol.inOrden();
        for (TerminoBusqueda termino : ordenados) {
            String clave = termino.getTermino();
            for (int largo = 1; largo <= clave.length(); largo++) {
                String prefijo = clave.substring(0, largo);
                List<String> esperados = new ArrayList<>();
                for (TerminoBusqueda candidato : ordenados) {
                    if (candidato.getTermino().startsWith(prefijo)) esperados.add(candidato.getTermino());
                }
                igual(esperados, claves(arbol.autocompletar(prefijo)));
            }
        }
        for (int i = 0; i < ordenados.size(); i++) {
            for (int j = i; j < ordenados.size(); j++) {
                igual(claves(ordenados.subList(i, j + 1)), claves(arbol.buscarPorRango(
                        ordenados.get(i).getTermino(), ordenados.get(j).getTermino())));
            }
        }
    }

    private static void topCSV() throws IOException {
        ArbolBinarioBusqueda arbol = new ArbolBinarioBusqueda();
        LectorCSV.cargarEnArbol("terminos.csv", arbol);
        secuencia(arbol.top5(), "pico y placa medellin", "dolares a pesos", "noticias colombia", "vuelos baratos", "python");
        igual(8500, arbol.top5().get(0).getNumBusquedas());
    }

    private static void topEmpates() {
        ArbolBinarioBusqueda arbol = arbol("d", "b", "f", "a", "c", "e", "g");
        secuencia(arbol.top5(), "a", "b", "c", "d", "e");
        arbol.actualizarTermino("g", 11, "otra", FECHA);
        secuencia(arbol.top5(), "g", "a", "b", "c", "d");
    }

    private static void topPequeno() {
        ArbolBinarioBusqueda arbol = arbol("b", "a");
        secuencia(arbol.top5(), "a", "b");
        arbol.registrarBusqueda("b");
        secuencia(arbol.top5(), "b", "a");
        arbol.eliminar("b");
        secuencia(arbol.top5(), "a");
    }

    private static void estadisticas() {
        ArbolBinarioBusqueda arbol = balanceado();
        igual(7, arbol.contarTotalNodos());
        igual(4, arbol.contarHojas());
        igual(3, arbol.getNodosInternos());
        igual(3, arbol.contarInternos());
        igual(2, arbol.obtenerAltura());
        igual(0, arbol.obtenerNivel("d"));
        igual(2, arbol.obtenerNivel("a"));
        igual(-1, arbol.obtenerNivel("x"));
        igual(0, arbol("a").obtenerAltura());
        igual(0, arbol("a").contarInternos());
    }

    private static void estructura() {
        String texto = arbol("d", "b", "f").visualizarEstructura();
        comprobar(texto.startsWith("RAÍZ: Término: 'd'"), "Falta raíz");
        comprobar(texto.contains("    IZQ: Término: 'b'"), "Falta hijo izquierdo");
        comprobar(texto.contains("    DER: Término: 'f'"), "Falta hijo derecho");
        comprobar(texto.contains(FECHA), "Falta fecha");
    }

    private static void vacio() {
        ArbolBinarioBusqueda arbol = new ArbolBinarioBusqueda();
        comprobar(arbol.estaVacio(), "No está vacío");
        igual(null, arbol.buscar("a"));
        igual(null, arbol.registrarBusqueda("a"));
        igual(null, arbol.obtenerMinimo());
        igual(null, arbol.obtenerMaximo());
        igual(null, arbol.sucesor("a"));
        igual(null, arbol.predecesor("a"));
        secuencia(arbol.inOrden());
        secuencia(arbol.preOrden());
        secuencia(arbol.postOrden());
        secuencia(arbol.porNiveles());
        secuencia(arbol.autocompletar("a"));
        secuencia(arbol.buscarPorRango("a", "z"));
        secuencia(arbol.top5());
        igual(0, arbol.contarTotalNodos());
        igual(0, arbol.contarHojas());
        igual(0, arbol.getNodosInternos());
        igual(-1, arbol.obtenerAltura());
        igual(-1, arbol.obtenerNivel("a"));
        comprobar(!arbol.eliminar("a"), "Eliminó en vacío");
        comprobar(!arbol.actualizarTermino("a", 1, "otra", FECHA), "Actualizó en vacío");
        igual("Árbol vacío.\n", arbol.visualizarEstructura());
        ordenValido(arbol);
    }

    private static void encapsulacion() throws Exception {
        comprobar(Modifier.isFinal(TerminoBusqueda.class.getDeclaredField("termino").getModifiers()), "Clave mutable");
        for (java.lang.reflect.Method metodo : TerminoBusqueda.class.getMethods()) {
            comprobar(!metodo.getName().equals("setTermino"), "Se expone setTermino");
        }
        for (Class<?> clase : Arrays.asList(TerminoBusqueda.class, NodoABB.class, ArbolBinarioBusqueda.class)) {
            for (java.lang.reflect.Field campo : clase.getDeclaredFields()) {
                comprobar(Modifier.isPrivate(campo.getModifiers()), "Atributo no privado: " + campo);
            }
        }
    }

    private static void terminosInvalidos() throws Exception {
        ArbolBinarioBusqueda arbol = balanceado();
        for (String invalido : new String[] {null, "", "   "}) {
            rechaza(() -> dato(invalido));
            rechaza(() -> arbol.buscar(invalido));
            rechaza(() -> arbol.registrarBusqueda(invalido));
            rechaza(() -> arbol.eliminar(invalido));
            rechaza(() -> arbol.autocompletar(invalido));
            rechaza(() -> arbol.sucesor(invalido));
            rechaza(() -> arbol.predecesor(invalido));
            rechaza(() -> arbol.obtenerNivel(invalido));
            rechaza(() -> arbol.buscarPorRango(invalido, "z"));
            rechaza(() -> arbol.buscarPorRango("a", invalido));
        }
        ordenValido(arbol);
    }

    private static void contadoresInvalidos() throws Exception {
        rechaza(() -> new TerminoBusqueda("a", -1, "otra", FECHA));
        TerminoBusqueda termino = dato("a");
        rechaza(() -> termino.setNumBusquedas(-1));
        igual(10, termino.getNumBusquedas());
        termino.setNumBusquedas(0);
        igual(0, termino.getNumBusquedas());
        termino.setNumBusquedas(Integer.MAX_VALUE);
        rechaza(termino::incrementarNumBusquedas);
        igual(Integer.MAX_VALUE, termino.getNumBusquedas());
        igual(FECHA, termino.getFechaUltBusqueda());
        ArbolBinarioBusqueda arbol = new ArbolBinarioBusqueda();
        arbol.insertar(termino);
        rechaza(() -> arbol.insertar(dato("a")));
        igual(1, arbol.contarTotalNodos());
        ordenValido(arbol);
    }

    private static void fechasInvalidas() throws Exception {
        for (String fecha : new String[] {null, "", "2026-02-29", "2026-04-31", "2026-13-01",
                "2026-00-01", "2026-01-00", "01/11/2026", "2026-1-01", "2026-01-01extra"}) {
            rechaza(() -> new TerminoBusqueda("a", 0, "otra", fecha));
            TerminoBusqueda termino = dato("a");
            rechaza(() -> termino.setFecha(fecha));
            igual(FECHA, termino.getFechaUltBusqueda());
        }
        igual("2024-02-29", new TerminoBusqueda("a", 0, "otra", "2024-02-29").getFechaUltBusqueda());
    }

    private static void otrosDatosInvalidos() throws Exception {
        rechaza(() -> new TerminoBusqueda("a", 0, null, FECHA));
        rechaza(() -> new TerminoBusqueda("a", 0, "  ", FECHA));
        rechaza(() -> balanceado().insertar(null));
        rechaza(() -> new NodoABB(null));
        TerminoBusqueda termino = dato("a");
        rechaza(() -> termino.setCategoria(""));
        igual("tecnologia", termino.getCategoria());
    }

    private static void representacionCompleta() {
        String texto = dato("java").toString();
        comprobar(texto.contains("java") && texto.contains("10") && texto.contains("tecnologia")
                && texto.contains(FECHA), "Faltan atributos");
    }

    private static void csvInvalido() throws Exception {
        for (String contenido : new String[] {"", "cabecera incorrecta\n",
                "termino,busquedas,categoria,fecha\na,1,otra,2026-10-01\nb,-1,otra,2026-10-01\n",
                "termino,busquedas,categoria,fecha\na,x,otra,2026-10-01\n",
                "termino,busquedas,categoria,fecha\na,1,otra,2026-02-30\n",
                "termino,busquedas,categoria,fecha\na,1,otra\n",
                "termino,busquedas,categoria,fecha\na,1,otra,2026-10-01,extra\n",
                "termino,busquedas,categoria,fecha\n,1,otra,2026-10-01\n"}) {
            Path archivo = Files.createTempFile("abb-invalido-", ".csv");
            try {
                Files.write(archivo, contenido.getBytes(StandardCharsets.UTF_8));
                ArbolBinarioBusqueda arbol = arbol("original");
                boolean fallo = false;
                try {
                    LectorCSV.cargarEnArbol(archivo.toString(), arbol);
                } catch (IOException e) {
                    fallo = true;
                    comprobar(!e.getMessage().isEmpty(), "Error sin contexto");
                }
                comprobar(fallo, "CSV inválido aceptado");
                secuencia(arbol.inOrden(), "original");
            } finally {
                Files.deleteIfExists(archivo);
            }
        }
    }

    private static void csvBOM() throws IOException {
        Path archivo = Files.createTempFile("abb-bom-", ".csv");
        try {
            Files.write(archivo, ("\uFEFFtermino,busquedas,categoria,fecha\n\n JAVA , 42 , tecnologia , 2024-02-29 \n")
                    .getBytes(StandardCharsets.UTF_8));
            List<TerminoBusqueda> datos = LectorCSV.leer(archivo.toString());
            igual(1, datos.size());
            igual("java", datos.get(0).getTermino());
            igual(42, datos.get(0).getNumBusquedas());
            igual("tecnologia", datos.get(0).getCategoria());
            igual("2024-02-29", datos.get(0).getFechaUltBusqueda());
        } finally {
            Files.deleteIfExists(archivo);
        }
    }

    private static void csvAusente() throws IOException {
        Path carpeta = Files.createTempDirectory("abb-ausente-");
        try {
            boolean fallo = false;
            try {
                LectorCSV.leer(carpeta.resolve("no-existe.csv").toString());
            } catch (IOException e) {
                fallo = true;
            }
            comprobar(fallo, "No informó archivo faltante");
        } finally {
            Files.deleteIfExists(carpeta);
        }
    }

    private static String ejecutarConsola(String entrada) throws Exception {
        InputStream entradaOriginal = System.in;
        PrintStream salidaOriginal = System.out;
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (PrintStream captura = new PrintStream(salida, true, "UTF-8")) {
            System.setIn(new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)));
            System.setOut(captura);
            Main.main(new String[0]);
        } finally {
            System.setIn(entradaOriginal);
            System.setOut(salidaOriginal);
        }
        return salida.toString("UTF-8");
    }

    private static void menuInsercion() throws Exception {
        String salida = ejecutarConsola("1\n  PRUEBA MENU  \n42\n categoria de prueba \n2024-02-29\n0\n");
        comprobar(salida.contains("Término: 'prueba menu' | Búsquedas: 42 | Categoría: categoria de prueba | Última búsqueda: 2024-02-29"),
                "Scanner perdió categoría o fecha");
        comprobar(salida.contains("Saliendo del programa"), "No terminó normalmente");
    }

    private static void menuNumerosInvalidos() throws Exception {
        String salida = ejecutarConsola("abc\n99\n-1\n999999999999999\n\n1\n\nprueba\nno\n-5\n999999999999\n7\n\notra\n2024-02-29\n7\n0\n");
        comprobar(salida.contains("Entrada inválida"), "No validó enteros");
        comprobar(salida.contains("El texto no puede estar vacío"), "No validó textos");
        comprobar(salida.contains("Búsquedas: 7 | Categoría: otra | Última búsqueda: 2024-02-29"), "No recuperó inserción");
        comprobar(salida.contains("Total: 31"), "No continuó tras errores");
    }

    private static void menuRecorridos() throws Exception {
        String salida = ejecutarConsola("3\nno\n8\n1\n2\n3\n4\n0\n7\n0\n");
        comprobar(!salida.contains("Prefijo: "), "Caso 3 cayó al caso 4");
        comprobar(salida.contains("Total: 30"), "No retornó al menú");
        int apariciones = salida.split("Término: 'medellin'", -1).length - 1;
        igual(4, apariciones);
    }

    private static void menuDatosInvalidos() throws Exception {
        String salida = ejecutarConsola("1\nprueba\n1\notra\n2026-02-30\n6\njava\n999\notra\nfecha\n11\nz\na\n7\n0\n");
        igual(3, salida.split("Operación rechazada:", -1).length - 1);
        comprobar(salida.contains("Total: 30"), "Cambió árbol tras inserción inválida");
        comprobar(salida.contains("Saliendo del programa"), "Se cerró por entrada inválida");
    }

    private static void menuFinEntrada() throws Exception {
        comprobar(ejecutarConsola("").contains("Fin de la entrada"), "Falló EOF inicial");
        comprobar(ejecutarConsola("1\njava\n").contains("Fin de la entrada"), "Falló EOF en inserción");
        comprobar(ejecutarConsola("3\n").contains("Fin de la entrada"), "Falló EOF en submenú");
    }

    private static void menuOperaciones() throws Exception {
        String salida = ejecutarConsola("2\njava\n4\nja\n5\n6\njava\n4321\nactualizada\n2024-02-29\n7\n8\nmedellin\n9\njava\n10\nmedellin\n11\njava\npython\n12\n13\n0\n");
        comprobar(salida.contains("Búsquedas: 4201"), "No registró búsqueda");
        comprobar(salida.contains("Nodos visitados:"), "No ejecutó consultas con poda");
        comprobar(salida.contains("Mínimo:") && salida.contains("Máximo:"), "Faltan extremos");
        comprobar(salida.contains("Término actualizado."), "No actualizó");
        comprobar(salida.contains("Total: 30") && salida.contains("Nivel: 0"), "Faltan estadísticas");
        comprobar(salida.contains("Término eliminado."), "No eliminó");
        comprobar(salida.contains("Predecesor:") && salida.contains("Sucesor:"), "Faltan vecinos");
        comprobar(salida.contains("RAÍZ: Término: 'medellin'"), "Falta dibujo");
        comprobar(salida.contains("Top 5 por búsquedas"), "Falta top");
        comprobar(!salida.contains("Operación rechazada:"), "Una operación válida fue rechazada");
    }

    private static void comparaciones() {
        ArbolBinarioBusqueda arbol = balanceado();
        igual(10L, arbol.getComparacionesInsercion());
        arbol.buscar("d");
        igual(1, arbol.getComparacionesUltimaBusqueda());
        arbol.buscar("a");
        igual(3, arbol.getComparacionesUltimaBusqueda());
        arbol.buscar("z");
        igual(3, arbol.getComparacionesUltimaBusqueda());
        arbol.insertar(dato("d"));
        igual(11L, arbol.getComparacionesInsercion());
        igual(0, new ArbolBinarioBusqueda().getComparacionesUltimaBusqueda());
    }
}

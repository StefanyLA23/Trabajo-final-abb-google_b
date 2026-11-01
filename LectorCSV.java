import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Lee el CSV sencillo de cuatro columnas del proyecto, sin mezclar E/S con el ABB. */
public class LectorCSV {
    /** Valida el archivo completo y devuelve sus registros en el orden original. */
    public static List<TerminoBusqueda> leer(String rutaArchivo) throws IOException {
        List<TerminoBusqueda> registros = new ArrayList<>();
        try (BufferedReader lector = Files.newBufferedReader(Paths.get(rutaArchivo), StandardCharsets.UTF_8)) {
            String cabecera = lector.readLine();
            if (cabecera == null || !cabecera.replace("\uFEFF", "").trim()
                    .equals("termino,busquedas,categoria,fecha")) {
                throw new IOException("Cabecera CSV inválida: se esperaba termino,busquedas,categoria,fecha.");
            }
            String linea;
            int numeroLinea = 1;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split(",", -1);
                if (partes.length != 4) {
                    throw new IOException("Línea " + numeroLinea + ": se requieren exactamente cuatro columnas.");
                }
                try {
                    registros.add(new TerminoBusqueda(partes[0], Integer.parseInt(partes[1].trim()),
                            partes[2], partes[3].trim()));
                } catch (IllegalArgumentException e) {
                    throw new IOException("Línea " + numeroLinea + ": " + e.getMessage(), e);
                }
            }
        }
        return registros;
    }

    /** Primero lee y valida; después inserta sin ordenar ni reiniciar los contadores. */
    public static int cargarEnArbol(String rutaArchivo, ArbolBinarioBusqueda arbol) throws IOException {
        if (arbol == null) throw new IllegalArgumentException("El árbol no puede ser nulo.");
        List<TerminoBusqueda> registros = leer(rutaArchivo);
        for (TerminoBusqueda registro : registros) arbol.insertar(registro);
        return registros.size();
    }
}

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Datos de un término. Su clave es inmutable para conservar el orden del ABB. */
public final class TerminoBusqueda {
    private final String termino;
    private int numBusquedas;
    private String categoria;
    private String fechaUltBusqueda;

    public TerminoBusqueda(String termino, int numBusquedas, String categoria, String fechaUltBusqueda) {
        this.termino = normalizarTermino(termino);
        setNumBusquedas(numBusquedas);
        setCategoria(categoria);
        setFecha(fechaUltBusqueda);
    }

    /** Todas las operaciones usan la misma clave: sin espacios externos y en minúsculas. */
    public static String normalizarTermino(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            throw new IllegalArgumentException("El término no puede estar vacío.");
        }
        return termino.trim().toLowerCase(Locale.ROOT);
    }

    public String getTermino() {
        return termino;
    }

    // No hay setTermino: cambiar una clave rompería la propiedad de orden del árbol.
    public int getNumBusquedas() {
        return numBusquedas;
    }

    public void setNumBusquedas(int numBusquedas) {
        if (numBusquedas < 0) {
            throw new IllegalArgumentException("El número de búsquedas no puede ser negativo.");
        }
        this.numBusquedas = numBusquedas;
    }

    /** Registra una búsqueda y la fecha del sistema; evita el desbordamiento del contador. */
    public void incrementarNumBusquedas() {
        if (numBusquedas == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("El contador alcanzó el máximo permitido.");
        }
        numBusquedas++;
        fechaUltBusqueda = LocalDate.now().toString();
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        if (categoria == null || categoria.trim().isEmpty()) {
            throw new IllegalArgumentException("La categoría no puede estar vacía.");
        }
        this.categoria = categoria.trim();
    }

    public String getFechaUltBusqueda() {
        return fechaUltBusqueda;
    }

    /** Valida tanto el formato AAAA-MM-DD como la existencia de la fecha. */
    public void setFecha(String fecha) {
        if (fecha == null || !fecha.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new IllegalArgumentException("La fecha debe tener el formato AAAA-MM-DD.");
        }
        try {
            LocalDate.parse(fecha);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha no existe en el calendario.");
        }
        this.fechaUltBusqueda = fecha;
    }

    @Override
    public String toString() {
        return "Término: '" + termino + "' | Búsquedas: " + numBusquedas
                + " | Categoría: " + categoria + " | Última búsqueda: " + fechaUltBusqueda;
    }
}

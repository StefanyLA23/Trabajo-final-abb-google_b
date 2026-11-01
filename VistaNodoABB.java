/** Fotografía inmutable de un nodo real; no permite modificar el árbol ni sus registros. */
public final class VistaNodoABB {
    private final String termino;
    private final int busquedas;
    private final String categoria;
    private final String fecha;
    private final VistaNodoABB izquierdo;
    private final VistaNodoABB derecho;

    public VistaNodoABB(TerminoBusqueda dato, VistaNodoABB izquierdo, VistaNodoABB derecho) {
        termino = dato.getTermino();
        busquedas = dato.getNumBusquedas();
        categoria = dato.getCategoria();
        fecha = dato.getFechaUltBusqueda();
        this.izquierdo = izquierdo;
        this.derecho = derecho;
    }

    public String getTermino() { return termino; }
    public int getBusquedas() { return busquedas; }
    public String getCategoria() { return categoria; }
    public String getFecha() { return fecha; }
    public VistaNodoABB getIzquierdo() { return izquierdo; }
    public VistaNodoABB getDerecho() { return derecho; }
}

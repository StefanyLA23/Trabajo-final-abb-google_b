/** Un nodo contiene un término y las referencias a sus dos hijos. */
public final class NodoABB {
    private TerminoBusqueda dato;
    private NodoABB izq;
    private NodoABB der;

    public NodoABB(TerminoBusqueda dato) {
        setDato(dato);
    }

    public TerminoBusqueda getDato() { return dato; }

    public void setDato(TerminoBusqueda dato) {
        if (dato == null) throw new IllegalArgumentException("El dato del nodo no puede ser nulo.");
        this.dato = dato;
    }

    public NodoABB getIzq() { return izq; }
    public void setIzq(NodoABB izq) { this.izq = izq; }
    public NodoABB getDer() { return der; }
    public void setDer(NodoABB der) { this.der = der; }
}

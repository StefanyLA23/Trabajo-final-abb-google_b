/** Error previsto de la API: código HTTP y mensaje comprensible para la interfaz. */
public final class ErrorHTTP extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int codigo;

    public ErrorHTTP(int codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public int getCodigo() { return codigo; }
}

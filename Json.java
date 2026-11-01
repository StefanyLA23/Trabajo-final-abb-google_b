import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON pequeño sin dependencias: análisis estricto y escritura con escape de caracteres. */
public final class Json {
    private final String texto;
    private int posicion;

    private Json(String texto) { this.texto = texto; }

    public static Object leer(String texto) {
        if (texto == null || texto.length() > 65536) throw new IllegalArgumentException("JSON demasiado grande.");
        Json lector = new Json(texto);
        Object resultado = lector.valor(0);
        lector.espacios();
        if (lector.posicion != texto.length()) throw lector.error();
        return resultado;
    }

    public static Map<String, Object> leerObjeto(String texto) {
        Object valor = leer(texto);
        if (!(valor instanceof Map<?, ?>)) throw new IllegalArgumentException("Se esperaba un objeto JSON.");
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entrada : ((Map<?, ?>) valor).entrySet()) {
            resultado.put((String) entrada.getKey(), entrada.getValue());
        }
        return resultado;
    }

    private Object valor(int profundidad) {
        if (profundidad > 32) throw new IllegalArgumentException("JSON con demasiados niveles.");
        espacios();
        if (posicion >= texto.length()) throw error();
        char caracter = texto.charAt(posicion);
        if (caracter == '"') return cadena();
        if (caracter == '{') {
            posicion++;
            Map<String, Object> objeto = new LinkedHashMap<>();
            espacios();
            if (tomar('}')) return objeto;
            do {
                espacios();
                String clave = cadena();
                if (objeto.containsKey(clave)) throw new IllegalArgumentException("Clave JSON repetida: " + clave);
                espacios();
                exigir(':');
                objeto.put(clave, valor(profundidad + 1));
                espacios();
                if (tomar('}')) return objeto;
                exigir(',');
            } while (true);
        }
        if (caracter == '[') {
            posicion++;
            List<Object> lista = new ArrayList<>();
            espacios();
            if (tomar(']')) return lista;
            do {
                lista.add(valor(profundidad + 1));
                espacios();
                if (tomar(']')) return lista;
                exigir(',');
            } while (true);
        }
        if (texto.startsWith("true", posicion)) { posicion += 4; return Boolean.TRUE; }
        if (texto.startsWith("false", posicion)) { posicion += 5; return Boolean.FALSE; }
        if (texto.startsWith("null", posicion)) { posicion += 4; return null; }
        return numero();
    }

    private Number numero() {
        int inicio = posicion;
        tomar('-');
        if (!tomar('0')) {
            if (posicion >= texto.length() || texto.charAt(posicion) < '1' || texto.charAt(posicion) > '9') throw error();
            digitos();
        }
        boolean decimal = false;
        if (tomar('.')) { decimal = true; digitosObligatorios(); }
        if (tomar('e') || tomar('E')) {
            decimal = true;
            if (!tomar('+')) tomar('-');
            digitosObligatorios();
        }
        try {
            String contenido = texto.substring(inicio, posicion);
            if (decimal) return new BigDecimal(contenido);
            return Long.valueOf(contenido);
        } catch (NumberFormatException e) {
            throw error();
        }
    }

    private void digitos() {
        while (posicion < texto.length() && texto.charAt(posicion) >= '0' && texto.charAt(posicion) <= '9') posicion++;
    }

    private void digitosObligatorios() {
        int inicio = posicion;
        digitos();
        if (posicion == inicio) throw error();
    }

    private String cadena() {
        exigir('"');
        StringBuilder resultado = new StringBuilder();
        while (posicion < texto.length()) {
            char caracter = texto.charAt(posicion++);
            if (caracter == '"') {
                String cadena = resultado.toString();
                validarUnicode(cadena);
                return cadena;
            }
            if (caracter < 32) throw error();
            if (caracter != '\\') { resultado.append(caracter); continue; }
            if (posicion >= texto.length()) throw error();
            char escape = texto.charAt(posicion++);
            switch (escape) {
                case '"': resultado.append('"'); break;
                case '\\': resultado.append('\\'); break;
                case '/': resultado.append('/'); break;
                case 'b': resultado.append('\b'); break;
                case 'f': resultado.append('\f'); break;
                case 'n': resultado.append('\n'); break;
                case 'r': resultado.append('\r'); break;
                case 't': resultado.append('\t'); break;
                case 'u':
                    int codigo = 0;
                    for (int i = 0; i < 4; i++) {
                        if (posicion >= texto.length()) throw error();
                        char hexadecimal = texto.charAt(posicion++);
                        int digito = "0123456789abcdef".indexOf(Character.toLowerCase(hexadecimal));
                        if (digito < 0) throw error();
                        codigo = codigo * 16 + digito;
                    }
                    resultado.append((char) codigo);
                    break;
                default: throw error();
            }
        }
        throw error();
    }

    private static void validarUnicode(String cadena) {
        for (int i = 0; i < cadena.length(); i++) {
            char caracter = cadena.charAt(i);
            if (Character.isHighSurrogate(caracter)) {
                if (i + 1 >= cadena.length() || !Character.isLowSurrogate(cadena.charAt(++i))) {
                    throw new IllegalArgumentException("Secuencia Unicode inválida.");
                }
            } else if (Character.isLowSurrogate(caracter)) {
                throw new IllegalArgumentException("Secuencia Unicode inválida.");
            }
        }
    }

    private void espacios() {
        while (posicion < texto.length() && " \n\r\t".indexOf(texto.charAt(posicion)) >= 0) posicion++;
    }

    private boolean tomar(char caracter) {
        if (posicion < texto.length() && texto.charAt(posicion) == caracter) { posicion++; return true; }
        return false;
    }

    private void exigir(char caracter) { if (!tomar(caracter)) throw error(); }
    private IllegalArgumentException error() { return new IllegalArgumentException("JSON inválido en posición " + posicion + "."); }

    public static String escribir(Object valor) {
        StringBuilder resultado = new StringBuilder();
        escribirValor(valor, resultado);
        return resultado.toString();
    }

    private static void escribirValor(Object valor, StringBuilder resultado) {
        if (valor == null) resultado.append("null");
        else if (valor instanceof String) escribirCadena((String) valor, resultado);
        else if (valor instanceof Boolean || valor instanceof Number) {
            if (valor instanceof Double && !Double.isFinite((Double) valor)
                    || valor instanceof Float && !Float.isFinite((Float) valor)) {
                throw new IllegalArgumentException("JSON no admite números infinitos.");
            }
            resultado.append(valor);
        } else if (valor instanceof Map<?, ?>) {
            resultado.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> entrada : ((Map<?, ?>) valor).entrySet()) {
                if (!(entrada.getKey() instanceof String)) throw new IllegalArgumentException("Clave JSON no textual.");
                if (!primero) resultado.append(',');
                escribirCadena((String) entrada.getKey(), resultado);
                resultado.append(':');
                escribirValor(entrada.getValue(), resultado);
                primero = false;
            }
            resultado.append('}');
        } else if (valor instanceof Iterable<?>) {
            resultado.append('[');
            boolean primero = true;
            for (Object elemento : (Iterable<?>) valor) {
                if (!primero) resultado.append(',');
                escribirValor(elemento, resultado);
                primero = false;
            }
            resultado.append(']');
        } else throw new IllegalArgumentException("Tipo no compatible con JSON.");
    }

    private static void escribirCadena(String cadena, StringBuilder resultado) {
        validarUnicode(cadena);
        resultado.append('"');
        for (int i = 0; i < cadena.length(); i++) {
            char caracter = cadena.charAt(i);
            switch (caracter) {
                case '"': resultado.append("\\\""); break;
                case '\\': resultado.append("\\\\"); break;
                case '\n': resultado.append("\\n"); break;
                case '\r': resultado.append("\\r"); break;
                case '\t': resultado.append("\\t"); break;
                default:
                    if (caracter < 32 || caracter == '<' || caracter == '>' || caracter == '&'
                            || caracter == '\u2028' || caracter == '\u2029') {
                        resultado.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) caracter));
                    } else resultado.append(caracter);
            }
        }
        resultado.append('"');
    }
}

# Google: índice de autocompletado con ABB

Aplicación académica de consola y web local para el trabajo final de Estructuras de Datos del ITM.
El árbol binario de búsqueda (ABB) se implementa con nodos propios, sin balanceo automático.
La interfaz permite operar el mismo backend Java mediante HTTP y visualizar sus
relaciones reales con SVG, sin dependencias externas ni conexión a Internet.

## Objetivo académico y problema empresarial

Un índice de un buscador debe localizar términos, ofrecer sugerencias por prefijo y
conservar su popularidad, categoría y última fecha de búsqueda. Si un diccionario
se importa alfabéticamente en un ABB sin balanceo, se forma una cadena: las consultas
pueden recorrer todos los registros. El proyecto permite observar cómo el orden
de inserción afecta la altura y las comparaciones necesarias para consultar datos.

El objetivo académico es explicar la propiedad del ABB, implementar sus operaciones
obligatorias y aplicar autocompletado, popularidad y Top 5 al escenario Google.
Las cifras de este documento proceden de ejecutar los programas del repositorio.

## Tecnologías

Java 17 compatible, programación orientada a objetos, recursión y biblioteca estándar:
`Scanner`, listas, cola FIFO, `java.time` y archivos UTF-8. No se utilizan frameworks,
bases de datos, `TreeMap`, `TreeSet` ni bibliotecas de árboles. El ABB tiene nodos
y algoritmos propios. El frontend utiliza HTML5, CSS3, JavaScript puro y SVG;
`com.sun.net.httpserver.HttpServer`, incluido en el JDK, entrega la web y la API.
No se requiere Node.js, npm, Live Server ni un segundo servidor.

## Datos del equipo

- Integrante 1: Estefania lotero Arevalo
- Integrante 2: Nicole D'yana Urbiñez
- Integrante 3: Juan Felipe Ocampo
- Grupo: Veterinaria sabaneta
- Docente: Miguel Hurtado Ortiz
- Fecha de entrega: 11-11-2026

## Compilar y ejecutar en PowerShell de VS Code

Abra **Terminal > Nueva terminal** y seleccione PowerShell. Se necesita un JDK con
`java` y `javac` disponibles. La compilación se verificó con JDK 27 y destino Java 17;
se puede usar JDK 17 o superior.

Abra la carpeta `Trabajo-final-abb-google` descomprimida en VS Code. En la terminal integrada ya debe encontrarse en esa carpeta (verifique que se vean `Main.java`, `terminos.csv` y `web/` con `dir`).

```powershell
java -version
javac -version
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d out *.java
java -cp out ServidorWeb
```

Abra **http://localhost:8080** en un navegador moderno. El comando inicia juntos
el backend HTTP y la interfaz. El servidor escucha exclusivamente en `127.0.0.1`.
Para detenerlo, pulse **Ctrl+C** en la terminal. Si el puerto ya está ocupado,
detenga la instancia anterior antes de iniciar otra.

El menú de consola sigue disponible, con las opciones 1 a 13 y 0:

```powershell
java -cp out Main
```

La consola y la web son procesos con árboles independientes. Sus cambios no se
comparten entre procesos ni se guardan en el CSV.

`-d out` guarda los `.class` en `out`. El CSV se busca en la carpeta actual:
ejecute los comandos desde la carpeta de los fuentes, no desde `out`.
Después de modificar código, vuelva a compilar antes de ejecutarlo.

`-Werror` hace fallar la compilación si hay advertencias. La compilación verificada
no produjo errores ni advertencias.

Para ejecutar las pruebas y el experimento del ABB:

```powershell
java -cp out PruebasABB
java -cp out PruebasHTTP
java -cp out ExperimentoABB
```

Si la terminal muestra mal las tildes, use UTF-8 en PowerShell y en la salida de Java:

```powershell
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -cp out ServidorWeb
```

Las comillas alrededor de las opciones `-D` evitan que PowerShell las interprete.

## Clases y responsabilidades

| Archivo | Responsabilidad |
| --- | --- |
| `TerminoBusqueda.java` | Clave, contador, categoría y fecha. Normaliza y valida los datos. |
| `NodoABB.java` | Registro y referencias privadas a hijo izquierdo y derecho. |
| `ArbolBinarioBusqueda.java` | Operaciones recursivas del ABB y consultas que devuelven objetos, listas o texto. No imprime ni lee archivos. |
| `LectorCSV.java` | Lectura UTF-8 y validación del CSV; conserva el orden de las filas. |
| `Main.java` | Menú, entrada con `Scanner`, mensajes y presentación de resultados. |
| `PruebasABB.java` | Casos reproducibles y verificaciones sin dependencias externas. |
| `ExperimentoABB.java` | Compara el orden original y el orden alfabético con los mismos 30 términos. |
| `VistaNodoABB.java` | Copia inmutable de los atributos y enlaces reales de cada nodo para el dibujo. |
| `ServicioABBWeb.java` | Adapta las operaciones existentes a respuestas JSON y sincroniza el acceso al único ABB de la sesión web. |
| `ServidorWeb.java` | Punto de entrada HTTP local; entrega los tres archivos web y atiende la API. |
| `Json.java` | Lectura estricta y escritura con escapes de JSON, sin bibliotecas externas. |
| `ErrorHTTP.java` | Error previsto con código HTTP y mensaje para el usuario. |
| `PruebasHTTP.java` | Pruebas HTTP reales, JSON, concurrencia, recursos y estructura del árbol. |
| `web/index.html` | Las cinco secciones, formularios accesibles, confirmaciones y contenedor SVG. |
| `web/styles.css` | Identidad visual azul, tarjetas, diseño responsive y estilos del árbol. |
| `web/app.js` | Peticiones HTTP, presentación, distribución del SVG y resaltado de secuencias recibidas de Java. |
| `terminos.csv` | Datos originales: 30 registros, sin modificaciones. |

Las cuatro clases originales conservan sus responsabilidades principales. La lectura
que antes estaba en `ArbolBinarioBusqueda.cargarDesdeCSV` se trasladó a
`LectorCSV.cargarEnArbol`. Los recorridos y `autocompletar` ahora devuelven listas;
`Main` se encarga de mostrarlas. Esto permite reutilizarlos en un frontend posterior.

## Reglas de datos y de negocio

- La clave se normaliza con `trim()` y minúsculas usando `Locale.ROOT`. No se eliminan
  espacios interiores ni tildes. El orden es el de `String.compareTo`, sin colación
  regional especial para español.
- La clave es `private final` y no existe `setTermino()`. Los metadatos sí tienen
  setters validados. Para usar otra clave se debe eliminar e insertar.
- No se aceptan claves o categorías nulas/vacías, contadores negativos ni fechas
  inexistentes. Las fechas usan el formato estricto `AAAA-MM-DD`; se aceptan años
  bisiestos válidos. No hay una restricción adicional de fechas pasadas o futuras.
- Un término nuevo conserva el contador y la fecha suministrados.
- Insertar un duplicado **solo suma uno al contador existente y pone la fecha actual**.
  No reemplaza la categoría ni suma el contador del objeto entrante. Los datos
  entrantes también deben ser válidos.
- `buscar()` es una consulta sin efectos secundarios. `registrarBusqueda()` consulta
  y, si encuentra el término, suma uno y actualiza la fecha. La opción 2 del menú
  utiliza esta última operación. Consultar prefijos, rangos o recorridos no incrementa.
- La fecha actual procede de `LocalDate.now()`, según el reloj y zona del equipo.
  Se rechaza un incremento cuando el contador ya vale `Integer.MAX_VALUE`.
- La actualización valida todo antes de modificar el registro, de modo que una fecha
  inválida no deja un contador cambiado parcialmente.
- Los rangos incluyen ambos extremos. El inicio debe ser menor o igual al final.
- Sucesor y predecesor son vecinos **estrictos** de la clave consultada. También se
  pueden consultar respecto de una clave ausente. Si no hay vecino se devuelve `null`.
- El Top 5 usa búsquedas descendentes y, en empate, clave alfabética ascendente.
- Las listas de resultados son nuevas, pero contienen los registros del árbol:
  quitar un elemento de la lista no elimina un nodo. Los setters de metadatos sí
  modifican el registro. La raíz y los nodos internos no se exponen desde el ABB.
- Los cambios son en memoria. Al salir y volver a iniciar se recargan los datos
  originales; el programa no sobrescribe `terminos.csv`.

El lector admite el formato sencillo del archivo del proyecto: cabecera
`termino,busquedas,categoria,fecha` y cuatro campos separados por comas. Acepta BOM
UTF-8 y líneas vacías; no implementa campos entrecomillados que contengan comas.
Valida todas las filas antes de comenzar a insertarlas. Ante un CSV inválido,
informa la línea y no inserta registros parciales por errores de lectura/validación.
Si falta el archivo al iniciar, el menú informa el problema y permite trabajar vacío.

## Tres funcionalidades específicas de Google

1. **Autocompletado por prefijo:** la opción 4 devuelve las coincidencias en orden
   alfabético y poda las ramas que no pueden contenerlas. Informa los nodos visitados.
2. **Popularidad y última búsqueda:** la opción 2 incrementa el contador y actualiza
   la fecha del término encontrado. Insertar un duplicado también incrementa uno,
   sin crear otro nodo ni sustituir los metadatos existentes.
3. **Top 5 de términos más buscados:** la opción 13 ordena por popularidad
   descendente y resuelve los empates alfabéticamente.

## Operaciones del ABB y demostración desde el menú

Para obtener los valores siguientes, inicie con el CSV original. Reinicie entre
demostraciones si una operación anterior insertó, actualizó, buscó o eliminó datos.
Los recorridos y consultas muestran los cuatro atributos, incluida la fecha.

| Opción | Cómo demostrarla | Resultado esperado |
| --- | --- | --- |
| 1. Insertar | Escriba `  PRUEBA JAVA  `, `25`, `tecnologia`, `2024-02-29`. | Clave `prueba java`, contador 25, categoría y fecha capturados correctamente; 31 nodos. |
| 1. Duplicado | En una sesión nueva inserte ` JAVA `, `999`, `otra`, `2024-02-29`. | Continúan 30 nodos; `java` pasa de 4200 a 4201, conserva `tecnologia` y recibe la fecha actual. |
| 2. Buscar | Consulte ` JAVA ` y luego una clave inexistente. | `java` se encuentra, suma una búsqueda y actualiza fecha. La otra consulta informa que no existe. |
| 3. Recorridos | Entre y elija sucesivamente `1`, `2`, `3`, `4`, `0`. | Inorden, preorden, postorden y niveles. `0` vuelve al menú principal sin pedir prefijo. |
| 4. Autocompletar | Use ` JA `. | Devuelve `java`; visita 7 de los 30 nodos en el árbol original. |
| 5. Extremos | Elija `5`. | Mínimo `algebra lineal`; máximo `vuelos baratos`. |
| 6. Actualizar | Use `java`, `4300`, `programacion`, `2024-02-29`. | Cambian los tres metadatos. La clave permanece `java`. Verifique con recorrido o prefijo para no incrementar el contador. |
| 7. Estadísticas | Elija `7` recién iniciado. | Total 30, hojas 8, internos 22, altura 9. |
| 8. Nivel | Consulte `medellin` y luego `java`. | Niveles 0 y 2, respectivamente. Una clave ausente se informa como no encontrada. |
| 9. Eliminar | Escriba el término y después consulte el recorrido o dibujo. | Elimina el registro y conserva el orden; la segunda eliminación de la misma clave informa que no existe. |
| 10. Vecinos | Consulte `medellin`. | Predecesor `java`, sucesor `metro medellin`. Para el mínimo no hay predecesor y para el máximo no hay sucesor. |
| 11. Rango | Desde `java` hasta `python`. | Devuelve 9 registros ordenados e inclusivos; visita 10 nodos. |
| 12. Estructura | Elija `12`. | Dibujo con `RAÍZ`, `IZQ`, `DER` y sangría según profundidad. |
| 13. Top 5 | Elija `13` recién iniciado. | Popularidad descendente según la tabla siguiente. |
| 0. Salir | Elija `0`. | Finaliza normalmente. |

El rango `[java, python]` contiene, en este orden: `java`, `medellin`,
`metro medellin`, `musica rock`, `noticias colombia`, `perros razas`,
`pico y placa medellin`, `puntos colombia`, `python`.

Top 5 inicial:

| Posición | Término | Búsquedas |
| --- | --- | ---: |
| 1 | pico y placa medellin | 8500 |
| 2 | dolares a pesos | 7100 |
| 3 | noticias colombia | 6200 |
| 4 | vuelos baratos | 5300 |
| 5 | python | 5000 |

Para demostrar las tres formas de eliminación de manera controlada:

1. Reinicie. Inserte `zz prueba` con contador `1`, categoría `prueba` y fecha
   `2024-02-29`. Con la opción 12 verá que es una hoja; elimínela con la opción 9.
2. Reinicie. Inserte primero `zz prueba` y después `zz prueba hijo`, con los mismos
   metadatos. `zz prueba` tendrá un hijo derecho. Elimínela y compruebe que el hijo
   ocupa su lugar. El caso de hijo izquierdo también está cubierto en las pruebas.
3. Reinicie. Elimine `medellin`, la raíz original con dos hijos. `metro medellin`,
   su sucesor, pasa a la raíz. Verifique con las opciones 12, 3 > 1 y 7: quedan 29
   nodos en orden. En las pruebas se verifican además raíz única y raíz con un hijo.

Para demostrar validaciones, escriba `hola`, `99` o un entero demasiado grande en
el menú. El programa vuelve a pedir una opción. Durante una inserción use un
contador negativo o una categoría vacía: se vuelve a pedir ese campo. Una fecha
como `2026-02-30` rechaza la operación completa y regresa al menú. Un rango de `z`
a `a` también se rechaza. El fin de entrada se maneja sin mostrar una excepción.

## Cómo funcionan los algoritmos

**Inserción y búsqueda.** En cada llamada se compara la clave con la del nodo:
menor sigue a la izquierda, mayor a la derecha, igual identifica el registro.
Un duplicado no crea un nodo adicional.

**Eliminación.** Una hoja se reemplaza por `null`. Un nodo con un hijo se reemplaza
por ese hijo. Con dos hijos se toma el mínimo del subárbol derecho (sucesor), se
traslada el registro completo y se elimina su nodo anterior. Cada llamada devuelve
la nueva raíz de su subárbol; por eso también funciona al eliminar la raíz global.

**Recorridos.** Inorden visita izquierdo-nodo-derecho; preorden visita
nodo-izquierdo-derecho; postorden visita izquierdo-derecho-nodo. Todos son recursivos.
El recorrido por niveles utiliza una `Queue` con `ArrayDeque`: retira el primero
de la cola y agrega sus hijos izquierdo y derecho al final, en ese orden.

**Poda de prefijos.** Los términos de un mismo prefijo forman un bloque contiguo
del orden lexicográfico. Si el nodo empieza con el prefijo, se consideran ambos
hijos y se agrega el nodo entre ellos. Si no coincide y su clave es menor que el
prefijo, se descarta el subárbol izquierdo. Si es mayor, se descarta el derecho.
Esto evita recorrer todo el árbol y mantiene el resultado alfabético.

**Poda de rango.** Solo se visita el hijo izquierdo si la clave actual supera el
límite inferior, y el derecho si está por debajo del superior. El nodo se incluye
si está dentro de los dos límites, incluidas las igualdades.

**Top 5.** La popularidad no determina la posición en el ABB. Se recorren todos sus
nodos recursivamente y se mantiene una lista ordenada de hasta cinco candidatos
(seis momentáneamente antes de descartar el último). No se supone que las claves
alfabéticamente mayores sean más populares.

`esABBValido()` comprueba recursivamente límites estrictos heredados de los
antecesores. Así detecta un nodo fuera de lugar incluso si está varios niveles
por debajo del nodo que impone el límite.

## Complejidad temporal y espacio del ABB

Sean `n` la cantidad de nodos, `h` la altura, `k` la cantidad de coincidencias y
`w` el ancho máximo. La tabla cuenta operaciones sobre claves como costo constante;
comparar cadenas y comprobar prefijos también depende de su longitud. Las listas
de resultados ocupan memoria adicional proporcional a su tamaño.

| Operación | Tiempo | Espacio auxiliar, sin contar la salida |
| --- | --- | --- |
| Insertar o procesar duplicado | O(h + 1) | O(h + 1), pila recursiva |
| Buscar, registrar búsqueda, actualizar, eliminar | O(h + 1) | O(h + 1) |
| Mínimo, máximo, sucesor, predecesor, nivel | O(h + 1) | O(h + 1) |
| Inorden, preorden, postorden | O(n) | O(h + 1), más O(n) de resultados |
| Por niveles | O(n) | O(w), más O(n) de resultados |
| Rango o autocompletado con poda | O(h + k + 1), peor caso O(n) | O(h + 1), más O(k) de resultados |
| Top 5 | O(n), pues ordenar hasta seis candidatos es constante | O(h + 1) y lista de tamaño constante |
| Conteos, altura, validación de orden | O(n) | O(h + 1) |
| Visualización textual | O(n(h + 1)), incluida la sangría | Depende del texto y de las cadenas de sangría; salida O(n(h + 1)) |
| Cargar CSV en un árbol vacío | O(n(h + 1)) en general; O(n²) en el peor caso | O(n) de registros leídos, más pila |

Con forma equilibrada, `h` es O(log n) y las operaciones por camino cuestan
O(log n). Con inserciones aleatorias, la altura esperada también es O(log n).
Este ABB **no garantiza** equilibrio: con claves ordenadas se convierte en una
cadena de altura `n - 1`, y buscar/insertar/eliminar puede costar O(n). La recursión
también consume una pila proporcional a esa altura; esta implementación está
pensada para los datos académicos del proyecto.

La altura se mide en **aristas**: árbol vacío = -1, un nodo = 0. La raíz tiene
nivel cero. Un nodo interno tiene al menos un hijo, incluida la raíz cuando aplica.

## Experimento del ABB con 30 términos

Ejecución real con JDK 27, compilación `--release 17`, sobre los 30 registros
originales. `ExperimentoABB` construye dos árboles nuevos: uno siguiendo el CSV y
otro ordenando una copia de la lista por clave. No modifica el archivo ni altera
contadores durante las mediciones, porque utiliza `buscar()`, no `registrarBusqueda()`.

El orden original del CSV es desordenado alfabéticamente; se conserva tal como fue
entregado. Se compara con la inserción alfabética de los mismos 30 términos, sin
generar otro conjunto ni mezclar las filas aleatoriamente.

Se cuenta **una comparación de clave por nodo no nulo** visitado por inserción o
búsqueda (`compareTo`), sin contar llamadas a `null`, comparaciones de caracteres,
ni el trabajo de ordenar la lista. Para búsqueda exitosa se consulta cada una de
las 30 claves exactamente una vez; la media es el total dividido por 30.

| Orden de inserción | Nodos | Altura | Comparaciones al insertar los 30 | Total de las 30 búsquedas exitosas | Media | Máximo | Búsqueda ausente `zzzzz` |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Original del CSV | 30 | 9 | 130 | 160 | 5,33 | 10 | 4 |
| Alfabético | 30 | 29 | 435 | 465 | 15,50 | 30 | 30 |

Visitas observadas en las consultas con poda (nodos no nulos examinados, no número
de comparaciones de caracteres):

| Orden | Prefijo `ja` (1 resultado) | Rango `[java, python]` (9 resultados) | Propiedad ABB |
| --- | ---: | ---: | --- |
| Original del CSV | 7 | 10 | Válida |
| Alfabético | 18 | 25 | Válida |

La inserción alfabética enlaza cada nuevo nodo a la derecha del anterior. Las
435 comparaciones corresponden a `0 + 1 + ... + 29`, y las 465 comparaciones de
búsqueda a `1 + 2 + ... + 30`. El orden original tiene menor altura, aunque no es
un árbol perfectamente equilibrado. La poda ahorra visitas cuando la estructura
permite descartar ramas; no elimina el costo de recorrer una cadena larga.

## Pruebas originales del ABB

Resultado real: **57 casos aprobados, 0 fallidos y 16.363 verificaciones**.
`PruebasABB.java` conserva sus 57 casos originales.
El ejecutor informa cada caso y termina con error si alguno falla; no requiere
activar `-ea` porque utiliza verificaciones propias.

Incluye carga de los 30 registros con atributos intactos, integridad SHA-256 del
CSV original, inserción, duplicados, consultas, popularidad/fecha, actualización
atómica, todas las formas de eliminación y de raíz, los cuatro recorridos,
extremos, vecinos, rangos, prefijos con poda observable, Top 5 y empates,
estadísticas, dibujo, árbol vacío, encapsulación y entradas inválidas.

También prueba archivos CSV temporales inválidos y entradas de consola simuladas
para verificar el menú, la captura de categoría/fecha, el `break`, el submenú,
la recuperación tras errores y el fin de entrada. Los archivos temporales se
eliminan al terminar cada caso.

Se insertan 120 claves en un orden mezclado con semilla fija y se eliminan en otro
orden fijo. **Después de cada inserción y eliminación** se compara el contenido
con una lista de referencia y se valida recursivamente la propiedad del ABB.
El autocompletado se contrasta con un filtrado de referencia para todos los
prefijos de las 30 claves; los rangos se contrastan para las 465 parejas posibles
de límites existentes. Las semillas fijas hacen reproducible el resultado.

La comprobación SHA-256 usa los bytes exactos del CSV entregado, incluido su orden
y finales de línea: `6015F70B3EA846EFD32AC4234C3D9221FD5A48CAB6DC5C1204F82C25A8E1F843`.
Si se edita deliberadamente ese archivo, debe revisarse esa prueba y repetirse
el experimento antes de cambiar los resultados documentados.

## Interfaz web: cinco secciones

### Inicio y buscador

La barra central solicita sugerencias a `/api/autocompletar` después de una breve
pausa al escribir. Java ejecuta la poda; JavaScript no filtra el diccionario ni
construye otro ABB. Se muestran hasta ocho sugerencias y se informa el total y
los nodos visitados. Hay navegación con flechas y Escape.

Al pulsar **Buscar** o seleccionar una sugerencia, se utiliza `registrarBusqueda()`:
el resultado incluye sus cuatro atributos, aumenta la popularidad y actualiza la
fecha. Los indicadores y el Top 5 se actualizan. Un término ausente produce un
mensaje comprensible y no crea registros.

### Administración

Hay formularios independientes para insertar, consultar sin incrementar popularidad,
actualizar metadatos y eliminar. La clave de actualización se selecciona y se envía
como referencia; no puede cambiarse. Las validaciones del navegador ayudan al usuario,
pero Java vuelve a validar todos los datos y rechaza campos inesperados.

Antes de eliminar se muestra un diálogo de confirmación. El término seleccionado
indica si es hoja, tiene uno o dos hijos, y si es la raíz. Se reutiliza exactamente
la eliminación recursiva existente. Los ejemplos de eliminación de la sección
del menú también se pueden realizar con estos formularios.

### Árbol ABB

`obtenerEstructura()` copia recursivamente cada nodo a `VistaNodoABB`. La fotografía
guarda cadenas, contador y fotografías de hijos; no contiene `NodoABB` ni referencias
modificables a `TerminoBusqueda`. Cambiar o eliminar el árbol no altera una fotografía
ya obtenida. La API serializa esos enlaces como `izquierdo` y `derecho`, usando `null`
cuando no existe un hijo.

JavaScript dibuja **esas relaciones**, sin reinsertar una lista para inventar un árbol.
Una distribución recursiva por contornos reserva espacio entre nodos de un mismo
nivel; la raíz queda arriba y los hijos se colocan al lado correspondiente. Los
nodos tienen ancho fijo y etiquetas de hasta dos líneas. Para términos muy largos
se abrevia la etiqueta, con texto completo en el título y el panel de detalles.

La vista empieza al 100 %, centrada en la raíz. Puede acercar, alejar, ajustar todo
el ancho y desplazarse horizontal o verticalmente. Seleccionar un nodo con clic,
Enter o Espacio resalta su tarjeta y muestra sus atributos, nivel y relaciones.
La leyenda distingue raíz, selección y visita.

El formulario **Sigue una búsqueda** solicita el camino a Java sin aumentar la
popularidad. Puede animarse cada paso con pausa ajustable. El botón **Limpiar
resaltado** cancela la secuencia. Se respeta la preferencia de movimiento reducido.
El dibujo y los detalles se vuelven a calcular después de insertar, actualizar,
buscar, eliminar, vaciar o restaurar los datos.

### Consultas y recorridos

Se ofrecen inorden, preorden, postorden y niveles, con explicación y secuencia numerada.
**Ver en el árbol** resalta los nodos en el orden devuelto por Java. Al modificar los
datos se invalida la secuencia anterior para no animar términos eliminados.

El panel contiene estadísticas, extremos, nivel, vecinos estrictos, rango inclusivo
con poda y Top 5. El rango informa cantidad de resultados y visitas. Las consultas
auxiliares y los recorridos no registran popularidad.

### Datos y experimento

Se muestra el número de registros originales, columnas y estado del árbol actual.
**Vaciar árbol** crea un árbol vacío; **Restaurar CSV** construye otro desde el archivo
original y reemplaza la sesión solo después de leerlo correctamente. Ambas acciones
requieren confirmación. Restaurar repetidamente no mezcla registros ni aumenta sus
contadores. El CSV no se modifica.

**Ejecutar experimento** llama a `ExperimentoABB.ejecutar(...)`, también utilizado por
el programa de consola. Los resultados son objetos con mediciones reales; no hay
alturas ni contadores escritos como constantes en el HTML. La ejecución utiliza
árboles independientes y no altera los datos de la sesión web. Las explicaciones
y los resultados educativos anteriores siguen vigentes.

## Conexión HTTP y JSON

```text
Navegador: HTML + CSS + JavaScript + SVG
                │ fetch, JSON UTF-8
                ▼
ServidorWeb → ServicioABBWeb → ArbolBinarioBusqueda
                                      │
                           TerminoBusqueda / NodoABB
```

Un servidor mantiene un solo ABB principal. El método de atención de
`ServicioABBWeb` es `synchronized`: una petición completa consulta o modifica el
árbol y convierte los resultados a valores separados mientras mantiene el bloqueo.
Así se protege también la instrumentación de visitas y comparaciones. El servidor
puede recibir peticiones concurrentes con un grupo de cuatro hilos.

`Json` maneja objetos, listas, cadenas, números, booleanos y `null`. Valida sintaxis,
Unicode, claves repetidas y profundidad. Los contadores se aceptan únicamente como
enteros válidos. Las peticiones tienen límite de 64 KiB; cada campo textual o parámetro
admite hasta 200 caracteres. Las reglas originales de las entidades no se cambiaron.

Los datos se dibujan con `textContent` y métodos del DOM/SVG; no se insertan como
HTML ejecutable. El JSON escapa comillas, barras, controles y caracteres de marcado.
El servidor no habilita CORS, rechaza orígenes externos y solo permite Host local.
Los archivos estáticos están en una lista cerrada: HTML, CSS y JavaScript. Solicitar
fuentes Java, CSV, README o contenido de `.git` devuelve 404.

### Rutas de la API

Todas las respuestas de la API son JSON UTF-8. Las listas de consultas utilizan
`resultados`; las consultas de un registro utilizan `termino`.

| Método y ruta | Entrada | Operación |
| --- | --- | --- |
| `GET /api/estado` | — | Estadísticas, registros CSV, estado de carga y revisión. |
| `GET /api/arbol` | — | Raíz, enlaces reales y estadísticas coherentes con la fotografía. |
| `GET /api/terminos` | — | Registros en inorden, para los selectores. |
| `POST /api/terminos` | JSON con `termino`, `busquedas`, `categoria`, `fecha` | Inserta o procesa duplicado. |
| `GET /api/termino?clave=java` | Clave | Consulta pura sin popularidad. |
| `PUT /api/termino?clave=java` | JSON con `busquedas`, `categoria`, `fecha` | Actualiza sin modificar la clave. |
| `DELETE /api/termino?clave=java` | Clave | Elimina usando el algoritmo existente. La interfaz pide confirmación. |
| `POST /api/buscar` | JSON con `termino` | Registra búsqueda, popularidad, fecha y camino. |
| `GET /api/autocompletar?prefijo=ja` | Prefijo | Sugerencias y visitas con poda. |
| `GET /api/camino?termino=java` | Término | Camino y resultado sin incrementar. |
| `GET /api/recorrido?orden=inorden` | `inorden`, `preorden`, `postorden` o `niveles` | Secuencia desde Java. |
| `GET /api/extremos` | — | Mínimo y máximo, o `null` en vacío. |
| `GET /api/nivel?termino=java` | Término | Nivel; 404 si no existe. |
| `GET /api/vecinos?termino=medellin` | Referencia | Predecesor y sucesor estrictos, aunque la referencia no exista. |
| `GET /api/rango?desde=java&hasta=python` | Límites inclusivos | Coincidencias y visitas. |
| `GET /api/top` | — | Cinco términos ordenados por popularidad y desempate alfabético. |
| `DELETE /api/arbol` | `{"confirmacion":"VACIAR"}` | Crea un árbol vacío. |
| `POST /api/restaurar` | `{"confirmacion":"RESTAURAR"}` | Reemplaza el árbol por el CSV original. |
| `GET /api/experimento` | — | Mediciones del experimento existente. |

Ejemplo de inserción desde PowerShell, con el servidor iniciado:

```powershell
$nuevoTermino = @{
    termino = "prueba web"
    busquedas = 25
    categoria = "tecnologia"
    fecha = "2024-02-29"
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/terminos" -ContentType "application/json; charset=utf-8" -Body ([System.Text.Encoding]::UTF8.GetBytes($nuevoTermino))
```

Se usa 200 para consultas o cambios completados, 201 para una clave nueva, 400 para
datos inválidos, 403 para origen no permitido, 404 para término/ruta ausente, 405 para
método incorrecto, 413 para cuerpo demasiado grande y 415 para contenido no JSON.
Los errores incluyen un campo `error` con un mensaje en español. Un fallo de lectura
o interno devuelve 500 sin publicar rutas privadas en la respuesta. Los rangos
invertidos se rechazan y los árboles vacíos devuelven listas vacías o `null` según
la consulta.

## Pruebas web y validación de la entrega

La compilación real con `--release 17 -Xlint:all -Werror` terminó sin errores ni
advertencias. Las suites se ejecutan con:

```powershell
java -cp out PruebasABB
java -cp out PruebasHTTP
java -cp out ExperimentoABB
```

| Suite ejecutada | Casos aprobados | Fallidos | Verificaciones |
| --- | ---: | ---: | ---: |
| ABB original | 57 | 0 | 16363 |
| HTTP e integración web | 48 | 0 | 999 |
| Total | **105** | **0** | **17362** |

`PruebasHTTP` inicia su propio servidor con puerto efímero y lo cierra al terminar;
no necesita que el servidor 8080 esté activo ni cambia su sesión. Comprueba entrega
de HTML/CSS/JavaScript, carga de 30 filas, CRUD, popularidad, fecha, duplicados, las
tres eliminaciones y raíz, recorridos, estadísticas, extremos, vecinos, rango,
prefijos, Top 5 y empates, estructura real, camino, restauración repetida y vacío.

También valida el experimento aislado, JSON inválido, Unicode y escapes, tipos y
tamaños de peticiones, métodos HTTP, archivos internos inaccesibles y 40 búsquedas
simultáneas sin perder incrementos. Se conservan los hashes del CSV y del archivo
original de las 57 pruebas.

La sintaxis de `app.js` se verificó con un motor JavaScript V8, sin añadir dependencias
al proyecto. Se revisaron los 55 identificadores de controles usados directamente
por JavaScript y los 71 identificadores HTML únicos. La distribución del SVG se probó
con los 30 nodos reales: 10 niveles, enlaces izquierdo/derecho correctos y separación
mínima de 22 unidades entre tarjetas de un mismo nivel al 100 % de zoom.

**Límite de la verificación visual:** no se pudo completar una revisión interactiva
en navegador. El acceso automatizado al navegador no estaba disponible y Computer
Use detuvo el intento porque no pudo verificar la URL activa de Chrome. Las pruebas
HTTP, de sintaxis y de distribución no sustituyen una revisión visual de las cinco
pantallas y sus interacciones en el navegador del equipo.

## Guion de demostración y capturas recomendadas

1. **Inicio:** capture el título, indicadores, Top 5 y sugerencias al escribir `ja`.
   Busque `java` y compruebe el incremento y la fecha en la tarjeta del resultado.
2. **Administración:** inserte `zz prueba`, con contador 1, categoría `prueba` y fecha
   `2024-02-29`; consulte y actualice sus metadatos. Elimínelo confirmando el diálogo.
   Para un hijo, inserte también `zz prueba hijo` antes de eliminar el padre.
3. **Árbol:** capture la raíz seleccionada y sus detalles. Pruebe zoom y desplazamiento.
   Busque el camino de `java` y observe el resaltado; use las pausas para una captura.
4. **Consultas:** capture un recorrido numerado y su resaltado en el árbol. Consulte
   los vecinos de `medellin`, el rango `[java, python]`, extremos y nivel.
5. **Datos:** restaure el CSV antes de medir. Capture la tabla calculada del experimento,
   con alturas 9 y 29. Repita la restauración y compruebe que `java` conserva 4200
   búsquedas. Pruebe vaciar y restaurar para demostrar ambos estados.
6. Para demostrar eliminación de raíz con dos hijos, restaure y elimine `medellin`.
   Capture el árbol antes y después: `metro medellin` ocupa la raíz. Al terminar,
   restaure para dejar los 30 registros originales.

Las capturas deben tomarse en el navegador al ejecutar la aplicación; no se incluyen
capturas simuladas ni se presentan como evidencia de una revisión visual completada.

## Limitaciones

El ABB no se balancea automáticamente y puede degradarse a una cadena con
inserciones ordenadas. Las ediciones solo permanecen en memoria durante la sesión;
el CSV original no se sobrescribe. El alcance es académico y local, sin persistencia
ni autenticación multiusuario. Las pestañas comparten la sesión web, pero los
procesos de consola son independientes. El botón de actualización sincroniza una
pestaña con cambios realizados desde otra; no hay notificaciones en tiempo real.

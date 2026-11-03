# MetroBus: rutas de transporte público de Medellín con Dijkstra

MetroBus es una aplicación de escritorio en **Java 17 con Maven** (ventana Swing, con un
modo de consola opcional) que, dadas dos direcciones
de Medellín (por ejemplo `CL 50 53-48` y `CR 70 x CL 48`), calcula cómo ir de una a otra
combinando **caminata, buses urbanos, Metro, Metroplús, tranvía y Metrocable**. Dice qué
vehículo tomar, dónde bajarse, cuánto dura el viaje y cuánto cuesta con las tarifas de
2026. Funciona **sin internet**: todos los datos (3 590 paradas, 252 rutas de bus y 75
estaciones) se descargaron una sola vez del servicio ArcGIS público de la Alcaldía de
Medellín y viajan dentro del repositorio, en `data/`. El único algoritmo de búsqueda de
caminos es **Dijkstra**. La aplicación se desarrolló con el IDE **Kiro**.

> **La decisión central del proyecto:** el mapa no se modela como "una parada = un nodo",
> sino como un grafo de **estados**: estar a pie en un lugar, o estar montado en una ruta
> concreta, en un sentido concreto, en una parada concreta. Sobre ese grafo de 13 895
> estados y 82 160 aristas, un solo Dijkstra resuelve a la vez la caminata, los
> transbordos y la espera. Se justifica en la
> [decisión 2](#2-por-qué-un-grafo-de-estados-y-no-un-nodo-por-parada).

**Estado honesto a 3 de noviembre de 2026:** la aplicación compila, abre la pantalla de
inicio y responde las consultas, pero **todavía no reproduce los 15 casos de referencia**:
ninguna duración cae dentro de ±2 minutos y 7 de 15 tarifas son incorrectas. Se explica en
[Pruebas](#7-pruebas) y en [Limitaciones conocidas](#8-limitaciones-conocidas). Este README
describe el código copiado de la carpeta del proyecto a las 13:27 de ese día; en ese
momento se estaba corrigiendo el enrutamiento.

## 1. El proyecto de un vistazo

```
   Usuario (ventana Swing, o consola con --consola; textos en español)
        │  origen y destino como texto
        ▼
┌──────────────────────────────┐
│ ui: gui/MainWindow,          │  ventana: mapa de la red y planificador
│     AppController ─┐         │
│     Menu ──────────┤         │  consola: menú de texto
│                    ▼         │
│     TripPlanner, TripPrinter │  validan, calculan y presentan el viaje
└──────────────┬───────────────┘
               │ texto
               ▼
┌──────────────────────────────┐
│ geocoding: AddressParser,    │  "CL 50 53-48" ──► (calle 50, carrera 53)
│            AddressLocator    │                ──► (lat 6,25…, lon −75,57…)
└──────────────┬───────────────┘
               │ Place (lugar con coordenadas)
               ▼
┌──────────────────────────────┐      ┌──────────────────────────────┐
│ routing: Router (Dijkstra),  │◄─────┤ graph: TransitGraph,         │
│          StepBuilder         │ lee  │        GraphBuilder          │
└──────────────┬───────────────┘      └──────────────▲───────────────┘
               │ Trip (pasos)                        │ construye una vez al iniciar
               ▼                                     │
┌──────────────────────────────┐      ┌──────────────┴───────────────┐
│ fares: FareCalculator        │      │ data: lectores CSV y JSON    │
│        (tarifas 2026)        │      │ (ArcGisReader, CsvReader…)   │
└──────────────────────────────┘      └──────────────▲───────────────┘
                                                     │
                                       data/*.csv + data/raw/*.json
```

| Componente | Qué se construyó | Archivos principales |
|---|---|---|
| Datos | Lectura de las paradas y rutas (JSON de ArcGIS) y de los tramos precalculados (CSV) | `data/ArcGisReader.java`, `data/StopArcGisReader.java`, `data/CsvReader.java`, `data/BusSegmentReader.java`, `data/TransitLineReader.java`, `data/WalkLinkReader.java` |
| Grafo | Construcción del grafo de estados e índice espacial | `graph/GraphBuilder.java`, `graph/TransitGraph.java` |
| Geocodificación | Dirección de Medellín a coordenadas, sin internet | `geocoding/AddressParser.java`, `geocoding/AddressLocator.java` |
| Enrutamiento | Dijkstra multi-origen y multi-destino; traducción a pasos | `routing/Router.java`, `routing/StepBuilder.java`, `routing/Parameters.java` |
| Tarifas | Precio del viaje con integración bus + Metro | `fares/FareCalculator.java`, `fares/FareConfig.java` |
| Interfaz gráfica | Ventana con el mapa real de la red y el planificador, adaptable desde 400 px de ancho (ver [DESIGN.md](DESIGN.md)) | `ui/gui/MainWindow.java`, `ui/gui/state/`, `ui/gui/view/`, `ui/TripPlanner.java`, `ui/PlanResult.java` |
| Interfaz de consola | Pantalla de inicio y menú de texto (`--consola`) | `ui/HomeScreen.java`, `ui/Menu.java`, `ui/TripPrinter.java` |
| Arranque | Elige el modo, carga el grafo y conecta las piezas | `Main.java` |

---

## 2. Compilar y ejecutar

**Requisitos:** JDK 17 o superior y Maven 3.6 o superior en el `PATH`. Los datos ya vienen
en `data/`; no hay que descargar nada más.

```bash
git clone https://github.com/mquijanoh09/MetroBus.git        # descarga el proyecto
cd MetroBus                                                  # la app busca ./data
mvn -DskipTests package                                      # compila y arma el .jar
java -jar target/transit-routing-1.0.0.jar                   # abre la ventana
java -jar target/transit-routing-1.0.0.jar --consola         # o el menú de texto
```

El cuarto comando abre la ventana en la pantalla de inicio: el formulario "¿A dónde vas?"
y, al lado, el mapa de las líneas de Metro, Metroplús, Metrocable y tranvía dibujado con
las coordenadas reales. En la medición la ventana estaba visible a los 0,5 s. Mientras
carga el grafo (unos 1,5 s) ya se puede escribir: el botón dice **Cargando la red…** y,
si se presiona Enter, la búsqueda queda en cola y corre sola al terminar la carga.
Con `--consola` se usa el menú de texto, del que se sale escribiendo `salir`. En un equipo
sin pantalla gráfica la app pasa sola al modo consola.

**Si las tildes salen como `?` en modo consola.** En una terminal de Linux sin configuración regional se
vieron `Medell?n` y `?` en los marcos de la pantalla de inicio. Con Java 18 o superior se
arregla con `-Dstdout.encoding=UTF-8`; en Windows normalmente basta `chcp 65001` antes de
ejecutar. Lo segundo no se probó.

| Objetivo | Qué produce | Para qué sirve |
|---|---|---|
| `mvn -DskipTests package` | `target/transit-routing-1.0.0.jar` (204 462 bytes), con Commons CSV dentro | Ejecutar la app con `java -jar` |
| `mvn test` | Informes en `target/surefire-reports/` | Correr las 423 pruebas JUnit |
| `mvn clean` | Borra `target/` | Compilar desde cero |

| Plugin del `pom.xml` | Por qué está |
|---|---|
| `maven-compiler-plugin` 3.11.0, `source`/`target` 17 | El código usa `Stream.toList()`, `switch` con flechas y `var`, que no existen en Java 11 |
| `maven-assembly-plugin` con `jar-with-dependencies` y `appendAssemblyId=false` | Mete Apache Commons CSV dentro del mismo `.jar`. Así el profesor ejecuta un solo archivo, sin armar un *classpath* |
| `maven-jar-plugin` con `mainClass` | Hace que `java -jar` sepa que debe arrancar `co.medellin.transit.Main` |
| `maven-surefire-plugin` 3.1.2 | Versión que reconoce JUnit 5 (las antiguas no encuentran las pruebas) |

`mvn -DskipTests package` tardó 15,4 s la primera vez (con las dependencias descargándose)
en un contenedor Linux con 4 núcleos Intel Xeon a 2,1 GHz y Java 21.

**Opciones de ejecución que existen de verdad en el código:**

| Opción | Efecto | Dónde se lee |
|---|---|---|
| `--consola` (sin importar mayúsculas) | Usa el menú de texto en lugar de la ventana | `Main.main` |
| Otro argumento, por ejemplo `java -jar … data` | Carpeta de datos del grafo; si falta, se usa `data.directory` de `config.properties` (`data`) | `Main.main` |
| `-Dtransit.data.dir=<carpeta>` | Carpeta de la que el localizador lee `raw/stops_*.json`. Si no se pasa, `Main` la iguala a la carpeta de datos del grafo | `Main.main`, `AddressLocator.buildDefaultReferenceTable` |
| `-Dlog.level=WARNING` | Oculta los mensajes de carga. Por defecto es `INFO` y se imprimen unas 180 líneas antes de la pantalla de inicio | `Main.configureLogging` |

`config.properties` está en `src/main/resources/` y viaja **dentro del `.jar`**; cambiarlo
exige volver a compilar.

---

## 3. Uso

### La ventana

El diseño completo está en [DESIGN.md](DESIGN.md). Hay una sola pantalla: el formulario
de origen y destino con tres ejemplos de un clic, y el mapa de la red. Al buscar, el
resultado reemplaza al formulario: la duración total en grande; la tarifa, la distancia a
pie y los transbordos; y el viaje por **tramos** (cada bus o línea es un tramo con su
insignia, por ejemplo `A` para el Metro o `310` para un bus con su nombre público, que se
lee de `routes.json` en lugar del código interno `90007`). El mapa resalta el viaje y
atenúa el resto. Los errores aparecen bajo el formulario y marcan el campo culpable.

- **Teclado:** Enter pasa al destino y busca, Alt+I intercambia origen y destino, Esc
  vuelve a editar. El foco siempre se ve.
- **Tamaños:** compacto (< 760 px, una columna con el mapa arriba), medio y amplio.
- **Lectores de pantalla:** todos los controles tienen nombre accesible. En Windows hay
  que activar una vez Java Access Bridge con `jabswitch -enable` (viene con el JDK).
- **Arquitectura:** un único estado inmutable (`AppState`) que solo cambia el
  `AppController`; las vistas se redibujan a partir de él. La consulta corre en un hilo
  aparte, así que la ventana no se congela.
- **Fuentes:** IBM Plex (licencia OFL, incluida) viaja dentro del `.jar`.

**Qué se verificó de la ventana:** se ejecutó en una pantalla virtual (Xvfb) a 400, 560,
760, 900, 1120 y 1300 px de ancho, con búsquedas reales de Metro, bus y error. Las pruebas
`AppControllerTest`, `ViewsTest`, `ModelTest`, `ThemeTest` y `MessagesTest` (45 en total)
cubren los estados, lo que muestra cada vista con datos reales, el contraste de los
colores y que cada control tenga nombre accesible.

### La consola

Con `--consola`, la interacción es un ciclo de dos preguntas:

| Entrada | Qué hace | Componente |
|---|---|---|
| Dirección de origen | Se analiza, se convierte en coordenadas y, si es válida, se pide el destino | `Menu.run`, `Menu.geocode` |
| Dirección de destino | Igual que el origen; después se calcula la ruta y se imprime | `Router.findRoute`, `StepBuilder.buildTrip`, `FareCalculator.calculate`, `TripPrinter.print` |
| `salir` (en el origen, sin importar mayúsculas) | Imprime `¡Hasta luego!` y termina | `Menu.run` |
| Línea vacía | Vuelve a preguntar | `Menu.run` |

**Lo importante de esta tabla** es que todo el trabajo pesado (leer 19 MB de datos y
construir el grafo) pasa **una sola vez**, antes de la pantalla de inicio; cada consulta
después solo recorre el grafo que ya está en memoria.

### Formato de las direcciones

Se aceptan las abreviaturas de las bases de datos de la Alcaldía y las palabras completas:
`CL`/`CALLE`, `CR`/`KR`/`CRA`/`CARRERA`, `DG`/`DIAGONAL`, `TV`/`TRANSVERSAL`, `CIRCULAR`.
El separador puede ser `#`, `X`, `CON` o simplemente un espacio.

| Entrada | Se interpreta como |
|---|---|
| `CL 50 53-48` | calle 50, carrera 53 |
| `CR 70 x CL 48` | carrera 70 con calle 48 |
| `CL 10 43A-20` | calle 10, carrera 43,075 (cada letra suma 0,075) |
| `CL 12 Sur x CR 50` | calle −12 (Sur vuelve negativo el número), carrera 50 |
| `CL 48EE 99D-` | calle 48,375, carrera 99,3 (el guion final se completa como `-0`) |
| `hola mundo`, `VDA EL YOLOMBO`, `CL 900 x CR 900` | `Error: Formato de dirección inválido.` |

Las tres entradas inválidas de la última fila se probaron en la app compilada.

### Sesión de ejemplo (salida real)

Comando: `java -Dlog.level=WARNING -Dstdout.encoding=UTF-8 -jar
target/transit-routing-1.0.0.jar --consola`, con la entrada `CL 50 53-48`, `CR 70 x CL 48`, `salir`
enviada por tubería. Por eso las direcciones tecleadas no aparecen en la salida. Es el
caso 1 de `data/test_cases.csv`.

```
Ingrese la dirección de origen (o 'salir' para terminar):
Ingrese la dirección de destino:

============================
   RUTA ENCONTRADA
============================

Duración total: 11 minutos
Distancia a pie: 0 metros
Tarifa total: $3.800 COP

Instrucciones:
1. Abordar 90081 en CL 50 53-48, Medellin
2. Viajar en 90081 por 7 minutos (7 min)
3. Bajar en CL 50 67-114, Medellin

============================

Ingrese la dirección de origen (o 'salir' para terminar):
¡Hasta luego!
```

**Lo que hay que notar en esa traza:**

1. `90081` es el `id_ruta` interno de la Alcaldía, no el código que ve el pasajero. En
   `routes.json`, `90081` es la ruta **255A** (Nuevo Occidente – Las Flores). El caso de
   referencia espera la **191**: la app eligió otro bus.
2. `Distancia a pie: 0 metros` es falso. Nadie sale de su casa montado en el bus: la
   caminata desde la dirección hasta la parada y desde la parada hasta el destino no se
   está sumando ([limitación R1](#enrutamiento)).
3. Los 11 minutos son 5 de espera más unos 7 de recorrido (los minutos se redondean de
   forma distinta en el total y en el paso). La referencia espera 21; la diferencia sale,
   sobre todo, de la caminata que falta.
4. La tarifa de 3 800 COP sí es la correcta para un bus urbano.

### Protecciones

| Situación | Qué hace el programa | Qué evita |
|---|---|---|
| Falta la carpeta de datos | Mensaje `Error fatal: No se encontró el directorio de datos` (en la ventana, además, un cuadro de diálogo) y sale con código 1 | Un `NullPointerException` incomprensible |
| Grafo vacío después de cargar | Mensaje `El grafo de transporte está vacío` y sale | Consultas que nunca encuentran nada |
| Dirección que no encaja en el patrón | `Error: Formato de dirección inválido.` con dos ejemplos | Coordenadas inventadas |
| Dirección que no se puede interpolar | `Error: No se pudo localizar la dirección.` | — |
| Coordenada fuera de la caja de Medellín (lat 6,15–6,35; lon −75,65 a −75,50) | `Error: La dirección está fuera de los límites de Medellín.` | Rutas hacia otro municipio |
| Origen y destino a menos de 50 m | `El origen y destino están muy cerca (menos de 50 metros).` | Una "ruta" absurda de 0 m |
| No hay camino | `No se encontró una ruta entre las direcciones indicadas.` | — |
| Excepción al calcular | `Error al calcular la ruta: …` y vuelve al menú | Que una consulta mala tumbe la sesión |
| `OutOfMemoryError` al cargar | Explica cómo subir la memoria con `-Xmx` | — |
| **Fin de la entrada en modo consola (Ctrl+D o tubería que se acaba sin `salir`)** | **No está protegido:** termina con `NoSuchElementException: No line found` y un volcado de pila | Se comprobó enviando una entrada que termina en `q` |

---

## 4. Decisiones de diseño

| # | Decisión | Alternativa descartada | Razón en una línea |
|---|---|---|---|
| 1 | Solo Dijkstra | Floyd–Warshall, BFS, A\* | Floyd necesitaría 772 MB solo de distancias; BFS ignora los pesos; A\* no tiene una buena heurística con esperas y penalizaciones |
| 2 | Grafo de estados (lugar, ruta, sentido) | Un nodo por parada | Con un nodo por parada no se distingue "estar en el bus 191" de "estar en el 255A", y el transbordo no cuesta nada |
| 3 | Dos costos por arista: real y percibido | Minimizar solo el tiempo real | El tiempo real puro elige rutas con transbordos innecesarios por ganar segundos |
| 4 | Un Dijkstra multi-origen y multi-destino | Un Dijkstra por cada pareja de paradas cercanas | Una dirección tiene decenas de paradas a 500 m; correr cientos de búsquedas no tiene sentido |
| 5 | Geocodificar interpolando las direcciones de las propias paradas | API de Google o Nominatim | El requisito es funcionar sin internet y sin llaves de API |
| 6 | Datos descargados y precalculados en CSV | Consultar ArcGIS en cada ejecución | El profesor debe clonar y ejecutar, sin depender de que el servicio responda |
| 7 | Índice espacial de cuadrícula | Revisar las 13 895 posiciones en cada consulta | Reduce la búsqueda de paradas cercanas a unas pocas celdas |
| 8 | Ventana Swing que se abre antes de cargar el grafo | JavaFX, interfaz web, solo consola | Swing viene en el JDK: el profesor no instala nada y ve la pantalla de inicio de inmediato |

### Qué es Dijkstra, antes de ver cómo se usa

**Contexto.** Un grafo con pesos es un conjunto de puntos (nodos) unidos por flechas
(aristas), cada una con un costo positivo. El algoritmo de Dijkstra encuentra el camino de
menor costo total desde un origen hasta todos los demás nodos. Su idea es que **el nodo
pendiente más barato ya no puede mejorar**: como no hay costos negativos, cualquier otro
camino hacia él pasaría por un nodo que ya cuesta al menos lo mismo.

**Analogía.** Es como derramar agua en el origen: el agua llega primero a los lugares más
cercanos y avanza por orden de distancia. Cuando el agua toca un lugar por primera vez, ese
fue el camino más corto.

**Ejemplo con números pequeños.** Cuatro nodos y estas aristas: A→B 4, A→C 1, C→B 2, B→D 1.

```
        4
   A ───────► B ──1──► D
   │          ▲
   1          2
   ▼          │
   C ─────────┘
```

| Paso | Se saca de la cola | Distancias conocidas después del paso | Por qué |
|---|---|---|---|
| 1 | A (0) | B = 4, C = 1 | Se relajan las dos aristas de A |
| 2 | C (1) | B = **3** | 1 + 2 = 3 mejora el 4 directo |
| 3 | B (3) | D = 4 | 3 + 1 |
| 4 | D (4) | — | Fin |

El camino más corto a B no es la flecha directa (4) sino A→C→B (3). Con una cola de
prioridad (montículo binario), el costo es O((V + E) log V). En MetroBus, V = 13 895
estados y E = 82 160 aristas.

### 1. Por qué solo Dijkstra

**Problema.** Hay que encontrar el viaje de menor costo en un grafo con pesos positivos
(segundos), en menos de 100 ms por consulta, en una consola que debe arrancar rápido.

| Alternativa descartada | Por qué no |
|---|---|
| Floyd–Warshall (todas las parejas) | Calcula de antemano las 13 895² = 193 071 025 distancias. Como `int` son 772 MB solo para la matriz de distancias, más otra igual para reconstruir caminos, y 13 895³ ≈ 2,7 × 10¹² operaciones. Además, el origen y el destino del usuario son direcciones arbitrarias, no nodos fijos |
| BFS (búsqueda en anchura) | Encuentra el camino con menos aristas, no el más rápido. Un bus de 30 paradas sería "más largo" que caminar 5 cuadras por 5 aristas |
| A\* | Necesita una heurística que nunca sobrestime. La distancia en línea recta dividida por la velocidad del Metro sí es válida, pero ignora las esperas y las penalizaciones, así que poda poco. El proyecto decidió, el 26 de octubre, usar un solo algoritmo bien probado |

**Qué se eligió.** Dijkstra con la `PriorityQueue` de Java, en `Router.dijkstra`
([detalle 1](#1-la-cola-no-actualiza-prioridades-se-insertan-duplicados)).

**Costo.** Cada consulta recorre muchos estados que no llevan a ningún lado, porque
Dijkstra avanza en todas las direcciones por igual.

**Verificación.** `RouterTest` tiene 14 pruebas sobre grafos pequeños con resultados
calculados a mano; 10 pasan. Las 4 que fallan esperan un costo 30 s menor
([limitación R5](#enrutamiento)). Las consultas reales tardaron entre 1 y 22 ms
([mediciones](#mediciones-de-rendimiento)).

### 2. Por qué un grafo de estados y no un nodo por parada

**Problema.** En una misma parada pueden pasar hasta 31 rutas. Si la parada fuera un solo
nodo, el algoritmo no sabría si el pasajero sigue en el mismo bus o cambió de bus, y un
transbordo no costaría nada: la ruta "óptima" saltaría de bus en cada parada.

**Alternativas.**

| Alternativa descartada | Por qué no |
|---|---|
| Un nodo por parada | No distingue en qué vehículo va el pasajero; los transbordos son gratis |
| Un nodo por (parada, ruta) y aristas de transbordo entre todas las rutas de la parada | En una parada con 31 rutas son 31 × 30 = 930 aristas de transbordo |

**Qué se eligió.** Tres clases de estado (`model/StateType.java`) y cuatro clases de arista
(`model/EdgeKind.java`):

| Estado | Significado | Cantidad medida |
|---|---|---|
| `WALK` | A pie en una parada o estación | 3 665 (3 590 paradas + 75 estaciones) |
| `BUS_STOP` | Dentro de una ruta de bus, en un sentido, en esa parada | 10 034 |
| `METRO_STATION` | Dentro de una línea de transporte masivo, en un sentido, en esa estación | 196 |

```
                         BOARD (espera + 120 s)              RIDE (tiempo del tramo)
 (parada P, WALK) ───────────────► (P, ruta R1, ida) ────────────► (Q, ruta R1, ida)
      ▲      │                                                            │
      │      │ BOARD                                                      │ ALIGHT (0 s)
      │      ▼                                                            ▼
      │   (P, ruta R2, ida) ─► …                                  (parada Q, WALK)
      │                                                                   │
      └──────────── WALK (caminata entre paradas a menos de 300 m) ───────┘
```

El estado `WALK` es un **eje**: para cambiar de bus hay que bajarse (ALIGHT) a ese estado y
volver a subir (BOARD), y cada subida cobra la espera y la penalización. Una parada con 31
rutas necesita 31 BOARD y 31 ALIGHT, es decir, 62 aristas en lugar de 930.

El fragmento que crea las aristas BOARD de los buses, en `GraphBuilder.build`:

```java
// Board edges for bus stops
for (Map.Entry<String, Integer> entry : stopRouteToStateId.entrySet()) {
    String[] parts = entry.getKey().split("_");
    int stopId = Integer.parseInt(parts[0]);
    Integer walkStateId = stopIdToWalkStateId.get(stopId);
    if (walkStateId == null) {
        continue;
    }
    int busStopStateId = entry.getValue();

    // Average wait time for buses (from config, default 300s = 5 min)
    int waitTimeSec = 300;
    int realSec = waitTimeSec;
    int perceivedCost = waitTimeSec + boardingPenaltySec;

    Edge boardEdge = new Edge(walkStateId, busStopStateId, EdgeKind.BOARD, realSec, perceivedCost);
    graph.addEdge(boardEdge);
    boardEdges++;
}
```

- La clave del mapa es `"<parada>_<ruta>_<sentido>"`; `split("_")` recupera la parada para
  encontrar su estado `WALK`.
- `realSec = 300`: la espera promedio sí se le muestra al usuario como parte del viaje.
- `perceivedCost = 300 + 120`: los 120 s extra solo existen para que Dijkstra prefiera
  menos transbordos (ver la [decisión 3](#3-por-qué-cada-arista-tiene-dos-costos)).
- El comentario dice "from config", pero el 300 está escrito a mano: la configuración no se
  lee aquí ([limitación G1](#grafo)).

**Costo.** El grafo tiene casi cuatro veces más nodos que paradas (13 895 frente a 3 590),
y la ruta de bus se convierte en parte de la identidad del nodo.

**Verificación.** La carga real imprime `Estados: 13895 (esperados: 13895)` y
`Aristas: 82160 (esperadas: 82160)`: coincide con el diseño de `graph-structure.md` y con
el prototipo de referencia en Python.

### 3. Por qué cada arista tiene dos costos

**Problema.** Si Dijkstra minimiza solo los segundos reales, propone rutas que nadie
tomaría: bajarse de un bus para tomar otro que ahorra 20 s, o encadenar seis caminatas
cortas.

**Qué se eligió.** Cada `Edge` guarda `realSec` (lo que se le muestra al usuario) y
`perceivedCost` (lo que minimiza Dijkstra):

| Arista | `realSec` | `perceivedCost` |
|---|---|---|
| RIDE de bus | metros ÷ 18 km/h + 20 s de parada | igual a `realSec` |
| RIDE de transporte masivo | `sec_to_next` de `transit_lines.csv` | igual a `realSec` |
| BOARD | espera promedio (300 s bus, 180 s masivo) | espera + 120 s |
| ALIGHT | 0 | 0 |
| WALK | `walk_sec` de `walk_links.csv` | `walk_sec × 1,5 + 30 s` |

**Ejemplo con números.** Dos opciones para el mismo viaje:

| | Opción A: bus directo | Opción B: dos buses |
|---|---|---|
| Tiempo real | 300 espera + 900 viaje = **1 200 s** | 300 + 400 + 300 + 150 = **1 150 s** |
| Penalizaciones | 1 subida × 120 = 120 | 2 subidas × 120 = 240 |
| Costo percibido | **1 320** | **1 390** |

Con tiempo real puro gana B por 50 s y obliga a un transbordo. Con costo percibido gana A,
que es lo que haría una persona. Después, la app muestra los 1 200 s **reales** de A, no
los 1 320.

**Costo.** Los valores 120 s, × 1,5 y +30 s son estimaciones calibradas con 300 viajes
aleatorios en el prototipo de Python, no datos oficiales. Con otras personas, otros valores
serían mejores.

**Verificación.** `Router.calculateRealDuration` suma solo `getRealSec()` a lo largo del
camino; las penalizaciones nunca llegan a la pantalla. Lo que todavía no se suma es la
caminata de acceso ([limitación R1](#enrutamiento)).

### 4. Por qué un solo Dijkstra con muchos orígenes y muchos destinos

**Problema.** Una dirección no es un nodo del grafo. Cerca de `CL 50 53-48` hay decenas de
paradas a menos de 500 m, y lo mismo pasa en el destino. ¿Desde cuál se empieza?

| Alternativa descartada | Por qué no |
|---|---|
| Elegir la parada más cercana al origen y la más cercana al destino | La más cercana puede no tener ninguna ruta útil |
| Un Dijkstra por cada pareja (parada de origen, parada de destino) | Con 40 × 40 parejas son 1 600 búsquedas por consulta |

**Qué se eligió.** Todas las paradas `WALK` a menos de 500 m del origen entran a la cola a
la vez, cada una con el costo de caminar hasta ella; la búsqueda acaba en cuanto saca de la
cola una parada cercana al destino. Fragmento de `Router.dijkstra`:

```java
// Add all sources with walking cost from origin (access walking)
for (State source : sources) {
    sourceSet.add(source.getId());
    // Calculate walking distance and time
    double distanceMeters = haversineDistance(origin.getCoord(), source.getPlace().getCoord());
    double walkDistanceMeters = distanceMeters * params.getWalkStreetFactor();
    int walkTimeSec = (int) Math.round(walkDistanceMeters / params.getWalkingSpeedMps());
    // Apply walk penalty
    int perceivedCost = (int) Math.round(walkTimeSec * params.getWalkPenaltyMultiplier()) + params.getWalkPenaltyConstant();

    distances[source.getId()] = perceivedCost;
    pq.offer(new Node(source.getId(), perceivedCost));
}
```

- `haversineDistance` da la distancia en línea recta sobre la esfera terrestre.
- `× getWalkStreetFactor()` (1,3) la convierte en una distancia aproximada por calles, porque
  nadie camina en diagonal a través de las manzanas.
- `÷ getWalkingSpeedMps()` (1,25 m/s = 4,5 km/h) la convierte en segundos.
- El costo inicial es **percibido**: así Dijkstra prefiere una parada a 100 m frente a una a
  450 m si el resto del viaje es parecido.

Es lo mismo que agregar un nodo virtual "origen" unido a todas esas paradas, pero sin
modificar el grafo, que queda de solo lectura.

**Costo.** Del lado del destino, el código **no** suma la caminata desde la parada final
hasta la dirección: cualquier parada a 500 m o menos cuenta como llegada
([limitación R2](#enrutamiento)).

**Verificación.** Las 15 consultas de referencia encuentran ruta; ninguna devuelve "No se
encontró una ruta".

### 5. Por qué geocodificar con las direcciones de las propias paradas

**Problema.** El usuario escribe una dirección, pero el grafo trabaja con latitud y
longitud. Los servicios de geocodificación (Google, Nominatim) exigen internet y, en el
caso de Google, una llave de pago.

**Qué se eligió.** La nomenclatura de Medellín es casi una cuadrícula: `CL 50 53-48`
significa calle 50 con carrera 53. Las 3 590 paradas traen su propia dirección y sus
coordenadas, así que sirven de mapa. `AddressLocator` analiza las direcciones de las
paradas (3 431 se pudieron analizar y 159 se descartaron, según el registro de carga) y,
para una dirección nueva, promedia las coordenadas de las 4 paradas más parecidas en
(calle, carrera), con peso 1 / distancia. Fragmento de `AddressLocator`:

```java
private List<WeightedPoint> findClosestPoints(double tCalle, double tCarrera, int n) {
    PriorityQueue<WeightedPoint> pq = new PriorityQueue<>(Comparator.comparingDouble(wp -> wp.distance));
    for (ReferencePoint ref : referenceTable) {
        double cDist = Math.abs(ref.calle - tCalle);
        double crDist = Math.abs(ref.carrera - tCarrera);
        double dist = Math.sqrt(cDist * cDist + crDist * crDist);
        double weight = dist < 0.01 ? 1000.0 : 1.0 / dist;
        pq.offer(new WeightedPoint(ref, dist, weight));
    }
    List<WeightedPoint> result = new ArrayList<>();
    for (int i = 0; i < n && !pq.isEmpty(); i++) result.add(pq.poll());
    return result;
}
```

- La distancia se mide en "unidades de dirección" (calles y carreras), no en metros.
- `dist < 0.01 ? 1000.0`: si la dirección coincide con una parada, 1 / 0 sería infinito; el
  peso 1 000 hace que esa parada domine el promedio sin dividir entre cero.

**Ejemplo.** Para `CL 50 53-48` el analizador da (50, 53). Si las cuatro paradas más
parecidas están a distancias 0, 1, 1 y 2, sus pesos son 1 000, 1, 1 y 0,5: la coordenada
resultante es prácticamente la de la primera.

**Costo.** Es una aproximación. Según `graph-structure.md`, en el prototipo el error medido
sobre 300 paradas fue de 75 m de mediana y 327 m en el percentil 90; en Java no se volvió a
medir. Las veredas (`VDA …`) y las circulares no se pueden ubicar. El prototipo, además,
rechaza direcciones a más de 6 unidades de la parada más cercana; el código Java no tiene
ese límite.

**Verificación.** `AddressParserTest` y `AddressLocatorTest`. Con
`-Dtransit.data.dir=data`, la mayoría de las pruebas de geocodificación pasan; sin la
opción, fallan 21 ([pruebas](#7-pruebas)).

### 6. Por qué los datos vienen precalculados en CSV

**Problema.** El servicio ArcGIS de la Alcaldía entrega paradas y trazados, pero no el
tiempo entre paradas, ni el orden de las estaciones del Metro, ni qué paradas están a
distancia caminable.

**Qué se eligió.** Unos scripts de Python (fuera de este repositorio) calcularon una vez
tres archivos derivados, y el programa Java solo los lee:

| Archivo | Filas | Qué contiene |
|---|---|---|
| `data/bus_segments.csv` | 9 550 | Tramo entre dos paradas consecutivas de una ruta, con la distancia **por la calle** (`used_m`) |
| `data/transit_lines.csv` | 98 | Estaciones de cada línea de Metro, Metroplús, tranvía y cable, en orden, con `sec_to_next` |
| `data/walk_links.csv` | 25 865 | Parejas de paradas o estaciones a menos de 300 m, con `walk_sec` |
| `data/raw/stops_*.json` | 10 043 registros, 3 590 paradas únicas | Las paradas tal como las entrega ArcGIS (6 archivos de 2 000) |
| `data/raw/routes.json` | 252 | Rutas de bus: `id_ruta`, `codigo`, `nombre`, `tipo` |

**Costo.** El repositorio pesa 19 MB de datos, y los datos son una foto del 26 de octubre
de 2026: si la Alcaldía cambia una ruta, la app no se entera.

**Verificación.** Los conteos de la tabla salen del registro de carga de la app y de
`wc -l`.

### 7. Por qué un índice espacial de cuadrícula

**Problema.** En cada consulta hay que encontrar los estados a menos de 500 m del origen y
del destino. Recorrer los 13 895 y calcular la distancia a cada uno funciona, pero es
desperdicio.

**Qué se eligió.** `TransitGraph` reparte los estados en celdas de 0,005° (unos 550 m) y,
para una consulta, solo revisa las celdas vecinas:

```java
double radiusDegrees = radiusMeters / 111000.0; // approximate meters to degrees
int cellsToSearch = (int) Math.ceil(radiusDegrees / GRID_CELL_SIZE_DEGREES) + 1;
```

Con 500 m: 500 / 111 000 = 0,0045°, que cabe en 1 celda, más 1 de margen = 2. Se revisa un
cuadrado de 5 × 5 celdas y después se filtra con la distancia exacta.

**Costo.** Una celda de 0,005° de longitud mide unos 553 m en Medellín, no exactamente
500 m; el margen de una celda cubre la diferencia.

**Verificación.** `routing/PerformanceTest.java` usa `getStatesNearPlace` sobre el grafo
real y pasa. No hay una prueba que compare el resultado con una búsqueda exhaustiva.

### 8. Por qué una ventana Swing que se abre antes de cargar el grafo

**Problema.** El requisito del proyecto es que, al ejecutar la app desde la consola, se
abra la pantalla de inicio. Construir el grafo tarda entre 0,7 y 0,8 s y el arranque
completo pasa de 1 s; con una ventana que aparece solo al final, el usuario ve un
segundo de nada.

| Alternativa descartada | Por qué no |
|---|---|
| JavaFX | Desde Java 11 no viene en el JDK: habría que agregar dependencias por sistema operativo al `pom.xml` |
| Interfaz web (servidor local y navegador) | Un servidor, un puerto y un navegador para una app de una sola persona |
| Solo consola | Era la versión anterior; se conserva como `--consola` |

**Qué se eligió.** `Main` abre la ventana **primero** y carga el grafo después; cuando
termina, le entrega a la ventana un `TripPlanner` ya armado. Fragmento de `Main.main`:

```java
// In graphical mode, show the home screen right away while the graph loads
if (!consoleMode) {
    window = MainWindow.open();
}
```

y, al final de la carga:

```java
// Phase 4a: Graphical mode - the window is already open; enable the planner
if (window != null) {
    TripPlanner tripPlanner = new TripPlanner(router, addressParser, addressLocator,
            fareCalculator, stepBuilder);
    window.showReady(tripPlanner, String.format(
            "Red cargada: %,d nodos y %,d conexiones.", graph.getStateCount(), graph.getEdgeCount())
            .replace(',', '.'));
    LOGGER.info("Graphical interface ready");
    return;
}
```

- `consoleMode` empieza en `GraphicsEnvironment.isHeadless()`: en un servidor sin pantalla
  la app no intenta abrir una ventana y cae a la consola.
- `showReady` usa `SwingUtilities.invokeLater`, porque Swing solo admite cambios desde su
  propio hilo de eventos y la carga corre en el hilo principal.
- `.replace(',', '.')` convierte el separador de miles al uso colombiano (13.895).
- El `return` deja vivo el programa: lo mantiene abierto el hilo de la ventana.

**Costo.** La lógica de "validar dirección, localizar, comprobar los 50 m, buscar ruta y
calcular la tarifa" quedó **dos veces**: en `TripPlanner.plan` (ventana) y en `Menu.run`
(consola), con mensajes casi iguales. Un cambio en una puede olvidarse en la otra
([limitación U4](#interfaz)).

**Verificación.** Captura en Xvfb al primer segundo (cargando) y a los ocho (lista), y
las pruebas de `ui`: 31 de 31 pasan.

---

## 5. Detalles de implementación que importan

### 1. La cola no actualiza prioridades: se insertan duplicados

La `PriorityQueue` de Java no tiene la operación "bajar la prioridad de un elemento". Cuando
se encuentra un camino más barato a un estado, se inserta **otro** nodo y el viejo se
ignora al salir. Fragmento de `Router.dijkstra`:

```java
Node current = pq.poll();
int currentId = current.stateId;
int currentCost = current.cost;

// Skip if already visited with better cost
if (visited[currentId]) {
    continue;
}
visited[currentId] = true;
```

**Si no estuviera el `if (visited…)`**, cada copia vieja volvería a relajar todas las
aristas de su estado con un costo peor. El resultado seguiría siendo correcto, pero el
trabajo podría multiplicarse por el número de copias.

### 2. Un origen nunca puede ser el destino (y por qué la caminata sola es imposible)

```java
if (targetSet.contains(currentId) && previous[currentId] != -1) {
```

`previous[currentId] != -1` exige haber recorrido al menos una arista. Sin esa condición, si
una parada estuviera a menos de 500 m del origen **y** del destino, la búsqueda terminaría
en el paso cero con un camino de un solo estado, y `StepBuilder` lanzaría
`Path must contain at least 2 states`.

**El costo de esta protección:** la app nunca puede responder "camine". En el caso 4
(`CL 30 65-10` a `CL 30 66-20`, una cuadra), la referencia es caminar 9 minutos gratis;
la app hace subir al usuario al Metroplús línea 1 dos minutos y le cobra 3 800 COP
(salida real). La corrección está en la [limitación R3](#enrutamiento).

### 3. El arreglo de adyacencia tiene tamaño fijo

`GraphBuilder` crea `new TransitGraph(EXPECTED_STATES)` con 13 895 posiciones. Si los datos
crecieran, `TransitGraph.expandAdjacency` no amplía nada: lanza
`UnsupportedOperationException("Dynamic adjacency expansion not supported…")`, porque el
arreglo es `final`. Hoy funciona porque el conteo es exacto.

### 4. Las estaciones se identifican con un hash

`walk_links.csv` nombra las estaciones como `STATION:<nombre>`, pero `WalkLinkReader` las
convierte en números con `Math.abs(stationKey.hashCode() % 100_000_000)`, y `GraphBuilder`
repite ese cálculo para encontrarlas:

```java
String stationKey = "STATION:" + stationName;
int hashId = Math.abs(stationKey.hashCode() % 100_000_000);
stopIdToWalkStateId.put(hashId, nextStateId[0]);
```

Si el hash de una estación coincidiera con el `id_paradero` de una parada, una de las dos
sobrescribiría a la otra en el mapa. Con los datos actuales no ocurre: la carga informa
`0 enlaces omitidos`. Pero depende de la suerte; lo robusto es usar la cadena
`STATION:nombre` como clave (el mapa `placeStringToWalkStateId` ya se llena con esas claves, pero nunca se consulta).

### 5. Los minutos del total y de cada paso se redondean distinto

`TripPrinter.printSummary` usa `trip.getTotalDurationSec() / 60` (división entera, hacia
abajo) y `StepBuilder.createRideStep` usa `Math.ceil(durationSec / 60.0)` (hacia arriba).
Por eso los pasos pueden sumar más minutos que el total impreso.

---

## 6. Estructura del proyecto

```
MetroBus/
├── pom.xml                         Maven: Java 17, Commons CSV, JUnit 5, Mockito
├── data/                           Datos (19 MB), ver decisión 6
│   ├── bus_segments.csv            9 550 tramos de bus
│   ├── transit_lines.csv           98 estaciones en orden por línea
│   ├── walk_links.csv              25 865 parejas caminables
│   ├── test_cases.csv              15 viajes de referencia
│   └── raw/                        JSON originales de ArcGIS (paradas, rutas, trazados)
└── src/main/java/co/medellin/transit/
    ├── Main.java                   Arranque: carga el grafo y abre la pantalla de inicio
    ├── ui/                         TripPlanner y PlanResult (lógica de una consulta);
    │   │                           HomeScreen, Menu, TripPrinter (modo consola)
    │   └── gui/                    MainWindow, Theme, Fonts; state/ (estado y controlador),
    │                               view/ (mapa, formulario, resultado), kit/, model/
    ├── geocoding/                  AddressParser (texto → números),
    │                               AddressLocator (→ lat/lon), StreetType, Cardinal…
    ├── routing/                    Router (Dijkstra), StepBuilder (camino → pasos),
    │                               Parameters (lee config.properties), DijkstraResult
    ├── fares/                      FareCalculator, FareConfig, FareResult, FareSegment
    ├── graph/                      GraphBuilder (construye), TransitGraph (consulta)
    ├── model/                      State, Edge, Place, Coordinate, Trip, Step, enums
    ├── data/                       ArcGisReader, CsvReader y un lector por archivo
    └── util/PerformanceBenchmark.java   Medición manual de tiempos (no la usa Main)
```

En la raíz hay además 22 archivos `.md` de seguimiento generados durante el desarrollo
con Kiro (`TASK_*.md`, `KIRO_*.md`, `FINAL_STATUS.md`…), `test_all_cases.ps1` y cuatro
registros `main-error*.txt` de 2 a 8 MB. No forman parte de la aplicación
([pendientes](#9-estado-actual-y-pendientes)).

**Por qué la separación es así.** Cada capa solo conoce a la de abajo: `model` no conoce a
nadie; `graph` no sabe de direcciones; `routing` no sabe de tarifas; `ui` no sabe cómo se
construye el grafo.

```
 ui ──► routing ──► graph ──► model
  │        │          ▲
  │        │          └── data
  ├──► geocoding ──► data
  └──► fares ──► graph, model
```

**Excepciones con su justificación:**

- `graph/GraphBuilder` importa `routing/Parameters` para leer las velocidades y
  penalizaciones; es una dependencia hacia arriba que podría evitarse moviendo
  `Parameters` a un paquete de configuración.
- `geocoding/AddressLocator` lee las paradas por su cuenta con `StopArcGisReader`, en vez de
  recibirlas del grafo, y encuentra la carpeta a través de una propiedad del sistema que
  `Main` fija justo antes de crearlo:

  ```java
  // AddressLocator reads its reference stops from this property; point it at our data
  if (System.getProperty("transit.data.dir") == null) {
      System.setProperty("transit.data.dir", dataDir.toString());
  }
  ```

  Funciona, pero es una comunicación escondida: quien cree un `AddressLocator()` en otro
  lugar (como las pruebas) recibe la ruta fija del autor
  ([limitación D1](#datos-y-geocodificación)). Los JSON de paradas se leen dos veces al
  arrancar. El constructor `AddressLocator(List<Stop>)` ya existe y evitaría ambas cosas.

---

## 7. Pruebas

```bash
mvn test                                           # las 423 pruebas
mvn test -DargLine="-Dtransit.data.dir=$PWD/data"  # igual, con la carpeta de paradas
```

Los 15 casos de referencia se ejecutaron con un script de bash, que no está en el
repositorio, que envía cada pareja de direcciones a la app y extrae `Duración total`,
`Distancia a pie`, `Tarifa total` y los pasos `Abordar`.

Todas las cifras de esta sección se midieron el 3 de noviembre de 2026 en un contenedor
Linux con 4 núcleos Intel Xeon a 2,1 GHz y OpenJDK 21, sobre una copia exacta de la carpeta
del proyecto.

### Resultado de `mvn test`

| Paquete | Pruebas | Fallos | Errores | Omitidas | Qué valida |
|---|---|---|---|---|---|
| `model` | 74 | 0 | 0 | 0 | Validaciones de `State`, `Edge`, `Place`, `Trip`… |
| `fares` | 27 | 0 | 0 | 0 | `FareCalculator` con viajes armados a mano |
| `ui` | 68 | 0 | 0 | 0 | Consola, `TripPlanner` y la ventana: estados, vistas, colores y accesibilidad |
| `MainTest` | 7 | 0 | 0 | 0 | Arranque |
| `data` | 96 | 24 | 1 | 0 | Lectores de CSV y JSON |
| `geocoding` | 95 | 21 | 0 | 0 | Análisis y localización de direcciones |
| `routing` | 70 | 4 | 3 | 0 | Dijkstra en grafos pequeños y casos de referencia |
| `graph` | 23 | 2 | 4 | 8 | Construcción del grafo |
| **Total** | **460** | **51** | **8** | **8** | `BUILD FAILURE` |

Con `-DargLine="-Dtransit.data.dir=…/data"` el total baja a **33 fallos y 8 errores**: 18 de
los 21 fallos de geocodificación eran solo la ruta fija de Windows. En el equipo del autor,
con Windows y antes de la ventana (399 pruebas), la ejecución del 3 de noviembre dio
35 fallos y 5 errores. Los 3 errores de
más aquí son `ReferenceTest`, `ReferenceCasesTest` y `QuickReferenceTest`, que tienen
escrita la ruta `C:\Users\marti\Downloads\…` y fuera de ese equipo no pueden ni
arrancar. La diferencia de 2 fallos no se investigó.

**Fallos agrupados por causa** (salida de `mvn test`):

| Grupo | Pruebas | Qué dicen |
|---|---|---|
| `WalkLinkReaderTest` | 10 fallos | Las pruebas escriben identificadores como `1001`; el lector exige `STOP:1001` o `STATION:nombre`, que es el formato real de `walk_links.csv` (con el archivo real lee 25 865 enlaces). Las pruebas quedaron atrás del formato |
| `StopArcGisReaderTest`, `PaginationTest`, `PreservationTest` | 9 fallos | Esperan el nombre de la parada y reciben su dirección o `Parada N` |
| `DataLoadingBugConditionTest` | 4 fallos, 1 error | Eran pruebas que demostraban un error ya corregido; ahora fallan porque el error ya no existe, y una busca un archivo `ReferenceTestSuite.java` que se borró |
| `RouterTest` | 4 fallos | Cada caminata cuesta 30 s más de lo esperado ([R5](#enrutamiento)) |
| `GraphBuilderTest`, `GraphBuilderIntegrationTest` | 2 fallos, 4 errores | Las pruebas escriben solo CSV en la carpeta temporal, sin paradas en `raw/`; sin paradas no se crea ningún estado `WALK` y el grafo queda vacío (`El grafo está vacío`) o con otros conteos |
| `AddressLocatorTest`, `GeocodingIntegrationTest` | 21 fallos (3 con la opción) | La ruta fija de Windows ([D1](#datos-y-geocodificación)) |
| Referencia | 3 errores | La ruta fija de Windows en las pruebas |

### Los 15 casos de referencia

`data/test_cases.csv` trae 15 viajes cuyo resultado esperado calculó el prototipo de
Python con los mismos datos y las reglas de `fares.md`. Se ejecutaron uno por uno contra la
app compilada en modo consola (salida real, procesada con un script). Se corrieron antes de
que se agregara la ventana; `Router`, `GraphBuilder`, `StepBuilder` y `FareCalculator` no
cambiaron con ella, y el caso 1 se repitió con la versión nueva con el mismo resultado. Los vehículos de la app se tradujeron
de `id_ruta` a código público con `routes.json` para poder compararlos.

| # | Min. esperados | Min. app | COP esperado | COP app | Vehículos esperados | Vehículos app | A pie (app) |
|---|---|---|---|---|---|---|---|
| 1 | 21 | 11 | 3 800 | 3 800 | bus 191 | bus 255A | 0 m |
| 2 | 34 | 21 | 3 820 | 3 820 | metro A | metro A | 349 m |
| 3 | 31 | 19 | 3 820 | 3 800 | Metroplús 2 | Metroplús 2 | 0 m |
| 4 | 9 | 4 | 0 | 3 800 | a pie | Metroplús 1 | 0 m |
| 5 | 24 | 13 | 3 800 | 3 800 | bus 195 | bus 195 | 0 m |
| 6 | 46 | 36 | 11 420 | 3 800 | bus 243 > metro B > bus 91 | bus 310 | 93 m |
| 7 | 18 | 9 | 3 820 | 3 820 | cable J | cable J | 0 m |
| 8 | 76 | 66 | 11 420 | 12 335 | bus 289 > cable P > cable K > bus 57 | igual | 118 m |
| 9 | 76 | 65 | 11 420 | 11 400 | bus 255P > cable J > metro B > bus 90 | bus 255D > bus 246D > bus 105 | 79 m |
| 10 | 28 | 17 | 4 715 | 7 600 | Metroplús 1 > bus 254i | Metroplús 1 > bus 281 | 112 m |
| 11 | 54 | 44 | 4 715 | 4 715 | bus C23i DERECHA > metro B | igual | 159 m |
| 12 | 66 | 53 | 8 515 | 12 335 | bus 304 > metro A > metro B > bus C23i | bus 304 > A > B > bus 227i-A | 299 m |
| 13 | 48 | 35 | 7 600 | 7 600 | bus 301 > bus C23_Alterna | igual | 0 m |
| 14 | 61 | 55 | 4 715 | 12 355 | bus 250Aii > cable J > metro B > metro A | igual | 102 m |
| 15 | 12 | 8 | 3 800 | 3 800 | bus 94 | bus 94 | 0 m |

**Lo que hay que notar en esta tabla:**

1. **Ninguna duración está dentro de ±2 minutos.** Todas son **más cortas**, entre 4 y 13
   minutos (10 de mediana). Una diferencia siempre en el mismo sentido apunta a algo que
   falta, no a ruido: la caminata de acceso y de salida, que no se suma
   ([R1](#enrutamiento)).
2. **Los vehículos coinciden en 9 de 15** (casos 2, 3, 5, 7, 8, 11, 13, 14 y 15). El grafo y
   Dijkstra encuentran el mismo recorrido que el prototipo cuando la diferencia de costo es
   clara.
3. **Las tarifas coinciden en 8 de 15** (dentro de ±100 COP). Los errores se explican todos
   por la regla de clasificación de `FareCalculator` ([F1 a F3](#tarifas)): 12 335 y 12 355
   son 3 800 + 915 + 3 820 + 3 800 y 3 800 + 915 + 3 820 + 3 820, es decir, cada tramo de
   Metro después del primero vuelve a pagar.
4. **El caso 4 cobra por un viaje que debía hacerse a pie** ([detalle 2](#2-un-origen-nunca-puede-ser-el-destino-y-por-qué-la-caminata-sola-es-imposible)).

**Por qué el script `test_all_cases.ps1` no sirve para medir esto.** Busca el texto
`Costo total:`, pero la app imprime `Tarifa total:`, así que el costo siempre queda en
`N/A`; y termina la entrada con `q` en lugar de `salir`, lo que provoca el
`NoSuchElementException` de la tabla de [protecciones](#protecciones).

### Mediciones de rendimiento

| Medición | Resultado | Cómo se midió |
|---|---|---|
| Construcción del grafo | 697 ms, 760 ms, 825 ms | Línea `Tiempo de carga` del registro, 3 arranques |
| Arranque completo hasta poder consultar | 1 029 ms, 1 065 ms, 1 141 ms | Línea `Initialization complete`, mismos 3 arranques (versión de consola, antes de la ventana; la carga no cambió) |
| Memoria usada al terminar la carga | 80 a 81 MB | Misma línea (diferencia de `totalMemory − freeMemory`) |
| Tiempo por consulta | 1 a 22 ms (15 consultas en una sola sesión: 8, 4, 6, 1, 3, 20, 1, 22, 9, 3, 4, 9, 6, 10, 1) | Línea `Route found in` con `-Dlog.level=FINE` |

**El resultado raro:** la meta de `config.properties` es arrancar en menos de 1 s, y el
grafo sí se construye en menos de 1 s, pero el arranque completo no: la propia app imprime
`WARNING: Graph loading time exceeded 1 second target`. La diferencia es de unos 300 ms. Una
causa probable, **no medida por separado**, es que `AddressLocator` vuelve a leer y
deduplicar las 10 043 paradas que el grafo ya había leído (ver la segunda excepción de la
[estructura](#6-estructura-del-proyecto)).

---

## 8. Limitaciones conocidas

### Datos y geocodificación

**D1. La ruta del equipo del autor sigue escrita en el código.** `AddressLocator()` lee
las paradas de `System.getProperty("transit.data.dir",
"C:\\Users\\marti\\Downloads\\transporte-medellin\\kiro\\data")`. La app ya no
depende de ella, porque `Main` fija la propiedad antes (se comprobó: sin ninguna opción,
el caso 1 devuelve su ruta). Pero las pruebas de geocodificación y las tres de referencia
sí: por eso fallan fuera de ese equipo. *Corrección:* pasar al localizador las paradas que
el grafo ya cargó, con `AddressLocator(List<Stop>)`, y quitar la ruta de las pruebas.

**D2. No se rechazan direcciones lejanas a cualquier parada.** El prototipo rechaza una
dirección si la parada de referencia más parecida está a más de 6 unidades; Java siempre
interpola. *Corrección:* devolver `Optional.empty()` si `closest.get(0).distance > 6`.

**D3. Veredas y circulares no se ubican.** No tienen número de calle y carrera.

### Grafo

**G1. Varios parámetros de `config.properties` se ignoran.** Están escritos a mano en
`GraphBuilder`: la espera del bus (300 s), la del transporte masivo (180 s para todos,
cuando la configuración dice Metro 240, Metroplús 300, tranvía 300 y cable 120), la
velocidad del bus (siempre 18 km/h; `getBusSpeed` no distingue Directa Centro ni Circular,
que van a 15) y la parada de 20 s. *Corrección:* leer `wait.*`, `bus.speed.*` y
`bus.dwellTimeSec` desde `Parameters`, y usar el campo `tipo` de `routes.json`.

**G2. El regreso de cada ruta de bus se estima en 120 s fijos.** `graph-structure.md` pide
línea recta × 1,3 entre la última parada de ida y la primera de vuelta (mediana 343 m).

**G3. Capacidad fija de 13 895 estados** ([detalle 3](#3-el-arreglo-de-adyacencia-tiene-tamaño-fijo)).

### Enrutamiento

**R1. La caminata al principio y al final no aparece en el viaje.** `Router` usa la
caminata de acceso para elegir la parada, pero `calculateRealDuration` solo suma aristas
del grafo y `StepBuilder` no crea los pasos "Camine hasta…". Resultado: `Distancia a pie:
0 metros` en 7 de 15 casos y todas las duraciones cortas. *Corrección:* guardar en
`DijkstraResult` los metros y segundos de acceso y de salida, sumarlos a la duración y
crear los dos pasos `WALK`.

**R2. El destino no cuesta nada.** Cualquier parada a 500 m del destino cuenta como llegada,
sin sumar la caminata restante. *Corrección:* como en el prototipo, al sacar un estado
destino calcular `d + caminata percibida hasta la dirección` y terminar cuando lo que queda
en la cola cueste más que el mejor total.

**R3. Nunca se recomienda caminar.** Ver el [detalle 2](#2-un-origen-nunca-puede-ser-el-destino-y-por-qué-la-caminata-sola-es-imposible).
*Corrección:* empezar con `best = costo percibido de caminar todo el trayecto` y, si al
final caminar en tiempo real es igual o más rápido, devolver un viaje a pie con tarifa 0.

**R4. Los buses se muestran con su `id_ruta` interno** (`Abordar 90081`) en lugar del
código público (`255A`) y el nombre. *Corrección:* usar `codigo` y `nombre` de
`routes.json` en `StepBuilder`.

**R5. `RouterTest` espera caminatas 30 s más baratas.** En `testFindRouteSimpleWalkPath`
el origen está encima de la parada; la prueba espera 120 s y el código da 150 s, porque la
caminata de acceso de 0 m cuesta `0 × 1,5 + 30 = 30 s`. Hay que decidir cuál de los dos sigue la especificación (en `algorithm.md`, la
caminata de acceso sí lleva × 1,5 + 30 s) y corregir el otro.

### Tarifas

**F1. Metroplús se cobra como bus.** `getRouteType` decide por el primer carácter del
identificador: letra = Metro, número = bus. Las líneas de Metroplús se llaman `1` y `2`,
así que pagan 3 800 en vez de 3 820 y nunca se integran con un bus (casos 3 y 10).

**F2. Cada tramo de transporte masivo después del primero vuelve a pagar.** La integración
solo mira las dos primeras subidas y exige modos distintos (`mode != firstBoardingMode`),
así que metro A > metro B paga dos veces. `fares.md` dice que dentro del sistema masivo, en
90 minutos, no hay recargo hasta la cuarta integración (casos 8, 12 y 14).

**F3. No se comprueba que el bus sea `Integrada`.** Cualquier bus seguido de Metro paga
4 715 por la pareja; según `fares.md`, solo las rutas `Integrada` tienen ese precio.

**F4. La ventana de 90 minutos no cuenta la espera.** Los pasos BOARD tienen duración 0 en
`StepBuilder`, y `FareCalculator` mide el tiempo sumando duraciones de pasos.

*Corrección de F1 a F4:* reescribir `FareCalculator.calculate` siguiendo el pseudocódigo de
la sección 3 de `fares.md`, con el modo y el `tipo` tomados del estado del grafo y no del
texto del identificador.

### Interfaz

**U1. Terminar la entrada sin `salir` tumba el programa** con `NoSuchElementException`.
*Corrección:* comprobar `scanner.hasNextLine()` antes de cada `nextLine()` en `Menu.run`.

**U2. Las tildes dependen de la codificación de la terminal**
([compilar y ejecutar](#2-compilar-y-ejecutar)).

**U3. El arranque imprime unas 180 líneas de registro** en la consola, porque el nivel
por defecto es `INFO`. *Corrección:* usar `WARNING` por defecto.

**U4. La consulta está programada dos veces**, en `TripPlanner.plan` y en `Menu.run`
([decisión 8](#8-por-qué-una-ventana-swing-que-se-abre-antes-de-cargar-el-grafo)).
*Corrección:* que `Menu` también llame a `TripPlanner.plan` y solo imprima el
`PlanResult`.

---

## 9. Estado actual y pendientes

**Repositorio (revisado el 3 de noviembre de 2026 en `C:\Users\marti\MetroBus`):** la rama
`main` **no tiene ningún commit**; hay 170 archivos preparados con `git add` y no hay un
remoto configurado, así que todavía no hay nada en `github.com/mquijanoh09/MetroBus`. El
`.gitignore` excluye `target/`, `*.class`, `.idea/` e `*.iml`.

| Prioridad | Pendiente | Por qué importa |
|---|---|---|
| 1 | Sumar la caminata de acceso y de salida ([R1](#enrutamiento), [R2](#enrutamiento)) | Explica el desfase de 4 a 13 minutos en los 15 casos |
| 2 | Quitar la ruta fija de Windows del código y de las pruebas ([D1](#datos-y-geocodificación)) | 21 pruebas fallan o no arrancan fuera del equipo del autor |
| 3 | Reescribir `FareCalculator` según `fares.md` ([F1 a F4](#tarifas)) | 7 de 15 tarifas son incorrectas |
| 4 | Permitir la respuesta "camine" ([R3](#enrutamiento)) | El caso 4 cobra 3 800 por una cuadra |
| 5 | Llevar `mvn test` a 0 fallos y 0 errores | Hoy termina en `BUILD FAILURE` |
| 6 | Mostrar el código público del bus ([R4](#enrutamiento)) | `Abordar 90081` no le dice nada al pasajero |
| 7 | Leer de la configuración las esperas y velocidades ([G1](#grafo)) | Hoy cambiar `config.properties` no cambia nada |
| 8 | Proteger el fin de la entrada ([U1](#interfaz)) | Evita un volcado de pila |
| 9 | Sacar de la raíz los `TASK_*.md`, `KIRO_*.md`, `main-error*.txt` y `verify_fix.class` | Son notas de desarrollo, no parte de la entrega |
| 10 | Hacer el primer commit, crear el remoto y publicar | El profesor debe poder clonar |

---

*Datos: Alcaldía de Medellín, servicio ArcGIS REST `VM_Movilidad`, descargados el 26 de
octubre de 2026. Tarifas: Acuerdo Metropolitano 02 de 2026. Los tiempos de viaje y de
espera son estimaciones, no horarios oficiales.*

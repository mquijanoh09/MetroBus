# DESIGN.md · Frontend de MetroBus

Este documento define cómo se ve y cómo se comporta la ventana de MetroBus. Sirve para que cada pantalla, color y texto salga de una decisión y no de un valor por defecto. Lo que no esté aquí no se inventa: primero se agrega a este documento.

**Alcance:** la interfaz gráfica en Swing (`co.medellin.transit.ui.gui`). El backend (ruteo, tarifas, grafo) no cambia por este documento.

---

## 0. Los seis pilares y cómo se cumplen aquí

MetroBus es una aplicación de escritorio en Java Swing. Así el profesor la clona y la ejecuta sin instalar nada. Cada pilar se aplica con las herramientas de Swing:

| Pilar | Cómo se cumple en MetroBus | Sección |
|---|---|---|
| **Intuitivo y centrado en el usuario** | Una sola pantalla con una sola tarea: origen, destino, buscar. Todo estado tiene una respuesta visible: "Cargando la red…", "Buscando…", el resultado o un error que dice qué hacer | 5, 5.4 |
| **Responsivo, primero lo compacto** | El diseño parte del ancho más angosto (una columna, como en un teléfono) y escala a dos columnas en pantallas anchas. La ventana se adapta a su tamaño en vivo | 4.1 |
| **Rendimiento** | Ninguna imagen: todo se dibuja con vectores. El grafo, el catálogo de rutas y el mapa se cargan de forma diferida y fuera del hilo de la interfaz. Hay presupuestos de tiempo medibles | 9 |
| **Accesibilidad** | Contraste medido (texto ≥ 4,5:1, gráficos ≥ 3:1), navegación completa con teclado, foco visible, nombres accesibles en todos los controles y soporte para lectores de pantalla vía Java Access Bridge | 2, 6, 10 |
| **Consistencia visual** | Un sistema de diseño en código: `Theme` define colores, tipos y espacios, y los componentes base solo se crean desde ahí | 2, 3, 8 |
| **Arquitectura modular** | Componentes Swing independientes que se dibujan a partir de un único estado de la app (`AppState`), con flujo de datos en un solo sentido. Es el mismo patrón que React, pero en Java | 8 |

**Límite honesto:** una app Swing no corre en un teléfono. "Mobile-first" significa aquí que el diseño compacto es el de referencia y que la ventana funciona bien desde 400 px de ancho.

---

## 1. Concepto: señalética de estación

MetroBus se diseña como la señalización de un sistema de transporte, no como una aplicación web genérica. La idea central es **el mapa es la portada**. Al abrir la app, lo primero que se ve es el esquema real de la red de Medellín, dibujado con las coordenadas de `data/transit_lines.csv`, y encima el formulario para planear el viaje.

Tres principios guían cada decisión:

1. **Datos reales a la vista.** Se muestran nombres de estaciones, códigos de línea y números de ruta del dataset. No se usan textos de relleno, iconos decorativos ni estadísticas de adorno.
2. **Un color significa un modo de transporte.** El color se reserva para identificar Metro, Metroplús, cable, tranvía, bus y caminata. La interfaz en sí es tinta sobre papel.
3. **Los números mandan.** La duración, la tarifa y la distancia son la respuesta que la persona busca. Por eso van en tipografía tabular y con el mayor tamaño de la pantalla.

### 1.1 Lo que evitamos (señales de interfaz genérica)

| No hacemos | Por qué | En su lugar |
|---|---|---|
| Barra superior con degradado | Es la firma de una plantilla | Barra plana de papel con una línea de tinta de 1 px |
| Tarjeta centrada con "Bienvenido" | No dice nada y retrasa la tarea | El formulario es lo primero, a la izquierda, con el mapa al lado |
| Fichas pastel con datos ("252 rutas") | Relleno que parece dashboard | Las líneas reales en el mapa, con su código |
| Esquinas redondeadas en todo | Look de kit de componentes | Radio de 2 px solo en campos y botones. Insignias y tramos son rectos |
| Sombras y tarjetas flotantes | Profundidad falsa | Separación con espacio y líneas de 1 px |
| Emoji o iconos de librería | Inconsistentes entre sistemas | Glifos dibujados con `Graphics2D` (punto, línea, insignia) |
| Verde y amarillo como marca | Ya se usó en la versión 1 y es lo esperable | Tinta como color de marca. El color queda para los modos |
| Mezclar "usted" y "tú" | Suena a texto generado | Siempre **tú**, frases cortas y concretas |

---

## 2. Color

Todos los colores viven en una sola clase (`Theme`). No se escribe un `new Color(...)` fuera de ella.

### 2.1 Base

| Token | Hex | Uso | Contraste medido |
|---|---|---|---|
| `PAPER` | `#F6F5F1` | Fondo de la ventana | — |
| `SURFACE` | `#FFFFFF` | Campos de texto y panel de resultado | — |
| `INK` | `#16191D` | Texto principal, botón principal, marca | 16,2:1 sobre papel |
| `INK_2` | `#4A5058` | Texto secundario y etiquetas | 7,5:1 sobre papel |
| `RULE` | `#D8D6CF` | Líneas divisorias y bordes de campos | Decorativo |
| `ERROR` | `#B42318` | Mensajes de error | 6,0:1 sobre papel |

### 2.2 Modos de transporte

Son colores **propios de MetroBus**. No pretenden copiar los colores oficiales del Metro de Medellín.

| Modo | Líneas en los datos | Relleno de insignia | Texto en insignia | Trazo en el mapa |
|---|---|---|---|---|
| Metro | A, B | `#1F4FA3` | blanco, 7,8:1 | `#1F4FA3`, 7,1:1 sobre papel |
| Metroplús | 1, 2 | `#1B7A4B` | blanco, 5,3:1 | `#1B7A4B`, 4,9:1 |
| Metrocable | H, J, K, L, M, P | `#B8480F` | blanco, 5,3:1 | `#B8480F`, 4,9:1 |
| Tranvía | T-A | `#9B1B47` | blanco, 8,0:1 | `#9B1B47`, 7,3:1 |
| Bus | 252 rutas (`id_ruta` de 5 dígitos) | `#E8B000` | tinta, 8,9:1 | `#9C6C00`, 4,2:1 |
| Caminar | — | sin insignia | — | `#6B7077` punteado, 4,6:1 |

Para el bus se usan dos tonos. El ámbar claro solo alcanza 1,8:1 sobre papel, así que como línea se usa el tono oscuro. Todos los trazos cumplen el mínimo de 3:1 para elementos gráficos, y todo el texto cumple 4,5:1.

Cómo saber el modo de un tramo: `Step.getRouteInfo()` trae `A`/`B` (Metro), `1`/`2` (Metroplús), `H J K L M P` (cable), `T-A` (tranvía) o un `id_ruta` de 5 dígitos (bus). Lo verifiqué contra los datos: ningún `id_ruta` de bus coincide con un código de línea.

### 2.3 Foco

El foco del teclado es un anillo de 2 px en `INK` con 2 px de separación. El ámbar no sirve como anillo de foco porque no llega a 3:1 sobre papel.

---

## 3. Tipografía

**Familia:** IBM Plex Sans para la interfaz e IBM Plex Mono para cifras (minutos, pesos, metros y horas). Las dos tienen licencia OFL 1.1. Los archivos `.ttf` van dentro de `src/main/resources/fonts/` junto con el archivo de licencia, y se registran con `Font.createFont` al arrancar. Así se ven igual en el computador del profesor sin instalar nada. Si la carga falla, se usa `Font.SANS_SERIF` y `Font.MONOSPACED`.

Pesos que se incluyen: Plex Sans Regular, Medium y SemiBold, y Plex Mono Medium. No se usa negrita completa.

| Rol | Fuente | Tamaño / peso | Ejemplo |
|---|---|---|---|
| Cifra principal | Plex Mono | 40 / Medium | `22 min` |
| Cifras secundarias | Plex Mono | 18 / Medium | `$3.820` · `349 m` |
| Título de sección | Plex Sans | 20 / SemiBold | `¿A dónde vas?` |
| Texto de tramo | Plex Sans | 15 / Regular | `De Poblado a Caribe` |
| Etiqueta | Plex Sans | 12 / Medium, MAYÚSCULAS, espaciado +0,06 em | `ORIGEN` |
| Texto auxiliar | Plex Sans | 13 / Regular, `INK_2` | `Ej.: CL 10 43A-20` |
| Insignia de línea | Plex Sans | 13 / SemiBold | `A`, `T-A`, `263` |

Reglas: el texto va alineado a la izquierda, nunca centrado, salvo dentro de las insignias. Como máximo hay tres tamaños por pantalla además de la cifra principal.

---

## 4. Espacio y retícula

- La unidad base es de **8 px**. Todos los márgenes y separaciones son múltiplos de 4 u 8.
- La ventana abre en **1120 × 720** y su mínimo es **400 × 560**. Respeta la escala de pantalla de Windows, porque Java la aplica sola. Se dibuja en coordenadas lógicas.

### 4.1 Diseño adaptable (primero lo compacto)

Se diseña primero el ancho **compacto** y los demás se derivan de él. Un `ComponentListener` de la ventana elige el diseño según el ancho del contenido, y se cambia en vivo al redimensionar, sin reiniciar nada.

| Ancho | Diseño | Panel de viaje | Mapa | Márgenes |
|---|---|---|---|---|
| **Compacto** < 760 px (referencia) | Una columna | Ancho completo | Franja de 180 px arriba del formulario. Con un resultado se puede expandir a 50 % del alto con el botón "Ver mapa" | 16 px |
| **Medio** 760–1099 px | Dos columnas | 360 px fijos | El resto | 24 px |
| **Amplio** ≥ 1100 px | Dos columnas | 400 px fijos | El resto | 32 px |

Reglas comunes:
- El texto se ajusta en varias líneas y nunca se corta con "…", salvo los rótulos del mapa.
- El panel de viaje tiene su propio scroll vertical. El mapa nunca tiene scroll: se reencuadra.
- Los objetivos táctiles y de clic miden al menos 40 × 40 px en los tres diseños.
- Hay 24 px entre bloques dentro del panel (16 px en compacto).

---

## 5. Pantallas

Hay una sola ventana con un solo diseño, que cambia de estado. La pantalla de inicio **es** el planificador, para que el usuario empiece a escribir sin pasar por una portada.

### 5.1 Inicio (estado vacío)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ ▮ MetroBus                                     Medellín · Valle de Aburrá    │
├──────────────────────────────────────────────────────────────────────────────┤
│                                  │                                           │
│  ¿A dónde vas?                   │          Niquía ●                         │
│                                  │                  \                        │
│  ORIGEN                          │                   ● (Metro A)             │
│  ● [ CL 10 43A-20            ]   │     San Javier ●──●──● San Antonio        │
│  │                       [⇅]     │      (J) /       │    \___ T-A ● Oriente │
│  ● [ CL 77 65-30             ]   │   La Aurora ●      │                      │
│  DESTINO                         │                    ●                      │
│                                  │                     \                     │
│  [ Buscar ruta  ↵ ]              │              La Estrella ●                │
│                                  │                                           │
│  Prueba con:                     │   ── Metro  ── Metroplús  ── Cable        │
│  CL 50 53-48 → CR 70 x CL 48     │   ── Tranvía  ·· Caminar                  │
│  CR 80 32-15 → CL 51 46-10       │                                           │
└──────────────────────────────────────────────────────────────────────────────┘
```

- **Barra superior:** 48 px de alto, fondo `PAPER` y una línea `INK` de 1 px abajo. A la izquierda va la marca: un rectángulo de tinta de 8 × 20 px seguido de "MetroBus" en Plex Sans SemiBold 18. A la derecha, "Medellín · Valle de Aburrá" en `INK_2`.
- **Formulario:** origen y destino van unidos por un riel vertical con dos puntos, como una línea de metro. El botón ⇅ intercambia los valores. Los ejemplos son enlaces de texto, no un combo, y al pulsar uno se llenan los campos y se busca.
- **Mapa:** muestra todas las líneas de `transit_lines.csv` con su color y un grosor de 4 px. Las estaciones son puntos blancos con borde del color de la línea. Solo se rotulan las estaciones terminales de cada línea, con nombres reales: Niquía, La Estrella, San Javier, Oriente, Arví, etc. Los rótulos no se solapan, y si dos chocan se omite el de la línea con menos estaciones. Los buses no se dibujan en reposo porque son 252 rutas y saturarían el mapa.
- **Proyección:** equirectangular, con la longitud multiplicada por `cos(6,25°)` para que el valle no se vea estirado. El encuadre usa los límites del dataset con un 6 % de margen.

### 5.2 Cargando

El mapa se dibuja de inmediato, porque `transit_lines.csv` pesa 5 KB y se lee antes que el grafo. Los campos están habilitados para escribir, pero el botón dice **"Cargando la red…"** y está deshabilitado. Si la persona presiona Enter durante la carga, la búsqueda queda en cola y corre en cuanto el grafo esté listo. No se usan barras de progreso indeterminadas.

### 5.3 Resultado

```
│  CL 10 43A-20 → CL 77 65-30        [Editar]  │                               │
│                                              │   (mapa: la ruta del viaje    │
│  22 min                                      │    resaltada; el resto de la  │
│  $3.820 · 349 m a pie · sin transbordos      │    red al 20 % de opacidad)   │
│  ──────────────────────────────────────────  │                               │
│  ·· Camina a Poblado                  4 min  │                               │
│  [A] De Poblado a Caribe             13 min  │                               │
│      Metro línea A                           │                               │
│  ·· Camina a CR 64A x CL 78           3 min  │                               │
│                                              │                               │
│  Tarifa con tarjeta Cívica.                  │                               │
```

- **Encabezado:** origen → destino en una línea, con un enlace "Editar" que vuelve al formulario sin borrar los campos.
- **Cifra principal:** la duración total en Plex Mono 40. Debajo, en una sola línea separada por puntos medios: tarifa, distancia a pie y transbordos (número de tramos en vehículo menos uno). Cuando `FareResult.isIntegrationApplied()` es verdadero, se agrega "· integración aplicada".
- **Tramos, no pasos:** los pasos `BOARD`, `RIDE` y `ALIGHT` consecutivos de una misma ruta se agrupan en **un tramo**: "De {parada de abordaje} a {parada de bajada}", con la insignia de la línea y la duración del `RIDE`. Los pasos `WALK` son tramos de caminata con una línea punteada a la izquierda y sin insignia.
- **Insignia:** un rectángulo recto de 28 × 22 px con el código: `A`, `1`, `K`, `T-A` o, para el bus, el `codigo` público de la ruta (por ejemplo `263`). En una segunda línea, en `INK_2`, va el nombre del modo y, para el bus, el `nombre` de la ruta (por ejemplo `12 de Octubre - Calle 90`). Ambos datos salen de `data/raw/routes.json`. Hoy el paso trae el `id_ruta` interno (`90111`), así que la interfaz lo traduce con un catálogo propio de la capa `ui`, sin tocar el backend.
- **Mapa:** se dibujan los tramos del viaje con su color a 6 px. Los puntos de origen y destino son círculos de tinta de 10 px y llevan rótulo. El resto de la red baja al 20 % de opacidad. El encuadre se ajusta al viaje con un 15 % de margen.

### 5.4 Estados de error

Los errores se muestran donde iría el resultado, en texto `ERROR` con una línea de 2 px a la izquierda. Nunca se usa un diálogo emergente, salvo el error fatal de arranque. El texto dice qué pasó y qué hacer:

| `PlanResult.Status` | Mensaje |
|---|---|
| `EMPTY_INPUT` | Escribe el origen y el destino. |
| `INVALID_FORMAT` | No reconocemos "{texto}" como dirección. Prueba con algo como CL 10 43A-20 o CR 70 x CL 48. |
| `NOT_FOUND` / `OUT_OF_BOUNDS` | "{texto}" queda fuera del área que cubre MetroBus. |
| `TOO_CLOSE` | Origen y destino están a menos de 50 m. Puedes ir caminando. |
| `NO_ROUTE` | No encontramos una ruta entre estas direcciones. Prueba con un punto cercano a una estación o a una vía principal. |
| `ERROR` | Algo falló al calcular la ruta. Intenta de nuevo. |

El campo que causó el error recibe el foco y su borde pasa a `ERROR`.

---

## 6. Interacción

| Acción | Teclado | Ratón |
|---|---|---|
| Pasar de origen a destino | Enter o Tab | Clic |
| Buscar | Enter en destino | Botón "Buscar ruta" |
| Intercambiar origen y destino | Alt + I | Botón ⇅ |
| Volver a editar | Esc | "Editar" |
| Salir | Alt + F4 | Cerrar ventana |

- La búsqueda corre fuera del hilo de Swing (`SwingWorker`). Mientras dura, el botón dice "Buscando…" y queda deshabilitado. La ventana nunca se congela.
- **Movimiento:** solo una transición. Al mostrar un resultado, el mapa se mueve del encuadre de la red al del viaje en 250 ms, con aceleración de salida y un `javax.swing.Timer` a 60 fps. Fuera de eso no hay animaciones.
- **Cursores:** mano sobre enlaces y botones, y texto sobre campos.

---

## 7. Voz y textos

- Siempre se trata de **tú**: "Escribe", "Prueba", "Camina".
- Los verbos van en imperativo para las instrucciones: "Camina a Poblado", no "Caminar a Poblado".
- Los números siguen el formato colombiano: `$3.820`, `1,5 km`, `1 h 05 min`. Los minutos se redondean hacia arriba, igual que en la consola.
- Se escriben los nombres propios de los datos tal como vienen. La única excepción es el sufijo ", Medellin" que el backend agrega a las direcciones: se quita porque repite la ciudad.
- No se usan signos de exclamación ni frases de cortesía vacías como "¡Gracias por usar MetroBus!".

---

## 8. Implementación en Swing

- **Sin dependencias nuevas.** Todo sale del JDK: Swing, Java2D y `Font.createFont`. El profesor clona y ejecuta como hoy.
- **Look and feel:** el multiplataforma (Metal), con `UIManager` ajustado desde `Theme`. Los botones, campos e insignias se pintan a mano para que no dependan del sistema operativo.
- **Antialiasing:** en todo `paintComponent` se usa `KEY_ANTIALIASING = ON` y `KEY_TEXT_ANTIALIASING = LCD_HRGB` en Windows.
- **Clases previstas en `ui.gui`:**
  - `Theme`: colores, fuentes y espaciado.
  - `Fonts`: carga de Plex.
  - `NetworkMap`: el mapa y su proyección.
  - `TripForm` y `TripResult`: formulario y resultado (ver 8.1).
  - `LegList`: agrupa los pasos en tramos.
  - `LineBadge`: la insignia de línea.
  - `RouteCatalog`: traduce `id_ruta` → `codigo` + `nombre` desde `routes.json`.
  - `MainWindow`: se conserva su API pública (`open`, `showReady`, `showFatalError`).
- `TripPlanner` y `PlanResult` siguen siendo la lógica sin vista. `PlanResult` solo gana `getField()`, que dice qué campo causó el error, para poder marcarlo.
- **Lo que no se toca:** `Router`, `GraphBuilder`, `FareCalculator` y `StepBuilder`.

### 8.1 Arquitectura: estado único y flujo en un solo sentido

Es el mismo modelo mental que React o Vue, implementado con clases de Java.

```
  acción del usuario ──► AppController ──► AppState (inmutable) ──► componentes se redibujan
         ▲                    │
         │                    └─► TripPlanner en SwingWorker (fuera del hilo de la interfaz)
         └──────────── los componentes solo emiten acciones, nunca cambian el estado ellos mismos
```

- **`AppState`** es un `record` inmutable con `phase` (`LOADING`, `IDLE`, `SEARCHING`, `RESULT`, `ERROR`), `originText`, `destinationText`, `result` (`PlanResult`), `network` (líneas para el mapa) y `layout` (`COMPACT`, `MEDIUM`, `WIDE`). Cada cambio crea un estado nuevo con métodos `with…`.
- **`AppStore`** guarda el estado actual y avisa a los suscriptores en el hilo de Swing. Es la única fuente de verdad.
- **`AppController`** recibe acciones (`search`, `swap`, `edit`, `pickExample`, `resize`) y produce el estado siguiente. No contiene nada de Swing, así que se prueba sin ventana.
- **Componentes de presentación** (`TripForm`, `TripResult`, `LegList`, `LineBadge`, `NetworkMap`, `TopBar`): cada uno recibe el estado con un método `render(AppState)` y dibuja. Ninguno conoce a los demás.
- **Componentes base reutilizables** (`ui.gui.kit`): `InkButton`, `TextLink`, `AddressField`, `Rule` y `Label`. Toman todo de `Theme`.
- **Paquetes:**
  - `ui.gui.state`: estado, store y controlador.
  - `ui.gui.kit`: componentes base.
  - `ui.gui.view`: componentes de pantalla.
  - `ui.gui`: `MainWindow`, que conecta todo.

## 9. Rendimiento

| Medida | Presupuesto | Cómo se logra |
|---|---|---|
| De ejecutar el jar a ver el formulario y el mapa | < 500 ms | La ventana se crea antes de cargar el grafo. El mapa usa solo `transit_lines.csv` (5 KB) |
| De ver la ventana a poder buscar | < 2 s | El grafo carga en un hilo aparte mientras ya se puede escribir |
| De pulsar "Buscar" al resultado | < 300 ms | El cálculo corre en `SwingWorker`. Sin red ni disco en esa ruta |
| Redibujar el mapa al redimensionar | < 16 ms por cuadro | Las líneas proyectadas se guardan en caché por tamaño y solo se reproyectan al cambiar el tamaño |
| Imágenes en el jar | 0 | Todo es vectorial (`Graphics2D`) |
| Fuentes en el jar | ≤ 1 MB | Solo 4 archivos de Plex. Si se pasa, se quita Plex Sans Medium |

**Carga diferida:**
- `RouteCatalog` lee `routes.json` la primera vez que un resultado incluye un bus, no al arrancar.
- Los rótulos del mapa se calculan una sola vez por tamaño de ventana.
- `MainWindow` registra en el log los tiempos reales de cada etapa para verificar los presupuestos.

## 10. Accesibilidad

- **Contraste:** según la tabla de la sección 2. Texto ≥ 4,5:1 y elementos gráficos ≥ 3:1.
- **El color nunca es la única señal:** cada tramo lleva su insignia con el código de la línea y el nombre del modo escrito. La caminata se distingue por la línea punteada, además del gris.
- **Teclado:** todo se hace sin ratón (sección 6). El orden de tabulación sigue el orden visual: origen, intercambiar, destino, buscar, ejemplos, resultado. El foco siempre es visible.
- **Lectores de pantalla:** cada control tiene `getAccessibleContext().setAccessibleName(...)` y una descripción. El mapa es decorativo para el lector: su descripción accesible resume el viaje en texto ("Ruta en Metro línea A de Poblado a Caribe"). Al terminar una búsqueda, el resumen ("22 minutos, 3.820 pesos, sin transbordos") se anuncia en una región que el lector lee. En Windows, el JDK trae Java Access Bridge para NVDA y JAWS. Viene desactivado y se activa una vez con `jabswitch -enable`; el README lo explicará.
- **Texto alternativo:** como no hay imágenes de mapa de bits, el equivalente es el nombre y la descripción accesibles de cada componente dibujado (insignias, mapa, riel del formulario).
- **Tamaño de texto:** nada por debajo de 12 px. Si Windows está al 150 %, todo escala con la ventana.

---

## 11. Criterios de aceptación

La versión cumple este documento cuando:

1. Al ejecutar `java -jar target/transit-routing-1.0.0.jar` se ve el formulario y el mapa real en menos de 1 segundo, antes de que termine de cargar el grafo.
2. No hay degradados, sombras, emoji ni texto centrado fuera de las insignias. Se revisa con una captura de cada estado.
3. Cada color usado en pantalla está en la tabla de la sección 2. Una prueba lo verifica sobre `Theme`.
4. Los cuatro ejemplos muestran su resultado con tramos agrupados e insignias correctas. Las rutas de bus muestran su `codigo` público y no el `id_ruta`.
5. Cada `PlanResult.Status` muestra su mensaje de la sección 5.4. Hay una prueba por estado.
6. Todo se puede hacer solo con teclado, siguiendo la sección 6.
7. Se ve correcto al 100 % y al 150 % de escala en Windows y con la ventana en su tamaño mínimo.
8. Las 24 pruebas actuales del frontend siguen pasando. Las que cambien por el nuevo texto se actualizan para comprobar el texto nuevo, nunca para comprobar menos.
9. Al redimensionar en vivo de 400 a 1400 px de ancho, el diseño pasa por compacto, medio y amplio sin cortes ni solapes. Se revisa con capturas a 400, 900 y 1300 px.
10. `AppController` tiene pruebas sin ventana para cada transición de estado: cargando → listo, buscar → resultado, buscar → error, editar e intercambiar.
11. Los tiempos registrados en el log cumplen los presupuestos de la sección 9 con los datos reales.
12. Cada control enfocable tiene nombre accesible. Una prueba recorre el árbol de componentes y falla si alguno no lo tiene.

---

## 12. Pendiente fuera del frontend

- `StepBuilder` arma las descripciones en infinitivo ("Caminar a…"). La interfaz construye su propio texto a partir del tipo de paso y su ubicación, así que no depende de esa redacción.
- Si el backend cambia los códigos de `getRouteInfo()`, hay que actualizar la tabla de la sección 2.2 y `RouteCatalog`.

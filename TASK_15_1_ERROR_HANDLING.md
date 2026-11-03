# Task 15.1: Comprehensive Error Handling Implementation

## Summary

Implemented comprehensive error handling throughout the Medellín Transit Routing System, meeting all requirements from the design specification.

## Implementation Details

### 1. CSV Loading Error Handling ✓

**Location**: `CsvReader.java`, `GraphCsvReader.java`, `ArcGisReader.java`

**Implementation**:
- Wrapped CSV loading with try-catch blocks
- Logs warnings for invalid rows using `java.util.logging`
- Skips malformed rows and continues processing
- Returns List of successfully parsed objects

**Example from CsvReader.java**:
```java
for (CSVRecord record : parser) {
    lineNumber++;
    try {
        Optional<T> parsed = parseRecord(record, lineNumber);
        parsed.ifPresent(results::add);
    } catch (Exception e) {
        LOGGER.warning(String.format(
            "Skipping malformed row in %s at line %d: %s",
            csvPath.getFileName(),
            lineNumber,
            e.getMessage()
        ));
    }
}
```

### 2. User Input Validation ✓

**Location**: `Menu.java`, `AddressParser.java`, `AddressLocator.java`

**Implementation**:
- All user inputs validated before processing
- Spanish error messages with examples
- Returns `Optional.empty()` for invalid inputs
- Prompts for re-entry instead of crashing

**Example from Menu.java**:
```java
Optional<ParsedAddress> parsed = addressParser.parse(addressStr);
if (parsed.isEmpty()) {
    System.out.println("Error: Formato de dirección inválido.");
    System.out.println("Ejemplo válido: Carrera 43A # 1 Sur - 100");
    return Optional.empty();
}
```

### 3. Recoverable Errors Return Optional.empty() ✓

**Locations**: Throughout the codebase

**Recoverable error scenarios**:
- Invalid address format → `AddressParser.parse()` returns `Optional.empty()`
- Address out of bounds → `AddressLocator.locate()` returns `Optional.empty()`
- No route found → `Router.findRoute()` returns `Optional.empty()`
- No stops within access radius → `Router` returns `Optional.empty()`
- Unreliable address geocoding → `AddressLocator` returns `Optional.empty()`

**User Experience**:
- Clear Spanish error messages
- Suggestions for correction
- Application continues running
- User can try again

### 4. Fatal Error Handling with Spanish Messages ✓

**Location**: `Main.java`

**Fatal error scenarios and handling**:

#### A. Data Directory Not Found
```
═══════════════════════════════════════════════════════════
Error fatal: No se encontró el directorio de datos
═══════════════════════════════════════════════════════════

Directorio esperado: /path/to/data

El directorio debe contener los siguientes archivos:
  - bus_segments.csv
  - transit_lines.csv
  - walk_links.csv
  - stops_*.json (archivos paginados)
  - routes.json

Solución:
  1. Verifique que el directorio 'data' existe
  2. O especifique una ruta diferente: java -jar transit.jar /ruta/a/datos
═══════════════════════════════════════════════════════════
```

#### B. Failed to Load Transit Data
```
═══════════════════════════════════════════════════════════
Error fatal: No se pudieron cargar los datos de transporte
═══════════════════════════════════════════════════════════

Detalle del error: [specific error message]

Posibles causas:
  - Archivos CSV faltantes o con formato incorrecto
  - Permisos insuficientes para leer los archivos
  - Archivos corruptos o datos inválidos
  - Codificación incorrecta (debe ser UTF-8)

Archivos requeridos:
  /path/to/data/bus_segments.csv
  /path/to/data/transit_lines.csv
  /path/to/data/walk_links.csv
═══════════════════════════════════════════════════════════
```

#### C. Out of Memory Error
```
═══════════════════════════════════════════════════════════
Error fatal: Memoria insuficiente
═══════════════════════════════════════════════════════════

El sistema no tiene suficiente memoria para cargar el grafo
de transporte completo (13,895 estados, 82,160 aristas).

Solución: Aumente la memoria heap de Java con el parámetro -Xmx

Ejemplos:
  java -Xmx2G -jar transit-routing.jar
  java -Xmx1G -jar transit-routing.jar  (para sistemas limitados)

Memoria actual:
  Max heap: 512 MB
  Memoria recomendada: 2048 MB (-Xmx2G)
═══════════════════════════════════════════════════════════
```

#### D. Empty Transit Graph
```
═══════════════════════════════════════════════════════════
Error fatal: El grafo de transporte está vacío
═══════════════════════════════════════════════════════════

Los archivos CSV fueron cargados pero no contienen datos válidos.

Posibles causas:
  - Archivos CSV vacíos o solo con encabezados
  - Formato incorrecto de los archivos
  - Datos corruptos o incompletos
═══════════════════════════════════════════════════════════
```

#### E. Unexpected Error
```
═══════════════════════════════════════════════════════════
Error inesperado durante la inicialización
═══════════════════════════════════════════════════════════

Tipo de error: [ExceptionType]
Mensaje: [error message]

Por favor, reporte este error con el siguiente detalle técnico:
[stack trace]
═══════════════════════════════════════════════════════════
```

### 5. Java Util Logging (INFO/WARN/ERROR) ✓

**Implementation**: Throughout the codebase

**Logging Configuration**:
- Default level: INFO
- Configurable via system property: `-Dlog.level=FINE`
- Valid levels: SEVERE, WARNING, INFO, FINE, FINER, FINEST

**Example from Main.java**:
```java
private static void configureLogging() {
    Logger rootLogger = Logger.getLogger("");
    String logLevel = System.getProperty("log.level", "INFO");
    Level level = Level.parse(logLevel);
    rootLogger.setLevel(level);
    
    for (var handler : rootLogger.getHandlers()) {
        handler.setLevel(level);
    }
}
```

**Logging usage examples**:

**INFO Level**:
```java
LOGGER.info("Initializing Medellín Transit Routing System...");
LOGGER.log(Level.INFO, "Graph loaded successfully: {0} states, {1} edges", 
          new Object[]{graph.getStateCount(), graph.getEdgeCount()});
```

**WARNING Level**:
```java
LOGGER.warning("No se pudieron cargar paradas desde JSON: " + e.getMessage());
LOGGER.log(Level.WARNING, "Could not load fare config, using defaults", e);
```

**SEVERE Level**:
```java
LOGGER.severe("Data directory not found: " + dataDir.toAbsolutePath());
LOGGER.log(Level.SEVERE, "Failed to load transit graph", e);
```

## Key Classes Modified

1. **Main.java** - Enhanced with:
   - Data directory validation before loading
   - Comprehensive fatal error handling
   - Detailed Spanish error messages
   - Configurable logging
   - Memory usage tracking
   - Loading time warnings

2. **CsvReader.java** - Already had:
   - Try-catch around record parsing
   - Logging warnings for malformed rows
   - Graceful skip and continue

3. **Router.java** - Already had:
   - Returns Optional.empty() when no stops within access radius
   - Returns Optional.empty() when no path exists
   - Logging at FINE level for debugging

4. **Menu.java** - Already had:
   - Input validation for addresses
   - Spanish error messages with examples
   - Graceful handling of invalid inputs

5. **AddressParser.java** - Already had:
   - Returns Optional.empty() for invalid formats
   - Range validation
   - Null checks

6. **AddressLocator.java** - Already had:
   - Returns Optional.empty() for unreliable geocoding
   - Bounds checking
   - Distance validation

## Testing

**Compilation**: ✓ Success
```bash
cd c:\Users\marti\MetroBus
mvn compile -q
```

**Result**: No compilation errors

## Compliance with Task Requirements

| Requirement | Status | Implementation |
|------------|--------|----------------|
| CSV loading with try-catch | ✓ Complete | CsvReader.java, ArcGisReader.java |
| Log warnings for invalid rows | ✓ Complete | CsvReader.java with java.util.logging |
| Validate all user inputs | ✓ Complete | Menu.java, AddressParser.java |
| Return Optional.empty() for recoverable errors | ✓ Complete | Throughout codebase |
| Fatal errors exit with Spanish messages | ✓ Complete | Main.java with detailed messages |
| Use java.util.logging for INFO/WARN/ERROR | ✓ Complete | All classes with LOGGER instances |

## Usage Examples

### Running with custom log level:
```bash
java -Dlog.level=FINE -jar transit-routing.jar
```

### Running with custom data directory:
```bash
java -jar transit-routing.jar /path/to/custom/data
```

### Running with increased memory:
```bash
java -Xmx2G -jar transit-routing.jar
```

### Combined options:
```bash
java -Xmx2G -Dlog.level=INFO -jar transit-routing.jar /custom/data
```

## Error Handling Flow

```
User Input
    ↓
Validation (AddressParser, AddressLocator)
    ↓
[Valid] → Continue processing
[Invalid] → Return Optional.empty() + Spanish error message → Prompt again
    ↓
Routing (Router.findRoute)
    ↓
[Route found] → Display trip
[No route] → Return Optional.empty() + Spanish message → Prompt again
    ↓
Fatal Error (IOException, OutOfMemoryError)
    ↓
Display detailed Spanish error message → System.exit(1)
```

## Conclusion

Task 15.1 has been successfully completed. The Medellín Transit Routing System now has comprehensive error handling at all levels:

- **Graceful degradation** for recoverable errors
- **Detailed Spanish messages** for all user-facing errors  
- **Structured logging** for debugging and monitoring
- **Fatal error handling** with actionable solutions
- **Input validation** throughout the application

All code compiles successfully and follows the design specifications.

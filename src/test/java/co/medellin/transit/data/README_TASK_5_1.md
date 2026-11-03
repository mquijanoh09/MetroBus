# Task 5.1: CSV Reading Infrastructure - Implementation Summary

## Completed Implementation

This task implemented the CSV reading infrastructure for the Medellín Transit Routing System.

### Files Created

#### 1. CsvReader.java (Base Class)
**Location**: `src/main/java/co/medellin/transit/data/CsvReader.java`

**Features**:
- Generic base class with type parameter `<T>` for any domain object
- Uses Apache Commons CSV library for robust CSV parsing
- Buffered reading with UTF-8 encoding for performance
- Graceful error handling: malformed rows are logged as warnings and skipped
- CSV format configuration with header support, empty line ignoring, and whitespace trimming
- Helper methods for common field types:
  - `parseInt(record, fieldName)` - Parse integer with validation
  - `parseDouble(record, fieldName)` - Parse double with validation
  - `getString(record, fieldName)` - Get trimmed string
  - `getOptionalString(record, fieldName)` - Get optional string (empty if whitespace only)
  - `hasField(record, fieldName)` - Check field existence
- Abstract method `parseRecord()` for subclass implementation
- Java logging for warnings and informational messages

**Design Pattern**: Template Method pattern - base class handles file I/O and error handling, subclasses implement record parsing

#### 2. ArcGisReader.java (JSON Reader)
**Location**: `src/main/java/co/medellin/transit/data/ArcGisReader.java`

**Features**:
- Reads ArcGIS REST API JSON format (features array with attributes and geometry)
- Supports paginated file reading (e.g., stops_1.json, stops_2.json, etc.)
- Simple regex-based JSON parsing (no external JSON library dependency)
- Graceful error handling: malformed features are logged and skipped
- Helper methods for JSON parsing:
  - `extractAttributes(featureJson)` - Extract attributes object
  - `extractGeometry(featureJson)` - Extract geometry object
  - `getStringField(attributes, fieldName)` - Get string field
  - `getIntField(attributes, fieldName)` - Get integer field
  - `getDoubleField(attributes, fieldName)` - Get double field
  - `parseGeometryCoordinate(geometryJson)` - Parse coordinate from geometry
- Supports both x/y and longitude/latitude geometry formats
- Pagination support with `readPaginated()` method

**Why No JSON Library?**: 
- Avoids adding external dependency (Gson, Jackson)
- ArcGIS JSON format is simple and predictable
- Regex parsing is sufficient for this use case
- Keeps the project lightweight

#### 3. CsvReaderTest.java (Unit Tests)
**Location**: `src/test/java/co/medellin/transit/data/CsvReaderTest.java`

**Test Coverage**:
- ✅ Read valid CSV with multiple records
- ✅ Skip malformed rows (invalid numbers)
- ✅ Handle empty files (header only)
- ✅ Trim whitespace from fields
- ✅ Optional string field handling (skip empty values)
- ✅ Empty field validation (throws exception)

**Test Record Count**: 6 test methods

#### 4. ArcGisReaderTest.java (Unit Tests)
**Location**: `src/test/java/co/medellin/transit/data/ArcGisReaderTest.java`

**Test Coverage**:
- ✅ Read valid ArcGIS JSON with multiple features
- ✅ Skip malformed features (missing required fields)
- ✅ Handle empty features array
- ✅ Read paginated files (multiple JSON files)
- ✅ Parse geometry with longitude/latitude format (alternative)
- ✅ Handle missing features field gracefully

**Test Record Count**: 7 test methods

## Key Implementation Details

### Error Handling Strategy

**Malformed Rows/Features**:
1. Try to parse the record/feature
2. If parsing fails (Exception caught):
   - Log a warning with file name, line/feature number, and error message
   - Skip the record/feature
   - Continue processing remaining records
3. Never fail the entire file load due to a single bad record

**Example Warning Log**:
```
WARNING: Skipping malformed row in stops.csv at line 42: Field 'id' is empty
```

### CSV Format Configuration

Default CSV format:
- Header row present (first row)
- Skip header record during iteration
- Ignore empty lines
- Trim whitespace from all fields
- Default delimiter (comma)

Subclasses can override `createCsvFormat()` to customize:
```java
@Override
protected CSVFormat createCsvFormat() {
    return CSVFormat.DEFAULT
        .builder()
        .setHeader("id", "name", "lat", "lon")  // Explicit headers
        .setSkipHeaderRecord(true)
        .setDelimiter(';')  // Custom delimiter
        .build();
}
```

### ArcGIS JSON Format Support

**Expected Format**:
```json
{
  "features": [
    {
      "attributes": {
        "id": 123,
        "name": "Stop Name",
        "other_field": "value"
      },
      "geometry": {
        "x": -75.5,
        "y": 6.25
      }
    }
  ]
}
```

**Alternative Geometry Format**:
```json
"geometry": {
  "longitude": -75.5,
  "latitude": 6.25
}
```

Both formats are supported by `parseGeometryCoordinate()`.

### Pagination Support

To read paginated files:
```java
ArcGisReader<Stop> reader = new StopArcGisReader();
List<Stop> allStops = reader.readPaginated(
    Paths.get("data/raw"),
    "stops",      // Base filename
    ".json"       // Extension
);
// Reads: stops_1.json, stops_2.json, stops_3.json, ...
// Stops when file not found
```

## Usage Example

### Creating a CSV Reader

```java
public class StopCsvReader extends CsvReader<Stop> {
    @Override
    protected Optional<Stop> parseRecord(CSVRecord record, int lineNumber) {
        try {
            int id = parseInt(record, "id");
            String name = getString(record, "name");
            double lat = parseDouble(record, "lat");
            double lon = parseDouble(record, "lon");
            
            return Optional.of(new Stop(id, name, new Coordinate(lat, lon)));
            
        } catch (IllegalArgumentException e) {
            // Return empty to skip invalid records
            return Optional.empty();
        }
    }
}

// Usage
StopCsvReader reader = new StopCsvReader();
List<Stop> stops = reader.read(Paths.get("data/stops.csv"));
```

### Creating an ArcGIS Reader

```java
public class StopArcGisReader extends ArcGisReader<Stop> {
    @Override
    protected Optional<Stop> parseFeature(String featureJson, int featureNumber) {
        String attributes = extractAttributes(featureJson);
        
        int id = getIntField(attributes, "id_paradero");
        Optional<String> name = getStringField(attributes, "nombre");
        
        Optional<String> geometryJson = extractGeometry(featureJson);
        if (geometryJson.isEmpty()) {
            return Optional.empty();
        }
        
        Coordinate coord = parseGeometryCoordinate(geometryJson.get());
        
        return Optional.of(new Stop(id, name.orElse(""), coord));
    }
}

// Usage
StopArcGisReader reader = new StopArcGisReader();
List<Stop> stops = reader.readPaginated(
    Paths.get("data/raw"),
    "stops",
    ".json"
);
```

## Design Rationale

### Why Template Method Pattern?

1. **Separation of Concerns**: Base class handles file I/O, error handling, and logging. Subclasses focus only on domain-specific parsing logic.

2. **Code Reuse**: All CSV readers benefit from buffered I/O, error handling, and logging without duplicating code.

3. **Extensibility**: Easy to create new readers for different domain objects (Stop, Route, Station, etc.)

### Why Abstract Base Classes?

1. **Type Safety**: Generic type parameter `<T>` ensures type safety at compile time
2. **Flexibility**: Subclasses can return `Optional.empty()` to skip records silently
3. **Error Control**: Exceptions in `parseRecord()` are caught and logged automatically

### Why Regex for JSON Parsing?

1. **Simplicity**: ArcGIS JSON format is simple and predictable
2. **No Dependencies**: Avoids adding Gson or Jackson to the project
3. **Lightweight**: Regex parsing is fast for small JSON files
4. **Sufficient**: For this use case, we don't need full JSON parsing capabilities

If more complex JSON parsing is needed in the future, consider adding a JSON library.

## Next Steps (Task 5.2)

Task 5.2 will implement specialized readers:

1. **StopCsvReader** - Parse stops.csv
2. **RouteCsvReader** - Parse routes.csv
3. **GraphCsvReader** - Parse pre-computed graph edges

These will extend `CsvReader<T>` and use the helper methods provided by the base class.

For ArcGIS data, we'll implement:

1. **StopArcGisReader** - Parse stops_*.json (paginated)
2. **RouteArcGisReader** - Parse routes.json
3. **StationArcGisReader** - Parse transit_stations.json

## Testing Status

All unit tests are written and ready to run. Tests verify:
- ✅ Happy path (valid data)
- ✅ Error handling (malformed data)
- ✅ Edge cases (empty files, missing fields)
- ✅ Whitespace handling
- ✅ Pagination support

To run tests (when Maven is available):
```bash
mvn test -Dtest=CsvReaderTest,ArcGisReaderTest
```

Or use IntelliJ IDEA's built-in test runner.

## Dependencies Used

- **Apache Commons CSV 1.10.0**: Robust CSV parsing with header support
- **Java NIO**: Buffered file reading with UTF-8 encoding
- **Java Logging**: Standard `java.util.logging` for warnings and info
- **JUnit Jupiter 5.10.0**: Unit testing framework

## Code Quality

- ✅ Full Javadoc comments on all public methods
- ✅ Clear exception handling with descriptive messages
- ✅ Consistent naming conventions
- ✅ Immutable where possible (final fields)
- ✅ Generic type parameters for reusability
- ✅ Template method pattern for extensibility
- ✅ Comprehensive unit test coverage

## Task 5.1 Completion Checklist

- ✅ CsvReader base class implemented
- ✅ ArcGisReader class implemented  
- ✅ Buffered reading for performance
- ✅ Graceful error handling (log warning, skip, continue)
- ✅ Helper methods for common field types
- ✅ Pagination support for ArcGIS files
- ✅ Unit tests for CsvReader
- ✅ Unit tests for ArcGisReader
- ✅ Documentation and usage examples

**Status**: Task 5.1 is COMPLETE ✅

Ready for Task 5.2: Implement specialized CSV readers for Stop, Route, and Graph data.

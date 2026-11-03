package co.medellin.transit.data;

import co.medellin.transit.model.Route;
import org.apache.commons.csv.CSVRecord;

import java.util.Optional;

/**
 * Reads route metadata from CSV format files.
 * Parses bus routes from CSV data.
 * 
 * Expected CSV fields:
 * - id_ruta (or route_id): unique route identifier (integer)
 * - codigo: user-visible route code
 * - nombre (or nombre_ruta or name): route display name
 * - tipo (or tipo_ruta or type): route type for fares
 */
public class RouteCsvReader extends CsvReader<Route> {

    /**
     * Parses a single CSV record into a Route object.
     * 
     * @param record the CSV record to parse
     * @param lineNumber the line number (for error reporting)
     * @return parsed Route, or empty to skip
     * @throws Exception if parsing fails
     */
    @Override
    protected Optional<Route> parseRecord(CSVRecord record, int lineNumber) throws Exception {
        // Extract route ID - try multiple field names
        int id;
        if (hasField(record, "id_ruta")) {
            id = parseInt(record, "id_ruta");
        } else if (hasField(record, "route_id")) {
            id = parseInt(record, "route_id");
        } else if (hasField(record, "id")) {
            id = parseInt(record, "id");
        } else {
            throw new IllegalArgumentException("Missing route ID field (id_ruta, route_id, or id)");
        }
        
        // Extract codigo
        String codigo;
        if (hasField(record, "codigo")) {
            codigo = getString(record, "codigo");
        } else if (hasField(record, "codigo_ruta")) {
            codigo = getString(record, "codigo_ruta");
        } else {
            // Default to id as string if no codigo
            codigo = String.valueOf(id);
        }
        
        if (codigo.isEmpty()) {
            codigo = String.valueOf(id);
        }
        
        // Extract route name - try multiple field names
        String nombre;
        if (hasField(record, "nombre")) {
            nombre = getString(record, "nombre");
        } else if (hasField(record, "nombre_ruta")) {
            nombre = getString(record, "nombre_ruta");
        } else if (hasField(record, "name")) {
            nombre = getString(record, "name");
        } else {
            // Default to "Ruta {codigo}" if no name provided
            nombre = "Ruta " + codigo;
        }
        
        if (nombre.isEmpty()) {
            nombre = "Ruta " + codigo;
        }
        
        // Extract route type - try multiple field names
        String tipo = "Directa Centro";  // Default
        if (hasField(record, "tipo")) {
            tipo = getString(record, "tipo");
        } else if (hasField(record, "tipo_ruta")) {
            tipo = getString(record, "tipo_ruta");
        } else if (hasField(record, "type")) {
            tipo = getString(record, "type");
        }
        
        if (tipo.isEmpty()) {
            tipo = "Directa Centro";
        }
        
        return Optional.of(new Route(id, codigo, nombre, tipo));
    }
}

package co.medellin.transit.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Base class for reading CSV files with buffered I/O and error handling.
 * Provides helper methods for parsing common field types and gracefully handles malformed rows.
 */
public abstract class CsvReader<T> {
    
    private static final Logger LOGGER = Logger.getLogger(CsvReader.class.getName());
    
    /**
     * Reads a CSV file and parses it into domain objects.
     * Malformed rows are logged as warnings and skipped.
     *
     * @param csvPath path to the CSV file
     * @return list of successfully parsed objects
     * @throws IOException if the file cannot be read
     */
    public List<T> read(Path csvPath) throws IOException {
        List<T> results = new ArrayList<>();
        List<String> skippedRows = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, createCsvFormat())) {
            
            int lineNumber = 1; // Header is line 1, data starts at line 2
            
            for (CSVRecord record : parser) {
                lineNumber++;
                
                try {
                    Optional<T> parsed = parseRecord(record, lineNumber);
                    parsed.ifPresent(results::add);
                    
                } catch (Exception e) {
                    // Collect skipped rows for summary logging
                    if (skippedRows.size() < 5) {
                        skippedRows.add(String.format("line %d: %s", lineNumber, e.getMessage()));
                    }
                }
            }
            
            // Log summary of skipped rows
            if (!skippedRows.isEmpty()) {
                int totalSkipped = lineNumber - 1 - results.size(); // -1 for header
                LOGGER.warning(String.format(
                    "Skipped %d malformed rows in %s. First %d examples: %s",
                    totalSkipped,
                    csvPath.getFileName(),
                    skippedRows.size(),
                    skippedRows
                ));
            }
            
            LOGGER.info(String.format(
                "Successfully loaded %d records from %s",
                results.size(),
                csvPath.getFileName()
            ));
        }
        
        return results;
    }
    
    /**
     * Creates the CSV format configuration for this reader.
     * Override to customize CSV format (delimiter, headers, etc.).
     *
     * @return CSV format configuration
     */
    protected CSVFormat createCsvFormat() {
        return CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();
    }
    
    /**
     * Parses a single CSV record into a domain object.
     * Return Optional.empty() to skip this record without logging a warning.
     *
     * @param record the CSV record to parse
     * @param lineNumber the line number in the file (for error reporting)
     * @return parsed object, or empty to skip
     * @throws Exception if parsing fails (will be caught and logged)
     */
    protected abstract Optional<T> parseRecord(CSVRecord record, int lineNumber) throws Exception;
    
    /**
     * Safely parses an integer field from a CSV record.
     *
     * @param record the CSV record
     * @param fieldName the field name
     * @return parsed integer
     * @throws NumberFormatException if the field is not a valid integer
     */
    protected int parseInt(CSVRecord record, String fieldName) {
        String value = record.get(fieldName).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is empty");
        }
        return Integer.parseInt(value);
    }
    
    /**
     * Safely parses a double field from a CSV record.
     *
     * @param record the CSV record
     * @param fieldName the field name
     * @return parsed double
     * @throws NumberFormatException if the field is not a valid double
     */
    protected double parseDouble(CSVRecord record, String fieldName) {
        String value = record.get(fieldName).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' is empty");
        }
        return Double.parseDouble(value);
    }
    
    /**
     * Safely gets a string field from a CSV record, with empty string handling.
     *
     * @param record the CSV record
     * @param fieldName the field name
     * @return trimmed string value
     */
    protected String getString(CSVRecord record, String fieldName) {
        return record.get(fieldName).trim();
    }
    
    /**
     * Safely gets an optional string field from a CSV record.
     * Returns Optional.empty() if the field is empty or whitespace-only.
     *
     * @param record the CSV record
     * @param fieldName the field name
     * @return optional string value
     */
    protected Optional<String> getOptionalString(CSVRecord record, String fieldName) {
        String value = record.get(fieldName).trim();
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }
    
    /**
     * Checks if a field exists in the CSV record.
     *
     * @param record the CSV record
     * @param fieldName the field name
     * @return true if the field exists
     */
    protected boolean hasField(CSVRecord record, String fieldName) {
        return record.isMapped(fieldName);
    }
}

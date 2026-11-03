package co.medellin.transit.data;

import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CsvReader base class.
 */
class CsvReaderTest {
    
    /**
     * Simple test record class
     */
    static class TestRecord {
        final int id;
        final String name;
        final double value;
        
        TestRecord(int id, String name, double value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }
    }
    
    /**
     * Test implementation of CsvReader
     */
    static class TestCsvReader extends CsvReader<TestRecord> {
        @Override
        protected Optional<TestRecord> parseRecord(CSVRecord record, int lineNumber) {
            int id = parseInt(record, "id");
            String name = getString(record, "name");
            double value = parseDouble(record, "value");
            
            return Optional.of(new TestRecord(id, name, value));
        }
    }
    
    @Test
    void testReadValidCsv(@TempDir Path tempDir) throws IOException {
        // Create test CSV file
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, """
            id,name,value
            1,Alice,10.5
            2,Bob,20.3
            3,Charlie,30.7
            """);
        
        // Read CSV
        TestCsvReader reader = new TestCsvReader();
        List<TestRecord> records = reader.read(csvFile);
        
        // Verify results
        assertEquals(3, records.size());
        
        assertEquals(1, records.get(0).id);
        assertEquals("Alice", records.get(0).name);
        assertEquals(10.5, records.get(0).value, 0.001);
        
        assertEquals(2, records.get(1).id);
        assertEquals("Bob", records.get(1).name);
        assertEquals(20.3, records.get(1).value, 0.001);
        
        assertEquals(3, records.get(2).id);
        assertEquals("Charlie", records.get(2).name);
        assertEquals(30.7, records.get(2).value, 0.001);
    }
    
    @Test
    void testSkipMalformedRows(@TempDir Path tempDir) throws IOException {
        // Create CSV with malformed rows
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, """
            id,name,value
            1,Alice,10.5
            invalid,Bob,20.3
            3,Charlie,30.7
            4,Dave,not_a_number
            5,Eve,50.1
            """);
        
        // Read CSV - should skip malformed rows
        TestCsvReader reader = new TestCsvReader();
        List<TestRecord> records = reader.read(csvFile);
        
        // Should have only 3 valid records (rows 1, 3, and 5)
        assertEquals(3, records.size());
        assertEquals(1, records.get(0).id);
        assertEquals(3, records.get(1).id);
        assertEquals(5, records.get(2).id);
    }
    
    @Test
    void testEmptyFile(@TempDir Path tempDir) throws IOException {
        // Create empty CSV file (just header)
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, "id,name,value\n");
        
        // Read CSV
        TestCsvReader reader = new TestCsvReader();
        List<TestRecord> records = reader.read(csvFile);
        
        // Should have no records
        assertEquals(0, records.size());
    }
    
    @Test
    void testHandleWhitespace(@TempDir Path tempDir) throws IOException {
        // Create CSV with extra whitespace
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, """
            id,name,value
            1,  Alice  ,10.5
            2,Bob  ,  20.3
              3,Charlie,30.7  
            """);
        
        // Read CSV
        TestCsvReader reader = new TestCsvReader();
        List<TestRecord> records = reader.read(csvFile);
        
        // Should trim whitespace
        assertEquals(3, records.size());
        assertEquals("Alice", records.get(0).name);
        assertEquals("Bob", records.get(1).name);
        assertEquals("Charlie", records.get(2).name);
    }
    
    @Test
    void testOptionalStringField(@TempDir Path tempDir) throws IOException {
        // Test reader that uses optional string field
        class OptionalFieldReader extends CsvReader<String> {
            @Override
            protected Optional<String> parseRecord(CSVRecord record, int lineNumber) {
                return getOptionalString(record, "optional");
            }
        }
        
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, """
            id,optional
            1,value
            2,
            3,  
            4,another
            """);
        
        OptionalFieldReader reader = new OptionalFieldReader();
        List<String> records = reader.read(csvFile);
        
        // Should skip empty optional fields
        assertEquals(2, records.size());
        assertEquals("value", records.get(0));
        assertEquals("another", records.get(1));
    }
    
    @Test
    void testParseIntThrowsOnEmpty(@TempDir Path tempDir) throws IOException {
        class StrictReader extends CsvReader<Integer> {
            @Override
            protected Optional<Integer> parseRecord(CSVRecord record, int lineNumber) {
                return Optional.of(parseInt(record, "value"));
            }
        }
        
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, """
            value
            
            123
            """);
        
        StrictReader reader = new StrictReader();
        List<Integer> records = reader.read(csvFile);
        
        // Should skip row with empty field
        assertEquals(1, records.size());
        assertEquals(123, records.get(0));
    }
}

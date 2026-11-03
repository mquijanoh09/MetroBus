package co.medellin.transit.geocoding;

import co.medellin.transit.model.Coordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that all addresses in data/test_cases.csv can be parsed and located.
 * Per requirements.md acceptance criteria: all 30 addresses (15 origins + 15 destinations) must locate.
 */
class TestCasesAddressTest {
    private static final Path TEST_CASES_FILE = Paths.get("data/test_cases.csv");
    private AddressParser parser;
    private AddressLocator locator;
    
    @BeforeEach
    void setUp() {
        parser = new AddressParser();
        locator = new AddressLocator();
    }
    
    @Test
    void testAllTestCaseAddressesLocate() throws IOException {
        // Read test_cases.csv and extract all addresses
        List<String> addresses = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(TEST_CASES_FILE.toFile()))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip header
                }
                String[] parts = line.split(",", -1);
                if (parts.length >= 2) {
                    addresses.add(parts[0].trim()); // origin
                    addresses.add(parts[1].trim()); // destination
                }
            }
        }
        
        assertEquals(30, addresses.size(), "Should have 30 addresses (15 origins + 15 destinations)");
        
        // Test each address
        List<String> failedToParse = new ArrayList<>();
        List<String> failedToLocate = new ArrayList<>();
        
        for (String addressStr : addresses) {
            Optional<ParsedAddress> parsed = parser.parse(addressStr);
            if (parsed.isEmpty()) {
                failedToParse.add(addressStr);
                continue;
            }
            
            Optional<Coordinate> coord = locator.locate(parsed.get());
            if (coord.isEmpty()) {
                failedToLocate.add(addressStr);
            }
        }
        
        // Assert all addresses parse and locate
        if (!failedToParse.isEmpty()) {
            fail(String.format("Failed to parse %d addresses: %s", 
                failedToParse.size(), String.join(", ", failedToParse)));
        }
        
        if (!failedToLocate.isEmpty()) {
            fail(String.format("Failed to locate %d addresses: %s", 
                failedToLocate.size(), String.join(", ", failedToLocate)));
        }
        
        // If we got here, all 30 addresses parsed and located successfully
        System.out.println("✓ All 30 addresses from test_cases.csv parsed and located successfully");
    }
}

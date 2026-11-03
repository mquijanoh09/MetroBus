package co.medellin.transit.data;

import co.medellin.transit.model.Coordinate;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads ArcGIS REST API JSON format files.
 * Handles paginated results (e.g., stops_1.json, stops_2.json, etc.).
 * 
 * ArcGIS JSON format typically includes:
 * - "features" array with feature objects
 * - Each feature has "attributes" (data fields) and "geometry" (coordinates)
 */
public abstract class ArcGisReader<T> {
    
    private static final Logger LOGGER = Logger.getLogger(ArcGisReader.class.getName());
    
    // Regex patterns for JSON parsing (simple approach without full JSON library)
    private static final Pattern FEATURES_PATTERN = Pattern.compile("\"features\"\\s*:\\s*\\[");
    private static final Pattern ATTRIBUTES_PATTERN = Pattern.compile("\"attributes\"\\s*:\\s*\\{([^}]*)\\}");
    private static final Pattern GEOMETRY_PATTERN = Pattern.compile("\"geometry\"\\s*:\\s*\\{([^}]*)\\}");
    private static final Pattern FIELD_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*([^,}]+)");
    
    /**
     * Reads a single ArcGIS JSON file and parses it into domain objects.
     * Malformed features are logged as warnings and skipped.
     *
     * @param jsonPath path to the JSON file
     * @return list of successfully parsed objects
     * @throws IOException if the file cannot be read
     */
    public List<T> read(Path jsonPath) throws IOException {
        List<T> results = new ArrayList<>();
        
        // Read entire file into memory (ArcGIS files are typically manageable size)
        String jsonContent = Files.readString(jsonPath, StandardCharsets.UTF_8);
        
        // Extract features array
        Matcher featuresMatcher = FEATURES_PATTERN.matcher(jsonContent);
        if (!featuresMatcher.find()) {
            LOGGER.warning(String.format(
                "No features array found in %s",
                jsonPath.getFileName()
            ));
            return results;
        }
        
        // Find all feature objects
        int startPos = featuresMatcher.end();
        String featuresSection = extractFeaturesArray(jsonContent, startPos);
        
        // Extract feature objects using brace counting algorithm
        List<String> featureObjects = extractFeatureObjects(featuresSection);
        
        // Parse each feature
        int featureNumber = 0;
        for (String featureJson : featureObjects) {
            featureNumber++;
            
            try {
                Optional<T> parsed = parseFeature(featureJson, featureNumber);
                parsed.ifPresent(results::add);
                
            } catch (Exception e) {
                LOGGER.warning(String.format(
                    "Skipping malformed feature in %s at feature %d: %s",
                    jsonPath.getFileName(),
                    featureNumber,
                    e.getMessage()
                ));
            }
        }
        
        LOGGER.info(String.format(
            "Successfully loaded %d features from %s",
            results.size(),
            jsonPath.getFileName()
        ));
        
        return results;
    }
    
    /**
     * Reads multiple paginated JSON files.
     * Supports two naming patterns:
     * 1. Offset-based: baseFilename_0.json, baseFilename_2000.json, baseFilename_4000.json
     * 2. Page-based (legacy): baseFilename_1.json, baseFilename_2.json, baseFilename_3.json
     * 
     * Checks for data/raw/ subdirectory first, falls back to base directory.
     *
     * @param baseDir directory containing the JSON files
     * @param baseFilename base filename without page number (e.g., "stops")
     * @param extension file extension (typically ".json")
     * @return combined list of all parsed objects from all pages
     * @throws IOException if any file read operation fails
     */
    public List<T> readPaginated(Path baseDir, String baseFilename, String extension) throws IOException {
        List<T> allResults = new ArrayList<>();
        
        // Try offset-based pattern in raw/ subdirectory first
        Path rawDir = baseDir.resolve("raw");
        if (Files.exists(rawDir)) {
            LOGGER.info(String.format("Searching for paginated files in: %s", rawDir));
            List<Path> offsetFiles = findOffsetBasedFiles(rawDir, baseFilename, extension);
            
            if (!offsetFiles.isEmpty()) {
                LOGGER.info(String.format(
                    "Found %d offset-based files matching pattern %s_\\d+%s in raw/",
                    offsetFiles.size(),
                    baseFilename,
                    extension
                ));
                
                for (Path filePath : offsetFiles) {
                    List<T> pageResults = read(filePath);
                    allResults.addAll(pageResults);
                }
                
                LOGGER.info(String.format(
                    "Loaded %d total features from %d offset-based files of %s",
                    allResults.size(),
                    offsetFiles.size(),
                    baseFilename
                ));
                
                return allResults;
            }
        }
        
        // Try offset-based pattern in base directory
        LOGGER.info(String.format("Searching for paginated files in: %s", baseDir));
        List<Path> offsetFiles = findOffsetBasedFiles(baseDir, baseFilename, extension);
        
        if (!offsetFiles.isEmpty()) {
            LOGGER.info(String.format(
                "Found %d offset-based files matching pattern %s_\\d+%s",
                offsetFiles.size(),
                baseFilename,
                extension
            ));
            
            for (Path filePath : offsetFiles) {
                List<T> pageResults = read(filePath);
                allResults.addAll(pageResults);
            }
            
            LOGGER.info(String.format(
                "Loaded %d total features from %d offset-based files of %s",
                allResults.size(),
                offsetFiles.size(),
                baseFilename
            ));
            
            return allResults;
        }
        
        // Fall back to old page-based naming (e.g., stops_1.json, stops_2.json)
        LOGGER.info(String.format(
            "No offset-based files found, trying page-based naming pattern %s_1%s, %s_2%s, ...",
            baseFilename,
            extension,
            baseFilename,
            extension
        ));
        
        int pageNumber = 1;
        while (true) {
            String filename = String.format("%s_%d%s", baseFilename, pageNumber, extension);
            Path filePath = baseDir.resolve(filename);
            
            if (!Files.exists(filePath)) {
                // No more pages
                if (pageNumber == 1) {
                    LOGGER.warning(String.format(
                        "No paginated files found for %s in %s",
                        baseFilename,
                        baseDir
                    ));
                }
                break;
            }
            
            List<T> pageResults = read(filePath);
            allResults.addAll(pageResults);
            pageNumber++;
        }
        
        if (pageNumber > 1) {
            LOGGER.info(String.format(
                "Loaded %d total features from %d page-based files of %s",
                allResults.size(),
                pageNumber - 1,
                baseFilename
            ));
        }
        
        return allResults;
    }
    
    /**
     * Finds all files matching offset-based naming pattern in the given directory.
     * Pattern: baseFilename_\d+extension (e.g., stops_0.json, stops_2000.json)
     * Files are sorted numerically by offset.
     *
     * @param directory directory to search
     * @param baseFilename base filename without offset
     * @param extension file extension
     * @return sorted list of matching files (empty if none found)
     * @throws IOException if directory listing fails
     */
    private List<Path> findOffsetBasedFiles(Path directory, String baseFilename, String extension) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return new ArrayList<>();
        }
        
        // Regex pattern to match: baseFilename_<digits>extension
        Pattern offsetPattern = Pattern.compile(
            Pattern.quote(baseFilename) + "_(\\d+)" + Pattern.quote(extension)
        );
        
        List<Path> matchingFiles = new ArrayList<>();
        
        try (var stream = Files.list(directory)) {
            stream.forEach(path -> {
                String filename = path.getFileName().toString();
                Matcher matcher = offsetPattern.matcher(filename);
                if (matcher.matches()) {
                    matchingFiles.add(path);
                }
            });
        }
        
        // Sort files by offset number (extracted from filename)
        matchingFiles.sort((p1, p2) -> {
            String name1 = p1.getFileName().toString();
            String name2 = p2.getFileName().toString();
            
            Matcher m1 = offsetPattern.matcher(name1);
            Matcher m2 = offsetPattern.matcher(name2);
            
            if (m1.matches() && m2.matches()) {
                int offset1 = Integer.parseInt(m1.group(1));
                int offset2 = Integer.parseInt(m2.group(1));
                return Integer.compare(offset1, offset2);
            }
            
            return name1.compareTo(name2);
        });
        
        return matchingFiles;
    }
    
    /**
     * Parses a single feature JSON object into a domain object.
     * Return Optional.empty() to skip this feature without logging a warning.
     *
     * @param featureJson the feature JSON string
     * @param featureNumber the feature number (for error reporting)
     * @return parsed object, or empty to skip
     * @throws Exception if parsing fails (will be caught and logged)
     */
    protected abstract Optional<T> parseFeature(String featureJson, int featureNumber) throws Exception;
    
    /**
     * Extracts the attributes object from a feature JSON.
     *
     * @param featureJson the feature JSON string
     * @return attributes JSON content
     * @throws IllegalArgumentException if attributes not found
     */
    protected String extractAttributes(String featureJson) {
        Matcher matcher = ATTRIBUTES_PATTERN.matcher(featureJson);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No attributes found in feature");
        }
        return matcher.group(1);
    }
    
    /**
     * Extracts the geometry object from a feature JSON.
     *
     * @param featureJson the feature JSON string
     * @return optional geometry JSON content
     */
    protected Optional<String> extractGeometry(String featureJson) {
        Matcher matcher = GEOMETRY_PATTERN.matcher(featureJson);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }
    
    /**
     * Gets a string field from JSON attributes.
     *
     * @param attributesJson the attributes JSON content
     * @param fieldName the field name
     * @return field value, or empty if not found
     */
    protected Optional<String> getStringField(String attributesJson, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(attributesJson);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }
    
    /**
     * Gets an integer field from JSON attributes.
     *
     * @param attributesJson the attributes JSON content
     * @param fieldName the field name
     * @return field value
     * @throws IllegalArgumentException if field not found or not a valid integer
     */
    protected int getIntField(String attributesJson, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(attributesJson);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found");
        }
        return Integer.parseInt(matcher.group(1));
    }
    
    /**
     * Gets a double field from JSON attributes.
     *
     * @param attributesJson the attributes JSON content
     * @param fieldName the field name
     * @return field value
     * @throws IllegalArgumentException if field not found or not a valid double
     */
    protected double getDoubleField(String attributesJson, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*(-?\\d+\\.?\\d*)");
        Matcher matcher = pattern.matcher(attributesJson);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found");
        }
        return Double.parseDouble(matcher.group(1));
    }
    
    /**
     * Parses coordinate from geometry JSON (x, y format).
     *
     * @param geometryJson the geometry JSON content
     * @return coordinate with lon/lat
     * @throws IllegalArgumentException if geometry is invalid
     */
    protected Coordinate parseGeometryCoordinate(String geometryJson) {
        // ArcGIS geometry format: { "x": lon, "y": lat } or { "longitude": lon, "latitude": lat }
        double lon, lat;
        
        // Try x/y format first
        Pattern xPattern = Pattern.compile("\"x\"\\s*:\\s*(-?\\d+\\.?\\d*)");
        Pattern yPattern = Pattern.compile("\"y\"\\s*:\\s*(-?\\d+\\.?\\d*)");
        
        Matcher xMatcher = xPattern.matcher(geometryJson);
        Matcher yMatcher = yPattern.matcher(geometryJson);
        
        if (xMatcher.find() && yMatcher.find()) {
            lon = Double.parseDouble(xMatcher.group(1));
            lat = Double.parseDouble(yMatcher.group(1));
        } else {
            // Try longitude/latitude format
            Pattern lonPattern = Pattern.compile("\"longitude\"\\s*:\\s*(-?\\d+\\.?\\d*)");
            Pattern latPattern = Pattern.compile("\"latitude\"\\s*:\\s*(-?\\d+\\.?\\d*)");
            
            Matcher lonMatcher = lonPattern.matcher(geometryJson);
            Matcher latMatcher = latPattern.matcher(geometryJson);
            
            if (!lonMatcher.find() || !latMatcher.find()) {
                throw new IllegalArgumentException("Invalid geometry format");
            }
            
            lon = Double.parseDouble(lonMatcher.group(1));
            lat = Double.parseDouble(latMatcher.group(1));
        }
        
        return new Coordinate(lat, lon);
    }
    
    /**
     * Extracts individual feature objects from the features array section using brace counting.
     * This correctly handles nested objects like {"attributes":{...},"geometry":{"x":...}}.
     * 
     * The algorithm:
     * 1. Tracks opening/closing braces using a counter
     * 2. Handles string literals (skips braces inside quotes to avoid false matches)
     * 3. Extracts complete feature objects including nested geometry
     * 
     * @param featuresSection the features array content (without outer brackets)
     * @return list of feature JSON strings
     */
    private List<String> extractFeatureObjects(String featuresSection) {
        List<String> features = new ArrayList<>();
        int i = 0;
        int length = featuresSection.length();
        
        while (i < length) {
            // Skip whitespace and commas
            while (i < length && (Character.isWhitespace(featuresSection.charAt(i)) || featuresSection.charAt(i) == ',')) {
                i++;
            }
            
            if (i >= length) {
                break;
            }
            
            // Check if we're at the start of a feature object
            char c = featuresSection.charAt(i);
            if (c == '{') {
                // Extract complete feature object using brace counting
                int start = i;
                int braceCount = 0;
                boolean inString = false;
                boolean escaped = false;
                
                while (i < length) {
                    c = featuresSection.charAt(i);
                    
                    if (escaped) {
                        // Skip escaped character
                        escaped = false;
                    } else if (c == '\\') {
                        // Next character is escaped
                        escaped = true;
                    } else if (c == '"') {
                        // Toggle string state
                        inString = !inString;
                    } else if (!inString) {
                        // Only count braces outside of strings
                        if (c == '{') {
                            braceCount++;
                        } else if (c == '}') {
                            braceCount--;
                            if (braceCount == 0) {
                                // Complete feature object extracted
                                String featureJson = featuresSection.substring(start, i + 1);
                                features.add(featureJson);
                                i++;
                                break;
                            }
                        }
                    }
                    
                    i++;
                }
            } else {
                // Not a feature object, skip this character
                i++;
            }
        }
        
        return features;
    }
    
    /**
     * Extracts the features array content from JSON.
     * Simple brace matching to find the end of the features array.
     */
    private String extractFeaturesArray(String json, int startPos) {
        int braceCount = 1; // We're already inside the array
        int endPos = startPos;
        
        while (endPos < json.length() && braceCount > 0) {
            char c = json.charAt(endPos);
            if (c == '[') {
                braceCount++;
            } else if (c == ']') {
                braceCount--;
            }
            endPos++;
        }
        
        return json.substring(startPos, endPos);
    }
}

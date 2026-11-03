package co.medellin.transit.geocoding;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Medellín street addresses following the city's address system.
 * 
 * <p>This parser implements the exact algorithm from the spec:
 * <ol>
 *   <li><b>Normalize</b>: Upper case, drop after comma, handle separators (#, No., N°), 
 *       collapse spaces, apply street type synonyms</li>
 *   <li><b>Parse with pattern</b>: STREET NUMBER[letters] [SUR] [X|CON|#] [STREET] NUMBER[letters]</li>
 *   <li><b>Letter handling</b>: Convert letter suffixes to fractional values (A=0.075, B=0.15, etc.)</li>
 *   <li><b>SUR handling</b>: SUR suffix makes numbers negative</li>
 * </ol>
 * 
 * <p>Valid examples:
 * <ul>
 *   <li>"cl 50 # 53-48" → CALLE 50, CARRERA 53, building 48</li>
 *   <li>"Calle 10 # 43A - 20" → CALLE 10, CARRERA 43.075, building 20</li>
 *   <li>"CL 12 Sur x CR 50" → CALLE -12 (SUR makes negative), CARRERA 50</li>
 *   <li>"CR 70 x CL 48 SUR" → CARRERA 70, CALLE -48</li>
 * </ul>
 * 
 * <p>Invalid examples that return empty:
 * <ul>
 *   <li>"hola mundo" - not an address format</li>
 *   <li>"VDA EL YOLOMBO" - rural addresses not supported</li>
 *   <li>"CL 900 x CR 900" - out of valid range</li>
 * </ul>
 */
public class AddressParser {
    
    // Pattern to match the address format after normalization
    // Matches: STREETTYPE NUMBER[LETTER] [SUR] [SEPARATOR] [STREETTYPE] NUMBER[LETTER] [SUR] [- NUMBER]
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "^(CL|CALLE|CR|KR|CRA|CARRERA|DG|DIAGONAL|TV|TRANSVERSAL|CIRCULAR)\\s+" +
        "([0-9]+)([A-Z]{1,2})?\\s*(SUR|NORTE)?\\s*" +
        "(?:[#X]|CON|No\\.?|N°)\\s*" +
        "(?:(CL|CALLE|CR|KR|CRA|CARRERA|DG|DIAGONAL|TV|TRANSVERSAL|CIRCULAR)\\s+)?" +
        "([0-9]+)([A-Z]{1,2})?\\s*(SUR|NORTE)?\\s*" +
        "(?:-\\s*([0-9]+))?$",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Parses a Medellín address string into structured components.
     * 
     * @param addressStr the address string to parse
     * @return ParsedAddress if valid, empty if invalid or unparseable
     */
    public Optional<ParsedAddress> parse(String addressStr) {
        if (addressStr == null || addressStr.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Step 1: Normalize the address
        String normalized = normalize(addressStr);
        
        // Step 2: Parse with regex pattern
        Matcher matcher = ADDRESS_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        
        try {
            // Extract components
            String streetType1Str = matcher.group(1);
            String number1Str = matcher.group(2);
            String letter1 = matcher.group(3);
            String cardinal1Str = matcher.group(4);
            
            String streetType2Str = matcher.group(5);  // May be null
            String number2Str = matcher.group(6);
            String letter2 = matcher.group(7);
            String cardinal2Str = matcher.group(8);
            
            String buildingStr = matcher.group(9);
            
            // Parse street types
            StreetType streetType1 = StreetType.fromString(streetType1Str);
            StreetType streetType2 = streetType2Str != null ? 
                                     StreetType.fromString(streetType2Str) : null;
            
            if (streetType1 == null) {
                return Optional.empty();
            }
            
            // Parse numbers with letter handling
            double number1 = parseNumberWithLetter(number1Str, letter1);
            double number2 = parseNumberWithLetter(number2Str, letter2);
            
            // Apply SUR handling (makes number negative)
            if (cardinal1Str != null && cardinal1Str.toUpperCase().equals("SUR")) {
                number1 = -Math.abs(number1);
            }
            if (cardinal2Str != null && cardinal2Str.toUpperCase().equals("SUR")) {
                number2 = -Math.abs(number2);
            }
            
            // Validate ranges (Medellín streets typically 1-200, reject extreme values)
            if (Math.abs(number1) > 300 || Math.abs(number2) > 300) {
                return Optional.empty();
            }
            
            // Parse building number
            Optional<Integer> buildingNumber = Optional.empty();
            if (buildingStr != null) {
                buildingNumber = Optional.of(Integer.parseInt(buildingStr));
            }
            
            // Determine which is primary/secondary based on street types
            // If both types specified: use them as given
            // If only one type: assume the pattern is STREET1 num1 # num2
            //   - If STREET1 is CALLE, then num2 is CARRERA
            //   - If STREET1 is CARRERA, then num2 is CALLE
            
            StreetType finalStreetType;
            double primaryNumber;
            double secondaryNumber;
            Cardinal cardinal;
            
            if (streetType2 != null) {
                // Explicit second street type - determine which is the "main" one
                // Convention: CALLE typically comes first in the result
                if (streetType1 == StreetType.CALLE || streetType1 == StreetType.CARRERA) {
                    finalStreetType = streetType1;
                    primaryNumber = number1;
                    secondaryNumber = number2;
                    cardinal = Cardinal.fromString(cardinal1Str);
                } else {
                    finalStreetType = streetType1;
                    primaryNumber = number1;
                    secondaryNumber = number2;
                    cardinal = Cardinal.fromString(cardinal1Str);
                }
            } else {
                // Only one street type specified
                // Convention: "CL 50 # 53" means Calle 50, Carrera 53
                finalStreetType = streetType1;
                primaryNumber = number1;
                secondaryNumber = number2;
                cardinal = Cardinal.fromString(cardinal1Str);
                if (cardinal == null) {
                    cardinal = Cardinal.fromString(cardinal2Str);
                }
            }
            
            return Optional.of(new ParsedAddress(
                finalStreetType,
                primaryNumber,
                secondaryNumber,
                buildingNumber,
                Optional.ofNullable(cardinal)
            ));
            
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Normalizes an address string according to the algorithm:
     * - Convert to upper case
     * - Drop everything after first comma
     * - Treat #, No., N° as separators
     * - Collapse multiple spaces to single space
     * - Apply street type synonyms
     * 
     * @param address the raw address string
     * @return normalized address string
     */
    private String normalize(String address) {
        // Upper case
        String normalized = address.toUpperCase();
        
        // Drop everything after first comma
        int commaIndex = normalized.indexOf(',');
        if (commaIndex >= 0) {
            normalized = normalized.substring(0, commaIndex);
        }
        
        // Normalize separators: treat No., N° as #
        normalized = normalized.replaceAll("No\\.?", "#");
        normalized = normalized.replaceAll("N°", "#");
        
        // Normalize X and CON as intersections (keep as is, pattern handles them)
        
        // Collapse multiple spaces
        normalized = normalized.replaceAll("\\s+", " ");
        
        // Trim
        normalized = normalized.trim();

        // Insert # before cross-street number if separator is missing
        // Handles: "CL 50 53-48" -> "CL 50 # 53-48", "CL 48EE 99D-" -> "CL 48EE # 99D-0"
        if (!normalized.contains("#") && !normalized.contains(" X ") && !normalized.contains(" CON ")) {
            normalized = normalized.replaceAll(
                "^((?:CL|CALLE|CR|KR|CRA|CARRERA|DG|DIAGONAL|TV|TRANSVERSAL|CIRCULAR)\\s+[0-9]+[A-Z]*(?:\\s+(?:SUR|NORTE))?)\\s+([0-9]+[A-Z]*)(.*)$",
                "$1 # $2$3"
            );
            normalized = normalized.replaceAll("-\\s*$", "-0");
        }

        return normalized;
    }
    
    /**
     * Parses a number with optional letter suffix.
     * Letter suffixes are converted to fractional values:
     * A = 0.075, B = 0.15, C = 0.225, etc. (each letter adds 0.075)
     * 
     * @param numberStr the numeric part
     * @param letter the letter suffix (may be null)
     * @return the number with fractional component if letter present
     */
    private double parseNumberWithLetter(String numberStr, String letter) {
        double base = Double.parseDouble(numberStr);
        
        if (letter != null && !letter.isEmpty()) {
            char letterChar = letter.charAt(0);
            // A=1, B=2, C=3, etc.
            int letterValue = letterChar - 'A' + 1;
            // Each letter adds 0.075
            double fraction = letterValue * 0.075;
            base += fraction;
        }
        
        return base;
    }
}

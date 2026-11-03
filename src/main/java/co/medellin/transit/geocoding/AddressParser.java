package co.medellin.transit.geocoding;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Medellín street addresses - port of reference/prototype.py parse_address() (lines 39-60).
 * 
 * Maintains ParsedAddress API (street types, cardinal) while using prototype logic for (calle, carrera) extraction.
 */
public class AddressParser {
    
    // Pattern matches prototype's regex with CIRCULAR/CQ and NORTE added, plus building number capture
    // Use word boundary after number to prevent letter suffix being part of longer word
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "(CL|CR|KR|CRA|DG|TV|CQ|CALLE|CARRERA|DIAGONAL|TRANSVERSAL|CIRCULAR|CIRCUNVALAR)\\.?\\s+" +
        "(\\d+\\s?[A-Z]{0,3}(?:\\s+BIS)?)\\s*(SUR|ESTE|NORTE)?\\s*(?:X|CON|#)?\\s*" +
        "(?:(CL|CR|KR|CRA|DG|TV|CQ|CALLE|CARRERA|CIRCULAR|CIRCUNVALAR)\\.?\\s+)?" +
        "(\\d+\\s?[A-Z]{0,3})\\s*(SUR|ESTE|NORTE)?(?:\\s*-\\s*(\\d+))?(?:\\s|$)",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Parses a Medellín address string.
     * 
     * @param addressStr the address string to parse
     * @return ParsedAddress if valid, empty if invalid or unparseable
     */
    public Optional<ParsedAddress> parse(String addressStr) {
        if (addressStr == null || addressStr.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Normalize: text before first comma (prototype line 52)
        String d = addressStr.toUpperCase();
        int commaIndex = d.indexOf(',');
        if (commaIndex >= 0) {
            d = d.substring(0, commaIndex);
        }
        
        // Replace separators: '#', 'N°', 'NO.' all become ' # ' (prototype line 53)
        d = d.replace("#", " # ");
        d = d.replace("N°", " # ");
        d = d.replace("NO.", " # ");
        
        // Collapse spaces (prototype line 54)
        d = d.replaceAll("\\s+", " ").trim();
        
        System.err.println("DEBUG normalize: input='" + addressStr + "' after='" + d + "'");
        
        // Match pattern (prototype line 55-57)
        Matcher m = ADDRESS_PATTERN.matcher(d);
        if (!m.find()) {
            return Optional.empty();
        }
        
        try {
            String streetType1Str = m.group(1);
            String number1Str = m.group(2);
            String cardinal1Str = m.group(3);
            String streetType2Str = m.group(4);  // may be null
            String number2Str = m.group(5);
            String cardinal2Str = m.group(6);
            String buildingStr = m.group(7);  // building number (may be null)
            
            // Debug: print what was captured
            String matched = m.group(0);
            System.err.println("DEBUG: d='" + d + "' matched='" + matched + "' number1Str='" + number1Str + "' number2Str='" + number2Str + "'");
            
            // Validation: Detect if keywords (CON, SUR, ESTE, NORTE) were consumed into number
            // "CL 50 CON CR 53" incorrectly captures "50 CON" because pattern allows \\d+\\s?[A-Z]{0,3}
            // Strip these keywords from the end of number strings
            if (number1Str.matches(".*\\s+(CON|SUR|ESTE|NORTE|NORT|EST|ON)\\s*$")) {
                number1Str = number1Str.replaceAll("\\s+(CON|SUR|ESTE|NORTE|NORT|EST|ON)\\s*$", "").trim();
            }
            if (number2Str != null && number2Str.matches(".*\\s+(CON|SUR|ESTE|NORTE|NORT|EST|ON)\\s*$")) {
                number2Str = number2Str.replaceAll("\\s+(CON|SUR|ESTE|NORTE|NORT|EST|ON)\\s*$", "").trim();
            }
            
            // Validation: Check for incomplete addresses (separator with no secondary number)
            // "CL 50 #" should be rejected - the '#' must be followed by a number
            // Also reject if the matched portion is clearly incomplete
            if (d.contains(" # ") && !matched.contains(" # ")) {
                // Separator in input but not in match = incomplete
                return Optional.empty();
            }
            if (d.contains(" X ") && !matched.contains(" X ")) {
                return Optional.empty();
            }
            if (d.contains(" CON ") && !matched.contains(" CON ")) {
                return Optional.empty();
            }
            
            // Map street types to CL or CR (prototype STREET dict, lines 41-42)
            String prototypeKind1 = mapToPrototypeKind(streetType1Str);
            if (prototypeKind1 == null) {
                return Optional.empty();
            }
            
            // Parse StreetType enum for API
            StreetType streetType1 = StreetType.fromString(streetType1Str);
            StreetType streetType2 = streetType2Str != null ? StreetType.fromString(streetType2Str) : null;
            
            // Parse numbers with letter handling (prototype's _number function, lines 45-48)
            Double num1 = parseNumber(number1Str, "SUR".equals(cardinal1Str));
            Double num2 = parseNumber(number2Str, "SUR".equals(cardinal2Str));
            
            if (num1 == null || num2 == null) {
                return Optional.empty();
            }
            
            // Validate: reject incomplete addresses (single number only)
            // The pattern requires both numbers, so this is already validated by regex
            
            // Validate: reject out-of-range addresses (> 300 is unrealistic for Medellín)
            if (Math.abs(num1) > 300 || Math.abs(num2) > 300) {
                return Optional.empty();
            }
            
            // Parse building number
            Optional<Integer> buildingNumber = Optional.empty();
            if (buildingStr != null && !buildingStr.trim().isEmpty()) {
                buildingNumber = Optional.of(Integer.parseInt(buildingStr.trim()));
            }
            
            // Parse cardinals for API
            Cardinal cardinal1 = Cardinal.fromString(cardinal1Str);
            Cardinal cardinal2 = Cardinal.fromString(cardinal2Str);
            Cardinal primaryCardinal = cardinal1 != null ? cardinal1 : cardinal2;
            
            // For ParsedAddress API: store ORIGINAL numbers as primary/secondary
            // The getCalleNumber()/getCarreraNumber() methods will apply prototype transformation
            return Optional.of(new ParsedAddress(
                streetType1,
                num1,         // primaryNumber: the first number as parsed
                num2,         // secondaryNumber: the second number as parsed  
                buildingNumber,
                Optional.ofNullable(primaryCardinal)
            ));
            
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Maps street type to prototype's CL or CR (prototype STREET dict, lines 41-42).
     * CL/CALLE/DG/DIAGONAL -> 'CL'
     * CR/KR/CRA/CARRERA/TV/TRANSVERSAL -> 'CR'
     * CIRCULAR/CQ/CIRCUNVALAR -> 'CL' (parsed but rejected in locate)
     */
    private String mapToPrototypeKind(String streetType) {
        if (streetType == null) return null;
        String upper = streetType.toUpperCase();
        
        return switch (upper) {
            case "CL", "CALLE", "DG", "DIAGONAL" -> "CL";
            case "CR", "KR", "CRA", "CARRERA", "TV", "TRANSVERSAL" -> "CR";
            case "CIRCULAR", "CQ", "CIRCUNVALAR" -> "CL";  // Parsed but rejected later
            default -> null;
        };
    }
    
    /**
     * Port of prototype's _number function (lines 45-48).
     * Parses number with optional letter suffix.
     * Letter fraction: 0.3 * (letter_index) / 4 for A-G
     * 
     * @param s the number string (may include letter and BIS)
     * @param south true if SUR modifier present (makes negative)
     * @return the number with fractional component, or null if invalid
     */
    private Double parseNumber(String s, boolean south) {
        if (s == null) return null;
        
        // Match: (\d+)\s*([A-Z]{0,3})  (prototype line 46)
        Pattern numPattern = Pattern.compile("(\\d+)\\s*([A-Z]{0,3})");
        Matcher m = numPattern.matcher(s.trim());
        
        if (!m.find()) {
            return null;
        }
        
        try {
            int base = Integer.parseInt(m.group(1));
            String letters = m.group(2);
            
            double n = base;
            
            // Add fraction for first letter if it's A-G (prototype line 47)
            // 0.3 * (ord(m.group(2)[0]) - 64) / 4 if m.group(2) and m.group(2)[0] in 'ABCDEFG'
            if (letters != null && !letters.isEmpty()) {
                char firstLetter = letters.charAt(0);
                if (firstLetter >= 'A' && firstLetter <= 'G') {
                    // ord('A') = 65, so (65-64)=1, (66-64)=2, etc.
                    n += 0.3 * (firstLetter - 64) / 4.0;
                }
            }
            
            // Apply SUR modifier (prototype line 48: return -n if south else n)
            if (south) {
                n = -n;
            }
            
            return n;
            
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

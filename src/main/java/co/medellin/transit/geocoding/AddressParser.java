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
    // Number pattern: \d+\s?[A-Z]{0,3}(?![A-Z]) - negative lookahead prevents 4+ consecutive letters
    // Separator/spacing: optional separator with flexible spacing, OR just required spacing to prevent "50"→"5"+"0"
    // After building dash: -\s*(\d+) or just - (captured as empty string via lookahead)
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
        "(CL|CR|KR|CRA|DG|TV|CQ|CALLE|CARRERA|DIAGONAL|TRANSVERSAL|CIRCULAR|CIRCUNVALAR)\\.?\\s+" +
        "(\\d+\\s?[A-Z]{0,3}(?![A-Z]))\\s*(SUR|ESTE|NORTE)?\\s*(?:(?:X|CON|#)\\s*)?\\s*" +
        "(?:(CL|CR|KR|CRA|DG|TV|CQ|CALLE|CARRERA|CIRCULAR|CIRCUNVALAR)\\.?\\s+)?" +
        "(\\d+\\s?[A-Z]{0,3}(?![A-Z]))\\s*(SUR|ESTE|NORTE)?(?:\\s*-\\s*(\\d+|(?=\\s*$)))?",
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
            
            // Strip cardinal/separator keywords from number strings if they were captured by \d+\s?[A-Z]{0,3}
            // The \b doesn't prevent "12 SUR" from matching since "SUR" is 3 letters
            // Also detect if we stripped a cardinal and use it
            if (number1Str.matches(".*\\s+(SUR|ESTE|NORTE)$")) {
                String[] parts = number1Str.split("\\s+");
                String keyword = parts[parts.length - 1];
                number1Str = number1Str.substring(0, number1Str.length() - keyword.length()).trim();
                if (cardinal1Str == null && (keyword.equals("SUR") || keyword.equals("ESTE") || keyword.equals("NORTE"))) {
                    cardinal1Str = keyword;
                }
            }
            if (number2Str.matches(".*\\s+(SUR|ESTE|NORTE)$")) {
                String[] parts = number2Str.split("\\s+");
                String keyword = parts[parts.length - 1];
                number2Str = number2Str.substring(0, number2Str.length() - keyword.length()).trim();
                if (cardinal2Str == null && (keyword.equals("SUR") || keyword.equals("ESTE") || keyword.equals("NORTE"))) {
                    cardinal2Str = keyword;
                }
            }
            // Also strip "CON" which should never be part of a number
            number1Str = number1Str.replaceAll("\\s+CON$", "").trim();
            number2Str = number2Str.replaceAll("\\s+CON$", "").trim();
            
            // Validation: If input has a separator (#, X, CON) but it's not in the matched part,
            // the address is incomplete (e.g., "CL 50 #" should be rejected)
            String matched = m.group(0);
            
            // Check for invalid parse where single number got split (e.g., "CL 50 " -> "5" + "0")
            // This happens when there's no separator and no second street type
            boolean hasSeparator = matched.contains(" # ") || matched.contains(" X ") || matched.contains(" CON ");
            boolean hasSecondStreetType = streetType2Str != null;
            if (!hasSeparator && !hasSecondStreetType) {
                // Without separator or second street type, check for suspicious single-digit splits
                String num1Clean = number1Str.replaceAll("[^\\d]", "");
                String num2Clean = number2Str.replaceAll("[^\\d]", "");
                // Only reject if BOTH are single digit AND there's no building number AND no space in original match
                // "CR 5 5-11" is valid (has building), "CL 50 " -> "5" "0" is invalid (no building, no space between)
                if (num1Clean.length() == 1 && num2Clean.length() == 1 && buildingStr == null) {
                    // Check if the match has proper spacing between numbers
                    // If the two numbers appear consecutively in the matched string without separator, it's invalid
                    String compactMatch = matched.replaceAll("\\s+", "");
                    if (compactMatch.matches(".*\\d\\d.*")) {
                        // Two digits adjacent = likely from splitting one number
                        return Optional.empty();
                    }
                }
            }
            
            // Also check: if separator is at the end of the match with trailing space, it's incomplete
            if (matched.trim().endsWith("#") || matched.trim().endsWith("X") || matched.trim().endsWith("CON")) {
                return Optional.empty();
            }
            
            if ((d.contains(" # ") && !matched.contains(" # ")) ||
                (d.contains(" X ") && !matched.contains(" X ")) ||
                (d.contains(" CON ") && !matched.contains(" CON "))) {
                // Separator in input but not in match = incomplete
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
            // Apply SUR/ESTE/NORTE modifiers when parsing
            boolean num1South = "SUR".equalsIgnoreCase(cardinal1Str);
            boolean num2South = "SUR".equalsIgnoreCase(cardinal2Str);
            
            Double num1 = parseNumber(number1Str, num1South);
            Double num2 = parseNumber(number2Str, num2South);
            
            if (num1 == null || num2 == null) {
                return Optional.empty();
            }
            
            // Validate: reject incomplete addresses (single number only)
            // The pattern requires both numbers, so this is already validated by regex
            
            // Validate: reject out-of-range addresses (> 300 is unrealistic for Medellín)
            if (Math.abs(num1) > 300 || Math.abs(num2) > 300) {
                return Optional.empty();
            }
            
            // Parse building number - check if we have a dash but no number after it
            Optional<Integer> buildingNumber = Optional.empty();
            if (buildingStr != null) {
                String trimmed = buildingStr.trim();
                if (!trimmed.isEmpty()) {
                    buildingNumber = Optional.of(Integer.parseInt(trimmed));
                } else {
                    // Empty string from lookahead means trailing dash with no number -> 0
                    buildingNumber = Optional.of(0);
                }
            }
            
            // Parse cardinals for API - prefer the first one, fallback to second
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
     * For multi-letter suffixes like "EE", only first letter counts if it's A-G
     * 
     * @param s the number string (may include letter suffix)
     * @param south true if SUR modifier present (makes negative)
     * @return the number with fractional component, or null if invalid
     */
    private Double parseNumber(String s, boolean south) {
        if (s == null) return null;
        
        // Match: (\d+)([A-Z]{0,2})  (prototype line 46, but we allow up to 2 letters)
        Pattern numPattern = Pattern.compile("(\\d+)([A-Z]{0,2})");
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

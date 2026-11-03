package co.medellin.transit.geocoding;

/**
 * Enum representing the types of streets in Medellín's address system.
 * Includes standard abbreviations and synonyms used in addresses.
 */
public enum StreetType {
    CALLE,          // Calle, CL
    CARRERA,        // Carrera, CR, KR, CRA
    DIAGONAL,       // Diagonal, DG
    CIRCULAR,       // Circular
    TRANSVERSAL;    // Transversal, TV
    
    /**
     * Parses a street type from a string, handling common synonyms and abbreviations.
     * 
     * @param str the street type string (case-insensitive)
     * @return the corresponding StreetType, or null if not recognized
     */
    public static StreetType fromString(String str) {
        if (str == null) {
            return null;
        }
        
        String upper = str.toUpperCase().trim();
        
        return switch (upper) {
            case "CALLE", "CL" -> CALLE;
            case "CARRERA", "CR", "KR", "CRA" -> CARRERA;
            case "DIAGONAL", "DG" -> DIAGONAL;
            case "CIRCULAR" -> CIRCULAR;
            case "TRANSVERSAL", "TV" -> TRANSVERSAL;
            default -> null;
        };
    }
}

package co.medellin.transit.geocoding;

/**
 * Enum representing cardinal directions used in Medellín addresses.
 * SUR (south) is particularly important as it makes address numbers negative.
 */
public enum Cardinal {
    NORTE,
    SUR,
    ESTE,
    OESTE;
    
    /**
     * Parses a cardinal direction from a string.
     * 
     * @param str the cardinal direction string (case-insensitive)
     * @return the corresponding Cardinal, or null if not recognized
     */
    public static Cardinal fromString(String str) {
        if (str == null) {
            return null;
        }
        
        String upper = str.toUpperCase().trim();
        
        return switch (upper) {
            case "NORTE" -> NORTE;
            case "SUR" -> SUR;
            case "ESTE" -> ESTE;
            case "OESTE" -> OESTE;
            default -> null;
        };
    }
}

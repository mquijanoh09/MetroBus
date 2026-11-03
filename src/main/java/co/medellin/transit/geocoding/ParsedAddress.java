package co.medellin.transit.geocoding;

import java.util.Optional;

/**
 * Represents a parsed Medellín address with its structured components.
 * 
 * <p>Medellín addresses follow the pattern:
 * (StreetType) (primaryNumber) [# | No. | N°] (secondaryNumber) [Cardinal] [- buildingNumber]
 * 
 * <p>Examples:
 * <ul>
 *   <li>"Calle 50 # 53-48" → CALLE, primary=50, secondary=53, building=48</li>
 *   <li>"CR 70 x CL 48 SUR" → CARRERA, primary=70, secondary=-48 (SUR makes it negative)</li>
 *   <li>"Calle 10 # 43A - 20" → CALLE, primary=10, secondary=43.075 (A=0.075), building=20</li>
 * </ul>
 */
public class ParsedAddress {
    private final StreetType streetType;
    private final double primaryNumber;      // Main street number (can be fractional for letter suffixes)
    private final double secondaryNumber;    // Cross street number (negative if SUR)
    private final Optional<Integer> buildingNumber;
    private final Optional<Cardinal> cardinal;
    
    /**
     * Creates a parsed address with all components.
     * 
     * @param streetType the type of street (CALLE, CARRERA, etc.)
     * @param primaryNumber the primary street number
     * @param secondaryNumber the secondary street number (negative if SUR)
     * @param buildingNumber the building number (optional)
     * @param cardinal the cardinal direction (optional)
     */
    public ParsedAddress(StreetType streetType, double primaryNumber, double secondaryNumber,
                        Optional<Integer> buildingNumber, Optional<Cardinal> cardinal) {
        this.streetType = streetType;
        this.primaryNumber = primaryNumber;
        this.secondaryNumber = secondaryNumber;
        this.buildingNumber = buildingNumber;
        this.cardinal = cardinal;
    }
    
    public StreetType getStreetType() {
        return streetType;
    }
    
    public double getPrimaryNumber() {
        return primaryNumber;
    }
    
    public double getSecondaryNumber() {
        return secondaryNumber;
    }
    
    public Optional<Integer> getBuildingNumber() {
        return buildingNumber;
    }
    
    public Optional<Cardinal> getCardinal() {
        return cardinal;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(streetType).append(" ").append(primaryNumber);
        sb.append(" # ").append(secondaryNumber);
        cardinal.ifPresent(c -> sb.append(" ").append(c));
        buildingNumber.ifPresent(b -> sb.append(" - ").append(b));
        return sb.toString();
    }
}

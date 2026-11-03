package co.medellin.transit.data;

/**
 * Represents a walking link between two places (stops or stations).
 * Parsed from walk_links.csv.
 */
public record WalkLink(
    int placeA,
    String nameA,
    int placeB,
    String nameB,
    String kind,
    double straightM,
    int walkSec
) {
    private static final java.util.Set<String> VALID_KINDS = java.util.Set.of(
        "stop-to-stop", "stop-to-station", "station-to-station"
    );
    
    /**
     * Validates the walk link data.
     * @throws IllegalArgumentException if validation fails
     */
    public WalkLink {
        if (placeA < 0) {
            throw new IllegalArgumentException("Place A ID must be >= 0, got: " + placeA);
        }
        if (placeB < 0) {
            throw new IllegalArgumentException("Place B ID must be >= 0, got: " + placeB);
        }
        // Empty names are allowed - some walk links don't have named places
        if (nameA == null) {
            nameA = "";
        }
        if (nameB == null) {
            nameB = "";
        }
        if (!VALID_KINDS.contains(kind)) {
            throw new IllegalArgumentException(
                "Kind must be one of " + VALID_KINDS + ", got: " + kind
            );
        }
        if (straightM <= 0) {
            throw new IllegalArgumentException("Straight distance must be > 0, got: " + straightM);
        }
        if (walkSec <= 0) {
            throw new IllegalArgumentException("Walk seconds must be > 0, got: " + walkSec);
        }
    }
}

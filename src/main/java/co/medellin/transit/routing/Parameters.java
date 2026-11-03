package co.medellin.transit.routing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration parameters for the routing engine.
 * Loads values from config.properties with fallback to hardcoded defaults.
 * 
 * <p>Key parameters:</p>
 * <ul>
 *   <li>accessRadiusMeters - maximum walking distance from origin/destination to transit stops</li>
 *   <li>boardingPenaltySec - penalty added to discourage excessive transfers</li>
 *   <li>walkPenaltyMultiplier - makes walking "feel" longer in route optimization</li>
 * </ul>
 */
public class Parameters {
    private static final Logger LOGGER = Logger.getLogger(Parameters.class.getName());
    
    // Hardcoded defaults (fallback if config.properties is missing or incomplete)
    private static final double DEFAULT_ACCESS_RADIUS_METERS = 500.0;
    private static final int DEFAULT_BOARDING_PENALTY_SEC = 120;
    private static final double DEFAULT_WALK_PENALTY_MULTIPLIER = 1.5;
    private static final int DEFAULT_WALK_PENALTY_CONSTANT = 30;
    private static final double DEFAULT_WALKING_SPEED_MPS = 1.25;
    private static final double DEFAULT_WALK_STREET_FACTOR = 1.3;
    
    private final double accessRadiusMeters;
    private final int boardingPenaltySec;
    private final double walkPenaltyMultiplier;
    private final int walkPenaltyConstant;
    private final double walkingSpeedMps;
    private final double walkStreetFactor;
    
    /**
     * Creates a Parameters instance by loading from config.properties.
     * Falls back to hardcoded defaults if properties are missing or invalid.
     */
    public Parameters() {
        Properties props = loadProperties();
        
        this.accessRadiusMeters = getDoubleProperty(props, "routing.accessRadiusMeters", DEFAULT_ACCESS_RADIUS_METERS);
        this.boardingPenaltySec = getIntProperty(props, "routing.boardingPenaltySec", DEFAULT_BOARDING_PENALTY_SEC);
        this.walkPenaltyMultiplier = getDoubleProperty(props, "routing.walkPenaltyMultiplier", DEFAULT_WALK_PENALTY_MULTIPLIER);
        this.walkPenaltyConstant = getIntProperty(props, "routing.walkPenaltyConstant", DEFAULT_WALK_PENALTY_CONSTANT);
        this.walkingSpeedMps = getDoubleProperty(props, "routing.walkingSpeedMps", DEFAULT_WALKING_SPEED_MPS);
        this.walkStreetFactor = getDoubleProperty(props, "routing.walkStreetFactor", DEFAULT_WALK_STREET_FACTOR);
        
        LOGGER.log(Level.INFO, "Routing parameters loaded: accessRadius={0}m, boardingPenalty={1}s, walkMultiplier={2}", 
                   new Object[]{accessRadiusMeters, boardingPenaltySec, walkPenaltyMultiplier});
    }
    
    /**
     * Creates a Parameters instance with explicit values (for testing).
     * 
     * @param accessRadiusMeters maximum walking distance to transit stops
     * @param boardingPenaltySec penalty for boarding a vehicle
     * @param walkPenaltyMultiplier multiplier for walk time in perceived cost
     */
    public Parameters(double accessRadiusMeters, int boardingPenaltySec, double walkPenaltyMultiplier) {
        this.accessRadiusMeters = accessRadiusMeters;
        this.boardingPenaltySec = boardingPenaltySec;
        this.walkPenaltyMultiplier = walkPenaltyMultiplier;
        this.walkPenaltyConstant = DEFAULT_WALK_PENALTY_CONSTANT;
        this.walkingSpeedMps = DEFAULT_WALKING_SPEED_MPS;
        this.walkStreetFactor = DEFAULT_WALK_STREET_FACTOR;
    }
    
    /**
     * Gets the maximum walking distance from origin/destination to transit stops (meters).
     * 
     * @return access radius in meters (default: 500)
     */
    public double getAccessRadiusMeters() {
        return accessRadiusMeters;
    }
    
    /**
     * Gets the boarding penalty applied to discourage excessive transfers (seconds).
     * This is added to the perceived cost but not the real duration.
     * 
     * @return boarding penalty in seconds (default: 120)
     */
    public int getBoardingPenaltySec() {
        return boardingPenaltySec;
    }
    
    /**
     * Gets the walk penalty multiplier that makes walking "feel" longer.
     * Applied to walking edges in perceived cost calculation.
     * 
     * @return walk penalty multiplier (default: 1.5)
     */
    public double getWalkPenaltyMultiplier() {
        return walkPenaltyMultiplier;
    }
    
    /**
     * Gets the constant penalty added to walking edges (seconds).
     * 
     * @return walk penalty constant in seconds (default: 30)
     */
    public int getWalkPenaltyConstant() {
        return walkPenaltyConstant;
    }
    
    /**
     * Gets the walking speed for distance/time calculations (meters per second).
     * 
     * @return walking speed in m/s (default: 1.25 = 4.5 km/h)
     */
    public double getWalkingSpeedMps() {
        return walkingSpeedMps;
    }
    
    /**
     * Gets the street factor for walking distance estimation.
     * Accounts for the fact that actual street distance is longer than straight-line distance.
     * 
     * @return street factor (default: 1.3)
     */
    public double getWalkStreetFactor() {
        return walkStreetFactor;
    }
    
    /**
     * Loads properties from config.properties file.
     * Returns empty Properties if file cannot be loaded.
     * 
     * @return loaded properties or empty Properties on error
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                LOGGER.warning("config.properties not found, using hardcoded defaults");
                return props;
            }
            props.load(input);
            LOGGER.info("Loaded config.properties");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error loading config.properties, using hardcoded defaults", e);
        }
        return props;
    }
    
    /**
     * Gets a double property value with fallback to default.
     * 
     * @param props properties object
     * @param key property key
     * @param defaultValue fallback value
     * @return property value or default if missing/invalid
     */
    private double getDoubleProperty(Properties props, String key, double defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            LOGGER.log(Level.FINE, "Property {0} not found, using default: {1}", new Object[]{key, defaultValue});
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid value for {0}: {1}, using default: {2}", 
                      new Object[]{key, value, defaultValue});
            return defaultValue;
        }
    }
    
    /**
     * Gets an integer property value with fallback to default.
     * 
     * @param props properties object
     * @param key property key
     * @param defaultValue fallback value
     * @return property value or default if missing/invalid
     */
    private int getIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            LOGGER.log(Level.FINE, "Property {0} not found, using default: {1}", new Object[]{key, defaultValue});
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid value for {0}: {1}, using default: {2}", 
                      new Object[]{key, value, defaultValue});
            return defaultValue;
        }
    }
}

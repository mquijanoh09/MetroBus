package co.medellin.transit.routing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Parameters configuration class.
 * Validates loading from config.properties and fallback to defaults.
 */
class ParametersTest {
    
    @Test
    void testDefaultConstructorLoadsFromConfigProperties() {
        // When: loading parameters with default constructor
        Parameters params = new Parameters();
        
        // Then: values should match config.properties
        assertEquals(500.0, params.getAccessRadiusMeters(), 0.001, 
                    "Access radius should be loaded from config");
        assertEquals(120, params.getBoardingPenaltySec(), 
                    "Boarding penalty should be loaded from config");
        assertEquals(1.5, params.getWalkPenaltyMultiplier(), 0.001, 
                    "Walk penalty multiplier should be loaded from config");
        assertEquals(30, params.getWalkPenaltyConstant(), 
                    "Walk penalty constant should be loaded from config");
        assertEquals(1.25, params.getWalkingSpeedMps(), 0.001, 
                    "Walking speed should be loaded from config");
        assertEquals(1.3, params.getWalkStreetFactor(), 0.001, 
                    "Walk street factor should be loaded from config");
    }
    
    @Test
    void testExplicitConstructor() {
        // When: creating parameters with explicit values
        Parameters params = new Parameters(600.0, 150, 2.0);
        
        // Then: values should match constructor arguments
        assertEquals(600.0, params.getAccessRadiusMeters(), 0.001);
        assertEquals(150, params.getBoardingPenaltySec());
        assertEquals(2.0, params.getWalkPenaltyMultiplier(), 0.001);
        
        // And: other values should have defaults
        assertEquals(30, params.getWalkPenaltyConstant());
        assertEquals(1.25, params.getWalkingSpeedMps(), 0.001);
        assertEquals(1.3, params.getWalkStreetFactor(), 0.001);
    }
    
    @Test
    void testAccessRadiusGetter() {
        // Given: parameters with specific access radius
        Parameters params = new Parameters(750.0, 120, 1.5);
        
        // When: getting access radius
        double radius = params.getAccessRadiusMeters();
        
        // Then: should return correct value
        assertEquals(750.0, radius, 0.001);
    }
    
    @Test
    void testBoardingPenaltyGetter() {
        // Given: parameters with specific boarding penalty
        Parameters params = new Parameters(500.0, 180, 1.5);
        
        // When: getting boarding penalty
        int penalty = params.getBoardingPenaltySec();
        
        // Then: should return correct value
        assertEquals(180, penalty);
    }
    
    @Test
    void testWalkPenaltyMultiplierGetter() {
        // Given: parameters with specific walk multiplier
        Parameters params = new Parameters(500.0, 120, 2.5);
        
        // When: getting walk penalty multiplier
        double multiplier = params.getWalkPenaltyMultiplier();
        
        // Then: should return correct value
        assertEquals(2.5, multiplier, 0.001);
    }
    
    @Test
    void testWalkPenaltyConstantGetter() {
        // Given: parameters instance
        Parameters params = new Parameters();
        
        // When: getting walk penalty constant
        int constant = params.getWalkPenaltyConstant();
        
        // Then: should return default value
        assertEquals(30, constant);
    }
    
    @Test
    void testWalkingSpeedGetter() {
        // Given: parameters instance
        Parameters params = new Parameters();
        
        // When: getting walking speed
        double speed = params.getWalkingSpeedMps();
        
        // Then: should return correct value (4.5 km/h = 1.25 m/s)
        assertEquals(1.25, speed, 0.001);
    }
    
    @Test
    void testWalkStreetFactorGetter() {
        // Given: parameters instance
        Parameters params = new Parameters();
        
        // When: getting walk street factor
        double factor = params.getWalkStreetFactor();
        
        // Then: should return correct value
        assertEquals(1.3, factor, 0.001);
    }
    
    @Test
    void testParametersArePositive() {
        // Given: default parameters
        Parameters params = new Parameters();
        
        // Then: all values should be positive
        assertTrue(params.getAccessRadiusMeters() > 0, 
                  "Access radius must be positive");
        assertTrue(params.getBoardingPenaltySec() > 0, 
                  "Boarding penalty must be positive");
        assertTrue(params.getWalkPenaltyMultiplier() > 0, 
                  "Walk penalty multiplier must be positive");
        assertTrue(params.getWalkPenaltyConstant() >= 0, 
                  "Walk penalty constant must be non-negative");
        assertTrue(params.getWalkingSpeedMps() > 0, 
                  "Walking speed must be positive");
        assertTrue(params.getWalkStreetFactor() >= 1.0, 
                  "Walk street factor must be >= 1.0");
    }
    
    @Test
    void testDefaultAccessRadiusFallback() {
        // When: parameters are created (config.properties may be missing)
        Parameters params = new Parameters();
        
        // Then: access radius should have a reasonable default
        double radius = params.getAccessRadiusMeters();
        assertTrue(radius >= 100 && radius <= 1000, 
                  "Access radius should be in reasonable range (100-1000m)");
    }
    
    @Test
    void testDefaultBoardingPenaltyFallback() {
        // When: parameters are created (config.properties may be missing)
        Parameters params = new Parameters();
        
        // Then: boarding penalty should have a reasonable default
        int penalty = params.getBoardingPenaltySec();
        assertTrue(penalty >= 60 && penalty <= 300, 
                  "Boarding penalty should be in reasonable range (60-300s)");
    }
    
    @Test
    void testDefaultWalkMultiplierFallback() {
        // When: parameters are created (config.properties may be missing)
        Parameters params = new Parameters();
        
        // Then: walk multiplier should have a reasonable default
        double multiplier = params.getWalkPenaltyMultiplier();
        assertTrue(multiplier >= 1.0 && multiplier <= 3.0, 
                  "Walk penalty multiplier should be in reasonable range (1.0-3.0)");
    }
    
    @Test
    void testParametersConsistency() {
        // Given: two parameter instances created separately
        Parameters params1 = new Parameters();
        Parameters params2 = new Parameters();
        
        // Then: they should have identical values
        assertEquals(params1.getAccessRadiusMeters(), params2.getAccessRadiusMeters(), 0.001,
                    "Access radius should be consistent across instances");
        assertEquals(params1.getBoardingPenaltySec(), params2.getBoardingPenaltySec(),
                    "Boarding penalty should be consistent across instances");
        assertEquals(params1.getWalkPenaltyMultiplier(), params2.getWalkPenaltyMultiplier(), 0.001,
                    "Walk penalty multiplier should be consistent across instances");
    }
    
    @Test
    void testExplicitConstructorAllowsCustomValues() {
        // When: creating multiple parameter instances with different values
        Parameters params1 = new Parameters(400.0, 100, 1.2);
        Parameters params2 = new Parameters(600.0, 140, 1.8);
        Parameters params3 = new Parameters(800.0, 180, 2.5);
        
        // Then: each should maintain its own values
        assertEquals(400.0, params1.getAccessRadiusMeters(), 0.001);
        assertEquals(100, params1.getBoardingPenaltySec());
        assertEquals(1.2, params1.getWalkPenaltyMultiplier(), 0.001);
        
        assertEquals(600.0, params2.getAccessRadiusMeters(), 0.001);
        assertEquals(140, params2.getBoardingPenaltySec());
        assertEquals(1.8, params2.getWalkPenaltyMultiplier(), 0.001);
        
        assertEquals(800.0, params3.getAccessRadiusMeters(), 0.001);
        assertEquals(180, params3.getBoardingPenaltySec());
        assertEquals(2.5, params3.getWalkPenaltyMultiplier(), 0.001);
    }
    
    @Test
    void testReasonableAccessRadiusForTransit() {
        // Given: default parameters
        Parameters params = new Parameters();
        
        // Then: access radius should be reasonable for walking to transit
        // 500m is about 6-7 minutes walk at typical walking speed
        double radius = params.getAccessRadiusMeters();
        assertTrue(radius >= 300 && radius <= 1000,
                  "Access radius should be reasonable for walking to transit (300-1000m)");
    }
    
    @Test
    void testBoardingPenaltyDiscouragesTransfers() {
        // Given: default parameters
        Parameters params = new Parameters();
        
        // Then: boarding penalty should be significant enough to discourage excessive transfers
        // 120 seconds = 2 minutes, enough to prefer direct routes when reasonable
        int penalty = params.getBoardingPenaltySec();
        assertTrue(penalty >= 60,
                  "Boarding penalty should be significant (>= 60s) to discourage transfers");
    }
    
    @Test
    void testWalkPenaltyMakesWalkingLessAttractive() {
        // Given: default parameters
        Parameters params = new Parameters();
        
        // Then: walk penalty multiplier should be > 1.0 to make walking less attractive
        double multiplier = params.getWalkPenaltyMultiplier();
        assertTrue(multiplier > 1.0,
                  "Walk penalty multiplier should be > 1.0 to discourage long walks");
        assertTrue(multiplier < 3.0,
                  "Walk penalty multiplier should be < 3.0 to keep walking reasonable");
    }
    
    @Test
    void testWalkingSpeedIsRealistic() {
        // Given: default parameters
        Parameters params = new Parameters();
        
        // Then: walking speed should be realistic for urban environment
        // 1.25 m/s = 4.5 km/h (typical comfortable walking speed)
        double speed = params.getWalkingSpeedMps();
        assertTrue(speed >= 1.0 && speed <= 1.5,
                  "Walking speed should be realistic (1.0-1.5 m/s)");
        
        // Verify conversion: 1.25 m/s * 3.6 = 4.5 km/h
        double kmh = speed * 3.6;
        assertEquals(4.5, kmh, 0.1, "Walking speed should be approximately 4.5 km/h");
    }
    
    @Test
    void testStreetFactorAccountsForNonStraightPaths() {
        // Given: default parameters
        Parameters params = new Parameters();
        
        // Then: street factor should account for actual street paths being longer
        // Factor of 1.3 means streets are ~30% longer than straight-line distance
        double factor = params.getWalkStreetFactor();
        assertTrue(factor >= 1.0,
                  "Street factor should be >= 1.0 (streets can't be shorter than straight line)");
        assertTrue(factor <= 2.0,
                  "Street factor should be <= 2.0 (reasonable for urban grid)");
    }
}

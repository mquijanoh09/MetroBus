package co.medellin.transit.fares;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration for fare calculation.
 * Loads fare values and integration rules from config.properties.
 */
public class FareConfig {
    private final int metroFareCOP;
    private final int busFareCOP;
    private final int integratedFareCOP;
    private final int integrationWindowMinutes;

    /**
     * Creates a FareConfig with specified values.
     *
     * @param metroFareCOP metro fare in Colombian Pesos
     * @param busFareCOP bus fare in Colombian Pesos
     * @param integratedFareCOP integrated fare for metro+bus in Colombian Pesos
     * @param integrationWindowMinutes time window for integration in minutes
     */
    public FareConfig(int metroFareCOP, int busFareCOP, int integratedFareCOP, int integrationWindowMinutes) {
        this.metroFareCOP = metroFareCOP;
        this.busFareCOP = busFareCOP;
        this.integratedFareCOP = integratedFareCOP;
        this.integrationWindowMinutes = integrationWindowMinutes;
    }

    /**
     * Loads fare configuration from config.properties.
     *
     * @return FareConfig with values from properties file
     * @throws IOException if properties file cannot be read
     */
    public static FareConfig fromProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream is = FareConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is == null) {
                throw new IOException("config.properties not found in classpath");
            }
            props.load(is);
        }

        int metroFare = Integer.parseInt(props.getProperty("fares.metroFare", "3820"));
        int busFare = Integer.parseInt(props.getProperty("fares.busFare", "3800"));
        int integratedFare = Integer.parseInt(props.getProperty("fares.integratedFare", "4715"));
        int integrationWindow = Integer.parseInt(props.getProperty("fares.integrationWindowMinutes", "90"));

        return new FareConfig(metroFare, busFare, integratedFare, integrationWindow);
    }

    /**
     * Creates a default FareConfig with hardcoded values (January 2026 rates).
     *
     * @return FareConfig with default values
     */
    public static FareConfig defaults() {
        return new FareConfig(3820, 3800, 4715, 90);
    }

    public int getMetroFareCOP() {
        return metroFareCOP;
    }

    public int getBusFareCOP() {
        return busFareCOP;
    }

    public int getIntegratedFareCOP() {
        return integratedFareCOP;
    }

    public int getIntegrationWindowMinutes() {
        return integrationWindowMinutes;
    }

    @Override
    public String toString() {
        return "FareConfig{" +
                "metroFareCOP=" + metroFareCOP +
                ", busFareCOP=" + busFareCOP +
                ", integratedFareCOP=" + integratedFareCOP +
                ", integrationWindowMinutes=" + integrationWindowMinutes +
                '}';
    }
}

package co.medellin.transit.ui;

/**
 * Displays the welcome screen for the Medellín Transit Routing System.
 * 
 * <p>This class provides a user-friendly entry point with ASCII art
 * and a welcome message in Spanish.
 */
public class HomeScreen {
    
    /**
     * Displays the welcome banner to the console.
     * 
     * <p>The banner includes:
     * <ul>
     *   <li>ASCII box drawing for visual appeal</li>
     *   <li>Application title in Spanish</li>
     *   <li>Brief usage instructions</li>
     * </ul>
     */
    public void display() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║                                        ║");
        System.out.println("║   Sistema de Rutas Medellín Metro     ║");
        System.out.println("║                                        ║");
        System.out.println("║   Bienvenido al sistema de             ║");
        System.out.println("║   planificación de rutas de            ║");
        System.out.println("║   transporte público de Medellín       ║");
        System.out.println("║                                        ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println();
    }
}

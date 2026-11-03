package co.medellin.transit.ui.gui;

/**
 * Help text shown to the user, in Spanish.
 */
final class HelpText {

    static final String ADDRESSES = String.join("\n",
            "Escriba las direcciones como se usan en Medellín:",
            "",
            "   Calle 10 # 43A - 20",
            "   Carrera 43A # 1 Sur - 100",
            "   CL 50 53-48",
            "   CR 70 x CL 48   (cruce de dos vías)",
            "",
            "Tipos de vía válidos: Calle o CL, Carrera, CR o KR,",
            "Diagonal o DG, Transversal o TV, y Circular.",
            "",
            "La aplicación busca la combinación de bus, Metro, Metroplús,",
            "tranvía y metrocable que llega más rápido, y calcula la tarifa",
            "con la tarjeta Cívica.");

    private HelpText() {
    }
}

package co.medellin.transit.ui.gui.model;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Number formats the Colombian way (DESIGN.md, section 7).
 */
public final class Format {

    private static final Locale CO = Locale.forLanguageTag("es-CO");

    private Format() {
    }

    /** 3820 becomes "$3.820". */
    public static String pesos(int amount) {
        NumberFormat format = NumberFormat.getIntegerInstance(CO);
        format.setGroupingUsed(true);
        return "$" + format.format(amount);
    }

    /** Minutes rounded up; past an hour, "1 h 05 min". */
    public static String duration(int seconds) {
        int minutes = (int) Math.ceil(seconds / 60.0);
        if (minutes < 60) {
            return minutes + " min";
        }
        return String.format("%d h %02d min", minutes / 60, minutes % 60);
    }

    /** Spoken form for screen readers: "22 minutos". */
    public static String durationSpoken(int seconds) {
        int minutes = (int) Math.ceil(seconds / 60.0);
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minuto" : " minutos");
        }
        int m = minutes % 60;
        return (minutes / 60) + (minutes / 60 == 1 ? " hora" : " horas") + (m > 0 ? " y " + m + " minutos" : "");
    }

    /** 349.4 becomes "349 m"; 1500 becomes "1,5 km". */
    public static String meters(double meters) {
        long rounded = Math.round(meters);
        if (rounded < 1000) {
            return rounded + " m";
        }
        return String.format(CO, "%.1f km", rounded / 1000.0);
    }

    /** "sin transbordos", "1 transbordo", "2 transbordos". */
    public static String transfers(int count) {
        if (count == 0) {
            return "sin transbordos";
        }
        return count + (count == 1 ? " transbordo" : " transbordos");
    }

    /** Place names come from the dataset as "..., Medellin"; drop the city suffix. */
    public static String place(String name) {
        if (name == null) {
            return "";
        }
        return name.replaceFirst(",\\s*Medell[ií]n\\s*$", "").trim();
    }
}

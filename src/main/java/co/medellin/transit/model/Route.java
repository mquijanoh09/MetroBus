package co.medellin.transit.model;

import java.util.Objects;

/**
 * Immutable representation of a transit route.
 * Can represent either a bus route or a metro line.
 */
public final class Route {
    private final int id;           // id_ruta from JSON (internal ID)
    private final String codigo;    // user-visible route code (e.g., "263")
    private final String nombre;    // route name (e.g., "12 de Octubre - Calle 90")
    private final String tipo;      // route type for fares (e.g., "Directa Centro", "Integrada")

    /**
     * Creates a new route.
     *
     * @param id unique internal identifier (id_ruta)
     * @param codigo user-visible route code
     * @param nombre route name
     * @param tipo route type (used for fare calculation)
     * @throws NullPointerException if any String parameter is null
     */
    public Route(int id, String codigo, String nombre, String tipo) {
        this.id = id;
        this.codigo = Objects.requireNonNull(codigo, "codigo cannot be null");
        this.nombre = Objects.requireNonNull(nombre, "nombre cannot be null");
        this.tipo = Objects.requireNonNull(tipo, "tipo cannot be null");
    }

    public int getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return id == route.id &&
               Objects.equals(codigo, route.codigo) &&
               Objects.equals(nombre, route.nombre) &&
               Objects.equals(tipo, route.tipo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, codigo, nombre, tipo);
    }

    @Override
    public String toString() {
        return "Route{" +
               "id=" + id +
               ", codigo='" + codigo + '\'' +
               ", nombre='" + nombre + '\'' +
               ", tipo='" + tipo + '\'' +
               '}';
    }
}

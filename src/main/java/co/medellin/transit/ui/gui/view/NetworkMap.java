package co.medellin.transit.ui.gui.view;

import co.medellin.transit.model.Coordinate;
import co.medellin.transit.ui.PlanResult;
import co.medellin.transit.ui.gui.Theme;
import co.medellin.transit.ui.gui.kit.Paint;
import co.medellin.transit.ui.gui.model.Format;
import co.medellin.transit.ui.gui.model.Leg;
import co.medellin.transit.ui.gui.model.LegBuilder;
import co.medellin.transit.ui.gui.model.NetworkLines;
import co.medellin.transit.ui.gui.model.TransitMode;
import co.medellin.transit.ui.gui.state.AppState;
import co.medellin.transit.ui.gui.state.ScreenLayout;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Schematic of the real network drawn from station coordinates
 * (DESIGN.md 5.1 and 5.3). With a result, the trip is drawn on top and the
 * rest of the network fades; the view glides to the trip in 250 ms.
 */
public final class NetworkMap extends co.medellin.transit.ui.gui.kit.Canvas {

    @Override
    protected javax.accessibility.AccessibleRole role() {
        return javax.accessibility.AccessibleRole.CANVAS;
    }

    /** Projection: longitude shrinks by cos(6.25°) so the valley is not stretched. */
    private static final double LON_SCALE = Math.cos(Math.toRadians(6.25));
    private static final int ANIMATION_MS = 250;

    private NetworkLines network = NetworkLines.empty();
    private PlanResult result;
    private List<Leg> legs = List.of();
    private ScreenLayout layout = ScreenLayout.WIDE;

    // View box in projected units (x = lon * LON_SCALE, y = -lat)
    private Rectangle2D view;
    private Rectangle2D from;
    private Rectangle2D to;
    private long animStart;
    private final Timer timer = new Timer(16, e -> tick());

    public NetworkMap() {
        setOpaque(true);
        getAccessibleContext().setAccessibleName("Mapa de la red de transporte");
        getAccessibleContext().setAccessibleDescription(
                "Esquema de las líneas de Metro, Metroplús, Metrocable y tranvía de Medellín.");
    }

    public void render(AppState s) {
        boolean changed = false;
        if (s.network() != network) {
            network = s.network();
            changed = true;
        }
        PlanResult r = s.showsResult() ? s.result() : null;
        if (r != result) {
            result = r;
            legs = r == null ? List.of() : LegBuilder.legs(r.getTrip());
            getAccessibleContext().setAccessibleDescription(r == null
                    ? "Esquema de las líneas de Metro, Metroplús, Metrocable y tranvía de Medellín."
                    : describeTrip());
            changed = true;
        }
        layout = s.layout();
        if (changed) {
            Rectangle2D target = targetBox();
            if (view == null || target == null || isShowing() == false) {
                view = target;
            } else {
                from = view;
                to = target;
                animStart = System.nanoTime();
                timer.start();
            }
            repaint();
        }
    }

    private String describeTrip() {
        StringBuilder sb = new StringBuilder("Ruta");
        for (Leg leg : legs) {
            if (!leg.isWalk()) {
                sb.append(" en ").append(leg.mode().label());
                if (leg.mode() != TransitMode.BUS) {
                    sb.append(" línea ").append(leg.routeCode());
                }
                sb.append(" de ").append(Format.place(leg.fromName())).append(" a ").append(Format.place(leg.toName()))
                        .append(",");
            }
        }
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }
        return legs.stream().allMatch(Leg::isWalk) ? "Ruta a pie." : sb + ".";
    }

    private void tick() {
        double t = Math.min(1.0, (System.nanoTime() - animStart) / 1e6 / ANIMATION_MS);
        double e = 1 - Math.pow(1 - t, 3); // ease out
        view = new Rectangle2D.Double(
                lerp(from.getX(), to.getX(), e), lerp(from.getY(), to.getY(), e),
                lerp(from.getWidth(), to.getWidth(), e), lerp(from.getHeight(), to.getHeight(), e));
        if (t >= 1.0) {
            timer.stop();
            view = to;
        }
        repaint();
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Box to show: whole network with 6 % margin, or the trip with 15 %. */
    private Rectangle2D targetBox() {
        List<Point2D> pts = new ArrayList<>();
        if (result != null) {
            pts.add(project(result.getOrigin().getCoord()));
            pts.add(project(result.getDestination().getCoord()));
            for (Leg leg : legs) {
                if (leg.fromCoord() != null) {
                    pts.add(project(leg.fromCoord()));
                }
                if (leg.toCoord() != null) {
                    pts.add(project(leg.toCoord()));
                }
            }
            return box(pts, 0.15, 0.01);
        }
        for (NetworkLines.Line line : network.lines()) {
            for (NetworkLines.Station st : line.stations()) {
                pts.add(project(st.coord()));
            }
        }
        return pts.isEmpty() ? null : box(pts, 0.06, 0.02);
    }

    private static Rectangle2D box(List<Point2D> pts, double margin, double minSize) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Point2D p : pts) {
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }
        double w = Math.max(maxX - minX, minSize);
        double h = Math.max(maxY - minY, minSize);
        double cx = (minX + maxX) / 2;
        double cy = (minY + maxY) / 2;
        w *= 1 + 2 * margin;
        h *= 1 + 2 * margin;
        return new Rectangle2D.Double(cx - w / 2, cy - h / 2, w, h);
    }

    private static Point2D project(Coordinate c) {
        return new Point2D.Double(c.getLon() * LON_SCALE, -c.getLat());
    }

    /** Projected point to screen, fitting the view box into the component. */
    private Point2D screen(Coordinate c, Rectangle area) {
        Point2D p = project(c);
        double scale = Math.min(area.width / view.getWidth(), area.height / view.getHeight());
        double ox = area.x + (area.width - view.getWidth() * scale) / 2;
        double oy = area.y + (area.height - view.getHeight() * scale) / 2;
        return new Point2D.Double(ox + (p.getX() - view.getX()) * scale, oy + (p.getY() - view.getY()) * scale);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Paint.smooth(g);
        int w = getWidth();
        int h = getHeight();
        g2.setColor(Theme.PAPER);
        g2.fillRect(0, 0, w, h);
        if (view == null) {
            view = targetBox();
        }
        if (view == null) {
            g2.dispose();
            return;
        }
        int pad = MainLayout.margin(layout);
        g2.setFont(Theme.hint());
        int rows = layout == ScreenLayout.COMPACT ? 0 : legendRows(g2.getFontMetrics(), w - 2 * pad);
        int legendH = rows == 0 ? 0 : 24 * rows + 32;
        Rectangle area = new Rectangle(pad, pad, w - 2 * pad, h - 2 * pad - legendH);

        boolean faded = result != null;
        Graphics2D clipped = (Graphics2D) g2.create();
        clipped.clipRect(0, 0, w, h - legendH - pad / 2);
        drawNetwork(clipped, area, faded);
        if (result != null) {
            drawTrip(clipped, area);
        } else {
            drawTerminalLabels(clipped, area);
        }
        clipped.dispose();
        if (legendH > 0) {
            drawLegend(g2, pad, h - pad - 24 * rows + 4, w - 2 * pad);
        }
        g2.dispose();
    }

    private void drawNetwork(Graphics2D g2, Rectangle area, boolean faded) {
        Graphics2D g = (Graphics2D) g2.create();
        if (faded) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        }
        Stroke line = new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        for (NetworkLines.Line l : network.lines()) {
            g.setColor(Theme.stroke(l.mode()));
            g.setStroke(line);
            g.draw(path(l.stations(), area));
        }
        g.setStroke(new BasicStroke(1.5f));
        for (NetworkLines.Line l : network.lines()) {
            for (NetworkLines.Station st : l.stations()) {
                Point2D p = screen(st.coord(), area);
                dot(g, p, 3.5, Theme.SURFACE, Theme.stroke(l.mode()));
            }
        }
        g.dispose();
    }

    private Path2D path(List<NetworkLines.Station> stations, Rectangle area) {
        Path2D path = new Path2D.Double();
        for (int i = 0; i < stations.size(); i++) {
            Point2D p = screen(stations.get(i).coord(), area);
            if (i == 0) {
                path.moveTo(p.getX(), p.getY());
            } else {
                path.lineTo(p.getX(), p.getY());
            }
        }
        return path;
    }

    private static void dot(Graphics2D g, Point2D p, double r, Color fill, Color border) {
        java.awt.geom.Ellipse2D e = new java.awt.geom.Ellipse2D.Double(p.getX() - r, p.getY() - r, 2 * r, 2 * r);
        g.setColor(fill);
        g.fill(e);
        g.setColor(border);
        g.draw(e);
    }

    private void drawTrip(Graphics2D g2, Rectangle area) {
        Coordinate prev = result.getOrigin().getCoord();
        for (Leg leg : legs) {
            if (leg.isWalk()) {
                walk(g2, area, prev, leg.toCoord());
                prev = leg.toCoord();
                continue;
            }
            // The backend may board right at the origin's nearest stop without a WALK step
            walk(g2, area, prev, leg.fromCoord());
            g2.setColor(Theme.stroke(leg.mode()));
            g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            List<NetworkLines.Station> stretch = leg.mode() == TransitMode.BUS ? List.of()
                    : network.stretch(leg.routeCode(), leg.fromCoord(), leg.toCoord());
            Path2D path = new Path2D.Double();
            Point2D start = screen(leg.fromCoord(), area);
            path.moveTo(start.getX(), start.getY());
            for (NetworkLines.Station st : stretch) {
                Point2D p = screen(st.coord(), area);
                path.lineTo(p.getX(), p.getY());
            }
            Point2D end = screen(leg.toCoord(), area);
            path.lineTo(end.getX(), end.getY());
            g2.draw(path);
            g2.setStroke(new BasicStroke(2f));
            dot(g2, start, 5, Theme.SURFACE, Theme.stroke(leg.mode()));
            dot(g2, end, 5, Theme.SURFACE, Theme.stroke(leg.mode()));
            prev = leg.toCoord();
        }
        walk(g2, area, prev, result.getDestination().getCoord());
        Point2D o = screen(result.getOrigin().getCoord(), area);
        Point2D d = screen(result.getDestination().getCoord(), area);
        g2.setStroke(new BasicStroke(2f));
        dot(g2, o, 6, Theme.INK, Theme.INK);
        dot(g2, d, 6, Theme.SURFACE, Theme.INK);
        g2.setFont(Theme.label());
        tag(g2, "Origen", o);
        tag(g2, "Destino", d);
    }

    /** Dotted walking segment; skipped when both ends are the same point. */
    private void walk(Graphics2D g2, Rectangle area, Coordinate a, Coordinate b) {
        if (a == null || b == null || a.equals(b)) {
            return;
        }
        Point2D pa = screen(a, area);
        Point2D pb = screen(b, area);
        if (pa.distance(pb) < 2) {
            return;
        }
        g2.setColor(Theme.WALK);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[]{0.1f, 7f}, 0f));
        g2.draw(new java.awt.geom.Line2D.Double(pa, pb));
    }

    private void tag(Graphics2D g2, String text, Point2D p) {
        FontMetrics fm = g2.getFontMetrics();
        int x = (int) p.getX() + 10;
        int y = (int) p.getY() - 8;
        int tw = fm.stringWidth(text);
        g2.setColor(Theme.SURFACE);
        g2.fillRect(x - 3, y - fm.getAscent(), tw + 6, fm.getHeight());
        g2.setColor(Theme.INK);
        g2.drawString(text, x, y);
    }

    /** Terminal station names; on collision, the line with fewer stations loses its label. */
    private void drawTerminalLabels(Graphics2D g2, Rectangle area) {
        if (layout == ScreenLayout.COMPACT && getHeight() < 260) {
            return;
        }
        g2.setFont(Theme.hint());
        FontMetrics fm = g2.getFontMetrics();
        List<NetworkLines.Line> lines = new ArrayList<>(network.lines());
        lines.sort(Comparator.comparingInt((NetworkLines.Line l) -> l.stations().size()).reversed());
        List<Rectangle> placed = new ArrayList<>();
        List<String> done = new ArrayList<>();
        for (NetworkLines.Line l : lines) {
            for (NetworkLines.Station st : List.of(l.first(), l.last())) {
                if (done.contains(st.name())) {
                    continue;
                }
                Point2D p = screen(st.coord(), area);
                int tw = fm.stringWidth(st.name());
                boolean rightSide = p.getX() + 8 + tw < getWidth() - 4;
                int x = rightSide ? (int) p.getX() + 8 : (int) p.getX() - 8 - tw;
                int y = (int) p.getY() + fm.getAscent() / 2 - 1;
                Rectangle r = new Rectangle(x - 2, y - fm.getAscent(), tw + 4, fm.getHeight());
                if (placed.stream().anyMatch(r::intersects) || r.x < 0) {
                    continue;
                }
                placed.add(r);
                done.add(st.name());
                g2.setColor(Theme.PAPER);
                g2.fill(r);
                g2.setColor(Theme.INK_2);
                g2.drawString(st.name(), x, y);
            }
        }
    }

    private List<TransitMode> legendModes() {
        List<TransitMode> modes = new ArrayList<>(List.of(TransitMode.METRO, TransitMode.METROPLUS,
                TransitMode.CABLE, TransitMode.TRAM, TransitMode.BUS, TransitMode.WALK));
        if (result == null) {
            modes.remove(TransitMode.BUS);
        }
        return modes;
    }

    private static String legendLabel(TransitMode m) {
        return m == TransitMode.WALK ? "Caminar" : m.label();
    }

    /** How many rows the legend needs so no item is cut off. */
    private int legendRows(FontMetrics fm, int width) {
        int rows = 1;
        int cx = 0;
        for (TransitMode m : legendModes()) {
            int item = 28 + fm.stringWidth(legendLabel(m));
            if (cx > 0 && cx + item > width) {
                rows++;
                cx = 0;
            }
            cx += item + Theme.S4;
        }
        return rows;
    }

    private void drawLegend(Graphics2D g2, int x, int y, int width) {
        g2.setFont(Theme.hint());
        FontMetrics fm = g2.getFontMetrics();
        int cx = x;
        for (TransitMode m : legendModes()) {
            int item = 28 + fm.stringWidth(legendLabel(m));
            if (cx > x && cx + item > x + width) {
                cx = x;
                y += 24;
            }
            g2.setColor(Theme.stroke(m));
            if (m == TransitMode.WALK) {
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                        new float[]{0.1f, 6f}, 0f));
            } else {
                g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            g2.drawLine(cx, y, cx + 20, y);
            g2.setColor(Theme.INK_2);
            String label = legendLabel(m);
            g2.drawString(label, cx + 28, y + fm.getAscent() / 2 - 1);
            cx += 28 + fm.stringWidth(label) + Theme.S4;
        }
    }

    /** For tests: the current view box, or null before the network loads. */
    Rectangle2D viewBox() {
        return view;
    }

    boolean showsTrip() {
        return result != null;
    }
}

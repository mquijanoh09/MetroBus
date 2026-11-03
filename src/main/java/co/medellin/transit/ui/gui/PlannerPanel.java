package co.medellin.transit.ui.gui;

import co.medellin.transit.model.Step;
import co.medellin.transit.model.Trip;
import co.medellin.transit.ui.PlanResult;
import co.medellin.transit.ui.TripPlanner;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Trip planner screen: origin and destination fields on the left, and the
 * route found (summary and step by step) on the right.
 */
final class PlannerPanel extends JPanel {

    /** Example trips the user can load with one click. All were checked against the real data. */
    static final String[][] EXAMPLES = {
            {"CL 10 43A-20", "CL 77 65-30", "CL 10 43A-20  >  CL 77 65-30"},
            {"CL 50 53-48", "CR 70 x CL 48", "CL 50 53-48  >  CR 70 x CL 48"},
            {"CR 80 32-15", "CL 51 46-10", "CR 80 32-15  >  CL 51 46-10"},
            {"CL 67 43-26", "CL 71 65-173", "CL 67 43-26  >  CL 71 65-173"},
    };
    private static final String EXAMPLE_PROMPT = "Elegir un ejemplo...";

    private final TripPlanner planner;
    private final JTextField originField = new JTextField();
    private final JTextField destinationField = new JTextField();
    private final JButton searchButton = Theme.primaryButton("Buscar ruta");
    private final JPanel results = new JPanel(new BorderLayout());
    private PlanResult lastResult;

    PlannerPanel(TripPlanner planner, Runnable onHome) {
        super(new BorderLayout(16, 0));
        this.planner = planner;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        add(buildForm(onHome), BorderLayout.WEST);

        results.setBackground(Theme.SURFACE);
        results.setBorder(Theme.cardBorder());
        add(results, BorderLayout.CENTER);
        showPlaceholder();
    }

    private JPanel buildForm(Runnable onHome) {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Theme.SURFACE);
        form.setBorder(Theme.cardBorder());
        form.setPreferredSize(new Dimension(320, 10));

        JButton back = Theme.secondaryButton("< Inicio");
        back.addActionListener(e -> onHome.run());

        JLabel title = new JLabel("Planear un viaje");
        title.setFont(Theme.font(Font.BOLD, 20));
        title.setForeground(Theme.TEXT);

        styleField(originField);
        styleField(destinationField);
        originField.addActionListener(e -> destinationField.requestFocusInWindow());
        destinationField.addActionListener(e -> search());

        JButton swap = Theme.secondaryButton("Intercambiar");
        swap.addActionListener(e -> {
            String origin = originField.getText();
            originField.setText(destinationField.getText());
            destinationField.setText(origin);
        });
        searchButton.addActionListener(e -> search());

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        actions.add(swap);
        actions.add(searchButton);

        JComboBox<String> examples = new JComboBox<>();
        examples.addItem(EXAMPLE_PROMPT);
        for (String[] example : EXAMPLES) {
            examples.addItem(example[2]);
        }
        examples.setFont(Theme.font(Font.PLAIN, 13));
        examples.setBackground(Theme.SURFACE);
        examples.addActionListener(e -> {
            int index = examples.getSelectedIndex();
            if (index > 0) {
                originField.setText(EXAMPLES[index - 1][0]);
                destinationField.setText(EXAMPLES[index - 1][1]);
                examples.setSelectedIndex(0);
                search();
            }
        });

        JLabel hint = new JLabel("<html>Ejemplos de formato:<br>Calle 10 # 43A - 20<br>"
                + "CR 70 x CL 48</html>");
        Theme.muted(hint, 12);

        add(form, back, title, label("Origen"), originField, label("Destino"), destinationField,
                actions, label("Probar un ejemplo"), examples, hint);
        return form;
    }

    private static void add(JPanel form, JComponent back, JComponent title, JComponent originLabel,
                            JComponent origin, JComponent destLabel, JComponent dest, JComponent actions,
                            JComponent examplesLabel, JComponent examples, JComponent hint) {
        JComponent[] all = {back, title, originLabel, origin, destLabel, dest, actions, examplesLabel, examples, hint};
        for (JComponent c : all) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (c != back && c != title && !(c instanceof JLabel)) {
                c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            }
        }
        form.add(back);
        form.add(Box.createVerticalStrut(16));
        form.add(title);
        form.add(Box.createVerticalStrut(16));
        form.add(originLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(origin);
        form.add(Box.createVerticalStrut(12));
        form.add(destLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(dest);
        form.add(Box.createVerticalStrut(16));
        form.add(actions);
        form.add(Box.createVerticalStrut(24));
        form.add(examplesLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(examples);
        form.add(Box.createVerticalStrut(12));
        form.add(hint);
        form.add(Box.createVerticalGlue());
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.font(Font.BOLD, 13));
        label.setForeground(Theme.TEXT);
        return label;
    }

    private static void styleField(JTextField field) {
        field.setFont(Theme.font(Font.PLAIN, 15));
        field.setForeground(Theme.TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
    }

    void focusOrigin() {
        originField.requestFocusInWindow();
    }

    /** Runs the search off the Swing thread so the window never freezes. */
    void search() {
        String origin = originField.getText();
        String destination = destinationField.getText();
        searchButton.setEnabled(false);
        showMessage("Buscando la mejor ruta...", false);

        new SwingWorker<PlanResult, Void>() {
            @Override
            protected PlanResult doInBackground() {
                return planner.plan(origin, destination);
            }

            @Override
            protected void done() {
                searchButton.setEnabled(true);
                try {
                    show(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    show(PlanResult.failure(PlanResult.Status.ERROR,
                            "Error al calcular la ruta: " + e.getCause().getMessage()));
                }
            }
        }.execute();
    }

    void show(PlanResult result) {
        lastResult = result;
        if (!result.isSuccess()) {
            showMessage(result.getMessage(), true);
            return;
        }
        results.removeAll();
        Trip trip = result.getTrip();

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        JLabel heading = new JLabel("Ruta encontrada");
        heading.setFont(Theme.font(Font.BOLD, 20));
        heading.setForeground(Theme.TEXT);
        JLabel fromTo = new JLabel(result.getOrigin().getName() + "  >  " + result.getDestination().getName());
        Theme.muted(fromTo, 13);

        JPanel summary = new JPanel(new GridLayout(1, 3, 10, 0));
        summary.setOpaque(false);
        summary.add(stat("Duración", Theme.duration(trip.getTotalDurationSec())));
        summary.add(stat("A pie", Theme.meters(trip.getTotalWalkMeters())));
        summary.add(stat("Tarifa", Theme.pesos(result.getFare().getTotalCOP())));

        for (JComponent c : new JComponent[]{heading, fromTo, summary}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        top.add(heading);
        top.add(Box.createVerticalStrut(2));
        top.add(fromTo);
        top.add(Box.createVerticalStrut(14));
        top.add(summary);
        if (result.getFare().isIntegrationApplied()) {
            JLabel integration = new JLabel("Incluye descuento por integración (tarjeta Cívica)");
            integration.setFont(Theme.font(Font.PLAIN, 13));
            integration.setForeground(Theme.PRIMARY_DARK);
            integration.setAlignmentX(Component.LEFT_ALIGNMENT);
            top.add(Box.createVerticalStrut(8));
            top.add(integration);
        }
        top.add(Box.createVerticalStrut(16));
        JLabel stepsTitle = new JLabel("Paso a paso");
        stepsTitle.setFont(Theme.font(Font.BOLD, 15));
        stepsTitle.setForeground(Theme.TEXT);
        stepsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(stepsTitle);
        top.add(Box.createVerticalStrut(6));

        JPanel steps = new JPanel();
        steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
        steps.setBackground(Theme.SURFACE);
        List<Step> list = trip.getSteps();
        for (int i = 0; i < list.size(); i++) {
            StepRow row = new StepRow(list.get(i), i == 0, i == list.size() - 1);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            steps.add(row);
        }
        JPanel stepsHolder = new JPanel(new BorderLayout());
        stepsHolder.setBackground(Theme.SURFACE);
        stepsHolder.add(steps, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(stepsHolder,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        results.add(top, BorderLayout.NORTH);
        results.add(scroll, BorderLayout.CENTER);
        results.revalidate();
        results.repaint();
    }

    private static JPanel stat(String label, String value) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Theme.PRIMARY_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        JLabel l = new JLabel(label);
        Theme.muted(l, 12);
        JLabel v = new JLabel(value);
        v.setFont(Theme.font(Font.BOLD, 20));
        v.setForeground(Theme.PRIMARY_DARK);
        panel.add(l);
        panel.add(v);
        return panel;
    }

    private void showPlaceholder() {
        results.removeAll();
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER));
        center.setOpaque(false);
        JLabel text = new JLabel("<html><div style='text-align:center;width:340px'>"
                + "<b>Escriba el origen y el destino</b><br><br>"
                + "o elija un ejemplo a la izquierda para ver cómo funciona.</div></html>");
        Theme.muted(text, 15);
        center.add(text);
        JPanel wrapper = new JPanel(new java.awt.GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(center);
        results.add(wrapper, BorderLayout.CENTER);
        results.revalidate();
        results.repaint();
    }

    private void showMessage(String message, boolean isError) {
        results.removeAll();
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(isError ? Theme.ERROR_LIGHT : Theme.PRIMARY_LIGHT);
        box.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JLabel text = new JLabel("<html><div style='width:420px'>" + escape(message) + "</div></html>");
        text.setFont(Theme.font(Font.PLAIN, 15));
        text.setForeground(isError ? Theme.ERROR : Theme.PRIMARY_DARK);
        box.add(text, BorderLayout.CENTER);
        results.add(box, BorderLayout.NORTH);
        results.revalidate();
        results.repaint();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** For tests. */
    JTextField originField() {
        return originField;
    }

    /** For tests. */
    JTextField destinationField() {
        return destinationField;
    }

    /** For tests. */
    JButton searchButton() {
        return searchButton;
    }

    /** For tests: the last result shown, or null before the first search. */
    PlanResult lastResult() {
        return lastResult;
    }
}

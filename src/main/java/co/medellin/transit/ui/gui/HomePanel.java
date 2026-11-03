package co.medellin.transit.ui.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

/**
 * Home screen: welcome text, what the app covers, the loading status and the
 * buttons to start planning a trip.
 */
final class HomePanel extends JPanel {

    private final JProgressBar progress = new JProgressBar();
    private final JLabel status = new JLabel("Cargando la red de transporte de Medellín...");
    private final JButton planButton = Theme.primaryButton("Planear un viaje");

    HomePanel(Runnable onPlan, Runnable onHelp, Runnable onExit) {
        super(new GridBagLayout());
        setBackground(Theme.BACKGROUND);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1, true),
                BorderFactory.createEmptyBorder(32, 40, 32, 40)));

        JLabel welcome = new JLabel("Bienvenido");
        welcome.setFont(Theme.font(Font.BOLD, 30));
        welcome.setForeground(Theme.TEXT);

        JLabel intro = new JLabel("<html><div style='width:520px'>"
                + "Escriba una dirección de origen y una de destino, y MetroBus le dice cómo "
                + "llegar en transporte público, cuánto tarda y cuánto cuesta."
                + "</div></html>");
        Theme.muted(intro, 16);

        JPanel modes = new JPanel(new GridLayout(1, 4, 10, 0));
        modes.setOpaque(false);
        modes.add(modeChip("Bus", "252 rutas"));
        modes.add(modeChip("Metro", "Líneas A y B"));
        modes.add(modeChip("Metroplús", "Líneas 1 y 2"));
        modes.add(modeChip("Cable y tranvía", "H, J, K, L, M, P y T-A"));

        progress.setIndeterminate(true);
        progress.setForeground(Theme.PRIMARY);
        progress.setBorderPainted(false);
        progress.setPreferredSize(new Dimension(520, 6));
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        Theme.muted(status, 13);

        planButton.setEnabled(false);
        planButton.addActionListener(e -> onPlan.run());
        JButton help = Theme.secondaryButton("Cómo escribir direcciones");
        help.addActionListener(e -> onHelp.run());
        JButton exit = Theme.secondaryButton("Salir");
        exit.addActionListener(e -> onExit.run());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(planButton);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(help);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(exit);

        for (Component c : new Component[]{welcome, intro, modes, progress, status, buttons}) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        card.add(welcome);
        card.add(Box.createVerticalStrut(10));
        card.add(intro);
        card.add(Box.createVerticalStrut(24));
        card.add(modes);
        card.add(Box.createVerticalStrut(28));
        card.add(progress);
        card.add(Box.createVerticalStrut(8));
        card.add(status);
        card.add(Box.createVerticalStrut(20));
        card.add(buttons);

        add(card);
    }

    private static JPanel modeChip(String title, String subtitle) {
        JPanel chip = new JPanel();
        chip.setLayout(new BoxLayout(chip, BoxLayout.Y_AXIS));
        chip.setBackground(Theme.PRIMARY_LIGHT);
        chip.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        JLabel t = new JLabel(title);
        t.setFont(Theme.font(Font.BOLD, 14));
        t.setForeground(Theme.PRIMARY_DARK);
        JLabel s = new JLabel(subtitle);
        Theme.muted(s, 12);
        chip.add(t);
        chip.add(s);
        return chip;
    }

    void setReady(String networkSummary) {
        progress.setIndeterminate(false);
        progress.setValue(progress.getMaximum());
        status.setText("Listo. " + networkSummary);
        status.setForeground(Theme.PRIMARY_DARK);
        planButton.setEnabled(true);
        planButton.requestFocusInWindow();
    }

    void setFailed(String message) {
        progress.setIndeterminate(false);
        progress.setValue(0);
        status.setText(message);
        status.setForeground(Theme.ERROR);
        planButton.setEnabled(false);
    }

    /** For tests. */
    boolean isPlanEnabled() {
        return planButton.isEnabled();
    }

    /** For tests. */
    String statusText() {
        return status.getText();
    }

    /** For tests. */
    JButton planButton() {
        return planButton;
    }
}

package co.medellin.transit.ui.gui.kit;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/**
 * Base for components we paint by hand. A plain JComponent has no accessible
 * context, so screen readers would skip it; this one has a name and a role.
 */
public abstract class Canvas extends JComponent {

    /** Painted components are decorative or read-only: they never take keyboard focus. */
    protected Canvas() {
        setFocusable(false);
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJComponent() {
                @Override
                public AccessibleRole getAccessibleRole() {
                    return role();
                }
            };
        }
        return accessibleContext;
    }

    protected AccessibleRole role() {
        return AccessibleRole.LABEL;
    }
}

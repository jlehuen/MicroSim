/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.2
 * @since 2026-01-05
 *
 * License: GNU General Public License v3.0
 */

package microsim.devices;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;

import microsim.VisualizationPanel;
import microsim.simulator.Simulator;

/**
 * The VisualizationFrame class provides an independent window
 * for the CPU, RAM and buses dynamic visualization.
 */
public class VisualizationFrame extends AbstractDevice {

    /** The singleton instance of the VisualizationFrame. */
    public static VisualizationFrame INSTANCE = null;

    private static final String TITLE_STRING = "CPU, RAM & Buses Visualization";
    private final VisualizationPanel visualizationPanel;

    /**
     * Constructs the VisualizationFrame.
     * @param simulator The simulator instance.
     */
    public VisualizationFrame(Simulator simulator) {
        INSTANCE = this;
        visualizationPanel = new VisualizationPanel(simulator);

        setTitle(TITLE_STRING);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(false);
        setResizable(true);

        setLayout(new BorderLayout());
        add(visualizationPanel, BorderLayout.CENTER);

        setMinimumSize(new Dimension(680, 420));
        setSize(780, 520);
        setLocation(240, 260);
        setVisible(false);
    }

    /**
     * Returns the underlying VisualizationPanel.
     * @return The VisualizationPanel instance.
     */
    public VisualizationPanel getVisualizationPanel() {
        return visualizationPanel;
    }
}

/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.2
 * @since 2026-01-05
 *
 * License: GNU General Public License v3.0
 */

package microsim.editor;

import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JSlider;
import microsim.simulator.Simulator;

/**
 * The SpeedSlider class provides a JSlider component to control the execution
 * speed of the simulation. It is embedded in the toolbar and allows the user
 * to adjust the delay and instruction throughput executed by the simulator.
 */
public class SpeedSlider extends Box {

    /** The maximum delay in milliseconds, corresponding to the slowest speed. */
    public final static int MAX_DELAY = 400;

    /**
     * Constructs a new SpeedSlider.
     * @param simulator The simulator instance whose execution speed will be controlled.
     */
    public SpeedSlider(Simulator simulator) {
        super(BoxLayout.Y_AXIS);

        // Slider from 0 (slowest, 400ms) to 100 (fastest, 5ms with 50-instruction batching)
        // Default position at 50 (medium speed, comfortable ~50 Hz)
        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        speedSlider.setPreferredSize(new Dimension(320, 22));
        speedSlider.setMinimumSize(new Dimension(220, 20));
        speedSlider.setMaximumSize(new Dimension(450, 24));
        speedSlider.setToolTipText("Execution speed");
        speedSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            updateSpeed(simulator, source.getValue());
        });

        // Set initial simulator speed based on default slider position (50)
        updateSpeed(simulator, 50);

        add(Box.createVerticalGlue());
        add(speedSlider);
        add(Box.createVerticalGlue());
        setMaximumSize(new Dimension(450, 28));
    }

    private static void updateSpeed(Simulator simulator, int val) {
        if (val <= 50) {
            // Slow to medium mode: 1 instruction per step, delay from 400ms down to 20ms
            int delay = 20 + (int) Math.round(380.0 * Math.pow((50.0 - val) / 50.0, 2.0));
            simulator.setExecutionSpeed(delay, 1);
        } else {
            // Fast mode: tick delay between 15ms and 5ms, batch size from 1 to 50 instructions
            double factor = (val - 50.0) / 50.0; // 0.0 to 1.0
            int batch = 1 + (int) Math.round(49.0 * Math.pow(factor, 1.8)); // 1 to 50 instructions per tick
            int delay = Math.max(5, (int) Math.round(15.0 - 10.0 * factor)); // 15ms down to 5ms
            simulator.setExecutionSpeed(delay, batch);
        }
    }
}

/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.1
 * @since 2025-12-17
 *
 * License: GNU General Public License v3.0
 */

package microsim.editor;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JSlider;
import javax.swing.BoxLayout;

import microsim.simulator.Simulator;

/**
 * The SpeedSlider class provides a JSlider component to control the execution
 * speed of the simulation. It is embedded in the toolbar and allows the user
 * to adjust the delay between each instruction executed by the simulator.
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

        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 0, MAX_DELAY, 0);
        speedSlider.setPreferredSize(new Dimension(200, 20));
        speedSlider.setToolTipText("Execution speed");
        speedSlider.addChangeListener(e -> {
            JSlider source = (JSlider)e.getSource();
            int speed = (int)source.getValue();
            simulator.setExecutionSpeedDelay(MAX_DELAY - speed);
        });                
        add(Box.createVerticalGlue());
        add(speedSlider);
        add(Box.createVerticalGlue());
        setMaximumSize(getPreferredSize());   
    }
}

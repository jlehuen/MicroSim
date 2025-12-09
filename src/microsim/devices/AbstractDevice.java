/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.0
 * @since 2025-12-09
 *
 * License: GNU General Public License v3.0
 */

package microsim.devices;

import javax.swing.JFrame;

/**
 * The AbstractDevice class serves as a base class for all simulated hardware
 * device windows in the application. It extends JFrame and provides a common
 * method to toggle the visibility of the device window.
 */
public abstract class AbstractDevice extends JFrame {

    /**
     * Default constructor for the AbstractDevice.
     * Initializes the JFrame.
     */
    public AbstractDevice() {
        super();
    }

    /**
     * Toggles the visibility of the device window.
     * If the window is visible, it becomes hidden, and vice versa.
     */
    public void toggleVisible() {
        setVisible(!isVisible());
    }
    
}

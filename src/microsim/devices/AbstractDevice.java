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

import javax.swing.JDialog;

import microsim.MicroSim;

/**
 * The AbstractDevice class serves as a base class for all simulated hardware
 * device windows in the application. It extends JDialog (non-modal, owned by
 * the main MicroSim frame) so device windows stay above the main window without
 * blocking modal dialogs (such as JFileChooser or JOptionPane) or covering
 * other applications.
 */
public abstract class AbstractDevice extends JDialog {

    /**
     * Default constructor for the AbstractDevice.
     * Initializes a non-modal JDialog owned by the main MicroSim frame.
     */
    public AbstractDevice() {
        super(MicroSim.self, false);
    }

    /**
     * Toggles the visibility of the device window.
     * If the window is visible, it becomes hidden, and vice versa.
     */
    public void toggleVisible() {
        setVisible(!isVisible());
    }
    
}

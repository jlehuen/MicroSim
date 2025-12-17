/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.1
 * @since 2025-12-17
 *
 * License: GNU General Public License v3.0
 */

package microsim.devices;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.*;

import microsim.simulator.Utils;

/**
 * The HeaterFrame class simulates a heater device with a thermometer.
 * It provides a graphical interface to visualize the temperature and the
 * state of the heater's burner. The device's status can be read by the CPU,
 * and the burner can be controlled by writing to a specific memory address.
 * This class is a singleton.
 */
public class HeaterFrame extends AbstractDevice {

    /** The singleton instance of the HeaterFrame. */
    public static final HeaterFrame INSTANCE = new HeaterFrame(); // Singleton

    private static final String TITLE_STRING = "Heater Control";
    private static final String BACKGROUND_IMAGE_NAME = "devices/heater.png";
    private static final String BURNING_IMAGE_NAME = "devices/burning.png";
    
    // Constants for drawing the thermometer
    private static final int THERMOMETER_X = 168;
    private static final int THERMOMETER_MAX_Y = 15;
    private static final int THERMOMETER_MIN_Y = 130;
    private static final int TEMP_THRESHOLD_20 = 81; // Corresponds to 20 degrees
    private static final int TEMP_THRESHOLD_10 = 105; // Corresponds to 15 degrees
    private static final int THRESHOLD_DELAY = 3;

	private HeaterPanel heaterPanel;

    /**
     * Private constructor for the HeaterFrame (Singleton pattern).
     * Initializes the panel and sets up the frame properties.
     */
    private HeaterFrame() {
    
        heaterPanel = new HeaterPanel();
        setContentPane(heaterPanel);
        pack(); // Automatically size the frame
        
        setTitle(TITLE_STRING);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        setVisible(false);
    }

    /**
     * Gets the status byte of the heater device.
     * @return The status byte.
     */
    public byte getStatus() {
        return heaterPanel.getStatusByte();
    }
    
    /**
     * Updates the state of the heater's burner.
     * @param value The control value from the CPU. Bit 7 controls the burner.
     */
    public void updateBurner(byte value) {
        heaterPanel.setBurnerState((value & 0x80) != 0);
    }

    /**
     * The HeaterPanel is the custom component that draws the heater,
     * thermometer, and burner animation. It also runs a thread to
     * simulate temperature changes.
     */
    private class HeaterPanel extends JPanel implements Runnable {
        private BufferedImage backgroundImage;
        private BufferedImage burningImage;
        private volatile boolean isBurnerOn = false;
        private volatile int temperatureY = 130; // Y-coord for thermometer line top
        private volatile byte statusByte = 0;

        /**
         * Constructs the HeaterPanel, loads images, and starts the simulation thread.
         */
        public HeaterPanel() {
            backgroundImage = Utils.loadBufferedImage(BACKGROUND_IMAGE_NAME);
            burningImage = Utils.loadBufferedImage(BURNING_IMAGE_NAME);
            Thread thermometerThread = new Thread(this);
            thermometerThread.setDaemon(true);
            thermometerThread.start();
        }

        /**
         * Returns the current status byte of the heater.
         * @return The status byte.
         */
        public byte getStatusByte() {
            return statusByte;
        }
        
        /**
         * Sets the state of the burner (on or off).
         * @param value true to turn the burner on, false to turn it off.
         */
        public void setBurnerState(boolean value) {
            this.isBurnerOn = value;
            // Update status byte's bit 7 accordingly burner state
            statusByte = (byte) ((statusByte & 0x7F) | (value ? 0x80 : 0x00));
        }

        /**
         * The main loop for the thermometer simulation thread.
         * It adjusts the temperature based on the burner state and updates the status byte.
         */
        @Override
        public void run() {
            while (true) {
                if (isBurnerOn) {
                    if (temperatureY > THERMOMETER_MAX_Y) {
                        temperatureY--; // Temperature rises
                    }
                } else {
                    if (temperatureY < THERMOMETER_MIN_Y) {
                        temperatureY++; // Temperature falls
                    }
                }

                // Update status byte based on temperature
                if (temperatureY < TEMP_THRESHOLD_10 - THRESHOLD_DELAY) {
                    statusByte |= 1; // Clear bit 0
                }
                if (temperatureY > TEMP_THRESHOLD_10 + THRESHOLD_DELAY) {
                    statusByte &= ~1; // Set bit 0
                }
                if (temperatureY < TEMP_THRESHOLD_20 - THRESHOLD_DELAY) {
                    statusByte |= 2; // Clear bit 1
                }
                if (temperatureY > TEMP_THRESHOLD_20 + THRESHOLD_DELAY) {
                    statusByte &= ~2; // Set bit 1
                }
                
                repaint();
                Utils.sleep(200); // Controls the speed of temperature change
            }
        }

        @Override
        public Dimension getPreferredSize() {
            if (backgroundImage != null) {
                return new Dimension(backgroundImage.getWidth(), backgroundImage.getHeight());
            } else {
                return new Dimension(200, 200); // Fallback size
            }
        }

        /**
         * Paints the component, including the background, thermometer, burner, and status byte.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, this);
            } else {
                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            // Draw thermometer
            g.setColor(Color.RED);
            g.fillRect(THERMOMETER_X, temperatureY, 2, THERMOMETER_MIN_Y - temperatureY);
            
            // Draw burner if on
            if (isBurnerOn) {
                g.drawImage(burningImage, 36, 20, this);
            }

            // Draw binary representation of status byte
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            g.setColor(Color.YELLOW);
            g.drawString(String.format("%8s", Integer.toBinaryString(statusByte & 0xFF)).replace(' ', '0'), 57, 180);
        }
    }
}
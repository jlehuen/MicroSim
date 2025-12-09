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

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.*;

import microsim.simulator.Utils;

/**
 * The LightsFrame class simulates a traffic lights device.
 * It provides a graphical interface to visualize two sets of traffic lights
 * (red, orange, green). The state of the lights is controlled by a single byte
 * written by the CPU to a specific memory address.
 * This class is a singleton.
 */
public class LightsFrame extends AbstractDevice {

    /** The singleton instance of the LightsFrame. */
    public static final LightsFrame INSTANCE = new LightsFrame(); // Singleton

    private static final String TITLE_STRING = "Traffic Lights";
    private static final String BACKGROUND_IMAGE_NAME = "devices/lights.png";

	// Center coordinates and sizes for the lights on the lights.png image
	private static final int DIAMETER = 23;
	private static final int FIRST_LIGHT_CENTER_X = 52;
	private static final int FIRST_LIGHT_CENTER_Y = 30;
	private static final int VERTICAL_SPACING = 25;
	private static final int HORIZONTAL_SPACING = 90;

	// Calculated center positions for each light
	private static final int LEFT_RED_CX = FIRST_LIGHT_CENTER_X;
	private static final int LEFT_RED_CY = FIRST_LIGHT_CENTER_Y;
	private static final int LEFT_ORANGE_CX = FIRST_LIGHT_CENTER_X;
	private static final int LEFT_ORANGE_CY = FIRST_LIGHT_CENTER_Y + VERTICAL_SPACING;
	private static final int LEFT_GREEN_CX = FIRST_LIGHT_CENTER_X;
	private static final int LEFT_GREEN_CY = FIRST_LIGHT_CENTER_Y + 2 * VERTICAL_SPACING;

	private static final int RIGHT_RED_CX = FIRST_LIGHT_CENTER_X + HORIZONTAL_SPACING;
	private static final int RIGHT_RED_CY = FIRST_LIGHT_CENTER_Y;
	private static final int RIGHT_ORANGE_CX = FIRST_LIGHT_CENTER_X + HORIZONTAL_SPACING;
	private static final int RIGHT_ORANGE_CY = FIRST_LIGHT_CENTER_Y + VERTICAL_SPACING;
	private static final int RIGHT_GREEN_CX = FIRST_LIGHT_CENTER_X + HORIZONTAL_SPACING;
	private static final int RIGHT_GREEN_CY = FIRST_LIGHT_CENTER_Y + 2 * VERTICAL_SPACING;

    private LightsPanel lightsPanel;

    /**
     * Private constructor for the LightsFrame (Singleton pattern).
     * Initializes the panel and sets up the frame properties.
     */
    private LightsFrame() {

        lightsPanel = new LightsPanel();
        setContentPane(lightsPanel);
        pack(); // Automatically size the frame
        
        setTitle(TITLE_STRING);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        setVisible(false);
    }

    /**
     * Updates the state of the traffic lights based on a control byte.
     * @param state The byte where each bit corresponds to a light's state (1=on, 0=off).
     */
    public void updateLights(byte state) {
        lightsPanel.updateLights(state);
    }

    /**
     * The LightsPanel is the custom component that draws the traffic lights image
     * and overlays the light states.
     */
    private class LightsPanel extends JPanel {
        private byte lightState = (byte) 0b11111100; // All lights on by default
        private BufferedImage backgroundImage;

        /**
         * Constructs the LightsPanel and loads the background image.
         */
        public LightsPanel() {
            backgroundImage = Utils.loadBufferedImage(BACKGROUND_IMAGE_NAME);
        }

        /**
         * Updates the internal state of the lights and triggers a repaint.
         * @param state The new state byte for the lights.
         */
        public void updateLights(byte state) {
            this.lightState = state;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            if (backgroundImage != null) {
                return new Dimension(backgroundImage.getWidth(), backgroundImage.getHeight());
            } else {
                // Fallback size if image fails to load
                return new Dimension(250, 450);
            }
        }

        /**
         * Paints the component, including the background image and the lights.
         * "Off" lights are indicated by drawing a black circle over them.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, this);
            } else {
                // If image failed to load, draw a default background
                g.setColor(Color.DARK_GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            // Draw black overlay for "off" lights
            g.setColor(Color.BLACK);
            int radius = DIAMETER / 2;

            if ((lightState & (1 << 7)) == 0) // Left Red
                g.fillOval(LEFT_RED_CX - radius, LEFT_RED_CY - radius, DIAMETER, DIAMETER);
            if ((lightState & (1 << 6)) == 0) // Left Orange
                g.fillOval(LEFT_ORANGE_CX - radius, LEFT_ORANGE_CY - radius, DIAMETER, DIAMETER);
            if ((lightState & (1 << 5)) == 0) // Left Green
                g.fillOval(LEFT_GREEN_CX - radius, LEFT_GREEN_CY - radius, DIAMETER, DIAMETER);
            if ((lightState & (1 << 4)) == 0) // Right Red
                g.fillOval(RIGHT_RED_CX - radius, RIGHT_RED_CY - radius, DIAMETER, DIAMETER);
            if ((lightState & (1 << 3)) == 0) // Right Orange
                g.fillOval(RIGHT_ORANGE_CX - radius, RIGHT_ORANGE_CY - radius, DIAMETER, DIAMETER);
            if ((lightState & (1 << 2)) == 0) // Right Green
                g.fillOval(RIGHT_GREEN_CX - radius, RIGHT_GREEN_CY - radius, DIAMETER, DIAMETER);
        
            // Draw binary representation of light state
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            g.setColor(Color.YELLOW);
            g.drawString(String.format("%8s", Integer.toBinaryString(lightState & 0xFF)).replace(' ', '0'), 68, 180);
        }
    }
}
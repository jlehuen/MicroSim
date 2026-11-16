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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import javax.swing.JFrame;
import javax.swing.JPanel;
import microsim.simulator.Utils;

/**
 * The LedBarFrame class simulates an 8-LED horizontal bar device.
 * It provides a graphical interface displaying 8 red LEDs arranged horizontally.
 * The LEDs are mapped to an 8-bit value written by the CPU to port 0x06:
 * bit 7 corresponds to the leftmost LED and bit 0 to the rightmost LED.
 * This class is implemented as a singleton.
 */
public class LedBarFrame extends AbstractDevice {

    /** The singleton instance of the LedBarFrame. */
    public static final LedBarFrame INSTANCE = new LedBarFrame(); // Singleton

    private static final String TITLE_STRING = "8-LED Bar";
    private static final String FONT_FILE_NAME = "fonts/Glass_TTY_VT220.ttf";

    private final LedBarPanel ledBarPanel;

    /**
     * Private constructor for the LedBarFrame (Singleton pattern).
     * Initializes the panel and sets up the frame properties.
     */
    private LedBarFrame() {
        ledBarPanel = new LedBarPanel();
        setContentPane(ledBarPanel);
        pack();

        setTitle(TITLE_STRING);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        setVisible(false);
    }

    /**
     * Updates the state of the 8 LEDs based on a control byte.
     * @param state The byte where each bit corresponds to an LED state (1=on, 0=off).
     */
    public void updateLeds(byte state) {
        ledBarPanel.updateLeds(state);
    }

    /**
     * Returns the current state byte of the LEDs.
     * @return The 8-bit state.
     */
    public byte getLedState() {
        return ledBarPanel.getLedState();
    }

    /**
     * The LedBarPanel is the custom component rendering the horizontal LED bar.
     */
    private static class LedBarPanel extends JPanel {
        private static final int PANEL_WIDTH = 320;
        private static final int PANEL_HEIGHT = 115;

        private static final int LED_COUNT = 8;
        private static final int LED_DIAMETER = 22;
        private static final int LED_RADIUS = LED_DIAMETER / 2;
        private static final int LED_CENTER_Y = 44;
        private static final int SPACING = 34;

        private byte ledState = 0;
        private final Font portFont;

        private static Font getPortFont(float size) {
            String[] fontCandidates = {"Monaco", "Consolas", "DejaVu Sans Mono", "Lucida Console", Font.MONOSPACED};
            for (String fontName : fontCandidates) {
                Font f = new Font(fontName, Font.PLAIN, (int) size);
                if (!f.getFamily().equals("Dialog") || fontName.equals(Font.MONOSPACED)) {
                    return f;
                }
            }
            return new Font(Font.MONOSPACED, Font.PLAIN, (int) size);
        }

        public LedBarPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(new Color(38, 42, 50));
            this.portFont = getPortFont(28f);
        }

        public void updateLeds(byte state) {
            this.ledState = state;
            repaint();
        }

        public byte getLedState() {
            return ledState;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Gray background extending to the window edges
            g2.setColor(new Color(38, 42, 50));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Subtle top highlight and bottom shadow for metallic look
            g2.setColor(new Color(65, 72, 85));
            g2.drawLine(0, 0, getWidth(), 0);
            g2.setColor(new Color(22, 24, 28));
            g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

            // Draw the 8 LEDs (bit 7 on the left to bit 0 on the right)
            Font bitLabelFont = new Font(Font.SANS_SERIF, Font.BOLD, 10);
            FontMetrics fmLabels = g2.getFontMetrics(bitLabelFont);

            int startX = (getWidth() - (LED_COUNT - 1) * SPACING) / 2;

            for (int i = 0; i < LED_COUNT; i++) {
                int bitIndex = 7 - i;
                int cx = startX + i * SPACING;
                int cy = LED_CENTER_Y;
                boolean isOn = ((ledState >> bitIndex) & 1) == 1;

                // Bit label (7 to 0) above LED
                g2.setFont(bitLabelFont);
                g2.setColor(new Color(165, 172, 188));
                String label = "b" + bitIndex;
                int labelW = fmLabels.stringWidth(label);
                g2.drawString(label, cx - labelW / 2, cy - LED_RADIUS - 6);

                // LED socket / metallic ring
                g2.setColor(new Color(20, 22, 26));
                g2.fillOval(cx - LED_RADIUS - 2, cy - LED_RADIUS - 2, LED_DIAMETER + 4, LED_DIAMETER + 4);
                g2.setColor(new Color(80, 86, 98));
                g2.drawOval(cx - LED_RADIUS - 2, cy - LED_RADIUS - 2, LED_DIAMETER + 4, LED_DIAMETER + 4);

                if (isOn) {
                    int lx = cx + 1;
                    int ly = cy + 1;

                    // Soft outer glow
                    Point2D center = new Point2D.Float(lx, ly);
                    float glowRadius = LED_RADIUS * 1.8f;
                    float[] dist = {0.0f, 0.5f, 1.0f};
                    Color[] glowColors = {
                        new Color(255, 50, 50, 180),
                        new Color(255, 20, 20, 80),
                        new Color(255, 0, 0, 0)
                    };
                    RadialGradientPaint glowPaint = new RadialGradientPaint(center, glowRadius, dist, glowColors);
                    g2.setPaint(glowPaint);
                    g2.fillOval((int) (lx - glowRadius), (int) (ly - glowRadius), (int) (glowRadius * 2), (int) (glowRadius * 2));

                    // LED active body
                    Color[] bodyColors = {
                        new Color(255, 140, 140),
                        new Color(255, 30, 30),
                        new Color(180, 0, 0)
                    };
                    float[] bodyDist = {0.0f, 0.6f, 1.0f};
                    Point2D highlightCenter = new Point2D.Float(lx - 3, ly - 3);
                    RadialGradientPaint bodyPaint = new RadialGradientPaint(highlightCenter, LED_RADIUS, bodyDist, bodyColors);
                    g2.setPaint(bodyPaint);
                    g2.fillOval(lx - LED_RADIUS, ly - LED_RADIUS, LED_DIAMETER, LED_DIAMETER);

                    // Specular highlight dot
                    g2.setColor(new Color(255, 255, 255, 220));
                    g2.fillOval(lx - 5, ly - 6, 4, 3);
                } else {
                    // LED off body (dark red/burgundy)
                    Point2D center = new Point2D.Float(cx, cy);
                    float[] offDist = {0.0f, 0.7f, 1.0f};
                    Color[] offColors = {
                        new Color(90, 20, 20),
                        new Color(50, 10, 10),
                        new Color(25, 5, 5)
                    };
                    RadialGradientPaint offPaint = new RadialGradientPaint(center, LED_RADIUS, offDist, offColors);
                    g2.setPaint(offPaint);
                    g2.fillOval(cx - LED_RADIUS, cy - LED_RADIUS, LED_DIAMETER, LED_DIAMETER);

                    // Dim reflection
                    g2.setColor(new Color(180, 180, 180, 50));
                    g2.fillOval(cx - 4, cy - 5, 4, 3);
                }
            }

            // Port number "06" in bottom left
            g2.setFont(portFont);
            g2.setColor(new Color(210, 220, 20));
            g2.drawString("06", 16, PANEL_HEIGHT - 13);

            // Bottom status line: Binary & Hex values
            Font valueFont = new Font(Font.MONOSPACED, Font.BOLD, 12);
            g2.setFont(valueFont);
            g2.setColor(new Color(240, 240, 120));

            int unsignedVal = ledState & 0xFF;
            String binString = String.format("%8s", Integer.toBinaryString(unsignedVal)).replace(' ', '0');
            String hexString = String.format("0x%02X", unsignedVal);
            String displayStr = binString + "  (" + hexString + ")";

            FontMetrics fmVals = g2.getFontMetrics(valueFont);
            int textW = fmVals.stringWidth(displayStr);
            int statusX = 54 + (getWidth() - 54 - textW) / 2;
            g2.drawString(displayStr, statusX, PANEL_HEIGHT - 17);

            g2.dispose();
        }
    }
}

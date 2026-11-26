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
 * The MatrixFrame class simulates an 8x8 red LED matrix peripheral.
 * It features an internal 8-byte display buffer corresponding to the 8 rows.
 * Control interface:
 * - Port 0x07 (ROW): selects the active row index (0 to 7).
 * - Port 0x08 (DATA): writes or reads the 8-bit pattern of the selected row
 *   (bit 7 is leftmost LED, bit 0 is rightmost LED).
 * This class is implemented as a singleton.
 */
public class MatrixFrame extends AbstractDevice {

    /** The singleton instance of the MatrixFrame. */
    public static final MatrixFrame INSTANCE = new MatrixFrame(); // Singleton

    private static final String TITLE_STRING = "8x8 LED Matrix";
    private static final String FONT_FILE_NAME = "fonts/Glass_TTY_VT220.ttf";

    private final MatrixPanel matrixPanel;

    /**
     * Private constructor for MatrixFrame (Singleton pattern).
     */
    private MatrixFrame() {
        matrixPanel = new MatrixPanel();
        setContentPane(matrixPanel);
        pack();

        setTitle(TITLE_STRING);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        setVisible(false);
    }

    /**
     * Selects the active row (0 to 7).
     * @param row The row index (clamped to 0..7).
     */
    public void selectRow(int row) {
        matrixPanel.selectRow(row);
    }

    /**
     * Returns the currently selected row index.
     * @return Row index (0 to 7).
     */
    public int getSelectedRow() {
        return matrixPanel.getSelectedRow();
    }

    /**
     * Writes 8-bit data to the currently selected row.
     * @param data The byte pattern for the 8 LEDs in the current row.
     */
    public void writeRowData(byte data) {
        matrixPanel.writeRowData(data);
    }

    /**
     * Reads the 8-bit data of the currently selected row.
     * @return The row's current byte pattern.
     */
    public byte readRowData() {
        return matrixPanel.readRowData();
    }

    /**
     * Resets all rows of the matrix to 0 (all LEDs off).
     */
    public void resetMatrix() {
        matrixPanel.resetMatrix();
    }

    /**
     * The MatrixPanel component renders the 8x8 LED matrix.
     */
    private static class MatrixPanel extends JPanel {
        private static final int PANEL_WIDTH = 270;
        private static final int PANEL_HEIGHT = 295;

        private static final int ROWS = 8;
        private static final int COLS = 8;

        private static final int LED_DIAMETER = 16;
        private static final int LED_RADIUS = LED_DIAMETER / 2;
        private static final int SPACING_X = 24;
        private static final int SPACING_Y = 24;
        private static final int GRID_START_X = 46;
        private static final int GRID_START_Y = 40;

        private final byte[] matrix = new byte[ROWS];
        private int selectedRow = 0;
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

        public MatrixPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(new Color(38, 42, 50));
            this.portFont = getPortFont(28f);
        }

        public void selectRow(int row) {
            this.selectedRow = row & 0x07;
            repaint();
        }

        public int getSelectedRow() {
            return selectedRow;
        }

        public void writeRowData(byte data) {
            matrix[selectedRow] = data;
            repaint();
        }

        public byte readRowData() {
            return matrix[selectedRow];
        }

        public void resetMatrix() {
            for (int r = 0; r < ROWS; r++) {
                matrix[r] = 0;
            }
            selectedRow = 0;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Gray background extending to all edges
            g2.setColor(new Color(38, 42, 50));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Top highlight and bottom shadow
            g2.setColor(new Color(65, 72, 85));
            g2.drawLine(0, 0, getWidth(), 0);
            g2.setColor(new Color(22, 24, 28));
            g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

            Font labelFont = new Font(Font.SANS_SERIF, Font.BOLD, 9);
            FontMetrics fmLbl = g2.getFontMetrics(labelFont);

            // Column labels (b7 to b0)
            g2.setFont(labelFont);
            for (int c = 0; c < COLS; c++) {
                int bitIndex = 7 - c;
                int cx = GRID_START_X + c * SPACING_X;
                String lbl = "b" + bitIndex;
                int w = fmLbl.stringWidth(lbl);
                g2.setColor(new Color(165, 172, 188));
                g2.drawString(lbl, cx - w / 2, GRID_START_Y - 10);
            }

            // Draw matrix grid
            for (int r = 0; r < ROWS; r++) {
                int cy = GRID_START_Y + r * SPACING_Y;
                boolean isCurrentRow = (r == selectedRow);

                // Row label and selection pointer
                if (isCurrentRow) {
                    g2.setColor(new Color(255, 210, 60)); // Active yellow indicator
                    g2.fillPolygon(new int[]{12, 18, 12}, new int[]{cy - 4, cy, cy + 4}, 3);
                    g2.setFont(labelFont);
                    g2.setColor(new Color(255, 220, 100));
                } else {
                    g2.setFont(labelFont);
                    g2.setColor(new Color(130, 136, 150));
                }
                String rLbl = "R" + r;
                g2.drawString(rLbl, 22, cy + 3);

                // Row LEDs
                byte rowByte = matrix[r];
                for (int c = 0; c < COLS; c++) {
                    int bitIndex = 7 - c;
                    int cx = GRID_START_X + c * SPACING_X;
                    boolean isOn = ((rowByte >> bitIndex) & 1) == 1;

                    // Socket / ring
                    g2.setColor(new Color(20, 22, 26));
                    g2.fillOval(cx - LED_RADIUS - 1, cy - LED_RADIUS - 1, LED_DIAMETER + 2, LED_DIAMETER + 2);
                    g2.setColor(new Color(75, 80, 92));
                    g2.drawOval(cx - LED_RADIUS - 1, cy - LED_RADIUS - 1, LED_DIAMETER + 2, LED_DIAMETER + 2);

                    if (isOn) {
                        int lx = cx + 1;
                        int ly = cy + 1;

                        // Outer glow
                        Point2D center = new Point2D.Float(lx, ly);
                        float glowRadius = LED_RADIUS * 1.6f;
                        float[] dist = {0.0f, 0.5f, 1.0f};
                        Color[] glowColors = {
                            new Color(255, 50, 50, 160),
                            new Color(255, 20, 20, 60),
                            new Color(255, 0, 0, 0)
                        };
                        RadialGradientPaint glowPaint = new RadialGradientPaint(center, glowRadius, dist, glowColors);
                        g2.setPaint(glowPaint);
                        g2.fillOval((int) (lx - glowRadius), (int) (ly - glowRadius), (int) (glowRadius * 2), (int) (glowRadius * 2));

                        // Active body
                        Color[] bodyColors = {
                            new Color(255, 140, 140),
                            new Color(255, 30, 30),
                            new Color(180, 0, 0)
                        };
                        float[] bodyDist = {0.0f, 0.6f, 1.0f};
                        Point2D highlightCenter = new Point2D.Float(lx - 2, ly - 2);
                        RadialGradientPaint bodyPaint = new RadialGradientPaint(highlightCenter, LED_RADIUS, bodyDist, bodyColors);
                        g2.setPaint(bodyPaint);
                        g2.fillOval(lx - LED_RADIUS, ly - LED_RADIUS, LED_DIAMETER, LED_DIAMETER);

                        // Specular dot
                        g2.setColor(new Color(255, 255, 255, 210));
                        g2.fillOval(lx - 3, ly - 4, 3, 2);
                    } else {
                        // Off body
                        Point2D center = new Point2D.Float(cx, cy);
                        float[] offDist = {0.0f, 0.7f, 1.0f};
                        Color[] offColors = {
                            new Color(80, 18, 18),
                            new Color(45, 10, 10),
                            new Color(20, 5, 5)
                        };
                        RadialGradientPaint offPaint = new RadialGradientPaint(center, LED_RADIUS, offDist, offColors);
                        g2.setPaint(offPaint);
                        g2.fillOval(cx - LED_RADIUS, cy - LED_RADIUS, LED_DIAMETER, LED_DIAMETER);
                    }
                }
            }

            // Port numbers "08/09" in bottom left
            g2.setFont(portFont);
            g2.setColor(new Color(210, 220, 20));
            g2.drawString("08/09", 14, PANEL_HEIGHT - 13);

            // Bottom status line: Selected Row + Hex value
            Font statusFont = new Font(Font.MONOSPACED, Font.BOLD, 11);
            g2.setFont(statusFont);
            g2.setColor(new Color(240, 240, 120));

            int unsignedRowVal = matrix[selectedRow] & 0xFF;
            String binStr = String.format("%8s", Integer.toBinaryString(unsignedRowVal)).replace(' ', '0');
            String hexStr = String.format("0x%02X", unsignedRowVal);
            String statusText = "R" + selectedRow + ": " + binStr + " (" + hexStr + ")";

            FontMetrics fmStatus = g2.getFontMetrics(statusFont);
            int textW = fmStatus.stringWidth(statusText);
            g2.drawString(statusText, getWidth() - textW - 14, PANEL_HEIGHT - 17);

            g2.dispose();
        }
    }
}

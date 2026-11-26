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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JPanel;
import microsim.simulator.Utils;

/**
 * The BannerFrame class simulates a 64x8 horizontal red LED banner / ticker peripheral.
 * It features an internal buffer of 64 column bytes (8 blocks of 8x8 matrices).
 *
 * Control interface:
 * - Port 0x09 (DATA): 8-bit column pattern (bit 0 is topmost LED, bit 7 is bottommost LED).
 * - Port 0x0A (COMMAND):
 *     0: Clear all (turn off all LEDs)
 *     1: Shift left by 1 column (column 63 cleared)
 *     2: Shift right by 1 column (column 0 cleared)
 *     3: Write DATA to column 0 (leftmost)
 *     4: Write DATA to column 63 (rightmost)
 *     5: Push right (Shift left + write DATA to column 63, ideal for ticker text scrolling)
 *     6: Push left (Shift right + write DATA to column 0)
 *
 * This class is implemented as a singleton.
 */
public class BannerFrame extends AbstractDevice {

    /** The singleton instance of the BannerFrame. */
    public static final BannerFrame INSTANCE = new BannerFrame();

    private static final String TITLE_STRING = "64x8 LED Banner";

    public static final int CMD_CLEAR = 0;
    public static final int CMD_SHIFT_LEFT = 1;
    public static final int CMD_SHIFT_RIGHT = 2;
    public static final int CMD_WRITE_LEFT = 3;
    public static final int CMD_WRITE_RIGHT = 4;
    public static final int CMD_PUSH_RIGHT = 5;
    public static final int CMD_PUSH_LEFT = 6;

    private final BannerPanel bannerPanel;

    private BannerFrame() {
        bannerPanel = new BannerPanel();
        setContentPane(bannerPanel);
        pack();

        setTitle(TITLE_STRING);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        setVisible(false);
    }

    public void setData(int data) {
        bannerPanel.setData(data);
    }

    public void executeCommand(int cmd) {
        bannerPanel.executeCommand(cmd);
    }

    public int getData() {
        return bannerPanel.getData();
    }

    public int getLastCommand() {
        return bannerPanel.getLastCommand();
    }

    public byte[] getColumns() {
        return bannerPanel.getColumns();
    }

    /**
     * The BannerPanel component renders the 32x8 horizontal LED banner.
     */
    private static class BannerPanel extends JPanel {
        private static final int COLS = 64;
        private static final int ROWS = 8;

        private static final int LED_DIAMETER = 8;
        private static final int LED_RADIUS = LED_DIAMETER / 2;
        private static final int SPACING_X = 11;
        private static final int SPACING_Y = 11;
        private static final int GROUP_GAP = 5;
        private static final int GRID_START_X = 18;
        private static final int GRID_START_Y = 22;

        private static final int PANEL_WIDTH = 780;
        private static final int PANEL_HEIGHT = 155;

        private final byte[] columns = new byte[COLS];
        private int lastData = 0;
        private int lastCommand = 0;

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

        public BannerPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(new Color(38, 42, 50));
            this.portFont = getPortFont(28f);
        }

        public synchronized void setData(int data) {
            this.lastData = data & 0xFF;
            repaint();
        }

        public synchronized void executeCommand(int cmd) {
            this.lastCommand = cmd & 0xFF;
            switch (lastCommand) {
                case CMD_CLEAR:
                    Arrays.fill(columns, (byte) 0);
                    break;
                case CMD_SHIFT_LEFT:
                    System.arraycopy(columns, 1, columns, 0, COLS - 1);
                    columns[COLS - 1] = 0;
                    break;
                case CMD_SHIFT_RIGHT:
                    System.arraycopy(columns, 0, columns, 1, COLS - 1);
                    columns[0] = 0;
                    break;
                case CMD_WRITE_LEFT:
                    columns[0] = (byte) lastData;
                    break;
                case CMD_WRITE_RIGHT:
                    columns[COLS - 1] = (byte) lastData;
                    break;
                case CMD_PUSH_RIGHT:
                    System.arraycopy(columns, 1, columns, 0, COLS - 1);
                    columns[COLS - 1] = (byte) lastData;
                    break;
                case CMD_PUSH_LEFT:
                    System.arraycopy(columns, 0, columns, 1, COLS - 1);
                    columns[0] = (byte) lastData;
                    break;
                default:
                    break;
            }
            repaint();
        }

        public synchronized int getData() {
            return lastData;
        }

        public synchronized int getLastCommand() {
            return lastCommand;
        }

        public synchronized byte[] getColumns() {
            return Arrays.copyOf(columns, columns.length);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Dark casing background extending to window edges
            g2.setColor(new Color(38, 42, 50));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Top metallic highlight and bottom shadow
            g2.setColor(new Color(65, 72, 85));
            g2.drawLine(0, 0, getWidth(), 0);
            g2.setColor(new Color(22, 24, 28));
            g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

            // Subtle sub-matrix borders for the 8 blocks of 8x8
            for (int b = 0; b < 8; b++) {
                int bx = GRID_START_X + b * (8 * SPACING_X + GROUP_GAP) - 4;
                int by = GRID_START_Y - 4;
                int bw = 8 * SPACING_X - (SPACING_X - LED_DIAMETER) + 8;
                int bh = 8 * SPACING_Y - (SPACING_Y - LED_DIAMETER) + 8;

                g2.setColor(new Color(28, 31, 37));
                g2.fillRoundRect(bx, by, bw, bh, 6, 6);
                g2.setColor(new Color(55, 60, 72));
                g2.drawRoundRect(bx, by, bw, bh, 6, 6);
            }

            // Draw LEDs (64 columns x 8 rows)
            byte[] colsSnapshot;
            int currentData;
            int currentCmd;
            synchronized (this) {
                colsSnapshot = Arrays.copyOf(columns, columns.length);
                currentData = lastData;
                currentCmd = lastCommand;
            }

            for (int c = 0; c < COLS; c++) {
                int block = c / 8;
                int cx = GRID_START_X + c * SPACING_X + block * GROUP_GAP;
                int colByte = colsSnapshot[c] & 0xFF;

                for (int r = 0; r < ROWS; r++) {
                    // bit 0 is top LED (r=0), bit 7 is bottom LED (r=7)
                    int bitIndex = r;
                    int cy = GRID_START_Y + r * SPACING_Y;
                    boolean isOn = ((colByte >> bitIndex) & 1) == 1;

                    // Socket / ring
                    g2.setColor(new Color(20, 22, 26));
                    g2.fillOval(cx - LED_RADIUS - 1, cy - LED_RADIUS - 1, LED_DIAMETER + 2, LED_DIAMETER + 2);
                    g2.setColor(new Color(75, 80, 92));
                    g2.drawOval(cx - LED_RADIUS - 1, cy - LED_RADIUS - 1, LED_DIAMETER + 2, LED_DIAMETER + 2);

                    if (isOn) {
                        // Offset by +1 pixel down and +1 pixel right when ON
                        int lx = cx + 1;
                        int ly = cy + 1;

                        // Outer soft glow
                        Point2D center = new Point2D.Float(lx, ly);
                        float glowRadius = LED_RADIUS * 1.6f;
                        float[] dist = {0.0f, 0.5f, 1.0f};
                        Color[] glowColors = {
                            new Color(255, 50, 50, 150),
                            new Color(255, 20, 20, 50),
                            new Color(255, 0, 0, 0)
                        };
                        RadialGradientPaint glowPaint = new RadialGradientPaint(center, glowRadius, dist, glowColors);
                        g2.setPaint(glowPaint);
                        g2.fillOval((int) (lx - glowRadius), (int) (ly - glowRadius), (int) (glowRadius * 2), (int) (glowRadius * 2));

                        // Bright red core with glossy highlight
                        Color[] coreColors = {
                            new Color(255, 230, 230),
                            new Color(255, 60, 60),
                            new Color(200, 10, 10)
                        };
                        RadialGradientPaint corePaint = new RadialGradientPaint(center, LED_RADIUS, dist, coreColors);
                        g2.setPaint(corePaint);
                        g2.fillOval(lx - LED_RADIUS, ly - LED_RADIUS, LED_DIAMETER, LED_DIAMETER);

                        // Specular point
                        g2.setColor(new Color(255, 255, 255, 200));
                        g2.fillOval(lx - 2, ly - 3, 3, 2);
                    } else {
                        // Off state
                        Point2D center = new Point2D.Float(cx, cy);
                        float[] offDist = {0.0f, 0.7f, 1.0f};
                        Color[] offColors = {
                            new Color(45, 15, 15),
                            new Color(28, 10, 10),
                            new Color(18, 8, 8)
                        };
                        RadialGradientPaint offPaint = new RadialGradientPaint(center, LED_RADIUS, offDist, offColors);
                        g2.setPaint(offPaint);
                        g2.fillOval(cx - LED_RADIUS, cy - LED_RADIUS, LED_DIAMETER, LED_DIAMETER);
                    }
                }
            }

            // Port numbers "0A/0B" in bottom left
            g2.setFont(portFont);
            g2.setColor(new Color(210, 220, 20));
            g2.drawString("0A/0B", 16, PANEL_HEIGHT - 13);

            // Bottom status info: DATA and LAST COMMAND
            Font statusFont = new Font(Font.MONOSPACED, Font.BOLD, 11);
            g2.setFont(statusFont);
            g2.setColor(new Color(240, 240, 120));

            String cmdName;
            switch (currentCmd) {
                case CMD_CLEAR: cmdName = "CLEAR"; break;
                case CMD_SHIFT_LEFT: cmdName = "SHL"; break;
                case CMD_SHIFT_RIGHT: cmdName = "SHR"; break;
                case CMD_WRITE_LEFT: cmdName = "WR_L"; break;
                case CMD_WRITE_RIGHT: cmdName = "WR_R"; break;
                case CMD_PUSH_RIGHT: cmdName = "PUSH_R"; break;
                case CMD_PUSH_LEFT: cmdName = "PUSH_L"; break;
                default: cmdName = String.format("CMD %d", currentCmd); break;
            }

            String statusText = String.format("DATA: 0x%02X (%s)  |  CMD: %s",
                currentData,
                String.format("%8s", Integer.toBinaryString(currentData)).replace(' ', '0'),
                cmdName
            );

            FontMetrics fmStatus = g2.getFontMetrics(statusFont);
            int textW = fmStatus.stringWidth(statusText);
            g2.drawString(statusText, getWidth() - textW - 16, PANEL_HEIGHT - 17);

            g2.dispose();
        }
    }
}

/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.2
 * @since 2026-11-26
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
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JFrame;
import javax.swing.JPanel;

import microsim.simulator.Utils;

/**
 * The ClockFrame class simulates a retro mantel clock device that provides
 * a day/night status bit to the CPU on port 0x04.
 * Bit 0 is 1 during daytime (07:00 - 22:00) and 0 during nighttime (22:00 - 07:00).
 * All artwork is rendered natively in Java2D.
 * This class is a singleton.
 */
public class ClockFrame extends AbstractDevice {

    /** The singleton instance of the ClockFrame. */
    public static final ClockFrame INSTANCE = new ClockFrame();

    private static final String TITLE_STRING = "Clock";
    private static final int DAY_START_MINUTES = 7 * 60;   // 07:00
    private static final int NIGHT_START_MINUTES = 22 * 60; // 22:00
    private static final int MINUTES_PER_DAY = 24 * 60;

    private final ClockPanel clockPanel;

    /**
     * Private constructor for the ClockFrame (Singleton pattern).
     * Initializes the panel and sets up the frame properties.
     */
    private ClockFrame() {
        clockPanel = new ClockPanel();
        setContentPane(clockPanel);
        pack();

        setTitle(TITLE_STRING);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        setVisible(false);
    }

    /**
     * Gets the status byte of the clock device.
     * Bit 0 is 1 for day, 0 for night.
     * @return The status byte.
     */
    public byte getStatus() {
        return clockPanel.getStatusByte();
    }

    /**
     * The ClockPanel renders an original flat-vector mantel clock with rotating hands,
     * a day/night dial theme, the digital time, and the binary status byte.
     */
    private static class ClockPanel extends JPanel implements Runnable {
        private static final int PANEL_WIDTH = 199;
        private static final int PANEL_HEIGHT = 198;
        private static final int CLOCK_CENTER_X = PANEL_WIDTH / 2;
        private static final int CLOCK_CENTER_Y = 86;
        private static final int CLOCK_RADIUS = 33;

        // Flat-vector palette
        private static final Color BG_DAY = new Color(135, 206, 250);
        private static final Color BG_NIGHT = new Color(38, 62, 115);
        private static final Color OUTLINE = new Color(18, 18, 22);
        private static final Color BODY_CREAM = new Color(239, 224, 188);
        private static final Color ACCENT_GOLD = new Color(224, 178, 76);

        private volatile int totalMinutes = 8 * 60; // Start at 08:00 (Day)
        private volatile byte statusByte = 1;
        private final Font portFont = getPortFont(32f);

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

        public ClockPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(BG_DAY);
            updateStatusByte();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggleDayNight();
                }
            });

            Thread clockThread = new Thread(this, "MicroSim-Clock");
            clockThread.setDaemon(true);
            clockThread.start();
        }

        public byte getStatusByte() {
            return statusByte;
        }

        public void toggleDayNight() {
            if (isDay()) {
                totalMinutes = NIGHT_START_MINUTES; // Jump to 22:00
            } else {
                totalMinutes = DAY_START_MINUTES + 60; // Jump to 08:00
            }
            updateStatusByte();
            repaint();
        }

        private boolean isDay() {
            return totalMinutes >= DAY_START_MINUTES && totalMinutes < NIGHT_START_MINUTES;
        }

        private void updateStatusByte() {
            statusByte = (byte) (isDay() ? 0x01 : 0x00);
        }

        @Override
        public void run() {
            while (true) {
                Utils.sleep(60);
                totalMinutes = (totalMinutes + 2) % MINUTES_PER_DAY;
                updateStatusByte();
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean day = isDay();
            g2.setColor(day ? BG_DAY : BG_NIGHT);
            g2.fillRect(0, 0, getWidth(), getHeight());

            BasicStroke outlineStroke = new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

            // 1. Outer golden arch handle at the top
            Arc2D outerArch = new Arc2D.Double(CLOCK_CENTER_X - 33, 10, 66, 66, 0, 180, Arc2D.OPEN);
            g2.setColor(ACCENT_GOLD);
            g2.fill(outerArch);
            g2.setStroke(outlineStroke);
            g2.setColor(OUTLINE);
            g2.draw(outerArch);

            // 2. Inner cream dome
            Arc2D innerDome = new Arc2D.Double(CLOCK_CENTER_X - 21, 22, 42, 42, 0, 180, Arc2D.OPEN);
            g2.setColor(BODY_CREAM);
            g2.fill(innerDome);
            g2.setColor(OUTLINE);
            g2.draw(innerDome);

            // 3. Main clock body (square-ish cabinet)
            int bodyX = CLOCK_CENTER_X - 46;
            int bodyY = 43;
            int bodyW = 92;
            int bodyH = 81;
            g2.setColor(BODY_CREAM);
            g2.fillRect(bodyX, bodyY, bodyW, bodyH);
            g2.setColor(OUTLINE);
            g2.drawRect(bodyX, bodyY, bodyW, bodyH);

            // 4. Top cornice / eaves on left and right
            RoundRectangle2D leftCornice = new RoundRectangle2D.Double(CLOCK_CENTER_X - 58, 40, 20, 13, 10, 10);
            RoundRectangle2D rightCornice = new RoundRectangle2D.Double(CLOCK_CENTER_X + 38, 40, 20, 13, 10, 10);
            g2.setColor(BODY_CREAM);
            g2.fill(leftCornice);
            g2.fill(rightCornice);
            g2.fillRect(CLOCK_CENTER_X - 44, 40, 88, 13); // Seamless bridge across top
            g2.setColor(OUTLINE);
            g2.drawLine(CLOCK_CENTER_X - 51, 40, CLOCK_CENTER_X - 21, 40);
            g2.drawLine(CLOCK_CENTER_X + 21, 40, CLOCK_CENTER_X + 51, 40);
            g2.draw(new Arc2D.Double(CLOCK_CENTER_X - 58, 40, 13, 13, 90, 180, Arc2D.OPEN));
            g2.draw(new Arc2D.Double(CLOCK_CENTER_X + 45, 40, 13, 13, -90, 180, Arc2D.OPEN));
            g2.drawLine(CLOCK_CENTER_X - 51, 53, CLOCK_CENTER_X - 40, 53);
            g2.drawLine(CLOCK_CENTER_X + 40, 53, CLOCK_CENTER_X + 51, 53);

            // 5. Golden rounded base
            RoundRectangle2D base = new RoundRectangle2D.Double(CLOCK_CENTER_X - 56, 122, 112, 18, 18, 18);
            g2.setColor(ACCENT_GOLD);
            g2.fill(base);
            g2.setColor(OUTLINE);
            g2.draw(base);

            // Ground line under the base
            g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(CLOCK_CENTER_X - 68, 140, CLOCK_CENTER_X + 68, 140);

            // 6. Circular dial (constant light-blue face)
            Color dialBg = new Color(190, 238, 252);
            g2.setColor(dialBg);
            g2.fillOval(CLOCK_CENTER_X - CLOCK_RADIUS, CLOCK_CENTER_Y - CLOCK_RADIUS,
                    CLOCK_RADIUS * 2, CLOCK_RADIUS * 2);
            g2.setStroke(outlineStroke);
            g2.setColor(OUTLINE);
            g2.drawOval(CLOCK_CENTER_X - CLOCK_RADIUS, CLOCK_CENTER_Y - CLOCK_RADIUS,
                    CLOCK_RADIUS * 2, CLOCK_RADIUS * 2);

            // 4 bold cardinal tick marks (12, 3, 6, 9)
            g2.setColor(OUTLINE);
            g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 4; i++) {
                double angle = Math.toRadians(i * 90 - 90);
                int r1 = CLOCK_RADIUS - 12;
                int r2 = CLOCK_RADIUS - 5;
                g2.drawLine(
                        CLOCK_CENTER_X + (int) Math.round(Math.cos(angle) * r1),
                        CLOCK_CENTER_Y + (int) Math.round(Math.sin(angle) * r1),
                        CLOCK_CENTER_X + (int) Math.round(Math.cos(angle) * r2),
                        CLOCK_CENTER_Y + (int) Math.round(Math.sin(angle) * r2));
            }

            int hours = (totalMinutes / 60) % 24;
            int minutes = totalMinutes % 60;

            // Hour hand (thick, rounded)
            double hourAngle = Math.toRadians(((hours % 12) + minutes / 60.0) * 30.0 - 90.0);
            int hourLen = CLOCK_RADIUS - 16;
            g2.setStroke(new BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(OUTLINE);
            g2.drawLine(CLOCK_CENTER_X, CLOCK_CENTER_Y,
                    CLOCK_CENTER_X + (int) Math.round(Math.cos(hourAngle) * hourLen),
                    CLOCK_CENTER_Y + (int) Math.round(Math.sin(hourAngle) * hourLen));

            // Minute hand (medium-thick, rounded)
            double minAngle = Math.toRadians(minutes * 6.0 - 90.0);
            int minLen = CLOCK_RADIUS - 8;
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(205, 68, 48));
            g2.drawLine(CLOCK_CENTER_X, CLOCK_CENTER_Y,
                    CLOCK_CENTER_X + (int) Math.round(Math.cos(minAngle) * minLen),
                    CLOCK_CENTER_Y + (int) Math.round(Math.sin(minAngle) * minLen));

            // Center cap
            g2.setColor(OUTLINE);
            g2.fillOval(CLOCK_CENTER_X - 3, CLOCK_CENTER_Y - 3, 6, 6);

            // Digital time
            String timeText = String.format("%02d:%02d", hours, minutes);
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
            g2.setColor(day ? OUTLINE : new Color(220, 235, 255));
            FontMetrics fm = g2.getFontMetrics();
            int textX = (PANEL_WIDTH - fm.stringWidth(timeText)) / 2;
            g2.drawString(timeText, textX, 163);

            // Binary status byte
            String binText = String.format("%8s", Integer.toBinaryString(statusByte & 0xFF)).replace(' ', '0');
            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
            g2.setColor(day ? new Color(25, 35, 65) : Color.YELLOW);
            fm = g2.getFontMetrics();
            int binX = (PANEL_WIDTH - fm.stringWidth(binText)) / 2;
            g2.drawString(binText, binX, 183);

            // Port number "04" in bottom left (same color as binary status)
            g2.setFont(portFont);
            g2.setColor(day ? new Color(25, 35, 65) : Color.YELLOW);
            g2.drawString("04", 14, PANEL_HEIGHT - 12);

            g2.dispose();
        }
    }
}

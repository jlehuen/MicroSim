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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import javax.swing.JPanel;
import microsim.simulator.Utils;

/**
 * The BuzzerFrame class simulates a retro 8-bit chiptune sound generator / buzzer.
 * Control interface:
 * - Port 0x04 (NOTE): MIDI note number (1 to 127, e.g. 60=C4, 69=A440), or 0 to stop.
 * - Port 0x05 (DURATION): Duration in units of 10 ms (e.g. 25 = 250 ms). If written,
 *   the tone automatically stops after this duration.
 * This class is implemented as a singleton.
 */
public class BuzzerFrame extends AbstractDevice {

    /** The singleton instance of the BuzzerFrame. */
    public static final BuzzerFrame INSTANCE = new BuzzerFrame(); // Singleton

    private static final String TITLE_STRING = "8-Bit Buzzer";
    private static final String FONT_FILE_NAME = "fonts/Glass_TTY_VT220.ttf";

    private final BuzzerPanel buzzerPanel;

    private BuzzerFrame() {
        buzzerPanel = new BuzzerPanel();
        setContentPane(buzzerPanel);
        pack();

        setTitle(TITLE_STRING);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        setVisible(false);
    }

    /**
     * Sets the active note (0 = stop, 1..127 = MIDI note).
     */
    public void setNote(int note) {
        buzzerPanel.setNote(note);
    }

    /**
     * Plays the current note for a specified duration in units of 10 ms.
     */
    public void setDuration(int durationUnitsOf10ms) {
        buzzerPanel.setDuration(durationUnitsOf10ms);
    }

    /**
     * Returns the active note code.
     */
    public int getNote() {
        return buzzerPanel.getNote();
    }

    /**
     * Returns 1 if currently sounding, 0 otherwise.
     */
    public int isPlaying() {
        return buzzerPanel.isPlaying() ? 1 : 0;
    }

    /**
     * Custom panel with audio generation thread and speaker visualization.
     */
    private static class BuzzerPanel extends JPanel {
        private static final int PANEL_WIDTH = 280;
        private static final int PANEL_HEIGHT = 130;

        private static final int SAMPLE_RATE = 44100;
        private static final String[] NOTE_NAMES = {
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
        };

        private volatile int currentNote = 0;
        private volatile boolean busy = false;
        private volatile boolean audioRunning = true;

        private SourceDataLine audioLine;
        private final Object lock = new Object();
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private ScheduledFuture<?> stopTask = null;

        // Visual animation counter
        private int waveStep = 0;
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

        public BuzzerPanel() {
            setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
            setBackground(new Color(38, 42, 50));
            this.portFont = getPortFont(28f);

            initAudio();
        }

        private void initAudio() {
            try {
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                if (AudioSystem.isLineSupported(info)) {
                    audioLine = (SourceDataLine) AudioSystem.getLine(info);
                    audioLine.open(format, 4096);
                    audioLine.start();

                    Thread audioThread = new Thread(this::audioLoop, "BuzzerAudioThread");
                    audioThread.setDaemon(true);
                    audioThread.start();
                }
            } catch (Exception ignored) {
                // Headless or audio unavailable
            }
        }

        private void audioLoop() {
            byte[] buffer = new byte[1024];
            double phase = 0.0;

            while (audioRunning) {
                int note;
                synchronized (lock) {
                    while (currentNote == 0 && audioRunning) {
                        try {
                            lock.wait(50);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    note = currentNote;
                }

                if (!audioRunning) break;

                if (note > 0 && audioLine != null) {
                    double freq = 440.0 * Math.pow(2.0, (note - 69) / 12.0);
                    double phaseStep = (2.0 * Math.PI * freq) / SAMPLE_RATE;

                    for (int i = 0; i < buffer.length; i++) {
                        // 8-bit square wave with smoothed corners for chiptune warmth
                        double sinVal = Math.sin(phase);
                        byte sample = (byte) (sinVal >= 0 ? 50 : -50);
                        buffer[i] = sample;
                        phase += phaseStep;
                        if (phase >= 2.0 * Math.PI) {
                            phase -= 2.0 * Math.PI;
                        }
                    }
                    audioLine.write(buffer, 0, buffer.length);
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {}
                }
            }
        }

        public void setNote(int note) {
            synchronized (lock) {
                if (stopTask != null && !stopTask.isDone()) {
                    stopTask.cancel(false);
                }
                this.currentNote = Math.max(0, Math.min(127, note));
                this.busy = (currentNote > 0);
                lock.notifyAll();
            }
            repaint();
        }

        public void setDuration(int unitsOf10ms) {
            if (unitsOf10ms <= 0) {
                setNote(0);
                return;
            }
            int durationMs = unitsOf10ms * 10;
            synchronized (lock) {
                if (stopTask != null && !stopTask.isDone()) {
                    stopTask.cancel(false);
                }
                busy = true;
                stopTask = scheduler.schedule(() -> {
                    synchronized (lock) {
                        currentNote = 0;
                        busy = false;
                        lock.notifyAll();
                    }
                    repaint();
                }, durationMs, TimeUnit.MILLISECONDS);
            }
        }

        public int getNote() {
            return currentNote;
        }

        public boolean isPlaying() {
            return busy;
        }

        public boolean isSounding() {
            return currentNote > 0;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Gray background extending to edges
            g2.setColor(new Color(38, 42, 50));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Top highlight and bottom shadow
            g2.setColor(new Color(65, 72, 85));
            g2.drawLine(0, 0, getWidth(), 0);
            g2.setColor(new Color(22, 24, 28));
            g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);

            boolean active = isSounding();

            // Speaker grill at left (cx=65, cy=65, radius=42)
            int scx = 65, scy = 65, srad = 42;
            Point2D spCenter = new Point2D.Float(scx, scy);
            RadialGradientPaint spPaint = new RadialGradientPaint(
                spCenter, srad,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{new Color(50, 55, 65), new Color(30, 33, 40), new Color(20, 22, 26)}
            );
            g2.setPaint(spPaint);
            g2.fillOval(scx - srad, scy - srad, srad * 2, srad * 2);

            g2.setColor(new Color(80, 86, 98));
            g2.drawOval(scx - srad, scy - srad, srad * 2, srad * 2);

            // Concentric grill holes
            g2.setColor(active ? new Color(255, 200, 50) : new Color(15, 17, 20));
            for (int r = 12; r <= 32; r += 10) {
                int count = r / 3;
                for (int a = 0; a < count; a++) {
                    double angle = a * (2 * Math.PI / count);
                    int hx = (int) (scx + r * Math.cos(angle));
                    int hy = (int) (scy + r * Math.sin(angle));
                    g2.fillOval(hx - 2, hy - 2, 4, 4);
                }
            }

            // Sound waves animation if active
            if (active) {
                waveStep = (waveStep + 1) % 4;
                g2.setColor(new Color(100, 220, 255, 180));
                g2.setStroke(new BasicStroke(2.0f));
                for (int w = 1; w <= 3; w++) {
                    int wRad = srad + w * 10;
                    g2.drawArc(scx - wRad, scy - wRad, wRad * 2, wRad * 2, -35, 70);
                }
            }

            // Right info panel
            int infoX = 145;

            // Status label
            Font boldFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
            g2.setFont(boldFont);
            if (active) {
                g2.setColor(new Color(50, 220, 100)); // Green ON
                g2.fillOval(infoX, 22, 10, 10);
                g2.drawString("SOUND ON", infoX + 16, 31);
            } else {
                g2.setColor(new Color(140, 145, 160)); // Gray IDLE
                g2.fillOval(infoX, 22, 10, 10);
                g2.drawString("IDLE", infoX + 16, 31);
            }

            // Note description
            Font noteFont = new Font(Font.MONOSPACED, Font.BOLD, 14);
            g2.setFont(noteFont);
            g2.setColor(new Color(240, 240, 120));

            String noteText;
            if (currentNote > 0) {
                int noteIdx = currentNote % 12;
                int octave = (currentNote / 12) - 1;
                double freq = 440.0 * Math.pow(2.0, (currentNote - 69) / 12.0);
                noteText = String.format("%s%d (%.1f Hz)", NOTE_NAMES[noteIdx], octave, freq);
            } else {
                noteText = "-- (Silence)";
            }
            g2.drawString(noteText, infoX, 58);

            // Port numbers "04/05" (calés en bas à droite)
            g2.setFont(portFont);
            g2.setColor(new Color(210, 220, 20));
            int portX = getWidth() - g2.getFontMetrics().stringWidth("04/05") - 10;
            g2.drawString("04/05", portX, getHeight() - 13);

            g2.dispose();
        }
    }
}

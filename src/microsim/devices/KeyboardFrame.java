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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import microsim.simulator.Utils;

/**
 * The KeyboardFrame class simulates a keyboard input device.
 * When the CPU needs to read a character, this window is displayed and waits
 * for a key press. The ASCII code of the pressed key is then sent back to the
 * CPU. This class uses a blocking queue to handle the asynchronous nature of

 * user input. It is implemented as a singleton.
 */
public class KeyboardFrame extends AbstractDevice {

    /** The singleton instance of the KeyboardFrame. */
    public static final KeyboardFrame INSTANCE = new KeyboardFrame(); // Singleton

    private static final String TITLE_STRING = "Keyboard Input";
    private static final String BACKGROUND_IMAGE_NAME = "devices/keyboard.png";
    
    /** A blocking queue to hold the ASCII code of the last key pressed. */
    private final BlockingQueue<Integer> keyQueue = new ArrayBlockingQueue<>(1);
    private final KeyboardPanel keyboardPanel;

    /**
     * Private constructor for the KeyboardFrame (Singleton pattern).
     * Initializes the panel and sets up the frame properties.
     */
    private KeyboardFrame() {

        keyboardPanel = new KeyboardPanel(keyQueue);
        setContentPane(keyboardPanel);
        pack(); // Automatically size the frame
        
        setTitle(TITLE_STRING);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        setVisible(false);
    }

    /**
     * Waits for a key to be pressed and returns its ASCII code.
     * This method is blocking and will pause the simulation thread until a key
     * is received. It makes the keyboard window visible and waits for the
     * KeyListener to put a value into the blocking queue.
     *
     * @return The ASCII code of the pressed key, or -1 if interrupted.
     */
    public int getAsciiCode() {
        // Ensure the window is visible to receive input
        SwingUtilities.invokeLater(() -> setVisible(true));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        try {
            keyboardPanel.setWaitingStatus(true);
            // This will block until a key is put into the queue by the KeyListener
            int ascii = keyQueue.take();
            setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            return ascii;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1; // Indicate an error or interruption
        } finally {
            keyboardPanel.setWaitingStatus(false);
        }
    }

    /**
     * The KeyboardPanel is the custom component that displays the keyboard image
     * and listens for key presses.
     */
    private static class KeyboardPanel extends JPanel implements KeyListener {
        private BufferedImage backgroundImage;
        private final BlockingQueue<Integer> keyQueue;
        private boolean waitingStatus = false;
        
        /**
         * Constructs the KeyboardPanel.
         * @param keyQueue The blocking queue to which pressed keys will be added.
         */
        public KeyboardPanel(BlockingQueue<Integer> keyQueue) {
            this.keyQueue = keyQueue;
            backgroundImage = Utils.loadBufferedImage(BACKGROUND_IMAGE_NAME);
            addKeyListener(this);
            setFocusable(true);
            setFocusTraversalKeysEnabled(false); // Allow us to capture TAB, etc.
        }

        /**
         * Sets the visual waiting status of the panel.
         * @param value true to show the waiting overlay, false otherwise.
         */
        public void setWaitingStatus(boolean value) {
            this.waitingStatus = value;
            this.repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            if (backgroundImage != null) {
                return new Dimension(backgroundImage.getWidth(), backgroundImage.getHeight());
            } else {
                return new Dimension(400, 150); // Fallback size
            }
        }

        /**
         * Paints the component, including the background image and a red overlay
         * when it is in a "waiting for input" state.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(backgroundImage, 0, 0, this);
            if (waitingStatus) {
                g.setColor(new Color(255, 0, 0, 80));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }
        
        /**
         * Handles typed keys (printable characters).
         * Adds the character's ASCII value to the queue.
         */
        @Override
        public void keyTyped(KeyEvent e) {
            char c = e.getKeyChar();
            // Check for standard printable ASCII characters
            if (c >= 32 && c <= 126) {
                keyQueue.offer((int) c);
            }
        }

        /**
         * Handles pressed keys, focusing on special keys like Enter, Backspace, etc.
         * Adds the corresponding ASCII value to the queue.
         */
        @Override
        public void keyPressed(KeyEvent e) {
            // Handle special keys that don't trigger keyTyped
            int keyCode = e.getKeyCode();
            int asciiValue = -1;

            switch (keyCode) {
                case KeyEvent.VK_ENTER:
                    asciiValue = 0x0D; // Carriage Return
                    break;
                case KeyEvent.VK_BACK_SPACE:
                    asciiValue = 0x08; // Backspace
                    break;
                case KeyEvent.VK_TAB:
                    asciiValue = 0x09; // Tab
                    break;
                case KeyEvent.VK_ESCAPE:
                    asciiValue = 0x1B; // Escape
                    break;
            }

            if (asciiValue != -1) {
                keyQueue.offer(asciiValue);
            }
        }

        /**
         * Not used, but required by the KeyListener interface.
         */
        @Override
        public void keyReleased(KeyEvent e) {
            // Not used
        }
    }
}
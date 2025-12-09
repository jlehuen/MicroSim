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

import java.awt.Font;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import microsim.simulator.RAM;
import microsim.simulator.Utils;

/**
 * The AsciiFrame class creates a graphical window to display a 4x16 grid of
 * ASCII characters from a specific range of memory addresses (0xC0 to 0xFF).
 * This class implements the Singleton pattern to ensure that only one instance
 * of the ASCII display window exists in the application.
 */
public class AsciiFrame extends AbstractDevice {

    /** The singleton instance of the AsciiFrame. */
    public static final AsciiFrame INSTANCE = new AsciiFrame(); // Singleton
    
    private static final String TITLE_STRING = "ASCII Display";
    private static final String FONT_FILE_NAME = "fonts/Glass_TTY_VT220.ttf";
    private static final String BACKGROUND_IMAGE_NAME = "devices/screen.png";

    private static final int ROWS = 4;
    private static final int COLS = 16;
    private static final int START_ADDRESS = 0xC0;
    
    private final JTextArea textArea;
    private Font customFont;
    private Font defaultFont;

    /**
     * A custom JPanel that draws a background image, scaled to fit the panel size.
     */
    private class ImagePanel extends JPanel {
        private Image backgroundImage;

        /**
         * Creates a new ImagePanel with a background image.
         * @param fileName The path to the background image file.
         */
        public ImagePanel(String fileName) {
            backgroundImage = Utils.loadBufferedImage(fileName);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, this.getWidth(), this.getHeight(), this);
            }
        }
    }

    /**
     * Constructs a new AsciiFrame.
     * This private constructor is part of the Singleton pattern. It initializes the
     * graphical window, text area, and a popup menu for font selection.
     */
    private AsciiFrame() {
        ImagePanel imagePanel = new ImagePanel(BACKGROUND_IMAGE_NAME);
        imagePanel.setLayout(new BorderLayout());

        // Try to Load TTY_VT220 font
        defaultFont = new Font(Font.MONOSPACED, Font.PLAIN, 18);
        customFont = Utils.loadFont(FONT_FILE_NAME, 22f);
        if (customFont == null) {
            customFont = defaultFont; // Initialize customFont with fallback
        }

        textArea = new JTextArea(ROWS, COLS);
        textArea.setForeground(Color.GREEN); // Green text
        textArea.setOpaque(false); // Make transparent
        textArea.setBackground(new Color(0,0,0,0)); // Transparent background
        textArea.setBorder(new EmptyBorder(new Insets(16, 16, 16, 16)));
        textArea.setFont(defaultFont);
        textArea.setEditable(false);
        
        imagePanel.add(textArea, BorderLayout.CENTER);
        setContentPane(imagePanel);
        pack(); // Automatically size the frame

        // Popup menu for font selection
        JPopupMenu popupMenu = new JPopupMenu();
        ButtonGroup fontGroup = new ButtonGroup();

        JRadioButtonMenuItem classicFontItem = new JRadioButtonMenuItem("Modern : Monospaced");
        classicFontItem.setSelected(true);
        classicFontItem.addActionListener(e -> textArea.setFont(defaultFont));
        fontGroup.add(classicFontItem);
        popupMenu.add(classicFontItem);

        JRadioButtonMenuItem modernFontItem = new JRadioButtonMenuItem("Vintage : VT100 TTY");
        modernFontItem.addActionListener(e -> textArea.setFont(customFont));
        fontGroup.add(modernFontItem);
        popupMenu.add(modernFontItem);

        // Disable modern font option if it failed to load
        if (customFont == defaultFont) modernFontItem.setEnabled(false);

        textArea.addMouseListener(new MouseAdapter() {
            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
            @Override
            public void mousePressed(MouseEvent e) { showPopup(e); }
            @Override
            public void mouseReleased(MouseEvent e) { showPopup(e); }
        });
        
        setTitle(TITLE_STRING);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setAlwaysOnTop(true);
        setResizable(false);
        setVisible(false);
    }

    /**
     * Updates the ASCII display with the current content from the specified
     * memory range (0xC0-0xFF). This method is called periodically to reflect
     * changes in the RAM.
     *
     * @param memory The RAM unit to read from.
     */
    public void update(RAM memory) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int address = START_ADDRESS + row * COLS + col;
                int value = memory.load(address);
                char c = (char) value;
                if (Character.isISOControl(c)) c = '?';
                sb.append(c);
            }
            sb.append("\n");
        }

        SwingUtilities.invokeLater(() -> {
            textArea.setText(sb.toString());
        });
    }
}
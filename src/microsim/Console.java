/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.2
 * @since 2026-01-05
 *
 * License: GNU General Public License v3.0
 */

package microsim;

import java.awt.Insets;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyleContext;

/**
 * The Console class represents a simple console output area
 * where text can be printed with different colors.
 */
public class Console extends JScrollPane {
    
	private JTextPane textPane;
	private StyledDocument document;
	private Style defaultStyle;

    public Console() {

		document = new DefaultStyledDocument(new StyleContext());
		textPane = new JTextPane(document);
		textPane.setMargin(new Insets(5, 5, 5, 5));
		textPane.setBackground(Color.BLACK);
		textPane.setFocusable(false);
		textPane.setEditable(false);

		String fontName = MicroSim.SYSTEM.equals("Windows") ? "Consolas" : "Monospaced";

		// Set default style
		defaultStyle = textPane.addStyle("Style", null);
		StyleConstants.setFontFamily(defaultStyle, fontName);
		StyleConstants.setFontSize(defaultStyle, 14);

		setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		setBorder(BorderFactory.createEmptyBorder());
		setViewportView(textPane);
    }

	/** 
	 * Prints a string to the console with the specified color.
	 * @param str The string to print.
	 * @param color The color of the text.
	 */
	private void print(String str, Color color) {
		SwingUtilities.invokeLater(() -> {
			try {
				StyleConstants.setForeground(defaultStyle, color);
				document.insertString(document.getLength(), str, defaultStyle);
				textPane.setCaretPosition(document.getLength());
			}
			catch (BadLocationException e) {
				e.printStackTrace();
			}
		});
	}

	public void println(String str, Color color) {
		print(str + "\n", color);
	}

	public void clear() {
		SwingUtilities.invokeLater(() -> {
			textPane.setText("");
		});
	}
}

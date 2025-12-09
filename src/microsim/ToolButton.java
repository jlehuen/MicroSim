/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.0
 * @since 2025-12-09
 *
 * License: GNU General Public License v3.0
 */

package microsim;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;

/**
 * The ToolButton class is a specialized JButton designed for use in the
 * application's toolbar. It simplifies the creation of toolbar buttons by
 * associating an identifier with each button, which is used to dispatch
 * actions in the ToolBar class.
 */
public class ToolButton extends JButton implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	/** The size of the button, though not currently used. */
	public static final int size = 11;

	/** The identifier for the button's action. */
	protected String ident;
	/** The text label for the button (currently not displayed). */
	protected String text;
	/** The toolbar that this button belongs to. */
	protected ToolBar toolbar;

	/**
	 * Constructs a new ToolButton.
	 *
	 * @param ident   The identifier for the action this button triggers.
	 * @param text    The text for the button (currently not shown).
	 * @param tiptext The tooltip text to display on hover.
	 * @param icon    The icon to display on the button.
	 * @param toolbar The parent toolbar that will handle the action.
	 */
	public ToolButton(String ident, String text, String tiptext, Icon icon, ToolBar toolbar) {
		super(icon);
		this.text = text;
		this.ident = ident;
		this.toolbar = toolbar;
		setToolTipText(tiptext);
		//setText(text);
        addActionListener(this);
	}

	/**
	 * Handles the action event when the button is clicked.
	 * It calls the `action` method of the parent toolbar, passing its identifier.
	 *
	 * @param e The ActionEvent object.
	 */
	public void actionPerformed(ActionEvent e) {
		toolbar.action(ident);
	}
}

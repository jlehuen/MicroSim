/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.0
 * @since 2025-12-09
 *
 * License: GNU General Public License v3.0
 */

package microsim.editor;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import microsim.MicroSim;
import microsim.simulator.Simulator;

/**
 * The EditorPanel class provides a sophisticated text editor for writing
 * assembly code. It features syntax highlighting, undo/redo functionality,
 * file operations (new, open, save), and drag-and-drop support for opening files.
 * It also manages the "dirty" state of the file and updates the main frame's
 * title accordingly.
 */
public class EditorPanel extends JPanel {

    private MicroSim mainframe;
    private RSyntaxTextArea textArea;
    private boolean dirtyFlag = false;
    private File currentFile;
    private static File lastDirectory = null;
    
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 0, 100); // Yellow with transparency

    /**
     * Constructs the editor panel.
     * @param frame The main frame of the application.
     */
    public EditorPanel(MicroSim frame) {
        this.mainframe = frame;
        setLayout(new BorderLayout());
        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
        textArea.setCodeFoldingEnabled(false);
        textArea.setAntiAliasingEnabled(true);
        
        TransferHandler defaultHandler = textArea.getTransferHandler();
        Simulator simulator = mainframe.getSimulator();
        textArea.setTransferHandler(new EditorTransferHandler(this, defaultHandler, simulator));

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setDirtyFlag(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setDirtyFlag(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setDirtyFlag(true);
            }
        });

        RTextScrollPane sp = new RTextScrollPane(textArea);
        sp.setBorder(BorderFactory.createEmptyBorder());
        add(sp, BorderLayout.CENTER);
        updateFrameTitle();
    }

    /**
     * Returns the underlying text area component.
     * @return The RSyntaxTextArea instance.
     */
    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    /**
     * Undoes the last edit action.
     */
    public void undo() {
		textArea.undoLastAction();
	}

    /**
     * Redoes the last undone edit action.
     */
	public void redo() {
		textArea.redoLastAction();
	}

    /**
     * Checks if the editor has unsaved changes.
     * @return true if the file has been modified, false otherwise.
     */
    public boolean isDirty() {
        return dirtyFlag;
    }

    /**
     * Sets the dirty flag indicating whether the file has unsaved changes.
     * @param dirtyFlag true to mark the file as modified, false otherwise.
     */
    public void setDirtyFlag(boolean dirtyFlag) {
        if (this.dirtyFlag != dirtyFlag) {
            this.dirtyFlag = dirtyFlag;
            updateFrameTitle();
        }
    }

    /**
     * Updates the main frame's title to reflect the current file name and modification state.
     */
    private void updateFrameTitle() {
        String baseTitle = mainframe.BASE_TITLE;
        String newTitle = baseTitle;

        if (currentFile != null) {
            newTitle += " - " + currentFile.getName();
        } else {
            newTitle += " - [untitled]";
        }

        if (dirtyFlag) {
            newTitle += " [modified]";
        }
        
        mainframe.setTitle(newTitle);
    }

    /**
     * Creates a new, empty file in the editor.
     * Prompts the user to save if there are unsaved changes.
     */
    public void newFile() {
        if (isDirty()) {
            int result = JOptionPane.showConfirmDialog(mainframe, "You have unsaved changes. Do you want to save them?", "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                saveFile();
                if (isDirty()) return; // Check if save was cancelled
            }
            else if (result == JOptionPane.CANCEL_OPTION) return;
        }
        textArea.setText("");
        currentFile = null;
        setDirtyFlag(false);
    }

    /**
     * Opens a file chooser to let the user select a file to open.
     * Prompts the user to save if there are unsaved changes.
     */
    public void openFile() {
        if (isDirty()) {
            int result = JOptionPane.showConfirmDialog(mainframe, "You have unsaved changes. Do you want to save them before opening a new file?", "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                saveFile();
                if (isDirty()) return; // Check if save was cancelled
            } else if (result == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        JFileChooser fileChooser = new JFileChooser();
        if (lastDirectory != null) {
            fileChooser.setCurrentDirectory(lastDirectory);
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("ASM Files", "asm", "txt");
        fileChooser.setFileFilter(filter);
        if (fileChooser.showOpenDialog(mainframe) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            lastDirectory = file.getParentFile();
            loadFile(file);
        }
    }

    /**
     * Loads the content of the specified file into the editor.
     * @param file The file to load.
     */
    public void loadFile(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            textArea.setText(content);
            currentFile = file;
            setDirtyFlag(false);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Saves the current content of the editor.
     * If the file is new, it opens a "Save As" dialog.
     */
    public void saveFile() {
        if (currentFile != null) {
            try {
                Files.write(currentFile.toPath(), textArea.getText().getBytes());
                setDirtyFlag(false);
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        else {
            JFileChooser fileChooser = new JFileChooser();
            if (lastDirectory != null) {
                fileChooser.setCurrentDirectory(lastDirectory);
            }
            FileNameExtensionFilter filter = new FileNameExtensionFilter("ASM Files", "asm");
            fileChooser.setFileFilter(filter);
            if (fileChooser.showSaveDialog(mainframe) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                lastDirectory = file.getParentFile();
                String filePath = file.getAbsolutePath();

                // Remove any existing extension then append .asm extension
                int dotIndex = filePath.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex > filePath.lastIndexOf(File.separator)) {
                    filePath = filePath.substring(0, dotIndex);
                }
                file = new File(filePath + ".asm");
                
                try {
                    Files.write(file.toPath(), textArea.getText().getBytes());
                    currentFile = file;
                    setDirtyFlag(false);
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns the file currently being edited.
     * @return The current file, or null if the file is new and unsaved.
     */
    public File getCurrentFile() {
        return currentFile;
    }

    /**
     * Highlights a specific line in the editor.
     * Used to show the current instruction being executed.
     * @param line The line number to highlight.
     * @return A tag that can be used to remove the highlight later.
     */
    public Object highlightLine(int line) {
        try {
            return textArea.addLineHighlight(line, HIGHLIGHT_COLOR);
        }
        catch (BadLocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Removes a previously added line highlight.
     * @param tag The tag returned by highlightLine().
     */
    public void removeLineHighlight(Object tag) {
        textArea.removeLineHighlight(tag);
    }

    /**
     * Reformats the code in the editor using the Formatter class.
     */
    public void reformat() {
        String text = textArea.getText();
        int tabSize = textArea.getTabSize();
        String reformattedText = Formatter.reformat(text, tabSize);
        textArea.setText(reformattedText);
    }
}

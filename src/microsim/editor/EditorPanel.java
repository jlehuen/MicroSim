/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.1
 * @since 2025-12-17
 *
 * License: GNU General Public License v3.0
 */

package microsim.editor;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import microsim.simulator.Utils;
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
    
    private static final Color HIGHLIGHT_COLOR = new Color(0, 255, 0, 100); // Green with transparency
    private static final Color ERROR_HIGHLIGHT_COLOR = new Color(255, 0, 0, 100); // Red with transparency
    private Object errorHighlightTag = null;

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
                removeErrorHighlight();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setDirtyFlag(true);
                removeErrorHighlight();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setDirtyFlag(true);
                removeErrorHighlight();
            }
        });

        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                removeErrorHighlight();
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
     * Returns the file currently being edited.
     * @return The current file, or null if the file is new and unsaved.
     */
    public File getCurrentFile() {
        return currentFile;
    }  

    /**
     * Sets new content in the editor, marking it as dirty and clearing the current file.
     * @param content The new content to set in the editor.
     */
    public void setNewContent(String content) {
        textArea.setText(content);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86); // Set to assembly for compiled code
        currentFile = null;
        setDirtyFlag(true);
    }

    /**
     * Returns the file extension of the current file.
     * @return The file extension as a string.
     */
    public boolean isCFile () {
        if (currentFile == null) return false;
        String ext = Utils.getFileExtension(currentFile);
        return "c".equalsIgnoreCase(ext);
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
        FileNameExtensionFilter cFilter = new FileNameExtensionFilter("C Files", "c");
        FileNameExtensionFilter asmFilter = new FileNameExtensionFilter("ASM Files", "asm");
        fileChooser.addChoosableFileFilter(cFilter);
        fileChooser.addChoosableFileFilter(asmFilter);
        fileChooser.setFileFilter(asmFilter); // Set ASM as default
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

            // Set syntax based on file extension
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".c")) {
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
            } else if (fileName.endsWith(".asm")) {
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
            } else {
                textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            }

            textArea.discardAllEdits();
            currentFile = file;
            mainframe.getToolBar().update();
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
            saveFile(currentFile);
        }
        else saveFileAs();
    }

    /**
     * Opens a "Save As" dialog to let the user choose a file to save the content to.
     */
    public void saveFileAs() {
        JFileChooser fileChooser = new JFileChooser();
        if (lastDirectory != null) {
            fileChooser.setCurrentDirectory(lastDirectory);
        }
        FileNameExtensionFilter cFilter = new FileNameExtensionFilter("C Files", "c");
        FileNameExtensionFilter asmFilter = new FileNameExtensionFilter("ASM Files", "asm");
        fileChooser.addChoosableFileFilter(cFilter);
        fileChooser.addChoosableFileFilter(asmFilter);
        fileChooser.setFileFilter(asmFilter); // Set ASM as default
        if (fileChooser.showSaveDialog(mainframe) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            lastDirectory = file.getParentFile();
            String filePath = file.getAbsolutePath();

            // Determine which filter was selected and append the corresponding extension
            FileNameExtensionFilter selectedFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();
            String[] extensions = selectedFilter.getExtensions();
            String extensionToAppend = extensions.length > 0 ? "." + extensions[0] : "";

            // Remove any existing extension then append the determined extension
            int dotIndex = filePath.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex > filePath.lastIndexOf(File.separator)) {
                filePath = filePath.substring(0, dotIndex);
            }
            file = new File(filePath + extensionToAppend);
            saveFile(file);
        }
    }

    /**
     * Saves the content of the editor to the specified file.
     * @param file The file to save to.
     */
    private void saveFile(File file) {
        try {
            Files.write(file.toPath(), textArea.getText().getBytes());
            currentFile = file;
            setDirtyFlag(false);
            mainframe.getToolBar().update();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
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
     * Highlights a specific line in the editor with an error color.
     * Used to show the location of an assembly error.
     * @param line The line number to highlight.
     */
    public void highlightErrorLine(int line) {
        try {
            removeErrorHighlight();
            textArea.setHighlightCurrentLine(false);
            errorHighlightTag = textArea.addLineHighlight(line, ERROR_HIGHLIGHT_COLOR);
            textArea.revalidate();
            textArea.repaint();
        }
        catch (BadLocationException e) {
            e.printStackTrace();
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
     * Removes a previously added error line highlight.
     */
    public void removeErrorHighlight() {
        if (errorHighlightTag != null) {
            textArea.removeLineHighlight(errorHighlightTag);
            errorHighlightTag = null;
            textArea.setHighlightCurrentLine(true);
        }
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
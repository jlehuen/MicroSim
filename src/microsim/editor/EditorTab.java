/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.2
 * @since 2026-01-05
 *
 * License: GNU General Public License v3.0
 */

package microsim.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import microsim.MicroSim;
import microsim.simulator.Simulator;
import microsim.simulator.Utils;

/**
 * The EditorTab class represents a single document open in the editor.
 * It manages its own RSyntaxTextArea, file reference, syntax style,
 * line highlights, and modification dirty flag.
 */
public class EditorTab extends JPanel {

    private final EditorPanel parentPanel;
    private final MicroSim mainframe;
    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;

    private File currentFile;
    private String title;
    private boolean dirtyFlag = false;

    private static final Color HIGHLIGHT_COLOR = new Color(0, 255, 0, 100);
    private static final Color ERROR_HIGHLIGHT_COLOR = new Color(255, 0, 0, 100);
    private Object errorHighlightTag = null;

    /**
     * Constructs a new editor tab.
     * @param parentPanel The containing EditorPanel.
     * @param mainframe The main application frame.
     * @param title The initial title of the tab.
     * @param file The file associated with this tab, or null if new/untitled.
     */
    public EditorTab(EditorPanel parentPanel, MicroSim mainframe, String title, File file) {
        this.parentPanel = parentPanel;
        this.mainframe = mainframe;
        this.title = title;
        this.currentFile = file;

        setLayout(new BorderLayout());

        textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
        textArea.setCodeFoldingEnabled(false);
        textArea.setAntiAliasingEnabled(true);

        TransferHandler defaultHandler = textArea.getTransferHandler();
        Simulator simulator = mainframe.getSimulator();
        textArea.setTransferHandler(new EditorTransferHandler(parentPanel, defaultHandler, simulator));

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

        scrollPane = new RTextScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        updateSyntaxStyle();
    }

    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    public RTextScrollPane getScrollPane() {
        return scrollPane;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
        if (file != null) {
            this.title = file.getName();
            updateSyntaxStyle();
        }
        parentPanel.updateTabHeader(this);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        parentPanel.updateTabHeader(this);
    }

    public boolean isDirty() {
        return dirtyFlag;
    }

    public void setDirtyFlag(boolean dirty) {
        if (this.dirtyFlag != dirty) {
            this.dirtyFlag = dirty;
            parentPanel.updateTabHeader(this);
            parentPanel.updateFrameTitle();
        }
    }

    public boolean isCFile() {
        if (currentFile != null) {
            String ext = Utils.getFileExtension(currentFile);
            return "c".equalsIgnoreCase(ext);
        }
        return title != null && title.toLowerCase().endsWith(".c");
    }

    public void updateSyntaxStyle() {
        String name = currentFile != null ? currentFile.getName().toLowerCase() : (title != null ? title.toLowerCase() : "");
        if (name.endsWith(".c")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_C);
        } else if (name.endsWith(".asm")) {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
        } else {
            textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_ASSEMBLER_X86);
        }
    }

    public void undo() {
        textArea.undoLastAction();
    }

    public void redo() {
        textArea.redoLastAction();
    }

    public void reformat() {
        String text = textArea.getText();
        int tabSize = textArea.getTabSize();
        String reformattedText = Formatter.reformat(text, tabSize);
        textArea.setText(reformattedText);
    }

    public void loadContent(File file) throws IOException {
        String content = new String(Files.readAllBytes(file.toPath()));
        textArea.setText(content);
        setCurrentFile(file);
        textArea.discardAllEdits();
        setDirtyFlag(false);
    }

    public void saveTo(File file) throws IOException {
        Files.write(file.toPath(), textArea.getText().getBytes());
        setCurrentFile(file);
        setDirtyFlag(false);
    }

    public Object highlightLine(int line) {
        try {
            return textArea.addLineHighlight(line, HIGHLIGHT_COLOR);
        } catch (BadLocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void highlightErrorLine(int line) {
        try {
            removeErrorHighlight();
            textArea.setHighlightCurrentLine(false);
            errorHighlightTag = textArea.addLineHighlight(line, ERROR_HIGHLIGHT_COLOR);
            textArea.revalidate();
            textArea.repaint();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void removeLineHighlight(Object tag) {
        if (tag != null) {
            textArea.removeLineHighlight(tag);
        }
    }

    public void removeErrorHighlight() {
        if (errorHighlightTag != null) {
            textArea.removeLineHighlight(errorHighlightTag);
            errorHighlightTag = null;
            textArea.setHighlightCurrentLine(true);
        }
    }
}

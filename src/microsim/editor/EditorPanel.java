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
import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import com.formdev.flatlaf.FlatClientProperties;

import microsim.MicroSim;
import microsim.simulator.Utils;

/**
 * The EditorPanel class provides a tabbed multi-file text editor.
 * It manages multiple EditorTab instances, allows opening/saving multiple files,
 * closing tabs, syntax highlighting (ASM / C), undo/redo, and line highlights.
 */
public class EditorPanel extends JPanel {

    private final MicroSim mainframe;
    private final JTabbedPane tabbedPane;
    private static File lastDirectory = null;
    private int untitledCounter = 1;

    private static final float DEFAULT_FONT_SIZE = RSyntaxTextArea.getDefaultFont().getSize2D();
    private static final float MIN_FONT_SIZE = 8f;
    private static final float MAX_FONT_SIZE = 36f;
    private static final float FONT_SIZE_STEP = 2f;
    private float fontSize = DEFAULT_FONT_SIZE;

    /**
     * Constructs the multi-tab EditorPanel.
     * @param mainframe The main application frame.
     */
    public EditorPanel(MicroSim mainframe) {
        this.mainframe = mainframe;
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // FlatLaf closable tabs support
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, "Close tab");
        tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK,
                (BiConsumer<JTabbedPane, Integer>) (pane, tabIndex) -> closeTab(tabIndex));

        // When switching tabs, update toolbar and window title
        tabbedPane.addChangeListener(e -> {
            updateFrameTitle();
            if (this.mainframe.getToolBar() != null) {
                this.mainframe.getToolBar().update();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        // Open initial empty document
        addNewTab();
    }

    /**
     * Creates and adds a new empty editor tab ("Untitled-X.asm").
     * @return The newly created EditorTab.
     */
    public EditorTab addNewTab() {
        String title = "Untitled-" + (untitledCounter++) + ".asm";
        return addNewTab(title, "", null);
    }

    /**
     * Creates and adds a new editor tab with specific title, content and file.
     * @param title The tab title.
     * @param content Initial text content.
     * @param file Associated file or null.
     * @return The created EditorTab.
     */
    public EditorTab addNewTab(String title, String content, File file) {
        EditorTab tab = new EditorTab(this, mainframe, title, file);
        if (content != null && !content.isEmpty()) {
            tab.getTextArea().setText(content);
            tab.getTextArea().discardAllEdits();
        }
        tabbedPane.addTab(title, tab);
        tabbedPane.setSelectedComponent(tab);
        updateTabHeader(tab);
        updateFrameTitle();
        if (mainframe.getToolBar() != null) {
            mainframe.getToolBar().update();
        }
        return tab;
    }

    /**
     * Returns the currently selected EditorTab, or null if none.
     * @return The active EditorTab.
     */
    public EditorTab getCurrentTab() {
        int index = tabbedPane.getSelectedIndex();
        if (index >= 0 && index < tabbedPane.getTabCount()) {
            return (EditorTab) tabbedPane.getComponentAt(index);
        }
        return null;
    }

    /**
     * Closes the tab at the given index, asking to save if modified.
     * If all tabs are closed, a new empty tab is automatically opened.
     * @param index Tab index to close.
     * @return true if closed, false if canceled by user.
     */
    public boolean closeTab(int index) {
        if (index < 0 || index >= tabbedPane.getTabCount()) {
            return false;
        }

        EditorTab tab = (EditorTab) tabbedPane.getComponentAt(index);
        if (tab.isDirty()) {
            int result = JOptionPane.showConfirmDialog(
                    mainframe,
                    "You have unsaved changes in '" + tab.getTitle() + "'. Do you want to save them before closing?",
                    "Unsaved Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                saveTab(tab);
                if (tab.isDirty()) {
                    return false; // User canceled save dialog
                }
            } else if (result == JOptionPane.CANCEL_OPTION) {
                return false;
            }
        }

        tabbedPane.removeTabAt(index);

        if (tabbedPane.getTabCount() == 0) {
            addNewTab();
        } else {
            updateFrameTitle();
            if (mainframe.getToolBar() != null) {
                mainframe.getToolBar().update();
            }
        }
        return true;
    }

    /**
     * Updates the tab header title and tooltip (appends * when dirty).
     * @param tab The tab to update.
     */
    public void updateTabHeader(EditorTab tab) {
        int index = tabbedPane.indexOfComponent(tab);
        if (index >= 0) {
            String label = tab.getTitle() + (tab.isDirty() ? " *" : "");
            tabbedPane.setTitleAt(index, label);
            tabbedPane.setToolTipTextAt(index,
                    tab.getCurrentFile() != null ? tab.getCurrentFile().getAbsolutePath() : tab.getTitle());
        }
    }

    /**
     * Updates the main window title based on the active tab.
     */
    public void updateFrameTitle() {
        EditorTab tab = getCurrentTab();
        String baseTitle = mainframe.BASE_TITLE;
        if (tab != null) {
            String fileName = tab.getCurrentFile() != null ? tab.getCurrentFile().getName() : tab.getTitle();
            String title = baseTitle + " - " + fileName;
            if (tab.isDirty()) {
                title += " [modified]";
            }
            mainframe.setTitle(title);
        } else {
            mainframe.setTitle(baseTitle);
        }
    }

    /**
     * Returns the RSyntaxTextArea of the active tab.
     * @return The active RSyntaxTextArea, or null.
     */
    public RSyntaxTextArea getTextArea() {
        EditorTab tab = getCurrentTab();
        return tab != null ? tab.getTextArea() : null;
    }

    /**
     * Undoes the last edit in the active tab.
     */
    public void undo() {
        EditorTab tab = getCurrentTab();
        if (tab != null) tab.undo();
    }

    /**
     * Redoes the last edit in the active tab.
     */
    public void redo() {
        EditorTab tab = getCurrentTab();
        if (tab != null) tab.redo();
    }

    /**
     * Returns the current file of the active tab.
     * @return File or null.
     */
    public File getCurrentFile() {
        EditorTab tab = getCurrentTab();
        return tab != null ? tab.getCurrentFile() : null;
    }

    /**
     * Sets new content in a new tab or the active tab (used after C compilation).
     * @param content Assembly code generated.
     */
    public void setNewContent(String content) {
        EditorTab activeTab = getCurrentTab();
        String asmTitle = "compiled.asm";
        if (activeTab != null && activeTab.getCurrentFile() != null) {
            String base = Utils.getFileNameWithoutExtension(activeTab.getCurrentFile());
            asmTitle = base + ".asm";
        }
        EditorTab asmTab = addNewTab(asmTitle, content, null);
        asmTab.setDirtyFlag(true);
    }

    /**
     * Checks if the active tab contains a C file.
     * @return true if C file.
     */
    public boolean isCFile() {
        EditorTab tab = getCurrentTab();
        return tab != null && tab.isCFile();
    }

    /**
     * Checks if the active tab has unsaved changes.
     * @return true if modified.
     */
    public boolean isDirty() {
        EditorTab tab = getCurrentTab();
        return tab != null && tab.isDirty();
    }

    /**
     * Sets the dirty flag of the active tab.
     * @param dirtyFlag true if modified.
     */
    public void setDirtyFlag(boolean dirtyFlag) {
        EditorTab tab = getCurrentTab();
        if (tab != null) {
            tab.setDirtyFlag(dirtyFlag);
        }
    }

    /**
     * Creates a new empty tab.
     */
    public void newFile() {
        addNewTab();
    }

    /**
     * Opens a file chooser to open a file.
     * Opens into a new tab (or switches to existing tab if already opened).
     */
    public void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        if (lastDirectory != null) {
            fileChooser.setCurrentDirectory(lastDirectory);
        }
        FileNameExtensionFilter cFilter = new FileNameExtensionFilter("C Files (*.c)", "c");
        FileNameExtensionFilter asmFilter = new FileNameExtensionFilter("ASM Files (*.asm)", "asm");
        fileChooser.addChoosableFileFilter(cFilter);
        fileChooser.addChoosableFileFilter(asmFilter);
        fileChooser.setFileFilter(asmFilter);

        if (fileChooser.showOpenDialog(mainframe) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            lastDirectory = file.getParentFile();
            loadFile(file);
        }
    }

    /**
     * Loads a file into a tab. If already opened, switches to that tab.
     * If the current tab is an untouched empty untitled document, reuses it.
     * Otherwise opens a new tab.
     * @param file The file to load.
     */
    public void loadFile(File file) {
        if (file == null || !file.exists()) return;

        // 1. Check if already open
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            EditorTab t = (EditorTab) tabbedPane.getComponentAt(i);
            if (t.getCurrentFile() != null && t.getCurrentFile().equals(file)) {
                tabbedPane.setSelectedIndex(i);
                return;
            }
        }

        // 2. Check if current tab is untouched untitled
        EditorTab current = getCurrentTab();
        if (current != null && current.getCurrentFile() == null && !current.isDirty()
                && current.getTextArea().getText().trim().isEmpty()) {
            try {
                current.loadContent(file);
                updateTabHeader(current);
                updateFrameTitle();
                if (mainframe.getToolBar() != null) mainframe.getToolBar().update();
                return;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        // 3. Otherwise create a new tab
        try {
            EditorTab tab = new EditorTab(this, mainframe, file.getName(), file);
            tab.loadContent(file);
            tabbedPane.addTab(file.getName(), tab);
            tabbedPane.setSelectedComponent(tab);
            updateTabHeader(tab);
            updateFrameTitle();
            if (mainframe.getToolBar() != null) mainframe.getToolBar().update();
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainframe, "Error reading file: " + file.getName(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Saves the active tab.
     */
    public void saveFile() {
        EditorTab tab = getCurrentTab();
        if (tab != null) {
            saveTab(tab);
        }
    }

    /**
     * Saves a specific tab. If it has no associated file, prompts with Save As.
     * @param tab The tab to save.
     */
    public void saveTab(EditorTab tab) {
        if (tab == null) return;
        if (tab.getCurrentFile() != null) {
            try {
                tab.saveTo(tab.getCurrentFile());
                updateTabHeader(tab);
                updateFrameTitle();
                if (mainframe.getToolBar() != null) mainframe.getToolBar().update();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(mainframe, "Error saving file: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            saveTabAs(tab);
        }
    }

    /**
     * Opens "Save As" dialog for active tab.
     */
    public void saveFileAs() {
        EditorTab tab = getCurrentTab();
        if (tab != null) {
            saveTabAs(tab);
        }
    }

    /**
     * Opens "Save As" dialog for a specific tab.
     * @param tab The tab to save as.
     */
    public void saveTabAs(EditorTab tab) {
        if (tab == null) return;

        JFileChooser fileChooser = new JFileChooser();
        if (lastDirectory != null) {
            fileChooser.setCurrentDirectory(lastDirectory);
        }
        FileNameExtensionFilter cFilter = new FileNameExtensionFilter("C Files (*.c)", "c");
        FileNameExtensionFilter asmFilter = new FileNameExtensionFilter("ASM Files (*.asm)", "asm");
        fileChooser.addChoosableFileFilter(cFilter);
        fileChooser.addChoosableFileFilter(asmFilter);

        if (tab.isCFile()) {
            fileChooser.setFileFilter(cFilter);
        } else {
            fileChooser.setFileFilter(asmFilter);
        }

        if (fileChooser.showSaveDialog(mainframe) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            lastDirectory = file.getParentFile();
            String filePath = file.getAbsolutePath();

            FileNameExtensionFilter selectedFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();
            String[] extensions = selectedFilter.getExtensions();
            String extensionToAppend = extensions.length > 0 ? "." + extensions[0] : "";

            int dotIndex = filePath.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex > filePath.lastIndexOf(File.separator)) {
                filePath = filePath.substring(0, dotIndex);
            }
            file = new File(filePath + extensionToAppend);

            try {
                tab.saveTo(file);
                updateTabHeader(tab);
                updateFrameTitle();
                if (mainframe.getToolBar() != null) mainframe.getToolBar().update();
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(mainframe, "Error saving file: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Highlights a line in the active tab.
     * @param line Line number.
     * @return Highlight tag.
     */
    public Object highlightLine(int line) {
        EditorTab tab = getCurrentTab();
        return tab != null ? tab.highlightLine(line) : null;
    }

    /**
     * Highlights an error line in the active tab.
     * @param line Line number.
     */
    public void highlightErrorLine(int line) {
        EditorTab tab = getCurrentTab();
        if (tab != null) tab.highlightErrorLine(line);
    }

    /**
     * Removes line highlight in active tab.
     * @param tag Highlight tag.
     */
    public void removeLineHighlight(Object tag) {
        EditorTab tab = getCurrentTab();
        if (tab != null) tab.removeLineHighlight(tag);
    }

    /**
     * Removes error highlight in active tab.
     */
    public void removeErrorHighlight() {
        EditorTab tab = getCurrentTab();
        if (tab != null) tab.removeErrorHighlight();
    }

    /**
     * Checks if the active tab has a breakpoint on the specified 0-based line.
     * @param line 0-based line index.
     * @return true if a breakpoint is set on that line.
     */
    public boolean hasBreakpointAtLine(int line) {
        EditorTab tab = getCurrentTab();
        return tab != null && tab.hasBreakpointAtLine(line);
    }

    /**
     * Reformats code in the active tab.
     */
    public void reformat() {
        EditorTab tab = getCurrentTab();
        if (tab != null) tab.reformat();
    }

    /**
     * Returns the default editor font size.
     * @return The default font size in points.
     */
    public float getDefaultFontSize() {
        return DEFAULT_FONT_SIZE;
    }

    /**
     * Returns the current editor font size shared across all tabs.
     * @return The current font size in points.
     */
    public float getEditorFontSize() {
        return fontSize;
    }

    /**
     * Updates the editor font size across all open tabs.
     * @param size New font size in points (clamped between 8 and 36).
     */
    public void setEditorFontSize(float size) {
        float clamped = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, size));
        if (Float.compare(clamped, this.fontSize) == 0) {
            return;
        }
        this.fontSize = clamped;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            EditorTab tab = (EditorTab) tabbedPane.getComponentAt(i);
            tab.applyFontSize(clamped);
        }
    }

    /**
     * Increases the editor font size by one step.
     */
    public void increaseFontSize() {
        setEditorFontSize(fontSize + FONT_SIZE_STEP);
    }

    /**
     * Decreases the editor font size by one step.
     */
    public void decreaseFontSize() {
        setEditorFontSize(fontSize - FONT_SIZE_STEP);
    }

    /**
     * Resets the editor font size to the default size.
     */
    public void resetFontSize() {
        setEditorFontSize(DEFAULT_FONT_SIZE);
    }
}

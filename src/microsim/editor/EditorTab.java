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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextScrollPane;

import microsim.MicroSim;
import microsim.simulator.CPU;
import microsim.simulator.RAM;
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

        textArea.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoverTooltip(e);
            }
        });

        scrollPane = new RTextScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setIconRowHeaderEnabled(true);
        Gutter gutter = scrollPane.getGutter();
        gutter.setBookmarkingEnabled(true);
        gutter.setBookmarkIcon(createBreakpointIcon());
        add(scrollPane, BorderLayout.CENTER);

        updateSyntaxStyle();
    }

    private static Icon createBreakpointIcon() {
        int size = 12;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(220, 38, 38));
        g2.fillOval(1, 1, size - 2, size - 2);
        g2.setColor(new Color(153, 27, 27));
        g2.drawOval(1, 1, size - 2, size - 2);
        g2.setColor(new Color(254, 202, 202));
        g2.fillOval(3, 3, 3, 3);
        g2.dispose();
        return new ImageIcon(img);
    }

    public boolean hasBreakpointAtLine(int line) {
        Gutter gutter = scrollPane.getGutter();
        if (gutter == null) return false;
        GutterIconInfo[] bookmarks = gutter.getBookmarks();
        if (bookmarks == null) return false;
        for (GutterIconInfo info : bookmarks) {
            try {
                int bLine = textArea.getLineOfOffset(info.getMarkedOffset());
                if (bLine == line) return true;
            } catch (BadLocationException ignored) {}
        }
        return false;
    }

    private void updateHoverTooltip(MouseEvent e) {
        Simulator sim = mainframe.getSimulator();
        if (sim == null || sim.getCPU() == null || sim.getRAM() == null) {
            textArea.setToolTipText(null);
            return;
        }
        int offset = textArea.viewToModel2D(e.getPoint());
        if (offset < 0) {
            textArea.setToolTipText(null);
            return;
        }
        try {
            String text = textArea.getText();
            if (offset >= text.length()) {
                textArea.setToolTipText(null);
                return;
            }
            int line = textArea.getLineOfOffset(offset);
            int lineStart = textArea.getLineStartOffset(line);
            int lineEnd = textArea.getLineEndOffset(line);
            String lineText = text.substring(lineStart, lineEnd);
            int semi = lineText.indexOf(';');
            if (semi != -1 && (offset - lineStart) >= semi) {
                textArea.setToolTipText(null);
                return;
            }

            int start = offset;
            int end = offset;
            while (start > lineStart && isTokenChar(text.charAt(start - 1))) start--;
            while (end < lineEnd && isTokenChar(text.charAt(end))) end++;
            if (start >= end) {
                textArea.setToolTipText(null);
                return;
            }

            String token = text.substring(start, end).trim();
            boolean bracketed = (start > lineStart && text.charAt(start - 1) == '[')
                    || (end < lineEnd && text.charAt(end) == ']');

            CPU cpu = sim.getCPU();
            RAM ram = sim.getRAM();
            int[] gpr = cpu.getRegisters();
            String upper = token.toUpperCase();

            int regVal = -1;
            String regName = null;
            switch (upper) {
                case "AL": regName = "AL"; regVal = gpr[0]; break;
                case "BL": regName = "BL"; regVal = gpr[1]; break;
                case "CL": regName = "CL"; regVal = gpr[2]; break;
                case "DL": regName = "DL"; regVal = gpr[3]; break;
                case "IP": regName = "IP"; regVal = cpu.getIP(); break;
                case "SP": regName = "SP"; regVal = cpu.getSP(); break;
                case "SR": regName = "SR"; regVal = cpu.getSR(); break;
            }

            if (regName != null) {
                String tip = regName + " = " + Utils.toHexString0x(regVal)
                        + " (dec: " + Utils.toSignedDecimalString(regVal)
                        + ", bin: " + Utils.toBinaryString(regVal) + ")";
                if (bracketed && regVal >= 0 && regVal < ram.getSize()) {
                    int memVal = ram.load(regVal);
                    tip += "  ->  RAM[" + Utils.toHexString0x(regVal) + "] = " + Utils.toHexString0x(memVal);
                }
                textArea.setToolTipText(tip);
                return;
            }

            if (bracketed || upper.startsWith("0X")) {
                int addr = -1;
                try {
                    addr = upper.startsWith("0X")
                            ? Integer.parseInt(upper.substring(2), 16)
                            : Integer.parseInt(upper);
                } catch (NumberFormatException ignored) {}
                if (addr >= 0 && addr < ram.getSize()) {
                    int memVal = ram.load(addr);
                    String ascii = (memVal >= 32 && memVal <= 126) ? " '" + (char) memVal + "'" : "";
                    textArea.setToolTipText("RAM[" + Utils.toHexString0x(addr) + "] = "
                            + Utils.toHexString0x(memVal) + " (dec: " + Utils.toSignedDecimalString(memVal) + ")" + ascii);
                    return;
                }
            }
            textArea.setToolTipText(null);
        } catch (BadLocationException ex) {
            textArea.setToolTipText(null);
        }
    }

    private static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
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
            Object tag = textArea.addLineHighlight(line, HIGHLIGHT_COLOR);
            textArea.repaint();
            return tag;
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
            textArea.repaint();
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

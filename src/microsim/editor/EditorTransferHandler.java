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

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

import microsim.simulator.Simulator;

/**
 * The EditorTransferHandler class provides combined drag-and-drop functionality
 * for files and default text transfer operations (copy, paste) for the editor.
 */
public class EditorTransferHandler extends TransferHandler {

    private Simulator simulator;
    private EditorPanel editorPanel;
    private TransferHandler defaultHandler;

    /**
     * Constructs a new EditorTransferHandler.
     * @param editorPanel The editor panel that will receive the dropped files.
     * @param defaultHandler The default transfer handler for text operations.
     * @param simulator The simulator instance to check its running state.
     */
    public EditorTransferHandler(EditorPanel editorPanel, TransferHandler defaultHandler, Simulator simulator) {
        this.editorPanel = editorPanel;
        this.defaultHandler = defaultHandler;
        this.simulator = simulator;
    }

    /**
     * Checks if the component can import the given data flavor.
     * This handler accepts file lists or delegates to the default handler.
     * @param support The TransferSupport object.
     * @return true if the data flavor is supported, false otherwise.
     */
    @Override
    public boolean canImport(TransferSupport support) {
        if (simulator.isRunning()) {
            return false; // Cannot drop files while simulator is running
        }
        if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return true;
        }
        return defaultHandler.canImport(support);
    }

    /**
     * Imports the data from a drag-and-drop or paste operation.
     * If it's a file list, it loads the file. Otherwise, it delegates to the default handler.
     * @param support The TransferSupport object containing the data.
     * @return true if the data was imported successfully, false otherwise.
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean importData(TransferSupport support) {
        if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            if (editorPanel.isDirty()) {
                int result = JOptionPane.showConfirmDialog(editorPanel, "You have unsaved changes. Do you want to save them before loading a new file?", "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    editorPanel.saveFile();
                    if (editorPanel.isDirty()) { // Check if save was cancelled
                        return false;
                    }
                } else if (result == JOptionPane.CANCEL_OPTION) {
                    return false;
                }
            }

            Transferable transferable = support.getTransferable();
            try {
                List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (files.size() > 0) {
                    editorPanel.loadFile(files.get(0));
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        return defaultHandler.importData(support);
    }

    @Override
    public void exportToClipboard(JComponent comp, java.awt.datatransfer.Clipboard clip, int action) throws IllegalStateException {
        defaultHandler.exportToClipboard(comp, clip, action);
    }
}

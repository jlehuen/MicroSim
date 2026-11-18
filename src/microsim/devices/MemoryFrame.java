/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.2
 * @since 2026-01-05
 *
 * License: GNU General Public License v3.0
 */

package microsim.devices;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

import microsim.MicroSim;
import microsim.simulator.RAM;
import microsim.simulator.CPU;
import microsim.simulator.Utils;

/**
 * The MemoryFrame class provides a graphical representation of the RAM content.
 * It displays the 256 bytes of memory in a grid, allowing the user to view
 * the data in either hexadecimal or ASCII format. The frame also highlights
 * the cells corresponding to the Instruction Pointer (IP) and Stack Pointer (SP).
 * This class is a singleton to ensure only one memory window exists.
 */
public class MemoryFrame extends AbstractDevice {

    /** The singleton instance of the MemoryFrame. */
    public static MemoryFrame INSTANCE = null; // Singleton

    private static final String TITLE_STRING = "256 Bytes Memory Content";
    private static final Font MONOSPACED_12 = new Font("Monospaced", Font.PLAIN, 12);
    
    /**
     * Defines the display modes for memory content.
     */
    private enum DisplayMode { 
        /** Display memory content as hexadecimal values. */
        HEX, 
        /** Display memory content as ASCII characters. */
        ASCII 
    }

    private JTable table;
    private RamTableModel model;
    private CPU cpu;
    private int headerRowHeight;
    private int programCodeSize = 0;
    private DisplayMode currentDisplayMode = DisplayMode.HEX;

    // Semantic RAM zone colors
    private static final Color COLOR_LAST_ACCESS = new Color(253, 224, 71); // Amber/Yellow highlight
    private static final Color COLOR_CODE_ZONE   = new Color(209, 250, 229); // Soft green (assembled code)
    private static final Color COLOR_STACK_ZONE  = new Color(219, 234, 254); // Soft blue (active stack)
    private static final Color COLOR_VRAM_ZONE   = new Color(254, 243, 199); // Soft warm tint (ASCII display 0xC0..0xFF)

    /**
     * Constructs the MemoryFrame.
     * @param memory The RAM to be visualized.
     * @param cpu The CPU, used to highlight the IP and SP registers.
     */
    public MemoryFrame(RAM memory, CPU cpu) {
        this.cpu = cpu;
        INSTANCE = this;

        model = new RamTableModel(memory);
        table = new JTable(model);
        int defaultRowHeight = table.getRowHeight();
        headerRowHeight = defaultRowHeight + 15;
        table.setRowHeight(0, headerRowHeight);
        table.setFont(MONOSPACED_12);
        table.setBackground(Color.LIGHT_GRAY);
        table.setRowSelectionAllowed(false);
        table.setFocusable(false);
        table.setTableHeader(null);

        // Set custom cell renderer
        table.setDefaultRenderer(Object.class, new IPCellRenderer());

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(30);
        }
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Create radio buttons for view selection
        JRadioButton hexButton = new JRadioButton("Show Hexa", true);
        JRadioButton asciiButton = new JRadioButton("Show ASCII");
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(hexButton);
        viewGroup.add(asciiButton);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        radioPanel.add(hexButton);
        radioPanel.add(asciiButton);
        radioPanel.setBackground(Color.DARK_GRAY);
        hexButton.setBackground(Color.DARK_GRAY);
        hexButton.setForeground(Color.WHITE);
        asciiButton.setBackground(Color.DARK_GRAY);
        asciiButton.setForeground(Color.WHITE);

        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        legendPanel.setBackground(Color.DARK_GRAY);
        legendPanel.add(createLegendItem("IP", Color.RED, Color.WHITE));
        legendPanel.add(createLegendItem("SP", Color.BLUE, Color.WHITE));
        legendPanel.add(createLegendItem("R/W", COLOR_LAST_ACCESS, Color.BLACK));
        legendPanel.add(createLegendItem("Code", COLOR_CODE_ZONE, Color.BLACK));
        legendPanel.add(createLegendItem("Stack", COLOR_STACK_ZONE, Color.BLACK));
        legendPanel.add(createLegendItem("VRAM", COLOR_VRAM_ZONE, Color.BLACK));

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBackground(Color.DARK_GRAY);
        southPanel.add(legendPanel, BorderLayout.NORTH);
        southPanel.add(radioPanel, BorderLayout.SOUTH);


        hexButton.addActionListener(e -> {
            if (currentDisplayMode != DisplayMode.HEX) {
                currentDisplayMode = DisplayMode.HEX;
                updateMemoryView();
            }
        });

        asciiButton.addActionListener(e -> {
            if (currentDisplayMode != DisplayMode.ASCII) {
                currentDisplayMode = DisplayMode.ASCII;
                updateMemoryView();
            }
        });

        // Main content pane setup
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.setBackground(Color.DARK_GRAY);
        contentPane.add(table, BorderLayout.CENTER);
        contentPane.add(southPanel, BorderLayout.SOUTH);

        // Add a margin
        ((JComponent) contentPane).setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));

        pack();
        setTitle(TITLE_STRING);
        setLocation(1040, 210);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setResizable(false);
        setAlwaysOnTop(true);
    }

    private JLabel createLegendItem(String text, Color bg, Color fg) {
        JLabel lbl = new JLabel(" " + text + " ");
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setForeground(fg);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        return lbl;
    }

    public void setProgramCodeSize(int size) {
        this.programCodeSize = Math.max(0, size);
    }

    /**
     * Refreshes the memory view to reflect any changes in the RAM.
     */
    public void updateMemoryView() {
        model.fireTableDataChanged();
        table.setRowHeight(0, headerRowHeight);
    }

    /**
     * The table model for displaying RAM content.
     */
    private class RamTableModel extends AbstractTableModel {
        private RAM memory;
        private final int numRows = 16;
        private final int numCols = 16;

        /**
         * Constructs the table model.
         * @param memory The RAM to be displayed.
         */
        public RamTableModel(RAM memory) {
            this.memory = memory;
        }

        @Override
        public int getRowCount() {
            return numRows + 1; // 16 data rows + 1 header row
        }

        @Override
        public int getColumnCount() {
            return numCols + 1; // 16 data columns + 1 header column
        }

        @Override
        public String getColumnName(int column) {
            return ""; // No longer used since the header is hidden
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex == 0) {
                if (columnIndex == 0) return " ";
                return String.format("%X", columnIndex - 1);
            }

            int dataRowIndex = rowIndex - 1;
            if (columnIndex == 0) {
                return String.format("%X", dataRowIndex);
            }

            int address = dataRowIndex * 16 + (columnIndex - 1);
            int value = memory.load(address);

            switch (currentDisplayMode) {
                case ASCII:
                    if (value >= 32 && value <= 126) { // Printable ASCII characters
                        return String.valueOf((char) value);
                    } else {
                        return " "; // Placeholder for non-printable characters
                    }
                case HEX:
                default:
                    return String.format("%02X", value);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }

    /**
     * A custom cell renderer to highlight the Instruction Pointer (IP) and Stack Pointer (SP).
     */
    private class IPCellRenderer extends DefaultTableCellRenderer {
        /**
         * Constructs the cell renderer.
         */
        public IPCellRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
            setVerticalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (row > 0 && column > 0) {
                int dataRowIndex = row - 1;
                int address = dataRowIndex * 16 + (column - 1);

                if (address == cpu.getIP()) {
                    c.setBackground(Color.RED);
                    c.setForeground(Color.WHITE);
                } else if (address == cpu.getSP()) {
                    c.setBackground(Color.BLUE);
                    c.setForeground(Color.WHITE);
                } else if (address == cpu.getLastAccessedRamAddress()) {
                    c.setBackground(COLOR_LAST_ACCESS);
                    c.setForeground(Color.BLACK);
                } else if (address >= 0xC0) {
                    c.setBackground(COLOR_VRAM_ZONE);
                    c.setForeground(Color.BLACK);
                } else if (address > cpu.getSP() && address <= 0xBF) {
                    c.setBackground(COLOR_STACK_ZONE);
                    c.setForeground(Color.BLACK);
                } else if (address < programCodeSize) {
                    c.setBackground(COLOR_CODE_ZONE);
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }

                int cellVal = model.memory.load(address) & 0xFF;
                String asciiStr = (cellVal >= 32 && cellVal <= 126) ? "'" + (char) cellVal + "'" : "non-printable";
                StringBuilder tip = new StringBuilder();
                tip.append("[").append(Utils.toHexString0x(address)).append(" / ").append(address).append("] = ")
                   .append(Utils.toHexString0x(cellVal))
                   .append(" | Unsigned: ").append(cellVal)
                   .append(" | Signed: ").append(Utils.toSignedDecimalString(cellVal))
                   .append(" | Bin: ").append(Utils.toBinaryString(cellVal))
                   .append(" | ASCII: ").append(asciiStr);
                if (address < programCodeSize && MicroSim.self != null && MicroSim.self.getSimulator() != null) {
                    String instr = MicroSim.self.getSimulator().getInstructionTextAt(address);
                    if (instr != null) {
                        tip.append(" | Code: ").append(instr);
                    }
                }
                setToolTipText(tip.toString());
            } else {
                c.setBackground(Color.DARK_GRAY);
                c.setForeground(Color.LIGHT_GRAY);
                setToolTipText(null);
            }
            return c;
        }
    }
}

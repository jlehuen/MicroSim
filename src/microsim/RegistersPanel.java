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

import java.awt.Font;
import java.awt.Color;
import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import microsim.simulator.CPU;
import microsim.simulator.Utils;
import microsim.simulator.Simulator;

/**
 * The RegistersPanel class displays the state of the CPU's registers in a table.
 * It shows the register name, and its value in binary, hexadecimal, and decimal
 * formats. The panel automatically updates as the simulation runs. Special
 * registers like IP, SP, and SR are highlighted with distinct colors.
 */
public class RegistersPanel extends JPanel {

    private final Simulator simulator;
    private final DefaultTableModel tableModel;
    private final String[] columnNames = {"Reg", "Binary", "Hex", "Dec"};
    private final String[] registerNames = {"AL", "BL", "CL", "DL", "IP", "SP", "SR"};
    private final Color LIGHT_GRAY = new Color(240, 240, 240);

    /**
     * Checks if a register is a general-purpose register.
     * @param name The name of the register.
     * @return true if it is a general-purpose register, false otherwise.
     */
    private boolean generalPurposeRegister(String name) {
        return "ALBLCLDL".contains(name);
    }

    /**
     * Constructs the RegistersPanel.
     * @param simulator The simulator instance to get CPU data from.
     */
    public RegistersPanel(Simulator simulator) {
        this.simulator = simulator;
        setLayout(new BorderLayout());

        tableModel = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(tableModel);
        table.setFont(new Font("Monospaced", Font.PLAIN, 14));
        table.setBackground(LIGHT_GRAY);
        table.getTableHeader().setReorderingAllowed(false);
        table.setEnabled(false);

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);

        // Custom renderer for row colors and centering
        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String registerName = (String) table.getModel().getValueAt(row, 0);
                if (registerName.equals("IP")) c.setForeground(Color.RED); // Instruction pointer
                else if (registerName.equals("SP")) c.setForeground(Color.BLUE); // Stack pointer
                else if (registerName.equals("SR")) c.setForeground(Color.ORANGE.darker()); // Status register
                else if (registerName.equals("")) c.setForeground(Color.ORANGE.darker()); // Flags labels
                else c.setForeground(Color.BLACK); // Color for other registers
                c.setBackground(LIGHT_GRAY); // Consistent background
                setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        initializeTable();
    }

    /**
     * Initializes the table with register names and default zero values.
     */
    private void initializeTable() {
        for (String name : registerNames) {
           if (generalPurposeRegister(name)) {
                tableModel.addRow(new Object[]{name, "00000000", "0x00", "0"});
            } else {
                tableModel.addRow(new Object[]{name, "00000000", "00", ""});
            }
        }
        tableModel.addRow(new Object[]{"", "   SZOCF", "", ""});
    }

    /**
     * Updates the register values in the table by fetching the latest data from the CPU.
     */
    public void update() {
        CPU cpu = simulator.getCPU();
        if (cpu == null) return;

        int[] gpr = cpu.getRegisters();
        updateRegisterValue(0, "AL", gpr[0]);
        updateRegisterValue(1, "BL", gpr[1]);
        updateRegisterValue(2, "CL", gpr[2]);
        updateRegisterValue(3, "DL", gpr[3]);
        updateRegisterValue(4, "IP", cpu.getIP());
        updateRegisterValue(5, "SP", cpu.getSP());
        updateRegisterValue(6, "SR", cpu.getSR());
    }

    /**
     * Updates a single row in the register table with new values.
     * @param rowIndex The index of the row to update.
     * @param name The name of the register.
     * @param value The new integer value of the register.
     */
    private void updateRegisterValue(int rowIndex, String name, int value) {
        tableModel.setValueAt(name, rowIndex, 0);
        tableModel.setValueAt(Utils.toBinaryString(value), rowIndex, 1);
        tableModel.setValueAt(Utils.toHexString0x(value), rowIndex, 2);
        if (generalPurposeRegister(name)) {
            tableModel.setValueAt(Utils.toSignedDecimalString(value), rowIndex, 3);
        } else {
            tableModel.setValueAt("", rowIndex, 3);
        }
    }
}

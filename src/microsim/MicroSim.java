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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLightLaf;

import microsim.devices.AsciiFrame;
import microsim.devices.HeaterFrame;
import microsim.devices.KeyboardFrame;
import microsim.devices.LightsFrame;
import microsim.editor.EditorPanel;
import microsim.simulator.Simulator;
import microsim.simulator.Utils;

import javax.swing.JTabbedPane;

/**
 * The MicroSim class is the main window of the 8-bit Microprocessor Simulator.
 * It sets up the entire user interface, including the toolbar, the editor panel,
 * and the register display panel. It also initializes and orchestrates the
 * interaction between these components and the simulator itself.
 */

public class MicroSim extends JFrame {

    public static MicroSim self; // Static reference for global access
	public static final String SYSTEM = Utils.getOperatingSystem();

    public final String BASE_TITLE = "8 Bits Microprocessor Simulator";
    private final Dimension DIMENSION = new Dimension(800, 600);
    private final int HORIZONTAL_DIVIDER_LOCATION = 250;
    private final int VERTICAL_DIVIDER_LOCATION = 400;
    
    private Simulator simulator;
    private ToolBar toolBar;
    private EditorPanel editor;
    private RegistersPanel registersPanel;
    private VisualizationPanel visualizationPanel;
    private Console console;

    public ToolBar getToolBar() { return toolBar; }
    public Console getConsole() { return console; }
    public Simulator getSimulator() { return simulator; }
    public EditorPanel getEditor() { return editor; }
    public RegistersPanel getRegisters() { return registersPanel; }
    public VisualizationPanel getVisualizationPanel() { return visualizationPanel; }

    /**
     * Constructs the main frame and all its components.
     */
    public MicroSim() {

        // Placement of device windows
        LightsFrame.INSTANCE.setLocation(0, 0);
        HeaterFrame.INSTANCE.setLocation(0, 280);
        KeyboardFrame.INSTANCE.setLocation(220, 0);
        AsciiFrame.INSTANCE.setLocation(555, 0);

        setTitle(BASE_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setMinimumSize(DIMENSION);
        setLocation(220, 210);

        simulator = new Simulator(this);
        editor = new EditorPanel(this);
        registersPanel = new RegistersPanel(simulator);
        toolBar = new ToolBar(editor, simulator);
        console = new Console();
        visualizationPanel = new VisualizationPanel(simulator);

        registersPanel.update();

        JTabbedPane editorAndVizTab = new JTabbedPane();
        editorAndVizTab.addTab("Editor", editor);
        editorAndVizTab.addTab("Visualization", visualizationPanel);

        JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        horizontalSplitPane.setLeftComponent(registersPanel);
        horizontalSplitPane.setRightComponent(editorAndVizTab);
        horizontalSplitPane.setDividerLocation(HORIZONTAL_DIVIDER_LOCATION);
        horizontalSplitPane.setDividerSize(0);
        horizontalSplitPane.setEnabled(false);

        JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        verticalSplitPane.setTopComponent(horizontalSplitPane);
        verticalSplitPane.setBottomComponent(console);
        verticalSplitPane.setDividerLocation(VERTICAL_DIVIDER_LOCATION);
        verticalSplitPane.setResizeWeight(1.0);
        verticalSplitPane.setDividerSize(5);
        verticalSplitPane.setEnabled(true);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(toolBar, BorderLayout.NORTH);
        getContentPane().add(verticalSplitPane, BorderLayout.CENTER);

        setVisible(true);
        self = this;
    }

    /**
     * The main entry point of the application.
     * It sets up the look and feel and creates the main frame.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // Set the application name for the macOS menu bar
        System.setProperty("apple.awt.application.name", "MicroSim 8 bits");

        // Force the application to use the English locale
        Locale.setDefault(Locale.US);

        // Set ToolTip background color to yellow
        UIManager.put("ToolTip.background", Color.YELLOW);
        
        FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> {
            new MicroSim();
        });
    }
}

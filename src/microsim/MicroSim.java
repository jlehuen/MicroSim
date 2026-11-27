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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLightLaf;

import microsim.devices.AsciiFrame;
import microsim.devices.BannerFrame;
import microsim.devices.BuzzerFrame;
import microsim.devices.ClockFrame;
import microsim.devices.HeaterFrame;
import microsim.devices.KeyboardFrame;
import microsim.devices.LedBarFrame;
import microsim.devices.LightsFrame;
import microsim.devices.MatrixFrame;
import microsim.devices.VisualizationFrame;
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
    public static final String VERSION = loadVersion();

    private static String loadVersion() {
        try (InputStream is = MicroSim.class.getResourceAsStream("/VERSION")) {
            if (is != null) {
                String v = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                if (!v.isEmpty()) return v;
            }
        } catch (IOException ignored) {}
        try {
            Path p = Paths.get("VERSION");
            if (Files.isRegularFile(p)) {
                String v = Files.readString(p, StandardCharsets.UTF_8).trim();
                if (!v.isEmpty()) return v;
            }
        } catch (IOException ignored) {}
        return "unknown";
    }

    public final String BASE_TITLE = "8 Bits Microprocessor Simulator";
    private final Dimension DIMENSION = new Dimension(1050, 600);
    private final int HORIZONTAL_DIVIDER_LOCATION = 250;
    private final int VERTICAL_DIVIDER_LOCATION = 400;
    
    private Simulator simulator;
    private ToolBar toolBar;
    private EditorPanel editor;
    private RegistersPanel registersPanel;
    private Console console;

    public ToolBar getToolBar() { return toolBar; }
    public Console getConsole() { return console; }
    public Simulator getSimulator() { return simulator; }
    public EditorPanel getEditor() { return editor; }
    public RegistersPanel getRegisters() { return registersPanel; }
    public VisualizationPanel getVisualizationPanel() {
        return VisualizationFrame.INSTANCE != null ? VisualizationFrame.INSTANCE.getVisualizationPanel() : null;
    }

    /**
     * Constructs the main frame and all its components.
     */
    public MicroSim() {
        self = this;

        setTitle(BASE_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1220, 800);
        setMinimumSize(DIMENSION);
        setLocation(220, 210);

        simulator = new Simulator(this);
        new VisualizationFrame(simulator);

        // Placement of device windows
        LightsFrame.INSTANCE.setLocation(0, 0);
        HeaterFrame.INSTANCE.setLocation(0, 280);
        BuzzerFrame.INSTANCE.setLocation(0, 490);
        KeyboardFrame.INSTANCE.setLocation(220, 0);
        LedBarFrame.INSTANCE.setLocation(220, 150);
        AsciiFrame.INSTANCE.setLocation(555, 0);
        MatrixFrame.INSTANCE.setLocation(555, 150);
        BannerFrame.INSTANCE.setLocation(220, 230);
        ClockFrame.INSTANCE.setLocation(555, 420);
        VisualizationFrame.INSTANCE.setLocation(240, 240);

        editor = new EditorPanel(this);
        registersPanel = new RegistersPanel(simulator);
        toolBar = new ToolBar(editor, simulator);
        console = new Console();
        console.println("Welcome to MicroSim version " + VERSION, Color.GREEN);

        registersPanel.update();

        JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        horizontalSplitPane.setLeftComponent(registersPanel);
        horizontalSplitPane.setRightComponent(editor);
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

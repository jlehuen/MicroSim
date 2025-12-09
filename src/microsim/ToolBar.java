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

import java.io.File;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JToolBar;

import microsim.devices.AsciiFrame;
import microsim.devices.HeaterFrame;
import microsim.devices.KeyboardFrame;
import microsim.devices.LightsFrame;
import microsim.devices.MemoryFrame;
import microsim.editor.EditorPanel;
import microsim.editor.SpeedSlider;
import microsim.simulator.Simulator;
import microsim.simulator.Utils;

/**
 * The ToolBar class creates and manages the main toolbar of the application.
 * It provides buttons for controlling the simulation (run, assemble, reset),
 * managing files (new, open), editing text (undo, redo, format), and toggling
 * the visibility of various hardware device windows.
 */
public class ToolBar extends JToolBar {

    private Simulator simulator;
    private EditorPanel editor;

    // Icons for simulation control
    private ImageIcon iconRunning = Utils.loadImageIcon("/icons/icon_running.gif");
    private ImageIcon iconRun = Utils.loadImageIcon("/icons/icon_start.png");
    private ImageIcon iconAsm = Utils.loadImageIcon("/icons/icon_asm.png");
    private ImageIcon iconRam = Utils.loadImageIcon("/icons/icon_ram.png");
    private ImageIcon iconReset = Utils.loadImageIcon("/icons/icon_reset.png");

    // Icons for device windows
    private ImageIcon iconAscii = Utils.loadImageIcon("/icons/icon_ascii.png");
    private ImageIcon iconKey = Utils.loadImageIcon("/icons/icon_key.png");
    private ImageIcon iconLights = Utils.loadImageIcon("/icons/icon_lights.png");
    private ImageIcon iconHeater = Utils.loadImageIcon("/icons/icon_heater.png");

    // Icons for file and editor operations
    private ImageIcon iconNew = Utils.loadImageIcon("/icons/icon_new.png");
    private ImageIcon iconOpen = Utils.loadImageIcon("/icons/icon_open.png");
    private ImageIcon iconUndo = Utils.loadImageIcon("/icons/icon_undo.png");
    private ImageIcon iconRedo = Utils.loadImageIcon("/icons/icon_redo.png");
    private ImageIcon iconMagic = Utils.loadImageIcon("/icons/icon_magic.png");

    // Icons for step-by-step execution mode
    private ImageIcon iconStep = Utils.loadImageIcon("/icons/icon_step.png");
    private ImageIcon iconStepOn = Utils.loadImageIcon("/icons/icon_on.png");
    private ImageIcon iconStepOff = Utils.loadImageIcon("/icons/icon_off.png");

    private ImageIcon iconManual = Utils.loadImageIcon("/icons/icon_man.png");

    // Toolbar buttons
    private ToolButton runButton, asmButton;
    private ToolButton asciiButton, keybButton, lightsButton, heaterButton;
    private ToolButton newButton, openButton, undoButton, redoButton, magicButton;
    private ToolButton modeButton, stepButton;

    /**
     * Constructs the toolbar.
     * @param editor The editor panel to interact with.
     * @param simulator The simulator to control.
     */
    public ToolBar(EditorPanel editor, Simulator simulator) {
        super();
        this.simulator = simulator;
        this.editor = editor;
                
        add(runButton = new ToolButton("RUN", "--", "Start / stop the execution", iconRun, this));
        add(modeButton = new ToolButton("STEPMODE", "--", "Step by step mode", iconStepOff, this));
        add(stepButton = new ToolButton("STEP", "--", "Execute one instruction", iconStep, this));
        add(asmButton = new ToolButton("ASM", "--", "Assemble the machine code", iconAsm, this));
        add(new ToolButton("RAM", "--", "Show / hide the memory content", iconRam, this));
        add(new ToolButton("RESET", "--", "Reset the system", iconReset, this));

        addSeparator();
        add(asciiButton = new ToolButton("DISPLAY", "--", "Show / hide the ASCII display", iconAscii, this));
        add(keybButton = new ToolButton("KEYBOARD", "--", "Show / hide the keyboard device", iconKey, this));
        add(lightsButton = new ToolButton("LIGHTS", "--", "Show / hide the traffic lights device", iconLights, this));
        add(heaterButton = new ToolButton("HEATER", "--", "Show / hide the heater device", iconHeater, this));

        addSeparator();
        add(newButton = new ToolButton("NEW", "--", "New program file", iconNew, this));
        add(openButton = new ToolButton("OPEN", "--", "Open program file", iconOpen, this));   
        add(undoButton = new ToolButton("UNDO", "--", "Undo edition", iconUndo, this));   
        add(redoButton = new ToolButton("REDO", "--", "Redo edition", iconRedo, this));
        add(magicButton = new ToolButton("MAGIC", "--", "Reformat code", iconMagic, this));

        add(Box.createHorizontalGlue());
        add(new SpeedSlider(simulator));
        add(Box.createHorizontalGlue());

        add(new ToolButton("HELP", "--", "User manual", iconManual, this));

        stepButton.setEnabled(false); // Step button only works in stepmode
    }

    /**
     * Dispatches actions based on the identifier of the button that was clicked.
     * @param ident The identifier of the action to perform.
     */
	public void action(String ident) {
		switch (ident) {
			case "RUN": action_run(); break;
            case "ASM": action_asm(); break;
            case "RAM": MemoryFrame.INSTANCE.toggleVisible(); break;
            case "RESET": action_reset(); break;

            case "DISPLAY": AsciiFrame.INSTANCE.toggleVisible(); break;
            case "KEYBOARD": KeyboardFrame.INSTANCE.toggleVisible(); break;
            case "LIGHTS": LightsFrame.INSTANCE.toggleVisible(); break;
            case "HEATER": HeaterFrame.INSTANCE.toggleVisible(); break;

            case "NEW": editor.newFile(); break;
            case "OPEN": editor.openFile(); break;
            case "UNDO": editor.undo(); break;
            case "REDO": editor.redo(); break;
            case "MAGIC": editor.reformat(); break;

            case "STEPMODE": action_mode(); break;
            case "STEP": simulator.step(); break;

            case "HELP": action_help(); break;
        }
	}

    private void action_reset() {
        simulator.reset();
        simulator.stepmode = false;
        modeButton.setIcon(iconStepOff);
    }

    /**
     * Toggles the step-by-step execution mode.
     */
    private void action_mode() {
        if (simulator.stepmode) {
            simulator.stepmode = false;
            modeButton.setIcon(iconStepOff);
            simulator.resume(); // Wake up the thread to continue execution
        } else {
            simulator.stepmode = true;
            modeButton.setIcon(iconStepOn);
        }
    }

    /**
     * Assembles the code from the editor.
     * If the file is not saved, it will be saved before assembly.
     */
    private void action_asm() {
        String code = editor.getTextArea().getText();
        if (code.trim().isEmpty()) {
            JOptionPane.showMessageDialog(MicroSim.self, "The editor is empty, there is nothing to assemble.", "Assembly Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (editor.getCurrentFile() != null) {
            editor.saveFile();
        }
        simulator.assemble(code);
        MemoryFrame.INSTANCE.setVisible(true); 
    } 

    /**
     * Updates the state of the toolbar buttons when the simulation starts.
     * Disables buttons that should not be used during execution.
     */
    public void simulationStarted() {
        runButton.setIcon(iconRunning);
        asmButton.setEnabled(false);

        keybButton.setEnabled(false);
        asciiButton.setEnabled(false);
        lightsButton.setEnabled(false);
        heaterButton.setEnabled(false);

        newButton.setEnabled(false);
        openButton.setEnabled(false);
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
        magicButton.setEnabled(false); 
    }

    /**
     * Updates the state of the toolbar buttons when the simulation stops.
     * Re-enables buttons that were disabled during execution.
     */
    public void simulationStopped() {
        runButton.setIcon(iconRun);
        asmButton.setEnabled(true);

        keybButton.setEnabled(true);
        asciiButton.setEnabled(true);
        lightsButton.setEnabled(true);
        heaterButton.setEnabled(true);

        newButton.setEnabled(true);
        openButton.setEnabled(true);
        undoButton.setEnabled(true);
        redoButton.setEnabled(true);
        magicButton.setEnabled(true);
    }

    /**
     * Sets the visibility and enabled state of the 'STEP' button.
     * @param value true to enable the button, false to disable it.
     */
    public void setStepButtonVisible(boolean value) {
        stepButton.setEnabled(value);
    }

    /**
     * Handles the action of the 'RUN' button.
     * Starts the simulation if it's not running, or stops it if it is.
     * Assembles the code before running if necessary.
     */
    private void action_run() {
        if (simulator.isRunning()) {
            simulator.stop();
        } else {
            String code = editor.getTextArea().getText();
            if (code.trim().isEmpty()) {
                JOptionPane.showMessageDialog(MicroSim.self, "The editor is empty, there is nothing to run.", "Execution Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Save the current file before running
            if (editor.getCurrentFile() != null) editor.saveFile();

            // Assemble the code then run if assembly was successful
            if (simulator.assemble(code)) simulator.run();
        }
    }

    /**
     * Opens the manual (man.html) in the default web browser.
     */
    private void action_help() {
        try {
            File manFile = new File("man/man.html");
            if (manFile.exists()) {
                java.awt.Desktop.getDesktop().browse(manFile.toURI());
            } else {
                JOptionPane.showMessageDialog(MicroSim.self, "Help file not found: " + manFile.getAbsolutePath(), "Help Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (java.io.IOException e) {
            JOptionPane.showMessageDialog(MicroSim.self, "Could not open help file: " + e.getMessage(), "Help Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.1
 * @since 2025-12-17
 *
 * License: GNU General Public License v3.0
 */

package microsim.simulator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import microsim.MicroSim;
import microsim.devices.MemoryFrame;
import microsim.editor.SpeedSlider;
import microsim.simulator.Assembler.AssemblyResult;

/**
 * The Simulator class is the core of the 8-bit Assembler Simulator.
 * It orchestrates the CPU, RAM, and the user interface, managing the execution
 * of assembly programs. It handles assembling the code, running it step by step
 * or continuously, and updating the UI to reflect the state of the machine.
 */
public class Simulator {

    private MicroSim mainFrame;
    private RAM ram = new RAM(256);
    private CPU cpu = new CPU(ram);
    private MemoryFrame memoryFrame = new MemoryFrame(ram, cpu);
    private Assembler assembler = new Assembler();

    private Thread cpuThread;
    private int executionSpeedDelay = SpeedSlider.MAX_DELAY;
    private volatile boolean running = false;
    private static final int MIN_HIGHLIGHT_DELAY = 10;

    private Map<Integer, Integer> addressToLineMap;
    private int lastHighlightedLine = -1;
    private Object highlightTag;

    /**
     * When true, the simulation is in step-by-step mode.
     */
    public volatile boolean stepmode = false;
    private volatile boolean stepRequested = false;

    /**
     * Constructs a new Simulator.
     * @param frame The main frame of the application.
     */
    public Simulator(MicroSim frame) {
        this.mainFrame = frame;
    }

    /**
     * Returns the CPU instance.
     * @return The CPU.
     */
    public CPU getCPU() {
        return cpu;
    }

    /**
     * Returns the RAM instance.
     * @return The RAM.
     */
    public RAM getRAM() {
        return ram;
    }

    /**
     * Sets the delay between each instruction execution to control the simulation speed.
     * @param delay The delay in milliseconds.
     */
    public void setExecutionSpeedDelay(int delay) {
        executionSpeedDelay = delay;
    }

    /**
     * Checks if the simulation is currently running.
     * @return true if the simulation is running, false otherwise.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Starts the simulation of the assembled program.
     * The simulation runs in a separate thread to keep the UI responsive.
     */
    public void run() {
        if (running) return;
        running = true;
        stepRequested = false;

        mainFrame.getToolBar().update(); // Notify toolbar to update buttons state
        mainFrame.getEditor().getTextArea().setHighlightCurrentLine(false); // Disable current line highlight
        mainFrame.getEditor().getTextArea().setEnabled(false);

        cpuThread = new Thread(() -> {
            if (lastHighlightedLine != -1) {
                mainFrame.getEditor().removeLineHighlight(highlightTag);
                lastHighlightedLine = -1;
                highlightTag = null;
            }

            while (running && !cpu.isFault() && ram.load(cpu.getIP()) != Opcodes.NONE) {
                int currentIp = cpu.getIP();
                final Integer lineToHighlight = addressToLineMap.get(currentIp);

                // Highlight the current line in the editor
                if (lineToHighlight != null && lineToHighlight != lastHighlightedLine) {
                    if (stepmode || (executionSpeedDelay >= MIN_HIGHLIGHT_DELAY)) {
                        SwingUtilities.invokeLater(() -> {
                            if (highlightTag != null) {
                                mainFrame.getEditor().removeLineHighlight(highlightTag);
                            }
                            highlightTag = mainFrame.getEditor().highlightLine(lineToHighlight);
                        });
                    }
                    else if (highlightTag != null) {
                        mainFrame.getEditor().removeLineHighlight(highlightTag);
                    }
                    lastHighlightedLine = lineToHighlight;
                }

                try {
                    if (stepmode) {
                        synchronized (this) {
                            mainFrame.getToolBar().setStepButtonVisible(true);
                            // Wait for a step request or a resume signal
                            while (!stepRequested && running && stepmode) wait();
                            stepRequested = false; // Consume the step request
                            mainFrame.getToolBar().setStepButtonVisible(false);
                        }   
                    }
                    if (running) cpu.step(); // Execute one instruction

                } catch (InterruptedException e) {
                    running = false; // Exit loop if interrupted
                } catch (Exception e) {
                    final String errorMessage = e.getMessage();
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(mainFrame, "Runtime Error: " + errorMessage, "Execution Error", JOptionPane.ERROR_MESSAGE)
                    );
                    running = false; // Stop the simulation
                } finally {
                    mainFrame.getToolBar().setStepButtonVisible(false);
                }

                // Update UI on the Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    memoryFrame.updateMemoryView();
                    mainFrame.getRegisters().update();
                });

                // Control execution speed
                if (!stepmode) {
                    try {
                        // Ensure a minimum delay to prevent UI freeze at max speed
                        Thread.sleep(Math.max(1, executionSpeedDelay));
                    } catch (InterruptedException e) {
                        running = false;
                    }
                }
            }
            running = false;

            SwingUtilities.invokeLater(() -> {
                if (highlightTag != null) {
                    mainFrame.getEditor().removeLineHighlight(highlightTag);
                }
                mainFrame.getEditor().getTextArea().setHighlightCurrentLine(true);
                mainFrame.getEditor().getTextArea().setEnabled(true);
                mainFrame.getToolBar().update();
            });
        });
        cpuThread.start();
    }

    /**
     * Stops the currently running simulation.
     */
    public void stop() {
        running = false;
        if (cpuThread != null) {
            cpuThread.interrupt();
        }
    }

    /**
     * Executes a single instruction if the simulation is in step mode.
     */
    public void step() {
        synchronized (this) {
            stepRequested = true;
            notifyAll(); // Notify the cpuThread to take one step
        }
    }

    /**
     * Resumes continuous execution from step mode.
     */
    public void resume() {
        synchronized (this) {
            stepRequested = false; // Ensure no pending step request
            notifyAll(); // Wake up the thread to continue non-step execution
        }
    }

    private int getLineFromException(Exception e) {
        String message = e.getMessage();
        if (message != null && message.contains(" on line ")) {
            try {
                String sLine = message.substring(message.lastIndexOf(" on line ") + 9);
                sLine = sLine.replaceAll("[^0-9]", "");
                return Integer.parseInt(sLine) - 1;
            } catch (NumberFormatException ex) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Assembles the given assembly program text.
     * On success, the machine code is loaded into RAM.
     * On failure, an error message is displayed.
     * @param program The assembly code as a String.
     * @return true if assembly was successful, false otherwise.
     */
    public boolean assemble(String program) {
        try {
            mainFrame.getEditor().removeErrorHighlight();
            AssemblyResult result = assembler.assemble(program);
            ram.loadProgram(result.machineCode);
            addressToLineMap = result.addressToLineMap;
            cpu.reset();
            SwingUtilities.invokeLater(() -> {
                memoryFrame.updateMemoryView();
                mainFrame.getRegisters().update();
            });
            return true;
        } catch (IllegalArgumentException e) {
            int line = getLineFromException(e);
            if (line != -1) {
                mainFrame.getEditor().highlightErrorLine(line);
            }
            JOptionPane.showMessageDialog(MicroSim.self, "Assembly Error: " + e.getMessage(), "Assembly Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Resets the simulator to its initial state.
     * Stops any running simulation, resets the CPU and clears UI highlights.
     */
    public void reset() {
        stop();
        cpu.reset();
        ram.reset();
        SwingUtilities.invokeLater(() -> {
            if (highlightTag != null) {
                mainFrame.getEditor().removeLineHighlight(highlightTag);
                highlightTag = null;
            }
            mainFrame.getEditor().getTextArea().setHighlightCurrentLine(true);
            memoryFrame.updateMemoryView();
            mainFrame.getRegisters().update();
        });
        lastHighlightedLine = -1;
    }

    /**
     * Loads and assembles a program from a file.
     * @param path The path to the assembly file.
     * @return true if the file was loaded and assembled successfully, false otherwise.
     */
    public boolean loadFile(String path) {
        try {
            mainFrame.getEditor().removeErrorHighlight();
            String program = new String(Files.readAllBytes(Paths.get(path)));
            AssemblyResult result = assembler.assemble(program);
            ram.loadProgram(result.machineCode);
            addressToLineMap = result.addressToLineMap;
            cpu.reset();
            SwingUtilities.invokeLater(() -> {
                memoryFrame.updateMemoryView();
                mainFrame.getRegisters().update();
            });
            return true;
        } catch (java.nio.file.NoSuchFileException e) {
            JOptionPane.showMessageDialog(MicroSim.self, "Error: File not found: " + path, "File Not Found", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(MicroSim.self, "Error reading file '" + path + "': " + e.getMessage(), "I/O Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (IllegalArgumentException e) {
            int line = getLineFromException(e);
            if (line != -1) {
                mainFrame.getEditor().highlightErrorLine(line);
            }
            JOptionPane.showMessageDialog(MicroSim.self, "Assembly Error: " + e.getMessage(), "Assembly Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}

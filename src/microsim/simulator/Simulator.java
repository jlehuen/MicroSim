/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.2
 * @since 2026-01-05
 *
 * License: GNU General Public License v3.0
 */

package microsim.simulator;

import java.awt.Color;
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
    private final Object animationLock = new Object();
    private volatile int executionSpeedDelay = SpeedSlider.MAX_DELAY;
    private volatile int executionBatchSize = 1;
    private volatile boolean running = false;
    private static final int MIN_HIGHLIGHT_DELAY = 10;

    private Map<Integer, Integer> addressToLineMap;
    private String[] sourceLines;
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
     * Returns the execution speed delay in milliseconds.
     * @return The delay in milliseconds.
     */
    public int getExecutionSpeedDelay() {
        return executionSpeedDelay;
    }

    /**
     * Returns the execution batch size.
     * @return The batch size.
     */
    public int getExecutionBatchSize() {
        return executionBatchSize;
    }

    /**
     * Sets both the tick delay and the instruction batch size to control the simulation speed.
     * @param delayMs Delay in milliseconds between execution cycles (always >= 1 ms).
     * @param batchSize Number of instructions executed per cycle.
     */
    public void setExecutionSpeed(int delayMs, int batchSize) {
        this.executionSpeedDelay = Math.max(1, delayMs);
        this.executionBatchSize = Math.max(1, batchSize);
    }

    /**
     * Sets the delay between each instruction execution to control the simulation speed.
     * @param delay The delay in milliseconds.
     */
    public void setExecutionSpeedDelay(int delay) {
        setExecutionSpeed(delay, 1);
    }

    /**
     * Checks if the simulation is currently running.
     * @return true if the simulation is running, false otherwise.
     */
              public boolean isRunning() {
                 return running;
             }    /**
     * Starts the simulation of the assembled program.
     * The simulation runs in a separate thread to keep the UI responsive.
     */
    public void run() {
        if (running) return;
        mainFrame.getConsole().println("Running program...", Color.GREEN);
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

            while (running && !cpu.isFault()) {
                // Step-by-step mode
                if (stepmode) {
                    try {
                        synchronized (this) {
                            mainFrame.getToolBar().setStepButtonVisible(true);
                            while (!stepRequested && running && stepmode) wait();
                            stepRequested = false;
                            mainFrame.getToolBar().setStepButtonVisible(false);
                        }
                        if (running) cpu.step();

                        int currentIp = cpu.getIP();
                        final Integer lineToHighlight = addressToLineMap.get(currentIp);
                        SwingUtilities.invokeAndWait(() -> {
                            if (highlightTag != null) {
                                mainFrame.getEditor().removeLineHighlight(highlightTag);
                            }
                            if (lineToHighlight != null) {
                                highlightTag = mainFrame.getEditor().highlightLine(lineToHighlight);
                            }
                            if (memoryFrame.isVisible()) {
                                memoryFrame.updateMemoryView();
                            }
                            mainFrame.getRegisters().update();
                            mainFrame.getVisualizationPanel().updateAndAnimate(null);
                        });
                    } catch (InterruptedException | java.lang.reflect.InvocationTargetException e) {
                        running = false;
                        Thread.currentThread().interrupt();
                        break;
                    }

                    if (cpu.isHalted()) {
                        mainFrame.getConsole().println("Program halted", Color.GREEN);
                        break;
                    }
                    continue;
                }

                // Continuous execution: execute instructions
                boolean isVisuOpen = (mainFrame.getVisualizationPanel() != null && mainFrame.getVisualizationPanel().isShowing());
                int currentBatch = isVisuOpen ? 1 : executionBatchSize;
                int currentDelay = executionSpeedDelay;
                boolean halted = false;

                for (int i = 0; i < currentBatch; i++) {
                    if (!running || cpu.isFault()) break;

                    // Highlight line only in visualization mode or slow mode
                    if (isVisuOpen || (currentBatch == 1 && currentDelay >= 40)) {
                        int currentIp = cpu.getIP();
                        final Integer lineToHighlight = addressToLineMap.get(currentIp);
                        if (lineToHighlight != null && lineToHighlight != lastHighlightedLine) {
                            SwingUtilities.invokeLater(() -> {
                                if (highlightTag != null) {
                                    mainFrame.getEditor().removeLineHighlight(highlightTag);
                                }
                                highlightTag = mainFrame.getEditor().highlightLine(lineToHighlight);
                            });
                            lastHighlightedLine = lineToHighlight;
                        }
                    }

                    try {
                        cpu.step();
                    } catch (Exception e) {
                        final String errorMessage = e.getMessage();
                        SwingUtilities.invokeLater(() -> 
                            JOptionPane.showMessageDialog(mainFrame, "Runtime Error: " + errorMessage, "Execution Error", JOptionPane.ERROR_MESSAGE)
                        );
                        running = false;
                        break;
                    }

                    if (cpu.isHalted()) {
                        mainFrame.getConsole().println("Program halted", Color.GREEN);
                        halted = true;
                        break;
                    }
                }

                if (!running || cpu.isFault() || halted) break;

                // Update UI: animation obligatoire si la fenêtre de visualisation est ouverte, sinon pas d'animation
                if (isVisuOpen) {
                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            if (memoryFrame.isVisible()) {
                                memoryFrame.updateMemoryView();
                            }
                            mainFrame.getRegisters().update();
                            mainFrame.getVisualizationPanel().updateAndAnimate(() -> {
                                synchronized (animationLock) {
                                    animationLock.notifyAll();
                                }
                            });
                        });

                        if (mainFrame.getVisualizationPanel().isAnimating()) {
                            synchronized (animationLock) {
                                if (mainFrame.getVisualizationPanel().isAnimating()) {
                                    animationLock.wait(3500);
                                }
                            }
                        }
                    } catch (InterruptedException | java.lang.reflect.InvocationTargetException e) {
                        running = false;
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    // Fast mode sans animation : cadence régulée par le slider
                    if (highlightTag != null) {
                        SwingUtilities.invokeLater(() -> {
                            if (highlightTag != null) {
                                mainFrame.getEditor().removeLineHighlight(highlightTag);
                                highlightTag = null;
                            }
                        });
                        lastHighlightedLine = -1;
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (memoryFrame.isVisible()) {
                            memoryFrame.updateMemoryView();
                        }
                        mainFrame.getRegisters().update();
                    });

                    // Sleep to maintain controlled cadence and keep Swing/OS completely fluid
                    try {
                        Thread.sleep(Math.max(1, currentDelay));
                    } catch (InterruptedException e) {
                        running = false;
                        break;
                    }
                }
            }
            running = false;

            SwingUtilities.invokeLater(() -> {
                if (highlightTag != null) {
                    mainFrame.getEditor().removeLineHighlight(highlightTag);
                    highlightTag = null;
                }
                mainFrame.getEditor().getTextArea().setHighlightCurrentLine(true);
                mainFrame.getEditor().getTextArea().setEnabled(true);
                mainFrame.getToolBar().update();
                if (memoryFrame.isVisible()) {
                    memoryFrame.updateMemoryView();
                }
                mainFrame.getRegisters().update();
                if (mainFrame.getVisualizationPanel().isShowing()) {
                    mainFrame.getVisualizationPanel().repaint();
                }
            });
        });
        cpuThread.start();
    }

    /**
     * Stops the currently running simulation.
     */
    public void stop() {
        if (!running) return;
        mainFrame.getConsole().println("Program stopped", Color.GREEN);
        running = false;
        if (mainFrame.getVisualizationPanel() != null) {
            mainFrame.getVisualizationPanel().stopAnimation();
        }
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
        mainFrame.getConsole().println("Assembling program...", Color.GREEN);
        try {
            mainFrame.getEditor().removeErrorHighlight();
            sourceLines = program.split("\\r?\\n");
            AssemblyResult result = assembler.assemble(program);
            ram.loadProgram(result.machineCode);
            addressToLineMap = result.addressToLineMap;
            cpu.reset();
            SwingUtilities.invokeLater(() -> {
                memoryFrame.updateMemoryView();
                mainFrame.getRegisters().update();
                mainFrame.getVisualizationPanel().updateAndAnimate(null);
            });
            mainFrame.getConsole().println("Assembly listing:", Color.GREEN);
            mainFrame.getConsole().println(result.listing, Color.ORANGE);
            return true;
        } catch (IllegalArgumentException e) {
            int line = getLineFromException(e);
            if (line != -1) {
                mainFrame.getEditor().highlightErrorLine(line);
            }
            mainFrame.getConsole().println("Assembly failed: " + e.getMessage(), Color.RED);
            JOptionPane.showMessageDialog(MicroSim.self, "Assembly Error: " + e.getMessage(), "Assembly Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Resets the simulator to its initial state.
     * Stops any running simulation, resets the CPU and clears UI highlights.
     */
    public void reset() {
        mainFrame.getConsole().println("System reset", Color.GREEN);
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
            mainFrame.getVisualizationPanel().updateAndAnimate(null);
        });
        lastHighlightedLine = -1;
    }

    /**
     * Loads and assembles a program from a file.
     * @param path The path to the assembly file.
     * @return true if the file was loaded and assembled successfully, false otherwise.
     */
    public boolean loadFile(String path) {
        mainFrame.getConsole().println("Loading and assembling file: " + path, Color.GREEN);
        try {
            mainFrame.getEditor().removeErrorHighlight();
            String program = new String(Files.readAllBytes(Paths.get(path)));
            sourceLines = program.split("\\r?\\n");
            AssemblyResult result = assembler.assemble(program);
            ram.loadProgram(result.machineCode);
            addressToLineMap = result.addressToLineMap;
            cpu.reset();
            SwingUtilities.invokeLater(() -> {
                memoryFrame.updateMemoryView();
                mainFrame.getRegisters().update();
                mainFrame.getVisualizationPanel().updateAndAnimate(null);
            });
            mainFrame.getConsole().println("File loaded and assembled successfully", Color.GREEN);
            return true;
        } catch (java.nio.file.NoSuchFileException e) {
            String errorMessage = "Error: File not found: " + path;
            mainFrame.getConsole().println(errorMessage, Color.RED);
            JOptionPane.showMessageDialog(MicroSim.self, errorMessage, "File Not Found", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (IOException e) {
            String errorMessage = "Error reading file '" + path + "': " + e.getMessage();
            mainFrame.getConsole().println(errorMessage, Color.RED);
            JOptionPane.showMessageDialog(MicroSim.self, errorMessage, "I/O Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (IllegalArgumentException e) {
            int line = getLineFromException(e);
            if (line != -1) {
                mainFrame.getEditor().highlightErrorLine(line);
            }
            String errorMessage = "Assembly Error: " + e.getMessage();
            mainFrame.getConsole().println(errorMessage, Color.RED);
            JOptionPane.showMessageDialog(MicroSim.self, errorMessage, "Assembly Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Returns the textual representation of the current instruction being executed or about to be executed.
     * @return The assembly instruction string, or null if unavailable.
     */
    public String getCurrentInstructionText() {
        if (cpu == null || addressToLineMap == null || sourceLines == null) return null;
        int ip = (mainFrame != null && mainFrame.getVisualizationPanel() != null && mainFrame.getVisualizationPanel().isAnimating())
                 ? cpu.getLastExecutedIP() : cpu.getIP();
        Integer line = addressToLineMap.get(ip);
        if (line == null && ip != cpu.getIP()) {
            line = addressToLineMap.get(cpu.getIP());
        }
        if (line != null && line >= 0 && line < sourceLines.length) {
            String text = sourceLines[line].trim();
            int semi = text.indexOf(';');
            if (semi != -1) text = text.substring(0, semi).trim();
            return text.isEmpty() ? null : text;
        }
        return null;
    }
}

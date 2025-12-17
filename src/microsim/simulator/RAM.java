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

import microsim.devices.AsciiFrame;

/**
 * Represents the Random Access Memory (RAM) unit of the 8-bit assembler simulator.
 * The memory is an array of integers, where each integer represents an 8-bit byte.
 * It provides methods for loading and storing data, and for loading a program.
 */
public class RAM {
    private final int[] data;
    private final int size;
    private AsciiFrame asciiFrame = AsciiFrame.INSTANCE;

    /**
     * Constructs a new Memory unit with the specified size.
     * It also initializes the memory area dedicated to the ASCII display (0xC0-0xFF)
     * with space characters.
     *
     * @param size The total number of bytes in the memory.
     */
    public RAM(int size) {
        this.size = size;
        this.data = new int[size];
        
        // Initialize ASCII display area with spaces
        for (int address = 0xC0; address <= 0xFF; address++) {
        	data[address] = 0x20; // ASCII for space
        }
    }

    /**
     * Loads a byte from the specified memory address.
     *
     * @param address The memory address to load from (0-255).
     * @return The 8-bit value at the specified address.
     * @throws IllegalArgumentException If the address is out of bounds.
     */
    public int load(int address) {
        if (address < 0 || address >= size) {
            throw new IllegalArgumentException("Address out of bounds: " + address);
        }
        return data[address];
    }

    /**
     * Stores a byte value at the specified memory address.
     * The value is masked with 0xFF to ensure it remains an 8-bit byte.
     * If the address is within the ASCII display range (0xC0-0xFF), the
     * display is updated.
     *
     * @param address The memory address to store to (0-255).
     * @param value   The 8-bit value to store.
     * @throws IllegalArgumentException If the address is out of bounds.
     */
    public void store(int address, int value) {
        if (address < 0 || address >= size) {
            throw new IllegalArgumentException("Address out of bounds: " + address);
        }
        data[address] = value & 0xFF; // Ensure value is a byte

        // If the modified address is in the ASCII display range, update the display
        if (address >= 0xC0 && address <= 0xFF) {
            asciiFrame.setVisible(true);
            asciiFrame.update(this);
        }
    }

    /**
     * Loads a program (an array of machine code) into memory.
     * The program is loaded starting at address 0. This method first clears the
     * main program memory area (0x00 to 0xBF) before loading the new program.
     * The memory area from 0xC0 to 0xFF (ASCII display) is not affected.
     *
     * @param program The array of integers representing the machine code.
     * @throws IllegalArgumentException If the program is too large for the memory area.
     */
    public void loadProgram(int[] program) {
        if (program.length > 0xC0) {
            throw new IllegalArgumentException("Program is too large for memory (max size 192 bytes)");
        }
        // Clear memory before loading program, up to 0xC0
        for (int i = 0; i < 0xC0; i++) {
            data[i] = 0;
        }
        System.arraycopy(program, 0, data, 0, program.length);
    }

    /**
     * Returns the total size of the memory.
     *
     * @return The size of the memory in bytes.
     */
    public int getSize() {
        return size;
    }

    /**
     * Forces an update of the ASCII display frame with the current memory content.
     */
    public void updateAsciiFrame() {
        asciiFrame.update(this);
    }

    /**
     * Resets the RAM to its initial state.
     * Clears the program memory area (0x00 to 0xBF) and resets the ASCII
     * display area (0xC0 to 0xFF) to spaces.
     */
    public void reset() {
        // Clear program memory
        for (int i = 0; i < 0xC0; i++) {
            data[i] = 0;
        }
        // Initialize ASCII display area with spaces
        for (int address = 0xC0; address <= 0xFF; address++) {
            data[address] = 0x20; // ASCII for space
        }
        // Update the ASCII display
        asciiFrame.update(this);
    }
}

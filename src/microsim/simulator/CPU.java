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

import microsim.devices.HeaterFrame;
import microsim.devices.KeyboardFrame;
import microsim.devices.LightsFrame;

/**
 * Represents the Central Processing Unit (CPU) of the 8-bit assembler simulator.
 * It fetches, decodes, and executes instructions from memory. It contains
 * general-purpose registers (AL, BL, CL, DL), a stack pointer (SP), an
 * instruction pointer (IP), and a status register (SR) with flags for zero,
 * carry, overflow, sign, and fault conditions.
 */
public class CPU {

    /** Port address for the simulated Keyboard device. */
    public final int KEYBOARD_PORT = 0x01;
    /** Port address for the simulated Traffic Lights device. */
    public final int LIGHTS_PORT = 0x02;
    /** Port address for the simulated Heater device. */
    public final int HEATER_PORT = 0x03;

    private final int maxSP = 191; // Base stack pointer value (0xBF)
    private final int minSP = 128; // Minimum stack pointer value (0x80), defines stack limit

    private final RAM memory;

    // Singleton instances of peripheral devices
    private final HeaterFrame heater = HeaterFrame.INSTANCE;
    private final KeyboardFrame keyboard = KeyboardFrame.INSTANCE;
    private final LightsFrame trafficLights = LightsFrame.INSTANCE;
    
    private final int[] registers = new int[4]; // General purpose registers AL, BL, CL, DL
    private int SP; // Stack Pointer
    private int IP; // Instruction Pointer
    
    // Status Flags
    private boolean zero;
    private boolean carry;
    private boolean overflow;
    private boolean sign;
    private boolean fault;
    private boolean halted;

    /**
     * Constructs a new CPU and links it to the provided memory unit.
     *
     * @param memory The memory unit the CPU will operate on.
     */
    public CPU(RAM memory) {
        this.memory = memory;
        reset();
    }

    /**
     * Returns the array of general-purpose registers.
     *
     * @return An array containing the values of registers AL, BL, CL, DL.
     */
    public int[] getRegisters() {
        return registers;
    }

    /**
     * Returns the current value of the stack pointer (SP).
     *
     * @return The stack pointer value.
     */
    public int getSP() {
        return SP;
    }

    /**
     * Returns the current value of the instruction pointer (IP).
     *
     * @return The instruction pointer value.
     */
    public int getIP() {
        return IP;
    }

    /**
     * Returns the status register (SR) as a single byte, where each bit
     * represents a flag: [---S ZOCF].
     * @return The integer value of the status register.
     */
    public int getSR() {
        int value = 0;
        if (fault) value |= 1;   // F (Bit 0)
        if (carry) value |= 2;   // C (Bit 1)
        if (overflow) value |= 4; // O (Bit 2)
        if (zero) value |= 8;    // Z (Bit 3)
        if (sign) value |= 16;   // S (Bit 4)
        return value;
    }

    /**
     * Checks if the Zero flag is set.
     *
     * @return True if the Zero flag is set, false otherwise.
     */
    public boolean isZero() {
        return zero;
    }

    /**
     * Checks if the Carry flag is set.
     *
     * @return True if the Carry flag is set, false otherwise.
     */
    public boolean isCarry() {
        return carry;
    }

    /**
     * Checks if the Fault flag is set, indicating a CPU error.
     *
     * @return True if the Fault flag is set, false otherwise.
     */
    public boolean isFault() {
        return fault;
    }

    /**
     * Checks if the Overflow flag is set.
     *
     * @return True if the Overflow flag is set, false otherwise.
     */
    public boolean isOverflow() {
        return overflow;
    }

    /**
     * Checks if the Sign flag is set.
     *
     * @return True if the Sign flag is set, false otherwise.
     */
    public boolean isSign() {
        return sign;
    }

    /**
     * Checks if the Halted flag is set.
     *
     * @return True if the Halted flag is set, false otherwise.
     */
    public boolean isHalted() {
        return halted;
    }

    /**
     * Resets the CPU to its initial state:
     * - All general-purpose registers are cleared.
     * - Stack pointer is set to its initial base value.
     * - Instruction pointer is set to 0.
     * - All status flags are cleared.
     */
    public void reset() {
        for (int i = 0; i < registers.length; i++) {
            registers[i] = 0;
        }
        zero = false;
        carry = false;
        overflow = false;
        sign = false;
        fault = false;
        halted = false;
        
        SP = maxSP;
        IP = 0;
    }

    /**
     * Fetches, decodes, and executes a single instruction from the memory location
     * pointed to by the Instruction Pointer (IP). This method is the core of the
     * CPU's execution cycle. It handles all defined opcodes, updates registers,
     * flags, and the IP accordingly.
     *
     * @throws IllegalStateException If the CPU is in a fault state or the IP is out of bounds.
     * @throws ArithmeticException   For division by zero.
     * @throws UnsupportedOperationException If an unknown or unimplemented opcode is encountered.
     */
    public void step() {
        if (fault) {
            throw new IllegalStateException("CPU is in a fault state. Reset to continue.");
        }

        try {
            if (IP < 0 || IP >= memory.getSize()) {
                throw new IllegalStateException("Instruction pointer is outside of memory");
            }

            int instr = memory.load(IP);
            int regTo, regFrom, number, memFrom;

            switch (instr) {
            
                case Opcodes.HALT:
                    halted = true;
                    IP++;
                    break;
                case Opcodes.MOV_REG_TO_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    registers[regTo] = registers[regFrom];
                    IP++;
                    break;
                case Opcodes.MOV_ADDRESS_TO_REG:
                    regTo = memory.load(++IP);
                    memFrom = memory.load(++IP);
                    registers[regTo] = memory.load(memFrom);
                    IP++;
                    break;
                case Opcodes.MOV_REGADDRESS_TO_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    registers[regTo] = memory.load(indirectRegisterAddress(regFrom));
                    IP++;
                    break;
                case Opcodes.MOV_REG_TO_ADDRESS:
                    memFrom = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    memory.store(memFrom, registers[regFrom]);
                    IP++;
                    break;
                case Opcodes.MOV_REG_TO_REGADDRESS:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    memory.store(indirectRegisterAddress(regTo), registers[regFrom]);
                    IP++;
                    break;
                case Opcodes.MOV_NUMBER_TO_ADDRESS:
                    memFrom = memory.load(++IP);
                    number = memory.load(++IP);
                    memory.store(memFrom, number);
                    IP++;
                    break;
                case Opcodes.MOV_NUMBER_TO_REGADDRESS:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    memory.store(indirectRegisterAddress(regTo), number);
                    IP++;
                    break;
                case Opcodes.MOV_STACK_OFFSET_TO_REG: // MOV reg, [SP+offset]
                    regTo = memory.load(++IP);
                    number = memory.load(++IP); // Here, 'number' is the offset
                    registers[regTo] = memory.load(SP + number);
                    IP++;
                    break;
                case Opcodes.MOV_REG_TO_STACK_OFFSET: // MOV [SP+offset], reg
                    number = memory.load(++IP); // Here, 'number' is the offset
                    regFrom = memory.load(++IP);
                    memory.store(SP + number, registers[regFrom]);
                    IP++;
                    break;
                case Opcodes.MOV_NUMBER_TO_REG:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    registers[regTo] = number;
                    IP++;
                    break;
                case Opcodes.ADD_REG_TO_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_add_reg = registers[regTo];
                    int val2_add_reg = registers[regFrom];
                    int result_add_reg = val1_add_reg + val2_add_reg;
                    setAddFlags(val1_add_reg, val2_add_reg, result_add_reg);
                    setZeroAndSignFlags(result_add_reg);
                    registers[regTo] = result_add_reg & 0xFF;
                    IP++;
                    break;
                case Opcodes.ADD_REGADDRESS_TO_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_add_regaddr = registers[regTo];
                    int val2_add_regaddr = memory.load(indirectRegisterAddress(regFrom));
                    int result_add_regaddr = val1_add_regaddr + val2_add_regaddr;
                    setAddFlags(val1_add_regaddr, val2_add_regaddr, result_add_regaddr);
                    setZeroAndSignFlags(result_add_regaddr);
                    registers[regTo] = result_add_regaddr & 0xFF;
                    IP++;
                    break;
                case Opcodes.ADD_ADDRESS_TO_REG:
                    regTo = memory.load(++IP);
                    memFrom = memory.load(++IP);
                    int val1_add_addr = registers[regTo];
                    int val2_add_addr = memory.load(memFrom);
                    int result_add_addr = val1_add_addr + val2_add_addr;
                    setAddFlags(val1_add_addr, val2_add_addr, result_add_addr);
                    setZeroAndSignFlags(result_add_addr);
                    registers[regTo] = result_add_addr & 0xFF;
                    IP++;
                    break;
                case Opcodes.ADD_NUMBER_TO_REG:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    int val1_add_num = registers[regTo];
                    int val2_add_num = number;
                    int result_add_num = val1_add_num + val2_add_num;
                    setAddFlags(val1_add_num, val2_add_num, result_add_num);
                    setZeroAndSignFlags(result_add_num);
                    registers[regTo] = result_add_num & 0xFF;
                    IP++;
                    break;
                case Opcodes.SUB_REG_FROM_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_sub_reg = registers[regTo];
                    int val2_sub_reg = registers[regFrom];
                    int result_sub_reg = val1_sub_reg - val2_sub_reg;
                    setSubFlags(val1_sub_reg, val2_sub_reg, result_sub_reg);
                    setZeroAndSignFlags(result_sub_reg);
                    registers[regTo] = result_sub_reg & 0xFF;
                    IP++;
                    break;
                case Opcodes.SUB_REGADDRESS_FROM_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_sub_regaddr = registers[regTo];
                    int val2_sub_regaddr = memory.load(indirectRegisterAddress(regFrom));
                    int result_sub_regaddr = val1_sub_regaddr - val2_sub_regaddr;
                    setSubFlags(val1_sub_regaddr, val2_sub_regaddr, result_sub_regaddr);
                    setZeroAndSignFlags(result_sub_regaddr);
                    registers[regTo] = result_sub_regaddr & 0xFF;
                    IP++;
                    break;
                case Opcodes.SUB_ADDRESS_FROM_REG:
                    regTo = memory.load(++IP);
                    memFrom = memory.load(++IP);
                    int val1_sub_addr = registers[regTo];
                    int val2_sub_addr = memory.load(memFrom);
                    int result_sub_addr = val1_sub_addr - val2_sub_addr;
                    setSubFlags(val1_sub_addr, val2_sub_addr, result_sub_addr);
                    setZeroAndSignFlags(result_sub_addr);
                    registers[regTo] = result_sub_addr & 0xFF;
                    IP++;
                    break;
                case Opcodes.SUB_NUMBER_FROM_REG:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    int val1_sub_num = registers[regTo];
                    int val2_sub_num = number;
                    int result_sub_num = val1_sub_num - val2_sub_num;
                    setSubFlags(val1_sub_num, val2_sub_num, result_sub_num);
                    setZeroAndSignFlags(result_sub_num);
                    registers[regTo] = result_sub_num & 0xFF;
                    IP++;
                    break;
                case Opcodes.MUL_REG_TO_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_mul_reg = registers[regTo];
                    int val2_mul_reg = registers[regFrom];
                    int result_mul_reg = val1_mul_reg * val2_mul_reg;
                    setMulFlags(val1_mul_reg, val2_mul_reg, result_mul_reg);
                    setZeroAndSignFlags(result_mul_reg);
                    registers[regTo] = result_mul_reg & 0xFF;
                    IP++;
                    break;
                case Opcodes.MUL_REGADDRESS_TO_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_mul_regaddr = registers[regTo];
                    int val2_mul_regaddr = memory.load(indirectRegisterAddress(regFrom));
                    int result_mul_regaddr = val1_mul_regaddr * val2_mul_regaddr;
                    setMulFlags(val1_mul_regaddr, val2_mul_regaddr, result_mul_regaddr);
                    setZeroAndSignFlags(result_mul_regaddr);
                    registers[regTo] = result_mul_regaddr & 0xFF;
                    IP++;
                    break;
                case Opcodes.MUL_ADDRESS_TO_REG:
                    regTo = memory.load(++IP);
                    memFrom = memory.load(++IP);
                    int val1_mul_addr = registers[regTo];
                    int val2_mul_addr = memory.load(memFrom);
                    int result_mul_addr = val1_mul_addr * val2_mul_addr;
                    setMulFlags(val1_mul_addr, val2_mul_addr, result_mul_addr);
                    setZeroAndSignFlags(result_mul_addr);
                    registers[regTo] = result_mul_addr & 0xFF;
                    IP++;
                    break;
                case Opcodes.MUL_NUMBER_TO_REG:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    int val1_mul_num = registers[regTo];
                    int val2_mul_num = number;
                    int result_mul_num = val1_mul_num * val2_mul_num;
                    setMulFlags(val1_mul_num, val2_mul_num, result_mul_num);
                    setZeroAndSignFlags(result_mul_num);
                    registers[regTo] = result_mul_num & 0xFF;
                    IP++;
                    break;
                case Opcodes.DIV_REG_FROM_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_div_reg = registers[regTo];
                    int val2_div_reg = registers[regFrom];
                    int result_div_reg = division(val1_div_reg, val2_div_reg);
                    setDivFlags(val1_div_reg, val2_div_reg, result_div_reg);
                    setZeroAndSignFlags(result_div_reg);
                    registers[regTo] = result_div_reg & 0xFF;
                    IP++;
                    break;
                case Opcodes.DIV_REGADDRESS_FROM_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_div_regaddr = registers[regTo];
                    int val2_div_regaddr = memory.load(indirectRegisterAddress(regFrom));
                    int result_div_regaddr = division(val1_div_regaddr, val2_div_regaddr);
                    setDivFlags(val1_div_regaddr, val2_div_regaddr, result_div_regaddr);
                    setZeroAndSignFlags(result_div_regaddr);
                    registers[regTo] = result_div_regaddr & 0xFF;
                    IP++;
                    break;
                case Opcodes.DIV_ADDRESS_FROM_REG:
                    regTo = memory.load(++IP);
                    memFrom = memory.load(++IP);
                    int val1_div_addr = registers[regTo];
                    int val2_div_addr = memory.load(memFrom);
                    int result_div_addr = division(val1_div_addr, val2_div_addr);
                    setDivFlags(val1_div_addr, val2_div_addr, result_div_addr);
                    setZeroAndSignFlags(result_div_addr);
                    registers[regTo] = result_div_addr & 0xFF;
                    IP++;
                    break;
                case Opcodes.DIV_NUMBER_FROM_REG:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    int val1_div_num = registers[regTo];
                    int val2_div_num = number;
                    int result_div_num = division(val1_div_num, val2_div_num);
                    setDivFlags(val1_div_num, val2_div_num, result_div_num);
                    setZeroAndSignFlags(result_div_num);
                    registers[regTo] = result_div_num & 0xFF;
                    IP++;
                    break;
                case Opcodes.INC_REG:
                    regTo = memory.load(++IP);
                    int val_inc = registers[regTo];
                    int result_inc = val_inc + 1;
                    setIncFlags(val_inc, result_inc);
                    setZeroAndSignFlags(result_inc);
                    registers[regTo] = result_inc & 0xFF;
                    IP++;
                    break;
                case Opcodes.DEC_REG:
                    regTo = memory.load(++IP);
                    int val_dec = registers[regTo];
                    int result_dec = val_dec - 1;
                    setDecFlags(val_dec, result_dec);
                    setZeroAndSignFlags(result_dec);
                    registers[regTo] = result_dec & 0xFF;
                    IP++;
                    break;
                case Opcodes.NEG_REG:
                    regTo = memory.load(++IP);
                    int val_neg = registers[regTo];
                    int result_neg = -val_neg;
                    setNegFlags(val_neg, result_neg);
                    setZeroAndSignFlags(result_neg);
                    registers[regTo] = result_neg & 0xFF;
                    IP++;
                    break;
                case Opcodes.CMP_REG_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_cmp_reg = registers[regTo];
                    int val2_cmp_reg = registers[regFrom];
                    int result_cmp_reg = val1_cmp_reg - val2_cmp_reg;
                    setSubFlags(val1_cmp_reg, val2_cmp_reg, result_cmp_reg);
                    setZeroAndSignFlags(result_cmp_reg);
                    IP++;
                    break;
                case Opcodes.CMP_REGADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_cmp_regaddr = registers[regTo];
                    int val2_cmp_regaddr = memory.load(indirectRegisterAddress(regFrom));
                    int result_cmp_regaddr = val1_cmp_regaddr - val2_cmp_regaddr;
                    setSubFlags(val1_cmp_regaddr, val2_cmp_regaddr, result_cmp_regaddr);
                    setZeroAndSignFlags(result_cmp_regaddr);
                    IP++;
                    break;
                case Opcodes.CMP_ADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    memFrom = memory.load(++IP);
                    int val1_cmp_addr = registers[regTo];
                    int val2_cmp_addr = memory.load(memFrom);
                    int result_cmp_addr = val1_cmp_addr - val2_cmp_addr;
                    setSubFlags(val1_cmp_addr, val2_cmp_addr, result_cmp_addr);
                    setZeroAndSignFlags(result_cmp_addr);
                    IP++;
                    break;
                case Opcodes.CMP_NUMBER_WITH_REG:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    int val1_cmp_num = registers[regTo];
                    int val2_cmp_num = number;
                    int result_cmp_num = val1_cmp_num - val2_cmp_num;
                    setSubFlags(val1_cmp_num, val2_cmp_num, result_cmp_num);
                    setZeroAndSignFlags(result_cmp_num);
                    IP++;
                    break;
                case Opcodes.JMP_REGADDRESS:
                    regTo = memory.load(++IP);
                    IP = registers[regTo];
                    break;
                case Opcodes.JMP_ADDRESS:
                    IP = memory.load(++IP);
                    break;
                case Opcodes.JZ_REGADDRESS:
                    regTo = memory.load(++IP);
                    if (zero) {
                        IP = registers[regTo];
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JZ_ADDRESS:
                    number = memory.load(++IP);
                    if (zero) {
                        IP = number;
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JNC_REGADDRESS:
                    regTo = memory.load(++IP);
                    if (!carry) {
                        IP = registers[regTo];
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JNC_ADDRESS:
                    number = memory.load(++IP);
                    if (!carry) {
                        IP = number;
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JNZ_REGADDRESS:
                    regTo = memory.load(++IP);
                    if (!zero) {
                        IP = registers[regTo];
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JNZ_ADDRESS:
                    number = memory.load(++IP);
                    if (!zero) {
                        IP = number;
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JA_REGADDRESS:
                    regTo = memory.load(++IP);
                    if (!zero && !carry) {
                        IP = registers[regTo];
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JA_ADDRESS:
                    number = memory.load(++IP);
                    if (!zero && !carry) {
                        IP = number;
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JNA_REGADDRESS:
                    regTo = memory.load(++IP);
                    if (zero || carry) {
                        IP = registers[regTo];
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JNA_ADDRESS:
                    number = memory.load(++IP);
                    if (zero || carry) {
                        IP = number;
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JS_REGADDRESS:
                    regTo = memory.load(++IP);
                    if (sign) {
                        IP = registers[regTo];
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JS_ADDRESS:
                    number = memory.load(++IP);
                    if (sign) {
                        IP = number;
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JNS_REGADDRESS:
                    regTo = memory.load(++IP);
                    if (!sign) {
                        IP = registers[regTo];
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JNS_ADDRESS:
                    number = memory.load(++IP);
                    if (!sign) {
                        IP = number;
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JC_REGADDRESS:
                    regTo = memory.load(++IP);
                    if (carry) {
                        IP = registers[regTo];
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.JC_ADDRESS:
                    number = memory.load(++IP);
                    if (carry) {
                        IP = number;
                    } else {
                        IP++;
                    }
                    break;
                case Opcodes.PUSH_REG:
                    regFrom = memory.load(++IP);
                    push(registers[regFrom]);
                    IP++;
                    break;
                case Opcodes.PUSH_REGADDRESS:
                    regFrom = memory.load(++IP);
                    push(memory.load(indirectRegisterAddress(regFrom)));
                    IP++;
                    break;
                case Opcodes.PUSH_ADDRESS:
                    memFrom = memory.load(++IP);
                    push(memory.load(memFrom));
                    IP++;
                    break;
                case Opcodes.PUSH_NUMBER:
                    number = memory.load(++IP);
                    push(number);
                    IP++;
                    break;
                case Opcodes.POP_REG:
                    regTo = memory.load(++IP);
                    registers[regTo] = pop();
                    IP++;
                    break;
                case Opcodes.CALL_REGADDRESS:
                    regTo = memory.load(++IP);
                    push(IP + 1);
                    IP = registers[regTo];
                    break;
                case Opcodes.CALL_ADDRESS:
                    number = memory.load(++IP);
                    push(IP + 1);
                    IP = number;
                    break;
                case Opcodes.RET:
                    IP = pop();
                    break;
                case Opcodes.PUSHF:
                    int flags = 0;
                    if (fault) flags |= 1;
                    if (carry) flags |= 2;
                    if (overflow) flags |= 4;
                    if (zero) flags |= 8;
                    if (sign) flags |= 16;
                    push(flags);
                    IP++;
                    break;
                case Opcodes.POPF:
                    flags = pop();
                    fault = (flags & 1) != 0;
                    carry = (flags & 2) != 0;
                    overflow = (flags & 4) != 0;
                    zero = (flags & 8) != 0;
                    sign = (flags & 16) != 0;
                    IP++;
                    break;
                case Opcodes.AND_REG_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_and_reg = registers[regTo] & registers[regFrom];
                    setZeroAndSignFlags(result_and_reg);
                    registers[regTo] = result_and_reg & 0xFF;
                    IP++;
                    break;
                case Opcodes.AND_REGADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_and_regaddr = registers[regTo] & memory.load(indirectRegisterAddress(regFrom));
                    setZeroAndSignFlags(result_and_regaddr);
                    registers[regTo] = result_and_regaddr & 0xFF;
                    IP++;
                    break;
                case Opcodes.AND_ADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    memFrom = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_and_addr = registers[regTo] & memory.load(memFrom);
                    setZeroAndSignFlags(result_and_addr);
                    registers[regTo] = result_and_addr & 0xFF;
                    IP++;
                    break;
                case Opcodes.AND_NUMBER_WITH_REG:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_and_num = registers[regTo] & number;
                    setZeroAndSignFlags(result_and_num);
                    registers[regTo] = result_and_num & 0xFF;
                    IP++;
                    break;
                case Opcodes.OR_REG_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_or_reg = registers[regTo] | registers[regFrom];
                    setZeroAndSignFlags(result_or_reg);
                    registers[regTo] = result_or_reg & 0xFF;
                    IP++;
                    break;
                case Opcodes.OR_REGADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_or_regaddr = registers[regTo] | memory.load(indirectRegisterAddress(regFrom));
                    setZeroAndSignFlags(result_or_regaddr);
                    registers[regTo] = result_or_regaddr & 0xFF;
                    IP++;
                    break;
                case Opcodes.OR_ADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    memFrom = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_or_addr = registers[regTo] | memory.load(memFrom);
                    setZeroAndSignFlags(result_or_addr);
                    registers[regTo] = result_or_addr & 0xFF;
                    IP++;
                    break;
                case Opcodes.OR_NUMBER_WITH_REG:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_or_num = registers[regTo] | number;
                    setZeroAndSignFlags(result_or_num);
                    registers[regTo] = result_or_num & 0xFF;
                    IP++;
                    break;
                case Opcodes.XOR_REG_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_xor_reg = registers[regTo] ^ registers[regFrom];
                    setZeroAndSignFlags(result_xor_reg);
                    registers[regTo] = result_xor_reg & 0xFF;
                    IP++;
                    break;
                case Opcodes.XOR_REGADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_xor_regaddr = registers[regTo] ^ memory.load(indirectRegisterAddress(regFrom));
                    setZeroAndSignFlags(result_xor_regaddr);
                    registers[regTo] = result_xor_regaddr & 0xFF;
                    IP++;
                    break;
                case Opcodes.XOR_ADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    memFrom = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_xor_addr = registers[regTo] ^ memory.load(memFrom);
                    setZeroAndSignFlags(result_xor_addr);
                    registers[regTo] = result_xor_addr & 0xFF;
                    IP++;
                    break;
                case Opcodes.XOR_NUMBER_WITH_REG:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_xor_num = registers[regTo] ^ number;
                    setZeroAndSignFlags(result_xor_num);
                    registers[regTo] = result_xor_num & 0xFF;
                    IP++;
                    break;
                case Opcodes.NOT_REG:
                    regTo = memory.load(++IP);
                    carry = false;
                    overflow = false;
                    int result_not = ~registers[regTo];
                    setZeroAndSignFlags(result_not);
                    registers[regTo] = result_not & 0xFF;
                    IP++;
                    break;
                case Opcodes.SHL_REG_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_shl_reg = registers[regTo];
                    int val2_shl_reg = registers[regFrom];
                    int result_shl_reg = val1_shl_reg << val2_shl_reg;
                    setShiftFlags(val1_shl_reg, val2_shl_reg, true); // true for left shift
                    setZeroAndSignFlags(result_shl_reg);
                    registers[regTo] = result_shl_reg & 0xFF;
                    IP++;
                    break;
                case Opcodes.SHL_REGADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_shl_regaddr = registers[regTo];
                    int val2_shl_regaddr = memory.load(indirectRegisterAddress(regFrom));
                    int result_shl_regaddr = val1_shl_regaddr << val2_shl_regaddr;
                    setShiftFlags(val1_shl_regaddr, val2_shl_regaddr, true);
                    setZeroAndSignFlags(result_shl_regaddr);
                    registers[regTo] = result_shl_regaddr & 0xFF;
                    IP++;
                    break;
                case Opcodes.SHL_ADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    memFrom = memory.load(++IP);
                    int val1_shl_addr = registers[regTo];
                    int val2_shl_addr = memory.load(memFrom);
                    int result_shl_addr = val1_shl_addr << val2_shl_addr;
                    setShiftFlags(val1_shl_addr, val2_shl_addr, true);
                    setZeroAndSignFlags(result_shl_addr);
                    registers[regTo] = result_shl_addr & 0xFF;
                    IP++;
                    break;
                case Opcodes.SHL_NUMBER_WITH_REG:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    int val1_shl_num = registers[regTo];
                    int val2_shl_num = number;
                    int result_shl_num = val1_shl_num << val2_shl_num;
                    setShiftFlags(val1_shl_num, val2_shl_num, true);
                    setZeroAndSignFlags(result_shl_num);
                    registers[regTo] = result_shl_num & 0xFF;
                    IP++;
                    break;
                case Opcodes.SHR_REG_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_shr_reg = registers[regTo];
                    int val2_shr_reg = registers[regFrom];
                    int result_shr_reg = val1_shr_reg >>> val2_shr_reg;
                    setShiftFlags(val1_shr_reg, val2_shr_reg, false); // false for right shift
                    setZeroAndSignFlags(result_shr_reg);
                    registers[regTo] = result_shr_reg & 0xFF;
                    IP++;
                    break;
                case Opcodes.SHR_REGADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    regFrom = memory.load(++IP);
                    int val1_shr_regaddr = registers[regTo];
                    int val2_shr_regaddr = memory.load(indirectRegisterAddress(regFrom));
                    int result_shr_regaddr = val1_shr_regaddr >>> val2_shr_regaddr;
                    setShiftFlags(val1_shr_regaddr, val2_shr_regaddr, false);
                    setZeroAndSignFlags(result_shr_regaddr);
                    registers[regTo] = result_shr_regaddr & 0xFF;
                    IP++;
                    break;
                case Opcodes.SHR_ADDRESS_WITH_REG:
                    regTo = memory.load(++IP);
                    memFrom = memory.load(++IP);
                    int val1_shr_addr = registers[regTo];
                    int val2_shr_addr = memory.load(memFrom);
                    int result_shr_addr = val1_shr_addr >>> val2_shr_addr;
                    setShiftFlags(val1_shr_addr, val2_shr_addr, false);
                    setZeroAndSignFlags(result_shr_addr);
                    registers[regTo] = result_shr_addr & 0xFF;
                    IP++;
                    break;
                case Opcodes.SHR_NUMBER_WITH_REG:
                    regTo = memory.load(++IP);
                    number = memory.load(++IP);
                    int val1_shr_num = registers[regTo];
                    int val2_shr_num = number;
                    int result_shr_num = val1_shr_num >>> val2_shr_num;
                    setShiftFlags(val1_shr_num, val2_shr_num, false);
                    setZeroAndSignFlags(result_shr_num);
                    registers[regTo] = result_shr_num & 0xFF;
                    IP++;
                    break;
                case Opcodes.OUT_IMM8:
                    int outPort = memory.load(++IP);
                    if (outPort == LIGHTS_PORT) {
                        // Traffic Lights port
                        trafficLights.setVisible(true);
                        trafficLights.updateLights((byte) registers[0]); // AL is always gpr[0]
                    } else if (outPort == HEATER_PORT) {
                        // Heater Control port
                        heater.setVisible(true);
                        heater.updateBurner((byte) registers[0]); // AL is always gpr[0]
                    } else {
                        // Potentially handle other OUT ports here in the future
                    }
                    IP++;
                    break;
                case Opcodes.IN_IMM8:
                    int inPort = memory.load(++IP);
                    if (inPort == HEATER_PORT) {
                        // Heater Control port
                        heater.setVisible(true);
                        registers[0] = heater.getStatus(); // AL is always gpr[0]
                    } else if (inPort == KEYBOARD_PORT) {
                        // Keyboard port
                        registers[0] = keyboard.getAsciiCode(); // This is a blocking call
                    } else {
                        // Potentially handle other IN ports here in the future
                        registers[0] = 0; // Default to 0 if port not handled
                    }
                    IP++;
                    break;
                default:
                    throw new UnsupportedOperationException("Opcode " + instr + " is not yet implemented");
            }

        } catch (Exception e) {
            fault = true;
            throw e;
        }
    }

    /**
     * Pushes a value onto the stack. The stack grows downwards in memory.
     *
     * @param value The 8-bit value to push onto the stack.
     * @throws IllegalStateException If a stack overflow occurs (SP goes below minSP).
     */
    private void push(int value) {
        memory.store(SP--, value);
        if (SP < minSP) {
            throw new IllegalStateException("Stack overflow");
        }
    }

    /**
     * Pops a value from the stack. The stack pointer is incremented before reading.
     *
     * @return The 8-bit value popped from the stack.
     * @throws IllegalStateException If a stack underflow occurs (SP goes above maxSP).
     */
    private int pop() {
        int value = memory.load(++SP);
        if (SP > maxSP) {
            throw new IllegalStateException("Stack underflow");
        }
        return value;
    }


    /**
     * Sets the Zero and Sign flags based on the 8-bit result of an operation.
     *
     * @param result The result of the operation.
     */
    private void setZeroAndSignFlags(int result) {
        int result8bit = result & 0xFF;
        zero = (result8bit == 0);
        sign = (result8bit & 0x80) != 0;
    }

    /**
     * Sets the Carry and Overflow flags for an addition operation.
     * @param operand1 The first operand.
     * @param operand2 The second operand.
     * @param result The full integer result of the addition.
     */
    private void setAddFlags(int operand1, int operand2, int result) {
        // Carry flag (unsigned overflow): Set if the sum exceeds 255.
        carry = ((operand1 & 0xFF) + (operand2 & 0xFF)) > 0xFF;

        // Overflow flag (signed overflow): Set if adding two numbers with the
        // same sign results in a number with the opposite sign.
        boolean op1_sign = (operand1 & 0x80) != 0;
        boolean op2_sign = (operand2 & 0x80) != 0;
        boolean result_sign = (result & 0x80) != 0;

        overflow = (op1_sign == op2_sign) && (op1_sign != result_sign);
    }

    /**
     * Sets the Carry (borrow) and Overflow flags for a subtraction operation.
     * @param operand1 The first operand (minuend).
     * @param operand2 The second operand (subtrahend).
     * @param result The full integer result of the subtraction.
     */
    private void setSubFlags(int operand1, int operand2, int result) {
        // Carry flag (borrow): Set if an unsigned borrow is required.
        carry = (operand1 & 0xFF) < (operand2 & 0xFF);

        // Overflow flag (signed overflow): Set if subtracting numbers with different
        // signs results in a number with the same sign as the subtrahend.
        boolean op1_sign = (operand1 & 0x80) != 0;
        boolean op2_sign = (operand2 & 0x80) != 0;
        boolean result_sign = (result & 0x80) != 0;

        overflow = (op1_sign != op2_sign) && (op1_sign != result_sign);
    }

    /**
     * Sets the Carry and Overflow flags for a multiplication operation.
     * @param operand1 The first operand.
     * @param operand2 The second operand.
     * @param result The full integer result of the multiplication.
     */
    private void setMulFlags(int operand1, int operand2, int result) {
        // Carry/Overflow: Set if the result cannot fit into a single byte.
        // For simplicity, we check if the result is outside the 8-bit unsigned range.
        boolean isOverflow = (result > 0xFF);
        carry = isOverflow;
        overflow = isOverflow;
    }

    /**
     * Sets flags for a division operation. For this simulator, Carry and Overflow
     * are cleared to a known state (false) as their behavior is often undefined.
     * @param operand1 The dividend.
     * @param operand2 The divisor.
     * @param result The result of the division.
     */
    private void setDivFlags(int operand1, int operand2, int result) {
        carry = false;
        overflow = false;
    }

    /**
     * Sets the Carry and Overflow flags for an increment operation.
     * @param operand The operand being incremented.
     * @param result The full integer result of the increment.
     */
    private void setIncFlags(int operand, int result) {
        // Carry flag: Set if the operand was 0xFF, causing a wrap-around.
        carry = (operand & 0xFF) == 0xFF;

        // Overflow flag: Set if the operand was 0x7F (127), wrapping to 0x80 (-128).
        overflow = (operand == 0x7F);
    }

    /**
     * Sets the Overflow flag for a decrement operation. The Carry flag is not affected.
     * @param operand The operand being decremented.
     * @param result The full integer result of the decrement.
     */
    private void setDecFlags(int operand, int result) {
        // The DEC instruction does not affect the Carry flag.

        // Overflow flag: Set if the operand was 0x80 (-128), wrapping to 0x7F (127).
        overflow = (operand == 0x80);
    }

    /**
     * Sets the Carry and Overflow flags for a negation operation.
     * @param operand The operand being negated.
     * @param result The full integer result of the negation.
     */
    private void setNegFlags(int operand, int result) {
        // Carry flag: Set if the original operand was not 0.
        carry = (operand & 0xFF) != 0;

        // Overflow flag: Set if the operand was 0x80 (-128), as its negation cannot be represented.
        overflow = (operand == 0x80);
    }

    /**
     * Sets the Carry flag for a shift operation (SHL or SHR).
     * The Overflow flag is always cleared by shift operations in this simulator.
     * @param operand The operand being shifted.
     * @param shiftAmount The number of bits to shift.
     * @param isLeftShift True for a left shift (SHL), false for a right shift (SHR).
     */
    private void setShiftFlags(int operand, int shiftAmount, boolean isLeftShift) {
        carry = false;
        overflow = false; // Shifts typically clear overflow

        if (shiftAmount == 0) {
            return; // No shift, no flags affected
        }

        if (isLeftShift) {
            // Carry flag for SHL: last bit shifted out (MSB of original operand)
            carry = ((operand << (shiftAmount - 1)) & 0x80) != 0;
        } else {
            // Carry flag for SHR: last bit shifted out (LSB of original operand)
            carry = ((operand >> (shiftAmount - 1)) & 0x01) != 0;
        }
    }

    /**
     * Performs integer division and handles division by zero.
     *
     * @param dividend The number to be divided.
     * @param divisor The number to divide by.
     * @return The result of the integer division.
     * @throws ArithmeticException If the divisor is zero.
     */
    private int division(int dividend, int divisor) {
        if (divisor == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return dividend / divisor;
    }

    /**
     * Calculates the effective memory address for indirect register addressing modes.
     * This is not currently used by any instruction in this simplified architecture
     * but is kept for potential future expansion.
     *
     * @param value The operand value from the machine code.
     * @return The calculated effective memory address.
     */
    private int indirectRegisterAddress(int value) {
        int reg = value % 8;
        int base;
        if (reg < registers.length) {
            base = registers[reg];
        } else {
            base = SP;
        }
        int offset = value / 8;
        if (offset > 15) { // Handle two's complement for negative offsets
            offset = offset - 32;
        }
        return base + offset;
    }
}
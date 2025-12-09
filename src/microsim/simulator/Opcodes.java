/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.0
 * @since 2025-12-09
 *
 * License: GNU General Public License v3.0
 */

package microsim.simulator;

/**
 * Defines the operation codes (opcodes) for the 8-bit assembler simulator.
 * Each opcode is a unique integer constant representing a specific instruction
 * that the CPU can execute.
 */
public final class Opcodes {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Opcodes() {
        // Private constructor to prevent instantiation
    }

    /** No operation / Halt instruction. */
    public static final int NONE = 0;

    // --- MOV instructions ---
    /** Move value from one register to another. */
    public static final int MOV_REG_TO_REG = 1;
    /** Move value from a memory address to a register. */
    public static final int MOV_ADDRESS_TO_REG = 2;
    /** Move value from a register-indirect address to a register. */
    public static final int MOV_REGADDRESS_TO_REG = 3;
    /** Move value from a register to a memory address. */
    public static final int MOV_REG_TO_ADDRESS = 4;
    /** Move value from a register to a register-indirect address. */
    public static final int MOV_REG_TO_REGADDRESS = 5;
    /** Move a literal number to a register. */
    public static final int MOV_NUMBER_TO_REG = 6;
    /** Move a literal number to a memory address. */
    public static final int MOV_NUMBER_TO_ADDRESS = 7;
    /** Move a literal number to a register-indirect address. */
    public static final int MOV_NUMBER_TO_REGADDRESS = 8;
    /** Move value from a stack-offset address [SP+offset] to a register. */
    public static final int MOV_STACK_OFFSET_TO_REG = 9;
    /** Move value from a register to a stack-offset address [SP+offset]. */
    public static final int MOV_REG_TO_STACK_OFFSET = 10;
    
    // --- ADD instructions ---
    /** Add value of a register to another register. */
    public static final int ADD_REG_TO_REG = 11;
    /** Add value from a register-indirect address to a register. */
    public static final int ADD_REGADDRESS_TO_REG = 12;
    /** Add value from a memory address to a register. */
    public static final int ADD_ADDRESS_TO_REG = 13;
    /** Add a literal number to a register. */
    public static final int ADD_NUMBER_TO_REG = 14;

    // --- SUB instructions ---
    /** Subtract value of a register from another register. */
    public static final int SUB_REG_FROM_REG = 15;
    /** Subtract value from a register-indirect address from a register. */
    public static final int SUB_REGADDRESS_FROM_REG = 16;
    /** Subtract value from a memory address from a register. */
    public static final int SUB_ADDRESS_FROM_REG = 17;
    /** Subtract a literal number from a register. */
    public static final int SUB_NUMBER_FROM_REG = 18;

    // --- INC/DEC/NEG instructions ---
    /** Increment a register by 1. */
    public static final int INC_REG = 19;
    /** Decrement a register by 1. */
    public static final int DEC_REG = 20;
    /** Negate a register (two's complement). */
    public static final int NEG_REG = 21;
    
    // --- CMP instructions ---
    /** Compare value of a register with another register. */
    public static final int CMP_REG_WITH_REG = 22;
    /** Compare value from a register-indirect address with a register. */
    public static final int CMP_REGADDRESS_WITH_REG = 23;
    /** Compare value from a memory address with a register. */
    public static final int CMP_ADDRESS_WITH_REG = 24;
    /** Compare a literal number with a register. */
    public static final int CMP_NUMBER_WITH_REG = 25;

    // --- JMP instructions ---
    /** Unconditional jump to an address specified by a register. */
    public static final int JMP_REGADDRESS = 26;
    /** Unconditional jump to a direct memory address. */
    public static final int JMP_ADDRESS = 27;

    // --- Conditional Jump instructions (Carry Flag) ---
    /** Jump if Carry flag is set (JC, JB). */
    public static final int JC_REGADDRESS = 28;
    /** Jump if Carry flag is set (JC, JB). */
    public static final int JC_ADDRESS = 29;
    /** Jump if No Carry flag is set (JNC, JAE). */
    public static final int JNC_REGADDRESS = 30;
    /** Jump if No Carry flag is set (JNC, JAE). */
    public static final int JNC_ADDRESS = 31;

    // --- Conditional Jump instructions (Zero Flag) ---
    /** Jump if Zero flag is set (JZ, JE). */
    public static final int JZ_REGADDRESS = 32;
    /** Jump if Zero flag is set (JZ, JE). */
    public static final int JZ_ADDRESS = 33;
    /** Jump if No Zero flag is set (JNZ, JNE). */
    public static final int JNZ_REGADDRESS = 34;
    /** Jump if No Zero flag is set (JNZ, JNE). */
    public static final int JNZ_ADDRESS = 35;

    // --- Conditional Jump instructions (Above/Below) ---
    /** Jump if Above (JA, JNBE) - (CF=0 and ZF=0). */
    public static final int JA_REGADDRESS = 36;
    /** Jump if Above (JA, JNBE) - (CF=0 and ZF=0). */
    public static final int JA_ADDRESS = 37;
    /** Jump if Not Above (JNA, JBE) - (CF=1 or ZF=1). */
    public static final int JNA_REGADDRESS = 38;
    /** Jump if Not Above (JNA, JBE) - (CF=1 or ZF=1). */
    public static final int JNA_ADDRESS = 39;

    // --- Conditional Jump instructions (Sign Flag) ---
    /** Jump if Sign flag is set (JS). */
    public static final int JS_REGADDRESS = 40;
    /** Jump if Sign flag is set (JS). */
    public static final int JS_ADDRESS = 41;
    /** Jump if No Sign flag is set (JNS). */
    public static final int JNS_REGADDRESS = 42;
    /** Jump if No Sign flag is set (JNS). */
    public static final int JNS_ADDRESS = 43;

    // --- Stack operations ---
    /** Push register value onto the stack. */
    public static final int PUSH_REG = 44;
    /** Push value from a register-indirect address onto the stack. */
    public static final int PUSH_REGADDRESS = 45;
    /** Push value from a memory address onto the stack. */
    public static final int PUSH_ADDRESS = 46;
    /** Push a literal number onto the stack. */
    public static final int PUSH_NUMBER = 47;
    /** Pop value from stack into a register. */
    public static final int POP_REG = 48;
    /** Push the flags register onto the stack. */
    public static final int PUSHF = 49;
    /** Pop the flags register from the stack. */
    public static final int POPF = 50;
    
    // --- Subroutine Call/Return ---
    /** Call subroutine at an address specified by a register. */
    public static final int CALL_REGADDRESS = 51;
    /** Call subroutine at a direct memory address. */
    public static final int CALL_ADDRESS = 52;
    /** Return from subroutine. */
    public static final int RET = 53;

    // --- Multiplication instructions ---
    /** Multiply register by value from another register. */
    public static final int MUL_REG_TO_REG = 54;
    /** Multiply register by value from a register-indirect address. */
    public static final int MUL_REGADDRESS_TO_REG = 55;
    /** Multiply register by value from a memory address. */
    public static final int MUL_ADDRESS_TO_REG = 56;
    /** Multiply register by a literal number. */
    public static final int MUL_NUMBER_TO_REG = 57;

    // --- Division instructions ---
    /** Divide register by value from another register. */
    public static final int DIV_REG_FROM_REG = 58;
    /** Divide register by value from a register-indirect address. */
    public static final int DIV_REGADDRESS_FROM_REG = 59;
    /** Divide register by value from a memory address. */
    public static final int DIV_ADDRESS_FROM_REG = 60;
    /** Divide register by a literal number. */
    public static final int DIV_NUMBER_FROM_REG = 61;

    // --- Logical AND instructions ---
    /** Bitwise AND register with another register. */
    public static final int AND_REG_WITH_REG = 62;
    /** Bitwise AND register with value from a register-indirect address. */
    public static final int AND_REGADDRESS_WITH_REG = 63;
    /** Bitwise AND register with value from a memory address. */
    public static final int AND_ADDRESS_WITH_REG = 64;
    /** Bitwise AND register with a literal number. */
    public static final int AND_NUMBER_WITH_REG = 65;

    // --- Logical OR instructions ---
    /** Bitwise OR register with another register. */
    public static final int OR_REG_WITH_REG = 66;
    /** Bitwise OR register with value from a register-indirect address. */
    public static final int OR_REGADDRESS_WITH_REG = 67;
    /** Bitwise OR register with value from a memory address. */
    public static final int OR_ADDRESS_WITH_REG = 68;
    /** Bitwise OR register with a literal number. */
    public static final int OR_NUMBER_WITH_REG = 69;

    // --- Logical XOR instructions ---
    /** Bitwise XOR register with another register. */
    public static final int XOR_REG_WITH_REG = 70;
    /** Bitwise XOR register with value from a register-indirect address. */
    public static final int XOR_REGADDRESS_WITH_REG = 71;
    /** Bitwise XOR register with value from a memory address. */
    public static final int XOR_ADDRESS_WITH_REG = 72;
    /** Bitwise XOR register with a literal number. */
    public static final int XOR_NUMBER_WITH_REG = 73;

    // --- Logical NOT instruction ---
    /** Bitwise NOT (one's complement) on a register. */
    public static final int NOT_REG = 74;

    // --- Shift Left instructions ---
    /** Shift left register by value from another register. */
    public static final int SHL_REG_WITH_REG = 75;
    /** Shift left register by value from a register-indirect address. */
    public static final int SHL_REGADDRESS_WITH_REG = 76;
    /** Shift left register by value from a memory address. */
    public static final int SHL_ADDRESS_WITH_REG = 77;
    /** Shift left register by a literal number. */
    public static final int SHL_NUMBER_WITH_REG = 78;

    // --- Shift Right instructions ---
    /** Shift right register by value from another register. */
    public static final int SHR_REG_WITH_REG = 79;
    /** Shift right register by value from a register-indirect address. */
    public static final int SHR_REGADDRESS_WITH_REG = 80;
    /** Shift right register by value from a memory address. */
    public static final int SHR_ADDRESS_WITH_REG = 81;
    /** Shift right register by a literal number. */
    public static final int SHR_NUMBER_WITH_REG = 82;

    // --- I/O instructions ---
    /** Output from AL to an immediate port address. */
    public static final int OUT_IMM8 = 83;
    /** Input from an immediate port address to AL. */
    public static final int IN_IMM8 = 84;
}
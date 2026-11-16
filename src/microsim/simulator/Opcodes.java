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

    // -- HLT instruction (0) ---
    public static final int HALT = 0;

    // --- MOV instructions (1-10) ---
    public static final int MOV_NUMBER_TO_REG = 1;      // reg, byte
    public static final int MOV_REG_TO_REG = 2;         // reg, reg
    public static final int MOV_ADDRESS_TO_REG = 3;     // reg, [addr]
    public static final int MOV_REGADDRESS_TO_REG = 4;  // reg, [reg]
    public static final int MOV_STACK_OFFSET_TO_REG = 5;// reg, [SP+offset]
    public static final int MOV_NUMBER_TO_ADDRESS = 6;      // [addr], byte
    public static final int MOV_REG_TO_ADDRESS = 7;         // [addr], reg
    public static final int MOV_NUMBER_TO_REGADDRESS = 8;   // [reg], byte
    public static final int MOV_REG_TO_REGADDRESS = 9;      // [reg], reg
    public static final int MOV_REG_TO_STACK_OFFSET = 10;   // [SP+offset], reg
    
    // --- ADD instructions (11-14) ---
    public static final int ADD_NUMBER_TO_REG = 11;     // reg, byte
    public static final int ADD_REG_TO_REG = 12;        // reg, reg
    public static final int ADD_ADDRESS_TO_REG = 13;    // reg, [addr]
    public static final int ADD_REGADDRESS_TO_REG = 14; // reg, [reg]

    // --- SUB instructions (15-18) ---
    public static final int SUB_NUMBER_FROM_REG = 15;   // reg, byte
    public static final int SUB_REG_FROM_REG = 16;      // reg, reg
    public static final int SUB_ADDRESS_FROM_REG = 17;  // reg, [addr]
    public static final int SUB_REGADDRESS_FROM_REG = 18; // reg, [reg]

    // --- INC/DEC/NEG instructions (19-21) ---
    public static final int INC_REG = 19;
    public static final int DEC_REG = 20;
    public static final int NEG_REG = 21;
    
    // --- CMP instructions (22-25) ---
    public static final int CMP_NUMBER_WITH_REG = 22;   // reg, byte
    public static final int CMP_REG_WITH_REG = 23;      // reg, reg
    public static final int CMP_ADDRESS_WITH_REG = 24;  // reg, [addr]
    public static final int CMP_REGADDRESS_WITH_REG = 25; // reg, [reg]

    // --- JMP instructions (26-27) ---
    public static final int JMP_ADDRESS = 26;
    public static final int JMP_REGADDRESS = 27;

    // --- Conditional Jump instructions (28-47) ---
    public static final int JC_ADDRESS = 28;
    public static final int JC_REGADDRESS = 29;
    public static final int JNC_ADDRESS = 30;
    public static final int JNC_REGADDRESS = 31;
    public static final int JZ_ADDRESS = 32;
    public static final int JZ_REGADDRESS = 33;
    public static final int JNZ_ADDRESS = 34;
    public static final int JNZ_REGADDRESS = 35;
    public static final int JA_ADDRESS = 36;
    public static final int JA_REGADDRESS = 37;
    public static final int JNA_ADDRESS = 38;
    public static final int JNA_REGADDRESS = 39;
    public static final int JS_ADDRESS = 40;
    public static final int JS_REGADDRESS = 41;
    public static final int JNS_ADDRESS = 42;
    public static final int JNS_REGADDRESS = 43;
    public static final int JO_ADDRESS = 44;
    public static final int JO_REGADDRESS = 45;
    public static final int JNO_ADDRESS = 46;
    public static final int JNO_REGADDRESS = 47;

    // --- Stack operations (48-54) ---
    public static final int PUSH_NUMBER = 48;       // byte
    public static final int PUSH_REG = 49;          // reg
    public static final int PUSH_ADDRESS = 50;      // [addr]
    public static final int PUSH_REGADDRESS = 51;   // [reg]
    public static final int POP_REG = 52;
    public static final int PUSHF = 53;
    public static final int POPF = 54;
    
    // --- Subroutine Call/Return (55-57) ---
    public static final int CALL_ADDRESS = 55;
    public static final int CALL_REGADDRESS = 56;
    public static final int RET = 57;

    // --- Multiplication instructions (58-61) ---
    public static final int MUL_NUMBER_TO_REG = 58;     // reg, byte
    public static final int MUL_REG_TO_REG = 59;        // reg, reg
    public static final int MUL_ADDRESS_TO_REG = 60;    // reg, [addr]
    public static final int MUL_REGADDRESS_TO_REG = 61; // reg, [reg]

    // --- Division instructions (62-65) ---
    public static final int DIV_NUMBER_FROM_REG = 62;   // reg, byte
    public static final int DIV_REG_FROM_REG = 63;      // reg, reg
    public static final int DIV_ADDRESS_FROM_REG = 64;  // reg, [addr]
    public static final int DIV_REGADDRESS_FROM_REG = 65; // reg, [reg]

    // --- Logical AND instructions (66-69) ---
    public static final int AND_NUMBER_WITH_REG = 66;   // reg, byte
    public static final int AND_REG_WITH_REG = 67;      // reg, reg
    public static final int AND_ADDRESS_WITH_REG = 68;  // reg, [addr]
    public static final int AND_REGADDRESS_WITH_REG = 69; // reg, [reg]

    // --- Logical OR instructions (70-73) ---
    public static final int OR_NUMBER_WITH_REG = 70;    // reg, byte
    public static final int OR_REG_WITH_REG = 71;       // reg, reg
    public static final int OR_ADDRESS_WITH_REG = 72;   // reg, [addr]
    public static final int OR_REGADDRESS_WITH_REG = 73;  // reg, [reg]

    // --- Logical XOR instructions (74-77) ---
    public static final int XOR_NUMBER_WITH_REG = 74;   // reg, byte
    public static final int XOR_REG_WITH_REG = 75;      // reg, reg
    public static final int XOR_ADDRESS_WITH_REG = 76;  // reg, [addr]
    public static final int XOR_REGADDRESS_WITH_REG = 77; // reg, [reg]

    // --- Logical NOT instruction (78) ---
    public static final int NOT_REG = 78;

    // --- Shift Left instructions (79-82) ---
    public static final int SHL_NUMBER_WITH_REG = 79;   // reg, byte
    public static final int SHL_REG_WITH_REG = 80;      // reg, reg
    public static final int SHL_ADDRESS_WITH_REG = 81;  // reg, [addr]
    public static final int SHL_REGADDRESS_WITH_REG = 82; // reg, [reg]

    // --- Shift Right instructions (83-86) ---
    public static final int SHR_NUMBER_WITH_REG = 83;   // reg, byte
    public static final int SHR_REG_WITH_REG = 84;      // reg, reg
    public static final int SHR_ADDRESS_WITH_REG = 85;  // reg, [addr]
    public static final int SHR_REGADDRESS_WITH_REG = 86; // reg, [reg]

    // --- I/O instructions (87-88) ---
    public static final int OUT_IMM8 = 87;
    public static final int IN_IMM8 = 88;
}
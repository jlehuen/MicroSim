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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Assembler class is responsible for converting assembly code into machine code
 * (an array of integers representing opcodes and operands). It performs a two-pass
 * process: the first pass identifies all labels and their corresponding addresses,
 * and the second pass replaces label references with those addresses to generate
 * the final machine code. It handles parsing instructions, operands, labels, and
 * various number formats (decimal, hexadecimal).
 */
public class Assembler {

    private Map<Integer, Integer> addressToLineMap;

    /**
     * Holds the result of the assembly process, including the generated machine
     * code and a map linking memory addresses to source code line numbers.
     */
    public static class AssemblyResult {
        public final int[] machineCode;
        public final Map<Integer, Integer> addressToLineMap;

        /**
         * Constructs an AssemblyResult.
         * @param machineCode The generated machine code.
         * @param addressToLineMap A map from memory addresses to line numbers.
         */
        AssemblyResult(int[] machineCode, Map<Integer, Integer> addressToLineMap) {
            this.machineCode = machineCode;
            this.addressToLineMap = addressToLineMap;
        }
    }

    /**
     * Represents an operand in an assembly instruction, holding its type and value.
     */
    private static class Operand {
        /**
         * Enum defining the possible types of an operand.
         */
        enum Type {
            REGISTER,
            REGADDRESS, // Register-addressed memory location (e.g., [AL])
            ADDRESS,    // Direct memory address (e.g., [100])
            NUMBER,     // Literal number (e.g., 10, 0xA)
            NUMBERS,    // List of numbers (for DB instruction with strings)
            LABEL,       // A symbolic label
            STACK_OFFSET // Stack pointer + offset (e.g., [SP+4])
        }

        Type type;
        Object value; // Can be Integer, String (for labels), or List<Integer> (for numbers)

        /**
         * Constructs an Operand with the given type and value.
         *
         * @param type  The type of the operand.
         * @param value The value of the operand.
         */
        Operand(Type type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    /**
     * Parses a string input into an integer, supporting decimal and hexadecimal formats.
     *
     * @param input The string to parse.
     * @return The parsed integer value.
     * @throws IllegalArgumentException If the number format is invalid.
     */
    private int parseNumber(String input, int lineNumber) {
        String lowerInput = input.toLowerCase().trim();
        if (lowerInput.isEmpty()) {
            throw new IllegalArgumentException("Invalid number format: empty string on line " + lineNumber + ".");
        }
        try {
            if (lowerInput.startsWith("0x")) {
                String hexValue = lowerInput.substring(2);
                if (hexValue.isEmpty()) {
                    throw new NumberFormatException("Hexadecimal value is empty.");
                }
                return Integer.parseInt(hexValue, 16);
            }
            return Integer.parseInt(lowerInput);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: '" + input + "' on line " + lineNumber + ".");
        }
    }

    /**
     * Parses a string input into a register index.
     *
     * @param input The string representing the register name (e.g., "AL", "SP").
     * @return The integer index of the register, or null if not a valid register.
     */
    private Integer parseRegister(String input) {
        switch (input.toUpperCase()) {
            case "AL": return 0;
            case "BL": return 1;
            case "CL": return 2;
            case "DL": return 3;
            case "SP": return 4;
            default: return null;
        }
    }

    /**
     * Checks if a string is a valid label format (starts with a letter or dot,
     * followed by alphanumeric characters or underscores).
     *
     * @param input The string to check.
     * @return The input string if it's a valid label, otherwise null.
     */
    private String parseLabel(String input) {
        // Labels must start with a letter or dot and contain alphanumeric characters or underscore
        if (input.matches("[.A-Za-z]\\w*")) {
            return input;
        }
        return null;
    }

    /**
     * Parses an operand string and returns an Operand object containing its type and value.
     * Handles registers, memory addresses (direct, register-indirect, and stack-relative),
     * numbers, strings, and characters.
     *
     * @param input The string representation of the operand.
     * @return An Operand object.
     * @throws IllegalArgumentException If the operand format is invalid.
     */
    private Operand getValue(String input, int lineNumber) {
        if (input.startsWith("[")) { // Memory address
            if (!input.endsWith("]")) {
                throw new IllegalArgumentException("Invalid memory operand format: missing ']' on line " + lineNumber);
            }
            String content = input.substring(1, input.length() - 1).trim();

            // Check for [SP +/- offset]
            String upperContent = content.toUpperCase();
            if (upperContent.startsWith("SP")) {
                String remaining = content.substring(2).trim();
                if (remaining.startsWith("+") || remaining.startsWith("-")) {
                    char operator = remaining.charAt(0);
                    String offsetStr = remaining.substring(1).trim();
                    int offset = parseNumber(offsetStr, lineNumber);
                    if (operator == '-') {
                        offset = -offset;
                    }
                    return new Operand(Operand.Type.STACK_OFFSET, offset);
                }
            }

            // If not [SP +/- x], fall back to existing logic
            Integer register = parseRegister(content);
            if (register != null) {
                return new Operand(Operand.Type.REGADDRESS, register);
            }
            try {
                int number = parseNumber(content, lineNumber);
                return new Operand(Operand.Type.ADDRESS, number);
            } catch (IllegalArgumentException e) {
                // Not a number, so it must be a label
                return new Operand(Operand.Type.ADDRESS, content); // Treat as label for now, resolve later
            }
        } else if (input.startsWith("\"")) { // String literal
            String text = input.substring(1, input.length() - 1);
            List<Integer> chars = new ArrayList<>();
            for (char c : text.toCharArray()) {
                chars.add((int) c);
            }
            return new Operand(Operand.Type.NUMBERS, chars);
        } else if (input.startsWith("'")) { // Character literal
            String character = input.substring(1, input.length() - 1);
            if (character.length() > 1) {
                throw new IllegalArgumentException("Only one character is allowed. Use String instead on line " + lineNumber);
            }
            return new Operand(Operand.Type.NUMBER, (int) character.charAt(0));
        } else { // Register, Number, or Label
            Integer register = parseRegister(input);
            if (register != null) {
                return new Operand(Operand.Type.REGISTER, register);
            }
            String label = parseLabel(input);
            if (label != null) {
                return new Operand(Operand.Type.LABEL, label);
            }
            int number = parseNumber(input, lineNumber);
            return new Operand(Operand.Type.NUMBER, number);
        }
    }

    /**
     * Assembles a given assembly source code into machine code.
     * This method performs a two-pass assembly process. The first pass builds a
     * symbol table of labels and their addresses. The second pass generates the
     * final machine code, replacing label references with their resolved addresses.
     *
     * @param source The assembly code as a single string.
     * @return An {@link AssemblyResult} containing the machine code and address-to-line mapping.
     * @throws IllegalArgumentException If there is a syntax error, duplicate label, or undefined label.
     */
    @SuppressWarnings("unchecked")
    public AssemblyResult assemble(String source) {
        addressToLineMap = new HashMap<>();
        int[] code = new int[256]; // Represents the full 256-byte memory map
        int addressCounter = 0;
        Map<String, Integer> labels = new HashMap<>();
        Map<Integer, String> labelReferences = new HashMap<>();

        String[] lines = source.split("\n");

        // First pass: Collect labels and generate preliminary machine code
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNumber = i + 1;

            // Strip comments
            int commentIndex = line.indexOf(';');
            if (commentIndex != -1) {
                line = line.substring(0, commentIndex);
            }
            line = line.trim();

            if (line.isEmpty()) { // Skip empty lines
                continue;
            }

            // Check for label definition
            int labelEnd = line.indexOf(":");
            if (labelEnd != -1) {
                String label = line.substring(0, labelEnd);
                if (labels.containsKey(label.toUpperCase())) {
                    throw new IllegalArgumentException("Duplicate label: " + label + " on line " + lineNumber);
                }
                labels.put(label, addressCounter); // Store label with current address
                line = line.substring(labelEnd + 1).trim();
            }

            if (line.isEmpty()) { // Line might have only a label
                continue;
            }
            
            addressToLineMap.put(addressCounter, i);
            // Parse instruction and operands
            String[] lineParts = line.split("\\s+", 2);
            String instruction = lineParts[0];
            String operandsString = (lineParts.length > 1) ? lineParts[1] : "";

            // Process instructions
            switch (instruction.toUpperCase()) {
                case "ORG":
                    Operand orgOp = getValue(operandsString, lineNumber);
                    if (orgOp.type != Operand.Type.NUMBER) {
                        throw new IllegalArgumentException("ORG directive requires a numeric address on line " + lineNumber);
                    }
                    addressCounter = (Integer) orgOp.value;
                    if (addressCounter < 0 || addressCounter >= code.length) {
                        throw new IllegalArgumentException("ORG address out of memory bounds (0-255) on line " + lineNumber);
                    }
                    break;
                case "DB":
                    Operand p1 = getValue(operandsString, lineNumber);
                    if (p1.type == Operand.Type.NUMBER) {
                        code[addressCounter++] = (Integer) p1.value;
                    } else if (p1.type == Operand.Type.NUMBERS) {
                        for (int val : (List<Integer>) p1.value) {
                            code[addressCounter++] = val;
                        }
                    } else {
                        throw new IllegalArgumentException("DB does not support this operand on line " + lineNumber);
                    }
                    break;
                case "HLT":
                    code[addressCounter++] = Opcodes.NONE;
                    break;
                case "MOV":
                case "ADD":
                case "SUB":
                case "CMP":
                case "AND":
                case "OR":
                case "XOR":
                case "SHL":
                case "SHR":
                case "MUL":
                case "DIV":
                    String[] parts = operandsString.split(",", 2);
                    if (parts.length != 2) {
                        throw new IllegalArgumentException(instruction.toUpperCase() + " requires two operands separated by a comma on line " + lineNumber);
                    }
                    Operand op1 = getValue(parts[0].trim(), lineNumber);
                    Operand op2 = getValue(parts[1].trim(), lineNumber);
                    int opCode;

                    switch (instruction.toUpperCase()) {
                        case "MOV":
                            if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.REGISTER) {
                                opCode = Opcodes.MOV_REG_TO_REG;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.ADDRESS) {
                                opCode = Opcodes.MOV_ADDRESS_TO_REG;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.REGADDRESS) {
                                opCode = Opcodes.MOV_REGADDRESS_TO_REG;
                            } else if (op1.type == Operand.Type.ADDRESS && op2.type == Operand.Type.REGISTER) {
                                opCode = Opcodes.MOV_REG_TO_ADDRESS;
                            } else if (op1.type == Operand.Type.REGADDRESS && op2.type == Operand.Type.REGISTER) {
                                opCode = Opcodes.MOV_REG_TO_REGADDRESS;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.NUMBER) {
                                opCode = Opcodes.MOV_NUMBER_TO_REG;
                            } else if (op1.type == Operand.Type.ADDRESS && op2.type == Operand.Type.NUMBER) {
                                opCode = Opcodes.MOV_NUMBER_TO_ADDRESS;
                            } else if (op1.type == Operand.Type.REGADDRESS && op2.type == Operand.Type.NUMBER) {
                                opCode = Opcodes.MOV_NUMBER_TO_REGADDRESS;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.STACK_OFFSET) {
                                opCode = Opcodes.MOV_STACK_OFFSET_TO_REG;
                            } else if (op1.type == Operand.Type.STACK_OFFSET && op2.type == Operand.Type.REGISTER) {
                                opCode = Opcodes.MOV_REG_TO_STACK_OFFSET;
                            } else {
                                throw new IllegalArgumentException("MOV does not support this operands on line " + lineNumber);
                            }
                            code[addressCounter++] = opCode;
                            addressCounter = addOperand(code, labelReferences, op1, addressCounter, lineNumber);
                            addressCounter = addOperand(code, labelReferences, op2, addressCounter, lineNumber);
                            break;
                        case "ADD":
                            if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.REGISTER) {
                                opCode = Opcodes.ADD_REG_TO_REG;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.REGADDRESS) {
                                opCode = Opcodes.ADD_REGADDRESS_TO_REG;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.ADDRESS) {
                                opCode = Opcodes.ADD_ADDRESS_TO_REG;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.NUMBER) {
                                opCode = Opcodes.ADD_NUMBER_TO_REG;
                            } else {
                                throw new IllegalArgumentException("ADD does not support this operands on line " + lineNumber);
                            }
                            break;
                        case "SUB":
                            if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.REGISTER) {
                                opCode = Opcodes.SUB_REG_FROM_REG;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.REGADDRESS) {
                                opCode = Opcodes.SUB_REGADDRESS_FROM_REG;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.ADDRESS) {
                                opCode = Opcodes.SUB_ADDRESS_FROM_REG;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.NUMBER) {
                                opCode = Opcodes.SUB_NUMBER_FROM_REG;
                            } else {
                                throw new IllegalArgumentException("SUB does not support this operands on line " + lineNumber);
                            }
                            code[addressCounter++] = opCode;
                            addressCounter = addOperand(code, labelReferences, op1, addressCounter, lineNumber);
                            addressCounter = addOperand(code, labelReferences, op2, addressCounter, lineNumber);
                            break;
                        case "CMP":
                            if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.REGISTER) {
                                opCode = Opcodes.CMP_REG_WITH_REG;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.REGADDRESS) {
                                opCode = Opcodes.CMP_REGADDRESS_WITH_REG;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.ADDRESS) {
                                opCode = Opcodes.CMP_ADDRESS_WITH_REG;
                            } else if (op1.type == Operand.Type.REGISTER && op2.type == Operand.Type.NUMBER) {
                                opCode = Opcodes.CMP_NUMBER_WITH_REG;
                            } else {
                                throw new IllegalArgumentException("CMP does not support this operands on line " + lineNumber);
                            }
                            code[addressCounter++] = opCode;
                            addressCounter = addOperand(code, labelReferences, op1, addressCounter, lineNumber);
                            addressCounter = addOperand(code, labelReferences, op2, addressCounter, lineNumber);
                            break;
                        // ... other cases
                    }
                    break;
                case "INC":
                case "DEC":
                case "NEG":
                case "NOT":
                case "POP":
                    op1 = getValue(operandsString, lineNumber);
                    if (op1.type != Operand.Type.REGISTER) {
                        throw new IllegalArgumentException(instruction.toUpperCase() + " does not support this operand on line " + lineNumber);
                    }
                    switch (instruction.toUpperCase()) {
                        case "INC": opCode = Opcodes.INC_REG; break;
                        case "DEC": opCode = Opcodes.DEC_REG; break;
                        case "NEG": opCode = Opcodes.NEG_REG; break;
                        case "NOT": opCode = Opcodes.NOT_REG; break;
                        case "POP": opCode = Opcodes.POP_REG; break;
                        default: throw new IllegalStateException("Unexpected instruction");
                    }
                    code[addressCounter++] = opCode;
                    addressCounter = addOperand(code, labelReferences, op1, addressCounter, lineNumber);
                    break;
                case "JMP":
                case "JZ": case "JE":
                case "JNC": case "JNB": case "JAE":
                case "JNZ": case "JNE":
                case "JA": case "JNBE":
                case "JNA": case "JBE":
                case "JC": case "JB":
                case "JS":
                case "JNS":
                case "CALL":
                    op1 = getValue(operandsString, lineNumber);
                    if (op1.type == Operand.Type.REGISTER) {
                        switch (instruction.toUpperCase()) {
                            case "JMP": opCode = Opcodes.JMP_REGADDRESS; break;
                            case "JZ": case "JE": opCode = Opcodes.JZ_REGADDRESS; break;
                            case "JNC": case "JNB": case "JAE": opCode = Opcodes.JNC_REGADDRESS; break;
                            case "JNZ": case "JNE": opCode = Opcodes.JNZ_REGADDRESS; break;
                            case "JA": case "JNBE": opCode = Opcodes.JA_REGADDRESS; break;
                            case "JNA": case "JBE": opCode = Opcodes.JNA_REGADDRESS; break;
                            case "JC": case "JB": opCode = Opcodes.JC_REGADDRESS; break;
                            case "JS": opCode = Opcodes.JS_REGADDRESS; break;
                            case "JNS": opCode = Opcodes.JNS_REGADDRESS; break;
                            case "CALL": opCode = Opcodes.CALL_REGADDRESS; break;
                            default: throw new IllegalStateException("Unexpected instruction");
                        }
                    } else if (op1.type == Operand.Type.NUMBER || op1.type == Operand.Type.LABEL) {
                        switch (instruction.toUpperCase()) {
                            case "JMP": opCode = Opcodes.JMP_ADDRESS; break;
                            case "JZ": case "JE": opCode = Opcodes.JZ_ADDRESS; break;
                            case "JNC": case "JNB": case "JAE": opCode = Opcodes.JNC_ADDRESS; break;
                            case "JNZ": case "JNE": opCode = Opcodes.JNZ_ADDRESS; break;
                            case "JA": case "JNBE": opCode = Opcodes.JA_ADDRESS; break;
                            case "JNA": case "JBE": opCode = Opcodes.JNA_ADDRESS; break;
                            case "JC": case "JB": opCode = Opcodes.JC_ADDRESS; break;
                            case "JS": opCode = Opcodes.JS_ADDRESS; break;
                            case "JNS": opCode = Opcodes.JNS_ADDRESS; break;
                            case "CALL": opCode = Opcodes.CALL_ADDRESS; break;
                            default: throw new IllegalStateException("Unexpected instruction");
                        }
                    } else {
                        throw new IllegalArgumentException(instruction.toUpperCase() + " does not support this operand on line " + lineNumber);
                    }
                    code[addressCounter++] = opCode;
                    addressCounter = addOperand(code, labelReferences, op1, addressCounter, lineNumber);
                    break;
                case "PUSH":
                    op1 = getValue(operandsString, lineNumber);
                    if (op1.type == Operand.Type.REGISTER) {
                        opCode = Opcodes.PUSH_REG;
                    } else if (op1.type == Operand.Type.REGADDRESS) {
                        opCode = Opcodes.PUSH_REGADDRESS;
                    } else if (op1.type == Operand.Type.ADDRESS || op1.type == Operand.Type.LABEL) {
                        opCode = Opcodes.PUSH_ADDRESS;
                    } else if (op1.type == Operand.Type.NUMBER) {
                        opCode = Opcodes.PUSH_NUMBER;
                    } else {
                        throw new IllegalArgumentException("PUSH does not support this operand on line " + lineNumber);
                    }
                    code[addressCounter++] = opCode;
                    addressCounter = addOperand(code, labelReferences, op1, addressCounter, lineNumber);
                    break;
                case "RET":
                    code[addressCounter++] = Opcodes.RET;
                    break;
                case "PUSHF":
                    code[addressCounter++] = Opcodes.PUSHF;
                    break;
                case "POPF":
                    code[addressCounter++] = Opcodes.POPF;
                    break;
                case "IN":
                    op1 = getValue(operandsString.trim(), lineNumber);
                    if (op1.type == Operand.Type.NUMBER) {
                        opCode = Opcodes.IN_IMM8;
                        code[addressCounter++] = opCode;
                        addressCounter = addOperand(code, labelReferences, op1, addressCounter, lineNumber);
                    } else {
                        throw new IllegalArgumentException("IN only supports `IN imm8` format on line " + lineNumber);
                    }
                    break;
                case "OUT":
                    op1 = getValue(operandsString.trim(), lineNumber);
                    if (op1.type == Operand.Type.NUMBER) {
                        opCode = Opcodes.OUT_IMM8;
                        code[addressCounter++] = opCode;
                        addressCounter = addOperand(code, labelReferences, op1, addressCounter, lineNumber);
                    } else {
                        throw new IllegalArgumentException("OUT only supports `OUT imm8` format on line " + lineNumber);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid instruction: " + instruction + " on line " + lineNumber);
            }
        }

        // Second pass: Replace labels with their actual addresses
        for (Map.Entry<Integer, String> entry : labelReferences.entrySet()) {
            int position = entry.getKey();
            String label = entry.getValue();
            if (labels.containsKey(label)) {
                code[position] = labels.get(label);
            } else {
                int lineNumber = addressToLineMap.get(position) + 1;
                throw new IllegalArgumentException("Undefined label: " + label + " on line " + lineNumber);
            }
        }

        int[] finalCode = new int[addressCounter];
        System.arraycopy(code, 0, finalCode, 0, addressCounter);
        
        return new AssemblyResult(finalCode, addressToLineMap);
    }

    /**
     * Adds an operand's value to the machine code array. If the operand is a label,
     * a placeholder (0) is added, and the label reference is stored for later resolution.
     *
     * @param code            The machine code array being built.
     * @param labelReferences A map to store positions of label references for the second pass.
     * @param operand         The Operand object to add.
     * @param addressCounter  The current address in the code array.
     * @return The new address counter after adding the operand.
     * @throws IllegalArgumentException If the operand value is of an unexpected type.
     */
    private int addOperand(int[] code, Map<Integer, String> labelReferences, Operand operand, int addressCounter, int lineNumber) {
        // If the operand's value is a String, it's a label that needs to be resolved.
        if (operand.value instanceof String) {
            addressToLineMap.put(addressCounter, lineNumber - 1); // Use lineNumber passed in
            labelReferences.put(addressCounter, (String) operand.value);
            code[addressCounter++] = 0; // Placeholder for the label address
        } else if (operand.value instanceof Integer) {
            code[addressCounter++] = (Integer) operand.value;
        } else {
            throw new IllegalArgumentException("Invalid operand value: " + operand.value + " on line " + lineNumber);
        }
        return addressCounter;
    }
}

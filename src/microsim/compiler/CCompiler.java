package microsim.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A very simple C compiler for the MicroSim 8-bit simulator.
 * It uses a static memory allocation model for variables.
 */
public class CCompiler {

    /**
     * Custom exception for compilation errors.
     */
    public static class CompilationException extends RuntimeException {
        public CompilationException(String message) {
            super(message);
        }
    }

    private static final int STATIC_VAR_START_ADDRESS = 0x70;

    private Map<String, Integer> symbolTable = new HashMap<>();
    private Map<String, String> localSymbolTable = new HashMap<>();
    private int nextStaticAddress = STATIC_VAR_START_ADDRESS;
    private int labelCounter = 0;

    private static final Pattern VAR_DECL_PATTERN_1    = Pattern.compile("^\\s*int\\s+([a-zA-Z_][a-zA-Z0-9_]*);");
    private static final Pattern VAR_DECL_PATTERN_2    = Pattern.compile("^\\s*int\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(\\d+);");
    private static final Pattern ASSIGNMENT_PATTERN    = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(\\d+);");
    private static final Pattern ASSIGN_VAR_PATTERN    = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*([a-zA-Z_][a-zA-Z0-9_]*);");
    private static final Pattern POINTER_VAR_PATTERN   = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*&([a-zA-Z_][a-zA-Z0-9_]*);");
    private static final Pattern DEREF_VAR_PATTERN     = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*\\*([a-zA-Z_][a-zA-Z0-9_]*);");

    private static final Pattern SUB_ASSIGN_PATTERN    = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*\\1\\s*-\\s*(\\d+);");
    private static final Pattern ADD_ASSIGN_PATTERN    = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*\\1\\s*\\+\\s*(\\d+);");
    private static final Pattern MUL_ASSIGN_PATTERN    = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*\\1\\s*\\*\\s*(\\d+);");
    private static final Pattern DIV_ASSIGN_PATTERN    = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*\\1\\s*\\/\\s*(\\d+);");
    private static final Pattern ADD_SELF_ASSIGN_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\+=\\s*(\\d+);");
    private static final Pattern SUB_SELF_ASSIGN_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*-=\\s*(\\d+);");
    private static final Pattern MUL_SELF_ASSIGN_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\*=\\s*(\\d+);");
    private static final Pattern DIV_SELF_ASSIGN_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\/=\\s*(\\d+);");
    private static final Pattern VAR_OP_VAR_ASSIGN_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*([+\\-*/])\\s*([a-zA-Z_][a-zA-Z0-9_]*);");
    private static final Pattern INCREMENT_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\+\\+;");
    private static final Pattern DECREMENT_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*--;");

    private static final Pattern MAIN_DECL_PATTERN     = Pattern.compile("^\\s*void\\s+main\\s*\\(\\)\\s*\\{");
    private static final Pattern FUNC_DECL_PATTERN     = Pattern.compile("^\\s*int\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(\\s*int\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\)\\s*\\{");
    private static final Pattern FUNC_CALL_PATTERN_1   = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(\\s*(\\d+|[a-zA-Z_][a-zA-Z0-9_]*)\\s*\\);");
    private static final Pattern FUNC_CALL_PATTERN_2   = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(\\s*(\\d+|[a-zA-Z_][a-zA-Z0-9_]*)\\s*\\);");
    private static final Pattern RETURN_PATTERN        = Pattern.compile("^\\s*return\\s+(\\d+|[a-zA-Z_][a-zA-Z0-9_]*);");

    private static final Pattern CONDITION_PATTERN     = Pattern.compile("\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*(>|<|==|!=|>=|<=)\\s*(\\d+|[a-zA-Z_][a-zA-Z0-9_]*)\\s*");
    private static final Pattern WHILE_PATTERN         = Pattern.compile("^\\s*while\\s*\\((.*)\\)\\s*\\{");
    private static final Pattern IF_PATTERN            = Pattern.compile("^\\s*if\\s*\\((.*)\\)\\s*\\{");

    public String compile(String sourceCode) {
        symbolTable.clear();
        nextStaticAddress = STATIC_VAR_START_ADDRESS;
        labelCounter = 0;

        // Remove C-style comments
        String cleanedSourceCode = sourceCode.replaceAll("/\\*.*?\\*/", ""); // Remove multi-line comments
        cleanedSourceCode = cleanedSourceCode.replaceAll("//.*", ""); // Remove single-line comments

        // First pass for symbol table
        String[] lines = cleanedSourceCode.split("\\r?\\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            Matcher declAssignMatcher = VAR_DECL_PATTERN_2.matcher(trimmedLine);
            Matcher declMatcher = VAR_DECL_PATTERN_1.matcher(trimmedLine);
            Matcher funcDeclMatcher = FUNC_DECL_PATTERN.matcher(trimmedLine);

            if (declAssignMatcher.matches()) {
                String varName = declAssignMatcher.group(1);
                if (!symbolTable.containsKey(varName)) {
                    symbolTable.put(varName, nextStaticAddress++);
                }
            } else if (declMatcher.matches()) {
                String varName = declMatcher.group(1);
                if (!symbolTable.containsKey(varName)) {
                    symbolTable.put(varName, nextStaticAddress++);
                }
            } else if (funcDeclMatcher.matches()) {
                // This is a function declaration, create a mangled "shadow" variable for the parameter
                String funcName = funcDeclMatcher.group(1);
                String paramName = funcDeclMatcher.group(2);
                String mangledParamName = funcName + "_" + paramName;
                if (!symbolTable.containsKey(mangledParamName)) {
                    symbolTable.put(mangledParamName, nextStaticAddress++);
                }
            }
        }

        // Second pass for code generation
        StringBuilder finalCode = new StringBuilder();
        finalCode.append("; C source compiled by MicroSim CCompiler (Static Memory Model)\n\n");

        // Add symbol table as comments
        if (!symbolTable.isEmpty()) {
            finalCode.append("; Variable addresses:\n");
            for (Map.Entry<String, Integer> entry : symbolTable.entrySet()) {
                finalCode.append(String.format(";   %-10s: 0x%02x\n", entry.getKey(), entry.getValue()));
            }
            finalCode.append("\n"); // Add a blank line for separation
        }

        finalCode.append("    CALL main_func\n");
        finalCode.append("    HLT\n");

        List<String> lineList = new ArrayList<>(List.of(lines));
        boolean mainFound = false;

        // Find and compile all functions
        while (!lineList.isEmpty()) {
            String line = lineList.get(0).trim(); // Peek

            if (line.isEmpty()) {
                lineList.remove(0);
                continue;
            }

            Matcher mainDeclMatcher = MAIN_DECL_PATTERN.matcher(line);
            Matcher funcDeclMatcher = FUNC_DECL_PATTERN.matcher(line);

            if (mainDeclMatcher.matches()) {
                mainFound = true;
                lineList.remove(0); // Consume function declaration line
                finalCode.append(compileFunctionBody("main_func", lineList, null));
            } else if (funcDeclMatcher.matches()) {
                lineList.remove(0); // Consume function declaration line
                String functionName = funcDeclMatcher.group(1);
                String paramName = funcDeclMatcher.group(2);
                finalCode.append(compileFunctionBody("func_" + functionName, lineList, paramName));
            } else {
                throw new CompilationException("Top-level code outside of a function is not supported: " + line);
            }
        }

        if (!mainFound) {
            throw new CompilationException("Error: 'void main()' function not found.");
        }

        return finalCode.toString();
    }

    private String compileLine(String line, List<String> remainingLines) {
        if (line.isEmpty()) {
            return "";
        }

        Matcher declAssignMatcher = VAR_DECL_PATTERN_2.matcher(line);
        if (declAssignMatcher.matches()) {
            // It's a declaration with assignment. We generate assignment code.
            String varName = declAssignMatcher.group(1);
            int value = Integer.parseInt(declAssignMatcher.group(2));

            if (!symbolTable.containsKey(varName)) {
                // This should not happen if the first pass was correct
                throw new CompilationException("Compiler error: Symbol not found for: " + varName);
            }
            int address = symbolTable.get(varName);

            String comment = "; int " + varName + " = " + value + ";";
            String line1 = String.format("    MOV AL, 0x%02x\t\t%s\n", value, comment);
            String line2 = String.format("    MOV [0x%02x], AL\n", address);
            return line1 + line2;
        }

        // Simple declaration, generate no code in the second pass.
        if (VAR_DECL_PATTERN_1.matcher(line).matches()) return "";

        Matcher whileMatcher = WHILE_PATTERN.matcher(line);
        if (whileMatcher.matches()) return compileWhileLoop(whileMatcher, remainingLines);

        Matcher ifMatcher = IF_PATTERN.matcher(line);
        if (ifMatcher.matches()) return compileIfElse(ifMatcher, remainingLines);

        Matcher funcCallMatcher = FUNC_CALL_PATTERN_1.matcher(line);
        if (funcCallMatcher.matches()) return compileFunctionCall(funcCallMatcher);

        Matcher returnMatcher = RETURN_PATTERN.matcher(line);
        if (returnMatcher.matches()) return compileReturn(returnMatcher);

        Matcher assignFuncCallMatcher = FUNC_CALL_PATTERN_2.matcher(line);
        if (assignFuncCallMatcher.matches()) return compileAssignFunctionCall(assignFuncCallMatcher);

        Matcher assignVarMatcher = ASSIGN_VAR_PATTERN.matcher(line);
        if (assignVarMatcher.matches()) return compileVarAssignment(assignVarMatcher);

        Matcher assignMatcher = ASSIGNMENT_PATTERN.matcher(line);
        if (assignMatcher.matches()) return compileAssignment(assignMatcher);

        Matcher addSelfAssignMatcher = ADD_SELF_ASSIGN_PATTERN.matcher(line);
        if (addSelfAssignMatcher.matches()) return compileAddSelfAssignment(addSelfAssignMatcher);

        Matcher subSelfAssignMatcher = SUB_SELF_ASSIGN_PATTERN.matcher(line);
        if (subSelfAssignMatcher.matches()) return compileSubSelfAssignment(subSelfAssignMatcher);

        Matcher mulSelfAssignMatcher = MUL_SELF_ASSIGN_PATTERN.matcher(line);
        if (mulSelfAssignMatcher.matches()) return compileMulSelfAssignment(mulSelfAssignMatcher);

        Matcher divSelfAssignMatcher = DIV_SELF_ASSIGN_PATTERN.matcher(line);
        if (divSelfAssignMatcher.matches()) return compileDivSelfAssignment(divSelfAssignMatcher);

        Matcher incrementMatcher = INCREMENT_PATTERN.matcher(line);
        if (incrementMatcher.matches()) return compileIncrement(incrementMatcher);

        Matcher decrementMatcher = DECREMENT_PATTERN.matcher(line);
        if (decrementMatcher.matches()) return compileDecrement(decrementMatcher);

        Matcher varOpVarMatcher = VAR_OP_VAR_ASSIGN_PATTERN.matcher(line);
        if (varOpVarMatcher.matches()) return compileVarOpVarAssignment(varOpVarMatcher);

        Matcher subAssignMatcher = SUB_ASSIGN_PATTERN.matcher(line);

        Matcher addAssignMatcher = ADD_ASSIGN_PATTERN.matcher(line);
        if (addAssignMatcher.matches()) return compileAddAssignment(addAssignMatcher);

        Matcher mulAssignMatcher = MUL_ASSIGN_PATTERN.matcher(line);
        if (mulAssignMatcher.matches()) return compileMulAssignment(mulAssignMatcher);

        Matcher divAssignMatcher = DIV_ASSIGN_PATTERN.matcher(line);
        if (divAssignMatcher.matches()) return compileDivAssignment(divAssignMatcher);

        Matcher addressOfAssignMatcher = POINTER_VAR_PATTERN.matcher(line);
        if (addressOfAssignMatcher.matches()) return compileAddressOfAssignment(addressOfAssignMatcher);

        Matcher derefAssignMatcher = DEREF_VAR_PATTERN.matcher(line);
        if (derefAssignMatcher.matches()) return compileDerefAssignment(derefAssignMatcher);

        if (!line.equals("}")) throw new CompilationException("Unsupported statement: " + line);

        return "";
    }

    private String getAddressMode(String varName) {
        // Check if varName is a local parameter that has a mangled global name
        if (localSymbolTable.containsKey(varName)) {
            varName = localSymbolTable.get(varName); // Use the mangled name
        }

        // Check global symbols
        if (symbolTable.containsKey(varName)) {
            int address = symbolTable.get(varName);
            return String.format("[0x%02x]", address);
        }

        // Variable not found
        throw new CompilationException("Undeclared variable: " + varName);
    }

    private String compileWhileLoop(Matcher whileMatcher, List<String> lines) {
        int currentLabelId = labelCounter++;
        String loopStartLabel = "loop_start_" + currentLabelId;
        String loopEndLabel = "loop_end_" + currentLabelId;

        StringBuilder loopCode = new StringBuilder();
        loopCode.append(String.format("\n%s:\t\t\t; while (%s) {\n", loopStartLabel, whileMatcher.group(1)));

        // Compile condition
        String condition = whileMatcher.group(1);
        String[] andConditions = condition.split("&&");

        for (String cond : andConditions) {
            Matcher conditionMatcher = CONDITION_PATTERN.matcher(cond.trim());
            if (!conditionMatcher.matches()) {
                throw new CompilationException("Unsupported 'while' condition: " + cond);
            }

            String varName = conditionMatcher.group(1);
            String operator = conditionMatcher.group(2);
            String operandRight = conditionMatcher.group(3);
            String jumpInstruction;

            switch (operator) {
                case ">":
                    jumpInstruction = "JNA"; // Jump if Not Above (<=)
                    break;
                case "<":
                    jumpInstruction = "JAE"; // Jump if Above or Equal (>=)
                    break;
                case "==":
                    jumpInstruction = "JNE"; // Jump if Not Equal
                    break;
                case "!=":
                    jumpInstruction = "JE"; // Jump if Equal
                    break;
                case ">=":
                    jumpInstruction = "JB"; // Jump if Below (<)
                    break;
                case "<=":
                    jumpInstruction = "JA"; // Jump if Above (>)
                    break;
                default:
                    throw new CompilationException("Unsupported operator in 'while' condition: " + operator);
            }

            String addressModeLeft = getAddressMode(varName);

            // Generate assembly for condition check
            loopCode.append(String.format("    MOV AL, %s\n", addressModeLeft)); // Load left var into AL

            // Check if right operand is a number or a variable
            try {
                int valueRight = Integer.parseInt(operandRight);
                // It's a number literal
                loopCode.append(String.format("    CMP AL, 0x%02x\n", valueRight));
            } catch (NumberFormatException e) {
                // It's a variable name
                String addressModeRight = getAddressMode(operandRight);
                loopCode.append(String.format("    MOV BL, %s\n", addressModeRight));
                loopCode.append("    CMP AL, BL\n");
            }

            loopCode.append(String.format("    %s  %s\n", jumpInstruction, loopEndLabel)); // Jump if condition is false
        }

        // Compile loop body
        int braceCount = 1; // Count for opening '{'
        while (!lines.isEmpty() && braceCount > 0) {
            String line = lines.remove(0).trim(); // Consume line
            if (line.equals("{")) {
                braceCount++;
            } else if (line.equals("}")) {
                braceCount--;
                if (braceCount == 0) { // Reached the matching closing brace
                    break;
                }
            }

            if (braceCount > 0 && !line.equals("{")) { // If not the opening brace itself, compile the line
                loopCode.append(compileLine(line, lines)); // Recursively compile body line, passing remaining lines
            }
        }
        if (braceCount > 0) {
            throw new CompilationException("Missing closing '}' for while loop.");
        }

        loopCode.append(String.format("    JMP %s\n", loopStartLabel)); // Jump back to start of loop
        loopCode.append(String.format("\n%s:\t\t\t; }\n", loopEndLabel)); // Closing brace comment

        return loopCode.toString();
    }

    private String compileFunctionBody(String functionName, List<String> lines, String paramName) {
        localSymbolTable.clear(); // Enter function scope

        StringBuilder functionCode = new StringBuilder();
        functionCode.append(String.format("\n%s:\n", functionName));

        // Function Prologue: Pop argument from stack into its "shadow" variable location
        if (paramName != null && !paramName.isEmpty()) {
            String mangledParamName = functionName.replace("func_", "") + "_" + paramName;
            localSymbolTable.put(paramName, mangledParamName);

            if (!symbolTable.containsKey(mangledParamName)) {
                throw new CompilationException("Compiler error: Mangled parameter not found in global table: " + mangledParamName);
            }
            int address = symbolTable.get(mangledParamName);
            functionCode.append(String.format("    MOV AL, [SP+2]\t\t; Load param '%s' from stack\n", paramName));
            functionCode.append(String.format("    MOV [0x%02x], AL\t; Store in shadow variable '%s'\n", address, mangledParamName));
        }

        int braceCount = 1; // Expecting '{' to be already consumed for the function start
        while (!lines.isEmpty() && braceCount > 0) {
            String line = lines.remove(0).trim(); // Consume line
            if (line.equals("{")) {
                braceCount++;
            } else if (line.equals("}")) {
                braceCount--;
                if (braceCount == 0) break; // Reached the matching closing brace
            }

            if (braceCount > 0 && !line.equals("{")) { // If not the opening brace itself, compile the line
                functionCode.append(compileLine(line, lines)); // Recursively compile body line, passing remaining lines
            }
        }
        if (braceCount > 0) {
            throw new CompilationException("Missing closing '}' for function " + functionName + ".");
        }

        functionCode.append("    RET\n"); // Return from function

        localSymbolTable.clear(); // Leave function scope
        return functionCode.toString();
    }

    private String compileIfElse(Matcher ifMatcher, List<String> lines) {
        int currentLabelId = labelCounter++;
        String elseLabel = "else_" + currentLabelId;
        String endIfLabel = "end_if_" + currentLabelId;

        StringBuilder ifCode = new StringBuilder();
        ifCode.append(String.format("\n; if (%s) {\n", ifMatcher.group(1)));

        // --- Condition Compilation ---
        String condition = ifMatcher.group(1);
        String[] andConditions = condition.split("&&");

        for (String cond : andConditions) {
            Matcher conditionMatcher = CONDITION_PATTERN.matcher(cond.trim());
            if (!conditionMatcher.matches()) {
                throw new CompilationException("Unsupported 'if' condition: " + cond);
            }

            String varNameLeft = conditionMatcher.group(1);
            String operator = conditionMatcher.group(2);
            String operandRight = conditionMatcher.group(3);
            String jumpInstruction;

            switch (operator) {
                case ">": jumpInstruction = "JNA"; break;
                case "<": jumpInstruction = "JAE"; break;
                case "==": jumpInstruction = "JNE"; break;
                case "!=": jumpInstruction = "JE"; break;
                case ">=": jumpInstruction = "JB"; break;
                case "<=": jumpInstruction = "JA"; break;
                default: throw new CompilationException("Unsupported operator in 'if' condition: " + operator);
            }

            String addressModeLeft = getAddressMode(varNameLeft);
            ifCode.append(String.format("    MOV AL, %s\n", addressModeLeft));

            try {
                int valueRight = Integer.parseInt(operandRight);
                ifCode.append(String.format("    CMP AL, 0x%02x\n", valueRight));
            } catch (NumberFormatException e) {
                String addressModeRight = getAddressMode(operandRight);
                ifCode.append(String.format("    MOV BL, %s\n", addressModeRight));
                ifCode.append("    CMP AL, BL\n");
            }

            // Jump to 'else' or 'end_if' if condition is false
            ifCode.append(String.format("    %s  %s\n", jumpInstruction, elseLabel));
        }

        // --- IF Body Compilation ---
        int braceCount = 1;
        while (!lines.isEmpty() && braceCount > 0) {
            String line = lines.remove(0).trim();
            if (line.equals("{")) { braceCount++; }
            else if (line.equals("}")) {
                braceCount--;
                if (braceCount == 0) break;
            }
            if (braceCount > 0 && !line.equals("{")) {
                ifCode.append(compileLine(line, lines));
            }
        }
        if (braceCount > 0) {
            throw new CompilationException("Missing closing '}' for if block.");
        }

        // --- ELSE Block Check and Compilation ---
        if (!lines.isEmpty() && lines.get(0).trim().equals("else")) {
            lines.remove(0); // consume 'else'
            if (lines.isEmpty() || !lines.get(0).trim().equals("{")) {
                throw new CompilationException("Missing '{' after 'else'.");
            }
            lines.remove(0); // consume '{'

            ifCode.append(String.format("    JMP %s\n", endIfLabel)); // Jump over else block from if-true path
            ifCode.append(String.format("%s:\t\t\t; } else {\n", elseLabel));

            // Else-Body Compilation
            braceCount = 1;
            while (!lines.isEmpty() && braceCount > 0) {
                String line = lines.remove(0).trim();
                if (line.equals("{")) { braceCount++; }
                else if (line.equals("}")) {
                    braceCount--;
                    if (braceCount == 0) break;
                }
                if (braceCount > 0 && !line.equals("{")) {
                    ifCode.append(compileLine(line, lines));
                }
            }
            if (braceCount > 0) {
                throw new CompilationException("Missing closing '}' for else block.");
            }
            ifCode.append(String.format("%s:\t\t\t; } end if-else\n", endIfLabel));

        } else {
            // No else block, so elseLabel is the end
            ifCode.append(String.format("%s:\t\t\t; } end if\n", elseLabel));
        }

        return ifCode.toString();
    }

    private String compileVarAssignment(Matcher assignVarMatcher) {
        String varNameLeft = assignVarMatcher.group(1);
        String varNameRight = assignVarMatcher.group(2);
        String addressModeLeft = getAddressMode(varNameLeft);
        String addressModeRight = getAddressMode(varNameRight);
        String comment = String.format("; %s = %s;", varNameLeft, varNameRight);
        String line1 = String.format("    MOV AL, %s\t\t%s\n", addressModeRight, comment);
        String line2 = String.format("    MOV %s, AL\n", addressModeLeft);
        return line1 + line2;
    }

    private String compileFunctionCall(Matcher funcCallMatcher) {
        String functionName = funcCallMatcher.group(1);
        String argument = funcCallMatcher.group(2);
        StringBuilder sb = new StringBuilder();
        String comment = String.format("; %s(%s);", functionName, argument);

        // PUSH argument onto the stack
        try {
            int literalValue = Integer.parseInt(argument);
            // It's a number literal
            sb.append(String.format("    PUSH 0x%02x\t\t\t%s\n", literalValue, comment));

        } catch (NumberFormatException e) {
            // It's a variable name
            String addressMode = getAddressMode(argument);
            sb.append(String.format("    PUSH %s\t\t%s\n", addressMode, comment));
        }

        // CALL the function
        sb.append(String.format("    CALL func_%s\n", functionName));

        // Caller-cleanup for Argument
        sb.append("    POP BL\t\t\t; Caller cleans up arg from stack into BL\n");

        return sb.toString();
    }

    private String compileReturn(Matcher returnMatcher) {
        String returnValue = returnMatcher.group(1);
        String comment = String.format("; return %s;", returnValue);

        // Move return value into AL register
        try {
            int literalValue = Integer.parseInt(returnValue);
            // It's a number literal
            return String.format("    MOV AL, 0x%02x\t\t%s\n", literalValue, comment);
        } catch (NumberFormatException e) {
            // It's a variable name
            String addressMode = getAddressMode(returnValue);
            return String.format("    MOV AL, %s\t\t%s\n", addressMode, comment);
        }
    }

    private String compileAssignFunctionCall(Matcher matcher) {
        String destVar = matcher.group(1);
        String functionName = matcher.group(2);
        String argument = matcher.group(3);
        String destAddressMode = getAddressMode(destVar);
        StringBuilder sb = new StringBuilder();
        String comment = String.format("; %s = %s(%s);", destVar, functionName, argument);

        sb.append("\n"); // Blank line for clarity

        try {
            int literalValue = Integer.parseInt(argument);
            sb.append(String.format("    PUSH 0x%02x\t\t\t%s\n", literalValue, comment));
        } catch (NumberFormatException e) {
            String addressMode = getAddressMode(argument);
            sb.append(String.format("    PUSH %s\t\t%s\n", addressMode, comment));
        }
        sb.append(String.format("    CALL func_%s\n", functionName));

        // Return value is in AL. Argument is on the stack.
        // 1. Store AL in destination variable.
        sb.append(String.format("    MOV %s, AL\t\t; Store return value in %s\n", destAddressMode, destVar));
        // 2. Clean up argument from stack by popping it into a scratch register.
        sb.append("    POP BL\t\t\t; Caller cleans up arg from stack into BL\n");

        return sb.toString();
    }

    private String compileDerefAssignment(Matcher derefAssignMatcher) {
        String varNameLeft = derefAssignMatcher.group(1);
        String varNameRight = derefAssignMatcher.group(2); // This is the pointer variable

        // Get the address where the pointer variable (varNameRight) is stored
        Integer ptrVarAddress = symbolTable.get(varNameRight);
        if (ptrVarAddress == null) {
            throw new CompilationException("Undeclared pointer variable: " + varNameRight);
        }

        // Get the address where the left-hand side variable (varNameLeft) is stored
        String addressModeLeft = getAddressMode(varNameLeft); // This gives [0xXX]

        String comment = String.format("; %s = *%s;", varNameLeft, varNameRight);
        StringBuilder sb = new StringBuilder();

        // 1. Load the value of the pointer variable (which is an address) into AL
        sb.append(String.format("    MOV AL, [0x%02x]\t\t%s\n", ptrVarAddress, comment));
        // 2. Use AL's content as an address to load the actual value into BL
        //    (assuming BL is a scratch register and can be used for indirect addressing)
        sb.append("    MOV BL, [AL]\n"); // Load value from the address pointed to by AL
        // 3. Store BL's content into the left-hand side variable
        sb.append(String.format("    MOV %s, BL\n", addressModeLeft));

        return sb.toString();
    }

    private String compileAddressOfAssignment(Matcher addressOfAssignMatcher) {
        String varNameLeft = addressOfAssignMatcher.group(1);
        String varNameRight = addressOfAssignMatcher.group(2);

        // Get the address of the right-hand side variable
        Integer addressOfRightVar = symbolTable.get(varNameRight);
        if (addressOfRightVar == null) {
            throw new CompilationException("Undeclared variable: " + varNameRight);
        }

        // Get the address mode for the left-hand side variable
        String addressModeLeft = getAddressMode(varNameLeft); // This gives [0xXX]

        String comment = String.format("; %s = &%s; (0x%02x)", varNameLeft, varNameRight, addressOfRightVar);
        String line1 = String.format("    MOV AL, 0x%02x\t\t%s\n", addressOfRightVar, comment);
        String line2 = String.format("    MOV %s, AL\n", addressModeLeft);
        return line1 + line2;
    }

    private String compileAssignment(Matcher assignMatcher) {
        String varName = assignMatcher.group(1);
        int value = Integer.parseInt(assignMatcher.group(2));
        String addressMode = getAddressMode(varName);
        String comment = "; " + varName + " = " + value + ";";
        String line1 = String.format("    MOV AL, 0x%02x\t\t%s\n", value, comment);
        String line2 = String.format("    MOV %s, AL\n", addressMode);
        return line1 + line2;
    }

    private String compileSubAssignment(Matcher subAssignMatcher) {
        String varName = subAssignMatcher.group(1);
        int value = Integer.parseInt(subAssignMatcher.group(2));
        String addressMode = getAddressMode(varName);
        String comment = "; " + varName + " = " + varName + " - " + value + ";";
        String line1 = String.format("    MOV AL, %s\t\t%s\n", addressMode, comment);
        String line2 = String.format("    SUB AL, 0x%02x\n", value);
        String line3 = String.format("    MOV %s, AL\n", addressMode);
        return line1 + line2 + line3;
    }

    private String compileAddAssignment(Matcher matcher) {
        String varName = matcher.group(1);
        int value = Integer.parseInt(matcher.group(2));
        String addressMode = getAddressMode(varName);
        String comment = "; " + varName + " = " + varName + " + " + value + ";";
        String line1 = String.format("    MOV AL, %s\t\t%s\n", addressMode, comment);
        String line2 = String.format("    ADD AL, 0x%02x\n", value);
        String line3 = String.format("    MOV %s, AL\n", addressMode);
        return line1 + line2 + line3;
    }

    private String compileMulAssignment(Matcher matcher) {
        String varName = matcher.group(1);
        int value = Integer.parseInt(matcher.group(2));
        String addressMode = getAddressMode(varName);
        String comment = "; " + varName + " = " + varName + " * " + value + ";";
        String line1 = String.format("    MOV AL, %s\t\t%s\n", addressMode, comment);
        String line2 = String.format("    MOV BL, 0x%02x\n", value);
        String line3 = "    MUL BL\n";
        String line4 = String.format("    MOV %s, AL\n", addressMode);
        return line1 + line2 + line3 + line4;
    }

    private String compileDivAssignment(Matcher matcher) {
        String varName = matcher.group(1);
        int value = Integer.parseInt(matcher.group(2));
        String addressMode = getAddressMode(varName);
        String comment = "; " + varName + " = " + varName + " / " + value + ";";
        String line1 = String.format("    MOV AL, %s\t\t%s\n", addressMode, comment);
        String line2 = String.format("    MOV BL, 0x%02x\n", value);
        String line3 = "    DIV BL\n";
        String line4 = String.format("    MOV %s, AL\n", addressMode);
        return line1 + line2 + line3 + line4;
    }

    private String compileAddSelfAssignment(Matcher matcher) {
        String varName = matcher.group(1);
        int value = Integer.parseInt(matcher.group(2));
        String addressMode = getAddressMode(varName);
        String comment = "; " + varName + " += " + value + ";";
        String line1 = String.format("    MOV AL, %s\t\t%s\n", addressMode, comment);
        String line2 = String.format("    ADD AL, 0x%02x\n", value);
        String line3 = String.format("    MOV %s, AL\n", addressMode);
        return line1 + line2 + line3;
    }

    private String compileSubSelfAssignment(Matcher matcher) {
        String varName = matcher.group(1);
        int value = Integer.parseInt(matcher.group(2));
        String addressMode = getAddressMode(varName);
        String comment = "; " + varName + " -= " + value + ";";
        String line1 = String.format("    MOV AL, %s\t\t%s\n", addressMode, comment);
        String line2 = String.format("    SUB AL, 0x%02x\n", value);
        String line3 = String.format("    MOV %s, AL\n", addressMode);
        return line1 + line2 + line3;
    }

    private String compileMulSelfAssignment(Matcher matcher) {
        String varName = matcher.group(1);
        int value = Integer.parseInt(matcher.group(2));
        String addressMode = getAddressMode(varName);
        String comment = "; " + varName + " *= " + value + ";";
        String line1 = String.format("    MOV AL, %s\t\t%s\n", addressMode, comment);
        String line2 = String.format("    MOV BL, 0x%02x\n", value);
        String line3 = "    MUL BL\n";
        String line4 = String.format("    MOV %s, AL\n", addressMode);
        return line1 + line2 + line3 + line4;
    }

    private String compileDivSelfAssignment(Matcher matcher) {
        String varName = matcher.group(1);
        int value = Integer.parseInt(matcher.group(2));
        String addressMode = getAddressMode(varName);
        String comment = "; " + varName + " /= " + value + ";";
        String line1 = String.format("    MOV AL, %s\t\t%s\n", addressMode, comment);
        String line2 = String.format("    MOV BL, 0x%02x\n", value);
        String line3 = "    DIV BL\n";
        String line4 = String.format("    MOV %s, AL\n", addressMode);
        return line1 + line2 + line3 + line4;
    }

    private String compileVarOpVarAssignment(Matcher matcher) {
        String destVar = matcher.group(1);
        String var1 = matcher.group(2);
        String op = matcher.group(3);
        String var2 = matcher.group(4);

        String destAddr = getAddressMode(destVar);
        String var1Addr = getAddressMode(var1);
        String var2Addr = getAddressMode(var2);

        String comment = String.format("; %s = %s %s %s;", destVar, var1, op, var2);
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("    MOV AL, %s\t\t%s\n", var1Addr, comment));
        sb.append(String.format("    MOV BL, %s\n", var2Addr));

        switch (op) {
            case "+":
                sb.append("    ADD AL, BL\n");
                break;
            case "-":
                sb.append("    SUB AL, BL\n");
                break;
            case "*":
                sb.append("    MUL BL\n");
                break;
            case "/":
                sb.append("    DIV BL\n");
                break;
        }

        sb.append(String.format("    MOV %s, AL\n", destAddr));

        return sb.toString();
    }

    private String compileIncrement(Matcher matcher) {
        String varName = matcher.group(1);
        String addressMode = getAddressMode(varName);
        String comment = "; " + varName + "++;";
        String line1 = String.format("    MOV AL, %s\t\t%s\n", addressMode, comment);
        String line2 = "    INC AL\n"; // INC increments AL by 1
        String line3 = String.format("    MOV %s, AL\n", addressMode);
        return line1 + line2 + line3;
    }

    private String compileDecrement(Matcher matcher) {
        String varName = matcher.group(1);
        String addressMode = getAddressMode(varName);
        String comment = "; " + varName + "--;";
        String line1 = String.format("    MOV AL, %s\t\t%s\n", addressMode, comment);
        String line2 = "    DEC AL\n"; // DEC decrements AL by 1
        String line3 = String.format("    MOV %s, AL\n", addressMode);
        return line1 + line2 + line3;
    }
}

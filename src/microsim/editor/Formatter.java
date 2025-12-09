/**
 * Project : MicroSim - 8 bits microprocessor simulator for educational purposes.
 *
 * @author Jérôme Lehuen
 * @version 1.0
 * @since 2025-12-09
 *
 * License: GNU General Public License v3.0
 */

package microsim.editor;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * The Formatter class provides static methods to automatically reformat
 * assembly code. It standardizes indentation, capitalizes labels and mnemonics,
 * aligns comments, and ensures consistent spacing, improving code readability.
 */
public class Formatter {

    /** A regex pattern to find and match register names (case-insensitive). */
    private static final Pattern registerPattern = Pattern.compile(
        "\\b(AL|BL|CL|DL|SP|IP|SR)\\b", Pattern.CASE_INSENSITIVE);

    /** A set of all jump and call mnemonics to identify them during formatting. */
    private static final java.util.Set<String> JUMP_MNEMONICS = java.util.Set.of(
        "JZ", "JNZ", "JC", "JNC", "JO", "JNO", "JS", "JNS", "JE", "JNE",
        "JB", "JNB", "JBE", "JA", "JAE", "JG", "JGE", "JMP", "CALL");

    /**
     * Checks if a given mnemonic is a jump or call instruction.
     * @param mnemonic The mnemonic to check.
     * @return true if it is a jump or call, false otherwise.
     */
    private static boolean isJumpOrCall(String mnemonic) {
        return JUMP_MNEMONICS.contains(mnemonic.toUpperCase());
    }

    /**
     * Capitalizes the first letter of a string and converts the rest to lowercase.
     * @param s The string to capitalize.
     * @return The capitalized string.
     */
    private static String capitalise(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /**
     * Calculates the visual length of a string, accounting for tab characters.
     * @param s The string to measure.
     * @param tabSize The number of spaces each tab represents.
     * @return The visual length of the string.
     */
    private static int calculateVisualLength(String s, int tabSize) {
        int length = 0;
        for (char c : s.toCharArray()) {
            if (c == '\t') {
                int remaining = length % tabSize;
                length += (tabSize - remaining);
            } else {
                length++;
            }
        }
        return length;
    }

    /**
     * Reformats the given assembly code text.
     * The process involves three passes:
     * 1. Collect all labels and standardize their capitalization.
     * 2. Format the code part of each line, separate out comments, and calculate the
     *    maximum code line length for comment alignment.
     * 3. Reconstruct the text, aligning comments to a consistent column.
     *
     * @param text The raw assembly code to reformat.
     * @param tabSize The width of a tab character in spaces.
     * @return The reformatted code as a single string.
     */
    public static String reformat(String text, int tabSize) {
        if (text == null) return "";

        String[] lines = text.split("\\r?\\n");
        Map<String, String> labelMap = new HashMap<>();

        // First pass: collect all labels
        for (String line : lines) {
            String codePart = line;
            int commentIndex = line.indexOf(';');
            if (commentIndex != -1) {
                codePart = line.substring(0, commentIndex);
            }
            int labelIndex = codePart.indexOf(':');
            if (labelIndex != -1) {
                String originalLabel = codePart.substring(0, labelIndex).trim();
                if (!originalLabel.isEmpty()) {
                    labelMap.put(originalLabel.toLowerCase(), capitalise(originalLabel));
                }
            }
        }

        String[] formattedCodeLines = new String[lines.length];
        String[] comments = new String[lines.length];
        int maxCodeLength = 0;

        // Second pass: format code, separate comments, and find max code length
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            int commentIndex = line.indexOf(';');
            if (commentIndex != -1) {
                comments[i] = line.substring(commentIndex).trim();
                line = line.substring(0, commentIndex);
            } else {
                comments[i] = null;
            }

            if (trimmed.isEmpty()) {
                formattedCodeLines[i] = "";
                continue;
            }
            if (trimmed.startsWith(";")) {
                formattedCodeLines[i] = trimmed;
                comments[i] = null; // It's a full-line comment
                continue;
            }
            if (line.trim().isEmpty()) {
                formattedCodeLines[i] = ""; // Line with only a comment
                continue;
            }

            String codePart = line.trim();
            String label = "";
            String instruction = "";

            int labelIndex = codePart.indexOf(':');
            if (labelIndex != -1) {
                String originalLabel = codePart.substring(0, labelIndex).trim();
                label = labelMap.get(originalLabel.toLowerCase());
                instruction = codePart.substring(labelIndex + 1).trim();

                if (label != null && !label.isEmpty()) {
                    formattedCodeLines[i] = label + ":";
                } else {
                    formattedCodeLines[i] = "";
                }

                if (instruction.isEmpty()) {
                    continue;
                }
            } else {
                instruction = codePart.trim();
                formattedCodeLines[i] = "";
            }

            String mnemonic = "";
            String operands = "";

            String[] parts = instruction.split("\\s+", 2);
            mnemonic = parts[0].toLowerCase();
            if (parts.length > 1) {
                operands = parts[1].replaceAll("\\s*,\\s*", ", ");
                if (isJumpOrCall(mnemonic)) {
                    String originalOperandLabel = operands.trim();
                    String camelCaseLabel = labelMap.get(originalOperandLabel.toLowerCase());
                    if (camelCaseLabel != null) {
                        operands = camelCaseLabel;
                    }
                } else {
                    Matcher matcher = registerPattern.matcher(operands);
                    StringBuffer sb = new StringBuffer();
                    while (matcher.find()) {
                        matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
                    }
                    matcher.appendTail(sb);
                    operands = sb.toString();
                }
            }

            StringBuilder formattedLine = new StringBuilder();
            if (!formattedCodeLines[i].isEmpty()) {
                formattedLine.append(formattedCodeLines[i]).append("\n");
            }
            formattedLine.append("\t"); // Indent instruction

            if (!mnemonic.isEmpty()) formattedLine.append(mnemonic);
            if (!operands.isEmpty()) formattedLine.append("\t").append(operands);

            String finalFormattedCode = formattedLine.toString().replaceAll("\\s+$", "");
            formattedCodeLines[i] = finalFormattedCode;

            if (comments[i] != null && !instruction.isEmpty()) {
                String codeLineForLength = finalFormattedCode.contains("\n") ? 
                    finalFormattedCode.substring(finalFormattedCode.lastIndexOf("\n") + 1) :
                    finalFormattedCode;
                int currentLength = calculateVisualLength(codeLineForLength, tabSize);
                if (currentLength > maxCodeLength) {
                    maxCodeLength = currentLength;
                }
            }
        }

        // Third pass: build the final text with aligned comments
        StringBuilder newText = new StringBuilder();
        int commentColumn = maxCodeLength + 4; // 4 spaces padding

        for (int i = 0; i < lines.length; i++) {
            String code = formattedCodeLines[i];
            String comment = comments[i];

            if (code == null) continue;

            newText.append(code);

            if (comment != null) {
                if (!code.trim().isEmpty() && !code.endsWith(":")) {
                    String codeLineForLength = code.contains("\n") ? 
                        code.substring(code.lastIndexOf("\n") + 1) :
                        code;
                    int currentLength = calculateVisualLength(codeLineForLength, tabSize);

                    int spacesNeeded = commentColumn - currentLength;
                    if (spacesNeeded < 1) spacesNeeded = 4;
                    newText.append(" ".repeat(spacesNeeded));
                    newText.append(comment);
                } else { // Comment on empty line or label line
                    if (!code.endsWith("\n") && !code.isEmpty()) newText.append("\n");
                    newText.append("\t\t\t").append(comment);
                }
            }
            if (i < lines.length - 1) {
                newText.append("\n");
            }
        }

        return newText.toString().replaceAll("\\n{3,}", "\n\n");
    }
}

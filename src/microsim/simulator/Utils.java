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

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import microsim.ToolBar;

/**
 * The Utils class provides a collection of static utility methods used
 * throughout the simulator. This includes methods for loading images,
 * pausing execution, and converting numbers into various string formats
 * (binary, hexadecimal, signed decimal).
 */
public class Utils {

    /**
     * Retrieves the file extension from a given File object.
     * @param file The file from which to extract the extension.
     * @return The file extension as a lowercase string, or an empty string if none exists.
     */
    public static String getFileExtension(File file) {
        String name = file.getName();
        int lastIndex = name.lastIndexOf('.');
        if (lastIndex > 0 && lastIndex < name.length() - 1) {
            return name.substring(lastIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * Loads an ImageIcon from the specified path within the JAR.
     * @param path The path to the image file.
     * @return An ImageIcon object, or null if loading fails.
     */
    public static ImageIcon loadImageIcon(String path) {
        java.net.URL imgURL = ToolBar.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
    
    /**
     * Loads an image from the specified path within the JAR.
     * @param path The path to the image file.
     * @return A BufferedImage object, or null if loading fails.
     */
    public static BufferedImage loadBufferedImage(String path) {
        try (InputStream is = Utils.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("Error loading image: " + path + " not found.");
                return null;
            }
            return ImageIO.read(is);
        } catch (IOException e) {
            System.err.format("Error loading %s: %s%n", path, e.getMessage());
            return null;
        }
    }

    /**
     * Loads a font from the specified path within the JAR.
     * @param path The path to the font file.
     * @return A Font object, or null if loading fails.
     */
    public static Font loadFont(String path, float size) {
        try (InputStream is = Utils.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("Error loading font: " + path + " not found.");
                return null;
            }
            return Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
        } catch (IOException | FontFormatException e) {
            System.err.format("Error loading font %s: %s%n", path, e.getMessage());
            return null;
        }
    }

	/**
     * Pauses the current thread for a specified number of milliseconds.
     * @param ms The duration to sleep in milliseconds.
     */
	public static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
     * Converts an integer value to an 8-bit binary string, padded with leading zeros.
     * @param value The integer to convert.
     * @return The 8-bit binary string representation.
     */
	public static String toBinaryString(int value) {
        return String.format("%8s", Integer.toBinaryString(value & 0xFF)).replace(' ', '0');
    }

    /**
     * Converts an integer value to a 2-digit hexadecimal string.
     * @param value The integer to convert.
     * @return The 2-digit hexadecimal string.
     */
    public static String toHexString(int value) {
        return String.format("%02X", value & 0xFF);
    }

    /**
     * Converts an integer value to a 2-digit hexadecimal string, prefixed with "0x".
     * @param value The integer to convert.
     * @return The hexadecimal string with a "0x" prefix.
     */
    public static String toHexString0x(int value) {
        return String.format("0x%02X", value & 0xFF);
    }

    /**
     * Converts an 8-bit integer value to its signed decimal string representation.
     * It interprets the 8-bit value as a two's complement number.
     * @param value The 8-bit integer to convert.
     * @return The signed decimal string.
     */
    public static String toSignedDecimalString(int value) {
        int signedValue = (value & 0xFF);
        if (signedValue > 127) {
            signedValue = signedValue - 256;
        }
        return Integer.toString(signedValue);
    }
}
package microsim.simulator;

import microsim.devices.BannerFrame;
import microsim.devices.BuzzerFrame;
import microsim.devices.HeaterFrame;
import microsim.devices.KeyboardFrame;
import microsim.devices.LedBarFrame;
import microsim.devices.LightsFrame;
import microsim.devices.MatrixFrame;

/**
 * Swing-backed implementation of PeripheralBus using the simulator's GUI frames.
 */
public class SwingPeripheralBus implements PeripheralBus {

    private final HeaterFrame heater = HeaterFrame.INSTANCE;
    private final KeyboardFrame keyboard = KeyboardFrame.INSTANCE;
    private final LightsFrame trafficLights = LightsFrame.INSTANCE;
    private final LedBarFrame ledBar = LedBarFrame.INSTANCE;
    private final MatrixFrame matrix = MatrixFrame.INSTANCE;
    private final BuzzerFrame buzzer = BuzzerFrame.INSTANCE;
    private final BannerFrame banner = BannerFrame.INSTANCE;

    @Override
    public int in(int port) {
        if (port == CPU.HEATER_PORT) {
            if (!heater.isVisible()) heater.setVisible(true);
            return heater.getStatus() & 0xFF;
        } else if (port == CPU.KEYBOARD_PORT) {
            return keyboard.getAsciiCode() & 0xFF;
        } else if (port == CPU.LEDS_PORT) {
            if (!ledBar.isVisible()) ledBar.setVisible(true);
            return ledBar.getLedState() & 0xFF;
        } else if (port == CPU.MATRIX_ROW_PORT) {
            if (!matrix.isVisible()) matrix.setVisible(true);
            return matrix.getSelectedRow();
        } else if (port == CPU.MATRIX_DATA_PORT) {
            if (!matrix.isVisible()) matrix.setVisible(true);
            return matrix.readRowData() & 0xFF;
        } else if (port == CPU.BUZZER_NOTE_PORT) {
            if (!buzzer.isVisible()) buzzer.setVisible(true);
            return buzzer.getNote();
        } else if (port == CPU.BUZZER_DURATION_PORT) {
            if (!buzzer.isVisible()) buzzer.setVisible(true);
            return buzzer.isPlaying();
        } else if (port == CPU.BANNER_DATA_PORT) {
            if (!banner.isVisible()) banner.setVisible(true);
            return banner.getData() & 0xFF;
        } else if (port == CPU.BANNER_CMD_PORT) {
            if (!banner.isVisible()) banner.setVisible(true);
            return banner.getLastCommand() & 0xFF;
        }
        return 0;
    }

    @Override
    public void out(int port, int value) {
        if (port == CPU.LIGHTS_PORT) {
            if (!trafficLights.isVisible()) trafficLights.setVisible(true);
            trafficLights.updateLights((byte) value);
        } else if (port == CPU.HEATER_PORT) {
            if (!heater.isVisible()) heater.setVisible(true);
            heater.updateBurner((byte) value);
        } else if (port == CPU.LEDS_PORT) {
            if (!ledBar.isVisible()) ledBar.setVisible(true);
            ledBar.updateLeds((byte) value);
        } else if (port == CPU.MATRIX_ROW_PORT) {
            if (!matrix.isVisible()) matrix.setVisible(true);
            matrix.selectRow(value);
        } else if (port == CPU.MATRIX_DATA_PORT) {
            if (!matrix.isVisible()) matrix.setVisible(true);
            matrix.writeRowData((byte) value);
        } else if (port == CPU.BUZZER_NOTE_PORT) {
            if (!buzzer.isVisible()) buzzer.setVisible(true);
            buzzer.setNote(value & 0x7F);
        } else if (port == CPU.BUZZER_DURATION_PORT) {
            if (!buzzer.isVisible()) buzzer.setVisible(true);
            buzzer.setDuration(value & 0xFF);
        } else if (port == CPU.BANNER_DATA_PORT) {
            if (!banner.isVisible()) banner.setVisible(true);
            banner.setData(value & 0xFF);
        } else if (port == CPU.BANNER_CMD_PORT) {
            if (!banner.isVisible()) banner.setVisible(true);
            banner.executeCommand(value & 0xFF);
        }
    }
}

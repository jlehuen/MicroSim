package microsim.headless;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import microsim.simulator.CPU;
import microsim.simulator.PeripheralBus;

/**
 * Headless implementation of PeripheralBus for automated grading.
 * Provides scripted keyboard input, configurable heater status,
 * and cycle-stamped logging of all OUT instructions.
 */
public class HeadlessPeripheralBus implements PeripheralBus {

    public static class OutputEvent {
        public final int cycle;
        public final int port;
        public final int value;

        public OutputEvent(int cycle, int port, int value) {
            this.cycle = cycle;
            this.port = port;
            this.value = value & 0xFF;
        }
    }

    private final Deque<Integer> keyboardQueue = new ArrayDeque<>();
    private final List<OutputEvent> outputs = new ArrayList<>();
    private int heaterStatus = 0;
    private int ledsStatus = 0;
    private int matrixSelectedRow = 0;
    private final int[] matrixRows = new int[8];
    private int buzzerNote = 0;
    private int buzzerDuration = 0;
    private int buzzerEndCycle = 0;
    private int bannerData = 0;
    private int bannerLastCommand = 0;
    private final int[] bannerColumns = new int[64];
    private int currentCycle = 0;
    private boolean keyboardExhausted = false;

    public void setCurrentCycle(int cycle) {
        this.currentCycle = cycle;
    }

    public void enqueueKey(int asciiCode) {
        keyboardQueue.addLast(asciiCode & 0xFF);
    }

    public void setHeaterStatus(int statusByte) {
        this.heaterStatus = statusByte & 0xFF;
    }

    public boolean isKeyboardExhausted() {
        return keyboardExhausted;
    }

    public int[] getBannerColumns() {
        return bannerColumns.clone();
    }

    public List<OutputEvent> getOutputs() {
        return outputs;
    }

    @Override
    public int in(int port) {
        if (port == CPU.KEYBOARD_PORT) {
            if (keyboardQueue.isEmpty()) {
                keyboardExhausted = true;
                return 0;
            }
            return keyboardQueue.removeFirst();
        } else if (port == CPU.HEATER_PORT) {
            return heaterStatus;
        } else if (port == CPU.LEDS_PORT) {
            return ledsStatus;
        } else if (port == CPU.MATRIX_ROW_PORT) {
            return matrixSelectedRow;
        } else if (port == CPU.MATRIX_DATA_PORT) {
            return matrixRows[matrixSelectedRow];
        } else if (port == CPU.BUZZER_NOTE_PORT) {
            return buzzerNote;
        } else if (port == CPU.BUZZER_DURATION_PORT) {
            return currentCycle < buzzerEndCycle ? 1 : 0;
        } else if (port == CPU.BANNER_DATA_PORT) {
            return bannerData;
        } else if (port == CPU.BANNER_CMD_PORT) {
            return bannerLastCommand;
        }
        return 0;
    }

    @Override
    public void out(int port, int value) {
        int val8 = value & 0xFF;
        outputs.add(new OutputEvent(currentCycle, port, val8));
        if (port == CPU.HEATER_PORT) {
            // Reflect burner bit 7 in heater status byte
            heaterStatus = (heaterStatus & 0x7F) | (val8 & 0x80);
        } else if (port == CPU.LEDS_PORT) {
            ledsStatus = val8;
        } else if (port == CPU.MATRIX_ROW_PORT) {
            matrixSelectedRow = val8 & 0x07;
        } else if (port == CPU.MATRIX_DATA_PORT) {
            matrixRows[matrixSelectedRow] = val8;
        } else if (port == CPU.BUZZER_NOTE_PORT) {
            buzzerNote = val8 & 0x7F;
        } else if (port == CPU.BUZZER_DURATION_PORT) {
            buzzerDuration = val8;
            buzzerEndCycle = currentCycle + Math.max(1, val8 * 2);
        } else if (port == CPU.BANNER_DATA_PORT) {
            bannerData = val8;
        } else if (port == CPU.BANNER_CMD_PORT) {
            bannerLastCommand = val8;
            switch (val8) {
                case 0:
                    Arrays.fill(bannerColumns, 0);
                    break;
                case 1:
                    System.arraycopy(bannerColumns, 1, bannerColumns, 0, 63);
                    bannerColumns[63] = 0;
                    break;
                case 2:
                    System.arraycopy(bannerColumns, 0, bannerColumns, 1, 63);
                    bannerColumns[0] = 0;
                    break;
                case 3:
                    bannerColumns[0] = bannerData;
                    break;
                case 4:
                    bannerColumns[63] = bannerData;
                    break;
                case 5:
                    System.arraycopy(bannerColumns, 1, bannerColumns, 0, 63);
                    bannerColumns[63] = bannerData;
                    break;
                case 6:
                    System.arraycopy(bannerColumns, 0, bannerColumns, 1, 63);
                    bannerColumns[0] = bannerData;
                    break;
                default:
                    break;
            }
        }
    }
}

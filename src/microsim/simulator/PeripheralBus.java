package microsim.simulator;

/**
 * Abstraction for CPU I/O port operations (IN / OUT instructions).
 * Allows decoupling the CPU core from Swing UI peripheral windows.
 */
public interface PeripheralBus {
    int in(int port);
    void out(int port, int value);
}

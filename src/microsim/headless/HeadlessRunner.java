package microsim.headless;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import microsim.headless.HeadlessPeripheralBus.OutputEvent;
import microsim.simulator.Assembler;
import microsim.simulator.Assembler.AssemblyResult;
import microsim.simulator.CPU;
import microsim.simulator.Opcodes;
import microsim.simulator.RAM;

/**
 * Headless CLI runner for MicroSim 8-bit assembly programs.
 * Outputs a deterministic JSON execution trace to stdout.
 */
public class HeadlessRunner {

    private static final int SENTINEL_RET_ADDR = 0xFF;

    public static void main(String[] args) {
        String asmPath = null;
        int maxCycles = 10000;
        int heaterStatus = 0;
        String callTarget = null;
        boolean traceAl = false;
        List<Integer> keys = new ArrayList<>();
        List<int[]> initRegs = new ArrayList<>();
        List<int[]> initMem = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--asm":
                    asmPath = args[++i];
                    break;
                case "--max-cycles":
                    maxCycles = Integer.parseInt(args[++i]);
                    break;
                case "--heater":
                    heaterStatus = parseInt(args[++i]);
                    break;
                case "--call":
                    callTarget = args[++i];
                    break;
                case "--trace-al":
                    traceAl = true;
                    break;
                case "--keys":
                    String keysArg = args[++i];
                    if (!keysArg.isEmpty()) {
                        for (String part : keysArg.split(",")) {
                            keys.add(parseInt(part.trim()));
                        }
                    }
                    break;
                case "--reg":
                    String[] rp = args[++i].split("=");
                    initRegs.add(new int[]{parseRegIndex(rp[0].trim()), parseInt(rp[1].trim())});
                    break;
                case "--mem":
                    String[] mp = args[++i].split("=");
                    int baseAddr = parseInt(mp[0].trim());
                    String[] bytes = mp[1].split(",");
                    for (int b = 0; b < bytes.length; b++) {
                        initMem.add(new int[]{(baseAddr + b) & 0xFF, parseInt(bytes[b].trim()) & 0xFF});
                    }
                    break;
                default:
                    System.err.println("Unknown option: " + args[i]);
                    System.exit(2);
            }
        }

        if (asmPath == null) {
            System.err.println("Missing required --asm <file> argument");
            System.exit(2);
        }

        String source;
        try {
            source = Files.readString(Path.of(asmPath), StandardCharsets.UTF_8);
        } catch (Exception e) {
            printErrorJson("Cannot read file: " + e.getMessage());
            return;
        }

        Assembler assembler = new Assembler();
        AssemblyResult result;
        try {
            result = assembler.assemble(source);
        } catch (Exception e) {
            printErrorJson(e.getMessage());
            return;
        }

        Integer callAddr = resolveCallTarget(callTarget, result.labels);

        RAM ram = new RAM(256, true);
        for (int addr = 0; addr < result.machineCode.length && addr < 256; addr++) {
            ram.store(addr, result.machineCode[addr]);
        }
        for (int[] kv : initMem) {
            ram.store(kv[0], kv[1]);
        }

        HeadlessPeripheralBus bus = new HeadlessPeripheralBus();
        for (int k : keys) {
            bus.enqueueKey(k);
        }
        bus.setHeaterStatus(heaterStatus);

        CPU cpu = new CPU(ram, bus);
        for (int[] rv : initRegs) {
            cpu.getRegisters()[rv[0]] = rv[1] & 0xFF;
        }

        if (callAddr != null) {
            cpu.prepareSubroutineCall(callAddr, SENTINEL_RET_ADDR);
        }

        int cycles = 0;
        boolean timeout = false;
        boolean returned = false;
        List<Integer> callTargets = new ArrayList<>();
        List<Integer> alTrace = new ArrayList<>();
        if (traceAl) {
            alTrace.add(cpu.getRegisters()[0] & 0xFF);
        }

        while (!cpu.isHalted() && !cpu.isFault()) {
            if (callAddr != null && cpu.getIP() == SENTINEL_RET_ADDR) {
                returned = true;
                break;
            }
            if (cycles >= maxCycles) {
                timeout = true;
                break;
            }
            int ipBefore = cpu.getIP() & 0xFF;
            int opBefore = ram.load(ipBefore) & 0xFF;
            if (opBefore == Opcodes.CALL_ADDRESS) {
                int target = ram.load((ipBefore + 1) & 0xFF) & 0xFF;
                if (!callTargets.contains(target)) {
                    callTargets.add(target);
                }
            } else if (opBefore == Opcodes.CALL_REGADDRESS) {
                int regIdx = ram.load((ipBefore + 1) & 0xFF) & 0x03;
                int target = cpu.getRegisters()[regIdx] & 0xFF;
                if (!callTargets.contains(target)) {
                    callTargets.add(target);
                }
            }
            bus.setCurrentCycle(cycles);
            try {
                cpu.step();
            } catch (Exception e) {
                // CPU sets fault=true on exception
                break;
            }
            cycles++;
            if (bus.isKeyboardExhausted()) {
                timeout = true;
                break;
            }
            if (traceAl) {
                int al = cpu.getRegisters()[0] & 0xFF;
                if (alTrace.isEmpty() || alTrace.get(alTrace.size() - 1) != al) {
                    alTrace.add(al);
                }
            }
        }

        printResultJson(cpu, ram, bus, cycles, timeout, returned, result.labels, callTargets, traceAl ? alTrace : null);
    }

    private static Integer resolveCallTarget(String callTarget, Map<String, Integer> labels) {
        if (callTarget == null || callTarget.isBlank()) {
            return null;
        }
        for (String candidate : callTarget.split(",")) {
            String token = candidate.trim();
            if (token.isEmpty()) continue;
            if (labels != null) {
                for (Map.Entry<String, Integer> entry : labels.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(token)) {
                        return entry.getValue();
                    }
                }
            }
            try {
                return parseInt(token);
            } catch (NumberFormatException ignored) {
                // Try next candidate
            }
        }
        return null;
    }

    private static int parseInt(String s) {
        String t = s.trim().toLowerCase();
        if (t.startsWith("0x")) {
            return Integer.parseInt(t.substring(2), 16);
        }
        return Integer.parseInt(t);
    }

    private static int parseRegIndex(String name) {
        switch (name.toUpperCase()) {
            case "AL": return 0;
            case "BL": return 1;
            case "CL": return 2;
            case "DL": return 3;
            default: throw new IllegalArgumentException("Unknown register: " + name);
        }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void printErrorJson(String error) {
        System.out.println("{\"assembled\":false,\"error\":\"" + jsonEscape(error) + "\"}");
    }

    private static void printResultJson(
            CPU cpu,
            RAM ram,
            HeadlessPeripheralBus bus,
            int cycles,
            boolean timeout,
            boolean returned,
            Map<String, Integer> labels,
            List<Integer> callTargets,
            List<Integer> alTrace) {
        StringBuilder sb = new StringBuilder();
        int[] regs = cpu.getRegisters();
        sb.append("{");
        sb.append("\"assembled\":true,");
        sb.append("\"error\":null,");
        sb.append("\"cycles\":").append(cycles).append(",");
        sb.append("\"halted\":").append(cpu.isHalted()).append(",");
        sb.append("\"returned\":").append(returned).append(",");
        sb.append("\"fault\":").append(cpu.isFault()).append(",");
        sb.append("\"timeout\":").append(timeout).append(",");
        sb.append("\"keyboard_exhausted\":").append(bus.isKeyboardExhausted()).append(",");
        sb.append("\"registers\":{");
        sb.append("\"AL\":").append(regs[0] & 0xFF).append(",");
        sb.append("\"BL\":").append(regs[1] & 0xFF).append(",");
        sb.append("\"CL\":").append(regs[2] & 0xFF).append(",");
        sb.append("\"DL\":").append(regs[3] & 0xFF).append(",");
        sb.append("\"SP\":").append(cpu.getSP() & 0xFF).append(",");
        sb.append("\"IP\":").append(cpu.getIP() & 0xFF).append(",");
        sb.append("\"SR\":").append(cpu.getSR() & 0xFF);
        sb.append("},");
        sb.append("\"flags\":{");
        sb.append("\"Z\":").append(cpu.isZero()).append(",");
        sb.append("\"C\":").append(cpu.isCarry()).append(",");
        sb.append("\"O\":").append(cpu.isOverflow()).append(",");
        sb.append("\"S\":").append(cpu.isSign()).append(",");
        sb.append("\"F\":").append(cpu.isFault());
        sb.append("},");
        sb.append("\"outputs\":[");
        List<OutputEvent> outs = bus.getOutputs();
        for (int i = 0; i < outs.size(); i++) {
            OutputEvent ev = outs.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"cycle\":").append(ev.cycle)
              .append(",\"port\":").append(ev.port)
              .append(",\"value\":").append(ev.value).append("}");
        }
        sb.append("],");
        if (alTrace != null) {
            sb.append("\"al_trace\":[");
            for (int i = 0; i < alTrace.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(alTrace.get(i));
            }
            sb.append("],");
        }
        sb.append("\"labels\":{");
        if (labels != null) {
            int idx = 0;
            for (Map.Entry<String, Integer> e : labels.entrySet()) {
                if (idx++ > 0) sb.append(",");
                sb.append("\"").append(jsonEscape(e.getKey())).append("\":").append(e.getValue() & 0xFF);
            }
        }
        sb.append("},");
        sb.append("\"call_targets\":[");
        for (int i = 0; i < callTargets.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(callTargets.get(i));
        }
        sb.append("],");
        sb.append("\"ram\":[");
        for (int i = 0; i < 256; i++) {
            if (i > 0) sb.append(",");
            sb.append(ram.load(i) & 0xFF);
        }
        sb.append("]");
        sb.append("}");
        System.out.println(sb.toString());
    }
}

package microsim;

import javax.swing.JPanel;
import javax.swing.Timer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import microsim.simulator.CPU;
import microsim.simulator.RAM;
import microsim.simulator.Simulator;
import microsim.simulator.Utils;

/**
 * A panel for visualizing the CPU, RAM, and buses in an educational and modern style.
 */
public class VisualizationPanel extends JPanel implements ActionListener {

    private static final int PADDING = 20;
    private static final int CPU_WIDTH = 200;
    private static final int RAM_WIDTH = 155;
    private static final int DEV_WIDTH = 150;
    private static final int RECT_HEIGHT = 205;
    private static final int BUS_THICKNESS = 20;
    private static final int BUS_TOP_MARGIN = 145; // Espace pour les bus et le bandeau de statut
    private static final int ANIMATION_DELAY = 16; // approx 60fps

    // Cadence fixe pédagogique : la fenêtre de visualisation est l'horloge maîtresse (clock)
    public static final int TRANSFER_DURATION_MS = 1200;       // Transfert écriture (WRITE, IO_WRITE) ou calcul ALU
    public static final int READ_TRANSFER_DURATION_MS = 2200;  // Transfert lecture (READ, IO_READ) en 2 phases (aller-retour)
    public static final int IDLE_CLOCK_CYCLE_MS = 600;        // Cadence pour les instructions simples sans bus/ALU

    // Typographies modernes
    private final Font monacoFont = new Font("Monaco", Font.PLAIN, 12);
    private final Font titleFont = new Font("SansSerif", Font.BOLD, 15);
    private final Font boldCodeFont = new Font("Menlo", Font.BOLD, 12);
    private final Font packetFont = new Font("Menlo", Font.BOLD, 13);
    private final Font statusFont = new Font("SansSerif", Font.BOLD, 11);
    private final Font busLabelFont = new Font("SansSerif", Font.BOLD, 11);
    private final Font badgeFont = new Font("SansSerif", Font.BOLD, 9);
    private final Font flagFont = new Font("SansSerif", Font.BOLD, 10);

    // Palette moderne Dark Mode
    private static final Color BG_PANEL = new Color(15, 23, 42);     // Slate 900
    private static final Color CARD_BG = new Color(30, 41, 59);      // Slate 800
    private static final Color CARD_BORDER = new Color(51, 65, 85);  // Slate 700
    
    private static final Color CPU_HEADER = new Color(51, 65, 85);    // Slate 700
    private static final Color ALU_HEADER = new Color(79, 70, 229);   // Indigo 600
    private static final Color RAM_HEADER = new Color(13, 148, 136);  // Teal 600
    private static final Color DEV_HEADER = new Color(180, 83, 9);    // Amber 700

    private static final Color BUS_ADDRESS = new Color(245, 158, 11); // Ambre / Orange
    private static final Color BUS_DATA = new Color(14, 165, 233);    // Cyan / Bleu ciel
    private static final Color BUS_CTRL = new Color(34, 197, 94);     // Vert vif

    private Simulator simulator;

    // Animation fields
    private Timer animationTimer;
    private Timer idleTimer;
    private boolean animationRunning = false;
    private double animationProgress = 0.0;
    private long animationStartTime = 0;
    private int targetDurationMs = 0;
    private int animatedValue;
    private int animatedAddress;
    private CPU.BusActivity animatedActivity;
    private boolean isAluAnimated = false;
    private String animatedAluOp = null;
    private boolean addressAnimationFinished = false;
    private Runnable onAnimationComplete = null;
    private int ramViewStartAddr = 0;

    public VisualizationPanel(Simulator simulator) {
        super();
        this.simulator = simulator;
        setBackground(BG_PANEL);
        animationTimer = new Timer(ANIMATION_DELAY, this);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(700, 515);
    }

    public boolean isAnimating() {
        return (animationTimer != null && animationTimer.isRunning()) || (idleTimer != null && idleTimer.isRunning());
    }

    private void startAnimation(CPU.BusActivity activity, int value, int address, boolean aluActive, String aluOp, Runnable onComplete) {
        if (animationTimer.isRunning()) {
            animationTimer.stop();
            if (this.onAnimationComplete != null) {
                Runnable prev = this.onAnimationComplete;
                this.onAnimationComplete = null;
                prev.run();
            }
        }
        if (idleTimer != null && idleTimer.isRunning()) {
            idleTimer.stop();
        }

        // Adjust RAM view to center the animated address (seulement pour accès mémoire RAM)
        if (activity == CPU.BusActivity.READ || activity == CPU.BusActivity.WRITE) {
            int numLines = Math.max(1, (getHeight() - BUS_TOP_MARGIN - 2 * PADDING - 55) / 16);
            if (address < this.ramViewStartAddr || address >= this.ramViewStartAddr + numLines) {
                this.ramViewStartAddr = Math.max(0, address - (numLines / 2));
            }
        }

        this.animatedActivity = activity;
        this.animatedValue = value;
        this.animatedAddress = address;
        this.isAluAnimated = aluActive;
        this.animatedAluOp = aluOp;
        this.animationProgress = 0.0;
        this.animationRunning = true;
        this.addressAnimationFinished = false;
        this.onAnimationComplete = onComplete;

        boolean isRead = (activity == CPU.BusActivity.READ || activity == CPU.BusActivity.IO_READ);
        // Durée fixe pédagogique de référence : la visualisation est l'horloge maîtresse
        this.targetDurationMs = isRead ? READ_TRANSFER_DURATION_MS : TRANSFER_DURATION_MS;
        this.animationStartTime = System.currentTimeMillis();

        animationTimer.start();
    }

    /**
     * Stoppe immédiatement toute animation ou temporisation en cours et libère les verrous en attente.
     */
    public void stopAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        if (idleTimer != null && idleTimer.isRunning()) {
            idleTimer.stop();
        }
        animationRunning = false;
        if (onAnimationComplete != null) {
            Runnable prev = onAnimationComplete;
            onAnimationComplete = null;
            prev.run();
        }
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        long elapsed = System.currentTimeMillis() - animationStartTime;
        boolean isRead = (animatedActivity == CPU.BusActivity.READ || animatedActivity == CPU.BusActivity.IO_READ);
        double totalDuration = isRead ? 2.0 : 1.0;

        if (targetDurationMs <= 0) {
            animationProgress = totalDuration;
        } else {
            double fraction = Math.min(1.0, (double) elapsed / targetDurationMs);
            animationProgress = fraction * totalDuration;
        }

        if (animationProgress >= 1.0 && isRead && !addressAnimationFinished) {
            addressAnimationFinished = true;
        }

        if (elapsed >= targetDurationMs || animationProgress >= totalDuration) {
            animationProgress = totalDuration;
            animationTimer.stop();
            animationRunning = false;
            CPU cpu = simulator.getCPU();
            if (cpu != null) {
                cpu.resetBusActivity();
                cpu.resetAluActive();
            }

            // Notify completion
            if (onAnimationComplete != null) {
                Runnable prev = onAnimationComplete;
                onAnimationComplete = null;
                prev.run();
            }
        }
        repaint();
    }
    
    public void updateAndAnimate(Runnable onComplete) {
        if (idleTimer != null && idleTimer.isRunning()) {
            idleTimer.stop();
        }

        CPU cpu = simulator.getCPU();
        boolean hasBus = cpu != null && cpu.getLastBusActivity() != CPU.BusActivity.NONE;
        boolean hasAlu = cpu != null && cpu.isAluActive();
        boolean shouldAnimate = this.isShowing() && (hasBus || hasAlu);
        
        if (shouldAnimate) {
            startAnimation(
                cpu.getLastBusActivity(), 
                cpu.getLastDataValue(), 
                cpu.getLastMemoryAddress(),
                hasAlu,
                cpu.getLastAluOperation(),
                onComplete
            );
        } else {
            repaint();
            if (cpu != null) {
                if (cpu.getLastBusActivity() != CPU.BusActivity.NONE) cpu.resetBusActivity();
                if (cpu.isAluActive()) cpu.resetAluActive();
            }
            if (this.isShowing() && onComplete != null) {
                // La fenêtre de visualisation est l'horloge : pause d'un cycle fixe pour les instructions sans bus/ALU
                this.onAnimationComplete = onComplete;
                idleTimer = new Timer(IDLE_CLOCK_CYCLE_MS, ev -> {
                    ((Timer) ev.getSource()).stop();
                    if (this.onAnimationComplete != null) {
                        Runnable prev = this.onAnimationComplete;
                        this.onAnimationComplete = null;
                        prev.run();
                    }
                });
                idleTimer.setRepeats(false);
                idleTimer.start();
            } else if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Positions des composants
        int cpuX = PADDING;
        int cpuY = BUS_TOP_MARGIN + PADDING;
        int aluX = PADDING;
        int aluY = cpuY + RECT_HEIGHT + PADDING + 10;
        int aluHeight = 95;
        int ramX = cpuX + 270;
        int ramY = BUS_TOP_MARGIN + PADDING;
        int devX = ramX + 240;
        int devY = BUS_TOP_MARGIN + PADDING;

        // Positions des bus horizontaux en haut
        int busStartX = PADDING;
        int busEndX = getWidth() - PADDING;
        int busLength = Math.max(10, busEndX - busStartX);
        int addressBusY = PADDING + 12;
        int dataBusY = addressBusY + BUS_THICKNESS + 8;
        int ctrlBusY = dataBusY + BUS_THICKNESS + 8;

        // Dérivations verticales
        int cpuConnectionX = cpuX + CPU_WIDTH / 2;
        int ramConnectionX = ramX + RAM_WIDTH / 2;
        int devConnectionX = devX + DEV_WIDTH / 2;

        // 1. Dessiner les composants statiques (CPU, ALU, RAM, DEVICE)
        drawStaticComponents(g2d, cpuX, cpuY, aluX, aluY, aluHeight, ramX, ramY, devX, devY);

        // 2. Dessiner les valeurs dynamiques (registres, flags, marqueurs IP/SP, mémoire)
        drawDynamicValues(g2d, cpuX, cpuY, ramX, ramY);

        // 3. Dessiner les bus, les dérivations et les paquets animés
        drawBusesAndAnimation(g2d, cpuX, cpuY, ramX, ramY, devX, devY,
                              busStartX, busEndX, busLength, 
                              addressBusY, dataBusY, ctrlBusY,
                              cpuConnectionX, ramConnectionX, devConnectionX);

        // 4. Bandeau pédagogique dynamique
        drawEducationalBanner(g2d, busStartX, ctrlBusY + BUS_THICKNESS / 2 + 10, busLength, 28);
    }

    /**
     * Dessine un bloc (carte) avec un en-tête coloré et des coins arrondis
     */
    private void drawCard(Graphics2D g2d, int x, int y, int width, int height, String title, Color headerColor) {
        int arc = 12;
        int headerHeight = 34;

        g2d.setColor(CARD_BG);
        g2d.fillRoundRect(x, y, width, height, arc, arc);

        g2d.setColor(headerColor);
        g2d.fillRoundRect(x, y, width, headerHeight + arc, arc, arc);
        g2d.fillRect(x, y + headerHeight, width, 2);

        g2d.setColor(CARD_BORDER);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(x, y, width, height, arc, arc);

        g2d.setColor(Color.WHITE);
        g2d.setFont(titleFont);
        g2d.drawString(title, x + 12, y + 29);
    }

    private void drawStaticComponents(Graphics2D g2d, int cpuX, int cpuY, int aluX, int aluY, int aluHeight, int ramX, int ramY, int devX, int devY) {
        int ramHeight = getHeight() - BUS_TOP_MARGIN - 2 * PADDING;
        CPU cpu = simulator.getCPU();

        // Liaison bus interne entre CPU et ALU
        int busInterneX = cpuX + CPU_WIDTH / 2 - 12;
        int busInterneY = cpuY + RECT_HEIGHT;
        int busInterneH = aluY - busInterneY;
        int busCenterX = busInterneX + 12;

        if (isAluAnimated && animationRunning) {
            // Bus interne sous tension (Indigo lumineux)
            g2d.setColor(new Color(79, 70, 229));
            g2d.fillRect(busInterneX, busInterneY, 24, busInterneH);
            g2d.setColor(new Color(165, 180, 252));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRect(busInterneX, busInterneY, 24, busInterneH);

            boolean downwards = (animationProgress < 0.5);
            drawInternalBusFlow(g2d, busCenterX, busInterneY, aluY, downwards);

            g2d.setFont(new Font("SansSerif", Font.BOLD, 9));
            g2d.setColor(new Color(224, 231, 255));
            g2d.drawString(downwards ? "OP IN" : "RES OUT", cpuX + CPU_WIDTH / 2 + 16, cpuY + RECT_HEIGHT + 18);
        } else {
            // Bus au repos
            g2d.setColor(new Color(30, 41, 59));
            g2d.fillRect(busInterneX, busInterneY, 24, busInterneH);
            g2d.setColor(new Color(71, 85, 105));
            g2d.setStroke(new BasicStroke(1.2f));
            g2d.drawRect(busInterneX, busInterneY, 24, busInterneH);
            g2d.setFont(new Font("SansSerif", Font.ITALIC, 9));
            g2d.setColor(new Color(148, 163, 184));
            g2d.drawString("Internal bus", cpuX + CPU_WIDTH / 2 + 16, cpuY + RECT_HEIGHT + 18);
        }

        // Cartes des composants
        drawCard(g2d, cpuX, cpuY, CPU_WIDTH, RECT_HEIGHT, "CPU", CPU_HEADER);
        drawCard(g2d, ramX, ramY, RAM_WIDTH, ramHeight, "RAM", RAM_HEADER);
        drawCard(g2d, devX, devY, DEV_WIDTH, RECT_HEIGHT, "PERIPHERALS", DEV_HEADER);

        // 3. Boîte ALU vivante
        String lastAlu = (cpu != null) ? cpu.getLastAluOperation() : null;
        if (lastAlu != null) {
            drawCard(g2d, aluX, aluY, CPU_WIDTH, aluHeight, "ALU", new Color(79, 70, 229));
            g2d.setFont(boldCodeFont);
            g2d.setColor(new Color(165, 180, 252));
            g2d.drawString(lastAlu, aluX + 10, aluY + 65);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2d.setColor(new Color(148, 163, 184));
            g2d.drawString("Result forwarded to internal bus", aluX + 10, aluY + 85);
        } else {
            drawCard(g2d, aluX, aluY, CPU_WIDTH, aluHeight, "ALU", ALU_HEADER);
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2d.setColor(new Color(203, 213, 225));
            g2d.drawString("Operations: ADD, SUB, CMP, MUL", aluX + 10, aluY + 65);
            g2d.drawString("Logic: AND, OR, XOR, NOT, SHL", aluX + 10, aluY + 85);
        }

        // Ports d'E/S dans le bloc Périphériques avec surbrillance du périphérique ciblé
        int[][] periphPorts = {
            {0x01}, {0x02}, {0x03}, {0x04, 0x05}, {0x06}, {0x07, 0x08}, {0x09, 0x0A}
        };
        int[] periphY = {65, 85, 105, 125, 145, 165, 185};
        String[] portNames = {
            "0x01: Keyboard", "0x02: Lights", "0x03: Heater", "0x04: Buzzer", "0x06: LED Bar", "0x07: LED Matrix", "0x09: LED Banner"
        };

        boolean isIOAnimated = animationRunning && (animatedActivity == CPU.BusActivity.IO_WRITE || animatedActivity == CPU.BusActivity.IO_READ);

        for (int p = 0; p < periphPorts.length; p++) {
            int pY = devY + periphY[p];
            boolean isTargetPort = false;
            if (isIOAnimated) {
                for (int port : periphPorts[p]) {
                    if (animatedAddress == port) {
                        isTargetPort = true;
                        break;
                    }
                }
            }

            if (isTargetPort) {
                g2d.setColor(animatedActivity == CPU.BusActivity.IO_WRITE ? new Color(120, 53, 15) : new Color(12, 74, 96));
                g2d.fillRoundRect(devX + 10, pY - 12, DEV_WIDTH - 20, 16, 4, 4);
                g2d.setColor(animatedActivity == CPU.BusActivity.IO_WRITE ? new Color(253, 230, 138) : new Color(186, 230, 253));
                g2d.setFont(boldCodeFont);
            } else {
                g2d.setColor(new Color(203, 213, 225));
                g2d.setFont(monacoFont);
            }
            g2d.drawString(portNames[p], devX + 14, pY);
        }
    }

    private void drawDynamicValues(Graphics2D g2d, int cpuX, int cpuY, int ramX, int ramY) {
        CPU cpu = simulator.getCPU();
        if (cpu == null) return;

        // Registres CPU
        int[] gpr = cpu.getRegisters();
        int regY = cpuY + 75;
        g2d.setFont(boldCodeFont);
        
        // Colonne 1 : registres généraux AL, BL, CL, DL
        String[] regNames = {"AL", "BL", "CL", "DL"};
        for (int i = 0; i < 4; i++) {
            g2d.setColor(new Color(15, 23, 42)); // Slate 900
            g2d.fillRoundRect(cpuX + 12, regY - 14, 80, 22, 6, 6);
            g2d.setColor(new Color(51, 65, 85)); // Slate 700
            g2d.drawRoundRect(cpuX + 12, regY - 14, 80, 22, 6, 6);

            g2d.setColor(new Color(203, 213, 225));
            g2d.drawString(regNames[i] + ": " + Utils.toHexString0x(gpr[i]), cpuX + 18, regY + 2);
            regY += 28;
        }

        // Colonne 2 : pointeurs (IP, SP, SR) et voyants des drapeaux
        regY = cpuY + 75;
        String[] ptrNames = {"IP", "SP", "SR"};
        int[] ptrValues = {cpu.getIP(), cpu.getSP(), cpu.getSR()};
        for (int i = 0; i < 3; i++) {
            g2d.setColor(new Color(15, 23, 42));
            g2d.fillRoundRect(cpuX + 105, regY - 14, 82, 22, 6, 6);
            g2d.setColor(new Color(51, 65, 85));
            g2d.drawRoundRect(cpuX + 105, regY - 14, 82, 22, 6, 6);

            g2d.setColor(i == 0 ? new Color(251, 146, 60) : new Color(203, 213, 225));
            g2d.drawString(ptrNames[i] + ": " + Utils.toHexString0x(ptrValues[i]), cpuX + 111, regY + 2);
            regY += 28;
        }

        // 1. Voyants des drapeaux (Flags Z, C, S, O) alignés vis-à-vis de DL
        int flagX = cpuX + 105;
        int flagY = regY - 14;
        int flagW = 18;
        int flagH = 22;
        String[] flagNames = {"Z", "C", "S", "O"};
        boolean[] flagVals = {cpu.isZero(), cpu.isCarry(), cpu.isSign(), cpu.isOverflow()};

        g2d.setFont(flagFont);
        for (int f = 0; f < 4; f++) {
            int fx = flagX + f * (flagW + 3);
            if (flagVals[f]) {
                g2d.setColor(new Color(22, 101, 52)); // Vert vif actif
                g2d.fillRoundRect(fx, flagY, flagW, flagH, 4, 4);
                g2d.setColor(new Color(34, 197, 94));
                g2d.drawRoundRect(fx, flagY, flagW, flagH, 4, 4);
                g2d.setColor(new Color(240, 253, 244));
            } else {
                g2d.setColor(new Color(15, 23, 42)); // Gris sombre inactif
                g2d.fillRoundRect(fx, flagY, flagW, flagH, 4, 4);
                g2d.setColor(new Color(51, 65, 85));
                g2d.drawRoundRect(fx, flagY, flagW, flagH, 4, 4);
                g2d.setColor(new Color(100, 116, 139));
            }
            g2d.drawString(flagNames[f], fx + 5, flagY + 15);
        }

        // Registre d'Instruction standard (IR)
        // Pas vertical de 28px identique aux autres registres (hauteur 22px + espacement 6px)
        int irX = cpuX + 12;
        int irY = cpuY + 173;
        int irW = CPU_WIDTH - 24;
        int irH = 22;

        g2d.setColor(new Color(15, 23, 42));
        g2d.fillRoundRect(irX, irY, irW, irH, 6, 6);
        g2d.setColor(new Color(51, 65, 85));
        g2d.drawRoundRect(irX, irY, irW, irH, 6, 6);

        String instrText = simulator.getCurrentInstructionText();
        g2d.setFont(boldCodeFont);
        g2d.setColor(new Color(203, 213, 225));
        g2d.drawString("IR: ", irX + 6, irY + 16);

        FontMetrics fm = g2d.getFontMetrics();
        int prefixWidth = fm.stringWidth("IR: ");

        if (instrText != null) {
            g2d.drawString(instrText, irX + 6 + prefixWidth, irY + 16);
        } else {
            g2d.setColor(new Color(100, 116, 139));
            g2d.drawString("(idle)", irX + 6 + prefixWidth, irY + 16);
        }

        // 4. Marqueurs [IP] et [SP] dans la colonne RAM
        RAM ram = simulator.getRAM();
        int startAddress = this.ramViewStartAddr;
        int numLines = Math.max(1, (getHeight() - BUS_TOP_MARGIN - 2 * PADDING - 65) / 16);
        int endAddress = Math.min(ram.getSize() - 1, startAddress + numLines - 1);
        int memY = ramY + 65;

        int currentIP = cpu.getIP();
        int currentSP = cpu.getSP();

        for (int addr = startAddress; addr <= endAddress; addr++) {
            String line = Utils.toHexString0x(addr) + ": " + Utils.toHexString0x(ram.load(addr));
            boolean isTarget = (animationRunning && (animatedActivity == CPU.BusActivity.READ || animatedActivity == CPU.BusActivity.WRITE) && addr == animatedAddress);

            if (isTarget) {
                g2d.setColor(animatedActivity == CPU.BusActivity.WRITE ? new Color(127, 29, 29) : new Color(12, 74, 96));
                g2d.fillRoundRect(ramX + 8, memY - 12, RAM_WIDTH - 16, 15, 4, 4);
                g2d.setColor(animatedActivity == CPU.BusActivity.WRITE ? new Color(254, 202, 202) : new Color(186, 230, 253));
                g2d.setFont(boldCodeFont);
            } else {
                g2d.setColor(new Color(203, 213, 225));
                g2d.setFont(monacoFont);
            }

            g2d.drawString(line, ramX + 12, memY);

            // Badges IP / SP dans la RAM
            int badgeX = ramX + 102;
            if (addr == currentIP) {
                int bY = memY - 11;
                int bH = 13;
                int arrowW = 5;
                int bW = 22;

                int[] xPoints = {
                    badgeX, 
                    badgeX + arrowW, 
                    badgeX + bW, 
                    badgeX + bW, 
                    badgeX + arrowW
                };
                int[] yPoints = {
                    bY + bH / 2, 
                    bY, 
                    bY, 
                    bY + bH, 
                    bY + bH
                };

                g2d.setColor(new Color(154, 52, 18));
                g2d.fillPolygon(xPoints, yPoints, 5);

                g2d.setColor(new Color(254, 215, 170, 140));
                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawPolygon(xPoints, yPoints, 5);

                g2d.setColor(new Color(254, 215, 170));
                g2d.setFont(badgeFont);
                g2d.drawString("IP", badgeX + arrowW + 3, memY - 1);
                badgeX += bW + 3;
            }
            if (addr == currentSP) {
                g2d.setColor(new Color(91, 33, 182));
                g2d.fillRoundRect(badgeX, memY - 11, 20, 13, 4, 4);
                g2d.setColor(new Color(237, 233, 254));
                g2d.setFont(badgeFont);
                g2d.drawString("SP", badgeX + 3, memY - 1);
            }

            memY += 16;
        }
    }

    /**
     * Dessine un paquet circulant sur les bus :
     * Option 3 : badge noir profond avec halo lumineux diffus (glow doré/blanc) et texte blanc éclatant.
     */
    private void drawPacket(Graphics2D g2d, int x, int y, String text, Color busColor) {
        g2d.setFont(packetFont);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int packetWidth = Math.max(54, textWidth + 20);
        int packetHeight = 26; // Déborde de 3px au-dessus et en-dessous du bus de 20px

        int px = x - packetWidth / 2;
        int py = y - packetHeight / 2;

        // 1. Halo lumineux diffus externe (glow doux)
        g2d.setColor(new Color(254, 240, 138, 35));
        g2d.fillRoundRect(px - 5, py - 5, packetWidth + 10, packetHeight + 10, 14, 14);

        // 2. Halo lumineux intermédiaire
        g2d.setColor(new Color(254, 240, 138, 80));
        g2d.fillRoundRect(px - 3, py - 3, packetWidth + 6, packetHeight + 6, 10, 10);

        // 3. Halo lumineux proche
        g2d.setColor(new Color(254, 240, 138, 140));
        g2d.fillRoundRect(px - 1, py - 1, packetWidth + 2, packetHeight + 2, 8, 8);

        // 4. Cœur du badge noir profond
        g2d.setColor(new Color(3, 7, 18)); // Noir profond
        g2d.fillRoundRect(px, py, packetWidth, packetHeight, 6, 6);

        // 5. Liseré intérieur lumineux doré
        g2d.setColor(new Color(254, 240, 138, 220));
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawRoundRect(px, py, packetWidth, packetHeight, 6, 6);

        // 6. Texte blanc pur éclatant
        g2d.setColor(Color.WHITE);
        int textX = x - textWidth / 2;
        int textY = y + (fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(text, textX, textY);
    }

    private void drawBusesAndAnimation(Graphics2D g2d, int cpuX, int cpuY, int ramX, int ramY, int devX, int devY,
                                       int busStartX, int busEndX, int busLength,
                                       int addressBusY, int dataBusY, int ctrlBusY,
                                       int cpuConnectionX, int ramConnectionX, int devConnectionX) {

        int connectionOffset = BUS_THICKNESS * 2;
        int ySpace = 3;

        // 1. Dérivations verticales d'abord (en arrière-plan)
        // Dérivations CPU
        drawBranch(g2d, cpuConnectionX - connectionOffset, addressBusY, cpuY - ySpace, BUS_ADDRESS);
        drawBranch(g2d, cpuConnectionX, dataBusY, cpuY - ySpace, BUS_DATA);
        drawBranch(g2d, cpuConnectionX + connectionOffset, ctrlBusY, cpuY - ySpace, BUS_CTRL);

        // Dérivations RAM
        drawBranch(g2d, ramConnectionX - connectionOffset, addressBusY, ramY - ySpace, BUS_ADDRESS);
        drawBranch(g2d, ramConnectionX, dataBusY, ramY - ySpace, BUS_DATA);
        drawBranch(g2d, ramConnectionX + connectionOffset, ctrlBusY, ramY - ySpace, BUS_CTRL);

        // Dérivations DEVICE (3 bus complets : Adresse/Port, Données, Contrôle)
        drawBranch(g2d, devConnectionX - connectionOffset, addressBusY, devY - ySpace, BUS_ADDRESS);
        drawBranch(g2d, devConnectionX, dataBusY, devY - ySpace, BUS_DATA);
        drawBranch(g2d, devConnectionX + connectionOffset, ctrlBusY, devY - ySpace, BUS_CTRL);

        // 2. Bus horizontaux dessinés par-dessus les dérivations verticales
        drawSingleBus(g2d, busStartX, addressBusY, busLength, BUS_ADDRESS, "Address");
        drawSingleBus(g2d, busStartX, dataBusY, busLength, BUS_DATA, "Data");
        drawSingleBus(g2d, busStartX, ctrlBusY, busLength, BUS_CTRL, "Control");

        // 3. Animation dynamique des paquets et flux de bus (RAM et Périphériques unifiés)
        if (animationRunning && animatedActivity != CPU.BusActivity.NONE) {
            boolean isIO = (animatedActivity == CPU.BusActivity.IO_WRITE || animatedActivity == CPU.BusActivity.IO_READ);
            int targetX = isIO ? devConnectionX : ramConnectionX;
            int targetY = isIO ? devY : ramY;
            boolean isWrite = (animatedActivity == CPU.BusActivity.WRITE || animatedActivity == CPU.BusActivity.IO_WRITE);

            // Chevrons lumineux défilant à l'intérieur des bus actifs
            if (isWrite) {
                drawBusFlow(g2d, cpuConnectionX - connectionOffset, targetX - connectionOffset, addressBusY, true);
                drawBusFlow(g2d, cpuConnectionX, targetX, dataBusY, true);
                drawBusFlow(g2d, cpuConnectionX + connectionOffset, targetX + connectionOffset, ctrlBusY, true);
            } else {
                if (animationProgress < 1.0) {
                    drawBusFlow(g2d, cpuConnectionX - connectionOffset, targetX - connectionOffset, addressBusY, true);
                    drawBusFlow(g2d, cpuConnectionX + connectionOffset, targetX + connectionOffset, ctrlBusY, true);
                } else {
                    drawBusFlow(g2d, targetX, cpuConnectionX, dataBusY, false);
                }
            }

            String addressStr = Utils.toHexString0x(animatedAddress);
            String dataStr = Utils.toHexString0x(animatedValue);
            String ctrlStr = isWrite ? "WRITE" : "READ";

            if (isWrite) {
                // ÉCRITURE (WRITE ou IO_WRITE) : Adresse/Port, Donnée et Contrôle partent du CPU vers la cible
                double progress = Math.min(1.0, animationProgress);

                if (progress < 0.25) {
                    double verticalProgress = progress / 0.25;
                    
                    int addrY = cpuY - (int)((cpuY - addressBusY) * verticalProgress);
                    drawPacket(g2d, cpuConnectionX - connectionOffset, addrY, addressStr, BUS_ADDRESS);

                    int dataY = cpuY - (int)((cpuY - dataBusY) * verticalProgress);
                    drawPacket(g2d, cpuConnectionX, dataY, dataStr, BUS_DATA);

                    int ctrlY = cpuY - (int)((cpuY - ctrlBusY) * verticalProgress);
                    drawPacket(g2d, cpuConnectionX + connectionOffset, ctrlY, ctrlStr, BUS_CTRL);

                } else if (progress < 0.75) {
                    double horizontalProgress = (progress - 0.25) / 0.5;

                    int addrX = (cpuConnectionX - connectionOffset) + (int)(((targetX - connectionOffset) - (cpuConnectionX - connectionOffset)) * horizontalProgress);
                    drawPacket(g2d, addrX, addressBusY, addressStr, BUS_ADDRESS);

                    int dataX = cpuConnectionX + (int)((targetX - cpuConnectionX) * horizontalProgress);
                    drawPacket(g2d, dataX, dataBusY, dataStr, BUS_DATA);

                    int ctrlX = (cpuConnectionX + connectionOffset) + (int)(((targetX + connectionOffset) - (cpuConnectionX + connectionOffset)) * horizontalProgress);
                    drawPacket(g2d, ctrlX, ctrlBusY, ctrlStr, BUS_CTRL);

                } else {
                    double verticalProgress = (progress - 0.75) / 0.25;

                    int addrY = addressBusY + (int)((targetY - addressBusY) * verticalProgress);
                    drawPacket(g2d, targetX - connectionOffset, addrY, addressStr, BUS_ADDRESS);

                    int dataY = dataBusY + (int)((targetY - dataBusY) * verticalProgress);
                    drawPacket(g2d, targetX, dataY, dataStr, BUS_DATA);

                    int ctrlY = ctrlBusY + (int)((targetY - ctrlBusY) * verticalProgress);
                    drawPacket(g2d, targetX + connectionOffset, ctrlY, ctrlStr, BUS_CTRL);
                }

            } else {
                // LECTURE (READ ou IO_READ)
                if (animationProgress < 1.0) {
                    // Phase 1 (0.0 -> 1.0) : Adresse et Contrôle vont du CPU vers la cible
                    double progress = animationProgress;

                    if (progress < 0.25) {
                        double verticalProgress = progress / 0.25;
                        
                        int addrY = cpuY - (int)((cpuY - addressBusY) * verticalProgress);
                        drawPacket(g2d, cpuConnectionX - connectionOffset, addrY, addressStr, BUS_ADDRESS);

                        int ctrlY = cpuY - (int)((cpuY - ctrlBusY) * verticalProgress);
                        drawPacket(g2d, cpuConnectionX + connectionOffset, ctrlY, ctrlStr, BUS_CTRL);

                    } else if (progress < 0.75) {
                        double horizontalProgress = (progress - 0.25) / 0.5;

                        int addrX = (cpuConnectionX - connectionOffset) + (int)(((targetX - connectionOffset) - (cpuConnectionX - connectionOffset)) * horizontalProgress);
                        drawPacket(g2d, addrX, addressBusY, addressStr, BUS_ADDRESS);

                        int ctrlX = (cpuConnectionX + connectionOffset) + (int)(((targetX + connectionOffset) - (cpuConnectionX + connectionOffset)) * horizontalProgress);
                        drawPacket(g2d, ctrlX, ctrlBusY, ctrlStr, BUS_CTRL);

                    } else {
                        double verticalProgress = (progress - 0.75) / 0.25;

                        int addrY = addressBusY + (int)((targetY - addressBusY) * verticalProgress);
                        drawPacket(g2d, targetX - connectionOffset, addrY, addressStr, BUS_ADDRESS);

                        int ctrlY = ctrlBusY + (int)((targetY - ctrlBusY) * verticalProgress);
                        drawPacket(g2d, targetX + connectionOffset, ctrlY, ctrlStr, BUS_CTRL);
                    }

                } else {
                    // Phase 2 (1.0 -> 2.0) : La Donnée revient de la cible vers le CPU sur le bus de données
                    double dataProgress = animationProgress - 1.0;

                    if (dataProgress < 0.25) {
                        double verticalProgress = dataProgress / 0.25;
                        int currentY = targetY - (int)((targetY - dataBusY) * verticalProgress);
                        drawPacket(g2d, targetX, currentY, dataStr, BUS_DATA);

                    } else if (dataProgress < 0.75) {
                        double horizontalProgress = (dataProgress - 0.25) / 0.5;
                        int currentX = targetX - (int)((targetX - cpuConnectionX) * horizontalProgress);
                        drawPacket(g2d, currentX, dataBusY, dataStr, BUS_DATA);

                    } else {
                        double verticalProgress = (dataProgress - 0.75) / 0.25;
                        int currentY = dataBusY + (int)((cpuY - dataBusY) * verticalProgress);
                        drawPacket(g2d, cpuConnectionX, currentY, dataStr, BUS_DATA);
                    }
                }
            }
        }
    }

    /**
     * Dessine un flux continu de petits chevrons lumineux se déplaçant dans le sens du signal sur le bus
     */
    private void drawBusFlow(Graphics2D g2d, int fromX, int toX, int busY, boolean toRight) {
        int start = Math.min(fromX, toX);
        int end = Math.max(fromX, toX);
        if (end - start < 15) return;

        int step = 20;
        int shift = (int) ((animationProgress * 350) % step);
        if (!toRight) {
            shift = step - shift;
        }

        g2d.setColor(new Color(255, 255, 255, 175)); // Blanc lumineux translucide
        for (int x = start + shift; x < end; x += step) {
            if (x < start + 6 || x > end - 6) continue;
            if (toRight) {
                int[] xp = {x - 3, x + 4, x - 3};
                int[] yp = {busY - 4, busY, busY + 4};
                g2d.fillPolygon(xp, yp, 3);
            } else {
                int[] xp = {x + 3, x - 4, x + 3};
                int[] yp = {busY - 4, busY, busY + 4};
                g2d.fillPolygon(xp, yp, 3);
            }
        }
    }

    /**
     * Dessine un flux continu de petits chevrons lumineux verticaux sur le bus interne CPU <-> ALU
     */
    private void drawInternalBusFlow(Graphics2D g2d, int busX, int startY, int endY, boolean downwards) {
        int height = endY - startY;
        if (height < 10) return;

        int step = 12;
        int shift = (int) ((animationProgress * 300) % step);
        if (!downwards) {
            shift = step - shift;
        }

        g2d.setColor(new Color(255, 255, 255, 220));
        for (int y = startY + shift; y < endY; y += step) {
            if (y < startY + 4 || y > endY - 4) continue;
            if (downwards) {
                int[] xp = {busX - 4, busX, busX + 4};
                int[] yp = {y - 3, y + 4, y - 3};
                g2d.fillPolygon(xp, yp, 3);
            } else {
                int[] xp = {busX - 4, busX, busX + 4};
                int[] yp = {y + 3, y - 4, y + 3};
                g2d.fillPolygon(xp, yp, 3);
            }
        }
    }

    private void drawSingleBus(Graphics2D g2d, int startX, int busY, int length, Color color, String label) {
        int halfThickness = BUS_THICKNESS / 2;

        g2d.setColor(color);
        g2d.fillRoundRect(startX, busY - halfThickness, length, BUS_THICKNESS, 6, 6);

        g2d.setColor(new Color(255, 255, 255, 140));
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawRoundRect(startX, busY - halfThickness, length, BUS_THICKNESS, 6, 6);

        g2d.setFont(busLabelFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString(label, startX + 12, busY + 4);
    }

    private void drawBranch(Graphics2D g2d, int x, int startY, int endY, Color color) {
        int width = BUS_THICKNESS;
        int height = Math.max(0, endY - startY);

        g2d.setColor(color);
        g2d.fillRect(x - width / 2, startY, width, height);

        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawRect(x - width / 2, startY, width, height);
    }

    private String getPortName(int port) {
        switch (port) {
            case 0x01: return "Keyboard";
            case 0x02: return "Lights";
            case 0x03: return "Heater";
            case 0x04: return "Buzzer Note";
            case 0x05: return "Buzzer Duration";
            case 0x06: return "LED Bar";
            case 0x07: return "LED Matrix Row";
            case 0x08: return "LED Matrix Data";
            case 0x09: return "LED Banner Data";
            case 0x0A: return "LED Banner Cmd";
            default: return "Port " + Utils.toHexString0x(port);
        }
    }

    /**
     * Draws an educational status banner explaining the exact bus transaction step
     */
    private void drawEducationalBanner(Graphics2D g2d, int x, int y, int width, int height) {
        String statusText;
        Color bannerBg;
        Color bannerBorder;
        Color dotColor;
        Color textColor;

        if (!animationRunning) {
            statusText = "System idle • Waiting for bus activity";
            bannerBg = new Color(30, 41, 59);
            bannerBorder = new Color(51, 65, 85);
            dotColor = new Color(148, 163, 184);
            textColor = new Color(226, 232, 240);
        } else {
            String addrHex = Utils.toHexString0x(animatedAddress);
            String valHex = Utils.toHexString0x(animatedValue);

            if (animatedActivity == CPU.BusActivity.WRITE) {
                bannerBg = new Color(69, 26, 3);
                bannerBorder = new Color(245, 158, 11);
                dotColor = BUS_ADDRESS;
                textColor = new Color(254, 243, 199);

                if (animationProgress < 0.25) {
                    statusText = "RAM Write: CPU outputs address " + addrHex + ", data " + valHex + " and WRITE command...";
                } else if (animationProgress < 0.75) {
                    statusText = "RAM Write: Synchronized transfer across Address, Data and Control buses...";
                } else {
                    statusText = "RAM Write: RAM receives WRITE command and stores value " + valHex + " at address " + addrHex + ".";
                }
            } else if (animatedActivity == CPU.BusActivity.READ) {
                bannerBg = new Color(8, 47, 73);
                bannerBorder = new Color(14, 165, 233);
                dotColor = BUS_DATA;
                textColor = new Color(224, 242, 254);

                if (animationProgress < 0.25) {
                    statusText = "RAM Read (1/2): CPU asserts address " + addrHex + " and READ command...";
                } else if (animationProgress < 0.75) {
                    statusText = "RAM Read (1/2): Address and READ command propagating to RAM...";
                } else if (animationProgress < 1.0) {
                    statusText = "RAM Read (1/2): RAM decodes address " + addrHex + " and extracts content...";
                } else if (animationProgress < 1.25) {
                    statusText = "RAM Read (2/2): RAM places value " + valHex + " onto Data bus...";
                } else if (animationProgress < 1.75) {
                    statusText = "RAM Read (2/2): Routing data " + valHex + " along Data bus to CPU...";
                } else {
                    statusText = "RAM Read (2/2): CPU latches value " + valHex + " into target register.";
                }
            } else if (animatedActivity == CPU.BusActivity.IO_WRITE) {
                bannerBg = new Color(69, 26, 3);
                bannerBorder = new Color(245, 158, 11);
                dotColor = BUS_ADDRESS;
                textColor = new Color(254, 243, 199);
                String pName = getPortName(animatedAddress);

                if (animationProgress < 0.25) {
                    statusText = "I/O Output (OUT): CPU outputs value " + valHex + " to port " + addrHex + " (" + pName + ") with WRITE command...";
                } else if (animationProgress < 0.75) {
                    statusText = "I/O Output (OUT): Routing across buses to PERIPHERALS unit...";
                } else {
                    statusText = "I/O Output (OUT): Peripheral " + pName + " (" + addrHex + ") receives value " + valHex + ".";
                }
            } else if (animatedActivity == CPU.BusActivity.IO_READ) {
                bannerBg = new Color(8, 47, 73);
                bannerBorder = new Color(14, 165, 233);
                dotColor = BUS_DATA;
                textColor = new Color(224, 242, 254);
                String pName = getPortName(animatedAddress);

                if (animationProgress < 0.25) {
                    statusText = "I/O Input (IN - 1/2): CPU polls port " + addrHex + " (" + pName + ") with READ command...";
                } else if (animationProgress < 0.75) {
                    statusText = "I/O Input (IN - 1/2): Port address and READ command propagating to peripheral...";
                } else if (animationProgress < 1.0) {
                    statusText = "I/O Input (IN - 1/2): Peripheral " + pName + " prepares data...";
                } else if (animationProgress < 1.25) {
                    statusText = "I/O Input (IN - 2/2): Port " + addrHex + " places value " + valHex + " onto Data bus...";
                } else if (animationProgress < 1.75) {
                    statusText = "I/O Input (IN - 2/2): Transferring data along Data bus to CPU...";
                } else {
                    statusText = "I/O Input (IN - 2/2): CPU latches value " + valHex + " into AL register.";
                }
            } else { // Pure ALU operation
                bannerBg = new Color(30, 27, 75); // Indigo 950
                bannerBorder = new Color(99, 102, 241); // Indigo 500
                dotColor = new Color(165, 180, 252);
                textColor = new Color(224, 231, 255);

                String op = (animatedAluOp != null) ? animatedAluOp : "Arithmetic/Logic Calculation";
                if (animationProgress < 0.5) {
                    statusText = "ALU Computation (1/2): Routing operands to ALU via Internal bus (" + op + ")...";
                } else {
                    statusText = "ALU Computation (2/2): Latching computed result back to CPU registers (" + op + ").";
                }
            }
        }

        g2d.setColor(bannerBg);
        g2d.fillRoundRect(x, y, width, height, 8, 8);
        g2d.setColor(bannerBorder);
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawRoundRect(x, y, width, height, 8, 8);

        g2d.setColor(dotColor);
        g2d.fillOval(x + 12, y + height / 2 - 4, 8, 8);

        g2d.setFont(statusFont);
        g2d.setColor(textColor);
        g2d.drawString(statusText, x + 28, y + height / 2 + 4);
    }
}

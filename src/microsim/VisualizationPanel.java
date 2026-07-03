package microsim;

import javax.swing.JPanel;
import javax.swing.Timer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
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
 * A panel for visualizing the CPU, RAM, and buses.
 */
public class VisualizationPanel extends JPanel implements ActionListener {

    private static final int PADDING = 20;
    private static final int CPU_WIDTH = 200;
    private static final int RAM_WIDTH = 150;
    private static final int DEV_WIDTH = 150;
    private static final int RECT_HEIGHT = 200;
    private static final int BUS_THICKNESS = 20;
    private static final int BUS_TOP_MARGIN = 130; // Espace pour les bus en haut
    private static final int ANIMATION_DELAY = 16; // approx 60fps
    private static final double ANIMATION_STEP = 0.005; // progress per frame

    private final Font monacoFont = new Font("Monaco", Font.PLAIN, 12);

    private Simulator simulator;

    // Animation fields
    private Timer animationTimer;
    private boolean animationRunning = false;
    private double animationProgress = 0.0;
    private int animatedValue;
    private int animatedAddress;
    private CPU.BusActivity animatedActivity;
    private boolean addressAnimationFinished = false;
    private Runnable onAnimationComplete = null;
    private int ramViewStartAddr = 0;

    public VisualizationPanel(Simulator simulator) {
        super();
        this.simulator = simulator;
        setBackground(Color.WHITE);
        animationTimer = new Timer(ANIMATION_DELAY, this);
    }

    public boolean isAnimating() {
        return animationTimer.isRunning();
    }

    private void startBusAnimation(CPU.BusActivity activity, int value, int address, Runnable onComplete) {
        if (animationTimer.isRunning()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        // Adjust RAM view to show the animated address
        int numLines = (getHeight() - BUS_TOP_MARGIN - 2 * PADDING - 40) / 15;
        if (address < this.ramViewStartAddr || address >= this.ramViewStartAddr + numLines) {
            this.ramViewStartAddr = Math.max(0, address - (numLines / 2)); // Center the address
        }

        this.animatedActivity = activity;
        this.animatedValue = value;
        this.animatedAddress = address;
        this.animationProgress = 0.0;
        this.animationRunning = true;
        this.addressAnimationFinished = false;
        this.onAnimationComplete = onComplete;
        animationTimer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        animationProgress += ANIMATION_STEP;

        double totalDuration = (animatedActivity == CPU.BusActivity.READ) ? 2.0 : 1.0;

        if (animationProgress >= 1.0 && animatedActivity == CPU.BusActivity.READ && !addressAnimationFinished) {
            addressAnimationFinished = true;
        }

        if (animationProgress >= totalDuration) {
            animationProgress = totalDuration;
            animationTimer.stop();
            animationRunning = false;
            simulator.getCPU().resetBusActivity();

            // Notify completion
            if (onAnimationComplete != null) {
                onAnimationComplete.run();
                onAnimationComplete = null; // Consume it
            }
        }
        repaint();
    }
    
    public void updateAndAnimate(Runnable onComplete) {
        CPU cpu = simulator.getCPU();
        boolean shouldAnimate = this.isShowing() && cpu != null && cpu.getLastBusActivity() != CPU.BusActivity.NONE && !animationTimer.isRunning();
        
        if (shouldAnimate) {
            startBusAnimation(
                cpu.getLastBusActivity(), 
                cpu.getLastDataValue(), 
                cpu.getLastMemoryAddress(),
                onComplete
            );
        } else {
            repaint();
            // If there's bus activity but we're not animating, we still need to reset it.
            if (cpu != null && cpu.getLastBusActivity() != CPU.BusActivity.NONE) {
                cpu.resetBusActivity();
            }
            if (onComplete != null) {
                onComplete.run(); // If no animation, run callback immediately
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setFont(monacoFont);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Define component positions (abaissés pour laisser place aux bus en haut)
        int cpuX = PADDING;
        int cpuY = BUS_TOP_MARGIN + PADDING;
        int aluX = PADDING;
        int aluY = cpuY + RECT_HEIGHT + PADDING;
        int ramX = cpuX + 300; // RAM à 300 pixels du bord gauche du CPU
        int ramY = BUS_TOP_MARGIN + PADDING;
        int devX = ramX + 300; // DEVICE à 300 pixels du bord gauche de la RAM
        int devY = BUS_TOP_MARGIN + PADDING;

        // Positions des bus horizontaux en haut
        int busStartX = PADDING;
        int busEndX = getWidth() - PADDING;
        int busLength = busEndX - busStartX;
        int addressBusY = PADDING + 15;
        int dataBusY = addressBusY + BUS_THICKNESS + 10;
        int ctrlBusY = dataBusY + BUS_THICKNESS + 10;

        // Positions des dérivations verticales
        int cpuConnectionX = cpuX + CPU_WIDTH / 2;
        int ramConnectionX = ramX + RAM_WIDTH / 2;
        int devConnectionX = devX + DEV_WIDTH / 2;

        // Draw static components (CPU, ALU, RAM boxes)
        drawStaticComponents(g2d, cpuX, cpuY, aluX, aluY, ramX, ramY, devX, devY);

        // Draw register and RAM values
        drawDynamicValues(g2d, cpuX, cpuY, ramX, ramY);

        // Draw buses and animations
        drawBusesAndAnimation(g2d, cpuX, cpuY, ramX, ramY, devX, devY,
                              busStartX, busEndX, busLength, 
                              addressBusY, dataBusY, ctrlBusY,
                              cpuConnectionX, ramConnectionX, devConnectionX);
    }

    private void drawStaticComponents(Graphics2D g2d, int cpuX, int cpuY, int aluX, int aluY, int ramX, int ramY, int devX, int devY) {

        g2d.setStroke(new BasicStroke(2));

        // Draw CPU Box
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(cpuX, cpuY, CPU_WIDTH, RECT_HEIGHT);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(cpuX, cpuY, CPU_WIDTH, RECT_HEIGHT);
        g2d.drawString("CPU", cpuX + 10, cpuY + 20);

        // Draw ALU Box
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(aluX, aluY, CPU_WIDTH, RECT_HEIGHT / 2);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(aluX, aluY, CPU_WIDTH, RECT_HEIGHT / 2);
        g2d.drawString("ALU", aluX + 10, aluY + 20);

        // Draw RAM Box
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(ramX, ramY, RAM_WIDTH, getHeight() - BUS_TOP_MARGIN - 2 * PADDING);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(ramX, ramY, RAM_WIDTH, getHeight() - BUS_TOP_MARGIN - 2 * PADDING);
        g2d.drawString("RAM", ramX + 10, ramY + 20);

        // Draw DEVICE Box
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(devX, devY, DEV_WIDTH, RECT_HEIGHT);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(devX, devY, DEV_WIDTH, RECT_HEIGHT);
        g2d.drawString("DEVICE", devX + 10, devY + 20);
    }

    private void drawDynamicValues(Graphics2D g2d, int cpuX, int cpuY, int ramX, int ramY) {
        CPU cpu = simulator.getCPU();
        if (cpu == null) return;

        // Draw CPU Registers
        int[] gpr = cpu.getRegisters();
        int regY = cpuY + 40;
        g2d.drawString("AL: " + Utils.toHexString0x(gpr[0]), cpuX + 20, regY); regY += 15;
        g2d.drawString("BL: " + Utils.toHexString0x(gpr[1]), cpuX + 20, regY); regY += 15;
        g2d.drawString("CL: " + Utils.toHexString0x(gpr[2]), cpuX + 20, regY); regY += 15;
        g2d.drawString("DL: " + Utils.toHexString0x(gpr[3]), cpuX + 20, regY); regY = cpuY + 40;
        g2d.drawString("IP: " + Utils.toHexString0x(cpu.getIP()), cpuX + 100, regY); regY += 15;
        g2d.drawString("SP: " + Utils.toHexString0x(cpu.getSP()), cpuX + 100, regY); regY += 15;
        g2d.drawString("SR: " + Utils.toHexString0x(cpu.getSR()), cpuX + 100, regY);

        // Draw RAM Content
        RAM ram = simulator.getRAM();
        int startAddress = this.ramViewStartAddr;
        int numLines = (getHeight() - BUS_TOP_MARGIN - 2 * PADDING - 40) / 15;
        int endAddress = Math.min(ram.getSize() - 1, startAddress + numLines - 1);
        int memY = ramY + 40;
        for (int addr = startAddress; addr <= endAddress; addr++) {
            String line = Utils.toHexString0x(addr) + ": " + Utils.toHexString0x(ram.load(addr));
            boolean shouldHighlightRed = false;
            if (animationRunning && addr == animatedAddress) {
                if (animatedActivity == CPU.BusActivity.WRITE && animationProgress >= 0.9) {
                    shouldHighlightRed = true;
                } else if (animatedActivity == CPU.BusActivity.READ && addressAnimationFinished) {
                    shouldHighlightRed = true;
                }
            }

            if (shouldHighlightRed) {
                g2d.setColor(Color.RED);
            } else if (animationRunning && addr == animatedAddress) {
                g2d.setColor(Color.MAGENTA);
            } else {
                g2d.setColor(Color.BLACK);
            }
            g2d.drawString(" " + line, ramX + 5, memY);
            memY += 15;
        }
        g2d.setColor(Color.BLACK);
    }

    /**
     * Dessine un octet (paquet) animé à la position spécifiée
     * @param g2d Le contexte graphique
     * @param x Position X du centre du paquet
     * @param y Position Y du centre du paquet
     * @param text Texte à afficher dans le paquet (valeur hexadécimale)
     */
    private void drawByte(Graphics2D g2d, int x, int y, String text) {
        int packetWidth = 45; // Largeur du paquet
        int packetHeight = 18; // Hauteur du paquet
        int textXOffset = 9; // Décalage X du texte par rapport au coin supérieur gauche
        int textYOffset = 4; // Décalage Y du texte par rapport au centre vertical
        // Dessiner le rectangle noir
        g2d.setColor(Color.BLACK);
        g2d.fillRect(x - packetWidth / 2, y - packetHeight / 2, packetWidth, packetHeight);
        // Dessiner le texte jaune
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x - packetWidth / 2 + textXOffset, y + textYOffset);
    }

    private void drawBusesAndAnimation(Graphics2D g2d, int cpuX, int cpuY, int ramX, int ramY, int devX, int devY,
                                      int busStartX, int busEndX, int busLength,
                                      int addressBusY, int dataBusY, int ctrlBusY,
                                      int cpuConnectionX, int ramConnectionX, int devConnectionX) {

        Color AdressColor = Color.ORANGE;
        Color DataColor = new Color(51, 204, 255);
        Color CtrlColor = new Color(102, 255, 102);
        
        // Draw Address Bus (horizontal)
        g2d.setColor(AdressColor);
        g2d.fillRect(busStartX, addressBusY - BUS_THICKNESS / 2, busLength, BUS_THICKNESS);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Address", busStartX + 10, addressBusY - BUS_THICKNESS / 2 + 15);

        // Draw Data Bus (horizontal)
        g2d.setColor(DataColor);
        g2d.fillRect(busStartX, dataBusY - BUS_THICKNESS / 2, busLength, BUS_THICKNESS);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Data", busStartX + 10, dataBusY - BUS_THICKNESS / 2 + 15);

        // Draw Ctrl Bus (horizontal)
        g2d.setColor(CtrlColor);
        g2d.fillRect(busStartX, ctrlBusY - BUS_THICKNESS / 2, busLength, BUS_THICKNESS);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Ctrl", busStartX + 10, ctrlBusY - BUS_THICKNESS / 2 + 15);

        int connectionOffset = BUS_THICKNESS * 2; // Décalage pour éviter la superposition des dérivations
        int ySpace = 2; // Espace entre les bus et les composants
        
        // Draw vertical connections from buses to CPU
        g2d.setColor(AdressColor);
        g2d.fillRect(cpuConnectionX - connectionOffset - BUS_THICKNESS / 2, addressBusY + BUS_THICKNESS / 2, 
                     BUS_THICKNESS, cpuY - addressBusY - BUS_THICKNESS / 2 - ySpace);
        
        g2d.setColor(DataColor);
        g2d.fillRect(cpuConnectionX - BUS_THICKNESS / 2, dataBusY + BUS_THICKNESS / 2, 
                     BUS_THICKNESS, cpuY - dataBusY - BUS_THICKNESS / 2 - ySpace);

        g2d.setColor(CtrlColor);
        g2d.fillRect(cpuConnectionX + connectionOffset - BUS_THICKNESS / 2, ctrlBusY + BUS_THICKNESS / 2, 
                     BUS_THICKNESS, cpuY - ctrlBusY - BUS_THICKNESS / 2 - ySpace);

        // Draw vertical connections from buses to RAM
        g2d.setColor(AdressColor);
        g2d.fillRect(ramConnectionX - connectionOffset - BUS_THICKNESS / 2, addressBusY + BUS_THICKNESS / 2, 
                     BUS_THICKNESS, ramY - addressBusY - BUS_THICKNESS / 2 - ySpace);
        
        g2d.setColor(DataColor);
        g2d.fillRect(ramConnectionX - BUS_THICKNESS / 2, dataBusY + BUS_THICKNESS / 2, 
                     BUS_THICKNESS, ramY - dataBusY - BUS_THICKNESS / 2 - ySpace);

        g2d.setColor(CtrlColor);
        g2d.fillRect(ramConnectionX + connectionOffset - BUS_THICKNESS / 2, ctrlBusY + BUS_THICKNESS / 2, 
                     BUS_THICKNESS, ramY - ctrlBusY - BUS_THICKNESS / 2 - ySpace);

        // Draw vertical connections from buses to DEVICE
        g2d.setColor(DataColor);
        g2d.fillRect(devConnectionX - BUS_THICKNESS - BUS_THICKNESS / 2, dataBusY + BUS_THICKNESS / 2, 
                     BUS_THICKNESS, devY - dataBusY - BUS_THICKNESS / 2 - ySpace);
        
        g2d.setColor(CtrlColor);
        g2d.fillRect(devConnectionX + BUS_THICKNESS - BUS_THICKNESS / 2, ctrlBusY + BUS_THICKNESS / 2, 
                     BUS_THICKNESS, devY - ctrlBusY - BUS_THICKNESS / 2 - ySpace);

        // Draw Animation
        if (animationRunning) {
            String addressStr = Utils.toHexString0x(animatedAddress);
            String dataStr = Utils.toHexString0x(animatedValue);

            if (animatedActivity == CPU.BusActivity.WRITE) {
                // WRITE: Address et data se déplacent ensemble du CPU vers la RAM
                double progress = Math.min(1.0, animationProgress);
                
                if (progress < 0.25) {
                    // Phase 1: Montée verticale du CPU vers le bus (0.0 -> 0.25)
                    double verticalProgress = progress / 0.25;
                    int currentY = cpuY - (int)((cpuY - addressBusY) * verticalProgress);
                    
                    // Address packet (dérivation gauche)
                    drawByte(g2d, cpuConnectionX - connectionOffset, currentY, addressStr);
                    
                    // Data packet (dérivation centrale)
                    int dataCurrentY = cpuY - (int)((cpuY - dataBusY) * verticalProgress);
                    drawByte(g2d, cpuConnectionX, dataCurrentY, dataStr);
                    
                } else if (progress < 0.75) {
                    // Phase 2: Déplacement horizontal sur le bus (0.25 -> 0.75)
                    double horizontalProgress = (progress - 0.25) / 0.5;
                    int addrCurrentX = (cpuConnectionX - connectionOffset) + (int)(((ramConnectionX - connectionOffset) - (cpuConnectionX - connectionOffset)) * horizontalProgress);
                    int dataCurrentX = cpuConnectionX + (int)((ramConnectionX - cpuConnectionX) * horizontalProgress);
                    
                    // Address packet
                    drawByte(g2d, addrCurrentX, addressBusY, addressStr);
                    
                    // Data packet
                    drawByte(g2d, dataCurrentX, dataBusY, dataStr);
                    
                } else {
                    // Phase 3: Descente verticale du bus vers la RAM (0.75 -> 1.0)
                    double verticalProgress = (progress - 0.75) / 0.25;
                    int currentY = addressBusY + (int)((ramY - addressBusY) * verticalProgress);
                    
                    // Address packet (dérivation gauche)
                    drawByte(g2d, ramConnectionX - connectionOffset, currentY, addressStr);
                    
                    // Data packet (dérivation centrale)
                    int dataCurrentY = dataBusY + (int)((ramY - dataBusY) * verticalProgress);
                    drawByte(g2d, ramConnectionX, dataCurrentY, dataStr);
                }
                
            } else if (animatedActivity == CPU.BusActivity.READ) {
                // READ: Address va du CPU à la RAM, puis data revient de la RAM au CPU
                
                if (animationProgress < 1.0) {
                    // Phase 1: Address se déplace du CPU vers la RAM (0.0 -> 1.0)
                    double progress = animationProgress;
                    
                    if (progress < 0.25) {
                        // Montée verticale du CPU vers le bus (dérivation gauche pour adresse)
                        double verticalProgress = progress / 0.25;
                        int currentY = cpuY - (int)((cpuY - addressBusY) * verticalProgress);
                        drawByte(g2d, cpuConnectionX - connectionOffset, currentY, addressStr);
                        
                    } else if (progress < 0.75) {
                        // Déplacement horizontal sur le bus d'adresse
                        double horizontalProgress = (progress - 0.25) / 0.5;
                        int currentX = (cpuConnectionX - connectionOffset) + (int)(((ramConnectionX - connectionOffset) - (cpuConnectionX - connectionOffset)) * horizontalProgress); 
                        drawByte(g2d, currentX, addressBusY, addressStr);
                        
                    } else {
                        // Descente verticale du bus vers la RAM (dérivation gauche)
                        double verticalProgress = (progress - 0.75) / 0.25;
                        int currentY = addressBusY + (int)((ramY - addressBusY) * verticalProgress);
                        drawByte(g2d, ramConnectionX - connectionOffset, currentY, addressStr);
                    }
                    
                } else {
                    // Phase 2: Data revient de la RAM vers le CPU (1.0 -> 2.0)
                    double dataProgress = animationProgress - 1.0;
                    
                    if (dataProgress < 0.25) {
                        // Montée verticale de la RAM vers le bus (dérivation centrale pour données)
                        double verticalProgress = dataProgress / 0.25;
                        int currentY = ramY - (int)((ramY - dataBusY) * verticalProgress);
                        drawByte(g2d, ramConnectionX, currentY, dataStr);
                        
                    } else if (dataProgress < 0.75) {
                        // Déplacement horizontal sur le bus de données (RAM vers CPU)
                        double horizontalProgress = (dataProgress - 0.25) / 0.5;
                        int currentX = ramConnectionX - (int)((ramConnectionX - cpuConnectionX) * horizontalProgress);
                        drawByte(g2d, currentX, dataBusY, dataStr);
                        
                    } else {
                        // Descente verticale du bus vers le CPU (dérivation centrale)
                        double verticalProgress = (dataProgress - 0.75) / 0.25;
                        int currentY = dataBusY + (int)((cpuY - dataBusY) * verticalProgress);
                        drawByte(g2d, cpuConnectionX, currentY, dataStr);
                    }
                }
            }
        }
    }
}

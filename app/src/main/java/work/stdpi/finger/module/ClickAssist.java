package work.stdpi.finger.module;

import work.stdpi.finger.util.Lwjgl;

import java.awt.AWTException;
import java.awt.Robot;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ClickAssist module (work.stdpi.finger namespace).
 *
 * Spec:
 * - When enabled and the game is in in-game state (no GUI open), on a left click press
 *   attempt one extra click with a configured probability.
 * - Targets Minecraft 1.8.9. Uses LWJGL 2 Mouse APIs via reflection to avoid classpath deps.
 * - Configuration via simple runtime commands printed to stdout (future: in-chat commands).
 */
public final class ClickAssist implements Runnable {
    private static final ClickAssist INSTANCE = new ClickAssist();

    public static ClickAssist get() { return INSTANCE; }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final SecureRandom rng = new SecureRandom();
    private volatile double chance = 0.5; // 50% default

    private Thread worker;
    private Robot robot;

    // Edge detection state
    private boolean prevLeftDown = false;

    private ClickAssist() {}

    public void start() {
        if (running.getAndSet(true)) return;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.err.println("[FingerAgent] Failed to init Robot: " + e);
            running.set(false);
            return;
        }
        worker = new Thread(this, "Finger-ClickAssist");
        worker.setDaemon(true);
        worker.start();
        System.out.println("[FingerAgent] ClickAssist enabled: " + enabled.get() + ", chance=" + (int)(chance*100) + "%");
    }

    public void stop() {
        running.set(false);
        if (worker != null) worker.interrupt();
        worker = null;
        robot = null;
    }

    public void setEnabled(boolean v) { enabled.set(v); }
    public boolean isEnabled() { return enabled.get(); }

    public void setChance(double probability) {
        if (Double.isNaN(probability)) return;
        if (probability < 0) probability = 0;
        if (probability > 1) probability = 1;
        this.chance = probability;
        System.out.println("[FingerAgent] ClickAssist chance set to " + (int)(this.chance*100) + "%");
    }

    public double getChance() { return chance; }

    @Override
    public void run() {
        // Poll at a modest rate; edge detection avoids consuming LWJGL event queue.
        while (running.get()) {
            try {
                loopOnce();
                // Sleep a few ms to avoid CPU burn; LWJGL sampling is cheap.
                Thread.sleep(2L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                // Never crash the agent thread; log and continue.
                System.err.println("[FingerAgent] ClickAssist loop error: " + t);
            }
        }
    }

    private void loopOnce() {
        // Ensure LWJGL Mouse is present and created.
        if (!Lwjgl.isMouseCreated()) {
            prevLeftDown = false;
            return;
        }

        // Only run when game has grabbed the mouse (in-game, no GUI open).
        if (!Lwjgl.isMouseGrabbed()) {
            prevLeftDown = false;
            return;
        }

        boolean leftDown = Lwjgl.isMouseButtonDown(0);

        if (enabled.get() && !prevLeftDown && leftDown) {
            // Left button just pressed; attempt extra click by probability.
            if (rng.nextDouble() < chance) {
                // Perform a quick release+press to register an extra click.
                // BUTTON1_MASK is 16.
                try {
                    robot.mouseRelease(16);
                    // A tiny delay helps avoid coalescing with the original event.
                    Thread.sleep(1L);
                    robot.mousePress(16);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (Throwable t) {
                    System.err.println("[FingerAgent] Robot click error: " + t);
                }
            }
        }

        prevLeftDown = leftDown;
    }

    // Simple text command interface (to be wired to chat in a future step).
    // Accepts commands like: "clickassist on", "clickassist off", "clickassist chance 65".
    public boolean handleCommand(String raw) {
        if (raw == null) return false;
        String s = raw.trim();
        if (!s.toLowerCase(Locale.ROOT).startsWith("clickassist")) return false;
        String[] parts = s.split("\\s+");
        if (parts.length == 1) {
            System.out.println("[FingerAgent] ClickAssist: on=" + enabled.get() + ", chance=" + (int)(chance*100) + "%");
            return true;
        }
        if ("on".equalsIgnoreCase(parts[1]) || "enable".equalsIgnoreCase(parts[1])) {
            setEnabled(true);
            return true;
        }
        if ("off".equalsIgnoreCase(parts[1]) || "disable".equalsIgnoreCase(parts[1])) {
            setEnabled(false);
            return true;
        }
        if ("chance".equalsIgnoreCase(parts[1]) && parts.length >= 3) {
            try {
                double pct = Double.parseDouble(parts[2]);
                setChance(pct / 100.0);
            } catch (NumberFormatException nfe) {
                System.out.println("[FingerAgent] Invalid chance: " + parts[2]);
            }
            return true;
        }
        System.out.println("[FingerAgent] Usage: clickassist [on|off|chance <0-100>]");
        return true;
    }
}


package work.stdpi.finger.agent;

import work.stdpi.finger.module.ClickAssist;

import java.lang.instrument.Instrumentation;

/**
 * Java agent entrypoint. Boots lightweight modules without touching MC bytecode.
 */
public final class FingerAgent {
    private static volatile boolean started;

    public static void premain(String args, Instrumentation inst) {
        start();
    }

    public static void agentmain(String args, Instrumentation inst) {
        start();
    }

    private static synchronized void start() {
        if (started) return;
        started = true;
        // Start ClickAssist module
        ClickAssist.get().start();
        System.out.println("[FingerAgent] ClickAssist module started.");
    }
}


package work.stdpi.finger.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Thin reflective wrappers around LWJGL 2 Mouse methods so we don't need
 * compile-time dependencies. This ensures the agent can attach to a live
 * 1.8.9 client without classpath setup.
 */
public final class Lwjgl {
    private static volatile Class<?> mouseCls;
    private static volatile Method isCreatedM;
    private static volatile Method isGrabbedM;
    private static volatile Method isButtonDownM;

    private static void ensure() {
        if (mouseCls != null) return;
        try {
            mouseCls = Class.forName("org.lwjgl.input.Mouse");
            isCreatedM = mouseCls.getMethod("isCreated");
            isGrabbedM = mouseCls.getMethod("isGrabbed");
            isButtonDownM = mouseCls.getMethod("isButtonDown", int.class);
        } catch (Throwable t) {
            // Not available yet
            mouseCls = null;
            isCreatedM = null;
            isGrabbedM = null;
            isButtonDownM = null;
        }
    }

    public static boolean isMouseCreated() {
        ensure();
        if (isCreatedM == null) return false;
        try {
            return (boolean) isCreatedM.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    public static boolean isMouseGrabbed() {
        ensure();
        if (isGrabbedM == null) return false;
        try {
            return (boolean) isGrabbedM.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    public static boolean isMouseButtonDown(int btn) {
        ensure();
        if (isButtonDownM == null) return false;
        try {
            return (boolean) isButtonDownM.invoke(null, btn);
        } catch (IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }
}


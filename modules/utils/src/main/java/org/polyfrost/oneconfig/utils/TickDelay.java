package org.polyfrost.oneconfig.utils;

@SuppressWarnings("unused")
public final class TickDelay {
    // TickDelay.h -- version specific workaround
    // see versions/src/main/java/org/polyfrost/oneconfig/utils/TickDelay.java for the real implementation
    private TickDelay() {}

    /**
     * Schedules a Runnable to be called after a certain amount of ticks.
     * <p>
     * If the amount of ticks is below 1, the Runnable will be called immediately.
     */
    public static void of(int ticks, Runnable function) {}
}

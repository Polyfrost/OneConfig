package cc.polyfrost.oneconfig.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * A simple class that can be used to profile code.
 */
public final class SimpleProfiler {
    static final HashMap<String, Long> startTimes = new HashMap<>();
    static final Logger LOGGER = LogManager.getLogger("OneConfig Profiler");

    /**
     * Push a profiler start time to the map. <br>
     *
     * @param msg the key for this tracker.
     * @return true if the key was not already in the map.
     * @see #pop(String)
     */
    public static boolean push(@NotNull final String msg) {
        return startTimes.put(msg, System.nanoTime()) == null;
    }

    /**
     * Pop a profiler start time from the map, and print the time to the log in the format: <br>
     * {@code [OneConfig Profiler/INFO] <msg> took <time>ms}
     *
     * @param msg the key that was used for {@link #push(String)}. This will also be used for the message (above)
     * @return the time (in milliseconds) since the push was called.
     * @see #push(String)
     */
    public static float pop(@NotNull final String msg) {
        return pop(msg, Level.DEBUG);
    }

    /**
     * Pop a profiler start time from the map, and print the time to the log in the format: <br>
     * {@code [OneConfig Profiler/<level>] <msg> took <time>ms}
     *
     * @param msg   the key that was used for {@link #push(String)}. This will also be used for the message (above)
     * @param level the log level to use.
     * @return the time (in milliseconds) since the push was called.
     * @see #push(String)
     */
    public static float pop(@NotNull final String msg, @NotNull Level level) {
        final float time = (System.nanoTime() - startTimes.remove(msg)) / 1000000f;
        LOGGER.log(level, msg + " took " + time + "ms");
        return time;
    }
}

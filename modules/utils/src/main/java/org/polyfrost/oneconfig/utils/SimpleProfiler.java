/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.utils;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.HashMap;

/**
 * A simple class that can be used to profile code.
 */
public final class SimpleProfiler {
    static final HashMap<String, Long> startTimes = new HashMap<>();
    static final Logger LOGGER = LoggerFactory.getLogger("OneConfig Profiler");

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
        LOGGER.atLevel(level).log(msg + " took " + time + "ms");
        return time;
    }
}

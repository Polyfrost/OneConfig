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

package org.polyfrost.oneconfig.utils.v1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows for easy multithreading
 * <p>
 * Modified from Seraph by Scherso under LGPL-2.1
 * <a href="https://github.com/Scherso/Seraph/blob/master/LICENSE">https://github.com/Scherso/Seraph/blob/master/LICENSE</a>
 * </p>
 */
public final class Multithreading {
    private Multithreading() {
    }

    /**
     * Runs the provided runnables asynchronously
     *
     * @param runnables The runnables to run
     * @see Multithreading#submit(Runnable)
     */
    public static void submit(Runnable... runnables) {
        for (Runnable runnable : runnables) {
            submit(runnable);
        }
    }

    /**
     * Submits the Runnable to the executor so it runs asynchronously
     *
     * @param runnable The runnable to run
     * @return The future representing the submitted runnable
     * @see ExecutorService#submit(Runnable)
     */
    public static Future<?> submit(Runnable runnable) {
        return getExecutor().submit(runnable);
    }

    /**
     * Schedules the runnable to run asynchronously after the specified delay
     *
     * @param runnable The runnable to run
     * @param delay    The delay before the runnable is run
     * @param timeUnit The {@link TimeUnit} of the delay
     * @see Multithreading#submitScheduled(Runnable, long, TimeUnit)
     */
    public static void schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        submitScheduled(runnable, delay, timeUnit);
    }

    /**
     * Submits the Runnable to the executor after a delay so it runs asynchronously
     *
     * @param runnable The runnable to run
     * @param delay    The delay before the runnable is run
     * @param timeUnit The {@link TimeUnit} of the delay
     * @return The future representing the submitted runnable
     * @see ScheduledExecutorService#schedule(Runnable, long, TimeUnit)
     */
    public static ScheduledFuture<?> submitScheduled(Runnable runnable, long delay, TimeUnit timeUnit) {
        return getScheduledExecutor().schedule(runnable, delay, timeUnit);
    }

    public static ExecutorService getExecutor() {
        return SharedPool.INSTANCE;
    }

    public static ScheduledExecutorService getScheduledExecutor() {
        return ScheduledPool.INSTANCE;
    }

    private static final class SharedPool {
        static final ExecutorService INSTANCE = Executors.newCachedThreadPool(threadFactory("OneConfig-"));
    }

    private static final class ScheduledPool {
        static final ScheduledExecutorService INSTANCE = Executors.newScheduledThreadPool(
                Math.max(1, Runtime.getRuntime().availableProcessors() - 2),
                threadFactory("OneConfig-Scheduled-"));
    }

    private static ThreadFactory threadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName(prefix + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }
}
package cc.polyfrost.oneconfig.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

/**
 * Allows for easy multithreading.
 * <p>
 * Taken from Seraph by Scherso under LGPL-2.1
 * <a href="https://github.com/Scherso/Seraph/blob/master/LICENSE">https://github.com/Scherso/Seraph/blob/master/LICENSE</a>
 * </p>
 */
public class Multithreading {
    private static final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("OneConfig-%d").build());
    private static final ScheduledExecutorService runnableExecutor = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() + 1);

    public static void runAsync(Runnable runnable) {
        executorService.submit(runnable);
    }

    public static void runAsync(Runnable... runnables) {
        for (Runnable runnable : runnables) {
            runAsync(runnable);
        }
    }

    public static Future<?> submit(Runnable runnable) {
        return executorService.submit(runnable);
    }

    public static ScheduledFuture<?> submitScheduled(Runnable runnable, long delay, TimeUnit timeUnit) {
        return runnableExecutor.schedule(runnable, delay, timeUnit);
    }

    public static void schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        runnableExecutor.schedule(runnable, delay, timeUnit);
    }
}

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

    /**
     * Runs the runnable asynchronously.
     *
     * @param runnable The runnable to run.
     * @see Multithreading#submit(Runnable)
     */
    public static void runAsync(Runnable runnable) {
        submit(runnable);
    }

    /**
     * Runs the provided runnables asynchronously.
     *
     * @param runnables The runnables to run.
     * @see Multithreading#runAsync(Runnable)
     */
    public static void runAsync(Runnable... runnables) {
        for (Runnable runnable : runnables) {
            runAsync(runnable);
        }
    }

    /**
     * Submits the Runnable to the executor, making it run asynchronously.
     *
     * @param runnable The runnable to run.
     * @return The future representing the submitted runnable.
     * @see ExecutorService#submit(Runnable)
     */
    public static Future<?> submit(Runnable runnable) {
        return executorService.submit(runnable);
    }

    /**
     * Schedules the runnable to run asynchronously after the specified delay.
     *
     * @param runnable The runnable to run.
     * @param delay    The delay before the runnable is run.
     * @param timeUnit The {@link TimeUnit} of the delay.
     * @see Multithreading#submitScheduled(Runnable, long, TimeUnit)
     */
    public static void schedule(Runnable runnable, long delay, TimeUnit timeUnit) {
        submitScheduled(runnable, delay, timeUnit);
    }

    /**
     * Submits the Runnable to the executor after a delay, making it run asynchronously.
     *
     * @param runnable The runnable to run.
     * @param delay    The delay before the runnable is run.
     * @param timeUnit The {@link TimeUnit} of the delay.
     * @return The future representing the submitted runnable.
     * @see ScheduledExecutorService#schedule(Runnable, long, TimeUnit)
     */
    public static ScheduledFuture<?> submitScheduled(Runnable runnable, long delay, TimeUnit timeUnit) {
        return runnableExecutor.schedule(runnable, delay, timeUnit);
    }
}

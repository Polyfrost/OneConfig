package cc.polyfrost.oneconfig.utils;

import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Similar to Multithreading, but using a thread pool instead.
 */

public final class ThreadUtils {
    public static final ThreadUtils INSTANCE = new ThreadUtils();
    private static final AtomicInteger threadCounter = new AtomicInteger(0);
    private static final ScheduledExecutorService scheduledExecutor;
    private static ThreadPoolExecutor poolExecutor;

    private ThreadUtils() {
    }

    public static ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public void setPoolExecutor(@NotNull ThreadPoolExecutor setPool) {
        poolExecutor = setPool;
    }

    public static ThreadPoolExecutor getPoolInstance() {
        ThreadUtils threadPool = INSTANCE;
        return poolExecutor;
    }

    public ThreadPoolExecutor getPoolExecutor() {
        return poolExecutor;
    }

    @NotNull
    public ScheduledFuture<?> schedule(@NotNull Runnable runnable, long initialDelay, long period, @NotNull TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(runnable, initialDelay, period, unit);
    }

    @NotNull
    public static ScheduledFuture<?> schedule(@NotNull Runnable runnable, long period, @NotNull TimeUnit unit) {
        ThreadUtils threadPool = INSTANCE;
        return scheduledExecutor.schedule(runnable, period, unit);
    }

    @NotNull
    public Future<?> submit(@NotNull Runnable runnable) {
        return poolExecutor.submit(runnable);
    }

    public static void runAsync(@NotNull Runnable runnable) {
        ThreadUtils threadPool = INSTANCE;
        poolExecutor.execute(runnable);
    }

    private static Thread scheduledPool(Runnable runnable) {
        return new Thread(runnable, Intrinsics.stringPlus("OneConfig Thread ", threadCounter.incrementAndGet()));
    }

    private static Thread threadPool(Runnable runnable) {
        String threadString = "OneConfig %s";
        Object[] counterNum = new Object[]{threadCounter.incrementAndGet()};
        String formattedString = String.format(threadString, Arrays.copyOf(counterNum, counterNum.length));
        return new Thread(runnable, formattedString);
    }

    static {
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(10, ThreadUtils::scheduledPool);
        scheduledExecutor = scheduledThreadPool;
        poolExecutor = new ThreadPoolExecutor(10, 30, 0L, TimeUnit.SECONDS, (BlockingQueue)(new LinkedBlockingQueue()), ThreadUtils::threadPool);
    }
}

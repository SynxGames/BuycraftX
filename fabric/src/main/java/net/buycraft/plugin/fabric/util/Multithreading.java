package net.buycraft.plugin.fabric.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

public class Multithreading {

    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(10,
            new ThreadFactoryBuilder()
                    .setNameFormat("Tebex-Async-%d")
                    .setDaemon(true)
                    .build()
    );

    public static ScheduledFuture<?> schedule(Runnable r, long initialDelay, long delay, TimeUnit unit) {
        return EXECUTOR_SERVICE.scheduleAtFixedRate(r, initialDelay, delay, unit);
    }

    public static ScheduledFuture<?> schedule(Runnable r, long delay, TimeUnit unit) {
        return EXECUTOR_SERVICE.schedule(r, delay, unit);
    }

    public static Executor delayedExecutor(long delay, TimeUnit unit) {
        return task -> schedule(task, delay, unit);
    }

    public static void runAsync(Runnable runnable) {
        EXECUTOR_SERVICE.execute(runnable);
    }

    public static Future<?> submit(Runnable runnable) {
        return EXECUTOR_SERVICE.submit(runnable);
    }
}

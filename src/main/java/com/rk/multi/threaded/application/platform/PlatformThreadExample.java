package com.rk.multi.threaded.application.platform;

import com.rk.multi.threaded.application.util.Utility;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PlatformThreadExample {

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        try (ExecutorService executor = Executors.newFixedThreadPool(100)) {
            try (ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor()) {
                monitor.scheduleAtFixedRate(() -> Utility.logSystemUsage("Periodic"), 0, 100, TimeUnit.MILLISECONDS);
                Runnable task = Utility.getRunnableTask();

                for (int i = 0; i < 1000; i++) {
                    executor.submit(task);
                }

                executor.shutdown();
                while (!executor.isTerminated()) {
                    Thread.sleep(10);
                }
            }
        }

        long end = System.currentTimeMillis();
        System.out.println("Platform Threads with Executor Service Total Time: " + (end - start) + " ms");
    }
}

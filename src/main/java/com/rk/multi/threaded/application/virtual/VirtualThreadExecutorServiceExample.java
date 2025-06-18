package com.rk.multi.threaded.application.virtual;

import com.rk.multi.threaded.application.util.Utility;

import java.util.concurrent.*;

public class VirtualThreadExecutorServiceExample {

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        try (ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor()) {
            monitor.scheduleAtFixedRate(() -> Utility.logSystemUsage("Periodic"), 0, 100, TimeUnit.MILLISECONDS);
            try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
                Runnable task = Utility.getRunnableTask();
                for (int i = 0; i < 1000; i++) {
                    executorService.submit(task);
                }
                executorService.shutdown();
                while (!executorService.isTerminated()) {
                    Thread.sleep(10);
                }
            }

        }

        long end = System.currentTimeMillis();
        System.out.println("Virtual Threads Total Time: " + (end - start) + " ms");
    }
}

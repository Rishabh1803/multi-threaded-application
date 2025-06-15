package com.rk.multi.threaded.application.platform;

import com.rk.multi.threaded.application.util.Utility;

import java.util.concurrent.*;

public class PlatformLatchExample {

    public static void main(String[] args) throws InterruptedException {
        int tasks = 1000;
        CountDownLatch latch = new CountDownLatch(tasks);

        long start;
        try (ExecutorService executor = Executors.newFixedThreadPool(100)) {
            try (ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor()) {
                monitor.scheduleAtFixedRate(() -> Utility.logSystemUsage("Periodic"), 0, 100, TimeUnit.MILLISECONDS);
                start = System.currentTimeMillis();

                for (int i = 0; i < tasks; i++) {
                    executor.execute(() -> {
                        try {
                            Thread.sleep(500); // I/O
                            for (int j = 0; j < 100_000; j++) {
                                double x = Math.sqrt(j);
                            } // CPU
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await();
                executor.shutdown();
                monitor.shutdown();
            }
        }

        long end = System.currentTimeMillis();
        System.out.println("Platform Threads with Latch Time: " + (end - start) + " ms");
    }
}

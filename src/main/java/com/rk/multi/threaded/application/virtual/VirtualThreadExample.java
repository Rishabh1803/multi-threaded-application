package com.rk.multi.threaded.application.virtual;

import com.rk.multi.threaded.application.util.Utility;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class VirtualThreadExample {
    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        try (ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor()) {
            monitor.scheduleAtFixedRate(() -> Utility.logSystemUsage("Periodic"), 0, 100, TimeUnit.MILLISECONDS);
            ThreadFactory factory = Thread.ofVirtual().factory();

            Runnable task = Utility.getRunnableTask();

            Thread[] threads = new Thread[1000];
            for (int i = 0; i < 1000; i++) {
                threads[i] = factory.newThread(task);
                threads[i].start();
            }

            for (Thread t : threads) {
                t.join();
            }
        }

        long end = System.currentTimeMillis();
        System.out.println("Virtual Threads Total Time: " + (end - start) + " ms");
    }
}

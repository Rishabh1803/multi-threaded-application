package com.rk.multi.threaded.application.util;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class Utility {

    public static Runnable getRunnableTask() {
        return () -> {
            try {
                Thread.sleep(500);
                for (int i = 0; i < 100_000; i++) { double x = Math.sqrt(i); }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    public static void logSystemUsage(String phase) {
        Runtime runtime = Runtime.getRuntime();
        int mb = 1024 * 1024;

        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / mb;
        int availableProcessors = runtime.availableProcessors();

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double systemLoad = osBean.getSystemLoadAverage();

        System.out.printf("[%s] Used Memory: %d MB | Available CPUs: %d | System Load Avg: %.2f%n", phase, usedMemory, availableProcessors, systemLoad);
    }
}

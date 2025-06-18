package com.rk.multi.threaded.application.benchmark;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyBenchmark {

    static final int TOTAL_TASKS = 100;
    static final int MAX_CONCURRENT_CALLS = 10;
    static final int MAX_RETRIES = 3;
    static final Duration LOG_INTERVAL = Duration.ofSeconds(3);

    static final HttpClient client = HttpClient.newHttpClient();
    static final AtomicInteger successCount = new AtomicInteger();
    static final AtomicInteger failureCount = new AtomicInteger();

    static final ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws InterruptedException {
        String mode = args.length > 0 ? args[0] : "virtual"; // "platform" or "virtual"

        ExecutorService executor = mode.equalsIgnoreCase("platform")
                ? Executors.newFixedThreadPool(MAX_CONCURRENT_CALLS)
                : Executors.newVirtualThreadPerTaskExecutor();

        Semaphore semaphore = new Semaphore(MAX_CONCURRENT_CALLS);

        monitor.scheduleAtFixedRate(ConcurrencyBenchmark::logResourceUsage, 0, LOG_INTERVAL.getSeconds(), TimeUnit.SECONDS);

        long startTime = System.nanoTime();

        List<CompletableFuture<Void>> futures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < TOTAL_TASKS; i++) {
            int taskId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    semaphore.acquire();
                    fetchWithRetryAsync(taskId, MAX_RETRIES).join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        monitor.shutdownNow();

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);

        long duration = System.nanoTime() - startTime;

        System.out.println("\n======= Benchmark Results =======");
        System.out.printf("Thread Mode      : %s%n", mode);
        System.out.printf("Total Tasks      : %d%n", TOTAL_TASKS);
        System.out.printf("Successful Calls : %d%n", successCount.get());
        System.out.printf("Failed Calls     : %d%n", failureCount.get());
        System.out.printf("Total Time Taken : %.2f sec%n", duration / 1_000_000_000.0);
        System.out.printf("Throughput       : %.2f req/sec%n",
                TOTAL_TASKS / (duration / 1_000_000_000.0));
    }

    private static CompletableFuture<Void> fetchWithRetryAsync(int taskId, int retriesLeft) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dummyjson.com/products?limit=100"))
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        System.out.printf("✅ Task %d OK (len: %d)%n", taskId, response.body().length());
                        successCount.incrementAndGet();
                    } else {
                        System.err.printf("❌ Task %d failed: %d%n", taskId, response.statusCode());
                        if (retriesLeft > 0) retry(taskId, retriesLeft);
                        else failureCount.incrementAndGet();
                    }
                })
                .exceptionally(ex -> {
                    System.err.printf("❌ Task %d error: %s%n", taskId, ex.getMessage());
                    if (retriesLeft > 0) retry(taskId, retriesLeft);
                    else failureCount.incrementAndGet();
                    return null;
                });
    }

    private static void retry(int taskId, int retriesLeft) {
        try {
            long backoff = (long) Math.pow(2, MAX_RETRIES - retriesLeft) * 200;
            Thread.sleep(backoff);
            fetchWithRetryAsync(taskId, retriesLeft - 1).join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void logResourceUsage() {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        long usedHeap = heap.getUsed() / (1024 * 1024);
        long maxHeap = heap.getMax() / (1024 * 1024);
        double load = osBean.getSystemLoadAverage();

        System.out.printf("📊 Heap: %d MB used / %d MB max | CPU Load: %.2f%n", usedHeap, maxHeap, load);
    }
}
package academy.backend.market_pulse.demo.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Запускать явно из IDE или через -Dtest=SynchronizationExercises.
 */
@Timeout(10)
class SynchronizationExercises {
    @Test
    void preservesAllConcurrentUpdates() throws Exception {
        var counter = new RequestCounter();
        int threads = 8;
        int updates = 10_000;
        var ready = new CountDownLatch(threads);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    for (int j = 0; j < updates; j++) {
                        counter.record("shared");
                    }
                    return null;
                }));
            }
            try {
                assertTrue(ready.await(3, TimeUnit.SECONDS));
            } finally {
                start.countDown();
            }
            for (Future<?> task : tasks) {
                task.get(3, TimeUnit.SECONDS);
            }
        }
        assertEquals(threads * updates, counter.counts.get("shared"));
    }

    private static <T> T awaitItem(java.util.concurrent.BlockingQueue<T> queue)
            throws InterruptedException {
        return queue.take();
    }

    @Test
    void consumesItemPublishedAfterConsumerStarts() throws Exception {
        var queue = new java.util.concurrent.ArrayBlockingQueue<String>(1);
        var started = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var result = pool.submit(() -> {
                started.countDown();
                return awaitItem(queue);
            });
            try {
                assertTrue(started.await(2, TimeUnit.SECONDS));
                org.junit.jupiter.api.Assertions.assertThrows(
                        java.util.concurrent.TimeoutException.class,
                        () -> result.get(100, TimeUnit.MILLISECONDS));
                queue.add("task");
                assertEquals("task", result.get(2, TimeUnit.SECONDS));
            } finally {
                result.cancel(true);
            }
        }
    }

    @Test
    void interruptsWaitingConsumer() throws Exception {
        var queue = new java.util.concurrent.ArrayBlockingQueue<String>(1);
        var entered = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var task = pool.submit(() -> {
                entered.countDown();
                try {
                    awaitItem(queue);
                } catch (InterruptedException e) {
                    interrupted.countDown();
                    Thread.currentThread().interrupt();
                }
            });
            try {
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                task.cancel(true);
                assertTrue(interrupted.await(1, TimeUnit.SECONDS));
            } finally {
                task.cancel(true);
            }
        }
    }

}

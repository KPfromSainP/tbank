package academy.backend.market_pulse.demo.concurrency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Запускать явно из IDE или через -Dtest=ExecutorExercises.
 */
@Timeout(10)
class ExecutorExercises {
    private static <T> Future<T> start(ExecutorService executor, Callable<T> task) {
        return executor.submit(task);
    }

    private static void stop(Future<?> task) {
        task.cancel(true);
    }

    @Test
    void returnsValueAndPreservesTaskFailure() throws Exception {
        try (var pool = Executors.newSingleThreadExecutor()) {
            assertEquals(42, start(pool, () -> 42).get(2, TimeUnit.SECONDS));
            var failed = start(pool, () -> {
                throw new IllegalStateException("boom");
            });
            var error = assertThrows(ExecutionException.class,
                    () -> failed.get(2, TimeUnit.SECONDS));
            assertInstanceOf(IllegalStateException.class, error.getCause());
        }
    }

    @Test
    void cancellationInterruptsRunningTask() throws Exception {
        var entered = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var task = pool.submit(() -> {
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    interrupted.countDown();
                    throw e;
                }
                return null;
            });
            try {
                assertTrue(entered.await(2, TimeUnit.SECONDS));
                stop(task);
                assertThrows(CancellationException.class, task::get);
                assertTrue(interrupted.await(1, TimeUnit.SECONDS));
            } finally {
                release.countDown();
            }
        }
    }
}

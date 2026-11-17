package academy.backend.market_pulse.demo.concurrency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(5)
class ExecutorFrameworkDemoTest {
    @Test
    @DisplayName("Callable/Future: результат и исключение доступны через get")
    void resultAndFailure() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var success = pool.submit(() -> 300);
            var failure = pool.submit(() -> { throw new IllegalStateException("API недоступен"); });
            assertEquals(300, success.get(2, TimeUnit.SECONDS));
            var error = assertThrows(ExecutionException.class, () -> failure.get(2, TimeUnit.SECONDS));
            assertInstanceOf(IllegalStateException.class, error.getCause());
            // FutureTask сохранил ошибку: UncaughtExceptionHandler её не получит.
            assertEquals(301, pool.submit(() -> 301).get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    @DisplayName("Таймаут get не отменяет загрузку; cancel(true) запрашивает interrupt")
    void timeoutThenCancel() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var result = pool.submit(() -> {
                started.countDown();
                try {
                    new CountDownLatch(1).await();
                } catch (InterruptedException error) {
                    interrupted.countDown();
                    throw error;
                }
                return 300;
            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> result.get(10, TimeUnit.MILLISECONDS));
            assertFalse(result.isDone());
            assertTrue(result.cancel(true));
            assertThrows(CancellationException.class, result::get);
            assertTrue(interrupted.await(2, TimeUnit.SECONDS));
        }
    }

    @Test
    @DisplayName("ThreadPoolExecutor: заняты worker и очередь — новая задача отклонена")
    void boundedPoolRejectsOverload() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try (var pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.AbortPolicy())) {
            try {
                var first = pool.submit(() -> { started.countDown(); release.await(); return 1; });
                assertTrue(started.await(2, TimeUnit.SECONDS));
                var queued = pool.submit(() -> 2);
                assertThrows(RejectedExecutionException.class, () -> pool.submit(() -> 3));
                release.countDown();
                assertEquals(1, first.get(2, TimeUnit.SECONDS));
                assertEquals(2, queued.get(2, TimeUnit.SECONDS));
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    @DisplayName("Исключение периодической задачи прекращает следующие запуски")
    void periodicFailureStopsRepetition() throws Exception {
        try (var scheduler = Executors.newSingleThreadScheduledExecutor()) {
            var task = scheduler.scheduleWithFixedDelay(() -> {
                throw new IllegalStateException("Ошибка обновления");
            }, 0, 10, TimeUnit.MILLISECONDS);
            assertThrows(ExecutionException.class, () -> task.get(2, TimeUnit.SECONDS));
            assertTrue(task.isDone());
        }
    }

    @Test
    @DisplayName("shutdown: принятые задачи завершаются, новые отклоняются")
    void shutdownLetsQueuedTaskFinish() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var pool = Executors.newSingleThreadExecutor();
        try {
            var running = pool.submit(() -> {
                started.countDown();
                release.await();
                return 1;
            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            var queued = pool.submit(() -> 2);

            pool.shutdown();
            assertThrows(RejectedExecutionException.class, () -> pool.submit(() -> 3));
            assertFalse(pool.isTerminated());
            release.countDown();

            assertEquals(1, running.get(2, TimeUnit.SECONDS));
            assertEquals(2, queued.get(2, TimeUnit.SECONDS));
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            pool.shutdownNow();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("shutdownNow: interrupt активной задаче и возврат ожидающей")
    void shutdownNowInterruptsRunningTask() throws Exception {
        var started = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var pool = Executors.newSingleThreadExecutor();
        try {
            var running = pool.submit(() -> {
                started.countDown();
                try {
                    release.await();
                } catch (InterruptedException error) {
                    interrupted.countDown();
                    throw error;
                }
                return 1;
            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            var queued = pool.submit(() -> 2);

            var neverStarted = pool.shutdownNow();
            assertEquals(1, neverStarted.size());
            assertTrue(interrupted.await(2, TimeUnit.SECONDS));
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
            assertThrows(ExecutionException.class, () -> running.get(2, TimeUnit.SECONDS));
            // Изъятие FutureTask из очереди само по себе не отменяет её Future.
            assertFalse(queued.isDone());
            assertTrue(queued.cancel(false));
        } finally {
            release.countDown();
            pool.shutdownNow();
            pool.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

}

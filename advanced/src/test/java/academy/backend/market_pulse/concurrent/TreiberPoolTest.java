package academy.backend.market_pulse.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
class TreiberPoolTest {
    @Test
    void interruptOfIdleWorkerStopsIt() throws Exception {
        AtomicReference<Thread> worker = new AtomicReference<>();
        CountDownLatch captured = new CountDownLatch(1);
        try (TreiberThreadPool pool = new TreiberThreadPool(1, error -> fail(error))) {
            pool.submit(() -> {
                worker.set(Thread.currentThread());
                captured.countDown();
            });
            assertTrue(captured.await(2, TimeUnit.SECONDS));
            Thread thread = worker.get();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (thread.getState() != Thread.State.WAITING && System.nanoTime() < deadline) {
                Thread.sleep(1);
            }
            assertEquals(Thread.State.WAITING, thread.getState());
            thread.interrupt();
            thread.join(1_000);
            assertFalse(thread.isAlive(), "Прерывание idle worker должно завершить его цикл");
        }
    }

    @Test
    void taskCancellationDoesNotInterruptNextTask() throws Exception {
        CountDownLatch firstInterrupted = new CountDownLatch(1);
        CountDownLatch secondCompleted = new CountDownLatch(1);
        AtomicBoolean secondSawInterrupt = new AtomicBoolean(true);
        try (TreiberThreadPool pool = new TreiberThreadPool(1, error -> fail(error))) {
            pool.submit(() -> {
                Thread.currentThread().interrupt();
                firstInterrupted.countDown();
            });
            assertTrue(firstInterrupted.await(2, TimeUnit.SECONDS));
            pool.submit(() -> {
                secondSawInterrupt.set(Thread.currentThread().isInterrupted());
                secondCompleted.countDown();
            });
            assertTrue(secondCompleted.await(2, TimeUnit.SECONDS));
            assertFalse(secondSawInterrupt.get(), "Флаг отменённой задачи не должен переходить к следующей");
        }
    }

    @Test
    void pendingTasksAreLifoAndFailuresDoNotKillWorker() throws Exception {
        List<Integer> seen = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(3);
        AtomicInteger errors = new AtomicInteger();
        try (TreiberThreadPool pool = new TreiberThreadPool(1, error -> errors.incrementAndGet())) {
            pool.submit(() -> {
                started.countDown();
                try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
            pool.submit(() -> { seen.add(1); done.countDown(); });
            pool.submit(() -> { seen.add(2); done.countDown(); });
            pool.submit(() -> { throw new IllegalStateException("quote failed"); });
            pool.submit(() -> { seen.add(3); done.countDown(); });
            release.countDown();
            assertTrue(done.await(3, TimeUnit.SECONDS));
            assertEquals(List.of(3, 2, 1), seen);
            assertEquals(1, errors.get());
        }
    }

    @Test
    void closeDiscardsPendingTasksAndInterruptsRunningTask() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        AtomicInteger pending = new AtomicInteger();
        TreiberThreadPool pool = new TreiberThreadPool(1, error -> fail(error));
        pool.submit(() -> {
            started.countDown();
            try { Thread.sleep(60_000); } catch (InterruptedException e) { interrupted.countDown(); }
        });
        assertTrue(started.await(2, TimeUnit.SECONDS));
        pool.submit(pending::incrementAndGet);
        pool.close();
        assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        assertEquals(0, pending.get());
        assertThrows(IllegalStateException.class, () -> pool.submit(() -> {}));
    }

    @Test
    void concurrentProducersDoNotLoseOrDuplicateTasks() throws Exception {
        int count = 4_000;
        AtomicInteger[] hits = new AtomicInteger[count];
        for (int i = 0; i < count; i++) { hits[i] = new AtomicInteger(); }
        CountDownLatch done = new CountDownLatch(count);
        try (TreiberThreadPool pool = new TreiberThreadPool(4, error -> fail(error))) {
            Thread[] producers = new Thread[4];
            for (int p = 0; p < producers.length; p++) {
                int offset = p * 1_000;
                producers[p] = Thread.ofPlatform().start(() -> {
                    for (int i = offset; i < offset + 1_000; i++) {
                        int index = i;
                        pool.submit(() -> { hits[index].incrementAndGet(); done.countDown(); });
                    }
                });
            }
            for (Thread producer : producers) { producer.join(); }
            assertTrue(done.await(5, TimeUnit.SECONDS));
            for (AtomicInteger hit : hits) { assertEquals(1, hit.get()); }
        }
    }
}

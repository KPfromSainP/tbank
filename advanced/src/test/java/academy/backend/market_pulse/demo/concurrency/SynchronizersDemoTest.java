package academy.backend.market_pulse.demo.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Запускать по одному тесту из IDE; CountDownLatch делает порядок событий наблюдаемым.
 */
@Timeout(5)
class SynchronizersDemoTest {
    @Test
    @DisplayName("CountDownLatch: дождаться всех загрузок; каждый countDown — в finally")
    void awaitAllQuotes() throws Exception {
        CountDownLatch loaded = new CountDownLatch(3);
        AtomicInteger completed = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(3)) {
            for (int i = 0; i < 3; i++) {
                pool.execute(() -> {
                    try {
                        completed.incrementAndGet();
                        System.out.println("Котировка загружена: " + Thread.currentThread().getName());
                    } finally {
                        loaded.countDown();
                    }
                });
            }
            assertTrue(loaded.await(2, TimeUnit.SECONDS));
            assertEquals(3, completed.get());
        }
    }

    @Test
    @DisplayName("Semaphore: не более двух обращений к API, даже если работников больше")
    void limitConcurrentRequests() throws Exception {
        Semaphore permits = new Semaphore(2);
        CountDownLatch inside = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(4)) {
            try {
                for (int i = 0; i < 4; i++) {
                    pool.submit(() -> {
                        permits.acquire();
                        try {
                            int current = active.incrementAndGet();
                            maximum.accumulateAndGet(current, Math::max);
                            inside.countDown();
                            release.await();
                        } finally {
                            active.decrementAndGet();
                            permits.release();
                        }
                        return null;
                    });
                }
                assertTrue(inside.await(2, TimeUnit.SECONDS));
                assertEquals(2, active.get());
                assertEquals(0, permits.availablePermits());
            } finally {
                release.countDown();
            }
        }
        assertEquals(2, maximum.get());
        assertEquals(2, permits.availablePermits());
    }

    @Test
    @DisplayName("ReentrantLock + Condition: ждать котировку, освободив lock")
    void awaitPriceCondition() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        var priceAvailable = lock.newCondition();
        int[] price = {0};
        CountDownLatch waiting = new CountDownLatch(1);
        try (var pool = Executors.newSingleThreadExecutor()) {
            var result = pool.submit(() -> {
                lock.lockInterruptibly();
                try {
                    while (price[0] == 0) {
                        waiting.countDown();
                        priceAvailable.await();
                    }
                    return price[0];
                } finally {
                    lock.unlock();
                }
            });
            assertTrue(waiting.await(2, TimeUnit.SECONDS));
            lock.lock();
            try {
                price[0] = 300;
                priceAvailable.signalAll();
            } finally {
                lock.unlock();
            }
            assertEquals(300, result.get(2, TimeUnit.SECONDS));
        }
    }
}

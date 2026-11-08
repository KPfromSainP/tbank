package academy.backend.market_pulse.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import academy.backend.market_pulse.dto.SubscriptionRequest;
import academy.backend.market_pulse.dto.SubscriptionSessionOptions;
import academy.backend.market_pulse.dto.SubscriptionTicker;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Проверяет сроки подписок, независимость тикеров, отмену и освобождение потоков. */
@Timeout(10)
class QuoteSubscriptionServiceTest {
    @Test
    void slowOutputOfOneTickerDoesNotBlockAnotherTicker() throws Exception {
        CountDownLatch firstOutput = new CountDownLatch(1);
        CountDownLatch releaseOutput = new CountDownLatch(1);
        CountDownLatch secondOutput = new CountDownLatch(2);
        try (var service = new QuoteSubscriptionService(new SubscriptionSessionOptions(2),
                SubscriptionTestClient.from(() -> BigDecimal.ONE), line -> {
                    if (line.startsWith("[SBER]")) {
                        firstOutput.countDown();
                        try { releaseOutput.await(); }
                        catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                    } else {
                        secondOutput.countDown();
                    }
                })) {
            try {
                service.watch(new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofSeconds(5)));
                assertTrue(firstOutput.await(2, TimeUnit.SECONDS));
                service.watch(new SubscriptionRequest("GAZP", Duration.ofMillis(20), Duration.ofSeconds(5)));
                assertTrue(secondOutput.await(2, TimeUnit.SECONDS));
            } finally {
                releaseOutput.countDown();
            }
        }
    }

    @Test
    void closePreservesCallingThreadsInterruptFlag() {
        QuoteSubscriptionService service = new QuoteSubscriptionService(new SubscriptionSessionOptions(1), SubscriptionTestClient.from(() -> BigDecimal.ONE), line -> {});
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedException.class, service::close);
            assertTrue(Thread.currentThread().isInterrupted(), "close должен восстановить флаг прерывания");
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void removeInterruptsOnlyThatSubscriptionAndAllowsRegisteringAgain() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);
        CountDownLatch otherUpdated = new CountDownLatch(1);
        AtomicInteger sberCalls = new AtomicInteger();
        try (QuoteSubscriptionService service = new QuoteSubscriptionService(new SubscriptionSessionOptions(2), SubscriptionTestClient.from(() -> {
            if (sberCalls.incrementAndGet() == 1) {
                started.countDown();
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    cancelled.countDown();
                    throw e;
                }
            }
            return BigDecimal.ONE;
        }), line -> { if (line.startsWith("[GAZP]")) { otherUpdated.countDown(); } })) {
            service.watch(new SubscriptionRequest("SBER", Duration.ofMillis(10), Duration.ofSeconds(5)));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertTrue(service.remove(new SubscriptionTicker("sber")));
            assertTrue(cancelled.await(2, TimeUnit.SECONDS));
            assertFalse(service.remove(new SubscriptionTicker("SBER")));
            service.watch(new SubscriptionRequest("GAZP", Duration.ofMillis(10), Duration.ofSeconds(1)));
            assertTrue(otherUpdated.await(2, TimeUnit.SECONDS));
            service.watch(new SubscriptionRequest("SBER", Duration.ofMillis(10), Duration.ofSeconds(1)));
        }
    }

    @Test
    void removedQueuedSubscriptionDoesNotStart() throws Exception {
        CountDownLatch busy = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicInteger removedCalls = new AtomicInteger();
        AtomicInteger catalogueCalls = new AtomicInteger();
        try (QuoteSubscriptionService service = new QuoteSubscriptionService(new SubscriptionSessionOptions(1), SubscriptionTestClient.from(() -> {
            if (catalogueCalls.incrementAndGet() == 1) {
                busy.countDown();
                release.await();
            } else {
                removedCalls.incrementAndGet();
            }
            return BigDecimal.ONE;
        }), line -> completed.countDown())) {
            service.watch(new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofSeconds(3)));
            assertTrue(busy.await(2, TimeUnit.SECONDS));
            service.watch(new SubscriptionRequest("GAZP", Duration.ofMillis(10), Duration.ofSeconds(3)));
            assertTrue(service.remove(new SubscriptionTicker("GAZP")));
            release.countDown();
            assertTrue(completed.await(2, TimeUnit.SECONDS));
            Thread.sleep(50);
            assertEquals(0, removedCalls.get());
        }
    }

    @Test
    void noOverlapAndNoQueuedRequestAfterExpiry() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        List<String> output = new CopyOnWriteArrayList<>();
        try (QuoteSubscriptionService service = new QuoteSubscriptionService(new SubscriptionSessionOptions(2), SubscriptionTestClient.from(() -> {
            calls.incrementAndGet();
            started.countDown();
            release.await();
            return BigDecimal.TEN;
        }), output::add)) {
            service.watch(new SubscriptionRequest("SBER", Duration.ofMillis(10), Duration.ofMillis(100)));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            Thread.sleep(180);
            assertEquals(1, calls.get());
            release.countDown();
            Thread.sleep(100);
            assertEquals(1, calls.get());
        }
        assertTrue(output.stream().anyMatch(line -> line.contains("10")));
    }

    @Test
    void queuedRequestIsSkippedIfItExpiresBeforeWorkerIsFree() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch firstDone = new CountDownLatch(1);
        AtomicInteger expiredCalls = new AtomicInteger();
        AtomicInteger catalogueCalls = new AtomicInteger();
        try (QuoteSubscriptionService service = new QuoteSubscriptionService(new SubscriptionSessionOptions(1), SubscriptionTestClient.from(() -> {
            if (catalogueCalls.incrementAndGet() == 1) {
                started.countDown();
                release.await();
            } else {
                expiredCalls.incrementAndGet();
            }
            return BigDecimal.ONE;
        }), line -> firstDone.countDown())) {
            service.watch(new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofSeconds(1)));
            assertTrue(started.await(2, TimeUnit.SECONDS));
            service.watch(new SubscriptionRequest("GAZP", Duration.ofMillis(10), Duration.ofMillis(40)));
            Thread.sleep(100); // Намеренно пересекаем дедлайн, пока единственный worker занят.
            release.countDown();
            assertTrue(firstDone.await(2, TimeUnit.SECONDS));
            Thread.sleep(50);
            assertEquals(0, expiredCalls.get());
        }
    }

    @Test
    void errorsAreRetriedAndExitInterruptsRequest() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch retryStarted = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        List<String> output = new CopyOnWriteArrayList<>();
        QuoteSubscriptionService service = new QuoteSubscriptionService(new SubscriptionSessionOptions(1), SubscriptionTestClient.from(() -> {
            if (calls.incrementAndGet() == 1) { throw new IllegalStateException("network failed"); }
            retryStarted.countDown();
            try { Thread.sleep(60_000); } catch (InterruptedException e) { interrupted.countDown(); throw e; }
            return BigDecimal.ONE;
        }), output::add);
        try {
            service.watch(new SubscriptionRequest("sber", Duration.ofMillis(10), Duration.ofSeconds(5)));
            assertTrue(retryStarted.await(2, TimeUnit.SECONDS));
            assertTrue(output.stream().anyMatch(line -> line.contains("network failed")));
        } finally {
            service.close();
        }
        assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        assertThrows(IllegalStateException.class,
                () -> service.watch(new SubscriptionRequest("GAZP", Duration.ofSeconds(1), Duration.ofSeconds(1))));
    }
}

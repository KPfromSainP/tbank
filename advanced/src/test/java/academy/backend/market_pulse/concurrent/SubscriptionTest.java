package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.dto.subscription.SubscriptionRequest;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionTest {
    @Test
    void idleSubscriptionReturnsItsDeadlineWithoutDispatcherWaitCap() {
        var subscription = new Subscription(new SubscriptionRequest("SBER", Duration.ofSeconds(5), Duration.ofSeconds(30)));
        assertTrue(subscription.scheduleIfDue(System.nanoTime()));
        assertTrue(subscription.beginRequest());
        subscription.finishRequest();

        long delay = subscription.delayUntilNextCheck(System.nanoTime());

        assertTrue(delay > Duration.ofSeconds(4).toNanos());
        assertTrue(delay <= Duration.ofSeconds(5).toNanos());
    }

    @Test
    void queuedSubscriptionWaitsForExpiryRatherThanAnotherRequest() {
        var subscription = new Subscription(new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofSeconds(5)));
        assertTrue(subscription.scheduleIfDue(System.nanoTime()));

        assertTrue(subscription.delayUntilNextCheck(System.nanoTime()) > Duration.ofSeconds(4).toNanos());
        assertEquals(0, subscription.delayUntilNextCheck(System.nanoTime() + Duration.ofSeconds(6).toNanos()));
    }

    @Test
    void runningAndCancelledSubscriptionsNeedNoTimedCheck() {
        var running = new Subscription(new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofSeconds(5)));
        assertTrue(running.scheduleIfDue(System.nanoTime()));
        assertTrue(running.beginRequest());
        assertEquals(Long.MAX_VALUE, running.delayUntilNextCheck(System.nanoTime()));
        running.finishRequest();

        var cancelled = new Subscription(new SubscriptionRequest("GAZP", Duration.ofSeconds(1), Duration.ofSeconds(5)));
        cancelled.cancel();
        assertEquals(Long.MAX_VALUE, cancelled.delayUntilNextCheck(System.nanoTime()));
    }

    @Test
    void queuedRequestDoesNotKeepExpiredSubscriptionActive() {
        var subscription = new Subscription(new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofSeconds(5)));
        assertTrue(subscription.scheduleIfDue(System.nanoTime()));

        assertTrue(subscription.isExpiredAndNotRunning(System.nanoTime() + Duration.ofSeconds(6).toNanos()));
    }

    @Test
    void runningRequestKeepsSubscriptionUntilItFinishes() {
        var subscription = new Subscription(new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofSeconds(5)));
        assertTrue(subscription.scheduleIfDue(System.nanoTime()));
        assertTrue(subscription.beginRequest());
        long afterExpiry = System.nanoTime() + Duration.ofSeconds(6).toNanos();

        assertFalse(subscription.isExpiredAndNotRunning(afterExpiry));
        subscription.finishRequest();
        assertTrue(subscription.isExpiredAndNotRunning(afterExpiry));
    }
}

package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.dto.subscription.SubscriptionRequest;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionTest {
    @Test
    void queuedRequestDoesNotKeepExpiredSubscriptionActive() {
        var subscription = new Subscription(new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofSeconds(5)));
        assertTrue(subscription.scheduleIfDue(System.nanoTime()));

        assertTrue(subscription.isExpiredAndIdle(System.nanoTime() + Duration.ofSeconds(6).toNanos()));
    }

    @Test
    void runningRequestKeepsSubscriptionUntilItFinishes() {
        var subscription = new Subscription(new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofSeconds(5)));
        assertTrue(subscription.scheduleIfDue(System.nanoTime()));
        assertTrue(subscription.beginRequest());
        long afterExpiry = System.nanoTime() + Duration.ofSeconds(6).toNanos();

        assertFalse(subscription.isExpiredAndIdle(afterExpiry));
        subscription.finishRequest();
        assertTrue(subscription.isExpiredAndIdle(afterExpiry));
    }
}

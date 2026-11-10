package academy.backend.market_pulse.dto.subscription;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Проверяет параметры на границе команды, до создания потоков и регистрации подписки.
 */
class SubscriptionRequestTest {
    @Test
    void parsesUnitsAndNormalizesTicker() {
        SubscriptionRequest request = SubscriptionRequest.parse(" sber ", "250ms", "2m");
        assertEquals("SBER", request.ticker());
        assertEquals(Duration.ofMillis(250), request.interval());
        assertEquals(Duration.ofMinutes(2), request.lifetime());
        assertEquals(Duration.ofHours(1), SubscriptionRequest.parse("GAZP", "1s", "1h").lifetime());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0s", "-1s", "1", "1.5s", "8d", "169h", "999999999999999999999h"})
    void rejectsInvalidDuration(String duration) {
        assertThrows(IllegalArgumentException.class, () -> SubscriptionRequest.parse("SBER", duration, "1m"));
        assertThrows(IllegalArgumentException.class, () -> SubscriptionRequest.parse("SBER", "1s", duration));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "S BER", "SBER!", "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567"})
    void rejectsInvalidTicker(String ticker) {
        assertThrows(IllegalArgumentException.class, () -> SubscriptionRequest.parse(ticker, "1s", "1m"));
        assertThrows(IllegalArgumentException.class, () -> new SubscriptionTicker(ticker));
    }

    @Test
    void rejectsInvalidTypedParameters() {
        assertThrows(IllegalArgumentException.class,
                () -> new SubscriptionRequest("SBER", Duration.ZERO, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new SubscriptionRequest("SBER", Duration.ofSeconds(1), Duration.ofDays(8)));
        assertThrows(IllegalArgumentException.class, () -> new SubscriptionConfig(0));
        assertThrows(IllegalArgumentException.class, () -> new SubscriptionConfig(-1));
    }
}

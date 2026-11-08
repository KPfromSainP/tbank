package academy.backend.market_pulse.refactoring.params;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

abstract class OrderCreationContractTest {

    abstract Order create(String ticker, String side, int quantity, BigDecimal price, String currency,
                          boolean market, boolean allOrNone, String comment);

    @Test
    void limitOrderIsNormalized() {
        Order order = create("sber", "BUY", 10, new BigDecimal("250.50"), null, false, false, "  срочно  ");

        assertThat(order).isEqualTo(new Order("SBER", "BUY", 10, new BigDecimal("250.50"), "RUB", false, "срочно"));
    }

    @Test
    void marketOrderDropsTheLimitPrice() {
        Order order = create("sber", "SELL", 3, new BigDecimal("999"), "usd", true, true, null);

        assertThat(order.limitPrice()).isNull();
        assertThat(order.currency()).isEqualTo("USD");
        assertThat(order.allOrNone()).isTrue();
        assertThat(order.comment()).isEmpty();
    }

    @Test
    void marketOrderDoesNotNeedAPrice() {
        assertThat(create("gazp", "BUY", 1, null, "", true, false, "")).extracting(Order::limitPrice).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void tickerIsRequired(String ticker) {
        assertThatThrownBy(() -> create(ticker, "BUY", 1, BigDecimal.ONE, null, false, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Тикер не задан");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5})
    void quantityMustBePositive(int quantity) {
        assertThatThrownBy(() -> create("sber", "BUY", quantity, BigDecimal.ONE, null, false, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Количество должно быть положительным");
    }

    @Test
    void limitOrderNeedsAPositivePrice() {
        assertThatThrownBy(() -> create("sber", "BUY", 1, null, null, false, false, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> create("sber", "BUY", 1, BigDecimal.ZERO, null, false, false, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

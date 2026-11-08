package academy.backend.market_pulse.refactoring.nesting;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

abstract class OrderValidatorContractTest {

    private static final BigDecimal CASH = new BigDecimal("1000");

    abstract Optional<String> firstError(OrderDraft draft);

    private static OrderDraft draft(String ticker, String side, int quantity, String price, int position) {
        return new OrderDraft(ticker, side, quantity, price == null ? null : new BigDecimal(price), CASH, position);
    }

    static Stream<Arguments> invalidDrafts() {
        return Stream.of(
                Arguments.of(null, "Заявка не задана"),
                Arguments.of(draft(null, "BUY", 1, "10", 0), "Тикер не задан"),
                Arguments.of(draft(" ", "BUY", 1, "10", 0), "Тикер не задан"),
                Arguments.of(draft("SBER", null, 1, "10", 0), "Неизвестная сторона сделки"),
                Arguments.of(draft("SBER", "HOLD", 1, "10", 0), "Неизвестная сторона сделки"),
                Arguments.of(draft("SBER", "BUY", 0, "10", 0), "Количество должно быть положительным"),
                Arguments.of(draft("SBER", "BUY", 1, null, 0), "Цена должна быть положительной"),
                Arguments.of(draft("SBER", "BUY", 1, "0", 0), "Цена должна быть положительной"),
                Arguments.of(draft("SBER", "BUY", 101, "10", 0), "Недостаточно средств"),
                Arguments.of(draft("SBER", "SELL", 6, "10", 5), "Недостаточно бумаг"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidDrafts")
    void firstErrorIsReported(OrderDraft draft, String expectedError) {
        assertThat(firstError(draft)).contains(expectedError);
    }

    @Test
    void firstFailingRuleWinsWhenSeveralAreViolated() {
        assertThat(firstError(draft("", "HOLD", -1, null, 0))).contains("Тикер не задан");
        assertThat(firstError(draft("SBER", "HOLD", -1, null, 0))).contains("Неизвестная сторона сделки");
        assertThat(firstError(draft("SBER", "BUY", -1, null, 0))).contains("Количество должно быть положительным");
    }

    @Test
    void validOrdersHaveNoErrorsAtTheLimit() {
        assertThat(firstError(draft("SBER", "BUY", 100, "10", 0))).isEmpty();   // ровно на сумму счёта
        assertThat(firstError(draft("SBER", "SELL", 5, "10", 5))).isEmpty();    // ровно вся позиция
    }
}

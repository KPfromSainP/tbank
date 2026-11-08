package academy.backend.market_pulse.refactoring.naming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

abstract class PositionMetricsContractTest {

    abstract BigDecimal profit(BigDecimal buyPrice, BigDecimal currentPrice, int quantity);

    abstract BigDecimal returnPercent(BigDecimal buyPrice, BigDecimal currentPrice);

    @ParameterizedTest(name = "купили по {0}, сейчас {1}, {2} шт. -> {3}")
    @CsvSource({"100, 120, 10, 200", "100, 80, 5, -100", "50, 50, 7, 0"})
    void profitIsTheDifferenceTimesQuantity(String buy, String current, int quantity, String expected) {
        assertThat(profit(new BigDecimal(buy), new BigDecimal(current), quantity)).isEqualByComparingTo(expected);
    }

    @ParameterizedTest(name = "купили по {0}, сейчас {1} -> {2}%")
    @CsvSource({"100, 125, 25.00", "80, 100, 25.00", "200, 150, -25.00", "3, 4, 33.33"})
    void returnIsPercentOfTheBuyPrice(String buy, String current, String expected) {
        assertThat(returnPercent(new BigDecimal(buy), new BigDecimal(current))).isEqualByComparingTo(expected);
    }

    @Test
    void zeroBuyPriceIsRejected() {
        assertThatThrownBy(() -> returnPercent(BigDecimal.ZERO, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Цена покупки не может быть нулевой");
    }
}

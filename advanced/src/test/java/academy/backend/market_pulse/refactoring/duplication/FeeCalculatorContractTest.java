package academy.backend.market_pulse.refactoring.duplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Одно и то же поведение до и после рефакторинга: тесты не знают, какая версия под ними. */
abstract class FeeCalculatorContractTest {

    abstract BigDecimal fee(String side, BigDecimal price, int quantity);

    @DisplayName("Комиссия складывается из брокерской (с минимумом) и биржевой")
    @ParameterizedTest(name = "{0} {2} шт. по {1} -> {3}")
    @CsvSource({
            "BUY, 100, 100, 31.00",
            "SELL, 100, 100, 41.00",
            "BUY, 10, 1, 1.00",     // сработал минимум 1.00
            "SELL, 10, 1, 1.50",    // сработал минимум 1.50
            "BUY, 50, 1, 1.01"      // 1.00 + 0.005 -> округление HALF_UP вверх
    })
    void feeIsCommissionPlusExchangeFee(String side, String price, int quantity, String expected) {
        assertThat(fee(side, new BigDecimal(price), quantity)).isEqualByComparingTo(expected);
    }

    @Test
    void resultIsRoundedToTwoDigits() {
        assertThat(fee("BUY", new BigDecimal("50"), 1).scale()).isEqualTo(2);
    }

    @Test
    void unknownSideIsRejected() {
        assertThatThrownBy(() -> fee("HOLD", BigDecimal.TEN, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HOLD");
    }

    @Test
    void quantityMustBePositiveEvenBeforeTheSideIsChecked() {
        assertThatThrownBy(() -> fee("HOLD", BigDecimal.TEN, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Количество");
    }
}

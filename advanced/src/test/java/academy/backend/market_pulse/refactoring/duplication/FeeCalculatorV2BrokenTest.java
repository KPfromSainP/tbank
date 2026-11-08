package academy.backend.market_pulse.refactoring.duplication;

import java.math.BigDecimal;

import org.junit.jupiter.api.Disabled;

/**
 * Демонстрация: те же тесты на «рефакторинге», изменившем поведение. Чтобы увидеть красный тест,
 * уберите {@code @Disabled} — упадёт случай с округлением.
 */
@Disabled("Демонстрация: убрать @Disabled, чтобы увидеть, как тесты ловят ошибочный рефакторинг")
class FeeCalculatorV2BrokenTest extends FeeCalculatorContractTest {

    private final FeeCalculatorV2Broken calculator = new FeeCalculatorV2Broken();

    @Override
    BigDecimal fee(String side, BigDecimal price, int quantity) {
        return calculator.fee(side, price, quantity);
    }
}

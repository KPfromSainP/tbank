package academy.backend.market_pulse.refactoring.selfwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Страховочная сетка для самостоятельного рефакторинга правил по типам инструментов. */
public abstract class InstrumentRulesContractTest {

    protected abstract int lotSize(String type);

    protected abstract int settlementDays(String type);

    protected abstract String label(String type);

    @ParameterizedTest(name = "{0}: {1}, лот {2}, расчёты T+{3}")
    @CsvSource({"STOCK, Акция, 10, 2", "BOND, Облигация, 1, 1", "ETF, Фонд, 1, 2"})
    void rulesByType(String type, String label, int lotSize, int settlementDays) {
        assertThat(label(type)).isEqualTo(label);
        assertThat(lotSize(type)).isEqualTo(lotSize);
        assertThat(settlementDays(type)).isEqualTo(settlementDays);
    }

    @Test
    void unknownTypeIsRejectedEverywhere() {
        assertThatThrownBy(() -> lotSize("CRYPTO")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CRYPTO");
        assertThatThrownBy(() -> settlementDays("CRYPTO")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> label("CRYPTO")).isInstanceOf(IllegalArgumentException.class);
    }
}

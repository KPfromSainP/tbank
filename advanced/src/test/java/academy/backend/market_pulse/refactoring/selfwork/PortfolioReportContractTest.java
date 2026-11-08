package academy.backend.market_pulse.refactoring.selfwork;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Страховочная сетка для самостоятельного рефакторинга: поведение отчёта менять нельзя. */
public abstract class PortfolioReportContractTest {

    protected abstract String render(List<Holding> holdings);

    private static Holding holding(String ticker, int quantity, String buy, String current) {
        return new Holding(ticker, quantity, new BigDecimal(buy), new BigDecimal(current));
    }

    @Test
    void emptyPortfolio() {
        assertThat(render(List.of())).isEqualTo("Портфель\n--------\nПусто\n");
    }

    @Test
    void singleProfitablePosition() {
        assertThat(render(List.of(holding("SBER", 10, "250", "260")))).isEqualTo("""
                Портфель
                --------
                SBER: 10 шт., +100.00
                Итого: 2600.00 (+100.00)
                Лучшая: SBER
                Худшая: SBER
                """);
    }

    @Test
    void mixedPortfolioHasBestAndWorstPositions() {
        List<Holding> holdings = List.of(
                holding("SBER", 10, "250", "260"),   // +100
                holding("GAZP", 100, "120.5", "118"), // -250
                holding("LKOH", 1, "7000", "7000"));  // 0

        assertThat(render(holdings)).isEqualTo("""
                Портфель
                --------
                SBER: 10 шт., +100.00
                GAZP: 100 шт., -250.00
                LKOH: 1 шт., 0.00
                Итого: 21400.00 (-150.00)
                Лучшая: SBER
                Худшая: GAZP
                """);
    }

    @Test
    void firstPositionWinsWhenResultsAreEqual() {
        List<Holding> holdings = List.of(holding("AAA", 1, "10", "15"), holding("BBB", 1, "20", "25"));

        assertThat(render(holdings)).contains("Лучшая: AAA").contains("Худшая: AAA");
    }
}

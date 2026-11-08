package academy.backend.market_pulse.refactoring.errors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

abstract class QuoteImporterContractTest {

    abstract ImportResult importLines(List<String> lines);

    @Test
    void validLinesAreImported() {
        ImportResult result = importLines(List.of("SBER;250.50;1.2", "GAZP; 120 ;-0.8"));

        assertThat(result.imported()).isEqualTo(2);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void everyBadLineIsReportedWithItsNumberAndReason() {
        ImportResult result = importLines(List.of(
                "SBER;250;1",          // 1 — хорошая
                "GAZP;120",            // 2 — не хватает поля
                " ;10;1",              // 3 — пустой тикер
                "LKOH;abc;1",          // 4 — цена не число
                "AAPL;-5;1",           // 5 — цена не положительная
                "FXUS;6.85;xyz",       // 6 — изменение не число
                ""));                  // 7 — пустая строка

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.errors()).containsExactly(
                "строка 2: ожидалось 3 поля",
                "строка 3: пустой тикер",
                "строка 4: некорректная цена",
                "строка 5: цена должна быть положительной",
                "строка 6: некорректное изменение",
                "строка 7: ожидалось 3 поля");
    }

    @Test
    void emptyInputIsNotAnError() {
        assertThat(importLines(List.of())).isEqualTo(new ImportResult(0, List.of()));
    }

    @Test
    void firstProblemOfALineHidesTheOthers() {
        assertThat(importLines(List.of("; abc ;xyz")).errors()).containsExactly("строка 1: пустой тикер");
    }
}

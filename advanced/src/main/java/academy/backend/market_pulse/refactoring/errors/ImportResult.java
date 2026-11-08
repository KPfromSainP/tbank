package academy.backend.market_pulse.refactoring.errors;

import java.util.List;

/** Итог импорта: сколько строк принято и какие отклонены (с причиной). */
public record ImportResult(int imported, List<String> errors) {
}

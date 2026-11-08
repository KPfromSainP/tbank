package academy.backend.market_pulse.refactoring.nesting;

import java.math.BigDecimal;
import java.util.Optional;

/** После рефакторинга: охранные условия (guard clauses) и вынесенные проверки — код читается сверху вниз. */
public class OrderValidatorV2 {

    public Optional<String> firstError(OrderDraft draft) {
        if (draft == null) {
            return Optional.of("Заявка не задана");
        }
        if (draft.ticker() == null || draft.ticker().isBlank()) {
            return Optional.of("Тикер не задан");
        }
        if (!isKnownSide(draft.side())) {
            return Optional.of("Неизвестная сторона сделки");
        }
        if (draft.quantity() <= 0) {
            return Optional.of("Количество должно быть положительным");
        }
        if (draft.price() == null || draft.price().signum() <= 0) {
            return Optional.of("Цена должна быть положительной");
        }
        return checkFunds(draft);
    }

    private boolean isKnownSide(String side) {
        return "BUY".equals(side) || "SELL".equals(side);
    }

    private Optional<String> checkFunds(OrderDraft draft) {
        if (draft.side().equals("BUY")) {
            BigDecimal required = draft.price().multiply(BigDecimal.valueOf(draft.quantity()));
            return required.compareTo(draft.availableCash()) > 0
                    ? Optional.of("Недостаточно средств")
                    : Optional.empty();
        }
        return draft.quantity() > draft.availablePosition()
                ? Optional.of("Недостаточно бумаг")
                : Optional.empty();
    }
}

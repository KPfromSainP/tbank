package academy.backend.market_pulse.refactoring.nesting;

import java.math.BigDecimal;
import java.util.Optional;

/** До рефакторинга: пять уровней вложенности, ошибка прячется в самом низу {@code else}. */
public class OrderValidatorV1 {

    public Optional<String> firstError(OrderDraft draft) {
        String error = null;
        if (draft != null) {
            if (draft.ticker() != null && !draft.ticker().isBlank()) {
                if (draft.side() != null && (draft.side().equals("BUY") || draft.side().equals("SELL"))) {
                    if (draft.quantity() > 0) {
                        if (draft.price() != null && draft.price().signum() > 0) {
                            if (draft.side().equals("BUY")) {
                                BigDecimal required = draft.price().multiply(BigDecimal.valueOf(draft.quantity()));
                                if (required.compareTo(draft.availableCash()) > 0) {
                                    error = "Недостаточно средств";
                                }
                            } else {
                                if (draft.quantity() > draft.availablePosition()) {
                                    error = "Недостаточно бумаг";
                                }
                            }
                        } else {
                            error = "Цена должна быть положительной";
                        }
                    } else {
                        error = "Количество должно быть положительным";
                    }
                } else {
                    error = "Неизвестная сторона сделки";
                }
            } else {
                error = "Тикер не задан";
            }
        } else {
            error = "Заявка не задана";
        }
        return Optional.ofNullable(error);
    }
}

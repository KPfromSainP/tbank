package academy.backend.market_pulse.refactoring.nesting;

import java.math.BigDecimal;

/** Черновик заявки и состояние счёта на момент проверки. */
public record OrderDraft(String ticker, String side, int quantity, BigDecimal price, BigDecimal availableCash,
                         int availablePosition) {
}

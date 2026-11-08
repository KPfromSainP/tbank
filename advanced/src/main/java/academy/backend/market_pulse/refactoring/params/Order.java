package academy.backend.market_pulse.refactoring.params;

import java.math.BigDecimal;

/** Заявка. {@code limitPrice} равна {@code null} у рыночной заявки. */
public record Order(String ticker, String side, int quantity, BigDecimal limitPrice, String currency,
                    boolean allOrNone, String comment) {
}

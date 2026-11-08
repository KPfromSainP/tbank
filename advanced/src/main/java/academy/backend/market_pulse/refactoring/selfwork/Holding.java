package academy.backend.market_pulse.refactoring.selfwork;

import java.math.BigDecimal;

/** Позиция в портфеле: тикер, количество, цена покупки и текущая цена. */
public record Holding(String ticker, int quantity, BigDecimal buyPrice, BigDecimal currentPrice) {
}

package academy.backend.market_pulse.refactoring.params;

import java.math.BigDecimal;

/**
 * Параметры заявки одним объектом (Introduce Parameter Object): имена полей читаются в месте
 * вызова, лишнее необязательное можно опустить.
 */
public record OrderRequest(String ticker, String side, int quantity, BigDecimal price, String currency,
                           boolean market, boolean allOrNone, String comment) {

    public static OrderRequest market(String ticker, String side, int quantity) {
        return new OrderRequest(ticker, side, quantity, null, null, true, false, null);
    }

    public static OrderRequest limit(String ticker, String side, int quantity, BigDecimal price) {
        return new OrderRequest(ticker, side, quantity, price, null, false, false, null);
    }

    public OrderRequest inCurrency(String newCurrency) {
        return new OrderRequest(ticker, side, quantity, price, newCurrency, market, allOrNone, comment);
    }

    public OrderRequest asAllOrNone() {
        return new OrderRequest(ticker, side, quantity, price, currency, market, true, comment);
    }

    public OrderRequest withComment(String newComment) {
        return new OrderRequest(ticker, side, quantity, price, currency, market, allOrNone, newComment);
    }
}

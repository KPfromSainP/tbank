package academy.backend.market_pulse.refactoring.params;

import java.math.BigDecimal;

/** До рефакторинга: восемь параметров, порядок легко перепутать, два {@code boolean} подряд. */
public class OrderFactoryV1 {

    public Order createOrder(String ticker, String side, int quantity, BigDecimal price, String currency,
                             boolean market, boolean allOrNone, String comment) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Тикер не задан");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }
        if (!market && (price == null || price.signum() <= 0)) {
            throw new IllegalArgumentException("Для лимитной заявки нужна положительная цена");
        }
        String normalizedCurrency = currency == null || currency.isBlank() ? "RUB" : currency.toUpperCase();
        String normalizedComment = comment == null ? "" : comment.trim();
        return new Order(ticker.toUpperCase(), side, quantity, market ? null : price, normalizedCurrency,
                allOrNone, normalizedComment);
    }
}

package academy.backend.market_pulse.refactoring.params;

/** После рефакторинга: один параметр-объект вместо восьми аргументов. */
public class OrderFactoryV2 {

    private static final String DEFAULT_CURRENCY = "RUB";

    public Order createOrder(OrderRequest request) {
        validate(request);
        return new Order(
                request.ticker().toUpperCase(),
                request.side(),
                request.quantity(),
                request.market() ? null : request.price(),
                currencyOf(request),
                request.allOrNone(),
                commentOf(request));
    }

    private void validate(OrderRequest request) {
        if (request.ticker() == null || request.ticker().isBlank()) {
            throw new IllegalArgumentException("Тикер не задан");
        }
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }
        if (!request.market() && (request.price() == null || request.price().signum() <= 0)) {
            throw new IllegalArgumentException("Для лимитной заявки нужна положительная цена");
        }
    }

    private String currencyOf(OrderRequest request) {
        return request.currency() == null || request.currency().isBlank()
                ? DEFAULT_CURRENCY
                : request.currency().toUpperCase();
    }

    private String commentOf(OrderRequest request) {
        return request.comment() == null ? "" : request.comment().trim();
    }
}

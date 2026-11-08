package academy.backend.market_pulse.refactoring.params;

import java.math.BigDecimal;

class OrderFactoryV1Test extends OrderCreationContractTest {

    private final OrderFactoryV1 factory = new OrderFactoryV1();

    @Override
    Order create(String ticker, String side, int quantity, BigDecimal price, String currency, boolean market,
                 boolean allOrNone, String comment) {
        return factory.createOrder(ticker, side, quantity, price, currency, market, allOrNone, comment);
    }
}

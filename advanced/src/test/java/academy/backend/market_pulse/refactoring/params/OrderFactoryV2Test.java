package academy.backend.market_pulse.refactoring.params;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OrderFactoryV2Test extends OrderCreationContractTest {

    private final OrderFactoryV2 factory = new OrderFactoryV2();

    @Override
    Order create(String ticker, String side, int quantity, BigDecimal price, String currency, boolean market,
                 boolean allOrNone, String comment) {
        return factory.createOrder(
                new OrderRequest(ticker, side, quantity, price, currency, market, allOrNone, comment));
    }

    @Test
    void fluentRequestReadsBetterThanEightPositionalArguments() {
        OrderRequest request = OrderRequest.limit("sber", "BUY", 10, new BigDecimal("250"))
                .inCurrency("usd")
                .asAllOrNone()
                .withComment("  по рынку не надо ");

        assertThat(factory.createOrder(request))
                .isEqualTo(new Order("SBER", "BUY", 10, new BigDecimal("250"), "USD", true, "по рынку не надо"));
    }
}

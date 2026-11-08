package academy.backend.market_pulse.refactoring.duplication;

import java.math.BigDecimal;

class FeeCalculatorV1Test extends FeeCalculatorContractTest {

    private final FeeCalculatorV1 calculator = new FeeCalculatorV1();

    @Override
    BigDecimal fee(String side, BigDecimal price, int quantity) {
        return calculator.fee(side, price, quantity);
    }
}

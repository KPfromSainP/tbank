package academy.backend.market_pulse.refactoring.duplication;

import java.math.BigDecimal;

class FeeCalculatorV2Test extends FeeCalculatorContractTest {

    private final FeeCalculatorV2 calculator = new FeeCalculatorV2();

    @Override
    BigDecimal fee(String side, BigDecimal price, int quantity) {
        return calculator.fee(side, price, quantity);
    }
}

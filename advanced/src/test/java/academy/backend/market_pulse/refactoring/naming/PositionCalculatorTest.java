package academy.backend.market_pulse.refactoring.naming;

import java.math.BigDecimal;

class PositionCalculatorTest extends PositionMetricsContractTest {

    private final PositionCalculator calculator = new PositionCalculator();

    @Override
    BigDecimal profit(BigDecimal buyPrice, BigDecimal currentPrice, int quantity) {
        return calculator.profit(buyPrice, currentPrice, quantity);
    }

    @Override
    BigDecimal returnPercent(BigDecimal buyPrice, BigDecimal currentPrice) {
        return calculator.returnPercent(buyPrice, currentPrice);
    }
}

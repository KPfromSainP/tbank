package academy.backend.market_pulse.refactoring.naming;

import java.math.BigDecimal;

class CalcTest extends PositionMetricsContractTest {

    private final Calc calc = new Calc();

    @Override
    BigDecimal profit(BigDecimal buyPrice, BigDecimal currentPrice, int quantity) {
        return calc.c1(buyPrice, currentPrice, quantity);
    }

    @Override
    BigDecimal returnPercent(BigDecimal buyPrice, BigDecimal currentPrice) {
        return calc.c2(buyPrice, currentPrice);
    }
}

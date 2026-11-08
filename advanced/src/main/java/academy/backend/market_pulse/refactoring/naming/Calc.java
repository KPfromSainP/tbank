package academy.backend.market_pulse.refactoring.naming;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** До рефакторинга: имена ничего не говорят, смысл спрятан в комментариях. */
public class Calc {

    // считает прибыль
    public BigDecimal c1(BigDecimal a, BigDecimal b, int n) {
        BigDecimal d = b.subtract(a);
        return d.multiply(BigDecimal.valueOf(n));
    }

    // считает доходность в процентах
    public BigDecimal c2(BigDecimal a, BigDecimal b) {
        if (a.signum() == 0) {
            throw new IllegalArgumentException("Цена покупки не может быть нулевой");
        }
        return b.subtract(a).multiply(new BigDecimal(100)).divide(a, 2, RoundingMode.HALF_UP);
    }
}

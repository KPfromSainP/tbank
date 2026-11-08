package academy.backend.market_pulse.refactoring.naming;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** После рефакторинга: имена объясняют смысл, комментарии не нужны. */
public class PositionCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal(100);

    public BigDecimal profit(BigDecimal buyPrice, BigDecimal currentPrice, int quantity) {
        BigDecimal profitPerShare = currentPrice.subtract(buyPrice);
        return profitPerShare.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal returnPercent(BigDecimal buyPrice, BigDecimal currentPrice) {
        if (buyPrice.signum() == 0) {
            throw new IllegalArgumentException("Цена покупки не может быть нулевой");
        }
        return currentPrice.subtract(buyPrice).multiply(ONE_HUNDRED).divide(buyPrice, 2, RoundingMode.HALF_UP);
    }
}

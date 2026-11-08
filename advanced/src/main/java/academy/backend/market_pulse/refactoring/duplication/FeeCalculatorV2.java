package academy.backend.market_pulse.refactoring.duplication;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** После рефакторинга: общий расчёт вынесен в метод, числа — в именованные константы. */
public class FeeCalculatorV2 {

    private static final BigDecimal BUY_RATE = new BigDecimal("0.003");
    private static final BigDecimal BUY_MINIMUM = new BigDecimal("1.00");
    private static final BigDecimal SELL_RATE = new BigDecimal("0.004");
    private static final BigDecimal SELL_MINIMUM = new BigDecimal("1.50");
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("0.0001");

    public BigDecimal fee(String side, BigDecimal price, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }
        BigDecimal gross = price.multiply(BigDecimal.valueOf(quantity));
        return switch (side) {
            case "BUY" -> fee(gross, BUY_RATE, BUY_MINIMUM);
            case "SELL" -> fee(gross, SELL_RATE, SELL_MINIMUM);
            default -> throw new IllegalArgumentException("Неизвестная сторона сделки: " + side);
        };
    }

    private BigDecimal fee(BigDecimal gross, BigDecimal commissionRate, BigDecimal minimumCommission) {
        BigDecimal commission = gross.multiply(commissionRate).max(minimumCommission);
        BigDecimal exchangeFee = gross.multiply(EXCHANGE_RATE);
        return commission.add(exchangeFee).setScale(2, RoundingMode.HALF_UP);
    }
}

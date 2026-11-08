package academy.backend.market_pulse.refactoring.duplication;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** До рефакторинга: расчёт повторяется в обеих ветках, числа разбросаны по коду. */
public class FeeCalculatorV1 {

    public BigDecimal fee(String side, BigDecimal price, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }
        if (side.equals("BUY")) {
            BigDecimal gross = price.multiply(BigDecimal.valueOf(quantity));
            BigDecimal commission = gross.multiply(new BigDecimal("0.003"));
            if (commission.compareTo(new BigDecimal("1.00")) < 0) {
                commission = new BigDecimal("1.00");
            }
            BigDecimal exchange = gross.multiply(new BigDecimal("0.0001"));
            return commission.add(exchange).setScale(2, RoundingMode.HALF_UP);
        } else if (side.equals("SELL")) {
            BigDecimal gross = price.multiply(BigDecimal.valueOf(quantity));
            BigDecimal commission = gross.multiply(new BigDecimal("0.004"));
            if (commission.compareTo(new BigDecimal("1.50")) < 0) {
                commission = new BigDecimal("1.50");
            }
            BigDecimal exchange = gross.multiply(new BigDecimal("0.0001"));
            return commission.add(exchange).setScale(2, RoundingMode.HALF_UP);
        } else {
            throw new IllegalArgumentException("Неизвестная сторона сделки: " + side);
        }
    }
}

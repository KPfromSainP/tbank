package academy.backend.market_pulse.refactoring.selfwork;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Задание для самостоятельной работы №1: длинный метод. Приёмы: Extract Method, Replace Temp with
 * Query, Extract Variable. Поведение (текст отчёта) менять нельзя — тесты {@code PortfolioReportTest}
 * должны оставаться зелёными после каждого шага.
 */
public class PortfolioReportV1 {

    public String render(List<Holding> holdings) {
        StringBuilder sb = new StringBuilder();
        sb.append("Портфель\n");
        sb.append("--------\n");
        if (holdings.isEmpty()) {
            sb.append("Пусто\n");
            return sb.toString();
        }
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        Holding best = null;
        Holding worst = null;
        for (Holding h : holdings) {
            BigDecimal cost = h.buyPrice().multiply(BigDecimal.valueOf(h.quantity()));
            BigDecimal value = h.currentPrice().multiply(BigDecimal.valueOf(h.quantity()));
            totalCost = totalCost.add(cost);
            totalValue = totalValue.add(value);
            BigDecimal pnl = value.subtract(cost);
            String sign = pnl.signum() > 0 ? "+" : "";
            sb.append(h.ticker()).append(": ").append(h.quantity()).append(" шт., ")
                    .append(sign).append(pnl.setScale(2, RoundingMode.HALF_UP)).append("\n");
            if (best == null || pnl.compareTo(best.currentPrice().subtract(best.buyPrice())
                    .multiply(BigDecimal.valueOf(best.quantity()))) > 0) {
                best = h;
            }
            if (worst == null || pnl.compareTo(worst.currentPrice().subtract(worst.buyPrice())
                    .multiply(BigDecimal.valueOf(worst.quantity()))) < 0) {
                worst = h;
            }
        }
        BigDecimal totalPnl = totalValue.subtract(totalCost);
        String totalSign = totalPnl.signum() > 0 ? "+" : "";
        sb.append("Итого: ").append(totalValue.setScale(2, RoundingMode.HALF_UP))
                .append(" (").append(totalSign).append(totalPnl.setScale(2, RoundingMode.HALF_UP)).append(")\n");
        sb.append("Лучшая: ").append(best.ticker()).append("\n");
        sb.append("Худшая: ").append(worst.ticker()).append("\n");
        return sb.toString();
    }
}

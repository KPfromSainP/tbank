package academy.backend.market_pulse.refactoring.selfwork.reference;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import academy.backend.market_pulse.refactoring.selfwork.Holding;

/** Эталон к заданию №1 (студентам заранее не показывать). */
public class PortfolioReportV2 {

    public String render(List<Holding> holdings) {
        StringBuilder report = new StringBuilder("Портфель\n--------\n");
        if (holdings.isEmpty()) {
            return report.append("Пусто\n").toString();
        }
        holdings.forEach(holding -> report.append(positionLine(holding)));
        report.append(totalLine(holdings));
        report.append("Лучшая: ").append(extreme(holdings, true).ticker()).append("\n");
        report.append("Худшая: ").append(extreme(holdings, false).ticker()).append("\n");
        return report.toString();
    }

    private String positionLine(Holding holding) {
        return holding.ticker() + ": " + holding.quantity() + " шт., " + signed(profit(holding)) + "\n";
    }

    private String totalLine(List<Holding> holdings) {
        BigDecimal totalValue = holdings.stream().map(this::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = holdings.stream().map(this::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return "Итого: " + scaled(totalValue) + " (" + signed(totalValue.subtract(totalCost)) + ")\n";
    }

    /** Первая позиция с наибольшей (или наименьшей) прибылью — как и в исходном цикле при равенстве побеждает первая. */
    private Holding extreme(List<Holding> holdings, boolean best) {
        Holding result = holdings.get(0);
        for (Holding holding : holdings) {
            int comparison = profit(holding).compareTo(profit(result));
            if (best ? comparison > 0 : comparison < 0) {
                result = holding;
            }
        }
        return result;
    }

    private BigDecimal cost(Holding holding) {
        return holding.buyPrice().multiply(BigDecimal.valueOf(holding.quantity()));
    }

    private BigDecimal value(Holding holding) {
        return holding.currentPrice().multiply(BigDecimal.valueOf(holding.quantity()));
    }

    private BigDecimal profit(Holding holding) {
        return value(holding).subtract(cost(holding));
    }

    private String signed(BigDecimal amount) {
        return (amount.signum() > 0 ? "+" : "") + scaled(amount);
    }

    private BigDecimal scaled(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}

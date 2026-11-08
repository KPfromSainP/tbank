package academy.backend.market_pulse.refactoring.selfwork;

import java.util.List;

/** Самостоятельная работа: рефакторите {@link PortfolioReportV1} — этот тест должен оставаться зелёным. */
class PortfolioReportTest extends PortfolioReportContractTest {

    private final PortfolioReportV1 report = new PortfolioReportV1();

    @Override
    protected String render(List<Holding> holdings) {
        return report.render(holdings);
    }
}

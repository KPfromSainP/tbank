package academy.backend.market_pulse.refactoring.selfwork.reference;

import java.util.List;

import academy.backend.market_pulse.refactoring.selfwork.Holding;
import academy.backend.market_pulse.refactoring.selfwork.PortfolioReportContractTest;

class PortfolioReportReferenceTest extends PortfolioReportContractTest {

    private final PortfolioReportV2 report = new PortfolioReportV2();

    @Override
    protected String render(List<Holding> holdings) {
        return report.render(holdings);
    }
}

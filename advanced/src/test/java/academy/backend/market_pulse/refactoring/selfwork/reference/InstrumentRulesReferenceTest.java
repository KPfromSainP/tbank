package academy.backend.market_pulse.refactoring.selfwork.reference;

import academy.backend.market_pulse.refactoring.selfwork.InstrumentRulesContractTest;

class InstrumentRulesReferenceTest extends InstrumentRulesContractTest {

    private final InstrumentRulesV2 rules = new InstrumentRulesV2();

    @Override
    protected int lotSize(String type) {
        return rules.lotSize(type);
    }

    @Override
    protected int settlementDays(String type) {
        return rules.settlementDays(type);
    }

    @Override
    protected String label(String type) {
        return rules.label(type);
    }
}

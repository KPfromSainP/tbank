package academy.backend.market_pulse.refactoring.selfwork;

/** Самостоятельная работа: рефакторите {@link InstrumentRulesV1} — этот тест должен оставаться зелёным. */
class InstrumentRulesTest extends InstrumentRulesContractTest {

    private final InstrumentRulesV1 rules = new InstrumentRulesV1();

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

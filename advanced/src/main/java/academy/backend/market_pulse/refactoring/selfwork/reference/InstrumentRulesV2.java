package academy.backend.market_pulse.refactoring.selfwork.reference;

/** Эталон к заданию №2 (студентам заранее не показывать). */
public class InstrumentRulesV2 {

    public int lotSize(String type) {
        return InstrumentKind.of(type).lotSize();
    }

    public int settlementDays(String type) {
        return InstrumentKind.of(type).settlementDays();
    }

    public String label(String type) {
        return InstrumentKind.of(type).label();
    }
}

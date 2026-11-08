package academy.backend.market_pulse.refactoring.selfwork.reference;

/** Тип инструмента со всеми его правилами в одном месте (эталон к заданию №2). */
public enum InstrumentKind {
    STOCK("Акция", 10, 2),
    BOND("Облигация", 1, 1),
    ETF("Фонд", 1, 2);

    private final String label;
    private final int lotSize;
    private final int settlementDays;

    InstrumentKind(String label, int lotSize, int settlementDays) {
        this.label = label;
        this.lotSize = lotSize;
        this.settlementDays = settlementDays;
    }

    public String label() {
        return label;
    }

    public int lotSize() {
        return lotSize;
    }

    public int settlementDays() {
        return settlementDays;
    }

    public static InstrumentKind of(String type) {
        try {
            return valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неизвестный тип инструмента: " + type);
        }
    }
}

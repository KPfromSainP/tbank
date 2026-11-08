package academy.backend.market_pulse.refactoring.selfwork;

/**
 * Задание для самостоятельной работы №2: один и тот же {@code switch} по типу повторяется в трёх
 * методах. Приёмы: Replace Conditional with Polymorphism (или enum с полями), Extract Class. Тесты
 * {@code InstrumentRulesTest} должны оставаться зелёными.
 */
public class InstrumentRulesV1 {

    public int lotSize(String type) {
        switch (type) {
            case "STOCK":
                return 10;
            case "BOND":
                return 1;
            case "ETF":
                return 1;
            default:
                throw new IllegalArgumentException("Неизвестный тип инструмента: " + type);
        }
    }

    public int settlementDays(String type) {
        switch (type) {
            case "STOCK":
                return 2;
            case "BOND":
                return 1;
            case "ETF":
                return 2;
            default:
                throw new IllegalArgumentException("Неизвестный тип инструмента: " + type);
        }
    }

    public String label(String type) {
        switch (type) {
            case "STOCK":
                return "Акция";
            case "BOND":
                return "Облигация";
            case "ETF":
                return "Фонд";
            default:
                throw new IllegalArgumentException("Неизвестный тип инструмента: " + type);
        }
    }
}

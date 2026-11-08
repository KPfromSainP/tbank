package academy.backend.market_pulse.refactoring.errors;

/** Строка не разобрана: причина — человекочитаемый текст без номера строки. */
public class QuoteParseException extends RuntimeException {

    public QuoteParseException(String reason) {
        super(reason);
    }
}

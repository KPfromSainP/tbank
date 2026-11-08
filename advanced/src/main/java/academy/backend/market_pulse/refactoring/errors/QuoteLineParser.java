package academy.backend.market_pulse.refactoring.errors;

import java.math.BigDecimal;

/** Разбор одной строки {@code TICKER;PRICE;CHANGE}: любая проблема — {@link QuoteParseException}. */
public class QuoteLineParser {

    private static final int FIELD_COUNT = 3;

    public void validate(String line) {
        String[] parts = line.split(";");
        if (parts.length != FIELD_COUNT) {
            throw new QuoteParseException("ожидалось 3 поля");
        }
        if (parts[0].trim().isEmpty()) {
            throw new QuoteParseException("пустой тикер");
        }
        BigDecimal price = parseNumber(parts[1], "некорректная цена");
        if (price.signum() <= 0) {
            throw new QuoteParseException("цена должна быть положительной");
        }
        parseNumber(parts[2], "некорректное изменение");
    }

    private BigDecimal parseNumber(String text, String reasonIfInvalid) {
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            throw new QuoteParseException(reasonIfInvalid);
        }
    }
}

package academy.backend.market_pulse.refactoring.errors;

import java.util.ArrayList;
import java.util.List;

/** После рефакторинга: разбор строки отдельно, обработка ошибок — в одном месте. */
public class QuoteImporterV2 {

    private final QuoteLineParser parser = new QuoteLineParser();

    public ImportResult importLines(List<String> lines) {
        int imported = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            try {
                parser.validate(lines.get(i));
                imported++;
            } catch (RuntimeException e) {
                errors.add("строка " + (i + 1) + ": " + e.getMessage());
            }
        }
        return new ImportResult(imported, errors);
    }
}

package academy.backend.market_pulse.refactoring.errors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** До рефакторинга: {@code try/catch} на каждом шаге, одно и то же сообщение собирается в разных местах. */
public class QuoteImporterV1 {

    public ImportResult importLines(List<String> lines) {
        int imported = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            try {
                String[] parts = lines.get(i).split(";");
                if (parts.length != 3) {
                    errors.add("строка " + (i + 1) + ": ожидалось 3 поля");
                    continue;
                }
                String ticker = parts[0].trim();
                if (ticker.isEmpty()) {
                    errors.add("строка " + (i + 1) + ": пустой тикер");
                    continue;
                }
                BigDecimal price;
                try {
                    price = new BigDecimal(parts[1].trim());
                } catch (NumberFormatException e) {
                    errors.add("строка " + (i + 1) + ": некорректная цена");
                    continue;
                }
                if (price.signum() <= 0) {
                    errors.add("строка " + (i + 1) + ": цена должна быть положительной");
                    continue;
                }
                try {
                    new BigDecimal(parts[2].trim());
                } catch (NumberFormatException e) {
                    errors.add("строка " + (i + 1) + ": некорректное изменение");
                    continue;
                }
                imported++;
            } catch (Exception e) {
                errors.add("строка " + (i + 1) + ": " + e.getMessage());
            }
        }
        return new ImportResult(imported, errors);
    }
}

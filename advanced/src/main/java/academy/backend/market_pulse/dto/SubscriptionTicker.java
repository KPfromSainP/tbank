package academy.backend.market_pulse.dto;

import java.util.Locale;
import java.util.Objects;

/**
 * Проверенный тикер в верхнем регистре для создания и удаления подписки.
 *
 * @param value от 1 до 32 букв латиницы, цифр, точек, дефисов или подчёркиваний
 */
public record SubscriptionTicker(String value) {
    /** Удаляет пробелы по краям и проверяет длину и допустимые символы тикера. */
    public SubscriptionTicker {
        value = Objects.requireNonNull(value, "Тикер обязателен").trim().toUpperCase(Locale.ROOT);
        if (!value.matches("[A-Z0-9._-]{1,32}")) {
            throw new IllegalArgumentException("Некорректный тикер");
        }
    }
}

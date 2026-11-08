package academy.backend.market_pulse.dto;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Проверенные параметры подписки: тикер, пауза между запросами и общий срок работы.
 *
 * @param ticker нормализованный тикер в верхнем регистре
 * @param interval пауза от завершения одного запроса до следующего, от 1 наносекунды до 7 дней
 * @param lifetime общий срок новых запросов, от 1 наносекунды до 7 дней
 */
public record SubscriptionRequest(String ticker, Duration interval, Duration lifetime) {
    /** Верхняя граница интервала и срока, безопасная для сравнения времени в наносекундах. */
    private static final Duration MAX_DURATION = Duration.ofDays(7);
    /** Формат длительности в интерактивной команде: положительное число и единица измерения. */
    private static final Pattern DURATION = Pattern.compile("([1-9][0-9]*)(ms|s|m|h)");

    /** Нормализует тикер и не позволяет создать запрос с недопустимыми длительностями. */
    public SubscriptionRequest {
        ticker = new SubscriptionTicker(ticker).value();
        validateDuration(interval);
        validateDuration(lifetime);
    }

    /** Преобразует три аргумента интерактивной команды в проверенный запрос. */
    public static SubscriptionRequest parse(String ticker, String interval, String lifetime) {
        return new SubscriptionRequest(ticker, parseDuration(interval), parseDuration(lifetime));
    }

    /** Разбирает длительность с единицей ms, s, m или h и проверяет переполнение. */
    private static Duration parseDuration(String text) {
        Matcher matcher = DURATION.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Длительность: положительное число и ms/s/m/h");
        }
        try {
            long count = Long.parseLong(matcher.group(1));
            return switch (matcher.group(2)) {
                case "ms" -> Duration.ofMillis(count);
                case "s" -> Duration.ofSeconds(count);
                case "m" -> Duration.ofMinutes(count);
                case "h" -> Duration.ofHours(count);
                default -> throw new IllegalArgumentException("Неизвестная единица времени");
            };
        } catch (ArithmeticException | NumberFormatException error) {
            throw new IllegalArgumentException("Слишком большая длительность", error);
        }
    }

    /** Ограничивает длительность семью днями, чтобы безопасно сравнивать сроки в наносекундах. */
    private static void validateDuration(Duration duration) {
        Objects.requireNonNull(duration, "Длительность обязательна");
        if (duration.isZero() || duration.isNegative() || duration.compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException("Интервал и срок: больше 0, не больше 7 дней");
        }
    }
}

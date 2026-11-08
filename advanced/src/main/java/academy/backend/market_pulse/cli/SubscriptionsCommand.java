package academy.backend.market_pulse.cli;

import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.concurrent.QuoteSubscriptionService;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/** Интерактивный пример семинара 10: watch TICKER INTERVAL LIFETIME / remove TICKER / exit. */
@Command(name = "subscriptions", description = "Временные подписки на котировки через учебный пул")
public final class SubscriptionsCommand implements java.util.concurrent.Callable<Integer> {
    private static final Pattern DURATION = Pattern.compile("([1-9][0-9]*)(ms|s|m|h)");
    private final TInvestApiClient client;

    @Option(names = "--workers", defaultValue = "3", description = "Количество платформенных работников")
    private int workers;

    public SubscriptionsCommand(TInvestApiClient client) {
        this.client = client;
    }

    @Override
    public Integer call() throws Exception {
        // NOTICE: запускать с MARKET_PULSE_CLIENT=HTTP_CLIENT для демонстрации отмены HTTP.
        // Учебный endpoint отдаёт каталог целиком. Каждый refresh делает отдельный HTTP-запрос
        // и выбирает нужный тикер; ни общего кеша, ни мутаций Instrument здесь нет.
        try (var service = new QuoteSubscriptionService(workers, ticker -> client.fetchQuotes().stream()
                .filter(quote -> quote.getInstrument().getTicker().equalsIgnoreCase(ticker))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Тикер не найден: " + ticker))
                .getPrice(), System.out::println)) {
            System.out.println("watch SBER 3s 30s — интервал после ответа и общий срок; remove SBER — отменить подписку; exit — остановить всё");
            var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String line;
            while ((line = input.readLine()) != null) {
                String command = line.trim();
                if (command.equalsIgnoreCase("exit")) {
                    break;
                }
                if (command.isEmpty()) {
                    continue;
                }
                String[] parts = command.split("\\s+");
                if (parts.length == 2 && parts[0].equalsIgnoreCase("remove")) {
                    boolean removed = service.remove(parts[1]);
                    System.out.println(removed ? "Подписка удалена: " + parts[1]
                            : "Подписка не найдена: " + parts[1]);
                    continue;
                }
                if (parts.length != 4 || !parts[0].equalsIgnoreCase("watch")) {
                    System.out.println("Формат: watch TICKER 3s 30s; единицы ms/s/m/h; remove TICKER; exit");
                    continue;
                }
                try {
                    service.watch(parts[1], parseDuration(parts[2]), parseDuration(parts[3]));
                    System.out.println("Подписка принята: " + parts[1]);
                } catch (IllegalArgumentException error) {
                    System.out.println("Ошибка: " + error.getMessage());
                }
            }
        }
        System.out.println("Подписки и пул остановлены");
        return 0;
    }

    static Duration parseDuration(String value) {
        Matcher matcher = DURATION.matcher(value);
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
}

package academy.backend.market_pulse.cli;

import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.client.InterruptibleTInvestApiClient;
import academy.backend.market_pulse.concurrent.QuoteSubscriptionService;
import academy.backend.market_pulse.dto.subscription.SubscriptionConfig;
import academy.backend.market_pulse.dto.subscription.SubscriptionRequest;
import academy.backend.market_pulse.dto.subscription.SubscriptionResult;
import academy.backend.market_pulse.dto.subscription.SubscriptionTicker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Интерактивная сессия: проверяет введённые параметры и передаёт готовые запросы сервису подписок.
 */
@Command(name = "subscriptions", description = "Временные подписки на котировки через учебный пул")
public final class SubscriptionsCommand implements Callable<Integer> {
    /**
     * Клиент приложения; команда передаёт его сервису без дополнительной функции-загрузчика.
     */
    private final TInvestApiClient client;

    /**
     * Введённое число worker-потоков; перед созданием сервиса проверяется в SubscriptionConfig.
     */
    @Option(names = "--workers", defaultValue = "3", description = "Количество платформенных worker-потоков")
    private int workers;

    public SubscriptionsCommand(TInvestApiClient client) {
        this.client = client;
    }

    /**
     * Создаёт сессию и освобождает её потоки при выходе или ошибке чтения.
     * Прерывание ожидания остановки сохраняется для вызывающего кода.
     */
    @Override
    public Integer call() {
        try {
            SubscriptionConfig config = new SubscriptionConfig(workers);
            // NOTICE: нужен контракт отмены, а не конкретная реализация HTTP-клиента.
            if (!(client instanceof InterruptibleTInvestApiClient interruptibleClient)) {
                throw new IllegalArgumentException("Для subscriptions нужен прерываемый клиент; "
                        + "выберите MARKET_PULSE_CLIENT=HTTP_CLIENT");
            }
            try (var service = new QuoteSubscriptionService(config, interruptibleClient,
                    result -> System.out.println(formatResult(result)),
                    error -> System.err.println("Ошибка задачи подписки: " + error))) {
                readCommands(service);
            }
            System.out.println("Подписки и пул остановлены");
            return 0;
        } catch (IllegalArgumentException error) {
            System.err.println("Ошибка параметров: " + error.getMessage());
            return 2;
        } catch (InterruptedException error) {
            // NOTICE: прерывание — сигнал завершить команду. Сохраняем его для вызывающего кода,
            // а сервис к этому моменту уже дождался остановки своих потоков.
            Thread.currentThread().interrupt();
            System.err.println("Ожидание остановки подписок прервано");
            return 130;
        } catch (IOException error) {
            System.err.println("Не удалось прочитать команду: " + error.getMessage());
            return 1;
        }
    }

    /** Преобразует данные результата в текст только на границе консольного интерфейса. */
    static String formatResult(SubscriptionResult result) {
        String message = switch (result) {
            case SubscriptionResult.PriceUpdate update -> update.price().toPlainString();
            case SubscriptionResult.Failure failure -> "Ошибка: " + failure.cause().getMessage();
        };
        return "[" + result.ticker() + "] " + message;
    }

    /**
     * Читает строки до exit или конца ввода, сохраняя общий System.in открытым.
     * Ожидание строки системной консоли не гарантирует реакцию на Thread.interrupt;
     * штатное завершение интерактивной сессии — exit или EOF.
     */
    private void readCommands(QuoteSubscriptionService service) throws IOException {
        System.out.println("watch SBER 3s 30s — интервал после ответа и общий срок; "
                + "remove SBER — отменить подписку; exit — остановить всё");
        var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = input.readLine()) != null) {
            String command = line.trim();
            if (command.equalsIgnoreCase("exit")) {
                return;
            }
            if (!command.isEmpty()) {
                executeCommand(command, service);
            }
        }
    }

    /**
     * Проверяет формат строки и сообщает об ошибке, не завершая интерактивную сессию.
     */
    private void executeCommand(String command, QuoteSubscriptionService service) {
        String[] parts = command.split("\\s+");
        try {
            if (parts.length == 4 && parts[0].equalsIgnoreCase("watch")) {
                watch(parts, service);
            } else if (parts.length == 2 && parts[0].equalsIgnoreCase("remove")) {
                remove(parts[1], service);
            } else {
                System.out.println("Формат: watch TICKER 3s 30s; единицы ms/s/m/h; remove TICKER; exit");
            }
        } catch (IllegalArgumentException error) {
            System.out.println("Ошибка: " + error.getMessage());
        }
    }

    /**
     * Создаёт проверенный объект параметров подписки и регистрирует его в сервисе.
     */
    private void watch(String[] parts, QuoteSubscriptionService service) {
        // NOTICE: сервис получает уже нормализованный тикер и проверенные длительности.
        SubscriptionRequest request = SubscriptionRequest.parse(parts[1], parts[2], parts[3]);
        service.watch(request);
        System.out.println("Подписка принята: " + request.ticker());
    }

    /**
     * Проверяет тикер и сообщает, существовала ли отменяемая подписка.
     */
    private void remove(String value, QuoteSubscriptionService service) {
        SubscriptionTicker ticker = new SubscriptionTicker(value);
        boolean removed = service.remove(ticker);
        System.out.println(removed ? "Подписка удалена: " + ticker.value()
                : "Подписка не найдена: " + ticker.value());
    }
}

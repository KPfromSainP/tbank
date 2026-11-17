package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.client.InterruptibleTInvestApiClient;
import academy.backend.market_pulse.dto.subscription.SubscriptionConfig;
import academy.backend.market_pulse.dto.subscription.SubscriptionRequest;
import academy.backend.market_pulse.dto.subscription.SubscriptionResult;
import academy.backend.market_pulse.dto.subscription.SubscriptionTicker;
import academy.backend.market_pulse.model.Quote;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Вариант подписок на стандартных executor с отменой через Future.
 * Scheduler назначает сроки, фиксированный пул выполняет HTTP. Команды вызываются последовательно из CLI.
 */
public final class ExecutorQuoteSubscriptionService implements QuoteSubscriptions {
    /**
     * Map связывает тикер с конкретным экземпляром подписки, включая повторную регистрацию.
     */
    private final ConcurrentMap<String, ExecutorSubscription> subscriptions = new ConcurrentHashMap<>();
    /**
     * Единственный scheduler назначает задачи, а HTTP выполняется в отдельном пуле.
     */
    private final ScheduledThreadPoolExecutor scheduler;
    /**
     * Фиксированный пул с неограниченной FIFO-очередью стандартного executor.
     */
    private final ExecutorService workers;
    /**
     * Прерываемый клиент с поддержкой одновременных запросов.
     */
    private final InterruptibleTInvestApiClient client;
    /**
     * Callback типизированного результата.
     */
    private final SubscriptionResultHandler resultHandler;
    /**
     * Отдельный callback для ошибки задачи, включая сбой resultHandler.
     */
    private final Consumer<RuntimeException> onTaskFailure;
    /**
     * Используется только потоком команд CLI.
     */
    private boolean closed;

    public ExecutorQuoteSubscriptionService(SubscriptionConfig config, InterruptibleTInvestApiClient client,
            SubscriptionResultHandler resultHandler, Consumer<RuntimeException> onTaskFailure) {
        this.client = Objects.requireNonNull(client);
        this.resultHandler = Objects.requireNonNull(resultHandler);
        this.onTaskFailure = Objects.requireNonNull(onTaskFailure);
        this.workers = Executors.newFixedThreadPool(config.workers(),
                Thread.ofPlatform().name("executor-quote-", 1).factory());
        this.scheduler = new ScheduledThreadPoolExecutor(1,
                Thread.ofPlatform().name("quote-scheduler").factory());
        this.scheduler.setRemoveOnCancelPolicy(true);
    }

    @Override
    public void watch(SubscriptionRequest request) {
        if (closed) {
            throw new IllegalStateException("Сервис закрыт");
        }
        ExecutorSubscription previous = subscriptions.get(request.ticker());
        if (previous != null && previous.isExpiredAndNotRunning(System.nanoTime())) {
            previous.cancel();
            subscriptions.remove(request.ticker(), previous);
        }
        ExecutorSubscription subscription = new ExecutorSubscription(request);
        if (subscriptions.putIfAbsent(request.ticker(), subscription) != null) {
            throw new IllegalArgumentException("Подписка уже существует: " + request.ticker());
        }
        schedule(subscription, 0);
    }

    /**
     * Назначает один запуск, ограничивая задержку оставшимся сроком подписки.
     */
    private void schedule(ExecutorSubscription subscription, long delay) {
        synchronized (subscription) {
            if (subscription.cancelled) {
                return;
            }
            long remaining = subscription.expiresAt - System.nanoTime();
            if (remaining <= 0) {
                subscriptions.remove(subscription.ticker, subscription);
                return;
            }
            subscription.next = scheduler.schedule(() -> dispatch(subscription),
                    Math.min(delay, remaining), TimeUnit.NANOSECONDS);
        }
    }

    /**
     * Публикует Future до передачи в executor, чтобы remove мог отменить и queued-задачу.
     */
    private void dispatch(ExecutorSubscription subscription) {
        synchronized (subscription) {
            if (subscription.cancelled) {
                return;
            }
            if (System.nanoTime() - subscription.expiresAt >= 0) {
                subscriptions.remove(subscription.ticker, subscription);
                return;
            }
            FutureTask<Void> task = new FutureTask<>(() -> {
                try {
                    refresh(subscription);
                } catch (RuntimeException error) {
                    reportTaskFailure(error);
                }
                return null;
            });
            // NOTICE: remove берёт тот же монитор. Future доступен до того, как task начнёт выполняться.
            subscription.request = task;
            workers.execute(task);
        }
    }

    /**
     * Завершение запроса назначает следующую паузу, сохраняя fixed delay и отсутствие перекрытия HTTP.
     */
    private void refresh(ExecutorSubscription subscription) {
        try {
            if (subscription.beginRequest()) {
                SubscriptionResult result = loadResult(subscription.ticker);
                subscription.publish(() -> resultHandler.handle(result));
            }
        } finally {
            synchronized (subscription) {
                subscription.running = false;
                subscription.request = null;
                schedule(subscription, subscription.intervalNanos);
            }
        }
    }

    private SubscriptionResult loadResult(String ticker) {
        try {
            Quote quote = client.fetchQuotes().stream()
                    .filter(value -> value.getInstrument().getTicker().equalsIgnoreCase(ticker))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Тикер не найден: " + ticker));
            return new SubscriptionResult.PriceUpdate(ticker, quote.getPrice());
        } catch (RuntimeException error) {
            return new SubscriptionResult.Failure(ticker, error);
        }
    }

    private void reportTaskFailure(RuntimeException error) {
        try {
            onTaskFailure.accept(error);
        } catch (RuntimeException reportingError) {
            System.err.println("Ошибка обработчика задачи: " + reportingError);
        }
    }

    @Override
    public boolean remove(SubscriptionTicker ticker) {
        ExecutorSubscription subscription = subscriptions.remove(ticker.value());
        if (subscription == null) {
            return false;
        }
        subscription.cancel();
        return true;
    }

    /**
     * Останавливает scheduler перед worker-пулом и возвращается после завершения кода запросов.
     */
    @Override
    public void close() throws InterruptedException {
        closed = true;
        subscriptions.values().forEach(ExecutorSubscription::cancel);
        subscriptions.clear();
        scheduler.shutdownNow();
        scheduler.close();
        workers.shutdownNow();
        workers.close();
        // NOTICE: стандартный close дожидается потоков и восстанавливает флаг прерывания вызывающего потока.
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Ожидание остановки подписок было прервано");
        }
    }
}

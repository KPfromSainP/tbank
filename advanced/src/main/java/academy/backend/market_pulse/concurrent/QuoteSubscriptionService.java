package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.dto.subscription.SubscriptionConfig;
import academy.backend.market_pulse.dto.subscription.SubscriptionRequest;
import academy.backend.market_pulse.dto.subscription.SubscriptionTicker;
import academy.backend.market_pulse.model.Quote;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.LockSupport;

/**
 * Управляет временными подписками: dispatcher следит за сроками, worker-потоки загружают котировки.
 * Параметры проверяются при создании DTO; каждая подписка защищает своё состояние.
 * Команды watch, remove и close вызываются последовательно из потока CLI.
 * Параллельные вызовы этих команд не входят в контракт учебного сервиса.
 */
public final class QuoteSubscriptionService implements AutoCloseable {
    /**
     * Верхняя граница ожидания dispatcher, если ближайшего запроса пока нет.
     */
    private static final long MAX_WAIT_NANOS = Duration.ofSeconds(1).toNanos();
    /**
     * Map подписок: добавление и удаление не требуют общего монитора сервиса.
     */
    private final ConcurrentMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    /**
     * Платформенные worker-потоки, выполняющие сетевые запросы вне потока dispatcher.
     */
    private final TreiberThreadPool pool;
    /**
     * Единственный поток, который выбирает подписки с наступившим сроком запуска.
     */
    private final Thread dispatcher;
    /**
     * Общий клиент; его реализации допускают одновременные запросы разных worker-потоков.
     */
    private final TInvestApiClient client;
    /**
     * Получатель сообщений; разные тикеры могут вызывать его одновременно.
     */
    private final SubscriptionResultHandler resultHandler;
    /**
     * Признак завершения, видимый вызывающим потокам и dispatcher без блокировки.
     */
    private volatile boolean closed;

    /**
     * Создаёт пул и запускает поток dispatcher с уже проверенными параметрами сессии.
     */
    public QuoteSubscriptionService(SubscriptionConfig config, TInvestApiClient client,
                                    SubscriptionResultHandler resultHandler) {
        this.client = Objects.requireNonNull(client);
        this.resultHandler = Objects.requireNonNull(resultHandler);
        this.pool = new TreiberThreadPool(config.workers(), error -> resultHandler.handle("Ошибка задачи: " + error));
        this.dispatcher = Thread.ofPlatform()
                .name("quote-dispatcher")
                .start(this::dispatch);
    }

    /**
     * Регистрирует подписку, отклоняя повторный тикер и регистрацию после закрытия сервиса.
     */
    public void watch(SubscriptionRequest request) {
        if (closed) {
            throw new IllegalStateException("Сервис закрыт");
        }
        Subscription subscription = new Subscription(request);
        if (subscriptions.putIfAbsent(request.ticker(), subscription) != null) {
            throw new IllegalArgumentException("Подписка уже существует: " + request.ticker());
        }
        wakeDispatcher();
    }

    /**
     * Удаляет подписку и отменяет только принадлежащий ей запрос.
     */
    public boolean remove(SubscriptionTicker ticker) {
        Subscription subscription = subscriptions.remove(ticker.value());
        if (subscription == null) {
            return false;
        }
        subscription.cancel();
        wakeDispatcher();
        return true;
    }

    /**
     * Обходит подписки и ожидает ближайшего срока или уведомления об изменении состояния.
     */
    private void dispatch() {
        while (!closed) {
            long delay = dispatchDueRequests();
            // NOTICE: park может вернуться без unpark. Каждый проход заново проверяет сроки;
            // unpark до park сохраняет permit и не теряет сигнал об изменении подписок.
            LockSupport.parkNanos(this, delay);
        }
    }

    /**
     * Удаляет истёкшие подписки, передаёт готовые запросы в пул и находит ближайший срок.
     */
    private long dispatchDueRequests() {
        long now = System.nanoTime();
        long delay = MAX_WAIT_NANOS;
        for (Subscription subscription : subscriptions.values()) {
            if (subscription.isExpiredAndIdle(now)) {
                subscriptions.remove(subscription.ticker(), subscription);
            } else {
                submitIfDue(subscription, now);
                delay = Math.min(delay, subscription.delayUntilNextCheck(now));
            }
        }
        return delay;
    }

    /**
     * Передаёт в пул один запрос, зарезервированный для готовой к обновлению подписки.
     */
    private void submitIfDue(Subscription subscription, long now) {
        if (subscription.scheduleIfDue(now)) {
            // Пул закрывается только после join dispatcher, поэтому submit здесь не гоняется с close пула.
            pool.submit(() -> refresh(subscription));
        }
    }

    /**
     * Загружает котировку вне блокировки подписки и завершает запрос при любом исходе.
     */
    private void refresh(Subscription subscription) {
        try {
            if (!subscription.beginRequest()) {
                return;
            }
            Quote quote = loadQuote(subscription.ticker());
            subscription.publish(quote.getPrice().toPlainString(), resultHandler);
        } catch (RuntimeException error) {
            // NOTICE: клиент сохраняет флаг при InterruptedException. Подписку уже отменил
            // remove или close; повторная отмена и удаление из Map здесь не нужны.
            if (!Thread.currentThread().isInterrupted()) {
                subscription.publish("Ошибка: " + error.getMessage(), resultHandler);
            }
        } finally {
            subscription.finishRequest();
            wakeDispatcher();
        }
    }

    /**
     * Загружает каталог через общий клиент и находит котировку нужного тикера.
     */
    private Quote loadQuote(String ticker) {
        // NOTICE: сетевой запрос не удерживает монитор подписки, поэтому remove может
        // сразу запросить прерывание HttpClient.send, даже если сервер ещё не ответил.
        return client.fetchQuotes().stream()
                .filter(quote -> quote.getInstrument().getTicker().equalsIgnoreCase(ticker))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Тикер не найден: " + ticker));
    }

    private void wakeDispatcher() {
        LockSupport.unpark(dispatcher);
    }

    /**
     * Отменяет подписки, завершает dispatcher, затем закрывает пул.
     * При прерывании сначала освобождает потоки и только затем возвращает сигнал вызывающему коду.
     */
    @Override
    public void close() throws InterruptedException {
        closed = true;
        subscriptions.values().forEach(Subscription::cancel);
        subscriptions.clear();
        dispatcher.interrupt();

        boolean interrupted = Thread.interrupted();
        while (dispatcher.isAlive()) {
            try {
                dispatcher.join();
            } catch (InterruptedException error) {
                interrupted = true;
            }
        }
        // NOTICE: dispatcher — единственный producer задач. После его join новых submit не будет.
        try {
            pool.close();
        } catch (InterruptedException error) {
            interrupted = true;
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Ожидание остановки подписок было прервано");
        }
    }
}

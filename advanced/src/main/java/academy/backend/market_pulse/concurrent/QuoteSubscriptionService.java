package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.client.TInvestApiClient;
import academy.backend.market_pulse.dto.SubscriptionRequest;
import academy.backend.market_pulse.dto.SubscriptionSessionOptions;
import academy.backend.market_pulse.dto.SubscriptionTicker;
import academy.backend.market_pulse.model.Quote;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * Управляет временными подписками: диспетчер следит за сроками, работники загружают котировки.
 * Параметры проверяются при создании объектов запросов; каждая подписка защищает своё состояние.
 */
public final class QuoteSubscriptionService implements AutoCloseable {
    /** Верхняя граница ожидания диспетчера, если ближайшего запроса пока нет. */
    private static final long MAX_WAIT_NANOS = Duration.ofSeconds(1).toNanos();
    /** Реестр тикеров: добавление и удаление не требуют общего монитора сервиса. */
    private final ConcurrentMap<String, Subscription> subscriptions = new ConcurrentHashMap<>();
    /** Платформенные работники, выполняющие сетевые запросы вне потока диспетчера. */
    private final TreiberThreadPool pool;
    /** Единственный поток, который выбирает подписки с наступившим сроком запуска. */
    private final Thread dispatcher;
    /** Общий клиент; его реализации допускают одновременные запросы разных работников. */
    private final TInvestApiClient client;
    /** Получатель сообщений; разные тикеры могут вызывать его одновременно. */
    private final Consumer<String> output;
    /** Признак завершения, видимый вызывающим потокам и диспетчеру без блокировки. */
    private volatile boolean closed;

    /** Создаёт пул и запускает поток диспетчера с уже проверенными параметрами сессии. */
    public QuoteSubscriptionService(SubscriptionSessionOptions options, TInvestApiClient client,
                                    Consumer<String> output) {
        this.client = Objects.requireNonNull(client);
        this.output = Objects.requireNonNull(output);
        pool = new TreiberThreadPool(options.workers(), error -> output.accept("Ошибка задачи: " + error));
        dispatcher = Thread.ofPlatform().name("quote-dispatcher").unstarted(this::dispatch);
        dispatcher.start();
    }

    /** Регистрирует подписку, отклоняя повторный тикер и регистрацию после закрытия сервиса. */
    public void watch(SubscriptionRequest request) {
        requireOpen();
        Subscription subscription = new Subscription(request);
        if (subscriptions.putIfAbsent(request.ticker(), subscription) != null) {
            throw new IllegalArgumentException("Подписка уже существует: " + request.ticker());
        }
        // NOTICE: close мог начаться между первой проверкой и добавлением в карту.
        // Повторная проверка убирает такую подписку без общей блокировки всех тикеров.
        if (closed) {
            subscription.cancel();
            subscriptions.remove(request.ticker(), subscription);
            throw new IllegalStateException("Сервис закрыт");
        }
        wakeDispatcher();
    }

    /** Удаляет подписку и отменяет только принадлежащий ей запрос. */
    public boolean remove(SubscriptionTicker ticker) {
        requireOpen();
        Subscription subscription = subscriptions.get(ticker.value());
        if (subscription == null) {
            return false;
        }
        subscription.cancel();
        boolean removed = subscriptions.remove(ticker.value(), subscription);
        wakeDispatcher();
        return removed;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Сервис закрыт");
        }
    }

    /** Обходит подписки и ожидает ближайшего срока или уведомления об изменении состояния. */
    private void dispatch() {
        try {
            while (!closed && !Thread.currentThread().isInterrupted()) {
                long delay = dispatchDueRequests();
                // NOTICE: park может вернуться без уведомления. Каждый новый обход заново
                // проверяет сроки; unpark до park сохраняет разрешение и не теряет изменение.
                LockSupport.parkNanos(this, delay);
            }
        } finally {
            // Прерывание диспетчера означает завершение сервиса, а не продолжение пустого цикла.
            shutdown();
        }
    }

    /** Удаляет истёкшие подписки, передаёт готовые запросы в пул и находит ближайший срок. */
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

    /** Передаёт зарезервированный запрос в пул, учитывая возможное одновременное закрытие. */
    private void submitIfDue(Subscription subscription, long now) {
        if (!closed && subscription.scheduleIfDue(now)) {
            try {
                pool.submit(() -> refresh(subscription));
            } catch (IllegalStateException error) {
                subscription.finishRequest();
                if (!closed) {
                    throw error;
                }
            }
        }
    }

    /** Загружает котировку вне блокировки подписки и завершает запрос при любом исходе. */
    private void refresh(Subscription subscription) {
        try {
            if (closed || !subscription.beginRequest()) {
                return;
            }
            Quote quote = loadQuote(subscription.ticker());
            subscription.publish(quote.getPrice().toPlainString(), output);
        } catch (RuntimeException error) {
            if (Thread.currentThread().isInterrupted()) {
                // NOTICE: HttpClientTInvestApiClient восстанавливает флаг и оборачивает
                // InterruptedException. Отмена завершает подписку и не становится сетевой ошибкой.
                subscription.cancel();
                subscriptions.remove(subscription.ticker(), subscription);
            } else {
                subscription.publish("Ошибка: " + error.getMessage(), output);
            }
        } finally {
            subscription.finishRequest();
            wakeDispatcher();
        }
    }

    /** Загружает каталог через общий клиент и находит котировку нужного тикера. */
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

    /** Запрещает новые подписки и запрашивает отмену диспетчера и всех запросов. */
    private void shutdown() {
        closed = true;
        subscriptions.values().forEach(Subscription::cancel);
        subscriptions.clear();
        dispatcher.interrupt();
        pool.shutdown();
    }

    /** Останавливает сервис, дожидается потоков и восстанавливает прерывание вызывающего потока. */
    @Override
    public void close() throws InterruptedException {
        shutdown();
        boolean interrupted = Thread.interrupted();
        while (true) {
            try {
                dispatcher.join();
                pool.awaitTermination();
                break;
            } catch (InterruptedException error) {
                // NOTICE: нельзя бросить ожидание на полпути и оставить работающие потоки.
                // Сначала завершаем освобождение ресурсов, затем возвращаем сигнал вызывающему коду.
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Ожидание остановки подписок было прервано");
        }
    }
}

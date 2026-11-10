package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.dto.subscription.SubscriptionRequest;

import java.time.Duration;

/**
 * Состояние одной подписки: сроки, поставленный запрос и выполняющий его поток.
 * Блокировка этого объекта согласует отмену и завершение только одного тикера.
 */
final class Subscription {
    /**
     * Предел ожидания, пока запрос уже поставлен в пул или выполняется.
     */
    private static final long MAX_WAIT_NANOS = Duration.ofSeconds(1).toNanos();
    /**
     * Нормализованный тикер; после создания не меняется.
     */
    private final String ticker;
    /**
     * Пауза от завершения одного запроса до готовности следующего.
     */
    private final long intervalNanos;
    /**
     * Момент прекращения новых запросов в шкале System.nanoTime.
     */
    private final long expiresAt;
    /**
     * Момент готовности следующего запроса; защищён монитором этой подписки.
     */
    private long nextRun;
    /**
     * Запрос уже ждёт worker-потока или выполняется; защищён монитором этой подписки.
     */
    private boolean requestScheduled;
    /**
     * Отмена видна обработчику вывода без захвата монитора состояния подписки.
     */
    private volatile boolean cancelled;
    /**
     * Согласует только вывод и его завершение при отмене; dispatcher этот монитор не захватывает.
     */
    private final Object resultLock = new Object();
    /**
     * Worker текущего запроса, либо null; чтение и interrupt согласованы с его освобождением.
     */
    private Thread runner;

    /**
     * Запоминает проверенные параметры; первый запрос готов к выполнению сразу.
     */
    Subscription(SubscriptionRequest request) {
        this.ticker = request.ticker();
        this.intervalNanos = request.interval().toNanos();
        this.nextRun = System.nanoTime();
        this.expiresAt = nextRun + request.lifetime().toNanos();
    }

    String ticker() {
        return ticker;
    }

    /**
     * Резервирует ровно один запрос, если наступил его срок и подписка ещё действует.
     */
    synchronized boolean scheduleIfDue(long now) {
        if (cancelled || requestScheduled || hasExpired(now) || now - nextRun < 0) {
            return false;
        }
        // NOTICE: резервируем запрос до передачи в пул, чтобы следующий обход dispatcher
        // не добавил второй запрос для того же тикера, пока первый ещё ждёт worker-потока.
        requestScheduled = true;
        return true;
    }

    /**
     * Привязывает запрос к worker-потоку, если подписка не отменена и не истекла в очереди.
     */
    synchronized boolean beginRequest() {
        if (cancelled || hasExpired(System.nanoTime())) {
            return false;
        }
        runner = Thread.currentThread();
        return true;
    }

    /**
     * Освобождает worker и отсчитывает следующую паузу от завершения запроса.
     */
    synchronized void finishRequest() {
        runner = null;
        requestScheduled = false;
        nextRun = System.nanoTime() + intervalNanos;
    }

    /**
     * Проверяет, можно ли удалить истёкшую подписку без незавершённого запроса.
     */
    synchronized boolean isExpiredAndIdle(long now) {
        return hasExpired(now) && !requestScheduled;
    }

    /**
     * Вычисляет время следующей проверки, не заставляя dispatcher опрашивать активный запрос.
     */
    synchronized long delayUntilNextCheck(long now) {
        if (requestScheduled || cancelled) {
            return MAX_WAIT_NANOS;
        }
        return Math.max(1, Math.min(MAX_WAIT_NANOS, Math.min(nextRun - now, expiresAt - now)));
    }

    /**
     * Отмечает отмену и прерывает worker, пока он ещё закреплён за этим запросом.
     */
    void cancel() {
        synchronized (this) {
            cancelled = true;
            // NOTICE: finishRequest использует тот же монитор. Worker не успеет перейти
            // к чужой задаче между чтением runner и interrupt, поэтому отменяется нужный запрос.
            if (runner != null) {
                runner.interrupt();
            }
        }
        synchronized (resultLock) {
            // Дожидаемся уже начавшегося вывода. Состояние подписки при этом не заблокировано.
        }
    }

    /**
     * Выводит сообщение действующей подписки, не удерживая монитор, нужный dispatcher.
     */
    void publish(String message, SubscriptionResultHandler resultHandler) {
        synchronized (resultLock) {
            if (!cancelled && !Thread.currentThread().isInterrupted()) {
                // NOTICE: отдельный монитор согласует вывод с cancel. Медленный получатель
                // сообщений не задерживает обход подписок и запуск запросов других тикеров.
                resultHandler.handle("[" + ticker + "] " + message);
            }
        }
    }

    /**
     * Сравнивает сроки по разности, не полагаясь на абсолютное значение nanoTime.
     */
    private boolean hasExpired(long now) {
        return now - expiresAt >= 0;
    }
}

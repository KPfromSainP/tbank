package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.dto.subscription.SubscriptionRequest;

/**
 * Состояние одной подписки: сроки, поставленный запрос и выполняющий его поток.
 * Блокировка этого объекта согласует отмену и завершение только одного тикера.
 * Запрос проходит IDLE → QUEUED → RUNNING → IDLE; runner задан только в RUNNING.
 * Отмена независима от фазы запроса: прерванный worker ещё должен завершить текущую работу.
 */
final class Subscription {
    private enum RequestState {
        IDLE, QUEUED, RUNNING
    }

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
     * Фаза запроса; защищена монитором этой подписки.
     */
    private RequestState requestState = RequestState.IDLE;
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
        if (cancelled || requestState != RequestState.IDLE || hasExpired(now) || now - nextRun < 0) {
            return false;
        }
        // NOTICE: резервируем запрос до передачи в пул, чтобы следующий обход dispatcher
        // не добавил второй запрос для того же тикера, пока первый ещё ждёт worker-потока.
        requestState = RequestState.QUEUED;
        return true;
    }

    /**
     * Привязывает запрос к worker-потоку, если подписка не отменена и не истекла в очереди.
     */
    synchronized boolean beginRequest() {
        if (cancelled || requestState != RequestState.QUEUED || hasExpired(System.nanoTime())) {
            return false;
        }
        runner = Thread.currentThread();
        requestState = RequestState.RUNNING;
        return true;
    }

    /**
     * Освобождает worker и отсчитывает следующую паузу от завершения запроса.
     */
    synchronized void finishRequest() {
        runner = null;
        requestState = RequestState.IDLE;
        nextRun = System.nanoTime() + intervalNanos;
    }

    /**
     * Проверяет, можно ли удалить истёкшую подписку без выполняющегося запроса.
     * Ожидающая в стеке задача не удерживает тикер: beginRequest отклонит её по сроку.
     */
    synchronized boolean isExpiredAndNotRunning(long now) {
        return hasExpired(now) && requestState != RequestState.RUNNING;
    }

    /**
     * Возвращает время до запуска или истечения срока; ограничение сна выбирает dispatcher.
     * Для работающего или отменённого запроса таймер не нужен: завершение разбудит dispatcher.
     */
    synchronized long delayUntilNextCheck(long now) {
        if (cancelled || requestState == RequestState.RUNNING) {
            return Long.MAX_VALUE;
        }
        long untilExpiry = expiresAt - now;
        return Math.max(0, requestState == RequestState.QUEUED
                ? untilExpiry : Math.min(nextRun - now, untilExpiry));
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
     * Выполняет публикацию действующей подписки, не удерживая монитор, нужный dispatcher.
     */
    void publish(Runnable publication) {
        synchronized (resultLock) {
            if (!cancelled && !Thread.currentThread().isInterrupted()) {
                // NOTICE: отдельный монитор согласует вывод с cancel. Медленный получатель
                // сообщений не задерживает обход подписок и запуск запросов других тикеров.
                publication.run();
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

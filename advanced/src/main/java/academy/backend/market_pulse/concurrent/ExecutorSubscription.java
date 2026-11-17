package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.dto.subscription.SubscriptionRequest;

import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;

/**
 * Состояние подписки на executor. Поля расписания и запроса защищены монитором этого объекта.
 * Future хранит механизм отмены, running отражает фактическое выполнение кода запроса.
 */
final class ExecutorSubscription {
    /**
     * Нормализованный тикер, сохранённый в DTO запроса.
     */
    final String ticker;
    /**
     * Пауза после завершения HTTP-запроса.
     */
    final long intervalNanos;
    /**
     * Момент окончания новых запросов в шкале nanoTime.
     */
    final long expiresAt;
    /**
     * Таймер следующего запуска, назначенный scheduler.
     */
    ScheduledFuture<?> next;
    /**
     * Конкретная загрузка в очереди или worker-потоке, отменяемая через Future.
     */
    FutureTask<Void> request;
    /**
     * Код загрузки уже начал выполняться и ещё не прошёл свой finally.
     */
    boolean running;
    /**
     * Отмена защищена монитором подписки, как назначение Future.
     */
    boolean cancelled;
    /**
     * Согласует callback и возврат из remove, сохраняя доступ scheduler к состоянию.
     */
    private final Object resultLock = new Object();

    ExecutorSubscription(SubscriptionRequest parameters) {
        this.ticker = parameters.ticker();
        this.intervalNanos = parameters.interval().toNanos();
        this.expiresAt = System.nanoTime() + parameters.lifetime().toNanos();
    }

    synchronized boolean isExpiredAndNotRunning(long now) {
        return now - expiresAt >= 0 && !running;
    }

    /**
     * Запрос мог истечь в очереди executor, поэтому срок проверяется перед HTTP.
     */
    synchronized boolean beginRequest() {
        if (cancelled || System.nanoTime() - expiresAt >= 0) {
            return false;
        }
        running = true;
        return true;
    }

    /**
     * Отмена согласована с назначением Future и с уже начавшейся публикацией.
     */
    void cancel() {
        synchronized (this) {
            cancelled = true;
            if (next != null) {
                next.cancel(false);
            }
            if (request != null) {
                // NOTICE: Future адресует конкретную задачу, даже если worker уже переиспользован.
                // cancel(true) меняет состояние Future сразу, а код запроса завершается кооперативно.
                request.cancel(true);
            }
        }
        synchronized (resultLock) {
            // Дожидаемся callback, который начался до отмены.
        }
    }

    /**
     * Callback выполняется без монитора, который нужен scheduler для обхода состояния.
     */
    void publish(Runnable publication) {
        synchronized (resultLock) {
            synchronized (this) {
                if (cancelled || Thread.currentThread().isInterrupted()) {
                    return;
                }
            }
            publication.run();
        }
    }
}

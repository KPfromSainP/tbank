package academy.backend.market_pulse.concurrent;

/**
 * Callback для результата или ошибки подписки, подготовленных к выводу.
 * Вызовы могут приходить из разных worker-потоков, поэтому реализация должна быть потокобезопасной.
 */
@FunctionalInterface
public interface SubscriptionResultHandler {
    void handle(String result);
}

package academy.backend.market_pulse.concurrent;

/**
 * Callback для результата или ошибки подписки, подготовленных к выводу.
 * Вызовы могут приходить из разных worker-потоков, поэтому реализация должна быть потокобезопасной.
 * Отмена ждёт уже начавшийся callback: он должен быстро завершаться или реагировать на interrupt.
 * RuntimeException из callback выводится в stderr обработчиком пула; worker продолжает работу.
 */
@FunctionalInterface
public interface SubscriptionResultHandler {
    void handle(String result);
}

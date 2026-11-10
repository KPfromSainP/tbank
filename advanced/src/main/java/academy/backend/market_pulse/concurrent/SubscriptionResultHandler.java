package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.dto.subscription.SubscriptionResult;

/**
 * Получатель котировки или ошибки подписки; формат представления выбирает реализация.
 * Вызовы могут приходить из разных worker-потоков, поэтому реализация должна быть потокобезопасной.
 * Отмена ждёт уже начавшийся callback: он должен быстро завершаться или реагировать на interrupt.
 * RuntimeException из callback передаётся отдельному обработчику ошибок задач; worker продолжает работу.
 */
@FunctionalInterface
public interface SubscriptionResultHandler {
    void handle(SubscriptionResult result);
}

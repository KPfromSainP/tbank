package academy.backend.market_pulse.dto.subscription;

/**
 * Вариант исполнения подписок, выбираемый аргументом --engine.
 */
public enum SubscriptionEngine {
    treiber, queue, executor
}

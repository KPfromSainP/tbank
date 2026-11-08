package academy.backend.market_pulse.dto;

/**
 * Проверенные параметры интерактивной сессии подписок.
 *
 * @param workers положительное число платформенных работников для выполнения запросов
 */
public record SubscriptionSessionOptions(int workers) {
    /** Не позволяет начать сессию без работников для выполнения запросов. */
    public SubscriptionSessionOptions {
        if (workers <= 0) {
            throw new IllegalArgumentException("Число работников должно быть положительным");
        }
    }
}

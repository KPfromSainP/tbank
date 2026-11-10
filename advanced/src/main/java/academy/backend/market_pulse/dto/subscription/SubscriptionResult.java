package academy.backend.market_pulse.dto.subscription;

import java.math.BigDecimal;
import java.util.Objects;

/** Результат запроса без привязки к формату вывода; ошибка сохраняет исходную причину. */
public sealed interface SubscriptionResult {
    String ticker();

    /** Неизменяемый снимок полученной цены. */
    record PriceUpdate(String ticker, BigDecimal price) implements SubscriptionResult {
        public PriceUpdate {
            Objects.requireNonNull(ticker);
            Objects.requireNonNull(price);
        }
    }

    /**
     * Ошибка загрузки котировки; не включает сбои обработчика результата.
     * Исходное исключение передаётся по ссылке для диагностики; получатель не должен изменять его.
     */
    record Failure(String ticker, RuntimeException cause) implements SubscriptionResult {
        public Failure {
            Objects.requireNonNull(ticker);
            Objects.requireNonNull(cause);
        }
    }
}

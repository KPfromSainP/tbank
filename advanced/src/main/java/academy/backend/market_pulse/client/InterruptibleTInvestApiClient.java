package academy.backend.market_pulse.client;

import academy.backend.market_pulse.exception.TInvestApiException;

/**
 * Клиент для параллельных подписок: допускает одновременные {@link #fetchQuotes()} из разных потоков.
 * Прерывание блокирующего ожидания завершает запрос с {@link TInvestApiException},
 * сохраняя флаг interrupt у вызывающего потока. Это позволяет освободить worker при отмене.
 * Обычный {@link TInvestApiClient} таких гарантий не даёт.
 */
@FunctionalInterface
public interface InterruptibleTInvestApiClient extends TInvestApiClient {
}

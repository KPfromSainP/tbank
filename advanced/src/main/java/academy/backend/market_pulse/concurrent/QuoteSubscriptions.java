package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.client.InterruptibleTInvestApiClient;
import academy.backend.market_pulse.dto.subscription.SubscriptionConfig;
import academy.backend.market_pulse.dto.subscription.SubscriptionEngine;
import academy.backend.market_pulse.dto.subscription.SubscriptionRequest;
import academy.backend.market_pulse.dto.subscription.SubscriptionTicker;

import java.util.function.Consumer;

/**
 * Контракт команд подписки, общий для собственного пула и стандартных executor.
 */
public interface QuoteSubscriptions extends AutoCloseable {
    void watch(SubscriptionRequest request);

    boolean remove(SubscriptionTicker ticker);

    @Override
    void close() throws InterruptedException;

    static QuoteSubscriptions create(SubscriptionEngine engine, SubscriptionConfig config,
            InterruptibleTInvestApiClient client, SubscriptionResultHandler resultHandler,
            Consumer<RuntimeException> onTaskFailure) {
        return switch (engine) {
            case treiber -> new QuoteSubscriptionService(config, client, resultHandler, onTaskFailure);
            case queue -> new QuoteSubscriptionService(new QueueThreadPool(config.workers(), onTaskFailure),
                    client, resultHandler);
            case executor -> new ExecutorQuoteSubscriptionService(config, client, resultHandler, onTaskFailure);
        };
    }
}

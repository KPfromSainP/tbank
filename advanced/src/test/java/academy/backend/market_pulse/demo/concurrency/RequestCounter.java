package academy.backend.market_pulse.demo.concurrency;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Счётчик запросов для упражнения на атомарное обновление одного ключа.
 */
final class RequestCounter {
    public final Map<String, Integer> counts = new ConcurrentHashMap<>();

    public void record(String key) {
        counts.merge(key, 1, Integer::sum);
    }
}

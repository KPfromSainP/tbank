package academy.backend.market_pulse.demo.concurrency;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Счётчик запросов для упражнения на атомарное обновление одного ключа.
 */
final class RequestCounter {
    final ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();

    void record(String key) {
        // TODO: исправьте потерю обновлений при одновременной записи по одному ключу.
        counts.put(key, counts.getOrDefault(key, 0) + 1);
    }
}

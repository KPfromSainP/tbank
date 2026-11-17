package academy.backend.market_pulse.demo.concurrency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(5)
class ConcurrentCollectionsDemoTest {
    @Test
    @DisplayName("synchronizedMap: составной сценарий защищаем тем же монитором")
    void synchronizedWrapper() {
        var prices = Collections.synchronizedMap(new HashMap<String, Integer>());
        synchronized (prices) {
            if (!prices.containsKey("SBER")) {
                prices.put("SBER", 300);
            }
            prices.forEach((ticker, price) -> System.out.println(ticker + " = " + price));
        }
        assertEquals(300, prices.get("SBER"));
    }

    @Test
    @DisplayName("ConcurrentHashMap: merge вместо неатомарного get + put")
    void atomicPerTickerCounter() throws Exception {
        ConcurrentHashMap<String, Integer> requests = new ConcurrentHashMap<>();
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<java.util.concurrent.Future<?>> tasks = new ArrayList<>();
            for (int i = 0; i < 1_000; i++) {
                tasks.add(pool.submit(() -> requests.merge("SBER", 1, Integer::sum)));
            }
            for (var task : tasks) {
                task.get(2, TimeUnit.SECONDS);
            }
        }
        assertEquals(1_000, requests.get("SBER"));
    }

    @Test
    @DisplayName("CopyOnWriteArrayList: итератор видит снимок до изменения")
    void snapshotIterator() {
        var tickers = new CopyOnWriteArrayList<>(List.of("SBER", "GAZP"));
        var snapshot = tickers.iterator();
        tickers.add("LKOH");
        List<String> seen = new ArrayList<>();
        snapshot.forEachRemaining(seen::add);
        assertEquals(List.of("SBER", "GAZP"), seen);
        assertEquals(3, tickers.size());
    }

    @Test
    @DisplayName("BlockingQueue: offer сообщает о переполнении; take ждёт задачу")
    void boundedTaskQueue() throws Exception {
        var queue = new ArrayBlockingQueue<String>(1);
        assertTrue(queue.offer("SBER"));
        assertFalse(queue.offer("GAZP"));
        assertEquals("SBER", queue.take());
        try (var pool = Executors.newSingleThreadExecutor()) {
            var next = pool.submit(queue::take);
            assertFalse(next.isDone());
            queue.put("GAZP");
            assertEquals("GAZP", next.get(2, TimeUnit.SECONDS));
        }
    }
}

package academy.backend.market_pulse.demo.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * Нейтральное демо: разделение массива, локальный подсчёт и объединение результатов.
 */
class ForkJoinArrayDemoTest {
    @Test
    void countsTheArrayFromTheSlide() {
        int[] values = {-2, 4, 0, 5, -1, 0, 3, -4};
        try (var pool = new ForkJoinPool(2)) {
            var result = pool.invoke(new SignCountTask(values, 0, values.length, 4));
            System.out.println(result);
            assertEquals(new SignCounts(3, 3, 2), result);
        }
    }

    @Test
    void handlesEmptyRangeAndUnevenRecursiveSplit() {
        int[] values = IntStream.rangeClosed(-500, 500).toArray();
        try (var pool = new ForkJoinPool(2)) {
            assertEquals(new SignCounts(500, 500, 1),
                    pool.invoke(new SignCountTask(values, 0, values.length, 16)));
            assertEquals(new SignCounts(0, 0, 0),
                    pool.invoke(new SignCountTask(values, 0, 0, 16)));
        }
    }
}

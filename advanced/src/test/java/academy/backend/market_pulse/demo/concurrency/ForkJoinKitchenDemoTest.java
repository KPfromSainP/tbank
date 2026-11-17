package academy.backend.market_pulse.demo.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * Вычисление числа порций по заказам кухни: только работа с готовыми данными, без I/O.
 */
class ForkJoinKitchenDemoTest {
    @Test
    void sumsOrdersFromTheSlide() {
        int[] portions = {2, 1, 3, 2, 4, 1, 2, 1};
        try (var pool = new ForkJoinPool(2)) {
            long total = pool.invoke(new PortionsTask(portions, 0, portions.length, 4));
            System.out.println("Всего порций: " + total);
            assertEquals(16L, total);
        }
    }

    @Test
    void matchesSequentialSumForUnevenAndEmptyRanges() {
        int[] portions = IntStream.range(0, 1001).map(i -> i % 5 + 1).toArray();
        try (var pool = new ForkJoinPool(2)) {
            long total = pool.invoke(new PortionsTask(portions, 0, portions.length, 16));
            long empty = pool.invoke(new PortionsTask(portions, 0, 0, 16));
            assertEquals(Arrays.stream(portions).asLongStream().sum(), total);
            assertEquals(0L, empty);
        }
    }
}

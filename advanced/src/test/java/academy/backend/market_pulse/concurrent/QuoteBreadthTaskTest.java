package academy.backend.market_pulse.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import academy.backend.market_pulse.dto.QuoteBreadth;
import academy.backend.market_pulse.model.Quote;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * Сопоставляет рекурсивный анализ котировок с ожидаемыми суммарными результатами.
 */
class QuoteBreadthTaskTest {
    @Test
    void recursiveAnalysisMatchesSequentialCountsIncludingEmptyInput() {
        List<Quote> quotes = IntStream.range(0, 1_001)
                .mapToObj(i -> new Quote(null, BigDecimal.TEN, BigDecimal.valueOf(i % 3 - 1)))
                .toList();
        try (ForkJoinPool pool = new ForkJoinPool(2)) {
            var result = pool.invoke(new QuoteBreadthTask(quotes));
            assertEquals(333, result.rising());
            assertEquals(334, result.falling());
            assertEquals(334, result.unchanged());
            assertEquals(new QuoteBreadth(0, 0, 0),
                    pool.invoke(new QuoteBreadthTask(List.of())));
        }
    }
}

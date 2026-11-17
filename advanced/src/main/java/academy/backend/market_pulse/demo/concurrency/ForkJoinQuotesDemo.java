package academy.backend.market_pulse.demo.concurrency;

import academy.backend.market_pulse.concurrent.QuoteBreadthTask;
import academy.backend.market_pulse.model.Currency;
import academy.backend.market_pulse.model.Quote;
import academy.backend.market_pulse.model.Stock;

import java.math.BigDecimal;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;

/**
 * Анализ загруженных котировок через ForkJoinPool.
 */
public final class ForkJoinQuotesDemo {
    private ForkJoinQuotesDemo() {
    }

    public static void main(String[] args) {
        var quotes = IntStream.range(0, 10_000).mapToObj(i -> new Quote(
                new Stock("DEMO" + i, "Инструмент " + i, Currency.RUB, "demo", BigDecimal.ZERO),
                BigDecimal.valueOf(100 + i), BigDecimal.valueOf(i % 3 - 1))).toList();
        try (var pool = new ForkJoinPool(4)) {
            var breadth = pool.invoke(new QuoteBreadthTask(quotes));
            System.out.println("Растут: " + breadth.rising() + ", падают: " + breadth.falling()
                    + ", без изменений: " + breadth.unchanged());
        }
        // На небольшом списке накладные расходы могут превышать выигрыш от параллелизма.
    }
}

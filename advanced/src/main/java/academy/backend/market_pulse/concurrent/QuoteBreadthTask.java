package academy.backend.market_pulse.concurrent;

import academy.backend.market_pulse.dto.QuoteBreadth;
import academy.backend.market_pulse.model.Quote;

import java.util.List;
import java.util.concurrent.RecursiveTask;

/**
 * Анализ уже загруженных котировок через ForkJoinPool с локальными результатами по диапазонам.
 */
public final class QuoteBreadthTask extends RecursiveTask<QuoteBreadth> {
    private static final int THRESHOLD = 64;
    private final List<Quote> quotes;
    private final int from;
    private final int to;

    public QuoteBreadthTask(List<Quote> quotes) {
        this(List.copyOf(quotes), 0, quotes.size());
    }

    private QuoteBreadthTask(List<Quote> quotes, int from, int to) {
        this.quotes = quotes;
        this.from = from;
        this.to = to;
    }

    /**
     * Делит диапазон до threshold и объединяет локальные результаты без общего счётчика.
     */
    @Override
    protected QuoteBreadth compute() {
        if (to - from <= THRESHOLD) {
            return countSequentially();
        }
        int middle = from + (to - from) / 2;
        QuoteBreadthTask left = new QuoteBreadthTask(quotes, from, middle);
        QuoteBreadthTask right = new QuoteBreadthTask(quotes, middle, to);
        // NOTICE: fork делает одну ветвь доступной для work stealing, текущий worker считает другую.
        left.fork();
        QuoteBreadth rightResult = right.compute();
        return left.join().plus(rightResult);
    }

    private QuoteBreadth countSequentially() {
        int rising = 0;
        int falling = 0;
        int unchanged = 0;
        for (int i = from; i < to; i++) {
            int sign = quotes.get(i).getChangePercent().signum();
            if (sign > 0) {
                rising++;
            } else if (sign < 0) {
                falling++;
            } else {
                unchanged++;
            }
        }
        return new QuoteBreadth(rising, falling, unchanged);
    }
}

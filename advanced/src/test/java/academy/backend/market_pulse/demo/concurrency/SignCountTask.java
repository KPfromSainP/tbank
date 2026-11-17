package academy.backend.market_pulse.demo.concurrency;

import java.util.concurrent.RecursiveTask;

/**
 * Каждый экземпляр обрабатывает полуинтервал [from, to) без общего изменяемого счётчика.
 */
final class SignCountTask extends RecursiveTask<SignCounts> {
    private final int[] values;
    private final int from;
    private final int to;
    private final int threshold;

    SignCountTask(int[] values, int from, int to, int threshold) {
        if (threshold < 1 || from < 0 || to < from || to > values.length) {
            throw new IllegalArgumentException("Invalid range or threshold");
        }
        this.values = values;
        this.from = from;
        this.to = to;
        this.threshold = threshold;
    }

    @Override
    protected SignCounts compute() {
        if (to - from <= threshold) {
            return countSequentially();
        }
        int mid = (from + to) >>> 1;
        var left = new SignCountTask(values, from, mid, threshold);
        var right = new SignCountTask(values, mid, to, threshold);
        left.fork();
        var rightResult = right.compute();
        return left.join().plus(rightResult);
    }

    private SignCounts countSequentially() {
        int positive = 0;
        int negative = 0;
        int zero = 0;
        for (int i = from; i < to; i++) {
            if (values[i] > 0) {
                positive++;
            } else if (values[i] < 0) {
                negative++;
            } else {
                zero++;
            }
        }
        return new SignCounts(positive, negative, zero);
    }
}

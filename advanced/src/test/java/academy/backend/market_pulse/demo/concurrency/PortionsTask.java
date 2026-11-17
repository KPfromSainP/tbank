package academy.backend.market_pulse.demo.concurrency;

import java.util.concurrent.RecursiveTask;

/**
 * Каждый экземпляр суммирует свой диапазон заказов, не изменяя исходный массив.
 */
final class PortionsTask extends RecursiveTask<Long> {
    private final int[] portions;
    private final int from;
    private final int to;
    private final int threshold;

    PortionsTask(int[] portions, int from, int to, int threshold) {
        if (threshold < 1 || from < 0 || to < from || to > portions.length) {
            throw new IllegalArgumentException("Invalid range or threshold");
        }
        this.portions = portions;
        this.from = from;
        this.to = to;
        this.threshold = threshold;
    }

    @Override
    protected Long compute() {
        if (to - from <= threshold) {
            long total = 0;
            for (int i = from; i < to; i++) {
                total += portions[i];
            }
            return total;
        }
        int middle = (from + to) >>> 1;
        var left = new PortionsTask(portions, from, middle, threshold);
        var right = new PortionsTask(portions, middle, to, threshold);
        left.fork();
        long rightResult = right.compute();
        return left.join() + rightResult;
    }
}

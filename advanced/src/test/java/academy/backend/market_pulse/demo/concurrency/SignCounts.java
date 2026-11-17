package academy.backend.market_pulse.demo.concurrency;

/**
 * Локальный результат подсчёта знаков в диапазоне массива.
 */
record SignCounts(int positive, int negative, int zero) {
    SignCounts plus(SignCounts other) {
        return new SignCounts(positive + other.positive, negative + other.negative, zero + other.zero);
    }
}

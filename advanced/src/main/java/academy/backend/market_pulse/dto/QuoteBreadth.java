package academy.backend.market_pulse.dto;

/**
 * Число растущих, падающих и неизменившихся котировок в загруженном наборе.
 */
public record QuoteBreadth(int rising, int falling, int unchanged) {
    public QuoteBreadth plus(QuoteBreadth other) {
        return new QuoteBreadth(rising + other.rising, falling + other.falling, unchanged + other.unchanged);
    }
}

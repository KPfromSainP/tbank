package academy.backend.market_pulse.benchmark;

import org.openjdk.jmh.annotations.*;

/**
 * Собственная последовательность ключей для каждого benchmark-потока.
 */
@State(Scope.Thread)
public class ConcurrentMapCursor {
    private int position;

    @Setup
    public void setup(org.openjdk.jmh.infra.ThreadParams thread) {
        position = thread.getThreadIndex() * 997;
    }

    Integer next() {
        return ConcurrentMapBenchmark.KEYS[position++ & (ConcurrentMapBenchmark.SIZE - 1)];
    }
}

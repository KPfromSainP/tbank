package academy.backend.market_pulse.benchmark;

import java.util.Map;

import org.openjdk.jmh.annotations.*;

/**
 * Общее состояние группы из трёх читателей и одного writer.
 */
@State(Scope.Group)
public class ConcurrentMapMixedState {
    @Param({"synchronizedMap", "concurrentHashMap"})
    public String kind;

    @Param({"false", "true"})
    public boolean hotKey;

    Map<Integer, Integer> map;

    @Setup
    public void setup() {
        map = ConcurrentMapBenchmark.populatedMap(kind);
    }
}

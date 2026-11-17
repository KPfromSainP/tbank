package academy.backend.market_pulse.benchmark;

import java.util.Map;

import org.openjdk.jmh.annotations.*;

/**
 * Подготовленная Map для сценария чтения.
 */
@State(Scope.Benchmark)
public class ConcurrentMapReadState {
    @Param({"hashMap", "synchronizedMap", "concurrentHashMap"})
    public String kind;
    Map<Integer, Integer> map;

    @Setup
    public void setup() {
        map = ConcurrentMapBenchmark.populatedMap(kind);
    }
}

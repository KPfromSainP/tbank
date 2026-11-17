package academy.backend.market_pulse.benchmark;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

/**
 * Сравнивает чтение и смешанную нагрузку Map при разных уровнях конкуренции.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class ConcurrentMapBenchmark {
    static final int SIZE = 4096;
    static final Integer[] KEYS = new Integer[SIZE];

    static {
        for (int i = 0; i < SIZE; i++) {
            KEYS[i] = i;
        }
    }

    @Benchmark
    public Integer read(ConcurrentMapReadState state, ConcurrentMapCursor cursor) {
        return state.map.get(cursor.next());
    }

    @Benchmark
    @Group("mixed")
    @GroupThreads(3)
    public Integer mixedRead(ConcurrentMapMixedState state, ConcurrentMapCursor cursor) {
        return state.map.get(state.hotKey ? KEYS[0] : cursor.next());
    }

    @Benchmark
    @Group("mixed")
    @GroupThreads(1)
    public Integer mixedWrite(ConcurrentMapMixedState state, ConcurrentMapCursor cursor) {
        Integer key = state.hotKey ? KEYS[0] : cursor.next();
        return state.map.put(key, key);
    }

    static Map<Integer, Integer> populatedMap(String kind) {
        Map<Integer, Integer> map = switch (kind) {
            case "hashMap" -> new HashMap<>(SIZE * 2);
            case "synchronizedMap" -> Collections.synchronizedMap(new HashMap<>(SIZE * 2));
            case "concurrentHashMap" -> new ConcurrentHashMap<>(SIZE);
            default -> throw new IllegalArgumentException(kind);
        };
        for (Integer key : KEYS) {
            map.put(key, key);
        }
        return map;
    }
}

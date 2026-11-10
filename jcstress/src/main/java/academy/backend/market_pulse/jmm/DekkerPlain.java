package academy.backend.market_pulse.jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Две обычные переменные: оба конкурентных участника могут прочитать исходные нули.
 *
 * <p>Сравнить с DekkerVolatile: тела конкурентных методов одинаковые, меняется только доступ к полям.
 */
@JCStressTest
@Outcome(
        id = {"1, 1", "0, 1", "1, 0"},
        expect = Expect.ACCEPTABLE,
        desc = "Хотя бы одно чтение увидело запись другого участника")
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Оба чтения вернули исходное значение 0")
@Outcome(expect = Expect.FORBIDDEN, desc = "Прочие исходы")
@State
public class DekkerPlain {
    /**
     * Переменная, в которую записывает первый участник и которую читает второй.
     */
    private int x;
    /**
     * Переменная, в которую записывает второй участник и которую читает первый.
     */
    private int y;

    @Actor
    public void writeXReadY(II_Result result) {
        x = 1;
        result.r1 = y;
    }

    @Actor
    public void writeYReadX(II_Result result) {
        y = 1;
        result.r2 = x;
    }
}

package academy.backend.market_pulse.jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

/**
 * Две volatile-переменные: одновременное наблюдение двух нулей запрещено.
 *
 * <p>Общий порядок volatile-действий не допускает цикл, необходимый для исхода (0, 0).
 */
@JCStressTest
@Outcome(
        id = {"1, 1", "0, 1", "1, 0"},
        expect = Expect.ACCEPTABLE,
        desc = "Хотя бы одно чтение увидело запись другого участника")
@Outcome(id = "0, 0", expect = Expect.FORBIDDEN, desc = "Оба чтения вернули исходное значение 0")
@Outcome(expect = Expect.FORBIDDEN, desc = "Прочие исходы")
@State
public class DekkerVolatile {
    /**
     * Первая переменная; volatile упорядочивает доступы конкурентных участников.
     */
    private volatile int x;
    /**
     * Вторая переменная с теми же гарантиями видимости и порядка.
     */
    private volatile int y;

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

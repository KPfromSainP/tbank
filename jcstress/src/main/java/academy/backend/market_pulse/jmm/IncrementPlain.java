package academy.backend.market_pulse.jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/**
 * Два инкремента обычного счётчика могут потерять одно обновление.
 *
 * <p>Arbiter читает счётчик после завершения обоих конкурентных участников.
 */
@JCStressTest
@Outcome(id = "2", expect = Expect.ACCEPTABLE, desc = "Оба инкремента сохранены: итоговое значение 2")
@Outcome(id = "1", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Один инкремент потерян: осталось значение 1")
@Outcome(expect = Expect.FORBIDDEN, desc = "Прочие исходы")
@State
public class IncrementPlain {
    /**
     * Общий счётчик; чтение, увеличение и запись намеренно не объединены в одну операцию.
     */
    private int counter;

    @Actor
    public void firstActor() {
        counter++;
    }

    @Actor
    public void secondActor() {
        counter++;
    }

    /**
     * Собирает итог после завершения обоих конкурентных участников.
     */
    @Arbiter
    public void readResult(I_Result result) {
        result.r1 = counter;
    }
}

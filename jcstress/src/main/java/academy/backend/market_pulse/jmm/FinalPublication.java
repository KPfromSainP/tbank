package academy.backend.market_pulse.jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/**
 * Корректное конструирование даёт специальные гарантии final-полю.
 *
 * <p>Ссылка holder обычная: читатель может не увидеть объект, но не должен увидеть value == 0.
 */
@JCStressTest
@Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Читатель не увидел ссылку на объект")
@Outcome(id = "42", expect = Expect.ACCEPTABLE, desc = "Объект виден с корректно инициализированным final-полем")
@Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Final-поле не должно содержать значение по умолчанию")
@Outcome(expect = Expect.FORBIDDEN, desc = "Прочие исходы")
@State
public class FinalPublication {
    private Holder holder;

    @Actor
    public void writer() {
        holder = new Holder();
    }

    @Actor
    public void reader(I_Result result) {
        // Одно чтение ссылки: повторное plain-чтение — уже другой эксперимент.
        Holder observed = holder;
        result.r1 = observed == null ? -1 : observed.value;
    }

    private static final class Holder {
        private final int value;

        private Holder() {
            value = 42;
        }
    }
}

package academy.backend.market_pulse.jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/**
 * Чтение установленного volatile-флага публикует предшествующую запись данных.
 *
 * <p>Сравнить с PublicationPlain: меняется только объявление ready.
 */
@JCStressTest
@Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Читатель не увидел установленный флаг")
@Outcome(id = "42", expect = Expect.ACCEPTABLE, desc = "Читатель увидел флаг и записанные данные")
@Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Флаг true, но data содержит исходное значение 0")
@Outcome(expect = Expect.FORBIDDEN, desc = "Прочие исходы")
@State
public class PublicationVolatile {
    /** Обычное поле данных, записываемое до установки флага готовности. */
    private int data;
    /** Флаг публикации: чтение true делает предшествующую запись data видимой читателю. */
    private volatile boolean ready;

    @Actor
    public void writer() {
        data = 42;
        ready = true;
    }

    @Actor
    public void reader(I_Result result) {
        // -1 — служебный исход: флаг не установлен, data не читаем.
        result.r1 = ready ? data : -1;
    }
}

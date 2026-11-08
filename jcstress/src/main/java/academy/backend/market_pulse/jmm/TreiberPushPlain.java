package academy.backend.market_pulse.jmm;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/**
 * Намеренно ошибочный push: конкурентные записи вершины могут потерять задачу.
 *
 * <p>Volatile-head сам по себе не исправит составную операцию; сравнить с TreiberPushCas.
 */
@JCStressTest
@Outcome(id = "3", expect = Expect.ACCEPTABLE, desc = "Обе задачи доступны")
@Outcome(id = {"1", "2"}, expect = Expect.ACCEPTABLE_INTERESTING, desc = "Один push потерян")
@Outcome(expect = Expect.FORBIDDEN, desc = "Неожиданный исход")
@State
public class TreiberPushPlain {
    private Node head;

    @Actor
    public void firstActor() {
        push(1);
    }

    @Actor
    public void secondActor() {
        push(2);
    }

    private void push(int task) {
        Node observed = head;
        // Между чтением и записью другой actor может опубликовать собственный узел.
        head = new Node(task, observed);
    }

    @Arbiter
    public void readResult(I_Result result) {
        // Биты задач: 1 = первая, 2 = вторая, 3 = обе. Порядок извлечения здесь не проверяем.
        int mask = 0;
        for (Node node = head; node != null; node = node.next()) {
            mask |= node.task();
        }
        result.r1 = mask;
    }

    private record Node(int task, Node next) {
    }
}

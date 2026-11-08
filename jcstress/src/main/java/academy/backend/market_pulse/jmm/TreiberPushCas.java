package academy.backend.market_pulse.jmm;

import java.util.concurrent.atomic.AtomicReference;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

/**
 * CAS-цикл сохраняет оба конкурентных push в стек.
 *
 * <p>Неизменяемые узлы не переиспользуются; AtomicReference обеспечивает публикацию вершины.
 */
@JCStressTest
@Outcome(id = "3", expect = Expect.ACCEPTABLE, desc = "Обе задачи доступны")
@Outcome(id = {"1", "2"}, expect = Expect.FORBIDDEN, desc = "Один push потерян")
@Outcome(expect = Expect.FORBIDDEN, desc = "Неожиданный исход")
@State
public class TreiberPushCas {
    private final AtomicReference<Node> head = new AtomicReference<>();

    @Actor
    public void firstActor() {
        push(1);
    }

    @Actor
    public void secondActor() {
        push(2);
    }

    private void push(int task) {
        Node observed;
        Node update;
        do {
            observed = head.get();
            update = new Node(task, observed);
        } while (!head.compareAndSet(observed, update));
    }

    @Arbiter
    public void readResult(I_Result result) {
        // Биты задач: 1 = первая, 2 = вторая, 3 = обе. Порядок извлечения здесь не проверяем.
        int mask = 0;
        for (Node node = head.get(); node != null; node = node.next()) {
            mask |= node.task();
        }
        result.r1 = mask;
    }

    private record Node(int task, Node next) {
    }
}

package academy.backend.market_pulse.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** LIFO: неизменяемые узлы публикуются через CAS. Узлы не переиспользуются (нет ABA из-за reuse). */
public final class TreiberStack<T> {
    private record Node<T>(T value, Node<T> next) { }

    private final AtomicReference<Node<T>> head = new AtomicReference<>();

    // NOTICE: push — составная операция. volatile-head не спасает от потерянного узла;
    // при неуспешном CAS заново читаем head и создаём узел с актуальным next.
    public void push(T value) {
        Objects.requireNonNull(value);
        Node<T> observed;
        Node<T> update;
        do {
            observed = head.get();
            update = new Node<>(value, observed);
        } while (!head.compareAndSet(observed, update));
    }

    // NOTICE: успешный CAS — точка линеаризации. null означает пустой стек в момент чтения.
    public T pop() {
        Node<T> observed;
        do {
            observed = head.get();
            if (observed == null) {
                return null;
            }
        } while (!head.compareAndSet(observed, observed.next()));
        return observed.value();
    }

    public void clear() {
        head.set(null);
    }
}

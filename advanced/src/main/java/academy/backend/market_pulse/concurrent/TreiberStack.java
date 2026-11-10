package academy.backend.market_pulse.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Конкурентный стек Трайбера: последняя добавленная задача извлекается первой.
 * Сравнение и замена вершины выполняются одной атомарной операцией, без блокировки потоков.
 */
public final class TreiberStack<T> {
    /**
     * Неизменяемый Node: значение и ссылка на предыдущую вершину стека.
     */
    private record Node<T>(T value, Node<T> next) {
    }

    /**
     * Head стека: compareAndSet согласует конкурентные push и pop.
     */
    private final AtomicReference<Node<T>> head = new AtomicReference<>();

    /**
     * Добавляет значение в head с повтором compareAndSet при конфликте.
     */
    public void push(T value) {
        Objects.requireNonNull(value);
        Node<T> observed;
        do {
            observed = head.get();
            // NOTICE: два потока могут прочитать одну вершину и создать два разных Node.
            // Простая запись head затёрла бы один из них, даже если head был бы volatile.
            // compareAndSet заменит вершину только если она всё ещё хранит ту же ссылку observed.
            // При конфликте читаем новую вершину и создаём Node с обновлённой ссылкой next.
        } while (!head.compareAndSet(observed, new Node<>(value, observed)));
    }

    /**
     * Извлекает последнюю добавленную запись; для пустого стека возвращает null.
     */
    public T pop() {
        Node<T> observed;
        do {
            observed = head.get();
            if (observed == null) {
                return null;
            }
            // NOTICE: только победитель сравнения и замены получает этот Node.
            // Node не возвращаются в стек повторно, поэтому старая вершина не появится снова.
        } while (!head.compareAndSet(observed, observed.next()));
        return observed.value();
    }

    /**
     * Отбрасывает оставшиеся задачи после остановки producer. Уже извлечённую задачу clear не отменяет.
     */
    public void clear() {
        head.set(null);
    }
}

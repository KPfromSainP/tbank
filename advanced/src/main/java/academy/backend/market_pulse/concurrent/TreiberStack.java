package academy.backend.market_pulse.concurrent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Конкурентный стек Трайбера: последняя добавленная задача извлекается первой.
 * Сравнение и замена вершины выполняются одной атомарной операцией, без блокировки потоков.
 */
public final class TreiberStack<T> {
    /** Неизменяемый узел: значение и ссылка на предыдущую вершину стека. */
    private record Node<T>(T value, Node<T> next) { }

    /** Уникальный маркер закрытия: не является пользовательским узлом и не извлекается. */
    private final Node<T> closed = new Node<>(null, null);
    /** Вершина стека: одна атомарная замена согласует добавление, извлечение и закрытие. */
    private final AtomicReference<Node<T>> head = new AtomicReference<>();

    /** Добавляет значение в вершину; после закрытия стека отклоняет новые значения. */
    public void push(T value) {
        Objects.requireNonNull(value);
        Node<T> observed;
        do {
            observed = head.get();
            if (observed == closed) {
                throw new IllegalStateException("Стек закрыт");
            }
            // NOTICE: два потока могут прочитать одну вершину и создать два разных узла.
            // Простая запись head затёрла бы один из них, даже если head был бы volatile.
            // compareAndSet заменит вершину только если она всё ещё хранит ту же ссылку observed.
            // При конфликте читаем новую вершину и создаём узел с обновлённой ссылкой next.
        } while (!head.compareAndSet(observed, new Node<>(value, observed)));
    }

    /** Извлекает последнюю добавленную запись; для пустого или закрытого стека возвращает null. */
    public T pop() {
        Node<T> observed;
        do {
            observed = head.get();
            if (observed == null || observed == closed) {
                return null;
            }
            // NOTICE: только победитель сравнения и замены получает этот узел.
            // Узлы не возвращаются в стек повторно, поэтому старая вершина не появится снова.
        } while (!head.compareAndSet(observed, observed.next()));
        return observed.value();
    }

    /** Удаляет ожидающие записи, сохраняя возможность добавлять новые, если стек не закрыт. */
    public void clear() {
        Node<T> observed;
        do {
            observed = head.get();
            if (observed == closed) {
                return;
            }
        } while (!head.compareAndSet(observed, null));
    }

    /** Одной атомарной заменой запрещает добавление записей и отбрасывает оставшиеся записи. */
    public void close() {
        // NOTICE: добавление и закрытие меняют одно поле. После установки маркера closed
        // ни один push со старым значением вершины уже не сможет завершиться успешно.
        head.getAndSet(closed);
    }
}

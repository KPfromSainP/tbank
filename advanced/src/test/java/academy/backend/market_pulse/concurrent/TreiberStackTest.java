package academy.backend.market_pulse.concurrent;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Проверяет порядок извлечения и запрет добавления после закрытия. */
class TreiberStackTest {
    @Test
    void closeCannotBeUndoneByClearOrConcurrentPush() throws Exception {
        TreiberStack<Integer> stack = new TreiberStack<>();
        Thread producer = Thread.ofPlatform().start(() -> {
            for (int value = 0; value < 10_000; value++) {
                try { stack.push(value); }
                catch (IllegalStateException closed) { return; }
            }
        });
        stack.close();
        producer.join();
        stack.clear();
        assertNull(stack.pop());
        assertThrows(IllegalStateException.class, () -> stack.push(1));
    }

    @Test
    void stackIsLifoAndCanBeCleared() {
        TreiberStack<String> stack = new TreiberStack<>();
        assertNull(stack.pop());
        stack.push("old");
        stack.push("new");
        assertEquals("new", stack.pop());
        assertEquals("old", stack.pop());
        stack.push("discard");
        stack.clear();
        assertNull(stack.pop());
    }
}

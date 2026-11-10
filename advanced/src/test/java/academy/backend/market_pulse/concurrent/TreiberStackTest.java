package academy.backend.market_pulse.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Проверяет порядок извлечения и очистку ожидающих задач.
 */
class TreiberStackTest {
    @Test
    void clearDiscardsPendingValuesAndAllowsNewPushes() {
        TreiberStack<String> stack = new TreiberStack<>();
        stack.push("discard");
        stack.clear();
        assertNull(stack.pop());
        stack.push("new");
        assertEquals("new", stack.pop());
    }

    @Test
    void stackIsLifo() {
        TreiberStack<String> stack = new TreiberStack<>();
        assertNull(stack.pop());
        stack.push("old");
        stack.push("new");
        assertEquals("new", stack.pop());
        assertEquals("old", stack.pop());
        assertNull(stack.pop());
    }
}

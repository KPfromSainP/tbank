package academy.backend.market_pulse.concurrent;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TreiberStackTest {
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

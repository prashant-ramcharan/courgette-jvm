package courgette.runtime;

import courgette.api.CourgetteAfterAll;
import courgette.api.CourgetteBeforeAll;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CourgetteCallbacksTest {
    private static final List<String> calls = new ArrayList<>();

    @Before
    public void resetCalls() {
        calls.clear();
    }

    @Test
    public void invokesBeforeAllCallbacksInOrder() {
        new CourgetteCallbacks(Callbacks.class).beforeAll();

        assertEquals(list("before-1", "before-2"), calls);
    }

    @Test
    public void invokesAfterAllCallbacksInOrder() {
        new CourgetteCallbacks(Callbacks.class).afterAll();

        assertEquals(list("after-1", "after-2"), calls);
    }

    private List<String> list(String... values) {
        return java.util.Arrays.asList(values);
    }

    public static class Callbacks {
        @CourgetteBeforeAll(order = 2)
        public static void beforeSecond() {
            calls.add("before-2");
        }

        @CourgetteBeforeAll(order = 1)
        public static void beforeFirst() {
            calls.add("before-1");
        }

        @CourgetteAfterAll(order = 2)
        public static void afterSecond() {
            calls.add("after-2");
        }

        @CourgetteAfterAll(order = 1)
        public static void afterFirst() {
            calls.add("after-1");
        }

        @CourgetteBeforeAll(order = 0)
        public void ignoredInstanceMethod() {
            calls.add("ignored");
        }

        @CourgetteAfterAll(order = 0)
        public static void ignoredMethodWithArgument(String value) {
            calls.add(value);
        }
    }
}

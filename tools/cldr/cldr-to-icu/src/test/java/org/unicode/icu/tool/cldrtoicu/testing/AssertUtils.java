// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.testing;

import static org.junit.Assert.fail;

/** Static assertion helpers (some of which can be removed if JUnit version is updated). */
public final class AssertUtils {
    // Functional interface acting as a lambda target.
    public interface CheckedRunnable<T extends Throwable> {
        void run() throws T;
    }

    /** Asserts that an exception is thrown by a given runnable. */
    public static <T extends Throwable> T assertThrows(Class<T> cls, CheckedRunnable<T> fn) {
        try {
            fn.run();
        } catch (Throwable t) {
            if (cls.isInstance(t)) {
                return cls.cast(t);
            }
            fail("expected " + cls.getName() + " but got " + t.getClass().getName());
        }
        fail("expected " + cls.getName() + " but nothing was thrown");
        throw new AssertionError("unreachable!");
    }

    private AssertUtils() {}
}

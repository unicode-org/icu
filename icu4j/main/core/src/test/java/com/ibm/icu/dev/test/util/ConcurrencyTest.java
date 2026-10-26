// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.CoreTestFmwk;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import org.junit.Assume;

/**
 * Base class for concurrent regression tests. Spins up {@link #THREAD_COUNT} threads, synchronizes
 * them at a barrier, runs the task, and collects errors. Threads that hang past {@link #TIMEOUT_MS}
 * trigger a failure with diagnostic info.
 *
 * <p>Tests are skipped unless exhaustiveness >= 5 (pass {@code -DICU.exhaustive=5} to Maven).
 */
public abstract class ConcurrencyTest extends CoreTestFmwk {

    protected static final int THREAD_COUNT = 16;
    protected static final int ITERATIONS = 2_000;
    private static final long TIMEOUT_MS = 30_000;
    private static final int MIN_EXHAUSTIVENESS = 5;

    @FunctionalInterface
    protected interface ConcurrentTask {
        void run(int threadId) throws Exception;
    }

    protected void runConcurrent(String testName, ConcurrentTask task) throws Exception {
        Assume.assumeTrue(
                "Concurrency tests require exhaustiveness >= "
                        + MIN_EXHAUSTIVENESS
                        + " (use -e "
                        + MIN_EXHAUSTIVENESS
                        + ")",
                getExhaustiveness() >= MIN_EXHAUSTIVENESS);

        final CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        Thread[] threads = new Thread[THREAD_COUNT];
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int tid = t;
            threads[t] =
                    new Thread(
                            () -> {
                                try {
                                    barrier.await();
                                    task.run(tid);
                                } catch (Throwable e) {
                                    errors.add(e);
                                }
                            },
                            testName + "-thread-" + t);
            threads[t].start();
        }

        for (Thread thread : threads) {
            thread.join(TIMEOUT_MS);
        }
        List<String> hung = new ArrayList<>();
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                hung.add(thread.getName());
                thread.interrupt();
            }
        }
        if (!hung.isEmpty()) {
            fail(
                    testName
                            + ": threads still alive after "
                            + TIMEOUT_MS
                            + "ms (possible deadlock): "
                            + String.join(", ", hung));
        }
        if (!errors.isEmpty()) {
            Throwable first = errors.get(0);
            fail(
                    testName
                            + ": "
                            + errors.size()
                            + " thread(s) threw exceptions. First: "
                            + first.getClass().getName()
                            + ": "
                            + first.getMessage());
        }
    }
}

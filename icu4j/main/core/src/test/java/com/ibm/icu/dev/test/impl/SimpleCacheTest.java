// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.impl;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.SimpleCache;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Concurrency tests for SimpleCache. */
@RunWith(JUnit4.class)
public class SimpleCacheTest extends CoreTestFmwk {

    @Test
    public void testConcurrency() throws InterruptedException {
        if (TestFmwk.getExhaustiveness() < 5) {
            return;
        }
        runConcurrencyTest(ICUCache.SOFT);
        runConcurrencyTest(ICUCache.WEAK);
    }

    private void runConcurrencyTest(int type) throws InterruptedException {
        final int numThreads = 20;
        final int numIterations = 1000;
        final SimpleCache<String, String> cache = new SimpleCache<>(type);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(numThreads);
        final AtomicLong hits = new AtomicLong(0);
        final List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        try {
            for (int i = 0; i < numThreads; i++) {
                final int threadId = i;
                exec.submit(
                        () -> {
                            try {
                                startLatch.await();
                                for (int j = 0; j < numIterations; j++) {
                                    String key = "key" + (j % 50);
                                    String val = "thread-" + threadId + "-iter-" + j;
                                    cache.put(key, val);

                                    String cached = cache.get(key);
                                    if (cached != null) {
                                        hits.incrementAndGet();
                                        // Verify that the value returned belongs to the correct key
                                        // pattern
                                        if (!cached.startsWith("thread-")
                                                || !cached.contains("-iter-")) {
                                            throw new AssertionError(
                                                    "SimpleCache("
                                                            + type
                                                            + ") data corruption: "
                                                            + cached);
                                        }
                                    } else if (type == ICUCache.SOFT) {
                                        // SoftReferences should not be cleared in a simple test run
                                        throw new AssertionError(
                                                "SimpleCache(SOFT) error: unexpected null for key "
                                                        + key);
                                    }
                                }
                            } catch (Throwable t) {
                                exceptions.add(t);
                            } finally {
                                endLatch.countDown();
                            }
                        });
            }

            startLatch.countDown();
            if (!endLatch.await(10, TimeUnit.SECONDS)) {
                errln("Concurrency test timed out for cache type " + type);
            }

            for (Throwable t : exceptions) {
                errln("Thread failed: " + t.getMessage());
            }

            // Sanity check: ensure we actually tested something non-null
            assertTrue("SimpleCache(" + type + ") was always null", hits.get() > 0);

        } finally {
            exec.shutdownNow();
        }
    }
}

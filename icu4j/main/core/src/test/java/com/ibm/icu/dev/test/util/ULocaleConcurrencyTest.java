// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Concurrency hammer tests for modernization effort (ULocale). */
@RunWith(JUnit4.class)
public class ULocaleConcurrencyTest extends CoreTestFmwk {

    private static final int NUM_THREADS = 50;
    private static final int ITERATIONS = 500; // Balanced stress test (25k total ops)

    @Test
    public void testULocaleConcurrency() throws InterruptedException {
        if (TestFmwk.getExhaustiveness() < 5) {
            return;
        }

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch endLatch = new CountDownLatch(NUM_THREADS);
        final List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        ExecutorService exec = Executors.newFixedThreadPool(NUM_THREADS);
        try {
            for (int i = 0; i < NUM_THREADS; i++) {
                exec.submit(
                        () -> {
                            try {
                                startLatch.await();
                                for (int j = 0; j < ITERATIONS; j++) {
                                    ULocale.canonicalize("en_US_POSIX");
                                    ULocale.canonicalize("ar_EG");
                                    ULocale.canonicalize("zh_Hans_CN");
                                }
                            } catch (Throwable t) {
                                exceptions.add(t);
                            } finally {
                                endLatch.countDown();
                            }
                        });
            }
            startLatch.countDown();
            if (!endLatch.await(60, TimeUnit.SECONDS)) {
                errln("ULocale concurrency test timed out");
            }
        } finally {
            exec.shutdownNow();
        }

        for (Throwable t : exceptions) {
            errln("ULocale thread failed: " + t.getMessage());
        }
    }
}

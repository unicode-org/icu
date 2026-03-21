// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.util.ULocale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Concurrency regression tests for ULocale.getDefault(Category) and setDefault(Category).
 *
 * <p>Note: ULocale.canonicalize() concurrency is tested separately in ULocaleConcurrencyTest.
 */
@RunWith(JUnit4.class)
public class ULocaleDefaultConcurrencyTest extends ConcurrencyTest {

    @Test
    public void testULocaleGetDefaultCategoryConcurrent() throws Exception {
        runConcurrent(
                "ULocaleGetDefaultCategory",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        ULocale loc = ULocale.getDefault(ULocale.Category.FORMAT);
                        assertNotNull("FORMAT default should not be null", loc);
                        ULocale display = ULocale.getDefault(ULocale.Category.DISPLAY);
                        assertNotNull("DISPLAY default should not be null", display);
                    }
                });
    }

    /**
     * Writer-contention test: concurrent setDefault(Category) calls interleaved with reads.
     * Verifies that readers always see a valid ULocale (never null, never a torn value).
     */
    @Test
    public void testULocaleSetDefaultCategoryConcurrent() throws Exception {
        ULocale original = ULocale.getDefault(ULocale.Category.FORMAT);
        ULocale[] candidates = {ULocale.US, ULocale.GERMANY, ULocale.JAPAN, ULocale.FRANCE};
        try {
            runConcurrent(
                    "ULocaleSetDefaultCategory",
                    tid -> {
                        for (int i = 0; i < ITERATIONS / 10; i++) {
                            if (tid % 4 == 0) {
                                ULocale loc = candidates[(tid + i) % candidates.length];
                                ULocale.setDefault(ULocale.Category.FORMAT, loc);
                            } else {
                                ULocale loc = ULocale.getDefault(ULocale.Category.FORMAT);
                                assertNotNull(
                                        "FORMAT default should never be null during writes", loc);
                            }
                        }
                    });
        } finally {
            ULocale.setDefault(ULocale.Category.FORMAT, original);
        }
    }
}

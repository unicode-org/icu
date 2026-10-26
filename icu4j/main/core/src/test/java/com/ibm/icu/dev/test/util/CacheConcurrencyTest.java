// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Concurrency regression tests for ICU4J caches: SimpleCache, MeasureUnit, Currency. */
@RunWith(JUnit4.class)
public class CacheConcurrencyTest extends ConcurrencyTest {

    @Test
    public void testSimpleCacheConcurrentPuts() throws Exception {
        ULocale[] locales = {ULocale.US, ULocale.GERMANY, ULocale.JAPAN, ULocale.FRANCE};

        runConcurrent(
                "SimpleCachePuts",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        ULocale loc = locales[(tid + i) % locales.length];
                        NumberFormat nf = NumberFormat.getInstance(loc);
                        assertNotNull("NumberFormat should not be null", nf);
                        String result = nf.format(12345.67);
                        assertFalse(
                                "Formatted result should not be empty",
                                result == null || result.isEmpty());
                    }
                });
    }

    @Test
    public void testSimpleCacheConcurrentReadsAndPuts() throws Exception {
        ULocale[] locales = {
            ULocale.US, ULocale.GERMANY, ULocale.JAPAN, ULocale.FRANCE, ULocale.CHINA, ULocale.KOREA
        };

        runConcurrent(
                "SimpleCacheReadsAndPuts",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        ULocale loc = locales[(tid + i) % locales.length];
                        NumberFormat nf = NumberFormat.getCurrencyInstance(loc);
                        assertNotNull("CurrencyInstance should not be null", nf);
                    }
                });
    }

    @Test
    public void testMeasureUnitConcurrentAccess() throws Exception {
        runConcurrent(
                "MeasureUnitAccess",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        Set<String> types = MeasureUnit.getAvailableTypes();
                        assertFalse(
                                "Available types should not be empty",
                                types == null || types.isEmpty());
                        for (String type : types) {
                            Set<MeasureUnit> units = MeasureUnit.getAvailable(type);
                            assertNotNull("Units for type " + type + " should not be null", units);
                        }
                    }
                });
    }

    @Test
    public void testCurrencyIsAvailableConcurrent() throws Exception {
        final AtomicInteger successCount = new AtomicInteger(0);
        boolean hasData = Currency.isAvailable("USD", null, null);

        runConcurrent(
                "CurrencyIsAvailable",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        boolean available = Currency.isAvailable("USD", null, null);
                        if (available) {
                            successCount.incrementAndGet();
                        }
                    }
                });

        if (hasData) {
            assertEquals(
                    "All calls should return true when data is available",
                    THREAD_COUNT * ITERATIONS,
                    successCount.get());
        }
    }
}

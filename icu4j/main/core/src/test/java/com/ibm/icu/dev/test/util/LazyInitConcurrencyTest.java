// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Concurrency regression tests for lazy initialization patterns: volatile DCL, ConcurrentHashMap
 * caches, and AtomicReferenceArray.
 */
@RunWith(JUnit4.class)
public class LazyInitConcurrencyTest extends ConcurrencyTest {

    @Test
    public void testPluralRulesForLocaleConcurrent() throws Exception {
        ULocale[] locales = {
            ULocale.ENGLISH,
            ULocale.FRENCH,
            ULocale.GERMAN,
            ULocale.JAPANESE,
            new ULocale("ar"),
            new ULocale("ru")
        };
        runConcurrent(
                "PluralRulesForLocale",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        ULocale loc = locales[(tid + i) % locales.length];
                        PluralRules rules = PluralRules.forLocale(loc);
                        assertNotNull("PluralRules should not be null for " + loc, rules);
                    }
                });
    }

    @Test
    public void testMeasureFormatConcurrent() throws Exception {
        runConcurrent(
                "MeasureFormat",
                tid -> {
                    MeasureFormat fmt =
                            MeasureFormat.getInstance(
                                    ULocale.ENGLISH, MeasureFormat.FormatWidth.WIDE);
                    MeasureUnit[] units = {
                        MeasureUnit.METER,
                        MeasureUnit.KILOMETER,
                        MeasureUnit.KILOGRAM,
                        MeasureUnit.CELSIUS
                    };
                    for (int i = 0; i < ITERATIONS; i++) {
                        MeasureUnit unit = units[(tid + i) % units.length];
                        String result = fmt.format(new Measure(42, unit));
                        assertNotNull("MeasureFormat result should not be null", result);
                        assertFalse("MeasureFormat result should not be empty", result.isEmpty());
                    }
                });
    }
}

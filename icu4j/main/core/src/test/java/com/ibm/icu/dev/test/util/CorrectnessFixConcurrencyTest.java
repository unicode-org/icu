// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.text.RelativeDateTimeFormatter;
import com.ibm.icu.util.ULocale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Concurrency regression tests for correctness bug fixes. */
@RunWith(JUnit4.class)
public class CorrectnessFixConcurrencyTest extends ConcurrencyTest {

    /** cloneAsThawed() must not unfreeze the shared frozen singleton. */
    @Test
    public void testDTPGCloneAsThawedDoesNotUnfreezeOriginal() throws Exception {
        DateTimePatternGenerator frozen =
                DateTimePatternGenerator.getFrozenInstance(ULocale.ENGLISH);
        assertTrue("getFrozenInstance should return frozen", frozen.isFrozen());

        runConcurrent(
                "DTPGCloneAsThawed",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        DateTimePatternGenerator thawed = frozen.cloneAsThawed();
                        assertFalse("cloneAsThawed result should not be frozen", thawed.isFrozen());
                        assertTrue(
                                "original must remain frozen after cloneAsThawed",
                                frozen.isFrozen());
                        String pattern = thawed.getBestPattern("yMMMd");
                        assertNotNull("getBestPattern should not return null", pattern);
                    }
                });

        assertTrue("original must still be frozen after all threads complete", frozen.isFrozen());
    }

    /** formatImpl() else-branch must synchronize on numberFormat. */
    @Test
    public void testRelativeDateTimeFormatterConcurrent() throws Exception {
        // Share a single instance across threads to exercise the synchronized(numberFormat) fix.
        RelativeDateTimeFormatter fmt = RelativeDateTimeFormatter.getInstance(ULocale.ENGLISH);
        runConcurrent(
                "RelativeDateTimeFormatter",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        String result =
                                fmt.format(
                                        i + 1,
                                        RelativeDateTimeFormatter.Direction.NEXT,
                                        RelativeDateTimeFormatter.RelativeUnit.DAYS);
                        assertNotNull("format result should not be null", result);
                        assertFalse("format result should not be empty", result.isEmpty());
                    }
                });
    }

    /** getLanguageToSet() must use holder pattern for thread-safe lazy initialization. */
    @Test
    public void testStandardPluralRangesConcurrent() throws Exception {
        runConcurrent(
                "StandardPluralRanges",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        PluralRules rules = PluralRules.forLocale(ULocale.ENGLISH);
                        assertNotNull("PluralRules should not be null", rules);
                        String keyword = rules.select(1);
                        assertNotNull("select() should not return null", keyword);
                    }
                });
    }
}

// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.lang.UScript;
import com.ibm.icu.lang.UScriptRun;
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

    /**
     * UScriptRun.parenStack was a static array shared across all instances. Concurrent instances
     * would corrupt each other's parenthesis tracking, producing wrong script codes.
     */
    @Test
    public void testUScriptRunConcurrent() throws Exception {
        // Mixed-script text with parentheses to exercise parenStack
        String latin = "Hello (world) test";
        String cjk = "\u4e16\u754c(\u4f60\u597d)\u6d4b\u8bd5";
        runConcurrent(
                "UScriptRun",
                tid -> {
                    String text = (tid % 2 == 0) ? latin : cjk;
                    for (int i = 0; i < ITERATIONS; i++) {
                        UScriptRun run = new UScriptRun(text);
                        while (run.next()) {
                            int script = run.getScriptCode();
                            assertTrue(
                                    "script code should be valid, got " + script,
                                    script >= 0 && script < UScript.CODE_LIMIT);
                        }
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

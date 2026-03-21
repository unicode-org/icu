// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.util;

import com.ibm.icu.text.Transliterator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Concurrency regression tests for Transliterator safe publication. */
@RunWith(JUnit4.class)
public class TransliteratorConcurrencyTest extends ConcurrencyTest {

    /** Concurrent transliteration must safely see the volatile sourceTargetUtility field. */
    @Test
    public void testTransliteratorConcurrentAccess() throws Exception {
        String[] ids = {"Any-Upper", "Any-Lower", "Any-Title", "Any-NFC", "Any-NFD"};
        runConcurrent(
                "TransliteratorAccess",
                tid -> {
                    for (int i = 0; i < ITERATIONS; i++) {
                        String id = ids[(tid + i) % ids.length];
                        Transliterator t = Transliterator.getInstance(id);
                        assertNotNull("Transliterator should not be null for " + id, t);
                        String result = t.transliterate("Hello World 123");
                        assertNotNull("transliterate result should not be null", result);
                        assertFalse("transliterate result should not be empty", result.isEmpty());
                    }
                });
    }
}

// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.text.BreakIterator;
import java.text.CharacterIterator;
import java.util.Arrays;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class BreakIteratorRegTest extends CoreTestFmwk {
    @Test
    public void TestRegUnreg() {
        Locale thailand_locale = new Locale("th", "TH", "");
        Locale foo_locale = new Locale("fu", "FU", "FOO");
        BreakIterator jwbi = BreakIterator.getWordInstance(Locale.JAPAN);
        BreakIterator uwbi = BreakIterator.getWordInstance(Locale.US);
        BreakIterator usbi = BreakIterator.getSentenceInstance(Locale.US);
        BreakIterator twbi = BreakIterator.getWordInstance(thailand_locale);
        BreakIterator rwbi = BreakIterator.getWordInstance(new Locale("", "", ""));

        BreakIterator sbi = usbi.clone();
        // todo: this will cause the test to fail, no way to set a breakiterator to null text so
        // can't fix yet.
        // String text = "This is some test, by golly. Boy, they don't make tests like they used to,
        // do they?  This here test ain't worth $2.50.  Nope.";
        // sbi.setText(text);

        assertTrue("unregister before register", !BreakIterator.unregister("")); // coverage

        Object key0 =
                BreakIterator.registerInstance(twbi.clone(), foo_locale, BreakIterator.KIND_WORD);
        Object key1 = BreakIterator.registerInstance(sbi, Locale.US, BreakIterator.KIND_WORD);
        Object key2 =
                BreakIterator.registerInstance(twbi.clone(), Locale.US, BreakIterator.KIND_WORD);

        {
            BreakIterator test0 = BreakIterator.getWordInstance(Locale.JAPAN);
            BreakIterator test1 = BreakIterator.getWordInstance(Locale.US);
            BreakIterator test2 = BreakIterator.getSentenceInstance(Locale.US);
            BreakIterator test3 = BreakIterator.getWordInstance(thailand_locale);
            BreakIterator test4 = BreakIterator.getWordInstance(foo_locale);

            assertEquals("japan word == japan word", test0, jwbi);
            assertEquals("us word == thai word", test1, twbi);
            assertEquals("us sentence == us sentence", test2, usbi);
            assertEquals("thai word == thai word", test3, twbi);
            assertEquals("foo word == thai word", test4, twbi);
        }

        // Locale[] locales = BreakIterator.getAvailableLocales();

        assertTrue("unregister us word (thai word)", BreakIterator.unregister(key2));
        assertTrue("unregister second time", !BreakIterator.unregister(key2));
        boolean error = false;
        try {
            BreakIterator.unregister(null);
        } catch (IllegalArgumentException e) {
            error = true;
        }

        assertTrue("unregister null", error);

        {
            CharacterIterator sci = BreakIterator.getWordInstance(Locale.US).getText();
            int len = sci.getEndIndex() - sci.getBeginIndex();
            assertEquals("us word text: " + getString(sci), len, 0);
        }

        assertTrue(
                "foo_locale",
                Arrays.asList(BreakIterator.getAvailableLocales()).contains(foo_locale));
        assertTrue("unregister foo word (thai word)", BreakIterator.unregister(key0));
        assertTrue(
                "no foo_locale",
                !Arrays.asList(BreakIterator.getAvailableLocales()).contains(foo_locale));
        assertEquals("us word == us sentence", BreakIterator.getWordInstance(Locale.US), usbi);

        assertTrue("unregister us word (us sentence)", BreakIterator.unregister(key1));
        {
            BreakIterator test0 = BreakIterator.getWordInstance(Locale.JAPAN);
            BreakIterator test1 = BreakIterator.getWordInstance(Locale.US);
            BreakIterator test2 = BreakIterator.getSentenceInstance(Locale.US);
            BreakIterator test3 = BreakIterator.getWordInstance(thailand_locale);
            BreakIterator test4 = BreakIterator.getWordInstance(foo_locale);

            assertEquals("japanese word break", test0, jwbi);
            assertEquals("us sentence-word break", test1, uwbi);
            assertEquals("us sentence break", test2, usbi);
            assertEquals("thai word break", test3, twbi);
            assertEquals("root word break", test4, rwbi);

            CharacterIterator sci = test1.getText();
            int len = sci.getEndIndex() - sci.getBeginIndex();
            assertEquals("us sentence-word break text: " + getString(sci), len, 0);
        }
    }

    private static String getString(CharacterIterator ci) {
        StringBuffer buf = new StringBuffer(ci.getEndIndex() - ci.getBeginIndex() + 2);
        buf.append("'");
        for (char c = ci.first(); c != CharacterIterator.DONE; c = ci.next()) {
            buf.append(c);
        }
        buf.append("'");
        return buf.toString();
    }
}

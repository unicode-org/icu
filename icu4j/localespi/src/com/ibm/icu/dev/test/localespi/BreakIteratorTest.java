/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.BreakIterator;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;

public class BreakIteratorTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new BreakIteratorTest().run(args);
    }

    private static final int CHARACTER_BRK = 0;
    private static final int WORD_BRK = 1;
    private static final int LINE_BRK = 2;
    private static final int SENTENCE_BRK = 3;

    public void TestGetInstance() {
        for (Locale loc : TestUtil.getICULocales()) {
            checkGetInstance(CHARACTER_BRK, loc);
            checkGetInstance(WORD_BRK, loc);
            checkGetInstance(LINE_BRK, loc);
            checkGetInstance(SENTENCE_BRK, loc);
        }
    }

    private void checkGetInstance(int type, Locale loc) {
        BreakIterator brkitr = null;
        String method = null;
        switch (type) {
        case CHARACTER_BRK:
            brkitr = BreakIterator.getCharacterInstance(loc);
            method = "getCharacterInstance";
            break;
        case WORD_BRK:
            brkitr = BreakIterator.getWordInstance(loc);
            method = "getWordInstance";
            break;
        case LINE_BRK:
            brkitr = BreakIterator.getLineInstance(loc);
            method = "getLineInstance";
            break;
        case SENTENCE_BRK:
            brkitr = BreakIterator.getSentenceInstance(loc);
            method = "getSentenceInstance";
            break;
        default:
            errln("FAIL: Unknown break iterator type");
            return;
        }

        if (TestUtil.isICUOnly(loc)) {
            if (!(brkitr instanceof com.ibm.icu.impl.jdkadapter.BreakIteratorICU)) {
                errln("FAIL: " + method + " returned JDK BreakIterator for locale " + loc);
            }
        } else {
            if (brkitr instanceof com.ibm.icu.impl.jdkadapter.BreakIteratorICU) {
                logln("INFO: " + method + " returned ICU BreakIterator for locale " + loc);
            } else {
                Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                switch (type) {
                case CHARACTER_BRK:
                    brkitr = BreakIterator.getCharacterInstance(iculoc);
                    break;
                case WORD_BRK:
                    brkitr = BreakIterator.getWordInstance(iculoc);
                    break;
                case LINE_BRK:
                    brkitr = BreakIterator.getLineInstance(iculoc);
                    break;
                case SENTENCE_BRK:
                    brkitr = BreakIterator.getSentenceInstance(iculoc);
                    break;
                }
                if (!(brkitr instanceof com.ibm.icu.impl.jdkadapter.BreakIteratorICU)) {
                    errln("FAIL: " + method + " returned JDK BreakIterator for locale " + iculoc);
                }
            }
        }
    }
}

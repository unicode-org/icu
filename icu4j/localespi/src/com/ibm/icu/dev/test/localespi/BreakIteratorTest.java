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

    /*
     * Check if getInstance returns the ICU implementation.
     */
    public void TestGetInstance() {
        for (Locale loc : BreakIterator.getAvailableLocales()) {
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

        boolean isIcuImpl = (brkitr instanceof com.ibm.icu.impl.jdkadapter.BreakIteratorICU);

        if (TestUtil.isICUExtendedLocale(loc)) {
            if (!isIcuImpl) {
                errln("FAIL: " + method + " returned JDK BreakIterator for locale " + loc);
            }
        } else {
            if (isIcuImpl) {
                logln("INFO: " + method + " returned ICU BreakIterator for locale " + loc);
            } 
            BreakIterator brkitrIcu = null;
            Locale iculoc = TestUtil.toICUExtendedLocale(loc);
            switch (type) {
            case CHARACTER_BRK:
                brkitrIcu = BreakIterator.getCharacterInstance(iculoc);
                break;
            case WORD_BRK:
                brkitrIcu = BreakIterator.getWordInstance(iculoc);
                break;
            case LINE_BRK:
                brkitrIcu = BreakIterator.getLineInstance(iculoc);
                break;
            case SENTENCE_BRK:
                brkitrIcu = BreakIterator.getSentenceInstance(iculoc);
                break;
            }
            if (isIcuImpl) {
                if (!brkitr.equals(brkitrIcu)) {
                    // BreakIterator.getXXXInstance returns a cached BreakIterator instance.
                    // BreakIterator does not override Object#equals, so the result may not be
                    // consistent.
//                        logln("INFO: " + method + " returned ICU BreakIterator for locale " + loc
//                                + ", but different from the one for locale " + iculoc);
                }
            } else {
                if (!(brkitrIcu instanceof com.ibm.icu.impl.jdkadapter.BreakIteratorICU)) {
                    errln("FAIL: " + method + " returned JDK BreakIterator for locale " + iculoc);
                }
            }
        }
    }
}

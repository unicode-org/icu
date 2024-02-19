// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.BreakIterator;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;

@RunWith(JUnit4.class)
public class BreakIteratorTest extends TestFmwk {
    private static final int CHARACTER_BRK = 0;
    private static final int WORD_BRK = 1;
    private static final int LINE_BRK = 2;
    private static final int SENTENCE_BRK = 3;

    /*
     * Check if getInstance returns the ICU implementation.
     */
    @Test
    public void TestGetInstance() {
        for (Locale loc : BreakIterator.getAvailableLocales()) {
            if (TestUtil.isExcluded(loc)) {
                logln("Skipped " + loc);
                continue;
            }
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

    /*
     * Testing the behavior of text break between ICU instance and its
     * equivalent created via the Locale SPI framework.
     */
    @Test
    public void TestICUEquivalent() {
        Locale[] TEST_LOCALES = {
                new Locale("en", "US"),
                new Locale("fr", "FR"),
                new Locale("th", "TH"),
                new Locale("zh", "CN"),
        };

        String[] TEST_DATA = {
                "International Components for Unicode (ICU) is an open source project of mature "
                + "C/C++ and Java libraries for Unicode support, software internationalization and "
                + "software globalization. ICU is widely portable to many operating systems and "
                + "environments. It gives applications the same results on all platforms and between "
                + "C/C++ and Java software. The ICU project is an open source development project "
                + "that is sponsored, supported and used by IBM and many other companies.",

                "L'International Components for Unicode (ICU) est un projet open source qui fourni "
                + "des biblioth\u00e8ques pour les langages informatique C/C++ et Java pour supporter "
                + "Unicode, l'internationalisation et la mondialisation des logiciels. ICU est largement "
                + "portable vers beaucoup de syst\u00e8mes d'exploitations et d'environnements. Il "
                + "donne aux applications les m\u00eames comportements et r\u00e9sultats sur toutes "
                + "les plateformes et entre les logiciels C/C++ et Java. Le projet ICU est un projet "
                + "dont les code sources sont disponibles qui est sponsoris\u00e9, support\u00e9 et "
                + "utilis\u00e9 par IBM et beaucoup d'autres entreprises.",

                "\u5728IBM\u7b49\u4f01\u696d\u4e2d\uff0c\u56fd\u9645\u5316\u7ecf\u5e38\u7b80\u5199"
                + "\u4e3aI18N (\u6216i18n\u6216I18n)\uff0c\u5176\u4e2d18\u4ee3\u8868\u4e86\u4e2d\u95f4"
                + "\u7701\u7565\u768418\u4e2a\u5b57\u6bcd\uff1b\u800c\u201c\u672c\u5730\u5316\u201d"
                + "\u540c\u53ef\u7b80\u5199\u4e3al10n\u3002\u9019\u4e24\u4e2a\u6982\u5ff5\u6709\u65f6"
                + "\u5408\u79f0\u5168\u7403\u5316\uff08g11n\uff09\uff0c\u4f46\u662f\u5168\u7403\u5316"
                + "\u7684\u6db5\u4e49\u66f4\u4e3a\u4e00\u822c\u5316\u3002\u53e6\u5916\u5076\u5c14\u4f1a"
                + "\u51fa\u73b0\u201cp13n\u201d\uff0c\u4ee3\u8868\u4e2a\u4eba\u5316\uff08personalization"
                + "\uff09\u3002",

                "\u0e01\u0e23\u0e38\u0e07\u0e40\u0e17\u0e1e\u0e21\u0e2b\u0e32\u0e19\u0e04\u0e23"
                + "\u0e43\u0e19\u0e1b\u0e31\u0e08\u0e08\u0e38\u0e1a\u0e31\u0e19\u0e40\u0e1b\u0e47"
                + "\u0e19\u0e28\u0e39\u0e19\u0e22\u0e4c\u0e01\u0e25\u0e32\u0e07\u0e01\u0e32\u0e23"
                + "\u0e1b\u0e01\u0e04\u0e23\u0e2d\u0e07 \u0e01\u0e32\u0e23\u0e28\u0e36\u0e01\u0e29"
                + "\u0e32 \u0e01\u0e32\u0e23\u0e04\u0e21\u0e19\u0e32\u0e04\u0e21\u0e02\u0e19\u0e2a"
                + "\u0e48\u0e07 \u0e01\u0e32\u0e23\u0e40\u0e07\u0e34\u0e19\u0e01\u0e32\u0e23\u0e18"
                + "\u0e19\u0e32\u0e04\u0e32\u0e23 \u0e01\u0e32\u0e23\u0e1e\u0e32\u0e13\u0e34\u0e0a"
                + "\u0e22\u0e4c \u0e01\u0e32\u0e23\u0e2a\u0e37\u0e48\u0e2d\u0e2a\u0e32\u0e23 \u0e2f"
                + "\u0e25\u0e2f \u0e42\u0e14\u0e22\u0e21\u0e35\u0e1e\u0e37\u0e49\u0e19\u0e17\u0e35"
                + "\u0e48\u0e17\u0e31\u0e49\u0e07\u0e2b\u0e21\u0e14 1,562.2 \u0e15\u0e32\u0e23\u0e32"
                + "\u0e07\u0e01\u0e34\u0e42\u0e25\u0e40\u0e21\u0e15\u0e23 \u0e1e\u0e34\u0e01\u0e31"
                + "\u0e14\u0e17\u0e32\u0e07\u0e20\u0e39\u0e21\u0e34\u0e28\u0e32\u0e2a\u0e15\u0e23"
                + "\u0e4c\u0e04\u0e37\u0e2d \u0e25\u0e30\u0e15\u0e34\u0e08\u0e39\u0e14 13\u00b0 45"
                + "\u2019 \u0e40\u0e2b\u0e19\u0e37\u0e2d \u0e25\u0e2d\u0e07\u0e08\u0e34\u0e08\u0e39"
                + "\u0e14 100\u00b0 31\u2019 \u0e15\u0e30\u0e27\u0e31\u0e19\u0e2d\u0e2d\u0e01"
        };

        BreakIterator[] jdkBrkItrs = new BreakIterator[4];
        com.ibm.icu.text.BreakIterator[] icuBrkItrs = new com.ibm.icu.text.BreakIterator[4];

        for (Locale loc : TEST_LOCALES) {
            Locale iculoc = TestUtil.toICUExtendedLocale(loc);

            jdkBrkItrs[0] = BreakIterator.getCharacterInstance(iculoc);
            jdkBrkItrs[1] = BreakIterator.getWordInstance(iculoc);
            jdkBrkItrs[2] = BreakIterator.getLineInstance(iculoc);
            jdkBrkItrs[3] = BreakIterator.getSentenceInstance(iculoc);

            icuBrkItrs[0] = com.ibm.icu.text.BreakIterator.getCharacterInstance(iculoc);
            icuBrkItrs[1] = com.ibm.icu.text.BreakIterator.getWordInstance(iculoc);
            icuBrkItrs[2] = com.ibm.icu.text.BreakIterator.getLineInstance(iculoc);
            icuBrkItrs[3] = com.ibm.icu.text.BreakIterator.getSentenceInstance(iculoc);

            for (String text : TEST_DATA) {
                for (int i = 0; i < 4; i++) {
                    compareBreaks(text, jdkBrkItrs[i], icuBrkItrs[i]);
                }
            }
        }
    }

    private void compareBreaks(String text, BreakIterator jdkBrk, com.ibm.icu.text.BreakIterator icuBrk) {
        jdkBrk.setText(text);
        icuBrk.setText(text);

        // Forward
        int jidx = jdkBrk.first();
        int iidx = icuBrk.first();
        if (jidx != iidx) {
            errln("FAIL: Different first boundaries (jdk=" + jidx + ",icu=" + iidx + ") for text:\n" + text);
        }
        while (true) {
            jidx = jdkBrk.next();
            iidx = icuBrk.next();
            if (jidx != iidx) {
                errln("FAIL: Different boundaries (jdk=" + jidx + ",icu=" + iidx + "direction=forward) for text:\n" + text);
            }
            if (jidx == BreakIterator.DONE) {
                break;
            }
        }

        // Backward
        jidx = jdkBrk.last();
        iidx = jdkBrk.last();
        if (jidx != iidx) {
            errln("FAIL: Different last boundaries (jdk=" + jidx + ",icu=" + iidx + ") for text:\n" + text);
        }
        while (true) {
            jidx = jdkBrk.previous();
            iidx = icuBrk.previous();
            if (jidx != iidx) {
                errln("FAIL: Different boundaries (jdk=" + jidx + ",icu=" + iidx + "direction=backward) for text:\n" + text);
            }
            if (jidx == BreakIterator.DONE) {
                break;
            }
        }
    }
}

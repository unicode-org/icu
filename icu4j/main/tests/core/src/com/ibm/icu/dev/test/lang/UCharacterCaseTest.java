// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
*******************************************************************************
* Copyright (C) 1996-2014, International Business Machines Corporation and
* others. All Rights Reserved.
*******************************************************************************
*/


package com.ibm.icu.dev.test.lang;


import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.impl.CaseMapImpl;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.CaseMap;
import com.ibm.icu.text.Edits;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.ULocale;


/**
* <p>Testing character casing</p>
* <p>Mostly following the test cases in strcase.cpp for ICU</p>
* @author Syn Wee Quek
* @since march 14 2002
*/
@RunWith(JUnit4.class)
public final class UCharacterCaseTest extends TestFmwk
{
    // constructor -----------------------------------------------------------

    /**
     * Constructor
     */
    public UCharacterCaseTest()
    {
    }

    // public methods --------------------------------------------------------

    /**
     * Testing the uppercase and lowercase function of UCharacter
     */
    @Test
    public void TestCharacter()
    {
        for (int i = 0; i < CHARACTER_LOWER_.length; i ++) {
            if (UCharacter.isLetter(CHARACTER_LOWER_[i]) &&
                !UCharacter.isLowerCase(CHARACTER_LOWER_[i])) {
                errln("FAIL isLowerCase test for \\u" +
                      hex(CHARACTER_LOWER_[i]));
                break;
            }
            if (UCharacter.isLetter(CHARACTER_UPPER_[i]) &&
                !(UCharacter.isUpperCase(CHARACTER_UPPER_[i]) ||
                  UCharacter.isTitleCase(CHARACTER_UPPER_[i]))) {
                errln("FAIL isUpperCase test for \\u" +
                      hex(CHARACTER_UPPER_[i]));
                break;
            }
            if (CHARACTER_LOWER_[i] !=
                UCharacter.toLowerCase(CHARACTER_UPPER_[i]) ||
                (CHARACTER_UPPER_[i] !=
                UCharacter.toUpperCase(CHARACTER_LOWER_[i]) &&
                CHARACTER_UPPER_[i] !=
                UCharacter.toTitleCase(CHARACTER_LOWER_[i]))) {
                errln("FAIL case conversion test for \\u" +
                      hex(CHARACTER_UPPER_[i]) +
                      " to \\u" + hex(CHARACTER_LOWER_[i]));
                break;
            }
            if (CHARACTER_LOWER_[i] !=
                UCharacter.toLowerCase(CHARACTER_LOWER_[i])) {
                errln("FAIL lower case conversion test for \\u" +
                      hex(CHARACTER_LOWER_[i]));
                break;
            }
            if (CHARACTER_UPPER_[i] !=
                UCharacter.toUpperCase(CHARACTER_UPPER_[i]) &&
                CHARACTER_UPPER_[i] !=
                UCharacter.toTitleCase(CHARACTER_UPPER_[i])) {
                errln("FAIL upper case conversion test for \\u" +
                      hex(CHARACTER_UPPER_[i]));
                break;
            }
            logln("Ok    \\u" + hex(CHARACTER_UPPER_[i]) + " and \\u" +
                  hex(CHARACTER_LOWER_[i]));
        }
    }

    @Test
    public void TestFolding()
    {
        // test simple case folding
        for (int i = 0; i < FOLDING_SIMPLE_.length; i += 3) {
            if (UCharacter.foldCase(FOLDING_SIMPLE_[i], true) !=
                FOLDING_SIMPLE_[i + 1]) {
                errln("FAIL: foldCase(\\u" + hex(FOLDING_SIMPLE_[i]) +
                      ", true) should be \\u" + hex(FOLDING_SIMPLE_[i + 1]));
            }
            if (UCharacter.foldCase(FOLDING_SIMPLE_[i],
                                    UCharacter.FOLD_CASE_DEFAULT) !=
                                                      FOLDING_SIMPLE_[i + 1]) {
                errln("FAIL: foldCase(\\u" + hex(FOLDING_SIMPLE_[i]) +
                      ", UCharacter.FOLD_CASE_DEFAULT) should be \\u"
                      + hex(FOLDING_SIMPLE_[i + 1]));
            }
            if (UCharacter.foldCase(FOLDING_SIMPLE_[i], false) !=
                FOLDING_SIMPLE_[i + 2]) {
                errln("FAIL: foldCase(\\u" + hex(FOLDING_SIMPLE_[i]) +
                      ", false) should be \\u" + hex(FOLDING_SIMPLE_[i + 2]));
            }
            if (UCharacter.foldCase(FOLDING_SIMPLE_[i],
                                    UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I) !=
                                    FOLDING_SIMPLE_[i + 2]) {
                errln("FAIL: foldCase(\\u" + hex(FOLDING_SIMPLE_[i]) +
                      ", UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I) should be \\u"
                      + hex(FOLDING_SIMPLE_[i + 2]));
            }
        }

        // Test full string case folding with default option and separate
        // buffers
        if (!FOLDING_DEFAULT_[0].equals(UCharacter.foldCase(FOLDING_MIXED_[0], true))) {
            errln("FAIL: foldCase(" + prettify(FOLDING_MIXED_[0]) +
                  ", true)=" + prettify(UCharacter.foldCase(FOLDING_MIXED_[0], true)) +
                  " should be " + prettify(FOLDING_DEFAULT_[0]));
        }

        if (!FOLDING_DEFAULT_[0].equals(UCharacter.foldCase(FOLDING_MIXED_[0], UCharacter.FOLD_CASE_DEFAULT))) {
                    errln("FAIL: foldCase(" + prettify(FOLDING_MIXED_[0]) +
                          ", UCharacter.FOLD_CASE_DEFAULT)=" + prettify(UCharacter.foldCase(FOLDING_MIXED_[0], UCharacter.FOLD_CASE_DEFAULT))
                          + " should be " + prettify(FOLDING_DEFAULT_[0]));
                }

        if (!FOLDING_EXCLUDE_SPECIAL_I_[0].equals(
                            UCharacter.foldCase(FOLDING_MIXED_[0], false))) {
            errln("FAIL: foldCase(" + prettify(FOLDING_MIXED_[0]) +
                  ", false)=" + prettify(UCharacter.foldCase(FOLDING_MIXED_[0], false))
                  + " should be " + prettify(FOLDING_EXCLUDE_SPECIAL_I_[0]));
        }

        if (!FOLDING_EXCLUDE_SPECIAL_I_[0].equals(
                                    UCharacter.foldCase(FOLDING_MIXED_[0], UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I))) {
            errln("FAIL: foldCase(" + prettify(FOLDING_MIXED_[0]) +
                  ", UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I)=" + prettify(UCharacter.foldCase(FOLDING_MIXED_[0], UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I))
                  + " should be " + prettify(FOLDING_EXCLUDE_SPECIAL_I_[0]));
        }

        if (!FOLDING_DEFAULT_[1].equals(UCharacter.foldCase(FOLDING_MIXED_[1], true))) {
           errln("FAIL: foldCase(" + prettify(FOLDING_MIXED_[1]) +
                 ", true)=" + prettify(UCharacter.foldCase(FOLDING_MIXED_[1], true))
                 + " should be " + prettify(FOLDING_DEFAULT_[1]));
        }

        if (!FOLDING_DEFAULT_[1].equals(UCharacter.foldCase(FOLDING_MIXED_[1], UCharacter.FOLD_CASE_DEFAULT))) {
            errln("FAIL: foldCase(" + prettify(FOLDING_MIXED_[1]) +
                         ", UCharacter.FOLD_CASE_DEFAULT)=" + prettify(UCharacter.foldCase(FOLDING_MIXED_[1], UCharacter.FOLD_CASE_DEFAULT))
                         + " should be " + prettify(FOLDING_DEFAULT_[1]));
        }

        // alternate handling for dotted I/dotless i (U+0130, U+0131)
        if (!FOLDING_EXCLUDE_SPECIAL_I_[1].equals(
                        UCharacter.foldCase(FOLDING_MIXED_[1], false))) {
            errln("FAIL: foldCase(" + prettify(FOLDING_MIXED_[1]) +
                  ", false)=" + prettify(UCharacter.foldCase(FOLDING_MIXED_[1], false))
                  + " should be " + prettify(FOLDING_EXCLUDE_SPECIAL_I_[1]));
        }

        if (!FOLDING_EXCLUDE_SPECIAL_I_[1].equals(
                                UCharacter.foldCase(FOLDING_MIXED_[1], UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I))) {
            errln("FAIL: foldCase(" + prettify(FOLDING_MIXED_[1]) +
                  ", UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I)=" + prettify(UCharacter.foldCase(FOLDING_MIXED_[1], UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I))
                  + " should be "
                  + prettify(FOLDING_EXCLUDE_SPECIAL_I_[1]));
        }
    }

    @Test
    public void TestInvalidCodePointFolding() {
        int[] invalidCodePoints = {
                0xD800, // lead surrogate
                0xDFFF, // trail surrogate
                0xFDD0, // noncharacter
                0xFFFF, // noncharacter
                0x110000, // out of range
                -1 // negative
        };
        for (int cp : invalidCodePoints) {
            assertEquals("Invalid code points should be echoed back",
                    cp, UCharacter.foldCase(cp, true));
            assertEquals("Invalid code points should be echoed back",
                    cp, UCharacter.foldCase(cp, false));
            assertEquals("Invalid code points should be echoed back",
                    cp, UCharacter.foldCase(cp, UCharacter.FOLD_CASE_DEFAULT));
            assertEquals("Invalid code points should be echoed back",
                    cp, UCharacter.foldCase(cp, UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I));
        }
    }

    /**
     * Testing the strings case mapping methods
     */
    @Test
    public void TestUpper()
    {
        // uppercase with root locale and in the same buffer
        if (!UPPER_ROOT_.equals(UCharacter.toUpperCase(UPPER_BEFORE_))) {
            errln("Fail " + UPPER_BEFORE_ + " after uppercase should be " +
                  UPPER_ROOT_ + " instead got " +
                  UCharacter.toUpperCase(UPPER_BEFORE_));
        }

        // uppercase with turkish locale and separate buffers
        if (!UPPER_TURKISH_.equals(UCharacter.toUpperCase(TURKISH_LOCALE_,
                                                         UPPER_BEFORE_))) {
            errln("Fail " + UPPER_BEFORE_ +
                  " after turkish-sensitive uppercase should be " +
                  UPPER_TURKISH_ + " instead of " +
                  UCharacter.toUpperCase(TURKISH_LOCALE_, UPPER_BEFORE_));
        }

        // uppercase a short string with root locale
        if (!UPPER_MINI_UPPER_.equals(UCharacter.toUpperCase(UPPER_MINI_))) {
            errln("error in toUpper(root locale)=\"" + UPPER_MINI_ +
                  "\" expected \"" + UPPER_MINI_UPPER_ + "\"");
        }

        if (!SHARED_UPPERCASE_TOPKAP_.equals(
                       UCharacter.toUpperCase(SHARED_LOWERCASE_TOPKAP_))) {
            errln("toUpper failed: expected \"" +
                  SHARED_UPPERCASE_TOPKAP_ + "\", got \"" +
                  UCharacter.toUpperCase(SHARED_LOWERCASE_TOPKAP_) + "\".");
        }

        if (!SHARED_UPPERCASE_TURKISH_.equals(
                  UCharacter.toUpperCase(TURKISH_LOCALE_,
                                         SHARED_LOWERCASE_TOPKAP_))) {
            errln("toUpper failed: expected \"" +
                  SHARED_UPPERCASE_TURKISH_ + "\", got \"" +
                  UCharacter.toUpperCase(TURKISH_LOCALE_,
                                     SHARED_LOWERCASE_TOPKAP_) + "\".");
        }

        if (!SHARED_UPPERCASE_GERMAN_.equals(
                UCharacter.toUpperCase(GERMAN_LOCALE_,
                                       SHARED_LOWERCASE_GERMAN_))) {
            errln("toUpper failed: expected \"" + SHARED_UPPERCASE_GERMAN_
                  + "\", got \"" + UCharacter.toUpperCase(GERMAN_LOCALE_,
                                        SHARED_LOWERCASE_GERMAN_) + "\".");
        }

        if (!SHARED_UPPERCASE_GREEK_.equals(
                UCharacter.toUpperCase(SHARED_LOWERCASE_GREEK_))) {
            errln("toLower failed: expected \"" + SHARED_UPPERCASE_GREEK_ +
                  "\", got \"" + UCharacter.toUpperCase(
                                        SHARED_LOWERCASE_GREEK_) + "\".");
        }
    }

    @Test
    public void TestLower()
    {
        if (!LOWER_ROOT_.equals(UCharacter.toLowerCase(LOWER_BEFORE_))) {
            errln("Fail " + LOWER_BEFORE_ + " after lowercase should be " +
                  LOWER_ROOT_ + " instead of " +
                  UCharacter.toLowerCase(LOWER_BEFORE_));
        }

        // lowercase with turkish locale
        if (!LOWER_TURKISH_.equals(UCharacter.toLowerCase(TURKISH_LOCALE_,
                                                          LOWER_BEFORE_))) {
            errln("Fail " + LOWER_BEFORE_ +
                  " after turkish-sensitive lowercase should be " +
                  LOWER_TURKISH_ + " instead of " +
                  UCharacter.toLowerCase(TURKISH_LOCALE_, LOWER_BEFORE_));
        }
        if (!SHARED_LOWERCASE_ISTANBUL_.equals(
                     UCharacter.toLowerCase(SHARED_UPPERCASE_ISTANBUL_))) {
            errln("1. toLower failed: expected \"" +
                  SHARED_LOWERCASE_ISTANBUL_ + "\", got \"" +
              UCharacter.toLowerCase(SHARED_UPPERCASE_ISTANBUL_) + "\".");
        }

        if (!SHARED_LOWERCASE_TURKISH_.equals(
                UCharacter.toLowerCase(TURKISH_LOCALE_,
                                       SHARED_UPPERCASE_ISTANBUL_))) {
            errln("2. toLower failed: expected \"" +
                  SHARED_LOWERCASE_TURKISH_ + "\", got \"" +
                  UCharacter.toLowerCase(TURKISH_LOCALE_,
                                SHARED_UPPERCASE_ISTANBUL_) + "\".");
        }
        if (!SHARED_LOWERCASE_GREEK_.equals(
                UCharacter.toLowerCase(GREEK_LOCALE_,
                                       SHARED_UPPERCASE_GREEK_))) {
            errln("toLower failed: expected \"" + SHARED_LOWERCASE_GREEK_ +
                  "\", got \"" + UCharacter.toLowerCase(GREEK_LOCALE_,
                                        SHARED_UPPERCASE_GREEK_) + "\".");
        }
    }

    @Test
    public void TestTitleRegression() throws java.io.IOException {
        boolean isIgnorable = UCharacter.hasBinaryProperty('\'', UProperty.CASE_IGNORABLE);
        assertTrue("Case Ignorable check of ASCII apostrophe", isIgnorable);
        assertEquals("Titlecase check",
                "The Quick Brown Fox Can't Jump Over The Lazy Dogs.",
                UCharacter.toTitleCase(ULocale.ENGLISH, "THE QUICK BROWN FOX CAN'T JUMP OVER THE LAZY DOGS.", null));
    }

    @Test
    public void TestTitle()
    {
         try{
            for (int i = 0; i < TITLE_DATA_.length;) {
                String test = TITLE_DATA_[i++];
                String expected = TITLE_DATA_[i++];
                ULocale locale = new ULocale(TITLE_DATA_[i++]);
                int breakType = Integer.parseInt(TITLE_DATA_[i++]);
                String optionsString = TITLE_DATA_[i++];
                BreakIterator iter =
                    breakType >= 0 ?
                        BreakIterator.getBreakInstance(locale, breakType) :
                        breakType == -2 ?
                            // Open a trivial break iterator that only delivers { 0, length }
                            // or even just { 0 } as boundaries.
                            new RuleBasedBreakIterator(".*;") :
                            null;
                int options = 0;
                if (optionsString.indexOf('L') >= 0) {
                    options |= UCharacter.TITLECASE_NO_LOWERCASE;
                }
                if (optionsString.indexOf('A') >= 0) {
                    options |= UCharacter.TITLECASE_NO_BREAK_ADJUSTMENT;
                }
                String result = UCharacter.toTitleCase(locale, test, iter, options);
                if (!expected.equals(result)) {
                    errln("titlecasing for " + prettify(test) + " (options " + options + ") should be " +
                          prettify(expected) + " but got " +
                          prettify(result));
                }
                if (options == 0) {
                    result = UCharacter.toTitleCase(locale, test, iter);
                    if (!expected.equals(result)) {
                        errln("titlecasing for " + prettify(test) + " should be " +
                              prettify(expected) + " but got " +
                              prettify(result));
                    }
                }
            }
         }catch(Exception ex){
            warnln("Could not find data for BreakIterators");
         }
    }

    // Not a @Test. See ICU4C intltest strcase.cpp TestCasingImpl().
    void TestCasingImpl(String input, String output, CaseMap.Title toTitle, Locale locale) {
        String result = toTitle.apply(locale, null, input, new StringBuilder(), null).toString();
        assertEquals("toTitle(" + input + ')', output, result);
    }

    @Test
    public void TestTitleOptions() {
        Locale root = Locale.ROOT;
        // New options in ICU 60.
        TestCasingImpl("ʻcAt! ʻeTc.", "ʻCat! ʻetc.",
                CaseMap.toTitle().wholeString(), root);
        TestCasingImpl("a ʻCaT. A ʻdOg! ʻeTc.", "A ʻCaT. A ʻdOg! ʻETc.",
                CaseMap.toTitle().sentences().noLowercase(), root);
        TestCasingImpl("49eRs", "49ers",
                CaseMap.toTitle().wholeString(), root);
        TestCasingImpl("«丰(aBc)»", "«丰(abc)»",
                CaseMap.toTitle().wholeString(), root);
        TestCasingImpl("49eRs", "49Ers",
                CaseMap.toTitle().wholeString().adjustToCased(), root);
        TestCasingImpl("«丰(aBc)»", "«丰(Abc)»",
                CaseMap.toTitle().wholeString().adjustToCased(), root);
        TestCasingImpl(" john. Smith", " John. Smith",
                CaseMap.toTitle().wholeString().noLowercase(), root);
        TestCasingImpl(" john. Smith", " john. smith",
                CaseMap.toTitle().wholeString().noBreakAdjustment(), root);
        TestCasingImpl("«ijs»", "«IJs»",
                CaseMap.toTitle().wholeString(), new Locale("nl", "BE"));
        TestCasingImpl("«ijs»", "«İjs»",
                CaseMap.toTitle().wholeString(), new Locale("tr", "DE"));

        // Test conflicting settings.
        // If & when we add more options, then the ORed combinations may become
        // indistinguishable from valid values.
        try {
            CaseMap.toTitle().noBreakAdjustment().adjustToCased().
                    apply(root, null, "", new StringBuilder(), null);
            fail("CaseMap.toTitle(multiple adjustment options) " +
                    "did not throw an IllegalArgumentException");
        } catch(IllegalArgumentException expected) {
        }
        try {
            CaseMap.toTitle().wholeString().sentences().
                    apply(root, null, "", new StringBuilder(), null);
            fail("CaseMap.toTitle(multiple iterator options) " +
                    "did not throw an IllegalArgumentException");
        } catch(IllegalArgumentException expected) {
        }
        BreakIterator iter = BreakIterator.getCharacterInstance(root);
        try {
            CaseMap.toTitle().wholeString().apply(root, iter, "", new StringBuilder(), null);
            fail("CaseMap.toTitle(iterator option + iterator) " +
                    "did not throw an IllegalArgumentException");
        } catch(IllegalArgumentException expected) {
        }
    }

    @Test
    public void TestLithuanianTitle() {
        ULocale LOC_LITHUANIAN = new ULocale("lt");

        assertEquals("Lithuanian titlecase check in Lithuanian",
                "\u0058\u0069\u0307\u0308",
                UCharacter.toTitleCase(LOC_LITHUANIAN, "\u0058\u0049\u0308", null));

        assertEquals("Lithuanian titlecase check in Lithuanian",
                "\u0058\u0069\u0307\u0308",
                UCharacter.toTitleCase(LITHUANIAN_LOCALE_, "\u0058\u0049\u0308", null));
    }

    @Test
    public void TestDutchTitle() {
        ULocale LOC_DUTCH = new ULocale("nl");
        int options = 0;
        options |= UCharacter.TITLECASE_NO_LOWERCASE;
        BreakIterator iter = BreakIterator.getWordInstance(LOC_DUTCH);

        assertEquals("Dutch titlecase check in English",
                "Ijssel Igloo Ijmuiden",
                UCharacter.toTitleCase(ULocale.ENGLISH, "ijssel igloo IJMUIDEN", null));

        assertEquals("Dutch titlecase check in Dutch",
                "IJssel Igloo IJmuiden",
                UCharacter.toTitleCase(LOC_DUTCH, "ijssel igloo IJMUIDEN", null));

        // Also check the behavior using Java Locale
        assertEquals("Dutch titlecase check in English (Java Locale)",
                "Ijssel Igloo Ijmuiden",
                UCharacter.toTitleCase(Locale.ENGLISH, "ijssel igloo IJMUIDEN", null));

        assertEquals("Dutch titlecase check in Dutch (Java Locale)",
                "IJssel Igloo IJmuiden",
                UCharacter.toTitleCase(DUTCH_LOCALE_, "ijssel igloo IJMUIDEN", null));

        iter.setText("ijssel igloo IjMUIdEN iPoD ijenough");
        assertEquals("Dutch titlecase check in Dutch with nolowercase option",
                "IJssel Igloo IJMUIdEN IPoD IJenough",
                UCharacter.toTitleCase(LOC_DUTCH, "ijssel igloo IjMUIdEN iPoD ijenough", iter, options));

        // Accented IJ testing

        String[][] dutchIJCasesData = {
                // input,           expectedFull,     expOnlyChanged
                {"ij",              "IJ",             "IJ"},
                {"IJ",              "IJ",             ""},
                {"íj́",              "ÍJ́",             "ÍJ"},
                {"ÍJ́",              "ÍJ́",             ""},
                {"íJ́",              "ÍJ́",             "Í"},
                {"Ij́",              "Ij́",             ""},
                {"ij́",              "Ij́",             "I"},
                {"ïj́",              "Ïj́",             "Ï"},
                {"íj\u0308",        "Íj\u0308",       "Í"},
                {"íj́\uD834\uDD6E",  "Íj́\uD834\uDD6E", "Í"}, // \uD834\uDD6E == \U0001D16E
                {"íj\u1ABE",        "Íj\u1ABE",       "Í"},

                {"ijabc",              "IJabc",             "IJ"},
                {"IJabc",              "IJabc",             ""},
                {"íj́abc",              "ÍJ́abc",             "ÍJ"},
                {"ÍJ́abc",              "ÍJ́abc",             ""},
                {"íJ́abc",              "ÍJ́abc",             "Í"},
                {"Ij́abc",              "Ij́abc",             ""},
                {"ij́abc",              "Ij́abc",             "I"},
                {"ïj́abc",              "Ïj́abc",             "Ï"},
                {"íjabc\u0308",        "Íjabc\u0308",       "Í"},
                {"íj́abc\uD834\uDD6E",  "ÍJ́abc\uD834\uDD6E", "ÍJ"},
                {"íjabc\u1ABE",        "Íjabc\u1ABE",       "Í"},

                // Bug ICU-21919
                {"Í",                  "Í",                 ""},
        };

        for (String[] caseDatum : dutchIJCasesData) {
            String input = caseDatum[0];
            String expectedFull = caseDatum[1];
            String expectedOnlyChanged = caseDatum[2];

            for (boolean isOnlyChanged : Arrays.asList(true, false)) {
                String testMsg = "Dutch accented ij"
                        + (isOnlyChanged ? ", only changes" : "");

                int testOptions = UCharacter.TITLECASE_NO_LOWERCASE
                        | (isOnlyChanged ? CaseMapImpl.OMIT_UNCHANGED_TEXT : 0);

                CaseMap.Title titleCaseMapBase = CaseMap.toTitle().noLowercase();
                CaseMap.Title titleCaseMap = isOnlyChanged ? titleCaseMapBase.omitUnchangedText() : titleCaseMapBase;

                String expected = isOnlyChanged ? expectedOnlyChanged : expectedFull;

                // Newer API for title casing
                StringBuilder resultBuilder = new StringBuilder();
                Edits edits = new Edits();
                titleCaseMap.apply(DUTCH_LOCALE_, null, input, resultBuilder, edits);
                String result = resultBuilder.toString();
                assertEquals(testMsg + ", [" + input + "]",
                        expected, result);

                // Older API for title casing (vs. Newer API)
                String oldApiResult = UCharacter.toTitleCase(LOC_DUTCH, input, null, testOptions);
                assertEquals(testMsg + ", Title.apply() vs UCharacter.toTitleCase()" + ", [" + input + "]",
                        result, oldApiResult);
            }
        }
    }

    @Test
    public void TestSpecial()
    {
        for (int i = 0; i < SPECIAL_LOCALES_.length; i ++) {
            int    j      = i * 3;
            Locale locale = SPECIAL_LOCALES_[i];
            String str    = SPECIAL_DATA_[j];
            if (locale != null) {
                if (!SPECIAL_DATA_[j + 1].equals(
                     UCharacter.toLowerCase(locale, str))) {
                    errln("error lowercasing special characters " +
                        hex(str) + " expected " + hex(SPECIAL_DATA_[j + 1])
                        + " for locale " + locale.toString() + " but got " +
                        hex(UCharacter.toLowerCase(locale, str)));
                }
                if (!SPECIAL_DATA_[j + 2].equals(
                     UCharacter.toUpperCase(locale, str))) {
                    errln("error uppercasing special characters " +
                        hex(str) + " expected " + SPECIAL_DATA_[j + 2]
                        + " for locale " + locale.toString() + " but got " +
                        hex(UCharacter.toUpperCase(locale, str)));
                }
            }
            else {
                String lower = UCharacter.toLowerCase(str);
                if (!SPECIAL_DATA_[j + 1].equals(lower)) {
                    errln("error lowercasing special characters " +
                        hex(str) + " expected " + SPECIAL_DATA_[j + 1] +
                        " but got " + hex(lower));
                }
                String upper = UCharacter.toUpperCase(str);
                if (!SPECIAL_DATA_[j + 2].equals(upper)) {
                    errln("error uppercasing special characters " +
                        hex(str) + " expected " + SPECIAL_DATA_[j + 2] +
                        " but got " + hex(upper));
                }
            }
        }

        // turkish & azerbaijani dotless i & dotted I
        // remove dot above if there was a capital I before and there are no
        // more accents above
        if (!SPECIAL_DOTTED_LOWER_TURKISH_.equals(UCharacter.toLowerCase(
                                        TURKISH_LOCALE_, SPECIAL_DOTTED_))) {
            errln("error in dots.toLower(tr)=\"" + SPECIAL_DOTTED_ +
                  "\" expected \"" + SPECIAL_DOTTED_LOWER_TURKISH_ +
                  "\" but got " + UCharacter.toLowerCase(TURKISH_LOCALE_,
                                                         SPECIAL_DOTTED_));
        }
        if (!SPECIAL_DOTTED_LOWER_GERMAN_.equals(UCharacter.toLowerCase(
                                             GERMAN_LOCALE_, SPECIAL_DOTTED_))) {
            errln("error in dots.toLower(de)=\"" + SPECIAL_DOTTED_ +
                  "\" expected \"" + SPECIAL_DOTTED_LOWER_GERMAN_ +
                  "\" but got " + UCharacter.toLowerCase(GERMAN_LOCALE_,
                                                         SPECIAL_DOTTED_));
        }

        // lithuanian dot above in uppercasing
        if (!SPECIAL_DOT_ABOVE_UPPER_LITHUANIAN_.equals(
             UCharacter.toUpperCase(LITHUANIAN_LOCALE_, SPECIAL_DOT_ABOVE_))) {
            errln("error in dots.toUpper(lt)=\"" + SPECIAL_DOT_ABOVE_ +
                  "\" expected \"" + SPECIAL_DOT_ABOVE_UPPER_LITHUANIAN_ +
                  "\" but got " + UCharacter.toUpperCase(LITHUANIAN_LOCALE_,
                                                         SPECIAL_DOT_ABOVE_));
        }
        if (!SPECIAL_DOT_ABOVE_UPPER_GERMAN_.equals(UCharacter.toUpperCase(
                                        GERMAN_LOCALE_, SPECIAL_DOT_ABOVE_))) {
            errln("error in dots.toUpper(de)=\"" + SPECIAL_DOT_ABOVE_ +
                  "\" expected \"" + SPECIAL_DOT_ABOVE_UPPER_GERMAN_ +
                  "\" but got " + UCharacter.toUpperCase(GERMAN_LOCALE_,
                                                         SPECIAL_DOT_ABOVE_));
        }

        // lithuanian adds dot above to i in lowercasing if there are more
        // above accents
        if (!SPECIAL_DOT_ABOVE_LOWER_LITHUANIAN_.equals(
            UCharacter.toLowerCase(LITHUANIAN_LOCALE_,
                                   SPECIAL_DOT_ABOVE_UPPER_))) {
            errln("error in dots.toLower(lt)=\"" + SPECIAL_DOT_ABOVE_UPPER_ +
                  "\" expected \"" + SPECIAL_DOT_ABOVE_LOWER_LITHUANIAN_ +
                  "\" but got " + UCharacter.toLowerCase(LITHUANIAN_LOCALE_,
                                                   SPECIAL_DOT_ABOVE_UPPER_));
        }
        if (!SPECIAL_DOT_ABOVE_LOWER_GERMAN_.equals(
            UCharacter.toLowerCase(GERMAN_LOCALE_,
                                   SPECIAL_DOT_ABOVE_UPPER_))) {
            errln("error in dots.toLower(de)=\"" + SPECIAL_DOT_ABOVE_UPPER_ +
                  "\" expected \"" + SPECIAL_DOT_ABOVE_LOWER_GERMAN_ +
                  "\" but got " + UCharacter.toLowerCase(GERMAN_LOCALE_,
                                                   SPECIAL_DOT_ABOVE_UPPER_));
        }
    }

    /**
     * Tests for case mapping in the file SpecialCasing.txt
     * This method reads in SpecialCasing.txt file for testing purposes.
     * A default path is provided relative to the src path, however the user
     * could set a system property to change the directory path.<br>
     * e.g. java -DUnicodeData="data_dir_path" com.ibm.dev.test.lang.UCharacterTest
     */
    @Test
    public void TestSpecialCasingTxt()
    {
        try
        {
            // reading in the SpecialCasing file
            BufferedReader input = TestUtil.getDataReader(
                                                  "unicode/SpecialCasing.txt");
            while (true)
            {
                String s = input.readLine();
                if (s == null) {
                    break;
                }
                if (s.length() == 0 || s.charAt(0) == '#') {
                    continue;
                }

                String chstr[] = getUnicodeStrings(s);
                StringBuffer strbuffer   = new StringBuffer(chstr[0]);
                StringBuffer lowerbuffer = new StringBuffer(chstr[1]);
                StringBuffer upperbuffer = new StringBuffer(chstr[3]);
                Locale locale = null;
                for (int i = 4; i < chstr.length; i ++) {
                    String condition = chstr[i];
                    if (Character.isLowerCase(chstr[i].charAt(0))) {
                        // specified locale
                        locale = new Locale(chstr[i], "");
                    }
                    else if (condition.compareToIgnoreCase("Not_Before_Dot")
                                                      == 0) {
                        // turns I into dotless i
                    }
                    else if (condition.compareToIgnoreCase(
                                                      "More_Above") == 0) {
                            strbuffer.append((char)0x300);
                            lowerbuffer.append((char)0x300);
                            upperbuffer.append((char)0x300);
                    }
                    else if (condition.compareToIgnoreCase(
                                                "After_Soft_Dotted") == 0) {
                            strbuffer.insert(0, 'i');
                            lowerbuffer.insert(0, 'i');
                            String lang = "";
                            if (locale != null) {
                                lang = locale.getLanguage();
                            }
                            if (lang.equals("tr") || lang.equals("az")) {
                                // this is to be removed when 4.0 data comes out
                                // and upperbuffer.insert uncommented
                                // see jitterbug 2344
                                chstr[i] = "After_I";
                                strbuffer.deleteCharAt(0);
                                lowerbuffer.deleteCharAt(0);
                                i --;
                                continue;
                                // upperbuffer.insert(0, '\u0130');
                            }
                            else {
                                upperbuffer.insert(0, 'I');
                            }
                    }
                    else if (condition.compareToIgnoreCase(
                                                      "Final_Sigma") == 0) {
                            strbuffer.insert(0, 'c');
                            lowerbuffer.insert(0, 'c');
                            upperbuffer.insert(0, 'C');
                    }
                    else if (condition.compareToIgnoreCase("After_I") == 0) {
                            strbuffer.insert(0, 'I');
                            lowerbuffer.insert(0, 'i');
                            String lang = "";
                            if (locale != null) {
                                lang = locale.getLanguage();
                            }
                            if (lang.equals("tr") || lang.equals("az")) {
                                upperbuffer.insert(0, 'I');
                            }
                    }
                }
                chstr[0] = strbuffer.toString();
                chstr[1] = lowerbuffer.toString();
                chstr[3] = upperbuffer.toString();
                if (locale == null) {
                    if (!UCharacter.toLowerCase(chstr[0]).equals(chstr[1])) {
                        errln(s);
                        errln("Fail: toLowerCase for character " +
                              Utility.escape(chstr[0]) + ", expected "
                              + Utility.escape(chstr[1]) + " but resulted in " +
                              Utility.escape(UCharacter.toLowerCase(chstr[0])));
                    }
                    if (!UCharacter.toUpperCase(chstr[0]).equals(chstr[3])) {
                        errln(s);
                        errln("Fail: toUpperCase for character " +
                              Utility.escape(chstr[0]) + ", expected "
                              + Utility.escape(chstr[3]) + " but resulted in " +
                              Utility.escape(UCharacter.toUpperCase(chstr[0])));
                    }
                }
                else {
                    if (!UCharacter.toLowerCase(locale, chstr[0]).equals(
                                                                   chstr[1])) {
                        errln(s);
                        errln("Fail: toLowerCase for character " +
                              Utility.escape(chstr[0]) + ", expected "
                              + Utility.escape(chstr[1]) + " but resulted in " +
                              Utility.escape(UCharacter.toLowerCase(locale,
                                                                    chstr[0])));
                    }
                    if (!UCharacter.toUpperCase(locale, chstr[0]).equals(
                                                                   chstr[3])) {
                        errln(s);
                        errln("Fail: toUpperCase for character " +
                              Utility.escape(chstr[0]) + ", expected "
                              + Utility.escape(chstr[3]) + " but resulted in " +
                              Utility.escape(UCharacter.toUpperCase(locale,
                                                                    chstr[0])));
                    }
                }
            }
            input.close();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
    }

    @Test
    public void TestUpperLower()
    {
        int upper[] = {0x0041, 0x0042, 0x00b2, 0x01c4, 0x01c6, 0x01c9, 0x01c8,
                        0x01c9, 0x000c};
        int lower[] = {0x0061, 0x0062, 0x00b2, 0x01c6, 0x01c6, 0x01c9, 0x01c9,
                        0x01c9, 0x000c};
        String upperTest = "abcdefg123hij.?:klmno";
        String lowerTest = "ABCDEFG123HIJ.?:KLMNO";

        // Checks LetterLike Symbols which were previously a source of
        // confusion [Bertrand A. D. 02/04/98]
        for (int i = 0x2100; i < 0x2138; i ++) {
            /* Unicode 5.0 adds lowercase U+214E (TURNED SMALL F) to U+2132 (TURNED CAPITAL F) */
            if (i != 0x2126 && i != 0x212a && i != 0x212b && i!=0x2132) {
                if (i != UCharacter.toLowerCase(i)) { // itself
                    errln("Failed case conversion with itself: \\u"
                            + Utility.hex(i, 4));
                }
                if (i != UCharacter.toUpperCase(i)) {
                    errln("Failed case conversion with itself: \\u"
                            + Utility.hex(i, 4));
                }
            }
        }
        for (int i = 0; i < upper.length; i ++) {
            if (UCharacter.toLowerCase(upper[i]) != lower[i]) {
                errln("FAILED UCharacter.tolower() for \\u"
                        + Utility.hex(upper[i], 4)
                        + " Expected \\u" + Utility.hex(lower[i], 4)
                        + " Got \\u"
                        + Utility.hex(UCharacter.toLowerCase(upper[i]), 4));
            }
        }
        logln("testing upper lower");
        for (int i = 0; i < upperTest.length(); i ++) {
            logln("testing to upper to lower");
            if (UCharacter.isLetter(upperTest.charAt(i)) &&
                !UCharacter.isLowerCase(upperTest.charAt(i))) {
                errln("Failed isLowerCase test at \\u"
                        + Utility.hex(upperTest.charAt(i), 4));
            }
            else if (UCharacter.isLetter(lowerTest.charAt(i))
                     && !UCharacter.isUpperCase(lowerTest.charAt(i))) {
                errln("Failed isUpperCase test at \\u"
                      + Utility.hex(lowerTest.charAt(i), 4));
            }
            else if (upperTest.charAt(i)
                            != UCharacter.toLowerCase(lowerTest.charAt(i))) {
                errln("Failed case conversion from \\u"
                        + Utility.hex(lowerTest.charAt(i), 4) + " To \\u"
                        + Utility.hex(upperTest.charAt(i), 4));
            }
            else if (lowerTest.charAt(i)
                    != UCharacter.toUpperCase(upperTest.charAt(i))) {
                errln("Failed case conversion : \\u"
                        + Utility.hex(upperTest.charAt(i), 4) + " To \\u"
                        + Utility.hex(lowerTest.charAt(i), 4));
            }
            else if (upperTest.charAt(i)
                    != UCharacter.toLowerCase(upperTest.charAt(i))) {
                errln("Failed case conversion with itself: \\u"
                        + Utility.hex(upperTest.charAt(i)));
            }
            else if (lowerTest.charAt(i)
                    != UCharacter.toUpperCase(lowerTest.charAt(i))) {
                errln("Failed case conversion with itself: \\u"
                        + Utility.hex(lowerTest.charAt(i)));
            }
        }
        logln("done testing upper Lower");
    }

    private void assertGreekUpper(String s, String expected) {
        assertEquals("toUpper/Greek(" + s + ')', expected, UCharacter.toUpperCase(GREEK_LOCALE_, s));
    }

    @Test
    public void TestGreekUpper() {
        // https://unicode-org.atlassian.net/browse/ICU-5456
        assertGreekUpper("άδικος, κείμενο, ίριδα", "ΑΔΙΚΟΣ, ΚΕΙΜΕΝΟ, ΙΡΙΔΑ");
        // https://bugzilla.mozilla.org/show_bug.cgi?id=307039
        // https://bug307039.bmoattachments.org/attachment.cgi?id=194893
        assertGreekUpper("Πατάτα", "ΠΑΤΑΤΑ");
        assertGreekUpper("Αέρας, Μυστήριο, Ωραίο", "ΑΕΡΑΣ, ΜΥΣΤΗΡΙΟ, ΩΡΑΙΟ");
        assertGreekUpper("Μαΐου, Πόρος, Ρύθμιση", "ΜΑΪΟΥ, ΠΟΡΟΣ, ΡΥΘΜΙΣΗ");
        assertGreekUpper("ΰ, Τηρώ, Μάιος", "Ϋ, ΤΗΡΩ, ΜΑΪΟΣ");
        assertGreekUpper("άυλος", "ΑΫΛΟΣ");
        assertGreekUpper("ΑΫΛΟΣ", "ΑΫΛΟΣ");
        assertGreekUpper("Άκλιτα ρήματα ή άκλιτες μετοχές", "ΑΚΛΙΤΑ ΡΗΜΑΤΑ Ή ΑΚΛΙΤΕΣ ΜΕΤΟΧΕΣ");
        // http://www.unicode.org/udhr/d/udhr_ell_monotonic.html
        assertGreekUpper("Επειδή η αναγνώριση της αξιοπρέπειας", "ΕΠΕΙΔΗ Η ΑΝΑΓΝΩΡΙΣΗ ΤΗΣ ΑΞΙΟΠΡΕΠΕΙΑΣ");
        assertGreekUpper("νομικού ή διεθνούς", "ΝΟΜΙΚΟΥ Ή ΔΙΕΘΝΟΥΣ");
        // http://unicode.org/udhr/d/udhr_ell_polytonic.html
        assertGreekUpper("Ἐπειδὴ ἡ ἀναγνώριση", "ΕΠΕΙΔΗ Η ΑΝΑΓΝΩΡΙΣΗ");
        assertGreekUpper("νομικοῦ ἢ διεθνοῦς", "ΝΟΜΙΚΟΥ Ή ΔΙΕΘΝΟΥΣ");
        // From Google bug report
        assertGreekUpper("Νέο, Δημιουργία", "ΝΕΟ, ΔΗΜΙΟΥΡΓΙΑ");
        // http://crbug.com/234797
        assertGreekUpper("Ελάτε να φάτε τα καλύτερα παϊδάκια!", "ΕΛΑΤΕ ΝΑ ΦΑΤΕ ΤΑ ΚΑΛΥΤΕΡΑ ΠΑΪΔΑΚΙΑ!");
        assertGreekUpper("Μαΐου, τρόλεϊ", "ΜΑΪΟΥ, ΤΡΟΛΕΪ");
        assertGreekUpper("Το ένα ή το άλλο.", "ΤΟ ΕΝΑ Ή ΤΟ ΑΛΛΟ.");
        // http://multilingualtypesetting.co.uk/blog/greek-typesetting-tips/
        assertGreekUpper("ρωμέικα", "ΡΩΜΕΪΚΑ");
        assertGreekUpper("ή.", "Ή.");
    }

    @Test
    public void TestArmenian() {
        Locale hy = new Locale("hy");  // Eastern Armenian
        Locale hyw = new Locale("hyw");  // Western Armenian
        Locale root = Locale.ROOT;
        // See ICU-13416:
        // և ligature ech-yiwn
        // uppercases to ԵՒ=ech+yiwn by default and in Western Armenian,
        // but to ԵՎ=ech+vew in Eastern Armenian.
        String s = "և Երևանի";

        assertEquals("upper root", "ԵՒ ԵՐԵՒԱՆԻ", CaseMap.toUpper().apply(root, s));
        assertEquals("upper hy", "ԵՎ ԵՐԵՎԱՆԻ", CaseMap.toUpper().apply(hy, s));
        assertEquals("upper hyw", "ԵՒ ԵՐԵՒԱՆԻ", CaseMap.toUpper().apply(hyw, s));

        assertEquals("title root", "Եւ Երևանի", CaseMap.toTitle().apply(root, null, s));
        assertEquals("title hy", "Եվ Երևանի", CaseMap.toTitle().apply(hy, null, s));
        assertEquals("title hyw", "Եւ Երևանի", CaseMap.toTitle().apply(hyw, null, s));
    }

    private static final class EditChange {
        private boolean change;
        private int oldLength, newLength;
        EditChange(boolean change, int oldLength, int newLength) {
            this.change = change;
            this.oldLength = oldLength;
            this.newLength = newLength;
        }
    }

    private static String printOneEdit(Edits.Iterator ei) {
        if (ei.hasChange()) {
            return "" + ei.oldLength() + "->" + ei.newLength();
        } else {
            return "" + ei.oldLength() + "=" + ei.newLength();
        }
    }

    /**
     * Maps indexes according to the expected edits.
     * A destination index can occur multiple times when there are source deletions.
     * Map according to the last occurrence, normally in a non-empty destination span.
     * Simplest is to search from the back.
     */
    private static int srcIndexFromDest(
            EditChange expected[], int srcLength, int destLength, int index) {
        int srcIndex = srcLength;
        int destIndex = destLength;
        int i = expected.length;
        while (index < destIndex && i > 0) {
            --i;
            int prevSrcIndex = srcIndex - expected[i].oldLength;
            int prevDestIndex = destIndex - expected[i].newLength;
            if (index == prevDestIndex) {
                return prevSrcIndex;
            } else if (index > prevDestIndex) {
                if (expected[i].change) {
                    // In a change span, map to its end.
                    return srcIndex;
                } else {
                    // In an unchanged span, offset within it.
                    return prevSrcIndex + (index - prevDestIndex);
                }
            }
            srcIndex = prevSrcIndex;
            destIndex = prevDestIndex;
        }
        // index is outside the string.
        return srcIndex;
    }

    private static int destIndexFromSrc(
            EditChange expected[], int srcLength, int destLength, int index) {
        int srcIndex = srcLength;
        int destIndex = destLength;
        int i = expected.length;
        while (index < srcIndex && i > 0) {
            --i;
            int prevSrcIndex = srcIndex - expected[i].oldLength;
            int prevDestIndex = destIndex - expected[i].newLength;
            if (index == prevSrcIndex) {
                return prevDestIndex;
            } else if (index > prevSrcIndex) {
                if (expected[i].change) {
                    // In a change span, map to its end.
                    return destIndex;
                } else {
                    // In an unchanged span, offset within it.
                    return prevDestIndex + (index - prevSrcIndex);
                }
            }
            srcIndex = prevSrcIndex;
            destIndex = prevDestIndex;
        }
        // index is outside the string.
        return destIndex;
    }

    private void checkEqualEdits(String name, Edits e1, Edits e2) {
        Edits.Iterator ei1 = e1.getFineIterator();
        Edits.Iterator ei2 = e2.getFineIterator();
        for (int i = 0;; ++i) {
            boolean ei1HasNext = ei1.next();
            boolean ei2HasNext = ei2.next();
            assertEquals(name + " next()[" + i + "]", ei1HasNext, ei2HasNext);
            assertEquals(name + " edit[" + i + "]", printOneEdit(ei1), printOneEdit(ei2));
            if (!ei1HasNext || !ei2HasNext) {
                break;
            }
        }
    }

    private static void checkEditsIter(
            String name, Edits.Iterator ei1, Edits.Iterator ei2,  // two equal iterators
            EditChange[] expected, boolean withUnchanged) {
        assertFalse(name, ei2.findSourceIndex(-1));
        assertFalse(name, ei2.findDestinationIndex(-1));

        int expSrcIndex = 0;
        int expDestIndex = 0;
        int expReplIndex = 0;
        for (int expIndex = 0; expIndex < expected.length; ++expIndex) {
            EditChange expect = expected[expIndex];
            String msg = name + ' ' + expIndex;
            if (withUnchanged || expect.change) {
                assertTrue(msg, ei1.next());
                assertEquals(msg, expect.change, ei1.hasChange());
                assertEquals(msg, expect.oldLength, ei1.oldLength());
                assertEquals(msg, expect.newLength, ei1.newLength());
                assertEquals(msg, expSrcIndex, ei1.sourceIndex());
                assertEquals(msg, expDestIndex, ei1.destinationIndex());
                assertEquals(msg, expReplIndex, ei1.replacementIndex());
            }

            if (expect.oldLength > 0) {
                assertTrue(msg, ei2.findSourceIndex(expSrcIndex));
                assertEquals(msg, expect.change, ei2.hasChange());
                assertEquals(msg, expect.oldLength, ei2.oldLength());
                assertEquals(msg, expect.newLength, ei2.newLength());
                assertEquals(msg, expSrcIndex, ei2.sourceIndex());
                assertEquals(msg, expDestIndex, ei2.destinationIndex());
                assertEquals(msg, expReplIndex, ei2.replacementIndex());
                if (!withUnchanged) {
                    // For some iterators, move past the current range
                    // so that findSourceIndex() has to look before the current index.
                    ei2.next();
                    ei2.next();
                }
            }

            if (expect.newLength > 0) {
                assertTrue(msg, ei2.findDestinationIndex(expDestIndex));
                assertEquals(msg, expect.change, ei2.hasChange());
                assertEquals(msg, expect.oldLength, ei2.oldLength());
                assertEquals(msg, expect.newLength, ei2.newLength());
                assertEquals(msg, expSrcIndex, ei2.sourceIndex());
                assertEquals(msg, expDestIndex, ei2.destinationIndex());
                assertEquals(msg, expReplIndex, ei2.replacementIndex());
                if (!withUnchanged) {
                    // For some iterators, move past the current range
                    // so that findSourceIndex() has to look before the current index.
                    ei2.next();
                    ei2.next();
                }
            }

            expSrcIndex += expect.oldLength;
            expDestIndex += expect.newLength;
            if (expect.change) {
                expReplIndex += expect.newLength;
            }
        }
        String msg = name + " end";
        assertFalse(msg, ei1.next());
        assertFalse(msg, ei1.hasChange());
        assertEquals(msg, 0, ei1.oldLength());
        assertEquals(msg, 0, ei1.newLength());
        assertEquals(msg, expSrcIndex, ei1.sourceIndex());
        assertEquals(msg, expDestIndex, ei1.destinationIndex());
        assertEquals(msg, expReplIndex, ei1.replacementIndex());

        assertFalse(name, ei2.findSourceIndex(expSrcIndex));
        assertFalse(name, ei2.findDestinationIndex(expDestIndex));

        // Check mapping of all indexes against a simple implementation
        // that works on the expected changes.
        // Iterate once forward, once backward, to cover more runtime conditions.
        int srcLength = expSrcIndex;
        int destLength = expDestIndex;
        List<Integer> srcIndexes = new ArrayList<>();
        List<Integer> destIndexes = new ArrayList<>();
        srcIndexes.add(-1);
        destIndexes.add(-1);
        int srcIndex = 0;
        int destIndex = 0;
        for (int i = 0; i < expected.length; ++i) {
            if (expected[i].oldLength > 0) {
                srcIndexes.add(srcIndex);
                if (expected[i].oldLength > 1) {
                    srcIndexes.add(srcIndex + 1);
                    if (expected[i].oldLength > 2) {
                        srcIndexes.add(srcIndex + expected[i].oldLength - 1);
                    }
                }
            }
            if (expected[i].newLength > 0) {
                destIndexes.add(destIndex);
                if (expected[i].newLength > 1) {
                    destIndexes.add(destIndex + 1);
                    if (expected[i].newLength > 2) {
                        destIndexes.add(destIndex + expected[i].newLength - 1);
                    }
                }
            }
            srcIndex += expected[i].oldLength;
            destIndex += expected[i].newLength;
        }
        srcIndexes.add(srcLength);
        destIndexes.add(destLength);
        srcIndexes.add(srcLength + 1);
        destIndexes.add(destLength + 1);
        Collections.reverse(destIndexes);
        // Zig-zag across the indexes to stress next() <-> previous().
        for (int i = 0; i < srcIndexes.size(); ++i) {
            for (int j : ZIG_ZAG) {
                if ((i + j) < srcIndexes.size()) {
                    int si = srcIndexes.get(i + j);
                    assertEquals(name + " destIndexFromSrc(" + si + "):",
                            destIndexFromSrc(expected, srcLength, destLength, si),
                            ei2.destinationIndexFromSourceIndex(si));
                }
            }
        }
        for (int i = 0; i < destIndexes.size(); ++i) {
            for (int j : ZIG_ZAG) {
                if ((i + j) < destIndexes.size()) {
                    int di = destIndexes.get(i + j);
                    assertEquals(name + " srcIndexFromDest(" + di + "):",
                            srcIndexFromDest(expected, srcLength, destLength, di),
                            ei2.sourceIndexFromDestinationIndex(di));
                }
            }
        }
    }

    private static final int[] ZIG_ZAG = { 0, 1, 2, 3, 2, 1 };

    @Test
    public void TestEdits() {
        Edits edits = new Edits();
        assertFalse("new Edits hasChanges", edits.hasChanges());
        assertEquals("new Edits numberOfChanges", 0, edits.numberOfChanges());
        assertEquals("new Edits", 0, edits.lengthDelta());
        edits.addUnchanged(1);  // multiple unchanged ranges are combined
        edits.addUnchanged(10000);  // too long, and they are split
        edits.addReplace(0, 0);
        edits.addUnchanged(2);
        assertFalse("unchanged 10003 hasChanges", edits.hasChanges());
        assertEquals("unchanged 10003 numberOfChanges", 0, edits.numberOfChanges());
        assertEquals("unchanged 10003", 0, edits.lengthDelta());
        edits.addReplace(2, 1);  // multiple short equal-lengths edits are compressed
        edits.addUnchanged(0);
        edits.addReplace(2, 1);
        edits.addReplace(2, 1);
        edits.addReplace(0, 10);
        edits.addReplace(100, 0);
        edits.addReplace(3000, 4000);  // variable-length encoding
        edits.addReplace(100000, 100000);
        assertTrue("some edits hasChanges", edits.hasChanges());
        assertEquals("some edits numberOfChanges", 7, edits.numberOfChanges());
        assertEquals("some edits", -3 + 10 - 100 + 1000, edits.lengthDelta());

        EditChange[] coarseExpectedChanges = new EditChange[] {
                new EditChange(false, 10003, 10003),
                new EditChange(true, 103106, 104013)
        };
        checkEditsIter("coarse",
                edits.getCoarseIterator(), edits.getCoarseIterator(),
                coarseExpectedChanges, true);
        checkEditsIter("coarse changes",
                edits.getCoarseChangesIterator(), edits.getCoarseChangesIterator(),
                coarseExpectedChanges, false);

        EditChange[] fineExpectedChanges = new EditChange[] {
                new EditChange(false, 10003, 10003),
                new EditChange(true, 2, 1),
                new EditChange(true, 2, 1),
                new EditChange(true, 2, 1),
                new EditChange(true, 0, 10),
                new EditChange(true, 100, 0),
                new EditChange(true, 3000, 4000),
                new EditChange(true, 100000, 100000)
        };
        checkEditsIter("fine",
                edits.getFineIterator(), edits.getFineIterator(),
                fineExpectedChanges, true);
        checkEditsIter("fine changes",
                edits.getFineChangesIterator(), edits.getFineChangesIterator(),
                fineExpectedChanges, false);

        edits.reset();
        assertFalse("reset hasChanges", edits.hasChanges());
        assertEquals("reset numberOfChanges", 0, edits.numberOfChanges());
        assertEquals("reset", 0, edits.lengthDelta());
        Edits.Iterator ei = edits.getCoarseChangesIterator();
        assertFalse("reset then iterator", ei.next());
    }

    @Test
    public void TestEditsFindFwdBwd() {
        // Some users need index mappings to be efficient when they are out of order.
        // The most interesting failure case for this test is it taking a very long time.
        Edits e = new Edits();
        int N = 200000;
        for (int i = 0; i < N; ++i) {
            e.addUnchanged(1);
            e.addReplace(3, 1);
        }
        Edits.Iterator iter = e.getFineIterator();
        for (int i = 0; i <= N; i += 2) {
            assertEquals("ascending", i * 2, iter.sourceIndexFromDestinationIndex(i));
            assertEquals("ascending", i * 2 + 1, iter.sourceIndexFromDestinationIndex(i + 1));
        }
        for (int i = N; i >= 0; i -= 2) {
            assertEquals("descending", i * 2 + 1, iter.sourceIndexFromDestinationIndex(i + 1));
            assertEquals("descending", i * 2, iter.sourceIndexFromDestinationIndex(i));
        }
    }

    @Test
    public void TestMergeEdits() {
        Edits ab = new Edits(), bc = new Edits(), ac = new Edits(), expected_ac = new Edits();

        // Simple: Two parallel non-changes.
        ab.addUnchanged(2);
        bc.addUnchanged(2);
        expected_ac.addUnchanged(2);

        // Simple: Two aligned changes.
        ab.addReplace(3, 2);
        bc.addReplace(2, 1);
        expected_ac.addReplace(3, 1);

        // Unequal non-changes.
        ab.addUnchanged(5);
        bc.addUnchanged(3);
        expected_ac.addUnchanged(3);
        // ab ahead by 2

        // Overlapping changes accumulate until they share a boundary.
        ab.addReplace(4, 3);
        bc.addReplace(3, 2);
        ab.addReplace(4, 3);
        bc.addReplace(3, 2);
        ab.addReplace(4, 3);
        bc.addReplace(3, 2);
        bc.addUnchanged(4);
        expected_ac.addReplace(14, 8);
        // bc ahead by 2

        // Balance out intermediate-string lengths.
        ab.addUnchanged(2);
        expected_ac.addUnchanged(2);

        // Insert something and delete it: Should disappear.
        ab.addReplace(0, 5);
        ab.addReplace(0, 2);
        bc.addReplace(7, 0);

        // Parallel change to make a new boundary.
        ab.addReplace(1, 2);
        bc.addReplace(2, 3);
        expected_ac.addReplace(1, 3);

        // Multiple ab deletions should remain separate at the boundary.
        ab.addReplace(1, 0);
        ab.addReplace(2, 0);
        ab.addReplace(3, 0);
        expected_ac.addReplace(1, 0);
        expected_ac.addReplace(2, 0);
        expected_ac.addReplace(3, 0);

        // Unequal non-changes can be split for another boundary.
        ab.addUnchanged(2);
        bc.addUnchanged(1);
        expected_ac.addUnchanged(1);
        // ab ahead by 1

        // Multiple bc insertions should create a boundary and remain separate.
        bc.addReplace(0, 4);
        bc.addReplace(0, 5);
        bc.addReplace(0, 6);
        expected_ac.addReplace(0, 4);
        expected_ac.addReplace(0, 5);
        expected_ac.addReplace(0, 6);
        // ab ahead by 1

        // Multiple ab deletions in the middle of a bc change are merged.
        bc.addReplace(2, 2);
        // bc ahead by 1
        ab.addReplace(1, 0);
        ab.addReplace(2, 0);
        ab.addReplace(3, 0);
        ab.addReplace(4, 1);
        expected_ac.addReplace(11, 2);

        // Multiple bc insertions in the middle of an ab change are merged.
        ab.addReplace(5, 6);
        bc.addReplace(3, 3);
        // ab ahead by 3
        bc.addReplace(0, 4);
        bc.addReplace(0, 5);
        bc.addReplace(0, 6);
        bc.addReplace(3, 7);
        expected_ac.addReplace(5, 25);

        // Delete around a deletion.
        ab.addReplace(4, 4);
        ab.addReplace(3, 0);
        ab.addUnchanged(2);
        bc.addReplace(2, 2);
        bc.addReplace(4, 0);
        expected_ac.addReplace(9, 2);

        // Insert into an insertion.
        ab.addReplace(0, 2);
        bc.addReplace(1, 1);
        bc.addReplace(0, 8);
        bc.addUnchanged(4);
        expected_ac.addReplace(0, 10);
        // bc ahead by 3

        // Balance out intermediate-string lengths.
        ab.addUnchanged(3);
        expected_ac.addUnchanged(3);

        // Deletions meet insertions.
        // Output order is arbitrary in principle, but we expect insertions first
        // and want to keep it that way.
        ab.addReplace(2, 0);
        ab.addReplace(4, 0);
        ab.addReplace(6, 0);
        bc.addReplace(0, 1);
        bc.addReplace(0, 3);
        bc.addReplace(0, 5);
        expected_ac.addReplace(0, 1);
        expected_ac.addReplace(0, 3);
        expected_ac.addReplace(0, 5);
        expected_ac.addReplace(2, 0);
        expected_ac.addReplace(4, 0);
        expected_ac.addReplace(6, 0);

        // End with a non-change, so that further edits are never reordered.
        ab.addUnchanged(1);
        bc.addUnchanged(1);
        expected_ac.addUnchanged(1);

        ac.mergeAndAppend(ab, bc);
        checkEqualEdits("ab+bc", expected_ac, ac);

        // Append more Edits.
        Edits ab2 = new Edits(), bc2 = new Edits();
        ab2.addUnchanged(5);
        bc2.addReplace(1, 2);
        bc2.addUnchanged(4);
        expected_ac.addReplace(1, 2);
        expected_ac.addUnchanged(4);
        ac.mergeAndAppend(ab2, bc2);
        checkEqualEdits("ab2+bc2", expected_ac, ac);

        // Append empty edits.
        Edits empty = new Edits();
        ac.mergeAndAppend(empty, empty);
        checkEqualEdits("empty+empty", expected_ac, ac);

        // Error: Append more edits with mismatched intermediate-string lengths.
        Edits mismatch = new Edits();
        mismatch.addReplace(1, 1);
        try {
            ac.mergeAndAppend(ab2, mismatch);
            fail("ab2+mismatch did not yield IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ac.mergeAndAppend(mismatch, bc2);
            fail("mismatch+bc2 did not yield IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void TestCaseMapWithEdits() {
        StringBuilder sb = new StringBuilder();
        Edits edits = new Edits();

        sb = CaseMap.toLower().omitUnchangedText().apply(TURKISH_LOCALE_, "IstanBul", sb, edits);
        assertEquals("toLower(Istanbul)", "ıb", sb.toString());
        EditChange[] lowerExpectedChanges = new EditChange[] {
                new EditChange(true, 1, 1),
                new EditChange(false, 4, 4),
                new EditChange(true, 1, 1),
                new EditChange(false, 2, 2)
        };
        checkEditsIter("toLower(Istanbul)",
                edits.getFineIterator(), edits.getFineIterator(),
                lowerExpectedChanges, true);

        sb.delete(0, sb.length());
        edits.reset();
        sb = CaseMap.toUpper().omitUnchangedText().apply(GREEK_LOCALE_, "Πατάτα", sb, edits);
        assertEquals("toUpper(Πατάτα)", "ΑΤΑΤΑ", sb.toString());
        EditChange[] upperExpectedChanges = new EditChange[] {
                new EditChange(false, 1, 1),
                new EditChange(true, 1, 1),
                new EditChange(true, 1, 1),
                new EditChange(true, 1, 1),
                new EditChange(true, 1, 1),
                new EditChange(true, 1, 1)
        };
        checkEditsIter("toUpper(Πατάτα)",
                edits.getFineIterator(), edits.getFineIterator(),
                upperExpectedChanges, true);

        sb.delete(0, sb.length());
        edits.reset();
        sb = CaseMap.toTitle().omitUnchangedText().noBreakAdjustment().noLowercase().apply(
                DUTCH_LOCALE_, null, "IjssEL IglOo", sb, edits);
        assertEquals("toTitle(IjssEL IglOo)", "J", sb.toString());
        EditChange[] titleExpectedChanges = new EditChange[] {
                new EditChange(false, 1, 1),
                new EditChange(true, 1, 1),
                new EditChange(false, 10, 10)
        };
        checkEditsIter("toTitle(IjssEL IglOo)",
                edits.getFineIterator(), edits.getFineIterator(),
                titleExpectedChanges, true);

        sb.delete(0, sb.length());
        edits.reset();
        sb = CaseMap.fold().omitUnchangedText().turkic().apply("IßtanBul", sb, edits);
        assertEquals("fold(IßtanBul)", "ıssb", sb.toString());
        EditChange[] foldExpectedChanges = new EditChange[] {
                new EditChange(true, 1, 1),
                new EditChange(true, 1, 2),
                new EditChange(false, 3, 3),
                new EditChange(true, 1, 1),
                new EditChange(false, 2, 2)
        };
        checkEditsIter("fold(IßtanBul)",
                edits.getFineIterator(), edits.getFineIterator(),
                foldExpectedChanges, true);
    }

    @Test
    public void TestCaseMapToString() {
        // String apply(..., CharSequence)
        // Omit unchanged text.
        assertEquals("toLower(Istanbul)", "ıb",
                CaseMap.toLower().omitUnchangedText().apply(TURKISH_LOCALE_, "IstanBul"));
        assertEquals("toUpper(Πατάτα)", "ΑΤΑΤΑ",
                CaseMap.toUpper().omitUnchangedText().apply(GREEK_LOCALE_, "Πατάτα"));
        assertEquals("toTitle(IjssEL IglOo)", "J",
                CaseMap.toTitle().omitUnchangedText().noBreakAdjustment().noLowercase().apply(
                        DUTCH_LOCALE_, null, "IjssEL IglOo"));
        assertEquals("fold(IßtanBul)", "ıssb",
                CaseMap.fold().omitUnchangedText().turkic().apply("IßtanBul"));

        // Return the whole result string.
        assertEquals("toLower(Istanbul)", "ıstanbul",
                CaseMap.toLower().apply(TURKISH_LOCALE_, "IstanBul"));
        assertEquals("toUpper(Πατάτα)", "ΠΑΤΑΤΑ",
                CaseMap.toUpper().apply(GREEK_LOCALE_, "Πατάτα"));
        assertEquals("toTitle(IjssEL IglOo)", "IJssEL IglOo",
                CaseMap.toTitle().noBreakAdjustment().noLowercase().apply(
                        DUTCH_LOCALE_, null, "IjssEL IglOo"));
        assertEquals("fold(IßtanBul)", "ısstanbul",
                CaseMap.fold().turkic().apply("IßtanBul"));
    }

    @Test
    public void TestCaseMapEditsIteratorDocs() {
        String input = "abcßDeF";
        // output: "abcssdef"

        StringBuilder sb = new StringBuilder();
        Edits edits = new Edits();
        CaseMap.fold().apply(input, sb, edits);

        String[] fineIteratorExpected = {
                "{ src[0..3] ≡ dest[0..3] (no-change) }",
                "{ src[3..4] ⇝ dest[3..5], repl[0..2] }",
                "{ src[4..5] ⇝ dest[5..6], repl[2..3] }",
                "{ src[5..6] ≡ dest[6..7] (no-change) }",
                "{ src[6..7] ⇝ dest[7..8], repl[3..4] }",
        };
        String[] fineChangesIteratorExpected = {
                "{ src[3..4] ⇝ dest[3..5], repl[0..2] }",
                "{ src[4..5] ⇝ dest[5..6], repl[2..3] }",
                "{ src[6..7] ⇝ dest[7..8], repl[3..4] }",
        };
        String[] coarseIteratorExpected = {
                "{ src[0..3] ≡ dest[0..3] (no-change) }",
                "{ src[3..5] ⇝ dest[3..6], repl[0..3] }",
                "{ src[5..6] ≡ dest[6..7] (no-change) }",
                "{ src[6..7] ⇝ dest[7..8], repl[3..4] }",
        };
        String[] coarseChangesIteratorExpected = {
                "{ src[3..5] ⇝ dest[3..6], repl[0..3] }",
                "{ src[6..7] ⇝ dest[7..8], repl[3..4] }",
        };

        // Expected destination indices when source index is queried
        int[] expectedDestFineEditIndices = {0, 0, 0, 3, 5, 6, 7};
        int[] expectedDestCoarseEditIndices = {0, 0, 0, 3, 3, 6, 7};
        int[] expectedDestFineStringIndices = {0, 1, 2, 3, 5, 6, 7};
        int[] expectedDestCoarseStringIndices = {0, 1, 2, 3, 6, 6, 7};

        // Expected source indices when destination index is queried
        int[] expectedSrcFineEditIndices = { 0, 0, 0, 3, 3, 4, 5, 6 };
        int[] expectedSrcCoarseEditIndices = { 0, 0, 0, 3, 3, 3, 5, 6 };
        int[] expectedSrcFineStringIndices = { 0, 1, 2, 3, 4, 4, 5, 6 };
        int[] expectedSrcCoarseStringIndices = { 0, 1, 2, 3, 5, 5, 5, 6 };

        // Demonstrate the iterator next() method:
        Edits.Iterator fineIterator = edits.getFineIterator();
        int i = 0;
        while (fineIterator.next()) {
            String expected = fineIteratorExpected[i++];
            String actual = fineIterator.toString();
            assertEquals("Iteration #" + i, expected, actual.substring(actual.length() - expected.length()));
        }
        Edits.Iterator fineChangesIterator = edits.getFineChangesIterator();
        i = 0;
        while (fineChangesIterator.next()) {
            String expected = fineChangesIteratorExpected[i++];
            String actual = fineChangesIterator.toString();
            assertEquals("Iteration #" + i, expected, actual.substring(actual.length() - expected.length()));
        }
        Edits.Iterator coarseIterator = edits.getCoarseIterator();
        i = 0;
        while (coarseIterator.next()) {
            String expected = coarseIteratorExpected[i++];
            String actual = coarseIterator.toString();
            assertEquals("Iteration #" + i, expected, actual.substring(actual.length() - expected.length()));
        }
        Edits.Iterator coarseChangesIterator = edits.getCoarseChangesIterator();
        i = 0;
        while (coarseChangesIterator.next()) {
            String expected = coarseChangesIteratorExpected[i++];
            String actual = coarseChangesIterator.toString();
            assertEquals("Iteration #" + i, expected, actual.substring(actual.length() - expected.length()));
        }

        // Demonstrate the iterator indexing methods:
        // fineIterator should have the same behavior as fineChangesIterator, and
        // coarseIterator should have the same behavior as coarseChangesIterator.
        for (int srcIndex=0; srcIndex<input.length(); srcIndex++) {
            fineIterator.findSourceIndex(srcIndex);
            fineChangesIterator.findSourceIndex(srcIndex);
            coarseIterator.findSourceIndex(srcIndex);
            coarseChangesIterator.findSourceIndex(srcIndex);

            assertEquals("Source index: " + srcIndex,
                    expectedDestFineEditIndices[srcIndex],
                    fineIterator.destinationIndex());
            assertEquals("Source index: " + srcIndex,
                    expectedDestFineEditIndices[srcIndex],
                    fineChangesIterator.destinationIndex());
            assertEquals("Source index: " + srcIndex,
                    expectedDestCoarseEditIndices[srcIndex],
                    coarseIterator.destinationIndex());
            assertEquals("Source index: " + srcIndex,
                    expectedDestCoarseEditIndices[srcIndex],
                    coarseChangesIterator.destinationIndex());

            assertEquals("Source index: " + srcIndex,
                    expectedDestFineStringIndices[srcIndex],
                    fineIterator.destinationIndexFromSourceIndex(srcIndex));
            assertEquals("Source index: " + srcIndex,
                    expectedDestFineStringIndices[srcIndex],
                    fineChangesIterator.destinationIndexFromSourceIndex(srcIndex));
            assertEquals("Source index: " + srcIndex,
                    expectedDestCoarseStringIndices[srcIndex],
                    coarseIterator.destinationIndexFromSourceIndex(srcIndex));
            assertEquals("Source index: " + srcIndex,
                    expectedDestCoarseStringIndices[srcIndex],
                    coarseChangesIterator.destinationIndexFromSourceIndex(srcIndex));
        }
        for (int destIndex=0; destIndex<input.length(); destIndex++) {
            fineIterator.findDestinationIndex(destIndex);
            fineChangesIterator.findDestinationIndex(destIndex);
            coarseIterator.findDestinationIndex(destIndex);
            coarseChangesIterator.findDestinationIndex(destIndex);

            assertEquals("Destination index: " + destIndex,
                    expectedSrcFineEditIndices[destIndex],
                    fineIterator.sourceIndex());
            assertEquals("Destination index: " + destIndex,
                    expectedSrcFineEditIndices[destIndex],
                    fineChangesIterator.sourceIndex());
            assertEquals("Destination index: " + destIndex,
                    expectedSrcCoarseEditIndices[destIndex],
                    coarseIterator.sourceIndex());
            assertEquals("Destination index: " + destIndex,
                    expectedSrcCoarseEditIndices[destIndex],
                    coarseChangesIterator.sourceIndex());

            assertEquals("Destination index: " + destIndex,
                    expectedSrcFineStringIndices[destIndex],
                    fineIterator.sourceIndexFromDestinationIndex(destIndex));
            assertEquals("Destination index: " + destIndex,
                    expectedSrcFineStringIndices[destIndex],
                    fineChangesIterator.sourceIndexFromDestinationIndex(destIndex));
            assertEquals("Destination index: " + destIndex,
                    expectedSrcCoarseStringIndices[destIndex],
                    coarseIterator.sourceIndexFromDestinationIndex(destIndex));
            assertEquals("Destination index: " + destIndex,
                    expectedSrcCoarseStringIndices[destIndex],
                    coarseChangesIterator.sourceIndexFromDestinationIndex(destIndex));
        }
    }

    @Test
    public void TestCaseMapGreekExtended() {
        // Ticket 13851
        String s = "\u1F80\u1F88\u1FFC";
        String result = CaseMap.toLower().apply(Locale.ROOT,  s);
        assertEquals("lower", "\u1F80\u1F80\u1FF3", result);
        result = CaseMap.toTitle().apply(Locale.ROOT, null, s);
        assertEquals("title", "\u1F88\u1F80\u1FF3", result);
    }

    @Test
    public void TestFoldBug20316() {
        String s = "廬ᾒ뻪ᣃइ垚Ⴡₓ렞체ꖲ갹ݖ䕷꾬쯎㊅ᦘᰄ㸜䡏遁럢豑黾奯㸀⊻줮끎蒹衤劔뽳趧熶撒쫃窩겨ཇ脌쵐嫑⟑겭㋋濜隣ᳰ봢ℼ櫩靛㉃炔鋳" +
                "оे⳨ᦧྃ깢粣ᑤꇪ찃̹鵄ዤꛛᰙ⡝捣쯋톐蕩栭쥀뎊ᄯ৻恳〬昴껤룩列潱ᑮ煃鶖안꽊鹭宪帐❖ा쥈잔";
        String result = CaseMap.fold().apply(s);
        assertTrue("廬ᾒ...->廬ἢι...", result.startsWith("廬ἢι"));
        s = "儊ẖ깸ᝓ恷ᇁ䜄쌼ꇸჃ䗑䘬䒥㈴槁蛚紆洔㖣믏亝醣黹Ά嶨䖕篕舀ꖧ₭ଯᒗ✧ԗ墖쁳㽎苊澎긁⾆⒞蠻왃囨ᡠ邏꾭⪐턣搤穳≠톲絋砖ሷ⠆" +
                "瞏惢鵶剕듘ᅤ♟Ԡⴠ⊡鹔ጙ갑⣚堟ᣗ✸㕇絮䠎瘗⟡놥擢ꉭ佱ྪ飹痵⿑⨴츿璿僖㯷넴鋰膄釚겼ナ黪差";
        result = CaseMap.fold().apply(s);
        assertTrue("儊ẖ...->儊h\u0331...", result.startsWith("儊h\u0331"));
    }

    // private data members - test data --------------------------------------

    private static final Locale TURKISH_LOCALE_ = new Locale("tr", "TR");
    private static final Locale GERMAN_LOCALE_ = new Locale("de", "DE");
    private static final Locale GREEK_LOCALE_ = new Locale("el", "GR");
    private static final Locale ENGLISH_LOCALE_ = new Locale("en", "US");
    private static final Locale LITHUANIAN_LOCALE_ = new Locale("lt", "LT");
    private static final Locale DUTCH_LOCALE_ = new Locale("nl");

    private static final int CHARACTER_UPPER_[] =
                      {0x41, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047,
                       0x00b1, 0x00b2, 0xb3, 0x0048, 0x0049, 0x004a, 0x002e,
                       0x003f, 0x003a, 0x004b, 0x004c, 0x4d, 0x004e, 0x004f,
                       0x01c4, 0x01c8, 0x000c, 0x0000};
    private static final int CHARACTER_LOWER_[] =
                      {0x61, 0x0062, 0x0063, 0x0064, 0x0065, 0x0066, 0x0067,
                       0x00b1, 0x00b2, 0xb3, 0x0068, 0x0069, 0x006a, 0x002e,
                       0x003f, 0x003a, 0x006b, 0x006c, 0x6d, 0x006e, 0x006f,
                       0x01c6, 0x01c9, 0x000c, 0x0000};

    /*
     * CaseFolding.txt says about i and its cousins:
     *   0049; C; 0069; # LATIN CAPITAL LETTER I
     *   0049; T; 0131; # LATIN CAPITAL LETTER I
     *
     *   0130; F; 0069 0307; # LATIN CAPITAL LETTER I WITH DOT ABOVE
     *   0130; T; 0069; # LATIN CAPITAL LETTER I WITH DOT ABOVE
     * That's all.
     * See CaseFolding.txt and the Unicode Standard for how to apply the case foldings.
     */
    private static final int FOLDING_SIMPLE_[] = {
        // input, default, exclude special i
        0x61,   0x61,  0x61,
        0x49,   0x69,  0x131,
        0x130,  0x130, 0x69,
        0x131,  0x131, 0x131,
        0xdf,   0xdf,  0xdf,
        0xfb03, 0xfb03, 0xfb03,
        0x1040e,0x10436,0x10436,
        0x5ffff,0x5ffff,0x5ffff
    };
    private static final String FOLDING_MIXED_[] =
                          {"\u0061\u0042\u0130\u0049\u0131\u03d0\u00df\ufb03\ud93f\udfff",
                           "A\u00df\u00b5\ufb03\uD801\uDC0C\u0130\u0131"};
    private static final String FOLDING_DEFAULT_[] =
         {"\u0061\u0062\u0069\u0307\u0069\u0131\u03b2\u0073\u0073\u0066\u0066\u0069\ud93f\udfff",
          "ass\u03bcffi\uD801\uDC34i\u0307\u0131"};
    private static final String FOLDING_EXCLUDE_SPECIAL_I_[] =
         {"\u0061\u0062\u0069\u0131\u0131\u03b2\u0073\u0073\u0066\u0066\u0069\ud93f\udfff",
          "ass\u03bcffi\uD801\uDC34i\u0131"};
    /**
     * "IESUS CHRISTOS"
     */
    private static final String SHARED_UPPERCASE_GREEK_ =
        "\u0399\u0395\u03a3\u03a5\u03a3\u0020\u03a7\u03a1\u0399\u03a3\u03a4\u039f\u03a3";
    /**
     * "iesus christos"
     */
    private static final String SHARED_LOWERCASE_GREEK_ =
        "\u03b9\u03b5\u03c3\u03c5\u03c2\u0020\u03c7\u03c1\u03b9\u03c3\u03c4\u03bf\u03c2";
    private static final String SHARED_LOWERCASE_TURKISH_ =
        "\u0069\u0073\u0074\u0061\u006e\u0062\u0075\u006c\u002c\u0020\u006e\u006f\u0074\u0020\u0063\u006f\u006e\u0073\u0074\u0061\u006e\u0074\u0131\u006e\u006f\u0070\u006c\u0065\u0021";
    private static final String SHARED_UPPERCASE_TURKISH_ =
        "\u0054\u004f\u0050\u004b\u0041\u0050\u0049\u0020\u0050\u0041\u004c\u0041\u0043\u0045\u002c\u0020\u0130\u0053\u0054\u0041\u004e\u0042\u0055\u004c";
    private static final String SHARED_UPPERCASE_ISTANBUL_ =
                                          "\u0130STANBUL, NOT CONSTANTINOPLE!";
    private static final String SHARED_LOWERCASE_ISTANBUL_ =
                                          "i\u0307stanbul, not constantinople!";
    private static final String SHARED_LOWERCASE_TOPKAP_ =
                                          "topkap\u0131 palace, istanbul";
    private static final String SHARED_UPPERCASE_TOPKAP_ =
                                          "TOPKAPI PALACE, ISTANBUL";
    private static final String SHARED_LOWERCASE_GERMAN_ =
                                          "S\u00FC\u00DFmayrstra\u00DFe";
    private static final String SHARED_UPPERCASE_GERMAN_ =
                                          "S\u00DCSSMAYRSTRASSE";

    private static final String UPPER_BEFORE_ =
         "\u0061\u0042\u0069\u03c2\u00df\u03c3\u002f\ufb03\ufb03\ufb03\ud93f\udfff";
    private static final String UPPER_ROOT_ =
         "\u0041\u0042\u0049\u03a3\u0053\u0053\u03a3\u002f\u0046\u0046\u0049\u0046\u0046\u0049\u0046\u0046\u0049\ud93f\udfff";
    private static final String UPPER_TURKISH_ =
         "\u0041\u0042\u0130\u03a3\u0053\u0053\u03a3\u002f\u0046\u0046\u0049\u0046\u0046\u0049\u0046\u0046\u0049\ud93f\udfff";
    private static final String UPPER_MINI_ = "\u00df\u0061";
    private static final String UPPER_MINI_UPPER_ = "\u0053\u0053\u0041";

    private static final String LOWER_BEFORE_ =
                      "\u0061\u0042\u0049\u03a3\u00df\u03a3\u002f\ud93f\udfff";
    private static final String LOWER_ROOT_ =
                      "\u0061\u0062\u0069\u03c3\u00df\u03c2\u002f\ud93f\udfff";
    private static final String LOWER_TURKISH_ =
                      "\u0061\u0062\u0131\u03c3\u00df\u03c2\u002f\ud93f\udfff";

    /**
     * each item is an array with input string, result string, locale ID, break iterator, options
     * the break iterator is specified as an int, same as in BreakIterator.KIND_*:
     * 0=KIND_CHARACTER  1=KIND_WORD  2=KIND_LINE  3=KIND_SENTENCE  4=KIND_TITLE  -1=default (NULL=words)  -2=no breaks (.*)
     * options: T=U_FOLD_CASE_EXCLUDE_SPECIAL_I  L=U_TITLECASE_NO_LOWERCASE  A=U_TITLECASE_NO_BREAK_ADJUSTMENT
     * see ICU4C source/test/testdata/casing.txt
     */
    private static final String TITLE_DATA_[] = {
        "\u0061\u0042\u0020\u0069\u03c2\u0020\u00df\u03c3\u002f\ufb03\ud93f\udfff",
        "\u0041\u0042\u0020\u0049\u03a3\u0020\u0053\u0073\u03a3\u002f\u0046\u0066\u0069\ud93f\udfff",
        "",
        "0",
        "",

        "\u0061\u0042\u0020\u0069\u03c2\u0020\u00df\u03c3\u002f\ufb03\ud93f\udfff",
        "\u0041\u0062\u0020\u0049\u03c2\u0020\u0053\u0073\u03c3\u002f\u0046\u0066\u0069\ud93f\udfff",
        "",
        "1",
        "",

        "\u02bbaMeLikA huI P\u016b \u02bb\u02bb\u02bbiA", "\u02bbAmelika Hui P\u016b \u02bb\u02bb\u02bbIa", // titlecase first _cased_ letter, j4933
        "",
        "-1",
        "",

        " tHe QUIcK bRoWn", " The Quick Brown",
        "",
        "4",
        "",

        "\u01c4\u01c5\u01c6\u01c7\u01c8\u01c9\u01ca\u01cb\u01cc",
        "\u01c5\u01c5\u01c5\u01c8\u01c8\u01c8\u01cb\u01cb\u01cb", // UBRK_CHARACTER
        "",
        "0",
        "",

        "\u01c9ubav ljubav", "\u01c8ubav Ljubav", // Lj vs. L+j
        "",
        "-1",
        "",

        "'oH dOn'T tItLeCaSe AfTeR lEtTeR+'",  "'Oh Don't Titlecase After Letter+'",
        "",
        "-1",
        "",

        "a \u02bbCaT. A \u02bbdOg! \u02bbeTc.",
        "A \u02bbCat. A \u02bbDog! \u02bbEtc.",
        "",
        "-1",
        "", // default

        "a \u02bbCaT. A \u02bbdOg! \u02bbeTc.",
        "A \u02bbcat. A \u02bbdog! \u02bbetc.",
        "",
        "-1",
        "A", // U_TITLECASE_NO_BREAK_ADJUSTMENT

        "a \u02bbCaT. A \u02bbdOg! \u02bbeTc.",
        "A \u02bbCaT. A \u02bbdOg! \u02bbETc.",
        "",
        "3",
        "L", // UBRK_SENTENCE and U_TITLECASE_NO_LOWERCASE


        "\u02bbcAt! \u02bbeTc.",
        "\u02bbCat! \u02bbetc.",
        "",
        "-2",
        "", // -2=Trivial break iterator

        "\u02bbcAt! \u02bbeTc.",
        "\u02bbcat! \u02bbetc.",
        "",
        "-2",
        "A", // U_TITLECASE_NO_BREAK_ADJUSTMENT

        "\u02bbcAt! \u02bbeTc.",
        "\u02bbCAt! \u02bbeTc.",
        "",
        "-2",
        "L", // U_TITLECASE_NO_LOWERCASE

        "\u02bbcAt! \u02bbeTc.",
        "\u02bbcAt! \u02bbeTc.",
        "",
        "-2",
        "AL", // Both options

        // Test case for ticket #7251: UCharacter.toTitleCase() throws OutOfMemoryError
        // when TITLECASE_NO_LOWERCASE encounters a single-letter word
        "a b c",
        "A B C",
        "",
        "1",
        "L" // U_TITLECASE_NO_LOWERCASE
    };


    /**
     * <p>basic string, lower string, upper string, title string</p>
     */
    private static final String SPECIAL_DATA_[] = {
        UTF16.valueOf(0x1043C) + UTF16.valueOf(0x10414),
        UTF16.valueOf(0x1043C) + UTF16.valueOf(0x1043C),
        UTF16.valueOf(0x10414) + UTF16.valueOf(0x10414),
        "ab'cD \uFB00i\u0131I\u0130 \u01C7\u01C8\u01C9 " +
                         UTF16.valueOf(0x1043C) + UTF16.valueOf(0x10414),
        "ab'cd \uFB00i\u0131ii\u0307 \u01C9\u01C9\u01C9 " +
                              UTF16.valueOf(0x1043C) + UTF16.valueOf(0x1043C),
        "AB'CD FFIII\u0130 \u01C7\u01C7\u01C7 " +
                              UTF16.valueOf(0x10414) + UTF16.valueOf(0x10414),
        // sigmas followed/preceded by cased letters
        "i\u0307\u03a3\u0308j \u0307\u03a3\u0308j i\u00ad\u03a3\u0308 \u0307\u03a3\u0308 ",
        "i\u0307\u03c3\u0308j \u0307\u03c3\u0308j i\u00ad\u03c2\u0308 \u0307\u03c3\u0308 ",
        "I\u0307\u03a3\u0308J \u0307\u03a3\u0308J I\u00ad\u03a3\u0308 \u0307\u03a3\u0308 "
    };
    private static final Locale SPECIAL_LOCALES_[] = {
        null,
        ENGLISH_LOCALE_,
        null,
    };

    private static final String SPECIAL_DOTTED_ =
            "I \u0130 I\u0307 I\u0327\u0307 I\u0301\u0307 I\u0327\u0307\u0301";
    private static final String SPECIAL_DOTTED_LOWER_TURKISH_ =
            "\u0131 i i i\u0327 \u0131\u0301\u0307 i\u0327\u0301";
    private static final String SPECIAL_DOTTED_LOWER_GERMAN_ =
            "i i\u0307 i\u0307 i\u0327\u0307 i\u0301\u0307 i\u0327\u0307\u0301";
    private static final String SPECIAL_DOT_ABOVE_ =
            "a\u0307 \u0307 i\u0307 j\u0327\u0307 j\u0301\u0307";
    private static final String SPECIAL_DOT_ABOVE_UPPER_LITHUANIAN_ =
            "A\u0307 \u0307 I J\u0327 J\u0301\u0307";
    private static final String SPECIAL_DOT_ABOVE_UPPER_GERMAN_ =
            "A\u0307 \u0307 I\u0307 J\u0327\u0307 J\u0301\u0307";
    private static final String SPECIAL_DOT_ABOVE_UPPER_ =
            "I I\u0301 J J\u0301 \u012e \u012e\u0301 \u00cc\u00cd\u0128";
    private static final String SPECIAL_DOT_ABOVE_LOWER_LITHUANIAN_ =
            "i i\u0307\u0301 j j\u0307\u0301 \u012f \u012f\u0307\u0301 i\u0307\u0300i\u0307\u0301i\u0307\u0303";
    private static final String SPECIAL_DOT_ABOVE_LOWER_GERMAN_ =
            "i i\u0301 j j\u0301 \u012f \u012f\u0301 \u00ec\u00ed\u0129";

    // private methods -------------------------------------------------------

    /**
     * Converting the hex numbers represented between ';' to Unicode strings
     * @param str string to break up into Unicode strings
     * @return array of Unicode strings ending with a null
     */
    private String[] getUnicodeStrings(String str)
    {
        List<String> v = new ArrayList<>(10);
        int start = 0;
        for (int casecount = 4; casecount > 0; casecount --) {
            int end = str.indexOf("; ", start);
            String casestr = str.substring(start, end);
            StringBuffer buffer = new StringBuffer();
            int spaceoffset = 0;
            while (spaceoffset < casestr.length()) {
                int nextspace = casestr.indexOf(' ', spaceoffset);
                if (nextspace == -1) {
                    nextspace = casestr.length();
                }
                buffer.append((char)Integer.parseInt(
                                     casestr.substring(spaceoffset, nextspace),
                                                      16));
                spaceoffset = nextspace + 1;
            }
            start = end + 2;
            v.add(buffer.toString());
        }
        int comments = str.indexOf(" #", start);
        if (comments != -1 && comments != start) {
            if (str.charAt(comments - 1) == ';') {
                comments --;
            }
            String conditions = str.substring(start, comments);
            int offset = 0;
            while (offset < conditions.length()) {
                int spaceoffset = conditions.indexOf(' ', offset);
                if (spaceoffset == -1) {
                    spaceoffset = conditions.length();
                }
                v.add(conditions.substring(offset, spaceoffset));
                offset = spaceoffset + 1;
            }
        }
        int size = v.size();
        String result[] = new String[size];
        for (int i = 0; i < size; i ++) {
            result[i] = v.get(i);
        }
        return result;
    }
}

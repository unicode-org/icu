//##header
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.translit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.UnicodeRegex;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author markdavis
 */
public class RegexUtilitiesTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new RegexUtilitiesTest().run(args);
    }

    /**
     * Check basic construction.
     */
    public void TestConstruction() {
        String[][] tests = {
                {"a"},
                {"a[a-z]b"},
                {"[ba-z]", "[a-z]"},
                {"q[ba-z]", "q[a-z]"},
                {"[ba-z]q", "[a-z]q"},
                {"a\\p{joincontrol}b", "a[\u200C\u200D]b"},
                {"a\\P{joincontrol}b", "a[^\u200C\u200D]b"},
                {"a[[:whitespace:]&[:Zl:]]b", "a[\\\u2028]b"},
                {"a [[:bc=cs:]&[:wspace:]] b", "a [\u00A0\u202F] b"},
        };
        for (int i = 0; i < tests.length; ++i) {
            final String source = tests[i][0];
            String expected = tests[i].length == 1 ? source : tests[i][1];
            String actual = UnicodeRegex.fix(source);
            assertEquals(source, expected, actual);
        } 
    }

    Transliterator hex = Transliterator.getInstance("hex");

    /**
     * Perform an exhaustive test on all Unicode characters to make sure that the UnicodeSet with each
     * character works.
     */
    public void TestCharacters() {
        UnicodeSet requiresQuote = new UnicodeSet("[\\$\\&\\-\\:\\[\\\\\\]\\^\\{\\}[:pattern_whitespace:]]");
        boolean skip = getInclusion() < 10;
        for (int cp = 0; cp < 0x110000; ++cp) {
            if (cp > 0xFF && skip && (cp % 37 != 0)) {
                continue;
            }
            String cpString = UTF16.valueOf(cp);
            String s = requiresQuote.contains(cp) ? "\\" + cpString : cpString;
            String pattern = null;
            final String rawPattern = "[" + s + s + "]";
            try {
                pattern = UnicodeRegex.fix(rawPattern);
            } catch (Exception e) {
                errln(e.getMessage());
                continue;
            }
            final String expected = "[" + s + "]";
            assertEquals("Doubled character works" + hex.transform(s), expected, pattern);

            // verify that we can create a regex pattern and use as expected
            String shouldNotMatch = UTF16.valueOf((cp + 1) % 0x110000);
            checkCharPattern(Pattern.compile(pattern), pattern, cpString, shouldNotMatch);

            // verify that the Pattern.compile works
            checkCharPattern(UnicodeRegex.compile(rawPattern), pattern, cpString, shouldNotMatch);
        }
    }

    /**
     * Check all integer Unicode properties to make sure they work.
     */
    public void TestUnicodeProperties() {
        final boolean skip = getInclusion() < 10;
        UnicodeSet temp = new UnicodeSet();
        for (int propNum = UProperty.INT_START; propNum < UProperty.INT_LIMIT; ++propNum) {
            if (skip && (propNum % 5 != 0)) {
                continue;
            }
            String propName = UCharacter.getPropertyName(propNum, NameChoice.LONG);
            final int intPropertyMinValue = UCharacter.getIntPropertyMinValue(propNum);
            int intPropertyMaxValue = UCharacter.getIntPropertyMaxValue(propNum);
            if (skip) { // only test first if not exhaustive
                intPropertyMaxValue = intPropertyMinValue;
            }
            for (int valueNum = intPropertyMinValue; valueNum <= intPropertyMaxValue; ++valueNum) {
                // hack for getting property value name
                String valueName = UCharacter.getPropertyValueName(propNum, valueNum, NameChoice.LONG);
                if (valueName == null) {
                    valueName = UCharacter.getPropertyValueName(propNum, valueNum, NameChoice.SHORT);
                    if (valueName == null) {
                        valueName = Integer.toString(valueNum);
                    }
                }
                temp.applyIntPropertyValue(propNum, valueNum);
                if (temp.size() == 0) {
                    continue;
                }
                final String prefix = "a";
                final String suffix = "b";
                String shouldMatch = prefix + UTF16.valueOf(temp.charAt(0)) + suffix;
                temp.complement();
                String shouldNotMatch = prefix + UTF16.valueOf(temp.charAt(0)) + suffix;

                // posix style pattern
                String rawPattern = prefix + "[:" + propName + "=" + valueName + ":]" + suffix;
                String rawNegativePattern = prefix + "[:^" + propName + "=" + valueName + ":]" + suffix;
                checkCharPattern(UnicodeRegex.compile(rawPattern), rawPattern, shouldMatch, shouldNotMatch);
                checkCharPattern(UnicodeRegex.compile(rawNegativePattern), rawNegativePattern, shouldNotMatch, shouldMatch);

                // perl style pattern
                rawPattern = prefix + "\\p{" + propName + "=" + valueName + "}" + suffix;
                rawNegativePattern = prefix + "\\P{" + propName + "=" + valueName + "}" + suffix;
                checkCharPattern(UnicodeRegex.compile(rawPattern), rawPattern, shouldMatch, shouldNotMatch);
                checkCharPattern(UnicodeRegex.compile(rawNegativePattern), rawNegativePattern, shouldNotMatch, shouldMatch);
            }
        }
    }

    /**
     * Utility for checking patterns
     */
    private void checkCharPattern(Pattern pat, String matchTitle, String shouldMatch, String shouldNotMatch) {
        Matcher matcher = pat.matcher(shouldMatch);
        assertTrue(matchTitle + " and " + shouldMatch, matcher.matches());
        matcher.reset(shouldNotMatch);
        assertFalse(matchTitle + " and " + shouldNotMatch, matcher.matches());
    }
}
//#endif

// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static com.ibm.icu.impl.number.parse.UnicodeSetStaticCache.get;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ibm.icu.impl.number.parse.UnicodeSetStaticCache;
import com.ibm.icu.impl.number.parse.UnicodeSetStaticCache.Key;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

/**
 * @author sffc
 *
 */
public class UnicodeSetStaticCacheTest {

    @Test
    public void testSetCoverage() {
        // Lenient comma/period should be supersets of strict comma/period;
        // it also makes the coverage logic cheaper.
        assertTrue("COMMA should be superset of STRICT_COMMA",
                get(Key.COMMA).containsAll(get(Key.STRICT_COMMA)));
        assertTrue("PERIOD should be superset of STRICT_PERIOD",
                get(Key.PERIOD).containsAll(get(Key.STRICT_PERIOD)));

        UnicodeSet decimals = get(Key.STRICT_COMMA).cloneAsThawed().addAll(get(Key.STRICT_PERIOD))
                .freeze();
        UnicodeSet grouping = decimals.cloneAsThawed().addAll(get(Key.OTHER_GROUPING_SEPARATORS))
                .freeze();
        UnicodeSet plusSign = get(Key.PLUS_SIGN);
        UnicodeSet minusSign = get(Key.MINUS_SIGN);
        UnicodeSet percent = get(Key.PERCENT_SIGN);
        UnicodeSet permille = get(Key.PERMILLE_SIGN);
        UnicodeSet infinity = get(Key.INFINITY);
        UnicodeSet nanLead = get(Key.NAN_LEAD);
        UnicodeSet scientificLead = get(Key.SCIENTIFIC_LEAD);

        for (ULocale locale : ULocale.getAvailableLocales()) {
            DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(locale);

            assertInSet(locale, decimals, dfs.getDecimalSeparatorString());
            assertInSet(locale, grouping, dfs.getGroupingSeparatorString());
            assertInSet(locale, plusSign, dfs.getPlusSignString());
            assertInSet(locale, minusSign, dfs.getMinusSignString());
            assertInSet(locale, percent, dfs.getPercentString());
            assertInSet(locale, permille, dfs.getPerMillString());
            assertInSet(locale, infinity, dfs.getInfinity());
            assertInSet(locale, nanLead, dfs.getNaN().codePointAt(0));
            assertInSet(locale, nanLead, UCharacter.foldCase(dfs.getNaN(), true).codePointAt(0));
            assertInSet(locale,
                    scientificLead,
                    UCharacter.foldCase(dfs.getExponentSeparator(), true).codePointAt(0));
        }
    }

    @Test
    public void testFrozen() {
        for (Key key : Key.values()) {
            assertTrue(get(key).isFrozen());
        }
    }

    @Test
    public void testUnions() {
        for (Key key1 : Key.values()) {
            for (Key key2 : Key.values()) {
                Key key3 = UnicodeSetStaticCache.unionOf(key1, key2);
                if (key3 != null) {
                    UnicodeSet s1 = get(key1);
                    UnicodeSet s2 = get(key2);
                    UnicodeSet s3 = get(key3);
                    UnicodeSet s1_s2 = s1.cloneAsThawed().addAll(s2);
                    assertEquals(key1 + "/" + key2 + "/" + key3, s1_s2, s3);
                }
            }
        }
    }

    static void assertInSet(ULocale locale, UnicodeSet set, String str) {
        if (str.codePointCount(0, str.length()) != 1) {
            // Ignore locale strings with more than one code point (usually a bidi mark)
            return;
        }
        assertInSet(locale, set, str.codePointAt(0));
    }

    static void assertInSet(ULocale locale, UnicodeSet set, int cp) {
        // If this test case fails, add the specified code point to the corresponding set in
        // UnicodeSetStaticCache.java
        assertTrue(
                locale
                        + " U+"
                        + Integer.toHexString(cp)
                        + " ("
                        + UCharacter.toString(cp)
                        + ") should be in "
                        + set,
                set.contains(cp));
    }
}

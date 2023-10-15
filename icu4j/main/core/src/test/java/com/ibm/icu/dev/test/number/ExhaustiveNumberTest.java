// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.number;

import com.ibm.icu.dev.test.CoreTestFmwk;
import static com.ibm.icu.impl.StaticUnicodeSets.get;

import java.math.BigDecimal;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.StaticUnicodeSets.Key;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.UnlocalizedNumberFormatter;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.NoUnit;
import com.ibm.icu.util.ULocale;

/**
 * Tests that are disabled except in exhaustive mode due to runtime.
 *
 * @author sffc
 */
public class ExhaustiveNumberTest extends CoreTestFmwk {
    @Before
    public void beforeMethod() {
        // Disable this test class except for exhaustive mode.
        // To enable exhaustive mode, pass the JVM argument "-DICU.exhaustive=10"
        org.junit.Assume.assumeTrue(getExhaustiveness() > 5);
    }

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
        UnicodeSet infinity = get(Key.INFINITY_SIGN);

        for (ULocale locale : ULocale.getAvailableLocales()) {
            DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(locale);

            if (!locale.getBaseName().startsWith("ks_Deva") || !logKnownIssue("22099", "locale-specific parse sets not supported")) {
                assertInSet(locale, decimals, dfs.getDecimalSeparatorString());
            }
            if (!locale.getBaseName().startsWith("nqo") || !logKnownIssue("CLDR-17023", "Number symbols and/or parseLenients messed up for N’Ko")) {
                assertInSet(locale, grouping, dfs.getGroupingSeparatorString());
            }
            assertInSet(locale, plusSign, dfs.getPlusSignString());
            assertInSet(locale, minusSign, dfs.getMinusSignString());
            assertInSet(locale, percent, dfs.getPercentString());
            assertInSet(locale, permille, dfs.getPerMillString());
            assertInSet(locale, infinity, dfs.getInfinity());
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
        // UnicodeSetStaticCache.java and numparse_unisets.cpp
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

    @Test
    public void test20417_PercentParity() {
        UnlocalizedNumberFormatter uNoUnitPercent = NumberFormatter.with().unit(NoUnit.PERCENT);
        UnlocalizedNumberFormatter uNoUnitPermille = NumberFormatter.with().unit(NoUnit.PERMILLE);
        UnlocalizedNumberFormatter uMeasurePercent = NumberFormatter.with().unit(MeasureUnit.PERCENT);
        UnlocalizedNumberFormatter uMeasurePermille = NumberFormatter.with().unit(MeasureUnit.PERMILLE);

        for (ULocale locale : ULocale.getAvailableLocales()) {
            String sNoUnitPercent = uNoUnitPercent.locale(locale).format(50).toString();
            String sNoUnitPermille = uNoUnitPermille.locale(locale).format(50).toString();
            String sMeasurePercent = uMeasurePercent.locale(locale).format(50).toString();
            String sMeasurePermille = uMeasurePermille.locale(locale).format(50).toString();

            assertEquals("Percent, locale " + locale, sNoUnitPercent, sMeasurePercent);
            assertEquals("Permille, locale " + locale, sNoUnitPermille, sMeasurePermille);
        }
    }

    @Test
    public void unlimitedRoundingBigDecimal() {
        BigDecimal ten10000 = BigDecimal.valueOf(10).pow(10000);
        BigDecimal longFraction = ten10000.subtract(BigDecimal.ONE).divide(ten10000);
        String expected = longFraction.toPlainString();
        String actual = NumberFormatter.withLocale(ULocale.ENGLISH).precision(Precision.unlimited())
                .format(longFraction).toString();
        assertEquals("All digits should be displayed", expected, actual);
    }

    @Test
    public void testConvertToAccurateDouble() {
        // based on https://github.com/google/double-conversion/issues/28
        double[] hardDoubles = {
                1651087494906221570.0,
                2.207817077636718750000000000000,
                1.818351745605468750000000000000,
                3.941719055175781250000000000000,
                3.738609313964843750000000000000,
                3.967735290527343750000000000000,
                1.328025817871093750000000000000,
                3.920967102050781250000000000000,
                1.015235900878906250000000000000,
                1.335227966308593750000000000000,
                1.344520568847656250000000000000,
                2.879127502441406250000000000000,
                3.695838928222656250000000000000,
                1.845344543457031250000000000000,
                3.793952941894531250000000000000,
                3.211402893066406250000000000000,
                2.565971374511718750000000000000,
                0.965156555175781250000000000000,
                2.700004577636718750000000000000,
                0.767097473144531250000000000000,
                1.780448913574218750000000000000,
                2.624839782714843750000000000000,
                1.305290222167968750000000000000,
                3.834922790527343750000000000000, };

        double[] exactDoubles = {
                51423,
                51423e10,
                -5074790912492772E-327,
                83602530019752571E-327,
                4.503599627370496E15,
                6.789512076111555E15,
                9.007199254740991E15,
                9.007199254740992E15 };

        for (double d : hardDoubles) {
            checkDoubleBehavior(d, true, "");
        }

        for (double d : exactDoubles) {
            checkDoubleBehavior(d, false, "");
        }

        assertEquals("NaN check failed",
                Double.NaN,
                new DecimalQuantity_DualStorageBCD(Double.NaN).toDouble());
        assertEquals("Inf check failed",
                Double.POSITIVE_INFINITY,
                new DecimalQuantity_DualStorageBCD(Double.POSITIVE_INFINITY).toDouble());
        assertEquals("-Inf check failed",
                Double.NEGATIVE_INFINITY,
                new DecimalQuantity_DualStorageBCD(Double.NEGATIVE_INFINITY).toDouble());

        // Generate random doubles
        String alert = "UNEXPECTED FAILURE: PLEASE REPORT THIS MESSAGE TO THE ICU TEAM: ";
        Random rnd = new Random();
        for (int i = 0; i < 100000; i++) {
            double d = Double.longBitsToDouble(rnd.nextLong());
            if (Double.isNaN(d) || Double.isInfinite(d))
                continue;
            checkDoubleBehavior(d, false, alert);
        }
    }

    private static void checkDoubleBehavior(double d, boolean explicitRequired, String alert) {
        DecimalQuantity_DualStorageBCD fq = new DecimalQuantity_DualStorageBCD(d);
        if (explicitRequired) {
            assertTrue(alert + "Should be using approximate double", !fq.explicitExactDouble);
        }
        fq.roundToInfinity();
        if (explicitRequired) {
            assertTrue(alert + "Should not be using approximate double", fq.explicitExactDouble);
        }
        DecimalQuantityTest
                .assertDoubleEquals(alert + "After conversion to exact BCD (double)", d, fq.toDouble());
        DecimalQuantityTest.assertBigDecimalEquals(alert + "After conversion to exact BCD (BigDecimal)",
                new BigDecimal(Double.toString(d)),
                fq.toBigDecimal());
    }
}

// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 **************************************************************************
 * Copyright (C) 1996-2016, International Business Machines Corporation   *
 * and others. All Rights Reserved.                                       *
 **************************************************************************
 */

/*
 * Port From: JDK 1.4b1 : java.text.Format.IntlTestDecimalFormatSymbols Source File:
 * java/text/format/IntlTestDecimalFormatSymbols.java
 */

/*
    @test 1.4 98/03/06
    @summary test International Decimal Format Symbols
*/

package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;
import java.util.Arrays;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IntlTestDecimalFormatSymbols extends CoreTestFmwk {
    // Test the API of DecimalFormatSymbols; primarily a simple get/set set.
    @Test
    public void TestSymbols() {
        DecimalFormatSymbols fr = new DecimalFormatSymbols(Locale.FRENCH);

        DecimalFormatSymbols en = new DecimalFormatSymbols(Locale.ENGLISH);

        assertNotEquals("ERROR: English DecimalFormatSymbols equal to French", en, fr);

        // just do some VERY basic tests to make sure that get/set work

        assertEquals("ERROR: getLocale failed", en.getLocale(), Locale.ENGLISH);
        assertEquals("ERROR: getULocale failed", en.getULocale(), ULocale.ENGLISH);
        assertEquals("ERROR: getLocale failed", en.getLocale(), Locale.ENGLISH);
        assertEquals("ERROR: getULocale failed", en.getULocale(), ULocale.ENGLISH);

        char zero = en.getZeroDigit();
        fr.setZeroDigit(zero);
        assertEquals("ERROR: get/set ZeroDigit failed", fr.getZeroDigit(), en.getZeroDigit());

        String[] digits = en.getDigitStrings();
        fr.setDigitStrings(digits);
        assertEquals(
                "ERROR: get/set DigitStrings failed", fr.getDigitStrings(), en.getDigitStrings());

        char sigDigit = en.getSignificantDigit();
        fr.setSignificantDigit(sigDigit);
        assertEquals(
                "ERROR: get/set SignificantDigit failed",
                fr.getSignificantDigit(),
                en.getSignificantDigit());

        Currency currency = Currency.getInstance("USD");
        fr.setCurrency(currency);
        assertEquals("ERROR: get/set Currency failed", fr.getCurrency(), currency);

        char group = en.getGroupingSeparator();
        fr.setGroupingSeparator(group);
        assertEquals(
                "ERROR: get/set GroupingSeparator failed",
                fr.getGroupingSeparator(),
                en.getGroupingSeparator());

        String groupStr = en.getGroupingSeparatorString();
        fr.setGroupingSeparatorString(groupStr);
        assertEquals(
                "ERROR: get/set GroupingSeparatorString failed",
                fr.getGroupingSeparatorString(),
                en.getGroupingSeparatorString());

        char decimal = en.getDecimalSeparator();
        fr.setDecimalSeparator(decimal);
        assertEquals(
                "ERROR: get/set DecimalSeparator failed",
                fr.getDecimalSeparator(),
                en.getDecimalSeparator());

        String decimalStr = en.getDecimalSeparatorString();
        fr.setDecimalSeparatorString(decimalStr);
        assertEquals(
                "ERROR: get/set DecimalSeparatorString failed",
                fr.getDecimalSeparatorString(),
                en.getDecimalSeparatorString());

        char monetaryGroup = en.getMonetaryGroupingSeparator();
        fr.setMonetaryGroupingSeparator(monetaryGroup);
        assertEquals(
                "ERROR: get/set MonetaryGroupingSeparator failed",
                fr.getMonetaryGroupingSeparator(),
                en.getMonetaryGroupingSeparator());

        String monetaryGroupStr = en.getMonetaryGroupingSeparatorString();
        fr.setMonetaryGroupingSeparatorString(monetaryGroupStr);
        assertEquals(
                "ERROR: get/set MonetaryGroupingSeparatorString failed",
                fr.getMonetaryGroupingSeparatorString(),
                en.getMonetaryGroupingSeparatorString());

        char monetaryDecimal = en.getMonetaryDecimalSeparator();
        fr.setMonetaryDecimalSeparator(monetaryDecimal);
        assertEquals(
                "ERROR: get/set MonetaryDecimalSeparator failed",
                fr.getMonetaryDecimalSeparator(),
                en.getMonetaryDecimalSeparator());

        String monetaryDecimalStr = en.getMonetaryDecimalSeparatorString();
        fr.setMonetaryDecimalSeparatorString(monetaryDecimalStr);
        assertEquals(
                "ERROR: get/set MonetaryDecimalSeparatorString failed",
                fr.getMonetaryDecimalSeparatorString(),
                en.getMonetaryDecimalSeparatorString());

        char perMill = en.getPerMill();
        fr.setPerMill(perMill);
        assertEquals("ERROR: get/set PerMill failed", fr.getPerMill(), en.getPerMill());

        String perMillStr = en.getPerMillString();
        fr.setPerMillString(perMillStr);
        assertEquals(
                "ERROR: get/set PerMillString failed",
                fr.getPerMillString(),
                en.getPerMillString());

        char percent = en.getPercent();
        fr.setPercent(percent);
        assertEquals("ERROR: get/set Percent failed", fr.getPercent(), en.getPercent());

        String percentStr = en.getPercentString();
        fr.setPercentString(percentStr);
        assertEquals(
                "ERROR: get/set PercentString failed",
                fr.getPercentString(),
                en.getPercentString());

        char digit = en.getDigit();
        fr.setDigit(digit);
        assertEquals("ERROR: get/set Digit failed", fr.getDigit(), en.getDigit());

        char patternSeparator = en.getPatternSeparator();
        fr.setPatternSeparator(patternSeparator);
        assertEquals(
                "ERROR: get/set PatternSeparator failed",
                fr.getPatternSeparator(),
                en.getPatternSeparator());

        String infinity = en.getInfinity();
        fr.setInfinity(infinity);
        String infinity2 = fr.getInfinity();
        assertEquals("ERROR: get/set Infinity failed", infinity, infinity2);

        String nan = en.getNaN();
        fr.setNaN(nan);
        String nan2 = fr.getNaN();
        assertEquals("ERROR: get/set NaN failed", nan, nan2);

        char minusSign = en.getMinusSign();
        fr.setMinusSign(minusSign);
        assertEquals("ERROR: get/set MinusSign failed", fr.getMinusSign(), en.getMinusSign());

        String minusSignStr = en.getMinusSignString();
        fr.setMinusSignString(minusSignStr);
        assertEquals(
                "ERROR: get/set MinusSignString failed",
                fr.getMinusSignString(),
                en.getMinusSignString());

        char plusSign = en.getPlusSign();
        fr.setPlusSign(plusSign);
        assertEquals("ERROR: get/set PlusSign failed", fr.getPlusSign(), en.getPlusSign());

        String plusSignStr = en.getPlusSignString();
        fr.setPlusSignString(plusSignStr);
        assertEquals(
                "ERROR: get/set PlusSignString failed",
                fr.getPlusSignString(),
                en.getPlusSignString());

        char padEscape = en.getPadEscape();
        fr.setPadEscape(padEscape);
        assertSame("ERROR: get/set PadEscape failed", fr.getPadEscape(), en.getPadEscape());

        String exponential = en.getExponentSeparator();
        fr.setExponentSeparator(exponential);
        assertSame(
                "ERROR: get/set Exponential failed",
                fr.getExponentSeparator(),
                en.getExponentSeparator());

        String exponentMultiplicationSign = en.getExponentMultiplicationSign();
        fr.setExponentMultiplicationSign(exponentMultiplicationSign);
        assertSame(
                "ERROR: get/set ExponentMultiplicationSign failed",
                fr.getExponentMultiplicationSign(),
                en.getExponentMultiplicationSign());

        // Test CurrencySpacing.
        // In CLDR 1.7, only root.txt has CurrencySpacing data. This data might
        // be different between en and fr in the future.
        for (int i = DecimalFormatSymbols.CURRENCY_SPC_CURRENCY_MATCH;
                i <= DecimalFormatSymbols.CURRENCY_SPC_INSERT;
                i++) {
            assertEquals(
                    "ERROR: get currency spacing item:" + i + " before the currency",
                    en.getPatternForCurrencySpacing(i, true),
                    fr.getPatternForCurrencySpacing(i, true));
            assertEquals(
                    "ERROR: get currency spacing item:" + i + " after currency",
                    en.getPatternForCurrencySpacing(i, false),
                    fr.getPatternForCurrencySpacing(i, false));
        }

        String dash = "-";
        en.setPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_INSERT, true, dash);
        assertEquals(
                "ERROR: set currency spacing pattern for before currency.",
                dash,
                en.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_INSERT, true));

        // DecimalFormatSymbols foo = new DecimalFormatSymbols(); //The variable is never used

        en = fr.clone();

        assertEquals("ERROR: Clone failed", en, fr);
    }

    @Test
    public void testCoverage() {
        DecimalFormatSymbols df = new DecimalFormatSymbols();
        DecimalFormatSymbols df2 = df.clone();
        if (!df.equals(df2) || df.hashCode() != df2.hashCode()) {
            errln("decimal format symbols clone, equals, or hashCode failed");
        }
    }

    @Test
    public void testPropagateZeroDigit() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setZeroDigit('\u1040');
        DecimalFormat df = new DecimalFormat("0");
        df.setDecimalFormatSymbols(dfs);
        assertEquals(
                "Should propagate char with number property zero", '\u1041', dfs.getDigits()[1]);
        assertEquals(
                "Should propagate char with number property zero",
                "\u1044\u1040\u1041\u1042\u1043",
                df.format(40123));
        dfs.setZeroDigit('a');
        df.setDecimalFormatSymbols(dfs);
        assertEquals("Should propagate char WITHOUT number property zero", 'b', dfs.getDigits()[1]);
        assertEquals(
                "Should propagate char WITHOUT number property zero", "eabcd", df.format(40123));
    }

    @Test
    public void testDigitSymbols() {
        final char defZero = '0';
        final char[] defDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        final String[] defDigitStrings = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        final String[] osmanyaDigitStrings = {
            "\uD801\uDCA0", "\uD801\uDCA1", "\uD801\uDCA2", "\uD801\uDCA3", "\uD801\uDCA4",
            "\uD801\uDCA5", "\uD801\uDCA6", "\uD801\uDCA7", "\uD801\uDCA8", "\uD801\uDCA9"
        };
        final String[] differentDigitStrings = {"0", "b", "3", "d", "5", "ff", "7", "h", "9", "j"};

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
        if (defZero != symbols.getCodePointZero()) {
            errln("ERROR: Code point zero initialize to ASCII 0");
        }

        symbols.setDigitStrings(osmanyaDigitStrings);
        if (!Arrays.equals(symbols.getDigitStrings(), osmanyaDigitStrings)) {
            errln("ERROR: Osmanya digits (supplementary) should be set");
        }
        if (Character.codePointAt(osmanyaDigitStrings[0], 0) != symbols.getCodePointZero()) {
            errln("ERROR: Code point zero be Osmanya code point zero");
        }
        if (defZero != symbols.getZeroDigit()) {
            errln("ERROR: Zero digit should be 0");
        }
        if (!Arrays.equals(symbols.getDigits(), defDigits)) {
            errln("ERROR: Char digits should be Latin digits");
        }

        // Check on copy
        DecimalFormatSymbols copy = symbols.clone();
        if (!Arrays.equals(copy.getDigitStrings(), osmanyaDigitStrings)) {
            errln("ERROR: Osmanya digits (supplementary) should be set");
        }
        if (Character.codePointAt(osmanyaDigitStrings[0], 0) != copy.getCodePointZero()) {
            errln("ERROR: Code point zero be Osmanya code point zero");
        }
        if (defZero != copy.getZeroDigit()) {
            errln("ERROR: Zero digit should be 0");
        }
        if (!Arrays.equals(copy.getDigits(), defDigits)) {
            errln("ERROR: Char digits should be Latin digits");
        }

        // Check on resource bundle
        DecimalFormatSymbols fromData =
                DecimalFormatSymbols.getInstance(new ULocale("en@numbers=osma"));
        if (!Arrays.equals(fromData.getDigitStrings(), osmanyaDigitStrings)) {
            errln("ERROR: Osmanya digits (supplementary) should be set");
        }
        if (Character.codePointAt(osmanyaDigitStrings[0], 0) != fromData.getCodePointZero()) {
            errln("ERROR: Code point zero be Osmanya code point zero");
        }
        if (defZero != fromData.getZeroDigit()) {
            errln("ERROR: Zero digit should be 0");
        }
        if (!Arrays.equals(fromData.getDigits(), defDigits)) {
            errln("ERROR: Char digits should be Latin digits");
        }

        symbols.setDigitStrings(differentDigitStrings);
        if (!Arrays.equals(symbols.getDigitStrings(), differentDigitStrings)) {
            errln("ERROR: Different digits should be set");
        }
        if (-1 != symbols.getCodePointZero()) {
            errln("ERROR: Code point zero should be invalid");
        }

        // Reset digits to Latin
        symbols.setZeroDigit(defZero);
        if (!Arrays.equals(symbols.getDigitStrings(), defDigitStrings)) {
            errln("ERROR: Latin digits should be set" + symbols.getDigitStrings()[0]);
        }
        if (defZero != symbols.getCodePointZero()) {
            errln("ERROR: Code point zero be ASCII 0");
        }
    }

    @Test
    public void testNumberingSystem() {
        Object[][] cases = {
            {"en", "latn", "1,234.56", '%'},
            {"en", "arab", "١٬٢٣٤٫٥٦", "٪\u061C"},
            {"en", "mathsanb", "𝟭,𝟮𝟯𝟰.𝟱𝟲", '%'},
            {"en", "mymr", "၁,၂၃၄.၅၆", '%'},
            {"my", "latn", "1,234.56", '%'},
            {"my", "arab", "١٬٢٣٤٫٥٦", "٪\u061C"},
            {"my", "mathsanb", "𝟭,𝟮𝟯𝟰.𝟱𝟲", '%'},
            {"my", "mymr", "၁,၂၃၄.၅၆", '%'},
            {"ar", "latn", "1,234.56", "\u200E%\u200E"},
            {"ar", "arab", "١٬٢٣٤٫٥٦", "٪\u061C"},
            {"en@numbers=thai", "mymr", "၁,၂၃၄.၅၆", '%'}, // conflicting numbering system
        };

        for (Object[] cas : cases) {
            ULocale loc = new ULocale((String) cas[0]);
            NumberingSystem ns = NumberingSystem.getInstanceByName((String) cas[1]);
            String expectedFormattedNumberString = (String) cas[2];
            String expectedPercentSign = String.valueOf(cas[3]);

            DecimalFormatSymbols dfs = DecimalFormatSymbols.forNumberingSystem(loc, ns);
            DecimalFormat df = new DecimalFormat("#,##0.##", dfs);
            String actual1 = df.format(1234.56);
            assertEquals(
                    "1234.56 with " + loc + " and " + ns.getName(),
                    expectedFormattedNumberString,
                    actual1);

            // The percent sign differs by numbering system.
            String actual2 = dfs.getPercentString();
            assertEquals(
                    "Percent sign with " + loc + " and " + ns.getName(),
                    expectedPercentSign,
                    actual2);

            // Coverage for JDK Locale overload
            DecimalFormatSymbols dfs2 = DecimalFormatSymbols.forNumberingSystem(loc.toLocale(), ns);
            assertEquals("JDK Locale and ICU Locale should produce the same object", dfs, dfs2);
        }
    }

    @Test
    public void testSetPatternForCurrencySpacing_notSharedByInstances() {
        for (int type = DecimalFormatSymbols.CURRENCY_SPC_CURRENCY_MATCH;
                type <= DecimalFormatSymbols.CURRENCY_SPC_INSERT;
                type++) {
            DecimalFormatSymbols dfs1 = DecimalFormatSymbols.getInstance(Locale.US);
            DecimalFormatSymbols dfs2 = DecimalFormatSymbols.getInstance(Locale.US);
            final String pattern = "foobar";
            // before
            dfs1.setPatternForCurrencySpacing(type, false, pattern);
            assertEquals(
                    "setPatternForCurrencySpacing() must set the pattern",
                    pattern,
                    dfs1.getPatternForCurrencySpacing(type, false));
            assertNotEquals(
                    "DFS instances must not share same pattern",
                    pattern,
                    dfs2.getPatternForCurrencySpacing(type, false));
            // after
            dfs1.setPatternForCurrencySpacing(type, true, pattern);
            assertEquals(
                    "setPatternForCurrencySpacing() must set the pattern",
                    pattern,
                    dfs1.getPatternForCurrencySpacing(type, true));
            assertNotEquals(
                    "DFS instances must not share same pattern",
                    pattern,
                    dfs2.getPatternForCurrencySpacing(type, true));
        }
    }
}

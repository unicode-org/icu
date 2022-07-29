// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2001-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

/**
 * Port From:   ICU4C v1.8.1 : format : NumberFormatTest
 * Source File: $ICU4oot/source/test/intltest/numfmtst.cpp
 **/

package com.ibm.icu.dev.test.format;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.dev.test.format.IntlTestDecimalFormatAPIC.FieldContainer;
import com.ibm.icu.impl.DontCareFieldPosition;
import com.ibm.icu.impl.ICUConfig;
import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.impl.data.ResourceReader;
import com.ibm.icu.impl.data.TokenIterator;
import com.ibm.icu.impl.number.PatternStringUtils;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.math.MathContext;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CurrencyPluralInfo;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.DisplayContext;
import com.ibm.icu.text.MeasureFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberFormat.NumberFormatFactory;
import com.ibm.icu.text.NumberFormat.SimpleNumberFormatFactory;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class NumberFormatTest extends TestFmwk {

    @Test
    public void TestRoundingScientific10542() {
        DecimalFormat format =
                new DecimalFormat("0.00E0");

        int[] roundingModes = {
              BigDecimal.ROUND_CEILING,
              BigDecimal.ROUND_DOWN,
              BigDecimal.ROUND_FLOOR,
              BigDecimal.ROUND_HALF_DOWN,
              BigDecimal.ROUND_HALF_EVEN,
              BigDecimal.ROUND_HALF_UP,
              BigDecimal.ROUND_UP};
        String[] descriptions = {
                "Round Ceiling",
                "Round Down",
                "Round Floor",
                "Round half down",
                "Round half even",
                "Round half up",
                "Round up"};

        double[] values = {-0.003006, -0.003005, -0.003004, 0.003014, 0.003015, 0.003016};
        // The order of these expected values correspond to the order of roundingModes and the order of values.
        String[][] expected = {
                {"-3.00E-3", "-3.00E-3", "-3.00E-3", "3.02E-3", "3.02E-3", "3.02E-3"},
                {"-3.00E-3", "-3.00E-3", "-3.00E-3", "3.01E-3", "3.01E-3", "3.01E-3"},
                {"-3.01E-3", "-3.01E-3", "-3.01E-3", "3.01E-3", "3.01E-3", "3.01E-3"},
                {"-3.01E-3", "-3.00E-3", "-3.00E-3", "3.01E-3", "3.01E-3", "3.02E-3"},
                {"-3.01E-3", "-3.00E-3", "-3.00E-3", "3.01E-3", "3.02E-3", "3.02E-3"},
                {"-3.01E-3", "-3.01E-3", "-3.00E-3", "3.01E-3", "3.02E-3", "3.02E-3"},
                {"-3.01E-3", "-3.01E-3", "-3.01E-3", "3.02E-3", "3.02E-3", "3.02E-3"}};
        verifyRounding(format, values, expected, roundingModes, descriptions);
        values = new double[]{-3006.0, -3005, -3004, 3014, 3015, 3016};
        // The order of these expected values correspond to the order of roundingModes and the order of values.
        expected = new String[][]{
                {"-3.00E3", "-3.00E3", "-3.00E3", "3.02E3", "3.02E3", "3.02E3"},
                {"-3.00E3", "-3.00E3", "-3.00E3", "3.01E3", "3.01E3", "3.01E3"},
                {"-3.01E3", "-3.01E3", "-3.01E3", "3.01E3", "3.01E3", "3.01E3"},
                {"-3.01E3", "-3.00E3", "-3.00E3", "3.01E3", "3.01E3", "3.02E3"},
                {"-3.01E3", "-3.00E3", "-3.00E3", "3.01E3", "3.02E3", "3.02E3"},
                {"-3.01E3", "-3.01E3", "-3.00E3", "3.01E3", "3.02E3", "3.02E3"},
                {"-3.01E3", "-3.01E3", "-3.01E3", "3.02E3", "3.02E3", "3.02E3"}};
        verifyRounding(format, values, expected, roundingModes, descriptions);
        values = new double[]{0.0, -0.0};
        // The order of these expected values correspond to the order of roundingModes and the order of values.
        expected = new String[][]{
                {"0.00E0", "-0.00E0"},
                {"0.00E0", "-0.00E0"},
                {"0.00E0", "-0.00E0"},
                {"0.00E0", "-0.00E0"},
                {"0.00E0", "-0.00E0"},
                {"0.00E0", "-0.00E0"},
                {"0.00E0", "-0.00E0"}};
        verifyRounding(format, values, expected, roundingModes, descriptions);
        values = new double[]{1e25, 1e25 + 1e15, 1e25 - 1e15};
        // The order of these expected values correspond to the order of roundingModes and the order of values.
        expected = new String[][]{
                {"1.00E25", "1.01E25", "1.00E25"},
                {"1.00E25", "1.00E25", "9.99E24"},
                {"1.00E25", "1.00E25", "9.99E24"},
                {"1.00E25", "1.00E25", "1.00E25"},
                {"1.00E25", "1.00E25", "1.00E25"},
                {"1.00E25", "1.00E25", "1.00E25"},
                {"1.00E25", "1.01E25", "1.00E25"}};
        verifyRounding(format, values, expected, roundingModes, descriptions);
        values = new double[]{-1e25, -1e25 + 1e15, -1e25 - 1e15};
        // The order of these expected values correspond to the order of roundingModes and the order of values.
        expected = new String[][]{
                {"-1.00E25", "-9.99E24", "-1.00E25"},
                {"-1.00E25", "-9.99E24", "-1.00E25"},
                {"-1.00E25", "-1.00E25", "-1.01E25"},
                {"-1.00E25", "-1.00E25", "-1.00E25"},
                {"-1.00E25", "-1.00E25", "-1.00E25"},
                {"-1.00E25", "-1.00E25", "-1.00E25"},
                {"-1.00E25", "-1.00E25", "-1.01E25"}};
        verifyRounding(format, values, expected, roundingModes, descriptions);
        values = new double[]{1e-25, 1e-25 + 1e-35, 1e-25 - 1e-35};
        // The order of these expected values correspond to the order of roundingModes and the order of values.
        expected = new String[][]{
                {"1.00E-25", "1.01E-25", "1.00E-25"},
                {"1.00E-25", "1.00E-25", "9.99E-26"},
                {"1.00E-25", "1.00E-25", "9.99E-26"},
                {"1.00E-25", "1.00E-25", "1.00E-25"},
                {"1.00E-25", "1.00E-25", "1.00E-25"},
                {"1.00E-25", "1.00E-25", "1.00E-25"},
                {"1.00E-25", "1.01E-25", "1.00E-25"}};
        verifyRounding(format, values, expected, roundingModes, descriptions);
        values = new double[]{-1e-25, -1e-25 + 1e-35, -1e-25 - 1e-35};
        // The order of these expected values correspond to the order of roundingModes and the order of values.
        expected = new String[][]{
                {"-1.00E-25", "-9.99E-26", "-1.00E-25"},
                {"-1.00E-25", "-9.99E-26", "-1.00E-25"},
                {"-1.00E-25", "-1.00E-25", "-1.01E-25"},
                {"-1.00E-25", "-1.00E-25", "-1.00E-25"},
                {"-1.00E-25", "-1.00E-25", "-1.00E-25"},
                {"-1.00E-25", "-1.00E-25", "-1.00E-25"},
                {"-1.00E-25", "-1.00E-25", "-1.01E-25"}};
        verifyRounding(format, values, expected, roundingModes, descriptions);
    }

    private void verifyRounding(DecimalFormat format, double[] values, String[][] expected, int[] roundingModes,
            String[] descriptions) {
        for (int i = 0; i < roundingModes.length; i++) {
            format.setRoundingMode(roundingModes[i]);
            for (int j = 0; j < values.length; j++) {
                assertEquals(descriptions[i]+" " +values[j], expected[i][j], format.format(values[j]));
            }
        }
    }

    @Test
    public void Test10419RoundingWith0FractionDigits() {
        Object[][] data = new Object[][]{
                {BigDecimal.ROUND_CEILING, 1.488, "2"},
                {BigDecimal.ROUND_DOWN, 1.588, "1"},
                {BigDecimal.ROUND_FLOOR, 1.588, "1"},
                {BigDecimal.ROUND_HALF_DOWN, 1.5, "1"},
                {BigDecimal.ROUND_HALF_EVEN, 2.5, "2"},
                {BigDecimal.ROUND_HALF_UP, 2.5, "3"},
                {BigDecimal.ROUND_UP, 1.5, "2"},
        };
        NumberFormat nff = NumberFormat.getNumberInstance(ULocale.ENGLISH);
        nff.setMaximumFractionDigits(0);
        for (Object[] item : data) {
          nff.setRoundingMode(((Integer) item[0]).intValue());
          assertEquals("Test10419", item[2], nff.format(item[1]));
        }
    }

    @Test
    public void TestParseNegativeWithFaLocale() {
        DecimalFormat parser = (DecimalFormat) NumberFormat.getInstance(new ULocale("fa"));
        try {
            double value = parser.parse("-0,5").doubleValue();
            assertEquals("Expect -0.5", -0.5, value);
        } catch (ParseException e) {
            TestFmwk.errln("Parsing -0.5 should have succeeded.");
        }
    }

    @Test
    public void TestParseNegativeWithAlternativeMinusSign() {
        DecimalFormat parser = (DecimalFormat) NumberFormat.getInstance(new ULocale("en"));
        try {
            double value = parser.parse("\u208B0.5").doubleValue();
            assertEquals("Expect -0.5", -0.5, value);
        } catch (ParseException e) {
            TestFmwk.errln("Parsing -0.5 should have succeeded.");
        }
    }

    // Test various patterns
    @Test
    public void TestPatterns() {

        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        final String pat[]    = { "#.#", "#.", ".#", "#" };
        int pat_length = pat.length;
        final String newpat[] = { "0.#", "0.", "#.0", "0" };
        final String num[]    = { "0",   "0.", ".0", "0" };
        for (int i=0; i<pat_length; ++i)
        {
            DecimalFormat fmt = new DecimalFormat(pat[i], sym);
            String newp = fmt.toPattern();
            if (!newp.equals(newpat[i]))
                errln("FAIL: Pattern " + pat[i] + " should transmute to " + newpat[i] +
                        "; " + newp + " seen instead");

            String s = ((NumberFormat)fmt).format(0);
            if (!s.equals(num[i]))
            {
                errln("FAIL: Pattern " + pat[i] + " should format zero as " + num[i] +
                        "; " + s + " seen instead");
                logln("Min integer digits = " + fmt.getMinimumIntegerDigits());
            }
            // BigInteger 0 - ticket#4731
            s = ((NumberFormat)fmt).format(BigInteger.ZERO);
            if (!s.equals(num[i]))
            {
                errln("FAIL: Pattern " + pat[i] + " should format BigInteger zero as " + num[i] +
                        "; " + s + " seen instead");
                logln("Min integer digits = " + fmt.getMinimumIntegerDigits());
            }
        }
    }

    @Test
    public void Test20186_SpacesAroundSemicolon() {
        DecimalFormat df = new DecimalFormat("0.00 ; -0.00");
        expect2(df, 1, "1.00 ");
        expect2(df, -1, " -1.00");

        df = new DecimalFormat("0.00;");
        expect2(df, 1, "1.00");
        expect2(df, -1, "-1.00");

        df = new DecimalFormat("0.00;0.00");
        expect2(df, 1, "1.00");
        expect(df, -1, "1.00");  // parses as 1, not -1

        df = new DecimalFormat(" 0.00 ; -0.00 ");
        expect2(df, 1, " 1.00 ");
        expect2(df, -1, " -1.00 ");
    }

    // Test exponential pattern
    @Test
    public void TestExponential() {

        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        final String pat[] = { "0.####E0", "00.000E00", "##0.######E000", "0.###E0;[0.###E0]" };
        int pat_length = pat.length;

        double val[] = { 0.01234, 123456789, 1.23e300, -3.141592653e-271 };
        int val_length = val.length;
        final String valFormat[] = {
                // 0.####E0
                "1.234E-2", "1.2346E8", "1.23E300", "-3.1416E-271",
                // 00.000E00
                "12.340E-03", "12.346E07", "12.300E299", "-31.416E-272",
                // ##0.######E000
                "12.34E-003", "123.4568E006", "1.23E300", "-314.1593E-273",
                // 0.###E0;[0.###E0]
                "1.234E-2", "1.235E8", "1.23E300", "[3.142E-271]" };
        /*double valParse[] =
            {
                0.01234, 123460000, 1.23E300, -3.1416E-271,
                0.01234, 123460000, 1.23E300, -3.1416E-271,
                0.01234, 123456800, 1.23E300, -3.141593E-271,
                0.01234, 123500000, 1.23E300, -3.142E-271,
            };*/ //The variable is never used

        int lval[] = { 0, -1, 1, 123456789 };
        int lval_length = lval.length;
        final String lvalFormat[] = {
                // 0.####E0
                "0E0", "-1E0", "1E0", "1.2346E8",
                // 00.000E00
                "00.000E00", "-10.000E-01", "10.000E-01", "12.346E07",
                // ##0.######E000
                "0E000", "-1E000", "1E000", "123.4568E006",
                // 0.###E0;[0.###E0]
                "0E0", "[1E0]", "1E0", "1.235E8" };
        int lvalParse[] =
            {
                0, -1, 1, 123460000,
                0, -1, 1, 123460000,
                0, -1, 1, 123456800,
                0, -1, 1, 123500000,
            };
        int ival = 0, ilval = 0;
        for (int p = 0; p < pat_length; ++p) {
            DecimalFormat fmt = new DecimalFormat(pat[p], sym);
            logln("Pattern \"" + pat[p] + "\" -toPattern-> \"" + fmt.toPattern() + "\"");
            int v;
            for (v = 0; v < val_length; ++v) {
                String s;
                s = ((NumberFormat) fmt).format(val[v]);
                logln(" " + val[v] + " -format-> " + s);
                if (!s.equals(valFormat[v + ival]))
                    errln("FAIL: Expected " + valFormat[v + ival]);

                ParsePosition pos = new ParsePosition(0);
                double a = fmt.parse(s, pos).doubleValue();
                if (pos.getIndex() == s.length()) {
                    logln("  -parse-> " + Double.toString(a));
                    // Use epsilon comparison as necessary
                } else
                    errln("FAIL: Partial parse (" + pos.getIndex() + " chars) -> " + a);
            }
            for (v = 0; v < lval_length; ++v) {
                String s;
                s = ((NumberFormat) fmt).format(lval[v]);
                logln(" " + lval[v] + "L -format-> " + s);
                if (!s.equals(lvalFormat[v + ilval]))
                    errln("ERROR: Expected " + lvalFormat[v + ilval] + " Got: " + s);

                ParsePosition pos = new ParsePosition(0);
                long a = 0;
                Number A = fmt.parse(s, pos);
                if (A != null) {
                    a = A.longValue();
                    if (pos.getIndex() == s.length()) {
                        logln("  -parse-> " + a);
                        if (a != lvalParse[v + ilval])
                            errln("FAIL: Expected " + lvalParse[v + ilval]);
                    } else
                        errln("FAIL: Partial parse (" + pos.getIndex() + " chars) -> " + Long.toString(a));
                } else {
                    errln("Fail to parse the string: " + s);
                }
            }
            ival += val_length;
            ilval += lval_length;
        }
    }

    // Test the handling of quotes
    @Test
    public void TestQuotes() {

        StringBuffer pat;
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        pat = new StringBuffer("a'fo''o'b#");
        DecimalFormat fmt = new DecimalFormat(pat.toString(), sym);
        String s = ((NumberFormat)fmt).format(123);
        logln("Pattern \"" + pat + "\"");
        logln(" Format 123 . " + s);
        if (!s.equals("afo'ob123"))
            errln("FAIL: Expected afo'ob123");

        s ="";
        pat = new StringBuffer("a''b#");
        fmt = new DecimalFormat(pat.toString(), sym);
        s = ((NumberFormat)fmt).format(123);
        logln("Pattern \"" + pat + "\"");
        logln(" Format 123 . " + s);
        if (!s.equals("a'b123"))
            errln("FAIL: Expected a'b123");
    }

    @Test
    public void TestParseCurrencyTrailingSymbol() {
        // see sun bug 4709840
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        float val = 12345.67f;
        String str = fmt.format(val);
        logln("val: " + val + " str: " + str);
        try {
            Number num = fmt.parse(str);
            logln("num: " + num);
        } catch (ParseException e) {
            errln("parse of '" + str + "' threw exception: " + e);
        }
    }

    /**
     * Test the handling of the currency symbol in patterns.
     **/
    @Test
    public void TestCurrencySign() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        StringBuffer pat = new StringBuffer("");
        char currency = 0x00A4;
        // "\xA4#,##0.00;-\xA4#,##0.00"
        pat.append(currency).append("#,##0.00;-").append(currency).append("#,##0.00");
        DecimalFormat fmt = new DecimalFormat(pat.toString(), sym);
        String s = ((NumberFormat) fmt).format(1234.56);
        pat = new StringBuffer();
        logln("Pattern \"" + fmt.toPattern() + "\"");
        logln(" Format " + 1234.56 + " . " + s);
        assertEquals("symbol, pos", "$1,234.56", s);

        s = ((NumberFormat) fmt).format(-1234.56);
        logln(" Format " + Double.toString(-1234.56) + " . " + s);
        assertEquals("symbol, neg", "-$1,234.56", s);

        pat.setLength(0);
        // "\xA4\xA4 #,##0.00;\xA4\xA4 -#,##0.00"
        pat.append(currency).append(currency).append(" #,##0.00;").append(currency).append(currency).append(" -#,##0.00");
        fmt = new DecimalFormat(pat.toString(), sym);
        s = ((NumberFormat) fmt).format(1234.56);
        logln("Pattern \"" + fmt.toPattern() + "\"");
        logln(" Format " + Double.toString(1234.56) + " . " + s);
        assertEquals("name, pos", "USD 1,234.56", s);

        s = ((NumberFormat) fmt).format(-1234.56);
        logln(" Format " + Double.toString(-1234.56) + " . " + s);
        assertEquals("name, neg", "USD -1,234.56", s);
    }

    @Test
    public void TestSpaceParsing() {
        // the data are:
        // the string to be parsed, parsed position, parsed error index
        String[][] DATA = {
                {"$124", "4", "-1"},
                {"$124 $124", "4", "-1"},
                {"$124 ", "4", "-1"},
                {"$124  ", "4", "-1"},
                {"$ 124 ", "5", "-1"},
                {"$\u00A0124 ", "5", "-1"},
                {" $ 124 ", "6", "-1"},
                {"124$", "4", "-1"},
                {"124 $", "5", "-1"},
                {"$124\u200A", "4", "-1"},
                {"$\u200A124", "5", "-1"},
        };
        NumberFormat foo = NumberFormat.getCurrencyInstance();
        for (int i = 0; i < DATA.length; ++i) {
            ParsePosition parsePosition = new ParsePosition(0);
            String stringToBeParsed = DATA[i][0];
            int parsedPosition = Integer.parseInt(DATA[i][1]);
            int errorIndex = Integer.parseInt(DATA[i][2]);
            try {
                Number result = foo.parse(stringToBeParsed, parsePosition);
                if (parsePosition.getIndex() != parsedPosition ||
                        parsePosition.getErrorIndex() != errorIndex) {
                    errln("FAILED parse " + stringToBeParsed + "; parse position: " + parsePosition.getIndex() + "; error position: " + parsePosition.getErrorIndex());
                }
                if (parsePosition.getErrorIndex() == -1 &&
                        result.doubleValue() != 124) {
                    errln("FAILED parse " + stringToBeParsed + "; value " + result.doubleValue());
                }
            } catch (Exception e) {
                errln("FAILED " + e.toString());
            }
        }
    }

    @Test
    public void TestSpaceParsingStrict() {
        // All trailing grouping separators should be ignored in strict mode, not just the first.
        Object[][] cases = {
                {"123 ", 3, -1},
                {"123  ", 3, -1},
                {"123  ,", 3, -1},
                {"123,", 3, -1},
                {"123, ", 3, -1},
                {"123,,", 3, -1},
                {"123,, ", 3, -1},
                {"123,,456", 3, -1},
                {"123 ,", 3, -1},
                {"123, ", 3, -1},
                {"123, 456", 3, -1},
                {"123  456", 3, -1}
        };
        DecimalFormat df = new DecimalFormat("#,###");
        df.setParseStrict(true);
        for (Object[] cas : cases) {
            String input = (String) cas[0];
            int expectedIndex = (Integer) cas[1];
            int expectedErrorIndex = (Integer) cas[2];
            ParsePosition ppos = new ParsePosition(0);
            df.parse(input, ppos);
            assertEquals("Failed on index: '" + input + "'", expectedIndex, ppos.getIndex());
            assertEquals("Failed on error: '" + input + "'", expectedErrorIndex, ppos.getErrorIndex());
        }
    }

    @Test
    public void TestMultiCurrencySign() {
        String[][] DATA = {
                // the fields in the following test are:
                // locale,
                // currency pattern (with negative pattern),
                // currency number to be formatted,
                // currency format using currency symbol name, such as "$" for USD,
                // currency format using currency ISO name, such as "USD",
                // currency format using plural name, such as "US dollars".
                // for US locale
                {"en_US", "\u00A4#,##0.00;-\u00A4#,##0.00", "1234.56", "$1,234.56", "USD 1,234.56", "US dollars 1,234.56"},
                {"en_US", "\u00A4#,##0.00;-\u00A4#,##0.00", "-1234.56", "-$1,234.56", "-USD 1,234.56", "-US dollars 1,234.56"},
                {"en_US", "\u00A4#,##0.00;-\u00A4#,##0.00", "1", "$1.00", "USD 1.00", "US dollars 1.00"},
                // for CHINA locale
                {"zh_CN", "\u00A4#,##0.00;(\u00A4#,##0.00)", "1234.56", "\u00A51,234.56", "CNY 1,234.56", "\u4EBA\u6C11\u5E01 1,234.56"},
                {"zh_CN", "\u00A4#,##0.00;(\u00A4#,##0.00)", "-1234.56", "(\u00A51,234.56)", "(CNY 1,234.56)", "(\u4EBA\u6C11\u5E01 1,234.56)"},
                {"zh_CN", "\u00A4#,##0.00;(\u00A4#,##0.00)", "1", "\u00A51.00", "CNY 1.00", "\u4EBA\u6C11\u5E01 1.00"}
        };

        String doubleCurrencyStr = "\u00A4\u00A4";
        String tripleCurrencyStr = "\u00A4\u00A4\u00A4";

        for (int i=0; i<DATA.length; ++i) {
            String locale = DATA[i][0];
            String pat = DATA[i][1];
            Double numberToBeFormat = new Double(DATA[i][2]);
            DecimalFormatSymbols sym = new DecimalFormatSymbols(new ULocale(locale));
            for (int j=1; j<=3; ++j) {
                // j represents the number of currency sign in the pattern.
                if (j == 2) {
                    pat = pat.replaceAll("\u00A4", doubleCurrencyStr);
                } else if (j == 3) {
                    pat = pat.replaceAll("\u00A4\u00A4", tripleCurrencyStr);
                }
                DecimalFormat fmt = new DecimalFormat(pat, sym);
                String s = ((NumberFormat) fmt).format(numberToBeFormat);
                // DATA[i][3] is the currency format result using a
                // single currency sign.
                // DATA[i][4] is the currency format result using
                // double currency sign.
                // DATA[i][5] is the currency format result using
                // triple currency sign.
                // DATA[i][j+2] is the currency format result using
                // 'j' number of currency sign.
                String currencyFormatResult = DATA[i][2+j];
                if (!s.equals(currencyFormatResult)) {
                    errln("FAIL format: Expected " + currencyFormatResult + " but got " + s);
                }
                try {
                    // mix style parsing
                    for (int k=3; k<=4; ++k) {
                        // DATA[i][3] is the currency format result using a
                        // single currency sign.
                        // DATA[i][4] is the currency format result using
                        // double currency sign.
                        // DATA[i][5] is the currency format result using
                        // triple currency sign.
                        // ICU 59: long name parsing requires currency mode.
                        String oneCurrencyFormat = DATA[i][k];
                        if (fmt.parse(oneCurrencyFormat).doubleValue() !=
                                numberToBeFormat.doubleValue()) {
                            errln("FAILED parse " + oneCurrencyFormat);
                        }
                    }
                } catch (ParseException e) {
                    errln("FAILED, DecimalFormat parse currency: " + e.toString());
                }
            }
        }
    }

    @Test
    public void TestCurrencyFormatForMixParsing() {
        MeasureFormat curFmt = MeasureFormat.getCurrencyFormat(new ULocale("en_US"));
        String[] formats = {
                "$1,234.56",  // string to be parsed
                "USD1,234.56",
                "US dollars1,234.56",
                "1,234.56 US dollars"
        };
        try {
            for (int i = 0; i < formats.length; ++i) {
                String stringToBeParsed = formats[i];
                CurrencyAmount parsedVal = (CurrencyAmount)curFmt.parseObject(stringToBeParsed);
                Number val = parsedVal.getNumber();
                if (!val.equals(new BigDecimal("1234.56"))) {
                    errln("FAIL: getCurrencyFormat of default locale (en_US) failed roundtripping the number. val=" + val);
                }
                if (!parsedVal.getCurrency().equals(Currency.getInstance("USD"))) {
                    errln("FAIL: getCurrencyFormat of default locale (en_US) failed roundtripping the currency");
                }
            }
        } catch (ParseException e) {
            errln("parse FAILED: " + e.toString());
        }
    }


    /** Starting in ICU 62, strict mode is actually strict with currency formats. */
    @Test
    public void TestMismatchedCurrencyFormatFail() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(ULocale.ENGLISH);
        assertEquals("Test assumes that currency sign is at the beginning",
                "\u00A4#,##0.00",
                df.toPattern());
        // Should round-trip on the correct currency format:
        expect2(df, 1.23, "\u00A41.23");
        df.setCurrency(Currency.getInstance("EUR"));
        expect2(df, 1.23, "\u20AC1.23");
        // Should parse with currency in the wrong place in lenient mode
        df.setParseStrict(false);
        expect(df, "1.23\u20AC", 1.23);
        expectParseCurrency(df, Currency.getInstance("EUR"), "1.23\u20AC");
        // Should NOT parse with currency in the wrong place in STRICT mode
        df.setParseStrict(true);
        {
            ParsePosition ppos = new ParsePosition(0);
            df.parse("1.23\u20AC", ppos);
            assertEquals("Should fail to parse", 0, ppos.getIndex());
        }
        {
            ParsePosition ppos = new ParsePosition(0);
            df.parseCurrency("1.23\u20AC", ppos);
            assertEquals("Should fail to parse currency", 0, ppos.getIndex());
        }
    }

    @Test
    public void TestDecimalFormatCurrencyParse() {
        // Locale.US
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        StringBuffer pat = new StringBuffer("");
        char currency = 0x00A4;
        // "\xA4#,##0.00;-\xA4#,##0.00"
        pat.append(currency).append(currency).append(currency).append("#,##0.00;-").append(currency).append(currency).append(currency).append("#,##0.00");
        DecimalFormat fmt = new DecimalFormat(pat.toString(), sym);
        String[][] DATA = {
                // the data are:
                // string to be parsed, the parsed result (number)
                {"$1.00", "1"},
                {"USD1.00", "1"},
                {"1.00 US dollar", "1"},
                {"$1,234.56", "1234.56"},
                {"USD1,234.56", "1234.56"},
                {"1,234.56 US dollar", "1234.56"},
        };
        try {
            for (int i = 0; i < DATA.length; ++i) {
                String stringToBeParsed = DATA[i][0];
                double parsedResult = Double.parseDouble(DATA[i][1]);
                Number num = fmt.parse(stringToBeParsed);
                if (num.doubleValue() != parsedResult) {
                    errln("FAIL parse: Expected " + parsedResult);
                }
            }
        } catch (ParseException e) {
            errln("FAILED, DecimalFormat parse currency: " + e.toString());
        }
    }

    /**
     * Test localized currency patterns.
     */
    @Test
    public void TestCurrency() {
        String[] DATA = {
                "fr_CA", "1,50\u00a0$",
                "de_DE", "1,50\u00a0\u20AC",
                "de_DE@currency=DEM", "1,50\u00a0DM",
                "fr_FR", "1,50\u00a0\u20AC",
                "fr_FR@currency=FRF", "1,50\u00a0F",
        };

        for (int i=0; i<DATA.length; i+=2) {
            Locale locale = new Locale(DATA[i]);
            NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
            String s = fmt.format(1.50);
            if (s.equals(DATA[i+1])) {
                logln("Ok: 1.50 x " + locale + " => " + s);
            } else {
                logln("FAIL: 1.50 x " + locale + " => " + s +
                        ", expected " + DATA[i+1]);
            }
        }

        // format currency with CurrencyAmount
        for (int i=0; i<DATA.length; i+=2) {
            Locale locale = new Locale(DATA[i]);

            Currency curr = Currency.getInstance(locale);
            logln("\nName of the currency is: " + curr.getName(locale, Currency.LONG_NAME, new boolean[] {false}));
            CurrencyAmount cAmt = new CurrencyAmount(1.5, curr);
            logln("CurrencyAmount object's hashCode is: " + cAmt.hashCode()); //cover hashCode

            NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);
            String sCurr = fmt.format(cAmt);
            if (sCurr.equals(DATA[i+1])) {
                logln("Ok: 1.50 x " + locale + " => " + sCurr);
            } else {
                errln("FAIL: 1.50 x " + locale + " => " + sCurr +
                        ", expected " + DATA[i+1]);
            }
        }

        //Cover MeasureFormat.getCurrencyFormat()
        ULocale save = ULocale.getDefault();
        ULocale.setDefault(ULocale.US);
        MeasureFormat curFmt = MeasureFormat.getCurrencyFormat();
        String strBuf = curFmt.format(new CurrencyAmount(new Float(1234.56), Currency.getInstance("USD")));

        try {
            CurrencyAmount parsedVal = (CurrencyAmount)curFmt.parseObject(strBuf);
            Number val = parsedVal.getNumber();
            if (!val.equals(new BigDecimal("1234.56"))) {
                errln("FAIL: getCurrencyFormat of default locale (en_US) failed roundtripping the number. val=" + val);
            }
            if (!parsedVal.getCurrency().equals(Currency.getInstance("USD"))) {
                errln("FAIL: getCurrencyFormat of default locale (en_US) failed roundtripping the currency");
            }
        }
        catch (ParseException e) {
            errln("FAIL: " + e.getMessage());
        }
        ULocale.setDefault(save);
    }

    @Test
    public void TestJavaCurrencyConversion() {
        java.util.Currency gbpJava = java.util.Currency.getInstance("GBP");
        Currency gbpIcu = Currency.getInstance("GBP");
        assertEquals("ICU should equal API value", gbpIcu, Currency.fromJavaCurrency(gbpJava));
        assertEquals("Java should equal API value", gbpJava, gbpIcu.toJavaCurrency());
        // Test CurrencyAmount constructors
        CurrencyAmount ca1 = new CurrencyAmount(123.45, gbpJava);
        CurrencyAmount ca2 = new CurrencyAmount(123.45, gbpIcu);
        assertEquals("CurrencyAmount from both Double constructors should be equal", ca1, ca2);
        // Coverage for the Number constructor
        ca1 = new CurrencyAmount(new BigDecimal("543.21"), gbpJava);
        ca2 = new CurrencyAmount(new BigDecimal("543.21"), gbpIcu);
        assertEquals("CurrencyAmount from both Number constructors should be equal", ca1, ca2);
    }

    @Test
    public void TestCurrencyIsoPluralFormat() {
        String[][] DATA = {
                // the data are:
                // locale,
                // currency amount to be formatted,
                // currency ISO code to be formatted,
                // format result using CURRENCYSTYLE,
                // format result using ISOCURRENCYSTYLE,
                // format result using PLURALCURRENCYSTYLE,
                {"en_US", "1", "USD", "$1.00", "USD 1.00", "1.00 US dollars"},
                {"en_US", "1234.56", "USD", "$1,234.56", "USD 1,234.56", "1,234.56 US dollars"},
                {"en_US", "-1234.56", "USD", "-$1,234.56", "-USD 1,234.56", "-1,234.56 US dollars"},
                {"zh_CN", "1", "USD", "US$1.00", "USD 1.00", "1.00 美元"},
                {"zh_CN", "1234.56", "USD", "US$1,234.56", "USD 1,234.56", "1,234.56 美元"},
                {"zh_CN", "1", "CNY", "¥1.00", "CNY 1.00", "1.00 人民币"},
                {"zh_CN", "1234.56", "CNY", "¥1,234.56", "CNY 1,234.56", "1,234.56 人民币"},
                {"ru_RU", "1", "RUB", "1,00 \u20BD", "1,00 RUB", "1,00 российского рубля"},
                {"ru_RU", "2", "RUB", "2,00 \u20BD", "2,00 RUB", "2,00 российского рубля"},
                {"ru_RU", "5", "RUB", "5,00 \u20BD", "5,00 RUB", "5,00 российского рубля"},
                // test locale without currency information
                {"root", "-1.23", "USD", "-US$ 1.23", "-USD 1.23", "-1.23 USD"},
                {"root@numbers=latn", "-1.23", "USD", "-US$ 1.23", "-USD 1.23", "-1.23 USD"}, // ensure that the root locale is still used with modifiers
                {"root@numbers=arab", "-1.23", "USD", "\u061C-\u0661\u066B\u0662\u0663\u00A0US$", "\u061C-\u0661\u066B\u0662\u0663\u00A0USD", "\u061C-\u0661\u066B\u0662\u0663 USD"}, // ensure that the root locale is still used with modifiers
                {"es_AR", "1", "INR", "INR\u00A01,00", "INR\u00A01,00", "1,00 rupia india"},
                {"ar_EG", "1", "USD", "١٫٠٠\u00A0US$", "١٫٠٠\u00A0USD", "١٫٠٠ دولار أمريكي"},
        };

        for (int i=0; i<DATA.length; ++i) {
            for (int k = NumberFormat.CURRENCYSTYLE;
                    k <= NumberFormat.PLURALCURRENCYSTYLE;
                    ++k) {
                // k represents currency format style.
                if ( k != NumberFormat.CURRENCYSTYLE &&
                        k != NumberFormat.ISOCURRENCYSTYLE &&
                        k != NumberFormat.PLURALCURRENCYSTYLE ) {
                    continue;
                }
                String localeString = DATA[i][0];
                Double numberToBeFormat = new Double(DATA[i][1]);
                String currencyISOCode = DATA[i][2];
                ULocale locale = new ULocale(localeString);
                NumberFormat numFmt = NumberFormat.getInstance(locale, k);
                numFmt.setCurrency(Currency.getInstance(currencyISOCode));
                String strBuf = numFmt.format(numberToBeFormat);
                int resultDataIndex = k-1;
                if ( k == NumberFormat.CURRENCYSTYLE ) {
                    resultDataIndex = k+2;
                }
                // DATA[i][resultDataIndex] is the currency format result
                // using 'k' currency style.
                String formatResult = DATA[i][resultDataIndex];
                if (!strBuf.equals(formatResult)) {
                    errln("FAIL: localeID: " + localeString + ", expected(" + formatResult.length() + "): \"" + formatResult + "\", actual(" + strBuf.length() + "): \"" + strBuf + "\"");
                }
                // test parsing, and test parsing for all currency formats.
                for (int j = 3; j < 6; ++j) {
                    // DATA[i][3] is the currency format result using
                    // CURRENCYSTYLE formatter.
                    // DATA[i][4] is the currency format result using
                    // ISOCURRENCYSTYLE formatter.
                    // DATA[i][5] is the currency format result using
                    // PLURALCURRENCYSTYLE formatter.
                    String oneCurrencyFormatResult = DATA[i][j];
                    CurrencyAmount val = numFmt.parseCurrency(oneCurrencyFormatResult, null);
                    if (val.getNumber().doubleValue() != numberToBeFormat.doubleValue()) {
                        errln("FAIL: getCurrencyFormat of locale " + localeString + " failed roundtripping the number. val=" + val + "; expected: " + numberToBeFormat);
                    }
                }
            }
        }
    }


    @Test
    public void TestMiscCurrencyParsing() {
        String[][] DATA = {
                // each has: string to be parsed, parsed position, error position
                {"1.00 ", "4", "-1", "0", "4"},
                {"1.00 UAE dirha", "4", "-1", "0", "4"},
                {"1.00 us dollar", "14", "-1", "14", "-1"},
                {"1.00 US DOLLAR", "14", "-1", "14", "-1"},
                {"1.00 usd", "8", "-1", "8", "-1"},
                {"1.00 USD", "8", "-1", "8", "-1"},
        };
        ULocale locale = new ULocale("en_US");
        for (int i=0; i<DATA.length; ++i) {
            String stringToBeParsed = DATA[i][0];
            int parsedPosition = Integer.parseInt(DATA[i][1]);
            int errorIndex = Integer.parseInt(DATA[i][2]);
            int currParsedPosition = Integer.parseInt(DATA[i][3]);
            int currErrorIndex = Integer.parseInt(DATA[i][4]);
            NumberFormat numFmt = NumberFormat.getInstance(locale, NumberFormat.CURRENCYSTYLE);
            ParsePosition parsePosition = new ParsePosition(0);
            Number val = numFmt.parse(stringToBeParsed, parsePosition);
            if (parsePosition.getIndex() != parsedPosition ||
                    parsePosition.getErrorIndex() != errorIndex) {
                errln("FAIL: parse failed on case "+i+". expected position: " + parsedPosition +"; actual: " + parsePosition.getIndex());
                errln("FAIL: parse failed on case "+i+". expected error position: " + errorIndex + "; actual: " + parsePosition.getErrorIndex());
            }
            if (parsePosition.getErrorIndex() == -1 &&
                    val.doubleValue() != 1.00) {
                errln("FAIL: parse failed. expected 1.00, actual:" + val);
            }
            parsePosition = new ParsePosition(0);
            CurrencyAmount amt = numFmt.parseCurrency(stringToBeParsed, parsePosition);
            if (parsePosition.getIndex() != currParsedPosition ||
                    parsePosition.getErrorIndex() != currErrorIndex) {
                errln("FAIL: parseCurrency failed on case "+i+". expected error position: " + currErrorIndex + "; actual: " + parsePosition.getErrorIndex());
                errln("FAIL: parseCurrency failed on case "+i+". expected position: " + currParsedPosition +"; actual: " + parsePosition.getIndex());
            }
            if (parsePosition.getErrorIndex() == -1 &&
                    amt.getNumber().doubleValue() != 1.00) {
                errln("FAIL: parseCurrency failed. expected 1.00, actual:" + val);
            }
        }
    }

    @Test
    public void TestParseCurrency() {
        class ParseCurrencyItem {
            private final String localeString;
            private final String descrip;
            private final String currStr;
            private final int    doubExpectPos;
            private final int    doubExpectVal;
            private final int    curExpectPos;
            private final int    curExpectVal;
            private final String curExpectCurr;

            ParseCurrencyItem(String locStr, String desc, String curr, int doubExPos, int doubExVal, int curExPos, int curExVal, String curExCurr) {
                localeString  = locStr;
                descrip       = desc;
                currStr       = curr;
                doubExpectPos  = doubExPos;
                doubExpectVal  = doubExVal;
                curExpectPos  = curExPos;
                curExpectVal  = curExVal;
                curExpectCurr = curExCurr;
            }
            public String getLocaleString()  { return localeString; }
            public String getDescrip()       { return descrip; }
            public String getCurrStr()       { return currStr; }
            public int    getDoubExpectPos()  { return doubExpectPos; }
            public int    getDoubExpectVal()  { return doubExpectVal; }
            public int    getCurExpectPos()  { return curExpectPos; }
            public int    getCurExpectVal()  { return curExpectVal; }
            public String getCurExpectCurr() { return curExpectCurr; }
        }
        // Note: In cases where the number occurs before the currency sign, non-currency mode will parse the number
        // and stop when it reaches the currency symbol.
        final ParseCurrencyItem[] parseCurrencyItems = {
                new ParseCurrencyItem( "en_US", "dollars2", "$2.00",            5,  2,  5,  2,  "USD" ),
                new ParseCurrencyItem( "en_US", "dollars4", "$4",               2,  4,  2,  4,  "USD" ),
                new ParseCurrencyItem( "en_US", "dollars9", "9\u00A0$",         3,  9,  3,  9,  "USD" ),
                new ParseCurrencyItem( "en_US", "pounds3",  "\u00A33.00",       0,  0,  5,  3,  "GBP" ),
                new ParseCurrencyItem( "en_US", "pounds5",  "\u00A35",          0,  0,  2,  5,  "GBP" ),
                new ParseCurrencyItem( "en_US", "pounds7",  "7\u00A0\u00A3",    1,  7,  3,  7,  "GBP" ),
                new ParseCurrencyItem( "en_US", "euros8",   "\u20AC8",          0,  0,  2,  8,  "EUR" ),

                new ParseCurrencyItem( "en_GB", "pounds3",  "\u00A33.00",       5,  3,  5,  3,  "GBP" ),
                new ParseCurrencyItem( "en_GB", "pounds5",  "\u00A35",          2,  5,  2,  5,  "GBP" ),
                new ParseCurrencyItem( "en_GB", "pounds7",  "7\u00A0\u00A3",    3,  7,  3,  7,  "GBP" ),
                new ParseCurrencyItem( "en_GB", "euros4",   "4,00\u00A0\u20AC", 4,400,  6,400,  "EUR" ),
                new ParseCurrencyItem( "en_GB", "euros6",   "6\u00A0\u20AC",    1,  6,  3,  6,  "EUR" ),
                new ParseCurrencyItem( "en_GB", "euros8",   "\u20AC8",          0,  0,  2,  8,  "EUR" ),
                new ParseCurrencyItem( "en_GB", "dollars4", "US$4",             0,  0,  4,  4,  "USD" ),

                new ParseCurrencyItem( "fr_FR", "euros4",   "4,00\u00A0\u20AC", 6,  4,  6,  4,  "EUR" ),
                new ParseCurrencyItem( "fr_FR", "euros6",   "6\u00A0\u20AC",    3,  6,  3,  6,  "EUR" ),
                new ParseCurrencyItem( "fr_FR", "euros8",   "\u20AC8",          2,  8,  2,  8,  "EUR" ),
                new ParseCurrencyItem( "fr_FR", "dollars2", "$2.00",            0,  0,  0,  0,  ""    ),
                new ParseCurrencyItem( "fr_FR", "dollars4", "$4",               0,  0,  0,  0,  ""    ),
        };
        for (ParseCurrencyItem item: parseCurrencyItems) {
            String localeString = item.getLocaleString();
            ULocale uloc = new ULocale(localeString);
            NumberFormat fmt = null;
            try {
                fmt = NumberFormat.getCurrencyInstance(uloc);
            } catch (Exception e) {
                errln("NumberFormat.getCurrencyInstance fails for locale " + localeString);
                continue;
            }
            String currStr = item.getCurrStr();
            ParsePosition parsePos = new ParsePosition(0);

            Number numVal = fmt.parse(currStr, parsePos);
            if ( parsePos.getIndex() != item.getDoubExpectPos() || (numVal != null && numVal.intValue() != item.getDoubExpectVal()) ) {
                if (numVal != null) {
                    errln("NumberFormat.getCurrencyInstance parse " + localeString + "/" + item.getDescrip() +
                            ", expect pos/val " + item.getDoubExpectPos() + "/" + item.getDoubExpectVal() +
                            ", get " + parsePos.getIndex() + "/" + numVal.intValue() );
                } else {
                    errln("NumberFormat.getCurrencyInstance parse " + localeString + "/" + item.getDescrip() +
                            ", expect pos/val " + item.getDoubExpectPos() + "/" + item.getDoubExpectVal() +
                            ", get " + parsePos.getIndex() + "/(NULL)" );
                }
            }

            parsePos.setIndex(0);
            int curExpectPos = item.getCurExpectPos();
            CurrencyAmount currAmt = fmt.parseCurrency(currStr, parsePos);
            if ( parsePos.getIndex() != curExpectPos || (currAmt != null && (currAmt.getNumber().intValue() != item.getCurExpectVal() ||
                    currAmt.getCurrency().getCurrencyCode().compareTo(item.getCurExpectCurr()) != 0)) ) {
                if (currAmt != null) {
                    errln("NumberFormat.getCurrencyInstance parseCurrency " + localeString + "/" + item.getDescrip() +
                            ", expect pos/val/curr " + curExpectPos + "/" + item.getCurExpectVal() + "/" + item.getCurExpectCurr() +
                            ", get " + parsePos.getIndex() + "/" + currAmt.getNumber().intValue() + "/" + currAmt.getCurrency().getCurrencyCode() );
                } else {
                    errln("NumberFormat.getCurrencyInstance parseCurrency " + localeString + "/" + item.getDescrip() +
                            ", expect pos/val/curr " + curExpectPos + "/" + item.getCurExpectVal() + "/" + item.getCurExpectCurr() +
                            ", get " + parsePos.getIndex() + "/(NULL)" );
                }
            }
        }
    }

    @Test
    public void TestParseCurrencyWithWhitespace() {
        DecimalFormat df = new DecimalFormat("#,##0.00 ¤¤");
        ParsePosition ppos = new ParsePosition(0);
        df.parseCurrency("1.00 us denmark", ppos);
        assertEquals("Expected to fail on 'us denmark' string", 4, ppos.getErrorIndex());
    }

    @Test
    public void TestParseCurrPatternWithDecStyle() {
        String currpat = "¤#,##0.00";
        String parsetxt = "x0y$";
        DecimalFormat decfmt = (DecimalFormat)NumberFormat.getInstance(new ULocale("en_US"), NumberFormat.NUMBERSTYLE);
        decfmt.applyPattern(currpat);
        ParsePosition ppos = new ParsePosition(0);
        Number value = decfmt.parse(parsetxt, ppos);
        if (ppos.getIndex() != 0) {
            errln("DecimalFormat.parse expected to fail but got ppos " + ppos.getIndex() + ", value " + value);
        }
    }

    /**
     * Test the Currency object handling, new as of ICU 2.2.
     */
    @Test
    public void TestCurrencyObject() {
        NumberFormat fmt =
                NumberFormat.getCurrencyInstance(Locale.US);

        expectCurrency(fmt, null, 1234.56, "$1,234.56");

        expectCurrency(fmt, Currency.getInstance(Locale.FRANCE),
                1234.56, "\u20AC1,234.56"); // Euro

        expectCurrency(fmt, Currency.getInstance(Locale.JAPAN),
                1234.56, "\u00A51,235"); // Yen

        expectCurrency(fmt, Currency.getInstance(new Locale("fr", "CH", "")),
                1234.56, "CHF 1,234.56"); // no more 0.05 rounding here, see cldrbug 5548

        expectCurrency(fmt, Currency.getInstance(Locale.US),
                1234.56, "$1,234.56");

        fmt = NumberFormat.getCurrencyInstance(Locale.FRANCE);

        expectCurrency(fmt, null, 1234.56, "1\u202F234,56 \u20AC");

        expectCurrency(fmt, Currency.getInstance(Locale.JAPAN),
                1234.56, "1\u202F235 JPY"); // Yen

        expectCurrency(fmt, Currency.getInstance(new Locale("fr", "CH", "")),
                1234.56, "1\u202F234,56 CHF"); // no more rounding here, see cldrbug 5548

        expectCurrency(fmt, Currency.getInstance(Locale.US),
                1234.56, "1\u202F234,56 $US");

        expectCurrency(fmt, Currency.getInstance(Locale.FRANCE),
                1234.56, "1\u202F234,56 \u20AC"); // Euro
    }

    @Test
    public void TestCompatibleCurrencies() {
        NumberFormat fmt =
                NumberFormat.getCurrencyInstance(Locale.US);
        expectParseCurrency(fmt, Currency.getInstance(Locale.JAPAN), "\u00A51,235"); // Yen half-width
        expectParseCurrency(fmt, Currency.getInstance(Locale.JAPAN), "\uFFE51,235"); // Yen full-wdith
    }

    @Test
    public void TestCurrencyPatterns() {
        int i;
        Random rnd = new Random(2017);
        Locale[] locs = NumberFormat.getAvailableLocales();
        for (i=0; i<locs.length; ++i) {
            if (rnd.nextDouble() < 0.9) {
                // Check a random subset for speed:
                // Otherwise, this test takes a large fraction of the entire time.
                continue;
            }
            NumberFormat nf = NumberFormat.getCurrencyInstance(locs[i]);
            // Make sure currency formats do not have a variable number
            // of fraction digits
            int min = nf.getMinimumFractionDigits();
            int max = nf.getMaximumFractionDigits();
            if (min != max) {
                String a = nf.format(1.0);
                String b = nf.format(1.125);
                errln("FAIL: " + locs[i] +
                        " min fraction digits != max fraction digits; "+
                        "x 1.0 => " + a +
                        "; x 1.125 => " + b);
            }

            // Make sure EURO currency formats have exactly 2 fraction digits
            if (nf instanceof DecimalFormat) {
                Currency curr = ((DecimalFormat) nf).getCurrency();
                if (curr != null && "EUR".equals(curr.getCurrencyCode())) {
                    if (min != 2 || max != 2) {
                        String a = nf.format(1.0);
                        errln("FAIL: " + locs[i] +
                                " is a EURO format but it does not have 2 fraction digits; "+
                                "x 1.0 => " +
                                a);
                    }
                }
            }
        }
    }

    /**
     * Do rudimentary testing of parsing.
     */
    @Test
    public void TestParse() {
        String arg = "0.0";
        DecimalFormat format = new DecimalFormat("00");
        double aNumber = 0l;
        try {
            aNumber = format.parse(arg).doubleValue();
        } catch (ParseException e) {
            System.out.println(e);
        }
        logln("parse(" + arg + ") = " + aNumber);
    }

    /**
     * Test proper rounding by the format method.
     */
    @Test
    public void TestRounding487() {

        NumberFormat nf = NumberFormat.getInstance();
        roundingTest(nf, 0.00159999, 4, "0.0016");
        roundingTest(nf, 0.00995, 4, "0.01");

        roundingTest(nf, 12.3995, 3, "12.4");

        roundingTest(nf, 12.4999, 0, "12");
        roundingTest(nf, - 19.5, 0, "-20");

    }

    /**
     * Test the functioning of the secondary grouping value.
     */
    @Test
    public void TestSecondaryGrouping() {

        DecimalFormatSymbols US = new DecimalFormatSymbols(Locale.US);
        DecimalFormat f = new DecimalFormat("#,##,###", US);

        expect(f, 123456789L, "12,34,56,789");
        expectPat(f, "#,##,##0");
        f.applyPattern("#,###");

        f.setSecondaryGroupingSize(4);
        expect(f, 123456789L, "12,3456,789");
        expectPat(f, "#,####,##0");
        NumberFormat g = NumberFormat.getInstance(new Locale("hi", "IN"));

        String out = "";
        long l = 1876543210L;
        out = g.format(l);

        // expect "1,87,65,43,210", but with Hindi digits
        //         01234567890123
        boolean ok = true;
        if (out.length() != 14) {
            ok = false;
        } else {
            for (int i = 0; i < out.length(); ++i) {
                boolean expectGroup = false;
                switch (i) {
                case 1 :
                case 4 :
                case 7 :
                case 10 :
                    expectGroup = true;
                    break;
                }
                // Later -- fix this to get the actual grouping
                // character from the resource bundle.
                boolean isGroup = (out.charAt(i) == 0x002C);
                if (isGroup != expectGroup) {
                    ok = false;
                    break;
                }
            }
        }
        if (!ok) {
            errln("FAIL  Expected "+ l + " x hi_IN . \"1,87,65,43,210\" (with Hindi digits), got \""
                    + out + "\"");
        } else {
            logln("Ok    " + l + " x hi_IN . \"" + out + "\"");
        }
    }

    /*
     * Internal test utility.
     */
    private void roundingTest(NumberFormat nf, double x, int maxFractionDigits, final String expected) {
        nf.setMaximumFractionDigits(maxFractionDigits);
        String out = nf.format(x);
        logln(x + " formats with " + maxFractionDigits + " fractional digits to " + out);
        if (!out.equals(expected))
            errln("FAIL: Expected " + expected);
    }

    /**
     * Upgrade to alphaWorks
     */
    @Test
    public void TestExponent() {
        DecimalFormatSymbols US = new DecimalFormatSymbols(Locale.US);
        DecimalFormat fmt1 = new DecimalFormat("0.###E0", US);
        DecimalFormat fmt2 = new DecimalFormat("0.###E+0", US);
        int n = 1234;
        expect2(fmt1, n, "1.234E3");
        expect2(fmt2, n, "1.234E+3");
        expect(fmt1, "1.234E+3", n); // Either format should parse "E+3"

    }

    /**
     * Upgrade to alphaWorks
     */
    @Test
    public void TestScientific() {

        DecimalFormatSymbols US = new DecimalFormatSymbols(Locale.US);

        // Test pattern round-trip
        final String PAT[] = { "#E0", "0.####E0", "00.000E00", "##0.####E000", "0.###E0;[0.###E0]" };
        int PAT_length = PAT.length;
        int DIGITS[] = {
                // min int, max int, min frac, max frac
                0, 1, 0, 0, // "#E0"
                1, 1, 0, 4, // "0.####E0"
                2, 2, 3, 3, // "00.000E00"
                1, 3, 0, 4, // "##0.####E000"
                1, 1, 0, 3, // "0.###E0;[0.###E0]"
        };
        for (int i = 0; i < PAT_length; ++i) {
            String pat = PAT[i];
            DecimalFormat df = new DecimalFormat(pat, US);
            String pat2 = df.toPattern();
            if (pat.equals(pat2)) {
                logln("Ok   Pattern rt \"" + pat + "\" . \"" + pat2 + "\"");
            } else {
                errln("FAIL Pattern rt \"" + pat + "\" . \"" + pat2 + "\"");
            }
            // Make sure digit counts match what we expect
            if (i == 0) continue; // outputs to 1,1,0,0 since at least one min digit is required.
            if (df.getMinimumIntegerDigits() != DIGITS[4 * i]
                    || df.getMaximumIntegerDigits() != DIGITS[4 * i + 1]
                            || df.getMinimumFractionDigits() != DIGITS[4 * i + 2]
                                    || df.getMaximumFractionDigits() != DIGITS[4 * i + 3]) {
                errln("FAIL \""+ pat+ "\" min/max int; min/max frac = "
                        + df.getMinimumIntegerDigits() + "/"
                        + df.getMaximumIntegerDigits() + ";"
                        + df.getMinimumFractionDigits() + "/"
                        + df.getMaximumFractionDigits() + ", expect "
                        + DIGITS[4 * i] + "/"
                        + DIGITS[4 * i + 1] + ";"
                        + DIGITS[4 * i + 2] + "/"
                        + DIGITS[4 * i + 3]);
            }
        }

        expect2(new DecimalFormat("#E0", US), 12345.0, "1.2345E4");
        expect(new DecimalFormat("0E0", US), 12345.0, "1E4");

        // pattern of NumberFormat.getScientificInstance(Locale.US) = "0.######E0" not "#E0"
        // so result = 1.234568E4 not 1.2345678901E4
        //when the pattern problem is finalized, delete comment mark'//'
        //of the following code
        expect2(NumberFormat.getScientificInstance(Locale.US), 12345.678901, "1.2345678901E4");
        logln("Testing NumberFormat.getScientificInstance(ULocale) ...");
        expect2(NumberFormat.getScientificInstance(ULocale.US), 12345.678901, "1.2345678901E4");

        expect(new DecimalFormat("##0.###E0", US), 12345.0, "12.34E3");
        expect(new DecimalFormat("##0.###E0", US), 12345.00001, "12.35E3");
        expect2(new DecimalFormat("##0.####E0", US), 12345, "12.345E3");

        // pattern of NumberFormat.getScientificInstance(Locale.US) = "0.######E0" not "#E0"
        // so result = 1.234568E4 not 1.2345678901E4
        expect2(NumberFormat.getScientificInstance(Locale.FRANCE), 12345.678901, "1,2345678901E4");
        logln("Testing NumberFormat.getScientificInstance(ULocale) ...");
        expect2(NumberFormat.getScientificInstance(ULocale.FRANCE), 12345.678901, "1,2345678901E4");

        expect(new DecimalFormat("##0.####E0", US), 789.12345e-9, "789.12E-9");
        expect2(new DecimalFormat("##0.####E0", US), 780.e-9, "780E-9");
        expect(new DecimalFormat(".###E0", US), 45678.0, ".457E5");
        expect2(new DecimalFormat(".###E0", US), 0, ".0E0");
        /*
        expect(new DecimalFormat[] { new DecimalFormat("#E0", US),
                                     new DecimalFormat("##E0", US),
                                     new DecimalFormat("####E0", US),
                                     new DecimalFormat("0E0", US),
                                     new DecimalFormat("00E0", US),
                                     new DecimalFormat("000E0", US),
                                   },
               new Long(45678000),
               new String[] { "4.5678E7",
                              "45.678E6",
                              "4567.8E4",
                              "5E7",
                              "46E6",
                              "457E5",
                            }
               );
        !
        ! Unroll this test into individual tests below...
        !
         */
        expect2(new DecimalFormat("#E0", US), 45678000, "4.5678E7");
        expect2(new DecimalFormat("##E0", US), 45678000, "45.678E6");
        expect2(new DecimalFormat("####E0", US), 45678000, "4567.8E4");
        expect(new DecimalFormat("0E0", US), 45678000, "5E7");
        expect(new DecimalFormat("00E0", US), 45678000, "46E6");
        expect(new DecimalFormat("000E0", US), 45678000, "457E5");
        /*
        expect(new DecimalFormat("###E0", US, status),
               new Object[] { new Double(0.0000123), "12.3E-6",
                              new Double(0.000123), "123E-6",
                              new Double(0.00123), "1.23E-3",
                              new Double(0.0123), "12.3E-3",
                              new Double(0.123), "123E-3",
                              new Double(1.23), "1.23E0",
                              new Double(12.3), "12.3E0",
                              new Double(123), "123E0",
                              new Double(1230), "1.23E3",
                             });
        !
        ! Unroll this test into individual tests below...
        !
         */
        expect2(new DecimalFormat("###E0", US), 0.0000123, "12.3E-6");
        expect2(new DecimalFormat("###E0", US), 0.000123, "123E-6");
        expect2(new DecimalFormat("###E0", US), 0.00123, "1.23E-3");
        expect2(new DecimalFormat("###E0", US), 0.0123, "12.3E-3");
        expect2(new DecimalFormat("###E0", US), 0.123, "123E-3");
        expect2(new DecimalFormat("###E0", US), 1.23, "1.23E0");
        expect2(new DecimalFormat("###E0", US), 12.3, "12.3E0");
        expect2(new DecimalFormat("###E0", US), 123.0, "123E0");
        expect2(new DecimalFormat("###E0", US), 1230.0, "1.23E3");
        /*
        expect(new DecimalFormat("0.#E+00", US, status),
               new Object[] { new Double(0.00012), "1.2E-04",
                              new Long(12000),     "1.2E+04",
                             });
        !
        ! Unroll this test into individual tests below...
        !
         */
        expect2(new DecimalFormat("0.#E+00", US), 0.00012, "1.2E-04");
        expect2(new DecimalFormat("0.#E+00", US), 12000, "1.2E+04");
    }

    /**
     * Upgrade to alphaWorks
     */
    @Test
    public void TestPad() {

        DecimalFormatSymbols US = new DecimalFormatSymbols(Locale.US);
        expect2(new DecimalFormat("*^##.##", US), 0, "^^^^0");
        expect2(new DecimalFormat("*^##.##", US), -1.3, "^-1.3");
        expect2(
                new DecimalFormat("##0.0####E0*_ 'g-m/s^2'", US),
                0,
                "0.0E0______ g-m/s^2");
        expect(
                new DecimalFormat("##0.0####E0*_ 'g-m/s^2'", US),
                1.0 / 3,
                "333.333E-3_ g-m/s^2");
        expect2(new DecimalFormat("##0.0####*_ 'g-m/s^2'", US), 0, "0.0______ g-m/s^2");
        expect(
                new DecimalFormat("##0.0####*_ 'g-m/s^2'", US),
                1.0 / 3,
                "0.33333__ g-m/s^2");

        // Test padding before a sign
        final String formatStr = "*x#,###,###,##0.0#;*x(###,###,##0.0#)";
        expect2(new DecimalFormat(formatStr, US), -10, "xxxxxxxxxx(10.0)");
        expect2(new DecimalFormat(formatStr, US), -1000, "xxxxxxx(1,000.0)");
        expect2(new DecimalFormat(formatStr, US), -1000000, "xxx(1,000,000.0)");
        expect2(new DecimalFormat(formatStr, US), -100.37, "xxxxxxxx(100.37)");
        expect2(new DecimalFormat(formatStr, US), -10456.37, "xxxxx(10,456.37)");
        expect2(new DecimalFormat(formatStr, US), -1120456.37, "xx(1,120,456.37)");
        expect2(new DecimalFormat(formatStr, US), -112045600.37, "(112,045,600.37)");
        expect2(new DecimalFormat(formatStr, US), -1252045600.37, "(1,252,045,600.37)");

        expect2(new DecimalFormat(formatStr, US), 10, "xxxxxxxxxxxx10.0");
        expect2(new DecimalFormat(formatStr, US), 1000, "xxxxxxxxx1,000.0");
        expect2(new DecimalFormat(formatStr, US), 1000000, "xxxxx1,000,000.0");
        expect2(new DecimalFormat(formatStr, US), 100.37, "xxxxxxxxxx100.37");
        expect2(new DecimalFormat(formatStr, US), 10456.37, "xxxxxxx10,456.37");
        expect2(new DecimalFormat(formatStr, US), 1120456.37, "xxxx1,120,456.37");
        expect2(new DecimalFormat(formatStr, US), 112045600.37, "xx112,045,600.37");
        expect2(new DecimalFormat(formatStr, US), 10252045600.37, "10,252,045,600.37");

        // Test padding between a sign and a number
        final String formatStr2 = "#,###,###,##0.0#*x;(###,###,##0.0#*x)";
        expect2(new DecimalFormat(formatStr2, US), -10, "(10.0xxxxxxxxxx)");
        expect2(new DecimalFormat(formatStr2, US), -1000, "(1,000.0xxxxxxx)");
        expect2(new DecimalFormat(formatStr2, US), -1000000, "(1,000,000.0xxx)");
        expect2(new DecimalFormat(formatStr2, US), -100.37, "(100.37xxxxxxxx)");
        expect2(new DecimalFormat(formatStr2, US), -10456.37, "(10,456.37xxxxx)");
        expect2(new DecimalFormat(formatStr2, US), -1120456.37, "(1,120,456.37xx)");
        expect2(new DecimalFormat(formatStr2, US), -112045600.37, "(112,045,600.37)");
        expect2(new DecimalFormat(formatStr2, US), -1252045600.37, "(1,252,045,600.37)");

        expect2(new DecimalFormat(formatStr2, US), 10, "10.0xxxxxxxxxxxx");
        expect2(new DecimalFormat(formatStr2, US), 1000, "1,000.0xxxxxxxxx");
        expect2(new DecimalFormat(formatStr2, US), 1000000, "1,000,000.0xxxxx");
        expect2(new DecimalFormat(formatStr2, US), 100.37, "100.37xxxxxxxxxx");
        expect2(new DecimalFormat(formatStr2, US), 10456.37, "10,456.37xxxxxxx");
        expect2(new DecimalFormat(formatStr2, US), 1120456.37, "1,120,456.37xxxx");
        expect2(new DecimalFormat(formatStr2, US), 112045600.37, "112,045,600.37xx");
        expect2(new DecimalFormat(formatStr2, US), 10252045600.37, "10,252,045,600.37");

        //testing the setPadCharacter(UnicodeString) and getPadCharacterString()
        DecimalFormat fmt = new DecimalFormat("#", US);
        char padString = 'P';
        fmt.setPadCharacter(padString);
        expectPad(fmt, "*P##.##", DecimalFormat.PAD_BEFORE_PREFIX, 5, padString);
        fmt.setPadCharacter('^');
        expectPad(fmt, "*^#", DecimalFormat.PAD_BEFORE_PREFIX, 1, '^');
        //commented until implementation is complete
        /*  fmt.setPadCharacter((UnicodeString)"^^^");
          expectPad(fmt, "*^^^#", DecimalFormat.kPadBeforePrefix, 3, (UnicodeString)"^^^");
          padString.remove();
          padString.append((UChar)0x0061);
          padString.append((UChar)0x0302);
          fmt.setPadCharacter(padString);
          UChar patternChars[]={0x002a, 0x0061, 0x0302, 0x0061, 0x0302, 0x0023, 0x0000};
          UnicodeString pattern(patternChars);
          expectPad(fmt, pattern , DecimalFormat.kPadBeforePrefix, 4, padString);
         */

        // Test multi-char padding sequence specified via pattern
        expect2(new DecimalFormat("*'😃'####.00", US), 1.1, "😃😃😃1.10");
    }

    @Test
    public void TestIgnorePadding() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        DecimalFormat fmt = new DecimalFormat("", dfs);
        fmt.setGroupingUsed(false);
        fmt.setFormatWidth(0);
        fmt.setPadCharacter('*');
        fmt.setPadPosition(0);
        fmt.setMinimumIntegerDigits(0);
        fmt.setMaximumIntegerDigits(8);
        fmt.setMinimumFractionDigits(0);
        fmt.setMaximumFractionDigits(0);
        String pattern = fmt.toPattern();
        if (pattern.startsWith("*")) {
            errln("ERROR toPattern result should ignore padding but get \"" + pattern + "\"");
        }
        fmt.applyPattern(pattern);
        String format = fmt.format(24);
        if (!format.equals("24")) {
             errln("ERROR format result expect 24 but get \"" + format + "\"");
       }
    }

    /**
     * Upgrade to alphaWorks
     */
    @Test
    public void TestPatterns2() {
        DecimalFormatSymbols US = new DecimalFormatSymbols(Locale.US);
        DecimalFormat fmt = new DecimalFormat("#", US);

        char hat = 0x005E; /*^*/

        expectPad(fmt, "*^#", DecimalFormat.PAD_BEFORE_PREFIX, 1, hat);
        expectPad(fmt, "$*^#", DecimalFormat.PAD_AFTER_PREFIX, 2, hat);
        expectPad(fmt, "#*^", DecimalFormat.PAD_BEFORE_SUFFIX, 1, hat);
        expectPad(fmt, "#$*^", DecimalFormat.PAD_AFTER_SUFFIX, 2, hat);
        expectPad(fmt, "$*^$#", -1);
        expectPad(fmt, "#$*^$", -1);
        expectPad(fmt, "'pre'#,##0*x'post'", DecimalFormat.PAD_BEFORE_SUFFIX, 12, (char) 0x0078 /*x*/);
        expectPad(fmt, "''#0*x", DecimalFormat.PAD_BEFORE_SUFFIX, 3, (char) 0x0078 /*x*/);
        expectPad(fmt, "'I''ll'*a###.##", DecimalFormat.PAD_AFTER_PREFIX, 10, (char) 0x0061 /*a*/);

        fmt.applyPattern("AA#,##0.00ZZ");
        fmt.setPadCharacter(hat);

        fmt.setFormatWidth(10);

        fmt.setPadPosition(DecimalFormat.PAD_BEFORE_PREFIX);
        expectPat(fmt, "*^AA#,##0.00ZZ");

        fmt.setPadPosition(DecimalFormat.PAD_BEFORE_SUFFIX);
        expectPat(fmt, "AA#,##0.00*^ZZ");

        fmt.setPadPosition(DecimalFormat.PAD_AFTER_SUFFIX);
        expectPat(fmt, "AA#,##0.00ZZ*^");

        //            12  3456789012
        String exp = "AA*^#,##0.00ZZ";
        fmt.setFormatWidth(12);
        fmt.setPadPosition(DecimalFormat.PAD_AFTER_PREFIX);
        expectPat(fmt, exp);

        fmt.setFormatWidth(13);
        //              12  34567890123
        expectPat(fmt, "AA*^##,##0.00ZZ");

        fmt.setFormatWidth(14);
        //              12  345678901234
        expectPat(fmt, "AA*^###,##0.00ZZ");

        fmt.setFormatWidth(15);
        //              12  3456789012345
        expectPat(fmt, "AA*^####,##0.00ZZ"); // This is the interesting case

        // The new implementation produces "AA*^#####,##0.00ZZ", which is functionally equivalent
        // to what the old implementation produced, "AA*^#,###,##0.00ZZ"
        fmt.setFormatWidth(16);
        //              12  34567890123456
        //expectPat(fmt, "AA*^#,###,##0.00ZZ");
        expectPat(fmt, "AA*^#####,##0.00ZZ");
    }

    @Test
    public void TestRegistration() {
        final ULocale SRC_LOC = ULocale.FRANCE;
        final ULocale SWAP_LOC = ULocale.US;

        class TestFactory extends SimpleNumberFormatFactory {
            NumberFormat currencyStyle;

            TestFactory() {
                super(SRC_LOC, true);
                currencyStyle = NumberFormat.getIntegerInstance(SWAP_LOC);
            }

            @Override
            public NumberFormat createFormat(ULocale loc, int formatType) {
                if (formatType == FORMAT_CURRENCY) {
                    return currencyStyle;
                }
                return null;
            }
        }

        NumberFormat f0 = NumberFormat.getIntegerInstance(SWAP_LOC);
        NumberFormat f1 = NumberFormat.getIntegerInstance(SRC_LOC);
        NumberFormat f2 = NumberFormat.getCurrencyInstance(SRC_LOC);
        Object key = NumberFormat.registerFactory(new TestFactory());
        NumberFormat f3 = NumberFormat.getCurrencyInstance(SRC_LOC);
        NumberFormat f4 = NumberFormat.getIntegerInstance(SRC_LOC);
        NumberFormat.unregister(key); // restore for other tests
        NumberFormat f5 = NumberFormat.getCurrencyInstance(SRC_LOC);

        float n = 1234.567f;
        logln("f0 swap int: " + f0.format(n));
        logln("f1 src int: " + f1.format(n));
        logln("f2 src cur: " + f2.format(n));
        logln("f3 reg cur: " + f3.format(n));
        logln("f4 reg int: " + f4.format(n));
        logln("f5 unreg cur: " + f5.format(n));

        if (!f3.format(n).equals(f0.format(n))) {
            errln("registered service did not match");
        }
        if (!f4.format(n).equals(f1.format(n))) {
            errln("registered service did not inherit");
        }
        if (!f5.format(n).equals(f2.format(n))) {
            errln("unregistered service did not match original");
        }
    }

    @Test
    public void TestScientific2() {
        // jb 2552
        DecimalFormat fmt = (DecimalFormat)NumberFormat.getCurrencyInstance();
        Number num = new Double(12.34);
        expect(fmt, num, "$12.34");
        fmt.setScientificNotation(true);
        expect(fmt, num, "$1.23E1");
        fmt.setScientificNotation(false);
        expect(fmt, num, "$12.34");
    }

    @Test
    public void TestScientificGrouping() {
        // jb 2552
        DecimalFormat fmt = new DecimalFormat("###.##E0");
        expect(fmt, .01234, "12.3E-3");
        expect(fmt, .1234, "123E-3");
        expect(fmt, 1.234, "1.23E0");
        expect(fmt, 12.34, "12.3E0");
        expect(fmt, 123.4, "123E0");
        expect(fmt, 1234, "1.23E3");
    }

    // additional coverage tests

    // sigh, can't have static inner classes, why not?

    static final class PI extends Number {
        /**
         * For serialization
         */
        private static final long serialVersionUID = -305601227915602172L;

        private PI() {}
        @Override
        public int intValue() { return (int)Math.PI; }
        @Override
        public long longValue() { return (long)Math.PI; }
        @Override
        public float  floatValue() { return (float)Math.PI; }
        @Override
        public double doubleValue() { return Math.PI; }
        @Override
        public byte byteValue() { return (byte)Math.PI; }
        @Override
        public short shortValue() { return (short)Math.PI; }

        public static final Number INSTANCE = new PI();
    }

    @Test
    public void TestCoverage() {
        NumberFormat fmt = NumberFormat.getNumberInstance(); // default locale
        logln(fmt.format(new BigInteger("1234567890987654321234567890987654321", 10)));

        fmt = NumberFormat.getScientificInstance(); // default locale

        logln(fmt.format(PI.INSTANCE));

        try {
            logln(fmt.format("12345"));
            errln("numberformat of string did not throw exception");
        }
        catch (Exception e) {
            logln("PASS: numberformat of string failed as expected");
        }

        int hash = fmt.hashCode();
        logln("hash code " + hash);

        logln("compare to string returns: " + fmt.equals(""));

        // For ICU 2.6 - alan
        DecimalFormatSymbols US = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("'*&'' '\u00A4' ''&*' #,##0.00", US);
        df.setCurrency(Currency.getInstance("INR"));
        expect2(df, 1.0, "*&' \u20B9 '&* 1.00");
        expect2(df, -2.0, "-*&' \u20B9 '&* 2.00");
        df.applyPattern("#,##0.00 '*&'' '\u00A4' ''&*'");
        expect2(df, 2.0, "2.00 *&' \u20B9 '&*");
        expect2(df, -1.0, "-1.00 *&' \u20B9 '&*");

        java.math.BigDecimal r;

        r = df.getRoundingIncrement();
        if (r != null) {
            errln("FAIL: rounding = " + r + ", expect null");
        }

        if (df.isScientificNotation()) {
            errln("FAIL: isScientificNotation = true, expect false");
        }

        // Create a new instance to flush out currency info
        df = new DecimalFormat("0.00000", US);
        df.setScientificNotation(true);
        if (!df.isScientificNotation()) {
            errln("FAIL: isScientificNotation = false, expect true");
        }
        df.setMinimumExponentDigits((byte)2);
        if (df.getMinimumExponentDigits() != 2) {
            errln("FAIL: getMinimumExponentDigits = " +
                    df.getMinimumExponentDigits() + ", expect 2");
        }
        df.setExponentSignAlwaysShown(true);
        if (!df.isExponentSignAlwaysShown()) {
            errln("FAIL: isExponentSignAlwaysShown = false, expect true");
        }
        df.setSecondaryGroupingSize(0);
        if (df.getSecondaryGroupingSize() != 0) {
            errln("FAIL: getSecondaryGroupingSize = " +
                    df.getSecondaryGroupingSize() + ", expect 0");
        }
        expect2(df, 3.14159, "3.14159E+00");

        // DecimalFormatSymbols#getInstance
        DecimalFormatSymbols decsym1 = DecimalFormatSymbols.getInstance();
        DecimalFormatSymbols decsym2 = new DecimalFormatSymbols();
        if (!decsym1.equals(decsym2)) {
            errln("FAIL: DecimalFormatSymbols returned by getInstance()" +
                    "does not match new DecimalFormatSymbols().");
        }
        decsym1 = DecimalFormatSymbols.getInstance(Locale.JAPAN);
        decsym2 = DecimalFormatSymbols.getInstance(ULocale.JAPAN);
        if (!decsym1.equals(decsym2)) {
            errln("FAIL: DecimalFormatSymbols returned by getInstance(Locale.JAPAN)" +
                    "does not match the one returned by getInstance(ULocale.JAPAN).");
        }

        // DecimalFormatSymbols#getAvailableLocales/#getAvailableULocales
        Locale[] allLocales = DecimalFormatSymbols.getAvailableLocales();
        if (allLocales.length == 0) {
            errln("FAIL: Got a empty list for DecimalFormatSymbols.getAvailableLocales");
        } else {
            logln("PASS: " + allLocales.length +
                    " available locales returned by DecimalFormatSymbols.getAvailableLocales");
        }
        ULocale[] allULocales = DecimalFormatSymbols.getAvailableULocales();
        if (allULocales.length == 0) {
            errln("FAIL: Got a empty list for DecimalFormatSymbols.getAvailableLocales");
        } else {
            logln("PASS: " + allULocales.length +
                    " available locales returned by DecimalFormatSymbols.getAvailableULocales");
        }
    }

    @Test
    public void TestLocalizedPatternSymbolCoverage() {
        String[] standardPatterns = { "#,##0.05+%;#,##0.05-%", "* @@@E0‰" };
        String[] localizedPatterns = { "▰⁖▰▰໐⁘໐໕†⁜⁙▰⁖▰▰໐⁘໐໕‡⁜", "⁂ ⁕⁕⁕⁑⁑໐‱" };

        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setGroupingSeparator('⁖');
        dfs.setDecimalSeparator('⁘');
        dfs.setPatternSeparator('⁙');
        dfs.setDigit('▰');
        dfs.setZeroDigit('໐');
        dfs.setSignificantDigit('⁕');
        dfs.setPlusSign('†');
        dfs.setMinusSign('‡');
        dfs.setPercent('⁜');
        dfs.setPerMill('‱');
        dfs.setExponentSeparator("⁑⁑"); // tests multi-char sequence
        dfs.setPadEscape('⁂');

        for (int i=0; i<2; i++) {
            String standardPattern = standardPatterns[i];
            String localizedPattern = localizedPatterns[i];

            DecimalFormat df1 = new DecimalFormat("#", dfs);
            df1.applyPattern(standardPattern);
            DecimalFormat df2 = new DecimalFormat("#", dfs);
            df2.applyLocalizedPattern(localizedPattern);
            assertEquals("DecimalFormat instances should be equal",
                    df1, df2);
            assertEquals("toPattern should match on localizedPattern instance",
                    standardPattern, df2.toPattern());
            assertEquals("toLocalizedPattern should match on standardPattern instance",
                    localizedPattern, df1.toLocalizedPattern());
        }
    }

    @Test
    public void TestParseEmpty(){
        String parsetxt = "";
        NumberFormat numfmt = NumberFormat.getInstance(new ULocale("en_US"), NumberFormat.NUMBERSTYLE);
        ParsePosition ppos = new ParsePosition(0);
        Number value = null;
        try {
            value = numfmt.parse(parsetxt, ppos);
            if (value==null) {
                logln("NumberFormat.parse empty string succeeds (no exception) with null return as expected, ppos " + ppos.getIndex());
            } else {
                errln("NumberFormat.parse empty string succeeds (no exception) but returns non-null value " + value + ", ppos " + ppos.getIndex());
            }
        } catch (IllegalArgumentException e){
            errln("NumberFormat.parse empty string throws IllegalArgumentException");
        }
     }

    @Test
    public void TestParseNull() throws ParseException {
        DecimalFormat df = new DecimalFormat();
        try {
            df.parse(null);
            fail("df.parse(null) didn't throw an exception");
        } catch (IllegalArgumentException e){}
        try {
            df.parse(null, null);
            fail("df.parse(null) didn't throw an exception");
        } catch (IllegalArgumentException e){}
        try {
            df.parseCurrency(null, null);
            fail("df.parse(null) didn't throw an exception");
        } catch (IllegalArgumentException e){}
    }

    @Test
    public void TestWhiteSpaceParsing() {
        DecimalFormatSymbols US = new DecimalFormatSymbols(Locale.US);
        DecimalFormat fmt = new DecimalFormat("a  b#0c  ", US);
        int n = 1234;
        expect(fmt, "a b1234c ", n);
        expect(fmt, "a   b1234c   ", n);
        expect(fmt, "ab1234", n);

        fmt.applyPattern("a b #");
        expect(fmt, "ab1234", n);
        expect(fmt, "ab  1234", n);
        expect(fmt, "a b1234", n);
        expect(fmt, "a   b1234", n);
        expect(fmt, " a b 1234", n);

        // Horizontal whitespace is allowed, but not vertical whitespace.
        expect(fmt, "\ta\u00A0b\u20001234", n);
        expect(fmt, "a   \u200A    b1234", n);
        expectParseException(fmt, "\nab1234", n);
        expectParseException(fmt, "a    \n   b1234", n);
        expectParseException(fmt, "a    \u0085   b1234", n);
        expectParseException(fmt, "a    \u2028   b1234", n);

        // Test all characters in the UTS 18 "blank" set stated in the API docstring.
        UnicodeSet blanks = new UnicodeSet("[[:Zs:][\\u0009]]").freeze();
        for (String space : blanks) {
            String str = "a " + space + " b1234c  ";
            expect(fmt, str, n);
        }

        // Arbitrary whitespace is not accepted in strict mode.
        fmt.setParseStrict(true);
        for (String space : blanks) {
            String str = "a " + space + " b1234c  ";
            expectParseException(fmt, str, n);
        }

        // Test default ignorable characters.  These should work in both lenient and strict.
        UnicodeSet defaultIgnorables = new UnicodeSet("[[:Bidi_Control:]]").freeze();
        fmt.setParseStrict(false);
        for (String ignorable : defaultIgnorables) {
            String str = "a b " + ignorable + "1234c  ";
            expect(fmt, str, n);
        }
        fmt.setParseStrict(true);
        for (String ignorable : defaultIgnorables) {
            String str = "a b " + ignorable + "1234c  ";
            expect(fmt, str, n);
        }

        // Test that other whitespace characters do not work
        fmt.setParseStrict(false);
        UnicodeSet otherWhitespace = new UnicodeSet("[[:whitespace:]]").removeAll(blanks).freeze();
        for (String space : otherWhitespace) {
            String str = "a  " + space + "  b1234";
            expectParseException(fmt, str, n);
        }
    }

    /**
     * Test currencies whose display name is a ChoiceFormat.
     */
    @Test
    public void TestComplexCurrency() {
        //  CLDR No Longer uses complex currency symbols.
        //  Skipping this test.
        //        Locale loc = new Locale("kn", "IN", "");
        //        NumberFormat fmt = NumberFormat.getCurrencyInstance(loc);

        //        expect2(fmt, 1.0, "Re.\u00a01.00");
        //        expect(fmt, 1.001, "Re.\u00a01.00"); // tricky
        //        expect2(fmt, 12345678.0, "Rs.\u00a01,23,45,678.00");
        //        expect2(fmt, 0.5, "Rs.\u00a00.50");
        //        expect2(fmt, -1.0, "-Re.\u00a01.00");
        //        expect2(fmt, -10.0, "-Rs.\u00a010.00");
    }

    @Test
    public void TestCurrencyKeyword() {
        ULocale locale = new ULocale("th_TH@currency=QQQ");
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        String result = format.format(12.34f);
        if (!"QQQ 12.34".equals(result)) {
            errln("got unexpected currency: " + result);
        }
    }

    /**
     * Test alternate numbering systems
     */
    @Test
    public void TestNumberingSystems() {
        class TestNumberingSystemItem {
            private final String localeName;
            private final double value;
            private final boolean isRBNF;
            private final String expectedResult;

            TestNumberingSystemItem(String loc, double val, boolean rbnf, String exp) {
                localeName  = loc;
                value = val;
                isRBNF = rbnf;
                expectedResult = exp;
            }
        }

        final TestNumberingSystemItem[] DATA = {
                new TestNumberingSystemItem( "en_US@numbers=thai",        1234.567, false, "\u0e51,\u0e52\u0e53\u0e54.\u0e55\u0e56\u0e57" ),
                new TestNumberingSystemItem( "en_US@numbers=thai",        1234.567, false, "\u0E51,\u0E52\u0E53\u0E54.\u0E55\u0E56\u0E57" ),
                new TestNumberingSystemItem( "en_US@numbers=hebr",        5678.0,   true,  "\u05D4\u05F3\u05EA\u05E8\u05E2\u05F4\u05D7" ),
                new TestNumberingSystemItem( "en_US@numbers=arabext",     1234.567, false, "\u06F1\u066c\u06F2\u06F3\u06F4\u066b\u06F5\u06F6\u06F7" ),
                new TestNumberingSystemItem( "de_DE@numbers=foobar",      1234.567, false, "1.234,567" ),
                new TestNumberingSystemItem( "ar_EG",                     1234.567, false, "\u0661\u066c\u0662\u0663\u0664\u066b\u0665\u0666\u0667" ),
                new TestNumberingSystemItem( "th_TH@numbers=traditional", 1234.567, false, "\u0E51,\u0E52\u0E53\u0E54.\u0E55\u0E56\u0E57" ), // fall back to native per TR35
                new TestNumberingSystemItem( "ar_MA",                     1234.567, false, "1.234,567" ),
                new TestNumberingSystemItem( "en_US@numbers=hanidec",     1234.567, false, "\u4e00,\u4e8c\u4e09\u56db.\u4e94\u516d\u4e03" ),
                new TestNumberingSystemItem( "ta_IN@numbers=native",      1234.567, false, "\u0BE7,\u0BE8\u0BE9\u0BEA.\u0BEB\u0BEC\u0BED" ),
                new TestNumberingSystemItem( "ta_IN@numbers=traditional", 1235.0,   true,  "\u0BF2\u0BE8\u0BF1\u0BE9\u0BF0\u0BEB" ),
                new TestNumberingSystemItem( "ta_IN@numbers=finance",     1234.567, false, "1,234.567" ), // fall back to default per TR35
                new TestNumberingSystemItem( "zh_TW@numbers=native",      1234.567, false, "\u4e00,\u4e8c\u4e09\u56db.\u4e94\u516d\u4e03" ),
                new TestNumberingSystemItem( "zh_TW@numbers=traditional", 1234.567, true,  "\u4E00\u5343\u4E8C\u767E\u4E09\u5341\u56DB\u9EDE\u4E94\u516D\u4E03" ),
                new TestNumberingSystemItem( "zh_TW@numbers=finance",     1234.567, true,  "\u58F9\u4EDF\u8CB3\u4F70\u53C3\u62FE\u8086\u9EDE\u4F0D\u9678\u67D2" ),
                new TestNumberingSystemItem( "en_US@numbers=mathsanb",    1234.567, false,  "𝟭,𝟮𝟯𝟰.𝟱𝟲𝟳" ), // ticket #13286
        };


        for (TestNumberingSystemItem item : DATA) {
            ULocale loc = new ULocale(item.localeName);
            NumberFormat fmt = NumberFormat.getInstance(loc);
            if (item.isRBNF) {
                expect3(fmt,item.value,item.expectedResult);
            } else {
                expect2(fmt,item.value,item.expectedResult);
            }
        }
    }

    // Coverage tests for methods not being called otherwise.
    @Test
    public void TestNumberingSystemCoverage() {
        // Test getAvaliableNames
        String[] availableNames = NumberingSystem.getAvailableNames();
        if (availableNames == null || availableNames.length <= 0) {
            errln("ERROR: NumberingSystem.getAvailableNames() returned a null or empty array.");
        } else {
            // Check for alphabetical order
            for (int i=0; i<availableNames.length-1; i++) {
                assertTrue("Names should be in alphabetical order",
                        availableNames[i].compareTo(availableNames[i+1]) < 0);
            }

            boolean latnFound = false;
            for (String name : availableNames){
                assertNotEquals("should not throw and should not be null",
                        null, NumberingSystem.getInstanceByName(name));
                if ("latn".equals(name)) {
                    latnFound = true;
                    break;
                }
            }

            if (!latnFound) {
                errln("ERROR: 'latn' numbering system not found on NumberingSystem.getAvailableNames().");
            }
        }

        assertEquals("Non-existing numbering system should return null",
                null, NumberingSystem.getInstanceByName("dummy"));

        // Test NumberingSystem.getInstance()
        NumberingSystem ns1 = NumberingSystem.getInstance();
        if (ns1 == null || ns1.isAlgorithmic()) {
            errln("ERROR: NumberingSystem.getInstance() returned a null or invalid NumberingSystem");
        }

        // Test NumberingSystem.getInstance(int,boolean,String)
        /* Parameters used: the ones used in the default constructor
         * radix = 10;
         * algorithmic = false;
         * desc = "0123456789";
         */
        NumberingSystem ns2 = NumberingSystem.getInstance(10, false, "0123456789");
        if (ns2 == null || ns2.isAlgorithmic()) {
            errln("ERROR: NumberingSystem.getInstance(int,boolean,String) returned a null or invalid NumberingSystem");
        }

        // Test NumberingSystem.getInstance(Locale)
        NumberingSystem ns3 = NumberingSystem.getInstance(Locale.ENGLISH);
        if (ns3 == null || ns3.isAlgorithmic()) {
            errln("ERROR: NumberingSystem.getInstance(Locale) returned a null or invalid NumberingSystem");
        }
    }

    @Test
    public void Test6816() {
        Currency cur1 = Currency.getInstance(new Locale("und", "PH"));

        NumberFormat nfmt = NumberFormat.getCurrencyInstance(new Locale("und", "PH"));
        DecimalFormatSymbols decsym = ((DecimalFormat)nfmt).getDecimalFormatSymbols();
        Currency cur2 = decsym.getCurrency();

        if ( !cur1.getCurrencyCode().equals("PHP") || !cur2.getCurrencyCode().equals("PHP")) {
            errln("FAIL: Currencies should match PHP: cur1 = "+cur1.getCurrencyCode()+"; cur2 = "+cur2.getCurrencyCode());
        }

    }

    @Test
    public void TestThreadedFormat() {

        class FormatTask implements Runnable {
            DecimalFormat fmt;
            StringBuffer buf;
            boolean inc;
            float num;

            FormatTask(DecimalFormat fmt, int index) {
                this.fmt = fmt;
                this.buf = new StringBuffer();
                this.inc = (index & 0x1) == 0;
                this.num = inc ? 0 : 10000;
            }

            @Override
            public void run() {
                if (inc) {
                    while (num < 10000) {
                        buf.append(fmt.format(num) + "\n");
                        num += 3.14159;
                    }
                } else {
                    while (num > 0) {
                        buf.append(fmt.format(num) + "\n");
                        num -= 3.14159;
                    }
                }
            }

            String result() {
                return buf.toString();
            }
        }

        DecimalFormat fmt = new DecimalFormat("0.####");
        FormatTask[] tasks = new FormatTask[8];
        for (int i = 0; i < tasks.length; ++i) {
            tasks[i] = new FormatTask(fmt, i);
        }

        TestUtil.runUntilDone(tasks);

        for (int i = 2; i < tasks.length; i++) {
            String str1 = tasks[i].result();
            String str2 = tasks[i-2].result();
            if (!str1.equals(str2)) {
                System.out.println("mismatch at " + i);
                System.out.println(str1);
                System.out.println(str2);
                errln("decimal format thread mismatch");

                break;
            }
            str1 = str2;
        }
    }

    @Test
    public void TestPerMill() {
        DecimalFormat fmt = new DecimalFormat("###.###\u2030");
        assertEquals("0.4857 x ###.###\u2030",
                "485.7\u2030", fmt.format(0.4857));

        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ENGLISH);
        sym.setPerMill('m');
        DecimalFormat fmt2 = new DecimalFormat("", sym);
        fmt2.applyLocalizedPattern("###.###m");
        assertEquals("0.4857 x ###.###m",
                "485.7m", fmt2.format(0.4857));
    }

    @Test
    public void TestIllegalPatterns() {
        // Test cases:
        // Prefix with "-:" for illegal patterns
        // Prefix with "+:" for legal patterns
        String DATA[] = {
                // Unquoted special characters in the suffix are illegal
                "-:000.000|###",
                "+:000.000'|###'",
        };
        for (int i=0; i<DATA.length; ++i) {
            String pat=DATA[i];
            boolean valid = pat.charAt(0) == '+';
            pat = pat.substring(2);
            Exception e = null;
            try {
                // locale doesn't matter here
                new DecimalFormat(pat);
            } catch (IllegalArgumentException e1) {
                e = e1;
            } catch (IndexOutOfBoundsException e1) {
                e = e1;
            }
            String msg = (e==null) ? "success" : e.getMessage();
            if ((e==null) == valid) {
                logln("Ok: pattern \"" + pat + "\": " + msg);
            } else {
                errln("FAIL: pattern \"" + pat + "\" should have " +
                        (valid?"succeeded":"failed") + "; got " + msg);
            }
        }
    }

    /**
     * Parse a CurrencyAmount using the given NumberFormat, with
     * the 'delim' character separating the number and the currency.
     */
    private static CurrencyAmount parseCurrencyAmount(String str, NumberFormat fmt,
            char delim)
                    throws ParseException {
        int i = str.indexOf(delim);
        return new CurrencyAmount(fmt.parse(str.substring(0,i)),
                Currency.getInstance(str.substring(i+1)));
    }

    /**
     * Return an integer representing the next token from this
     * iterator.  The integer will be an index into the given list, or
     * -1 if there are no more tokens, or -2 if the token is not on
     * the list.
     */
    private static int keywordIndex(String tok) {
        for (int i=0; i<KEYWORDS.length; ++i) {
            if (tok.equals(KEYWORDS[i])) {
                return i;
            }
        }
        return -1;
    }

    private static final String KEYWORDS[] = {
        /*0*/ "ref=", // <reference pattern to parse numbers>
        /*1*/ "loc=", // <locale for formats>
        /*2*/ "f:",   // <pattern or '-'> <number> <exp. string>
        /*3*/ "fp:",  // <pattern or '-'> <number> <exp. string> <exp. number>
        /*4*/ "rt:",  // <pattern or '-'> <(exp.) number> <(exp.) string>
        /*5*/ "p:",   // <pattern or '-'> <string> <exp. number>
        /*6*/ "perr:", // <pattern or '-'> <invalid string>
        /*7*/ "pat:", // <pattern or '-'> <exp. toPattern or '-' or 'err'>
        /*8*/ "fpc:", // <loc or '-'> <curr.amt> <exp. string> <exp. curr.amt>
        /*9*/ "strict=", // true or false
    };

    @SuppressWarnings("resource")  // InputStream is will be closed by the ResourceReader.
    @Test
    public void TestCases() {
        String caseFileName = "NumberFormatTestCases.txt";
        java.io.InputStream is = NumberFormatTest.class.getResourceAsStream(caseFileName);

        ResourceReader reader = new ResourceReader(is, caseFileName, "utf-8");
        TokenIterator tokens = new TokenIterator(reader);

        Locale loc = new Locale("en", "US", "");
        DecimalFormat ref = null, fmt = null;
        MeasureFormat mfmt = null;
        String pat = null, str = null, mloc = null;
        boolean strict = false;

        try {
            for (;;) {
                String tok = tokens.next();
                if (tok == null) {
                    break;
                }
                String where = "(" + tokens.getLineNumber() + ") ";
                int cmd = keywordIndex(tok);
                switch (cmd) {
                case 0:
                    // ref= <reference pattern>
                    ref = new DecimalFormat(tokens.next(),
                            new DecimalFormatSymbols(Locale.US));
                    ref.setParseStrict(strict);
                    logln("Setting reference pattern to:\t" + ref);
                    break;
                case 1:
                    // loc= <locale>
                    loc = LocaleUtility.getLocaleFromName(tokens.next());
                    pat = ((DecimalFormat) NumberFormat.getInstance(loc)).toPattern();
                    logln("Setting locale to:\t" + loc + ", \tand pattern to:\t" + pat);
                    break;
                case 2: // f:
                case 3: // fp:
                case 4: // rt:
                case 5: // p:
                    tok = tokens.next();
                    if (!tok.equals("-")) {
                        pat = tok;
                    }
                    try {
                        fmt = new DecimalFormat(pat, new DecimalFormatSymbols(loc));
                        fmt.setParseStrict(strict);
                    } catch (IllegalArgumentException iae) {
                        errln(where + "Pattern \"" + pat + '"');
                        iae.printStackTrace();
                        tokens.next(); // consume remaining tokens
                        //tokens.next();
                        if (cmd == 3) tokens.next();
                        continue;
                    }
                    str = null;
                    try {
                        if (cmd == 2 || cmd == 3 || cmd == 4) {
                            // f: <pattern or '-'> <number> <exp. string>
                            // fp: <pattern or '-'> <number> <exp. string> <exp. number>
                            // rt: <pattern or '-'> <number> <string>
                            String num = tokens.next();
                            str = tokens.next();
                            Number n = ref.parse(num);
                            assertEquals(where + '"' + pat + "\".format(" + num + ")",
                                    str, fmt.format(n));
                            if (cmd == 3) { // fp:
                                n = ref.parse(tokens.next());
                            }
                            if (cmd != 2) { // != f:
                                assertEquals(where + '"' + pat + "\".parse(\"" + str + "\")",
                                        n, fmt.parse(str));
                            }
                        }
                        // p: <pattern or '-'> <string to parse> <exp. number>
                        else {
                            str = tokens.next();
                            String expstr = tokens.next();
                            Number parsed = fmt.parse(str);
                            Number exp = ref.parse(expstr);
                            assertEquals(where + '"' + pat + "\".parse(\"" + str + "\")",
                                    exp, parsed);
                        }
                    } catch (ParseException e) {
                        errln(where + '"' + pat + "\".parse(\"" + str +
                                "\") threw an exception");
                        e.printStackTrace();
                    }
                    break;
                case 6:
                    // perr: <pattern or '-'> <invalid string>
                    errln("Under construction");
                    return;
                case 7:
                    // pat: <pattern> <exp. toPattern, or '-' or 'err'>
                    String testpat = tokens.next();
                    String exppat  = tokens.next();
                    boolean err    = exppat.equals("err");
                    if (testpat.equals("-")) {
                        if (err) {
                            errln("Invalid command \"pat: - err\" at " +  tokens.describePosition());
                            continue;
                        }
                        testpat = pat;
                    }
                    if (exppat.equals("-")) exppat = testpat;
                    try {
                        DecimalFormat f = null;
                        if (testpat == pat) { // [sic]
                            f = fmt;
                        } else {
                            f = new DecimalFormat(testpat);
                            f.setParseStrict(strict);
                        }
                        if (err) {
                            errln(where + "Invalid pattern \"" + testpat +
                                    "\" was accepted");
                        } else {
                            assertEquals(where + '"' + testpat + "\".toPattern()",
                                    exppat, f.toPattern());
                        }
                    } catch (IllegalArgumentException iae2) {
                        if (err) {
                            logln("Ok: " + where + "Invalid pattern \"" + testpat +
                                    "\" threw an exception");
                        } else {
                            errln(where + "Valid pattern \"" + testpat +
                                    "\" threw an exception");
                            iae2.printStackTrace();
                        }
                    }
                    break;
                case 8: // fpc:
                    tok = tokens.next();
                    if (!tok.equals("-")) {
                        mloc = tok;
                        ULocale l = new ULocale(mloc);
                        try {
                            mfmt = MeasureFormat.getCurrencyFormat(l);
                        } catch (IllegalArgumentException iae) {
                            errln(where + "Loc \"" + tok + '"');
                            iae.printStackTrace();
                            tokens.next(); // consume remaining tokens
                            tokens.next();
                            tokens.next();
                            continue;
                        }
                    }
                    str = null;
                    try {
                        // fpc: <loc or '-'> <curr.amt> <exp. string> <exp. curr.amt>
                        String currAmt = tokens.next();
                        str = tokens.next();
                        CurrencyAmount target = parseCurrencyAmount(currAmt, ref, '/');
                        String formatResult = mfmt.format(target);
                        assertEquals(where + "getCurrencyFormat(" + mloc + ").format(" + currAmt + ")",
                                str, formatResult);
                        target = parseCurrencyAmount(tokens.next(), ref, '/');
                        CurrencyAmount parseResult = (CurrencyAmount) mfmt.parseObject(str);
                        assertEquals(where + "getCurrencyFormat(" + mloc + ").parse(\"" + str + "\")",
                                target, parseResult);
                    } catch (ParseException e) {
                        errln(where + '"' + pat + "\".parse(\"" + str +
                                "\") threw an exception");
                        e.printStackTrace();
                    }
                    break;
                case 9: // strict= true or false
                    strict = "true".equalsIgnoreCase(tokens.next());
                    logln("Setting strict to:\t" + strict);
                    break;
                case -1:
                    errln("Unknown command \"" + tok + "\" at " + tokens.describePosition());
                    return;
                }
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    public void TestFieldPositionDecimal() {
        DecimalFormat nf = (DecimalFormat) com.ibm.icu.text.NumberFormat.getInstance(ULocale.ENGLISH);
        nf.setPositivePrefix("FOO");
        nf.setPositiveSuffix("BA");
        StringBuffer buffer = new StringBuffer();
        FieldPosition fp = new FieldPosition(NumberFormat.Field.DECIMAL_SEPARATOR);
        nf.format(35.47, buffer, fp);
        assertEquals("35.47", "FOO35.47BA", buffer.toString());
        assertEquals("fp begin", 5, fp.getBeginIndex());
        assertEquals("fp end", 6, fp.getEndIndex());
    }

    @Test
    public void TestFieldPositionInteger() {
        DecimalFormat nf = (DecimalFormat) com.ibm.icu.text.NumberFormat.getInstance(ULocale.ENGLISH);
        nf.setPositivePrefix("FOO");
        nf.setPositiveSuffix("BA");
        StringBuffer buffer = new StringBuffer();
        FieldPosition fp = new FieldPosition(NumberFormat.Field.INTEGER);
        nf.format(35.47, buffer, fp);
        assertEquals("35.47", "FOO35.47BA", buffer.toString());
        assertEquals("fp begin", 3, fp.getBeginIndex());
        assertEquals("fp end", 5, fp.getEndIndex());
    }

    @Test
    public void TestFieldPositionFractionButInteger() {
        DecimalFormat nf = (DecimalFormat) com.ibm.icu.text.NumberFormat.getInstance(ULocale.ENGLISH);
        nf.setPositivePrefix("FOO");
        nf.setPositiveSuffix("BA");
        StringBuffer buffer = new StringBuffer();
        FieldPosition fp = new FieldPosition(NumberFormat.Field.FRACTION);
        nf.format(35, buffer, fp);
        assertEquals("35", "FOO35BA", buffer.toString());
        assertEquals("fp begin", 5, fp.getBeginIndex());
        assertEquals("fp end", 5, fp.getEndIndex());
    }

    @Test
    public void TestFieldPositionFraction() {
        DecimalFormat nf = (DecimalFormat) com.ibm.icu.text.NumberFormat.getInstance(ULocale.ENGLISH);
        nf.setPositivePrefix("FOO");
        nf.setPositiveSuffix("BA");
        StringBuffer buffer = new StringBuffer();
        FieldPosition fp = new FieldPosition(NumberFormat.Field.FRACTION);
        nf.format(35.47, buffer, fp);
        assertEquals("35.47", "FOO35.47BA", buffer.toString());
        assertEquals("fp begin", 6, fp.getBeginIndex());
        assertEquals("fp end", 8, fp.getEndIndex());
    }

    @Test
    public void TestFieldPositionCurrency() {
        DecimalFormat nf = (DecimalFormat) com.ibm.icu.text.NumberFormat.getCurrencyInstance(Locale.US);
        double amount = 35.47;
        double negAmount = -34.567;
        FieldPosition cp = new FieldPosition(NumberFormat.Field.CURRENCY);

        StringBuffer buffer0 = new StringBuffer();
        nf.format(amount, buffer0, cp);
        assertEquals("$35.47", "$35.47", buffer0.toString());
        assertEquals("cp begin", 0, cp.getBeginIndex());
        assertEquals("cp end", 1, cp.getEndIndex());

        StringBuffer buffer01 = new StringBuffer();
        nf.format(negAmount, buffer01, cp);
        assertEquals("-$34.57", "-$34.57", buffer01.toString());
        assertEquals("cp begin", 1, cp.getBeginIndex());
        assertEquals("cp end", 2, cp.getEndIndex());

        nf.setCurrency(Currency.getInstance(Locale.FRANCE));
        StringBuffer buffer1 = new StringBuffer();
        nf.format(amount, buffer1, cp);
        assertEquals("€35.47", "€35.47", buffer1.toString());
        assertEquals("cp begin", 0, cp.getBeginIndex());
        assertEquals("cp end", 1, cp.getEndIndex());

        nf.setCurrency(Currency.getInstance(new Locale("fr", "ch", "")));
        StringBuffer buffer2 = new StringBuffer();
        nf.format(amount, buffer2, cp);
        assertEquals("CHF 35.47", "CHF 35.47", buffer2.toString());
        assertEquals("cp begin", 0, cp.getBeginIndex());
        assertEquals("cp end", 3, cp.getEndIndex());

        StringBuffer buffer20 = new StringBuffer();
        nf.format(negAmount, buffer20, cp);
        assertEquals("-CHF 34.57", "-CHF 34.57", buffer20.toString());
        assertEquals("cp begin", 1, cp.getBeginIndex());
        assertEquals("cp end", 4, cp.getEndIndex());

        nf = (DecimalFormat) com.ibm.icu.text.NumberFormat.getCurrencyInstance(Locale.FRANCE);
        StringBuffer buffer3 = new StringBuffer();
        nf.format(amount, buffer3, cp);
        assertEquals("35,47 €", "35,47 €", buffer3.toString());
        assertEquals("cp begin", 6, cp.getBeginIndex());
        assertEquals("cp end", 7, cp.getEndIndex());

        StringBuffer buffer4 = new StringBuffer();
        nf.format(negAmount, buffer4, cp);
        assertEquals("-34,57 €", "-34,57 €", buffer4.toString());
        assertEquals("cp begin", 7, cp.getBeginIndex());
        assertEquals("cp end", 8, cp.getEndIndex());

        nf.setCurrency(Currency.getInstance(new Locale("fr", "ch")));
        StringBuffer buffer5 = new StringBuffer();
        nf.format(negAmount, buffer5, cp);
        assertEquals("-34,57 CHF", "-34,57 CHF", buffer5.toString());
        assertEquals("cp begin", 7, cp.getBeginIndex());
        assertEquals("cp end", 10, cp.getEndIndex());

        NumberFormat plCurrencyFmt = NumberFormat.getInstance(new Locale("fr", "ch"), NumberFormat.PLURALCURRENCYSTYLE);
        StringBuffer buffer6 = new StringBuffer();
        plCurrencyFmt.format(negAmount, buffer6, cp);
        assertEquals("-34.57 francs suisses", "-34.57 francs suisses", buffer6.toString());
        assertEquals("cp begin", 7, cp.getBeginIndex());
        assertEquals("cp end", 21, cp.getEndIndex());

        // Positive value with PLURALCURRENCYSTYLE.
        plCurrencyFmt = NumberFormat.getInstance(new Locale("ja", "ch"), NumberFormat.PLURALCURRENCYSTYLE);
        StringBuffer buffer7 = new StringBuffer();
        plCurrencyFmt.format(amount, buffer7, cp);
        assertEquals("35.47 スイス フラン", "35.47 スイス フラン", buffer7.toString());
        assertEquals("cp begin", 6, cp.getBeginIndex());
        assertEquals("cp end", 13, cp.getEndIndex());

        // PLURALCURRENCYSTYLE for non-ASCII.
        plCurrencyFmt = NumberFormat.getInstance(new Locale("ja", "de"), NumberFormat.PLURALCURRENCYSTYLE);
        StringBuffer buffer8 = new StringBuffer();
        plCurrencyFmt.format(negAmount, buffer8, cp);
        assertEquals("-34.57 ユーロ", "-34.57 ユーロ", buffer8.toString());
        assertEquals("cp begin", 7, cp.getBeginIndex());
        assertEquals("cp end", 10, cp.getEndIndex());

        nf = (DecimalFormat) com.ibm.icu.text.NumberFormat.getCurrencyInstance(Locale.JAPAN);
        nf.setCurrency(Currency.getInstance(new Locale("ja", "jp")));
        StringBuffer buffer9 = new StringBuffer();
        nf.format(negAmount, buffer9, cp);
        assertEquals("-￥35", "-￥35", buffer9.toString());
        assertEquals("cp begin", 1, cp.getBeginIndex());
        assertEquals("cp end", 2, cp.getEndIndex());

        // Negative value with PLURALCURRENCYSTYLE.
        plCurrencyFmt = NumberFormat.getInstance(new Locale("ja", "ch"), NumberFormat.PLURALCURRENCYSTYLE);
        StringBuffer buffer10 = new StringBuffer();
        plCurrencyFmt.format(negAmount, buffer10, cp);
        assertEquals("-34.57 スイス フラン", "-34.57 スイス フラン", buffer10.toString());
        assertEquals("cp begin", 7, cp.getBeginIndex());
        assertEquals("cp end", 14, cp.getEndIndex());

        // Negative value with PLURALCURRENCYSTYLE, Arabic digits.
        nf = (DecimalFormat) com.ibm.icu.text.NumberFormat.getCurrencyInstance(new Locale("ar", "eg"));
        plCurrencyFmt = NumberFormat.getInstance(new Locale("ar", "eg"), NumberFormat.PLURALCURRENCYSTYLE);
        StringBuffer buffer11 = new StringBuffer();
        plCurrencyFmt.format(negAmount, buffer11, cp);
        assertEquals("؜-٣٤٫٥٧ جنيه مصري", "؜-٣٤٫٥٧ جنيه مصري", buffer11.toString());
        assertEquals("cp begin", 8, cp.getBeginIndex());
        assertEquals("cp end", 17, cp.getEndIndex());
    }

    @Test
    public void TestRounding() {
        DecimalFormat nf = (DecimalFormat) com.ibm.icu.text.NumberFormat.getInstance(ULocale.ENGLISH);
        if (false) { // for debugging specific value
            nf.setRoundingMode(BigDecimal.ROUND_HALF_UP);
            checkRounding(nf, new BigDecimal("300.0300000000"), 0, new BigDecimal("0.020000000"));
        }
        // full tests
        int[] roundingIncrements = {1, 2, 5, 20, 50, 100};
        int[] testValues = {0, 300};
        for (int j = 0; j < testValues.length; ++j) {
            for (int mode = BigDecimal.ROUND_UP; mode < BigDecimal.ROUND_HALF_EVEN; ++mode) {
                nf.setRoundingMode(mode);
                for (int increment = 0; increment < roundingIncrements.length; ++increment) {
                    BigDecimal base = new BigDecimal(testValues[j]);
                    BigDecimal rInc = new BigDecimal(roundingIncrements[increment]);
                    checkRounding(nf,  base, 20, rInc);
                    rInc = new BigDecimal("1.000000000").divide(rInc);
                    checkRounding(nf,  base, 20, rInc);
                }
            }
        }
    }

    @Test
    public void TestRoundingPattern() {
        class TestRoundingPatternItem {
            String     pattern;
            BigDecimal roundingIncrement;
            double     testCase;
            String     expected;

            TestRoundingPatternItem(String pattern, BigDecimal roundingIncrement, double testCase, String expected) {
                this.pattern = pattern;
                this.roundingIncrement = roundingIncrement;
                this.testCase = testCase;
                this.expected = expected;
            }
        };

        TestRoundingPatternItem []tests = {
                new TestRoundingPatternItem("##0.65", new BigDecimal("0.65"), 1.234, "1.30"),
                new TestRoundingPatternItem("#50", new BigDecimal("50"), 1230, "1250")
        };

        DecimalFormat df = (DecimalFormat) com.ibm.icu.text.NumberFormat.getInstance(ULocale.ENGLISH);
        String result;
        for (int i = 0; i < tests.length; i++) {
            df.applyPattern(tests[i].pattern);

            result = df.format(tests[i].testCase);

            if (!tests[i].expected.equals(result)) {
                errln("String Pattern Rounding Test Failed: Pattern: \"" + tests[i].pattern + "\" Number: " + tests[i].testCase + " - Got: " + result + " Expected: " + tests[i].expected);
            }

            df.setRoundingIncrement(tests[i].roundingIncrement);

            result = df.format(tests[i].testCase);

            if (!tests[i].expected.equals(result)) {
                errln("BigDecimal Rounding Test Failed: Pattern: \"" + tests[i].pattern + "\" Number: " + tests[i].testCase + " - Got: " + result + " Expected: " + tests[i].expected);
            }
        }
    }

    @Test
    public void TestBigDecimalRounding() {
        String figure = "50.000000004";
        Double dbl = new Double(figure);
        BigDecimal dec = new BigDecimal(figure);

        DecimalFormat f = (DecimalFormat) NumberFormat.getInstance();
        f.applyPattern("00.00######");

        assertEquals("double format", "50.00", f.format(dbl));
        assertEquals("bigdec format", "50.00", f.format(dec));

        int maxFracDigits = f.getMaximumFractionDigits();
        BigDecimal roundingIncrement = new BigDecimal("1").movePointLeft(maxFracDigits);

        f.setRoundingIncrement(roundingIncrement);
        f.setRoundingMode(BigDecimal.ROUND_DOWN);
        assertEquals("Rounding down", f.format(dbl), f.format(dec));

        f.setRoundingIncrement(roundingIncrement);
        f.setRoundingMode(BigDecimal.ROUND_HALF_UP);
        assertEquals("Rounding half up", f.format(dbl), f.format(dec));
    }

    void checkRounding(DecimalFormat nf, BigDecimal base, int iterations, BigDecimal increment) {
        nf.setRoundingIncrement(increment.toBigDecimal());
        BigDecimal lastParsed = new BigDecimal(Integer.MIN_VALUE); // used to make sure that rounding is monotonic
        for (int i = -iterations; i <= iterations; ++i) {
            BigDecimal iValue = base.add(increment.multiply(new BigDecimal(i)).movePointLeft(1));
            BigDecimal smallIncrement = new BigDecimal("0.00000001");
            if (iValue.signum() != 0) {
                smallIncrement.multiply(iValue); // scale unless zero
            }
            // we not only test the value, but some values in a small range around it.
            lastParsed = checkRound(nf, iValue.subtract(smallIncrement), lastParsed);
            lastParsed = checkRound(nf, iValue, lastParsed);
            lastParsed = checkRound(nf, iValue.add(smallIncrement), lastParsed);
        }
    }

    private BigDecimal checkRound(DecimalFormat nf, BigDecimal iValue, BigDecimal lastParsed) {
        String formatedBigDecimal = nf.format(iValue);
        String formattedDouble = nf.format(iValue.doubleValue());
        if (!equalButForTrailingZeros(formatedBigDecimal, formattedDouble)) {

            errln("Failure at: " + iValue + " (" + iValue.doubleValue() + ")"
                    + ",\tRounding-mode: " + roundingModeNames[nf.getRoundingMode()]
                            + ",\tRounding-increment: " + nf.getRoundingIncrement()
                            + ",\tdouble: " + formattedDouble
                            + ",\tBigDecimal: " + formatedBigDecimal);

        } else {
            logln("Value: " + iValue
                    + ",\tRounding-mode: " + roundingModeNames[nf.getRoundingMode()]
                            + ",\tRounding-increment: " + nf.getRoundingIncrement()
                            + ",\tdouble: " + formattedDouble
                            + ",\tBigDecimal: " + formatedBigDecimal);
        }
        try {
            // Number should have compareTo(...)
            BigDecimal parsed = toBigDecimal(nf.parse(formatedBigDecimal));
            if (lastParsed.compareTo(parsed) > 0) {
                errln("Rounding wrong direction!: " + lastParsed + " > " + parsed);
            }
            lastParsed = parsed;
        } catch (ParseException e) {
            errln("Parse Failure with: " + formatedBigDecimal);
        }
        return lastParsed;
    }

    static BigDecimal toBigDecimal(Number number) {
        return number instanceof BigDecimal ? (BigDecimal) number
                : number instanceof BigInteger ? new BigDecimal((BigInteger)number)
        : number instanceof java.math.BigDecimal ? new BigDecimal((java.math.BigDecimal)number)
                : number instanceof Double ? new BigDecimal(number.doubleValue())
        : number instanceof Float ? new BigDecimal(number.floatValue())
                : new BigDecimal(number.longValue());
    }

    static String[] roundingModeNames = {
        "ROUND_UP", "ROUND_DOWN", "ROUND_CEILING", "ROUND_FLOOR",
        "ROUND_HALF_UP", "ROUND_HALF_DOWN", "ROUND_HALF_EVEN",
        "ROUND_UNNECESSARY"
    };

    private static boolean equalButForTrailingZeros(String formatted1, String formatted2) {
        if (formatted1.length() == formatted2.length()) return formatted1.equals(formatted2);
        return stripFinalZeros(formatted1).equals(stripFinalZeros(formatted2));
    }

    private static String stripFinalZeros(String formatted) {
        int len1 = formatted.length();
        char ch;
        while (len1 > 0 && ((ch = formatted.charAt(len1-1)) == '0' || ch == '.')) --len1;
        if (len1==1 && ((ch = formatted.charAt(len1-1)) == '-')) --len1;
        return formatted.substring(0,len1);
    }

    //------------------------------------------------------------------
    // Support methods
    //------------------------------------------------------------------

    /** Format-Parse test */
    public void expect2(NumberFormat fmt, Number n, String exp) {
        // Don't round-trip format test, since we explicitly do it
        expect(fmt, n, exp, false);
        expect(fmt, exp, n);
    }
    /** Format-Parse test */
    public void expect3(NumberFormat fmt, Number n, String exp) {
        // Don't round-trip format test, since we explicitly do it
        expect_rbnf(fmt, n, exp, false);
        expect_rbnf(fmt, exp, n);
    }

    /** Format-Parse test (convenience) */
    public void expect2(NumberFormat fmt, double n, String exp) {
        expect2(fmt, new Double(n), exp);
    }
    /** RBNF Format-Parse test (convenience) */
    public void expect3(NumberFormat fmt, double n, String exp) {
        expect3(fmt, new Double(n), exp);
    }

    /** Format-Parse test (convenience) */
    public void expect2(NumberFormat fmt, long n, String exp) {
        expect2(fmt, new Long(n), exp);
    }
    /** RBNF Format-Parse test (convenience) */
    public void expect3(NumberFormat fmt, long n, String exp) {
        expect3(fmt, new Long(n), exp);
    }

    /** Format test */
    public void expect(NumberFormat fmt, Number n, String exp, boolean rt) {
        StringBuffer saw = new StringBuffer();
        FieldPosition pos = new FieldPosition(0);
        fmt.format(n, saw, pos);
        String pat = ((DecimalFormat)fmt).toPattern();
        if (saw.toString().equals(exp)) {
            logln("Ok   " + n + " x " +
                    pat + " = \"" +
                    saw + "\"");
            // We should be able to round-trip the formatted string =>
            // number => string (but not the other way around: number
            // => string => number2, might have number2 != number):
            if (rt) {
                try {
                    Number n2 = fmt.parse(exp);
                    StringBuffer saw2 = new StringBuffer();
                    fmt.format(n2, saw2, pos);
                    if (!saw2.toString().equals(exp)) {
                        errln("expect() format test rt, locale " + fmt.getLocale(ULocale.VALID_LOCALE) +
                                ", FAIL \"" + exp + "\" => " + n2 + " => \"" + saw2 + '"');
                    }
                } catch (ParseException e) {
                    errln("expect() format test rt, locale " + fmt.getLocale(ULocale.VALID_LOCALE) +
                            ", " + e.getMessage());
                    return;
                }
            }
        } else {
            errln("expect() format test, locale " + fmt.getLocale(ULocale.VALID_LOCALE) +
                    ", FAIL " + n + " x " + pat + " = \"" + saw + "\", expected \"" + exp + "\"");
        }
    }
    /** RBNF format test */
    public void expect_rbnf(NumberFormat fmt, Number n, String exp, boolean rt) {
        StringBuffer saw = new StringBuffer();
        FieldPosition pos = new FieldPosition(0);
        fmt.format(n, saw, pos);
        if (saw.toString().equals(exp)) {
            logln("Ok   " + n + " = \"" +
                    saw + "\"");
            // We should be able to round-trip the formatted string =>
            // number => string (but not the other way around: number
            // => string => number2, might have number2 != number):
            if (rt) {
                try {
                    Number n2 = fmt.parse(exp);
                    StringBuffer saw2 = new StringBuffer();
                    fmt.format(n2, saw2, pos);
                    if (!saw2.toString().equals(exp)) {
                        errln("expect_rbnf() format test rt, locale " + fmt.getLocale(ULocale.VALID_LOCALE) +
                                ", FAIL \"" + exp + "\" => " + n2 + " => \"" + saw2 + '"');
                    }
                } catch (ParseException e) {
                    errln("expect_rbnf() format test rt, locale " + fmt.getLocale(ULocale.VALID_LOCALE) +
                            ", " + e.getMessage());
                    return;
                }
            }
        } else {
            errln("expect_rbnf() format test, locale " + fmt.getLocale(ULocale.VALID_LOCALE) +
                    ", FAIL " + n + " = \"" + saw + "\", expected \"" + exp + "\"");
        }
    }

    /** Format test (convenience) */
    public void expect(NumberFormat fmt, Number n, String exp) {
        expect(fmt, n, exp, true);
    }

    /** Format test (convenience) */
    public void expect(NumberFormat fmt, double n, String exp) {
        expect(fmt, new Double(n), exp);
    }

    /** Format test (convenience) */
    public void expect(NumberFormat fmt, long n, String exp) {
        expect(fmt, new Long(n), exp);
    }

    /** Parse test */
    public void expect(NumberFormat fmt, String str, Number n) {
        Number num = null;
        try {
            num = fmt.parse(str);
        } catch (ParseException e) {
            errln(e.getMessage());
            return;
        }
        String pat = ((DecimalFormat)fmt).toPattern();
        // A little tricky here -- make sure Double(12345.0) and
        // Long(12345) match.
        if (num.equals(n) || num.doubleValue() == n.doubleValue()) {
            logln("Ok   \"" + str + "\" x " +
                    pat + " = " +
                    num);
        } else {
            errln("expect() parse test, locale " + fmt.getLocale(ULocale.VALID_LOCALE) +
                    ", FAIL \"" + str + "\" x " + pat + " = " + num + ", expected " + n);
        }
    }

    /** RBNF Parse test */
    public void expect_rbnf(NumberFormat fmt, String str, Number n) {
        Number num = null;
        try {
            num = fmt.parse(str);
        } catch (ParseException e) {
            errln(e.getMessage());
            return;
        }
        // A little tricky here -- make sure Double(12345.0) and
        // Long(12345) match.
        if (num.equals(n) || num.doubleValue() == n.doubleValue()) {
            logln("Ok   \"" + str + " = " +
                    num);
        } else {
            errln("expect_rbnf() parse test, locale " + fmt.getLocale(ULocale.VALID_LOCALE) +
                    ", FAIL \"" + str + " = " + num + ", expected " + n);
        }
    }

    /** Parse test (convenience) */
    public void expect(NumberFormat fmt, String str, double n) {
        expect(fmt, str, new Double(n));
    }

    /** Parse test (convenience) */
    public void expect(NumberFormat fmt, String str, long n) {
        expect(fmt, str, new Long(n));
    }

    /** Parse test */
    public void expectParseException(DecimalFormat fmt, String str, Number n) {
        Number num = null;
        try {
            num = fmt.parse(str);
            errln("Expected failure, but passed: " + n + " on " + fmt.toPattern() + " -> " + num);
        } catch (ParseException e) {
        }
    }

    private void expectCurrency(NumberFormat nf, Currency curr,
            double value, String string) {
        DecimalFormat fmt = (DecimalFormat) nf;
        if (curr != null) {
            fmt.setCurrency(curr);
        }
        String s = fmt.format(value).replace('\u00A0', ' ');

        if (s.equals(string)) {
            logln("Ok: " + value + " x " + curr + " => " + s);
        } else {
            errln("FAIL: " + value + " x " + curr + " => " + s +
                    ", expected " + string);
        }
    }

    public void expectPad(DecimalFormat fmt, String pat, int pos) {
        expectPad(fmt, pat, pos, 0, (char)0);
    }

    public void expectPad(DecimalFormat fmt, final String pat, int pos, int width, final char pad) {
        int apos = 0, awidth = 0;
        char apadStr;
        try {
            fmt.applyPattern(pat);
            apos = fmt.getPadPosition();
            awidth = fmt.getFormatWidth();
            apadStr = fmt.getPadCharacter();
        } catch (Exception e) {
            apos = -1;
            awidth = width;
            apadStr = pad;
        }

        if (apos == pos && awidth == width && apadStr == pad) {
            logln("Ok   \"" + pat + "\" pos="
                    + apos + ((pos == -1) ? "" : " width=" + awidth + " pad=" + apadStr));
        } else {
            errln("FAIL \"" + pat + "\" pos=" + apos + " width="
                    + awidth + " pad=" + apadStr + ", expected "
                    + pos + " " + width + " " + pad);
        }
    }

    public void expectPat(DecimalFormat fmt, final String exp) {
        String pat = fmt.toPattern();
        if (pat.equals(exp)) {
            logln("Ok   \"" + pat + "\"");
        } else {
            errln("FAIL \"" + pat + "\", expected \"" + exp + "\"");
        }
    }


    private void expectParseCurrency(NumberFormat fmt, Currency expected, String text) {
        ParsePosition pos = new ParsePosition(0);
        CurrencyAmount currencyAmount = fmt.parseCurrency(text, pos);
        assertTrue("Parse of " + text + " should have succeeded.", pos.getIndex() > 0);
        assertEquals("Currency should be correct.", expected, currencyAmount.getCurrency());
    }

    @Test
    public void TestJB3832(){
        ULocale locale = new ULocale("pt_PT@currency=PTE");
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        Currency curr = Currency.getInstance(locale);
        logln("\nName of the currency is: " + curr.getName(locale, Currency.LONG_NAME, new boolean[] {false}));
        CurrencyAmount cAmt = new CurrencyAmount(1150.50, curr);
        logln("CurrencyAmount object's hashCode is: " + cAmt.hashCode()); //cover hashCode
        String str = format.format(cAmt);
        String expected = "1,150$50\u00a0\u200b";
        if(!expected.equals(str)){
            errln("Did not get the expected output Expected: "+expected+" Got: "+ str);
        }
    }

    @Test
    public void TestScientificWithGrouping() {
        // Grouping separators are not allowed in the pattern, but we can enable them via the API.
        DecimalFormat df = new DecimalFormat("###0.000E0");
        df.setGroupingUsed(true);
        df.setGroupingSize(3);
        expect2(df, 123, "123.0E0");
        expect2(df, 1234, "1,234E0");
        expect2(df, 12340, "1.234E4");
    }

    @Test
    public void TestStrictParse() {
        // Pass both strict and lenient:
        String[] pass = {
                "0",           // single zero before end of text is not leading
                "0 ",          // single zero at end of number is not leading
                "0.",          // single zero before period (or decimal, it's ambiguous) is not leading
                "0,",          // single zero before comma (not group separator) is not leading
                "0.0",         // single zero before decimal followed by digit is not leading
                "0. ",         // same as above before period (or decimal) is not leading
                "0.100,5",     // comma stops parse of decimal (no grouping)
                "0.100,,5",    // two commas also stops parse
                ".00",         // leading decimal is ok, even with zeros
                "1234567",     // group separators are not required
                "12345, ",     // comma not followed by digit is not a group separator, but end of number
                "1,234, ",     // if group separator is present, group sizes must be appropriate
                "1,234,567",   // ...secondary too
                "0E",          // an exponent not followed by zero or digits is not an exponent
                "00",          // leading zero before zero - used to be error - see ticket #7913
                "012",         // leading zero before digit - used to be error - see ticket #7913
                "0,456",       // leading zero before group separator - used to be error - see ticket #7913
                "999,999",     // see ticket #6863
                "-99,999",     // see ticket #6863
                "-999,999",    // see ticket #6863
                "-9,999,999",  // see ticket #6863
        };
        // Pass lenient, fail strict:
        String[] fail = {
                "1,2",       // wrong number of digits after group separator
                ",.02",      // leading group separator before decimal
                "1,.02",     // group separator before decimal
                ",0",        // leading group separator before a single digit
                ",1",        // leading group separator before a single digit
                "1,45",      // wrong number of digits in primary group
                "1,45 that", // wrong number of digits in primary group
                "1,45.34",   // wrong number of digits in primary group
                "1234,567",  // wrong number of digits in secondary group
                "12,34,567", // wrong number of digits in secondary group
                "1,23,456,7890", // wrong number of digits in primary and secondary groups
        };
        // Fail both lenient and strict:
        String[] failBoth = {
        };

        DecimalFormat nf = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH);
        runStrictParseBatch(nf, pass, fail, failBoth);

        String[] scientificPass = {
                "0E2",      // single zero before exponent is ok
                "1234E2",   // any number of digits before exponent is ok
                "1,234E",   // an exponent string not followed by zero or digits is not an exponent
                "00E2",     // leading zeroes now allowed in strict mode - see ticket #
        };
        String[] scientificFail = {
        };
        String[] scientificFailBoth = {
        };

        nf = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH);
        runStrictParseBatch(nf, scientificPass, scientificFail, scientificFailBoth);

        String[] mixedPass = {
                "12,34,567",
                "12,34,567,",
                "12,34,567, that",
                "12,34,567 that",
        };
        String[] mixedFail = {
                "12,34,56",
                "12,34,56,",
                "12,34,56, that ",
                "12,34,56 that",
        };
        String[] mixedFailBoth = {
        };

        nf = new DecimalFormat("#,##,##0.#");
        runStrictParseBatch(nf, mixedPass, mixedFail, mixedFailBoth);
    }

    void runStrictParseBatch(DecimalFormat nf, String[] pass, String[] fail, String[] failBoth) {
        nf.setParseStrict(false);
        runStrictParseTests("should pass", nf, pass, true);
        runStrictParseTests("should also pass", nf, fail, true);
        runStrictParseTests("should fail", nf, failBoth, false);
        nf.setParseStrict(true);
        runStrictParseTests("should still pass", nf, pass, true);
        runStrictParseTests("should fail", nf, fail, false);
        runStrictParseTests("should still fail", nf, failBoth, false);
    }

    void runStrictParseTests(String msg, DecimalFormat nf, String[] tests, boolean pass) {
        logln("");
        logln("pattern: '" + nf.toPattern() + "'");
        logln(msg);
        for (int i = 0; i < tests.length; ++i) {
            String str = tests[i];
            ParsePosition pp = new ParsePosition(0);
            Number n = nf.parse(str, pp);
            String formatted = n != null ? nf.format(n) : "null";
            String err = pp.getErrorIndex() == -1 ? "" : "(error at " + pp.getErrorIndex() + ")";
            if ((err.length() == 0) != pass) {
                errln("'" + str + "' parsed '" +
                        str.substring(0, pp.getIndex()) +
                        "' returned " + n + " formats to '" +
                        formatted + "' " + err);
            } else {
                if (err.length() > 0) {
                    err = "got expected " + err;
                }
                logln("'" + str + "' parsed '" +
                        str.substring(0, pp.getIndex()) +
                        "' returned " + n + " formats to '" +
                        formatted + "' " + err);
            }
        }
    }

    @Test
    public void TestJB5251(){
        //save default locale
        ULocale defaultLocale = ULocale.getDefault();
        ULocale.setDefault(new ULocale("qr_QR"));
        try {
            NumberFormat.getInstance();
        }
        catch (Exception e) {
            errln("Numberformat threw exception for non-existent locale. It should use the default.");
        }
        //reset default locale
        ULocale.setDefault(defaultLocale);
    }

    @Test
    public void TestParseReturnType() {
        String[] defaultLong = {
                "123",
                "123.0",
                "0.0",
                "-9223372036854775808", // Min Long
                "9223372036854775807" // Max Long
        };

        String[] defaultNonLong = {
                "12345678901234567890",
                "9223372036854775808",
                "-9223372036854775809"
        };

        String[] doubles = {
                "-0.0",
                "NaN",
                "\u221E"    // Infinity
        };

        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        DecimalFormat nf = new DecimalFormat("#.#", sym);

        if (nf.isParseBigDecimal()) {
            errln("FAIL: isParseDecimal() must return false by default");
        }

        // isParseBigDecimal() is false
        for (int i = 0; i < defaultLong.length; i++) {
            try {
                Number n = nf.parse(defaultLong[i]);
                if (!(n instanceof Long)) {
                    errln("FAIL: parse does not return Long instance");
                }
            } catch (ParseException e) {
                errln("parse of '" + defaultLong[i] + "' threw exception: " + e);
            }
        }
        for (int i = 0; i < defaultNonLong.length; i++) {
            try {
                Number n = nf.parse(defaultNonLong[i]);
                if (n instanceof Long) {
                    errln("FAIL: parse returned a Long");
                }
            } catch (ParseException e) {
                errln("parse of '" + defaultNonLong[i] + "' threw exception: " + e);
            }
        }
        // parse results for doubls must be always Double
        for (int i = 0; i < doubles.length; i++) {
            try {
                Number n = nf.parse(doubles[i]);
                if (!(n instanceof Double)) {
                    errln("FAIL: parse does not return Double instance");
                }
            } catch (ParseException e) {
                errln("parse of '" + doubles[i] + "' threw exception: " + e);
            }
        }

        // force this DecimalFormat to return BigDecimal
        nf.setParseBigDecimal(true);
        if (!nf.isParseBigDecimal()) {
            errln("FAIL: isParseBigDecimal() must return true");
        }

        // isParseBigDecimal() is true
        for (int i = 0; i < defaultLong.length + defaultNonLong.length; i++) {
            String input = (i < defaultLong.length) ? defaultLong[i] : defaultNonLong[i - defaultLong.length];
            try {
                Number n = nf.parse(input);
                if (!(n instanceof BigDecimal)) {
                    errln("FAIL: parse does not return BigDecimal instance");
                }
            } catch (ParseException e) {
                errln("parse of '" + input + "' threw exception: " + e);
            }
        }
        // parse results for doubls must be always Double
        for (int i = 0; i < doubles.length; i++) {
            try {
                Number n = nf.parse(doubles[i]);
                if (!(n instanceof Double)) {
                    errln("FAIL: parse does not return Double instance");
                }
            } catch (ParseException e) {
                errln("parse of '" + doubles[i] + "' threw exception: " + e);
            }
        }
    }

    @Test
    public void TestNonpositiveMultiplier() {
        DecimalFormat df = new DecimalFormat("0");

        // test zero multiplier

        try {
            df.setMultiplier(0);

            // bad
            errln("DecimalFormat.setMultiplier(0) did not throw an IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            // good
        }

        // test negative multiplier

        try {
            df.setMultiplier(-1);

            if (df.getMultiplier() != -1) {
                errln("DecimalFormat.setMultiplier(-1) did not change the multiplier to -1");
                return;
            }

            // good
        } catch (IllegalArgumentException ex) {
            // bad
            errln("DecimalFormat.setMultiplier(-1) threw an IllegalArgumentException");
            return;
        }

        expect(df, "1122.123", -1122.123);
        expect(df, "-1122.123", 1122.123);
        expect(df, "1.2", -1.2);
        expect(df, "-1.2", 1.2);

        expect2(df, Long.MAX_VALUE, BigInteger.valueOf(Long.MAX_VALUE).negate().toString());
        expect2(df, Long.MIN_VALUE, BigInteger.valueOf(Long.MIN_VALUE).negate().toString());
        expect2(df, Long.MAX_VALUE / 2, BigInteger.valueOf(Long.MAX_VALUE / 2).negate().toString());
        expect2(df, Long.MIN_VALUE / 2, BigInteger.valueOf(Long.MIN_VALUE / 2).negate().toString());

        expect2(df, BigDecimal.valueOf(Long.MAX_VALUE), BigDecimal.valueOf(Long.MAX_VALUE).negate().toString());
        expect2(df, BigDecimal.valueOf(Long.MIN_VALUE), BigDecimal.valueOf(Long.MIN_VALUE).negate().toString());

        expect2(df, java.math.BigDecimal.valueOf(Long.MAX_VALUE), java.math.BigDecimal.valueOf(Long.MAX_VALUE).negate().toString());
        expect2(df, java.math.BigDecimal.valueOf(Long.MIN_VALUE), java.math.BigDecimal.valueOf(Long.MIN_VALUE).negate().toString());
    }

    @Test
    public void TestJB5358() {
        int numThreads = 10;
        String numstr = "12345";
        double expected = 12345;
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        DecimalFormat fmt = new DecimalFormat("#.#", sym);
        ArrayList errors = new ArrayList();

        ParseThreadJB5358[] threads = new ParseThreadJB5358[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new ParseThreadJB5358((DecimalFormat)fmt.clone(), numstr, expected, errors);
            threads[i].start();
        }
        for (int i = 0; i < numThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        if (errors.size() != 0) {
            StringBuffer errBuf = new StringBuffer();
            for (int i = 0; i < errors.size(); i++) {
                errBuf.append((String)errors.get(i));
                errBuf.append("\n");
            }
            errln("FAIL: " + errBuf);
        }
    }

    static private class ParseThreadJB5358 extends Thread {
        private final DecimalFormat decfmt;
        private final String numstr;
        private final double expect;
        private final ArrayList errors;

        public ParseThreadJB5358(DecimalFormat decfmt, String numstr, double expect, ArrayList errors) {
            this.decfmt = decfmt;
            this.numstr = numstr;
            this.expect = expect;
            this.errors = errors;
        }

        @Override
        public void run() {
            for (int i = 0; i < 10000; i++) {
                try {
                    Number n = decfmt.parse(numstr);
                    if (n.doubleValue() != expect) {
                        synchronized(errors) {
                            errors.add(new String("Bad parse result - expected:" + expect + " actual:" + n.doubleValue()));
                        }
                    }
                } catch (Throwable t) {
                    synchronized(errors) {
                        errors.add(new String(t.getClass().getName() + " - " + t.getMessage()));
                    }
                }
            }
        }
    }

    @Test
    public void TestSetCurrency() {
        DecimalFormatSymbols decf1 = DecimalFormatSymbols.getInstance(ULocale.US);
        DecimalFormatSymbols decf2 = DecimalFormatSymbols.getInstance(ULocale.US);
        decf2.setCurrencySymbol("UKD");
        DecimalFormat format1 = new DecimalFormat("000.000", decf1);
        DecimalFormat format2 = new DecimalFormat("000.000", decf2);
        Currency euro = Currency.getInstance("EUR");
        format1.setCurrency(euro);
        format2.setCurrency(euro);
        assertEquals("Reset with currency symbol", format1, format2);
    }

    /*
     * Testing the method public StringBuffer format(Object number, ...)
     */
    @Test
    public void TestFormat() {
        NumberFormat nf = NumberFormat.getInstance();
        StringBuffer sb = new StringBuffer("dummy");
        FieldPosition fp = new FieldPosition(0);

        // Tests when "if (number instanceof Long)" is true
        try {
            nf.format(new Long("0"), sb, fp);
        } catch (Exception e) {
            errln("NumberFormat.format(Object number, ...) was not suppose to "
                    + "return an exception for a Long object. Error: " + e);
        }

        // Tests when "else if (number instanceof BigInteger)" is true
        try {
            nf.format((Object)new BigInteger("0"), sb, fp);
        } catch (Exception e) {
            errln("NumberFormat.format(Object number, ...) was not suppose to "
                    + "return an exception for a BigInteger object. Error: " + e);
        }

        // Tests when "else if (number instanceof java.math.BigDecimal)" is true
        try {
            nf.format((Object)new java.math.BigDecimal("0"), sb, fp);
        } catch (Exception e) {
            errln("NumberFormat.format(Object number, ...) was not suppose to "
                    + "return an exception for a java.math.BigDecimal object. Error: " + e);
        }

        // Tests when "else if (number instanceof com.ibm.icu.math.BigDecimal)" is true
        try {
            nf.format((Object)new com.ibm.icu.math.BigDecimal("0"), sb, fp);
        } catch (Exception e) {
            errln("NumberFormat.format(Object number, ...) was not suppose to "
                    + "return an exception for a com.ibm.icu.math.BigDecimal object. Error: " + e);
        }

        // Tests when "else if (number instanceof CurrencyAmount)" is true
        try {
            CurrencyAmount ca = new CurrencyAmount(0.0, Currency.getInstance(new ULocale("en_US")));
            nf.format((Object)ca, sb, fp);
        } catch (Exception e) {
            errln("NumberFormat.format(Object number, ...) was not suppose to "
                    + "return an exception for a CurrencyAmount object. Error: " + e);
        }

        // Tests when "else if (number instanceof Number)" is true
        try {
            nf.format(0.0, sb, fp);
        } catch (Exception e) {
            errln("NumberFormat.format(Object number, ...) was not suppose to "
                    + "to return an exception for a Number object. Error: " + e);
        }

        // Tests when "else" is true
        try {
            nf.format(new Object(), sb, fp);
            errln("NumberFormat.format(Object number, ...) was suppose to "
                    + "return an exception for an invalid object.");
        } catch (Exception e) {
        }

        try {
            nf.format(new String("dummy"), sb, fp);
            errln("NumberFormat.format(Object number, ...) was suppose to "
                    + "return an exception for an invalid object.");
        } catch (Exception e) {
        }
    }

    /*
     * Coverage tests for the implementation of abstract format methods not being called otherwise
     */
    @Test
    public void TestFormatAbstractImplCoverage() {
        NumberFormat df = DecimalFormat.getInstance(Locale.ENGLISH);
        NumberFormat cdf = CompactDecimalFormat.getInstance(Locale.ENGLISH, CompactDecimalFormat.CompactStyle.SHORT);
        NumberFormat rbf = new RuleBasedNumberFormat(ULocale.ENGLISH, RuleBasedNumberFormat.SPELLOUT);

        /*
         *  Test  NumberFormat.format(BigDecimal,StringBuffer,FieldPosition)
         */
        StringBuffer sb = new StringBuffer();
        String result = df.format(new BigDecimal(2000.43), sb, new FieldPosition(0)).toString();
        if (!"2,000.43".equals(result)) {
            errln("DecimalFormat failed. Expected: 2,000.43 - Actual: " + result);
        }

        sb.delete(0, sb.length());
        result = cdf.format(new BigDecimal(2000.43), sb, new FieldPosition(0)).toString();
        if (!"2K".equals(result)) {
            errln("DecimalFormat failed. Expected: 2K - Actual: " + result);
        }

        sb.delete(0, sb.length());
        result = rbf.format(new BigDecimal(2000.43), sb, new FieldPosition(0)).toString();
        if (!"two thousand point four three".equals(result)) {
            errln("DecimalFormat failed. Expected: 'two thousand point four three' - Actual: '" + result + "'");
        }
    }

    /*
     * Tests the method public final static NumberFormat getInstance(int style) public static NumberFormat
     * getInstance(Locale inLocale, int style) public static NumberFormat getInstance(ULocale desiredLocale, int choice)
     */
    @Test
    public void TestGetInstance() {
        // Tests "public final static NumberFormat getInstance(int style)"
        int maxStyle = NumberFormat.STANDARDCURRENCYSTYLE;

        int[] invalid_cases = { NumberFormat.NUMBERSTYLE - 1, NumberFormat.NUMBERSTYLE - 2,
                maxStyle + 1, maxStyle + 2 };

        for (int i = NumberFormat.NUMBERSTYLE; i < maxStyle; i++) {
            try {
                NumberFormat.getInstance(i);
            } catch (Exception e) {
                errln("NumberFormat.getInstance(int style) was not suppose to "
                        + "return an exception for passing value of " + i);
            }
        }

        for (int i = 0; i < invalid_cases.length; i++) {
            try {
                NumberFormat.getInstance(invalid_cases[i]);
                errln("NumberFormat.getInstance(int style) was suppose to "
                        + "return an exception for passing value of " + invalid_cases[i]);
            } catch (Exception e) {
            }
        }

        // Tests "public static NumberFormat getInstance(Locale inLocale, int style)"
        String[] localeCases = { "en_US", "fr_FR", "de_DE", "jp_JP" };

        for (int i = NumberFormat.NUMBERSTYLE; i < maxStyle; i++) {
            for (int j = 0; j < localeCases.length; j++) {
                try {
                    NumberFormat.getInstance(new Locale(localeCases[j]), i);
                } catch (Exception e) {
                    errln("NumberFormat.getInstance(Locale inLocale, int style) was not suppose to "
                            + "return an exception for passing value of " + localeCases[j] + ", " + i);
                }
            }
        }

        // Tests "public static NumberFormat getInstance(ULocale desiredLocale, int choice)"
        // Tests when "if (choice < NUMBERSTYLE || choice > PLURALCURRENCYSTYLE)" is true
        for (int i = 0; i < invalid_cases.length; i++) {
            try {
                NumberFormat.getInstance((ULocale) null, invalid_cases[i]);
                errln("NumberFormat.getInstance(ULocale inLocale, int choice) was not suppose to "
                        + "return an exception for passing value of " + invalid_cases[i]);
            } catch (Exception e) {
            }
        }
    }

    /*
     * Tests the class public static abstract class NumberFormatFactory
     */
    @Test
    public void TestNumberFormatFactory() {
        /*
         * The following class allows the method public NumberFormat createFormat(Locale loc, int formatType) to be
         * tested.
         */
        class TestFactory extends NumberFormatFactory {
            @Override
            public Set<String> getSupportedLocaleNames() {
                return null;
            }

            @Override
            public NumberFormat createFormat(ULocale loc, int formatType) {
                return null;
            }
        }

        /*
         * The following class allows the method public NumberFormat createFormat(ULocale loc, int formatType) to be
         * tested.
         */
        class TestFactory1 extends NumberFormatFactory {
            @Override
            public Set<String> getSupportedLocaleNames() {
                return null;
            }

            @Override
            public NumberFormat createFormat(Locale loc, int formatType) {
                return null;
            }
        }

        TestFactory tf = new TestFactory();
        TestFactory1 tf1 = new TestFactory1();

        /*
         * Tests the method public boolean visible()
         */
        if (tf.visible() != true) {
            errln("NumberFormatFactory.visible() was suppose to return true.");
        }

        /*
         * Tests the method public NumberFormat createFormat(Locale loc, int formatType)
         */
        if (tf.createFormat(new Locale(""), 0) != null) {
            errln("NumberFormatFactory.createFormat(Locale loc, int formatType) " + "was suppose to return null");
        }

        /*
         * Tests the method public NumberFormat createFormat(ULocale loc, int formatType)
         */
        if (tf1.createFormat(new ULocale(""), 0) != null) {
            errln("NumberFormatFactory.createFormat(ULocale loc, int formatType) " + "was suppose to return null");
        }
    }

    /*
     * Tests the class public static abstract class SimpleNumberFormatFactory extends NumberFormatFactory
     */
    @Test
    public void TestSimpleNumberFormatFactory() {
        class TestSimpleNumberFormatFactory extends SimpleNumberFormatFactory {
            /*
             * Tests the method public SimpleNumberFormatFactory(Locale locale)
             */
            TestSimpleNumberFormatFactory() {
                super(new Locale(""));
            }
        }
        @SuppressWarnings("unused")
        TestSimpleNumberFormatFactory tsnff = new TestSimpleNumberFormatFactory();
    }

    /*
     * Tests the method public static ULocale[] getAvailableLocales()
     */
    @SuppressWarnings("static-access")
    @Test
    public void TestGetAvailableLocales() {
        // Tests when "if (shim == null)" is true
        @SuppressWarnings("serial")
        class TestGetAvailableLocales extends NumberFormat {
            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public StringBuffer format(BigInteger number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public StringBuffer format(java.math.BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public StringBuffer format(BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public Number parse(String text, ParsePosition parsePosition) {
                return null;
            }
        }

        try {
            TestGetAvailableLocales test = new TestGetAvailableLocales();
            test.getAvailableLocales();
        } catch (Exception e) {
            errln("NumberFormat.getAvailableLocales() was not suppose to "
                    + "return an exception when getting getting available locales.");
        }
    }

    /*
     * Tests the method public void setMinimumIntegerDigits(int newValue)
     */
    @Test
    public void TestSetMinimumIntegerDigits() {
        NumberFormat nf = NumberFormat.getInstance();
        // For valid array, it is displayed as {min value, max value}
        // Tests when "if (minimumIntegerDigits > maximumIntegerDigits)" is true
        int[][] cases = { { -1, 0 }, { 0, 1 }, { 1, 0 }, { 2, 0 }, { 2, 1 }, { 10, 0 } };
        int[] expectedMax = { 1, 1, 0, 0, 1, 0 };
        if (cases.length != expectedMax.length) {
            errln("Can't continue test case method TestSetMinimumIntegerDigits "
                    + "since the test case arrays are unequal.");
        } else {
            for (int i = 0; i < cases.length; i++) {
                nf.setMinimumIntegerDigits(cases[i][0]);
                nf.setMaximumIntegerDigits(cases[i][1]);
                if (nf.getMaximumIntegerDigits() != expectedMax[i]) {
                    errln("NumberFormat.setMinimumIntegerDigits(int newValue "
                            + "did not return an expected result for parameter " + cases[i][0] + " and " + cases[i][1]
                                    + " and expected " + expectedMax[i] + " but got " + nf.getMaximumIntegerDigits());
                }
            }
        }
    }

    /*
     * Tests the method public int getRoundingMode() public void setRoundingMode(int roundingMode)
     */
    @Test
    public void TestRoundingMode() {
        @SuppressWarnings("serial")
        class TestRoundingMode extends NumberFormat {
            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public StringBuffer format(BigInteger number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public StringBuffer format(java.math.BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public StringBuffer format(BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
                return null;
            }

            @Override
            public Number parse(String text, ParsePosition parsePosition) {
                return null;
            }
        }
        TestRoundingMode tgrm = new TestRoundingMode();

        // Tests the function 'public void setRoundingMode(int roundingMode)'
        try {
            tgrm.setRoundingMode(0);
            errln("NumberFormat.setRoundingMode(int) was suppose to return an exception");
        } catch (Exception e) {
        }

        // Tests the function 'public int getRoundingMode()'
        try {
            tgrm.getRoundingMode();
            errln("NumberFormat.getRoundingMode() was suppose to return an exception");
        } catch (Exception e) {
        }
    }

    /*
     * Testing lenient decimal/grouping separator parsing
     */
    @Test
    public void TestLenientSymbolParsing() {
        DecimalFormat fmt = new DecimalFormat();
        DecimalFormatSymbols sym = new DecimalFormatSymbols();

        expect(fmt, "12\u300234", 12.34);

        // Ticket#7345 - case 1
        // Even strict parsing, the decimal separator set in the symbols
        // should be successfully parsed.

        sym.setDecimalSeparator('\u3002');

        // non-strict
        fmt.setDecimalFormatSymbols(sym);

        // strict - failed before the fix for #7345
        fmt.setParseStrict(true);
        expect(fmt, "23\u300245", 23.45);
        fmt.setParseStrict(false);


        // Ticket#7345 - case 2
        // Decimal separator variants other than DecimalFormatSymbols.decimalSeparator
        // should not hide the grouping separator DecimalFormatSymbols.groupingSeparator.
        sym.setDecimalSeparator('.');
        sym.setGroupingSeparator(',');
        fmt.setDecimalFormatSymbols(sym);

        expect(fmt, "1,234.56", 1234.56);

        sym.setGroupingSeparator('\uFF61');
        fmt.setDecimalFormatSymbols(sym);

        expect(fmt, "2\uFF61345.67", 2345.67);

        // Ticket#7128
        //
        sym.setGroupingSeparator(',');
        fmt.setDecimalFormatSymbols(sym);

        String skipExtSepParse = ICUConfig.get("com.ibm.icu.text.DecimalFormat.SkipExtendedSeparatorParsing", "false");
        if (skipExtSepParse.equals("true")) {
            // When the property SkipExtendedSeparatorParsing is true,
            // DecimalFormat does not use the extended equivalent separator
            // data and only uses the one in DecimalFormatSymbols.
            expect(fmt, "23 456", 23);
        } else {
            // Lenient separator parsing is enabled by default.
            // A space character below is interpreted as a
            // group separator, even ',' is used as grouping
            // separator in the symbols.
            expect(fmt, "12 345", 12345);
        }
    }

    /*
     * Testing currency driven max/min fraction digits problem
     * reported by ticket#7282
     */
    @Test
    public void TestCurrencyFractionDigits() {
        double value = 99.12345;

        // Create currency instance
        NumberFormat cfmt  = NumberFormat.getCurrencyInstance(new ULocale("ja_JP"));
        String text1 = cfmt.format(value);

        // Reset the same currency and format the test value again
        cfmt.setCurrency(cfmt.getCurrency());
        String text2 = cfmt.format(value);

        // output1 and output2 must be identical
        if (!text1.equals(text2)) {
            errln("NumberFormat.format() should return the same result - text1="
                    + text1 + " text2=" + text2);
        }
    }

    /*
     * Testing rounding to negative zero problem
     * reported by ticket#7609
     */
    @Test
    public void TestNegZeroRounding() {

        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance();
        df.setRoundingMode(MathContext.ROUND_HALF_UP);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(1);
        String text1 = df.format(-0.01);

        df.setRoundingIncrement(0.1);
        String text2 = df.format(-0.01);

        // output1 and output2 must be identical
        if (!text1.equals(text2)) {
            errln("NumberFormat.format() should return the same result - text1="
                    + text1 + " text2=" + text2);
        }

    }

    @Test
    public void TestCurrencyAmountCoverage() {
        CurrencyAmount ca, cb;

        try {
            ca = new CurrencyAmount(null, (Currency) null);
            errln("NullPointerException should have been thrown.");
        } catch (NullPointerException ex) {
        }
        try {
            ca = new CurrencyAmount(new Integer(0), (Currency) null);
            errln("NullPointerException should have been thrown.");
        } catch (NullPointerException ex) {
        }

        ca = new CurrencyAmount(new Integer(0), Currency.getInstance(new ULocale("ja_JP")));
        cb = new CurrencyAmount(new Integer(1), Currency.getInstance(new ULocale("ja_JP")));
        if (ca.equals(null)) {
            errln("Comparison should return false.");
        }
        if (!ca.equals(ca)) {
            errln("comparison should return true.");
        }
        if (ca.equals(cb)) {
            errln("Comparison should return false.");
        }
    }

    @Test
    public void TestExponentParse() {
        ParsePosition parsePos = new ParsePosition(0);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat fmt = new DecimalFormat("#####", symbols);
        Number result = fmt.parse("5.06e-27", parsePos);
        if ( result.doubleValue() != 5.06E-27 || parsePos.getIndex() != 8) {
            errln("ERROR: ERROR: parse failed - expected 5.06E-27, 8; got " + result.doubleValue() + ", " + parsePos.getIndex());
        }
    }

    @Test
    public void TestExplicitParents() {
        // We use these for testing because decimal and grouping separators will be inherited from es_419
        // starting with CLDR 2.0
        String[] DATA = {
                "es", "CO", "", "1.250,75",
                "es", "ES", "", "1.250,75",
                "es", "GQ", "", "1.250,75",
                "es", "MX", "", "1,250.75",
                "es", "US", "", "1,250.75",
                "es", "VE", "", "1.250,75",

        };

        for (int i=0; i<DATA.length; i+=4) {
            Locale locale = new Locale(DATA[i], DATA[i+1], DATA[i+2]);
            NumberFormat fmt = NumberFormat.getInstance(locale);
            String s = fmt.format(1250.75);
            if (s.equals(DATA[i+3])) {
                logln("Ok: 1250.75 x " + locale + " => " + s);
            } else {
                errln("FAIL: 1250.75 x " + locale + " => " + s +
                        ", expected " + DATA[i+3]);
            }
        }
    }

    /*
     * Test case for #9240
     * ICU4J 49.1 DecimalFormat did not clone the internal object holding
     * formatted text attribute information properly. Therefore, DecimalFormat
     * created by cloning may return incorrect results or may throw an exception
     * when formatToCharacterIterator is invoked from multiple threads.
     */
    @Test
    public void TestFormatToCharacterIteratorThread() {
        final int COUNT = 10;

        DecimalFormat fmt1 = new DecimalFormat("#0");
        DecimalFormat fmt2 = (DecimalFormat)fmt1.clone();

        int[] res1 = new int[COUNT];
        int[] res2 = new int[COUNT];

        Thread t1 = new Thread(new FormatCharItrTestThread(fmt1, 1, res1));
        Thread t2 = new Thread(new FormatCharItrTestThread(fmt2, 100, res2));

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            //TODO
        }

        int val1 = res1[0];
        int val2 = res2[0];

        for (int i = 0; i < COUNT; i++) {
            if (res1[i] != val1) {
                errln("Inconsistent first run limit in test thread 1");
            }
            if (res2[i] != val2) {
                errln("Inconsistent first run limit in test thread 2");
            }
        }
    }

    /*
     * This feature had to do with a limitation in DigitList.java that no longer exists in the
     * new implementation.
     *
    @Test
    public void TestParseMaxDigits() {
        DecimalFormat fmt = new DecimalFormat();
        String number = "100000000000";
        int newParseMax = number.length() - 1;

        fmt.setParseMaxDigits(-1);

        // Default value is 1000
        if (fmt.getParseMaxDigits() != 1000) {
            errln("Fail valid value checking in setParseMaxDigits.");
        }

        try {
            if (fmt.parse(number).doubleValue() == Float.POSITIVE_INFINITY) {
                errln("Got Infinity but should NOT when parsing number: " + number);
            }

            fmt.setParseMaxDigits(newParseMax);

            if (fmt.parse(number).doubleValue() != Float.POSITIVE_INFINITY) {
                errln("Did not get Infinity but should when parsing number: " + number);
            }
        } catch (ParseException ex) {

        }
    }
    */

    private static class FormatCharItrTestThread implements Runnable {
        private final NumberFormat fmt;
        private final int num;
        private final int[] result;

        FormatCharItrTestThread(NumberFormat fmt, int num, int[] result) {
            this.fmt = fmt;
            this.num = num;
            this.result = result;
        }

        @Override
        public void run() {
            for (int i = 0; i < result.length; i++) {
                AttributedCharacterIterator acitr = fmt.formatToCharacterIterator(num);
                acitr.first();
                result[i] = acitr.getRunLimit();
            }
        }
    }

    @Test
    public void TestRoundingBehavior() {
        final Object[][] TEST_CASES = {
                {
                    ULocale.US,                             // ULocale - null for default locale
                    "#.##",                                 // Pattern
                    Integer.valueOf(BigDecimal.ROUND_DOWN), // Rounding Mode or null (implicit)
                    Double.valueOf(0.0d),                   // Rounding increment, Double or BigDecimal, or null (implicit)
                    Double.valueOf(123.4567d),              // Input value, Long, Double, BigInteger or BigDecimal
                    "123.45"                                // Expected result, null for exception
                },
                {
                    ULocale.US,
                    "#.##",
                    null,
                    Double.valueOf(0.1d),
                    Double.valueOf(123.4567d),
                    "123.5"
                },
                {
                    ULocale.US,
                    "#.##",
                    Integer.valueOf(BigDecimal.ROUND_DOWN),
                    Double.valueOf(0.1d),
                    Double.valueOf(123.4567d),
                    "123.4"
                },
                {
                    ULocale.US,
                    "#.##",
                    Integer.valueOf(BigDecimal.ROUND_UNNECESSARY),
                    null,
                    Double.valueOf(123.4567d),
                    null
                },
                {
                    ULocale.US,
                    "#.##",
                    Integer.valueOf(BigDecimal.ROUND_DOWN),
                    null,
                    Long.valueOf(1234),
                    "1234"
                },
        };

        int testNum = 1;

        for (Object[] testCase : TEST_CASES) {
            // 0: locale
            // 1: pattern
            ULocale locale = testCase[0] == null ? ULocale.getDefault() : (ULocale)testCase[0];
            String pattern = (String)testCase[1];

            DecimalFormat fmt = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale));

            // 2: rounding mode
            Integer roundingMode = null;
            if (testCase[2] != null) {
                roundingMode = (Integer)testCase[2];
                fmt.setRoundingMode(roundingMode);
            }

            // 3: rounding increment
            if (testCase[3] != null) {
                if (testCase[3] instanceof Double) {
                    fmt.setRoundingIncrement((Double)testCase[3]);
                } else if (testCase[3] instanceof BigDecimal) {
                    fmt.setRoundingIncrement((BigDecimal)testCase[3]);
                } else if (testCase[3] instanceof java.math.BigDecimal) {
                    fmt.setRoundingIncrement((java.math.BigDecimal)testCase[3]);
                }
            }

            // 4: input number
            String s = null;
            boolean bException = false;
            try {
                s = fmt.format(testCase[4]);
            } catch (ArithmeticException e) {
                bException = true;
            }

            if (bException) {
                if (testCase[5] != null) {
                    errln("Test case #" + testNum + ": ArithmeticException was thrown.");
                }
            } else {
                if (testCase[5] == null) {
                    errln("Test case #" + testNum +
                            ": ArithmeticException must be thrown, but got formatted result: " +
                            s);
                } else {
                    assertEquals("Test case #" + testNum, testCase[5], s);
                }
            }

            testNum++;
        }
    }

    @Test
    public void TestSignificantDigits() {
        double input[] = {
                0, 0,
                123, -123,
                12345, -12345,
                123.45, -123.45,
                123.44501, -123.44501,
                0.001234, -0.001234,
                0.00000000123, -0.00000000123,
                0.0000000000000000000123, -0.0000000000000000000123,
                1.2, -1.2,
                0.0000000012344501, -0.0000000012344501,
                123445.01, -123445.01,
                12344501000000000000000000000000000.0, -12344501000000000000000000000000000.0,
        };
        String[] expected = {
                "0.00", "0.00",
                "123", "-123",
                "12345", "-12345",
                "123.45", "-123.45",
                "123.45", "-123.45",
                "0.001234", "-0.001234",
                "0.00000000123", "-0.00000000123",
                "0.0000000000000000000123", "-0.0000000000000000000123",
                "1.20", "-1.20",
                "0.0000000012345", "-0.0000000012345",
                "123450", "-123450",
                "12345000000000000000000000000000000", "-12345000000000000000000000000000000",
        };
        DecimalFormat numberFormat =
                (DecimalFormat) NumberFormat.getInstance(ULocale.US);
        numberFormat.setSignificantDigitsUsed(true);
        numberFormat.setMinimumSignificantDigits(3);
        numberFormat.setMaximumSignificantDigits(5);
        numberFormat.setGroupingUsed(false);
        for (int i = 0; i < input.length; i++) {
            assertEquals("TestSignificantDigits", expected[i], numberFormat.format(input[i]));
        }

        // Test for ICU-20063
        {
            DecimalFormat df = new DecimalFormat("0.######", DecimalFormatSymbols.getInstance(ULocale.US));
            df.setSignificantDigitsUsed(true);
            expect(df, 9.87654321, "9.87654");
            df.setMaximumSignificantDigits(3);
            expect(df, 9.87654321, "9.88");
            // setSignificantDigitsUsed with maxSig only
            df.setSignificantDigitsUsed(true);
            expect(df, 9.87654321, "9.88");
            df.setMinimumSignificantDigits(2);
            expect(df, 9, "9.0");
            // setSignificantDigitsUsed with both minSig and maxSig
            df.setSignificantDigitsUsed(true);
            expect(df, 9, "9.0");
            // setSignificantDigitsUsed to false: should revert to fraction rounding
            df.setSignificantDigitsUsed(false);
            expect(df, 9.87654321, "9.876543");
            expect(df, 9, "9");
            df.setSignificantDigitsUsed(true);
            df.setMinimumSignificantDigits(2);
            expect(df, 9.87654321, "9.87654");
            expect(df, 9, "9.0");
            // setSignificantDigitsUsed with minSig only
            df.setSignificantDigitsUsed(true);
            expect(df, 9.87654321, "9.87654");
            expect(df, 9, "9.0");
        }
    }

    @Test
    public void TestSetMaxFracAndRoundIncr() {
        class SetMxFrAndRndIncrItem {
            String descrip;
            String localeID;
            int    style;
            int    minInt;
            int    minFrac;
            int    maxFrac;
            double roundIncr;
            String expPattern;
            double valueToFmt;
            String expFormat;
             // Simple constructor
            public SetMxFrAndRndIncrItem(String desc, String loc, int stl, int mnI, int mnF, int mxF,
                                                double rdIn, String ePat, double val, String eFmt) {
                descrip = desc;
                localeID = loc;
                style = stl;
                minInt = mnI;
                minFrac = mnF;
                maxFrac = mxF;
                roundIncr = rdIn;
                expPattern = ePat;
                valueToFmt = val;
                expFormat = eFmt ;
            }
        };

        final SetMxFrAndRndIncrItem[] items = {
            //                         descrip                     locale   style                      mnI mnF mxF rdInc   expPat        value  expFmt
            new SetMxFrAndRndIncrItem( "01 en_US DEC 1/0/3/0.0",    "en_US", NumberFormat.NUMBERSTYLE,  1,  0,  3, 0.0,    "#,##0.###",  0.128, "0.128" ),
            new SetMxFrAndRndIncrItem( "02 en_US DEC 1/0/1/0.0",    "en_US", NumberFormat.NUMBERSTYLE,  1,  0,  1, 0.0,    "#,##0.#",    0.128, "0.1"   ),
            new SetMxFrAndRndIncrItem( "03 en_US DEC 1/0/1/0.01",   "en_US", NumberFormat.NUMBERSTYLE,  1,  0,  1, 0.01,   "#,##0.#",    0.128, "0.1"   ),
            new SetMxFrAndRndIncrItem( "04 en_US DEC 1/1/1/0.01",   "en_US", NumberFormat.NUMBERSTYLE,  1,  1,  1, 0.01,   "#,##0.0",    0.128, "0.1"   ),
            new SetMxFrAndRndIncrItem( "05 en_US DEC 1/0/1/0.1",    "en_US", NumberFormat.NUMBERSTYLE,  1,  0,  1, 0.1,    "#,##0.1",    0.128, "0.1"   ), // use incr
            new SetMxFrAndRndIncrItem( "06 en_US DEC 1/1/1/0.1",    "en_US", NumberFormat.NUMBERSTYLE,  1,  1,  1, 0.1,    "#,##0.1",    0.128, "0.1"   ), // use incr

            new SetMxFrAndRndIncrItem( "10 en_US DEC 1/0/1/0.02",   "en_US", NumberFormat.NUMBERSTYLE,  1,  0,  1, 0.02,   "#,##0.#",    0.128, "0.1"   ),
            new SetMxFrAndRndIncrItem( "11 en_US DEC 1/0/2/0.02",   "en_US", NumberFormat.NUMBERSTYLE,  1,  0,  2, 0.02,   "#,##0.02",   0.128, "0.12"  ), // use incr
            new SetMxFrAndRndIncrItem( "12 en_US DEC 1/0/3/0.02",   "en_US", NumberFormat.NUMBERSTYLE,  1,  0,  3, 0.02,   "#,##0.02#",  0.128, "0.12"  ), // use incr
            new SetMxFrAndRndIncrItem( "13 en_US DEC 1/1/1/0.02",   "en_US", NumberFormat.NUMBERSTYLE,  1,  1,  1, 0.02,   "#,##0.0",    0.128, "0.1"   ),
            new SetMxFrAndRndIncrItem( "14 en_US DEC 1/1/2/0.02",   "en_US", NumberFormat.NUMBERSTYLE,  1,  1,  2, 0.02,   "#,##0.02",   0.128, "0.12"  ), // use incr
            new SetMxFrAndRndIncrItem( "15 en_US DEC 1/1/3/0.02",   "en_US", NumberFormat.NUMBERSTYLE,  1,  1,  3, 0.02,   "#,##0.02#",  0.128, "0.12"  ), // use incr
            new SetMxFrAndRndIncrItem( "16 en_US DEC 1/2/2/0.02",   "en_US", NumberFormat.NUMBERSTYLE,  1,  2,  2, 0.02,   "#,##0.02",   0.128, "0.12"  ), // use incr
            new SetMxFrAndRndIncrItem( "17 en_US DEC 1/2/3/0.02",   "en_US", NumberFormat.NUMBERSTYLE,  1,  2,  3, 0.02,   "#,##0.02#",  0.128, "0.12"  ), // use incr
            new SetMxFrAndRndIncrItem( "18 en_US DEC 1/3/3/0.02",   "en_US", NumberFormat.NUMBERSTYLE,  1,  3,  3, 0.02,   "#,##0.020",  0.128, "0.120" ), // use incr

            new SetMxFrAndRndIncrItem( "20 en_US DEC 1/1/1/0.0075", "en_US", NumberFormat.NUMBERSTYLE,  1,  1,  1, 0.0075, "#,##0.0",    0.019, "0.0"    ),
            new SetMxFrAndRndIncrItem( "21 en_US DEC 1/1/2/0.0075", "en_US", NumberFormat.NUMBERSTYLE,  1,  1,  2, 0.0075, "#,##0.0075", 0.004, "0.0075" ), // use incr
            new SetMxFrAndRndIncrItem( "22 en_US DEC 1/1/2/0.0075", "en_US", NumberFormat.NUMBERSTYLE,  1,  1,  2, 0.0075, "#,##0.0075", 0.019, "0.0225" ), // use incr
            new SetMxFrAndRndIncrItem( "23 en_US DEC 1/1/3/0.0075", "en_US", NumberFormat.NUMBERSTYLE,  1,  1,  3, 0.0075, "#,##0.0075", 0.004, "0.0075" ), // use incr
            new SetMxFrAndRndIncrItem( "24 en_US DEC 1/1/3/0.0075", "en_US", NumberFormat.NUMBERSTYLE,  1,  1,  3, 0.0075, "#,##0.0075", 0.019, "0.0225" ), // use incr
            new SetMxFrAndRndIncrItem( "25 en_US DEC 1/2/2/0.0075", "en_US", NumberFormat.NUMBERSTYLE,  1,  2,  2, 0.0075, "#,##0.0075", 0.004, "0.0075" ), // use incr
            new SetMxFrAndRndIncrItem( "26 en_US DEC 1/2/2/0.0075", "en_US", NumberFormat.NUMBERSTYLE,  1,  2,  2, 0.0075, "#,##0.0075", 0.019, "0.0225" ), // use incr
            new SetMxFrAndRndIncrItem( "27 en_US DEC 1/2/3/0.0075", "en_US", NumberFormat.NUMBERSTYLE,  1,  2,  3, 0.0075, "#,##0.0075", 0.004, "0.0075" ), // use incr
            new SetMxFrAndRndIncrItem( "28 en_US DEC 1/2/3/0.0075", "en_US", NumberFormat.NUMBERSTYLE,  1,  2,  3, 0.0075, "#,##0.0075", 0.019, "0.0225" ), // use incr
            new SetMxFrAndRndIncrItem( "29 en_US DEC 1/3/3/0.0075", "en_US", NumberFormat.NUMBERSTYLE,  1,  3,  3, 0.0075, "#,##0.0075", 0.004, "0.0075" ), // use incr
            new SetMxFrAndRndIncrItem( "2A en_US DEC 1/3/3/0.0075", "en_US", NumberFormat.NUMBERSTYLE,  1,  3,  3, 0.0075, "#,##0.0075", 0.019, "0.0225" ), // use incr
        };

        for (SetMxFrAndRndIncrItem item: items) {
            ULocale locale = new ULocale(item.localeID);
            DecimalFormat df = (DecimalFormat)NumberFormat.getInstance(locale, item.style);
            df.setMinimumIntegerDigits(item.minInt);
            df.setMinimumFractionDigits(item.minFrac);
            df.setMaximumFractionDigits(item.maxFrac);
            df.setRoundingIncrement(item.roundIncr);

            boolean roundIncrUsed = (item.roundIncr != 0 &&
                    !PatternStringUtils.ignoreRoundingIncrement(java.math.BigDecimal.valueOf(item.roundIncr),item.maxFrac));
            int fracForRoundIncr = 0;
            if (roundIncrUsed) {
                double  testIncr = item.roundIncr;
                for (; testIncr > ((int)testIncr); testIncr *= 10.0, fracForRoundIncr++);
            }
            if (fracForRoundIncr < item.minFrac) {
                fracForRoundIncr = item.minFrac;
            }

            int minInt = df.getMinimumIntegerDigits();
            if (minInt != item.minInt) {
                errln("test " + item.descrip + ": getMinimumIntegerDigits, expected " + item.minInt + ", got " + minInt);
            }
            int minFrac = df.getMinimumFractionDigits();
            int expMinFrac = (roundIncrUsed)? fracForRoundIncr: item.minFrac;
            if (minFrac != expMinFrac) {
                errln("test " + item.descrip + ": getMinimumFractionDigits, expected " + expMinFrac + ", got " + minFrac);
            }
            int maxFrac = df.getMaximumFractionDigits();
            int expMaxFrac = (roundIncrUsed)? fracForRoundIncr: item.maxFrac;
            if (maxFrac != expMaxFrac) {
                errln("test " + item.descrip + ": getMaximumFractionDigits, expected " + expMaxFrac + ", got " + maxFrac);
            }
            java.math.BigDecimal bigdec = df.getRoundingIncrement(); // why doesn't this return com.ibm.icu.math.BigDecimal?
            double roundIncr = (bigdec != null)? bigdec.doubleValue(): 0.0;
            double expRoundIncr = (roundIncrUsed)? item.roundIncr: 0.0;
            if (roundIncr != expRoundIncr) {
                errln("test " + item.descrip + ": getRoundingIncrement, expected " + expRoundIncr + ", got " + roundIncr);
            }

            String getPattern = df.toPattern();
            if (!getPattern.equals(item.expPattern)) {
                errln("test " + item.descrip + ": toPattern, expected " + item.expPattern + ", got " + getPattern);
            }
            String getFormat = df.format(item.valueToFmt);
            if (!getFormat.equals(item.expFormat)) {
                errln("test " + item.descrip + ": format, expected " + item.expFormat + ", got " + getFormat);
            }
        }
    }

    @Test
    public void TestMinIntMinFracZero() {
        class TestMinIntMinFracItem {
            double value;;
            String expDecFmt;
            String expCurFmt;
             // Simple constructor
            public TestMinIntMinFracItem(double valueIn, String expDecFmtIn, String expCurFmtIn) {
                value = valueIn;
                expDecFmt = expDecFmtIn;
                expCurFmt = expCurFmtIn;
            }
        };

        final TestMinIntMinFracItem[] items = {
            //                              decFmt curFmt
            new TestMinIntMinFracItem( 10.0, "10", "$10" ),
            new TestMinIntMinFracItem(  0.9, ".9", "$.9" ),
            new TestMinIntMinFracItem(  0.0, "0",  "$0"  ),
        };
        int minInt, minFrac;

        NumberFormat decFormat = NumberFormat.getInstance(ULocale.US, NumberFormat.NUMBERSTYLE);
        decFormat.setMinimumIntegerDigits(0);
        decFormat.setMinimumFractionDigits(0);
        minInt = decFormat.getMinimumIntegerDigits();
        minFrac = decFormat.getMinimumFractionDigits();
        if (minInt != 0 || minFrac != 0) {
            errln("after setting DECIMAL  minInt=minFrac=0, get minInt " + minInt + ", minFrac " + minFrac);
        }
        String decPattern = ((DecimalFormat)decFormat).toPattern();
        if (decPattern.length() < 3 || decPattern.indexOf("#.#")< 0) {
            errln("after setting DECIMAL  minInt=minFrac=0, expect pattern to contain \"#.#\", but get " + decPattern);
        }

        NumberFormat curFormat = NumberFormat.getInstance(ULocale.US, NumberFormat.CURRENCYSTYLE);
        curFormat.setMinimumIntegerDigits(0);
        curFormat.setMinimumFractionDigits(0);
        minInt = curFormat.getMinimumIntegerDigits();
        minFrac = curFormat.getMinimumFractionDigits();
        if (minInt != 0 || minFrac != 0) {
            errln("after setting CURRENCY minInt=minFrac=0, get minInt " + minInt + ", minFrac " + minFrac);
        }

        for (TestMinIntMinFracItem item: items) {
            String decString = decFormat.format(item.value);
            if (!decString.equals(item.expDecFmt)) {
                errln("format DECIMAL  value " + item.value + ", expected \"" + item.expDecFmt + "\", got \"" + decString + "\"");
            }
            String curString = curFormat.format(item.value);
            if (!curString.equals(item.expCurFmt)) {
                errln("format CURRENCY value " + item.value + ", expected \"" + item.expCurFmt + "\", got \"" + curString + "\"");
            }
        }
    }

    @Test
    public void TestBug9936() {
        DecimalFormat numberFormat =
                (DecimalFormat) NumberFormat.getInstance(ULocale.US);
        assertFalse("", numberFormat.areSignificantDigitsUsed());

        numberFormat.setSignificantDigitsUsed(true);
        assertTrue("", numberFormat.areSignificantDigitsUsed());

        numberFormat.setSignificantDigitsUsed(false);
        assertFalse("", numberFormat.areSignificantDigitsUsed());

        numberFormat.setMinimumSignificantDigits(3);
        assertTrue("", numberFormat.areSignificantDigitsUsed());

        numberFormat.setSignificantDigitsUsed(false);
        numberFormat.setMaximumSignificantDigits(6);
        assertTrue("", numberFormat.areSignificantDigitsUsed());
    }

    @Test
    public void TestShowZero() {
        DecimalFormat numberFormat =
                (DecimalFormat) NumberFormat.getInstance(ULocale.US);
        numberFormat.setSignificantDigitsUsed(true);
        numberFormat.setMaximumSignificantDigits(3);
        assertEquals("TestShowZero", "0", numberFormat.format(0.0));
    }

    @Test
    public void TestCurrencyPlurals() {
        String[][] tests = {
                {"en", "USD", "1", "1 US dollar"},
                {"en", "USD", "1.0", "1.0 US dollars"},
                {"en", "USD", "1.00", "1.00 US dollars"},
                {"en", "USD", "1.99", "1.99 US dollars"},
                {"en", "AUD", "1", "1 Australian dollar"},
                {"en", "AUD", "1.00", "1.00 Australian dollars"},
                {"sl", "USD", "1", "1 ameri\u0161ki dolar"},
                {"sl", "USD", "2", "2 ameri\u0161ka dolarja"},
                {"sl", "USD", "3", "3 ameri\u0161ki dolarji"},
                {"sl", "USD", "5", "5 ameriških dolarjev"},
                {"fr", "USD", "1.99", "1,99 dollar des États-Unis"},
                {"ru", "RUB", "1", "1 \u0440\u043E\u0441\u0441\u0438\u0439\u0441\u043A\u0438\u0439 \u0440\u0443\u0431\u043B\u044C"},
                {"ru", "RUB", "2", "2 \u0440\u043E\u0441\u0441\u0438\u0439\u0441\u043A\u0438\u0445 \u0440\u0443\u0431\u043B\u044F"},
                {"ru", "RUB", "5", "5 \u0440\u043E\u0441\u0441\u0438\u0439\u0441\u043A\u0438\u0445 \u0440\u0443\u0431\u043B\u0435\u0439"},
        };
        for (String test[] : tests) {
            DecimalFormat numberFormat = (DecimalFormat) DecimalFormat.getInstance(new ULocale(test[0]), NumberFormat.PLURALCURRENCYSTYLE);
            numberFormat.setCurrency(Currency.getInstance(test[1]));
            double number = Double.parseDouble(test[2]);
            int dotPos = test[2].indexOf('.');
            int decimals = dotPos < 0 ? 0 : test[2].length() - dotPos - 1;
            int digits = dotPos < 0 ? test[2].length() : test[2].length() - 1;
            numberFormat.setMaximumFractionDigits(decimals);
            numberFormat.setMinimumFractionDigits(decimals);
            String actual = numberFormat.format(number);
            assertEquals(test[0] + "\t" + test[1] + "\t" + test[2], test[3], actual);
            numberFormat.setMaximumSignificantDigits(digits);
            numberFormat.setMinimumSignificantDigits(digits);
            actual = numberFormat.format(number);
            assertEquals(test[0] + "\t" + test[1] + "\t" + test[2], test[3], actual);
        }
    }

    @Test
    public void TestCustomCurrencySignAndSeparator() {
        DecimalFormatSymbols custom = new DecimalFormatSymbols(ULocale.US);

        custom.setCurrencySymbol("*");
        custom.setMonetaryGroupingSeparator('^');
        custom.setMonetaryDecimalSeparator(':');

        DecimalFormat fmt = new DecimalFormat("\u00A4 #,##0.00", custom);

        final String numstr = "* 1^234:56";
        expect2(fmt, 1234.56, numstr);
    }

    @Test
    public void TestParseSignsAndMarks() {
        class SignsAndMarksItem {
            public String locale;
            public boolean lenient;
            public String numString;
            public double value;
             // Simple constructor
            public SignsAndMarksItem(String loc, boolean lnt, String numStr, double val) {
                locale = loc;
                lenient = lnt;
                numString = numStr;
                value = val;
            }
        };
        final SignsAndMarksItem[] items = {
            // *** Note, ICU4J lenient number parsing does not handle arbitrary whitespace, but can
            // treat some whitespace as a grouping separator. The cases marked *** below depend
            // on isGroupingUsed() being set for the locale, which in turn depends on grouping
            // separators being present in the decimalFormat pattern for the locale (& num sys).
            //
            //                    locale                lenient numString                               value
            new SignsAndMarksItem("en",                 false,  "12",                                    12 ),
            new SignsAndMarksItem("en",                 true,   "12",                                    12 ),
            new SignsAndMarksItem("en",                 false,  "-23",                                  -23 ),
            new SignsAndMarksItem("en",                 true,   "-23",                                  -23 ),
            new SignsAndMarksItem("en",                 true,   "- 23",                                 -23 ), // ***
            new SignsAndMarksItem("en",                 false,  "\u200E-23",                            -23 ),
            new SignsAndMarksItem("en",                 true,   "\u200E-23",                            -23 ),
            new SignsAndMarksItem("en",                 true,   "\u200E- 23",                           -23 ), // ***

            new SignsAndMarksItem("en@numbers=arab",    false,  "\u0663\u0664",                          34 ),
            new SignsAndMarksItem("en@numbers=arab",    true,   "\u0663\u0664",                          34 ),
            new SignsAndMarksItem("en@numbers=arab",    false,  "-\u0664\u0665",                        -45 ),
            new SignsAndMarksItem("en@numbers=arab",    true,   "-\u0664\u0665",                        -45 ),
            new SignsAndMarksItem("en@numbers=arab",    true,   "- \u0664\u0665",                       -45 ), // ***
            new SignsAndMarksItem("en@numbers=arab",    false,  "\u200F-\u0664\u0665",                  -45 ),
            new SignsAndMarksItem("en@numbers=arab",    true,   "\u200F-\u0664\u0665",                  -45 ),
            new SignsAndMarksItem("en@numbers=arab",    true,   "\u200F- \u0664\u0665",                 -45 ), // ***

            new SignsAndMarksItem("en@numbers=arabext", false,  "\u06F5\u06F6",                          56 ),
            new SignsAndMarksItem("en@numbers=arabext", true,   "\u06F5\u06F6",                          56 ),
            new SignsAndMarksItem("en@numbers=arabext", false,  "-\u06F6\u06F7",                        -67 ),
            new SignsAndMarksItem("en@numbers=arabext", true,   "-\u06F6\u06F7",                        -67 ),
            new SignsAndMarksItem("en@numbers=arabext", true,   "- \u06F6\u06F7",                       -67 ), // ***
            new SignsAndMarksItem("en@numbers=arabext", false,  "\u200E-\u200E\u06F6\u06F7",            -67 ),
            new SignsAndMarksItem("en@numbers=arabext", true,   "\u200E-\u200E\u06F6\u06F7",            -67 ),
            new SignsAndMarksItem("en@numbers=arabext", true,   "\u200E-\u200E \u06F6\u06F7",           -67 ), // ***

            new SignsAndMarksItem("he",                 false,  "12",                                    12 ),
            new SignsAndMarksItem("he",                 true,   "12",                                    12 ),
            new SignsAndMarksItem("he",                 false,  "-23",                                  -23 ),
            new SignsAndMarksItem("he",                 true,   "-23",                                  -23 ),
            new SignsAndMarksItem("he",                 true,   "- 23",                                 -23 ), // ***
            new SignsAndMarksItem("he",                 false,  "\u200E-23",                            -23 ),
            new SignsAndMarksItem("he",                 true,   "\u200E-23",                            -23 ),
            new SignsAndMarksItem("he",                 true,   "\u200E- 23",                           -23 ), // ***

            new SignsAndMarksItem("ar",                 false,  "\u0663\u0664",                          34 ),
            new SignsAndMarksItem("ar",                 true,   "\u0663\u0664",                          34 ),
            new SignsAndMarksItem("ar",                 false,  "-\u0664\u0665",                        -45 ),
            new SignsAndMarksItem("ar",                 true,   "-\u0664\u0665",                        -45 ),
            new SignsAndMarksItem("ar",                 true,   "- \u0664\u0665",                       -45 ), // ***
            new SignsAndMarksItem("ar",                 false,  "\u200F-\u0664\u0665",                  -45 ),
            new SignsAndMarksItem("ar",                 true,   "\u200F-\u0664\u0665",                  -45 ),
            new SignsAndMarksItem("ar",                 true,   "\u200F- \u0664\u0665",                 -45 ), // ***

            new SignsAndMarksItem("ar_MA",              false,  "12",                                    12 ),
            new SignsAndMarksItem("ar_MA",              true,   "12",                                    12 ),
            new SignsAndMarksItem("ar_MA",              false,  "-23",                                  -23 ),
            new SignsAndMarksItem("ar_MA",              true,   "-23",                                  -23 ),
            new SignsAndMarksItem("ar_MA",              true,   "- 23",                                 -23 ), // ***
            new SignsAndMarksItem("ar_MA",              false,  "\u200E-23",                            -23 ),
            new SignsAndMarksItem("ar_MA",              true,   "\u200E-23",                            -23 ),
            new SignsAndMarksItem("ar_MA",              true,   "\u200E- 23",                           -23 ), // ***

            new SignsAndMarksItem("fa",                 false,  "\u06F5\u06F6",                          56 ),
            new SignsAndMarksItem("fa",                 true,   "\u06F5\u06F6",                          56 ),
            new SignsAndMarksItem("fa",                 false,  "\u2212\u06F6\u06F7",                   -67 ),
            new SignsAndMarksItem("fa",                 true,   "\u2212\u06F6\u06F7",                   -67 ),
            new SignsAndMarksItem("fa",                 true,   "\u2212 \u06F6\u06F7",                  -67 ), // ***
            new SignsAndMarksItem("fa",                 false,  "\u200E\u2212\u200E\u06F6\u06F7",       -67 ),
            new SignsAndMarksItem("fa",                 true,   "\u200E\u2212\u200E\u06F6\u06F7",       -67 ),
            new SignsAndMarksItem("fa",                 true,   "\u200E\u2212\u200E \u06F6\u06F7",      -67 ), // ***

            new SignsAndMarksItem("ps",                 false,  "\u06F5\u06F6",                          56 ),
            new SignsAndMarksItem("ps",                 true,   "\u06F5\u06F6",                          56 ),
            new SignsAndMarksItem("ps",                 false,  "-\u06F6\u06F7",                        -67 ),
            new SignsAndMarksItem("ps",                 true,   "-\u06F6\u06F7",                        -67 ),
            new SignsAndMarksItem("ps",                 true,   "- \u06F6\u06F7",                       -67 ), // ***
            new SignsAndMarksItem("ps",                 false,  "\u200E-\u200E\u06F6\u06F7",            -67 ),
            new SignsAndMarksItem("ps",                 true,   "\u200E-\u200E\u06F6\u06F7",            -67 ),
            new SignsAndMarksItem("ps",                 true,   "\u200E-\u200E \u06F6\u06F7",           -67 ), // ***
            new SignsAndMarksItem("ps",                 false,  "-\u200E\u06F6\u06F7",                  -67 ),
            new SignsAndMarksItem("ps",                 true,   "-\u200E\u06F6\u06F7",                  -67 ),
            new SignsAndMarksItem("ps",                 true,   "-\u200E \u06F6\u06F7",                 -67 ), // ***
        };
        for (SignsAndMarksItem item: items) {
            ULocale locale = new ULocale(item.locale);
            NumberFormat numfmt = NumberFormat.getInstance(locale);
            if (numfmt != null) {
                numfmt.setParseStrict(!item.lenient);
                ParsePosition ppos = new ParsePosition(0);
                Number num = numfmt.parse(item.numString, ppos);
                if (num != null && ppos.getIndex() == item.numString.length()) {
                    double parsedValue = num.doubleValue();
                    if (parsedValue != item.value) {
                        errln("FAIL: locale " + item.locale + ", lenient " + item.lenient + ", parse of \"" + item.numString + "\" gives value " + parsedValue);
                    }
                } else {
                    errln("FAIL: locale " + item.locale + ", lenient " + item.lenient + ", parse of \"" + item.numString + "\" gives position " + ppos.getIndex());
                }
            } else {
                errln("FAIL: NumberFormat.getInstance for locale " + item.locale);
            }
        }
    }

    @Test
    public void TestContext() {
        // just a minimal sanity check for now
        NumberFormat nfmt = NumberFormat.getInstance();
        DisplayContext context = nfmt.getContext(DisplayContext.Type.CAPITALIZATION);
        if (context != DisplayContext.CAPITALIZATION_NONE) {
            errln("FAIL: Initial NumberFormat.getContext() is not CAPITALIZATION_NONE");
        }
        nfmt.setContext(DisplayContext.CAPITALIZATION_FOR_STANDALONE);
        context = nfmt.getContext(DisplayContext.Type.CAPITALIZATION);
        if (context != DisplayContext.CAPITALIZATION_FOR_STANDALONE) {
            errln("FAIL: NumberFormat.getContext() does not return the value set, CAPITALIZATION_FOR_STANDALONE");
        }
    }

    @Test
    public void TestAccountingCurrency() {
        String[][] tests = {
                //locale              num         curr fmt per loc     curr std fmt         curr acct fmt        rt
                {"en_US",             "1234.5",   "$1,234.50",         "$1,234.50",         "$1,234.50",         "true"},
                {"en_US@cf=account",  "1234.5",   "$1,234.50",         "$1,234.50",         "$1,234.50",         "true"},
                {"en_US",             "-1234.5",  "-$1,234.50",        "-$1,234.50",        "($1,234.50)",       "true"},
                {"en_US@cf=standard", "-1234.5",  "-$1,234.50",        "-$1,234.50",        "($1,234.50)",       "true"},
                {"en_US@cf=account",  "-1234.5",  "($1,234.50)",       "-$1,234.50",        "($1,234.50)",       "true"},
                {"en_US",             "0",        "$0.00",             "$0.00",             "$0.00",             "true"},
                {"en_US",             "-0.2",     "-$0.20",            "-$0.20",            "($0.20)",           "true"},
                {"en_US@cf=standard", "-0.2",     "-$0.20",            "-$0.20",            "($0.20)",           "true"},
                {"en_US@cf=account",  "-0.2",     "($0.20)",           "-$0.20",            "($0.20)",           "true"},
                {"ja_JP",             "10000",    "￥10,000",          "￥10,000",          "￥10,000",          "true" },
                {"ja_JP",             "-1000.5",  "-￥1,000",          "-￥1,000",          "(￥1,000)",         "false"},
                {"ja_JP@cf=account",  "-1000.5",  "(￥1,000)",         "-￥1,000",          "(￥1,000)",         "false"},
                {"de_DE",             "-23456.7", "-23.456,70\u00A0€", "-23.456,70\u00A0€", "-23.456,70\u00A0€", "true" },
                {"en_ID",             "1234.5",   "IDR 1,234.50",      "IDR 1,234.50",      "IDR 1,234.50",      "true"},
                {"en_ID@cf=account",  "1234.5",   "IDR 1,234.50",      "IDR 1,234.50",      "IDR 1,234.50",      "true"},
                {"en_ID@cf=standard", "1234.5",   "IDR 1,234.50",      "IDR 1,234.50",      "IDR 1,234.50",      "true"},
                {"en_ID",             "-1234.5",  "-IDR 1,234.50",     "-IDR 1,234.50",     "(IDR 1,234.50)",    "true"},
                {"en_ID@cf=account",  "-1234.5",  "(IDR 1,234.50)",    "-IDR 1,234.50",     "(IDR 1,234.50)",    "true"},
                {"en_ID@cf=standard", "-1234.5",  "-IDR 1,234.50",     "-IDR 1,234.50",     "(IDR 1,234.50)",    "true"},
                {"sh_ME",             "1234.5",   "1.234,50 €",        "1.234,50 €",        "1.234,50 €",        "true"},
                {"sh_ME@cf=account",  "1234.5",   "1.234,50 €",        "1.234,50 €",        "1.234,50 €",        "true"},
                {"sh_ME@cf=standard", "1234.5",   "1.234,50 €",        "1.234,50 €",        "1.234,50 €",        "true"},
                {"sh_ME",             "-1234.5",  "-1.234,50 €",       "-1.234,50 €",       "(1.234,50 €)",      "true"},
                {"sh_ME@cf=account",  "-1234.5",  "(1.234,50 €)",      "-1.234,50 €",       "(1.234,50 €)",      "true"},
                {"sh_ME@cf=standard", "-1234.5",  "-1.234,50 €",       "-1.234,50 €",       "(1.234,50 €)",      "true"},
        };
        for (String[] data : tests) {
            ULocale loc = new ULocale(data[0]);
            double num = Double.parseDouble(data[1]);
            String fmtPerLocExpected   = data[2];
            String fmtStandardExpected = data[3];
            String fmtAccountExpected  = data[4];
            boolean rt = Boolean.parseBoolean(data[5]);

            NumberFormat fmtPerLoc = NumberFormat.getInstance(loc, NumberFormat.CURRENCYSTYLE);
            expect(fmtPerLoc, num, fmtPerLocExpected, rt);

            NumberFormat fmtStandard = NumberFormat.getInstance(loc, NumberFormat.STANDARDCURRENCYSTYLE);
            expect(fmtStandard, num, fmtStandardExpected, rt);

            NumberFormat fmtAccount = NumberFormat.getInstance(loc, NumberFormat.ACCOUNTINGCURRENCYSTYLE);
            expect(fmtAccount, num, fmtAccountExpected, rt);
        }
    }

    /**
     * en_ID/sh_ME uses language only locales en/sh which requires NumberFormatServiceShim to fill in the currency, but
     * prior to ICU-20631, currency was not filled in for accounting, cash and standard, so currency placeholder was
     * used instead of the desired locale's currency.
     */
    @Test
    public void TestCurrencyFormatForMissingLocale() {
        ULocale loc = new ULocale("sh_ME");
        Currency cur = Currency.getInstance(loc);
        NumberFormat curFmt = NumberFormat.getInstance(loc, NumberFormat.CURRENCYSTYLE);
        assertNotNull("NumberFormat is missing currency instance for CURRENCYSTYLE", curFmt.getCurrency());
        assertEquals("Currency instance is not for the desired locale for CURRENCYSTYLE", cur, curFmt.getCurrency());
        assertEquals("NumberFormat format outputs wrong value for CURRENCYSTYLE", "-1.234,50 €", curFmt.format(-1234.5d));
        NumberFormat accFmt = NumberFormat.getInstance(loc, NumberFormat.ACCOUNTINGCURRENCYSTYLE);
        assertNotNull("NumberFormat is missing currency instance for ACCOUNTINGCURRENCYSTYLE", accFmt.getCurrency());
        assertEquals("Currency instance is not for the desired locale for ACCOUNTINGCURRENCYSTYLE", cur, accFmt.getCurrency());
        assertEquals("NumberFormat format outputs wrong value for ACCOUNTINGCURRENCYSTYLE", "(1.234,50 €)", accFmt.format(-1234.5d));
        NumberFormat cashFmt = NumberFormat.getInstance(loc, NumberFormat.CASHCURRENCYSTYLE);
        assertNotNull("NumberFormat is missing currency instance for CASHCURRENCYSTYLE", cashFmt.getCurrency());
        assertEquals("Currency instance is not for the desired locale for CASHCURRENCYSTYLE", cur, cashFmt.getCurrency());
        assertEquals("NumberFormat format outputs wrong value for CASHCURRENCYSTYLE", "-1.234,50 €", cashFmt.format(-1234.5d));
        NumberFormat stdFmt = NumberFormat.getInstance(loc, NumberFormat.STANDARDCURRENCYSTYLE);
        assertNotNull("NumberFormat is missing currency instance for STANDARDCURRENCYSTYLE", stdFmt.getCurrency());
        assertEquals("Currency instance is not for the desired locale for STANDARDCURRENCYSTYLE", cur, stdFmt.getCurrency());
        assertEquals("NumberFormat format outputs wrong value for STANDARDCURRENCYSTYLE", "-1.234,50 €", stdFmt.format(-1234.5d));
    }

    @Test
    public void TestCurrencyUsage() {
        // the 1st one is checking setter/getter, while the 2nd one checks for getInstance
        // compare the Currency and Currency Cash Digits
        // Note that as of CLDR 26:
        // * TWD and PKR switched from 0 decimals to 2; ISK still has 0, so change test to that
        // * CAD rounds to .05 in the cash style only.
        for (int i = 0; i < 2; i++) {
            String original_expected = "ISK 124";
            DecimalFormat custom = null;
            if (i == 0) {
                custom = (DecimalFormat) DecimalFormat.getInstance(new ULocale("en_US@currency=ISK"),
                        DecimalFormat.CURRENCYSTYLE);

                String original = custom.format(123.567);
                assertEquals("Test Currency Context", original_expected, original);

                // test the getter
                assertEquals("Test Currency Context Purpose", custom.getCurrencyUsage(),
                        Currency.CurrencyUsage.STANDARD);
                custom.setCurrencyUsage(Currency.CurrencyUsage.CASH);
                assertEquals("Test Currency Context Purpose", custom.getCurrencyUsage(), Currency.CurrencyUsage.CASH);
            } else {
                custom = (DecimalFormat) DecimalFormat.getInstance(new ULocale("en_US@currency=ISK"),
                        DecimalFormat.CASHCURRENCYSTYLE);

                // test the getter
                assertEquals("Test Currency Context Purpose", custom.getCurrencyUsage(), Currency.CurrencyUsage.CASH);
            }

            String cash_currency = custom.format(123.567);
            String cash_currency_expected = "ISK 124";
            assertEquals("Test Currency Context", cash_currency_expected, cash_currency);
        }

        // the 1st one is checking setter/getter, while the 2nd one checks for getInstance
        // compare the Currency and Currency Cash Rounding
        for (int i = 0; i < 2; i++) {
            String original_rounding_expected = "CA$123.57";
            DecimalFormat fmt = null;
            if (i == 0) {
                fmt = (DecimalFormat) DecimalFormat.getInstance(new ULocale("en_US@currency=CAD"),
                        DecimalFormat.CURRENCYSTYLE);

                String original_rounding = fmt.format(123.566);
                assertEquals("Test Currency Context", original_rounding_expected, original_rounding);

                fmt.setCurrencyUsage(Currency.CurrencyUsage.CASH);
            } else {
                fmt = (DecimalFormat) DecimalFormat.getInstance(new ULocale("en_US@currency=CAD"),
                        DecimalFormat.CASHCURRENCYSTYLE);
            }

            String cash_rounding_currency = fmt.format(123.567);
            String cash__rounding_currency_expected = "CA$123.55";
            assertEquals("Test Currency Context", cash__rounding_currency_expected, cash_rounding_currency);
        }

        // the 1st one is checking setter/getter, while the 2nd one checks for getInstance
        // Test the currency change
        for (int i = 0; i < 2; i++) {
            DecimalFormat fmt2 = null;
            if (i == 1) {
                fmt2 = (DecimalFormat) NumberFormat.getInstance(new ULocale("en_US@currency=JPY"),
                        NumberFormat.CURRENCYSTYLE);
                fmt2.setCurrencyUsage(Currency.CurrencyUsage.CASH);
            } else {
                fmt2 = (DecimalFormat) NumberFormat.getInstance(new ULocale("en_US@currency=JPY"),
                        NumberFormat.CASHCURRENCYSTYLE);
            }

            fmt2.setCurrency(Currency.getInstance("PKR"));
            String PKR_changed = fmt2.format(123.567);
            String PKR_changed_expected = "PKR 124";
            assertEquals("Test Currency Context", PKR_changed_expected, PKR_changed);
        }
    }

    @Test
    public void TestCurrencyWithMinMaxFractionDigits() {
        DecimalFormat df = new DecimalFormat();
        df.applyPattern("¤#,##0.00");
        df.setCurrency(Currency.getInstance("USD"));
        assertEquals("Basic currency format fails", "$1.23", df.format(1.234));
        df.setMaximumFractionDigits(4);
        assertEquals("Currency with max fraction == 4", "$1.234", df.format(1.234));
        df.setMinimumFractionDigits(4);
        assertEquals("Currency with min fraction == 4", "$1.2340", df.format(1.234));
    }

    @Test
    public void TestParseRequiredDecimalPoint() {

        String[] testPattern = { "00.####", "00.0", "00" };

        String value2Parse = "99";
        String value2ParseWithDecimal = "99.9";
        double parseValue  =  99;
        double parseValueWithDecimal = 99.9;
        DecimalFormat parser = new DecimalFormat();
        double result;
        boolean hasDecimalPoint;
        for (int i = 0; i < testPattern.length; i++) {
            parser.applyPattern(testPattern[i]);
            hasDecimalPoint = testPattern[i].contains(".");

            parser.setDecimalPatternMatchRequired(false);
            try {
                result = parser.parse(value2Parse).doubleValue();
                assertEquals("wrong parsed value", parseValue, result);
            } catch (ParseException e) {
                TestFmwk.errln("Parsing " + value2Parse + " should have succeeded with " + testPattern[i] +
                            " and isDecimalPointMatchRequired set to: " + parser.isDecimalPatternMatchRequired());
            }
            try {
                result = parser.parse(value2ParseWithDecimal).doubleValue();
                assertEquals("wrong parsed value", parseValueWithDecimal, result);
            } catch (ParseException e) {
                TestFmwk.errln("Parsing " + value2ParseWithDecimal + " should have succeeded with " + testPattern[i] +
                            " and isDecimalPointMatchRequired set to: " + parser.isDecimalPatternMatchRequired());
            }

            parser.setDecimalPatternMatchRequired(true);
            try {
                result = parser.parse(value2Parse).doubleValue();
                if(hasDecimalPoint){
                    TestFmwk.errln("Parsing " + value2Parse + " should NOT have succeeded with " + testPattern[i] +
                            " and isDecimalPointMatchRequired set to: " + parser.isDecimalPatternMatchRequired());
                }
            } catch (ParseException e) {
                    // OK, should fail
            }
            try {
                result = parser.parse(value2ParseWithDecimal).doubleValue();
                if(!hasDecimalPoint){
                    TestFmwk.errln("Parsing " + value2ParseWithDecimal + " should NOT have succeeded with " + testPattern[i] +
                            " and isDecimalPointMatchRequired set to: " + parser.isDecimalPatternMatchRequired() +
                            " (got: " + result + ")");
                }
            } catch (ParseException e) {
                    // OK, should fail
            }
        }
    }

    @Test
    public void TestCurrFmtNegSameAsPositive() {
        DecimalFormatSymbols decfmtsym = DecimalFormatSymbols.getInstance(Locale.US);
        decfmtsym.setMinusSign('\u200B'); // ZERO WIDTH SPACE, in ICU4J cannot set to empty string
        DecimalFormat decfmt = new DecimalFormat("\u00A4#,##0.00;-\u00A4#,##0.00", decfmtsym);
        String currFmtResult = decfmt.format(-100.0);
        if (!currFmtResult.equals("\u200B$100.00")) {
            errln("decfmt.toPattern results wrong, expected \u200B$100.00, got " + currFmtResult);
        }
    }

    @Test
    public void TestNumberFormatTestDataToString() {
        new DataDrivenNumberFormatTestData().toString();
    }

   // Testing for Issue 11805.
    @Test
    public void TestFormatToCharacterIteratorIssue11805 () {
        final double number = -350.76;
        DecimalFormat dfUS = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.US);
        String strUS = dfUS.format(number);
        Set<AttributedCharacterIterator.Attribute> resultUS  = dfUS.formatToCharacterIterator(number).getAllAttributeKeys();
        assertEquals("Negative US Results: " + strUS, 5, resultUS.size());

        // For each test, add assert that all the fields are present and in the right spot.
        // TODO: Add tests for identify and position of each field, as in IntlTestDecimalFormatAPIC.

        DecimalFormat dfDE = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.GERMANY);
        String strDE = dfDE.format(number);
        Set<AttributedCharacterIterator.Attribute> resultDE  = dfDE.formatToCharacterIterator(number).getAllAttributeKeys();
        assertEquals("Negative DE Results: " + strDE, 5, resultDE.size());

        DecimalFormat dfIN = (DecimalFormat) DecimalFormat.getCurrencyInstance(new Locale("hi", "in"));
        String strIN = dfIN.format(number);
        Set<AttributedCharacterIterator.Attribute> resultIN  = dfIN.formatToCharacterIterator(number).getAllAttributeKeys();
        assertEquals("Negative IN Results: " + strIN, 5, resultIN.size());

        DecimalFormat dfJP = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.JAPAN);
        String strJP = dfJP.format(number);
        Set<AttributedCharacterIterator.Attribute> resultJP  = dfJP.formatToCharacterIterator(number).getAllAttributeKeys();
        assertEquals("Negative JA Results: " + strJP, 3, resultJP.size());

        DecimalFormat dfGB = (DecimalFormat) DecimalFormat.getCurrencyInstance(new Locale("en", "gb"));
        String strGB = dfGB.format(number);
        Set<AttributedCharacterIterator.Attribute> resultGB  = dfGB.formatToCharacterIterator(number).getAllAttributeKeys();
        assertEquals("Negative GB Results: " + strGB , 5, resultGB.size());

        DecimalFormat dfPlural = (DecimalFormat) NumberFormat.getInstance(new Locale("en", "gb"),
            NumberFormat.PLURALCURRENCYSTYLE);
        strGB = dfPlural.format(number);
        resultGB = dfPlural.formatToCharacterIterator(number).getAllAttributeKeys();
        assertEquals("Negative GB Results: " + strGB , 5, resultGB.size());

        strGB = dfPlural.format(1);
        resultGB = dfPlural.formatToCharacterIterator(1).getAllAttributeKeys();
        assertEquals("Negative GB Results: " + strGB , 4, resultGB.size());

        // Test output with unit value.
        DecimalFormat auPlural = (DecimalFormat) NumberFormat.getInstance(new Locale("en", "au"),
                NumberFormat.PLURALCURRENCYSTYLE);
        String strAU = auPlural.format(1L);
        Set<AttributedCharacterIterator.Attribute> resultAU  =
                auPlural.formatToCharacterIterator(1L).getAllAttributeKeys();
        assertEquals("Unit AU Result: " + strAU , 4, resultAU.size());

        // Verify Permille fields.
        DecimalFormatSymbols sym = new DecimalFormatSymbols(new Locale("en", "gb"));
        DecimalFormat dfPermille = new DecimalFormat("####0.##\u2030", sym);
        strGB = dfPermille.format(number);
        resultGB = dfPermille.formatToCharacterIterator(number).getAllAttributeKeys();
        assertEquals("Negative GB Permille Results: " + strGB , 3, resultGB.size());
    }

    // Testing for Issue 11808.
    @Test
    public void TestRoundUnnecessarytIssue11808 () {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();
        StringBuffer result = new StringBuffer("");
        df.setRoundingMode(BigDecimal.ROUND_UNNECESSARY);
        df.applyPattern("00.0#E0");

        try {
            df.format(99999.0, result, new FieldPosition(0));
            fail("Missing ArithmeticException for double: " + result);
        } catch (ArithmeticException expected) {
            // The exception should be thrown, since rounding is needed.
        }

        try {
            result = df.format(99999, result, new FieldPosition(0));
            fail("Missing ArithmeticException for int: " + result);
       } catch (ArithmeticException expected) {
           // The exception should be thrown, since rounding is needed.
        }

        try {
            result = df.format(new BigInteger("999999"), result, new FieldPosition(0));
            fail("Missing ArithmeticException for BigInteger: " + result);
        } catch (ArithmeticException expected) {
            // The exception should be thrown, since rounding is needed.
        }

        try {
            result = df.format(new BigDecimal("99999"), result, new FieldPosition(0));
            fail("Missing ArithmeticException for BigDecimal: " + result);
        } catch (ArithmeticException expected) {
            // The exception should be thrown, since rounding is needed.
        }

        try {
            result = df.format(new BigDecimal("-99999"), result, new FieldPosition(0));
            fail("Missing ArithmeticException for BigDecimal: " + result);
        } catch (ArithmeticException expected) {
            // The exception should be thrown, since rounding is needed.
        }
    }

    // Testing for Issue 11735.
    @Test
    public void TestNPEIssue11735() {
        DecimalFormat fmt = new DecimalFormat("0", new DecimalFormatSymbols(new ULocale("en")));
        ParsePosition ppos = new ParsePosition(0);
        assertEquals("Currency symbol missing in parse. Expect null result.",
                fmt.parseCurrency("53.45", ppos), null);
    }

    private void CompareAttributedCharacterFormatOutput(AttributedCharacterIterator iterator,
        List<FieldContainer> expected, String formattedOutput) {

        List<FieldContainer> result = new ArrayList<>();
        while (iterator.getIndex() != iterator.getEndIndex()) {
            int start = iterator.getRunStart();
            int end = iterator.getRunLimit();
            Iterator it = iterator.getAttributes().keySet().iterator();
            AttributedCharacterIterator.Attribute attribute = (AttributedCharacterIterator.Attribute) it.next();
            // For positions with both INTEGER and GROUPING attributes, we want the GROUPING attribute.
            if (it.hasNext() && attribute.equals(NumberFormat.Field.INTEGER)) {
                attribute = (AttributedCharacterIterator.Attribute) it.next();
            }
            Object value = iterator.getAttribute(attribute);
            result.add(new FieldContainer(start, end, attribute, value));
            iterator.setIndex(end);
        }
        assertEquals("Comparing vector length for " + formattedOutput,
            expected.size(), result.size());

        if (!expected.containsAll(result)) {
          // Print information on the differences.
          for (int i = 0; i < expected.size(); i++) {
            System.out.println("     expected[" + i + "] =" +
                expected.get(i).start + " " +
                expected.get(i).end + " " +
                expected.get(i).attribute + " " +
                expected.get(i).value);
            System.out.println(" result[" + i + "] =" +
                result.get(i).start + " " +
                result.get(i).end + " " +
                result.get(i).attribute + " " +
                result.get(i).value);
          }
        }
        assertTrue("Comparing vector results for " + formattedOutput, expected.containsAll(result));
    }

    // Testing for Issue 11914, missing FieldPositions for some field types.
    @Test
    public void TestNPEIssue11914() {
        // First test: Double value with grouping separators.
        List<FieldContainer> v1 = new ArrayList<>(7);
        v1.add(new FieldContainer(0, 3, NumberFormat.Field.INTEGER));
        v1.add(new FieldContainer(3, 4, NumberFormat.Field.GROUPING_SEPARATOR));
        v1.add(new FieldContainer(4, 7, NumberFormat.Field.INTEGER));
        v1.add(new FieldContainer(7, 8, NumberFormat.Field.GROUPING_SEPARATOR));
        v1.add(new FieldContainer(8, 11, NumberFormat.Field.INTEGER));
        v1.add(new FieldContainer(11, 12, NumberFormat.Field.DECIMAL_SEPARATOR));
        v1.add(new FieldContainer(12, 15, NumberFormat.Field.FRACTION));

        Number number = new Double(123456789.9753);
        ULocale usLoc = new ULocale("en-US");
        DecimalFormatSymbols US = new DecimalFormatSymbols(usLoc);

        NumberFormat outFmt = NumberFormat.getNumberInstance(usLoc);
        String numFmtted = outFmt.format(number);
        AttributedCharacterIterator iterator =
                outFmt.formatToCharacterIterator(number);
        CompareAttributedCharacterFormatOutput(iterator, v1, numFmtted);

        // Second test: Double with scientific notation formatting.
        List<FieldContainer> v2 = new ArrayList<>(7);
        v2.add(new FieldContainer(0, 1, NumberFormat.Field.INTEGER));
        v2.add(new FieldContainer(1, 2, NumberFormat.Field.DECIMAL_SEPARATOR));
        v2.add(new FieldContainer(2, 5, NumberFormat.Field.FRACTION));
        v2.add(new FieldContainer(5, 6, NumberFormat.Field.EXPONENT_SYMBOL));
        v2.add(new FieldContainer(6, 7, NumberFormat.Field.EXPONENT_SIGN));
        v2.add(new FieldContainer(7, 8, NumberFormat.Field.EXPONENT));
        DecimalFormat fmt2 = new DecimalFormat("0.###E+0", US);

        numFmtted = fmt2.format(number);
        iterator = fmt2.formatToCharacterIterator(number);
        CompareAttributedCharacterFormatOutput(iterator, v2, numFmtted);

        // Third test. BigInteger with grouping separators.
        List<FieldContainer> v3 = new ArrayList<>(7);
        v3.add(new FieldContainer(0, 1, NumberFormat.Field.SIGN));
        v3.add(new FieldContainer(1, 2, NumberFormat.Field.INTEGER));
        v3.add(new FieldContainer(2, 3, NumberFormat.Field.GROUPING_SEPARATOR));
        v3.add(new FieldContainer(3, 6, NumberFormat.Field.INTEGER));
        v3.add(new FieldContainer(6, 7, NumberFormat.Field.GROUPING_SEPARATOR));
        v3.add(new FieldContainer(7, 10, NumberFormat.Field.INTEGER));
        v3.add(new FieldContainer(10, 11, NumberFormat.Field.GROUPING_SEPARATOR));
        v3.add(new FieldContainer(11, 14, NumberFormat.Field.INTEGER));
        v3.add(new FieldContainer(14, 15, NumberFormat.Field.GROUPING_SEPARATOR));
        v3.add(new FieldContainer(15, 18, NumberFormat.Field.INTEGER));
        v3.add(new FieldContainer(18, 19, NumberFormat.Field.GROUPING_SEPARATOR));
        v3.add(new FieldContainer(19, 22, NumberFormat.Field.INTEGER));
        v3.add(new FieldContainer(22, 23, NumberFormat.Field.GROUPING_SEPARATOR));
        v3.add(new FieldContainer(23, 26, NumberFormat.Field.INTEGER));
        BigInteger bigNumberInt = new BigInteger("-1234567890246813579");
        String fmtNumberBigInt = outFmt.format(bigNumberInt);

        iterator = outFmt.formatToCharacterIterator(bigNumberInt);
        CompareAttributedCharacterFormatOutput(iterator, v3, fmtNumberBigInt);

        // Fourth test: BigDecimal with exponential formatting.
        List<FieldContainer> v4 = new ArrayList<>(7);
        v4.add(new FieldContainer(0, 1, NumberFormat.Field.SIGN));
        v4.add(new FieldContainer(1, 2, NumberFormat.Field.INTEGER));
        v4.add(new FieldContainer(2, 3, NumberFormat.Field.DECIMAL_SEPARATOR));
        v4.add(new FieldContainer(3, 6, NumberFormat.Field.FRACTION));
        v4.add(new FieldContainer(6, 7, NumberFormat.Field.EXPONENT_SYMBOL));
        v4.add(new FieldContainer(7, 8, NumberFormat.Field.EXPONENT_SIGN));
        v4.add(new FieldContainer(8, 9, NumberFormat.Field.EXPONENT));

        java.math.BigDecimal numberBigD = new java.math.BigDecimal(-123456789);
        String fmtNumberBigDExp = fmt2.format(numberBigD);

        iterator = fmt2.formatToCharacterIterator(numberBigD);
        CompareAttributedCharacterFormatOutput(iterator, v4, fmtNumberBigDExp);

    }

    // Test that the decimal is shown even when there are no fractional digits
    @Test
    public void Test11621() throws Exception {
        String pat = "0.##E0";

        DecimalFormatSymbols icuSym = new DecimalFormatSymbols(Locale.US);
        DecimalFormat icuFmt = new DecimalFormat(pat, icuSym);
        icuFmt.setDecimalSeparatorAlwaysShown(true);
        String icu = ((NumberFormat)icuFmt).format(299792458);

        java.text.DecimalFormatSymbols jdkSym = new java.text.DecimalFormatSymbols(Locale.US);
        java.text.DecimalFormat jdkFmt = new java.text.DecimalFormat(pat,jdkSym);
        jdkFmt.setDecimalSeparatorAlwaysShown(true);
        String jdk = ((java.text.NumberFormat)jdkFmt).format(299792458);

        assertEquals("ICU and JDK placement of decimal in exponent", jdk, icu);
    }

    private void checkFormatWithField(String testInfo, Format format, Object object,
            String expected, Format.Field field, int begin, int end) {
        StringBuffer buffer = new StringBuffer();
        FieldPosition pos = new FieldPosition(field);
        format.format(object, buffer, pos);

        assertEquals("Test " + testInfo + ": incorrect formatted text", expected, buffer.toString());

        if (begin != pos.getBeginIndex() || end != pos.getEndIndex()) {
            assertEquals("Index mismatch", field + " " + begin + ".." + end,
                pos.getFieldAttribute() + " " + pos.getBeginIndex() + ".." + pos.getEndIndex());
        }
    }

    @Test
    public void TestMissingFieldPositionsCurrency() {
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getCurrencyInstance(ULocale.US);
        Number number = new Double(92314587.66);
        String result = "$92,314,587.66";

        checkFormatWithField("currency", formatter, number, result,
            NumberFormat.Field.CURRENCY, 0, 1);
        checkFormatWithField("integer", formatter, number, result,
            NumberFormat.Field.INTEGER, 1, 11);
        checkFormatWithField("grouping separator", formatter, number, result,
            NumberFormat.Field.GROUPING_SEPARATOR, 3, 4);
        checkFormatWithField("decimal separator", formatter, number, result,
            NumberFormat.Field.DECIMAL_SEPARATOR, 11, 12);
        checkFormatWithField("fraction", formatter, number, result,
            NumberFormat.Field.FRACTION, 12, 14);
    }

    @Test
    public void TestMissingFieldPositionsNegativeDouble() {
        // test for exponential fields with double
        DecimalFormatSymbols us_symbols = new DecimalFormatSymbols(ULocale.US);
        Number number = new Double(-12345678.90123);
        DecimalFormat formatter = new DecimalFormat("0.#####E+00", us_symbols);
        String numFmtted = formatter.format(number);

        checkFormatWithField("sign", formatter, number, numFmtted,
            NumberFormat.Field.SIGN, 0, 1);
        checkFormatWithField("integer", formatter, number, numFmtted,
            NumberFormat.Field.INTEGER, 1, 2);
        checkFormatWithField("decimal separator", formatter, number, numFmtted,
            NumberFormat.Field.DECIMAL_SEPARATOR, 2, 3);
        checkFormatWithField("exponent symbol", formatter, number, numFmtted,
            NumberFormat.Field.EXPONENT_SYMBOL, 8, 9);
        checkFormatWithField("exponent sign", formatter, number, numFmtted,
            NumberFormat.Field.EXPONENT_SIGN, 9, 10);
        checkFormatWithField("exponent", formatter, number, numFmtted,
            NumberFormat.Field.EXPONENT, 10, 12);
    }

    @Test
    public void TestMissingFieldPositionsPerCent() {
        // Check PERCENT
        DecimalFormat percentFormat = (DecimalFormat) NumberFormat.getPercentInstance(ULocale.US);
        Number number = new Double(-0.986);
        String numberFormatted = percentFormat.format(number);
        checkFormatWithField("sign", percentFormat, number, numberFormatted,
            NumberFormat.Field.SIGN, 0, 1);
        checkFormatWithField("integer", percentFormat, number, numberFormatted,
            NumberFormat.Field.INTEGER, 1, 3);
        checkFormatWithField("percent", percentFormat, number, numberFormatted,
            NumberFormat.Field.PERCENT, 3, 4);
    }

    @Test
    public void TestMissingFieldPositionsPerCentPattern() {
        // Check PERCENT with more digits
        DecimalFormatSymbols us_symbols = new DecimalFormatSymbols(ULocale.US);
        DecimalFormat fmtPercent = new DecimalFormat("0.#####%", us_symbols);
        Number number = new Double(-0.986);
        String numFmtted = fmtPercent.format(number);

        checkFormatWithField("sign", fmtPercent, number, numFmtted,
            NumberFormat.Field.SIGN, 0, 1);
        checkFormatWithField("integer", fmtPercent, number, numFmtted,
            NumberFormat.Field.INTEGER, 1, 3);
        checkFormatWithField("decimal separator", fmtPercent, number, numFmtted,
            NumberFormat.Field.DECIMAL_SEPARATOR, 3, 4);
        checkFormatWithField("fraction", fmtPercent, number, numFmtted,
            NumberFormat.Field.FRACTION, 4, 5);
        checkFormatWithField("percent", fmtPercent, number, numFmtted,
            NumberFormat.Field.PERCENT, 5, 6);
    }

    @Test
    public void TestMissingFieldPositionsPerMille() {
        // Check PERMILLE
        DecimalFormatSymbols us_symbols = new DecimalFormatSymbols(ULocale.US);
        DecimalFormat fmtPerMille = new DecimalFormat("0.######‰", us_symbols);
        Number numberPermille = new Double(-0.98654);
        String numFmtted = fmtPerMille.format(numberPermille);

        checkFormatWithField("sign", fmtPerMille, numberPermille, numFmtted,
            NumberFormat.Field.SIGN, 0, 1);
        checkFormatWithField("integer", fmtPerMille, numberPermille, numFmtted,
            NumberFormat.Field.INTEGER, 1, 4);
        checkFormatWithField("decimal separator", fmtPerMille, numberPermille, numFmtted,
            NumberFormat.Field.DECIMAL_SEPARATOR, 4, 5);
        checkFormatWithField("fraction", fmtPerMille, numberPermille, numFmtted,
            NumberFormat.Field.FRACTION, 5, 7);
        checkFormatWithField("permille", fmtPerMille, numberPermille, numFmtted,
            NumberFormat.Field.PERMILLE, 7, 8);
    }

    @Test
    public void TestMissingFieldPositionsNegativeBigInt() {
      DecimalFormatSymbols us_symbols = new DecimalFormatSymbols(ULocale.US);
        DecimalFormat formatter = new DecimalFormat("0.#####E+0", us_symbols);
        Number number = new BigDecimal("-123456789987654321");
        String bigDecFmtted = formatter.format(number);

        checkFormatWithField("sign", formatter, number, bigDecFmtted,
            NumberFormat.Field.SIGN, 0, 1);
        checkFormatWithField("integer", formatter, number, bigDecFmtted,
            NumberFormat.Field.INTEGER, 1, 2);
        checkFormatWithField("decimal separator", formatter, number, bigDecFmtted,
            NumberFormat.Field.DECIMAL_SEPARATOR, 2, 3);
        checkFormatWithField("exponent symbol", formatter, number, bigDecFmtted,
            NumberFormat.Field.EXPONENT_SYMBOL, 8, 9);
        checkFormatWithField("exponent sign", formatter, number, bigDecFmtted,
            NumberFormat.Field.EXPONENT_SIGN, 9, 10);
        checkFormatWithField("exponent", formatter, number, bigDecFmtted,
            NumberFormat.Field.EXPONENT, 10, 12);
    }

    @Test
    public void TestMissingFieldPositionsNegativeLong() {
        Number number = new Long("-123456789987654321");
        DecimalFormatSymbols us_symbols = new DecimalFormatSymbols(ULocale.US);
        DecimalFormat formatter = new DecimalFormat("0.#####E+0", us_symbols);
        String longFmtted = formatter.format(number);

        checkFormatWithField("sign", formatter, number, longFmtted,
            NumberFormat.Field.SIGN, 0, 1);
        checkFormatWithField("integer", formatter, number, longFmtted,
            NumberFormat.Field.INTEGER, 1, 2);
        checkFormatWithField("decimal separator", formatter, number, longFmtted,
            NumberFormat.Field.DECIMAL_SEPARATOR, 2, 3);
        checkFormatWithField("exponent symbol", formatter, number, longFmtted,
            NumberFormat.Field.EXPONENT_SYMBOL, 8, 9);
        checkFormatWithField("exponent sign", formatter, number, longFmtted,
            NumberFormat.Field.EXPONENT_SIGN, 9, 10);
        checkFormatWithField("exponent", formatter, number, longFmtted,
            NumberFormat.Field.EXPONENT, 10, 12);
    }

    @Test
    public void TestMissingFieldPositionsPositiveBigDec() {
        // Check complex positive;negative pattern.
        DecimalFormatSymbols us_symbols = new DecimalFormatSymbols(ULocale.US);
        DecimalFormat fmtPosNegSign = new DecimalFormat("+0.####E+00;-0.#######E+0", us_symbols);
        Number positiveExp = new Double("9876543210");
        String posExpFormatted = fmtPosNegSign.format(positiveExp);

        checkFormatWithField("sign", fmtPosNegSign, positiveExp, posExpFormatted,
            NumberFormat.Field.SIGN, 0, 1);
        checkFormatWithField("integer", fmtPosNegSign, positiveExp, posExpFormatted,
            NumberFormat.Field.INTEGER, 1, 2);
        checkFormatWithField("decimal separator", fmtPosNegSign, positiveExp, posExpFormatted,
            NumberFormat.Field.DECIMAL_SEPARATOR, 2, 3);
        checkFormatWithField("fraction", fmtPosNegSign, positiveExp, posExpFormatted,
            NumberFormat.Field.FRACTION, 3, 7);
        checkFormatWithField("exponent symbol", fmtPosNegSign, positiveExp, posExpFormatted,
            NumberFormat.Field.EXPONENT_SYMBOL, 7, 8);
        checkFormatWithField("exponent sign", fmtPosNegSign, positiveExp, posExpFormatted,
            NumberFormat.Field.EXPONENT_SIGN, 8, 9);
        checkFormatWithField("exponent", fmtPosNegSign, positiveExp, posExpFormatted,
            NumberFormat.Field.EXPONENT, 9, 11);
    }

    @Test
    public void TestMissingFieldPositionsNegativeBigDec() {
        // Check complex positive;negative pattern.
      DecimalFormatSymbols us_symbols = new DecimalFormatSymbols(ULocale.US);
        DecimalFormat fmtPosNegSign = new DecimalFormat("+0.####E+00;-0.#######E+0", us_symbols);
        Number negativeExp = new BigDecimal("-0.000000987654321083");
        String negExpFormatted = fmtPosNegSign.format(negativeExp);

        checkFormatWithField("sign", fmtPosNegSign, negativeExp, negExpFormatted,
            NumberFormat.Field.SIGN, 0, 1);
        checkFormatWithField("integer", fmtPosNegSign, negativeExp, negExpFormatted,
            NumberFormat.Field.INTEGER, 1, 2);
        checkFormatWithField("decimal separator", fmtPosNegSign, negativeExp, negExpFormatted,
            NumberFormat.Field.DECIMAL_SEPARATOR, 2, 3);
        checkFormatWithField("fraction", fmtPosNegSign, negativeExp, negExpFormatted,
            NumberFormat.Field.FRACTION, 3, 7);
        checkFormatWithField("exponent symbol", fmtPosNegSign, negativeExp, negExpFormatted,
            NumberFormat.Field.EXPONENT_SYMBOL, 7, 8);
        checkFormatWithField("exponent sign", fmtPosNegSign, negativeExp, negExpFormatted,
            NumberFormat.Field.EXPONENT_SIGN, 8, 9);
        checkFormatWithField("exponent", fmtPosNegSign, negativeExp, negExpFormatted,
            NumberFormat.Field.EXPONENT, 9, 11);
    }

    @Test
    public void TestStringSymbols() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(ULocale.US);

        // Attempt digits with multiple code points.
        String[] customDigits = {"(0)", "(1)", "(2)", "(3)", "(4)", "(5)", "(6)", "(7)", "(8)", "(9)"};
        symbols.setDigitStrings(customDigits);
        DecimalFormat fmt = new DecimalFormat("#,##0.0#", symbols);
        expect2(fmt, 1234567.89, "(1),(2)(3)(4),(5)(6)(7).(8)(9)");

        // Scientific notation should work.
        fmt.applyPattern("@@@E0");
        expect2(fmt, 1230000, "(1).(2)(3)E(6)");

        // Grouping and decimal with multiple code points (supported in parsing since ICU 61)
        symbols.setDecimalSeparatorString("~~");
        symbols.setGroupingSeparatorString("^^");
        fmt.setDecimalFormatSymbols(symbols);
        fmt.applyPattern("#,##0.0#");
        expect2(fmt, 1234567.89, "(1)^^(2)(3)(4)^^(5)(6)(7)~~(8)(9)");

        // Digits starting at U+1D7CE MATHEMATICAL BOLD DIGIT ZERO
        // These are all single code points, so parsing will work.
        for (int i=0; i<10; i++) customDigits[i] = new String(Character.toChars(0x1D7CE+i));
        symbols.setDigitStrings(customDigits);
        symbols.setDecimalSeparatorString("😁");
        symbols.setGroupingSeparatorString("😎");
        fmt.setDecimalFormatSymbols(symbols);
        expect2(fmt, 1234.56, "𝟏😎𝟐𝟑𝟒😁𝟓𝟔");
    }

    @Test
    public void TestArabicCurrencyPatternInfo() {
        ULocale arLocale = new ULocale("ar");

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(arLocale);
        String currSpacingPatn = symbols.getPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_CURRENCY_MATCH, true);
        if (currSpacingPatn==null || currSpacingPatn.length() == 0) {
            errln("locale ar, getPatternForCurrencySpacing returns null or 0-length string");
        }

        DecimalFormat currAcctFormat = (DecimalFormat)NumberFormat.getInstance(arLocale, NumberFormat.ACCOUNTINGCURRENCYSTYLE);
        String currAcctPatn = currAcctFormat.toPattern();
        if (currAcctPatn==null || currAcctPatn.length() == 0) {
            errln("locale ar, toPattern for ACCOUNTINGCURRENCYSTYLE returns null or 0-length string");
        }
    }

    @Test
    public void TestMinMaxOverrides()
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
                NoSuchMethodException, SecurityException {
        Class<?>[] baseClasses = {NumberFormat.class, NumberFormat.class, DecimalFormat.class};
        String[] names = {"Integer", "Fraction", "Significant"};
        for (int i = 0; i < 3; i++) {
            DecimalFormat df = new DecimalFormat();
            Class<?> base = baseClasses[i];
            String name = names[i];
            Method getMinimum = base.getDeclaredMethod("getMinimum" + name + "Digits");
            Method setMinimum = base.getDeclaredMethod("setMinimum" + name + "Digits", Integer.TYPE);
            Method getMaximum = base.getDeclaredMethod("getMaximum" + name + "Digits");
            Method setMaximum = base.getDeclaredMethod("setMaximum" + name + "Digits", Integer.TYPE);

            // Check max overrides min
            setMinimum.invoke(df, 2);
            assertEquals(name + " getMin A", 2, getMinimum.invoke(df));
            setMaximum.invoke(df, 3);
            assertEquals(name + " getMin B", 2, getMinimum.invoke(df));
            assertEquals(name + " getMax B", 3, getMaximum.invoke(df));
            setMaximum.invoke(df, 2);
            assertEquals(name + " getMin C", 2, getMinimum.invoke(df));
            assertEquals(name + " getMax C", 2, getMaximum.invoke(df));
            setMaximum.invoke(df, 1);
            assertEquals(name + " getMin D", 1, getMinimum.invoke(df));
            assertEquals(name + " getMax D", 1, getMaximum.invoke(df));

            // Check min overrides max
            setMaximum.invoke(df, 2);
            assertEquals(name + " getMax E", 2, getMaximum.invoke(df));
            setMinimum.invoke(df, 1);
            assertEquals(name + " getMin F", 1, getMinimum.invoke(df));
            assertEquals(name + " getMax F", 2, getMaximum.invoke(df));
            setMinimum.invoke(df, 2);
            assertEquals(name + " getMin G", 2, getMinimum.invoke(df));
            assertEquals(name + " getMax G", 2, getMaximum.invoke(df));
            setMinimum.invoke(df, 3);
            assertEquals(name + " getMin H", 3, getMinimum.invoke(df));
            assertEquals(name + " getMax H", 3, getMaximum.invoke(df));
        }
    }

    @Test
    public void TestSetMathContext() throws ParseException {
        java.math.MathContext fourDigits = new java.math.MathContext(4);
        java.math.MathContext unlimitedCeiling = new java.math.MathContext(0, RoundingMode.CEILING);

        // Test rounding
        DecimalFormat df = new DecimalFormat();
        assertEquals("Default format", "9,876.543", df.format(9876.5432));
        df.setMathContext(fourDigits);
        assertEquals("Format with fourDigits", "9,877", df.format(9876.5432));
        df.setMathContext(unlimitedCeiling);
        assertEquals("Format with unlimitedCeiling", "9,876.544", df.format(9876.5432));

        // Test multiplication
        df = new DecimalFormat("0.000%");
        assertEquals("Default multiplication", "12.001%", df.format(0.120011));
        df.setMathContext(fourDigits);
        assertEquals("Multiplication with fourDigits", "12.000%", df.format(0.120011));
        df.setMathContext(unlimitedCeiling);
        assertEquals("Multiplication with unlimitedCeiling", "12.002%", df.format(0.120011));

        // Test simple division
        df = new DecimalFormat("0%");
        assertEquals("Default division", 0.12001, df.parse("12.001%").doubleValue());
        df.setMathContext(fourDigits);
        // NOTE: Since ICU 61, division no longer occurs with percentage parsing.
        // assertEquals("Division with fourDigits", 0.12, df.parse("12.001%").doubleValue());
        assertEquals("Division with fourDigits", 0.12001, df.parse("12.001%").doubleValue());
        df.setMathContext(unlimitedCeiling);
        assertEquals("Division with unlimitedCeiling", 0.12001, df.parse("12.001%").doubleValue());

        // Test extreme division
        df = new DecimalFormat();
        df.setMultiplier(1000000007); // prime number
        String hugeNumberString = "9876543212345678987654321234567898765432123456789"; // 49 digits
        BigInteger huge34Digits = new BigInteger("9876543143209876985185182338271622000000");
        BigInteger huge4Digits = new BigInteger("9877000000000000000000000000000000000000");
        BigInteger actual34Digits = ((BigDecimal) df.parse(hugeNumberString)).toBigIntegerExact();
        assertEquals("Default extreme division", huge34Digits, actual34Digits);
        df.setMathContext(fourDigits);
        BigInteger actual4Digits = ((BigDecimal) df.parse(hugeNumberString)).toBigIntegerExact();
        assertEquals("Extreme division with fourDigits", huge4Digits, actual4Digits);
    }

    /**
     * ArithmeticException is thrown when inverting multiplier produces a non-terminating
     * decimal result in conjunction with MathContext of unlimited precision.
     */
    @Test
    public void testSetMathContextArithmeticException() {
        DecimalFormat df = new DecimalFormat();
        df.setMultiplier(7);
        try {
            df.setMathContext(java.math.MathContext.UNLIMITED);
            fail("Extreme division with unlimited precision should throw ArithmeticException");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    /**
     * ArithmeticException is thrown when inverting multiplier produces a non-terminating
     * decimal result in conjunction with MathContext of unlimited precision.
     */
    @Test
    public void testSetMathContextICUArithmeticException() {
        DecimalFormat df = new DecimalFormat();
        df.setMultiplier(7);
        try {
            df.setMathContextICU(new MathContext(0));
            fail("Extreme division with unlimited precision should throw ArithmeticException");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    /**
     * ArithmeticException is thrown when inverting multiplier produces a non-terminating
     * decimal result in conjunction with MathContext of unlimited precision.
     */
    @Test
    public void testSetMultiplierArithmeticException() {
        DecimalFormat df = new DecimalFormat();
        df.setMathContext(java.math.MathContext.UNLIMITED);
        try {
            df.setMultiplier(7);
            fail("Extreme division with unlimited precision should throw ArithmeticException");
        } catch (ArithmeticException e) {
            // expected
        }
    }

    @Test
    public void Test10436() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH);
        df.setRoundingMode(MathContext.ROUND_CEILING);
        df.setMinimumFractionDigits(0);
        df.setMaximumFractionDigits(0);
        assertEquals("-.99 should round toward infinity", "-0", df.format(-0.99));
    }

    @Test
    public void Test10765() {
        NumberFormat fmt = NumberFormat.getInstance(new ULocale("en"));
        fmt.setMinimumIntegerDigits(10);
        FieldPosition pos = new FieldPosition(NumberFormat.Field.GROUPING_SEPARATOR);
        StringBuffer sb = new StringBuffer();
        fmt.format(1234567, sb, pos);
        assertEquals("Should have multiple grouping separators", "0,001,234,567", sb.toString());
        assertEquals("FieldPosition should report the first occurrence", 1, pos.getBeginIndex());
        assertEquals("FieldPosition should report the first occurrence", 2, pos.getEndIndex());
    }

    @Test
    public void Test10997() {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new ULocale("en-US"));
        fmt.setMinimumFractionDigits(4);
        fmt.setMaximumFractionDigits(4);
        String str1 = fmt.format(new CurrencyAmount(123.45, Currency.getInstance("USD")));
        String str2 = fmt.format(new CurrencyAmount(123.45, Currency.getInstance("EUR")));
        assertEquals("minFrac 4 should be respected in default currency", "$123.4500", str1);
        assertEquals("minFrac 4 should be respected in different currency", "€123.4500", str2);
    }

    @Test
    public void Test11020() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(ULocale.FRANCE);
        DecimalFormat fmt = new DecimalFormat("0.05E0", sym);
        String result = fmt.format(12301.2).replace('\u00a0', ' ');
        assertEquals("Rounding increment should be applied after magnitude scaling", "1,25E4", result);
    }

    @Test
    public void Test11025() {
        String pattern = "¤¤ **####0.00";
        DecimalFormatSymbols sym = new DecimalFormatSymbols(ULocale.FRANCE);
        DecimalFormat fmt = new DecimalFormat(pattern, sym);
        String result = fmt.format(433.0);
        assertEquals("Number should be padded to 11 characters", "EUR *433,00", result);
    }

    @Test
    public void Test11640_TripleCurrencySymbol() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance();
        df.applyPattern("¤¤¤ 0");
        String result = df.getPositivePrefix();
        assertEquals("Triple-currency should give long name on getPositivePrefix", "US dollars ", result);
    }

    @Test
    public void Test11645() {
        String pattern = "#,##0.0#";
        DecimalFormat fmt = (DecimalFormat) NumberFormat.getInstance();
        fmt.applyPattern(pattern);
        DecimalFormat fmtCopy;

        final int newMultiplier = 37;
        fmtCopy = (DecimalFormat) fmt.clone();
        assertNotEquals("Value before setter", fmtCopy.getMultiplier(), newMultiplier);
        fmtCopy.setMultiplier(newMultiplier);
        assertEquals("Value after setter", fmtCopy.getMultiplier(), newMultiplier);
        fmtCopy.applyPattern(pattern);
        assertEquals("Value after applyPattern", fmtCopy.getMultiplier(), newMultiplier);
        assertFalse("multiplier", fmt.equals(fmtCopy));

        final int newRoundingMode = RoundingMode.CEILING.ordinal();
        fmtCopy = (DecimalFormat) fmt.clone();
        assertNotEquals("Value before setter", fmtCopy.getRoundingMode(), newRoundingMode);
        fmtCopy.setRoundingMode(newRoundingMode);
        assertEquals("Value after setter", fmtCopy.getRoundingMode(), newRoundingMode);
        fmtCopy.applyPattern(pattern);
        assertEquals("Value after applyPattern", fmtCopy.getRoundingMode(), newRoundingMode);
        assertFalse("roundingMode", fmt.equals(fmtCopy));

        final Currency newCurrency = Currency.getInstance("EAT");
        fmtCopy = (DecimalFormat) fmt.clone();
        assertNotEquals("Value before setter", fmtCopy.getCurrency(), newCurrency);
        fmtCopy.setCurrency(newCurrency);
        assertEquals("Value after setter", fmtCopy.getCurrency(), newCurrency);
        fmtCopy.applyPattern(pattern);
        assertEquals("Value after applyPattern", fmtCopy.getCurrency(), newCurrency);
        assertFalse("currency", fmt.equals(fmtCopy));

        final CurrencyUsage newCurrencyUsage = CurrencyUsage.CASH;
        fmtCopy = (DecimalFormat) fmt.clone();
        assertNotEquals("Value before setter", fmtCopy.getCurrencyUsage(), newCurrencyUsage);
        fmtCopy.setCurrencyUsage(CurrencyUsage.CASH);
        assertEquals("Value after setter", fmtCopy.getCurrencyUsage(), newCurrencyUsage);
        fmtCopy.applyPattern(pattern);
        assertEquals("Value after applyPattern", fmtCopy.getCurrencyUsage(), newCurrencyUsage);
        assertFalse("currencyUsage", fmt.equals(fmtCopy));
    }

    @Test
    public void Test11646() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new ULocale("en_US"));
        String pattern = "\u00a4\u00a4\u00a4 0.00 %\u00a4\u00a4";
        DecimalFormat fmt = new DecimalFormat(pattern, symbols);

        // Test equality with affixes. set affix methods can't capture special
        // characters which is why equality should fail.
        {
          DecimalFormat fmtCopy = (DecimalFormat) fmt.clone();
          assertEquals("", fmt, fmtCopy);
          fmtCopy.setPositivePrefix(fmtCopy.getPositivePrefix());
          assertNotEquals("", fmt, fmtCopy);
        }
        {
          DecimalFormat fmtCopy = (DecimalFormat) fmt.clone();
          assertEquals("", fmt, fmtCopy);
          fmtCopy.setPositiveSuffix(fmtCopy.getPositiveSuffix());
          assertNotEquals("", fmt, fmtCopy);
        }
        {
          DecimalFormat fmtCopy = (DecimalFormat) fmt.clone();
          assertEquals("", fmt, fmtCopy);
          fmtCopy.setNegativePrefix(fmtCopy.getNegativePrefix());
          assertNotEquals("", fmt, fmtCopy);
        }
        {
          DecimalFormat fmtCopy = (DecimalFormat) fmt.clone();
          assertEquals("", fmt, fmtCopy);
          fmtCopy.setNegativeSuffix(fmtCopy.getNegativeSuffix());
          assertNotEquals("", fmt, fmtCopy);
        }
    }

    @Test
    public void Test11648() {
        DecimalFormat df = new DecimalFormat("0.00");
        df.setScientificNotation(true);
        String pat = df.toPattern();
        assertEquals("A valid scientific notation pattern should be produced", "0.00E0", pat);
    }

    @Test
    public void Test11649() {
        String pattern = "\u00a4\u00a4\u00a4 0.00";
        DecimalFormat fmt = new DecimalFormat(pattern);
        fmt.setCurrency(Currency.getInstance("USD"));
        assertEquals("Triple currency sign should format long name", "US dollars 12.34", fmt.format(12.34));

        String newPattern = fmt.toPattern();
        assertEquals("Should produce a valid pattern", pattern, newPattern);

        DecimalFormat fmt2 = new DecimalFormat(newPattern);
        fmt2.setCurrency(Currency.getInstance("USD"));
        assertEquals("Triple currency sign pattern should round-trip", "US dollars 12.34", fmt2.format(12.34));

        String quotedPattern = "\u00a4\u00a4'\u00a4' 0.00";
        DecimalFormat fmt3 = new DecimalFormat(quotedPattern);
        assertEquals("Should be treated as double currency sign", "USD\u00a4 12.34", fmt3.format(12.34));

        String outQuotedPattern = fmt3.toPattern();
        assertEquals("Double currency sign with quoted sign should round-trip", quotedPattern, outQuotedPattern);
    }

    @Test
    @Ignore
    public void Test11686() {
        // Only passes with slow mode.
        // TODO: Re-enable this test with slow mode.
        DecimalFormat df = new DecimalFormat();
        df.setPositiveSuffix("0K");
        df.setNegativeSuffix("0N");
        expect2(df, 123, "1230K");
        expect2(df, -123, "-1230N");
    }

    @Test
    public void Test11839() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.ENGLISH);
        dfs.setMinusSignString("a∸");
        dfs.setPlusSignString("b∔"); //  ∔  U+2214 DOT PLUS
        DecimalFormat df = new DecimalFormat("0.00+;0.00-", dfs);
        String result = df.format(-1.234);
        assertEquals("Locale-specific minus sign should be used", "1.23a∸", result);
        result = df.format(1.234);
        assertEquals("Locale-specific plus sign should be used", "1.23b∔", result);
        // Test round-trip with parse
        expect2(df, -456, "456.00a∸");
        expect2(df, 456, "456.00b∔");
    }

    @Test
    public void Test12753() {
        ULocale locale = new ULocale("en-US");
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        symbols.setDecimalSeparator('*');
        DecimalFormat df = new DecimalFormat("0.00", symbols);
        df.setDecimalPatternMatchRequired(true);
        try {
            df.parse("123");
            fail("Parsing integer succeeded even though setDecimalPatternMatchRequired was set");
        } catch (ParseException e) {
            // Parse failed (expected)
        }
    }

    @Test
    public void Test12962() {
        String pat = "**0.00";
        DecimalFormat df = new DecimalFormat(pat);
        String newPat = df.toPattern();
        assertEquals("Format width changed upon calling applyPattern", pat.length(), newPat.length());
    }

    @Test
    public void Test10354() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setNaN("");
        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(dfs);
        try {
            df.formatToCharacterIterator(Double.NaN);
            // pass
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void Test11913() {
        NumberFormat df = DecimalFormat.getInstance();
        String result = df.format(new BigDecimal("1.23456789E400"));
        assertEquals("Should format more than 309 digits", "12,345,678", result.substring(0, 10));
        assertEquals("Should format more than 309 digits", 534, result.length());
    }

    @Test
    public void Test12045() {
        if (logKnownIssue("12045", "XSU is missing from fr")) { return; }

        NumberFormat nf = NumberFormat.getInstance(new ULocale("fr"), NumberFormat.PLURALCURRENCYSTYLE);
        ParsePosition ppos = new ParsePosition(0);
        try {
            CurrencyAmount result = nf.parseCurrency("2,34 XSU", ppos);
            assertEquals("Parsing should succeed on XSU",
                         new CurrencyAmount(2.34, Currency.getInstance("XSU")), result);
            // pass
        } catch (Exception e) {
            //throw new AssertionError("Should have been able to parse XSU", e);
            throw new AssertionError("Should have been able to parse XSU: " + e.getMessage());
        }
    }

    @Test
    public void Test11739() {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new ULocale("sr_BA"));
        ((DecimalFormat) nf).applyPattern("#,##0.0 ¤¤¤");
        ParsePosition ppos = new ParsePosition(0);
        CurrencyAmount result = nf.parseCurrency("1.500 амерички долар", ppos);
        assertEquals("Should parse to 1500 USD", new CurrencyAmount(1500, Currency.getInstance("USD")), result);
    }

    @Test
    public void Test11647() {
        DecimalFormat df = new DecimalFormat();
        df.applyPattern("¤¤¤¤#");
        String actual = df.format(123);
        assertEquals("Should replace 4 currency signs with U+FFFD", "\uFFFD123", actual);
    }

    @Test
    public void Test12567() {
        DecimalFormat df1 = (DecimalFormat) NumberFormat.getInstance(NumberFormat.PLURALCURRENCYSTYLE);
        DecimalFormat df2 = (DecimalFormat) NumberFormat.getInstance(NumberFormat.NUMBERSTYLE);
        df2.setCurrency(df1.getCurrency());
        df2.setCurrencyPluralInfo(df1.getCurrencyPluralInfo());
        df1.applyPattern("0.00");
        df2.applyPattern("0.00");
        assertEquals("df1 == df2", df1, df2);
        assertEquals("df2 == df1", df2, df1);
        df2.setPositivePrefix("abc");
        assertNotEquals("df1 != df2", df1, df2);
        assertNotEquals("df2 != df1", df2, df1);
    }

    @Test
    public void Test11897_LocalizedPatternSeparator() {
        // In a locale with a different <list> symbol, like arabic,
        // kPatternSeparatorSymbol should still be ';'
        {
            DecimalFormatSymbols dfs = new DecimalFormatSymbols(new ULocale("ar"));
            assertEquals("pattern separator symbol should be ;",
                    ';',
                    dfs.getPatternSeparator());
        }

        // However, the custom symbol should be used in localized notation
        // when set manually via API
        {
            DecimalFormatSymbols dfs = new DecimalFormatSymbols(new ULocale("en"));
            dfs.setPatternSeparator('!');
            DecimalFormat df = new DecimalFormat("0", dfs);
            df.applyPattern("a0;b0"); // should not throw
            assertEquals("should apply the normal pattern",
                    df.getNegativePrefix(),
                    "b");
            df.applyLocalizedPattern("c0!d0"); // should not throw
            assertEquals("should apply the localized pattern",
                    df.getNegativePrefix(),
                    "d");
        }
    }

    @Test
    public void Test13055() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getPercentInstance();
        df.setMaximumFractionDigits(0);
        df.setRoundingMode(BigDecimal.ROUND_HALF_EVEN);
        assertEquals("Should round percent toward even number", "216%", df.format(2.155));
    }

    @Test
    public void Test13056() {
        DecimalFormat df = new DecimalFormat("#,##0");
        assertEquals("Primary grouping should return 3", 3, df.getGroupingSize());
        assertEquals("Secondary grouping should return 0", 0, df.getSecondaryGroupingSize());
        df.setSecondaryGroupingSize(3);
        assertEquals("Primary grouping should still return 3", 3, df.getGroupingSize());
        assertEquals("Secondary grouping should round-trip", 3, df.getSecondaryGroupingSize());
        df.setGroupingSize(4);
        assertEquals("Primary grouping should return 4", 4, df.getGroupingSize());
        assertEquals("Secondary should remember explicit setting and return 3", 3, df.getSecondaryGroupingSize());
    }

    @Test
    public void Test13074() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(new ULocale("bg-BG"));
        String result = df.format(987654.321);
        assertEquals("Locale 'bg' should not use monetary grouping", "987654,32 лв.", result);
    }

    @Test
    public void Test13088and13162() {
        ULocale loc = new ULocale("fa");
        String pattern1 = "%\u00A0#,##0;%\u00A0-#,##0";
        double num = -12.34;
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(loc);
        // If the symbols ever change in locale data, please call the setters so that this test
        // continues to use the old symbols.
        // The fa percent symbol does change in CLDR 32, so....
        symbols.setPercentString("‎٪");
        assertEquals("Checking for expected symbols", "‎−", symbols.getMinusSignString());
        assertEquals("Checking for expected symbols", "‎٪", symbols.getPercentString());
        DecimalFormat numfmt = new DecimalFormat(pattern1, symbols);
        expect2(numfmt, num, "‎٪ ‎−۱٬۲۳۴");
        String pattern2 = "%#,##0;%-#,##0";
        numfmt = new DecimalFormat(pattern2, symbols);
        expect2(numfmt, num, "‎٪‎−۱٬۲۳۴");
    }

    @Test
    public void Test13113_MalformedPatterns() {
        String[][] cases = {
                {"'", "quoted literal"},
                {"ab#c'd", "quoted literal"},
                {"ab#c*", "unquoted literal"},
                {"0#", "# cannot follow 0"},
                {".#0", "0 cannot follow #"},
                {"@0", "Cannot mix @ and 0"},
                {"0@", "Cannot mix 0 and @"},
                {"#x#", "unquoted special character"},
                {"@#@", "# inside of a run of @"},
        };
        for (String[] cas : cases) {
            try {
                new DecimalFormat(cas[0]);
                fail("Should have thrown on malformed pattern");
            } catch (IllegalArgumentException ex) {
                assertTrue("Exception should contain \"Malformed pattern\": " + ex.getMessage(),
                        ex.getMessage().contains("Malformed pattern"));
                assertTrue("Exception should contain \"" + cas[1] + "\"" + ex.getMessage(),
                        ex.getMessage().contains(cas[1]));
            }
        }
    }

    @Test
    public void Test13118() {
        DecimalFormat df = new DecimalFormat("@@@");
        df.setScientificNotation(true);
        for (double d=12345.67; d>1e-6; d/=10) {
            String result = df.format(d);
            assertEquals("Should produce a string of expected length on " + d,
                    d > 1 ? 6 : 7, result.length());
        }
    }

    @Test
    public void Test13289() {
        DecimalFormat df = new DecimalFormat("#00.0#E0");
        String result = df.format(0.00123);
        assertEquals("Should ignore scientific minInt if maxInt>minInt", "1.23E-3", result);
    }

    @Test
    public void Test13310() {
        // Note: if minInt > 8, then maxInt can be greater than 8.
        assertEquals("Should not throw an assertion error",
                "100000007.6E-1",
                new DecimalFormat("000000000.0#E0").format(10000000.76d));
    }

    @Test
    public void Test13391() throws ParseException {
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(new ULocale("ccp"));
        df.setParseStrict(true);
        String expected = "\uD804\uDD37\uD804\uDD38,\uD804\uDD39\uD804\uDD3A\uD804\uDD3B";
        assertEquals("Should produce expected output in ccp", expected, df.format(12345));
        Number result = df.parse(expected);
        assertEquals("Should parse to 12345 in ccp", 12345, result.longValue());

        df = (DecimalFormat) NumberFormat.getScientificInstance(new ULocale("ccp"));
        df.setParseStrict(true);
        String expectedScientific = "\uD804\uDD37.\uD804\uDD39E\uD804\uDD38";
        assertEquals("Should produce expected scientific output in ccp",
                expectedScientific, df.format(130));
        Number resultScientific = df.parse(expectedScientific);
        assertEquals("Should parse scientific to 130 in ccp",
                130, resultScientific.longValue());
    }

    @Test
    public void Test13453_AffixContent() {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getScientificInstance();
        assertEquals("Scientific should NOT be included", "", df.getPositiveSuffix());

        df = CompactDecimalFormat.getInstance(ULocale.ENGLISH, CompactDecimalFormat.CompactStyle.SHORT);
        assertEquals("Compact should NOT be included", "", df.getPositiveSuffix());

        df = (DecimalFormat) DecimalFormat.getInstance(NumberFormat.ISOCURRENCYSTYLE);
        df.setCurrency(Currency.getInstance("GBP"));
        assertEquals("ISO currency SHOULD be included", "GBP", df.getPositivePrefix());

        df = (DecimalFormat) DecimalFormat.getInstance(NumberFormat.PLURALCURRENCYSTYLE);
        df.setCurrency(Currency.getInstance("GBP"));
        assertEquals("Plural name SHOULD be included", " British pounds", df.getPositiveSuffix());
    }

    @Test
    public void Test11035_FormatCurrencyAmount() {
        double amount = 12345.67;
        String expected = "12,345$67 ​";
        Currency cur = Currency.getInstance("PTE");

        // Test three ways to set currency via API

        ULocale loc1 = new ULocale("pt_PT");
        NumberFormat fmt1 = NumberFormat.getCurrencyInstance(loc1);
        fmt1.setCurrency(cur);
        String actualSetCurrency = fmt1.format(amount);

        ULocale loc2 = new ULocale("pt_PT@currency=PTE");
        NumberFormat fmt2 = NumberFormat.getCurrencyInstance(loc2);
        String actualLocaleString = fmt2.format(amount);

        ULocale loc3 = new ULocale("pt_PT");
        NumberFormat fmt3 = NumberFormat.getCurrencyInstance(loc3);
        CurrencyAmount curAmt = new CurrencyAmount(amount, cur);
        String actualCurrencyAmount = fmt3.format(curAmt);

        assertEquals("Custom Currency Pattern, Set Currency", expected, actualSetCurrency);
        assertEquals("Custom Currency Pattern, Locale String", expected, actualCurrencyAmount);
        assertEquals("Custom Currency Pattern, CurrencyAmount", expected, actualLocaleString);
    }

    @Test
    public void testPercentZero() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getPercentInstance();
        String actual = df.format(0);
        assertEquals("Should have one zero digit", "0%", actual);
    }

    @Test
    public void testCurrencyZeroRounding() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance();
        df.setMaximumFractionDigits(0);
        String actual = df.format(0);
        assertEquals("Should have zero fraction digits", "$0", actual);
    }

    @Test
    public void testCustomCurrencySymbol() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance();
        df.setCurrency(Currency.getInstance("USD"));
        DecimalFormatSymbols symbols = df.getDecimalFormatSymbols();
        symbols.setCurrencySymbol("#");
        df.setDecimalFormatSymbols(symbols);
        String actual = df.format(123);
        assertEquals("Should use '#' instad of '$'", "# 123.00", actual);
    }

    @Test
    public void TestBasicSerializationRoundTrip() throws IOException, ClassNotFoundException {
        DecimalFormat df0 = new DecimalFormat("A-**#####,#00.00b¤");

        // Write to byte stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(df0);
        oos.flush();
        baos.close();
        byte[] bytes = baos.toByteArray();

        // Read from byte stream
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object obj = ois.readObject();
        ois.close();
        DecimalFormat df1 = (DecimalFormat) obj;

        // Test equality
        assertEquals("Did not round-trip through serialization", df0, df1);

        // Test basic functionality
        String str0 = df0.format(12345.67);
        String str1 = df1.format(12345.67);
        assertEquals("Serialized formatter does not produce same output", str0, str1);
    }

    @Test
    public void testGetSetCurrency() {
        DecimalFormat df = new DecimalFormat("¤#", DecimalFormatSymbols.getInstance(ULocale.US));
        assertEquals("Currency should start out as the locale default", Currency.getInstance("USD"), df.getCurrency());
        Currency curr = Currency.getInstance("EUR");
        df.setCurrency(curr);
        assertEquals("Currency should equal EUR after set", curr, df.getCurrency());
        String result = df.format(123);
        assertEquals("Currency should format as expected in EUR", "€123.00", result);
    }

    @Test
    public void testRoundingModeSetters() {
        DecimalFormat df1 = new DecimalFormat();
        DecimalFormat df2 = new DecimalFormat();

        df1.setRoundingMode(java.math.BigDecimal.ROUND_CEILING);
        assertNotEquals("Rounding mode was set to a non-default", df1, df2);
        df2.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_CEILING);
        assertEquals("Rounding mode from icu.math and java.math should be the same", df1, df2);
        df2.setRoundingMode(java.math.RoundingMode.CEILING.ordinal());
        assertEquals("Rounding mode ordinal from java.math.RoundingMode should be the same", df1, df2);
    }

    @Test
    public void testCurrencySignificantDigits() {
        ULocale locale = new ULocale("en-US");
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        df.setMaximumSignificantDigits(2);
        String result = df.format(1234);
        assertEquals("Currency rounding should obey significant digits", "$1,200", result);
    }

    @Test
    public void testParseStrictScientific() {
        // See ticket #13057
        DecimalFormat df = (DecimalFormat) NumberFormat.getScientificInstance();
        df.setParseStrict(true);
        ParsePosition ppos = new ParsePosition(0);
        Number result0 = df.parse("123E4", ppos);
        assertEquals("Should accept number with exponent", 1230000L, result0);
        assertEquals("Should consume the whole number", 5, ppos.getIndex());
        ppos.setIndex(0);
        result0 = df.parse("123", ppos);
        // #13737: For backwards compatibility, do NOT require the exponent.
        assertEquals("Should NOT reject number without exponent", 123L, result0);
        ppos.setIndex(0);
        CurrencyAmount result1 = df.parseCurrency("USD123", ppos);
        assertEquals("Should NOT reject currency without exponent",
                new CurrencyAmount(123L, Currency.getInstance("USD")),
                result1);
    }

    @Test
    public void testParseLenientScientific() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getScientificInstance();
        ParsePosition ppos = new ParsePosition(0);
        Number result0 = df.parse("123E", ppos);
        assertEquals("Should parse the number in lenient mode", 123L, result0);
        assertEquals("Should stop before the E", 3, ppos.getIndex());
        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        dfs.setExponentSeparator("EE");
        df.setDecimalFormatSymbols(dfs);
        ppos.setIndex(0);
        result0 = df.parse("123EE", ppos);
        assertEquals("Should parse the number in lenient mode", 123L, result0);
        assertEquals("Should stop before the EE", 3, ppos.getIndex());
    }

    @Test
    public void testParseAcceptAsciiPercentPermilleFallback() {
        ULocale loc = new ULocale("ar");
        DecimalFormat df = (DecimalFormat) NumberFormat.getPercentInstance(loc);
        ParsePosition ppos = new ParsePosition(0);
        Number result = df.parse("42%", ppos);
        assertEquals("Should parse as 0.42 even in ar", new BigDecimal("0.42"), result);
        assertEquals("Should consume the entire string even in ar", 3, ppos.getIndex());
        // TODO: Is there a better way to make a localized permille formatter?
        df.applyPattern(df.toPattern().replace("%", "‰"));
        ppos.setIndex(0);
        result = df.parse("42‰", ppos);
        assertEquals("Should parse as 0.042 even in ar", new BigDecimal("0.042"), result);
        assertEquals("Should consume the entire string even in ar", 3, ppos.getIndex());
    }

    @Test
    public void testParseSubtraction() {
        // TODO: Is this a case we need to support? It prevents us from automatically parsing
        // minus signs that appear after the number, like  in "12-" vs "-12".
        DecimalFormat df = new DecimalFormat();
        String str = "12 - 5";
        ParsePosition ppos = new ParsePosition(0);
        Number n1 = df.parse(str, ppos);
        Number n2 = df.parse(str, ppos);
        assertEquals("Should parse 12 and -5", 12, n1.intValue());
        assertEquals("Should parse 12 and -5", -5, n2.intValue());
    }

    @Test
    public void testSetPrefixDefaultSuffix() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getPercentInstance();
        df.setPositivePrefix("+");
        assertEquals("Should have manual plus sign and auto percent sign", "+100%", df.format(1));
    }

    @Test
    public void testMultiCodePointPaddingInPattern() {
        DecimalFormat df = new DecimalFormat("a*'நி'###0b");
        String result = df.format(12);
        assertEquals("Multi-codepoint padding should not be split", "aநிநி12b", result);
        df = new DecimalFormat("a*😁###0b");
        result = df.format(12);
        assertEquals("Single-codepoint padding should not be split", "a😁😁12b", result);
        df = new DecimalFormat("a*''###0b");
        result = df.format(12);
        assertEquals("Quote should be escapable in padding syntax", "a''12b", result);
    }

    @Test
    public void Test13737_ParseScientificStrict() {
        NumberFormat df = NumberFormat.getScientificInstance(ULocale.ENGLISH);
        df.setParseStrict(true);
        // Parse Test: exponent is not required, even in strict mode
        expect(df, "1.2", 1.2);
    }

    // TODO: Investigate this test and re-enable if appropriate.
    @Test
    @Ignore
    public void testParseAmbiguousAffixes() {
        BigDecimal positive = new BigDecimal("0.0567");
        BigDecimal negative = new BigDecimal("-0.0567");
        DecimalFormat df = new DecimalFormat();
        df.setParseBigDecimal(true);

        String[] patterns = { "+0.00%;-0.00%", "+0.00%;0.00%", "0.00%;-0.00%" };
        String[] inputs = { "+5.67%", "-5.67%", "5.67%" };
        boolean[][] expectedPositive = {
                { true, false, true },
                { true, false, false },
                { true, false, true }
        };

        for (int i=0; i<patterns.length; i++) {
            String pattern = patterns[i];
            df.applyPattern(pattern);
            for (int j=0; j<inputs.length; j++) {
                String input = inputs[j];
                ParsePosition ppos = new ParsePosition(0);
                Number actual = df.parse(input, ppos);
                BigDecimal expected = expectedPositive[i][j] ? positive : negative;
                String message = "Pattern " + pattern + " with input " + input;
                assertEquals(message, expected, actual);
                assertEquals(message, input.length(), ppos.getIndex());
            }
        }
    }

    @Test
    public void testParseIgnorables() {
        // Also see the test case "test parse ignorables" in numberformattestspecification.txt
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
        dfs.setPercentString("\u200E%\u200E");
        DecimalFormat df = new DecimalFormat("0 %;-0a", dfs);
        ParsePosition ppos = new ParsePosition(0);
        Number result = df.parse("42\u200E%\u200E ", ppos);
        assertEquals("Should parse as percentage", new BigDecimal("0.42"), result);
        assertEquals("Should consume the trailing bidi since it is in the symbol", 5, ppos.getIndex());
        ppos.setIndex(0);
        result = df.parse("-42a\u200E ", ppos);
        assertEquals("Should parse as percent", -0.42, result.doubleValue());
        assertEquals("Should not consume the trailing bidi or whitespace", 4, ppos.getIndex());

        // A few more cases based on the docstring:
        expect(df, "42%", 0.42);
        expect(df, "42 %", 0.42);
        expect(df, "42   %", 0.42);
        expect(df, "42\u00A0%", 0.42);
    }

    @Test
    public void testCustomCurrencyUsageOverridesPattern() {
        DecimalFormat df = new DecimalFormat("#,##0.###");
        expect2(df, 1234, "1,234");
        df.setCurrencyUsage(CurrencyUsage.STANDARD);
        expect2(df, 1234, "1,234.00");
        df.setCurrencyUsage(null);
        expect2(df, 1234, "1,234");
    }

    @Test
    public void testCurrencyUsageFractionOverrides() {
        NumberFormat df = DecimalFormat.getCurrencyInstance(ULocale.US);
        expect2(df, 35.0, "$35.00");
        df.setMinimumFractionDigits(3);
        expect2(df, 35.0, "$35.000");
        df.setMaximumFractionDigits(3);
        expect2(df, 35.0, "$35.000");
        df.setMinimumFractionDigits(-1);
        expect2(df, 35.0, "$35.00");
        df.setMaximumFractionDigits(-1);
        expect2(df, 35.0, "$35.00");
    }

    @Test
    public void testParseVeryVeryLargeExponent() {
        DecimalFormat df = new DecimalFormat();
        ParsePosition ppos = new ParsePosition(0);

        Object[][] cases = {
                {"1.2E+1234567890", Double.POSITIVE_INFINITY},
                {"1.2E+999999999", new com.ibm.icu.math.BigDecimal("1.2E+999999999")},
                {"1.2E+1000000000", Double.POSITIVE_INFINITY},
                {"-1.2E+999999999", new com.ibm.icu.math.BigDecimal("-1.2E+999999999")},
                {"-1.2E+1000000000", Double.NEGATIVE_INFINITY},
                {"1.2E-999999999", new com.ibm.icu.math.BigDecimal("1.2E-999999999")},
                {"1.2E-1000000000", 0.0},
                {"-1.2E-999999999", new com.ibm.icu.math.BigDecimal("-1.2E-999999999")},
                {"-1.2E-1000000000", -0.0},

        };

        for (Object[] cas : cases) {
            ppos.setIndex(0);
            String input = (String) cas[0];
            Number expected = (Number) cas[1];
            Number actual = df.parse(input, ppos);
            assertEquals(input, expected, actual);
        }
    }

    @Test
    public void testStringMethodsNPE() {
        String[] npeMethods = {
                "applyLocalizedPattern",
                "applyPattern",
                "setNegativePrefix",
                "setNegativeSuffix",
                "setPositivePrefix",
                "setPositiveSuffix"
        };
        for (String npeMethod : npeMethods) {
            DecimalFormat df = new DecimalFormat();
            try {
                DecimalFormat.class.getDeclaredMethod(npeMethod, String.class).invoke(df, (String) null);
                fail("NullPointerException not thrown in method " + npeMethod);
            } catch (InvocationTargetException e) {
                assertTrue("Exception should be NullPointerException in method " + npeMethod,
                        e.getCause() instanceof NullPointerException);
            } catch (Exception e) {
                // Other reflection exceptions
                throw new AssertionError("Reflection error in method " + npeMethod + ": " + e.getMessage());
            }
        }

        // Also test the constructors
        try {
            new DecimalFormat(null);
            fail("NullPointerException not thrown in 1-parameter constructor");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            new DecimalFormat(null, new DecimalFormatSymbols());
            fail("NullPointerException not thrown in 2-parameter constructor");
        } catch (NullPointerException e) {
            // Expected
        }
        try {
            new DecimalFormat(null, new DecimalFormatSymbols(), CurrencyPluralInfo.getInstance(), 0);
            fail("NullPointerException not thrown in 4-parameter constructor");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void testParseNoExponent() throws ParseException {
        DecimalFormat df = new DecimalFormat();
        assertEquals("Parse no exponent has wrong default", false, df.isParseNoExponent());
        Number result1 = df.parse("123E4");
        df.setParseNoExponent(true);
        assertEquals("Parse no exponent getter is broken", true, df.isParseNoExponent());
        Number result2 = df.parse("123E4");
        assertEquals("Exponent did not parse before setParseNoExponent", result1, new Long(1230000));
        assertEquals("Exponent parsed after setParseNoExponent", result2, new Long(123));
    }

    @Test
    public void testMinimumGroupingDigits() {
        String[][] allExpected = {
                {"123", "123"},
                {"1,230", "1230"},
                {"12,300", "12,300"},
                {"1,23,000", "1,23,000"}
        };

        DecimalFormat df = new DecimalFormat("#,##,##0");
        assertEquals("Minimum grouping digits has wrong default", 1, df.getMinimumGroupingDigits());

        for (int l = 123, i=0; l <= 123000; l *= 10, i++) {
            df.setMinimumGroupingDigits(1);
            assertEquals("Minimum grouping digits getter is broken", 1, df.getMinimumGroupingDigits());
            String actual = df.format(l);
            assertEquals("Output is wrong for 1, "+i, allExpected[i][0], actual);
            df.setMinimumGroupingDigits(2);
            assertEquals("Minimum grouping digits getter is broken", 2, df.getMinimumGroupingDigits());
            actual = df.format(l);
            assertEquals("Output is wrong for 2, "+i, allExpected[i][1], actual);
        }

        String[] locales = {"en-US", "es"};
        int[] groupingDigits = {
          1,
          DecimalFormat.MINIMUM_GROUPING_DIGITS_AUTO,
          DecimalFormat.MINIMUM_GROUPING_DIGITS_MIN2
        };
        int[] values = {1000, 10000};
        String[] allExpected2 = {
          // locale: en-US
          "1,000", "10,000",  // minimumGroupingDigits = 1
          "1,000", "10,000",  // minimumGroupingDigits = MINIMUM_GROUPING_DIGITS_AUTO
          "1000" , "10,000",  // minimumGroupingDigits = MINIMUM_GROUPING_DIGITS_MIN2
          // locale: es
          "1.000", "10.000",  // minimumGroupingDigits = 1
          "1000",  "10.000",  // minimumGroupingDigits = MINIMUM_GROUPING_DIGITS_AUTO
          "1000",  "10.000"   // minimumGroupingDigits = MINIMUM_GROUPING_DIGITS_MIN2
        };

        int i = 0;
        for (String locale : locales) {
          for (int minimumGroupingDigits : groupingDigits) {
            for (int value : values) {
              NumberFormat f = NumberFormat.getInstance(new ULocale(locale));
              df = (DecimalFormat) f;
              df.setMinimumGroupingDigits(minimumGroupingDigits);
              String actual = df.format(value);
              String expected = allExpected2[i++];
              assertEquals("Output is wrong for " + value +
                  " locale=" + locale + " minimumGroupingDigits=" + minimumGroupingDigits,
                  expected, actual);
            }
          }
        }

    }

    @Test
    public void testParseCaseSensitive() {
        String[] patterns = {"a#b", "A#B"};
        String[] inputs = {"a500b", "A500b", "a500B", "a500e10b", "a500E10b"};
        int[][] expectedParsePositions = {
                {5, 5, 5, 8, 8}, // case insensitive, pattern 0
                {5, 0, 4, 4, 8}, // case sensitive, pattern 0
                {5, 5, 5, 8, 8}, // case insensitive, pattern 1
                {0, 4, 0, 0, 0}, // case sensitive, pattern 1
        };

        for (int p = 0; p < patterns.length; p++) {
            String pat = patterns[p];
            DecimalFormat df = new DecimalFormat(pat);
            assertEquals("parseCaseSensitive default is wrong", false, df.isParseCaseSensitive());
            for (int i = 0; i < inputs.length; i++) {
                String inp = inputs[i];
                df.setParseCaseSensitive(false);
                assertEquals("parseCaseSensitive getter is broken", false, df.isParseCaseSensitive());
                ParsePosition actualInsensitive = new ParsePosition(0);
                df.parse(inp, actualInsensitive);
                assertEquals("Insensitive, pattern "+p+", input "+i,
                        expectedParsePositions[p*2][i], actualInsensitive.getIndex());
                df.setParseCaseSensitive(true);
                assertEquals("parseCaseSensitive getter is broken", true, df.isParseCaseSensitive());
                ParsePosition actualSensitive = new ParsePosition(0);
                df.parse(inp, actualSensitive);
                assertEquals("Sensitive, pattern "+p+", input "+i,
                        expectedParsePositions[p*2+1][i], actualSensitive.getIndex());
            }
        }
    }

    @Test
    public void testPlusSignAlwaysShown() throws ParseException {
        double[] numbers = {0.012, 5.78, 0, -0.012, -5.78};
        ULocale[] locs = {new ULocale("en-US"), new ULocale("ar-EG"), new ULocale("es-CL")};
        String[][][] expecteds = {
                // en-US
                {
                    // decimal
                    { "+0.012", "+5.78", "+0", "-0.012", "-5.78" },
                    // currency
                    { "+$0.01", "+$5.78", "+$0.00", "-$0.01", "-$5.78" }
                },
                // ar-EG (interesting because the plus sign string starts with \u061C)
                {
                    // decimal
                    {
                        "\u061C+\u0660\u066B\u0660\u0661\u0662", // "؜+٠٫٠١٢"
                        "\u061C+\u0665\u066B\u0667\u0668", // "؜+٥٫٧٨"
                        "\u061C+\u0660", // "؜+٠"
                        "\u061C-\u0660\u066B\u0660\u0661\u0662", // "؜-٠٫٠١٢"
                        "\u061C-\u0665\u066B\u0667\u0668", // "؜-٥٫٧٨"
                    },
                    // currency (\062C.\0645.\200F is the currency sign in ar for EGP)
                    {
                        "\u061C+\u0660\u066B\u0660\u0661\u00A0\u062C.\u0645.\u200F",
                        "\u061C+\u0665\u066B\u0667\u0668\u00A0\u062C.\u0645.\u200F",
                        "\u061C+\u0660\u066B\u0660\u0660\u00A0\u062C.\u0645.\u200F",
                        "\u061C-\u0660\u066B\u0660\u0661\u00A0\u062C.\u0645.\u200F",
                        "\u061C-\u0665\u066B\u0667\u0668\u00A0\u062C.\u0645.\u200F"
                    }
                },
                // es-CL (interesting because of position of sign in currency)
                {
                    // decimal
                    { "+0,012", "+5,78", "+0", "-0,012", "-5,78" },
                    // currency (note: rounding for es-CL's currency, CLP, is 0 fraction digits)
                    { "$+0", "$+6", "$+0", "$-0", "$-6" }
                }
        };

        for (int i=0; i<locs.length; i++) {
            ULocale loc = locs[i];
            DecimalFormat df1 = (DecimalFormat) NumberFormat.getNumberInstance(loc);
            assertFalse("Default should be false", df1.isSignAlwaysShown());
            df1.setSignAlwaysShown(true);
            assertTrue("Getter should now return true", df1.isSignAlwaysShown());
            DecimalFormat df2 = (DecimalFormat) NumberFormat.getCurrencyInstance(loc);
            assertFalse("Default should be false", df2.isSignAlwaysShown());
            df2.setSignAlwaysShown(true);
            assertTrue("Getter should now return true", df2.isSignAlwaysShown());
            for (int j=0; j<2; j++) {
                DecimalFormat df = (j == 0) ? df1 : df2;
                for (int k=0; k<numbers.length; k++) {
                    double d = numbers[k];
                    String exp = expecteds[i][j][k];
                    String act = df.format(d);
                    assertEquals("Locale " + loc + ", type " + j + ", " + d, exp, act);
                    BigDecimal parsedExp = BigDecimal.valueOf(d);
                    if (j == 1) {
                        // Currency-round expected parse output
                        int scale = (i == 2) ? 0 : 2;
                        parsedExp = parsedExp.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
                    }
                    Number parsedNum = df.parse(exp);
                    BigDecimal parsedAct = (parsedNum.getClass() == BigDecimal.class)
                            ? (BigDecimal) parsedNum
                            : BigDecimal.valueOf(parsedNum.doubleValue());
                    assertEquals(
                            "Locale " + loc + ", type " + j + ", " + d + ", " + parsedExp + " => " + parsedAct,
                            0, parsedExp.compareTo(parsedAct));
                }
            }
        }
    }

    @Test
    public void Test20073_StrictPercentParseErrorIndex() {
        ParsePosition parsePosition = new ParsePosition(0);
        DecimalFormat df = new DecimalFormat("0%", DecimalFormatSymbols.getInstance(Locale.US));
        df.setParseStrict(true);
        Number number = df.parse("%2%", parsePosition);
        assertNull("", number);
        assertEquals("", 0, parsePosition.getIndex());
        assertEquals("", 0, parsePosition.getErrorIndex());
    }

    @Test
    public void Test11626_CustomizeCurrencyPluralInfo() throws ParseException {
        // Use locale sr because it has interesting plural rules.
        ULocale locale = ULocale.forLanguageTag("sr");
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        CurrencyPluralInfo info = CurrencyPluralInfo.getInstance(locale);
        info.setCurrencyPluralPattern("one", "0 qwerty");
        info.setCurrencyPluralPattern("few", "0 dvorak");
        DecimalFormat df = new DecimalFormat("#", symbols, info, NumberFormat.CURRENCYSTYLE);
        df.setCurrency(Currency.getInstance("USD"));
        df.setMaximumFractionDigits(0);

        assertEquals("Plural one", "1 qwerty", df.format(1));
        assertEquals("Plural few", "3 dvorak", df.format(3));
        assertEquals("Plural other", "99 америчких долара", df.format(99));

        info.setPluralRules("few: n is 1; one: n in 2..4");
        df.setCurrencyPluralInfo(info);
        assertEquals("Plural one", "1 dvorak", df.format(1));
        assertEquals("Plural few", "3 qwerty", df.format(3));
        assertEquals("Plural other", "99 америчких долара", df.format(99));
    }

    @Test
    public void TestNarrowCurrencySymbols() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(ULocale.CANADA);
        df.setCurrency(Currency.getInstance("USD"));
        expect2(df, 123.45, "US$123.45");
        String pattern = df.toPattern();
        pattern = pattern.replace("¤", "¤¤¤¤¤");
        df.applyPattern(pattern);
        // Note: Narrow currency is not parseable because of ambiguity.
        assertEquals("Narrow currency symbol for USD in en_CA is US$",
                "US$123.45", df.format(123.45));
    }

    @Test
    public void TestAffixOverrideBehavior() {
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(ULocale.ENGLISH);
        expect2(df, 100, "100");
        expect2(df, -100, "-100");
        // This is not the right way to set an override plus sign, but we need to support it for compatibility.
        df.setPositivePrefix("+");
        expect2(df, 100, "+100");
        expect2(df, -100, "-100"); // note: the positive prefix does not affect the negative prefix
        df.applyPattern("a0");
        expect2(df, 100, "a100");
        expect2(df, -100, "-a100");
    }

    @Test
    public void TestCurrencyRoundingMinWithoutMax() {
        NumberFormat currencyFormat = DecimalFormat.getCurrencyInstance(Locale.US);
        currencyFormat.setCurrency(Currency.getInstance("AUD"));
        currencyFormat.setMinimumFractionDigits(0);
        expect(currencyFormat, 0.001, "A$0");

        // NOTE: The size of the increment takes precedent over minFrac since ICU 59.
        // CAD-Cash uses nickel rounding.
        currencyFormat = DecimalFormat.getCurrencyInstance(Locale.US);
        currencyFormat.setCurrency(Currency.getInstance("CAD"));
        ((DecimalFormat)currencyFormat).setCurrencyUsage(CurrencyUsage.CASH);
        currencyFormat.setMinimumFractionDigits(0);
        // expect(currencyFormat, 0.08, "CA$0.1");  // ICU 58 and down
        expect(currencyFormat, 0.08, "CA$0.10");  // ICU 59 and up
    }

    @Test
    public void testParsePositionIncrease() {
        String input = "123\n456\n$789";
        ParsePosition ppos = new ParsePosition(0);
        DecimalFormat df = new DecimalFormat();
        df.parse(input, ppos);
        assertEquals("Should stop after first entry", 3, ppos.getIndex());
        ppos.setIndex(ppos.getIndex() + 1);
        df.parse(input, ppos);
        assertEquals("Should stop after second entry", 7, ppos.getIndex());
        ppos.setIndex(ppos.getIndex() + 1);
        df.parseCurrency(input, ppos); // test parseCurrency API as well
        assertEquals("Should stop after third entry", 12, ppos.getIndex());
    }

    @Test
    public void testTrailingMinusSign() {
        String input = "52-";
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(ULocale.ENGLISH);
        ParsePosition ppos = new ParsePosition(0);
        Number result = df.parse(input, ppos);
        assertEquals("Trailing sign should NOT be accepted after the number in English by default",
                52.0,
                result.doubleValue(),
                0.0);
        df.applyPattern("#;#-");
        ppos.setIndex(0);
        result = df.parse(input, ppos);
        assertEquals("Trailing sign SHOULD be accepted if there is one in the pattern",
                -52.0,
                result.doubleValue(),
                0.0);
    }

    @Test
    public void testScientificCustomSign() {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        dfs.setMinusSignString("nnn");
        dfs.setPlusSignString("ppp");
        DecimalFormat df = new DecimalFormat("0E0", dfs);
        df.setExponentSignAlwaysShown(true);
        expect2(df, 0.5, "5Ennn1");
        expect2(df, 50, "5Eppp1");
    }

    @Test
    public void testParsePercentInPattern() {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        DecimalFormat df = new DecimalFormat("0x%", dfs);
        df.setParseStrict(true);
        expect2(df, 0.5, "50x%");
    }

    @Test
    public void testParseIsoStrict() {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        DecimalFormat df = new DecimalFormat("¤¤0;-0¤¤", dfs);
        df.setCurrency(Currency.getInstance("USD"));
        df.setParseStrict(true);
        expect2(df, 45, "USD 45.00");
        expect2(df, -45, "-45.00 USD");
    }

    @Test
    public void test13684_FrenchPercentParsing() {
        NumberFormat numberFormat = NumberFormat.getPercentInstance(ULocale.FRENCH);
        numberFormat.setParseStrict(true);
        ParsePosition ppos = new ParsePosition(0);
        Number percentage = numberFormat.parse("8\u00A0%", ppos);
        assertEquals("Should parse successfully", 0.08, percentage.doubleValue());
        assertEquals("Should consume whole string", 3, ppos.getIndex());
    }

    @Test
    public void testStrictParseCurrencyLongNames() {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(ULocale.ENGLISH, DecimalFormat.PLURALCURRENCYSTYLE);
        df.setParseStrict(true);
        df.setCurrency(Currency.getInstance("USD"));
        double input = 514.23;
        String formatted = df.format(input);
        String expected = "514.23 US dollars";
        assertEquals("Should format as expected", expected, formatted);
        ParsePosition ppos = new ParsePosition(0);
        CurrencyAmount ca = df.parseCurrency(formatted, ppos);
        assertEquals("Should consume whole number", ppos.getIndex(), 17);
        assertEquals("Number should round-trip", ca.getNumber().doubleValue(), input);
        assertEquals("Should get correct currency", ca.getCurrency().getCurrencyCode(), "USD");
        // Should also round-trip in non-currency parsing
        expect2(df, input, expected);
    }

    @Test
    public void testStrictParseCurrencySpacing() {
        DecimalFormat df = new DecimalFormat("¤ 0", DecimalFormatSymbols.getInstance(ULocale.ROOT));
        df.setCurrency(Currency.getInstance("USD"));
        df.setParseStrict(true);
        expect2(df, -51.42, "-US$ 51.42");
    }

    @Test
    public void testCaseSensitiveCustomIsoCurrency() {
        DecimalFormat df = new DecimalFormat("¤¤0", DecimalFormatSymbols.getInstance(ULocale.ENGLISH));
        df.setCurrency(Currency.getInstance("ICU"));
        ParsePosition ppos = new ParsePosition(0);
        df.parseCurrency("icu123", ppos);
        assertEquals("Should succeed", 6, ppos.getIndex());
        assertEquals("Should succeed", -1, ppos.getErrorIndex());
    }

    @Test
    public void testCurrencyPluralAffixOverrides() {
        // The affix setters should override CurrencyPluralInfo, used in the plural currency constructor.
        DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(ULocale.ENGLISH, NumberFormat.PLURALCURRENCYSTYLE);
        assertEquals("Defaults to unknown currency", " (unknown currency)", df.getPositiveSuffix());
        df.setCurrency(Currency.getInstance("USD"));
        assertEquals("Should resolve to CurrencyPluralInfo", " US dollars", df.getPositiveSuffix());
        df.setPositiveSuffix("lala");
        assertEquals("Custom suffix should round-trip", "lala", df.getPositiveSuffix());
        assertEquals("Custom suffix should be used in formatting", "123.00lala", df.format(123));
    }

    @Test
    public void testParseDoubleMinus() {
        DecimalFormat df = new DecimalFormat("-0", DecimalFormatSymbols.getInstance(ULocale.ENGLISH));
        expect2(df, -5, "--5");
    }

    @Test
    public void testParsePercentRegression() {
        DecimalFormat df1 = (DecimalFormat) NumberFormat.getInstance(ULocale.ENGLISH);
        DecimalFormat df2 = (DecimalFormat) NumberFormat.getPercentInstance(ULocale.ENGLISH);
        df1.setParseStrict(false);
        df2.setParseStrict(false);

        {
            ParsePosition ppos = new ParsePosition(0);
            Number result = df1.parse("50%", ppos);
            assertEquals("df1 should accept a number but not the percent sign", 2, ppos.getIndex());
            assertEquals("df1 should return the number as 50", 50.0, result.doubleValue());
        }
        {
            ParsePosition ppos = new ParsePosition(0);
            Number result = df2.parse("50%", ppos);
            assertEquals("df2 should accept the percent sign", 3, ppos.getIndex());
            assertEquals("df2 should return the number as 0.5", 0.5, result.doubleValue());
        }
        {
            ParsePosition ppos = new ParsePosition(0);
            Number result = df2.parse("50", ppos);
            assertEquals("df2 should return the number as 0.5 even though the percent sign is missing",
                    0.5,
                    result.doubleValue());
        }
    }

    @Test
    public void test13148_GroupingSeparatorOverride() throws Exception {
        DecimalFormat fmt = (DecimalFormat)NumberFormat.getInstance(new ULocale("en", "ZA"));
        DecimalFormatSymbols symbols = fmt.getDecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
        fmt.setDecimalFormatSymbols(symbols);
        Number number = fmt.parse("300,000");
        assertEquals("Should use custom symbols and not monetary symbols", 300000L, number);
    }

    @Test
    public void test11897_LocalizedPatternSeparator() {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        dfs.setPatternSeparator('!');
        DecimalFormat df = new DecimalFormat("0", dfs);
        df.applyPattern("a0;b0"); // should not throw
        assertEquals("should apply the normal pattern", df.getNegativePrefix(), "b");
        df.applyLocalizedPattern("c0!d0"); // should not throw
        assertEquals("should apply the localized pattern", df.getNegativePrefix(), "d");
    }

    @Test
    public void test13777_ParseLongNameNonCurrencyMode() {
        // Currency long name should round-trip even when non-currency parsing is used.
        NumberFormat df = NumberFormat.getInstance(ULocale.US, NumberFormat.PLURALCURRENCYSTYLE);
        expect2(df, 1.5, "1.50 US dollars");
    }

    @Test
    public void test13804_EmptyStringsWhenParsing() {
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        dfs.setCurrencySymbol("");
        dfs.setDecimalSeparatorString("");
        dfs.setDigitStrings(new String[] { "", "", "", "", "", "", "", "", "", "" });
        dfs.setExponentMultiplicationSign("");
        dfs.setExponentSeparator("");
        dfs.setGroupingSeparatorString("");
        dfs.setInfinity("");
        dfs.setInternationalCurrencySymbol("");
        dfs.setMinusSignString("");
        dfs.setMonetaryDecimalSeparatorString("");
        dfs.setMonetaryGroupingSeparatorString("");
        dfs.setNaN("");
        dfs.setPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_INSERT, false, "");
        dfs.setPatternForCurrencySpacing(DecimalFormatSymbols.CURRENCY_SPC_INSERT, true, "");
        dfs.setPercentString("");
        dfs.setPerMillString("");
        dfs.setPlusSignString("");

        DecimalFormat df = new DecimalFormat("0", dfs);
        df.setGroupingUsed(true);
        df.setScientificNotation(true);
        df.setParseStrict(false); // enable all matchers
        df.format(0); // should not throw or hit infinite loop
        String[] samples = new String[] {
                "",
                "123",
                "$123",
                "-",
                "+",
                "44%",
                "1E+2.3"
        };
        for (String sample : samples) {
            logln("Attempting parse on: " + sample);
            // We don't care about the results, only that we don't throw and don't loop.
            ParsePosition ppos = new ParsePosition(0);
            df.parse(sample, ppos);
            ppos = new ParsePosition(0);
            df.parseCurrency(sample, ppos);
        }

        // Test with a nonempty exponent separator symbol to cover more code
        dfs.setExponentSeparator("E");
        df.setDecimalFormatSymbols(dfs);
        {
            ParsePosition ppos = new ParsePosition(0);
            df.parse("1E+2.3", ppos);
        }
    }

    @Test
    public void Test20037_ScientificIntegerOverflow() throws ParseException {
        NumberFormat nf = NumberFormat.getInstance(ULocale.US);

        // Test overflow of exponent
        Number result = nf.parse("1E-2147483648");
        assertEquals("Should snap to zero",
                "0", result.toString());

        // Test edge case overflow of exponent
        // Note: the behavior is different from C++; this is probably due to the
        // intermediate BigDecimal form, which has its own restrictions
        result = nf.parse("1E-2147483647E-1");
        assertEquals("Should not overflow and should parse only the first exponent",
                "0.0", result.toString());

        // For Java, we should get *pretty close* to 2^31.
        result = nf.parse("1E-547483647");
        assertEquals("Should *not* snap to zero",
                "1E-547483647", result.toString());

        // Test edge case overflow of exponent
        result = nf.parse(".0003e-2147483644");
        assertEquals("Should not overflow",
                "0", result.toString());

        // Test largest parseable exponent
        // This is limited by ICU's BigDecimal implementation
        result = nf.parse("1e999999999");
        assertEquals("Should not overflow",
                "1E+999999999", result.toString());

        // Test max value as well
        String[] infinityInputs = {
                "9876e1000000000",
                "9876e2147483640",
                "9876e2147483641",
                "9876e2147483642",
                "9876e2147483643",
                "9876e2147483644",
                "9876e2147483645",
                "9876e2147483646",
                "9876e2147483647",
                "9876e2147483648",
                "9876e2147483649",
        };
        for (String input : infinityInputs) {
            result = nf.parse(input);
            assertEquals("Should become Infinity: " + input,
                    "Infinity", result.toString());
        }
    }

    @Test
    public void test13840_ParseLongStringCrash() throws ParseException {
        NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH);
        String bigString =
            "111111111111111111111111111111111111111111111111111111111111111111111" +
            "111111111111111111111111111111111111111111111111111111111111111111111" +
            "111111111111111111111111111111111111111111111111111111111111111111111" +
            "111111111111111111111111111111111111111111111111111111111111111111111" +
            "111111111111111111111111111111111111111111111111111111111111111111111" +
            "111111111111111111111111111111111111111111111111111111111111111111111";
        Number result = nf.parse(bigString);

        // Normalize the input string:
        BigDecimal expectedBigDecimal = new BigDecimal(bigString);
        String expectedUString = expectedBigDecimal.toString();

        // Get the output string:
        BigDecimal actualBigDecimal = (BigDecimal) result;
        String actualUString = actualBigDecimal.toString();

        assertEquals("Should round-trip without crashing", expectedUString, actualUString);
    }

    @Test
    public void test20348_CurrencyPrefixOverride() {
        DecimalFormat fmt = (DecimalFormat) NumberFormat.getCurrencyInstance(ULocale.ENGLISH);
        assertEquals("Initial pattern",
            "¤#,##0.00", fmt.toPattern());
        assertEquals("Initial prefix",
            "¤", fmt.getPositivePrefix());
        assertEquals("Initial suffix",
            "-¤", fmt.getNegativePrefix());
        assertEquals("Initial format",
            "\u00A4100.00", fmt.format(100));

        fmt.setPositivePrefix("$");
        assertEquals("Set positive prefix pattern",
            "$#,##0.00;-\u00A4#,##0.00", fmt.toPattern());
        assertEquals("Set positive prefix prefix",
            "$", fmt.getPositivePrefix());
        assertEquals("Set positive prefix suffix",
            "-¤", fmt.getNegativePrefix());
        assertEquals("Set positive prefix format",
            "$100.00", fmt.format(100));

        fmt.setNegativePrefix("-$");
        assertEquals("Set negative prefix pattern",
            "$#,##0.00;'-'$#,##0.00", fmt.toPattern());
        assertEquals("Set negative prefix prefix",
            "$", fmt.getPositivePrefix());
        assertEquals("Set negative prefix suffix",
            "-$", fmt.getNegativePrefix());
        assertEquals("Set negative prefix format",
            "$100.00", fmt.format(100));
    }

    @Test
    public void test20956_MonetarySymbolGetters() {
        Locale locale = new Locale.Builder().setLocale(Locale.forLanguageTag("et")).build();
        DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
        Currency currency = Currency.getInstance("EEK");

        decimalFormat.setCurrency(currency);

        DecimalFormatSymbols decimalFormatSymbols = decimalFormat.getDecimalFormatSymbols();
        assertEquals("MONETARY DECIMAL SEPARATOR", ".", decimalFormatSymbols.getMonetaryDecimalSeparatorString());
        assertEquals("DECIMAL SEPARATOR", ",", decimalFormatSymbols.getDecimalSeparatorString());
        assertEquals("MONETARY GROUPING SEPARATOR", " ", decimalFormatSymbols.getMonetaryGroupingSeparatorString());
        assertEquals("GROUPING SEPARATOR", " ", decimalFormatSymbols.getGroupingSeparatorString());
        assertEquals("CURRENCY SYMBOL", "kr", decimalFormatSymbols.getCurrencySymbol());

        StringBuffer sb = new StringBuffer();
        decimalFormat.format(new BigDecimal(12345.12), sb, DontCareFieldPosition.INSTANCE);
        assertEquals("OUTPUT", "12 345.12 kr", sb.toString());
    }

    @Test
    public void test20358_GroupingInPattern() {
        DecimalFormat fmt = (DecimalFormat) NumberFormat.getInstance(ULocale.ENGLISH);
        assertEquals("Initial pattern",
            "#,##0.###", fmt.toPattern());
        assertTrue("Initial grouping",
            fmt.isGroupingUsed());
        assertEquals("Initial format",
            "54,321", fmt.format(54321));

        fmt.setGroupingUsed(false);
        assertEquals("Set grouping false",
            "0.###", fmt.toPattern());
        assertFalse("Set grouping false grouping",
            fmt.isGroupingUsed());
        assertEquals("Set grouping false format",
            "54321", fmt.format(54321));

        fmt.setGroupingUsed(true);
        assertEquals("Set grouping true",
            "#,##0.###", fmt.toPattern());
        assertTrue("Set grouping true grouping",
            fmt.isGroupingUsed());
        assertEquals("Set grouping true format",
            "54,321", fmt.format(54321));
    }

    @Test
    public void test13731_DefaultCurrency() {
        {
            NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH, NumberFormat.CURRENCYSTYLE);
            assertEquals("symbol", "¤1.10", nf.format(1.1));
            assertEquals("currency", "XXX", nf.getCurrency().getCurrencyCode());
        }
        {
            NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH, NumberFormat.ISOCURRENCYSTYLE);
            assertEquals("iso_code", "XXX 1.10", nf.format(1.1));
            assertEquals("currency", "XXX", nf.getCurrency().getCurrencyCode());
        }
        {
            NumberFormat nf = NumberFormat.getInstance(ULocale.ENGLISH, NumberFormat.PLURALCURRENCYSTYLE);
            assertEquals("plural", "1.10 (unknown currency)", nf.format(1.1));
            assertEquals("currency", "XXX", nf.getCurrency().getCurrencyCode());
        }
    }

    @Test
    public void test20499_CurrencyVisibleDigitsPlural() {
        ULocale locale = new ULocale("ro-RO");
        NumberFormat nf = NumberFormat.getInstance(locale, NumberFormat.PLURALCURRENCYSTYLE);
        String expected = "24,00 lei românești";
        for (int i=0; i<5; i++) {
            String actual = nf.format(24);
            assertEquals("iteration " + i, expected, actual);
        }
    }

    @Test
    public void test13735_GroupingSizeGetter() {
        DecimalFormatSymbols EN = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        {
            DecimalFormat df = new DecimalFormat("0", EN);
            assertEquals("pat 0: ", 0, df.getGroupingSize());
            df.setGroupingUsed(false);
            assertEquals("pat 0 then disabled: ", 0, df.getGroupingSize());
            df.setGroupingUsed(true);
            assertEquals("pat 0 then enabled: ", 0, df.getGroupingSize());
        }
        {
            DecimalFormat df = new DecimalFormat("#,##0", EN);
            assertEquals("pat #,##0: ", 3, df.getGroupingSize());
            df.setGroupingUsed(false);
            assertEquals("pat #,##0 then disabled: ", 3, df.getGroupingSize());
            df.setGroupingUsed(true);
            assertEquals("pat #,##0 then enabled: ", 3, df.getGroupingSize());
        }
    }

    @Test
    public void test13734_StrictFlexibleWhitespace() {
        DecimalFormatSymbols EN = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
        {
          DecimalFormat df = new DecimalFormat("+0", EN);
          df.setParseStrict(true);
          ParsePosition ppos = new ParsePosition(0);
          Number result = df.parse("+  33", ppos);
          assertEquals("ppos: ", 0, ppos.getIndex());
          assertEquals("result: ", null, result);
        }
        {
          DecimalFormat df = new DecimalFormat("+ 0", EN);
          df.setParseStrict(true);
          ParsePosition ppos = new ParsePosition(0);
          Number result = df.parse("+  33", ppos);
          assertEquals("ppos: ", 0, ppos.getIndex());
          assertEquals("result: ", null, result);
        }
    }

    @Test
    public void test20961_CurrencyPluralPattern() {
        DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(ULocale.US, NumberFormat.PLURALCURRENCYSTYLE);
        assertEquals("Currency pattern", "#,##0.00 ¤¤¤", decimalFormat.toPattern());
    }

    @Test
    public void test13733_StrictAndLenient() {
        Object[][] cases = { {"CA$ 12", "¤ 0", 12, 12},
                {"CA$12", "¤0", 12, 12},
                {"CAD 12", "¤¤ 0", 12, 12},
                {"12 CAD", "0 ¤¤", 12, 12},
                {"12 Canadian dollars", "0 ¤¤¤", 12, 12},
                {"$12 ", "¤¤¤¤0", 12, 12},
                {"12$", "0¤¤¤¤", 12, 12},
                {"CA$ 12", "¤0", 0, 12},
                {"CA$ 12", "0 ¤¤", 0, 12},
                {"CA$ 12", "0 ¤¤¤", 0, 12},
                {"CA$ 12", "¤¤¤¤0", 0, 12},
                {"CA$ 12", "0¤¤¤¤", 0, 12},
                {"CA$12", "¤ 0", 0, 12},
                {"CA$12", "¤¤ 0", 0, 12},
                {"CA$12", "0 ¤¤", 0, 12},
                {"CA$12", "0 ¤¤¤", 0, 12},
                {"CA$12", "0¤¤¤¤", 0, 12},
                {"CAD 12", "¤0", 0, 12},
                {"CAD 12", "0 ¤¤", 0, 12},
                {"CAD 12", "0 ¤¤¤", 0, 12},
                {"CAD 12", "¤¤¤¤0", 0, 12},
                {"CAD 12", "0¤¤¤¤", 0, 12},
                {"12 CAD", "¤ 0", 0, 12},
                {"12 CAD", "¤0", 0, 12},
                {"12 CAD", "¤¤ 0", 0, 12},
                {"12 CAD", "¤¤¤¤0", 0, 12},
                {"12 CAD", "0¤¤¤¤", 0, 12},
                {"12 Canadian dollars", "¤ 0", 0, 12},
                {"12 Canadian dollars", "¤0", 0, 12},
                {"12 Canadian dollars", "¤¤ 0", 0, 12},
                {"12 Canadian dollars", "¤¤¤¤0", 0, 12},
                {"12 Canadian dollars", "0¤¤¤¤", 0, 12},
                {"$12 ", "¤ 0", 0, 12},
                {"$12 ", "¤¤ 0", 0, 12},
                {"$12 ", "0 ¤¤", 0, 12},
                {"$12 ", "0 ¤¤¤", 0, 12},
                {"$12 ", "0¤¤¤¤", 0, 12},
                {"12$", "¤ 0", 0, 12},
                {"12$", "¤0", 0, 12},
                {"12$", "¤¤ 0", 0, 12},
                {"12$", "0 ¤¤", 0, 12},
                {"12$", "0 ¤¤¤", 0, 12},
                {"12$", "¤¤¤¤0", 0, 12} };

        for (Object[] cas : cases) {
            String inputString = (String) cas[0];
            String patternString = (String) cas[1];
            int expectedStrictParse = (int) cas[2];
            int expectedLenientParse = (int) cas[3];

            int parsedStrictValue = 0;
            int parsedLenientValue = 0;
            ParsePosition ppos = new ParsePosition(0);
            DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
            DecimalFormat df = new DecimalFormat(patternString, dfs);

            df.setParseStrict(true);
            CurrencyAmount ca_strict = df.parseCurrency(inputString, ppos);
            if (null != ca_strict) {
                parsedStrictValue = ca_strict.getNumber().intValue();
            }
            assertEquals("Strict parse of " + inputString + " using " + patternString,
                    parsedStrictValue, expectedStrictParse);

            ppos.setIndex(0);
            df.setParseStrict(false);
            CurrencyAmount ca_lenient = df.parseCurrency(inputString, ppos);
            if (null != ca_lenient) {
                parsedLenientValue = ca_lenient.getNumber().intValue();
            }
            assertEquals("Strict parse of " + inputString + " using " + patternString,
                    parsedLenientValue, expectedLenientParse);
        }
    }

    @Test
    public void Test20425_IntegerIncrement() {
        DecimalFormat df = new DecimalFormat("##00");
        df.setRoundingIncrement(1);
        String actual = df.format(1235.5);
        assertEquals("Should round to integer", "1236", actual);
    }

    @Test
    public void Test20425_FractionWithIntegerIncrement() {
        DecimalFormat df = new DecimalFormat("0.0");
        df.setRoundingIncrement(1);
        String actual = df.format(8.6);
        assertEquals("Should have a fraction digit", "9.0", actual);
    }

    @Test
    public void Test21232_ParseTimeout() throws ParseException {
        DecimalFormat df = new DecimalFormat();
        StringBuilder input = new StringBuilder();
        input.append("4444444444444444444444444444444444444444");
        for (int i = 0; i < 8; i++) {
            input.append(input);
        }
        assertEquals("Long input of digits", 10240, input.length());
        df.parse(input.toString());
        // Should not hang
    }

    @Test
    public void Test21556_CurrencyAsDecimal() {
        {
            DecimalFormat df = new DecimalFormat("a0¤00b");
            df.setCurrency(Currency.getInstance("EUR"));
            StringBuffer result = new StringBuffer();
            FieldPosition fp = new FieldPosition(NumberFormat.Field.CURRENCY);
            df.format(3.141, result, fp);
            assertEquals("Basic test: format", "a3€14b", result.toString());
            assertEquals("Basic test: toPattern", "a0¤00b", df.toPattern());
            assertEquals("Basic test: field position begin", 2, fp.getBeginIndex());
            assertEquals("Basic test: field position end", 3, fp.getEndIndex());
        }

        {
            NumberFormat nf = NumberFormat.getCurrencyInstance(new ULocale("en-GB"));
            DecimalFormat df = (DecimalFormat) nf;
            df.applyPattern("a0¤00b");
            StringBuffer result = new StringBuffer();
            FieldPosition fp = new FieldPosition(NumberFormat.Field.CURRENCY);
            df.format(3.141, result, fp);
            assertEquals("Via applyPattern: format", "a3£14b", result.toString());
            assertEquals("Via applyPattern: toPattern", "a0¤00b", df.toPattern());
            assertEquals("Via applyPattern: field position begin", 2, fp.getBeginIndex());
            assertEquals("Via applyPattern: field position end", 3, fp.getEndIndex());
        }
    }
}

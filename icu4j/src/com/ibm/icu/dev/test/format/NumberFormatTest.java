/*
 *******************************************************************************
 * Copyright (C) 2001, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/format/NumberFormatTest.java,v $ 
 * $Date: 2002/09/07 00:15:25 $ 
 * $Revision: 1.7 $
 *
 *****************************************************************************************
 */

/** 
 * Port From:   ICU4C v1.8.1 : format : NumberFormatTest
 * Source File: $ICU4CRoot/source/test/intltest/numfmtst.cpp
 **/

package com.ibm.icu.dev.test.format;

import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
import com.ibm.icu.util.*;
import java.util.Locale;
import java.text.ParsePosition;
import java.text.ParseException;
import java.text.FieldPosition;

public class NumberFormatTest extends com.ibm.icu.dev.test.TestFmwk {
    private static final char EURO = '\u20ac';

    public static void main(String[] args) throws Exception {
        new NumberFormatTest().run(args);
    }
    
    // Test various patterns
    public void TestPatterns() {
    
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        final String pat[]    = { "#.#", "#.", ".#", "#" };
        int pat_length = pat.length;
        final String newpat[] = { "#0.#", "#0.", "#.0", "#" };
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
        }
    }
    
    // Test exponential pattern
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
    
    /**
     * Test the handling of the currency symbol in patterns.
     **/
    public void TestCurrencySign() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        StringBuffer pat = new StringBuffer("");
        char currency = 0x00A4;
        // "\xA4#,##0.00;-\xA4#,##0.00"
        pat.append(currency).append("#,##0.00;-").append(currency).append("#,##0.00");
        DecimalFormat fmt = new DecimalFormat(pat.toString(), sym);
        String s = ((NumberFormat) fmt).format(1234.56);
        pat = new StringBuffer("");
        logln("Pattern \"" + fmt.toPattern() + "\"");
        logln(" Format " + 1234.56 + " . " + s);
        if (!s.equals("$1,234.56"))
            errln("FAIL: Expected $1,234.56");
        s = "";
        s = ((NumberFormat) fmt).format(-1234.56);
        logln(" Format " + Double.toString(-1234.56) + " . " + s);
        if (!s.equals("-$1,234.56"))
            errln("FAIL: Expected -$1,234.56");
    
        pat = new StringBuffer("");
        // "\xA4\xA4 #,##0.00;\xA4\xA4 -#,##0.00"
        pat.append(currency).append(currency).append(" #,##0.00;").append(currency).append(currency).append(" -#,##0.00"); 
        fmt = new DecimalFormat(pat.toString(), sym);
        s = "";
        s = ((NumberFormat) fmt).format(1234.56);
        logln("Pattern \"" + fmt.toPattern() + "\"");
        logln(" Format " + Double.toString(1234.56) + " . " + s);
    
        if (!s.equals("USD 1,234.56"))
            errln("FAIL: Expected USD 1,234.56");
        s = "";
        s = ((NumberFormat) fmt).format(-1234.56);
        logln(" Format " + Double.toString(-1234.56) + " . " + s);
        if (!s.equals("USD -1,234.56"))
            errln("FAIL: Expected USD -1,234.56");
    
    }
    
    /**
     * Test localized currency patterns.
     */
    public void TestCurrency() {
        NumberFormat currencyFmt = 
            NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH);
    
        String s;
        s = currencyFmt.format(1.50);
        logln("Un pauvre ici a..........." + s);
            
        if (!s.equals("1,50 $"))
            errln("FAIL: Expected 1,50 $, got " + s);
        s = "";
        currencyFmt = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        s = currencyFmt.format(1.50);
        logln("Un pauvre en Allemagne a.." + s);
        if (!s.equals("1,50 " + EURO))
            errln("FAIL: Expected 1,50 DM, got " + s);
        s = "";
        currencyFmt = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        s = currencyFmt.format(1.50);
        logln("Un pauvre en France a....." + s);
        if (!s.equals("1,50 " + EURO))
            errln("FAIL: Expected 1,50 F, got " + s);
    
    }

    /**
     * Test the Currency registration-related API.
     */
    public void TestCurrencyRegistration() {
        // available locales
        Locale[] locales = Currency.getAvailableLocales();
        logln("available locales");
        for (int i = 0; i < locales.length; ++i) {
            logln("[" + i + "] " + locales[i].toString());
        }

        // identical instance
        Currency fr0 = Currency.getInstance(Locale.FRANCE);
        Currency fr1 = Currency.getInstance(Locale.FRANCE);
        if (fr0 != fr1) {
            errln("non-identical currencies for locale");
        }

        Currency us0 = Currency.getInstance(Locale.US);

        // replace US with FR
        Object key = Currency.register(fr0, Locale.US);

        logln("FRENCH currency: " + fr0);
        logln("US currency: " + us0);

        // query US and get FR back
        Currency us1 = Currency.getInstance(Locale.US);
        if (us1 != fr0) {
            errln("registry failed");
        }
        logln("new US currency: " + us1);

        // unregister and get US back
        if (!Currency.unregister(key)) {
            errln("failed to unregister key: " + key);
        }
        Currency us2 = Currency.getInstance(Locale.US);
        if (!us2.equals(us0)) {
            errln("after unregister US didn't get original currency back: " + us2 + " != " + us0);
        }
    }

    /**
     * Test the Currency object handling, new as of ICU 2.2.
     */
    public void TestCurrencyObject() {
        NumberFormat fmt = 
            NumberFormat.getCurrencyInstance(Locale.US);
        
        expectCurrency(fmt, null, 1234.56, "$1,234.56");

        expectCurrency(fmt, Currency.getInstance(Locale.FRANCE),
                       1234.56, "\u20AC1,234.56"); // Euro

        expectCurrency(fmt, Currency.getInstance(Locale.JAPAN),
                       1234.56, "\uFFE51,235"); // Yen

        expectCurrency(fmt, Currency.getInstance(new Locale("fr", "CH", "")),
                       1234.56, "CHF1,234.50"); // 0.25 rounding

        expectCurrency(fmt, Currency.getInstance(Locale.US),
                       1234.56, "$1,234.56");

        fmt = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        
        expectCurrency(fmt, null, 1234.56, "1 234,56 \u20AC");

        expectCurrency(fmt, Currency.getInstance(Locale.JAPAN),
                       1234.56, "1 235 \uFFE5"); // Yen

        expectCurrency(fmt, Currency.getInstance(new Locale("fr", "CH", "")),
                       1234.56, "1 234,50 CHF"); // 0.25 rounding

        expectCurrency(fmt, Currency.getInstance(Locale.US),
                       1234.56, "1 234,56 USD");

        expectCurrency(fmt, Currency.getInstance(Locale.FRANCE),
                       1234.56, "1 234,56 \u20AC"); // Euro
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

    public void TestCurrencyPatterns() {
        int i;
        Locale[] locs = NumberFormat.getAvailableLocales();
        for (i=0; i<locs.length; ++i) {
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
                DecimalFormat df = (DecimalFormat) nf;
                if ("EUR".equals(df.getCurrency().getCurrencyCode())) {
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
    public void TestParse() {
        String arg = "0.0";
        DecimalFormat format = new DecimalFormat("00");
        double aNumber = 0l;
        try {
            aNumber = format.parse(arg).doubleValue();
        } catch (java.text.ParseException e) {
            System.out.println(e);
        }
        logln("parse(" + arg + ") = " + aNumber);
    }
    
    /**
     * Test proper rounding by the format method.
     */
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
    public void TestSecondaryGrouping() {
    
        DecimalFormatSymbols US = new DecimalFormatSymbols(Locale.US);
        DecimalFormat f = new DecimalFormat("#,##,###", US);
    
        expect(f, 123456789L, "12,34,56,789");
        expectPat(f, "#,##,###");
        f.applyPattern("#,###");
    
        f.setSecondaryGroupingSize(4);
        expect(f, 123456789L, "12,3456,789");
        expectPat(f, "#,####,###");
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
    
    public void roundingTest(NumberFormat nf, double x, int maxFractionDigits, final String expected) {
        nf.setMaximumFractionDigits(maxFractionDigits);
        String out = nf.format(x);
        logln(x + " formats with " + maxFractionDigits + " fractional digits to " + out); 
        if (!out.equals(expected))
            errln("FAIL: Expected " + expected);
    }
    
    /**
     * Upgrade to alphaWorks
     */
    public void expect(NumberFormat fmt, String str, int n) {
        Long num = new Long(0);
        try {
        num = (Long)fmt.parse(str);
        } catch (java.text.ParseException e) {
            logln(e.getMessage());
        }
        String pat = ((DecimalFormat)fmt).toPattern();
        if (num.longValue() == n) {
            logln("Ok   \"" + str + "\" x " +
                  pat + " = " +
                  num.toString());
        } else {
            errln("FAIL \"" + str + "\" x " +
                  pat + " = " +
                  num.toString() + ", expected " + n + "L");
        }
    }
    
    /**
     * Upgrade to alphaWorks
     */        
    public void expect(NumberFormat fmt, final double n, final String exp) {
        StringBuffer saw = new StringBuffer("");
        FieldPosition pos = new FieldPosition(0); 
        saw = fmt.format(n, saw, pos);
        String pat = ((DecimalFormat)fmt).toPattern();
        if (saw.toString().equals(exp)) {
            logln("Ok   " + Double.toString(n) + " x " +
                  pat + " = \"" +
                  saw + "\"");
        } else {
            errln("FAIL " + Double.toString(n) + " x " +
                  pat + " = \"" +
                  saw + "\", expected \"" + exp + "\"");
        }    
    }
    
    /**
     * Upgrade to alphaWorks
     */
    public void TestExponent() {
        DecimalFormatSymbols US = new DecimalFormatSymbols(Locale.US);
        DecimalFormat fmt1 = new DecimalFormat("0.###E0", US);
        DecimalFormat fmt2 = new DecimalFormat("0.###E+0", US);
        int n = 1234;
        expect(fmt1, n, "1.234E3");
        expect(fmt2, n, "1.234E+3");
        expect(fmt1, "1.234E3", n);
        expect(fmt1, "1.234E+3", n); // Either format should parse "E+3"
        expect(fmt2, "1.234E+3", n);
    }
    
    /**
     * Upgrade to alphaWorks
     */
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
    
        expect(new DecimalFormat("#E0", US), 12345.0, "1.2345E4");
        expect(new DecimalFormat("0E0", US), 12345.0, "1E4");
    
        // pattern of NumberFormat.getScientificInstance(Locale.US) = "0.######E0" not "#E0"
        // so result = 1.234568E4 not 1.2345678901E4
        //when the pattern problem is finalized, delete comment mark'//' 
        //of the following code
        expect(NumberFormat.getScientificInstance(Locale.US), 12345.678901, "1.2345678901E4");
    
        expect(new DecimalFormat("##0.###E0", US), 12345.0, "12.34E3");
        expect(new DecimalFormat("##0.###E0", US), 12345.00001, "12.35E3");
        expect(new DecimalFormat("##0.####E0", US), 12345, "12.345E3");
    
        // pattern of NumberFormat.getScientificInstance(Locale.US) = "0.######E0" not "#E0"
        // so result = 1.234568E4 not 1.2345678901E4
        expect(NumberFormat.getScientificInstance(Locale.FRANCE), 12345.678901, "1,2345678901E4");
    
        expect(new DecimalFormat("##0.####E0", US), 789.12345e-9, "789.12E-9");
        expect(new DecimalFormat("##0.####E0", US), 780.e-9, "780E-9");
        expect(new DecimalFormat(".###E0", US), 45678.0, ".457E5");
        expect(new DecimalFormat(".###E0", US), 0, ".0E0");
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
        expect(new DecimalFormat("#E0", US), 45678000, "4.5678E7");
        expect(new DecimalFormat("##E0", US), 45678000, "45.678E6");
        expect(new DecimalFormat("####E0", US), 45678000, "4567.8E4");
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
        expect(new DecimalFormat("###E0", US), 0.0000123, "12.3E-6");
        expect(new DecimalFormat("###E0", US), 0.000123, "123E-6");
        expect(new DecimalFormat("###E0", US), 0.00123, "1.23E-3");
        expect(new DecimalFormat("###E0", US), 0.0123, "12.3E-3");
        expect(new DecimalFormat("###E0", US), 0.123, "123E-3");
        expect(new DecimalFormat("###E0", US), 1.23, "1.23E0");
        expect(new DecimalFormat("###E0", US), 12.3, "12.3E0");
        expect(new DecimalFormat("###E0", US), 123.0, "123E0");
        expect(new DecimalFormat("###E0", US), 1230.0, "1.23E3");
        /*
        expect(new DecimalFormat("0.#E+00", US, status),
               new Object[] { new Double(0.00012), "1.2E-04",
                              new Long(12000),     "1.2E+04",
                             });
        !
        ! Unroll this test into individual tests below...
        !
        */
        expect(new DecimalFormat("0.#E+00", US), 0.00012, "1.2E-04");
        expect(new DecimalFormat("0.#E+00", US), 12000, "1.2E+04");
    }
    
    /**
     * Upgrade to alphaWorks
     */
    public void TestPad() {
    
        DecimalFormatSymbols US = new DecimalFormatSymbols(Locale.US);
        expect(new DecimalFormat("*^##.##", US), 0, "^^^^0");
        expect(new DecimalFormat("*^##.##", US), -1.3, "^-1.3");
        expect(
            new DecimalFormat("##0.0####E0*_ g-m/s^2", US), 
            0, 
            "0.0E0______ g-m/s^2"); 
        expect(
            new DecimalFormat("##0.0####E0*_ g-m/s^2", US), 
            1.0 / 3, 
            "333.333E-3_ g-m/s^2"); 
        expect(new DecimalFormat("##0.0####*_ g-m/s^2", US), 0, "0.0______ g-m/s^2");
        expect(
            new DecimalFormat("##0.0####*_ g-m/s^2", US), 
            1.0 / 3, 
            "0.33333__ g-m/s^2"); 
    
        // Test padding before a sign
        final String formatStr = "*x#,###,###,##0.0#;*x(###,###,##0.0#)";
        expect(new DecimalFormat(formatStr, US), -10, "xxxxxxxxxx(10.0)");
        expect(new DecimalFormat(formatStr, US), -1000, "xxxxxxx(1,000.0)");
        expect(new DecimalFormat(formatStr, US), -1000000, "xxx(1,000,000.0)");
        expect(new DecimalFormat(formatStr, US), -100.37, "xxxxxxxx(100.37)");
        expect(new DecimalFormat(formatStr, US), -10456.37, "xxxxx(10,456.37)");
        expect(new DecimalFormat(formatStr, US), -1120456.37, "xx(1,120,456.37)");
        expect(new DecimalFormat(formatStr, US), -112045600.37, "(112,045,600.37)");
        expect(new DecimalFormat(formatStr, US), -1252045600.37, "(1,252,045,600.37)");
    
        expect(new DecimalFormat(formatStr, US), 10, "xxxxxxxxxxxx10.0");
        expect(new DecimalFormat(formatStr, US), 1000, "xxxxxxxxx1,000.0");
        expect(new DecimalFormat(formatStr, US), 1000000, "xxxxx1,000,000.0");
        expect(new DecimalFormat(formatStr, US), 100.37, "xxxxxxxxxx100.37");
        expect(new DecimalFormat(formatStr, US), 10456.37, "xxxxxxx10,456.37");
        expect(new DecimalFormat(formatStr, US), 1120456.37, "xxxx1,120,456.37");
        expect(new DecimalFormat(formatStr, US), 112045600.37, "xx112,045,600.37");
        expect(new DecimalFormat(formatStr, US), 10252045600.37, "10,252,045,600.37");
    
        // Test padding between a sign and a number
        final String formatStr2 = "#,###,###,##0.0#*x;(###,###,##0.0#*x)";
        expect(new DecimalFormat(formatStr2, US), -10, "(10.0xxxxxxxxxx)");
        expect(new DecimalFormat(formatStr2, US), -1000, "(1,000.0xxxxxxx)");
        expect(new DecimalFormat(formatStr2, US), -1000000, "(1,000,000.0xxx)");
        expect(new DecimalFormat(formatStr2, US), -100.37, "(100.37xxxxxxxx)");
        expect(new DecimalFormat(formatStr2, US), -10456.37, "(10,456.37xxxxx)");
        expect(new DecimalFormat(formatStr2, US), -1120456.37, "(1,120,456.37xx)");
        expect(new DecimalFormat(formatStr2, US), -112045600.37, "(112,045,600.37)");
        expect(new DecimalFormat(formatStr2, US), -1252045600.37, "(1,252,045,600.37)"); 
    
        expect(new DecimalFormat(formatStr2, US), 10, "10.0xxxxxxxxxxxx");
        expect(new DecimalFormat(formatStr2, US), 1000, "1,000.0xxxxxxxxx");
        expect(new DecimalFormat(formatStr2, US), 1000000, "1,000,000.0xxxxx");
        expect(new DecimalFormat(formatStr2, US), 100.37, "100.37xxxxxxxxxx");
        expect(new DecimalFormat(formatStr2, US), 10456.37, "10,456.37xxxxxxx");
        expect(new DecimalFormat(formatStr2, US), 1120456.37, "1,120,456.37xxxx");
        expect(new DecimalFormat(formatStr2, US), 112045600.37, "112,045,600.37xx");
        expect(new DecimalFormat(formatStr2, US), 10252045600.37, "10,252,045,600.37");
    
        //testing the setPadCharacter(UnicodeString) and getPadCharacterString()
        DecimalFormat fmt = new DecimalFormat("#", US);
        char padString = 'P';
        fmt.setPadCharacter(padString);
        expectPad(fmt, "*P##.##", DecimalFormat.PAD_BEFORE_PREFIX, 5, padString);
        fmt.setPadCharacter('^');
        expectPad(fmt, "*^#", DecimalFormat.PAD_BEFORE_PREFIX, 1, '^');
        //commented untill implementation is complete
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
    }
    
    /**
     * Upgrade to alphaWorks
     */
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
    
        fmt.setFormatWidth(16);
        //              12  34567890123456
        expectPat(fmt, "AA*^#,###,##0.00ZZ");
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
}

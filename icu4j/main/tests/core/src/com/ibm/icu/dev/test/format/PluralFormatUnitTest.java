/*
 *******************************************************************************
 * Copyright (C) 2007-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.TestFmwk;

import com.ibm.icu.text.*;
import com.ibm.icu.util.ULocale;

import java.text.ParsePosition;

/**
 * @author tschumann (Tim Schumann)
 *
 */
public class PluralFormatUnitTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new PluralFormatUnitTest().run(args);
    }

    public void TestConstructor() {
        // Test correct formatting of numbers.
        PluralFormat plFmts[] = new PluralFormat[8];
        plFmts[0] = new PluralFormat();
        plFmts[0].applyPattern("other{#}");
        plFmts[1] = new PluralFormat(PluralRules.DEFAULT);
        plFmts[1].applyPattern("other{#}");
        plFmts[2] = new PluralFormat(PluralRules.DEFAULT, "other{#}");
        plFmts[3] = new PluralFormat("other{#}");
        plFmts[4] = new PluralFormat(ULocale.getDefault());
        plFmts[4].applyPattern("other{#}");
        plFmts[5] = new PluralFormat(ULocale.getDefault(), PluralRules.DEFAULT);
        plFmts[5].applyPattern("other{#}");
        plFmts[6] = new PluralFormat(ULocale.getDefault(),
                                   PluralRules.DEFAULT,
                                   "other{#}");
        plFmts[7] = new PluralFormat(ULocale.getDefault(), "other{#}");

        // These plural formats should produce the same output as a
        // NumberFormat for the default locale.
        NumberFormat numberFmt = NumberFormat.getInstance(ULocale.getDefault());
        for (int n = 1; n < 13; n++) {
            String result = numberFmt.format(n);
            for (int k = 0; k < plFmts.length; ++k) {
                this.assertEquals("PluralFormat's output is not as expected",
                                  result, plFmts[k].format(n));
            }
        }
        // Test some bigger numbers.
        for (int n = 100; n < 113; n++) {
            String result = numberFmt.format(n*n);
            for (int k = 0; k < plFmts.length; ++k) {
                this.assertEquals("PluralFormat's output is not as expected",
                                  result, plFmts[k].format(n*n));
            }
        }
    }

    public void TestApplyPatternAndFormat() {
        // Create rules for testing.
        PluralRules oddAndEven =  PluralRules.createRules("odd: n mod 2 is 1");
        {
            // Test full specified case for testing RuleSet
            PluralFormat plfOddAndEven = new PluralFormat(oddAndEven);
            plfOddAndEven.applyPattern("odd{# is odd.} other{# is even.}");

            // Test fall back to other.
            PluralFormat plfOddOrEven = new PluralFormat(oddAndEven);
            plfOddOrEven.applyPattern("other{# is odd or even.}");

            NumberFormat numberFormat =
                NumberFormat.getInstance(ULocale.getDefault());
            for (int i = 0; i < 22; ++i) {
                assertEquals("Fallback to other gave wrong results",
                             numberFormat.format(i) + " is odd or even.",
                             plfOddOrEven.format(i));
                assertEquals("Fully specified PluralFormat gave wrong results",
                        numberFormat.format(i) + ((i%2 == 1) ?  " is odd."
                                                             :  " is even."),
                        plfOddAndEven.format(i));
            }

            // Check that double definition results in an exception.
            try {
                PluralFormat plFmt = new PluralFormat(oddAndEven);
                plFmt.applyPattern("odd{foo} odd{bar} other{foobar}");
                errln("Double definition of a plural case message should " +
                       "provoke an exception but did not.");
            }catch (IllegalArgumentException e){}
            try {
                PluralFormat plFmt = new PluralFormat(oddAndEven);
                plFmt.applyPattern("odd{foo} other{bar} other{foobar}");
                errln("Double definition of a plural case message should " +
                       "provoke an exception but did not.");
            }catch (IllegalArgumentException e){}
        }
        // omit other keyword.
        try {
            PluralFormat plFmt = new PluralFormat(oddAndEven);
            plFmt.applyPattern("odd{foo}");
            errln("Not defining plural case other should result in an " +
                    "exception but did not.");
        }catch (IllegalArgumentException e){}

        // Test unknown keyword.
        try {
            PluralFormat plFmt = new PluralFormat(oddAndEven);
            plFmt.applyPattern("otto{foo} other{bar}");
            errln("Defining a message for an unknown keyword should result in" +
                    "an exception but did not.");
        }catch (IllegalArgumentException e){}

        // Test invalid keyword.
        try {
            PluralFormat plFmt = new PluralFormat(oddAndEven);
            plFmt.applyPattern("1odd{foo} other{bar}");
            errln("Defining a message for an invalid keyword should result in" +
                    "an exception but did not.");
        }catch (IllegalArgumentException e){}

        // Test invalid syntax
        //   -- comma between keyword{message} clauses
        //   -- space in keywords
        //   -- keyword{message1}{message2}
        try {
            PluralFormat plFmt = new PluralFormat(oddAndEven);
            plFmt.applyPattern("odd{foo},other{bar}");
            errln("Separating keyword{message} items with other characters " +
                    "than space should provoke an exception but did not.");
        }catch (IllegalArgumentException e){}
        try {
            PluralFormat plFmt = new PluralFormat(oddAndEven);
            plFmt.applyPattern("od d{foo} other{bar}");
            errln("Spaces inside keywords should provoke an exception but " +
                    "did not.");
        }catch (IllegalArgumentException e){}
        try {
            PluralFormat plFmt = new PluralFormat(oddAndEven);
            plFmt.applyPattern("odd{foo}{foobar}other{foo}");
            errln("Defining multiple messages after a keyword should provoke " +
                    "an exception but did not.");
        }catch (IllegalArgumentException e){}

        // Check that nested format is preserved.
        {
            PluralFormat plFmt = new PluralFormat(oddAndEven);
            plFmt.applyPattern("odd{The number {0, number, #.#0} is odd.}" +
                               "other{The number {0, number, #.#0} is even.}");
            for (int i = 1; i < 3; ++i) {
                assertEquals("format did not preserve a nested format string.",
                              ((i % 2 == 1) ?
                                      "The number {0, number, #.#0} is odd."
                                    : "The number {0, number, #.#0} is even."),
                              plFmt.format(i));
            }

        }
        // Check that a pound sign in curly braces is preserved.
        {
            PluralFormat plFmt = new PluralFormat(oddAndEven);
            plFmt.applyPattern("odd{The number {#} is odd.}" +
                               "other{The number {#} is even.}");
            for (int i = 1; i < 3; ++i) {
                assertEquals("format did not preserve # inside curly braces.",
                              ((i % 2 == 1) ? "The number {#} is odd."
                                            : "The number {#} is even."),
                              plFmt.format(i));
            }

        }
    }

    public void TestSetLocale() {
        // Create rules for testing.
        PluralRules oddAndEven = PluralRules.createRules("odd__: n mod 2 is 1");

        PluralFormat plFmt = new PluralFormat(oddAndEven);
        plFmt.applyPattern("odd__{odd} other{even}");
        plFmt.setLocale(ULocale.ENGLISH);

        // Check that pattern gets deleted.
        NumberFormat nrFmt = NumberFormat.getInstance(ULocale.ENGLISH);
        assertEquals("pattern was not resetted by setLocale() call.",
                     nrFmt.format(5),
                     plFmt.format(5));

        // Check that rules got updated.
        try {
            plFmt.applyPattern("odd__{odd} other{even}");
            errln("SetLocale should reset rules but did not.");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().indexOf("Unknown keyword") < 0){
                errln("Wrong exception thrown");
            }
        }
        plFmt.applyPattern("one{one} other{not one}");
        for (int i = 0; i < 20; ++i) {
            assertEquals("Wrong ruleset loaded by setLocale()",
                         ((i==1) ? "one" : "not one"),
                         plFmt.format(i));
        }
    }

    public void TestParse() {
        PluralFormat plFmt = new PluralFormat("other{test}");
        try {
            plFmt.parse("test", new ParsePosition(0));
            errln("parse() should throw an UnsupportedOperationException but " +
                    "did not");
        } catch (UnsupportedOperationException e) {
        }

        plFmt = new PluralFormat("other{test}");
        try {
            plFmt.parseObject("test", new ParsePosition(0));
            errln("parse() should throw an UnsupportedOperationException but " +
                    "did not");
        } catch (UnsupportedOperationException e) {
        }
    }

    public void TestPattern() {
        Object[] args = { "acme", null };

        {
            PluralFormat pf = new PluralFormat("  one {one ''widget} other {# widgets}  ");
            String pat = pf.toPattern();
            logln("pf pattern: '" + pat + "'");

            assertEquals("no leading spaces", "o", pat.substring(0, 1));
            assertEquals("no trailing spaces", "}", pat.substring(pat.length() - 1));
        }

        MessageFormat pfmt = new MessageFormat("The disk ''{0}'' contains {1, plural,  one {one ''''{1, number, #.0}'''' widget} other {# widgets}}.");
        System.out.println();
        for (int i = 0; i < 3; ++i) {
            args[1] = new Integer(i);
            logln(pfmt.format(args));
        }
        PluralFormat pf = (PluralFormat)pfmt.getFormatsByArgumentIndex()[1];
        logln(pf.toPattern());
        logln(pfmt.toPattern());
        MessageFormat pfmt2 = new MessageFormat(pfmt.toPattern());
        assertEquals("message formats are equal", pfmt, pfmt2);
    }
}

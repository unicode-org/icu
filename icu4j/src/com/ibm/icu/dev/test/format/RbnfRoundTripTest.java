/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.dev.test.TestFmwk;

import java.util.Locale;

public class RbnfRoundTripTest extends TestFmwk {
    /**
     * Puts a copyright in the .class file
     */
    private static final String copyrightNotice
        = "Copyright \u00a91997-1998 IBM Corp.  All rights reserved.";

    public static void main(String[] args) {
        RbnfRoundTripTest test = new RbnfRoundTripTest();

        try {
            test.run(args);
        }
        catch (Throwable e) {
            System.out.println("Entire test failed because of exception: "
                            + e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Perform an exhaustive round-trip test on the English spellout rules
     */
    public void TestEnglishSpelloutRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(Locale.US,
                        RuleBasedNumberFormat.SPELLOUT);

        doTest(formatter, -12345678, 12345678);
    }

    /**
     * Perform an exhaustive round-trip test on the duration-formatting rules
     */
    public void TestDurationsRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(Locale.US,
                        RuleBasedNumberFormat.DURATION);

        doTest(formatter, 0, 12345678);
    }

    /**
     * Perform an exhaustive round-trip test on the Spanish spellout rules
     */
    public void TestSpanishSpelloutRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(new Locale("es", "es",
                        ""), RuleBasedNumberFormat.SPELLOUT);

        doTest(formatter, -12345678, 12345678);
    }

    /**
     * Perform an exhaustive round-trip test on the French spellout rules
     */
    public void TestFrenchSpelloutRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(Locale.FRANCE,
                        RuleBasedNumberFormat.SPELLOUT);

        doTest(formatter, -12345678, 12345678);
    }

    /**
     * Perform an exhaustive round-trip test on the Swiss French spellout rules
     */
    public void TestSwissFrenchSpelloutRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(new Locale("fr", "CH",
                        ""), RuleBasedNumberFormat.SPELLOUT);

        doTest(formatter, -12345678, 12345678);
    }

    /**
     * Perform an exhaustive round-trip test on the Italian spellout rules
     */
    public void TestItalianSpelloutRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(Locale.ITALIAN,
                        RuleBasedNumberFormat.SPELLOUT);

        doTest(formatter, -999999, 999999);
    }

    /**
     * Perform an exhaustive round-trip test on the German spellout rules
     */
    public void TestGermanSpelloutRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(Locale.GERMANY,
                        RuleBasedNumberFormat.SPELLOUT);

        doTest(formatter, 0, 12345678);
    }

    /**
     * Perform an exhaustive round-trip test on the Swedish spellout rules
     */
    public void TestSwedishSpelloutRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(new Locale("sv", "SE",
                        ""), RuleBasedNumberFormat.SPELLOUT);

        doTest(formatter, 0, 12345678);
    }

    /**
     * Perform an exhaustive round-trip test on the Dutch spellout rules
     */
    public void TestDutchSpelloutRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(new Locale("nl", "NL",
                        ""), RuleBasedNumberFormat.SPELLOUT);

        doTest(formatter, -12345678, 12345678);
    }

    /**
     * Perform an exhaustive round-trip test on the Japanese spellout rules
     */
    public void TestJapaneseSpelloutRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(Locale.JAPAN,
                        RuleBasedNumberFormat.SPELLOUT);

        doTest(formatter, 0, 12345678);
    }

    /**
     * Perform an exhaustive round-trip test on the Russian spellout rules
     */
    public void TestRussianSpelloutRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(new Locale("ru", "RU",
                        ""), RuleBasedNumberFormat.SPELLOUT);

        doTest(formatter, 0, 12345678);
    }

    /**
     * Perform an exhaustive round-trip test on the Greek spellout rules
     */
    public void TestGreekSpelloutRT() {
        RuleBasedNumberFormat formatter
                        = new RuleBasedNumberFormat(new Locale("el", "GR",
                        ""), RuleBasedNumberFormat.SPELLOUT);

        doTest(formatter, 0, 12345678);
    }

    void doTest(RuleBasedNumberFormat formatter,  long lowLimit,
                    long highLimit) {
        try {
            long count = 0;
            long increment = 1;
            for (long i = lowLimit; i <= highLimit; i += increment) {
                if (count % 1000 == 0)
                    logln(Long.toString(i));

                if (Math.abs(i) < 5000)
                    increment = 1;
                else if (Math.abs(i) < 500000)
                    increment = 2737;
                else
                    increment = 267437;

                String text = formatter.format(i);
                long rt = formatter.parse(text).longValue();

                if (rt != i) {
                    errln("Round-trip failed: " + i + " -> " + text +
                                    " -> " + rt);
                }

                ++count;
            }

            if (lowLimit < 0) {
                double d = 1.234;
                while (d < 1000) {
                    String text = formatter.format(d);
                    double rt = formatter.parse(text).doubleValue();

                    if (rt != d) {
                        errln("Round-trip failed: " + d + " -> " + text +
                                        " -> " + rt);
                    }
                    d *= 10;
                }
            }
        }
        catch (Throwable e) {
            errln("Test failed with exception: " + e.toString());
            e.printStackTrace();
        }
    }
}


// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2004-2013, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.text.RuleBasedNumberFormat;
import com.ibm.icu.util.ULocale;
import java.text.ParseException;
import java.util.Locale;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RBNFParseTest extends CoreTestFmwk {
    @Test
    public void TestParse() {

        // these rules make no sense but behave rationally
        String[] okrules = {
            "random text",
            "%foo:bar",
            "%foo: bar",
            "0:",
            "0::",
            "%%foo:;",
            "-",
            "-1",
            "-:",
            ".",
            ".1",
            "[",
            "]",
            "[]",
            "[foo]",
            "[[]",
            "[]]",
            "[[]]",
            "[][]",
            "<",
            ">",
            "=",
            "==",
            "===",
            "=foo=",
        };

        String[] exceptrules = {
            "",
            ";",
            ";;",
            ":",
            "::",
            ":1",
            ":;",
            ":;:;",
            "<<",
            "<<<",
            "10:;9:;",
            ">>",
            ">>>",
            "10:", // formatting any value with a one's digit will fail
            "11: << x", // formatting a multiple of 10 causes rollback rule to fail
            "%%foo: 0 foo; 10: =%%bar=; %%bar: 0: bar; 10: =%%foo=;",
        };

        String[][] allrules = {
            okrules, exceptrules,
        };

        for (int j = 0; j < allrules.length; ++j) {
            String[] tests = allrules[j];
            boolean except = tests == exceptrules;
            for (int i = 0; i < tests.length; ++i) {
                logln("----------");
                logln("rules: '" + tests[i] + "'");
                boolean caughtException = false;
                try {
                    RuleBasedNumberFormat fmt = new RuleBasedNumberFormat(tests[i], Locale.US);
                    logln("1.23: " + fmt.format(20));
                    logln("-123: " + fmt.format(-123));
                    logln(".123: " + fmt.format(.123));
                    logln(" 123: " + fmt.format(123));
                } catch (Exception e) {
                    if (!except) {
                        errln("Unexpected exception: " + e.getMessage());
                    } else {
                        caughtException = true;
                    }
                }
                if (except && !caughtException) {
                    errln("expected exception but didn't get one!");
                }
            }
        }
    }

    private void parseFormat(RuleBasedNumberFormat rbnf, String s, String target) {
        try {
            Number n = rbnf.parse(s);
            String t = rbnf.format(n);
            assertEquals(rbnf.getLocale(ULocale.ACTUAL_LOCALE) + ": " + s + " : " + n, target, t);
        } catch (java.text.ParseException e) {
            fail("exception:" + e);
        }
    }

    private void parseList(
            RuleBasedNumberFormat rbnf_en, RuleBasedNumberFormat rbnf_fr, String[][] lists) {
        for (int i = 0; i < lists.length; ++i) {
            String[] list = lists[i];
            String s = list[0];
            String target_en = list[1];
            String target_fr = list[2];

            parseFormat(rbnf_en, s, target_en);
            parseFormat(rbnf_fr, s, target_fr);
        }
    }

    @Test
    @Ignore("https://unicode-org.atlassian.net/browse/ICU-13793")
    public void TestLenientParse() throws Exception {
        RuleBasedNumberFormat rbnf_en, rbnf_fr;

        // TODO: this still passes, but setLenientParseMode should have no effect now.
        // Did it ever test what it was supposed to?
        rbnf_en = new RuleBasedNumberFormat(Locale.ENGLISH, RuleBasedNumberFormat.SPELLOUT);
        rbnf_en.setLenientParseMode(true);
        rbnf_fr = new RuleBasedNumberFormat(Locale.FRENCH, RuleBasedNumberFormat.SPELLOUT);
        rbnf_fr.setLenientParseMode(true);

        Number n = rbnf_en.parse("1,2 million");
        logln(n.toString());

        String[][] lists = {
            {"1,2", "one", "un virgule deux"},
            {"1,2 million", "one", "un virgule deux"},
            {"1,2 millions", "one", "un million deux cent mille"},
            {"1.2", "one point two", "un"},
            {"1.2 million", "one million two hundred thousand", "un"},
            {"1.2 millions", "one million two hundred thousand", "un"},
        };

        Locale.setDefault(Locale.FRANCE);
        logln("Default locale:" + Locale.getDefault());
        logln("rbnf_en:" + rbnf_en.getDefaultRuleSetName());
        logln("rbnf_fr:" + rbnf_en.getDefaultRuleSetName());
        parseList(rbnf_en, rbnf_fr, lists);

        Locale.setDefault(Locale.US);
        logln("Default locale:" + Locale.getDefault());
        logln("rbnf_en:" + rbnf_en.getDefaultRuleSetName());
        logln("rbnf_fr:" + rbnf_en.getDefaultRuleSetName());
        parseList(rbnf_en, rbnf_fr, lists);
    }

    @Test
    public void TestParseRuleDescriptorOverflow23002() {
        try {
            RuleBasedNumberFormat rbnf =
                    new RuleBasedNumberFormat(
                            "0110110/300113001103000113001103000110i/3013033:", new Locale("as"));
        } catch (IllegalArgumentException e) {
            return;
        }
        errln("expected exception but didn't get one!");
    }

    @Test
    public void TestBadParse() {
        RuleBasedNumberFormat rbnf =
                new RuleBasedNumberFormat(Locale.JAPAN, RuleBasedNumberFormat.SPELLOUT);
        String[] testData = {
            "・・・・・・・・・・・・・・・・・・・・・・・・",
        };

        for (String testString : testData) {
            try {
                rbnf.parse(testString);
                errln("Unexpected success: " + testString);
            } catch (ParseException e) {
                // success!
            }
        }
    }

    @Test
    public void Test23184EmptyRuleSet() {
        try {
            RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat("x00:>%>>;%;<0<<", Locale.US);
            errln("Failed: should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // success!
        }
        try {
            RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat("x00:>%>>;%:;<0<<", Locale.US);
            errln("Failed: should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // success!
        }
    }

    @Test
    public void TestNullRuleSet() {
        try {
            RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat("x00:a>>>b>#>", Locale.US);
            errln("Failed: should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // success!
        }
    }

    @Test
    public void TestDividedByZero() {
        try {
            RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat("7060920374060940374/4:[]");
            errln("Failed: should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // success!
        }
    }

    @Test
    public void TestICU23144() {
        // Construct a chained 75-depth ModulusSubstitution ('>>>') RBNF RuleSet.
        // Verifies that the internal delegation counter accurately increments at each >>> step,
        // cleanly tripping the 'RECURSION_LIMIT' guard condition without thread-stack exhaustion.
        StringBuilder ruleDef = new StringBuilder("%infinite-recursion:\n0: ;\n");
        for (int i = 1; i <= 75; ++i) {
            ruleDef.append(i).append(": >>>;\n");
        }

        try {
            RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(ruleDef.toString(), Locale.US);
            // In Java, exceeding RECURSION_LIMIT inside parse() evaluates to zero/unparsed,
            // producing a standard ParseException for a malformed parse traversal.
            rbnf.parse("75");
        } catch (java.text.ParseException expected) {
            // Success! RECURSION_LIMIT correctly terminated the execution tree before
            // Stack-Overflow.
            logln(
                    "TestICU23144 (Java): Successfully survived 75-depth >>> recursion chain via ParseException.");
        } catch (Exception e) {
            errln("TestICU23144 (Java) failed with unexpected Exception: " + e.getMessage());
        }
    }

    @Test
    public void TestICU23591() {
        StringBuilder fuzzBuf =
                new StringBuilder(
                        "922E1<\u8686\u8686\u8686\u8686\u8686\u8686\u4400\u00FF\u0000;=>>=>>\u0001>>");
        for (int i = 0; i < 132; ++i) {
            fuzzBuf.append(";>>>");
        }
        fuzzBuf.append(
                ";>>\u0008\u0000\u0000\u3132\u3734\u3834\u3633\u3834\u3000\u3C00\u0000\u3B44\u3C00"
                        + "\u6300\u7275\u6572\u636E\u4D79\u7461\u68630<\u4400;<\u3B30\u3C00\u3000"
                        + "\u3C00\uC6C6\uC6C6\uC6C6\uC6C6\uC6C6\u0100D\u3B40");
        String fuzzStr = fuzzBuf.toString();
        try {
            RuleBasedNumberFormat rbnf = new RuleBasedNumberFormat(fuzzStr, new ULocale("ba"));
            rbnf.parse(fuzzStr);
            errln("Expected exception when parsing fuzzStr");
        } catch (IllegalArgumentException | IllegalStateException | ParseException expected) {
            // Expected clean rejection without hang or stack overflow.
        }

        StringBuilder chainRules = new StringBuilder("0: z;\n");
        for (int i = 1; i <= 7000; ++i) {
            chainRules.append(i).append(": >>>;\n");
        }
        RuleBasedNumberFormat chainRbnf =
                new RuleBasedNumberFormat(chainRules.toString(), ULocale.US);
        try {
            chainRbnf.parse("a");
            errln("Expected ParseException when parsing 7000-rule >>> chain");
        } catch (ParseException expected) {
            // Expected.
        }

        RuleBasedNumberFormat selfRef =
                new RuleBasedNumberFormat("%s:\n0: x;\n1: a=%s=;", ULocale.US);
        try {
            selfRef.parse("ax");
            errln("Expected ParseException for self-referential SameValueSubstitution");
        } catch (ParseException expected) {
            // Expected.
        }
    }
}

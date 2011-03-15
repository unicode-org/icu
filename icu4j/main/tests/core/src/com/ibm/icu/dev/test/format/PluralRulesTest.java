/*
 *******************************************************************************
 * Copyright (C) 2007-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;

/**
 * @author dougfelt (Doug Felt)
 */
public class PluralRulesTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new PluralRulesTest().run(args);
    }

    private static final String[] parseTestData = {
        "a: n is 1", "a:1",
        "a: n mod 10 is 2", "a:2,12,22",
        "a: n is not 1", "a:0,2,3,4,5",
        "a: n mod 3 is not 1", "a:0,2,3,5,6,8,9",
        "a: n in 2..5", "a:2,3,4,5",
        "a: n within 2..5", "a:2,3,4,5",
        "a: n not in 2..5", "a:0,1,6,7,8",
        "a: n not within 2..5", "a:0,1,6,7,8",
        "a: n mod 10 in 2..5", "a:2,3,4,5,12,13,14,15,22,23,24,25",
        "a: n mod 10 within 2..5", "a:2,3,4,5,12,13,14,15,22,23,24,25",
        "a: n mod 10 is 2 and n is not 12", "a:2,22,32,42",
        "a: n mod 10 in 2..3 or n mod 10 is 5", "a:2,3,5,12,13,15,22,23,25",
        "a: n mod 10 within 2..3 or n mod 10 is 5", "a:2,3,5,12,13,15,22,23,25",
        "a: n is 1 or n is 4 or n is 23", "a:1,4,23",
        "a: n mod 2 is 1 and n is not 3 and n in 1..11", "a:1,5,7,9,11",
        "a: n mod 2 is 1 and n is not 3 and n within 1..11", "a:1,5,7,9,11",
        "a: n mod 2 is 1 or n mod 5 is 1 and n is not 6", "a:1,3,5,7,9,11,13,15,16",
        "a: n in 2..5; b: n in 5..8; c: n mod 2 is 1", "a:2,3,4,5;b:6,7,8;c:1,9,11",
        "a: n within 2..5; b: n within 5..8; c: n mod 2 is 1", "a:2,3,4,5;b:6,7,8;c:1,9,11",
    };

    private String[] getTargetStrings(String targets) {
        List list = new ArrayList(50);
        String[] valSets = Utility.split(targets, ';');
        for (int i = 0; i < valSets.length; ++i) {
            String[] temp = Utility.split(valSets[i], ':');
            String key = temp[0].trim();
            String[] vals = Utility.split(temp[1], ',');
            for (int j = 0; j < vals.length; ++j) {
                String valString = vals[j].trim();
                int val = Integer.parseInt(valString);
                while (list.size() <= val) {
                    list.add(null);
                }
                if (list.get(val) != null) {
                    fail("key: " + list.get(val) + " already set for: " + val);
                }
                list.set(val, key);
            }
        }

        String[] result = (String[]) list.toArray(new String[list.size()]);
        for (int i = 0; i < result.length; ++i) {
            if (result[i] == null) {
                result[i] = "other";
            }
        }
        return result;
    }

    private void checkTargets(PluralRules rules, String[] targets) {
        for (int i = 0; i < targets.length; ++i) {
            assertEquals("value " + i, targets[i], rules.select(i));
        }
    }

    public void testParseEmpty() throws ParseException {
        PluralRules rules = PluralRules.parseDescription("a:n");
        assertEquals("empty", "a", rules.select(0));
    }

    public void testParsing() {
        for (int i = 0; i < parseTestData.length; i += 2) {
            String pattern = parseTestData[i];
            String expected = parseTestData[i + 1];

            logln("pattern[" + i + "] " + pattern);
            try {
                PluralRules rules = PluralRules.createRules(pattern);
                String[] targets = getTargetStrings(expected);
                checkTargets(rules, targets);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    private static String[][] equalityTestData = {
        { "a: n is 5",
          "a: n in 2..6 and n not in 2..4 and n is not 6" },
        { "a: n in 2..3",
          "a: n is 2 or n is 3",
          "a: n is 3 and n in 2..5 or n is 2" },
        { "a: n is 12; b:n mod 10 in 2..3",
          "b: n mod 10 in 2..3 and n is not 12; a: n in 12..12",
          "b: n is 13; a: n is 12; b: n mod 10 is 2 or n mod 10 is 3" },
    };

    private static String[][] inequalityTestData = {
        { "a: n mod 8 is 3",
          "a: n mod 7 is 3"
        },
        { "a: n mod 3 is 2 and n is not 5",
          "a: n mod 6 is 2 or n is 8 or n is 11"
        }
    };

    private void compareEquality(String id, Object[] objects, boolean shouldBeEqual) {
        for (int i = 0; i < objects.length; ++i) {
            Object lhs = objects[i];
            int start = shouldBeEqual ? i : i + 1;
            for (int j = start; j < objects.length; ++j) {
                Object rhs = objects[j];
                if (shouldBeEqual != lhs.equals(rhs)) {
                    String msg = shouldBeEqual ? "should be equal" : "should not be equal";
                    fail(id + " " + msg + " (" + i + ", " + j + "):\n    " + lhs + "\n    " + rhs);
                }
                // assertEquals("obj " + i + " and " + j, lhs, rhs);
            }
        }
    }

    private void compareEqualityTestSets(String[][] sets, boolean shouldBeEqual) {
        for (int i = 0; i < sets.length; ++i) {
            String[] patterns = sets[i];
            PluralRules[] rules = new PluralRules[patterns.length];
            for (int j = 0; j < patterns.length; ++j) {
                rules[j] = PluralRules.createRules(patterns[j]);
            }
            compareEquality("test " + i, rules, shouldBeEqual);
        }
    }

    public void testEquality() {
        compareEqualityTestSets(equalityTestData, true);
    }

    public void testInequality() {
        compareEqualityTestSets(inequalityTestData, false);
    }

    public void testBuiltInRules() {
        // spot check
        PluralRules rules = PluralRules.forLocale(ULocale.US);
        assertEquals("us 0", PluralRules.KEYWORD_OTHER, rules.select(0));
        assertEquals("us 1", PluralRules.KEYWORD_ONE, rules.select(1));
        assertEquals("us 2", PluralRules.KEYWORD_OTHER, rules.select(2));

        rules = PluralRules.forLocale(ULocale.JAPAN);
        assertEquals("ja 0", PluralRules.KEYWORD_OTHER, rules.select(0));
        assertEquals("ja 1", PluralRules.KEYWORD_OTHER, rules.select(1));
        assertEquals("ja 2", PluralRules.KEYWORD_OTHER, rules.select(2));

        rules = PluralRules.forLocale(ULocale.createCanonical("ru"));
        assertEquals("ru 0", PluralRules.KEYWORD_MANY, rules.select(0));
        assertEquals("ru 1", PluralRules.KEYWORD_ONE, rules.select(1));
        assertEquals("ru 2", PluralRules.KEYWORD_FEW, rules.select(2));
    }

    public void testFunctionalEquivalent() {
        // spot check
        ULocale unknown = ULocale.createCanonical("zz_ZZ");
        ULocale un_equiv = PluralRules.getFunctionalEquivalent(unknown, null);
        assertEquals("unknown locales have root", ULocale.ROOT, un_equiv);

        ULocale jp_equiv = PluralRules.getFunctionalEquivalent(ULocale.JAPAN, null);
        ULocale cn_equiv = PluralRules.getFunctionalEquivalent(ULocale.CHINA, null);
        assertEquals("japan and china equivalent locales", jp_equiv, cn_equiv);

        boolean[] available = new boolean[1];
        ULocale russia = ULocale.createCanonical("ru_RU");
        ULocale ru_ru_equiv = PluralRules.getFunctionalEquivalent(russia, available);
        assertFalse("ru_RU not listed", available[0]);

        ULocale russian = ULocale.createCanonical("ru");
        ULocale ru_equiv = PluralRules.getFunctionalEquivalent(russian, available);
        assertTrue("ru listed", available[0]);
        assertEquals("ru and ru_RU equivalent locales", ru_ru_equiv, ru_equiv);
    }

    public void testAvailableULocales() {
        ULocale[] locales = PluralRules.getAvailableULocales();
        Set localeSet = new HashSet();
        localeSet.addAll(Arrays.asList(locales));

        assertEquals("locales are unique in list", locales.length, localeSet.size());
    }

    /*
     * Test the method public static PluralRules parseDescription(String description)
     */
    public void TestParseDescription() {
        try {
            if (PluralRules.DEFAULT != PluralRules.parseDescription("")) {
                errln("PluralRules.parseDescription(String) was suppose "
                        + "to return PluralRules.DEFAULT when String is of " + "length 0.");
            }
        } catch (ParseException e) {
            errln("PluralRules.parseDescription(String) was not suppose " + "to return an exception.");
        }
    }

    /*
     * Tests the method public static PluralRules createRules(String description)
     */
    public void TestCreateRules() {
        try {
            if (PluralRules.createRules(null) != null) {
                errln("PluralRules.createRules(String) was suppose to "
                        + "return null for an invalid String descrtiption.");
            }
        } catch (Exception e) {
        }
    }

    /*
     * Tests the method public int hashCode()
     */
    public void TestHashCode() {
// Bad test, breaks whenever PluralRules implementation changes.
//        PluralRules pr = PluralRules.DEFAULT;
//        if (106069776 != pr.hashCode()) {
//            errln("PluralRules.hashCode() was suppose to return 106069776 " + "when PluralRules.DEFAULT.");
//        }
    }

    /*
     * Tests the method public boolean equals(PluralRules rhs)
     */
    public void TestEquals() {
        PluralRules pr = PluralRules.DEFAULT;

        if (pr.equals((PluralRules) null)) {
            errln("PluralRules.equals(PluralRules) was supposed to return false " + "when passing null.");
        }
    }

    private void assertRuleValue(String rule, double value) {
        assertRuleKeyValue("a:" + rule, "a", value);
    }

    private void assertRuleKeyValue(String rule, String key, double value) {
        PluralRules pr = PluralRules.createRules(rule);
        assertEquals(rule, value, pr.getUniqueKeywordValue(key));
    }

    /*
     * Tests getUniqueKeywordValue()
     */
    public void TestGetUniqueKeywordValue() {
        assertRuleValue("n is 1", 1);
        assertRuleValue("n in 2..2", 2);
        assertRuleValue("n within 2..2", 2);
        assertRuleValue("n in 3..4", PluralRules.NO_UNIQUE_VALUE);
        assertRuleValue("n within 3..4", PluralRules.NO_UNIQUE_VALUE);
        assertRuleValue("n is 2 or n is 2", 2);
        assertRuleValue("n is 2 and n is 2", 2);
        assertRuleValue("n is 2 or n is 3", PluralRules.NO_UNIQUE_VALUE);
        assertRuleValue("n is 2 and n is 3", PluralRules.NO_UNIQUE_VALUE);
        assertRuleValue("n is 2 or n in 2..3", PluralRules.NO_UNIQUE_VALUE);
        assertRuleValue("n is 2 and n in 2..3", 2);
        assertRuleKeyValue("a: n is 1", "not_defined", PluralRules.NO_UNIQUE_VALUE); // key not defined
        assertRuleKeyValue("a: n is 1", "other", PluralRules.NO_UNIQUE_VALUE); // key matches default rule
    }

    /**
     * The version in PluralFormatUnitTest is not really a test, and it's in the wrong place
     * anyway, so I'm putting a variant of it here.
     */
    public void TestGetSamples() {
        Set<ULocale> uniqueRuleSet = new HashSet<ULocale>();
        for (ULocale locale : PluralRules.getAvailableULocales()) {
           uniqueRuleSet.add(PluralRules.getFunctionalEquivalent(locale, null));
        }
        for (ULocale locale : uniqueRuleSet) {
            PluralRules rules = PluralRules.forLocale(locale);
            logln("\nlocale: " + (locale == ULocale.ROOT ? "root" : locale.toString()) + ", rules: " + rules);
            Set<String> keywords = rules.getKeywords();
            for (String keyword : keywords) {
                Collection<Double> list = rules.getSamples(keyword);
                logln("keyword: " + keyword + ", samples: " + list);

                assertNotNull("list is not null", list);
                if (list != null) {
                    assertTrue("list is not empty", !list.isEmpty());

                    for (double value : list) {
                        assertEquals("value " + value + " matches keyword", keyword, rules.select(value));
                    }
                }
            }

            assertNull("list is null", rules.getSamples("@#$%^&*"));
        }
    }

    /**
     * Returns the empty set if the keyword is not defined, null if there are an unlimited
     * number of values for the keyword, or the set of values that trigger the keyword.
     */
     public void TestGetAllKeywordValues() {
         // data is pairs of strings, the rule, and the expected values as arguments
         String[] data = {
             "a: n in 2..5", "a: 2,3,4,5; other: null; b:",
             "a: n not in 2..5", "a: null; other: null",
             "a: n within 2..5", "a: null; other: null",
             "a: n not within 2..5", "a: null; other: null",
             "a: n in 2..5 or n within 6..8", "a: null", // ignore 'other' here on out, always null
             "a: n in 2..5 and n within 6..8", "a:",
             "a: n in 2..5 and n within 5..8", "a: 5",
             "a: n within 2..5 and n within 6..8", "a:", // our sampling catches these
             "a: n within 2..5 and n within 5..8", "a: 5", // ''
             "a: n within 1..2 and n within 2..3 or n within 3..4 and n within 4..5", "a: 2,4",
             "a: n within 1..2 and n within 2..3 or n within 3..4 and n within 4..5 " +
               "or n within 5..6 and n within 6..7", "a: null", // but not this...
             "a: n mod 3 is 0", "a: null",
             "a: n mod 3 is 0 and n within 1..2", "a:",
             "a: n mod 3 is 0 and n within 0..5", "a: 0,3",
             "a: n mod 3 is 0 and n within 0..6", "a: null", // similarly with mod, we don't catch...
             "a: n mod 3 is 0 and n in 3..12", "a: 3,6,9,12",
         };
         for (int i = 0; i < data.length; i += 2) {
             String ruleDescription = data[i];
             String result = data[i+1];

             PluralRules p = PluralRules.createRules(ruleDescription);
             for (String ruleResult : result.split(";")) {
                 String[] ruleAndValues = ruleResult.split(":");
                 String keyword = ruleAndValues[0].trim();
                 String valueList = ruleAndValues.length < 2 ? null : ruleAndValues[1];
                 if (valueList != null) {
                     valueList = valueList.trim();
                 }
                 Collection<Double> values;
                 if (valueList == null || valueList.length() == 0) {
                     values = Collections.<Double>emptyList();
                 } else if ("null".equals(valueList)) {
                     values = null;
                 } else {
                     values = new ArrayList<Double>();
                     for (String value : valueList.split(",")) {
                         values.add(Double.parseDouble(value));
                     }
                 }

                 Collection<Double> results = p.getAllKeywordValues(keyword);
                 assertEquals("keyword '" + keyword + "'", values, results);

                 if (results != null) {
                     try {
                         results.add(PluralRules.NO_UNIQUE_VALUE);
                         fail("returned set is modifiable");
                     } catch (UnsupportedOperationException e) {
                         // pass
                     }
                 }
             }
         }
     }
}

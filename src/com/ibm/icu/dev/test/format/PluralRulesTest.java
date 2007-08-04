/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.PluralRules;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author dougfelt (Doug Felt)
 *
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
        "a: n not in 2..5", "a:0,1,6,7,8",
        "a: n mod 10 in 2..5", "a:2,3,4,5,12,13,14,15,22,23,24,25",
        "a: n mod 10 is 2 and n is not 12", "a:2,22,32,42",
        "a: n mod 10 in 2..3 or n mod 10 is 5", "a:2,3,5,12,13,15,22,23,25",
        "a: n is 1 or n is 4 or n is 23", "a:1,4,23",
        "a: n mod 2 is 1 and n is not 3 and n in 1..11", "a:1,5,7,9,11",
        "a: n mod 2 is 1 or n mod 5 is 1 and n is not 6", "a:1,3,5,7,9,11,13,15,16",
        "a: n in 2..5; b: n in 5..8; c: n mod 2 is 1", "a:2,3,4,5;b:6,7,8;c:1,9,11",
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
        { "a: n in 2..3", 
            "a: n is 2 or n is 3", 
            "a:n is 3 and n in 2..5 or n is 2" },
        { "a: n is 12; b:n mod 10 in 2..3",
          "b: n mod 10 in 2..3 and n is not 12; a: n in 12..12",
          "b: n is 13; a: n in 12..13; b: n mod 10 is 2 or n mod 10 is 3" },
    };

    private void compareEquality(Object[] objects) {
        for (int i = 0; i < objects.length; ++i) {
            Object lhs = objects[i];
            for (int j = i; j < objects.length; ++j) {
                Object rhs = objects[j];
                assertEquals("obj " + i + " and " + j, lhs, rhs);
            }
        }
    }

    public void testEquality() {
        for (int i = 0; i < equalityTestData.length; ++i) {
            String[] patterns = equalityTestData[i];
            PluralRules[] rules = new PluralRules[patterns.length];
            for (int j = 0; j < patterns.length; ++j) {
                rules[j] = PluralRules.createRules(patterns[j]);
            }
            compareEquality(rules);
        }
    }
}

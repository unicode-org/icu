// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/
package com.ibm.icu.dev.test.util;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.TextTrieMap;
import com.ibm.icu.text.UnicodeSet;

@RunWith(JUnit4.class)
public class TextTrieMapTest extends TestFmwk {

    private static final Integer SUN = new Integer(1);
    private static final Integer MON = new Integer(2);
    private static final Integer TUE = new Integer(3);
    private static final Integer WED = new Integer(4);
    private static final Integer THU = new Integer(5);
    private static final Integer FRI = new Integer(6);
    private static final Integer SAT = new Integer(7);

    private static final Integer SUP1 = new Integer(8);
    private static final Integer SUP2 = new Integer(9);
    private static final Integer SUP3 = new Integer(10);
    private static final Integer SUP4 = new Integer(11);
    private static final Integer SUP5 = new Integer(12);

    private static final Integer FOO = new Integer(-1);
    private static final Integer BAR = new Integer(-2);

    private static final Object[][] TESTDATA = {
        {"Sunday", SUN},
        {"Monday", MON},
        {"Tuesday", TUE},
        {"Wednesday", WED},
        {"Thursday", THU},
        {"Friday", FRI},
        {"Saturday", SAT},
        {"Sun", SUN},
        {"Mon", MON},
        {"Tue", TUE},
        {"Wed", WED},
        {"Thu", THU},
        {"Fri", FRI},
        {"Sat", SAT},
        {"S", SUN},
        {"M", MON},
        {"T", TUE},
        {"W", WED},
        {"T", THU},
        {"F", FRI},
        {"S", SAT},
        {"L📺", SUP1}, // L, 0xD83D, 0xDCFA
        {"L📺1", SUP2}, // L, 0xD83D, 0xDCFA, 1
        {"L📻", SUP3}, // L, 0xD83D, 0xDCFB
        {"L🃏", SUP4}, // L, 0xD83C, 0xDCCF
        {"📺", SUP5}, // 0xD83D, 0xDCFA
        {"📻", SUP5}, // 0xD83D, 0xDCFB
        {"🃏", SUP5}, // 0xD83C, 0xDCCF
    };

    private static final Object[][] TESTCASES = {
        {"Sunday", SUN, SUN},
        {"sunday", null, SUN},
        {"Mo", MON, MON},
        {"mo", null, MON},
        {"Thursday Friday", THU, THU},
        {"T", new Object[]{TUE, THU}, new Object[]{TUE, THU}},
        {"TEST", new Object[]{TUE, THU}, new Object[]{TUE, THU}},
        {"SUN", new Object[]{SUN, SAT}, SUN},
        {"super", null, SUN},
        {"NO", null, null},
        {"L📺", SUP1, SUP1},
        {"l📺", null, SUP1},
    };

    @Test
    public void TestCaseSensitive() {
        Iterator itr = null;
        TextTrieMap map = new TextTrieMap(false);
        for (int i = 0; i < TESTDATA.length; i++) {
            map.put((String)TESTDATA[i][0], TESTDATA[i][1]);
        }

        logln("Test for get(String)");
        for (int i = 0; i < TESTCASES.length; i++) {
            itr = map.get((String)TESTCASES[i][0]);
            checkResult("get(String) case " + i, itr, TESTCASES[i][1]);
        }

        logln("Test for get(String, int)");
        StringBuffer textBuf = new StringBuffer();
        for (int i = 0; i < TESTCASES.length; i++) {
            textBuf.setLength(0);
            for (int j = 0; j < i; j++) {
                textBuf.append('X');
            }
            textBuf.append(TESTCASES[i][0]);
            itr = map.get(textBuf.toString(), i);
            checkResult("get(String, int) case " + i, itr, TESTCASES[i][1]);
        }

        logln("Test for partial match");
        for (Object[] cas : TESTDATA) {
            String str = (String) cas[0];
            for (int i = 0; i < str.length() - 1; i++) {
                TextTrieMap.Output output = new TextTrieMap.Output();
                map.get(str.substring(0, i), 0, output);
                assertTrue("Partial string means partial match", output.partialMatch);
            }
            String bad = str + "x";
            TextTrieMap.Output output = new TextTrieMap.Output();
            map.get(bad, 0, output);
            assertFalse("No partial match on bad string", output.partialMatch);
        }
        TextTrieMap.Output output = new TextTrieMap.Output();
        map.get("Sunday", 0, output);
        assertFalse("No partial match on string with no continuation", output.partialMatch);

        logln("Test for LeadCodePoints");
        // Note: The 📺 and 📻 have the same lead surrogate
        UnicodeSet expectedLeadCodePoints = new UnicodeSet("[SMTWFL📺📻🃏]");
        UnicodeSet actualLeadCodePoints = new UnicodeSet();
        map.putLeadCodePoints(actualLeadCodePoints);
        assertEquals("leadCodePoints", expectedLeadCodePoints, actualLeadCodePoints);

        // Add duplicated entry
        map.put("Sunday", FOO);
        // Add duplicated entry with different casing
        map.put("sunday", BAR);

        // Make sure the all entries are returned
        itr = map.get("Sunday");
        checkResult("Get Sunday", itr, new Object[]{FOO, SUN});
    }

    @Test
    public void TestCaseInsensitive() {
        Iterator itr = null;
        TextTrieMap map = new TextTrieMap(true);
        for (int i = 0; i < TESTDATA.length; i++) {
            map.put((String)TESTDATA[i][0], TESTDATA[i][1]);
        }

        logln("Test for get(String)");
        for (int i = 0; i < TESTCASES.length; i++) {
            itr = map.get((String)TESTCASES[i][0]);
            checkResult("get(String) case " + i, itr, TESTCASES[i][2]);
        }

        logln("Test for get(String, int)");
        StringBuffer textBuf = new StringBuffer();
        for (int i = 0; i < TESTCASES.length; i++) {
            textBuf.setLength(0);
            for (int j = 0; j < i; j++) {
                textBuf.append('X');
            }
            textBuf.append(TESTCASES[i][0]);
            itr = map.get(textBuf.toString(), i);
            checkResult("get(String, int) case " + i, itr, TESTCASES[i][2]);
        }

        logln("Test for partial match");
        for (Object[] cas : TESTDATA) {
            String str = (String) cas[0];
            for (int i = 0; i < str.length() - 1; i++) {
                TextTrieMap.Output output = new TextTrieMap.Output();
                map.get(str.substring(0, i), 0, output);
                assertTrue("Partial string means partial match", output.partialMatch);
            }
            String bad = str + "x";
            TextTrieMap.Output output = new TextTrieMap.Output();
            map.get(bad, 0, output);
            assertFalse("No partial match on bad string", output.partialMatch);
        }
        TextTrieMap.Output output = new TextTrieMap.Output();
        map.get("Sunday", 0, output);
        assertFalse("No partial match on string with no continuation", output.partialMatch);

        logln("Test for LeadCodePoints");
        UnicodeSet expectedLeadCodePoints = new UnicodeSet("[smtwfl📺📻🃏]");
        UnicodeSet actualLeadCodePoints = new UnicodeSet();
        map.putLeadCodePoints(actualLeadCodePoints);
        assertEquals("leadCodePoints", expectedLeadCodePoints, actualLeadCodePoints);

        // Add duplicated entry
        map.put("Sunday", FOO);
        // Add duplicated entry with different casing
        map.put("sunday", BAR);

        // Make sure the all entries are returned
        itr = map.get("Sunday");
        checkResult("Get Sunday", itr, new Object[]{SUN, FOO, BAR});
    }

    private boolean eql(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            if (o1 == null && o2 == null) {
                return true;
            }
            return false;
        }
        return o1.equals(o2);
    }

    private void checkResult(String memo, Iterator itr, Object expected) {
        if (itr == null) {
            if (expected != null) {
                String expectedStr = (expected instanceof Object[])
                        ? Arrays.toString((Object[]) expected)
                        : expected.toString();
                errln("FAIL: Empty results: " + memo + ": Expected: " + expectedStr);
            }
            return;
        }
        if (expected == null && itr != null) {
            errln("FAIL: Empty result is expected");
            return;
        }

        Object[] exp;
        if (expected instanceof Object[]) {
            exp = (Object[])expected;
        } else {
            exp = new Object[]{expected};
        }

        boolean[] found = new boolean[exp.length];
        while (itr.hasNext()) {
            Object val = itr.next();
            for (int i = 0; i < exp.length; i++) {
                if (eql(exp[i], val)) {
                    found[i] = true;
                }
            }
        }
        for (int i = 0; i < exp.length; i++) {
            if (found[i] == false) {
                errln("FAIL: The search result does not contain " + exp[i]);
            }
        }
    }
}

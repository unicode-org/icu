/*
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
*/
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.TextTrieMap;

public class TextTrieMapTest extends TestFmwk {

    private static final Integer SUN = new Integer(1);
    private static final Integer MON = new Integer(2);
    private static final Integer TUE = new Integer(3);
    private static final Integer WED = new Integer(4);
    private static final Integer THU = new Integer(5);
    private static final Integer FRI = new Integer(6);
    private static final Integer SAT = new Integer(7);

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
        {"S", SAT}
    };

    private static final Object[][] TESTCASES = {
        {"Sunday", SUN, SUN},
        {"sunday", null, SUN},
        {"Mo", MON, MON},
        {"mo", null, MON},
        {"Thursday Friday", THU, THU},
        {"T", THU, THU},
        {"TEST", THU, THU},
        {"SUN", SAT, SUN},
        {"super", null, SAT},
        {"NO", null, null}
    };
    
    public static void main(String[] args) throws Exception {
        TextTrieMapTest test = new TextTrieMapTest();
        test.run(args);
    }

    public void TestCaseSensitive() {
        TextTrieMap map = new TextTrieMap(false);
        for (int i = 0; i < TESTDATA.length; i++) {
            map.put((String)TESTDATA[i][0], TESTDATA[i][1]);
        }

        logln("Test for get(String)");
        for (int i = 0; i < TESTCASES.length; i++) {
            Object value = map.get((String)TESTCASES[i][0]);
            if (!eql(value, TESTCASES[i][1])) {
                errln("Invalid search results - Expected:" + TESTCASES[i][1] + " Actual:" + value);
            }
        }

        logln("Test for get(String, int)");
        StringBuffer textBuf = new StringBuffer();
        for (int i = 0; i < TESTCASES.length; i++) {
            textBuf.setLength(0);
            for (int j = 0; j < i; j++) {
                textBuf.append('X');
            }
            textBuf.append(TESTCASES[i][0]);
            Object value = map.get(textBuf.toString(), i);
            if (!eql(value, TESTCASES[i][1])) {
                errln("Invalid search results - Expected:" + TESTCASES[i][1] + " Actual:" + value);
            }
        }

        // Add duplicated entry
        Object prev = map.put("Sunday", FOO);
        if (!eql(prev, SUN)) {
            errln("The previous value of duplicated entry is not valid - Expected:" + SUN + " Actual:" + prev);
        }
        // Make sure the value is updated
        Object value = map.get("Sunday");
        if (!eql(value, FOO)) {
            errln("The map value is not valid - Expected:" + FOO + " Actual:" + value);
        }

        // Add duplicated entry with different casing
        prev = map.put("sunday", BAR);
        if (!eql(prev, null)) {
            errln("The value should be new in the trie map - Expected:null" + " Actual:" + prev);
        }
        // Make sure the value is valid
        value = map.get("sunday");
        if (!eql(value, BAR)) {
            errln("The map value is not valid - Expected:" + BAR + " Actual:" + value);
        }

    }

    public void TestCaseInsensitive() {
        TextTrieMap map = new TextTrieMap(true);
        for (int i = 0; i < TESTDATA.length; i++) {
            map.put((String)TESTDATA[i][0], TESTDATA[i][1]);
        }

        logln("Test for get(String)");
        for (int i = 0; i < TESTCASES.length; i++) {
            Object value = map.get((String)TESTCASES[i][0]);
            if (!eql(value, TESTCASES[i][2])) {
                errln("Invalid search results - Expected:" + TESTCASES[i][2] + " Actual:" + value);
            }
        }
        
        logln("Test for get(String, int)");
        StringBuffer textBuf = new StringBuffer();
        for (int i = 0; i < TESTCASES.length; i++) {
            textBuf.setLength(0);
            for (int j = 0; j < i; j++) {
                textBuf.append('X');
            }
            textBuf.append(TESTCASES[i][0]);
            Object value = map.get(textBuf.toString(), i);
            if (!eql(value, TESTCASES[i][2])) {
                errln("Invalid search results - Expected:" + TESTCASES[i][2] + " Actual:" + value);
            }
        }

        // Add duplicated entry
        Object prev = map.put("Sunday", FOO);
        if (!eql(prev, SUN)) {
            errln("The previous value of duplicated entry is not valid - Expected:" + SUN + " Actual:" + prev);
        }
        // Make sure the value is updated
        Object value = map.get("Sunday");
        if (!eql(value, FOO)) {
            errln("The map value is not valid - Expected:" + FOO + " Actual:" + value);
        }

        // Add duplicated entry with different casing
        prev = map.put("sunday", BAR);
        if (!eql(prev, FOO)) {
            errln("The value should be new in the trie map - Expected:" + FOO + " Actual:" + prev);
        }
        // Make sure the value is updated
        value = map.get("sunday");
        if (!eql(value, BAR)) {
            errln("The map value is not valid - Expected:" + BAR + " Actual:" + value);
        }

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
}

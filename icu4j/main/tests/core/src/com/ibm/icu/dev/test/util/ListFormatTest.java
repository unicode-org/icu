/*
 *******************************************************************************
 * Copyright (C) 2012-2012, Google, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.Arrays;
import java.util.Collection;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.ListFormat;
import com.ibm.icu.util.ULocale;

public class ListFormatTest extends TestFmwk {
    public static void main(String[] args) {
        new ListFormatTest().run(args);
    }
    
    String[] HardcodedTestData = {
            "",
            "A",
            "A and B",
            "A; B, and C",
            "A; B, C, and D",
            "A; B, C, D, and E"
    };

    public void TestBasic() {
        ListFormat formatter = new ListFormat("{0} and {1}", "{0}; {1}", "{0}, {1}", "{0}, and {1}");
        checkData(formatter, HardcodedTestData);
    }
    
    String[] EnglishTestData = {
            "",
            "A",
            "A and B",
            "A, B, and C",
            "A, B, C, and D",
            "A, B, C, D, and E"
    };
    
    public void TestEnglish() {
        checkData(ListFormat.getInstance(ULocale.ENGLISH), EnglishTestData);
        checkData(ListFormat.getInstance(ULocale.US), EnglishTestData);
    }
    
    String[] JapaneseTestData = {
            "",
            "A",
            "A、B",
            "A、B、C",
            "A、B、C、D",
            "A、B、C、D、E"
    };

    public void TestJapanese() {
        checkData(ListFormat.getInstance(ULocale.JAPANESE), JapaneseTestData);
    }
    
    String[] RootTestData = {
            "",
            "A",
            "A, B",
            "A, B, C",
            "A, B, C, D",
            "A, B, C, D, E"
    };

    public void TestSpecial() {
        checkData(ListFormat.getInstance(ULocale.ROOT), RootTestData);
        checkData(ListFormat.getInstance(new ULocale("xxx")), RootTestData);
    }


    public void checkData(ListFormat listFormat, String[] strings) {
        assertEquals("0", strings[0], listFormat.format());
        assertEquals("1", strings[1], listFormat.format("A"));
        assertEquals("2", strings[2], listFormat.format("A", "B"));
        assertEquals("3", strings[3], listFormat.format("A", "B", "C"));
        assertEquals("4", strings[4], listFormat.format("A", "B", "C", "D"));
        assertEquals("5", strings[5], listFormat.format("A", "B", "C", "D", "E"));
    }
}

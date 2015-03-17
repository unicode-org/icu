/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

public class TestAll extends TestGroup {

    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(new String[] {
            "BreakIteratorTest",
            "CollatorTest",
            "DateFormatSymbolsTest",
            "DateFormatTest",
            "DecimalFormatSymbolsTest",
            "NumberFormatTest",
            "CurrencyNameTest",
            "LocaleNameTest",
            "TimeZoneNameTest",
        });
    }
}

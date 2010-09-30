/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

public class TestPackaging extends TestGroup {

    public static void main(String[] args) {
        new TestPackaging().run(args);
    }

    public TestPackaging() {
        super(testList(), "ICU Packaging tests");
    }

    public static String[] testList() {
        return new String[] { "TestLocaleNamePackaging" };
    }

    public static final String CLASS_TARGET_NAME  = "Packaging";
}

/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.timezone;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {
    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }

    public TestAll() {
        super(new String[] {
            "TimeZoneTest",
            "TimeZoneRegression",
            "TimeZoneBoundaryTest",
//             "TimeZoneAliasTest",
        });
    }

    public static final String CLASS_TARGET_NAME = "TimeZone";
}

/*
 **************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                           *
 **************************************************************************
 *
 */

package com.ibm.icu.dev.test.timescale;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run time scale tests as a batch.
 */
public class TestAll extends TestGroup {

    public TestAll() {
        super(
            new String[] {
                "TimeScaleAPITest",
                "TimeScaleDataTest",
                "TimeScaleMonkeyTest",
            },
            "All TimeScale tests");
    }

    public static void main(String[] args)
    {
        new TestAll().run(args);
    }

    public static final String CLASS_TARGET_NAME  = "TimeScale";
}

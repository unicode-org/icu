// Copyright 2006 Google Inc.  All Rights Reserved.
/*
******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and        *
* others. All Rights Reserved.                                               *
******************************************************************************
*/


package com.ibm.icu.dev.test.duration;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all other tests as a batch.
 */
public class TestAll extends TestGroup {

    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(new String[] {
                  "com.ibm.icu.dev.test.duration.ICUDurationTest",
                  "com.ibm.icu.dev.test.duration.DataReadWriteTest",
                  "com.ibm.icu.dev.test.duration.PeriodBuilderFactoryTest",
                  "com.ibm.icu.dev.test.duration.PeriodBuilderTest",
                  "com.ibm.icu.dev.test.duration.PeriodTest",
                  "com.ibm.icu.dev.test.duration.ResourceBasedPeriodFormatterDataServiceTest",
                  "com.ibm.icu.dev.test.duration.languages.TestAll",
              },
              "Duration Tests");
    }

    public static final String CLASS_TARGET_NAME = "Duration";
}


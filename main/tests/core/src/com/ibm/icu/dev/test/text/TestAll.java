/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.text;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run text tests.
 */
public class TestAll extends TestGroup {
    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }

    public TestAll() {
        super(new String[] {
                  "SpoofCheckerTest"
              },
              "Text Tests");
    }

    public static final String CLASS_TARGET_NAME = "SpoofChecker";
}

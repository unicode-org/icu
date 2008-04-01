/**
*******************************************************************************
* Copyright (C) 2008, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/

package com.ibm.icu.dev.test.charset;

import com.ibm.icu.dev.test.TestFmwk.TestGroup;

/**
 * Top level test used to run all collation and search tests as a batch.
 */
public class TestAll extends TestGroup {
    public static void main(String[] args) {
        new TestAll().run(args);
    }

    public TestAll() {
        super(
              new String[] {
                  "TestCharset",
                  "TestConversion"
              },
              "All ICU Charset Tests"
              );
    }

    public static final String CLASS_TARGET_NAME = "Charset";
}
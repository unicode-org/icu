/*
******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and        *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

// Copyright 2006 Google Inc.  All Rights Reserved.

package com.ibm.icu.dev.test.duration.languages;

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
		  //                  "com.ibm.icu.dev.test.duration.languages.Test_ar_EG",
                  "com.ibm.icu.dev.test.duration.languages.Test_en",
                  "com.ibm.icu.dev.test.duration.languages.Test_es",
                  "com.ibm.icu.dev.test.duration.languages.Test_fr",
                  "com.ibm.icu.dev.test.duration.languages.Test_he_IL",
                  "com.ibm.icu.dev.test.duration.languages.Test_hi",
                  "com.ibm.icu.dev.test.duration.languages.Test_it",
                  "com.ibm.icu.dev.test.duration.languages.Test_ja",
                  "com.ibm.icu.dev.test.duration.languages.Test_ko",
                  "com.ibm.icu.dev.test.duration.languages.Test_zh_Hans",
                  "com.ibm.icu.dev.test.duration.languages.Test_zh_Hans_SG",
                  "com.ibm.icu.dev.test.duration.languages.Test_zh_Hant",
                  "com.ibm.icu.dev.test.duration.languages.Test_zh_Hant_HK",
              },
              "Duration Language Tests");
    }

    public static final String CLASS_TARGET_NAME = "DurationLanguages";
}


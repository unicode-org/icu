/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.Collator;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;

public class CollatorTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new CollatorTest().run(args);
    }

    public void TestGetInstance() {
        for (Locale loc : TestUtil.getICULocales()) {
            Collator coll = Collator.getInstance(loc);
            if (TestUtil.isICUOnly(loc)) {
                if (!(coll instanceof com.ibm.icu.impl.jdkadapter.CollatorICU)) {
                    errln("FAIL: getInstance returned JDK Collator for locale " + loc);
                }
            } else {
                if (coll instanceof com.ibm.icu.impl.jdkadapter.CollatorICU) {
                    logln("INFO: getInstance returned ICU Collator for locale " + loc);
                } else {
                    Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                    coll = Collator.getInstance(iculoc);
                    if (!(coll instanceof com.ibm.icu.impl.jdkadapter.CollatorICU)) {
                        errln("FAIL: getInstance returned JDK Collator for locale " + iculoc);
                    }
                }
            }
        }
    }
}

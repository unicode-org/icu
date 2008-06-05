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

    /*
     * Check if getInstance returns the ICU implementation.
     */
    public void TestGetInstance() {
        for (Locale loc : Collator.getAvailableLocales()) {
            Collator coll = Collator.getInstance(loc);

            boolean isIcuImpl = (coll instanceof com.ibm.icu.impl.jdkadapter.CollatorICU);

            if (TestUtil.isICUExtendedLocale(loc)) {
                if (!isIcuImpl) {
                    errln("FAIL: getInstance returned JDK Collator for locale " + loc);
                }
            } else {
                if (isIcuImpl) {
                    logln("INFO: getInstance returned ICU Collator for locale " + loc);
                }
                Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                Collator collIcu = Collator.getInstance(iculoc);
                if (isIcuImpl) {
                    if (!coll.equals(collIcu)) {
                        errln("FAIL: getInstance returned ICU Collator for locale " + loc
                                + ", but different from the one for locale " + iculoc);
                    }
                } else {
                    if (!(collIcu instanceof com.ibm.icu.impl.jdkadapter.CollatorICU)) {
                        errln("FAIL: getInstance returned JDK Collator for locale " + iculoc);
                    }
                }
            }
        }
    }
}

/*
 *******************************************************************************
 * Copyright (C) 2005, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.normalizer;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.Normalizer;

public class NormalizerRegressionTests extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new NormalizerRegressionTests().run(args);
    }

    public void TestJB4472() {
	String tamil = "\u0b87\u0ba8\u0bcd\u0ba4\u0bbf\u0baf\u0bbe";
	logln("Normalized: " + Normalizer.isNormalized(tamil, Normalizer.NFC, 0));
    }
}

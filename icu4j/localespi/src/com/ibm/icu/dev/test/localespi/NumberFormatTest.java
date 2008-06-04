/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.text.NumberFormat;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;

public class NumberFormatTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new NumberFormatTest().run(args);
    }

    private static final int DEFAULT_TYPE = 0;
    private static final int NUMBER_TYPE = 1;
    private static final int INTEGER_TYPE  = 2;
    private static final int PERCENT_TYPE = 3;
    private static final int CURRENCY_TYPE = 4;

    public void TestGetInstance() {
        for (Locale loc : TestUtil.getICULocales()) {
            checkGetInstance(DEFAULT_TYPE, loc);
            checkGetInstance(NUMBER_TYPE, loc);
            checkGetInstance(INTEGER_TYPE, loc);
            checkGetInstance(PERCENT_TYPE, loc);
            checkGetInstance(CURRENCY_TYPE, loc);
        }
    }

    private void checkGetInstance(int type, Locale loc) {
        NumberFormat nf = null;
        String method = null;

        switch (type) {
        case DEFAULT_TYPE:
            nf = NumberFormat.getInstance(loc);
            method = "getInstance";
            break;
        case NUMBER_TYPE:
            nf = NumberFormat.getNumberInstance(loc);
            method = "getNumberInstance";
            break;
        case INTEGER_TYPE:
            nf = NumberFormat.getIntegerInstance(loc);
            method = "getIntegerInstance";
            break;
        case PERCENT_TYPE:
            nf = NumberFormat.getPercentInstance(loc);
            method = "getPercentInstance";
            break;
        case CURRENCY_TYPE:
            nf = NumberFormat.getCurrencyInstance(loc);
            method = "getCurrencyInstance";
            break;
        default:
            errln("FAIL: Unknown number format type");
            return;
        }

        if (TestUtil.isICUOnly(loc)) {
            if (!(nf instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatICU)) {
                errln("FAIL: " + method + " returned JDK NumberFormat for locale " + loc);
            }
        } else {
            if (nf instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatICU) {
                logln("INFO: " + method + " returned ICU NumberFormat for locale " + loc);
            } else {
                Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                switch (type) {
                case DEFAULT_TYPE:
                    nf = NumberFormat.getInstance(iculoc);
                    method = "getInstance";
                    break;
                case NUMBER_TYPE:
                    nf = NumberFormat.getNumberInstance(iculoc);
                    method = "getNumberInstance";
                    break;
                case INTEGER_TYPE:
                    nf = NumberFormat.getIntegerInstance(iculoc);
                    method = "getIntegerInstance";
                    break;
                case PERCENT_TYPE:
                    nf = NumberFormat.getPercentInstance(iculoc);
                    method = "getPercentInstance";
                    break;
                case CURRENCY_TYPE:
                    nf = NumberFormat.getCurrencyInstance(iculoc);
                    method = "getCurrencyInstance";
                    break;
                }
                if (!(nf instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatICU)) {
                    errln("FAIL: " + method + " returned JDK NumberFormat for locale " + iculoc);
                }
            }
        }
    }
}

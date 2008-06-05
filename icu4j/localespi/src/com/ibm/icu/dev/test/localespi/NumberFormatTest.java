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

    /*
     * Check if getInstance returns the ICU implementation.
     */
    public void TestGetInstance() {
        for (Locale loc : NumberFormat.getAvailableLocales()) {
            checkGetInstance(DEFAULT_TYPE, loc);
            checkGetInstance(NUMBER_TYPE, loc);
            checkGetInstance(INTEGER_TYPE, loc);
            checkGetInstance(PERCENT_TYPE, loc);
            checkGetInstance(CURRENCY_TYPE, loc);
        }
    }

    private void checkGetInstance(int type, Locale loc) {
        NumberFormat nf;
        String method;

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

        boolean isIcuImpl = (nf instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatICU);
        if (TestUtil.isICUExtendedLocale(loc)) {
            if (!isIcuImpl) {
                errln("FAIL: " + method + " returned JDK NumberFormat for locale " + loc);
            }
        } else {
            if (isIcuImpl) {
                logln("INFO: " + method + " returned ICU NumberFormat for locale " + loc);
            }
            Locale iculoc = TestUtil.toICUExtendedLocale(loc);
            NumberFormat nfIcu = null;
            switch (type) {
            case DEFAULT_TYPE:
                nfIcu = NumberFormat.getInstance(iculoc);
                break;
            case NUMBER_TYPE:
                nfIcu = NumberFormat.getNumberInstance(iculoc);
                break;
            case INTEGER_TYPE:
                nfIcu = NumberFormat.getIntegerInstance(iculoc);
                break;
            case PERCENT_TYPE:
                nfIcu = NumberFormat.getPercentInstance(iculoc);
                break;
            case CURRENCY_TYPE:
                nfIcu = NumberFormat.getCurrencyInstance(iculoc);
                break;
            }
            if (isIcuImpl) {
                if (!nf.equals(nfIcu)) {
                    errln("FAIL: " + method + " returned ICU NumberFormat for locale " + loc
                            + ", but different from the one for locale " + iculoc);
                }
            } else {
                if (!(nfIcu instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatICU)) {
                    errln("FAIL: " + method + " returned JDK NumberFormat for locale " + iculoc);
                }
            }
        }
    }
}

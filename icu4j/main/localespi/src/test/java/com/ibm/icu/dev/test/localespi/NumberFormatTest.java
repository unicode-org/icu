// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class NumberFormatTest extends TestFmwk {
    private static final int DEFAULT_TYPE = 0;
    private static final int NUMBER_TYPE = 1;
    private static final int INTEGER_TYPE  = 2;
    private static final int PERCENT_TYPE = 3;
    private static final int CURRENCY_TYPE = 4;

    /*
     * Check if getInstance returns the ICU implementation.
     */
    @Test
    public void TestGetInstance() {
        for (Locale loc : NumberFormat.getAvailableLocales()) {
            if (TestUtil.isExcluded(loc)) {
                logln("Skipped " + loc);
                continue;
            }
            checkGetInstance(DEFAULT_TYPE, loc);
            checkGetInstance(NUMBER_TYPE, loc);
            checkGetInstance(INTEGER_TYPE, loc);
            checkGetInstance(PERCENT_TYPE, loc);
            checkGetInstance(CURRENCY_TYPE, loc);
        }
    }

    private void checkGetInstance(int type, Locale loc) {
        NumberFormat nf;
        String[] method = new String[1];
        nf = getJDKInstance(type, loc, method);

        boolean isIcuImpl = (nf instanceof com.ibm.icu.impl.jdkadapter.DecimalFormatICU)
                            || (nf instanceof com.ibm.icu.impl.jdkadapter.NumberFormatICU);
        if (TestUtil.isICUExtendedLocale(loc)) {
            if (!isIcuImpl) {
                errln("FAIL: " + method[0] + " returned JDK NumberFormat for locale " + loc);
            }
        } else if (isIcuImpl) {
            logln("INFO: " + method[0] + " returned ICU NumberFormat for locale " + loc);
            Locale iculoc = TestUtil.toICUExtendedLocale(loc);
            NumberFormat nfIcu = null;
            nfIcu = getJDKInstance(type, iculoc, null);
            if (!nf.equals(nfIcu)) {
                errln("FAIL: " + method[0] + " returned ICU NumberFormat for locale " + loc
                        + ", but different from the one for locale " + iculoc);
            }
        }
    }

    private NumberFormat getJDKInstance(int type, Locale loc, String[] methodName) {
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
        }
        if (methodName != null) {
            methodName[0] = method;
        }
        return nf;
    }

    private com.ibm.icu.text.NumberFormat getICUInstance(int type, Locale loc, String[] methodName) {
        com.ibm.icu.text.NumberFormat icunf = null;
        String method = null;

        switch (type) {
        case DEFAULT_TYPE:
            icunf = com.ibm.icu.text.NumberFormat.getInstance(loc);
            method = "getInstance";
            break;
        case NUMBER_TYPE:
            icunf = com.ibm.icu.text.NumberFormat.getNumberInstance(loc);
            method = "getNumberInstance";
            break;
        case INTEGER_TYPE:
            icunf = com.ibm.icu.text.NumberFormat.getIntegerInstance(loc);
            method = "getIntegerInstance";
            break;
        case PERCENT_TYPE:
            icunf = com.ibm.icu.text.NumberFormat.getPercentInstance(loc);
            method = "getPercentInstance";
            break;
        case CURRENCY_TYPE:
            icunf = com.ibm.icu.text.NumberFormat.getCurrencyInstance(loc);
            method = "getCurrencyInstance";
            break;
        }
        if (methodName != null) {
            methodName[0] = method;
        }
        return icunf;
    }

    /*
     * Testing the behavior of number format between ICU instance and its
     * equivalent created via the Locale SPI framework.
     */
    @Test
    public void TestICUEquivalent() {
        Locale[] TEST_LOCALES = {
                new Locale("en", "US"),
                new Locale("de", "DE"),
                new Locale("zh"),
        };

        long[] TEST_LONGS = {
                40L,
                -1578L,
                112233445566778899L,
        };

        double[] TEST_DOUBLES = {
                0.0451D,
                -1.679D,
                124578.369D,
        };

        // We want to really be sure that we test those exact classes.
        @SuppressWarnings("UnnecessaryBoxing")
        Object[] TEST_NUMBERS = {
                Byte.valueOf((byte)13),
                Integer.valueOf(3961),
                Long.valueOf(-3451237890000L),
                Float.valueOf(1.754F),
                Double.valueOf(-129.942362353D),
                new BigInteger("-15253545556575859505"),
                new BigDecimal("3.14159265358979323846264338"),
        };

        String[] methodName = new String[1];
        for (Locale loc : TEST_LOCALES) {
            for (int type = 0; type <= 4; type++) {
                Locale iculoc = TestUtil.toICUExtendedLocale(loc);
                NumberFormat nf = getJDKInstance(type, iculoc, methodName);
                com.ibm.icu.text.NumberFormat icunf = getICUInstance(type, loc, null);

                String s1, s2;
                Number n1, n2;
                boolean pe1, pe2;
                for (long l : TEST_LONGS) {
                    s1 = nf.format(l);
                    s2 = icunf.format(l);

                    if (!s1.equals(s2)) {
                        errln("FAIL: Different results for formatting long " + l + " by NumberFormat("
                                + methodName[0] + ") in locale " + loc + " - JDK:" + s1 + " ICU:" + s2);
                    }

                    pe1 = false;
                    n1 = n2 = null;
                    try {
                        n1 = nf.parse(s1);
                    } catch (ParseException e) {
                        pe1 = true;
                    }
                    pe2 = false;
                    try {
                        n2 = icunf.parse(s2);
                    } catch (ParseException e) {
                        pe2 = true;
                    }
                    if ((pe1 && !pe2) || (!pe1 && pe2)) {
                        errln("FAIL: ParseException thrown by " + (pe1 ? "JDK" : "ICU")
                                + " NumberFormat(" + methodName[0] + ") for parsing long" + l
                                + " in locale " + loc);
                    } else if (!pe1 && !pe2 && !n1.equals(n2)) {
                        errln("FAIL: Different results for parsing long " + l + " by NumberFormat("
                                + methodName[0] + ") in locale " + loc + " - JDK:" + n1 + " ICU:" + n2);
                    } else if (pe1 && pe2) {
                        logln("INFO: ParseException thrown by both JDK and ICU NumberFormat("
                                + methodName[0] + ") for parsing long " + l + " in locale " + loc);
                    }
                }

                for (double d : TEST_DOUBLES) {
                    s1 = nf.format(d);
                    s2 = icunf.format(d);

                    if (!s1.equals(s2)) {
                        errln("FAIL: Different results for formatting double " + d + " by NumberFormat("
                                + methodName[0] + ") in locale " + loc + " - JDK:" + s1 + " ICU:" + s2);
                    }

                    pe1 = false;
                    n1 = n2 = null;
                    try {
                        n1 = nf.parse(s1);
                    } catch (ParseException e) {
                        pe1 = true;
                    }
                    pe2 = false;
                    try {
                        n2 = icunf.parse(s2);
                    } catch (ParseException e) {
                        pe2 = true;
                    }
                    if ((pe1 && !pe2) || (!pe1 && pe2)) {
                        errln("FAIL: ParseException thrown by " + (pe1 ? "JDK" : "ICU")
                                + " NumberFormat(" + methodName[0] + ") for parsing double" + d
                                + " in locale " + loc);
                    } else if (!pe1 && !pe2 && !n1.equals(n2)) {
                        errln("FAIL: Different results for parsing double " + d + " by NumberFormat("
                                + methodName[0] + ") in locale " + loc + " - JDK:" + n1 + " ICU:" + n2);
                    } else if (pe1 && pe2) {
                        logln("INFO: ParseException thrown by both JDK and ICU NumberFormat("
                                + methodName[0] + ") for parsing double " + d + " in locale " + loc);
                    }
                }

                for (Object o : TEST_NUMBERS) {
                    s1 = nf.format(o);
                    s2 = icunf.format(o);

                    if (!s1.equals(s2)) {
                        errln("FAIL: Different results for formatting " + o.getClass().getName() + " by NumberFormat("
                                + methodName[0] + ") in locale " + loc + " - JDK:" + s1 + " ICU:" + s2);
                    }

                    pe1 = false;
                    n1 = n2 = null;
                    try {
                        n1 = nf.parse(s1);
                    } catch (ParseException e) {
                        pe1 = true;
                    }
                    pe2 = false;
                    try {
                        n2 = icunf.parse(s2);
                    } catch (ParseException e) {
                        pe2 = true;
                    }
                    if ((pe1 && !pe2) || (!pe1 && pe2)) {
                        errln("FAIL: ParseException thrown by " + (pe1 ? "JDK" : "ICU")
                                + " NumberFormat(" + methodName[0] + ") for parsing " + o.getClass().getName()
                                + " in locale " + loc);
                    } else if (!pe1 && !pe2 && !n1.equals(n2)) {
                        errln("FAIL: Different results for parsing " + o.getClass().getName() + " by NumberFormat("
                                + methodName[0] + ") in locale " + loc + " - JDK:" + n1 + " ICU:" + n2);
                    } else if (pe1 && pe2) {
                        logln("INFO: ParseException thrown by both JDK and ICU NumberFormat("
                                + methodName[0] + ") for parsing " + o.getClass().getName() + " in locale " + loc);
                    }
                }
            }
        }
    }

    @Test
    public void TestKeywords() {
        // ICU provider variant is appended
        ULocale uloc = new ULocale("en_US_" + TestUtil.ICU_VARIANT + "@numbers=arab;currency=EUR");
        Locale loc = uloc.toLocale();
        NumberFormat jdkNfmt = NumberFormat.getCurrencyInstance(loc);
        com.ibm.icu.text.NumberFormat icuNfmt = com.ibm.icu.text.NumberFormat.getCurrencyInstance(uloc);

        final double num = 12345.67d;
        String jdkOut = jdkNfmt.format(num);
        String icuOut = icuNfmt.format(num);

        if (!jdkOut.equals(icuOut)) {
            errln("FAIL: JDK number format with Locale " + loc + " is " + jdkOut + ", expected: " + icuOut);
        }
    }
}

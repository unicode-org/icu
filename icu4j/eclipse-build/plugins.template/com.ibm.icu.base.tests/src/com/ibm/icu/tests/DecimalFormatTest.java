// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2006-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import java.util.Locale;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.DecimalFormatSymbols;

public class DecimalFormatTest extends ICUTestCase {
    private static final long lmax = Long.MAX_VALUE;
    private static final double dsmall = 23.33;
        
    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.clone()'
     */
    public void testClone() {
        DecimalFormat df = new DecimalFormat("#,#0.00");
        DecimalFormat df2 = new DecimalFormat("#,#0.00");
        DecimalFormat dfn = new DecimalFormat("#,#0.00");
        dfn.setNegativePrefix(dfn.getNegativePrefix() + '!');
        testEHCS(df, df2, dfn);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.DecimalFormat(DecimalFormat)'
     */
    public void testDecimalFormatDecimalFormat() {
        // tested implicitly
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.DecimalFormat()'
     */
    public void testDecimalFormat() {
        DecimalFormat df = new DecimalFormat();
        assertEquals("9,223,372,036,854,775,807", df.format(lmax));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.DecimalFormat(String)'
     */
    public void testDecimalFormatString() {
        DecimalFormat df = new DecimalFormat("#,##0.000");
        assertEquals("23.330", df.format(dsmall));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.DecimalFormat(String, DecimalFormatSymbols)'
     */
    public void testDecimalFormatStringDecimalFormatSymbols() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.FRANCE);
        DecimalFormat df = new DecimalFormat("#,##0.000", sym);
        assertEquals("23,330", df.format(dsmall));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.getDecimalFormatSymbols()'
     */
    public void testGetDecimalFormatSymbols() {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.FRANCE);
        DecimalFormat df = new DecimalFormat("#,##0.000", sym);
        assertEquals(sym, df.getDecimalFormatSymbols());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols)'
     */
    public void testSetDecimalFormatSymbols() {
        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.FRANCE));
        assertEquals("23,33", df.format(dsmall));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.getPositivePrefix()'
     */
    public void testGetPositivePrefix() {
        DecimalFormat df = new DecimalFormat("+#,##0.#;-#,##0.#");
        assertEquals("+", df.getPositivePrefix());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.setPositivePrefix(String)'
     */
    public void testSetPositivePrefix() {
        DecimalFormat df = new DecimalFormat("+#,##0.#;-#,##0.#");
        df.setPositivePrefix("?");
        assertEquals("?23.3", df.format(dsmall));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.getNegativePrefix()'
     */
    public void testGetNegativePrefix() {
        DecimalFormat df = new DecimalFormat("+#,##0.#;-#,##0.#");
        assertEquals("-", df.getNegativePrefix());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.setNegativePrefix(String)'
     */
    public void testSetNegativePrefix() {
        DecimalFormat df = new DecimalFormat("+#,##0.#;-#,##0.#");
        df.setNegativePrefix("~");
        assertEquals("~23.3", df.format(-dsmall));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.getPositiveSuffix()'
     */
    public void testGetPositiveSuffix() {
        DecimalFormat df = new DecimalFormat("+#,##0.#**;-#,##0.#~~");
        assertEquals("**", df.getPositiveSuffix());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.setPositiveSuffix(String)'
     */
    public void testSetPositiveSuffix() {
        DecimalFormat df = new DecimalFormat("+#,##0.#;-#,##0.#");
        df.setPositiveSuffix("**");
        assertEquals("+23.3**", df.format(dsmall));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.getNegativeSuffix()'
     */
    public void testGetNegativeSuffix() {
        DecimalFormat df = new DecimalFormat("+#,##0.#**;-#,##0.#~~");
        assertEquals("~~", df.getNegativeSuffix());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.setNegativeSuffix(String)'
     */
    public void testSetNegativeSuffix() {
        DecimalFormat df = new DecimalFormat("+#,##0.#;-#,##0.#");
        df.setNegativeSuffix("~~");
        assertEquals("-23.3~~", df.format(-dsmall));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.getMultiplier()'
     */
    public void testGetMultiplier() {
        DecimalFormat df = new DecimalFormat("%000");
        df.setMultiplier(1000);
        assertEquals(1000, df.getMultiplier());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.setMultiplier(int)'
     */
    public void testSetMultiplier() {
        DecimalFormat df = new DecimalFormat("%000");
        assertEquals("%012", df.format(.123));
        df.setMultiplier(1000);
        assertEquals("%123", df.format(.123));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.getGroupingSize()'
     */
    public void testGetGroupingSize() {
        DecimalFormat df = new DecimalFormat("#,#0.#");
        assertEquals(2, df.getGroupingSize());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.setGroupingSize(int)'
     */
    public void testSetGroupingSize() {
        DecimalFormat df = new DecimalFormat("#,##0.##");
        assertEquals("1,234,567.89", df.format(1234567.89));
        df.setGroupingSize(2);
        assertEquals("1,23,45,67.89", df.format(1234567.89));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.isDecimalSeparatorAlwaysShown()'
     */
    public void testIsDecimalSeparatorAlwaysShown() {
        DecimalFormat df = new DecimalFormat("#.#");
        df.setDecimalSeparatorAlwaysShown(false);
        assertEquals("1", df.format(1));
        assertEquals("1.2", df.format(1.2));
        df.setDecimalSeparatorAlwaysShown(true);
        assertEquals("1.", df.format(1));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.setDecimalSeparatorAlwaysShown(boolean)'
     */
    public void testSetDecimalSeparatorAlwaysShown() {
        DecimalFormat df = new DecimalFormat("#.#");
        df.setDecimalSeparatorAlwaysShown(false);
        assertFalse(df.isDecimalSeparatorAlwaysShown());
        df.setDecimalSeparatorAlwaysShown(true);
        assertTrue(df.isDecimalSeparatorAlwaysShown());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.toPattern()'
     */
    public void testToPattern() {
        DecimalFormat df = new DecimalFormat("#,##0.##");
        assertEquals("#,##0.##", df.toPattern());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.toLocalizedPattern()'
     */
    public void testToLocalizedPattern() {
        DecimalFormat df = new DecimalFormat("#,##0.##", new DecimalFormatSymbols(Locale.FRANCE));
        assertEquals("#,##0.##", df.toPattern());
        assertEquals("#\u00a0##0,##", df.toLocalizedPattern());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.applyPattern(String)'
     */
    public void testApplyPattern() {
        DecimalFormat df = new DecimalFormat("#,##0.##");
        df.applyPattern("#,0.#");
        assertEquals("1,2,3.4", df.format(123.4));
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormat.applyLocalizedPattern(String)'
     */
    public void testApplyLocalizedPattern() {
        DecimalFormat df = new DecimalFormat("#,##0.##", new DecimalFormatSymbols(Locale.FRANCE));
        df.applyLocalizedPattern("#\u00a00,#");
        assertEquals("1\u00a02\u00a03,4", df.format(123.4));
    }
}

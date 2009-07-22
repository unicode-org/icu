/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;

import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class NumberFormatTest extends ICUTestCase {

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.NumberFormat(NumberFormat)'
     */
    public void testNumberFormat() {
        NumberFormat nf = new NumberFormat(java.text.NumberFormat.getInstance());
        assertEquals(nf, NumberFormat.getInstance());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.format(Object, StringBuffer, FieldPosition)'
     */
    public void testFormatObjectStringBufferFieldPosition() {
        Number num = new Long(1234L);
        StringBuffer buf = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        NumberFormat.getInstance().format(num, buf, fp);
        assertEquals("1,234", buf.toString());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.parseObject(String, ParsePosition)'
     */
    public void testParseObjectStringParsePosition() {
        ParsePosition pp = new ParsePosition(0);
        Object result = NumberFormat.getInstance().parse("1,234", pp);
        assertEquals(result, new Long(1234));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.format(double)'
     */
    public void testFormatDouble() {
        assertEquals("1,234.567", NumberFormat.getInstance().format(1234.567));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.format(long)'
     */
    public void testFormatLong() {
        assertEquals("1,234", NumberFormat.getInstance().format(1234L));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.format(BigInteger)'
     */
    public void testFormatBigInteger() {
        // note, java doesn't handle biginteger with full precision.
        BigInteger bi = new BigInteger("123456");
        assertEquals("123,456", java.text.NumberFormat.getInstance().format(bi));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.format(double, StringBuffer, FieldPosition)'
     */
    public void testFormatDoubleStringBufferFieldPosition() {
        StringBuffer buf = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        assertEquals("123,456.789", NumberFormat.getInstance().format(123456.789, buf, fp).toString());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.format(long, StringBuffer, FieldPosition)'
     */
    public void testFormatLongStringBufferFieldPosition() {
        StringBuffer buf = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        assertEquals("123,456", NumberFormat.getInstance().format(123456L, buf, fp).toString());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.format(BigInteger, StringBuffer, FieldPosition)'
     */
    public void testFormatBigIntegerStringBufferFieldPosition() {
        // note, java doesn't handle biginteger with full precision.
        StringBuffer buf = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        BigInteger bi = new BigInteger("123456");
        assertEquals("123,456", java.text.NumberFormat.getInstance().format(bi, buf, fp).toString());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.parse(String, ParsePosition)'
     */
    public void testParseStringParsePosition() {
        ParsePosition pp = new ParsePosition(3);
        assertEquals(new Long(123456), NumberFormat.getInstance().parse("xxx123,456yyy", pp));
        assertEquals(10, pp.getIndex());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.parse(String)'
     */
    public void testParseString() throws ParseException {
        Number result = NumberFormat.getInstance().parse("123,456,yyy");
        assertEquals(new Long(123456), result);
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.isParseIntegerOnly()'
     */
    public void testIsParseIntegerOnly() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setParseIntegerOnly(true);
        assertTrue(nf.isParseIntegerOnly());
        nf.setParseIntegerOnly(false);
        assertFalse(nf.isParseIntegerOnly());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.setParseIntegerOnly(boolean)'
     */
    public void testSetParseIntegerOnly() throws ParseException {
        String str = "123.456,yyy";
        NumberFormat nf = NumberFormat.getInstance();
        assertEquals(new Double(123.456), nf.parse(str));
        nf.setParseIntegerOnly(true);
        assertEquals(new Long(123), nf.parse(str));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getInstance()'
     */
    public void testGetInstance() {
        // used everywhere, no need to test
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getInstance(Locale)'
     */
    public void testGetInstanceLocale() {
        NumberFormat nf = NumberFormat.getInstance(Locale.GERMANY);
        assertEquals("123,456", nf.format(123.456));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getInstance(ULocale)'
     */
    public void testGetInstanceULocale() {
        NumberFormat nf = NumberFormat.getInstance(ULocale.GERMANY);
        assertEquals("123,456", nf.format(123.456));            
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getNumberInstance()'
     */
    public void testGetNumberInstance() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        assertEquals("123,456.789", nf.format(123456.789));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getNumberInstance(Locale)'
     */
    public void testGetNumberInstanceLocale() {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.GERMANY);
        assertEquals("123.456,789", nf.format(123456.789));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getNumberInstance(ULocale)'
     */
    public void testGetNumberInstanceULocale() {
        NumberFormat nf = NumberFormat.getNumberInstance(ULocale.GERMANY);
        assertEquals("123.456,789", nf.format(123456.789));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getIntegerInstance()'
     */
    public void testGetIntegerInstance() {
        NumberFormat nf = NumberFormat.getIntegerInstance();
        assertEquals("123,457", nf.format(123456.789)); // rounds
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getIntegerInstance(Locale)'
     */
    public void testGetIntegerInstanceLocale() {
        NumberFormat nf = NumberFormat.getIntegerInstance(Locale.GERMANY);
        assertEquals("123.457", nf.format(123456.789)); // rounds
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getIntegerInstance(ULocale)'
     */
    public void testGetIntegerInstanceULocale() {
        NumberFormat nf = NumberFormat.getIntegerInstance(ULocale.GERMANY);
        assertEquals("123.457", nf.format(123456.789)); // rounds
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getCurrencyInstance()'
     */
    public void testGetCurrencyInstance() {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        assertEquals("$123,456.99", nf.format(123456.99));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getCurrencyInstance(Locale)'
     */
    public void testGetCurrencyInstanceLocale() {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        assertEquals("123.456,99 \u20AC", nf.format(123456.99));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getCurrencyInstance(ULocale)'
     */
    public void testGetCurrencyInstanceULocale() {
        NumberFormat nf = NumberFormat.getCurrencyInstance(ULocale.GERMANY);
        assertEquals("123.456,99 \u20AC", nf.format(123456.99));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getPercentInstance()'
     */
    public void testGetPercentInstance() {
        NumberFormat nf = NumberFormat.getPercentInstance();
        assertEquals("123,456%", nf.format(1234.56));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getPercentInstance(Locale)'
     */
    public void testGetPercentInstanceLocale() {
        NumberFormat nf = NumberFormat.getPercentInstance(Locale.GERMANY);
        assertEquals("123.456%", nf.format(1234.56));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getPercentInstance(ULocale)'
     */
    public void testGetPercentInstanceULocale() {
        NumberFormat nf = NumberFormat.getPercentInstance(ULocale.GERMANY);
        assertEquals("123.456%", nf.format(1234.56));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getScientificInstance()'
     */
    public void testGetScientificInstance() {
        NumberFormat nf = NumberFormat.getScientificInstance();
        assertEquals(".123456E4", nf.format(1234.56));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getScientificInstance(Locale)'
     */
    public void testGetScientificInstanceLocale() {
        NumberFormat nf = NumberFormat.getScientificInstance(Locale.GERMANY);
        assertEquals(",123456E4", nf.format(1234.56));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getScientificInstance(ULocale)'
     */
    public void testGetScientificInstanceULocale() {
        NumberFormat nf = NumberFormat.getScientificInstance(ULocale.GERMANY);
        assertEquals(",123456E4", nf.format(1234.56));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getAvailableLocales()'
     */
    public void testGetAvailableLocales() {
        Locale[] ilocales = NumberFormat.getAvailableLocales();
        if (ICUTestCase.testingWrapper) {
            Locale[] jlocales = java.text.NumberFormat.getAvailableLocales();
            for (int i = 0; i < ilocales.length; ++i) {
                assertEquals(jlocales[i], ilocales[i]);
            }
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getAvailableULocales()'
     */
    public void testGetAvailableULocales() {
        ULocale[] ulocales = NumberFormat.getAvailableULocales();
        if (ICUTestCase.testingWrapper) {
            Locale[] jlocales = java.text.NumberFormat.getAvailableLocales();
            for (int i = 0; i < ulocales.length; ++i) {
                assertEquals(jlocales[i], ulocales[i].toLocale());
            }
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.isGroupingUsed()'
     */
    public void testIsGroupingUsed() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true);
        assertTrue(nf.isGroupingUsed());
        nf.setGroupingUsed(false);
        assertFalse(nf.isGroupingUsed());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.setGroupingUsed(boolean)'
     */
    public void testSetGroupingUsed() {
        NumberFormat nf = NumberFormat.getInstance();
        assertEquals("123,456,789", nf.format(123456789));
        nf.setGroupingUsed(false);
        assertEquals("123456789", nf.format(123456789));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getMaximumIntegerDigits()'
     */
    public void testGetMaximumIntegerDigits() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumIntegerDigits(4);
        assertEquals(4, nf.getMaximumIntegerDigits());
        nf.setMaximumIntegerDigits(6);
        assertEquals(6, nf.getMaximumIntegerDigits());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.setMaximumIntegerDigits(int)'
     */
    public void testSetMaximumIntegerDigits() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumIntegerDigits(4);
        assertEquals("3,456", nf.format(123456)); // high digits truncated
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getMinimumIntegerDigits()'
     */
    public void testGetMinimumIntegerDigits() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(4);
        assertEquals(4, nf.getMinimumIntegerDigits());
        nf.setMinimumIntegerDigits(6);
        assertEquals(6, nf.getMinimumIntegerDigits());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.setMinimumIntegerDigits(int)'
     */
    public void testSetMinimumIntegerDigits() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumIntegerDigits(4);
        assertEquals("0,012", nf.format(12)); // pad out with zero, grouping still used
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getMaximumFractionDigits()'
     */
    public void testGetMaximumFractionDigits() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);
        assertEquals(4, nf.getMaximumFractionDigits());
        nf.setMaximumFractionDigits(6);
        assertEquals(6, nf.getMaximumFractionDigits());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.setMaximumFractionDigits(int)'
     */
    public void testSetMaximumFractionDigits() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);
        assertEquals("1.2346", nf.format(1.2345678)); // low digits rounded
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.getMinimumFractionDigits()'
     */
    public void testGetMinimumFractionDigits() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(4);
        assertEquals(4, nf.getMinimumFractionDigits());
        nf.setMinimumFractionDigits(6);
        assertEquals(6, nf.getMinimumFractionDigits());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.setMinimumFractionDigits(int)'
     */
    public void testSetMinimumFractionDigits() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(4);
        assertEquals("1.2000", nf.format(1.2));
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.toString()'
     */
    public void testToString() {
        assertNotNull(NumberFormat.getInstance().toString());
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.hashCode()'
     */
    public void testHashCode() {
        NumberFormat nf = NumberFormat.getInstance();
        NumberFormat eq = NumberFormat.getInstance(Locale.US);
        NumberFormat neq = NumberFormat.getInstance(Locale.GERMANY);
                
        ICUTestCase.testEHCS(nf, eq, neq);
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.clone()'
     */
    public void testClone() {
        // see testHashCode
    }

    /*
     * Test method for 'com.ibm.icu.x.text.NumberFormat.equals(Object)'
     */
    public void testEqualsObject() {
        // see testHashCode
    }
}

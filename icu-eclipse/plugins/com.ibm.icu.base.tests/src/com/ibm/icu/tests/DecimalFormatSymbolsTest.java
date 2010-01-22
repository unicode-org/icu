/*
 *******************************************************************************
 * Copyright (C) 2006-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import java.util.Locale;

import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.ULocale;

public class DecimalFormatSymbolsTest extends ICUTestCase {

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.hashCode()'
     */
    public void testHashCode() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        DecimalFormatSymbols dfs2 = new DecimalFormatSymbols(ULocale.US);
        DecimalFormatSymbols dfsn = new DecimalFormatSymbols(Locale.FRANCE);
        testEHCS(dfs, dfs2, dfsn);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.DecimalFormatSymbols(DecimalFormatSymbols)'
     */
    public void testDecimalFormatSymbolsDecimalFormatSymbols() {
        // implicitly tested everywhere
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.DecimalFormatSymbols()'
     */
    public void testDecimalFormatSymbols() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        assertTrue(-1 != dfs.getDecimalSeparator());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.DecimalFormatSymbols(Locale)'
     */
    public void testDecimalFormatSymbolsLocale() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.US);
        assertTrue(-1 != dfs.getDecimalSeparator());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.DecimalFormatSymbols(ULocale)'
     */
    public void testDecimalFormatSymbolsULocale() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertTrue(-1 != dfs.getDecimalSeparator());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getZeroDigit()'
     */
    public void testGetZeroDigit() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals('0', dfs.getZeroDigit());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setZeroDigit(char)'
     */
    public void testSetZeroDigit() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        char value = dfs.getZeroDigit();
        char value1 = (char)(value + 1);
        dfs.setZeroDigit(value1);
        char result = dfs.getZeroDigit();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getGroupingSeparator()'
     */
    public void testGetGroupingSeparator() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals(',', dfs.getGroupingSeparator());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setGroupingSeparator(char)'
     */
    public void testSetGroupingSeparator() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        char value = dfs.getGroupingSeparator();
        char value1 = (char)(value + 1);
        dfs.setGroupingSeparator(value1);
        char result = dfs.getGroupingSeparator();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getDecimalSeparator()'
     */
    public void testGetDecimalSeparator() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals('.', dfs.getDecimalSeparator());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setDecimalSeparator(char)'
     */
    public void testSetDecimalSeparator() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        char value = dfs.getDecimalSeparator();
        char value1 = (char)(value + 1);
        dfs.setDecimalSeparator(value1);
        char result = dfs.getDecimalSeparator();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getPerMill()'
     */
    public void testGetPerMill() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals('\u2030', dfs.getPerMill());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setPerMill(char)'
     */
    public void testSetPerMill() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        char value = dfs.getPerMill();
        char value1 = (char)(value + 1);
        dfs.setPerMill(value1);
        char result = dfs.getPerMill();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getPercent()'
     */
    public void testGetPercent() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals('%', dfs.getPercent());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setPercent(char)'
     */
    public void testSetPercent() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        char value = dfs.getPercent();
        char value1 = (char)(value + 1);
        dfs.setPercent(value1);
        char result = dfs.getPercent();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getDigit()'
     */
    public void testGetDigit() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals('#', dfs.getDigit());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setDigit(char)'
     */
    public void testSetDigit() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        char value = dfs.getDigit();
        char value1 = (char)(value + 1);
        dfs.setDigit(value1);
        char result = dfs.getDigit();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getPatternSeparator()'
     */
    public void testGetPatternSeparator() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals(';', dfs.getPatternSeparator());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setPatternSeparator(char)'
     */
    public void testSetPatternSeparator() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        char value = dfs.getPatternSeparator();
        char value1 = (char)(value + 1);
        dfs.setPatternSeparator(value1);
        char result = dfs.getPatternSeparator();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getInfinity()'
     */
    public void testGetInfinity() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals("\u221e", dfs.getInfinity());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setInfinity(String)'
     */
    public void testSetInfinity() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        String value = dfs.getInfinity();
        String value1 = value + "!";
        dfs.setInfinity(value1);
        String result = dfs.getInfinity();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getNaN()'
     */
    public void testGetNaN() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertNotNull(dfs.getNaN()); // java returns missing character???
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setNaN(String)'
     */
    public void testSetNaN() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        String value = dfs.getNaN();
        String value1 = value + "!";
        dfs.setNaN(value1);
        String result = dfs.getNaN();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getMinusSign()'
     */
    public void testGetMinusSign() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals('-', dfs.getMinusSign());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setMinusSign(char)'
     */
    public void testSetMinusSign() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        char value = dfs.getMinusSign();
        char value1 = (char)(value + 1);
        dfs.setMinusSign(value1);
        char result = dfs.getMinusSign();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getCurrencySymbol()'
     */
    public void testGetCurrencySymbol() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals("$", dfs.getCurrencySymbol());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setCurrencySymbol(String)'
     */
    public void testSetCurrencySymbol() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        String value = dfs.getCurrencySymbol();
        String value1 = value + "!";
        dfs.setCurrencySymbol(value1);
        String result = dfs.getCurrencySymbol();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getInternationalCurrencySymbol()'
     */
    public void testGetInternationalCurrencySymbol() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals("USD", dfs.getInternationalCurrencySymbol());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setInternationalCurrencySymbol(String)'
     */
    public void testSetInternationalCurrencySymbol() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        String value = dfs.getInternationalCurrencySymbol();
        String value1 = value + "!";
        dfs.setInternationalCurrencySymbol(value1);
        String result = dfs.getInternationalCurrencySymbol();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.getMonetaryDecimalSeparator()'
     */
    public void testGetMonetaryDecimalSeparator() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        assertEquals('.', dfs.getMonetaryDecimalSeparator());
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.setMonetaryDecimalSeparator(char)'
     */
    public void testSetMonetaryDecimalSeparator() {
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(ULocale.US);
        char value = dfs.getMonetaryDecimalSeparator();
        char value1 = (char)(value + 1);
        dfs.setMonetaryDecimalSeparator(value1);
        char result = dfs.getMonetaryDecimalSeparator();
        assertNotEqual(value, result);
        assertEquals(value1, result);
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.clone()'
     */
    public void testClone() {
        // tested in testHashcode
    }

    /*
     * Test method for 'com.ibm.icu.text.DecimalFormatSymbols.equals(Object)'
     */
    public void testEqualsObject() {
        // tested in testHashcode
    }
}

/*
 *******************************************************************************
 * Copyright (C) 2006-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import java.util.Locale;

import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;

public class CollatorTest extends ICUTestCase {
    private static final String s1 = "Fu\u0308nf"; // capital F + u + diaresis
    private static final String s2 = "fu\u0308nf"; // u + diaresis
    private static final String s3 = "f\u00fcnf"; // u-umlaut
    private static final String s4 = "fu\u0308\u0316nf"; // u + diaresis above + grave below
    private static final String s5 = "fu\u0316\u0308nf"; // u + grave below + diaresis above

    /*
     * Test method for 'com.ibm.icu.text.Collator.hashCode()'
     */
    public void testHashCode() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.SECONDARY);
        Collator c2 = Collator.getInstance();
        c2.setStrength(Collator.SECONDARY);
        Collator cn = Collator.getInstance();
        cn.setStrength(Collator.TERTIARY);
        testEHCS(c, c2, cn);
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.Collator(Collator)'
     */
    public void testCollator() {
        // implicitly tested everywhere
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.setStrength(int)'
     */
    public void testSetStrength() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        assertTrue(0 == c.compare(s1, s2));
        c.setStrength(Collator.SECONDARY);
        assertTrue(0 == c.compare(s1, s2));
        c.setStrength(Collator.TERTIARY);
        assertTrue(0 < c.compare(s1, s2));
        assertTrue(0 == c.compare(s2, s3));
        c.setStrength(Collator.QUATERNARY);
        assertTrue(0 > c.compare(s2, s3));
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.setDecomposition(int)'
     */
    public void testSetDecomposition() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.TERTIARY);
        assertTrue(0 != c.compare(s4, s5));
        c.setDecomposition(Collator.IDENTICAL);
        assertTrue(0 == c.compare(s4, s5));
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.getInstance()'
     */
    public void testGetInstance() {
        // implicitly tested everywhere
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.getInstance(ULocale)'
     */
    public void testGetInstanceULocale() {
        Collator c = Collator.getInstance(ULocale.GERMANY);
        assertNotNull(c);
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.getInstance(Locale)'
     */
    public void testGetInstanceLocale() {
        Collator c = Collator.getInstance(Locale.GERMANY);
        assertNotNull(c);
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.getAvailableLocales()'
     */
    public void testGetAvailableLocales() {
        assertNotNull(Collator.getAvailableLocales());
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.getAvailableULocales()'
     */
    public void testGetAvailableULocales() {
        assertNotNull(Collator.getAvailableULocales());
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.getKeywords()'
     */
    public void testGetKeywords() {
        assertEquals(0, Collator.getKeywords().length);
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.getKeywordValues(String)'
     */
    public void testGetKeywordValues() {
        assertEquals(0, Collator.getKeywordValues("").length);
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.getStrength()'
     */
    public void testGetStrength() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        assertEquals(Collator.PRIMARY, c.getStrength());
        c.setStrength(Collator.SECONDARY);
        assertEquals(Collator.SECONDARY, c.getStrength());
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.getDecomposition()'
     */
    public void testGetDecomposition() {
        Collator c = Collator.getInstance();
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        assertEquals(Collator.CANONICAL_DECOMPOSITION, c.getDecomposition());
        c.setDecomposition(Collator.NO_DECOMPOSITION);
        assertEquals(Collator.NO_DECOMPOSITION, c.getDecomposition());
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.compare(Object, Object)'
     */
    public void testCompareObjectObject() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        assertTrue(0 == c.compare((Object)s1, (Object)s2));
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.equals(String, String)'
     */
    public void testEqualsStringString() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        assertTrue(c.equals(s1, s2));
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.compare(String, String)'
     */
    public void testCompareStringString() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        assertTrue(0 == c.compare(s1, s2));
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.getCollationKey(String)'
     */
    public void testGetCollationKey() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        CollationKey k1 = c.getCollationKey(s1);
        CollationKey k2 = c.getCollationKey(s2);
        assertTrue(k1.equals(k2));
        c.setStrength(Collator.TERTIARY);
        k1 = c.getCollationKey(s1);
        k2 = c.getCollationKey(s2);
        assertFalse(k1.equals(k2));
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.toString()'
     */
    public void testToString() {
        assertNotNull(Collator.getInstance().toString());
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.clone()'
     */
    public void testClone() {
        // tested above
    }

    /*
     * Test method for 'com.ibm.icu.text.Collator.equals(Object)'
     */
    public void testEqualsObject() {
        // tested above
    }
}

/*
 *******************************************************************************
 * Copyright (C) 2006-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Collator;

public class CollationKeyTest extends ICUTestCase {

    /*
     * Test method for 'com.ibm.icu.text.CollationKey.hashCode()'
     */
    public void testHashCode() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        CollationKey k1 = c.getCollationKey("This");
        CollationKey k2 = c.getCollationKey("this");
        c.setStrength(Collator.TERTIARY);
        CollationKey kn = c.getCollationKey("this");
        testEHCS(k1, k2, kn);
    }

    /*
     * Test method for 'com.ibm.icu.text.CollationKey.CollationKey(CollationKey)'
     */
    public void testCollationKey() {
        // implicitly tested everywhere
    }

    /*
     * Test method for 'com.ibm.icu.text.CollationKey.compareTo(CollationKey)'
     */
    public void testCompareToCollationKey() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        CollationKey k1 = c.getCollationKey("This");
        CollationKey k2 = c.getCollationKey("this");
        c.setStrength(Collator.TERTIARY);
        CollationKey k3 = c.getCollationKey("this");
        assertTrue(0 == k1.compareTo(k2));
        assertFalse(0 == k1.compareTo(k3));
    }

    /*
     * Test method for 'com.ibm.icu.text.CollationKey.compareTo(Object)'
     */
    public void testCompareToObject() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        CollationKey k1 = c.getCollationKey("This");
        CollationKey k2 = c.getCollationKey("this");
        assertTrue(0 == k1.compareTo((Object)k2));
    }

    /*
     * Test method for 'com.ibm.icu.text.CollationKey.equals(Object)'
     */
    public void testEqualsObject() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        CollationKey k1 = c.getCollationKey("This");
        CollationKey k2 = c.getCollationKey("this");
        assertTrue(k1.equals((Object)k2));
    }

    /*
     * Test method for 'com.ibm.icu.text.CollationKey.toString()'
     */
    public void testToString() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        CollationKey k1 = c.getCollationKey("This");
        assertNotNull(k1.toString());
    }

    /*
     * Test method for 'com.ibm.icu.text.CollationKey.getSourceString()'
     */
    public void testGetSourceString() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        CollationKey k1 = c.getCollationKey("This");
        assertEquals("This", k1.getSourceString());
    }

    /*
     * Test method for 'com.ibm.icu.text.CollationKey.toByteArray()'
     */
    public void testToByteArray() {
        Collator c = Collator.getInstance();
        c.setStrength(Collator.PRIMARY);
        CollationKey k1 = c.getCollationKey("This");
        byte[] key = k1.toByteArray();
        assertNotNull(key);
        assertTrue(0 < key.length);
    }
}

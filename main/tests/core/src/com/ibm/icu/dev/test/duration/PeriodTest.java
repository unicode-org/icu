/*
******************************************************************************
* Copyright (C) 2007-2010, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

// Copyright 2006 Google Inc.  All Rights Reserved.

package com.ibm.icu.dev.test.duration;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.duration.Period;
import com.ibm.icu.impl.duration.TimeUnit;

public class PeriodTest extends TestFmwk {

    /**
     * Invoke the tests.
     */
    public static void main(String[] args) {
        new PeriodTest().run(args);
    }

    public void testIsSet() {
        Period p = Period.at(0, TimeUnit.YEAR);
        assertTrue(null, p.isSet());
        assertTrue(null, p.isSet(TimeUnit.YEAR));
        assertFalse(null, p.isSet(TimeUnit.MONTH));
        assertEquals(null, 0f, p.getCount(TimeUnit.YEAR), .1f);
        p = p.omit(TimeUnit.YEAR);
        assertFalse(null, p.isSet(TimeUnit.YEAR));
    }

    public void testMoreLessThan() {
        Period p = Period.moreThan(1, TimeUnit.YEAR);
        assertTrue(null, p.isMoreThan());
        assertFalse(null, p.isLessThan());
        p = p.at();
        assertFalse(null, p.isMoreThan());
        assertFalse(null, p.isLessThan());
        p = p.lessThan();
        assertFalse(null, p.isMoreThan());
        assertTrue(null, p.isLessThan());
        p = p.moreThan();
        assertTrue(null, p.isMoreThan());
        assertFalse(null, p.isLessThan());

        p = Period.lessThan(1, TimeUnit.YEAR);
        assertFalse(null, p.isMoreThan());
        assertTrue(null, p.isLessThan());

        p = Period.at(1, TimeUnit.YEAR);
        assertFalse(null, p.isMoreThan());
        assertFalse(null, p.isLessThan());

        assertEquals(null, 1f, p.getCount(TimeUnit.YEAR), .1f);
    }

    public void testFuturePast() {
        Period p = Period.at(1, TimeUnit.YEAR).inFuture();
        assertTrue(null, p.isInFuture());
        p = p.inPast();
        assertFalse(null, p.isInFuture());
        p = p.inFuture(true);
        assertTrue(null, p.isInFuture());
        p = p.inFuture(false);
        assertFalse(null, p.isInFuture());
    }

    public void testAnd() {
        Period p = Period.at(1, TimeUnit.YEAR).and(3, TimeUnit.MONTH)
                .inFuture();
        assertTrue(null, p.isSet(TimeUnit.YEAR));
        assertTrue(null, p.isSet(TimeUnit.MONTH));
        assertEquals(null, 3f, p.getCount(TimeUnit.MONTH), .1f);
        p = p.and(2, TimeUnit.MONTH);
        assertEquals(null, 2f, p.getCount(TimeUnit.MONTH), .1f);
    }

    public void testInvalidCount() {
        try {
            Period.at(-1, TimeUnit.YEAR);
            fail("at -1");
        } catch (IllegalArgumentException e) {
            // passed
        }
        try {
            Period.moreThan(-1, TimeUnit.YEAR);
            fail("moreThan -1");
        } catch (IllegalArgumentException e) {
            // passed
        }
        try {
            Period.lessThan(-1, TimeUnit.YEAR);
            fail("lessThan -1");
        } catch (IllegalArgumentException e) {
            // passed
        }
        Period p = Period.at(1, TimeUnit.YEAR);
        try {
            p = p.and(-1, TimeUnit.MONTH);
            fail("and -1");
        } catch (IllegalArgumentException e) {
            // passed
        }
    }
}

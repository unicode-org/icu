/*
******************************************************************************
* Copyright (C) 2007-2008, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*/

// Copyright 2007 Google Inc.  All Rights Reserved.

package com.ibm.icu.dev.test.duration;

import com.ibm.icu.dev.test.TestFmwk;

import com.ibm.icu.impl.duration.BasicPeriodFormatterService;
import com.ibm.icu.impl.duration.PeriodBuilder;
import com.ibm.icu.impl.duration.TimeUnit;
import com.ibm.icu.impl.duration.Period;
import com.ibm.icu.impl.duration.PeriodBuilderFactory;
import com.ibm.icu.impl.duration.TimeUnitConstants;

public class PeriodBuilderFactoryTest extends TestFmwk implements TimeUnitConstants {
    private PeriodBuilderFactory pbf;

    private static final long[] approxDurations = {
      36525L*24*60*60*10, 3045*24*60*60*10L, 7*24*60*60*1000L, 24*60*60*1000L, 
      60*60*1000L, 60*1000L, 1000L, 1L
    };
    
    /**
     * Invoke the tests.
     */
    public static void main(String[] args) {
        new PeriodBuilderFactoryTest().run(args);
    }

    public void testSetAvailableUnitRange() {
        // sanity check, make sure by default all units are set
        pbf = BasicPeriodFormatterService.getInstance().newPeriodBuilderFactory();
        pbf.setLocale("en"); // in en locale, all units always available
        PeriodBuilder b = pbf.getSingleUnitBuilder();
        for (TimeUnit unit = YEAR; unit != null; unit = unit.smaller()) {
            Period p = b.create((long)(approxDurations[unit.ordinal()]*2.5));
            assertTrue(null, p.isSet(unit));
        }

        pbf.setAvailableUnitRange(MINUTE, MONTH);
        // units that are not available are never set
        b = pbf.getSingleUnitBuilder();
        for (TimeUnit unit = YEAR; unit != null; unit = unit.smaller()) {
            Period p = b.create((long)(approxDurations[unit.ordinal()]*2.5));
            assertEquals(null, p.isSet(unit), unit.ordinal() >= MONTH.ordinal() && unit.ordinal() <= MINUTE.ordinal());
        }

        // fixed unit builder returns null when unit is not available
        for (TimeUnit unit = YEAR; unit != null; unit = unit.smaller()) {
            b = pbf.getFixedUnitBuilder(unit);
            if (unit.ordinal() >= MONTH.ordinal() && unit.ordinal() <= MINUTE.ordinal()) {
                assertNotNull(null, b);
            } else {
                assertNull(null, b);
            }
        }

        // can't set empty range
        try {
            pbf.setAvailableUnitRange(MONTH, MINUTE);
            fail("set empty range");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    public void testSetUnitIsAvailable() {
        pbf = BasicPeriodFormatterService.getInstance().newPeriodBuilderFactory();
        pbf.setAvailableUnitRange(MONTH, MONTH);
        assertNotNull(null, pbf.getSingleUnitBuilder());
        assertNotNull(null, pbf.getOneOrTwoUnitBuilder());
        assertNotNull(null, pbf.getMultiUnitBuilder(2));

        // now no units are available, make sure we can't generate a builder
        pbf.setUnitIsAvailable(MONTH, false);
        assertNull(null, pbf.getSingleUnitBuilder());
        assertNull(null, pbf.getOneOrTwoUnitBuilder());
        assertNull(null, pbf.getMultiUnitBuilder(2));

        pbf.setUnitIsAvailable(DAY, true);
        assertNotNull(null, pbf.getSingleUnitBuilder());
        assertNotNull(null, pbf.getOneOrTwoUnitBuilder());
        assertNotNull(null, pbf.getMultiUnitBuilder(2));
    }
}

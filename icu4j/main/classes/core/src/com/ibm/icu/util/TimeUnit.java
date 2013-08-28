/*
 **************************************************************************
 * Copyright (C) 2008-2013, Google, International Business Machines
 * Corporation and others. All Rights Reserved.
 **************************************************************************
 */
package com.ibm.icu.util;


/**
 * Measurement unit for time units.
 * @see TimeUnitAmount
 * @see TimeUnit
 * @author markdavis
 * @stable ICU 4.0
 */
public class TimeUnit extends MeasureUnit {
    private static final long serialVersionUID = -2839973855554750484L;

    /** 
     * Supports selected time duration units
     */
    private final int index;

    // Total number of time units. Adjust as necessary.
    static final int TIME_UNIT_COUNT = 7;
    
    private static TimeUnit[] values = new TimeUnit[TIME_UNIT_COUNT];

    /** 
     * Constant value for supported time unit.
     * @stable ICU 4.0
     */
    public static TimeUnit
    SECOND = new TimeUnit("second", 6),
    MINUTE = new TimeUnit("minute", 5),
    HOUR = new TimeUnit("hour", 4),
    DAY = new TimeUnit("day", 3),
    WEEK = new TimeUnit("week", 2),
    MONTH = new TimeUnit("month", 1),
    YEAR = new TimeUnit("year", 0);
    

    // idx must be sequential and must order time units from largest to smallest.
    // e.g YEAR is 0; MONTH is 1; ...; SECOND is 6.
    private TimeUnit(String name, int idx) {
        super("duration", name);
        this.index = idx;
        values[idx] = this; // store in values array
    }

    /**
     * @return the available values
     * @stable ICU 4.0
     */
    public static TimeUnit[] values() {
        return values.clone();
    }

    // Returns the index for this TimeUnit. Something between 0 inclusive and
    // number of time units exclusive. Smaller time units have larger indexes.
    int getIndex() {
        return index;
    }
}

/*
 *******************************************************************************
 * Copyright (C) 2008, Google, International Business Machines Corporation and *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * Measurement unit for time units.
 * @see TimeUnitAmount
 * @see TimeUnit
 * @author markdavis
 * @draft ICU 4.0
 * @provisional This API might change or be removed in a future release.
 */
public class TimeUnit extends MeasureUnit {
    /** 
     * Supports selected time duration units
     */
    private String name;

    private static TimeUnit[] values = new TimeUnit[7]; // adjust count if new items are added
    private static int valueCount = 0;

    /** 
     * Constant value for supported time unit.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public static TimeUnit
    SECOND = new TimeUnit("second"),
    MINUTE = new TimeUnit("minute"),
    HOUR = new TimeUnit("hour"),
    DAY = new TimeUnit("day"),
    WEEK = new TimeUnit("week"),
    MONTH = new TimeUnit("month"),
    YEAR = new TimeUnit("year");

    private TimeUnit(String name) {
        this.name = name;
        values[valueCount++] = this; // store in values array
    }

    /**
     * @return the available values
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public static TimeUnit[] values() {
        return (TimeUnit[])values.clone();
    }

    /**
     * A string representation for debugging.
     * It is for debugging purpose. The value might change.
     * Please do not count on the value.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public String toString() {
        return name;
    }
}

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
    SECOND = new TimeUnit("SECOND"),
    MINUTE = new TimeUnit("MINUTE"),
    HOUR = new TimeUnit("HOUR"),
    DAY = new TimeUnit("DAY"),
    WEEK = new TimeUnit("WEEK"),
    MONTH = new TimeUnit("MONTH"),
    YEAR = new TimeUnit("YEAR");

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
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public String toString() {
        return name;
    }
}

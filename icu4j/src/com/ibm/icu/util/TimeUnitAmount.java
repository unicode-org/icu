/*
 *******************************************************************************
 * Copyright (C) 2008, Google, International Business Machines Corporation and *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * Express a duration as a time unit and number. Patterned after Currency.
 * <p>Immutable.
 * @see TimeUnitAmount
 * @see com.ibm.icu.text.TimeUnitFormat
 * @author markdavis
 * @draft ICU 4.0
 * @provisional This API might change or be removed in a future release.
 */
public class TimeUnitAmount extends Measure {

    /**
     * Create from a number and unit.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitAmount(Number number, TimeUnit unit) {
        super(number, unit);
    }

    /**
     * Create from a number and unit.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitAmount(double number, TimeUnit unit) {
        super(new Double(number), unit);
    }

    /**
     * Get the unit (convenience to avoid cast).
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnit getTimeUnit() {
        return (TimeUnit) getUnit();
    }
}

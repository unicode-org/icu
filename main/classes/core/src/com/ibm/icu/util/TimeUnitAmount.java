/*
 **************************************************************************
 * Copyright (C) 2008-2009, Google, International Business Machines
 * Corporation and others. All Rights Reserved.
 **************************************************************************
 */
package com.ibm.icu.util;

/**
 * Express a duration as a time unit and number. Patterned after Currency.
 * <p>Immutable.
 * @see TimeUnitAmount
 * @see com.ibm.icu.text.TimeUnitFormat
 * @author markdavis
 * @stable ICU 4.0
 */
public class TimeUnitAmount extends Measure {

    /**
     * Create from a number and unit.
     * @stable ICU 4.0
     */
    public TimeUnitAmount(Number number, TimeUnit unit) {
        super(number, unit);
    }

    /**
     * Create from a number and unit.
     * @stable ICU 4.0
     */
    public TimeUnitAmount(double number, TimeUnit unit) {
        super(new Double(number), unit);
    }

    /**
     * Get the unit (convenience to avoid cast).
     * @stable ICU 4.0
     */
    public TimeUnit getTimeUnit() {
        return (TimeUnit) getUnit();
    }
}

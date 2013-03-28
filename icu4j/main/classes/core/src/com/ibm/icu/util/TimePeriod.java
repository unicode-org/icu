/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.util.Iterator;

/**
 * TimePeriod represents a time period.  TimePeriod objects are immutable.
 * <p>Example usage:
 * <pre>
 *   Period p = Period.forAmounts(
 *       new TimeUnitAmount (TimeUnit.WEEK, 5),
 *       new TimeUnitAmount (TimeUnit.DAY, 40),
 *       new TimeUnitAmount (TimeUnit.HOUR, 2),
 *       new TimeUnitAmount (TimeUnit.SECOND, 8));
 * </pre>
 * @draft ICU 52
 */
public final class TimePeriod implements Iterable<TimeUnitAmount> {
       
    /**
     * Returns a new TimePeriod that matches the given time unit amounts.
     * @param amounts the TimeUnitAmounts. Must be non-empty. Normalization of the
     *   amounts and inclusion/exclusion of 0 amounts is up to caller.
     * @return the new TimePeriod object
     * @throws IllegalArgumentException if multiple TimeUnitAmount objects match
     * the same time unit or if any but the smallest TimeUnit has a fractional value
     * Or if amounts is empty.
     * @draft ICU 52
     */
    public static TimePeriod forAmounts(TimeUnitAmount ...amounts) {
            return null;
    }

    /**
     * Returns a new TimePeriod that matches the given time unit amounts.
     * @param amounts the TimeUnitAmounts. Must be non-empty. Normalization of the
     *   amounts and inclusion/exclusion of 0 amounts is up to caller.
     * @return the new TimePeriod object
     * @throws IllegalArgumentException if multiple TimeUnitAmount objects match
     * the same time unit or if any but the smallest TimeUnit has a fractional value
     * Or if amounts is empty.
     * @draft ICU 52
     */
    public static TimePeriod forAmounts(Iterable<TimeUnitAmount> amounts) {
        return null;
    }
        
    /**
     * Gets the value for a specific time unit.
     * @param timeUnit the time unit.
     * @return the TimeUnitAmount or null if no value is present for given TimeUnit.
     *  A non-existing value and a zero value are two different things.
     * @draft ICU 52
     */
    public TimeUnitAmount getAmount(TimeUnit timeUnit) {
        return null;
    }

    /**
     * Returned iterator iterates over all TimeUnitAmount objects in this object.
     * Iterated TimeUnitAmount objects are ordered from largest TimeUnit to
     * smallest TimeUnit.
     * @draft ICU 52
     */
    public Iterator<TimeUnitAmount> iterator() {
      return null;
    }

    /**
     * Returns the number of TimeUnitAmount objects in this object.
     */
    public int size() {
      return 0;
    }
    
    /**
     * Two TimePeriod objects are equal if they contain equal TimeUnitAmount objects.
     */
    @Override
    public boolean equals(Object rhs) {
        return true;
    }
    
    @Override
    public int hashCode() {
        return 0;
    }
}


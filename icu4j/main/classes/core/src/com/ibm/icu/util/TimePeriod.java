/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
 * @provisional This API might change or be removed in a future release.
 */
public final class TimePeriod implements Iterable<TimeUnitAmount> {
    
    private final TimeUnitAmount[] fields;
    private final int length;
    private final int hash;
       
    /**
     * Constructor.
     * @param amounts the TimeUnitAmounts. Must be non-empty. Normalization of the
     *   amounts and inclusion/exclusion of 0 amounts is up to caller. The Number
     *   object in each TimeUnitAmount must not change. Otherwise the created
     *   TimePeriod object may not work as expected.
     * @throws IllegalArgumentException if multiple TimeUnitAmount objects match
     * the same time unit or if any but the smallest TimeUnit has a fractional value
     * Or if amounts is empty.
     * @draft ICU 52
     * @provisional This API might change or be removed in a future release.
     */
    public TimePeriod(TimeUnitAmount ...amounts) {
        fields = new TimeUnitAmount[TimeUnit.TIME_UNIT_COUNT];
        int tempSize = 0;
        for (TimeUnitAmount tua : amounts) {
            int index = tua.getTimeUnit().getIndex();
            if (fields[index] != null) {
                throw new IllegalArgumentException(
                        "Only one TimeUnitAmount per unit allowed.");
            }
            fields[index] = tua;
            tempSize++;
        }
        length = tempSize;
        if (length == 0) {
            throw new IllegalArgumentException(
                    "There must be at least one TimeUnitAmount.");
        }
        boolean fractionalFieldEncountered = false;
        for (TimeUnitAmount tua : this) {
            if (fractionalFieldEncountered) {
                throw new IllegalArgumentException(
                    "Only the smallest time unit can have a fractional amount.");
            }
            double value = tua.getNumber().doubleValue();
            if (value != Math.floor(value)) {
                fractionalFieldEncountered = true;
            }
        }
        hash = computeHash(fields);  
    }
        
    /**
     * Gets the value for a specific time unit.
     * @param timeUnit the time unit.
     * @return the TimeUnitAmount or null if no value is present for given TimeUnit.
     *  A non-existing value and a zero value are two different things.
     * @draft ICU 52
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitAmount getAmount(TimeUnit timeUnit) {
        return fields[timeUnit.getIndex()];
    }

    /**
     * Returned iterator iterates over all TimeUnitAmount objects in this object.
     * Iterated TimeUnitAmount objects are ordered from largest TimeUnit to
     * smallest TimeUnit. Remove method on returned iterator throws an
     * UnsupportedOperationException.
     * @draft ICU 52
     * @provisional This API might change or be removed in a future release.
     */
    public Iterator<TimeUnitAmount> iterator() {
      return new TPIterator();
    }

    /**
     * Returns the number of TimeUnitAmount objects in this object.
     * @draft ICU 52
     */
    public int length() {
      return length;
    }
    
    /**
     * Two TimePeriod objects are equal if they contain equal TimeUnitAmount objects.
     * @draft ICU 52
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimePeriod)) {
            return false;
        }
        TimePeriod rhs = (TimePeriod) o;
        if (this.hash != rhs.hash) {
            return false;
        }
        return Arrays.equals(fields, rhs.fields);
    }

    /**
     * Overrides hashCode().
     * @draft ICU 52
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public int hashCode() {
        return hash;
    }
    
    private static int computeHash(TimeUnitAmount[] fields) {
        int result = 0;
        for (TimeUnitAmount amount : fields) {
            result *= 31;
            if (amount != null) {
                result += amount.hashCode();
            }
        }
        return result;
    }
    
    private class TPIterator implements Iterator<TimeUnitAmount> {
        
        private int index = 0;

        public boolean hasNext() {
            while (index < fields.length && fields[index] == null) {
                index++;
            }
            return index < fields.length;
        }

        public TimeUnitAmount next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return fields[index++];
        }

        public void remove() {
            throw new UnsupportedOperationException();           
        }
    } 
}


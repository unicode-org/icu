/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ibm.icu.math.BigDecimal;

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
    
    private final TimeUnitAmount[] fields;
    private final int size;
    private final int hash;
    
    private TimePeriod(TimeUnitAmount[] fields, int size, int hash) {
        this.fields = fields;
        this.size = size;
        this.hash = hash;
    }
       
    /**
     * Returns a new TimePeriod that matches the given time unit amounts.
     * @param amounts the TimeUnitAmounts. Must be non-empty. Normalization of the
     *   amounts and inclusion/exclusion of 0 amounts is up to caller. The Number
     *   in each TimeUnitAmount must either be a Byte, Short, Integer, Long, Float,
     *   Double, BigInteger, or BigDecimal or it must implement Cloneable and have
     *   a public clone method.
     * @return the new TimePeriod object
     * @throws IllegalArgumentException if multiple TimeUnitAmount objects match
     * the same time unit or if any but the smallest TimeUnit has a fractional value
     * Or if amounts is empty.
     * @draft ICU 52
     */
    public static TimePeriod forAmounts(TimeUnitAmount ...amounts) {
        return forAmounts(Arrays.asList(amounts));      
    }

    /**
     * Returns a new TimePeriod that matches the given time unit amounts.
     * @param amounts the TimeUnitAmounts. Must be non-empty. Normalization of the
     *   amounts and inclusion/exclusion of 0 amounts is up to caller. The Number
     *   in each TimeUnitAmount must either be a Byte, Short, Integer, Long, Float,
     *   Double, BigInteger, or BigDecimal or it must implement Cloneable and have
     *   a public clone method.
     * @return the new TimePeriod object
     * @throws IllegalArgumentException if multiple TimeUnitAmount objects match
     * the same time unit or if any but the smallest TimeUnit has a fractional value
     * Or if amounts is empty.
     * @draft ICU 52
     */
    public static TimePeriod forAmounts(Iterable<TimeUnitAmount> amounts) {
        TimeUnitAmount[] fields = new TimeUnitAmount[TimeUnit.TIME_UNIT_COUNT];
        int size = 0;
        for (TimeUnitAmount tua : amounts) {
            int index = tua.getTimeUnit().getIndex();
            if (fields[index] != null) {
                throw new IllegalArgumentException(
                        "Only one TimeUnitAmount per unit allowed.");
            }
            // This line is necessary to guarantee immutability of the TimePeriod
            // class. A Number object, which is in TimeUnitAmount, need not be immutable,
            // but Double is immutable.
            fields[index] = cloneIfNecessary(tua);
            size++;
        }
        if (size == 0) {
            throw new IllegalArgumentException(
                    "There must be at least one TimeUnitAmount.");
        }
        TimePeriod result = new TimePeriod(fields, size, computeHash(fields));
        boolean fractionalFieldEncountered = false;
        for (TimeUnitAmount tua : result) {
            if (fractionalFieldEncountered) {
                throw new IllegalArgumentException(
                    "Only the smallest time unit can have a fractional amount.");
            }
            double value = tua.getNumber().doubleValue();
            if (value != Math.floor(value)) {
                fractionalFieldEncountered = true;
            }
        }
        return result;
    }
        
    /**
     * Gets the value for a specific time unit.
     * @param timeUnit the time unit.
     * @return the TimeUnitAmount or null if no value is present for given TimeUnit.
     *  A non-existing value and a zero value are two different things.
     * @draft ICU 52
     */
    public TimeUnitAmount getAmount(TimeUnit timeUnit) {
        return cloneIfNecessary(fields[timeUnit.getIndex()]);
    }

    /**
     * Returned iterator iterates over all TimeUnitAmount objects in this object.
     * Iterated TimeUnitAmount objects are ordered from largest TimeUnit to
     * smallest TimeUnit. Remove method on returned iterator throws an
     * UnsupportedOperationException.
     * @draft ICU 52
     */
    public Iterator<TimeUnitAmount> iterator() {
      return new TPIterator();
    }

    /**
     * Returns the number of TimeUnitAmount objects in this object.
     */
    public int size() {
      return size;
    }
    
    /**
     * Two TimePeriod objects are equal if they contain equal TimeUnitAmount objects.
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
    
    private static TimeUnitAmount cloneIfNecessary(TimeUnitAmount tua) {
        if (tua == null) {
            return null;
        }
        Number number = tua.getNumber();
        if (number instanceof Double || number instanceof Integer ||
            number instanceof Long || number instanceof Float ||
            number instanceof Byte || number instanceof Short ||
            number instanceof BigInteger || number instanceof BigDecimal) {
          return tua;            
        }
        return new TimeUnitAmount(cloneNumber(number), tua.getTimeUnit());
    }
    
    private static Number cloneNumber(Number number) {
        // Since Number doesn't implement Cloneable, we have to use reflection
        // to find the clone() method. If this method throws an exception, it
        // means that the subclass of Number can't be cloned.
        Class<? extends Number> clz = number.getClass();
        Method cloneMethod;
        try {
            cloneMethod = clz.getMethod("clone");
            cloneMethod.setAccessible(true);
            return (Number) cloneMethod.invoke(number);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
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


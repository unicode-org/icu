/*
**********************************************************************
* Copyright (c) 2004-2009, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: April 20, 2004
* Since: ICU 3.0
**********************************************************************
*/
package com.ibm.icu.util;

import java.lang.Number;

/**
 * An amount of a specified unit, consisting of a Number and a Unit.
 * For example, a length measure consists of a Number and a length
 * unit, such as feet or meters.  This is an abstract class.
 * Subclasses specify a concrete Unit type.
 *
 * <p>Measure objects are parsed and formatted by subclasses of
 * MeasureFormat.
 *
 * <p>Measure objects are immutable.
 *
 * @see java.lang.Number
 * @see com.ibm.icu.util.MeasureUnit
 * @see com.ibm.icu.text.MeasureFormat
 * @author Alan Liu
 * @stable ICU 3.0
 */
public abstract class Measure {
    
    private Number number;

    private MeasureUnit unit;

    /**
     * Constructs a new object given a number and a unit.
     * @param number the number
     * @param unit the unit
     * @stable ICU 3.0
     */
    protected Measure(Number number, MeasureUnit unit) {
        if (number == null || unit == null) {
            throw new NullPointerException();
        }
        this.number = number;
        this.unit = unit;
    }
    
    /**
     * Returns true if the given object is equal to this object.
     * @return true if this object is equal to the given object
     * @stable ICU 3.0
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        try {
            Measure m = (Measure) obj;
            return unit.equals(m.unit) && numbersEqual(number, m.number);
        } catch (ClassCastException e) {
            return false;
        }
    }
    
    /*
     * See if two numbers are identical or have the same double value.
     * @param a A number
     * @param b Another number to be compared with
     * @return Returns true if two numbers are identical or have the same double value.
     */
    // TODO improve this to catch more cases (two different longs that have same double values, BigDecimals, etc)
    private static boolean numbersEqual(Number a, Number b) {
        if (a.equals(b)) {
            return true;
        }
        if (a.doubleValue() == b.doubleValue()) {
            return true;
        }
        return false;
    }

    /**
     * Returns a hashcode for this object.
     * @return a 32-bit hash
     * @stable ICU 3.0
     */
    public int hashCode() {
        return number.hashCode() ^ unit.hashCode();
    }

    /**
     * Returns a string representation of this object.
     * @return a string representation consisting of the ISO currency
     * code together with the numeric amount
     * @stable ICU 3.0
     */
    public String toString() {
        return number.toString() + ' ' + unit.toString();
    }

    /**
     * Returns the numeric value of this object.
     * @return this object's Number
     * @stable ICU 3.0
     */
    public Number getNumber() {
        return number;
    }

    /**
     * Returns the unit of this object.
     * @return this object's Unit
     * @stable ICU 3.0
     */
    public MeasureUnit getUnit() {
        return unit;
    }
}

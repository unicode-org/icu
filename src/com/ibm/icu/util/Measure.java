/*
**********************************************************************
* Copyright (c) 2004, International Business Machines
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
 * @draft ICU 3.0
 * @deprecated This is a draft API and might change in a future release of ICU.
 */
public abstract class Measure {
    
    private Number number;

    private MeasureUnit unit;

    /**
     * Constructs a new object given a number and a unit.
     * @param number the number
     * @param unit the unit
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
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
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        try {
            Measure m = (Measure) obj;
            return number.equals(m.number) && unit.equals(m.unit);
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Returns a hashcode for this object.
     * @return a 32-bit hash
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public int hashCode() {
        return number.hashCode() ^ unit.hashCode();
    }

    /**
     * Returns a string representation of this object.
     * @return a string representation consisting of the ISO currency
     * code together with the numeric amount
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public String toString() {
        return number.toString() + ' ' + unit.toString();
    }

    /**
     * Returns the numeric value of this object.
     * @return this object's Number
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public Number getNumber() {
        return number;
    }

    /**
     * Returns the unit of this object.
     * @return this object's Unit
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public MeasureUnit getUnit() {
        return unit;
    }
}

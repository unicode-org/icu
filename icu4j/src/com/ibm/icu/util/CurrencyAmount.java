/*
**********************************************************************
* Copyright (c) 2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: April 12, 2004
* Since: ICU 3.0
**********************************************************************
*/
package com.ibm.icu.util;

import java.lang.Number;

/**
 * An amount of currency, consisting of a Number and a Currency.  This
 * is a subclass of Number, and delegates all Number API to the
 * enclosed Number object.  CurrencyAmount objects are immutable.
 *
 * <p>NumberFormat formats and parses CurrencyAmount objects.  During
 * formatting, the currency of the object is used to determine the
 * currency display name or symbol, the rounding, and the fraction
 * digit counts.  During parsing, if a valid currency is parsed, it is
 * stored in the parsed CurrencyAmount object.
 *
 * @see java.lang.Number
 * @see Currency
 * @see com.ibm.icu.text.NumberFormat
 * @author Alan Liu
 * @draft ICU 3.0
 */
public class CurrencyAmount extends Number {
    
    private Number number;

    private Currency currency;

    /**
     * Constructs a new object given a number and a currency.
     * @param number the number
     * @param currency the currency
     * @draft ICU 3.0
     */
    public CurrencyAmount(Number number, Currency currency) {
        if (number == null || currency == null) {
            throw new NullPointerException();
        }
        this.number = number;
        this.currency = currency;
    }

    /**
     * Constructs a new object given a double value and a currency.
     * @param number a double value
     * @param currency the currency
     * @draft ICU 3.0
     */
    public CurrencyAmount(double number, Currency currency) {
        this(new Double(number), currency);
    }    
    
    /**
     * Returns true if the given object is equal to this object.
     * @return true if this object is equal to the given object
     * @draft ICU 3.0
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        try {
            CurrencyAmount amt = (CurrencyAmount) obj;
            return number.equals(amt.number) &&
                currency.equals(amt.currency);
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Returns a hashcode for this object.
     * @return a 32-bit hash
     * @draft ICU 3.0
     */
    public int hashCode() {
        return number.hashCode() ^ currency.hashCode();
    }

    /**
     * Returns a string representation of this object.
     * @return a string representation consisting of the ISO currency
     * code together with the numeric amount
     * @draft ICU 3.0
     */
    public String toString() {
        return currency.getCurrencyCode() + ' ' + number;
    }

    /**
     * Returns the numeric value of this object.
     * @return this object's Number
     * @draft ICU 3.0
     */
    public Number getNumber() {
        return number;
    }

    /**
     * Returns the currency of this object.
     * @return this object's Currency
     * @draft ICU 3.0
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * Returns the value of this current amount as an int.
     * @return getNumber().intValue()
     * @draft ICU 3.0
     */
    public int intValue() {
        return number.intValue();
    }

    /**
     * Returns the value of this current amount as an long.
     * @return getNumber().longValue()
     * @draft ICU 3.0
     */
    public long longValue() {
        return number.longValue();
    }

    /**
     * Returns the value of this current amount as an float.
     * @return getNumber().floatValue()
     * @draft ICU 3.0
     */
    public float floatValue() {
        return number.floatValue();
    }

    /**
     * Returns the value of this current amount as an double.
     * @return getNumber().doubleValue()
     * @draft ICU 3.0
     */
    public double doubleValue() {
        return number.doubleValue();
    }
}

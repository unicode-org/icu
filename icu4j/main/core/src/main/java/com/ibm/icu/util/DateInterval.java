// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 *   Copyright (C) 2008-2009, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.io.Serializable;


/**
 * This class represents date interval.
 * It is a pair of long representing from date 1 to date 2.
 * @stable ICU 4.0
 */
public final class DateInterval implements Serializable {

    private static final long serialVersionUID = 1;

    private final long fromDate;
    private final long toDate;

    /** 
     * Constructor given from date and to date.
     * @param from      The from date in date interval.
     * @param to        The to date in date interval.
     * @stable ICU 4.0
     */
    public DateInterval(long from, long to)
    {
        fromDate = from;
        toDate = to;
    }

    /** 
     * Get the from date.
     * @return  the from date in dateInterval.
     * @stable ICU 4.0
     */
    public long getFromDate()
    {
        return fromDate;
    }

    /** 
     * Get the to date.
     * @return  the to date in dateInterval.
     * @stable ICU 4.0
     */
    public long getToDate()
    {
        return toDate;
    }

    /**
     * Override equals
     * @stable ICU 4.0
     */
    public boolean equals(Object a) {
        if ( a instanceof DateInterval ) {
            DateInterval di = (DateInterval)a;
            return fromDate == di.fromDate && toDate == di.toDate;
        }
        return false;
    }

    /**
     * Override hashcode
     * @stable ICU 4.0
     */
    public int hashCode() {
        return (int)(fromDate + toDate);
    }

    /**
     * Override toString
     * @stable ICU 4.0
     */
    public String toString() {
        return String.valueOf(fromDate) + " " + String.valueOf(toDate);
    }

} // end class DateInterval

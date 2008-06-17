/*
 *******************************************************************************
 *   Copyright (C) 2008, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.io.Serializable;


/**
 * This class represents date interval.
 * It is a pair of long representing from date 1 to date 2.
 * @draft ICU 4.0
 * @provisional This API might change or be removed in a future release.
 */
public final class DateInterval implements Serializable {

    private static final long serialVersionUID = 1;

    private final long fromDate;
    private final long toDate;

    /** 
     * Constructor given from date and to date.
     * @param from      The from date in date interval.
     * @param to        The to date in date interval.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public DateInterval(long from, long to)
    {
        fromDate = from;
        toDate = to;
    }

    /** 
     * Get the from date.
     * @return  the from date in dateInterval.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public long getFromDate()
    {
        return fromDate;
    }

    /** 
     * Get the to date.
     * @return  the to date in dateInterval.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public long getToDate()
    {
        return toDate;
    }

    /**
     * Override equals
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public int hashCode() {
        return (int)(fromDate + toDate);
    }

    /**
     * Override toString
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public String toString() {
        return String.valueOf(fromDate) + " " + String.valueOf(toDate);
    }

} // end class DateInterval

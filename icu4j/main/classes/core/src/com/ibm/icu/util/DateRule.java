// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.util;

import java.util.Date;

/**
 * <b>Note:</b> The Holiday framework is a technology preview.
 * Despite its age, is still draft API, and clients should treat it as such.
 *
 * DateRule is an interface for calculating the date of an event.
 * It supports both recurring events and those which occur only once.
 * DateRule is useful for storing information about holidays,
 * Daylight Savings Time rules, and other events such as meetings.
 *
 * @see SimpleDateRule
 * @draft ICU 2.8 (retainAll)
 * @provisional This API might change or be removed in a future release.
 */
public interface DateRule
{
    /**
     * Return the first occurrance of the event represented by this rule
     * that is on or after the given start date.
     *
     * @param start Only occurrances on or after this date are returned.
     *
     * @return      The date on which this event occurs, or null if it
     *              does not occur on or after the start date.
     *
     * @see #firstBetween
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    abstract public Date    firstAfter(Date start);

    /**
     * Return the first occurrance of the event represented by this rule
     * that is on or after the given start date and before the given
     * end date.
     *
     * @param start Only occurrances on or after this date are returned.
     * @param end   Only occurrances before this date are returned.
     *
     * @return      The date on which this event occurs, or null if it
     *              does not occur between the start and end dates.
     *
     * @see #firstAfter
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    abstract public Date    firstBetween(Date start, Date end);

    /**
     * Checks whether this event occurs on the given date.  This does
     * <em>not</em> take time of day into account; instead it checks
     * whether this event and the given date are on the same day.
     * This is useful for applications such as determining whether a given
     * day is a holiday.
     *
     * @param date  The date to check.
     * @return      true if this event occurs on the given date.
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    abstract public boolean isOn(Date date);

    /**
     * Check whether this event occurs at least once between the two
     * dates given.
     * @draft ICU 2.8
     * @provisional This API might change or be removed in a future release.
     */
    abstract public boolean isBetween(Date start, Date end);
}

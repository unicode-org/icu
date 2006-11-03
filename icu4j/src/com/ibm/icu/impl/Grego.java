/**
 *******************************************************************************
 * Copyright (C) 2003-2006, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 * Partial port from ICU4C's Grego class in i18n/gregoimp.h.
 *
 * Methods ported, or moved here from OlsonTimeZone, initially
 * for work on Jitterbug 5470:
 *   tzdata2006n Brazil incorrect fall-back date 2009-mar-01
 * Only the methods necessary for that work are provided - this is not a full
 * port of ICU4C's Grego class (yet).
 *
 * These utilities are used by both OlsonTimeZone and SimpleTimeZone.
 */
package com.ibm.icu.impl;

/**
 * A utility class providing proleptic Gregorian calendar functions
 * used by time zone and calendar code.  Do not instantiate.
 *
 * Note:  Unlike GregorianCalendar, all computations performed by this
 * class occur in the pure proleptic GregorianCalendar.
 */
public class Grego {
    /**
     * Return true if the given year is a leap year.
     * @param year Gregorian year, with 0 == 1 BCE, -1 == 2 BCE, etc.
     * @return true if the year is a leap year
     */
    public static final boolean isLeapYear(int year) {
        // year&0x3 == year%4
        return ((year&0x3) == 0) && ((year%100 != 0) || (year%400 == 0));
    }

    private static final int[] MONTH_LENGTH = new int[] {
        31,28,31,30,31,30,31,31,30,31,30,31,
        31,29,31,30,31,30,31,31,30,31,30,31
    };

    /**
     * Return the number of days in the given month.
     * @param year Gregorian year, with 0 == 1 BCE, -1 == 2 BCE, etc.
     * @param month 0-based month, with 0==Jan
     * @return the number of days in the given month
     */
    public static final int monthLength(int year, int month) {
        return MONTH_LENGTH[month + (isLeapYear(year) ? 12 : 0)];
    }

    /**
     * Return the length of a previous month of the Gregorian calendar.
     * @param y the extended year
     * @param m the 0-based month number
     * @return the number of days in the month previous to the given month
     */
    public static final int previousMonthLength(int y, int m) {
        return (m > 0) ? monthLength(y, m-1) : 31;
    }
}

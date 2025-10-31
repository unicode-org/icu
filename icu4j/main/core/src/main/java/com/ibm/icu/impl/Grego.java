// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/**
 * ****************************************************************************** Copyright (C)
 * 2003-2014, International Business Machines Corporation and others. All Rights Reserved.
 * ****************************************************************************** Partial port from
 * ICU4C's Grego class in i18n/gregoimp.h.
 *
 * <p>Methods ported, or moved here from OlsonTimeZone, initially for work on Jitterbug 5470:
 * tzdata2006n Brazil incorrect fall-back date 2009-mar-01 Only the methods necessary for that work
 * are provided - this is not a full port of ICU4C's Grego class (yet).
 *
 * <p>These utilities are used by both OlsonTimeZone and SimpleTimeZone.
 */
package com.ibm.icu.impl;

import java.util.Locale;

/**
 * A utility class providing proleptic Gregorian calendar functions used by time zone and calendar
 * code. Do not instantiate.
 *
 * <p>Note: Unlike GregorianCalendar, all computations performed by this class occur in the pure
 * proleptic GregorianCalendar.
 */
public class Grego {

    // Max/min milliseconds
    public static final long MIN_MILLIS = -184303902528000000L;
    public static final long MAX_MILLIS = 183882168921600000L;

    public static final int MILLIS_PER_SECOND = 1000;
    public static final int MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
    public static final int MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
    public static final int MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

    //  January 1, 1 CE Gregorian
    private static final int JULIAN_1_CE = 1721426;

    //  January 1, 1970 CE Gregorian
    private static final int JULIAN_1970_CE = 2440588;

    private static final int[] MONTH_LENGTH =
            new int[] {
                31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31,
                31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
            };

    private static final int[] DAYS_BEFORE =
            new int[] {
                0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334,
                0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335
            };

    /**
     * Return true if the given year is a leap year.
     *
     * @param year Gregorian year, with 0 == 1 BCE, -1 == 2 BCE, etc.
     * @return true if the year is a leap year
     */
    public static final boolean isLeapYear(int year) {
        // year&0x3 == year%4
        return ((year & 0x3) == 0) && ((year % 100 != 0) || (year % 400 == 0));
    }

    /**
     * Return the number of days in the given month.
     *
     * @param year Gregorian year, with 0 == 1 BCE, -1 == 2 BCE, etc.
     * @param month 0-based month, with 0==Jan
     * @return the number of days in the given month
     */
    public static final int monthLength(int year, int month) {
        return MONTH_LENGTH[month + (isLeapYear(year) ? 12 : 0)];
    }

    /**
     * Return the length of a previous month of the Gregorian calendar.
     *
     * @param year Gregorian year, with 0 == 1 BCE, -1 == 2 BCE, etc.
     * @param month 0-based month, with 0==Jan
     * @return the number of days in the month previous to the given month
     */
    public static final int previousMonthLength(int year, int month) {
        return (month > 0) ? monthLength(year, month - 1) : 31;
    }

    /**
     * Convert a year, month, and day-of-month, given in the proleptic Gregorian calendar, to 1970
     * epoch days.
     *
     * @param year Gregorian year, with 0 == 1 BCE, -1 == 2 BCE, etc.
     * @param month 0-based month, with 0==Jan
     * @param dom 1-based day of month
     * @return the day number, with day 0 == Jan 1 1970
     */
    public static long fieldsToDay(int year, int month, int dom) {
        int y = year - 1;
        long julian =
                365 * y
                        + floorDivide(y, 4)
                        + (JULIAN_1_CE - 3)
                        + // Julian cal
                        floorDivide(y, 400)
                        - floorDivide(y, 100)
                        + 2
                        + // => Gregorian cal
                        DAYS_BEFORE[month + (isLeapYear(year) ? 12 : 0)]
                        + dom; // => month/dom
        return julian - JULIAN_1970_CE; // JD => epoch day
    }

    /**
     * Return the day of week on the 1970-epoch day
     *
     * @param day the 1970-epoch day (integral value)
     * @return the day of week
     */
    public static int dayOfWeek(long day) {
        Pair<Long, Integer> result = floorDivideAndRemainer(day + 5 /* Calendar.THURSDAY */, 7);
        int dayOfWeek = result.second;
        dayOfWeek = (dayOfWeek == 0) ? 7 : dayOfWeek;
        return dayOfWeek;
    }

    public static Pair<Integer, Integer> dayToYear(long day) {
        // Convert from 1970 CE epoch to 1 CE epoch (Gregorian calendar)
        day += JULIAN_1970_CE - JULIAN_1_CE;
        Pair<Long, Integer> n400 = floorDivideAndRemainer(day, 146097);
        Pair<Long, Integer> n100 = floorDivideAndRemainer(n400.second, 36524);
        Pair<Long, Integer> n4 = floorDivideAndRemainer(n100.second, 1461);
        Pair<Long, Integer> n1 = floorDivideAndRemainer(n4.second, 365);

        int year = (int) (400 * n400.first + 100 * n100.first + 4 * n4.first + n1.first);
        int dayOfYear = n1.second;
        if (n100.first == 4 || n1.first == 4) {
            dayOfYear = 365; // Dec 31 at end of 4- or 400-yr cycle
        } else {
            ++year;
        }
        dayOfYear++; // 1-based day of year
        return new Pair<Integer, Integer>(year, dayOfYear);
    }

    public static int[] dayToFields(long day, int[] fields) {
        if (fields == null || fields.length < 5) {
            fields = new int[5];
        }
        Pair<Integer, Integer> result = dayToYear(day);
        int year = result.first;
        int dayOfYear = result.second;
        // Convert from 1970 CE epoch to 1 CE epoch (Gregorian calendar)
        day += JULIAN_1970_CE - JULIAN_1_CE;

        boolean isLeap = isLeapYear(year);
        int correction = 0;
        int march1 = isLeap ? 60 : 59; // zero-based DOY for March 1
        if (dayOfYear > march1) {
            correction = isLeap ? 1 : 2;
        }
        int month = (12 * (dayOfYear - 1 + correction) + 6) / 367; // zero-based month
        int dayOfMonth = dayOfYear - DAYS_BEFORE[isLeap ? month + 12 : month]; // one-based DOM
        int dayOfWeek = (int) ((day + 2) % 7); // day 0 is Monday(2)
        if (dayOfWeek < 1 /* Sunday */) {
            dayOfWeek += 7;
        }

        fields[0] = year;
        fields[1] = month;
        fields[2] = dayOfMonth;
        fields[3] = dayOfWeek;
        fields[4] = dayOfYear;

        return fields;
    }

    /*
     * Convert long time to date/time fields
     *
     * result[0] : year
     * result[1] : month
     * result[2] : dayOfMonth
     * result[3] : dayOfWeek
     * result[4] : dayOfYear
     * result[5] : millisecond in day
     */
    public static int[] timeToFields(long time, int[] fields) {
        if (fields == null || fields.length < 6) {
            fields = new int[6];
        }
        Pair<Long, Integer> result =
                floorDivideAndRemainer(time, 24 * 60 * 60 * 1000 /* milliseconds per day */);
        dayToFields(result.first, fields);
        fields[5] = result.second;
        return fields;
    }

    public static int timeToYear(long time) {
        return dayToYear(
                        floorDivideAndRemainer(time, 24 * 60 * 60 * 1000 /* milliseconds per day */)
                                .first)
                .first;
    }

    public static long floorDivide(long numerator, long denominator) {
        // We do this computation in order to handle
        // a numerator of Long.MIN_VALUE correctly
        return (numerator >= 0) ? numerator / denominator : ((numerator + 1) / denominator) - 1;
    }

    private static Pair<Long, Integer> floorDivideAndRemainer(long numerator, int denominator) {
        if (numerator >= 0) {
            return new Pair<Long, Integer>(
                    floorDivide(numerator, denominator), (int) (numerator % denominator));
        }
        long quotient = floorDivide(numerator, denominator);
        return new Pair<Long, Integer>(quotient, (int) (numerator - (quotient * denominator)));
    }

    /*
     * Returns the ordinal number for the specified day of week in the month.
     * The valid return value is 1, 2, 3, 4 or -1.
     */
    public static int getDayOfWeekInMonth(int year, int month, int dayOfMonth) {
        int weekInMonth = (dayOfMonth + 6) / 7;
        if (weekInMonth == 4) {
            if (dayOfMonth + 7 > monthLength(year, month)) {
                weekInMonth = -1;
            }
        } else if (weekInMonth == 5) {
            weekInMonth = -1;
        }
        return weekInMonth;
    }

    /**
     * Convenient method for formatting time to ISO 8601 style date string.
     *
     * @param time long time
     * @return ISO-8601 date string
     */
    public static String timeToString(long time) {
        int[] fields = timeToFields(time, null);
        int millis = fields[5];
        int hour = millis / MILLIS_PER_HOUR;
        millis = millis % MILLIS_PER_HOUR;
        int min = millis / MILLIS_PER_MINUTE;
        millis = millis % MILLIS_PER_MINUTE;
        int sec = millis / MILLIS_PER_SECOND;
        millis = millis % MILLIS_PER_SECOND;

        return String.format(
                (Locale) null,
                "%04d-%02d-%02dT%02d:%02d:%02d.%03dZ",
                fields[0],
                fields[1] + 1,
                fields[2],
                hour,
                min,
                sec,
                millis);
    }
}

/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/SimpleDateRule.java,v $ 
 * $Date: 2003/09/04 01:00:59 $ 
 * $Revision: 1.9 $
 *
 *****************************************************************************************
 */

package com.ibm.icu.util;

import java.util.Date;
import java.util.SimpleTimeZone;

/**
 * Simple implementation of DateRule.
 * @draft ICU 2.2
 */
public class SimpleDateRule implements DateRule
{
    /**
     * Construct a rule for a fixed date within a month
     *
     * @param month         The month in which this rule occurs (0-based).
     * @param dayOfMonth    The date in that month (1-based).
     * @draft ICU 2.2
     */
    public SimpleDateRule(int month, int dayOfMonth)
    {
        this.month      = month;
        this.dayOfMonth = dayOfMonth;
        this.dayOfWeek  = 0;
    }

    /**
     * Construct a rule for a weekday within a month, e.g. the first Monday.
     *
     * @param month         The month in which this rule occurs (0-based).
     * @param dayOfMonth    A date within that month (1-based).
     * @param dayOfWeek     The day of the week on which this rule occurs.
     * @param after         If true, this rule selects the first dayOfWeek
     *                      on or after dayOfMonth.  If false, the rule selects
     *                      the first dayOfWeek on or before dayOfMonth.
     * @draft ICU 2.2
     */
    public SimpleDateRule(int month, int dayOfMonth, int dayOfWeek, boolean after)
    {
        this.month      = month;
        this.dayOfMonth = dayOfMonth;
        this.dayOfWeek  = after ? dayOfWeek : -dayOfWeek;
    }

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
     * @draft ICU 2.2
     */
    public Date firstAfter(Date start)
    {
        if (startDate != null && start.before(startDate)) {
            start = startDate;
        }
        return doFirstBetween(start, endDate);
    }

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
     * @draft ICU 2.2
     */
    public Date firstBetween(Date start, Date end)
    {
        // Pin to the min/max dates for this rule
        if (startDate != null && start.before(startDate)) {
            start = startDate;
        }
        if (endDate != null && end.after(endDate)) {
            end = endDate;
        }
        return doFirstBetween(start, end);
    }

    /**
     * Checks whether this event occurs on the given date.  This does
     * <em>not</em> take time of day into account; instead it checks
     * whether this event and the given date are on the same day.
     * This is useful for applications such as determining whether a given
     * day is a holiday.
     *
     * @param date  The date to check.
     * @return      true if this event occurs on the given date.
     * @draft ICU 2.2
     *
     */
    public boolean isOn(Date date)
    {
        if (startDate != null && date.before(startDate)) {
            return false;
        }
        if (endDate != null && date.after(endDate)) {
            return false;
        }

        Calendar c = calendar;

        synchronized(c) {
            c.setTime(date);

            int dayOfYear = c.get(Calendar.DAY_OF_YEAR);

            c.setTime(computeInYear(c.get(Calendar.YEAR), c));

            //System.out.println("  isOn: dayOfYear = " + dayOfYear);
            //System.out.println("        holiday   = " + c.get(Calendar.DAY_OF_YEAR));

            return c.get(Calendar.DAY_OF_YEAR) == dayOfYear;
        }
    }

    /**
     * Check whether this event occurs at least once between the two
     * dates given.
     * @draft ICU 2.2
     */
    public boolean isBetween(Date start, Date end)
    {
        return firstBetween(start, end) != null; // TODO: optimize?
    }

    private Date doFirstBetween(Date start, Date end)
    {
        Calendar c = calendar;

        synchronized(c) {
            c.setTime(start);

            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);

            // If the rule is earlier in the year than the start date
            // we have to go to the next year.
            if (month > this.month) {
                year++;
            }

            // Figure out when the rule lands in the given year
            Date result = computeInYear(year, c);

            // If the rule is in the same month as the start date, it's possible
            // to get a result that's before the start.  If so, go to next year.
            if (month == this.month && result.before(start)) {
                result = computeInYear(year+1, c);
            }

            if (end != null && result.after(end)) {
                return null;
            }
            return result;
        }
    }

    private Date computeInYear(int year, Calendar c)
    {
        if (c == null) c = calendar;

        synchronized(c) {
            c.clear();
            c.set(Calendar.ERA, c.getMaximum(Calendar.ERA));
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DATE, dayOfMonth);

            //System.out.println("     computeInYear: start at " + c.getTime().toString());

            if (dayOfWeek != 0) {
                c.setTime(c.getTime());        // JDK 1.1.2 workaround
                int weekday = c.get(Calendar.DAY_OF_WEEK);

                //System.out.println("                    weekday = " + weekday);
                //System.out.println("                    dayOfYear = " + c.get(Calendar.DAY_OF_YEAR));

                int delta = 0;
                if (dayOfWeek > 0) {
                    // We want the first occurrance of the given day of the week
                    // on or after the specified date in the month.
                    delta = (dayOfWeek - weekday + 7) % 7;
                }
                else if (dayOfWeek < 0) {
                    // We want the first occurrance of the (-dayOfWeek)
                    // on or before the specified date in the month.
                    delta = -((dayOfWeek + weekday + 7) % 7);
                }
                //System.out.println("                    adding " + delta + " days");
                c.add(Calendar.DATE, delta);
            }

            return c.getTime();
        }
    }

    /**
     * @draft ICU 2.2
     */
    public void setCalendar(Calendar c) {
        calendar = c;
    }

    static GregorianCalendar gCalendar = new GregorianCalendar(new SimpleTimeZone(0, "UTC"));

    Calendar calendar = gCalendar;

    private int     month;
    private int     dayOfMonth;
    private int     dayOfWeek;

    private Date    startDate = null;
    private Date    endDate = null;
};

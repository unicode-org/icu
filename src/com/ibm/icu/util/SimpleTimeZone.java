/*
*   Copyright (C) 1996-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*/

package com.ibm.icu.util;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * <code>SimpleTimeZone</code> is a concrete subclass of <code>TimeZone</code>
 * that represents a time zone for use with a Gregorian calendar. This
 * class does not handle historical changes.
 *
 * <P>
 * Use a negative value for <code>dayOfWeekInMonth</code> to indicate that
 * <code>SimpleTimeZone</code> should count from the end of the month backwards.
 * For example, Daylight Savings Time ends at the last
 * (dayOfWeekInMonth = -1) Sunday in October, at 2 AM in standard time.
 *
 * @see      Calendar
 * @see      GregorianCalendar
 * @see      TimeZone
 * @author   David Goldsmith, Mark Davis, Chen-Lieh Huang, Alan Liu
 * @stable ICU 2.0
 */
public class SimpleTimeZone extends TimeZone {
    /**
     * Constructs a SimpleTimeZone with the given base time zone offset from GMT
     * and time zone ID. Timezone IDs can be obtained from
     * TimeZone.getAvailableIDs. Normally you should use TimeZone.getDefault to
     * construct a TimeZone.
     *
     * @param rawOffset  The given base time zone offset to GMT.
     * @param ID         The time zone ID which is obtained from
     *                   TimeZone.getAvailableIDs.
     * @stable ICU 2.0
     */
    public SimpleTimeZone(int rawOffset, String ID)
    {
        this.rawOffset = rawOffset;
        setID (ID);
        dstSavings = millisPerHour; // In case user sets rules later
    }

    /**
     * Construct a SimpleTimeZone with the given base time zone offset from
     * GMT, time zone ID, time to start and end the daylight time. Timezone IDs
     * can be obtained from TimeZone.getAvailableIDs. Normally you should use
     * TimeZone.getDefault to create a TimeZone. For a time zone that does not
     * use daylight saving time, do not use this constructor; instead you should
     * use SimpleTimeZone(rawOffset, ID).
     *
     * By default, this constructor specifies day-of-week-in-month rules. That
     * is, if the startDay is 1, and the startDayOfWeek is SUNDAY, then this
     * indicates the first Sunday in the startMonth. A startDay of -1 likewise
     * indicates the last Sunday. However, by using negative or zero values for
     * certain parameters, other types of rules can be specified.
     *
     * Day of month. To specify an exact day of the month, such as March 1, set
     * startDayOfWeek to zero.
     *
     * Day of week after day of month. To specify the first day of the week
     * occurring on or after an exact day of the month, make the day of the week
     * negative. For example, if startDay is 5 and startDayOfWeek is -MONDAY,
     * this indicates the first Monday on or after the 5th day of the
     * startMonth.
     *
     * Day of week before day of month. To specify the last day of the week
     * occurring on or before an exact day of the month, make the day of the
     * week and the day of the month negative. For example, if startDay is -21
     * and startDayOfWeek is -WEDNESDAY, this indicates the last Wednesday on or
     * before the 21st of the startMonth.
     *
     * The above examples refer to the startMonth, startDay, and startDayOfWeek;
     * the same applies for the endMonth, endDay, and endDayOfWeek.
     *
     * @param rawOffset       The given base time zone offset to GMT.
     * @param ID              The time zone ID which is obtained from
     *                        TimeZone.getAvailableIDs.
     * @param startMonth      The daylight savings starting month. Month is
     *                        0-based. eg, 0 for January.
     * @param startDay        The daylight savings starting
     *                        day-of-week-in-month. Please see the member
     *                        description for an example.
     * @param startDayOfWeek  The daylight savings starting day-of-week. Please
     *                        see the member description for an example.
     * @param startTime       The daylight savings starting time in local wall
     *                        time, which is standard time in this case. Please see the
     *                        member description for an example.
     * @param endMonth        The daylight savings ending month. Month is
     *                        0-based. eg, 0 for January.
     * @param endDay          The daylight savings ending day-of-week-in-month.
     *                        Please see the member description for an example.
     * @param endDayOfWeek    The daylight savings ending day-of-week. Please
     *                        see the member description for an example.
     * @param endTime         The daylight savings ending time in local wall time,
     *                        which is daylight time in this case. Please see the
     *                        member description for an example.
     * @exception IllegalArgumentException the month, day, dayOfWeek, or time
     * parameters are out of range for the start or end rule
     * @stable ICU 2.0
     */
    public SimpleTimeZone(int rawOffset, String ID,
                          int startMonth, int startDay, int startDayOfWeek, int startTime,
                          int endMonth, int endDay, int endDayOfWeek, int endTime)
    {
        this(rawOffset, ID,
             startMonth, startDay, startDayOfWeek, startTime, WALL_TIME,
             endMonth, endDay, endDayOfWeek, endTime, WALL_TIME,
             millisPerHour);
    }

    /**
     * Constructor.  This constructor is identical to the 10-argument
     * constructor, but also takes a dstSavings parameter.
     * @param dstSavings   The amount of time in ms saved during DST.
     * @exception IllegalArgumentException the month, day, dayOfWeek, or time
     * parameters are out of range for the start or end rule
     * @stable ICU 2.0
     */
    public SimpleTimeZone(int rawOffset, String ID,
                          int startMonth, int startDay, int startDayOfWeek, int startTime,
                          int endMonth, int endDay, int endDayOfWeek, int endTime,
                          int dstSavings)
    {
        this(rawOffset, ID,
             startMonth, startDay, startDayOfWeek, startTime, WALL_TIME,
             endMonth, endDay, endDayOfWeek, endTime, WALL_TIME,
             dstSavings);
    }

    /**
     * Constructor.
     */
    SimpleTimeZone(int rawOffset, String ID,
                   int startMonth, int startDay, int startDayOfWeek,
                   int startTime, int startTimeMode,
                   int endMonth, int endDay, int endDayOfWeek,
                   int endTime, int endTimeMode,
                   int dstSavings) {
        setID(ID);
        this.rawOffset      = rawOffset;
        this.startMonth     = startMonth;
        this.startDay       = startDay;
        this.startDayOfWeek = startDayOfWeek;
        this.startTime      = startTime;
        this.startTimeMode  = startTimeMode;
        this.endMonth       = endMonth;
        this.endDay         = endDay;
        this.endDayOfWeek   = endDayOfWeek;
        this.endTime        = endTime;
        this.endTimeMode    = endTimeMode;
        this.dstSavings     = dstSavings;
        // this.useDaylight    = true; // Set by decodeRules
        decodeRules();
        if (dstSavings <= 0) {
            throw new IllegalArgumentException("Illegal DST savings");
        }
    }

    /**
     * Constructor for TimeZoneData.  Takes as input an array and an index
     * into the array.  The array format is that of TimeZoneData.DATA.
     */
    SimpleTimeZone(String ID, int[] data, int i) {
        setID(ID);
        rawOffset = data[i+1]*1000;
        if (data[i] == 0) {
            dstSavings = millisPerHour; // In case user sets rules later
        } else {
            startMonth     = data[i+2];
            startDay       = data[i+3];
            startDayOfWeek = data[i+4];
            startTime      = data[i+5]*60000;
            startTimeMode  = data[i+6];
            endMonth       = data[i+7];
            endDay         = data[i+8];
            endDayOfWeek   = data[i+9];
            endTime        = data[i+10]*60000;
            endTimeMode    = data[i+11];
            dstSavings     = data[i+12]*60000;
            decodeRules();
            if (dstSavings <= 0) {
                throw new IllegalArgumentException("Illegal DST savings");
            }
        }
    }

    /**
     * Sets the daylight savings starting year.
     *
     * @param year  The daylight savings starting year.
     * @stable ICU 2.0
     */
    public void setStartYear(int year)
    {
        startYear = year;
    }

    /**
     * Sets the daylight savings starting rule. For example, Daylight Savings
     * Time starts at the first Sunday in April, at 2 AM in standard time.
     * Therefore, you can set the start rule by calling:
     * setStartRule(TimeFields.APRIL, 1, TimeFields.SUNDAY, 2*60*60*1000);
     *
     * @param month             The daylight savings starting month. Month is
     *                          0-based. eg, 0 for January.
     * @param dayOfWeekInMonth  The daylight savings starting
     *                          day-of-week-in-month. Please see the member
     *                          description for an example.
     * @param dayOfWeek         The daylight savings starting day-of-week.
     *                          Please see the member description for an
     *                          example.
     * @param time              The daylight savings starting time in local wall
     *                          time, which is standard time in this case. Please see
     *                          the member description for an example.
     * @exception IllegalArgumentException the month, dayOfWeekInMonth,
     * dayOfWeek, or time parameters are out of range
     * @stable ICU 2.0
     */
    public void setStartRule(int month, int dayOfWeekInMonth, int dayOfWeek,
                             int time)
    {
        startMonth = month;
        startDay = dayOfWeekInMonth;
        startDayOfWeek = dayOfWeek;
        startTime = time;
        startTimeMode = WALL_TIME;
        // useDaylight = true; // Set by decodeRules
        decodeStartRule();
    }

    /**
     * Sets the DST start rule to a fixed date within a month.
     *
     * @param month         The month in which this rule occurs (0-based).
     * @param dayOfMonth    The date in that month (1-based).
     * @param time          The time of that day (number of millis after midnight)
     *                      when DST takes effect in local wall time, which is
     *                      standard time in this case.
     * @exception IllegalArgumentException the month,
     * dayOfMonth, or time parameters are out of range
     * @stable ICU 2.0
     */
    public void setStartRule(int month, int dayOfMonth, int time) {
        setStartRule(month, dayOfMonth, 0, time);
    }

    /**
     * Sets the DST start rule to a weekday before or after a give date within
     * a month, e.g., the first Monday on or after the 8th.
     *
     * @param month         The month in which this rule occurs (0-based).
     * @param dayOfMonth    A date within that month (1-based).
     * @param dayOfWeek     The day of the week on which this rule occurs.
     * @param time          The time of that day (number of millis after midnight)
     *                      when DST takes effect in local wall time, which is
     *                      standard time in this case.
     * @param after         If true, this rule selects the first dayOfWeek on
     *                      or after dayOfMonth.  If false, this rule selects
     *                      the last dayOfWeek on or before dayOfMonth.
     * @exception IllegalArgumentException the month, dayOfMonth,
     * dayOfWeek, or time parameters are out of range
     * @stable ICU 2.0
     */
    public void setStartRule(int month, int dayOfMonth, int dayOfWeek, int time, boolean after)
    {
        if (after)
            setStartRule(month, dayOfMonth, -dayOfWeek, time);
        else
            setStartRule(month, -dayOfMonth, -dayOfWeek, time);
    }

    /**
     * Sets the daylight savings ending rule. For example, Daylight Savings Time
     * ends at the last (-1) Sunday in October, at 2 AM in standard time.
     * Therefore, you can set the end rule by calling:
     * setEndRule(TimeFields.OCTOBER, -1, TimeFields.SUNDAY, 2*60*60*1000);
     *
     * @param month             The daylight savings ending month. Month is
     *                          0-based. eg, 0 for January.
     * @param dayOfWeekInMonth  The daylight savings ending
     *                          day-of-week-in-month. Please see the member
     *                          description for an example.
     * @param dayOfWeek         The daylight savings ending day-of-week. Please
     *                          see the member description for an example.
     * @param time              The daylight savings ending time in local wall time,
     *                          which is daylight time in this case. Please see the
     *                          member description for an example.
     * @exception IllegalArgumentException the month, dayOfWeekInMonth,
     * dayOfWeek, or time parameters are out of range
     * @stable ICU 2.0
     */
    public void setEndRule(int month, int dayOfWeekInMonth, int dayOfWeek,
                           int time)
    {
        endMonth = month;
        endDay = dayOfWeekInMonth;
        endDayOfWeek = dayOfWeek;
        endTime = time;
        endTimeMode = WALL_TIME;
        // useDaylight = true; // Set by decodeRules
        decodeEndRule();
    }

    /**
     * Sets the DST end rule to a fixed date within a month.
     *
     * @param month         The month in which this rule occurs (0-based).
     * @param dayOfMonth    The date in that month (1-based).
     * @param time          The time of that day (number of millis after midnight)
     *                      when DST ends in local wall time, which is daylight
     *                      time in this case.
     * @exception IllegalArgumentException the month,
     * dayOfMonth, or time parameters are out of range
     * @stable ICU 2.0
     */
    public void setEndRule(int month, int dayOfMonth, int time)
    {
        setEndRule(month, dayOfMonth, 0, time);
    }

    /**
     * Sets the DST end rule to a weekday before or after a give date within
     * a month, e.g., the first Monday on or after the 8th.
     *
     * @param month         The month in which this rule occurs (0-based).
     * @param dayOfMonth    A date within that month (1-based).
     * @param dayOfWeek     The day of the week on which this rule occurs.
     * @param time          The time of that day (number of millis after midnight)
     *                      when DST ends in local wall time, which is daylight
     *                      time in this case.
     * @param after         If true, this rule selects the first dayOfWeek on
     *                      or after dayOfMonth.  If false, this rule selects
     *                      the last dayOfWeek on or before dayOfMonth.
     * @exception IllegalArgumentException the month, dayOfMonth,
     * dayOfWeek, or time parameters are out of range
     * @stable ICU 2.0
     */
    public void setEndRule(int month, int dayOfMonth, int dayOfWeek, int time, boolean after)
    {
        if (after)
            setEndRule(month, dayOfMonth, -dayOfWeek, time);
        else
            setEndRule(month, -dayOfMonth, -dayOfWeek, time);
    }

   /**
     * Returns the difference in milliseconds between local time and
     * UTC, taking into account both the raw offset and the effect of
     * daylight savings, for the specified date and time.  This method
     * assumes that the start and end month are distinct.  It also
     * uses a default {@link GregorianCalendar} object as its
     * underlying calendar, such as for determining leap years.  Do
     * not use the result of this method with a calendar other than a
     * default <code>GregorianCalendar</code>.
     *
     * <p><em>Note:  In general, clients should use
     * <code>Calendar.get(ZONE_OFFSET) + Calendar.get(DST_OFFSET)</code>
     * instead of calling this method.</em>
     *
     * @param era       The era of the given date.
     * @param year      The year in the given date.
     * @param month     The month in the given date. Month is 0-based. e.g.,
     *                  0 for January.
     * @param day       The day-in-month of the given date.
     * @param dayOfWeek The day-of-week of the given date.
     * @param millis    The milliseconds in day in <em>standard</em> local time.
     * @return          The milliseconds to add to UTC to get local time.
     * @exception       IllegalArgumentException the era, month, day,
     *                  dayOfWeek, or millis parameters are out of range
     * @stable ICU 2.0
     */
    public int getOffset(int era, int year, int month, int day, int dayOfWeek,
                         int millis)
    {
        // Check the month before indexing into staticMonthLength. This
	// duplicates the test that occurs in the 7-argument getOffset(),
	// however, this is unavoidable. We don't mind because this method, in
	// fact, should not be called; internal code should always call the
	// 7-argument getOffset(), and outside code should use Calendar.get(int
	// field) with fields ZONE_OFFSET and DST_OFFSET. We can't get rid of
	// this method because it's public API. - liu 8/10/98
        if (month < Calendar.JANUARY
            || month > Calendar.DECEMBER) {
            throw new IllegalArgumentException("Illegal month " + month);
        }
	int monthLength, prevMonthLength;

    // Lazy-initialize this to prevent circular static init dependency
    if (internalCal == null) {
        // Hack: Use the y/m/d constructor in the following line.
        // This prevents the infinite recursion that results when
        // the GC wants to call STZ.getOffset. - liu
        internalCal = new GregorianCalendar(0, 0, 0);
        // Note: No need to synchronize this; we'll use whatever
        // instance we end up with (ref assignment is atomic).
    }

	if ((era == GregorianCalendar.AD) && internalCal.isLeapYear(year)) {
	    monthLength = staticLeapMonthLength[month];
	    prevMonthLength = (month > 1) ? staticLeapMonthLength[month - 1] : 31;
	} else {
	    monthLength = staticMonthLength[month];
	    prevMonthLength = (month > 1) ? staticMonthLength[month - 1] : 31;
	}

        return getOffset(era, year, month, day, dayOfWeek, millis,
			 monthLength, prevMonthLength);
    }

    /**
     * Gets offset, for current date, modified in case of
     * daylight savings. This is the offset to add <em>to</em> UTC to get local time.
     * Gets the time zone offset, for current date, modified in case of daylight
     * savings. This is the offset to add *to* UTC to get local time. Assume
     * that the start and end month are distinct.
     * @param era           The era of the given date.
     * @param year          The year in the given date.
     * @param month         The month in the given date. Month is 0-based. e.g.,
     *                      0 for January.
     * @param day           The day-in-month of the given date.
     * @param dayOfWeek     The day-of-week of the given date.
     * @param millis        The milliseconds in day in <em>standard</em> local time.
     * @param monthLength   The length of the given month in days.
     * @param prevMonthLength The length of the previous month in days.
     * @return              The offset to add *to* GMT to get local time.
     * @exception IllegalArgumentException the era, month, day,
     * dayOfWeek, millis, or monthLength parameters are out of range
     */
    int getOffset(int era, int year, int month, int day, int dayOfWeek,
                  int millis, int monthLength, int prevMonthLength) {
        if (false) {
            /* Use this parameter checking code for normal operation.  Only one
             * of these two blocks should actually get compiled into the class
             * file.  */
            if ((era != GregorianCalendar.AD && era != GregorianCalendar.BC)
                || month < Calendar.JANUARY
                || month > Calendar.DECEMBER
                || day < 1
                || day > monthLength
                || dayOfWeek < Calendar.SUNDAY
                || dayOfWeek > Calendar.SATURDAY
                || millis < 0
                || millis >= millisPerDay
                || monthLength < 28
                || monthLength > 31
                || prevMonthLength < 28
                || prevMonthLength > 31) {
                throw new IllegalArgumentException();
            }
        } else {
            /* This parameter checking code is better for debugging, but
             * overkill for normal operation.  Only one of these two blocks
             * should actually get compiled into the class file.  */
            if (era != GregorianCalendar.AD && era != GregorianCalendar.BC) {
                throw new IllegalArgumentException("Illegal era " + era);
            }
            if (month < Calendar.JANUARY
                || month > Calendar.DECEMBER) {
                throw new IllegalArgumentException("Illegal month " + month);
            }
            if (day < 1
                || day > monthLength) {
                throw new IllegalArgumentException("Illegal day " + day);
            }
            if (dayOfWeek < Calendar.SUNDAY
                || dayOfWeek > Calendar.SATURDAY) {
                throw new IllegalArgumentException("Illegal day of week " + dayOfWeek);
            }
            if (millis < 0
                || millis >= millisPerDay) {
                throw new IllegalArgumentException("Illegal millis " + millis);
            }
            if (monthLength < 28
                || monthLength > 31) {
                throw new IllegalArgumentException("Illegal month length " + monthLength);
            }
            if (prevMonthLength < 28
                || prevMonthLength > 31) {
                throw new IllegalArgumentException("Illegal previous month length " + prevMonthLength);
            }
        }

        int result = rawOffset;

        // Bail out if we are before the onset of daylight savings time
        if (!useDaylight || year < startYear || era != GregorianCalendar.AD) return result;

        // Check for southern hemisphere.  We assume that the start and end
        // month are different.
        boolean southern = (startMonth > endMonth);

        // Compare the date to the starting and ending rules.+1 = date>rule, -1
        // = date<rule, 0 = date==rule.
        int startCompare = compareToRule(month, monthLength, prevMonthLength,
                                         day, dayOfWeek, millis,
                                         startTimeMode == UTC_TIME ? -rawOffset : 0,
                                         startMode, startMonth, startDayOfWeek,
                                         startDay, startTime);
        int endCompare = 0;

        /* We don't always have to compute endCompare.  For many instances,
         * startCompare is enough to determine if we are in DST or not.  In the
         * northern hemisphere, if we are before the start rule, we can't have
         * DST.  In the southern hemisphere, if we are after the start rule, we
         * must have DST.  This is reflected in the way the next if statement
         * (not the one immediately following) short circuits. */
        if (southern != (startCompare >= 0)) {
            /* For the ending rule comparison, we add the dstSavings to the millis
             * passed in to convert them from standard to wall time.  We then must
             * normalize the millis to the range 0..millisPerDay-1. */
            endCompare = compareToRule(month, monthLength, prevMonthLength,
                                       day, dayOfWeek, millis,
                                       endTimeMode == WALL_TIME ? dstSavings :
                                        (endTimeMode == UTC_TIME ? -rawOffset : 0),
                                       endMode, endMonth, endDayOfWeek,
                                       endDay, endTime);
        }

        // Check for both the northern and southern hemisphere cases.  We
        // assume that in the northern hemisphere, the start rule is before the
        // end rule within the calendar year, and vice versa for the southern
        // hemisphere.
        if ((!southern && (startCompare >= 0 && endCompare < 0)) ||
            (southern && (startCompare >= 0 || endCompare < 0)))
            result += dstSavings;

        return result;
    }

    /**
     * Compare a given date in the year to a rule. Return 1, 0, or -1, depending
     * on whether the date is after, equal to, or before the rule date. The
     * millis are compared directly against the ruleMillis, so any
     * standard-daylight adjustments must be handled by the caller.
     *
     * @return  1 if the date is after the rule date, -1 if the date is before
     *          the rule date, or 0 if the date is equal to the rule date.
     */
    private static int compareToRule(int month, int monthLen, int prevMonthLen,
                                     int dayOfMonth,
                                     int dayOfWeek, int millis, int millisDelta,
                                     int ruleMode, int ruleMonth, int ruleDayOfWeek,
                                     int ruleDay, int ruleMillis)
    {
        // Make adjustments for startTimeMode and endTimeMode
        millis += millisDelta;
        while (millis >= millisPerDay) {
            millis -= millisPerDay;
            ++dayOfMonth;
            dayOfWeek = 1 + (dayOfWeek % 7); // dayOfWeek is one-based
            if (dayOfMonth > monthLen) {
                dayOfMonth = 1;
                /* When incrementing the month, it is desirible to overflow
                 * from DECEMBER to DECEMBER+1, since we use the result to
                 * compare against a real month. Wraparound of the value
                 * leads to bug 4173604. */
                ++month;
            }
        }
        while (millis < 0) {
            millis += millisPerDay;
            --dayOfMonth;
            dayOfWeek = 1 + ((dayOfWeek+5) % 7); // dayOfWeek is one-based
            if (dayOfMonth < 1) {
                dayOfMonth = prevMonthLen;
                --month;
            }
        }
        
        if (month < ruleMonth) return -1;
        else if (month > ruleMonth) return 1;

        int ruleDayOfMonth = 0;
        switch (ruleMode)
        {
        case DOM_MODE:
            ruleDayOfMonth = ruleDay;
            break;
        case DOW_IN_MONTH_MODE:
            // In this case ruleDay is the day-of-week-in-month
            if (ruleDay > 0)
                ruleDayOfMonth = 1 + (ruleDay - 1) * 7 +
                    (7 + ruleDayOfWeek - (dayOfWeek - dayOfMonth + 1)) % 7;
            else // Assume ruleDay < 0 here
            {
                ruleDayOfMonth = monthLen + (ruleDay + 1) * 7 -
                    (7 + (dayOfWeek + monthLen - dayOfMonth) - ruleDayOfWeek) % 7;
            }
            break;
        case DOW_GE_DOM_MODE:
            ruleDayOfMonth = ruleDay +
                (49 + ruleDayOfWeek - ruleDay - dayOfWeek + dayOfMonth) % 7;
            break;
        case DOW_LE_DOM_MODE:
            ruleDayOfMonth = ruleDay -
                (49 - ruleDayOfWeek + ruleDay + dayOfWeek - dayOfMonth) % 7;
            // Note at this point ruleDayOfMonth may be <1, although it will
            // be >=1 for well-formed rules.
            break;
        }

        if (dayOfMonth < ruleDayOfMonth) return -1;
        else if (dayOfMonth > ruleDayOfMonth) return 1;

        if (millis < ruleMillis) return -1;
        else if (millis > ruleMillis) return 1;
        else return 0;
    }

    /**
     * Overrides TimeZone
     * Gets the GMT offset for this time zone.
     * @stable ICU 2.0
     */
    public int getRawOffset()
    {
        // The given date will be taken into account while
        // we have the historical time zone data in place.
        return rawOffset;
    }

    /**
     * Overrides TimeZone
     * Sets the base time zone offset to GMT.
     * This is the offset to add *to* UTC to get local time.
     * Please see TimeZone.setRawOffset for descriptions on the parameter.
     * @stable ICU 2.0
     */
    public void setRawOffset(int offsetMillis)
    {
        this.rawOffset = offsetMillis;
    }

    /**
     * Sets the amount of time in ms that the clock is advanced during DST.
     * @param millisSavedDuringDST the number of milliseconds the time is
     * advanced with respect to standard time when the daylight savings rules
     * are in effect. A positive number, typically one hour (3600000).
     * @stable ICU 2.0
     */
    public void setDSTSavings(int millisSavedDuringDST) {
        if (millisSavedDuringDST <= 0) {
            throw new IllegalArgumentException("Illegal DST savings");
        }
        dstSavings = millisSavedDuringDST;
    }

    /**
     * Returns the amount of time in ms that the clock is advanced during DST.
     * @return the number of milliseconds the time is
     * advanced with respect to standard time when the daylight savings rules
     * are in effect. A positive number, typically one hour (3600000).
     * @stable ICU 2.0
     */
    public int getDSTSavings() {
        return dstSavings;
    }

    /**
     * Overrides TimeZone
     * Queries if this time zone uses Daylight Savings Time.
     * @stable ICU 2.0
     */
    public boolean useDaylightTime()
    {
        return useDaylight;
    }

    /**
     * Overrides TimeZone
     * Queries if the given date is in Daylight Savings Time.
     * @stable ICU 2.0
     */
    public boolean inDaylightTime(Date date)
    {
        GregorianCalendar gc = new GregorianCalendar(this);
        gc.setTime(date);
        return gc.inDaylightTime();
    }

    /**
     * Overrides Cloneable
     * @stable ICU 2.0
     */
    public Object clone()
    {
        return super.clone();
        // other fields are bit-copied
    }

    /**
     * Override hashCode.
     * Generates the hash code for the SimpleDateFormat object
     * @stable ICU 2.0
     */
    public synchronized int hashCode()
    {
        return startMonth ^ startDay ^ startDayOfWeek ^ startTime ^
            endMonth ^ endDay ^ endDayOfWeek ^ endTime ^ rawOffset;
    }

    /**
     * Compares the equality of two SimpleTimeZone objects.
     *
     * @param obj  The SimpleTimeZone object to be compared with.
     * @return     True if the given obj is the same as this SimpleTimeZone
     *             object; false otherwise.
     * @stable ICU 2.0
     */
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!(obj instanceof SimpleTimeZone))
            return false;

        SimpleTimeZone that = (SimpleTimeZone) obj;

        return getID().equals(that.getID()) &&
            hasSameRules(that);
    }

    /**
     * Return true if this zone has the same rules and offset as another zone.
     * @param other the TimeZone object to be compared with
     * @return true if the given zone has the same rules and offset as this one
     * @stable ICU 2.0
     */
    public boolean hasSameRules(TimeZone other) {
        if (this == other) return true;
        if (!(other instanceof SimpleTimeZone)) return false;
        SimpleTimeZone that = (SimpleTimeZone) other;
        return super.hasSameRules(other) &&
            (!useDaylight
             // Only check rules if using DST
             || (dstSavings == that.dstSavings &&
                 startMode == that.startMode &&
                 startMonth == that.startMonth &&
                 startDay == that.startDay &&
                 startDayOfWeek == that.startDayOfWeek &&
                 startTime == that.startTime &&
                 startTimeMode == that.startTimeMode &&
                 endMode == that.endMode &&
                 endMonth == that.endMonth &&
                 endDay == that.endDay &&
                 endDayOfWeek == that.endDayOfWeek &&
                 endTime == that.endTime &&
                 endTimeMode == that.endTimeMode &&
                 startYear == that.startYear));
    }

    /**
     * Return a string representation of this time zone.
     * @return  a string representation of this time zone.
     * @stable ICU 2.0
     */
    public String toString() {
        return getClass().getName() +
            "[id=" + getID() +
            ",offset=" + rawOffset +
            ",dstSavings=" + dstSavings +
            ",useDaylight=" + useDaylight +
            ",startYear=" + startYear +
            ",startMode=" + startMode +
            ",startMonth=" + startMonth +
            ",startDay=" + startDay +
            ",startDayOfWeek=" + startDayOfWeek +
            ",startTime=" + startTime +
            ",startTimeMode=" + startTimeMode +
            ",endMode=" + endMode +
            ",endMonth=" + endMonth +
            ",endDay=" + endDay +
            ",endDayOfWeek=" + endDayOfWeek +
            ",endTime=" + endTime +
            ",endTimeMode=" + endTimeMode + ']';
    }

    // =======================privates===============================

    /**
     * The month in which daylight savings time starts.  This value must be
     * between <code>Calendar.JANUARY</code> and
     * <code>Calendar.DECEMBER</code> inclusive.  This value must not equal
     * <code>endMonth</code>.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int startMonth;

    /**
     * This field has two possible interpretations:
     * <dl>
     * <dt><code>startMode == DOW_IN_MONTH</code></dt>
     * <dd>
     * <code>startDay</code> indicates the day of the month of
     * <code>startMonth</code> on which daylight
     * savings time starts, from 1 to 28, 30, or 31, depending on the
     * <code>startMonth</code>.
     * </dd>
     * <dt><code>startMode != DOW_IN_MONTH</code></dt>
     * <dd>
     * <code>startDay</code> indicates which <code>startDayOfWeek</code> in th
     * month <code>startMonth</code> daylight
     * savings time starts on.  For example, a value of +1 and a
     * <code>startDayOfWeek</code> of <code>Calendar.SUNDAY</code> indicates the
     * first Sunday of <code>startMonth</code>.  Likewise, +2 would indicate the
     * second Sunday, and -1 the last Sunday.  A value of 0 is illegal.
     * </dd>
     * </ul>
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int startDay;

    /**
     * The day of the week on which daylight savings time starts.  This value
     * must be between <code>Calendar.SUNDAY</code> and
     * <code>Calendar.SATURDAY</code> inclusive.
     * <p>If <code>useDaylight</code> is false or
     * <code>startMode == DAY_OF_MONTH</code>, this value is ignored.
     * @serial
     */
    private int startDayOfWeek;

    /**
     * The time in milliseconds after midnight at which daylight savings
     * time starts.  This value is expressed as wall time, standard time,
     * or UTC time, depending on the setting of <code>startTimeMode</code>.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int startTime;

    /**
     * The format of startTime, either WALL_TIME, STANDARD_TIME, or UTC_TIME.
     * @serial
     */
    private int startTimeMode;

    /**
     * The month in which daylight savings time ends.  This value must be
     * between <code>Calendar.JANUARY</code> and
     * <code>Calendar.UNDECIMBER</code>.  This value must not equal
     * <code>startMonth</code>.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int endMonth;

    /**
     * This field has two possible interpretations:
     * <dl>
     * <dt><code>endMode == DOW_IN_MONTH</code></dt>
     * <dd>
     * <code>endDay</code> indicates the day of the month of
     * <code>endMonth</code> on which daylight
     * savings time ends, from 1 to 28, 30, or 31, depending on the
     * <code>endMonth</code>.
     * </dd>
     * <dt><code>endMode != DOW_IN_MONTH</code></dt>
     * <dd>
     * <code>endDay</code> indicates which <code>endDayOfWeek</code> in th
     * month <code>endMonth</code> daylight
     * savings time ends on.  For example, a value of +1 and a
     * <code>endDayOfWeek</code> of <code>Calendar.SUNDAY</code> indicates the
     * first Sunday of <code>endMonth</code>.  Likewise, +2 would indicate the
     * second Sunday, and -1 the last Sunday.  A value of 0 is illegal.
     * </dd>
     * </ul>
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int endDay;

    /**
     * The day of the week on which daylight savings time ends.  This value
     * must be between <code>Calendar.SUNDAY</code> and
     * <code>Calendar.SATURDAY</code> inclusive.
     * <p>If <code>useDaylight</code> is false or
     * <code>endMode == DAY_OF_MONTH</code>, this value is ignored.
     * @serial
     */
    private int endDayOfWeek;

    /**
     * The time in milliseconds after midnight at which daylight savings
     * time ends.  This value is expressed as wall time, standard time,
     * or UTC time, depending on the setting of <code>endTimeMode</code>.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int endTime;

    /**
     * The format of endTime, either WALL_TIME, STANDARD_TIME, or UTC_TIME.
     * @serial
     */
    private int endTimeMode;

    /**
     * The year in which daylight savings time is first observed.  This is an AD
     * value.  If this value is less than 1 then daylight savings is observed
     * for all AD years.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int startYear;

    /**
     * The offset in milliseconds between this zone and GMT.  Negative offsets
     * are to the west of Greenwich.  To obtain local <em>standard</em> time,
     * add the offset to GMT time.  To obtain local wall time it may also be
     * necessary to add <code>dstSavings</code>.
     * @serial
     */
    private int rawOffset;

    /**
     * A boolean value which is true if and only if this zone uses daylight
     * savings time.  If this value is false, several other fields are ignored.
     * @serial
     */
    private boolean useDaylight=false; // indicate if this time zone uses DST

    private static final int millisPerHour = 60*60*1000;
    private static final int millisPerDay  = 24*millisPerHour;

    /**
     * This field was serialized in JDK 1.1, so we have to keep it that way
     * to maintain serialization compatibility. However, there's no need to
     * recreate the array each time we create a new time zone.
     * @serial An array of bytes containing the values {31, 28, 31, 30, 31, 30,
     * 31, 31, 30, 31, 30, 31}.  This is ignored as of the Java 2 platform v1.2, however, it must
     * be streamed out for compatibility with JDK 1.1.
     */
    private final byte monthLength[] = staticMonthLength;
    private final static byte staticMonthLength[] = {31,28,31,30,31,30,31,31,30,31,30,31};
    private final static byte staticLeapMonthLength[] = {31,29,31,30,31,30,31,31,30,31,30,31};
    // Lazy-initialize this to prevent circular static init dependency
    private static GregorianCalendar internalCal = null;

    /**
     * Variables specifying the mode of the start rule.  Takes the following
     * values:
     * <dl>
     * <dt><code>DOM_MODE</code></dt>
     * <dd>
     * Exact day of week; e.g., March 1.
     * </dd>
     * <dt><code>DOW_IN_MONTH_MODE</code></dt>    
     * <dd>
     * Day of week in month; e.g., last Sunday in March.
     * </dd>
     * <dt><code>DOW_GE_DOM_MODE</code></dt>
     * <dd>
     * Day of week after day of month; e.g., Sunday on or after March 15.
     * </dd>
     * <dt><code>DOW_LE_DOM_MODE</code></dt>
     * <dd>
     * Day of week before day of month; e.g., Sunday on or before March 15.
     * </dd>
     * </dl>
     * The setting of this field affects the interpretation of the
     * <code>startDay</code> field.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int startMode;

    /**
     * Variables specifying the mode of the end rule.  Takes the following
     * values:
     * <dl>
     * <dt><code>DOM_MODE</code></dt>
     * <dd>
     * Exact day of week; e.g., March 1.
     * </dd>
     * <dt><code>DOW_IN_MONTH_MODE</code></dt>    
     * <dd>
     * Day of week in month; e.g., last Sunday in March.
     * </dd>
     * <dt><code>DOW_GE_DOM_MODE</code></dt>
     * <dd>
     * Day of week after day of month; e.g., Sunday on or after March 15.
     * </dd>
     * <dt><code>DOW_LE_DOM_MODE</code></dt>
     * <dd>
     * Day of week before day of month; e.g., Sunday on or before March 15.
     * </dd>
     * </dl>
     * The setting of this field affects the interpretation of the
     * <code>endDay</code> field.
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int endMode;

    /**
     * A positive value indicating the amount of time saved during DST in
     * milliseconds.
     * Typically one hour (3600000); sometimes 30 minutes (1800000).
     * <p>If <code>useDaylight</code> is false, this value is ignored.
     * @serial
     */
    private int dstSavings;

    /**
     * Constants specifying values of startMode and endMode.
     */
    private static final int DOM_MODE          = 1; // Exact day of month, "Mar 1"
    private static final int DOW_IN_MONTH_MODE = 2; // Day of week in month, "lastSun"
    private static final int DOW_GE_DOM_MODE   = 3; // Day of week after day of month, "Sun>=15"
    private static final int DOW_LE_DOM_MODE   = 4; // Day of week before day of month, "Sun<=21"

    /**
     * Constant for a rule specified as wall time.  Wall time is standard time
     * for the onset rule, and daylight time for the end rule.  Most rules
     * are specified as wall time.
     */
    static final int WALL_TIME = 0; // Zero for backward compatibility

    /**
     * Constant for a rule specified as standard time.
     */
    static final int STANDARD_TIME = 1;

    /**
     * Constant for a rule specified as UTC.  EU rules are specified as UTC
     * time.
     */
    static final int UTC_TIME = 2;

    // Proclaim compatibility with 1.1
    static final long serialVersionUID = -403250971215465050L;

    // the internal serial version which says which version was written
    // - 0 (default) for version up to JDK 1.1.3
    // - 1 for version from JDK 1.1.4, which includes 3 new fields
    // - 2 for JDK 1.3, which includes 2 new files
    static final int currentSerialVersion = 2;

    /**
     * The version of the serialized data on the stream.  Possible values:
     * <dl>
     * <dt><b>0</b> or not present on stream</dt>
     * <dd>
     * JDK 1.1.3 or earlier.
     * </dd>
     * <dt><b>1</b></dt>
     * <dd>
     * JDK 1.1.4 or later.  Includes three new fields: <code>startMode</code>,
     * <code>endMode</code>, and <code>dstSavings</code>.
     * </dd>
     * <dt><b>2</b></dt>
     * <dd>
     * JDK 1.3 or later.  Includes two new fields: <code>startTimeMode</code>
     * and <code>endTimeMode</code>.
     * </dd>
     * </dl>
     * When streaming out this class, the most recent format
     * and the highest allowable <code>serialVersionOnStream</code>
     * is written.
     * @serial
     */
    private int serialVersionOnStream = currentSerialVersion;

    //----------------------------------------------------------------------
    // Rule representation
    //
    // We represent the following flavors of rules:
    //       5        the fifth of the month
    //       lastSun  the last Sunday in the month
    //       lastMon  the last Monday in the month
    //       Sun>=8   first Sunday on or after the eighth
    //       Sun<=25  last Sunday on or before the 25th
    // This is further complicated by the fact that we need to remain
    // backward compatible with the 1.1 FCS.  Finally, we need to minimize
    // API changes.  In order to satisfy these requirements, we support
    // three representation systems, and we translate between them.
    //
    // INTERNAL REPRESENTATION
    // This is the format SimpleTimeZone objects take after construction or
    // streaming in is complete.  Rules are represented directly, using an
    // unencoded format.  We will discuss the start rule only below; the end
    // rule is analogous.
    //   startMode      Takes on enumerated values DAY_OF_MONTH,
    //                  DOW_IN_MONTH, DOW_AFTER_DOM, or DOW_BEFORE_DOM.
    //   startDay       The day of the month, or for DOW_IN_MONTH mode, a
    //                  value indicating which DOW, such as +1 for first,
    //                  +2 for second, -1 for last, etc.
    //   startDayOfWeek The day of the week.  Ignored for DAY_OF_MONTH.
    //
    // ENCODED REPRESENTATION
    // This is the format accepted by the constructor and by setStartRule()
    // and setEndRule().  It uses various combinations of positive, negative,
    // and zero values to encode the different rules.  This representation
    // allows us to specify all the different rule flavors without altering
    // the API.
    //   MODE              startMonth    startDay    startDayOfWeek
    //   DOW_IN_MONTH_MODE >=0           !=0         >0
    //   DOM_MODE          >=0           >0          ==0
    //   DOW_GE_DOM_MODE   >=0           >0          <0
    //   DOW_LE_DOM_MODE   >=0           <0          <0
    //   (no DST)          don't care    ==0         don't care
    //
    // STREAMED REPRESENTATION
    // We must retain binary compatibility with the 1.1 FCS.  The 1.1 code only
    // handles DOW_IN_MONTH_MODE and non-DST mode, the latter indicated by the
    // flag useDaylight.  When we stream an object out, we translate into an
    // approximate DOW_IN_MONTH_MODE representation so the object can be parsed
    // and used by 1.1 code.  Following that, we write out the full
    // representation separately so that contemporary code can recognize and
    // parse it.  The full representation is written in a "packed" format,
    // consisting of a version number, a length, and an array of bytes.  Future
    // versions of this class may specify different versions.  If they wish to
    // include additional data, they should do so by storing them after the
    // packed representation below.
    //----------------------------------------------------------------------

    /**
     * Given a set of encoded rules in startDay and startDayOfMonth, decode
     * them and set the startMode appropriately.  Do the same for endDay and
     * endDayOfMonth.  Upon entry, the day of week variables may be zero or
     * negative, in order to indicate special modes.  The day of month
     * variables may also be negative.  Upon exit, the mode variables will be
     * set, and the day of week and day of month variables will be positive.
     * This method also recognizes a startDay or endDay of zero as indicating
     * no DST.
     */
    private void decodeRules()
    {
        decodeStartRule();
        decodeEndRule();
    }

    /**
     * Decode the start rule and validate the parameters.  The parameters are
     * expected to be in encoded form, which represents the various rule modes
     * by negating or zeroing certain values.  Representation formats are:
     * <p>
     * <pre>
     *            DOW_IN_MONTH  DOM    DOW>=DOM  DOW<=DOM  no DST
     *            ------------  -----  --------  --------  ----------
     * month       0..11        same    same      same     don't care
     * day        -5..5         1..31   1..31    -1..-31   0
     * dayOfWeek   1..7         0      -1..-7    -1..-7    don't care
     * time        0..ONEDAY    same    same      same     don't care
     * </pre>
     * The range for month does not include UNDECIMBER since this class is
     * really specific to GregorianCalendar, which does not use that month.
     * The range for time includes ONEDAY (vs. ending at ONEDAY-1) because the
     * end rule is an exclusive limit point.  That is, the range of times that
     * are in DST include those >= the start and < the end.  For this reason,
     * it should be possible to specify an end of ONEDAY in order to include the
     * entire day.  Although this is equivalent to time 0 of the following day,
     * it's not always possible to specify that, for example, on December 31.
     * While arguably the start range should still be 0..ONEDAY-1, we keep
     * the start and end ranges the same for consistency.
     */
    private void decodeStartRule() {
        useDaylight = (startDay != 0) && (endDay != 0);
        if (startDay != 0) {
            if (startMonth < Calendar.JANUARY || startMonth > Calendar.DECEMBER) {
                throw new IllegalArgumentException(
                        "Illegal start month " + startMonth);
            }
            if (startTime < 0 || startTime >= millisPerDay) {
                throw new IllegalArgumentException(
                        "Illegal start time " + startTime);
            }
            if (startDayOfWeek == 0) {
                startMode = DOM_MODE;
            } else {
                if (startDayOfWeek > 0) {
                    startMode = DOW_IN_MONTH_MODE;
                } else {
                    startDayOfWeek = -startDayOfWeek;
                    if (startDay > 0) {
                        startMode = DOW_GE_DOM_MODE;
                    } else {
                        startDay = -startDay;
                        startMode = DOW_LE_DOM_MODE;
                    }
                }
                if (startDayOfWeek > Calendar.SATURDAY) {
                    throw new IllegalArgumentException(
                           "Illegal start day of week " + startDayOfWeek);
                }
            }
            if (startMode == DOW_IN_MONTH_MODE) {
                if (startDay < -5 || startDay > 5) {
                    throw new IllegalArgumentException(
                            "Illegal start day of week in month " + startDay);
                }
            } else if (startDay < 1 || startDay > staticMonthLength[startMonth]) {
                throw new IllegalArgumentException(
                        "Illegal start day " + startDay);
            }
        }
    }

    /**
     * Decode the end rule and validate the parameters.  This method is exactly
     * analogous to decodeStartRule().
     * @see decodeStartRule
     */
    private void decodeEndRule() {
        useDaylight = (startDay != 0) && (endDay != 0);
        if (endDay != 0) {
            if (endMonth < Calendar.JANUARY || endMonth > Calendar.DECEMBER) {
                throw new IllegalArgumentException(
                        "Illegal end month " + endMonth);
            }
            if (endTime < 0 || endTime >= millisPerDay) {
                throw new IllegalArgumentException(
                        "Illegal end time " + endTime);
            }
            if (endDayOfWeek == 0) {
                endMode = DOM_MODE;
            } else {
                if (endDayOfWeek > 0) {
                    endMode = DOW_IN_MONTH_MODE;
                } else {
                    endDayOfWeek = -endDayOfWeek;
                    if (endDay > 0) {
                        endMode = DOW_GE_DOM_MODE;
                    } else {
                        endDay = -endDay;
                        endMode = DOW_LE_DOM_MODE;
                    }
                }
                if (endDayOfWeek > Calendar.SATURDAY) {
                    throw new IllegalArgumentException(
                           "Illegal end day of week " + endDayOfWeek);
                }
            }
            if (endMode == DOW_IN_MONTH_MODE) {
                if (endDay < -5 || endDay > 5) {
                    throw new IllegalArgumentException(
                            "Illegal end day of week in month " + endDay);
                }
            } else if (endDay < 1 || endDay > staticMonthLength[endMonth]) {
                throw new IllegalArgumentException(
                        "Illegal end day " + endDay);
            }
        }
    }

    /**
     * Make rules compatible to 1.1 FCS code.  Since 1.1 FCS code only understands
     * day-of-week-in-month rules, we must modify other modes of rules to their
     * approximate equivalent in 1.1 FCS terms.  This method is used when streaming
     * out objects of this class.  After it is called, the rules will be modified,
     * with a possible loss of information.  startMode and endMode will NOT be
     * altered, even though semantically they should be set to DOW_IN_MONTH_MODE,
     * since the rule modification is only intended to be temporary.
     */
    private void makeRulesCompatible()
    {
        switch (startMode)
        {
        case DOM_MODE:
            startDay = 1 + (startDay / 7);
            startDayOfWeek = Calendar.SUNDAY;
            break;
        case DOW_GE_DOM_MODE:
            // A day-of-month of 1 is equivalent to DOW_IN_MONTH_MODE
            // that is, Sun>=1 == firstSun.
            if (startDay != 1)
                startDay = 1 + (startDay / 7);
            break;
        case DOW_LE_DOM_MODE:
            if (startDay >= 30)
                startDay = -1;
            else
                startDay = 1 + (startDay / 7);
            break;
        }

        switch (endMode)
        {
        case DOM_MODE:
            endDay = 1 + (endDay / 7);
            endDayOfWeek = Calendar.SUNDAY;
            break;
        case DOW_GE_DOM_MODE:
            // A day-of-month of 1 is equivalent to DOW_IN_MONTH_MODE
            // that is, Sun>=1 == firstSun.
            if (endDay != 1)
                endDay = 1 + (endDay / 7);
            break;
        case DOW_LE_DOM_MODE:
            if (endDay >= 30)
                endDay = -1;
            else
                endDay = 1 + (endDay / 7);
            break;
        }

        /* Adjust the start and end times to wall time.  This works perfectly
         * well unless it pushes into the next or previous day.  If that
         * happens, we attempt to adjust the day rule somewhat crudely.  The day
         * rules have been forced into DOW_IN_MONTH mode already, so we change
         * the day of week to move forward or back by a day.  It's possible to
         * make a more refined adjustment of the original rules first, but in
         * most cases this extra effort will go to waste once we adjust the day
         * rules anyway. */
        switch (startTimeMode) {
        case UTC_TIME:
            startTime += rawOffset;
            break;
        }
        while (startTime < 0) {
            startTime += millisPerDay;
            startDayOfWeek = 1 + ((startDayOfWeek+5) % 7); // Back 1 day
        }
        while (startTime >= millisPerDay) {
            startTime -= millisPerDay;
            startDayOfWeek = 1 + (startDayOfWeek % 7); // Forward 1 day
        }

        switch (endTimeMode) {
        case UTC_TIME:
            endTime += rawOffset + dstSavings;
            break;
        case STANDARD_TIME:
            endTime += dstSavings;
        }
        while (endTime < 0) {
            endTime += millisPerDay;
            endDayOfWeek = 1 + ((endDayOfWeek+5) % 7); // Back 1 day
        }
        while (endTime >= millisPerDay) {
            endTime -= millisPerDay;
            endDayOfWeek = 1 + (endDayOfWeek % 7); // Forward 1 day
        }
    }

    /**
     * Pack the start and end rules into an array of bytes.  Only pack
     * data which is not preserved by makeRulesCompatible.
     */
    private byte[] packRules()
    {
        byte[] rules = new byte[6];
        rules[0] = (byte)startDay;
        rules[1] = (byte)startDayOfWeek;
        rules[2] = (byte)endDay;
        rules[3] = (byte)endDayOfWeek;

        // As of serial version 2, include time modes
        rules[4] = (byte)startTimeMode;
        rules[5] = (byte)endTimeMode;

        return rules;
    }

    /**
     * Given an array of bytes produced by packRules, interpret them
     * as the start and end rules.
     */
    private void unpackRules(byte[] rules)
    {
        startDay       = rules[0];
        startDayOfWeek = rules[1];
        endDay         = rules[2];
        endDayOfWeek   = rules[3];

        // As of serial version 2, include time modes
        if (rules.length >= 6) {
            startTimeMode = rules[4];
            endTimeMode   = rules[5];
        }
    }

    /**
     * Pack the start and end times into an array of bytes.  This is required
     * as of serial version 2.
     */
    private int[] packTimes() {
        int[] times = new int[2];
        times[0] = startTime;
        times[1] = endTime;
        return times;
    }

    /**
     * Unpack the start and end times from an array of bytes.  This is required
     * as of serial version 2.
     */
    private void unpackTimes(int[] times) {
        startTime = times[0];
        endTime = times[1];
    }

    /**
     * Save the state of this object to a stream (i.e., serialize it).
     *
     * @serialData We write out two formats, a JDK 1.1 compatible format, using
     * <code>DOW_IN_MONTH_MODE</code> rules, in the required section, followed
     * by the full rules, in packed format, in the optional section.  The
     * optional section will be ignored by JDK 1.1 code upon stream in.
     * <p> Contents of the optional section: The length of a byte array is
     * emitted (int); this is 4 as of this release. The byte array of the given
     * length is emitted. The contents of the byte array are the true values of
     * the fields <code>startDay</code>, <code>startDayOfWeek</code>,
     * <code>endDay</code>, and <code>endDayOfWeek</code>.  The values of these
     * fields in the required section are approximate values suited to the rule
     * mode <code>DOW_IN_MONTH_MODE</code>, which is the only mode recognized by
     * JDK 1.1.
     */
    private void writeObject(ObjectOutputStream stream)
         throws IOException
    {
        // Construct a binary rule
        byte[] rules = packRules();
        int[] times = packTimes();

        // Convert to 1.1 FCS rules.  This step may cause us to lose information.
        makeRulesCompatible();

        // Write out the 1.1 FCS rules
        stream.defaultWriteObject();

        // Write out the binary rules in the optional data area of the stream.
        stream.writeInt(rules.length);
        stream.write(rules);
        stream.writeObject(times);

        // Recover the original rules.  This recovers the information lost
        // by makeRulesCompatible.
        unpackRules(rules);
        unpackTimes(times);
    }

    /**
     * Reconstitute this object from a stream (i.e., deserialize it).
     *
     * We handle both JDK 1.1
     * binary formats and full formats with a packed byte array.
     */
    private void readObject(ObjectInputStream stream)
         throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();

        if (serialVersionOnStream < 1)
        {
            // Fix a bug in the 1.1 SimpleTimeZone code -- namely,
            // startDayOfWeek and endDayOfWeek were usually uninitialized.  We can't do
            // too much, so we assume SUNDAY, which actually works most of the time.
            if (startDayOfWeek == 0) startDayOfWeek = Calendar.SUNDAY;
            if (endDayOfWeek == 0) endDayOfWeek = Calendar.SUNDAY;

            // The variables dstSavings, startMode, and endMode are post-1.1, so they
            // won't be present if we're reading from a 1.1 stream.  Fix them up.
            startMode = endMode = DOW_IN_MONTH_MODE;
            dstSavings = millisPerHour;
        }
        else
        {
            // For 1.1.4, in addition to the 3 new instance variables, we also
            // store the actual rules (which have not be made compatible with 1.1)
            // in the optional area.  Read them in here and parse them.
            int length = stream.readInt();
            byte[] rules = new byte[length];
            stream.readFully(rules);
            unpackRules(rules);
        }

        if (serialVersionOnStream >= 2) {
            int[] times = (int[]) stream.readObject();
            unpackTimes(times);
        }

        serialVersionOnStream = currentSerialVersion;
    }
}

//eof
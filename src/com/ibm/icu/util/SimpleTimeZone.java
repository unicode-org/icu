/*
*   Copyright (C) 1996-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*/

package com.ibm.icu.util;
import com.ibm.icu.impl.JDKTimeZone;

///CLOVER:USECLASS
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
public class SimpleTimeZone extends JDKTimeZone {
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
    public SimpleTimeZone(int rawOffset, String ID) {
        this(new java.util.SimpleTimeZone(rawOffset, ID));
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
                          int endMonth, int endDay, int endDayOfWeek, int endTime) {
        this(new java.util.SimpleTimeZone(rawOffset, ID, startMonth, startDay,
                                 startDayOfWeek, startTime, endMonth,
                                 endDay, endDayOfWeek, endTime));
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
                          int dstSavings) {
        this(new java.util.SimpleTimeZone(rawOffset, ID, startMonth, startDay,
                                 startDayOfWeek, startTime, endMonth,
                                 endDay, endDayOfWeek, endTime, dstSavings));
    }

    /**
     * Sets the daylight savings starting year.
     *
     * @param year  The daylight savings starting year.
     * @stable ICU 2.0
     */
    public void setStartYear(int year) {
        unwrapSTZ().setStartYear(year);
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
                             int time) {
        unwrapSTZ().setStartRule(month, dayOfWeekInMonth, dayOfWeek, time);
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
        unwrapSTZ().setStartRule(month, dayOfMonth, time);
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
    public void setStartRule(int month, int dayOfMonth, int dayOfWeek, int time, boolean after) {
        unwrapSTZ().setStartRule(month, dayOfMonth, dayOfWeek, time, after);
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
                           int time) {
        unwrapSTZ().setEndRule(month, dayOfWeekInMonth, dayOfWeek, time);
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
    public void setEndRule(int month, int dayOfMonth, int time) {
        unwrapSTZ().setEndRule(month, dayOfMonth, time);
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
    public void setEndRule(int month, int dayOfMonth, int dayOfWeek, int time, boolean after) {
        unwrapSTZ().setEndRule(month, dayOfMonth, dayOfWeek, time, after);
    }

    /**
     * Sets the amount of time in ms that the clock is advanced during DST.
     * @param millisSavedDuringDST the number of milliseconds the time is
     * advanced with respect to standard time when the daylight savings rules
     * are in effect. A positive number, typically one hour (3600000).
     * @stable ICU 2.0
     */
    public void setDSTSavings(int millisSavedDuringDST) {
        unwrapSTZ().setDSTSavings(millisSavedDuringDST);
    }

    /**
     * Returns the amount of time in ms that the clock is advanced during DST.
     * @return the number of milliseconds the time is
     * advanced with respect to standard time when the daylight savings rules
     * are in effect. A positive number, typically one hour (3600000).
     * @stable ICU 2.0
     */
    public int getDSTSavings() {
        return unwrapSTZ().getDSTSavings();
    }

    /**
     * Constructs a SimpleTimeZone that wraps the given
     * java.util.SimpleTimeZone.  Do not call; use the TimeZone
     * API.
     * @internal
     */
    public SimpleTimeZone(java.util.SimpleTimeZone tz) {
        super(tz);
    }

    /**
     * Returns the java.util.SimpleTimeZone that this class wraps.
     */
    java.util.SimpleTimeZone unwrapSTZ() {
        return (java.util.SimpleTimeZone) unwrap();
    }
}

//eof

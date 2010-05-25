/*
*   Copyright (C) 1996-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*/

package com.ibm.icu.util;
import com.ibm.icu.impl.JDKTimeZone;

import java.io.IOException;
import java.util.Date;

import com.ibm.icu.impl.OlsonTimeZone;
import com.ibm.icu.impl.Utility;

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
    private static final long serialVersionUID = -7034676239311322769L;

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
        //this(new java.util.SimpleTimeZone(rawOffset, ID), ID);
        //super(ID);
        construct(rawOffset, 0, 0, 0,
                0, WALL_TIME,
                0, 0, 0,
                0, WALL_TIME,
                MILLIS_PER_HOUR);
        super.setID(ID);
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
/*        this(new java.util.SimpleTimeZone(rawOffset, ID, startMonth, startDay,
                                          startDayOfWeek, startTime, endMonth,
                                          endDay, endDayOfWeek, endTime), ID);*/
       // STZInfo xinfo = getSTZInfo();
       // xinfo.setStart(startMonth, startDay, startDayOfWeek, startTime, -1, false);
       // xinfo.setEnd(endMonth, endDay, endDayOfWeek, endTime, -1, false);
        construct(rawOffset,
                startMonth, startDay, startDayOfWeek,
                startTime, WALL_TIME,
                endMonth, endDay, endDayOfWeek,
                endTime, WALL_TIME,
                MILLIS_PER_HOUR);
        super.setID(ID);
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
        /*this(new java.util.SimpleTimeZone(rawOffset, ID, startMonth, startDay,
                                          startDayOfWeek, startTime, endMonth,
                                          endDay, endDayOfWeek, endTime, dstSavings), ID);*/
        this.raw = rawOffset;
        this.dst = dstSavings;
        /*
        STZInfo xinfo = getSTZInfo();
        xinfo.setStart(startMonth, startDay, startDayOfWeek, startTime, -1, false);
        xinfo.setEnd(endMonth, endDay, endDayOfWeek, endTime, -1, false);
        */

        construct(rawOffset,
                startMonth, startDay, startDayOfWeek,
                startTime, WALL_TIME,
                endMonth, endDay, endDayOfWeek,
                endTime, WALL_TIME,
                dstSavings); 
        super.setID(ID);
    }
    

    /**
     * Set the raw offset.
     *
     * @param offsetMillis the raw offset of the time zone
     * @draft ICU 3.4
     * @provisional This API might change or be removed in a future release.
     */
    public void setRawOffset(int offsetMillis) {
        raw = offsetMillis;
    }
  
    /**
     * get the raw offset.
     *
     * @return the raw offset
     * @internal revisit for ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public int getRawOffset() {
        return raw;
    }


    /**
     * Sets the daylight savings starting year.
     *
     * @param year  The daylight savings starting year.
     * @stable ICU 2.0
     */
    public void setStartYear(int year) {
        //unwrapSTZ().setStartYear(year);

        getSTZInfo().sy = year;
        this.startYear = year;
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
        //unwrapSTZ().setStartRule(month, dayOfWeekInMonth, dayOfWeek, time);

        getSTZInfo().setStart(month, dayOfWeekInMonth, dayOfWeek, time, -1, false);
        setStartRule(month, dayOfWeekInMonth, dayOfWeek, time, WALL_TIME);
    }
    /**
     * Sets the daylight savings starting rule. For example, in the U.S., Daylight Savings
     * Time starts at the first Sunday in April, at 2 AM in standard time.
     * Therefore, you can set the start rule by calling:
     * setStartRule(TimeFields.APRIL, 1, TimeFields.SUNDAY, 2*60*60*1000);
     * The dayOfWeekInMonth and dayOfWeek parameters together specify how to calculate
     * the exact starting date.  Their exact meaning depend on their respective signs,
     * allowing various types of rules to be constructed, as follows:<ul>
     *   <li>If both dayOfWeekInMonth and dayOfWeek are positive, they specify the
     *       day of week in the month (e.g., (2, WEDNESDAY) is the second Wednesday
     *       of the month).
     *   <li>If dayOfWeek is positive and dayOfWeekInMonth is negative, they specify
     *       the day of week in the month counting backward from the end of the month.
     *       (e.g., (-1, MONDAY) is the last Monday in the month)
     *   <li>If dayOfWeek is zero and dayOfWeekInMonth is positive, dayOfWeekInMonth
     *       specifies the day of the month, regardless of what day of the week it is.
     *       (e.g., (10, 0) is the tenth day of the month)
     *   <li>If dayOfWeek is zero and dayOfWeekInMonth is negative, dayOfWeekInMonth
     *       specifies the day of the month counting backward from the end of the
     *       month, regardless of what day of the week it is (e.g., (-2, 0) is the
     *       next-to-last day of the month).
     *   <li>If dayOfWeek is negative and dayOfWeekInMonth is positive, they specify the
     *       first specified day of the week on or after the specfied day of the month.
     *       (e.g., (15, -SUNDAY) is the first Sunday after the 15th of the month
     *       [or the 15th itself if the 15th is a Sunday].)
     *   <li>If dayOfWeek and DayOfWeekInMonth are both negative, they specify the
     *       last specified day of the week on or before the specified day of the month.
     *       (e.g., (-20, -TUESDAY) is the last Tuesday before the 20th of the month
     *       [or the 20th itself if the 20th is a Tuesday].)</ul>
     * @param month the daylight savings starting month. Month is 0-based.
     * eg, 0 for January.
     * @param dayOfWeekInMonth the daylight savings starting
     * day-of-week-in-month. Please see the member description for an example.
     * @param dayOfWeek the daylight savings starting day-of-week. Please see
     * the member description for an example.
     * @param time the daylight savings starting time. Please see the member
     * description for an example.
     */
     
    private void setStartRule(int month, int dayOfWeekInMonth, int dayOfWeek, int time, int mode)
    {
        startMonth     =  month;
        startDay       = dayOfWeekInMonth;
        startDayOfWeek = dayOfWeek;
        startTime      = time;
        startTimeMode  = mode;
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
       // unwrapSTZ().setStartRule(month, dayOfMonth, time);

        getSTZInfo().setStart(month, -1, -1, time, dayOfMonth, false);
        setStartRule(month, dayOfMonth, 0, time, WALL_TIME);
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
        //unwrapSTZ().setStartRule(month, dayOfMonth, dayOfWeek, time, after);

        getSTZInfo().setStart(month, -1, dayOfWeek, time, dayOfMonth, after);
        setStartRule(month, after ? dayOfMonth : -dayOfMonth,
                -dayOfWeek, time, WALL_TIME);

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
    public void setEndRule(int month, int dayOfWeekInMonth, int dayOfWeek, int time) {
        //unwrapSTZ().setEndRule(month, dayOfWeekInMonth, dayOfWeek, time);

        getSTZInfo().setEnd(month, dayOfWeekInMonth, dayOfWeek, time, -1, false);
        setEndRule(month, dayOfWeekInMonth, dayOfWeek, time, WALL_TIME);        
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
        //unwrapSTZ().setEndRule(month, dayOfMonth, time);

        getSTZInfo().setEnd(month, -1, -1, time, dayOfMonth, false);
        setEndRule(month, dayOfMonth, WALL_TIME, time);
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
       // unwrapSTZ().setEndRule(month, dayOfMonth, dayOfWeek, time, after);

        getSTZInfo().setEnd(month, -1, dayOfWeek, time, dayOfMonth, after);
        setEndRule(month, dayOfMonth, dayOfWeek, time, WALL_TIME, after);
    }
    private void setEndRule(int month, int dayOfMonth, int dayOfWeek, 
    						int time, int mode, boolean after){
        setEndRule(month, after ? dayOfMonth : -dayOfMonth, -dayOfWeek, time, mode);
    }
    /**
     * Sets the daylight savings ending rule. For example, in the U.S., Daylight
     * Savings Time ends at the last (-1) Sunday in October, at 2 AM in standard time.
     * Therefore, you can set the end rule by calling:
     * setEndRule(TimeFields.OCTOBER, -1, TimeFields.SUNDAY, 2*60*60*1000);
     * Various other types of rules can be specified by manipulating the dayOfWeek
     * and dayOfWeekInMonth parameters.  For complete details, see the documentation
     * for setStartRule().
     * @param month the daylight savings ending month. Month is 0-based.
     * eg, 0 for January.
     * @param dayOfWeekInMonth the daylight savings ending
     * day-of-week-in-month. See setStartRule() for a complete explanation.
     * @param dayOfWeek the daylight savings ending day-of-week. See setStartRule()
     * for a complete explanation.
     * @param time the daylight savings ending time. Please see the member
     * description for an example.
     */
    private void setEndRule(int month, int dayOfWeekInMonth, int dayOfWeek, int time, int mode){
        endMonth     = month;
        endDay       = dayOfWeekInMonth;
        endDayOfWeek = dayOfWeek;
        endTime      = time;
        endTimeMode  = mode;
        decodeEndRule();
    }
    /**
     * Sets the amount of time in ms that the clock is advanced during DST.
     * @param millisSavedDuringDST the number of milliseconds the time is
     * advanced with respect to standard time when the daylight savings rules
     * are in effect. A positive number, typically one hour (3600000).
     * @stable ICU 2.0
     */
    public void setDSTSavings(int millisSavedDuringDST) {
        //unwrapSTZ().setDSTSavings(millisSavedDuringDST);
        if (millisSavedDuringDST <= 0) {
            throw new IllegalArgumentException();
        }
        dst = millisSavedDuringDST;
    }

    /**
     * Returns the amount of time in ms that the clock is advanced during DST.
     * @return the number of milliseconds the time is
     * advanced with respect to standard time when the daylight savings rules
     * are in effect. A positive number, typically one hour (3600000).
     * @stable ICU 2.0
     */
    public int getDSTSavings() {
        return dst;
    }

    /**
     * Constructs a SimpleTimeZone that wraps the given
     * java.util.SimpleTimeZone.  Do not call; use the TimeZone
     * API.
     * @internal
     */
    public SimpleTimeZone(java.util.SimpleTimeZone tz, String ID) {
        super(tz);
        super.setID(ID);
        dst = tz.getDSTSavings();
        raw = tz.getRawOffset();
    }
    
    /**
     * Returns the java.util.SimpleTimeZone that this class wraps.
     *
    java.util.SimpleTimeZone unwrapSTZ() {
        return (java.util.SimpleTimeZone) unwrap();
    }
    */
    // on JDK 1.4 and later, can't deserialize a SimpleTimeZone as a SimpleTimeZone...
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        String id = getID();
        /*
        if (id!=null && !(zone instanceof java.util.SimpleTimeZone && zone.getID().equals(id))) {
            // System.out.println("*** readjust " + zone.getClass().getName() + " " + zone.getID() + " ***");
            java.util.SimpleTimeZone stz = 
                new java.util.SimpleTimeZone(raw, id);
            if (dst != 0) {
                stz.setDSTSavings(dst);
                // if it is 0, then there shouldn't be start/end rules and the default
                // behavior should be no dst
            }

            if (xinfo != null) {
                xinfo.applyTo(stz);
            }
            zoneJDK = stz;
        }
        */
        /* set all instance variables in this object
         * to the values in zone 
         */            
         if (xinfo != null) {
             xinfo.applyTo(this);
         }
        
    }
    /**
     * Returns a string representation of this object.
     * @return  a string representation of this object
     * @draft ICU 3.4.2
     * @provisional This API might change or be removed in a future release.
     */
    public String toString() {
        return "SimpleTimeZone: " + getID();
    }

    private STZInfo getSTZInfo() {
        if (xinfo == null) {
            xinfo = new STZInfo();
        }
        return xinfo;
    }
//  WARNING: assumes that no rule is measured from the end of February,
//  since we don't handle leap years. Could handle assuming always
//  Gregorian, since we know they didn't have daylight time when
//  Gregorian calendar started.
  //  private static final int[] STATICMONTHLENGTH = new int[]{31,29,31,30,31,30,31,31,30,31,30,31};
    private final byte monthLength[] = staticMonthLength;
    private final static byte staticMonthLength[] = {31,29,31,30,31,30,31,31,30,31,30,31};
    private final static byte staticLeapMonthLength[] = {31,29,31,30,31,30,31,31,30,31,30,31};

//  -------------------------------------

    /**
     * @internal revisit for ICU 3.6
     */
    public int getOffset(int era, int year, int month, int day,
                         int dayOfWeek, int millis) 
    {
        // Check the month before indexing into STATICMONTHLENGTH. This
        // duplicates the test that occurs in the 7-argument getOffset(),
        // however, this is unavoidable. We don't mind because this method, in
        // fact, should not be called; internal code should always call the
        // 7-argument getOffset(), and outside code should use Calendar.get(int
        // field) with fields ZONE_OFFSET and DST_OFFSET. We can't get rid of
        // this method because it's public API. - liu 8/10/98
        if(month < Calendar.JANUARY || month > Calendar.DECEMBER) {
            throw new IllegalArgumentException();
        }

        return getOffset(era, year, month, day, dayOfWeek, millis, staticMonthLength[month]);
    }

    /**
     * @internal revisit for ICU 3.6
     */
    public int getOffset(int era, int year, int month, int day,
                              int dayOfWeek, int millis, 
                              int monthLength)  {
        // Check the month before indexing into STATICMONTHLENGTH. This
        // duplicates a test that occurs in the 9-argument getOffset(),
        // however, this is unavoidable. We don't mind because this method, in
        // fact, should not be called; internal code should always call the
        // 9-argument getOffset(), and outside code should use Calendar.get(int
        // field) with fields ZONE_OFFSET and DST_OFFSET. We can't get rid of
        // this method because it's public API. - liu 8/10/98
        if(month < Calendar.JANUARY || month > Calendar.DECEMBER) {
            throw new IllegalArgumentException();
        }
        
        // TODO FIX We don't handle leap years yet!
        int prevMonthLength = (month >= 1) ? staticMonthLength[month - 1] : 31;

        return getOffset(era, year, month, day, dayOfWeek, millis,
                         monthLength, prevMonthLength);
    }

    int getOffset(int era, int year, int month, int day,
                  int dayOfWeek, int millis, 
                  int monthLength, int prevMonthLength ){

        if (true) {
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
                || millis >= MILLIS_PER_DAY
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
                throw new IllegalArgumentException("Illegal day " + day+" max month len: "+monthLength);
            }
            if (dayOfWeek < Calendar.SUNDAY
                || dayOfWeek > Calendar.SATURDAY) {
                throw new IllegalArgumentException("Illegal day of week " + dayOfWeek);
            }
            if (millis < 0
                || millis >= MILLIS_PER_DAY) {
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

        int result = raw;

        // Bail out if we are before the onset of daylight savings time
        if (!useDaylight || year < startYear || era != GregorianCalendar.AD) return result;

        // Check for southern hemisphere.  We assume that the start and end
        // month are different.
        boolean southern = (startMonth > endMonth);

        // Compare the date to the starting and ending rules.+1 = date>rule, -1
        // = date<rule, 0 = date==rule.
        int startCompare = compareToRule(month, monthLength, prevMonthLength,
                                         day, dayOfWeek, millis,
                                         startTimeMode == UTC_TIME ? -raw : 0,
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
                                       endTimeMode == WALL_TIME ? dst :
                                        (endTimeMode == UTC_TIME ? -raw : 0),
                                       endMode, endMonth, endDayOfWeek,
                                       endDay, endTime);
        }

        // Check for both the northern and southern hemisphere cases.  We
        // assume that in the northern hemisphere, the start rule is before the
        // end rule within the calendar year, and vice versa for the southern
        // hemisphere.
        if ((!southern && (startCompare >= 0 && endCompare < 0)) ||
            (southern && (startCompare >= 0 || endCompare < 0)))
            result += dst;

        return result;
    }

//     -------------------------------------
    private static final int
        DOM_MODE = 1,
        DOW_IN_MONTH_MODE=2,
        DOW_GE_DOM_MODE=3,
        DOW_LE_DOM_MODE=4;

    /**
     * TimeMode is used, together with a millisecond offset after
     * midnight, to specify a rule transition time.  Most rules
     * transition at a local wall time, that is, according to the
     * current time in effect, either standard, or DST.  However, some
     * rules transition at local standard time, and some at a specific
     * UTC time.  Although it might seem that all times could be
     * converted to wall time, thus eliminating the need for this
     * parameter, this is not the case.
     * @draft ICU 3.6
     */
     private static final int WALL_TIME = 0,
                              STANDARD_TIME = 1,
                              UTC_TIME = 2;

    /**
     * Compare a given date in the year to a rule. Return 1, 0, or -1, depending
     * on whether the date is after, equal to, or before the rule date. The
     * millis are compared directly against the ruleMillis, so any
     * standard-daylight adjustments must be handled by the caller.
     *
     * @return  1 if the date is after the rule date, -1 if the date is before
     *          the rule date, or 0 if the date is equal to the rule date.
     */
    private int compareToRule(int month, int monthLen, int prevMonthLen,
                                  int dayOfMonth,
                                  int dayOfWeek, int millis, int millisDelta,
                                  int ruleMode, int ruleMonth, int ruleDayOfWeek,
                                  int ruleDay, int ruleMillis)
    {
        // Make adjustments for startTimeMode and endTimeMode
    	
    	millis += millisDelta;
    	
        while (millis >= MILLIS_PER_DAY) {
            millis -= MILLIS_PER_DAY;
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
            millis += MILLIS_PER_DAY;
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

        if (millis < ruleMillis){
        	return -1;
        }else if (millis > ruleMillis){
        	return 1;
        }else{
        	return 0;
        }
    }
    // data needed for streaming mutated SimpleTimeZones in JDK14
    private int raw;// the TimeZone's raw GMT offset 
    private int dst = 3600000;
    private STZInfo xinfo = null;
    private int startMonth, startDay, startDayOfWeek;   // the month, day, DOW, and time DST starts
    private int startTime;
    private int startTimeMode, endTimeMode; // Mode for startTime, endTime; see TimeMode
    private int endMonth, endDay, endDayOfWeek; // the month, day, DOW, and time DST ends
    private int endTime;
    private int startYear;  // the year these DST rules took effect 
    private boolean useDaylight; // flag indicating whether this TimeZone uses DST
    private int startMode, endMode;   // flags indicating what kind of rules the DST rules are
    
    /**
     * @internal revisit for ICU 3.6
     */
    public boolean useDaylightTime(){
        return useDaylight;
    }
    
    /**
     * @internal revisit for ICU 3.6
     */
    public boolean inDaylightTime(Date date){
        GregorianCalendar gc = new GregorianCalendar(this);
        gc.setTime(date);
        return gc.inDaylightTime();
    }

    /**
     * @internal revisit for ICU 3.6
     */
    public SimpleTimeZone(int raw,  String ID,
                          int startMonth, int startDay,
                          int startDayOfWeek, int startTime,
                          int startTimeMode,
                          int endMonth, int endDay,
                          int endDayOfWeek, int endTime,
                          int endTimeMode,int dst){
        /*this(new java.util.SimpleTimeZone(rawOffsetGMT, ID, savingsStartMonth, savingsStartDay,
                savingsStartDayOfWeek, savingsStartTime, savingsEndMonth,
                savingsEndDay, savingsEndDayOfWeek, savingsEndTime, savingsDST), ID);*/
        construct(raw,
                  startMonth, startDay, startDayOfWeek,
                  startTime, startTimeMode,
                  endMonth, endDay, endDayOfWeek,
                  endTime, endTimeMode,
                  dst);
    }

//  -------------------------------------
/*
    SimpleTimeZone(int rawOffsetGMT, String ID,
        int savingsStartMonth, int savingsStartDay,
        int savingsStartDayOfWeek, int savingsStartTime,
        int savingsEndMonth, int savingsEndDay,
        int savingsEndDayOfWeek, int savingsEndTime)
    {
        construct(rawOffsetGMT,
                  savingsStartMonth, savingsStartDay, savingsStartDayOfWeek,
                  savingsStartTime, WALL_TIME,
                  savingsEndMonth, savingsEndDay, savingsEndDayOfWeek,
                  savingsEndTime, WALL_TIME,
                  TimeZone.MILLIS_PER_DAY);
    }
*/
//     -------------------------------------
/*
    SimpleTimeZone(int rawOffsetGMT, String ID,
        int savingsStartMonth, int savingsStartDay,
        int savingsStartDayOfWeek, int savingsStartTime,
        int savingsEndMonth, int savingsEndDay,
        int savingsEndDayOfWeek, int savingsEndTime,
        int savingsDST)
    {
        construct(rawOffsetGMT,
                  savingsStartMonth, savingsStartDay, savingsStartDayOfWeek,
                  savingsStartTime, WALL_TIME,
                  savingsEndMonth, savingsEndDay, savingsEndDayOfWeek,
                  savingsEndTime, WALL_TIME,
                  savingsDST);
    }
    */
    /**
     * Internal construction method.
     */
    private void construct(int raw,
                           int startMonth,
                           int startDay,
                           int startDayOfWeek,
                           int startTime,
                           int startTimeMode,
                           int endMonth,
                           int endDay,
                           int endDayOfWeek,
                           int endTime,
                           int endTimeMode,
                           int dst) {
        this.raw            = raw;
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
        this.dst            = dst;
        this.startYear      = 0;
        this.startMode      = DOM_MODE;
        this.endMode        = DOM_MODE;

        decodeRules();

        if (dst <= 0) {
            throw new IllegalArgumentException();
        }
    }
    private void decodeRules(){
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

        useDaylight = (boolean)((startDay != 0) && (endDay != 0) ? true : false  );
        if (useDaylight && dst == 0) {
            dst = TimeZone.MILLIS_PER_DAY;
        }
        if (startDay != 0) {
            if (startMonth < Calendar.JANUARY || startMonth > Calendar.DECEMBER) {
                throw new IllegalArgumentException();
            }
            if (startTime < 0 || startTime > TimeZone.MILLIS_PER_DAY ||
                startTimeMode < WALL_TIME || startTimeMode > UTC_TIME) {
                throw new IllegalArgumentException();
            }
            if (startDayOfWeek == 0) {
                startMode = DOM_MODE;
            } else {
                if (startDayOfWeek > 0) {
                    startMode = DOW_IN_MONTH_MODE;
                } else {
                    startDayOfWeek = (int)-startDayOfWeek;
                    if (startDay > 0) {
                        startMode = DOW_GE_DOM_MODE;
                    } else {
                        startDay = (int)-startDay;
                        startMode = DOW_LE_DOM_MODE;
                    }
                }
                if (startDayOfWeek > Calendar.SATURDAY) {
                    throw new IllegalArgumentException();
                }
            }
            if (startMode == DOW_IN_MONTH_MODE) {
                if (startDay < -5 || startDay > 5) {
                    throw new IllegalArgumentException();
                }
            } else if (startDay < 1 || startDay > staticMonthLength[startMonth]) {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Decode the end rule and validate the parameters.  This method is exactly
     * analogous to decodeStartRule().
     * @see #decodeStartRule
     */
    private void decodeEndRule() {
        useDaylight = (boolean)((startDay != 0) && (endDay != 0) ? true : false);
        if (useDaylight && dst == 0) {
            dst = TimeZone.MILLIS_PER_DAY;
        }
        if (endDay != 0) {
            if (endMonth < Calendar.JANUARY || endMonth > Calendar.DECEMBER) {
                throw new IllegalArgumentException();
            }
            if (endTime < 0 || endTime > TimeZone.MILLIS_PER_DAY ||
                endTimeMode < WALL_TIME || endTimeMode > UTC_TIME) {
                throw new IllegalArgumentException();
            }
            if (endDayOfWeek == 0) {
                endMode = DOM_MODE;
            } else {
                if (endDayOfWeek > 0) {
                    endMode = DOW_IN_MONTH_MODE;
                } else {
                    endDayOfWeek = (int)-endDayOfWeek;
                    if (endDay > 0) {
                        endMode = DOW_GE_DOM_MODE;
                    } else {
                        endDay = (int)-endDay;
                        endMode = DOW_LE_DOM_MODE;
                    }
                }
                if (endDayOfWeek > Calendar.SATURDAY) {
                    throw new IllegalArgumentException();
                }
            }
            if (endMode == DOW_IN_MONTH_MODE) {
                if (endDay < -5 || endDay > 5) {
                    throw new IllegalArgumentException();
                }
            } else if (endDay<1 || endDay > staticMonthLength[endMonth]) {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Return true if obj is a SimpleTimeZone equivalent to this.
     * @return true if obj is a SimpleTimeZone equivalent to this
     * @draft ICU 3.4.2
     * @provisional This API might change or be removed in a future release.
     */
    public boolean equals(Object obj){
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SimpleTimeZone that = (SimpleTimeZone) obj;
        return raw     == that.raw &&
            useDaylight     == that.useDaylight &&
            idEquals(getID(),that.getID()) &&
            (!useDaylight
             // Only check rules if using DST
             || (dst            == that.dst &&
                 startMode      == that.startMode &&
                 startMonth     == that.startMonth &&
                 startDay       == that.startDay &&
                 startDayOfWeek == that.startDayOfWeek &&
                 startTime      == that.startTime &&
                 startTimeMode  == that.startTimeMode &&
                 endMode        == that.endMode &&
                 endMonth       == that.endMonth &&
                 endDay         == that.endDay &&
                 endDayOfWeek   == that.endDayOfWeek &&
                 endTime        == that.endTime &&
                 endTimeMode    == that.endTimeMode &&
                 startYear      == that.startYear ));

    }
    private boolean idEquals(String id1, String id2){
        if(id1==null && id2==null){
            return true;
        }
        if(id1!=null && id2!=null){
            return id1.equals(id2);
        }
        return false;
    }

    /**
     * Return the hash code.
     * @return the hash code
     * @draft ICU 3.4.2
     * @provisional This API might change or be removed in a future release.
     */
    public int hashCode(){
    	int ret = (int)( super.hashCode() +
    					 raw ^ (raw>>>8) +
    					 (useDaylight?0:1));
    	if(!useDaylight){
    		ret += (int)(dst ^ (dst>>>10) +
    				startMode ^ (startMode>>>11) +
    				startMonth ^ (startMonth>>>12) +
    				startDay ^ (startDay>>>13) +
    				startDayOfWeek ^ (startDayOfWeek>>>14) +
    				startTime ^ (startTime>>>15) +
    				startTimeMode ^ (startTimeMode>>>16) +
    				endMode ^ (endMode>>>17) +
    				endMonth ^ (endMonth>>>18) +
    				endDay ^ (endDay>>>19) +
    				endDayOfWeek ^ (endDayOfWeek>>>20) +
    				endTime ^ (endTime>>>21) +
    				endTimeMode ^ (endTimeMode>>>22) +
    				startYear ^ (startYear>>>23));
    	}
		return ret;
    }

    /**
     * Return a clone of this time zone.
     * @return a clone of this time zone
     * @draft ICU 3.4.2
     * @provisional This API might change or be removed in a future release.
     */
    public Object clone() {
        SimpleTimeZone clone = new SimpleTimeZone( raw, getID());
        clone.startMonth     = startMonth;
        clone.startDay       = startDay;
        clone.startDayOfWeek = startDayOfWeek;
        clone.startTime      = startTime;
        clone.startTimeMode  = startTimeMode;
        clone.endMonth       = endMonth;
        clone.endDay         = endDay;
        clone.endDayOfWeek   = endDayOfWeek;
        clone.endTime        = endTime;
        clone.endTimeMode    = endTimeMode;
        clone.dst            = dst;
        clone.startYear      = startYear;
        clone.startMode      = startMode;
        clone.endMode        = endMode;
        clone.useDaylight    = useDaylight;
        return clone;
    }

    /**
     * @internal revisit for ICU 3.6
     */
    public boolean hasSameRules(TimeZone othr) {
    	if(!(othr instanceof SimpleTimeZone)){
    		return false;
    	}
    	SimpleTimeZone other = (SimpleTimeZone)othr;
        return other != null &&
        raw     == other.raw &&
        useDaylight     == other.useDaylight &&
        (!useDaylight
         // Only check rules if using DST
         || (dst     == other.dst &&
             startMode      == other.startMode &&
             startMonth     == other.startMonth &&
             startDay       == other.startDay &&
             startDayOfWeek == other.startDayOfWeek &&
             startTime      == other.startTime &&
             startTimeMode  == other.startTimeMode &&
             endMode        == other.endMode &&
             endMonth       == other.endMonth &&
             endDay         == other.endDay &&
             endDayOfWeek   == other.endDayOfWeek &&
             endTime        == other.endTime &&
             endTimeMode    == other.endTimeMode &&
             startYear      == other.startYear));
    }

}

//eof

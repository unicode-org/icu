/*
 * $RCSfile: IBMCalendar.java,v $ $Revision: 1.3 $ $Date: 2000/03/01 17:31:17 $
 *
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 *
 */
package com.ibm.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.text.MessageFormat;
import java.text.DateFormat;
import com.ibm.text.DateFormatSymbols;
import com.ibm.text.SimpleDateFormat;


/**
 * Calendar utility class.
 *
 * The API methods on this class fall into several categories:
 * <ul>
 *  <li>Static methods for retrieving {@link java.text.DateFormat} and
 *      {@link java.text.DateFormatSymbols} objects that match a particular
 *      subclass of <code>Calendar</code>.
 *
 *  <li>Methods that were added to {@link java.util.Calendar Calendar} in JDK 1.2 or that
 *      we hope to add in a later release.
 *
 *  <li>Methods that make <code>Calendar</code> easier to subclass.  This includes
 *      default implementations for methods declared abstract on Calendar,
 *      so that subclasses are not forced to implement them.  In addition,
 *      there are several new utility methods that make it easier for
 *      subclassers to implement methods such as roll, add, computeTime,
 *      and computeFields.
 *  </ul>
 * <p>
 * <b>Note:</b> You should always use {@link #roll roll} and {@link #add add} rather
 * than attempting to perform arithmetic operations directly on the fields
 * of a <tt>Calendar</tt>.  It is quite possible for <tt>Calendar</tt> subclasses
 * to have fields with non-linear behavior, for example missing months
 * or days during non-leap years.  The subclasses' <tt>add</tt> and <tt>roll</tt>
 * methods will take this into account, while simple arithmetic manipulations
 * may give invalid results.
 *  <p>
 *  <b>Subclassing</b>
 *  <br>
 *  It is possible to create subclasses of IBMCalendar that interpret
 *  dates according to other calendar systems that are not yet supported
 *  by Sun or IBM.  In order to do so, you must do the following:
 *  <ul>
 *  <li>Override {@link #getMinimum getMinimum},
 *      {@link #getGreatestMinimum getGreatestMinimum},
 *      {@link #getMaximum getMaximum}, and 
 *      {@link #getLeastMaximum getLeastMaximum} to return
 *      appropriate values for the new calendar system.
 *
 *  <li>If there are any fields whose minimum or maximum value can vary
 *      depending on the actual date set in the calendar (e.g. the number
 *      of days in a month
 *      varies depending on the month and/or year), you must override
 *      {@link #getActualMaximum getActualMaximum} and/or
 *      {@link #getActualMinimum getActualMinimum}
 *      to take this into account.
 *
 *  <li>Override {@link #computeTime computeTime} and {@link #computeFields computeFields}
 *      to perform the conversion from field values to milliseconds and
 *      vice-versa.  These two methods are the real heart of any subclass
 *      and are typically the ones that require the most work.  The protected
 *      utility method {@link #weekNumber weekNumber} is often helpful when implementing
 *      these two methods.
 *
 *  <li>If there are any fields whose ranges are not always continuous, you
 *      must override {@link #roll roll} and {@link #add add} to take this
 *      into account.  For example, in the Hebrew calendar the month "Adar I"
 *      only occurs in leap years; in other years the calendar jumps from
 *      Shevat (month #4) to Adar (month #6).  The
 *      {@link HebrewCalendar#add HebrewCalendar.add} and
 *      {@link HebrewCalendar#roll HebrewCalendar.roll}
 *      methods take this into account, so that adding
 *      1 month to Shevat gives the proper result (Adar) in a non-leap year.
 *      The protected utility method {@link #pinField pinField} is often useful
 *      when implementing these two methods.
 *  </ul>
 *  See the individual descriptions of the above methods for more information
 *  on their implications for subclassing.
 *
 * @author Laura Werner
 */
public abstract class IBMCalendar extends java.util.Calendar {

    private static String copyright = "Copyright \u00a9 1997-1998 IBM Corp. All Rights Reserved.";

    protected IBMCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }
    
    /**
     * Compare this object to another {@link Calendar} or to a {@link Date}.
     * <p>
     * @param when  the {@link Calendar} or {@link Date} object to
     *              be compared against.
     *
     * @return true if this object's absolute time is before that
     *              represented by <code>when</code>, false otherwise.
     *
     * @throws IllegalArgumentException if <code>when</code> is null or is
     *          not a <code>Calendar</code> or <code>Date</code>.
     */
    public boolean before(Object when)
    {
        if (this == when) {
            return false;
        }
        else if (when instanceof Calendar) {
            return getTime().before(((Calendar)when).getTime());
        }
        else if (when instanceof Date) {
            return getTime().before((Date)when);
        } else {
            throw new IllegalArgumentException("argument must be a non-null Calendar or Date");
        }
    }

    /**
     * Compare this object to another {@link Calendar} or to a {@link Date}.
     * <p>
     * @param when  the {@link Calendar} or {@link Date} object to be compared against.
     *
     * @return true if this object's absolute time is after that
     *              represented by <code>when</code>, false otherwise.
     *
     * @throws IllegalArgumentException if <code>when</code> is null or is
     *          not a <code>Calendar</code> or <code>Date</code>.
     */
    public boolean after(Object when)
    {
        if (this == when) {
            return false;
        }
        else if (when instanceof Calendar) {
            return getTime().after(((Calendar)when).getTime());
        }
        else if (when instanceof Date) {
            return getTime().after((Date)when);
        } else {
            throw new IllegalArgumentException("argument must be a non-null Calendar or Date");
        }            
    }

    /**
     * Compares this object to another {@link Calendar} and returns <code>true</code>
     * if they are the same.  Unlike {@link #before before} and {@link #after after},
     * this method
     * compares all of the properties of the calendar rather than comparing only
     * its time in milliseconds.
     * <p>
     * @param obj the {@link Calendar} object to compair against.
     */
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;

        Calendar that = (Calendar) obj;

        return
            getTimeInMillis() == that.getTime().getTime() &&
            isLenient() == that.isLenient() &&
            getFirstDayOfWeek() == that.getFirstDayOfWeek() &&
            getMinimalDaysInFirstWeek() == that.getMinimalDaysInFirstWeek() &&
            getTimeZone().equals(that.getTimeZone());
    }
    
    /**
     * Return the maximum value that this field could have, given the current date.
     * For example, with the Gregorian date "Feb 3, 1997" and the
     * {@link #DAY_OF_MONTH DAY_OF_MONTH} field, the actual
     * maximum is 28; for "Feb 3, 1996" it is 29.
     * <p>
     * <b>Note:</b>This method has been added to {@link java.util.Calendar} in JDK 1.2; it
     * is provided here so that 1.1 clients can take advantage of it as well.
     * <p>
     * <b>Subclassing:</b><br>
     * For the {@link #WEEK_OF_YEAR WEEK_OF_YEAR}, {@link #WEEK_OF_MONTH WEEK_OF_MONTH},
     * and {@link #DAY_OF_WEEK_IN_MONTH DAY_OF_WEEK_IN_MONTH} fields, this method
     * calls <code>getActualMaximum(DAY_OF_YEAR)</code> or
     * <code>getActualMaximum(DAY_OF_MONTH)</code> and then uses that value in a call to
     * {@link #weekNumber weekNumber} to determine the result.  However, for all other
     * fields it iterates between the values returned by
     * {@link #getLeastMaximum getLeastMaximum} and {@link #getMaximum getMaximum}
     * to determine the actual maximum value for the field.
     * <p>
     * There is almost always a more efficient way to accomplish this,
     * and thus you should almost always override this method in your subclass.
     * For most fields, you can simply return the same thing as
     * {@link #getMaximum getMaximum}.  If your class has an internal method that
     * calculates the number of days in a month or year, your getActualMaximum override
     * can use those functions in its implementation.  For fields that you cannot
     * handle efficiently, you can simply call <code>super.getActualMaximum(field)</code>.
     * <p>
     * @param field the field whose maximum is desired
     *
     * @return the maximum of the given field for the current date of this calendar
     *
     * @see #getMaximum
     * @see #getLeastMaximum
     */
    public int getActualMaximum(int field) {
        int result;
        
        switch (field) {
            //
            // For the week-related fields, there's a shortcut.  Since we know the
            // current DAY_OF_WEEK and DAY_OF_MONTH or DAY_OF_YEAR, we can compute
            // the the maximum for this field in terms of the maximum DAY_OF_*,
            // on the theory that's easier to determine.
            //
            case WEEK_OF_YEAR:
                result = weekNumber(getActualMaximum(DAY_OF_YEAR),
                                    get(DAY_OF_YEAR), get(DAY_OF_WEEK));
                break;
                
            case WEEK_OF_MONTH:
                result = weekNumber(getActualMaximum(DAY_OF_MONTH),
                                    get(DAY_OF_MONTH), get(DAY_OF_WEEK));
                break;
          
            case DAY_OF_WEEK_IN_MONTH:
                int weekLength = getMaximum(DAY_OF_WEEK) - getMinimum(DAY_OF_WEEK) + 1;
                result = (getActualMaximum(DAY_OF_MONTH) - 1) / weekLength + 1;
                break;
            
            // For all other fields, do it the hard way....
            default:
                result = getActualHelper(field, getLeastMaximum(field), getMaximum(field));
                break;
        }
        return result;
    }
    
    /**
     * Return the minimum value that this field could have, given the current date.
     * For most fields, this is the same as {@link #getMinimum getMinimum}
     * and {@link #getGreatestMinimum getGreatestMinimum}.  However, some fields,
     * especially those related to week number, are more complicated.
     * <p>
     * For example, assume {@link #getMinimalDaysInFirstWeek getMinimalDaysInFirstWeek}
     * returns 4 and {@link #getFirstDayOfWeek getFirstDayOfWeek} returns SUNDAY.
     * If the first day of the month is Sunday, Monday, Tuesday, or Wednesday
     * there will be four or more days in the first week, so it will be week number 1,
     * and <code>getActualMinimum(WEEK_OF_MONTH)</code> will return 1.  However,
     * if the first of the month is a Thursday, Friday, or Saturday, there are
     * <em>not</em> four days in that week, so it is week number 0, and 
     * <code>getActualMinimum(WEEK_OF_MONTH)</code> will return 0.
     * <p>
     * <b>Note:</b>This method has been added to java.util.Calendar in JDK 1.2; it
     * is provided here so that 1.1 clients can take advantage of it as well.
     * <p>
     * <b>Subclassing:</b><br>
     * For the {@link #WEEK_OF_YEAR WEEK_OF_YEAR}, {@link #WEEK_OF_MONTH WEEK_OF_MONTH},
     * and {@link #DAY_OF_WEEK_IN_MONTH DAY_OF_WEEK_IN_MONTH} fields, this method
     * calls <code>getActualMinimum(DAY_OF_YEAR)</code> or
     * <code>getActualMinimum(DAY_OF_MONTH)</code> and then uses that value in a call to
     * {@link #weekNumber weekNumber} to determine the result.  However, for all other
     * fields it iterates between the values returned by
     * {@link #getMinimum getMinimum} and {@link #getGreatestMinimum getGreatestMinimum}
     * to determine the actual maximum value for the field.
     * <p>
     * There is almost always a more efficient way to accomplish this,
     * and thus you should almost always override this method in your subclass.
     * For most fields, you can simply return the same thing as
     * {@link #getMinimum getMinimum}.  For fields that you cannot
     * handle efficiently, you can simply call <code>super.getActualMaximum(field)</code>.
     * <p>
     * @param field the field whose actual minimum value is desired.
     * @return the minimum of the given field for the current date of this calendar
     *
     * @see #getMinimum
     * @see #getGreatestMinimum
     */
    public int getActualMinimum(int field) {
        int result;
        
        switch (field) {
            //
            // For the week-related fields, there's a shortcut.  Since we know the
            // current DAY_OF_WEEK and DAY_OF_MONTH or DAY_OF_YEAR, we can compute
            // the the maximum for this field in terms of the maximum DAY_OF_*,
            // on the theory that's easier to determine.
            //
            case WEEK_OF_YEAR:
                result = weekNumber(getActualMinimum(DAY_OF_YEAR),
                                    get(DAY_OF_YEAR), get(DAY_OF_WEEK));
                break;
                
            case WEEK_OF_MONTH:
                result = weekNumber(getActualMinimum(DAY_OF_MONTH),
                                    get(DAY_OF_MONTH), get(DAY_OF_WEEK));
                break;
          
            case DAY_OF_WEEK_IN_MONTH:
                int weekLength = getMaximum(DAY_OF_WEEK) - getMinimum(DAY_OF_WEEK) + 1;
                result = (getActualMinimum(DAY_OF_MONTH) - 1) / weekLength + 1;
                break;
            
            // For all other fields, do it the hard way....
            default:
                result = getActualHelper(field, getGreatestMinimum(field), getMinimum(field));
                break;
        }
        return result;
    }

    private int getActualHelper(int field, int startValue, int endValue)
    {
        int result = startValue;
        
        if (startValue == endValue) {
            // if we know that the maximum value is always the same, just return it
            result = startValue;
        } 
        else if (startValue != endValue) {
            // clone the calendar so we don't mess with the real one, and set it to
            // accept anything for the field values
            Calendar work = (Calendar)this.clone();
            work.setLenient(true);

            //
            // now try each value from the start to the end one by one until
            // we get a value that normalizes to another value.  The last value that
            // normalizes to itself is the actual maximum for the current date
            //
            int delta = (endValue > startValue) ? 1 : -1;
            
            startValue += delta;
            result = startValue;

            while (result != endValue) {
                work.set(field, startValue);
                if (work.get(field) != startValue) {
                    break;
                } else {
                    result = startValue;
                    startValue += delta;
                }
            }
        }
        return result;
    }
    
    /**
     * Rolls (up/down) a single unit of time on the given field.  If the
     * field is rolled past its maximum allowable value, it will "wrap" back
     * to its minimum and continue rolling. For
     * example, to roll the current date up by one day, you can call:
     * <p>
     * <code>roll({@link #DATE}, true)</code>
     * <p>
     * When rolling on the {@link #YEAR} field, it will roll the year
     * value in the range between 1 and the value returned by calling
     * {@link #getMaximum getMaximum}({@link #YEAR}).
     * <p>
     * When rolling on certain fields, the values of other fields may conflict and
     * need to be changed.  For example, when rolling the <code>MONTH</code> field
     * for the Gregorian date 1/31/96 upward, the <code>DAY_OF_MONTH</code> field
     * must be adjusted so that the result is 2/29/96 rather than the invalid
     * 2/31/96.
     * <p>
     * <b>Note:</b> Calling <tt>roll(field, true)</tt> N times is <em>not</em>
     * necessarily equivalent to calling <tt>roll(field, N)</tt>.  For example,
     * imagine that you start with the date Gregorian date January 31, 1995.  If you call
     * <tt>roll(Calendar.MONTH, 2)</tt>, the result will be March 31, 1995.
     * But if you call <tt>roll(Calendar.MONTH, true)</tt>, the result will be
     * February 28, 1995.  Calling it one more time will give March 28, 1995, which
     * is usually not the desired result.
     * <p>
     * <b>Note:</b> You should always use <tt>roll</tt> and <tt>add</tt> rather
     * than attempting to perform arithmetic operations directly on the fields
     * of a <tt>Calendar</tt>.  It is quite possible for <tt>Calendar</tt> subclasses
     * to have fields with non-linear behavior, for example missing months
     * or days during non-leap years.  The subclasses' <tt>add</tt> and <tt>roll</tt>
     * methods will take this into account, while simple arithmetic manipulations
     * may give invalid results.
     * <p>
     * @param field the calendar field to roll.
     *
     * @param up    indicates if the value of the specified time field is to be
     *              rolled up or rolled down. Use <code>true</code> if rolling up,
     *              <code>false</code> otherwise.
     *
     * @exception   IllegalArgumentException if the field is invalid or refers
     *              to a field that cannot be handled by this method.
     * @see #roll(int, int)
     * @see #add
     */
    public final void roll(int field, boolean up)
    {
        roll(field, up ? +1 : -1);
    }

    /**
     * Rolls (up/down) a specified amount time on the given field.  For
     * example, to roll the current date up by three days, you can call
     * <code>roll(Calendar.DATE, 3)</code>.  If the
     * field is rolled past its maximum allowable value, it will "wrap" back
     * to its minimum and continue rolling.  
     * For example, calling <code>roll(Calendar.DATE, 10)</code>
     * on a Gregorian calendar set to 4/25/96 will result in the date 4/5/96.
     * <p>
     * When rolling on certain fields, the values of other fields may conflict and
     * need to be changed.  For example, when rolling the {@link #MONTH MONTH} field
     * for the Gregorian date 1/31/96 by +1, the {@link #DAY_OF_MONTH DAY_OF_MONTH} field
     * must be adjusted so that the result is 2/29/96 rather than the invalid
     * 2/31/96.
     * <p>
     * The <code>IBMCalendar</code> implementation of this method is able to roll
     * all fields except for {@link #ERA ERA}, {@link #DST_OFFSET DST_OFFSET},
     * and {@link #ZONE_OFFSET ZONE_OFFSET}.  Subclasses may, of course, add support for
     * additional fields in their overrides of <code>roll</code>.
     * <p>
     * <b>Note:</b> You should always use <tt>roll</tt> and <tt>add</tt> rather
     * than attempting to perform arithmetic operations directly on the fields
     * of a <tt>Calendar</tt>.  It is quite possible for <tt>Calendar</tt> subclasses
     * to have fields with non-linear behavior, for example missing months
     * or days during non-leap years.  The subclasses' <tt>add</tt> and <tt>roll</tt>
     * methods will take this into account, while simple arithmetic manipulations
     * may give invalid results.
     * <p>
     * <b>Subclassing:</b><br>
     * This implementation of <code>roll</code> assumes that the behavior of the
     * field is continuous between its minimum and maximum, which are found by
     * calling {@link #getActualMinimum getActualMinimum} and {@link #getActualMaximum getActualMaximum}.
     * For most such fields, simple addition, subtraction, and modulus operations
     * are sufficient to perform the roll.  For week-related fields,
     * the results of {@link #getFirstDayOfWeek getFirstDayOfWeek} and
     * {@link #getMinimalDaysInFirstWeek getMinimalDaysInFirstWeek} are also necessary.
     * Subclasses can override these two methods if their values differ from the defaults.
     * <p>
     * Subclasses that have fields for which the assumption of continuity breaks
     * down must overide <code>roll</code> to handle those fields specially.
     * For example, in the Hebrew calendar the month "Adar I"
     * only occurs in leap years; in other years the calendar jumps from
     * Shevat (month #4) to Adar (month #6).  The 
     * {@link HebrewCalendar#roll HebrewCalendar.roll} method takes this into account,
     * so that rolling the month of Shevat by one gives the proper result (Adar) in a
     * non-leap year.
     * <p>
     * @param field     the calendar field to roll.
     * @param amount    the amount by which the field should be rolled.
     *
     * @exception   IllegalArgumentException if the field is invalid or refers
     *              to a field that cannot be handled by this method.
     * @see #roll(int, boolean)
     * @see #add
     */
    public void roll(int field, int amount)
    {
        if (amount == 0) return; // Nothing to do

        complete();

        switch (field) {
        case DAY_OF_MONTH:
        case AM_PM:
        case MINUTE:
        case SECOND:
        case MILLISECOND:
            // These are the standard roll instructions.  These work for all
            // simple cases, that is, cases in which the limits are fixed, such
            // as the hour, the day of the month, and the era.
            {
                int min = getActualMinimum(field);
                int max = getActualMaximum(field);
                int gap = max - min + 1;

                int value = internalGet(field) + amount;
                value = (value - min) % gap;
                if (value < 0) value += gap;
                value += min;

                set(field, value);
                return;
            }

        case HOUR:
        case HOUR_OF_DAY:
            // Rolling the hour is difficult on the ONSET and CEASE days of
            // daylight savings.  For example, if the change occurs at
            // 2 AM, we have the following progression:
            // ONSET: 12 Std -> 1 Std -> 3 Dst -> 4 Dst
            // CEASE: 12 Dst -> 1 Dst -> 1 Std -> 2 Std
            // To get around this problem we don't use fields; we manipulate
            // the time in millis directly.
            {
                // Assume min == 0 in calculations below
                Date start = getTime();
                int oldHour = internalGet(field);
                int max = getActualMaximum(field);
                int newHour = (oldHour + amount) % (max + 1);
                if (newHour < 0) {
                    newHour += max + 1;
                }
                setTime(new Date(start.getTime() + HOUR_MS * (newHour - oldHour)));
                return;
            }

        case MONTH:
            // Rolling the month involves both pinning the final value
            // and adjusting the DAY_OF_MONTH if necessary.  We only adjust the
            // DAY_OF_MONTH if, after updating the MONTH field, it is illegal.
            // E.g., <jan31>.roll(MONTH, 1) -> <feb28> or <feb29>.
            {
                int max = getActualMaximum(MONTH);
                int mon = (internalGet(MONTH) + amount) % (max+1);
                
                if (mon < 0) mon += (max + 1);
                set(MONTH, mon);
                
                // Keep the day of month in range.  We don't want to spill over
                // into the next month; e.g., we don't want jan31 + 1 mo -> feb31 ->
                // mar3.
                pinField(DAY_OF_MONTH);
                return;
            }

        case YEAR:
            // Rolling the year can involve pinning the DAY_OF_MONTH.
            set(YEAR, internalGet(YEAR) + amount);
            pinField(MONTH);
            pinField(DAY_OF_MONTH);
            return;

        case WEEK_OF_MONTH:
            {
                // This is tricky, because during the roll we may have to shift
                // to a different day of the week.  For example:

                //    s  m  t  w  r  f  s
                //          1  2  3  4  5
                //    6  7  8  9 10 11 12

                // When rolling from the 6th or 7th back one week, we go to the
                // 1st (assuming that the first partial week counts).  The same
                // thing happens at the end of the month.

                // The other tricky thing is that we have to figure out whether
                // the first partial week actually counts or not, based on the
                // minimal first days in the week.  And we have to use the
                // correct first day of the week to delineate the week
                // boundaries.

                // Here's our algorithm.  First, we find the real boundaries of
                // the month.  Then we discard the first partial week if it
                // doesn't count in this locale.  Then we fill in the ends with
                // phantom days, so that the first partial week and the last
                // partial week are full weeks.  We then have a nice square
                // block of weeks.  We do the usual rolling within this block,
                // as is done elsewhere in this method.  If we wind up on one of
                // the phantom days that we added, we recognize this and pin to
                // the first or the last day of the month.  Easy, eh?

                // Normalize the DAY_OF_WEEK so that 0 is the first day of the week
                // in this locale.  We have dow in 0..6.
                int dow = internalGet(DAY_OF_WEEK) - getFirstDayOfWeek();
                if (dow < 0) dow += 7;

                // Find the day of the week (normalized for locale) for the first
                // of the month.
                int fdm = (dow - internalGet(DAY_OF_MONTH) + 1) % 7;
                if (fdm < 0) fdm += 7;

                // Get the first day of the first full week of the month,
                // including phantom days, if any.  Figure out if the first week
                // counts or not; if it counts, then fill in phantom days.  If
                // not, advance to the first real full week (skip the partial week).
                int start;
                if ((7 - fdm) < getMinimalDaysInFirstWeek())
                    start = 8 - fdm; // Skip the first partial week
                else
                    start = 1 - fdm; // This may be zero or negative

                // Get the day of the week (normalized for locale) for the last
                // day of the month.
                int monthLen = getActualMaximum(DAY_OF_MONTH);
                int ldm = (monthLen - internalGet(DAY_OF_MONTH) + dow) % 7;
                // We know monthLen >= DAY_OF_MONTH so we skip the += 7 step here.

                // Get the limit day for the blocked-off rectangular month; that
                // is, the day which is one past the last day of the month,
                // after the month has already been filled in with phantom days
                // to fill out the last week.  This day has a normalized DOW of 0.
                int limit = monthLen + 7 - ldm;

                // Now roll between start and (limit - 1).
                int gap = limit - start;
                int day_of_month = (internalGet(DAY_OF_MONTH) + amount*7 -
                                    start) % gap;
                if (day_of_month < 0) day_of_month += gap;
                day_of_month += start;

                // Finally, pin to the real start and end of the month.
                if (day_of_month < 1) day_of_month = 1;
                if (day_of_month > monthLen) day_of_month = monthLen;

                // Set the DAY_OF_MONTH.  We rely on the fact that this field
                // takes precedence over everything else (since all other fields
                // are also set at this point).  If this fact changes (if the
                // disambiguation algorithm changes) then we will have to unset
                // the appropriate fields here so that DAY_OF_MONTH is attended
                // to.
                set(DAY_OF_MONTH, day_of_month);
                return;
            }
        case WEEK_OF_YEAR:
            {
                // This follows the outline of WEEK_OF_MONTH, except it applies
                // to the whole year.  Please see the comment for WEEK_OF_MONTH
                // for general notes.

                // Normalize the DAY_OF_WEEK so that 0 is the first day of the week
                // in this locale.  We have dow in 0..6.
                int dow = internalGet(DAY_OF_WEEK) - getFirstDayOfWeek();
                if (dow < 0) dow += 7;

                // Find the day of the week (normalized for locale) for the first
                // of the year.
                int fdy = (dow - internalGet(DAY_OF_YEAR) + 1) % 7;
                if (fdy < 0) fdy += 7;

                // Get the first day of the first full week of the year,
                // including phantom days, if any.  Figure out if the first week
                // counts or not; if it counts, then fill in phantom days.  If
                // not, advance to the first real full week (skip the partial week).
                int start;
                if ((7 - fdy) < getMinimalDaysInFirstWeek())
                    start = 8 - fdy; // Skip the first partial week
                else
                    start = 1 - fdy; // This may be zero or negative

                // Get the day of the week (normalized for locale) for the last
                // day of the year.
                int yearLen = getActualMaximum(DAY_OF_YEAR);
                int ldy = (yearLen - internalGet(DAY_OF_YEAR) + dow) % 7;
                // We know yearLen >= DAY_OF_YEAR so we skip the += 7 step here.

                // Get the limit day for the blocked-off rectangular year; that
                // is, the day which is one past the last day of the year,
                // after the year has already been filled in with phantom days
                // to fill out the last week.  This day has a normalized DOW of 0.
                int limit = yearLen + 7 - ldy;

                // Now roll between start and (limit - 1).
                int gap = limit - start;
                int day_of_year = (internalGet(DAY_OF_YEAR) + amount*7 -
                                    start) % gap;
                if (day_of_year < 0) day_of_year += gap;
                day_of_year += start;

                // Finally, pin to the real start and end of the month.
                if (day_of_year < 1) day_of_year = 1;
                if (day_of_year > yearLen) day_of_year = yearLen;

                // Make sure that the year and day of year are attended to by
                // clearing other fields which would normally take precedence.
                // If the disambiguation algorithm is changed, this section will
                // have to be updated as well.
                set(DAY_OF_YEAR, day_of_year);
                clear(MONTH);
                return;
            }
        case DAY_OF_YEAR:
            {
                // Roll the day of year using millis.  Compute the millis for
                // the start of the year, and get the length of the year.
                long delta = amount * DAY_MS; // Scale up from days to millis
                long min2 = time - (internalGet(DAY_OF_YEAR) - 1) * DAY_MS;
                int yearLength = getActualMaximum(DAY_OF_YEAR);
                time = (time + delta - min2) % (yearLength*DAY_MS);
                if (time < 0) time += yearLength*DAY_MS;
                setTimeInMillis(time + min2);
                return;
            }
        case DAY_OF_WEEK:
            {
                // Roll the day of week using millis.  Compute the millis for
                // the start of the week, using the first day of week setting.
                // Restrict the millis to [start, start+7days).
                long delta = amount * DAY_MS; // Scale up from days to millis
                // Compute the number of days before the current day in this
                // week.  This will be a value 0..6.
                int leadDays = internalGet(DAY_OF_WEEK) - getFirstDayOfWeek();
                if (leadDays < 0) leadDays += 7;
                long min2 = time - leadDays * DAY_MS;
                time = (time + delta - min2) % WEEK_MS;
                if (time < 0) time += WEEK_MS;
                setTimeInMillis(time + min2);
                return;
            }
        case DAY_OF_WEEK_IN_MONTH:
            {
                // Roll the day of week in the month using millis.  Determine
                // the first day of the week in the month, and then the last,
                // and then roll within that range.
                long delta = amount * WEEK_MS; // Scale up from weeks to millis
                // Find the number of same days of the week before this one
                // in this month.
                int preWeeks = (internalGet(DAY_OF_MONTH) - 1) / 7;
                // Find the number of same days of the week after this one
                // in this month.
                int postWeeks = (getActualMaximum(DAY_OF_MONTH) -
                                 internalGet(DAY_OF_MONTH)) / 7;
                // From these compute the min and gap millis for rolling.
                long min2 = time - preWeeks * WEEK_MS;
                long gap2 = WEEK_MS * (preWeeks + postWeeks + 1); // Must add 1!
                // Roll within this range
                time = (time + delta - min2) % gap2;
                if (time < 0) time += gap2;
                setTimeInMillis(time + min2);
                return;
            }
        default:
            // Other fields cannot be rolled by this method
            throw new IllegalArgumentException("IBMCalendar.roll: field not supported");
        }
    }
    
    /**
     * Add a signed amount to a specified field, using this calendar's rules.
     * For example, to add three days to the current date, you can call
     * <code>add(Calendar.DATE, 3)</code>. 
     * <p>
     * When adding to certain fields, the values of other fields may conflict and
     * need to be changed.  For example, when adding one to the {@link #MONTH MONTH} field
     * for the Gregorian date 1/31/96, the {@link #DAY_OF_MONTH DAY_OF_MONTH} field
     * must be adjusted so that the result is 2/29/96 rather than the invalid
     * 2/31/96.
     * <p>
     * The <code>IBMCalendar</code> implementation of this method is able to add to
     * all fields except for {@link #ERA ERA}, {@link #DST_OFFSET DST_OFFSET},
     * and {@link #ZONE_OFFSET ZONE_OFFSET}.  Subclasses may, of course, add support for
     * additional fields in their overrides of <code>add</code>.
     * <p>
     * <b>Note:</b> You should always use <tt>roll</tt> and <tt>add</tt> rather
     * than attempting to perform arithmetic operations directly on the fields
     * of a <tt>Calendar</tt>.  It is quite possible for <tt>Calendar</tt> subclasses
     * to have fields with non-linear behavior, for example missing months
     * or days during non-leap years.  The subclasses' <tt>add</tt> and <tt>roll</tt>
     * methods will take this into account, while simple arithmetic manipulations
     * may give invalid results.
     * <p>
     * <b>Subclassing:</b><br>
     * This implementation of <code>add</code> assumes that the behavior of the
     * field is continuous between its minimum and maximum, which are found by
     * calling {@link #getActualMinimum getActualMinimum} and
     * {@link #getActualMaximum getActualMaximum}.
     * For such fields, simple arithmetic operations are sufficient to
     * perform the add.
     * <p>
     * Subclasses that have fields for which this assumption of continuity breaks
     * down must overide <code>add</code> to handle those fields specially.
     * For example, in the Hebrew calendar the month "Adar I"
     * only occurs in leap years; in other years the calendar jumps from
     * Shevat (month #4) to Adar (month #6).  The 
     * {@link HebrewCalendar#add HebrewCalendar.add} method takes this into account,
     * so that adding one month
     * to a date in Shevat gives the proper result (Adar) in a non-leap year.
     * <p>
     * @param field     the time field.
     * @param amount    the amount to add to the field.
     *
     * @exception   IllegalArgumentException if the field is invalid or refers
     *              to a field that cannot be handled by this method.
     * @see #roll(int, int)
     */
    public void add(int field, int amount)
    {
        if (amount == 0) return;   // Do nothing!

        long delta = amount;

        switch (field) {
        case YEAR:
            {
                int year = get(YEAR) + amount;
                set(YEAR, year);
                pinField(DAY_OF_MONTH);
                return;
            }
        
        case MONTH:
            // About the best we can do for a default implementation is
            // assume a constant number of months per year.  Subclasses
            // with a variable number of months will have to do this
            // one themselves
            {
                int month = this.get(MONTH) + amount;
                int year = get(YEAR);
                int length = getMaximum(MONTH) + 1;
                
                if (month >= 0) {
                    set(YEAR, year + month/length);
                    set(MONTH, month % length);
                } else {
                    set(YEAR, year + (month + 1)/length - 1);
                    month %= length;
                    if (month < 0) {
                        month += length;
                    }
                    set(MONTH, month);
                }
                pinField(DAY_OF_MONTH);
                return;
            }
            
        case WEEK_OF_YEAR:
        case WEEK_OF_MONTH:
        case DAY_OF_WEEK_IN_MONTH:
            delta *= WEEK_MS;
            break;

        case AM_PM:
            delta *= 12 * HOUR_MS;
            break;

        case DAY_OF_MONTH:
        case DAY_OF_YEAR:
        case DAY_OF_WEEK:
            delta *= DAY_MS;
            break;

        case HOUR_OF_DAY:
        case HOUR:
            delta *= HOUR_MS;
            break;

        case MINUTE:
            delta *= MINUTE_MS;
            break;

        case SECOND:
            delta *= SECOND_MS;
            break;

        case MILLISECOND:
            break;

        default:
            throw new IllegalArgumentException("IBMCalendar.add: field not supported");
        }
        setTimeInMillis(getTimeInMillis() + delta);
    }

    
    /**
     * Return the name of this calendar in the language of the given locale.
     */
    public String getDisplayName(Locale loc) {
        return this.getClass().getName();
    }
    
    //-------------------------------------------------------------------------
    // Public static interface for creating custon DateFormats for different
    // types of Calendars.
    //-------------------------------------------------------------------------
    
    /**
     * Create a {@link DateFormat} object that can be used to format dates in
     * the calendar system specified by <code>cal</code>.
     * <p>
     * <b>Note:</b> When this functionality is moved into the core JDK, this method
     * will probably be replaced by a new overload of {@link DateFormat#getInstance}.
     * <p>
     * @param cal   The calendar system for which a date format is desired.
     *
     * @param dateStyle The type of date format desired.  This can be
     *              {@link DateFormat#SHORT}, {@link DateFormat#MEDIUM},
     *              etc.
     *
     * @param locale The locale for which the date format is desired.
     *
     * @see java.text.DateFormat#getDateInstance
     */
    static public DateFormat getDateFormat(Calendar cal, int dateStyle, Locale locale)
    {
        return getDateTimeFormat(cal, locale, dateStyle, -1);
    }
    
    /**
     * Create a {@link DateFormat} object that can be used to format times in
     * the calendar system specified by <code>cal</code>.
     * <p>
     * <b>Note:</b> When this functionality is moved into the core JDK, this method
     * will probably be replaced by a new overload of {@link DateFormat#getInstance}.
     * <p>
     * @param cal   The calendar system for which a time format is desired.
     *
     * @param timeStyle The type of time format desired.  This can be
     *              {@link DateFormat#SHORT}, {@link DateFormat#MEDIUM},
     *              etc.
     *
     * @param locale The locale for which the time format is desired.
     *
     * @see java.text.DateFormat#getTimeInstance
     */
    static public DateFormat getTimeFormat(Calendar cal, int timeStyle, Locale locale)
    {
        return getDateTimeFormat(cal, locale, -1, timeStyle);
    }
    
    /**
     * Create a {@link DateFormat} object that can be used to format dates and times in
     * the calendar system specified by <code>cal</code>.
     * <p>
     * <b>Note:</b> When this functionality is moved into the core JDK, this method
     * will probably be replaced by a new overload of {@link DateFormat#getInstance}.
     * <p>
     * @param cal   The calendar system for which a date/time format is desired.
     *
     * @param dateStyle The type of date format desired.  This can be
     *              {@link DateFormat#SHORT}, {@link DateFormat#MEDIUM},
     *              etc.
     *
     * @param timeStyle The type of time format desired.  This can be
     *              {@link DateFormat#SHORT}, {@link DateFormat#MEDIUM},
     *              etc.
     *
     * @param locale The locale for which the date/time format is desired.
     *
     * @see java.text.DateFormat#getDateTimeInstance
     */
    static public DateFormat getDateTimeFormat(Calendar cal, int dateStyle,
                                        int timeStyle, Locale locale)
    {
        return getDateTimeFormat(cal, locale, dateStyle, timeStyle);
    }

    /**
     * Get the {@link DateFormatSymbols} object that should be used to format a
     * calendar system's dates in the given locale.
     * <p>
     * <b>Note:</b> When this functionality is moved into the core JDK, this method
     * will probably be replace by a new constructor on <tt>DateFormatSymbols</tt>.
     * <p>
     * <b>Subclassing:</b><br>
     * When creating a new Calendar subclass, you must create the
     * {@link ResourceBundle ResourceBundle}
     * containing its {@link DateFormatSymbols DateFormatSymbols} in a specific place.
     * The resource bundle name is based on the calendar's fully-specified
     * class name, with ".resources" inserted at the end of the package name
     * (just before the class name) and "Symbols" appended to the end.
     * For example, the bundle corresponding to "com.ibm.util.HebrewCalendar"
     * is "com.ibm.util.resources.HebrewCalendarSymbols".
     * <p>
     * Within the ResourceBundle, this method searches for five keys: 
     * <ul>
     * <li><b>DayNames</b> -
     *      An array of strings corresponding to each possible
     *      value of the <code>DAY_OF_WEEK</code> field.  Even though
     *      <code>DAY_OF_WEEK</code> starts with <code>SUNDAY</code> = 1,
     *      This array is 0-based; the name for Sunday goes in the
     *      first position, at index 0.  If this key is not found
     *      in the bundle, the day names are inherited from the
     *      default <code>DateFormatSymbols</code> for the requested locale.
     *
     * <li><b>DayAbbreviations</b> -
     *      An array of abbreviated day names corresponding
     *      to the values in the "DayNames" array.  If this key
     *      is not found in the resource bundle, the "DayNames"
     *      values are used instead.  If neither key is found,
     *      the day abbreviations are inherited from the default
     *      <code>DateFormatSymbols</code> for the locale.
     *
     * <li><b>MonthNames</b> -
     *      An array of strings corresponding to each possible
     *      value of the <code>MONTH</code> field.  If this key is not found
     *      in the bundle, the month names are inherited from the
     *      default <code>DateFormatSymbols</code> for the requested locale.
     *
     * <li><b>MonthAbbreviations</b> -
     *      An array of abbreviated day names corresponding
     *      to the values in the "MonthNames" array.  If this key
     *      is not found in the resource bundle, the "MonthNames"
     *      values are used instead.  If neither key is found,
     *      the day abbreviations are inherited from the default
     *      <code>DateFormatSymbols</code> for the locale.
     *
     * <li><b>Eras</b> -
     *      An array of strings corresponding to each possible
     *      value of the <code>ERA</code> field.  If this key is not found
     *      in the bundle, the era names are inherited from the
     *      default <code>DateFormatSymbols</code> for the requested locale.
     * </ul>
     * <p>
     * @param cal       The calendar system whose date format symbols are desired.
     * @param locale    The locale whose symbols are desired.
     *
     * @see java.text.DateFormatSymbols#DateFormatSymbols(java.util.Locale)
     */
    static public DateFormatSymbols getDateFormatSymbols(Calendar cal,
                                                         Locale locale)
    {
        ResourceBundle bundle = null;
        try {
            bundle = getDateFormatBundle(cal, locale);
        }
        catch (MissingResourceException e) {
            if (!(cal instanceof GregorianCalendar)) {
                // Ok for symbols to be missing for a Gregorian calendar, but
                // not for any other type.
                throw e;
            }
        }
        return getDateFormatSymbols(null, bundle, locale);
    }

    /**
     * Fetch a custom calendar's DateFormatSymbols out of the given resource
     * bundle.  Symbols that are not overridden are inherited from the
     * default DateFormatSymbols for the locale.
     * @see java.text.DateFormatSymbols#DateFormatSymbols
     */
    static protected DateFormatSymbols getDateFormatSymbols(DateFormatSymbols result,
                                                            ResourceBundle bundle,
                                                            Locale locale)
    {
        // Get the default symbols for the locale, since most calendars will only
        // need to override month names and will want everything else the same
        if (result == null) {
            result = new DateFormatSymbols(locale);
        }

        //
        // Fetch the day names from the resource bundle.  If they're not found,
        // it's ok; we'll just use the default ones.
        // Allow a null ResourceBundle just for the sake of completeness;
        // this is useful for calendars that don't have any overridden symbols
        //
        if (bundle != null) {
            try {
                String[] temp = bundle.getStringArray("DayNames");
                result.setWeekdays(temp);
                result.setShortWeekdays(temp);

                temp = bundle.getStringArray("DayAbbreviations");
                result.setShortWeekdays( temp );
            }
            catch (MissingResourceException e) {
            }

            try {
                String[] temp = bundle.getStringArray("MonthNames");
                result.setMonths( temp );
                result.setShortMonths( temp );

                temp = bundle.getStringArray("MonthAbbreviations");
                result.setShortMonths( temp );
            }
            catch (MissingResourceException e) {
            }

            try {
                String[] temp = bundle.getStringArray("Eras");
                result.setEras( temp );
            }
            catch (MissingResourceException e) {
            }
        }
        return result;
    }

    protected DateFormatSymbols getDateFormatSymbols(Locale locale) {
        return getDateFormatSymbols(null, getDateFormatBundle(this, locale), locale);
    }
    
    /**
     * Private utility method to retrive a date and/or time format
     * for the specified calendar and locale.  This method has knowledge of
     * (and is partly copied from) the corresponding code in SimpleDateFormat,
     * but it knows how to find the right resource bundle based on the calendar class.
     * <p>
     * @param cal       The calendar system whose date/time format is desired.
     *
     * @param timeStyle The type of time format desired.  This can be
     *                  <code>DateFormat.SHORT</code>, etc, or -1 if the time
     *                  of day should not be included in the format.
     *
     * @param dateStyle The type of date format desired.  This can be
     *                  <code>DateFormat.SHORT</code>, etc, or -1 if the date
     *                  should not be included in the format.
     *
     * @param loc       The locale for which the date/time format is desired.
     *
     * @see java.text.DateFormat#getDateTimeInstance
     */
    static private DateFormat getDateTimeFormat(Calendar cal, Locale loc,
                                            int dateStyle, int timeStyle)
    {
        if (cal instanceof IBMCalendar) {
            return ((IBMCalendar)cal).getDateTimeFormat(dateStyle,timeStyle,loc);
        } else {
            return formatHelper(cal, loc, dateStyle, timeStyle);
        }
    }
    
    protected DateFormat getDateTimeFormat(int dateStyle, int timeStyle, Locale loc) {
        return formatHelper(this, loc, dateStyle, timeStyle);
    }
    
    static private DateFormat formatHelper(Calendar cal, Locale loc,
                                            int dateStyle, int timeStyle)
    {
        // See if there are any custom resources for this calendar
        // If not, just use the default DateFormat
        DateFormat result = null;
        DateFormatSymbols symbols = null;

        ResourceBundle bundle = getDateFormatBundle(cal, loc);

        if (bundle != null) {
            if (cal instanceof IBMCalendar) {
                symbols = ((IBMCalendar)cal).getDateFormatSymbols(loc);
            } else {
                symbols = getDateFormatSymbols(null, bundle, loc);
            }
            
            try {
                String[] patterns = bundle.getStringArray("DateTimePatterns");

                String pattern = null;
                if ((timeStyle >= 0) && (dateStyle >= 0)) {
                    Object[] dateTimeArgs = { patterns[timeStyle],
                                             patterns[dateStyle + 4] };
                    pattern = MessageFormat.format(patterns[8], dateTimeArgs);
                }
                else if (timeStyle >= 0) {
                    pattern = patterns[timeStyle];
                }
                else if (dateStyle >= 0) {
                    pattern = patterns[dateStyle + 4];
                }
                else {
                    throw new IllegalArgumentException("No date or time style specified");
                }
                result = new SimpleDateFormat(pattern, symbols);
            } catch (MissingResourceException e) {
                // No custom patterns
                result = SimpleDateFormat.getDateTimeInstance(dateStyle, timeStyle, loc);
                ((java.text.SimpleDateFormat)result).setDateFormatSymbols(oldStyleSymbols(symbols, loc));
            }
        } else {
            result = DateFormat.getDateTimeInstance(dateStyle, timeStyle, loc);
        }
        result.setCalendar(cal);
        return result;
    }
    
    private static final java.text.DateFormatSymbols oldStyleSymbols(DateFormatSymbols syms, Locale loc) {
        java.text.DateFormatSymbols result = new java.text.DateFormatSymbols(loc);
        result.setAmPmStrings(syms.getAmPmStrings());
        result.setEras(syms.getEras());
        result.setLocalPatternChars(syms.getLocalPatternChars());
        result.setMonths(syms.getMonths());
        result.setShortMonths(syms.getShortMonths());
        result.setShortWeekdays(syms.getShortWeekdays());
        result.setWeekdays(syms.getWeekdays());
        result.setZoneStrings(syms.getZoneStrings());
        return result;
    }

    /**
     * Find the ResourceBundle containing the date format information for
     * a specified calendar subclass in a given locale.
     * <p>
     * The resource bundle name is based on the calendar's fully-specified
     * class name, with ".resources" inserted at the end of the package name
     * (just before the class name) and "Symbols" appended to the end.
     * For example, the bundle corresponding to "com.ibm.util.HebrewCalendar"
     * is "com.ibm.util.resources.HebrewCalendarSymbols".
     */
    static protected ResourceBundle getDateFormatBundle(Calendar cal, Locale locale)
                                  throws MissingResourceException
    {
        // Find the calendar's class name, which we're going to use to construct the
        // resource bundle name.
        String fullName = cal.getClass().getName();
        int lastDot = fullName.lastIndexOf('.');
        String className = fullName.substring(lastDot+1);

        // The name of the ResourceBundle itself is the calendar's fully-qualified
        // name, with ".resources" inserted in the package and "Symbols" appended
        String bundleName = fullName.substring(0, lastDot+1) + "resources."
                                + className + "Symbols";
        
        ResourceBundle result = null;
        try {
            result = ResourceBundle.getBundle(bundleName, locale);
        }
        catch (MissingResourceException e) {
            if (!(cal instanceof GregorianCalendar)) {
                // Ok for symbols to be missing for a Gregorian calendar, but
                // not for any other type.
                throw e;
            }
        }
        return result;
    }
    

    //-------------------------------------------------------------------------
    // Protected utility methods for use by subclasses.  These are very handy
    // for implementing add, roll, and computeFields.
    //-------------------------------------------------------------------------
    
    /**
     * Adjust the specified field so that it is within
     * the allowable range for the date to which this calendar is set.
     * For example, in a Gregorian calendar pinning the {@link #DAY_OF_MONTH DAY_OF_MONTH}
     * field for a calendar set to April 31 would cause it to be set
     * to April 30.
     * <p>
     * <b>Subclassing:</b>
     * <br>
     * This utility method is intended for use by subclasses that need to implement
     * their own overrides of {@link #roll roll} and {@link #add add}.
     * <p>
     * <b>Note:</b>
     * <code>pinField</code> is implemented in terms of
     * {@link #getActualMinimum getActualMinimum}
     * and {@link #getActualMaximum getActualMaximum}.  If either of those methods uses
     * a slow, iterative algorithm for a particular field, it would be
     * unwise to attempt to call <code>pinField</code> for that field.  If you
     * really do need to do so, you should override this method to do
     * something more efficient for that field.
     * <p>
     * @param field The calendar field whose value should be pinned.
     *
     * @see #getActualMinimum
     * @see #getActualMaximum
     */
    protected void pinField(int field) {
        int max = getActualMaximum(field);
        int min = getActualMinimum(field);
        
        if (fields[field] > max) {
            set(field, max);
        } else if (fields[field] < min) {
            set(fields[field], min);
        }
    }

    /**
     * Return the week number of a day, within a period. This may be the week number in
     * a year or the week number in a month. Usually this will be a value >= 1, but if
     * some initial days of the period are excluded from week 1, because
     * {@link #getMinimalDaysInFirstWeek getMinimalDaysInFirstWeek} is > 1, then 
     * the week number will be zero for those
     * initial days. This method requires the day number and day of week for some
     * known date in the period in order to determine the day of week 
     * on the desired day.
     * <p>
     * <b>Subclassing:</b>
     * <br>
     * This method is intended for use by subclasses in implementing their
     * {@link #computeTime computeTime} and/or {@link #computeFields computeFields} methods.
     * It is often useful in {@link #getActualMinimum getActualMinimum} and
     * {@link #getActualMaximum getActualMaximum} as well.
     * <p>
     * This variant is handy for computing the week number of some other
     * day of a period (often the first or last day of the period) when its day
     * of the week is not known but the day number and day of week for some other
     * day in the period (e.g. the current date) <em>is</em> known.
     * <p>
     * @param desiredDay    The {@link #DAY_OF_YEAR DAY_OF_YEAR} or
     *              {@link #DAY_OF_MONTH DAY_OF_MONTH} whose week number is desired.
     *              Should be 1 for the first day of the period.
     *
     * @param knownDayOfPeriod   The {@link #DAY_OF_YEAR DAY_OF_YEAR}
     *              or {@link #DAY_OF_MONTH DAY_OF_MONTH} for a day in the period whose
     *              {@link #DAY_OF_WEEK DAY_OF_WEEK} is specified by the
     *              <code>knownDayOfWeek</code> parameter.
     *              Should be 1 for first day of period.
     *
     * @param knownDayOfWeek  The {@link #DAY_OF_WEEK DAY_OF_WEEK} for the day
     *              corresponding to the <code>knownDayOfPeriod</code> parameter.
     *              1-based with 1=Sunday.
     *
     * @return      The week number (one-based), or zero if the day falls before
     *              the first week because
     *              {@link #getMinimalDaysInFirstWeek getMinimalDaysInFirstWeek}
     *              is more than one.
     */
    protected int weekNumber(int desiredDay, int dayOfPeriod, int dayOfWeek)
    {
        int length = getMaximum(DAY_OF_WEEK) - getMinimum(DAY_OF_WEEK) + 1;

        // Determine the day of the week of the first day of the period
        // in question (either a year or a month).  Zero represents the
        // first day of the week on this calendar.
        int periodStartDayOfWeek = (dayOfWeek - getFirstDayOfWeek() - dayOfPeriod + 1) % length;
        if (periodStartDayOfWeek < 0) periodStartDayOfWeek += length;

        // Compute the week number.  Initially, ignore the first week, which
        // may be fractional (or may not be).  We add periodStartDayOfWeek in
        // order to fill out the first week, if it is fractional.
        int weekNo = (desiredDay + periodStartDayOfWeek - 1)/length;

        // If the first week is long enough, then count it.  If
        // the minimal days in the first week is one, or if the period start
        // is zero, we always increment weekNo.
        if ((length - periodStartDayOfWeek) >= getMinimalDaysInFirstWeek()) ++weekNo;

        return weekNo;
    }

    /**
     * Return the week number of a day, within a period. This may be the week number in
     * a year, or the week number in a month. Usually this will be a value >= 1, but if
     * some initial days of the period are excluded from week 1, because
     * {@link #getMinimalDaysInFirstWeek getMinimalDaysInFirstWeek} is > 1,
     * then the week number will be zero for those
     * initial days. This method requires the day of week for the given date in order to 
     * determine the result.
     * <p>
     * <b>Subclassing:</b>
     * <br>
     * This method is intended for use by subclasses in implementing their
     * {@link #computeTime computeTime} and/or {@link #computeFields computeFields} methods.
     * It is often useful in {@link #getActualMinimum getActualMinimum} and
     * {@link #getActualMaximum getActualMaximum} as well.
     * <p>
     * @param dayOfPeriod   The {@link #DAY_OF_YEAR DAY_OF_YEAR} or
     *                      {@link #DAY_OF_MONTH DAY_OF_MONTH} whose week number is desired.
     *                      Should be 1 for the first day of the period.
     *
     * @param dayofWeek     The {@link #DAY_OF_WEEK DAY_OF_WEEK} for the day
     *                      corresponding to the <code>dayOfPeriod</code> parameter.
     *                      1-based with 1=Sunday.
     *
     * @return      The week number (one-based), or zero if the day falls before
     *              the first week because
     *              {@link #getMinimalDaysInFirstWeek getMinimalDaysInFirstWeek}
     *              is more than one.
     */
    protected final int weekNumber(int dayOfPeriod, int dayOfWeek)
    {
        return weekNumber(dayOfPeriod, dayOfPeriod, dayOfWeek);
    }

    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------
    
    private static final int  SECOND_MS = 1000;
    private static final int  MINUTE_MS = 60*SECOND_MS;
    private static final int  HOUR_MS   = 60*MINUTE_MS;
    private static final long DAY_MS    = 24*HOUR_MS;
    private static final long WEEK_MS   = 7*DAY_MS;
}

/*
 * @(#)Calendar.java	1.48 99/11/05
 *
 * (C) Copyright Taligent, Inc. 1996-1998 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996-1998 - All Rights Reserved
 *
 * Portions copyright (c) 1996-1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 *   The original version of this source code and documentation is copyrighted
 * and owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These
 * materials are provided under terms of a License Agreement between Taligent
 * and Sun. This technology is protected by multiple US and International
 * patents. This notice and attribution to Taligent may not be removed.
 *   Taligent is a registered trademark of Taligent, Inc.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */

package com.ibm.util;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.text.MessageFormat;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import com.ibm.text.DateFormat;
import com.ibm.text.DateFormatSymbols;
import com.ibm.text.SimpleDateFormat;

/**
 * <code>Calendar</code> is an abstract base class for converting between
 * a <code>Date</code> object and a set of integer fields such as
 * <code>YEAR</code>, <code>MONTH</code>, <code>DAY</code>, <code>HOUR</code>,
 * and so on. (A <code>Date</code> object represents a specific instant in
 * time with millisecond precision. See
 * {@link Date}
 * for information about the <code>Date</code> class.)
 *
 * <p><b>Note:</b>  This class is similar, but not identical, to the class
 * <code>java.util.Calendar</code>.  Changes in this class include:
 *
 * <ul>
 *  <li>Static methods for retrieving {@link DateFormat} and
 *      {@link DateFormatSymbols} objects that match a particular
 *      subclass of <code>Calendar</code>.
 *
 *  <li>Methods that make <code>Calendar</code> easier to subclass.  This includes
 *      default implementations for methods declared abstract on Calendar,
 *      so that subclasses are not forced to implement them.  In addition,
 *      there are several new utility methods that make it easier for
 *      subclassers to implement methods such as roll, add, computeTime,
 *      and computeFields.
 *  </ul>
 *
 * <p>
 * Subclasses of <code>Calendar</code> interpret a <code>Date</code>
 * according to the rules of a specific calendar system. The platform
 * provides one concrete subclass of <code>Calendar</code>:
 * <code>GregorianCalendar</code>. Future subclasses could represent
 * the various types of lunar calendars in use in many parts of the world.
 *
 * <p>
 * Like other locale-sensitive classes, <code>Calendar</code> provides a
 * class method, <code>getInstance</code>, for getting a generally useful
 * object of this type. <code>Calendar</code>'s <code>getInstance</code> method
 * returns a <code>GregorianCalendar</code> object whose
 * time fields have been initialized with the current date and time:
 * <blockquote>
 * <pre>
 * Calendar rightNow = Calendar.getInstance();
 * </pre>
 * </blockquote>
 *
 * <p>A <code>Calendar</code> object can produce all the time field values
 * needed to implement the date-time formatting for a particular language and
 * calendar style (for example, Japanese-Gregorian, Japanese-Traditional).
 * <code>Calendar</code> defines the range of values returned by certain fields,
 * as well as their meaning.  For example, the first month of the year has value
 * <code>MONTH</code> == <code>JANUARY</code> for all calendars.  Other values
 * are defined by the concrete subclass, such as <code>ERA</code> and
 * <code>YEAR</code>.  See individual field documentation and subclass
 * documentation for details.
 *
 * <p>When a <code>Calendar</code> is <em>lenient</em>, it accepts a wider range
 * of field values than it produces.  For example, a lenient
 * <code>GregorianCalendar</code> interprets <code>MONTH</code> ==
 * <code>JANUARY</code>, <code>DAY_OF_MONTH</code> == 32 as February 1.  A
 * non-lenient <code>GregorianCalendar</code> throws an exception when given
 * out-of-range field settings.  When calendars recompute field values for
 * return by <code>get()</code>, they normalize them.  For example, a
 * <code>GregorianCalendar</code> always produces <code>DAY_OF_MONTH</code>
 * values between 1 and the length of the month.
 *
 * <p><code>Calendar</code> defines a locale-specific seven day week using two
 * parameters: the first day of the week and the minimal days in first week
 * (from 1 to 7).  These numbers are taken from the locale resource data when a
 * <code>Calendar</code> is constructed.  They may also be specified explicitly
 * through the API.
 *
 * <p>When setting or getting the <code>WEEK_OF_MONTH</code> or
 * <code>WEEK_OF_YEAR</code> fields, <code>Calendar</code> must determine the
 * first week of the month or year as a reference point.  The first week of a
 * month or year is defined as the earliest seven day period beginning on
 * <code>getFirstDayOfWeek()</code> and containing at least
 * <code>getMinimalDaysInFirstWeek()</code> days of that month or year.  Weeks
 * numbered ..., -1, 0 precede the first week; weeks numbered 2, 3,... follow
 * it.  Note that the normalized numbering returned by <code>get()</code> may be
 * different.  For example, a specific <code>Calendar</code> subclass may
 * designate the week before week 1 of a year as week <em>n</em> of the previous
 * year.
 *
 * <p> When computing a <code>Date</code> from time fields, two special
 * circumstances may arise: there may be insufficient information to compute the
 * <code>Date</code> (such as only year and month but no day in the month), or
 * there may be inconsistent information (such as "Tuesday, July 15, 1996" --
 * July 15, 1996 is actually a Monday).
 *
 * <p>
 * <strong>Insufficient information.</strong> The calendar will use default
 * information to specify the missing fields. This may vary by calendar; for
 * the Gregorian calendar, the default for a field is the same as that of the
 * start of the epoch: i.e., YEAR = 1970, MONTH = JANUARY, DATE = 1, etc.
 *
 * <p>
 * <strong>Inconsistent information.</strong> If fields conflict, the calendar
 * will give preference to fields set more recently. For example, when
 * determining the day, the calendar will look for one of the following
 * combinations of fields.  The most recent combination, as determined by the
 * most recently set single field, will be used.
 *
 * <blockquote>
 * <pre>
 * MONTH + DAY_OF_MONTH
 * MONTH + WEEK_OF_MONTH + DAY_OF_WEEK
 * MONTH + DAY_OF_WEEK_IN_MONTH + DAY_OF_WEEK
 * DAY_OF_YEAR
 * DAY_OF_WEEK + WEEK_OF_YEAR
 * </pre>
 * </blockquote>
 *
 * For the time of day:
 *
 * <blockquote>
 * <pre>
 * HOUR_OF_DAY
 * AM_PM + HOUR
 * </pre>
 * </blockquote>
 *
 * <p>
 * <strong>Note:</strong> for some non-Gregorian calendars, different
 * fields may be necessary for complete disambiguation. For example, a full
 * specification of the historial Arabic astronomical calendar requires year,
 * month, day-of-month <em>and</em> day-of-week in some cases.
 *
 * <p>
 * <strong>Note:</strong> There are certain possible ambiguities in
 * interpretation of certain singular times, which are resolved in the
 * following ways:
 * <ol>
 *     <li> 24:00:00 "belongs" to the following day. That is,
 *          23:59 on Dec 31, 1969 &lt; 24:00 on Jan 1, 1970 &lt; 24:01:00 on Jan 1, 1970
 *
 *     <li> Although historically not precise, midnight also belongs to "am",
 *          and noon belongs to "pm", so on the same day,
 *          12:00 am (midnight) &lt; 12:01 am, and 12:00 pm (noon) &lt; 12:01 pm
 * </ol>
 *
 * <p>
 * The date or time format strings are not part of the definition of a
 * calendar, as those must be modifiable or overridable by the user at
 * runtime. Use {@link DateFormat}
 * to format dates.
 *
 * <p><strong>Field manipulation methods</strong></p>
 * 
 * <p><code>Calendar</code> fields can be changed using three methods:
 * <code>set()</code>, <code>add()</code>, and <code>roll()</code>.</p>
 * 
 * <p><strong><code>set(f, value)</code></strong> changes field
 * <code>f</code> to <code>value</code>.  In addition, it sets an
 * internal member variable to indicate that field <code>f</code> has
 * been changed. Although field <code>f</code> is changed immediately,
 * the calendar's milliseconds is not recomputed until the next call to
 * <code>get()</code>, <code>getTime()</code>, or
 * <code>getTimeInMillis()</code> is made. Thus, multiple calls to
 * <code>set()</code> do not trigger multiple, unnecessary
 * computations. As a result of changing a field using
 * <code>set()</code>, other fields may also change, depending on the
 * field, the field value, and the calendar system. In addition,
 * <code>get(f)</code> will not necessarily return <code>value</code>
 * after the fields have been recomputed. The specifics are determined by
 * the concrete calendar class.</p>
 * 
 * <p><em>Example</em>: Consider a <code>GregorianCalendar</code>
 * originally set to August 31, 1999. Calling <code>set(Calendar.MONTH,
 * Calendar.SEPTEMBER)</code> sets the calendar to September 31,
 * 1999. This is a temporary internal representation that resolves to
 * October 1, 1999 if <code>getTime()</code>is then called. However, a
 * call to <code>set(Calendar.DAY_OF_MONTH, 30)</code> before the call to
 * <code>getTime()</code> sets the calendar to September 30, 1999, since
 * no recomputation occurs after <code>set()</code> itself.</p>
 * 
 * <p><strong><code>add(f, delta)</code></strong> adds <code>delta</code>
 * to field <code>f</code>.  This is equivalent to calling <code>set(f,
 * get(f) + delta)</code> with two adjustments:</p>
 * 
 * <blockquote>
 *   <p><strong>Add rule 1</strong>. The value of field <code>f</code>
 *   after the call minus the value of field <code>f</code> before the
 *   call is <code>delta</code>, modulo any overflow that has occurred in
 *   field <code>f</code>. Overflow occurs when a field value exceeds its
 *   range and, as a result, the next larger field is incremented or
 *   decremented and the field value is adjusted back into its range.</p>
 * 
 *   <p><strong>Add rule 2</strong>. If a smaller field is expected to be
 *   invariant, but &nbsp; it is impossible for it to be equal to its
 *   prior value because of changes in its minimum or maximum after field
 *   <code>f</code> is changed, then its value is adjusted to be as close
 *   as possible to its expected value. A smaller field represents a
 *   smaller unit of time. <code>HOUR</code> is a smaller field than
 *   <code>DAY_OF_MONTH</code>. No adjustment is made to smaller fields
 *   that are not expected to be invariant. The calendar system
 *   determines what fields are expected to be invariant.</p>
 * </blockquote>
 * 
 * <p>In addition, unlike <code>set()</code>, <code>add()</code> forces
 * an immediate recomputation of the calendar's milliseconds and all
 * fields.</p>
 * 
 * <p><em>Example</em>: Consider a <code>GregorianCalendar</code>
 * originally set to August 31, 1999. Calling <code>add(Calendar.MONTH,
 * 13)</code> sets the calendar to September 30, 2000. <strong>Add rule
 * 1</strong> sets the <code>MONTH</code> field to September, since
 * adding 13 months to August gives September of the next year. Since
 * <code>DAY_OF_MONTH</code> cannot be 31 in September in a
 * <code>GregorianCalendar</code>, <strong>add rule 2</strong> sets the
 * <code>DAY_OF_MONTH</code> to 30, the closest possible value. Although
 * it is a smaller field, <code>DAY_OF_WEEK</code> is not adjusted by
 * rule 2, since it is expected to change when the month changes in a
 * <code>GregorianCalendar</code>.</p>
 * 
 * <p><strong><code>roll(f, delta)</code></strong> adds
 * <code>delta</code> to field <code>f</code> without changing larger
 * fields. This is equivalent to calling <code>add(f, delta)</code> with
 * the following adjustment:</p>
 * 
 * <blockquote>
 *   <p><strong>Roll rule</strong>. Larger fields are unchanged after the
 *   call. A larger field represents a larger unit of
 *   time. <code>DAY_OF_MONTH</code> is a larger field than
 *   <code>HOUR</code>.</p>
 * </blockquote>
 * 
 * <p><em>Example</em>: Consider a <code>GregorianCalendar</code>
 * originally set to August 31, 1999. Calling <code>roll(Calendar.MONTH,
 * 8)</code> sets the calendar to April 30, <strong>1999</strong>.  Add
 * rule 1 sets the <code>MONTH</code> field to April. Using a
 * <code>GregorianCalendar</code>, the <code>DAY_OF_MONTH</code> cannot
 * be 31 in the month April. Add rule 2 sets it to the closest possible
 * value, 30. Finally, the <strong>roll rule</strong> maintains the
 * <code>YEAR</code> field value of 1999.</p>
 * 
 * <p><em>Example</em>: Consider a <code>GregorianCalendar</code>
 * originally set to Sunday June 6, 1999. Calling
 * <code>roll(Calendar.WEEK_OF_MONTH, -1)</code> sets the calendar to
 * Tuesday June 1, 1999, whereas calling
 * <code>add(Calendar.WEEK_OF_MONTH, -1)</code> sets the calendar to
 * Sunday May 30, 1999. This is because the roll rule imposes an
 * additional constraint: The <code>MONTH</code> must not change when the
 * <code>WEEK_OF_MONTH</code> is rolled. Taken together with add rule 1,
 * the resultant date must be between Tuesday June 1 and Saturday June
 * 5. According to add rule 2, the <code>DAY_OF_WEEK</code>, an invariant
 * when changing the <code>WEEK_OF_MONTH</code>, is set to Tuesday, the
 * closest possible value to Sunday (where Sunday is the first day of the
 * week).</p>
 * 
 * <p><strong>Usage model</strong>. To motivate the behavior of
 * <code>add()</code> and <code>roll()</code>, consider a user interface
 * component with increment and decrement buttons for the month, day, and
 * year, and an underlying <code>GregorianCalendar</code>. If the
 * interface reads January 31, 1999 and the user presses the month
 * increment button, what should it read? If the underlying
 * implementation uses <code>set()</code>, it might read March 3, 1999. A
 * better result would be February 28, 1999. Furthermore, if the user
 * presses the month increment button again, it should read March 31,
 * 1999, not March 28, 1999. By saving the original date and using either
 * <code>add()</code> or <code>roll()</code>, depending on whether larger
 * fields should be affected, the user interface can behave as most users
 * will intuitively expect.</p>
 *
 * <p><b>SUBCLASSING</b>
 *
 * <p>While ICU4J supports a number of calendar systems, it does not
 * support all of them.  In order to support a new calendar system,
 * <code>Calendar</code> must be subclassed.
 * Subclassing <code>Calendar</code> is not trivial.  This class has
 * been modified from the original <code>java.util.Calendar</code>
 * to improve subclassing support, but there are still several things
 * to look out for.  To create a subclass of <code>Calendar</code>,
 * begin by doing the following:
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
 * <p><b>Note:</b> You should always use {@link #roll roll} and {@link #add add} rather
 * than attempting to perform arithmetic operations directly on the fields
 * of a <tt>Calendar</tt>.  It is quite possible for <tt>Calendar</tt> subclasses
 * to have fields with non-linear behavior, for example missing months
 * or days during non-leap years.  The subclasses' <tt>add</tt> and <tt>roll</tt>
 * methods will take this into account, while simple arithmetic manipulations
 * may give invalid results.
 *
 * @see          Date
 * @see          GregorianCalendar
 * @see          TimeZone
 * @see          DateFormat
 * @version      $Revision: 1.10 $ $Date: 2000/10/27 22:25:52 $
 * @author Mark Davis, David Goldsmith, Chen-Lieh Huang, Alan Liu, Laura Werner
 * @since JDK1.1
 */
public abstract class Calendar implements Serializable, Cloneable {

    // Data flow in Calendar
    // ---------------------

    // The current time is represented in two ways by Calendar: as UTC
    // milliseconds from the epoch start (1 January 1970 0:00 UTC), and as local
    // fields such as MONTH, HOUR, AM_PM, etc.  It is possible to compute the
    // millis from the fields, and vice versa.  The data needed to do this
    // conversion is encapsulated by a TimeZone object owned by the Calendar.
    // The data provided by the TimeZone object may also be overridden if the
    // user sets the ZONE_OFFSET and/or DST_OFFSET fields directly. The class
    // keeps track of what information was most recently set by the caller, and
    // uses that to compute any other information as needed.

    // If the user sets the fields using set(), the data flow is as follows.
    // This is implemented by the Calendar subclass's computeTime() method.
    // During this process, certain fields may be ignored.  The disambiguation
    // algorithm for resolving which fields to pay attention to is described
    // above.

    //   local fields (YEAR, MONTH, DATE, HOUR, MINUTE, etc.)
    //           |
    //           | Using Calendar-specific algorithm
    //           V
    //   local standard millis
    //           |
    //           | Using TimeZone or user-set ZONE_OFFSET / DST_OFFSET
    //           V
    //   UTC millis (in time data member)

    // If the user sets the UTC millis using setTime(), the data flow is as
    // follows.  This is implemented by the Calendar subclass's computeFields()
    // method.

    //   UTC millis (in time data member)
    //           |
    //           | Using TimeZone getOffset()
    //           V
    //   local standard millis
    //           |
    //           | Using Calendar-specific algorithm
    //           V
    //   local fields (YEAR, MONTH, DATE, HOUR, MINUTE, etc.)

    // In general, a round trip from fields, through local and UTC millis, and
    // back out to fields is made when necessary.  This is implemented by the
    // complete() method.  Resolving a partial set of fields into a UTC millis
    // value allows all remaining fields to be generated from that value.  If
    // the Calendar is lenient, the fields are also renormalized to standard
    // ranges when they are regenerated.

    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * era, e.g., AD or BC in the Julian calendar. This is a calendar-specific
     * value; see subclass documentation.
     * @see GregorianCalendar#AD
     * @see GregorianCalendar#BC
     */
    public final static int ERA = 0;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * year. This is a calendar-specific value; see subclass documentation.
     */
    public final static int YEAR = 1;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * month. This is a calendar-specific value. The first month of the year is
     * <code>JANUARY</code>; the last depends on the number of months in a year.
     * @see #JANUARY
     * @see #FEBRUARY
     * @see #MARCH
     * @see #APRIL
     * @see #MAY
     * @see #JUNE
     * @see #JULY
     * @see #AUGUST
     * @see #SEPTEMBER
     * @see #OCTOBER
     * @see #NOVEMBER
     * @see #DECEMBER
     * @see #UNDECIMBER
     */
    public final static int MONTH = 2;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * week number within the current year.  The first week of the year, as
     * defined by <code>getFirstDayOfWeek()</code> and
     * <code>getMinimalDaysInFirstWeek()</code>, has value 1.  Subclasses define
     * the value of <code>WEEK_OF_YEAR</code> for days before the first week of
     * the year.
     * @see #getFirstDayOfWeek
     * @see #getMinimalDaysInFirstWeek
     */
    public final static int WEEK_OF_YEAR = 3;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * week number within the current month.  The first week of the month, as
     * defined by <code>getFirstDayOfWeek()</code> and
     * <code>getMinimalDaysInFirstWeek()</code>, has value 1.  Subclasses define
     * the value of <code>WEEK_OF_MONTH</code> for days before the first week of
     * the month.
     * @see #getFirstDayOfWeek
     * @see #getMinimalDaysInFirstWeek
     */
    public final static int WEEK_OF_MONTH = 4;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * day of the month. This is a synonym for <code>DAY_OF_MONTH</code>.
     * The first day of the month has value 1.
     * @see #DAY_OF_MONTH
     */
    public final static int DATE = 5;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * day of the month. This is a synonym for <code>DATE</code>.
     * The first day of the month has value 1.
     * @see #DATE
     */
    public final static int DAY_OF_MONTH = 5;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the day
     * number within the current year.  The first day of the year has value 1.
     */
    public final static int DAY_OF_YEAR = 6;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the day
     * of the week.  This field takes values <code>SUNDAY</code>,
     * <code>MONDAY</code>, <code>TUESDAY</code>, <code>WEDNESDAY</code>,
     * <code>THURSDAY</code>, <code>FRIDAY</code>, and <code>SATURDAY</code>.
     * @see #SUNDAY
     * @see #MONDAY
     * @see #TUESDAY
     * @see #WEDNESDAY
     * @see #THURSDAY
     * @see #FRIDAY
     * @see #SATURDAY
     */
    public final static int DAY_OF_WEEK = 7;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * ordinal number of the day of the week within the current month. Together
     * with the <code>DAY_OF_WEEK</code> field, this uniquely specifies a day
     * within a month.  Unlike <code>WEEK_OF_MONTH</code> and
     * <code>WEEK_OF_YEAR</code>, this field's value does <em>not</em> depend on
     * <code>getFirstDayOfWeek()</code> or
     * <code>getMinimalDaysInFirstWeek()</code>.  <code>DAY_OF_MONTH 1</code>
     * through <code>7</code> always correspond to <code>DAY_OF_WEEK_IN_MONTH
     * 1</code>; <code>8</code> through <code>15</code> correspond to
     * <code>DAY_OF_WEEK_IN_MONTH 2</code>, and so on.
     * <code>DAY_OF_WEEK_IN_MONTH 0</code> indicates the week before
     * <code>DAY_OF_WEEK_IN_MONTH 1</code>.  Negative values count back from the
     * end of the month, so the last Sunday of a month is specified as
     * <code>DAY_OF_WEEK = SUNDAY, DAY_OF_WEEK_IN_MONTH = -1</code>.  Because
     * negative values count backward they will usually be aligned differently
     * within the month than positive values.  For example, if a month has 31
     * days, <code>DAY_OF_WEEK_IN_MONTH -1</code> will overlap
     * <code>DAY_OF_WEEK_IN_MONTH 5</code> and the end of <code>4</code>.
     * @see #DAY_OF_WEEK
     * @see #WEEK_OF_MONTH
     */
    public final static int DAY_OF_WEEK_IN_MONTH = 8;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating
     * whether the <code>HOUR</code> is before or after noon.
     * E.g., at 10:04:15.250 PM the <code>AM_PM</code> is <code>PM</code>.
     * @see #AM
     * @see #PM
     * @see #HOUR
     */
    public final static int AM_PM = 9;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * hour of the morning or afternoon. <code>HOUR</code> is used for the 12-hour
     * clock.
     * E.g., at 10:04:15.250 PM the <code>HOUR</code> is 10.
     * @see #AM_PM
     * @see #HOUR_OF_DAY
     */
    public final static int HOUR = 10;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * hour of the day. <code>HOUR_OF_DAY</code> is used for the 24-hour clock.
     * E.g., at 10:04:15.250 PM the <code>HOUR_OF_DAY</code> is 22.
     * @see #HOUR
     */
    public final static int HOUR_OF_DAY = 11;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * minute within the hour.
     * E.g., at 10:04:15.250 PM the <code>MINUTE</code> is 4.
     */
    public final static int MINUTE = 12;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * second within the minute.
     * E.g., at 10:04:15.250 PM the <code>SECOND</code> is 15.
     */
    public final static int SECOND = 13;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * millisecond within the second.
     * E.g., at 10:04:15.250 PM the <code>MILLISECOND</code> is 250.
     */
    public final static int MILLISECOND = 14;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * raw offset from GMT in milliseconds.
     */
    public final static int ZONE_OFFSET = 15;
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * daylight savings offset in milliseconds.
     */
    public final static int DST_OFFSET = 16;
    /**
     * The number of distict fields recognized by <code>get</code> and <code>set</code>.
     * Field numbers range from <code>0..FIELD_COUNT-1</code>.
     */
    public final static int FIELD_COUNT = 17;

    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Sunday.
     */
    public final static int SUNDAY = 1;
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Monday.
     */
    public final static int MONDAY = 2;
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Tuesday.
     */
    public final static int TUESDAY = 3;
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Wednesday.
     */
    public final static int WEDNESDAY = 4;
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Thursday.
     */
    public final static int THURSDAY = 5;
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Friday.
     */
    public final static int FRIDAY = 6;
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Saturday.
     */
    public final static int SATURDAY = 7;

    /**
     * Value of the <code>MONTH</code> field indicating the
     * first month of the year.
     */
    public final static int JANUARY = 0;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * second month of the year.
     */
    public final static int FEBRUARY = 1;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * third month of the year.
     */
    public final static int MARCH = 2;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * fourth month of the year.
     */
    public final static int APRIL = 3;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * fifth month of the year.
     */
    public final static int MAY = 4;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * sixth month of the year.
     */
    public final static int JUNE = 5;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * seventh month of the year.
     */
    public final static int JULY = 6;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * eighth month of the year.
     */
    public final static int AUGUST = 7;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * ninth month of the year.
     */
    public final static int SEPTEMBER = 8;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * tenth month of the year.
     */
    public final static int OCTOBER = 9;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * eleventh month of the year.
     */
    public final static int NOVEMBER = 10;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * twelfth month of the year.
     */
    public final static int DECEMBER = 11;
    /**
     * Value of the <code>MONTH</code> field indicating the
     * thirteenth month of the year. Although <code>GregorianCalendar</code>
     * does not use this value, lunar calendars do.
     */
    public final static int UNDECIMBER = 12;

    /**
     * Value of the <code>AM_PM</code> field indicating the
     * period of the day from midnight to just before noon.
     */
    public final static int AM = 0;
    /**
     * Value of the <code>AM_PM</code> field indicating the
     * period of the day from noon to just before midnight.
     */
    public final static int PM = 1;

    /**
     * Value returned by getDayOfWeekType(int dayOfWeek) to indicate a
     * weekday.
     * @see #WEEKEND
     * @see #WEEKEND_ONSET
     * @see #WEEKEND_CEASE
     * @see #getDayOfWeekType
     */
    public static final int WEEKDAY = 0;

    /**
     * Value returned by getDayOfWeekType(int dayOfWeek) to indicate a
     * weekend day.
     * @see #WEEKDAY
     * @see #WEEKEND_ONSET
     * @see #WEEKEND_CEASE
     * @see #getDayOfWeekType
     */
    public static final int WEEKEND = 1;

    /**
     * Value returned by getDayOfWeekType(int dayOfWeek) to indicate a
     * day that starts as a weekday and transitions to the weekend.
     * Call getWeekendTransition() to get the point of transition.
     * @see #WEEKDAY
     * @see #WEEKEND
     * @see #WEEKEND_CEASE
     * @see #getDayOfWeekType
     */
    public static final int WEEKEND_ONSET = 2;

    /**
     * Value returned by getDayOfWeekType(int dayOfWeek) to indicate a
     * day that starts as the weekend and transitions to a weekday.
     * Call getWeekendTransition() to get the point of transition.
     * @see #WEEKDAY
     * @see #WEEKEND
     * @see #WEEKEND_ONSET
     * @see #getDayOfWeekType
     */
    public static final int WEEKEND_CEASE = 3;

    // Internal notes:
    // Calendar contains two kinds of time representations: current "time" in
    // milliseconds, and a set of time "fields" representing the current time.
    // The two representations are usually in sync, but can get out of sync
    // as follows.
    // 1. Initially, no fields are set, and the time is invalid.
    // 2. If the time is set, all fields are computed and in sync.
    // 3. If a single field is set, the time is invalid.
    // Recomputation of the time and fields happens when the object needs
    // to return a result to the user, or use a result for a computation.

    /**
     * The field values for the currently set time for this calendar.
     * This is an array of <code>FIELD_COUNT</code> integers, with index values
     * <code>ERA</code> through <code>DST_OFFSET</code>.
     * @serial
     */
    protected int           fields[]; // NOTE: Make transient when possible

    /**
     * Pseudo-time-stamps which specify when each field was set. There
     * are two special values, UNSET and INTERNALLY_SET. Values from
     * MINIMUM_USER_SET to Integer.MAX_VALUE are legal user set values.
     */
    transient int           stamp[];

    /**
     * The currently set time for this calendar, expressed in milliseconds after
     * January 1, 1970, 0:00:00 GMT.
     * @see #isTimeSet
     * @serial
     */
    protected long          time;

    /**
     * True if then the value of <code>time</code> is valid.
     * The time is made invalid by a change to an item of <code>field[]</code>.
     * @see #time
     * @serial
     */
    protected boolean       isTimeSet; // NOTE: Make transient when possible

    /**
     * True if <code>fields[]</code> are in sync with the currently set time.
     * If false, then the next attempt to get the value of a field will
     * force a recomputation of all fields from the current value of
     * <code>time</code>.
     * @serial
     */
    protected boolean       areFieldsSet; // NOTE: Make transient when possible

    /**
     * True if all fields have been set.
     * @serial
     */
    transient boolean       areAllFieldsSet;

    /**
     * True if this calendar allows out-of-range field values during computation
     * of <code>time</code> from <code>fields[]</code>.
     * @see #setLenient
     * @serial
     */
    private boolean         lenient = true;

    /**
     * The <code>TimeZone</code> used by this calendar. </code>Calendar</code>
     * uses the time zone data to translate between locale and GMT time.
     * @serial
     */
    private TimeZone        zone;

    /**
     * The first day of the week, with possible values <code>SUNDAY</code>,
     * <code>MONDAY</code>, etc.  This is a locale-dependent value.
     * @serial
     */
    private int             firstDayOfWeek;

    /**
     * The number of days required for the first week in a month or year,
     * with possible values from 1 to 7.  This is a locale-dependent value.
     * @serial
     */
    private int             minimalDaysInFirstWeek;

    /**
     * First day of the weekend in this calendar's locale.  Must be in
     * the range SUNDAY...SATURDAY (1..7).  The weekend starts at
     * weekendOnsetMillis milliseconds after midnight on that day of
     * the week.  This value is taken from locale resource data.
     */
    private int weekendOnset;

    /**
     * Milliseconds after midnight at which the weekend starts on the
     * day of the week weekendOnset.  Times that are greater than or
     * equal to weekendOnsetMillis are considered part of the weekend.
     * Must be in the range 0..24*60*60*1000-1.  This value is taken
     * from locale resource data.
     */
    private int weekendOnsetMillis;

    /**
     * Day of the week when the weekend stops in this calendar's
     * locale.  Must be in the range SUNDAY...SATURDAY (1..7).  The
     * weekend stops at weekendCeaseMillis milliseconds after midnight
     * on that day of the week.  This value is taken from locale
     * resource data.
     */
    private int weekendCease;

    /**
     * Milliseconds after midnight at which the weekend stops on the
     * day of the week weekendCease.  Times that are greater than or
     * equal to weekendCeaseMillis are considered not to be the
     * weekend.  Must be in the range 0..24*60*60*1000-1.  This value
     * is taken from locale resource data.
     */
    private int weekendCeaseMillis;

    /**
     * Cache to hold the firstDayOfWeek and minimalDaysInFirstWeek
     * of a Locale.
     */
    private static Hashtable cachedLocaleData = new Hashtable(3);

    // Special values of stamp[]
    static final int        UNSET = 0;
    static final int        INTERNALLY_SET = 1;
    static final int        MINIMUM_USER_STAMP = 2;

    /**
     * The next available value for <code>stamp[]</code>, an internal array.
     * This actually should not be written out to the stream, and will probably
     * be removed from the stream in the near future.  In the meantime,
     * a value of <code>MINIMUM_USER_STAMP</code> should be used.
     * @serial
     */
    private int             nextStamp = MINIMUM_USER_STAMP;

    // the internal serial version which says which version was written
    // - 0 (default) for version up to JDK 1.1.5
    // - 1 for version from JDK 1.1.6, which writes a correct 'time' value
    //     as well as compatible values for other fields.  This is a
    //     transitional format.
    // - 2 (not implemented yet) a future version, in which fields[],
    //     areFieldsSet, and isTimeSet become transient, and isSet[] is
    //     removed. In JDK 1.1.6 we write a format compatible with version 2.
    static final int        currentSerialVersion = 1;
    
    /**
     * The version of the serialized data on the stream.  Possible values:
     * <dl>
     * <dt><b>0</b> or not present on stream</dt>
     * <dd>
     * JDK 1.1.5 or earlier.
     * </dd>
     * <dt><b>1</b></dt>
     * <dd>
     * JDK 1.1.6 or later.  Writes a correct 'time' value
     * as well as compatible values for other fields.  This is a
     * transitional format.
     * </dd>
     * </dl>
     * When streaming out this class, the most recent format
     * and the highest allowable <code>serialVersionOnStream</code>
     * is written.
     * @serial
     * @since JDK1.1.6
     */
    private int             serialVersionOnStream = currentSerialVersion;

    // Proclaim serialization compatibility with JDK 1.1
    static final long       serialVersionUID = -1807547505821590642L;

    /**
     * Constructs a Calendar with the default time zone
     * and locale.
     * @see     TimeZone#getDefault
     */
    protected Calendar()
    {
        this(TimeZone.getDefault(), Locale.getDefault());
    }

    /**
     * Constructs a calendar with the specified time zone and locale.
     * @param zone the time zone to use
     * @param aLocale the locale for the week data
     */
    protected Calendar(TimeZone zone, Locale aLocale)
    {
        fields = new int[FIELD_COUNT];
        stamp = new int[FIELD_COUNT];

        this.zone = zone;
        setWeekCountData(aLocale);
        setWeekendData(aLocale);
    }

    /**
     * Gets a calendar using the default time zone and locale.
     * @return a Calendar.
     */
    public static synchronized Calendar getInstance()
    {
        return new GregorianCalendar();
    }

    /**
     * Gets a calendar using the specified time zone and default locale.
     * @param zone the time zone to use
     * @return a Calendar.
     */
    public static synchronized Calendar getInstance(TimeZone zone)
    {
        return new GregorianCalendar(zone, Locale.getDefault());
    }

    /**
     * Gets a calendar using the default time zone and specified locale.
     * @param aLocale the locale for the week data
     * @return a Calendar.
     */
    public static synchronized Calendar getInstance(Locale aLocale)
    {
        return new GregorianCalendar(TimeZone.getDefault(), aLocale);
    }

    /**
     * Gets a calendar with the specified time zone and locale.
     * @param zone the time zone to use
     * @param aLocale the locale for the week data
     * @return a Calendar.
     */
    public static synchronized Calendar getInstance(TimeZone zone,
                                                    Locale aLocale)
    {
        return new GregorianCalendar(zone, aLocale);
    }

    /**
     * Gets the list of locales for which Calendars are installed.
     * @return the list of locales for which Calendars are installed.
     */
    public static synchronized Locale[] getAvailableLocales()
    {
        return DateFormat.getAvailableLocales();
    }

    /**
     * Converts the current field values in <code>fields[]</code>
     * to the millisecond time value
     * <code>time</code>.
     */
    protected abstract void computeTime();

    /**
     * Converts 
     * the current millisecond time value
     * <code>time</code>
     * to field values in <code>fields[]</code>.
     * This allows you to sync up the time field values with
     * a new time that is set for the calendar.  The time is <em>not</em>
     * recomputed first; to recompute the time, then the fields, call the
     * <code>complete</code> method.
     * @see #complete
     */
    protected abstract void computeFields();

    /**
     * Gets this Calendar's current time.
     * @return the current time.
     */
    public final Date getTime() {
        return new Date( getTimeInMillis() );
    }

    /**
     * Sets this Calendar's current time with the given Date.
     * <p>
     * Note: Calling <code>setTime()</code> with
     * <code>Date(Long.MAX_VALUE)</code> or <code>Date(Long.MIN_VALUE)</code>
     * may yield incorrect field values from <code>get()</code>.
     * @param date the given Date.  */
    public final void setTime(Date date) {
        setTimeInMillis( date.getTime() );
    }

    /**
     * Gets this Calendar's current time as a long.
     * @return the current time as UTC milliseconds from the epoch.
     */
    protected long getTimeInMillis() {
        if (!isTimeSet) updateTime();
        return time;
    }

    /**
     * Sets this Calendar's current time from the given long value.
     * @param date the new time in UTC milliseconds from the epoch.
     */
    protected void setTimeInMillis( long millis ) {
        isTimeSet = true;
        time = millis;
        areFieldsSet = false;
        if (!areFieldsSet) {
            computeFields();
            areFieldsSet = true;
            areAllFieldsSet = true;
        }
    }

    /**
     * Gets the value for a given time field.
     * @param field the given time field.
     * @return the value for the given time field.
     */
    public final int get(int field)
    {
        complete();
        return fields[field];
    }

    /**
     * Gets the value for a given time field. This is an internal
     * fast time field value getter for the subclasses.
     * @param field the given time field.
     * @return the value for the given time field.
     */
    protected final int internalGet(int field)
    {
        return fields[field];
    }

    /**
     * Sets the value for the given time field.  This is an internal
     * fast setter for subclasses.  It does not affect the areFieldsSet, isTimeSet,
     * or areAllFieldsSet flags.
     */
    final void internalSet(int field, int value)
    {
        fields[field] = value;
    }

    /**
     * Sets the time field with the given value.
     * @param field the given time field.
     * @param value the value to be set for the given time field.
     */
    public final void set(int field, int value)
    {
        isTimeSet = false;
        fields[field] = value;
        stamp[field] = nextStamp++;
        areFieldsSet = false;
    }

    /**
     * Sets the values for the fields year, month, and date.
     * Previous values of other fields are retained.  If this is not desired,
     * call <code>clear</code> first.
     * @param year the value used to set the YEAR time field.
     * @param month the value used to set the MONTH time field.
     * Month value is 0-based. e.g., 0 for January.
     * @param date the value used to set the DATE time field.
     */
    public final void set(int year, int month, int date)
    {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, date);
    }

    /**
     * Sets the values for the fields year, month, date, hour, and minute.
     * Previous values of other fields are retained.  If this is not desired,
     * call <code>clear</code> first.
     * @param year the value used to set the YEAR time field.
     * @param month the value used to set the MONTH time field.
     * Month value is 0-based. e.g., 0 for January.
     * @param date the value used to set the DATE time field.
     * @param hour the value used to set the HOUR_OF_DAY time field.
     * @param minute the value used to set the MINUTE time field.
     */
    public final void set(int year, int month, int date, int hour, int minute)
    {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, date);
        set(HOUR_OF_DAY, hour);
        set(MINUTE, minute);
    }

    /**
     * Sets the values for the fields year, month, date, hour, minute, and second.
     * Previous values of other fields are retained.  If this is not desired,
     * call <code>clear</code> first.
     * @param year the value used to set the YEAR time field.
     * @param month the value used to set the MONTH time field.
     * Month value is 0-based. e.g., 0 for January.
     * @param date the value used to set the DATE time field.
     * @param hour the value used to set the HOUR_OF_DAY time field.
     * @param minute the value used to set the MINUTE time field.
     * @param second the value used to set the SECOND time field.
     */
    public final void set(int year, int month, int date, int hour, int minute,
                          int second)
    {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, date);
        set(HOUR_OF_DAY, hour);
        set(MINUTE, minute);
        set(SECOND, second);
    }

    /**
     * Clears the values of all the time fields.
     */
    public final void clear()
    {
        fields = new int[FIELD_COUNT];
        stamp = new int[FIELD_COUNT];
        areFieldsSet = false;
        areAllFieldsSet = false;
        isTimeSet = false;
    }

    /**
     * Clears the value in the given time field.
     * @param field the time field to be cleared.
     */
    public final void clear(int field)
    {
        fields[field] = 0;
        stamp[field] = UNSET;
        areFieldsSet = false;
        areAllFieldsSet = false;
        isTimeSet = false;
    }

    /**
     * Determines if the given time field has a value set.
     * @return true if the given time field has a value set; false otherwise.
     */
    public final boolean isSet(int field)
    {
        return stamp[field] != UNSET;
    }

    /**
     * Fills in any unset fields in the time field list.
     */
    protected void complete()
    {
        if (!isTimeSet) updateTime();
        if (!areFieldsSet) {
            computeFields(); // fills in unset fields
            areFieldsSet = true;
            areAllFieldsSet = true;
        }
    }

    /**
     * Compares this calendar to the specified object.
     * The result is <code>true</code> if and only if the argument is
     * not <code>null</code> and is a <code>Calendar</code> object that
     * represents the same calendar as this object.
     * @param obj the object to compare with.
     * @return <code>true</code> if the objects are the same;
     * <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }

        Calendar that = (Calendar) obj;

        return
            getTimeInMillis() == that.getTime().getTime() &&
            isLenient() == that.isLenient() &&
            getFirstDayOfWeek() == that.getFirstDayOfWeek() &&
            getMinimalDaysInFirstWeek() == that.getMinimalDaysInFirstWeek() &&
            getTimeZone().equals(that.getTimeZone());
    }

    /**
     * Returns a hash code for this calendar.
     * @return a hash code value for this object. 
     * @since 1.2
     */
    public int hashCode() {
        /* Don't include the time because (a) we don't want the hash value to
         * move around just because a calendar is set to different times, and
         * (b) we don't want to trigger a time computation just to get a hash.
         * Note that it is not necessary for unequal objects to always have
         * unequal hashes, but equal objects must have equal hashes.  */
        return (lenient ? 1 : 0)
            | (firstDayOfWeek << 1)
            | (minimalDaysInFirstWeek << 4)
            | (zone.hashCode() << 7);
    }

    /**
     * Return the difference in milliseconds between the moment this
     * calendar is set to and the moment the given calendar or Date object
     * is set to.
     */
    private long compare(Object that) {
        long thatMs;
        if (that instanceof Calendar) {
            thatMs = ((Calendar)that).getTimeInMillis();
        } else if (that instanceof Date) {
            thatMs = ((Date)that).getTime();
        } else {
            throw new IllegalArgumentException("argument must be a non-null Calendar or Date");
        }
        return getTimeInMillis() - thatMs;
    }

    /**
     * Compares the time field records.
     * Equivalent to comparing result of conversion to UTC.
     * @param when the Calendar to be compared with this Calendar.
     * @return true if the current time of this Calendar is before
     * the time of Calendar when; false otherwise.
     */
    public boolean before(Object when) {
        return compare(when) < 0;
    }

    /**
     * Compares the time field records.
     * Equivalent to comparing result of conversion to UTC.
     * @param when the Calendar to be compared with this Calendar.
     * @return true if the current time of this Calendar is after
     * the time of Calendar when; false otherwise.
     */
    public boolean after(Object when) {
        return compare(when) > 0;
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
            // This is broken -- it's causing regression tests to
            // fail.  One problem is that it's calling get(), causing
            // a field resolution -- it shouldn't, since the calendar
            // may be in an intermediate state.  [Liu 2000-10-13]
//~            //
//~            // For the week-related fields, there's a shortcut.  Since we know the
//~            // current DAY_OF_WEEK and DAY_OF_MONTH or DAY_OF_YEAR, we can compute
//~            // the the maximum for this field in terms of the maximum DAY_OF_*,
//~            // on the theory that's easier to determine.
//~            //
//~            case WEEK_OF_YEAR:
//~                result = weekNumber(getActualMaximum(DAY_OF_YEAR),
//~                                    get(DAY_OF_YEAR), get(DAY_OF_WEEK));
//~                break;
//~                
//~            case WEEK_OF_MONTH:
//~                result = weekNumber(getActualMaximum(DAY_OF_MONTH),
//~                                    get(DAY_OF_MONTH), get(DAY_OF_WEEK));
//~                break;
//~          
//~            case DAY_OF_WEEK_IN_MONTH:
//~                int weekLength = getMaximum(DAY_OF_WEEK) - getMinimum(DAY_OF_WEEK) + 1;
//~                result = (getActualMaximum(DAY_OF_MONTH) - 1) / weekLength + 1;
//~                break;
            
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
            // This is broken -- it's causing regression tests to
            // fail.  One problem is that it's calling get(), causing
            // a field resolution -- it shouldn't, since the calendar
            // may be in an intermediate state.  [Liu 2000-10-13]
//~            //
//~            // For the week-related fields, there's a shortcut.  Since we know the
//~            // current DAY_OF_WEEK and DAY_OF_MONTH or DAY_OF_YEAR, we can compute
//~            // the the maximum for this field in terms of the maximum DAY_OF_*,
//~            // on the theory that's easier to determine.
//~            //
//~            case WEEK_OF_YEAR:
//~                result = weekNumber(getActualMinimum(DAY_OF_YEAR),
//~                                    get(DAY_OF_YEAR), get(DAY_OF_WEEK));
//~                break;
//~                
//~            case WEEK_OF_MONTH:
//~                result = weekNumber(getActualMinimum(DAY_OF_MONTH),
//~                                    get(DAY_OF_MONTH), get(DAY_OF_WEEK));
//~                break;
//~          
//~            case DAY_OF_WEEK_IN_MONTH:
//~                int weekLength = getMaximum(DAY_OF_WEEK) - getMinimum(DAY_OF_WEEK) + 1;
//~                result = (getActualMinimum(DAY_OF_MONTH) - 1) / weekLength + 1;
//~                break;
            
            // For all other fields, do it the hard way....
            default:
                result = getActualHelper(field, getGreatestMinimum(field), getMinimum(field));
                break;
        }
        return result;
    }

    private int getActualHelper(int field, int startValue, int endValue) {

        if (startValue == endValue) {
            // if we know that the maximum value is always the same, just return it
            return startValue;
        } 

        // clone the calendar so we don't mess with the real one, and set it to
        // accept anything for the field values
        Calendar work = (Calendar) this.clone();
        work.setLenient(true);
        
        // If we're counting weeks, set the day of the week to Sunday.  We know the
        // last week of a month or year will contain the first day of the week.
        if (field == WEEK_OF_YEAR || field == WEEK_OF_MONTH) {
            work.set(DAY_OF_WEEK, firstDayOfWeek);
        }
        
        // now try each value from the start to the end one by one until
        // we get a value that normalizes to another value.  The last value that
        // normalizes to itself is the actual maximum for the current date
        final int delta = (endValue > startValue) ? 1 : -1;
        
        int result = startValue;

        do {
            work.set(field, startValue);
            if (work.get(field) != startValue) {
                break;
            } else {
                result = startValue;
                startValue += delta;
            }
        } while (result != endValue);

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
     * The <code>com.ibm.util.Calendar</code> implementation of this method is able to roll
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
            throw new IllegalArgumentException("Calendar.roll: field not supported");
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
     * The <code>com.ibm.util.Calendar</code> implementation of this method is able to add to
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
            throw new IllegalArgumentException("Calendar.add: field not supported");
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
    // Interface for creating custon DateFormats for different types of Calendars
    //-------------------------------------------------------------------------

    public DateFormat getDateTimeFormat(int dateStyle, int timeStyle, Locale loc) {
        return formatHelper(this, loc, dateStyle, timeStyle);
    }
    
    static private DateFormat formatHelper(Calendar cal, Locale loc,
                                            int dateStyle, int timeStyle)
    {
        // See if there are any custom resources for this calendar
        // If not, just use the default DateFormat
        DateFormat result = null;
        DateFormatSymbols symbols = null;

        ResourceBundle bundle = DateFormatSymbols.getDateFormatBundle(cal, loc);

        if (bundle != null) {
            //if (cal instanceof com.ibm.util.Calendar) {
            //    symbols = ((com.ibm.util.Calendar)cal).getDateFormatSymbols(loc);
            //} else {
            //    symbols = getDateFormatSymbols(null, bundle, loc);
            //}
            symbols = new DateFormatSymbols(cal, loc);

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
                if (dateStyle == -1) {
	                result = SimpleDateFormat.getTimeInstance(timeStyle, loc);
	            } else if (timeStyle == -1) {
	                result = SimpleDateFormat.getDateInstance(dateStyle, loc);
	            } else {
	                result = SimpleDateFormat.getDateTimeInstance(dateStyle, timeStyle, loc);
	            }
                //((java.text.SimpleDateFormat)result).setDateFormatSymbols(oldStyleSymbols(symbols, loc));
                ((SimpleDateFormat)result).setDateFormatSymbols(symbols); // aliu
            }
        } else {
            if (dateStyle == -1) {
	            result = SimpleDateFormat.getTimeInstance(timeStyle, loc);
	        } else if (timeStyle == -1) {
	            result = SimpleDateFormat.getDateInstance(dateStyle, loc);
	        } else {
	            result = SimpleDateFormat.getDateTimeInstance(dateStyle, timeStyle, loc);
	        }
        }
        result.setCalendar(cal);
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
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

//|    /**
//|     * Date Arithmetic function.
//|     * Adds the specified (signed) amount of time to the given time field,
//|     * based on the calendar's rules. For example, to subtract 5 days from
//|     * the current time of the calendar, you can achieve it by calling:
//|     * <p>add(Calendar.DATE, -5).
//|     * @param field the time field.
//|     * @param amount the amount of date or time to be added to the field.
//|     */
//|    abstract public void add(int field, int amount);
//|
//|    /**
//|     * Time Field Rolling function.
//|     * Rolls (up/down) a single unit of time on the given time field. For
//|     * example, to roll the current date up by one day, you can achieve it
//|     * by calling:
//|     * <p>roll(Calendar.DATE, true).
//|     * When rolling on the year or Calendar.YEAR field, it will roll the year
//|     * value in the range between 1 and the value returned by calling
//|     * getMaximum(Calendar.YEAR).
//|     * When rolling on the month or Calendar.MONTH field, other fields like
//|     * date might conflict and, need to be changed. For instance,
//|     * rolling the month on the date 01/31/96 will result in 02/29/96.
//|     * When rolling on the hour-in-day or Calendar.HOUR_OF_DAY field, it will
//|     * roll the hour value in the range between 0 and 23, which is zero-based.
//|     * @param field the time field.
//|     * @param up indicates if the value of the specified time field is to be
//|     * rolled up or rolled down. Use true if rolling up, false otherwise.
//|     */
//|    abstract public void roll(int field, boolean up);
//|
//|    /**
//|     * Time Field Rolling function.
//|     * Rolls up or down the specified number of units on the given time field.
//|     * (A negative roll amount means to roll down.)
//|     * [NOTE:  This default implementation on Calendar just repeatedly calls the
//|     * version of roll() that takes a boolean and rolls by one unit.  This may not
//|     * always do the right thing.  For example, if the DAY_OF_MONTH field is 31,
//|     * rolling through February will leave it set to 28.  The GregorianCalendar
//|     * version of this function takes care of this problem.  Other subclasses
//|     * should also provide overrides of this function that do the right thing.
//|     * 
//|     * @since 1.2
//|     */
//|    public void roll(int field, int amount)
//|    {
//|        while (amount > 0) {
//|            roll(field, true);
//|            amount--;
//|        }
//|        while (amount < 0) {
//|            roll(field, false);
//|            amount++;
//|        }
//|    }

    /**
     * [NEW]
     * Return the difference between the given time and the time this
     * calendar object is set to.  If this calendar is set
     * <em>before</em> the given time, the returned value will be
     * positive.  If this calendar is set <em>after</em> the given
     * time, the returned value will be negative.  The
     * <code>field</code> parameter specifies the units of the return
     * value.  For example, if <code>fieldDifference(when,
     * Calendar.MONTH)</code> returns 3, then this calendar is set to
     * 3 months before <code>when</code>, and possibly some addition
     * time less than one month.
     *
     * <p>As a side effect of this call, this calendar is advanced
     * toward <code>when</code> by the given amount.  That is, calling
     * this method has the side effect of calling <code>add(field,
     * n)</code>, where <code>n</code> is the return value.
     *
     * <p>Usage: To use this method, call it first with the largest
     * field of interest, then with progressively smaller fields.  For
     * example:
     *
     * <pre>
     * int y = cal.fieldDifference(when, Calendar.YEAR);
     * int m = cal.fieldDifference(when, Calendar.MONTH);
     * int d = cal.fieldDifference(when, Calendar.DATE);</pre>
     *
     * computes the difference between <code>cal</code> and
     * <code>when</code> in years, months, and days.
     *
     * <p>Note: <code>fieldDifference()</code> is
     * <em>asymmetrical</em>.  That is, in the following code:
     *
     * <pre>
     * cal.setTime(date1);
     * int m1 = cal.fieldDifference(date2, Calendar.MONTH);
     * int d1 = cal.fieldDifference(date2, Calendar.DATE);
     * cal.setTime(date2);
     * int m2 = cal.fieldDifference(date1, Calendar.MONTH);
     * int d2 = cal.fieldDifference(date1, Calendar.DATE);</pre>
     *
     * one might expect that <code>m1 == -m2 && d1 == -d2</code>.
     * However, this is not generally the case, because of
     * irregularities in the underlying calendar system (e.g., the
     * Gregorian calendar has a varying number of days per month).
     *
     * @param when the date to compare this calendar's time to
     * @param field the field in which to compute the result
     * @return the difference, either positive or negative, between
     * this calendar's time and <code>when</code>, in terms of
     * <code>field</code>.
     */
    public int fieldDifference(Date when, int field) {
        int result = 0;
        long targetMs = when.getTime();
        long ms = getTimeInMillis();
        if (ms < targetMs) {
            for (;;) {
                add(field, 1);
                long newMs = getTimeInMillis();
                if (newMs > targetMs) {
                    setTimeInMillis(ms);
                    break;
                }
                ms = newMs;
                ++result;
            }
        } else if (ms > targetMs) {
            for (;;) {
                add(field, -1);
                long newMs = getTimeInMillis();
                if (newMs < targetMs) {
                    setTimeInMillis(ms);
                    break;
                }
                ms = newMs;
                --result;
            }
        }
        return result;
    }

    /**
     * Sets the time zone with the given time zone value.
     * @param value the given time zone.
     */
    public void setTimeZone(TimeZone value)
    {
        zone = value;
        /* Recompute the fields from the time using the new zone.  This also
         * works if isTimeSet is false (after a call to set()).  In that case
         * the time will be computed from the fields using the new zone, then
         * the fields will get recomputed from that.  Consider the sequence of
         * calls: cal.setTimeZone(EST); cal.set(HOUR, 1); cal.setTimeZone(PST).
         * Is cal set to 1 o'clock EST or 1 o'clock PST?  Answer: PST.  More
         * generally, a call to setTimeZone() affects calls to set() BEFORE AND
         * AFTER it up to the next call to complete().
         */
        areFieldsSet = false;
    }

    /**
     * Gets the time zone.
     * @return the time zone object associated with this calendar.
     */
    public TimeZone getTimeZone()
    {
        return zone;
    }

    /**
     * Specify whether or not date/time interpretation is to be lenient.  With
     * lenient interpretation, a date such as "February 942, 1996" will be
     * treated as being equivalent to the 941st day after February 1, 1996.
     * With strict interpretation, such dates will cause an exception to be
     * thrown.
     *
     * @see DateFormat#setLenient
     */
    public void setLenient(boolean lenient)
    {
        this.lenient = lenient;
    }

    /**
     * Tell whether date/time interpretation is to be lenient.
     */
    public boolean isLenient()
    {
        return lenient;
    }

    /**
     * Sets what the first day of the week is; e.g., Sunday in US,
     * Monday in France.
     * @param value the given first day of the week.
     */
    public void setFirstDayOfWeek(int value)
    {
        firstDayOfWeek = value;
    }

    /**
     * Gets what the first day of the week is; e.g., Sunday in US,
     * Monday in France.
     * @return the first day of the week.
     */
    public int getFirstDayOfWeek()
    {
        return firstDayOfWeek;
    }

    /**
     * Sets what the minimal days required in the first week of the year are;
     * For example, if the first week is defined as one that contains the first
     * day of the first month of a year, call the method with value 1. If it
     * must be a full week, use value 7.
     * @param value the given minimal days required in the first week
     * of the year.
     */
    public void setMinimalDaysInFirstWeek(int value)
    {
        minimalDaysInFirstWeek = value;
    }

    /**
     * Gets what the minimal days required in the first week of the year are;
     * e.g., if the first week is defined as one that contains the first day
     * of the first month of a year, getMinimalDaysInFirstWeek returns 1. If
     * the minimal days required must be a full week, getMinimalDaysInFirstWeek
     * returns 7.
     * @return the minimal days required in the first week of the year.
     */
    public int getMinimalDaysInFirstWeek()
    {
        return minimalDaysInFirstWeek;
    }

    /**
     * Gets the minimum value for the given time field.
     * e.g., for Gregorian DAY_OF_MONTH, 1.
     * @param field the given time field.
     * @return the minimum value for the given time field.
     */
    abstract public int getMinimum(int field);

    /**
     * Gets the maximum value for the given time field.
     * e.g. for Gregorian DAY_OF_MONTH, 31.
     * @param field the given time field.
     * @return the maximum value for the given time field.
     */
    abstract public int getMaximum(int field);

    /**
     * Gets the highest minimum value for the given field if varies.
     * Otherwise same as getMinimum(). For Gregorian, no difference.
     * @param field the given time field.
     * @return the highest minimum value for the given time field.
     */
    abstract public int getGreatestMinimum(int field);

    /**
     * Gets the lowest maximum value for the given field if varies.
     * Otherwise same as getMaximum(). e.g., for Gregorian DAY_OF_MONTH, 28.
     * @param field the given time field.
     * @return the lowest maximum value for the given time field.
     */
    abstract public int getLeastMaximum(int field);

//|    /**
//|     * Return the minimum value that this field could have, given the current date.
//|     * For the Gregorian calendar, this is the same as getMinimum() and getGreatestMinimum().
//|     *
//|     * The version of this function on Calendar uses an iterative algorithm to determine the
//|     * actual minimum value for the field.  There is almost always a more efficient way to
//|     * accomplish this (in most cases, you can simply return getMinimum()).  GregorianCalendar
//|     * overrides this function with a more efficient implementation.
//|     *
//|     * @param field the field to determine the minimum of
//|     * @return the minimum of the given field for the current date of this Calendar
//|     * @since 1.2
//|     */
//|    public int getActualMinimum(int field) {
//|        int fieldValue = getGreatestMinimum(field);
//|        int endValue = getMinimum(field);
//|
//|        // if we know that the minimum value is always the same, just return it
//|        if (fieldValue == endValue) {
//|            return fieldValue;
//|        }
//|
//|        // clone the calendar so we don't mess with the real one, and set it to
//|        // accept anything for the field values
//|        Calendar work = (Calendar)this.clone();
//|        work.setLenient(true);
//|
//|        // now try each value from getLeastMaximum() to getMaximum() one by one until
//|        // we get a value that normalizes to another value.  The last value that
//|        // normalizes to itself is the actual minimum for the current date
//|        int result = fieldValue;
//|
//|        do {
//|            work.set(field, fieldValue);
//|            if (work.get(field) != fieldValue) {
//|                break;
//|            } else {
//|                result = fieldValue;
//|                fieldValue--;
//|            }
//|        } while (fieldValue >= endValue);
//|
//|        return result;
//|    }
//|
//|    /**
//|     * Return the maximum value that this field could have, given the current date.
//|     * For example, with the date "Feb 3, 1997" and the DAY_OF_MONTH field, the actual
//|     * maximum would be 28; for "Feb 3, 1996" it s 29.  Similarly for a Hebrew calendar,
//|     * for some years the actual maximum for MONTH is 12, and for others 13.
//|     *
//|     * The version of this function on Calendar uses an iterative algorithm to determine the
//|     * actual maximum value for the field.  There is almost always a more efficient way to
//|     * accomplish this (in most cases, you can simply return getMaximum()).  GregorianCalendar
//|     * overrides this function with a more efficient implementation.
//|     *
//|     * @param field the field to determine the maximum of
//|     * @return the maximum of the given field for the current date of this Calendar
//|     * @since 1.2
//|     */
//|    public int getActualMaximum(int field) {
//|        int fieldValue = getLeastMaximum(field);
//|        int endValue = getMaximum(field);
//|
//|        // if we know that the maximum value is always the same, just return it
//|        if (fieldValue == endValue) {
//|            return fieldValue;
//|        }
//|
//|        // clone the calendar so we don't mess with the real one, and set it to
//|        // accept anything for the field values
//|        Calendar work = (Calendar)this.clone();
//|        work.setLenient(true);
//|
//|        // if we're counting weeks, set the day of the week to Sunday.  We know the
//|        // last week of a month or year will contain the first day of the week.
//|        if (field == WEEK_OF_YEAR || field == WEEK_OF_MONTH)
//|            work.set(DAY_OF_WEEK, firstDayOfWeek);
//|
//|        // now try each value from getLeastMaximum() to getMaximum() one by one until
//|        // we get a value that normalizes to another value.  The last value that
//|        // normalizes to itself is the actual maximum for the current date
//|        int result = fieldValue;
//|
//|        do {
//|            work.set(field, fieldValue);
//|            if (work.get(field) != fieldValue) {
//|                break;
//|            } else {
//|                result = fieldValue;
//|                fieldValue++;
//|            }
//|        } while (fieldValue <= endValue);
//|
//|        return result;
//|    }

    //-------------------------------------------------------------------------
    // Weekend support -- determining which days of the week are the weekend
    // in a given locale
    //-------------------------------------------------------------------------

    /**
     * Return whether the given day of the week is a weekday, a
     * weekend day, or a day that transitions from one to the other,
     * in this calendar system.  If a transition occurs at midnight,
     * then the days before and after the transition will have the
     * type WEEKDAY or WEEKEND.  If a transition occurs at a time
     * other than midnight, then the day of the transition will have
     * the type WEEKEND_ONSET or WEEKEND_CEASE.  In this case, the
     * method getWeekendTransition() will return the point of
     * transition.
     * @param dayOfWeek either SUNDAY, MONDAY, TUESDAY, WEDNESDAY,
     * THURSDAY, FRIDAY, or SATURDAY
     * @return either WEEKDAY, WEEKEND, WEEKEND_ONSET, or
     * WEEKEND_CEASE
     * @exception IllegalArgumentException if dayOfWeek is not
     * between SUNDAY and SATURDAY, inclusive
     * @see #WEEKDAY
     * @see #WEEKEND
     * @see #WEEKEND_ONSET
     * @see #WEEKEND_CEASE
     * @see #getWeekendTransition
     * @see #isWeekend(Date)
     * @see #isWeekend()
     */
    public int getDayOfWeekType(int dayOfWeek) {
        if (dayOfWeek < SUNDAY || dayOfWeek > SATURDAY) {
            throw new IllegalArgumentException("Invalid day of week");
        }
        if (weekendOnset < weekendCease) {
            if (dayOfWeek < weekendOnset || dayOfWeek > weekendCease) {
                return WEEKDAY;
            }
        } else {
            if (dayOfWeek > weekendCease && dayOfWeek < weekendOnset) {
                return WEEKDAY;
            }
        } 
        if (dayOfWeek == weekendOnset) {
            return (weekendOnsetMillis == 0) ? WEEKEND : WEEKEND_ONSET;
        }
        if (dayOfWeek == weekendCease) {
            return (weekendCeaseMillis == 0) ? WEEKDAY : WEEKEND_CEASE;
        }
        return WEEKEND;
    }

    /**
     * Return the time during the day at which the weekend begins in
     * this calendar system, if getDayOfWeekType(dayOfWeek) ==
     * WEEKEND_ONSET, or at which the weekend ends, if
     * getDayOfWeekType(dayOfWeek) == WEEKEND_CEASE.  If
     * getDayOfWeekType(dayOfWeek) has some other value, then an
     * exception is thrown.
     * @param dayOfWeek either SUNDAY, MONDAY, TUESDAY, WEDNESDAY,
     * THURSDAY, FRIDAY, or SATURDAY
     * @return the milliseconds after midnight at which the
     * weekend begins or ends
     * @exception IllegalArgumentException if dayOfWeek is not
     * WEEKEND_ONSET or WEEKEND_CEASE
     * @see #getDayOfWeekType
     * @see #isWeekend(Date)
     * @see #isWeekend()
     */
    public int getWeekendTransition(int dayOfWeek) {
        if (dayOfWeek == weekendOnset) {
            return weekendOnsetMillis;
        } else if (dayOfWeek == weekendCease) {
            return weekendCeaseMillis;
        }
        throw new IllegalArgumentException("Not weekend transition day");
    }

    /**
     * Return true if the given date and time is in the weekend in
     * this calendar system.  Equivalent to calling setTime() followed
     * by isWeekend().  Note: This method changes the time this
     * calendar is set to.
     * @param date the date and time
     * @return true if the given date and time is part of the
     * weekend
     * @see #getDayOfWeekType
     * @see #getWeekendTransition
     * @see #isWeekend()
     */
    public boolean isWeekend(Date date) {
        setTime(date);
        return isWeekend();
    }

    /**
     * Return true if this Calendar's current date and time is in the
     * weekend in this calendar system.
     * @return true if the given date and time is part of the
     * weekend
     * @see #getDayOfWeekType
     * @see #getWeekendTransition
     * @see #isWeekend(Date)
     */
    public boolean isWeekend() {
        int dow =  get(DAY_OF_WEEK);
        int dowt = getDayOfWeekType(dow);
        switch (dowt) {
        case WEEKDAY:
            return false;
        case WEEKEND:
            return true;
        default: // That is, WEEKEND_ONSET or WEEKEND_CEASE
            // Use internalGet() because the above call to get() populated
            // all fields.
            // [Note: There should be a better way to get millis in day.
            //  For ICU4J, submit request for a MILLIS_IN_DAY field
            //  and a DAY_NUMBER field (could be Julian day #). - aliu]
            int millisInDay = internalGet(MILLISECOND) + 1000 * (internalGet(SECOND) +
                60 * (internalGet(MINUTE) + 60 * internalGet(HOUR_OF_DAY)));
            int transition = getWeekendTransition(dow);
            return (dowt == WEEKEND_ONSET)
                ? (millisInDay >= transition)
                : (millisInDay <  transition);
        }
        // (We can never reach this point.)
    }

    /**
     * Read the locale weekend data for the given locale.
     *
     * This is the initial placement and format of this data -- it may very
     * well change in the future.  See the locale files themselves for
     * details.
     */
    private void setWeekendData(Locale loc) {
        ResourceBundle resource =
            ResourceBundle.getBundle("com.ibm.util.resources.CalendarData",
                                     loc);
        String[] data = resource.getStringArray("Weekend");
        weekendOnset       = Integer.parseInt(data[0]);
        weekendOnsetMillis = Integer.parseInt(data[1]);
        weekendCease       = Integer.parseInt(data[2]);
        weekendCeaseMillis = Integer.parseInt(data[3]);
    }

    //-------------------------------------------------------------------------
    // End of weekend support
    //-------------------------------------------------------------------------

    /**
     * Overrides Cloneable
     */
    public Object clone()
    {
        try {
            Calendar other = (Calendar) super.clone();

            other.fields = new int[FIELD_COUNT];
            other.stamp = new int[FIELD_COUNT];
            System.arraycopy(this.fields, 0, other.fields, 0, FIELD_COUNT);
            System.arraycopy(this.stamp, 0, other.stamp, 0, FIELD_COUNT);

            other.zone = (TimeZone) zone.clone();
            return other;
        }
        catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    private static final String[] FIELD_NAME = {
        ",ERA=", ",YEAR=", ",MONTH=", ",WEEK_OF_YEAR=", ",WEEK_OF_MONTH=", ",DAY_OF_MONTH=",
        ",DAY_OF_YEAR=", ",DAY_OF_WEEK=", ",DAY_OF_WEEK_IN_MONTH=", ",AM_PM=", ",HOUR=",
        ",HOUR_OF_DAY=", ",MINUTE=", ",SECOND=", ",MILLISECOND=", ",ZONE_OFFSET=",
        ",DST_OFFSET="
    };

    /**
     * Return a string representation of this calendar. This method 
     * is intended to be used only for debugging purposes, and the 
     * format of the returned string may vary between implementations. 
     * The returned string may be empty but may not be <code>null</code>.
     * 
     * @return  a string representation of this calendar.
     */
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(getClass().getName());
        buffer.append("[time=");
        buffer.append(isTimeSet ? String.valueOf(time) : "?");
        buffer.append(",areFieldsSet=");
        buffer.append(areFieldsSet);
        buffer.append(",areAllFieldsSet=");
        buffer.append(areAllFieldsSet);
        buffer.append(",lenient=");
        buffer.append(lenient);
        buffer.append(",zone=");
        buffer.append(zone);
        buffer.append(",firstDayOfWeek=");
        buffer.append(firstDayOfWeek);
        buffer.append(",minimalDaysInFirstWeek=");
        buffer.append(minimalDaysInFirstWeek);
        for (int i=0; i<FIELD_COUNT; ++i) {
            buffer.append(FIELD_NAME[i]);
            buffer.append(isSet(i) ? String.valueOf(fields[i]) : "?");
        }
        buffer.append(']');
        return buffer.toString();
    }

    // =======================privates===============================

    /**
     * Both firstDayOfWeek and minimalDaysInFirstWeek are locale-dependent.
     * They are used to figure out the week count for a specific date for
     * a given locale. These must be set when a Calendar is constructed.
     * @param desiredLocale the given locale.
     */
    private void setWeekCountData(Locale desiredLocale)
    {
    /* try to get the Locale data from the cache */
    int[] data = (int[]) cachedLocaleData.get(desiredLocale);
    if (data == null) {  /* cache miss */
        ResourceBundle resource
        = ResourceBundle.getBundle("java.text.resources.LocaleElements",
                       desiredLocale);
        String[] dateTimePatterns =
        resource.getStringArray("DateTimeElements");
        data = new int[2];
        data[0] = Integer.parseInt(dateTimePatterns[0]);
        data[1] = Integer.parseInt(dateTimePatterns[1]);
        /* cache update */
        cachedLocaleData.put(desiredLocale, data);
    }
    firstDayOfWeek = data[0];
    minimalDaysInFirstWeek = data[1];
    }

    /**
     * Recompute the time and update the status fields isTimeSet
     * and areFieldsSet.  Callers should check isTimeSet and only
     * call this method if isTimeSet is false.
     */
    private void updateTime() {
        computeTime();
        // If we are lenient, we need to recompute the fields to normalize
        // the values.  Also, if we haven't set all the fields yet (i.e.,
        // in a newly-created object), we need to fill in the fields. [LIU]
        if (isLenient() || !areAllFieldsSet) areFieldsSet = false;
        isTimeSet = true;
    }

    /**
     * Save the state of this object to a stream (i.e., serialize it).
     *
     * Ideally, <code>Calendar</code> would only write out its state data and
     * the current time, and not write any field data out, such as
     * <code>fields[]</code>, <code>isTimeSet</code>, <code>areFieldsSet</code>,
     * and <code>isSet[]</code>.  <code>nextStamp</code> also should not be part
     * of the persistent state. Unfortunately, this didn't happen before JDK 1.1
     * shipped. To be compatible with JDK 1.1, we will always have to write out
     * the field values and state flags.  However, <code>nextStamp</code> can be
     * removed from the serialization stream; this will probably happen in the
     * near future.
     */
    private void writeObject(ObjectOutputStream stream)
         throws IOException
    {
        // Try to compute the time correctly, for the future (stream
        // version 2) in which we don't write out fields[] or isSet[].
        if (!isTimeSet) {
            try {
                updateTime();
            }
            catch (IllegalArgumentException e) {}
        }

        // Write out the 1.1 FCS object.
        stream.defaultWriteObject();
    }

    /**
     * Reconstitute this object from a stream (i.e., deserialize it).
     */
    /*
    private void readObject(ObjectInputStream stream)
         throws IOException, ClassNotFoundException
    {
        stream.defaultReadObject();

        stamp = new int[FIELD_COUNT];

        // Starting with version 2 (not implemented yet), we expect that
        // fields[], isSet[], isTimeSet, and areFieldsSet may not be
        // streamed out anymore.  We expect 'time' to be correct.
        if (serialVersionOnStream >= 2)
        {
            isTimeSet = true;
            if (fields == null) fields = new int[FIELD_COUNT];
            if (isSet == null) isSet = new boolean[FIELD_COUNT];
        }
        else if (serialVersionOnStream == 0)
        {
            for (int i=0; i<FIELD_COUNT; ++i)
                stamp[i] = isSet[i] ? INTERNALLY_SET : UNSET;
        }

        serialVersionOnStream = currentSerialVersion;
    }
    */

    //----------------------------------------------------------------------
    // Subclass API
    //  For use by subclasses of Calendar
    //----------------------------------------------------------------------

    protected void _TEMPORARY_markAllFieldsSet() {
        for (int i=0; i<stamp.length; ++i) {
            stamp[i] = INTERNALLY_SET;
        }
    }
}

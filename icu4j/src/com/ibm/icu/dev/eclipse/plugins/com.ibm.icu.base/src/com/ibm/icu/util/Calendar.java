/*
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */


package com.ibm.icu.util;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.text.DateFormat;

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
 * <code>java.util.Calendar</code>.  Changes are detailed below.
 *
 * <p>
 * Subclasses of <code>Calendar</code> interpret a <code>Date</code>
 * according to the rules of a specific calendar system.  ICU4J contains
 * several subclasses implementing different international calendar systems.
 *
 * <p>
 * Like other locale-sensitive classes, <code>Calendar</code> provides a
 * class method, <code>getInstance</code>, for getting a generally useful
 * object of this type. <code>Calendar</code>'s <code>getInstance</code> method
 * returns a calendar of a type appropriate to the locale, whose
 * time fields have been initialized with the current date and time:
 * <blockquote>
 * <pre>Calendar rightNow = Calendar.getInstance()</pre>
 * </blockquote>
 *
 * <p>When a <code>ULocale</code> is used by <code>getInstance</code>, its
 * '<code>calendar</code>' tag and value are retrieved if present.  If a recognized
 * value is supplied, a calendar is provided and configured as appropriate.
 * Currently recognized tags are "buddhist", "chinese", "coptic", "ethiopic", 
 * "gregorian", "hebrew", "islamic", "islamic-civil", and "japanese".  For
 * example: <blockquote>
 * <pre>Calendar cal = Calendar.getInstance(new ULocale("en_US@calendar=japanese"));</pre>
 * </blockquote> will return an instance of JapaneseCalendar (using en_US conventions for
 * minimum days in first week, start day of week, et cetera).
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
 * DAY_OF_WEEK + WEEK_OF_YEAR</pre>
 * </blockquote>
 *
 * For the time of day:
 *
 * <blockquote>
 * <pre>
 * HOUR_OF_DAY
 * AM_PM + HOUR</pre>
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
 * <p><b>Note:</b> You should always use {@link #roll roll} and {@link #add add} rather
 * than attempting to perform arithmetic operations directly on the fields
 * of a <tt>Calendar</tt>.  It is quite possible for <tt>Calendar</tt> subclasses
 * to have fields with non-linear behavior, for example missing months
 * or days during non-leap years.  The subclasses' <tt>add</tt> and <tt>roll</tt>
 * methods will take this into account, while simple arithmetic manipulations
 * may give invalid results.
 *
 * <p><big><big><b>Calendar Architecture in ICU4J</b></big></big></p>
 *
 * <p>Recently the implementation of <code>Calendar</code> has changed
 * significantly in order to better support subclassing. The original
 * <code>Calendar</code> class was designed to support subclassing, but
 * it had only one implemented subclass, <code>GregorianCalendar</code>.
 * With the implementation of several new calendar subclasses, including
 * the <code>BuddhistCalendar</code>, <code>ChineseCalendar</code>,
 * <code>HebrewCalendar</code>, <code>IslamicCalendar</code>, and
 * <code>JapaneseCalendar</code>, the subclassing API has been reworked
 * thoroughly. This section details the new subclassing API and other
 * ways in which <code>com.ibm.icu.util.Calendar</code> differs from
 * <code>java.util.Calendar</code>.
 * </p>
 *
 * <p><big><b>Changes</b></big></p>
 *
 * <p>Overview of changes between the classic <code>Calendar</code>
 * architecture and the new architecture.
 *
 * <ul>
 *
 *   <li>The <code>fields[]</code> array is <code>private</code> now
 *     instead of <code>protected</code>.  Subclasses must access it
 *     using the methods {@link #internalSet} and
 *     {@link #internalGet}.  <b>Motivation:</b> Subclasses should
 *     not directly access data members.</li>
 *
 *   <li>The <code>time</code> long word is <code>private</code> now
 *     instead of <code>protected</code>.  Subclasses may access it using
 *     the method {@link #internalGetTimeInMillis}, which does not
 *     provoke an update. <b>Motivation:</b> Subclasses should not
 *     directly access data members.</li>
 *
 *   <li>The scope of responsibility of subclasses has been drastically
 *     reduced. As much functionality as possible is implemented in the
 *     <code>Calendar</code> base class. As a result, it is much easier
 *     to subclass <code>Calendar</code>. <b>Motivation:</b> Subclasses
 *     should not have to reimplement common code. Certain behaviors are
 *     common across calendar systems: The definition and behavior of
 *     week-related fields and time fields, the arithmetic
 *     ({@link #add(int, int) add} and {@link #roll(int, int) roll}) behavior of many
 *     fields, and the field validation system.</li>
 *
 *   <li>The subclassing API has been completely redesigned.</li>
 *
 *   <li>The <code>Calendar</code> base class contains some Gregorian
 *     calendar algorithmic support that subclasses can use (specifically
 *     in {@link #handleComputeFields}).  Subclasses can use the
 *     methods <code>getGregorianXxx()</code> to obtain precomputed
 *     values. <b>Motivation:</b> This is required by all
 *     <code>Calendar</code> subclasses in order to implement consistent
 *     time zone behavior, and Gregorian-derived systems can use the
 *     already computed data.</li>
 *
 *   <li>The <code>FIELD_COUNT</code> constant has been removed. Use
 *     {@link #getFieldCount}.  In addition, framework API has been
 *     added to allow subclasses to define additional fields.
 *     <b>Motivation: </b>The number of fields is not constant across
 *     calendar systems.</li>
 *
 *   <li>The range of handled dates has been narrowed from +/-
 *     ~300,000,000 years to +/- ~5,000,000 years. In practical terms
 *     this should not affect clients. However, it does mean that client
 *     code cannot be guaranteed well-behaved results with dates such as
 *     <code>Date(Long.MIN_VALUE)</code> or
 *     <code>Date(Long.MAX_VALUE)</code>. Instead, the
 *     <code>Calendar</code> constants {@link #MIN_DATE},
 *     {@link #MAX_DATE}, {@link #MIN_MILLIS},
 *     {@link #MAX_MILLIS}, {@link #MIN_JULIAN}, and
 *     {@link #MAX_JULIAN} should be used. <b>Motivation:</b> With
 *     the addition of the {@link #JULIAN_DAY} field, Julian day
 *     numbers must be restricted to a 32-bit <code>int</code>.  This
 *     restricts the overall supported range. Furthermore, restricting
 *     the supported range simplifies the computations by removing
 *     special case code that was used to accomodate arithmetic overflow
 *     at millis near <code>Long.MIN_VALUE</code> and
 *     <code>Long.MAX_VALUE</code>.</li>
 *
 *   <li>New fields are implemented: {@link #JULIAN_DAY} defines
 *     single-field specification of the
 *     date. {@link #MILLISECONDS_IN_DAY} defines a single-field
 *     specification of the wall time. {@link #DOW_LOCAL} and
 *     {@link #YEAR_WOY} implement localized day-of-week and
 *     week-of-year behavior.</li>
 *
 *   <li>Subclasses can access millisecond constants
 *     {@link #ONE_SECOND}, {@link #ONE_MINUTE},
 *     {@link #ONE_HOUR}, {@link #ONE_DAY}, and
 *     {@link #ONE_WEEK} defined in <code>Calendar</code>.</li>
 *
 *   <li>New API has been added to suport calendar-specific subclasses
 *     of <code>DateFormat</code>.</li>
 *
 *   <li>Several subclasses have been implemented, representing
 *     various international calendar systems.</li>
 *
 * </ul>
 *
 * <p><big><b>Subclass API</b></big></p>
 *
 * <p>The original <code>Calendar</code> API was based on the experience
 * of implementing a only a single subclass,
 * <code>GregorianCalendar</code>. As a result, all of the subclassing
 * kinks had not been worked out. The new subclassing API has been
 * refined based on several implemented subclasses. This includes methods
 * that must be overridden and methods for subclasses to call. Subclasses
 * no longer have direct access to <code>fields</code> and
 * <code>stamp</code>. Instead, they have new API to access
 * these. Subclasses are able to allocate the <code>fields</code> array
 * through a protected framework method; this allows subclasses to
 * specify additional fields. </p>
 *
 * <p>More functionality has been moved into the base class. The base
 * class now contains much of the computational machinery to support the
 * Gregorian calendar. This is based on two things: (1) Many calendars
 * are based on the Gregorian calendar (such as the Buddhist and Japanese
 * imperial calendars). (2) <em>All</em> calendars require basic
 * Gregorian support in order to handle timezone computations. </p>
 *
 * <p>Common computations have been moved into
 * <code>Calendar</code>. Subclasses no longer compute the week related
 * fields and the time related fields. These are commonly handled for all
 * calendars by the base class. </p>
 *
 * <p><b>Subclass computation of time <tt>=&gt;</tt> fields</b>
 *
 * <p>The {@link #ERA}, {@link #YEAR},
 * {@link #EXTENDED_YEAR}, {@link #MONTH},
 * {@link #DAY_OF_MONTH}, and {@link #DAY_OF_YEAR} fields are
 * computed by the subclass, based on the Julian day. All other fields
 * are computed by <code>Calendar</code>.
 *
 * <ul>
 *
 *   <li>Subclasses should implement {@link #handleComputeFields}
 *     to compute the {@link #ERA}, {@link #YEAR},
 *     {@link #EXTENDED_YEAR}, {@link #MONTH},
 *     {@link #DAY_OF_MONTH}, and {@link #DAY_OF_YEAR} fields,
 *     based on the value of the {@link #JULIAN_DAY} field. If there
 *     are calendar-specific fields not defined by <code>Calendar</code>,
 *     they must also be computed. These are the only fields that the
 *     subclass should compute. All other fields are computed by the base
 *     class, so time and week fields behave in a consistent way across
 *     all calendars. The default version of this method in
 *     <code>Calendar</code> implements a proleptic Gregorian
 *     calendar. Within this method, subclasses may call
 *     <code>getGregorianXxx()</code> to obtain the Gregorian calendar
 *     month, day of month, and extended year for the given date.</li>
 *
 * </ul>
 *
 * <p><b>Subclass computation of fields <tt>=&gt;</tt> time</b>
 *
 * <p>The interpretation of most field values is handled entirely by
 * <code>Calendar</code>. <code>Calendar</code> determines which fields
 * are set, which are not, which are set more recently, and so on. In
 * addition, <code>Calendar</code> handles the computation of the time
 * from the time fields and handles the week-related fields. The only
 * thing the subclass must do is determine the extended year, based on
 * the year fields, and then, given an extended year and a month, it must
 * return a Julian day number.
 *
 * <ul>
 *
 *   <li>Subclasses should implement {@link #handleGetExtendedYear}
 *     to return the extended year for this calendar system, based on the
 *     {@link #YEAR}, {@link #EXTENDED_YEAR}, and any fields that
 *     the calendar system uses that are larger than a year, such as
 *     {@link #ERA}.</li>
 *
 *   <li>Subclasses should implement {@link #handleComputeMonthStart}
 *     to return the Julian day number
 *     associated with a month and extended year. This is the Julian day
 *     number of the day before the first day of the month. The month
 *     number is zero-based. This computation should not depend on any
 *     field values.</li>
 *
 * </ul>
 *
 * <p><b>Other methods</b>
 *
 * <ul>
 *
 *   <li>Subclasses should implement {@link #handleGetMonthLength}
 *     to return the number of days in a
 *     given month of a given extended year. The month number, as always,
 *     is zero-based.</li>
 *
 *   <li>Subclasses should implement {@link #handleGetYearLength}
 *     to return the number of days in the given
 *     extended year. This method is used by
 *     <tt>computeWeekFields</tt> to compute the
 *     {@link #WEEK_OF_YEAR} and {@link #YEAR_WOY} fields.</li>
 *
 *   <li>Subclasses should implement {@link #handleGetLimit}
 *     to return the {@link #MINIMUM},
 *     {@link #GREATEST_MINIMUM}, {@link #LEAST_MAXIMUM}, or
 *     {@link #MAXIMUM} of a field, depending on the value of
 *     <code>limitType</code>. This method only needs to handle the
 *     fields {@link #ERA}, {@link #YEAR}, {@link #MONTH},
 *     {@link #WEEK_OF_YEAR}, {@link #WEEK_OF_MONTH},
 *     {@link #DAY_OF_MONTH}, {@link #DAY_OF_YEAR},
 *     {@link #DAY_OF_WEEK_IN_MONTH}, {@link #YEAR_WOY}, and
 *     {@link #EXTENDED_YEAR}.  Other fields are invariant (with
 *     respect to calendar system) and are handled by the base
 *     class.</li>
 *
 *   <li>Optionally, subclasses may override {@link #validateField}
 *     to check any subclass-specific fields. If the
 *     field's value is out of range, the method should throw an
 *     <code>IllegalArgumentException</code>. The method may call
 *     <code>super.validateField(field)</code> to handle fields in a
 *     generic way, that is, to compare them to the range
 *     <code>getMinimum(field)</code>..<code>getMaximum(field)</code>.</li>
 *
 *   <li>Optionally, subclasses may override
 *     {@link #handleCreateFields} to create an <code>int[]</code>
 *     array large enough to hold the calendar's fields. This is only
 *     necessary if the calendar defines additional fields beyond those
 *     defined by <code>Calendar</code>. The length of the result must be
 *     at least {@link #BASE_FIELD_COUNT} and no more than
 *     {@link #MAX_FIELD_COUNT}.</li>
 *
 *   <li>Optionally, subclasses may override
 *     {@link #handleGetDateFormat} to create a
 *     <code>DateFormat</code> appropriate to this calendar. This is only
 *     required if a calendar subclass redefines the use of a field (for
 *     example, changes the {@link #ERA} field from a symbolic field
 *     to a numeric one) or defines an additional field.</li>
 *
 *   <li>Optionally, subclasses may override {@link #roll roll} and
 *     {@link #add add} to handle fields that are discontinuous. For
 *     example, in the Hebrew calendar the month &quot;Adar I&quot; only
 *     occurs in leap years; in other years the calendar jumps from
 *     Shevat (month #4) to Adar (month #6). The {@link
 *     HebrewCalendar#add HebrewCalendar.add} and {@link
 *     HebrewCalendar#roll HebrewCalendar.roll} methods take this into
 *     account, so that adding 1 month to Shevat gives the proper result
 *     (Adar) in a non-leap year. The protected utility method {@link
 *     #pinField pinField} is often useful when implementing these two
 *     methods. </li>
 *
 * </ul>
 *
 * <p><big><b>Normalized behavior</b></big>
 *
 * <p>The behavior of certain fields has been made consistent across all
 * calendar systems and implemented in <code>Calendar</code>.
 *
 * <ul>
 *
 *   <li>Time is normalized. Even though some calendar systems transition
 *     between days at sunset or at other times, all ICU4J calendars
 *     transition between days at <em>local zone midnight</em>.  This
 *     allows ICU4J to centralize the time computations in
 *     <code>Calendar</code> and to maintain basic correpsondences
 *     between calendar systems. Affected fields: {@link #AM_PM},
 *     {@link #HOUR}, {@link #HOUR_OF_DAY}, {@link #MINUTE},
 *     {@link #SECOND}, {@link #MILLISECOND},
 *     {@link #ZONE_OFFSET}, and {@link #DST_OFFSET}.</li>
 *
 *   <li>DST behavior is normalized. Daylight savings time behavior is
 *     computed the same for all calendar systems, and depends on the
 *     value of several <code>GregorianCalendar</code> fields: the
 *     {@link #YEAR}, {@link #MONTH}, and
 *     {@link #DAY_OF_MONTH}. As a result, <code>Calendar</code>
 *     always computes these fields, even for non-Gregorian calendar
 *     systems. These fields are available to subclasses.</li>
 *
 *   <li>Weeks are normalized. Although locales define the week
 *     differently, in terms of the day on which it starts, and the
 *     designation of week number one of a month or year, they all use a
 *     common mechanism. Furthermore, the day of the week has a simple
 *     and consistent definition throughout history. For example,
 *     although the Gregorian calendar introduced a discontinuity when
 *     first instituted, the day of week was not disrupted. For this
 *     reason, the fields {@link #DAY_OF_WEEK}, <code>WEEK_OF_YEAR,
 *     WEEK_OF_MONTH</code>, {@link #DAY_OF_WEEK_IN_MONTH},
 *     {@link #DOW_LOCAL}, {@link #YEAR_WOY} are all computed in
 *     a consistent way in the base class, based on the
 *     {@link #EXTENDED_YEAR}, {@link #DAY_OF_YEAR},
 *     {@link #MONTH}, and {@link #DAY_OF_MONTH}, which are
 *     computed by the subclass.</li>
 *
 * </ul>
 *
 * <p><big><b>Supported range</b></big>
 *
 * <p>The allowable range of <code>Calendar</code> has been
 * narrowed. <code>GregorianCalendar</code> used to attempt to support
 * the range of dates with millisecond values from
 * <code>Long.MIN_VALUE</code> to <code>Long.MAX_VALUE</code>. This
 * introduced awkward constructions (hacks) which slowed down
 * performance. It also introduced non-uniform behavior at the
 * boundaries. The new <code>Calendar</code> protocol specifies the
 * maximum range of supportable dates as those having Julian day numbers
 * of <code>-0x7F000000</code> to <code>+0x7F000000</code>. This
 * corresponds to years from ~5,000,000 BCE to ~5,000,000 CE. Programmers
 * should use the constants {@link #MIN_DATE} (or
 * {@link #MIN_MILLIS} or {@link #MIN_JULIAN}) and
 * {@link #MAX_DATE} (or {@link #MAX_MILLIS} or
 * {@link #MAX_JULIAN}) in <code>Calendar</code> to specify an
 * extremely early or extremely late date.</p>
 *
 * <p><big><b>General notes</b></big>
 *
 * <ul>
 *
 *   <li>Calendars implementations are <em>proleptic</em>. For example,
 *     even though the Gregorian calendar was not instituted until the
 *     16th century, the <code>GregorianCalendar</code> class supports
 *     dates before the historical onset of the calendar by extending the
 *     calendar system backward in time. Similarly, the
 *     <code>HebrewCalendar</code> extends backward before the start of
 *     its epoch into zero and negative years. Subclasses do not throw
 *     exceptions because a date precedes the historical start of a
 *     calendar system. Instead, they implement
 *     {@link #handleGetLimit} to return appropriate limits on
 *     {@link #YEAR}, {@link #ERA}, etc. fields. Then, if the
 *     calendar is set to not be lenient, out-of-range field values will
 *     trigger an exception.</li>
 *
 *   <li>Calendar system subclasses compute a <em>extended
 *     year</em>. This differs from the {@link #YEAR} field in that
 *     it ranges over all integer values, including zero and negative
 *     values, and it encapsulates the information of the
 *     {@link #YEAR} field and all larger fields.  Thus, for the
 *     Gregorian calendar, the {@link #EXTENDED_YEAR} is computed as
 *     <code>ERA==AD ? YEAR : 1-YEAR</code>. Another example is the Mayan
 *     long count, which has years (<code>KUN</code>) and nested cycles
 *     of years (<code>KATUN</code> and <code>BAKTUN</code>). The Mayan
 *     {@link #EXTENDED_YEAR} is computed as <code>TUN + 20 * (KATUN
 *     + 20 * BAKTUN)</code>. The <code>Calendar</code> base class uses
 *     the {@link #EXTENDED_YEAR} field to compute the week-related
 *     fields.</li>
 *
 * </ul>
 *
 * @see          Date
 * @see          GregorianCalendar
 * @see          TimeZone
 * @see          DateFormat
 * @author Mark Davis, David Goldsmith, Chen-Lieh Huang, Alan Liu, Laura Werner
 * @stable ICU 2.0
 */
public class Calendar implements Serializable, Cloneable, Comparable {
    private static final long serialVersionUID = 1;
        
    /**
     * @internal
     */
    public final java.util.Calendar calendar;
        
    /**
     * @internal
     * @param delegate the Calendar to which to delegate
     */
    public Calendar(java.util.Calendar delegate) {
        this.calendar = delegate;
    }
        
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * era, e.g., AD or BC in the Julian calendar. This is a calendar-specific
     * value; see subclass documentation.
     * @see GregorianCalendar#AD
     * @see GregorianCalendar#BC
     * @stable ICU 2.0
     */
    public final static int ERA = 0;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * year. This is a calendar-specific value; see subclass documentation.
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public final static int WEEK_OF_MONTH = 4;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * day of the month. This is a synonym for <code>DAY_OF_MONTH</code>.
     * The first day of the month has value 1.
     * @see #DAY_OF_MONTH
     * @stable ICU 2.0
     */
    public final static int DATE = 5;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * day of the month. This is a synonym for <code>DATE</code>.
     * The first day of the month has value 1.
     * @see #DATE
     * @stable ICU 2.0
     */
    public final static int DAY_OF_MONTH = 5;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the day
     * number within the current year.  The first day of the year has value 1.
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public final static int DAY_OF_WEEK_IN_MONTH = 8;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating
     * whether the <code>HOUR</code> is before or after noon.
     * E.g., at 10:04:15.250 PM the <code>AM_PM</code> is <code>PM</code>.
     * @see #AM
     * @see #PM
     * @see #HOUR
     * @stable ICU 2.0
     */
    public final static int AM_PM = 9;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * hour of the morning or afternoon. <code>HOUR</code> is used for the 12-hour
     * clock.
     * E.g., at 10:04:15.250 PM the <code>HOUR</code> is 10.
     * @see #AM_PM
     * @see #HOUR_OF_DAY
     * @stable ICU 2.0
     */
    public final static int HOUR = 10;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * hour of the day. <code>HOUR_OF_DAY</code> is used for the 24-hour clock.
     * E.g., at 10:04:15.250 PM the <code>HOUR_OF_DAY</code> is 22.
     * @see #HOUR
     * @stable ICU 2.0
     */
    public final static int HOUR_OF_DAY = 11;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * minute within the hour.
     * E.g., at 10:04:15.250 PM the <code>MINUTE</code> is 4.
     * @stable ICU 2.0
     */
    public final static int MINUTE = 12;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * second within the minute.
     * E.g., at 10:04:15.250 PM the <code>SECOND</code> is 15.
     * @stable ICU 2.0
     */
    public final static int SECOND = 13;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * millisecond within the second.
     * E.g., at 10:04:15.250 PM the <code>MILLISECOND</code> is 250.
     * @stable ICU 2.0
     */
    public final static int MILLISECOND = 14;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * raw offset from GMT in milliseconds.
     * @stable ICU 2.0
     */
    public final static int ZONE_OFFSET = 15;
        
    /**
     * Field number for <code>get</code> and <code>set</code> indicating the
     * daylight savings offset in milliseconds.
     * @stable ICU 2.0
     */
    public final static int DST_OFFSET = 16;
        
    /**
     * Field number for <code>get()</code> and <code>set()</code>
     * indicating the extended year corresponding to the
     * <code>WEEK_OF_YEAR</code> field.  This may be one greater or less
     * than the value of <code>EXTENDED_YEAR</code>.
     * @stable ICU 2.0
     */
    public static final int YEAR_WOY = 17;
        
    /**
     * Field number for <code>get()</code> and <code>set()</code>
     * indicating the localized day of week.  This will be a value from 1
     * to 7 inclusive, with 1 being the localized first day of the week.
     * @stable ICU 2.0
     */
    public static final int DOW_LOCAL = 18;
        
    /**
     * Field number for <code>get()</code> and <code>set()</code>
     * indicating the extended year.  This is a single number designating
     * the year of this calendar system, encompassing all supra-year
     * fields.  For example, for the Julian calendar system, year numbers
     * are positive, with an era of BCE or CE.  An extended year value for
     * the Julian calendar system assigns positive values to CE years and
     * negative values to BCE years, with 1 BCE being year 0.
     * @stable ICU 2.0
     */
    public static final int EXTENDED_YEAR = 19;
        
    /**
     * Field number for <code>get()</code> and <code>set()</code>
     * indicating the modified Julian day number.  This is different from
     * the conventional Julian day number in two regards.  First, it
     * demarcates days at local zone midnight, rather than noon GMT.
     * Second, it is a local number; that is, it depends on the local time
     * zone.  It can be thought of as a single number that encompasses all
     * the date-related fields.
     * @stable ICU 2.0
     */
    public static final int JULIAN_DAY = 20;
        
    /**
     * Field number for <code>get()</code> and <code>set()</code>
     * indicating the milliseconds in the day.  This ranges from 0 to
     * 23:59:59.999 (regardless of DST).  This field behaves
     * <em>exactly</em> like a composite of all time-related fields, not
     * including the zone fields.  As such, it also reflects
     * discontinuities of those fields on DST transition days.  On a day of
     * DST onset, it will jump forward.  On a day of DST cessation, it will
     * jump backward.  This reflects the fact that is must be combined with
     * the DST_OFFSET field to obtain a unique local time value.
     * @stable ICU 2.0
     */
    public static final int MILLISECONDS_IN_DAY = 21;
        
    /**
     * The number of fields defined by this class.  Subclasses may define
     * addition fields starting with this number.
     * @stable ICU 2.0
     */
    protected static final int BASE_FIELD_COUNT = 22;
        
    /**
     * The maximum number of fields possible.  Subclasses must not define
     * more total fields than this number.
     * @stable ICU 2.0
     */
    protected static final int MAX_FIELD_COUNT = 32;
        
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Sunday.
     * @stable ICU 2.0
     */
    public final static int SUNDAY = 1;
        
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Monday.
     * @stable ICU 2.0
     */
    public final static int MONDAY = 2;
        
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Tuesday.
     * @stable ICU 2.0
     */
    public final static int TUESDAY = 3;
        
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Wednesday.
     * @stable ICU 2.0
     */
    public final static int WEDNESDAY = 4;
        
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Thursday.
     * @stable ICU 2.0
     */
    public final static int THURSDAY = 5;
        
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Friday.
     * @stable ICU 2.0
     */
    public final static int FRIDAY = 6;
        
    /**
     * Value of the <code>DAY_OF_WEEK</code> field indicating
     * Saturday.
     * @stable ICU 2.0
     */
    public final static int SATURDAY = 7;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * first month of the year.
     * @stable ICU 2.0
     */
    public final static int JANUARY = 0;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * second month of the year.
     * @stable ICU 2.0
     */
    public final static int FEBRUARY = 1;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * third month of the year.
     * @stable ICU 2.0
     */
    public final static int MARCH = 2;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * fourth month of the year.
     * @stable ICU 2.0
     */
    public final static int APRIL = 3;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * fifth month of the year.
     * @stable ICU 2.0
     */
    public final static int MAY = 4;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * sixth month of the year.
     * @stable ICU 2.0
     */
    public final static int JUNE = 5;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * seventh month of the year.
     * @stable ICU 2.0
     */
    public final static int JULY = 6;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * eighth month of the year.
     * @stable ICU 2.0
     */
    public final static int AUGUST = 7;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * ninth month of the year.
     * @stable ICU 2.0
     */
    public final static int SEPTEMBER = 8;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * tenth month of the year.
     * @stable ICU 2.0
     */
    public final static int OCTOBER = 9;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * eleventh month of the year.
     * @stable ICU 2.0
     */
    public final static int NOVEMBER = 10;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * twelfth month of the year.
     * @stable ICU 2.0
     */
    public final static int DECEMBER = 11;
        
    /**
     * Value of the <code>MONTH</code> field indicating the
     * thirteenth month of the year. Although <code>GregorianCalendar</code>
     * does not use this value, lunar calendars do.
     * @stable ICU 2.0
     */
    public final static int UNDECIMBER = 12;
        
    /**
     * Value of the <code>AM_PM</code> field indicating the
     * period of the day from midnight to just before noon.
     * @stable ICU 2.0
     */
    public final static int AM = 0;
        
    /**
     * Value of the <code>AM_PM</code> field indicating the
     * period of the day from noon to just before midnight.
     * @stable ICU 2.0
     */
    public final static int PM = 1;
        
    /**
     * Value returned by getDayOfWeekType(int dayOfWeek) to indicate a
     * weekday.
     * @see #WEEKEND
     * @see #WEEKEND_ONSET
     * @see #WEEKEND_CEASE
     * @see #getDayOfWeekType
     * @stable ICU 2.0
     */
    public static final int WEEKDAY = 0;
        
    /**
     * Value returned by getDayOfWeekType(int dayOfWeek) to indicate a
     * weekend day.
     * @see #WEEKDAY
     * @see #WEEKEND_ONSET
     * @see #WEEKEND_CEASE
     * @see #getDayOfWeekType
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public static final int WEEKEND_CEASE = 3;
    /**
     * Gets a calendar using the default time zone and locale.
     * @return a Calendar.
     * @stable ICU 2.0
     */
    public static synchronized Calendar getInstance() {
        return new Calendar(java.util.Calendar.getInstance());
    }
        
    /**
     * Gets a calendar using the specified time zone and default locale.
     * @param zone the time zone to use
     * @return a Calendar.
     * @stable ICU 2.0
     */
    public static synchronized Calendar getInstance(TimeZone zone) {
        return new Calendar(java.util.Calendar.getInstance(zone.timeZone));
    }
        
    /**
     * Gets a calendar using the default time zone and specified locale.
     * @param aLocale the locale for the week data
     * @return a Calendar.
     * @stable ICU 2.0
     */
    public static synchronized Calendar getInstance(Locale aLocale) {
        return new Calendar(java.util.Calendar.getInstance(aLocale));
    }
        
    /**
     * Gets a calendar using the default time zone and specified locale.  
     * @param locale the ulocale for the week data
     * @return a Calendar.
     * @stable ICU 3.2
     */
    public static synchronized Calendar getInstance(ULocale locale)     {
        return new Calendar(java.util.Calendar.getInstance(locale.toLocale()));
    }
        
    /**
     * Gets a calendar with the specified time zone and locale.
     * @param zone the time zone to use
     * @param aLocale the locale for the week data
     * @return a Calendar.
     * @stable ICU 2.0
     */
    public static synchronized Calendar getInstance(TimeZone zone, Locale aLocale) {
        return new Calendar(java.util.Calendar.getInstance(zone.timeZone, aLocale));
    }
        
    /**
     * Gets a calendar with the specified time zone and locale.
     * @param zone the time zone to use
     * @param locale the ulocale for the week data
     * @return a Calendar.
     * @stable ICU 3.2
     */
    public static synchronized Calendar getInstance(TimeZone zone, ULocale locale) {
        return new Calendar(java.util.Calendar.getInstance(zone.timeZone, locale.toLocale()));
    }
        
    /**
     * Gets the list of locales for which Calendars are installed.
     * @return the list of locales for which Calendars are installed.
     * @stable ICU 2.0
     */
    public static Locale[] getAvailableLocales() {
        return java.util.Calendar.getAvailableLocales();
    }
        
    /**
     * Gets the list of locales for which Calendars are installed.
     * @return the list of locales for which Calendars are installed.
     * @draft ICU 3.2 (retain)
     */
    public static ULocale[] getAvailableULocales() {
        if (availableULocales == null) {
            Locale[] locales = java.util.Calendar.getAvailableLocales();
            ULocale[] ulocales = new ULocale[locales.length];
            for (int i = 0; i < locales.length; ++i) {
                ulocales[i] = ULocale.forLocale(locales[i]);
            }
            availableULocales = ulocales;
        }
        return (ULocale[])availableULocales.clone();
    }
    private static ULocale[] availableULocales;
        
    /**
     * Gets this Calendar's current time.
     * @return the current time.
     * @stable ICU 2.0
     */
    public final Date getTime() {
        return calendar.getTime();
    }
        
    /**
     * Sets this Calendar's current time with the given Date.
     * <p>
     * Note: Calling <code>setTime()</code> with
     * <code>Date(Long.MAX_VALUE)</code> or <code>Date(Long.MIN_VALUE)</code>
     * may yield incorrect field values from <code>get()</code>.
     * @param date the given Date.
     * @stable ICU 2.0
     */
    public final void setTime(Date date) {
        calendar.setTime(date);
    }
        
    /**
     * Gets this Calendar's current time as a long.
     * @return the current time as UTC milliseconds from the epoch.
     * @stable ICU 2.0
     */
    public long getTimeInMillis() {
        return calendar.getTime().getTime();
    }
        
    /**
     * Sets this Calendar's current time from the given long value.
     * @param millis the new time in UTC milliseconds from the epoch.
     * @stable ICU 2.0
     */
    public void setTimeInMillis(long millis) {
        calendar.setTime(new Date(millis));
    }
        
    /**
     * Gets the value for a given time field.
     * @param field the given time field.
     * @return the value for the given time field.
     * @stable ICU 2.0
     */
    public final int get(int field) {
        return calendar.get(field);
    }
                
    /**
     * Sets the time field with the given value.
     * @param field the given time field.
     * @param value the value to be set for the given time field.
     * @stable ICU 2.0
     */
    public final void set(int field, int value) {
        calendar.set(field, value);
    }
        
    /**
     * Sets the values for the fields year, month, and date.
     * Previous values of other fields are retained.  If this is not desired,
     * call <code>clear</code> first.
     * @param year the value used to set the YEAR time field.
     * @param month the value used to set the MONTH time field.
     * Month value is 0-based. e.g., 0 for January.
     * @param date the value used to set the DATE time field.
     * @stable ICU 2.0
     */
    public final void set(int year, int month, int date) {
        calendar.set(year, month, date);
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
     * @stable ICU 2.0
     */
    public final void set(int year, int month, int date, int hour, int minute) {
        calendar.set(year, month, date, hour, minute);
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
     * @stable ICU 2.0
     */
    public final void set(int year, int month, int date, int hour, int minute, int second) {
        calendar.set(year, month, date, hour, minute, second);
    }
        
    /**
     * Clears the values of all the time fields.
     * @stable ICU 2.0
     */
    public final void clear() {
        calendar.clear();
    }
        
    /**
     * Clears the value in the given time field.
     * @param field the time field to be cleared.
     * @stable ICU 2.0
     */
    public final void clear(int field) {
        calendar.clear(field);
    }
        
    /**
     * Determines if the given time field has a value set.
     * @return true if the given time field has a value set; false otherwise.
     * @stable ICU 2.0
     */
    public final boolean isSet(int field) {
        return calendar.isSet(field);
    }
                
    /**
     * Compares this calendar to the specified object.
     * The result is <code>true</code> if and only if the argument is
     * not <code>null</code> and is a <code>Calendar</code> object that
     * represents the same calendar as this object.
     * @param obj the object to compare with.
     * @return <code>true</code> if the objects are the same;
     * <code>false</code> otherwise.
     * @stable ICU 2.0
     */
    public boolean equals(Object obj) {
        try {
            return calendar.equals(((Calendar)obj).calendar);
        }
        catch (Exception e) {
            return false;
        }
    }
        
    /**
     * Returns true if the given Calendar object is equivalent to this
     * one.  An equivalent Calendar will behave exactly as this one
     * does, but it may be set to a different time.  By contrast, for
     * the equals() method to return true, the other Calendar must
     * be set to the same time.
     *
     * @param other the Calendar to be compared with this Calendar
     * @stable ICU 2.4
     */
    public boolean isEquivalentTo(Calendar other) {
        return this.getClass() == other.getClass() &&
            isLenient() == other.isLenient() &&
            getFirstDayOfWeek() == other.getFirstDayOfWeek() &&
            getMinimalDaysInFirstWeek() == other.getMinimalDaysInFirstWeek() &&
            getTimeZone().equals(other.getTimeZone());
    }
        
    /**
     * Returns a hash code for this calendar.
     * @return a hash code value for this object.
     * @stable ICU 2.0
     */
    public int hashCode() {
        return calendar.hashCode();
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
            throw new IllegalArgumentException(that + "is not a Calendar or Date");
        }
        return getTimeInMillis() - thatMs;
    }
        
    /**
     * Compares the time field records.
     * Equivalent to comparing result of conversion to UTC.
     * @param when the Calendar to be compared with this Calendar.
     * @return true if the current time of this Calendar is before
     * the time of Calendar when; false otherwise.
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public boolean after(Object when) {
        return compare(when) > 0;
    }
        
    /**
     * Return the maximum value that this field could have, given the
     * current date.  For example, with the Gregorian date February 3, 1997
     * and the {@link #DAY_OF_MONTH DAY_OF_MONTH} field, the actual maximum
     * is 28; for February 3, 1996 it is 29.
     *
     * <p>The actual maximum computation ignores smaller fields and the
     * current value of like-sized fields.  For example, the actual maximum
     * of the DAY_OF_YEAR or MONTH depends only on the year and supra-year
     * fields.  The actual maximum of the DAY_OF_MONTH depends, in
     * addition, on the MONTH field and any other fields at that
     * granularity (such as ChineseCalendar.IS_LEAP_MONTH).  The
     * DAY_OF_WEEK_IN_MONTH field does not depend on the current
     * DAY_OF_WEEK; it returns the maximum for any day of week in the
     * current month.  Likewise for the WEEK_OF_MONTH and WEEK_OF_YEAR
     * fields.
     *
     * @param field the field whose maximum is desired
     * @return the maximum of the given field for the current date of this calendar
     * @see #getMaximum
     * @see #getLeastMaximum
     * @stable ICU 2.0
     */
    public int getActualMaximum(int field) {
        return calendar.getActualMaximum(field);
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
     * @param field the field whose actual minimum value is desired.
     * @return the minimum of the given field for the current date of this calendar
     *
     * @see #getMinimum
     * @see #getGreatestMinimum
     * @stable ICU 2.0
     */
    public int getActualMinimum(int field) {
        return calendar.getActualMinimum(field);
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
     * @stable ICU 2.0
     */
    public final void roll(int field, boolean up) {
        calendar.roll(field, up);
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
     * The <code>com.ibm.icu.util.Calendar</code> implementation of this method is able to roll
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
     * @stable ICU 2.0
     */
    public void roll(int field, int amount) {
        calendar.roll(field, amount);
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
     * The <code>com.ibm.icu.util.Calendar</code> implementation of this method is able to add to
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
     * @stable ICU 2.0
     */
    public void add(int field, int amount) {
        calendar.add(field, amount);
    }
        
    /**
     * Return the name of this calendar in the language of the given locale.
     * @stable ICU 2.0
     */
    public String getDisplayName(Locale loc) {
        return "Calendar";
    }
        
    /**
     * Return the name of this calendar in the language of the given locale.
     * @stable ICU 3.2
     */
    public String getDisplayName(ULocale loc) {
        return "Calendar";
    }
        
    /**
     * Compares the times (in millis) represented by two
     * <code>Calendar</code> objects.
     *
     * @param that the <code>Calendar</code> to compare to this.
     * @return <code>0</code> if the time represented by 
     * this <code>Calendar</code> is equal to the time represented 
     * by that <code>Calendar</code>, a value less than 
     * <code>0</code> if the time represented by this is before
     * the time represented by that, and a value greater than
     * <code>0</code> if the time represented by this
     * is after the time represented by that.
     * @throws NullPointerException if that 
     * <code>Calendar</code> is null.
     * @throws IllegalArgumentException if the time of that 
     * <code>Calendar</code> can't be obtained because of invalid
     * calendar values.
     * @stable ICU 3.4
     */
    public int compareTo(Calendar that) {
        long v = getTimeInMillis() - that.getTimeInMillis();
        return v < 0 ? -1 : (v > 0 ? 1 : 0);
    }
        
    /**
     * Implement comparable API as a convenience override of
     * {@link #compareTo(Calendar)}.
     * @stable ICU 3.4
     */
    public int compareTo(Object that) {
        return compareTo((Calendar)that);
    }
        
    //-------------------------------------------------------------------------
    // Interface for creating custon DateFormats for different types of Calendars
    //-------------------------------------------------------------------------
        
    /**
     * Return a <code>DateFormat</code> appropriate to this calendar.
     * @stable ICU 2.0
     */
    public DateFormat getDateTimeFormat(int dateStyle, int timeStyle, Locale loc) {
        return formatHelper(this, loc, dateStyle, timeStyle);
    }
        
    /**
     * Return a <code>DateFormat</code> appropriate to this calendar.
     * @stable ICU 3.2
     */
    public DateFormat getDateTimeFormat(int dateStyle, int timeStyle, ULocale loc) {
        return formatHelper(this, loc.toLocale(), dateStyle, timeStyle);
    }
        
    /*
     * Utility for formatting.
     */
    private static DateFormat formatHelper(Calendar cal, Locale loc, int dateStyle, int timeStyle) {
        // Assume there is only one kind of calendar.  In Java 6 there will also be Japanese
        // calendars, but we'll worry about that when it happens.
        DateFormat df = DateFormat.getDateTimeInstance(dateStyle, timeStyle, loc);
        df.setCalendar(cal);
        return df;
    }

    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------
        
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
     * 3 months before <code>when</code>, and possibly some additional
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
     * @stable ICU 2.0
     */
    public int fieldDifference(Date when, int field) {
        int min = 0;
        long startMs = getTimeInMillis();
        long targetMs = when.getTime();
        // Always add from the start millis.  This accomodates
        // operations like adding years from February 29, 2000 up to
        // February 29, 2004.  If 1, 1, 1, 1 is added to the year
        // field, the DOM gets pinned to 28 and stays there, giving an
        // incorrect DOM difference of 1.  We have to add 1, reset, 2,
        // reset, 3, reset, 4.
        if (startMs < targetMs) {
            int max = 1;
            // Find a value that is too large
            for (;;) {
                setTimeInMillis(startMs);
                add(field, max);
                long ms = getTimeInMillis();
                if (ms == targetMs) {
                    return max;
                } else if (ms > targetMs) {
                    break;
                } else {
                    max <<= 1;
                    if (max < 0) {
                        // Field difference too large to fit into int
                        throw new RuntimeException();
                    }
                }
            }
            // Do a binary search
            while ((max - min) > 1) {
                int t = (min + max) / 2;
                setTimeInMillis(startMs);
                add(field, t);
                long ms = getTimeInMillis();
                if (ms == targetMs) {
                    return t;
                } else if (ms > targetMs) {
                    max = t;
                } else {
                    min = t;
                }
            }
        } else if (startMs > targetMs) {
            if (false) {
                // This works, and makes the code smaller, but costs
                // an extra object creation and an extra couple cycles
                // of calendar computation.
                setTimeInMillis(targetMs);
                min = -fieldDifference(new Date(startMs), field);
            }
            int max = -1;
            // Find a value that is too small
            for (;;) {
                setTimeInMillis(startMs);
                add(field, max);
                long ms = getTimeInMillis();
                if (ms == targetMs) {
                    return max;
                } else if (ms < targetMs) {
                    break;
                } else {
                    max <<= 1;
                    if (max == 0) {
                        // Field difference too large to fit into int
                        throw new RuntimeException();
                    }
                }
            }
            // Do a binary search
            while ((min - max) > 1) {
                int t = (min + max) / 2;
                setTimeInMillis(startMs);
                add(field, t);
                long ms = getTimeInMillis();
                if (ms == targetMs) {
                    return t;
                } else if (ms < targetMs) {
                    max = t;
                } else {
                    min = t;
                }
            }
        }
        // Set calendar to end point
        setTimeInMillis(startMs);
        add(field, min);
        return min;
    }
        
    /**
     * Sets the time zone with the given time zone value.
     * @param value the given time zone.
     * @stable ICU 2.0
     */
    public void setTimeZone(TimeZone value) {
        calendar.setTimeZone(value.timeZone);
    }
        
    /**
     * Gets the time zone.
     * @return the time zone object associated with this calendar.
     * @stable ICU 2.0
     */
    public TimeZone getTimeZone() {
        return new TimeZone(calendar.getTimeZone());
    }
        
    /**
     * Specify whether or not date/time interpretation is to be lenient.  With
     * lenient interpretation, a date such as "February 942, 1996" will be
     * treated as being equivalent to the 941st day after February 1, 1996.
     * With strict interpretation, such dates will cause an exception to be
     * thrown.
     *
     * @see DateFormat#setLenient
     * @stable ICU 2.0
     */
    public void setLenient(boolean lenient)     {
        calendar.setLenient(lenient);
    }
        
    /**
     * Tell whether date/time interpretation is to be lenient.
     * @stable ICU 2.0
     */
    public boolean isLenient() {
        return calendar.isLenient();
    }
        
    /**
     * Sets what the first day of the week is; e.g., Sunday in US,
     * Monday in France.
     * @param value the given first day of the week.
     * @stable ICU 2.0
     */
    public void setFirstDayOfWeek(int value) {
        calendar.setFirstDayOfWeek(value);
    }
        
    /**
     * Gets what the first day of the week is; e.g., Sunday in US,
     * Monday in France.
     * @return the first day of the week.
     * @stable ICU 2.0
     */
    public int getFirstDayOfWeek() {
        return calendar.getFirstDayOfWeek();
    }
        
    /**
     * Sets what the minimal days required in the first week of the year are.
     * For example, if the first week is defined as one that contains the first
     * day of the first month of a year, call the method with value 1. If it
     * must be a full week, use value 7.
     * @param value the given minimal days required in the first week
     * of the year.
     * @stable ICU 2.0
     */
    public void setMinimalDaysInFirstWeek(int value) {
        calendar.setMinimalDaysInFirstWeek(value);
    }
        
    /**
     * Gets what the minimal days required in the first week of the year are;
     * e.g., if the first week is defined as one that contains the first day
     * of the first month of a year, getMinimalDaysInFirstWeek returns 1. If
     * the minimal days required must be a full week, getMinimalDaysInFirstWeek
     * returns 7.
     * @return the minimal days required in the first week of the year.
     * @stable ICU 2.0
     */
    public int getMinimalDaysInFirstWeek() {
        return calendar.getMinimalDaysInFirstWeek();
    }
        
    /**
     * Gets the minimum value for the given time field.
     * e.g., for Gregorian DAY_OF_MONTH, 1.
     * @param field the given time field.
     * @return the minimum value for the given time field.
     * @stable ICU 2.0
     */
    public final int getMinimum(int field) {
        return calendar.getMinimum(field);
    }
        
    /**
     * Gets the maximum value for the given time field.
     * e.g. for Gregorian DAY_OF_MONTH, 31.
     * @param field the given time field.
     * @return the maximum value for the given time field.
     * @stable ICU 2.0
     */
    public final int getMaximum(int field) {
        return calendar.getMaximum(field);
    }
        
    /**
     * Gets the highest minimum value for the given field if varies.
     * Otherwise same as getMinimum(). For Gregorian, no difference.
     * @param field the given time field.
     * @return the highest minimum value for the given time field.
     * @stable ICU 2.0
     */
    public final int getGreatestMinimum(int field) {
        return calendar.getGreatestMinimum(field);
    }
        
    /**
     * Gets the lowest maximum value for the given field if varies.
     * Otherwise same as getMaximum(). e.g., for Gregorian DAY_OF_MONTH, 28.
     * @param field the given time field.
     * @return the lowest maximum value for the given time field.
     * @stable ICU 2.0
     */
    public final int getLeastMaximum(int field) {
        return calendar.getLeastMaximum(field);
    }
        
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
     * @stable ICU 2.0
     */
    public int getDayOfWeekType(int dayOfWeek) {
        // weekend always full saturday and sunday
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("illegal day of week: " + dayOfWeek);
        }
        return dayOfWeek >= SATURDAY ? WEEKEND : WEEKDAY;
    }
        
    /**
     * Return the time during the day at which the weekend begins or end in
     * this calendar system.  If getDayOfWeekType(dayOfWeek) ==
     * WEEKEND_ONSET return the time at which the weekend begins.  If
     * getDayOfWeekType(dayOfWeek) == WEEKEND_CEASE return the time at
     * which the weekend ends.  If getDayOfWeekType(dayOfWeek) has some
     * other value, then throw an exception.
     * @param dayOfWeek either SUNDAY, MONDAY, TUESDAY, WEDNESDAY,
     * THURSDAY, FRIDAY, or SATURDAY
     * @return the milliseconds after midnight at which the
     * weekend begins or ends
     * @exception IllegalArgumentException if dayOfWeek is not
     * WEEKEND_ONSET or WEEKEND_CEASE
     * @see #getDayOfWeekType
     * @see #isWeekend(Date)
     * @see #isWeekend()
     * @stable ICU 2.0
     */
    public int getWeekendTransition(int dayOfWeek) {
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
     * @stable ICU 2.0
     */
    public boolean isWeekend(Date date) {
        calendar.setTime(date);
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
     * @stable ICU 2.0
     */
    public boolean isWeekend() {
        return calendar.get(Calendar.DAY_OF_WEEK) >= SATURDAY;
    }
        
    //-------------------------------------------------------------------------
    // End of weekend support
    //-------------------------------------------------------------------------
        
    /**
     * Overrides Cloneable
     * @stable ICU 2.0
     */
    public Object clone() {
        return new Calendar((java.util.Calendar)calendar.clone());
    }
        
    /**
     * Return a string representation of this calendar. This method
     * is intended to be used only for debugging purposes, and the
     * format of the returned string may vary between implementations.
     * The returned string may be empty but may not be <code>null</code>.
     *
     * @return  a string representation of this calendar.
     * @stable ICU 2.0
     */
    public String toString() {
        return calendar.toString();
    }

    /**
     * Return the current Calendar type.
     * Note, in 3.0 this function will return 'gregorian' in Calendar to emulate legacy behavior
     * @return type of calendar (gregorian, etc)
     * @internal ICU 3.0
     */
    public String getType() {
        return "gregorian";
    }
}


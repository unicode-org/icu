/*
 * @(#)DateFormat.java	1.37 99/11/02
 *
 * (C) Copyright Taligent, Inc. 1996 - All Rights Reserved
 * (C) Copyright IBM Corp. 1996 - All Rights Reserved
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

package com.ibm.text;
import java.text.Format;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.ParseException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import com.ibm.util.TimeZone;
import com.ibm.util.Calendar;
import com.ibm.util.GregorianCalendar;
import java.util.Date;
import java.text.resources.*;

/**
 * DateFormat is an abstract class for date/time formatting subclasses which
 * formats and parses dates or time in a language-independent manner.
 * The date/time formatting subclass, such as SimpleDateFormat, allows for
 * formatting (i.e., date -> text), parsing (text -> date), and
 * normalization.  The date is represented as a <code>Date</code> object or
 * as the milliseconds since January 1, 1970, 00:00:00 GMT.
 *
 * <p>DateFormat provides many class methods for obtaining default date/time
 * formatters based on the default or a given loacle and a number of formatting
 * styles. The formatting styles include FULL, LONG, MEDIUM, and SHORT. More
 * detail and examples of using these styles are provided in the method
 * descriptions.
 *
 * <p>DateFormat helps you to format and parse dates for any locale.
 * Your code can be completely independent of the locale conventions for
 * months, days of the week, or even the calendar format: lunar vs. solar.
 *
 * <p>To format a date for the current Locale, use one of the
 * static factory methods:
 * <pre>
 *  myString = DateFormat.getDateInstance().format(myDate);
 * </pre>
 * <p>If you are formatting multiple numbers, it is
 * more efficient to get the format and use it multiple times so that
 * the system doesn't have to fetch the information about the local
 * language and country conventions multiple times.
 * <pre>
 *  DateFormat df = DateFormat.getDateInstance();
 *  for (int i = 0; i < a.length; ++i) {
 *    output.println(df.format(myDate[i]) + "; ");
 *  }
 * </pre>
 * <p>To format a number for a different Locale, specify it in the
 * call to getDateInstance().
 * <pre>
 *  DateFormat df = DateFormat.getDateInstance(Locale.FRANCE);
 * </pre>
 * <p>You can use a DateFormat to parse also.
 * <pre>
 *  myDate = df.parse(myString);
 * </pre>
 * <p>Use getDateInstance to get the normal date format for that country.
 * There are other static factory methods available.
 * Use getTimeInstance to get the time format for that country.
 * Use getDateTimeInstance to get a date and time format. You can pass in 
 * different options to these factory methods to control the length of the
 * result; from SHORT to MEDIUM to LONG to FULL. The exact result depends
 * on the locale, but generally:
 * <ul><li>SHORT is completely numeric, such as 12.13.52 or 3:30pm
 * <li>MEDIUM is longer, such as Jan 12, 1952
 * <li>LONG is longer, such as January 12, 1952 or 3:30:32pm
 * <li>FULL is pretty completely specified, such as
 * Tuesday, April 12, 1952 AD or 3:30:42pm PST.
 * </ul>
 *
 * <p>You can also set the time zone on the format if you wish.
 * If you want even more control over the format or parsing,
 * (or want to give your users more control),
 * you can try casting the DateFormat you get from the factory methods
 * to a SimpleDateFormat. This will work for the majority
 * of countries; just remember to put it in a try block in case you
 * encounter an unusual one.
 *
 * <p>You can also use forms of the parse and format methods with
 * ParsePosition and FieldPosition to
 * allow you to
 * <ul><li>progressively parse through pieces of a string.
 * <li>align any particular field, or find out where it is for selection
 * on the screen.
 * </ul>
 *
 * @see          Format
 * @see          NumberFormat
 * @see          SimpleDateFormat
 * @see          com.ibm.util.Calendar
 * @see          com.ibm.util.GregorianCalendar
 * @see          com.ibm.util.TimeZone
 * @version      1.37 11/02/99
 * @author       Mark Davis, Chen-Lieh Huang, Alan Liu
 */
public abstract class DateFormat extends Format {

    /**
     * The calendar that <code>DateFormat</code> uses to produce the time field
     * values needed to implement date and time formatting.  Subclasses should
     * initialize this to a calendar appropriate for the locale associated with
     * this <code>DateFormat</code>.
     * @serial
     */
    protected Calendar calendar;

    /**
     * The number formatter that <code>DateFormat</code> uses to format numbers
     * in dates and times.  Subclasses should initialize this to a number format
     * appropriate for the locale associated with this <code>DateFormat</code>.
     * @serial
     */
    protected NumberFormat numberFormat;

    /**
     * Useful constant for ERA field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int ERA_FIELD = 0;
    /**
     * Useful constant for YEAR field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int YEAR_FIELD = 1;
    /**
     * Useful constant for MONTH field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int MONTH_FIELD = 2;
    /**
     * Useful constant for DATE field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int DATE_FIELD = 3;
    /**
     * Useful constant for one-based HOUR_OF_DAY field alignment.
     * Used in FieldPosition of date/time formatting.
     * HOUR_OF_DAY1_FIELD is used for the one-based 24-hour clock.
     * For example, 23:59 + 01:00 results in 24:59.
     */
    public final static int HOUR_OF_DAY1_FIELD = 4;
    /**
     * Useful constant for zero-based HOUR_OF_DAY field alignment.
     * Used in FieldPosition of date/time formatting.
     * HOUR_OF_DAY0_FIELD is used for the zero-based 24-hour clock.
     * For example, 23:59 + 01:00 results in 00:59.
     */
    public final static int HOUR_OF_DAY0_FIELD = 5;
    /**
     * Useful constant for MINUTE field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int MINUTE_FIELD = 6;
    /**
     * Useful constant for SECOND field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int SECOND_FIELD = 7;
    /**
     * Useful constant for MILLISECOND field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int MILLISECOND_FIELD = 8;
    /**
     * Useful constant for DAY_OF_WEEK field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int DAY_OF_WEEK_FIELD = 9;
    /**
     * Useful constant for DAY_OF_YEAR field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int DAY_OF_YEAR_FIELD = 10;
    /**
     * Useful constant for DAY_OF_WEEK_IN_MONTH field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int DAY_OF_WEEK_IN_MONTH_FIELD = 11;
    /**
     * Useful constant for WEEK_OF_YEAR field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int WEEK_OF_YEAR_FIELD = 12;
    /**
     * Useful constant for WEEK_OF_MONTH field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int WEEK_OF_MONTH_FIELD = 13;
    /**
     * Useful constant for AM_PM field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int AM_PM_FIELD = 14;
    /**
     * Useful constant for one-based HOUR field alignment.
     * Used in FieldPosition of date/time formatting.
     * HOUR1_FIELD is used for the one-based 12-hour clock.
     * For example, 11:30 PM + 1 hour results in 12:30 AM.
     */
    public final static int HOUR1_FIELD = 15;
    /**
     * Useful constant for zero-based HOUR field alignment.
     * Used in FieldPosition of date/time formatting.
     * HOUR0_FIELD is used for the zero-based 12-hour clock.
     * For example, 11:30 PM + 1 hour results in 00:30 AM.
     */
    public final static int HOUR0_FIELD = 16;
    /**
     * Useful constant for TIMEZONE field alignment.
     * Used in FieldPosition of date/time formatting.
     */
    public final static int TIMEZONE_FIELD = 17;

    // Proclaim serial compatibility with 1.1 FCS
    private static final long serialVersionUID = 7218322306649953788L;

    /**
     * Overrides Format.
     * Formats a time object into a time string. Examples of time objects
     * are a time value expressed in milliseconds and a Date object.
     * @param obj must be a Number or a Date.
     * @param toAppendTo the string buffer for the returning time string.
     * @return the formatted time string.
     * @param fieldPosition keeps track of the position of the field
     * within the returned string.
     * On input: an alignment field,
     * if desired. On output: the offsets of the alignment field. For
     * example, given a time text "1996.07.10 AD at 15:08:56 PDT",
     * if the given fieldPosition is DateFormat.YEAR_FIELD, the
     * begin index and end index of fieldPosition will be set to
     * 0 and 4, respectively.
     * Notice that if the same time field appears
     * more than once in a pattern, the fieldPosition will be set for the first
     * occurence of that time field. For instance, formatting a Date to
     * the time string "1 PM PDT (Pacific Daylight Time)" using the pattern
     * "h a z (zzzz)" and the alignment field DateFormat.TIMEZONE_FIELD,
     * the begin index and end index of fieldPosition will be set to
     * 5 and 8, respectively, for the first occurence of the timezone
     * pattern character 'z'.
     * @see java.text.Format
     */
    public final StringBuffer format(Object obj, StringBuffer toAppendTo,
                                     FieldPosition fieldPosition)
    {
        if (obj instanceof Date)
            return format( (Date)obj, toAppendTo, fieldPosition );
        else if (obj instanceof Number)
            return format( new Date(((Number)obj).longValue()),
                          toAppendTo, fieldPosition );
        else 
            throw new IllegalArgumentException("Cannot format given Object as a Date");
    }

    /**
     * Formats a Date into a date/time string.
     * @param date a Date to be formatted into a date/time string.
     * @param toAppendTo the string buffer for the returning date/time string.
     * @param fieldPosition keeps track of the position of the field
     * within the returned string.
     * On input: an alignment field,
     * if desired. On output: the offsets of the alignment field. For
     * example, given a time text "1996.07.10 AD at 15:08:56 PDT",
     * if the given fieldPosition is DateFormat.YEAR_FIELD, the
     * begin index and end index of fieldPosition will be set to
     * 0 and 4, respectively.
     * Notice that if the same time field appears
     * more than once in a pattern, the fieldPosition will be set for the first
     * occurence of that time field. For instance, formatting a Date to
     * the time string "1 PM PDT (Pacific Daylight Time)" using the pattern
     * "h a z (zzzz)" and the alignment field DateFormat.TIMEZONE_FIELD,
     * the begin index and end index of fieldPosition will be set to
     * 5 and 8, respectively, for the first occurence of the timezone
     * pattern character 'z'.
     * @return the formatted date/time string.
     */
    public abstract StringBuffer format(Date date, StringBuffer toAppendTo,
                                        FieldPosition fieldPosition);

    /**
     * Formats a Date into a date/time string.
     * @param date the time value to be formatted into a time string.
     * @return the formatted time string.
     */
    public final String format(Date date)
    {
        return format(date, new StringBuffer(),new FieldPosition(0)).toString();
    }

    /**
     * Parse a date/time string.
     *
     * @param text  The date/time string to be parsed
     *
     * @return      A Date, or null if the input could not be parsed
     *
     * @exception  ParseException  If the given string cannot be parsed as a date.
     *
     * @see #parse(String, ParsePosition)
     */
    public Date parse(String text) throws ParseException
    {
        ParsePosition pos = new ParsePosition(0);
        Date result = parse(text, pos);
        if (pos.getIndex() == 0) // ICU4J
            throw new ParseException("Unparseable date: \"" + text + "\"" ,
                                     pos.getErrorIndex()); // ICU4J
        return result;
    }

    /**
     * Parse a date/time string according to the given parse position.  For
     * example, a time text "07/10/96 4:5 PM, PDT" will be parsed into a Date
     * that is equivalent to Date(837039928046).
     *
     * <p> By default, parsing is lenient: If the input is not in the form used
     * by this object's format method but can still be parsed as a date, then
     * the parse succeeds.  Clients may insist on strict adherence to the
     * format by calling setLenient(false).
     *
     * @see #setLenient(boolean)
     *
     * @param text  The date/time string to be parsed
     *
     * @param pos   On input, the position at which to start parsing; on
     *              output, the position at which parsing terminated, or the
     *              start position if the parse failed.
     *
     * @return      A Date, or null if the input could not be parsed
     */
    public abstract Date parse(String text, ParsePosition pos);

    /**
     * Parse a date/time string into an Object.  This convenience method simply
     * calls parse(String, ParsePosition).
     *
     * @see #parse(String, ParsePosition)
     */
    public Object parseObject (String source, ParsePosition pos)
    {
        return parse(source, pos);
    }

    /**
     * Constant for full style pattern.
     */
    public static final int FULL = 0;
    /**
     * Constant for long style pattern.
     */
    public static final int LONG = 1;
    /**
     * Constant for medium style pattern.
     */
    public static final int MEDIUM = 2;
    /**
     * Constant for short style pattern.
     */
    public static final int SHORT = 3;
    /**
     * Constant for default style pattern.  Its value is MEDIUM.
     */
    public static final int DEFAULT = MEDIUM;

    /**
     * Gets the time formatter with the default formatting style
     * for the default locale.
     * @return a time formatter.
     */
    public final static DateFormat getTimeInstance()
    {
        return get(-1, DEFAULT, Locale.getDefault());
    }

    /**
     * Gets the time formatter with the given formatting style
     * for the default locale.
     * @param style the given formatting style. For example,
     * SHORT for "h:mm a" in the US locale.
     * @return a time formatter.
     */
    public final static DateFormat getTimeInstance(int style)
    {
        return get(-1, style, Locale.getDefault());
    }

    /**
     * Gets the time formatter with the given formatting style
     * for the given locale.
     * @param style the given formatting style. For example,
     * SHORT for "h:mm a" in the US locale.
     * @param aLocale the given locale.
     * @return a time formatter.
     */
    public final static DateFormat getTimeInstance(int style,
                                                 Locale aLocale)
    {
        return get(-1, style, aLocale);
    }

    /**
     * Gets the date formatter with the default formatting style
     * for the default locale.
     * @return a date formatter.
     */
    public final static DateFormat getDateInstance()
    {
        return get(DEFAULT, -1, Locale.getDefault());
    }

    /**
     * Gets the date formatter with the given formatting style
     * for the default locale.
     * @param style the given formatting style. For example,
     * SHORT for "M/d/yy" in the US locale.
     * @return a date formatter.
     */
    public final static DateFormat getDateInstance(int style)
    {
        return get(style, -1, Locale.getDefault());
    }

    /**
     * Gets the date formatter with the given formatting style
     * for the given locale.
     * @param style the given formatting style. For example,
     * SHORT for "M/d/yy" in the US locale.
     * @param aLocale the given locale.
     * @return a date formatter.
     */
    public final static DateFormat getDateInstance(int style,
                                                 Locale aLocale)
    {
        return get(style, -1, aLocale);
    }

    /**
     * Gets the date/time formatter with the default formatting style
     * for the default locale.
     * @return a date/time formatter.
     */
    public final static DateFormat getDateTimeInstance()
    {
        return get(DEFAULT, DEFAULT, Locale.getDefault());
    }

    /**
     * Gets the date/time formatter with the given date and time
     * formatting styles for the default locale.
     * @param dateStyle the given date formatting style. For example,
     * SHORT for "M/d/yy" in the US locale.
     * @param timeStyle the given time formatting style. For example,
     * SHORT for "h:mm a" in the US locale.
     * @return a date/time formatter.
     */
    public final static DateFormat getDateTimeInstance(int dateStyle,
                                                       int timeStyle)
    {
        return get(timeStyle, dateStyle, Locale.getDefault());
    }

    /**
     * Gets the date/time formatter with the given formatting styles
     * for the given locale.
     * @param dateStyle the given date formatting style.
     * @param timeStyle the given time formatting style.
     * @param aLocale the given locale.
     * @return a date/time formatter.
     */
    public final static DateFormat
        getDateTimeInstance(int dateStyle, int timeStyle, Locale aLocale)
    {
        return get(timeStyle, dateStyle, aLocale);
    }

    /**
     * Get a default date/time formatter that uses the SHORT style for both the
     * date and the time.
     */
    public final static DateFormat getInstance() {
        return getDateTimeInstance(SHORT, SHORT);
    }

    /**
     * Gets the set of locales for which DateFormats are installed.
     * @return the set of locales for which DateFormats are installed.
     */
    public static Locale[] getAvailableLocales()
    {
        return LocaleData.getAvailableLocales("DateTimePatterns");
    }

    /**
     * Set the calendar to be used by this date format.  Initially, the default
     * calendar for the specified or default locale is used.
     * @param newCalendar the new Calendar to be used by the date format
     */
    public void setCalendar(Calendar newCalendar)
    {
        this.calendar = newCalendar;
    }

    /**
     * Gets the calendar associated with this date/time formatter.
     * @return the calendar associated with this date/time formatter.
     */
    public Calendar getCalendar()
    {
        return calendar;
    }

    /**
     * Allows you to set the number formatter.
     * @param newNumberFormat the given new NumberFormat.
     */
    public void setNumberFormat(NumberFormat newNumberFormat)
    {
        this.numberFormat = newNumberFormat;
    }

    /**
     * Gets the number formatter which this date/time formatter uses to
     * format and parse a time.
     * @return the number formatter which this date/time formatter uses.
     */
    public NumberFormat getNumberFormat()
    {
        return numberFormat;
    }

    /**
     * Sets the time zone for the calendar of this DateFormat object.
     * @param zone the given new time zone.
     */
    public void setTimeZone(TimeZone zone)
    {
        calendar.setTimeZone(zone);
    }

    /**
     * Gets the time zone.
     * @return the time zone associated with the calendar of DateFormat.
     */
    public TimeZone getTimeZone()
    {
        return calendar.getTimeZone();
    }

    /**
     * Specify whether or not date/time parsing is to be lenient.  With
     * lenient parsing, the parser may use heuristics to interpret inputs that
     * do not precisely match this object's format.  With strict parsing,
     * inputs must match this object's format.
     * @param lenient when true, parsing is lenient
     * @see com.ibm.util.Calendar#setLenient
     */
    public void setLenient(boolean lenient)
    {
        calendar.setLenient(lenient);
    }

    /**
     * Tell whether date/time parsing is to be lenient.
     */
    public boolean isLenient()
    {
        return calendar.isLenient();
    }

    /**
     * Overrides hashCode
     */
    public int hashCode() {
        return numberFormat.hashCode();
        // just enough fields for a reasonable distribution
    }

    /**
     * Overrides equals
     */
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DateFormat other = (DateFormat) obj;
        return (// calendar.equivalentTo(other.calendar) // THIS API DOESN'T EXIST YET!
                calendar.getFirstDayOfWeek() == other.calendar.getFirstDayOfWeek() &&
                calendar.getMinimalDaysInFirstWeek() == other.calendar.getMinimalDaysInFirstWeek() &&
                calendar.isLenient() == other.calendar.isLenient() &&
                calendar.getTimeZone().equals(other.calendar.getTimeZone()) &&
                numberFormat.equals(other.numberFormat));
    }

    /**
     * Overrides Cloneable
     */
    public Object clone()
    {
        DateFormat other = (DateFormat) super.clone();
        other.calendar = (Calendar) calendar.clone();
        other.numberFormat = (NumberFormat) numberFormat.clone();
        return other;
    }

    /**
     * Creates a DateFormat with the given time and/or date style in the given
     * locale.
     * @param dateStyle a value from 0 to 3 indicating the time format,
     * or -1 to indicate no date
     * @param timeStyle a value from 0 to 3 indicating the time format,
     * or -1 to indicate no time
     * @param loc the locale for the format
     */
    private static DateFormat get(int dateStyle, int timeStyle, Locale loc) {
        if (timeStyle < -1 || timeStyle > 3) {
            throw new IllegalArgumentException("Illegal time style " + timeStyle);
        }
        if (dateStyle < -1 || dateStyle > 3) {
            throw new IllegalArgumentException("Illegal date style " + dateStyle);
        }
        try {
            return new SimpleDateFormat(timeStyle, dateStyle, loc);
        } catch (MissingResourceException e) {
            return new SimpleDateFormat("M/d/yy h:mm a");
        }
    }

    /**
     * Create a new date format.
     */
    protected DateFormat() {}

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    //-------------------------------------------------------------------------
    // Public static interface for creating custon DateFormats for different
    // types of Calendars.
    //-------------------------------------------------------------------------
    
    /**
     * Create a {@link DateFormat} object that can be used to format dates in
     * the calendar system specified by <code>cal</code>.
     * <p>
     * @param cal   The calendar system for which a date format is desired.
     *
     * @param dateStyle The type of date format desired.  This can be
     *              {@link DateFormat#SHORT}, {@link DateFormat#MEDIUM},
     *              etc.
     *
     * @param locale The locale for which the date format is desired.
     */
    static final public DateFormat getDateInstance(Calendar cal, int dateStyle, Locale locale)
    {
        return getDateTimeInstance(cal, dateStyle, -1, locale);
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
     * @see DateFormat#getTimeInstance
     */
    static final public DateFormat getTimeInstance(Calendar cal, int timeStyle, Locale locale)
    {
        return getDateTimeInstance(cal, -1, timeStyle, locale);
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
     * @see DateFormat#getDateTimeInstance
     */
    static final public DateFormat getDateTimeInstance(Calendar cal, int dateStyle,
                                                 int timeStyle, Locale locale)
    {
        return cal.getDateTimeFormat(dateStyle, timeStyle, locale);
    }

    /**
     * Convenience overload
     */
    static final public DateFormat getInstance(Calendar cal, Locale locale) {
        return getDateTimeInstance(cal, SHORT, SHORT, locale);
    }

    /**
     * Convenience overload
     */
    static final public DateFormat getInstance(Calendar cal) {
        return getInstance(cal, Locale.getDefault());
    }

    /**
     * Convenience overload
     */
    static final public DateFormat getDateInstance(Calendar cal, int dateStyle) {
        return getDateInstance(cal, dateStyle, Locale.getDefault());
    }

    /**
     * Convenience overload
     */
    static final public DateFormat getTimeInstance(Calendar cal, int timeStyle) {
        return getTimeInstance(cal, timeStyle, Locale.getDefault());
    }

    /**
     * Convenience overload
     */
    static final public DateFormat getDateTimeInstance(Calendar cal, int dateStyle, int timeStyle) {
        return getDateTimeInstance(cal, dateStyle, timeStyle, Locale.getDefault());
    }
}

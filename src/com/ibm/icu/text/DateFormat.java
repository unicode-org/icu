/*
*   Copyright (C) 1996-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*/

package com.ibm.icu.text;

//import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.UFormat;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;

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
 *  DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.FRANCE);
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
 * <h4>Synchronization</h4>
 *
 * Date formats are not synchronized. It is recommended to create separate 
 * format instances for each thread. If multiple threads access a format 
 * concurrently, it must be synchronized externally. 
 *
 * @see          UFormat
 * @see          NumberFormat
 * @see          SimpleDateFormat
 * @see          com.ibm.icu.util.Calendar
 * @see          com.ibm.icu.util.GregorianCalendar
 * @see          com.ibm.icu.util.TimeZone
 * @author       Mark Davis, Chen-Lieh Huang, Alan Liu
 * @stable ICU 2.0
 */
public abstract class DateFormat extends UFormat {

    /**
     * The calendar that <code>DateFormat</code> uses to produce the time field
     * values needed to implement date and time formatting.  Subclasses should
     * initialize this to a calendar appropriate for the locale associated with
     * this <code>DateFormat</code>.
     * @serial
     * @stable ICU 2.0
     */
    protected Calendar calendar;

    /**
     * The number formatter that <code>DateFormat</code> uses to format numbers
     * in dates and times.  Subclasses should initialize this to a number format
     * appropriate for the locale associated with this <code>DateFormat</code>.
     * @serial
     * @stable ICU 2.0
     */
    protected NumberFormat numberFormat;

    /**
     * FieldPosition selector for 'G' field alignment,
     * corresponding to the {@link Calendar#ERA} field.
     * @stable ICU 2.0
     */
    public final static int ERA_FIELD = 0;

    /**
     * FieldPosition selector for 'y' field alignment,
     * corresponding to the {@link Calendar#YEAR} field.
     * @stable ICU 2.0
     */
    public final static int YEAR_FIELD = 1;

    /**
     * FieldPosition selector for 'M' field alignment,
     * corresponding to the {@link Calendar#MONTH} field.
     * @stable ICU 2.0
     */
    public final static int MONTH_FIELD = 2;

    /**
     * FieldPosition selector for 'd' field alignment,
     * corresponding to the {@link Calendar#DATE} field.
     * @stable ICU 2.0
     */
    public final static int DATE_FIELD = 3;

    /**
     * FieldPosition selector for 'k' field alignment,
     * corresponding to the {@link Calendar#HOUR_OF_DAY} field.
     * HOUR_OF_DAY1_FIELD is used for the one-based 24-hour clock.
     * For example, 23:59 + 01:00 results in 24:59.
     * @stable ICU 2.0
     */
    public final static int HOUR_OF_DAY1_FIELD = 4;

    /**
     * FieldPosition selector for 'H' field alignment,
     * corresponding to the {@link Calendar#HOUR_OF_DAY} field.
     * HOUR_OF_DAY0_FIELD is used for the zero-based 24-hour clock.
     * For example, 23:59 + 01:00 results in 00:59.
     * @stable ICU 2.0
     */
    public final static int HOUR_OF_DAY0_FIELD = 5;

    /**
     * FieldPosition selector for 'm' field alignment,
     * corresponding to the {@link Calendar#MINUTE} field.
     * @stable ICU 2.0
     */
    public final static int MINUTE_FIELD = 6;

    /**
     * FieldPosition selector for 's' field alignment,
     * corresponding to the {@link Calendar#SECOND} field.
     * @stable ICU 2.0
     */
    public final static int SECOND_FIELD = 7;

    /**
     * FieldPosition selector for 'S' field alignment,
     * corresponding to the {@link Calendar#MILLISECOND} field.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final static int FRACTIONAL_SECOND_FIELD = 8;

    /**
     * Alias for FRACTIONAL_SECOND_FIELD.
     * @deprecated ICU 3.0 use FRACTIONAL_SECOND_FIELD.
     */
    public final static int MILLISECOND_FIELD = FRACTIONAL_SECOND_FIELD;

    /**
     * FieldPosition selector for 'E' field alignment,
     * corresponding to the {@link Calendar#DAY_OF_WEEK} field.
     * @stable ICU 2.0
     */
    public final static int DAY_OF_WEEK_FIELD = 9;

    /**
     * FieldPosition selector for 'D' field alignment,
     * corresponding to the {@link Calendar#DAY_OF_YEAR} field.
     * @stable ICU 2.0
     */
    public final static int DAY_OF_YEAR_FIELD = 10;

    /**
     * FieldPosition selector for 'F' field alignment,
     * corresponding to the {@link Calendar#DAY_OF_WEEK_IN_MONTH} field.
     * @stable ICU 2.0
     */
    public final static int DAY_OF_WEEK_IN_MONTH_FIELD = 11;

    /**
     * FieldPosition selector for 'w' field alignment,
     * corresponding to the {@link Calendar#WEEK_OF_YEAR} field.
     * @stable ICU 2.0
     */
    public final static int WEEK_OF_YEAR_FIELD = 12;

    /**
     * FieldPosition selector for 'W' field alignment,
     * corresponding to the {@link Calendar#WEEK_OF_MONTH} field.
     * @stable ICU 2.0
     */
    public final static int WEEK_OF_MONTH_FIELD = 13;

    /**
     * FieldPosition selector for 'a' field alignment,
     * corresponding to the {@link Calendar#AM_PM} field.
     * @stable ICU 2.0
     */
    public final static int AM_PM_FIELD = 14;

    /**
     * FieldPosition selector for 'h' field alignment,
     * corresponding to the {@link Calendar#HOUR} field.
     * HOUR1_FIELD is used for the one-based 12-hour clock.
     * For example, 11:30 PM + 1 hour results in 12:30 AM.
     * @stable ICU 2.0
     */
    public final static int HOUR1_FIELD = 15;

    /**
     * FieldPosition selector for 'K' field alignment,
     * corresponding to the {@link Calendar#HOUR} field.
     * HOUR0_FIELD is used for the zero-based 12-hour clock.
     * For example, 11:30 PM + 1 hour results in 00:30 AM.
     * @stable ICU 2.0
     */
    public final static int HOUR0_FIELD = 16;

    /**
     * FieldPosition selector for 'z' field alignment,
     * corresponding to the {@link Calendar#ZONE_OFFSET} and
     * {@link Calendar#DST_OFFSET} fields.
     * @stable ICU 2.0
     */
    public final static int TIMEZONE_FIELD = 17;

    /**
     * FieldPosition selector for 'Y' field alignment,
     * corresponding to the {@link Calendar#YEAR_WOY} field.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final static int YEAR_WOY_FIELD = 18;

    /**
     * FieldPosition selector for 'e' field alignment,
     * corresponding to the {@link Calendar#DOW_LOCAL} field.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final static int DOW_LOCAL_FIELD = 19;

    /**
     * FieldPosition selector for 'u' field alignment,
     * corresponding to the {@link Calendar#EXTENDED_YEAR} field.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final static int EXTENDED_YEAR_FIELD = 20;

    /**
     * FieldPosition selector for 'g' field alignment,
     * corresponding to the {@link Calendar#JULIAN_DAY} field.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final static int JULIAN_DAY_FIELD = 21;

    /**
     * FieldPosition selector for 'A' field alignment,
     * corresponding to the {@link Calendar#MILLISECONDS_IN_DAY} field.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final static int MILLISECONDS_IN_DAY_FIELD = 22;

    /**
     * FieldPosition selector for 'Z' field alignment,
     * corresponding to the {@link Calendar#ZONE_OFFSET} and
     * {@link Calendar#DST_OFFSET} fields.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final static int TIMEZONE_RFC_FIELD = 23;

    /**
     * Number of FieldPosition selectors for DateFormat.
     * Valid selectors range from 0 to FIELD_COUNT-1.
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final static int FIELD_COUNT = 24; // must == DateFormatSymbols.patternChars.length()

    // Proclaim serial compatibility with 1.1 FCS
    private static final long serialVersionUID = 7218322306649953788L;

    /**
     * Overrides Format.
     * Formats a time object into a time string. Examples of time objects
     * are a time value expressed in milliseconds and a Date object.
     * @param obj must be a Number or a Date or a Calendar.
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
     * @stable ICU 2.0
     */
    public final StringBuffer format(Object obj, StringBuffer toAppendTo,
                                     FieldPosition fieldPosition)
    {
        if (obj instanceof Calendar)
            return format( (Calendar)obj, toAppendTo, fieldPosition );
        else if (obj instanceof Date)
            return format( (Date)obj, toAppendTo, fieldPosition );
        else if (obj instanceof Number)
            return format( new Date(((Number)obj).longValue()),
                          toAppendTo, fieldPosition );
        else 
            throw new IllegalArgumentException("Cannot format given Object as a Date");
    }

    /**
     * Formats a date into a date/time string.
     * @param cal a Calendar set to the date and time to be formatted
     * into a date/time string.
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
     * @stable ICU 2.0
     */
    public abstract StringBuffer format(Calendar cal, StringBuffer toAppendTo,
                                        FieldPosition fieldPosition);

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
     * @stable ICU 2.0
     */
    public final StringBuffer format(Date date, StringBuffer toAppendTo,
                                     FieldPosition fieldPosition) {
        // Use our Calendar object
        calendar.setTime(date);
        return format(calendar, toAppendTo, fieldPosition);
    }

    /**
     * Formats a Date into a date/time string.
     * @param date the time value to be formatted into a time string.
     * @return the formatted time string.
     * @stable ICU 2.0
     */
    public final String format(Date date)
    {
        return format(date, new StringBuffer(64),new FieldPosition(0)).toString();
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
     * @stable ICU 2.0
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
     * Parse a date/time string according to the given parse position.
     * For example, a time text "07/10/96 4:5 PM, PDT" will be parsed
     * into a Calendar that is equivalent to Date(837039928046).  The
     * caller should clear the calendar before calling this method,
     * unless existing field information is to be kept.
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
     * @param cal   The calendar into which parsed data will be stored.
     *              In general, this should be cleared before calling this
     *              method.  If this parse fails, the calendar may still
     *              have been modified.
     *
     * @param pos   On input, the position at which to start parsing; on
     *              output, the position at which parsing terminated, or the
     *              start position if the parse failed.
     * @stable ICU 2.0
     */
    public abstract void parse(String text, Calendar cal, ParsePosition pos);

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
     * @stable ICU 2.0
     */
    public final Date parse(String text, ParsePosition pos) {
        int start = pos.getIndex();
        calendar.clear();
        parse(text, calendar, pos);
        if (pos.getIndex() != start) {
            try {
                return calendar.getTime();
            } catch (IllegalArgumentException e) {
                // This occurs if the calendar is non-lenient and there is
                // an out-of-range field.  We don't know which field was
                // illegal so we set the error index to the start.
                pos.setIndex(start);
                pos.setErrorIndex(start);
            }
        }
        return null;
    }

    /**
     * Parse a date/time string into an Object.  This convenience method simply
     * calls parse(String, ParsePosition).
     *
     * @see #parse(String, ParsePosition)
     * @stable ICU 2.0
     */
    public Object parseObject (String source, ParsePosition pos)
    {
        return parse(source, pos);
    }

    /**
     * Constant for full style pattern.
     * @stable ICU 2.0
     */
    public static final int FULL = 0;

    /**
     * Constant for long style pattern.
     * @stable ICU 2.0
     */
    public static final int LONG = 1;

    /**
     * Constant for medium style pattern.
     * @stable ICU 2.0
     */
    public static final int MEDIUM = 2;

    /**
     * Constant for short style pattern.
     * @stable ICU 2.0
     */
    public static final int SHORT = 3;

    /**
     * Constant for default style pattern.  Its value is MEDIUM.
     * @stable ICU 2.0
     */
    public static final int DEFAULT = MEDIUM;

    /**
     * Gets the time formatter with the default formatting style
     * for the default locale.
     * @return a time formatter.
     * @stable ICU 2.0
     */
    public final static DateFormat getTimeInstance()
    {
        return get(-1, DEFAULT, ULocale.getDefault());
    }

    /**
     * Gets the time formatter with the given formatting style
     * for the default locale.
     * @param style the given formatting style. For example,
     * SHORT for "h:mm a" in the US locale.
     * @return a time formatter.
     * @stable ICU 2.0
     */
    public final static DateFormat getTimeInstance(int style)
    {
        return get(-1, style, ULocale.getDefault());
    }

    /**
     * Gets the time formatter with the given formatting style
     * for the given locale.
     * @param style the given formatting style. For example,
     * SHORT for "h:mm a" in the US locale.
     * @param aLocale the given locale.
     * @return a time formatter.
     * @stable ICU 2.0
     */
    public final static DateFormat getTimeInstance(int style,
                                                 Locale aLocale)
    {
        return get(-1, style, ULocale.forLocale(aLocale));
    }

    /**
     * Gets the time formatter with the given formatting style
     * for the given locale.
     * @param style the given formatting style. For example,
     * SHORT for "h:mm a" in the US locale.
     * @param locale the given ulocale.
     * @return a time formatter.
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final static DateFormat getTimeInstance(int style,
                                                 ULocale locale)
    {
        return get(-1, style, locale);
    }

    /**
     * Gets the date formatter with the default formatting style
     * for the default locale.
     * @return a date formatter.
     * @stable ICU 2.0
     */
    public final static DateFormat getDateInstance()
    {
        return get(DEFAULT, -1, ULocale.getDefault());
    }

    /**
     * Gets the date formatter with the given formatting style
     * for the default locale.
     * @param style the given formatting style. For example,
     * SHORT for "M/d/yy" in the US locale.
     * @return a date formatter.
     * @stable ICU 2.0
     */
    public final static DateFormat getDateInstance(int style)
    {
        return get(style, -1, ULocale.getDefault());
    }

    /**
     * Gets the date formatter with the given formatting style
     * for the given locale.
     * @param style the given formatting style. For example,
     * SHORT for "M/d/yy" in the US locale.
     * @param aLocale the given locale.
     * @return a date formatter.
     * @stable ICU 2.0
     */
    public final static DateFormat getDateInstance(int style,
                                                 Locale aLocale)
    {
        return get(style, -1, ULocale.forLocale(aLocale));
    }

    /**
     * Gets the date formatter with the given formatting style
     * for the given locale.
     * @param style the given formatting style. For example,
     * SHORT for "M/d/yy" in the US locale.
     * @param locale the given ulocale.
     * @return a date formatter.
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final static DateFormat getDateInstance(int style,
                                                 ULocale locale)
    {
        return get(style, -1, locale);
    }

    /**
     * Gets the date/time formatter with the default formatting style
     * for the default locale.
     * @return a date/time formatter.
     * @stable ICU 2.0
     */
    public final static DateFormat getDateTimeInstance()
    {
        return get(DEFAULT, DEFAULT, ULocale.getDefault());
    }

    /**
     * Gets the date/time formatter with the given date and time
     * formatting styles for the default locale.
     * @param dateStyle the given date formatting style. For example,
     * SHORT for "M/d/yy" in the US locale.
     * @param timeStyle the given time formatting style. For example,
     * SHORT for "h:mm a" in the US locale.
     * @return a date/time formatter.
     * @stable ICU 2.0
     */
    public final static DateFormat getDateTimeInstance(int dateStyle,
                                                       int timeStyle)
    {
        return get(dateStyle, timeStyle, ULocale.getDefault());
    }

    /**
     * Gets the date/time formatter with the given formatting styles
     * for the given locale.
     * @param dateStyle the given date formatting style.
     * @param timeStyle the given time formatting style.
     * @param aLocale the given locale.
     * @return a date/time formatter.
     * @stable ICU 2.0
     */
    public final static DateFormat
        getDateTimeInstance(int dateStyle, int timeStyle, Locale aLocale)
    {
        return get(dateStyle, timeStyle, ULocale.forLocale(aLocale));
    }

    /**
     * Gets the date/time formatter with the given formatting styles
     * for the given locale.
     * @param dateStyle the given date formatting style.
     * @param timeStyle the given time formatting style.
     * @param locale the given ulocale.
     * @return a date/time formatter.
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final static DateFormat
        getDateTimeInstance(int dateStyle, int timeStyle, ULocale locale)
    {
        return get(dateStyle, timeStyle, locale);
    }

    /**
     * Get a default date/time formatter that uses the SHORT style for both the
     * date and the time.
     * @stable ICU 2.0
     */
    public final static DateFormat getInstance() {
        return getDateTimeInstance(SHORT, SHORT);
    }

    /**
     * Gets the set of locales for which DateFormats are installed.
     * @return the set of locales for which DateFormats are installed.
     * @stable ICU 2.0
     */
    public static Locale[] getAvailableLocales()
    {
        return ICUResourceBundle.getAvailableLocales(ICUResourceBundle.ICU_BASE_NAME);
    }

    /**
     * Gets the set of locales for which DateFormats are installed.
     * @return the set of locales for which DateFormats are installed.
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static ULocale[] getAvailableULocales()
    {
        return ICUResourceBundle.getAvailableULocales(ICUResourceBundle.ICU_BASE_NAME);
    }

    /**
     * Set the calendar to be used by this date format.  Initially, the default
     * calendar for the specified or default locale is used.
     * @param newCalendar the new Calendar to be used by the date format
     * @stable ICU 2.0
     */
    public void setCalendar(Calendar newCalendar)
    {
        this.calendar = newCalendar;
    }

    /**
     * Gets the calendar associated with this date/time formatter.
     * @return the calendar associated with this date/time formatter.
     * @stable ICU 2.0
     */
    public Calendar getCalendar()
    {
        return calendar;
    }

    /**
     * Allows you to set the number formatter.
     * @param newNumberFormat the given new NumberFormat.
     * @stable ICU 2.0
     */
    public void setNumberFormat(NumberFormat newNumberFormat)
    {
        this.numberFormat = newNumberFormat;
        /*In order to parse String like "11.10.2001" to DateTime correctly 
          in Locale("fr","CH") [Richard/GCL]
        */
        this.numberFormat.setParseIntegerOnly(true);
    }

    /**
     * Gets the number formatter which this date/time formatter uses to
     * format and parse a time.
     * @return the number formatter which this date/time formatter uses.
     * @stable ICU 2.0
     */
    public NumberFormat getNumberFormat()
    {
        return numberFormat;
    }

    /**
     * Sets the time zone for the calendar of this DateFormat object.
     * @param zone the given new time zone.
     * @stable ICU 2.0
     */
    public void setTimeZone(TimeZone zone)
    {
        calendar.setTimeZone(zone);
    }

    /**
     * Gets the time zone.
     * @return the time zone associated with the calendar of DateFormat.
     * @stable ICU 2.0
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
     * @see com.ibm.icu.util.Calendar#setLenient
     * @stable ICU 2.0
     */
    public void setLenient(boolean lenient)
    {
        calendar.setLenient(lenient);
    }

    /**
     * Tell whether date/time parsing is to be lenient.
     * @stable ICU 2.0
     */
    public boolean isLenient()
    {
        return calendar.isLenient();
    }

    /**
     * Overrides hashCode
     * @stable ICU 2.0
     */
    ///CLOVER:OFF
    // turn off code coverage since all subclasses override this
    public int hashCode() {
        return numberFormat.hashCode();
        // just enough fields for a reasonable distribution
    }
    ///CLOVER:ON

    /**
     * Overrides equals
     * @stable ICU 2.0
     */
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DateFormat other = (DateFormat) obj;
        return (calendar.isEquivalentTo(other.calendar) &&
                numberFormat.equals(other.numberFormat));
    }

    /**
     * Overrides Cloneable
     * @stable ICU 2.0
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
    private static DateFormat get(int dateStyle, int timeStyle, ULocale loc) {
        if (timeStyle < -1 || timeStyle > 3) {
            throw new IllegalArgumentException("Illegal time style " + timeStyle);
        }
        if (dateStyle < -1 || dateStyle > 3) {
            throw new IllegalArgumentException("Illegal date style " + dateStyle);
        }
        try {
            return new SimpleDateFormat(timeStyle, dateStyle, loc);
        } catch (MissingResourceException e) {
            ///CLOVER:OFF
            // coverage requires separate run with no data, so skip
            return new SimpleDateFormat("M/d/yy h:mm a");
            ///CLOVER:ON
        }
    }

    /**
     * Create a new date format.
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    static final public DateFormat getDateInstance(Calendar cal, int dateStyle, Locale locale)
    {
        return getDateTimeInstance(cal, dateStyle, -1, ULocale.forLocale(locale));
    }
    
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
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    static final public DateFormat getDateInstance(Calendar cal, int dateStyle, ULocale locale)
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
     * @stable ICU 2.0
     */
    static final public DateFormat getTimeInstance(Calendar cal, int timeStyle, Locale locale)
    {
        return getDateTimeInstance(cal, -1, timeStyle, ULocale.forLocale(locale));
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
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    static final public DateFormat getTimeInstance(Calendar cal, int timeStyle, ULocale locale)
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
     * @stable ICU 2.0
     */
    static final public DateFormat getDateTimeInstance(Calendar cal, int dateStyle,
                                                 int timeStyle, Locale locale)
    {
        return cal.getDateTimeFormat(dateStyle, timeStyle, ULocale.forLocale(locale));
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
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    static final public DateFormat getDateTimeInstance(Calendar cal, int dateStyle,
                                                 int timeStyle, ULocale locale)
    {
        return cal.getDateTimeFormat(dateStyle, timeStyle, locale);
    }

    /**
     * Convenience overload
     * @stable ICU 2.0
     */
    static final public DateFormat getInstance(Calendar cal, Locale locale) {
        return getDateTimeInstance(cal, SHORT, SHORT, ULocale.forLocale(locale));
    }

    /**
     * Convenience overload
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    static final public DateFormat getInstance(Calendar cal, ULocale locale) {
        return getDateTimeInstance(cal, SHORT, SHORT, locale);
    }

    /**
     * Convenience overload
     * @stable ICU 2.0
     */
    static final public DateFormat getInstance(Calendar cal) {
        return getInstance(cal, ULocale.getDefault());
    }

    /**
     * Convenience overload
     * @stable ICU 2.0
     */
    static final public DateFormat getDateInstance(Calendar cal, int dateStyle) {
        return getDateInstance(cal, dateStyle, ULocale.getDefault());
    }

    /**
     * Convenience overload
     * @stable ICU 2.0
     */
    static final public DateFormat getTimeInstance(Calendar cal, int timeStyle) {
        return getTimeInstance(cal, timeStyle, ULocale.getDefault());
    }

    /**
     * Convenience overload
     * @stable ICU 2.0
     */
    static final public DateFormat getDateTimeInstance(Calendar cal, int dateStyle, int timeStyle) {
        return getDateTimeInstance(cal, dateStyle, timeStyle, ULocale.getDefault());
    }
}

/*
*   Copyright (C) 1996-2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*/

package com.ibm.icu.text;

import java.io.InvalidObjectException;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.RelativeDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * DateFormat is an abstract class for date/time formatting subclasses which
 * formats and parses dates or time in a language-independent manner.
 * The date/time formatting subclass, such as SimpleDateFormat, allows for
 * formatting (i.e., date -> text), parsing (text -> date), and
 * normalization.  The date is represented as a <code>Date</code> object or
 * as the milliseconds since January 1, 1970, 00:00:00 GMT.
 *
 * <p>DateFormat provides many class methods for obtaining default date/time
 * formatters based on the default or a given locale and a number of formatting
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
     * @stable ICU 3.0
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
     * @stable ICU 3.0
     */
    public final static int YEAR_WOY_FIELD = 18;

    /**
     * FieldPosition selector for 'e' field alignment,
     * corresponding to the {@link Calendar#DOW_LOCAL} field.
     * @stable ICU 3.0
     */
    public final static int DOW_LOCAL_FIELD = 19;

    /**
     * FieldPosition selector for 'u' field alignment,
     * corresponding to the {@link Calendar#EXTENDED_YEAR} field.
     * @stable ICU 3.0
     */
    public final static int EXTENDED_YEAR_FIELD = 20;

    /**
     * FieldPosition selector for 'g' field alignment,
     * corresponding to the {@link Calendar#JULIAN_DAY} field.
     * @stable ICU 3.0
     */
    public final static int JULIAN_DAY_FIELD = 21;

    /**
     * FieldPosition selector for 'A' field alignment,
     * corresponding to the {@link Calendar#MILLISECONDS_IN_DAY} field.
     * @stable ICU 3.0
     */
    public final static int MILLISECONDS_IN_DAY_FIELD = 22;

    /**
     * FieldPosition selector for 'Z' field alignment,
     * corresponding to the {@link Calendar#ZONE_OFFSET} and
     * {@link Calendar#DST_OFFSET} fields.
     * @stable ICU 3.0
     */
    public final static int TIMEZONE_RFC_FIELD = 23;

    /**
     * FieldPosition selector for 'v' field alignment,
     * corresponding to the {@link Calendar#ZONE_OFFSET} and
     * {@link Calendar#DST_OFFSET} fields.  This displays the generic zone
     * name, if available.
     * @stable ICU 3.4
     */
    public final static int TIMEZONE_GENERIC_FIELD = 24;
 

    
    /**
     * FieldPosition selector for 'c' field alignment,
     * corresponding to the {@link Calendar#DAY_OF_WEEK} field. 
     * This displays the stand alone day name, if available.
     * @stable ICU 3.4
     */
    public final static int STANDALONE_DAY_FIELD = 25;
    
    /**
     * FieldPosition selector for 'L' field alignment,
     * corresponding to the {@link Calendar#MONTH} field.  
     * This displays the stand alone month name, if available.
     * @stable ICU 3.4
     */
    public final static int STANDALONE_MONTH_FIELD = 26;
    
    /**
     * FieldPosition selector for 'Q' field alignment,
     * corresponding to the {@link Calendar#MONTH} field.  
     * This displays the quarter.
     * @stable ICU 3.6
     */
    public final static int QUARTER_FIELD = 27;
    
    /**
     * FieldPosition selector for 'q' field alignment,
     * corresponding to the {@link Calendar#MONTH} field.  
     * This displays the stand alone quarter, if available.
     * @stable ICU 3.6
     */
    public final static int STANDALONE_QUARTER_FIELD = 28;
    
    /**
     * FieldPosition selector for 'V' field alignment,
     * corresponding to the {@link Calendar#ZONE_OFFSET} and
     * {@link Calendar#DST_OFFSET} fields.  This displays the fallback timezone
     * name when VVVV is specified, and the short standard or daylight
     * timezone name ignoring commonlyUsed when a single V is specified.
     * @stable ICU 3.8
     */
    public final static int TIMEZONE_SPECIAL_FIELD = 29;

    /**
     * Number of FieldPosition selectors for DateFormat.
     * Valid selectors range from 0 to FIELD_COUNT-1.
     * @stable ICU 3.0
     */
    public final static int FIELD_COUNT = 30; // must == DateFormatSymbols.patternChars.length()

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
     * occurrence of that time field. For instance, formatting a Date to
     * the time string "1 PM PDT (Pacific Daylight Time)" using the pattern
     * "h a z (zzzz)" and the alignment field DateFormat.TIMEZONE_FIELD,
     * the begin index and end index of fieldPosition will be set to
     * 5 and 8, respectively, for the first occurrence of the timezone
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
            throw new IllegalArgumentException("Cannot format given Object (" + obj.getClass().getName() + ") as a Date");
    }

    /**
     * Formats a date into a date/time string.
     * @param cal a Calendar set to the date and time to be formatted
     * into a date/time string.  When the calendar type is different from
     * the internal calendar held by this DateFormat instance, the date
     * and the time zone will be inherited from the input calendar, but
     * other calendar field values will be calculated by the internal calendar.
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
     * occurrence of that time field. For instance, formatting a Date to
     * the time string "1 PM PDT (Pacific Daylight Time)" using the pattern
     * "h a z (zzzz)" and the alignment field DateFormat.TIMEZONE_FIELD,
     * the begin index and end index of fieldPosition will be set to
     * 5 and 8, respectively, for the first occurrence of the timezone
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
     * occurrence of that time field. For instance, formatting a Date to
     * the time string "1 PM PDT (Pacific Daylight Time)" using the pattern
     * "h a z (zzzz)" and the alignment field DateFormat.TIMEZONE_FIELD,
     * the begin index and end index of fieldPosition will be set to
     * 5 and 8, respectively, for the first occurrence of the timezone
     * pattern character 'z'.
     * @return the formatted date/time string.
     * @stable ICU 2.0
     */
    public StringBuffer format(Date date, StringBuffer toAppendTo,
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
     *              have been modified.  When the calendar type is different
     *              from the internal calendar held by this DateFormat
     *              instance, calendar field values will be parsed based
     *              on the internal calendar initialized with the time and
     *              the time zone taken from this calendar, then the
     *              parse result (time in milliseconds and time zone) will
     *              be set back to this calendar.
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
    public Date parse(String text, ParsePosition pos) {
        Date result = null;
        int start = pos.getIndex();
        TimeZone tzsav = calendar.getTimeZone();
        calendar.clear();
        parse(text, calendar, pos);
        if (pos.getIndex() != start) {
            try {
                result = calendar.getTime();
            } catch (IllegalArgumentException e) {
                // This occurs if the calendar is non-lenient and there is
                // an out-of-range field.  We don't know which field was
                // illegal so we set the error index to the start.
                pos.setIndex(start);
                pos.setErrorIndex(start);
            }
        }
        // Restore TimeZone
        calendar.setTimeZone(tzsav);
        return result;
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
     * Constant for empty style pattern.
     * @stable ICU 3.8
     */
    public static final int NONE = -1;
    
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
     * Constant for relative style mask.
     * @stable ICU 3.8
     */
    public static final int RELATIVE = (1 << 7);

    /**
     * Constant for relative full style pattern.
     * @stable ICU 3.8
     */
    public static final int RELATIVE_FULL = RELATIVE | FULL;

    /**
     * Constant for relative style pattern.
     * @stable ICU 3.8
     */
    public static final int RELATIVE_LONG = RELATIVE | LONG;

    /**
     * Constant for relative style pattern.
     * @stable ICU 3.8
     */
    public static final int RELATIVE_MEDIUM = RELATIVE | MEDIUM;

    /**
     * Constant for relative style pattern.
     * @stable ICU 3.8
     */
    public static final int RELATIVE_SHORT = RELATIVE | SHORT;

    /**
     * Constant for relative default style pattern.
     * @stable ICU 3.8
     */
    public static final int RELATIVE_DEFAULT = RELATIVE | DEFAULT;

    /* Below are pre-defined skeletons
     *
     * <P>
     * A skeleton 
     * <ul>
     * <li>
     * 1. only keeps the field pattern letter and ignores all other parts 
     *    in a pattern, such as space, punctuations, and string literals.
     * <li>
     * 2. hides the order of fields. 
     * <li>
     * 3. might hide a field's pattern letter length.
     *
     *    For those non-digit calendar fields, the pattern letter length is 
     *    important, such as MMM, MMMM, and MMMMM; E and EEEE, 
     *    and the field's pattern letter length is honored.
     *    
     *    For the digit calendar fields,  such as M or MM, d or dd, yy or yyyy, 
     *    the field pattern length is ignored and the best match, which is 
     *    defined in date time patterns, will be returned without honor 
     *    the field pattern letter length in skeleton.
     * </ul>
     */
    /** 
     * Constant for date pattern with minute and second.
     * @stable ICU 4.0
     */
    public static final String MINUTE_SECOND = "ms";

    /** 
     * Constant for date pattern with hour and minute in 24-hour presentation.
     * @stable ICU 4.0
     */
    public static final String HOUR24_MINUTE = "Hm";

    /** 
     * Constant for date pattern with hour, minute, and second in
     * 24-hour presentation.
     * @stable ICU 4.0
     */
    public static final String HOUR24_MINUTE_SECOND = "Hms";      

    /** 
     * Constant for date pattern with hour, minute, and second.
     * @stable ICU 4.0
     */
    public static final String HOUR_MINUTE_SECOND = "hms";

    /** 
     * Constant for date pattern with standalone month.
     * @stable ICU 4.0
     */
    public static final String STANDALONE_MONTH = "LLLL";

    /** 
     * Constant for date pattern with standalone abbreviated month.
     * @stable ICU 4.0
     */
    public static final String ABBR_STANDALONE_MONTH = "LLL";

    /** 
     * Constant for date pattern with year and quarter.
     * @stable ICU 4.0
     */
    public static final String YEAR_QUARTER = "yQQQ";
    
    /** 
     * Constant for date pattern with year and abbreviated quarter.
     * @stable ICU 4.0
     */
    public static final String YEAR_ABBR_QUARTER = "yQ";

    
    /* Below are skeletons that date interval pre-defined in resource file.
     * Users are encouraged to use them in date interval format factory methods.
     */
    /** 
     * Constant for date pattern with hour and minute.
     * @stable ICU 4.0
     */
    public static final String HOUR_MINUTE = "hm";

    /** 
     * Constant for date pattern with year.
     * @stable ICU 4.0
     */
    public static final String YEAR = "y";

    /** 
     * Constant for date pattern with day.
     * @stable ICU 4.0
     */
    public static final String DAY = "d";

    /** 
     * Constant for date pattern with numeric month, weekday, and day.
     * @stable ICU 4.0
     */
    public static final String NUM_MONTH_WEEKDAY_DAY = "MEd";

    /** 
     * Constant for date pattern with year and numeric month.
     * @stable ICU 4.0
     */
    public static final String YEAR_NUM_MONTH = "yM";              

    /** 
     * Constant for date pattern with numeric month and day.
     * @stable ICU 4.0
     */
    public static final String NUM_MONTH_DAY = "Md";

    /** 
     * Constant for date pattern with year, numeric month, weekday, and day.
     * @stable ICU 4.0
     */
    public static final String YEAR_NUM_MONTH_WEEKDAY_DAY = "yMEd";

    /** 
     * Constant for date pattern with abbreviated month, weekday, and day.
     * @stable ICU 4.0
     */
    public static final String ABBR_MONTH_WEEKDAY_DAY = "MMMEd";

    /** 
     * Constant for date pattern with year and month.
     * @stable ICU 4.0
     */
    public static final String YEAR_MONTH = "yMMMM";

    /** 
     * Constant for date pattern with year and abbreviated month.
     * @stable ICU 4.0
     */
    public static final String YEAR_ABBR_MONTH = "yMMM";

    /** 
     * Constant for date pattern having month and day.
     * @stable ICU 4.0
     */
    public static final String MONTH_DAY = "MMMMd";

    /** 
     * Constant for date pattern with abbreviated month and day.
     * @stable ICU 4.0
     */
    public static final String ABBR_MONTH_DAY = "MMMd"; 

    /** 
     * Constant for date pattern with month, weekday, and day.
     * @stable ICU 4.0
     */
    public static final String MONTH_WEEKDAY_DAY = "MMMMEEEEd";

    /** 
     * Constant for date pattern with year, abbreviated month, weekday, 
     * and day.
     * @stable ICU 4.0
     */
    public static final String YEAR_ABBR_MONTH_WEEKDAY_DAY = "yMMMEd"; 

    /** 
     * Constant for date pattern with year, month, weekday, and day.
     * @stable ICU 4.0
     */
    public static final String YEAR_MONTH_WEEKDAY_DAY = "yMMMMEEEEd";

    /** 
     * Constant for date pattern with year, month, and day.
     * @stable ICU 4.0
     */
    public static final String YEAR_MONTH_DAY = "yMMMMd";

    /** 
     * Constant for date pattern with year, abbreviated month, and day.
     * @stable ICU 4.0
     */
    public static final String YEAR_ABBR_MONTH_DAY = "yMMMd";

    /** 
     * Constant for date pattern with year, numeric month, and day.
     * @stable ICU 4.0
     */
    public static final String YEAR_NUM_MONTH_DAY = "yMd";

    /** 
     * Constant for date pattern with numeric month.
     * @stable ICU 4.0
     */
    public static final String NUM_MONTH = "M";

    /** 
     * Constant for date pattern with abbreviated month.
     * @stable ICU 4.0
     */
    public static final String ABBR_MONTH = "MMM";

    /** 
     * Constant for date pattern with month.
     * @stable ICU 4.0
     */
    public static final String MONTH = "MMMM";

    /** 
     * Constant for date pattern with hour, minute, and generic timezone.
     * @stable ICU 4.0
     */
    public static final String HOUR_MINUTE_GENERIC_TZ = "hmv";

    /** 
     * Constant for date pattern with hour, minute, and timezone.
     * @stable ICU 4.0
     */
    public static final String HOUR_MINUTE_TZ = "hmz";

    /** 
     * Constant for date pattern with hour.
     * @stable ICU 4.0
     */
    public static final String HOUR = "h";

    /** 
     * Constant for date pattern with hour and generic timezone.
     * @stable ICU 4.0
     */
    public static final String HOUR_GENERIC_TZ = "hv";

    /** 
     * Constant for date pattern with hour and timezone.
     * @stable ICU 4.0
     */
    public static final String HOUR_TZ = "hz";

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
     * @stable ICU 3.2
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
     * @stable ICU 3.2
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
     * @stable ICU 3.2
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
     * @draft ICU 3.2 (retain)
     * @provisional This API might change or be removed in a future release.
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
        if((timeStyle != -1 && (timeStyle & RELATIVE)>0) || (dateStyle != -1 && (dateStyle & RELATIVE)>0)) {
            RelativeDateFormat r = new RelativeDateFormat(timeStyle, dateStyle /* offset? */, loc);
            return r;
        }
    
        if (timeStyle < -1 || timeStyle > 3) {
            throw new IllegalArgumentException("Illegal time style " + timeStyle);
        }
        if (dateStyle < -1 || dateStyle > 3) {
            throw new IllegalArgumentException("Illegal date style " + dateStyle);
        }
        try {
            Calendar cal = Calendar.getInstance(loc);
            DateFormat result = cal.getDateTimeFormat(dateStyle, timeStyle, loc);
            result.setLocale(cal.getLocale(ULocale.VALID_LOCALE),
                 cal.getLocale(ULocale.ACTUAL_LOCALE));
            return result;
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
     * @stable ICU 3.2
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
     * @stable ICU 3.2
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
     * @stable ICU 3.2
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
     * @stable ICU 3.2
     * @provisional This API might change or be removed in a future release.
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

    /**
     * Convenience overload
     * @stable ICU 4.0
     */
    public final static DateFormat getPatternInstance(String pattern) {
        return getPatternInstance(pattern, ULocale.getDefault());
    }

    /**
     * Convenience overload
     * @stable ICU 4.0
     */
    public final static DateFormat getPatternInstance(String pattern, Locale locale) {
        return getPatternInstance(pattern, ULocale.forLocale(locale));
    }

    /**
     * Create a {@link DateFormat} object that can be used to format dates and times in
     * the given locale.
     * <p>
     * <b>Note:</b> When this functionality is moved into the core JDK, this method
     * will probably be replaced by a new overload of {@link DateFormat#getInstance}.
     * <p>
     *
     * @param pattern The pattern that selects the fields to be formatted. (Uses the 
     *              {@link DateTimePatternGenerator}.) This can be {@link DateFormat#ABBR_MONTH}, 
     *              {@link DateFormat#MONTH_WEEKDAY_DAY}, etc.
     *
     * @param locale The locale for which the date/time format is desired.
     *
     * @stable ICU 4.0
     */
    public final static DateFormat getPatternInstance(String pattern, ULocale locale) {
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);
        final String bestPattern = generator.getBestPattern(pattern);
        return new SimpleDateFormat(bestPattern, locale);
    }

    /**
     * Convenience overload
     * @stable ICU 4.0
     */
    public final static DateFormat getPatternInstance(Calendar cal, String pattern, Locale locale) {
        return getPatternInstance(cal, pattern, ULocale.forLocale(locale));
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
     * @param pattern The pattern that selects the fields to be formatted. (Uses the 
     *              {@link DateTimePatternGenerator}.)  This can be
     *              {@link DateFormat#ABBR_MONTH}, {@link DateFormat#MONTH_WEEKDAY_DAY},
     *              etc.
     *
     * @param locale The locale for which the date/time format is desired.
     *
     * @stable ICU 4.0
     */
    public final static DateFormat getPatternInstance(Calendar cal, String pattern, ULocale locale) {
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);
        final String bestPattern = generator.getBestPattern(pattern);
        SimpleDateFormat format = new SimpleDateFormat(bestPattern, locale);
        format.setCalendar(cal);
        return format;
    }

    /**
     * The instances of this inner class are used as attribute keys and values
     * in AttributedCharacterIterator that
     * DateFormat.formatToCharacterIterator() method returns.
     * <p>
     * There is no public constructor to this class, the only instances are the
     * constants defined here.
     * <p>
     * @stable ICU 3.8
     */
    public static class Field extends Format.Field {

        private static final long serialVersionUID = -3627456821000730829L;

        // Max number of calendar fields
        private static final int CAL_FIELD_COUNT;

        // Table for mapping calendar field number to DateFormat.Field
        private static final Field[] CAL_FIELDS;
 
        // Map for resolving DateFormat.Field by name
        private static final Map FIELD_NAME_MAP;

        static {
            GregorianCalendar cal = new GregorianCalendar();
            CAL_FIELD_COUNT = cal.getFieldCount();
            CAL_FIELDS = new Field[CAL_FIELD_COUNT];
            FIELD_NAME_MAP = new HashMap(CAL_FIELD_COUNT);
        }

        // Java fields -------------------

        /**
         * Constant identifying the time of day indicator(am/pm).
         * @stable ICU 3.8
         */
        public static final Field AM_PM = new Field("am pm", Calendar.AM_PM);

        /**
         * Constant identifying the day of month field.
         * @stable ICU 3.8
         */
        public static final Field DAY_OF_MONTH = new Field("day of month", Calendar.DAY_OF_MONTH);

        /**
         * Constant identifying the day of week field.
         * @stable ICU 3.8
         */
        public static final Field DAY_OF_WEEK = new Field("day of week", Calendar.DAY_OF_WEEK);

        /**
         * Constant identifying the day of week in month field.
         * @stable ICU 3.8
         */
        public static final Field DAY_OF_WEEK_IN_MONTH = new Field("day of week in month", Calendar.DAY_OF_WEEK_IN_MONTH);

        /**
         * Constant identifying the day of year field.
         * @stable ICU 3.8
         */
        public static final Field DAY_OF_YEAR = new Field("day of year", Calendar.DAY_OF_YEAR);

        /**
         * Constant identifying the era field.
         * @stable ICU 3.8
         */
        public static final Field ERA = new Field("era", Calendar.ERA);

        /**
         * Constant identifying the hour(0-23) of day field.
         * @stable ICU 3.8
         */
        public static final Field HOUR_OF_DAY0 = new Field("hour of day", Calendar.HOUR_OF_DAY);

        /**
         * Constant identifying the hour(1-24) of day field.
         * @stable ICU 3.8
         */
        public static final Field HOUR_OF_DAY1 = new Field("hour of day 1", -1);

        /**
         * Constant identifying the hour(0-11) field.
         * @stable ICU 3.8
         */
        public static final Field HOUR0 = new Field("hour", Calendar.HOUR);

        /**
         * Constant identifying the hour(1-12) field.
         * @stable ICU 3.8
         */
        public static final Field HOUR1 = new Field("hour 1", -1);

        /**
         * Constant identifying the millisecond field.
         * @stable ICU 3.8
         */
        public static final Field MILLISECOND = new Field("millisecond", Calendar.MILLISECOND);

        /**
         * Constant identifying the minute field.
         * @stable ICU 3.8
         */
        public static final Field MINUTE = new Field("minute", Calendar.MINUTE);

        /**
         * Constant identifying the month field.
         * @stable ICU 3.8
         */
        public static final Field MONTH = new Field("month", Calendar.MONTH);

        /**
         * Constant identifying the second field.
         * @stable ICU 3.8
         */
        public static final Field SECOND = new Field("second", Calendar.SECOND);

        /**
         * Constant identifying the time zone field.
         * @stable ICU 3.8
         */
        public static final Field TIME_ZONE = new Field("time zone", -1);

        /**
         * Constant identifying the week of month field.
         * @stable ICU 3.8
         */
        public static final Field WEEK_OF_MONTH = new Field("week of month", Calendar.WEEK_OF_MONTH);

        /**
         * Constant identifying the week of year field.
         * @stable ICU 3.8
         */
        public static final Field WEEK_OF_YEAR = new Field("week of year", Calendar.WEEK_OF_YEAR);

        /**
         * Constant identifying the year field.
         * @stable ICU 3.8
         */
        public static final Field YEAR = new Field("year", Calendar.YEAR);


        // ICU only fields -------------------

        /**
         * Constant identifying the local day of week field.
         * @stable ICU 3.8
         */
        public static final Field DOW_LOCAL = new Field("local day of week", Calendar.DOW_LOCAL);

        /**
         * Constant identifying the extended year field.
         * @stable ICU 3.8
         */
        public static final Field EXTENDED_YEAR = new Field("extended year", Calendar.EXTENDED_YEAR);

        /**
         * Constant identifying the Julian day field.
         * @stable ICU 3.8
         */
        public static final Field JULIAN_DAY = new Field("Julian day", Calendar.JULIAN_DAY);

        /**
         * Constant identifying the milliseconds in day field.
         * @stable ICU 3.8
         */
        public static final Field MILLISECONDS_IN_DAY = new Field("milliseconds in day", Calendar.MILLISECONDS_IN_DAY);

        /**
         * Constant identifying the year used with week of year field.
         * @stable ICU 3.8
         */
        public static final Field YEAR_WOY = new Field("year for week of year", Calendar.YEAR_WOY);

        /**
         * Constant identifying the quarter field.
         * @stable ICU 3.8
         */
        public static final Field QUARTER = new Field("quarter", -1);

        // Stand alone types are variants for its base types.  So we do not define Field for
        // them.
        /*
        public static final Field STANDALONE_DAY = new Field("stand alone day of week", Calendar.DAY_OF_WEEK);
        public static final Field STANDALONE_MONTH = new Field("stand alone month", Calendar.MONTH);
        public static final Field STANDALONE_QUARTER = new Field("stand alone quarter", -1);
        */

        // Corresponding calendar field
        private final int calendarField;

        /**
         * Constructs a <code>DateFormat.Field</code> with the given name and
         * the <code>Calendar</code> field which this attribute represents.  Use -1 for
         * <code>calendarField</code> if this field does not have a corresponding
         * <code>Calendar</code> field.
         * 
         * @param name          Name of the attribute
         * @param calendarField <code>Calendar</code> field constant
         * 
         * @stable ICU 3.8
         */
        protected Field(String name, int calendarField) {
            super(name);
            this.calendarField = calendarField;
            if (this.getClass() == DateFormat.Field.class) {
                FIELD_NAME_MAP.put(name, this);
                if (calendarField >= 0 && calendarField < CAL_FIELD_COUNT) {
                    CAL_FIELDS[calendarField] = this;
                }
            }
        }

        /**
         * Returns the <code>Field</code> constant that corresponds to the <code>
         * Calendar</code> field <code>calendarField</code>.  If there is no
         * corresponding <code>Field</code> is available, null is returned.
         * 
         * @param calendarField <code>Calendar</code> field constant
         * @return <code>Field</code> associated with the <code>calendarField</code>,
         * or null if no associated <code>Field</code> is available.
         * @throws IllegalArgumentException if <code>calendarField</code> is not
         * a valid <code>Calendar</code> field constant.
         * 
         * @stable ICU 3.8
         */
        public static DateFormat.Field ofCalendarField(int calendarField) {
            if (calendarField < 0 || calendarField >= CAL_FIELD_COUNT) {
                throw new IllegalArgumentException("Calendar field number is out of range");
            }
            return CAL_FIELDS[calendarField];
        }
        
        /**
         * Returns the <code>Calendar</code> field associated with this attribute.
         * If there is no corresponding <code>Calendar</code> available, this will
         * return -1.
         * 
         * @return <code>Calendar</code> constant for this attribute.
         * 
         * @stable ICU 3.8
         */
        public int getCalendarField() {
            return calendarField;
        }
        
        /**
         * Resolves instances being deserialized to the predefined constants.
         * 
         * @throws InvalidObjectException if the constant could not be resolved.
         * 
         * @stable ICU 3.8
         */
        protected Object readResolve() throws InvalidObjectException {
            ///CLOVER:OFF
            if (this.getClass() != DateFormat.Field.class) {
                throw new InvalidObjectException("A subclass of DateFormat.Field must implement readResolve.");
            }
            ///CLOVER:ON
            Object o = FIELD_NAME_MAP.get(this.getName());
            ///CLOVER:OFF
            if (o == null) {
                throw new InvalidObjectException("Unknown attribute name.");
            }
            ///CLOVER:ON
            return o;
        }
    }
}

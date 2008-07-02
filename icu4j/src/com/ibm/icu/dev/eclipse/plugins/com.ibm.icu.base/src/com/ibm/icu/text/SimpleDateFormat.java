/*
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;

/**
 * <code>SimpleDateFormat</code> is a concrete class for formatting and
 * parsing dates in a locale-sensitive manner. It allows for formatting
 * (date -> text), parsing (text -> date), and normalization.
 *
 * <p>
 * <code>SimpleDateFormat</code> allows you to start by choosing
 * any user-defined patterns for date-time formatting. However, you
 * are encouraged to create a date-time formatter with either
 * <code>getTimeInstance</code>, <code>getDateInstance</code>, or
 * <code>getDateTimeInstance</code> in <code>DateFormat</code>. Each
 * of these class methods can return a date/time formatter initialized
 * with a default format pattern. You may modify the format pattern
 * using the <code>applyPattern</code> methods as desired.
 * For more information on using these methods, see
 * {@link DateFormat}.
 *
 * <p>
 * <strong>Time Format Syntax:</strong>
 * <p>
 * To specify the time format use a <em>time pattern</em> string.
 * In this pattern, all ASCII letters are reserved as pattern letters,
 * which are defined as the following:
 * <blockquote>
 * <pre>
 * Symbol   Meaning                 Presentation        Example
 * ------   -------                 ------------        -------
 * G        era designator          (Text)              AD
 * y&#x2020;       year                    (Number)            1996
 * Y*       year (week of year)     (Number)            1997
 * u*       extended year           (Number)            4601
 * M        month in year           (Text & Number)     July & 07
 * d        day in month            (Number)            10
 * h        hour in am/pm (1~12)    (Number)            12
 * H        hour in day (0~23)      (Number)            0
 * m        minute in hour          (Number)            30
 * s        second in minute        (Number)            55
 * S        fractional second       (Number)            978
 * E        day of week             (Text)              Tuesday
 * e*       day of week (local 1~7) (Number)            2
 * D        day in year             (Number)            189
 * F        day of week in month    (Number)            2 (2nd Wed in July)
 * w        week in year            (Number)            27
 * W        week in month           (Number)            2
 * a        am/pm marker            (Text)              PM
 * k        hour in day (1~24)      (Number)            24
 * K        hour in am/pm (0~11)    (Number)            0
 * z        time zone               (Text)              Pacific Standard Time
 * Z        time zone (RFC 822)     (Number)            -0800
 * v        time zone (generic)     (Text)              Pacific Time
 * g*       Julian day              (Number)            2451334
 * A*       milliseconds in day     (Number)            69540000
 * '        escape for text         (Delimiter)         'Date='
 * ''       single quote            (Literal)           'o''clock'
 * </pre>
 * </blockquote>
 * <tt><b>*</b></tt> These items are not supported by Java's SimpleDateFormat.<br>
 * <tt><b>&#x2020;</b></tt> ICU interprets a single 'y' differently than Java.</p>
 * <p>
 * The count of pattern letters determine the format.
 * <p>
 * <strong>(Text)</strong>: 4 or more pattern letters--use full form,
 * &lt; 4--use short or abbreviated form if one exists.
 * <p>
 * <strong>(Number)</strong>: the minimum number of digits. Shorter
 * numbers are zero-padded to this amount. Year is handled specially;
 * that is, if the count of 'y' is 2, the Year will be truncated to 2 digits.
 * (e.g., if "yyyy" produces "1997", "yy" produces "97".)
 * Unlike other fields, fractional seconds are padded on the right with zero.
 * <p>
 * <strong>(Text & Number)</strong>: 3 or over, use text, otherwise use number.
 * <p>
 * Any characters in the pattern that are not in the ranges of ['a'..'z']
 * and ['A'..'Z'] will be treated as quoted text. For instance, characters
 * like ':', '.', ' ', '#' and '@' will appear in the resulting time text
 * even they are not embraced within single quotes.
 * <p>
 * A pattern containing any invalid pattern letter will result in a thrown
 * exception during formatting or parsing.
 *
 * <p>
 * <strong>Examples Using the US Locale:</strong>
 * <blockquote>
 * <pre>
 * Format Pattern                         Result
 * --------------                         -------
 * "yyyy.MM.dd G 'at' HH:mm:ss vvvv" ->>  1996.07.10 AD at 15:08:56 Pacific Time
 * "EEE, MMM d, ''yy"                ->>  Wed, July 10, '96
 * "h:mm a"                          ->>  12:08 PM
 * "hh 'o''clock' a, zzzz"           ->>  12 o'clock PM, Pacific Daylight Time
 * "K:mm a, vvv"                     ->>  0:00 PM, PT
 * "yyyyy.MMMMM.dd GGG hh:mm aaa"    ->>  01996.July.10 AD 12:08 PM
 * </pre>
 * </blockquote>
 * <strong>Code Sample:</strong>
 * <blockquote>
 * <pre>
 * SimpleTimeZone pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, "PST");
 * pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2*60*60*1000);
 * pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*60*60*1000);
 * <br>
 * // Format the current time.
 * SimpleDateFormat formatter
 *     = new SimpleDateFormat ("yyyy.MM.dd G 'at' hh:mm:ss a zzz");
 * Date currentTime_1 = new Date();
 * String dateString = formatter.format(currentTime_1);
 * <br>
 * // Parse the previous string back into a Date.
 * ParsePosition pos = new ParsePosition(0);
 * Date currentTime_2 = formatter.parse(dateString, pos);
 * </pre>
 * </blockquote>
 * In the example, the time value <code>currentTime_2</code> obtained from
 * parsing will be equal to <code>currentTime_1</code>. However, they may not be
 * equal if the am/pm marker 'a' is left out from the format pattern while
 * the "hour in am/pm" pattern symbol is used. This information loss can
 * happen when formatting the time in PM.
 *
 * <p>
 * When parsing a date string using the abbreviated year pattern ("yy"),
 * SimpleDateFormat must interpret the abbreviated year
 * relative to some century.  It does this by adjusting dates to be
 * within 80 years before and 20 years after the time the SimpleDateFormat
 * instance is created. For example, using a pattern of "MM/dd/yy" and a
 * SimpleDateFormat instance created on Jan 1, 1997,  the string
 * "01/11/12" would be interpreted as Jan 11, 2012 while the string "05/04/64"
 * would be interpreted as May 4, 1964.
 * During parsing, only strings consisting of exactly two digits, as defined by
 * {@link java.lang.Character#isDigit(char)}, will be parsed into the default
 * century.
 * Any other numeric string, such as a one digit string, a three or more digit
 * string, or a two digit string that isn't all digits (for example, "-1"), is
 * interpreted literally.  So "01/02/3" or "01/02/003" are parsed, using the
 * same pattern, as Jan 2, 3 AD.  Likewise, "01/02/-3" is parsed as Jan 2, 4 BC.
 *
 * <p>
 * If the year pattern does not have exactly two 'y' characters, the year is
 * interpreted literally, regardless of the number of digits.  So using the
 * pattern "MM/dd/yyyy", "01/11/12" parses to Jan 11, 12 A.D.
 *
 * <p>
 * When numeric fields abut one another directly, with no intervening delimiter
 * characters, they constitute a run of abutting numeric fields.  Such runs are
 * parsed specially.  For example, the format "HHmmss" parses the input text
 * "123456" to 12:34:56, parses the input text "12345" to 1:23:45, and fails to
 * parse "1234".  In other words, the leftmost field of the run is flexible,
 * while the others keep a fixed width.  If the parse fails anywhere in the run,
 * then the leftmost field is shortened by one character, and the entire run is
 * parsed again. This is repeated until either the parse succeeds or the
 * leftmost field is one character in length.  If the parse still fails at that
 * point, the parse of the run fails.
 *
 * <p>
 * For time zones that have no names, use strings GMT+hours:minutes or
 * GMT-hours:minutes.
 *
 * <p>
 * The calendar defines what is the first day of the week, the first week
 * of the year, whether hours are zero based or not (0 vs 12 or 24), and the
 * time zone. There is one common decimal format to handle all the numbers;
 * the digit count is handled programmatically according to the pattern.
 *
 * <h4>Synchronization</h4>
 *
 * Date formats are not synchronized. It is recommended to create separate
 * format instances for each thread. If multiple threads access a format
 * concurrently, it must be synchronized externally.
 *
 * @see          com.ibm.icu.util.Calendar
 * @see          com.ibm.icu.util.GregorianCalendar
 * @see          com.ibm.icu.util.TimeZone
 * @see          DateFormat
 * @see          DateFormatSymbols
 * @see          DecimalFormat
 * @author       Mark Davis, Chen-Lieh Huang, Alan Liu
 * @stable ICU 2.0
 */
public class SimpleDateFormat extends DateFormat {
    private static final long serialVersionUID = 1;
       
    /**
     * Construct a SimpleDateFormat using the default pattern for the default
     * locale.  <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     *
     * @see DateFormat
     * @stable ICU 2.0
     */
    public SimpleDateFormat() {
        super(new java.text.SimpleDateFormat());
    }
        
    /**
     * Construct a SimpleDateFormat using the given pattern in the default
     * locale.  <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     * @param pattern the pattern to use
     * @stable ICU 2.0
     */
    public SimpleDateFormat(String pattern) {
        super(new java.text.SimpleDateFormat(pattern));
    }
        
    /**
     * Construct a SimpleDateFormat using the given pattern and locale.
     * <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     * @param pattern the pattern to use
     * @param loc the locale to use for localization
     * @stable ICU 2.0
     */
    public SimpleDateFormat(String pattern, Locale loc) {
        super(new java.text.SimpleDateFormat(pattern, loc));
    }
        
    /**
     * Construct a SimpleDateFormat using the given pattern and locale.
     * <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     * @param pattern the pattern to use
     * @param loc the ulocale to use for localization
     * @stable ICU 3.2
     */
    public SimpleDateFormat(String pattern, ULocale loc) {
        this(pattern, loc.toLocale());
    }
        
    /**
     * Construct a SimpleDateFormat using the given pattern and
     * locale-specific symbol data.
     * Warning: uses default locale for digits!
     * @param pattern the pattern to use
     * @param formatData the symbols to use for localization
     * @stable ICU 2.0
     */
    public SimpleDateFormat(String pattern, DateFormatSymbols formatData) {
        super(new java.text.SimpleDateFormat(pattern, formatData.dfs));
    }
        
    /**
     * Sets the 100-year period 2-digit years will be interpreted as being in
     * to begin on the date the user specifies.
     * @param startDate During parsing, two digit years will be placed in the range
     * <code>startDate</code> to <code>startDate + 100 years</code>.
     * @stable ICU 2.0
     */
    public void set2DigitYearStart(Date startDate) {
        ((java.text.SimpleDateFormat)dateFormat).set2DigitYearStart(startDate);
    }
        
    /**
     * Returns the beginning date of the 100-year period 2-digit years are interpreted
     * as being within.
     * @return the start of the 100-year period into which two digit years are
     * parsed
     * @stable ICU 2.0
     */
    public Date get2DigitYearStart() {
        return ((java.text.SimpleDateFormat)dateFormat).get2DigitYearStart();
    }
        
    /**
     * Overrides DateFormat.
     * <p>Formats a date or time, which is the standard millis
     * since January 1, 1970, 00:00:00 GMT.
     * <p>Example: using the US locale:
     * "yyyy.MM.dd G 'at' HH:mm:ss zzz" ->> 1996.07.10 AD at 15:08:56 PDT
     * @param cal the calendar whose date-time value is to be formatted into a date-time string
     * @param toAppendTo where the new date-time text is to be appended
     * @param pos the formatting position. On input: an alignment field,
     * if desired. On output: the offsets of the alignment field.
     * @return the formatted date-time string.
     * @see DateFormat
     * @stable ICU 2.0
     */
    public StringBuffer format(Calendar cal, StringBuffer toAppendTo, FieldPosition pos) {
        StringBuffer result;
        synchronized(dateFormat) {
            java.util.Calendar oldCal = dateFormat.getCalendar();
            dateFormat.setCalendar(cal.calendar);
            result = dateFormat.format(cal.getTime(), toAppendTo, pos);
            dateFormat.setCalendar(oldCal);
        }
        return result;
    }
        
    /**
     * Overrides DateFormat
     * @see DateFormat
     * @stable ICU 2.0
     */
    public void parse(String text, Calendar cal, ParsePosition parsePos) {
        // TODO: cannot set the parsed timezone, document?
        cal.setTime(dateFormat.parse(text, parsePos));
    }
        
    /**
     * Return a pattern string describing this date format.
     * @stable ICU 2.0
     */
    public String toPattern() {
        return ((java.text.SimpleDateFormat)dateFormat).toPattern();
    }
        
    /**
     * Return a localized pattern string describing this date format.
     * @stable ICU 2.0
     */
    public String toLocalizedPattern() {
        return ((java.text.SimpleDateFormat)dateFormat).toLocalizedPattern();
    }
        
    /**
     * Apply the given unlocalized pattern string to this date format.
     * @stable ICU 2.0
     */
    public void applyPattern(String pattern) {
        ((java.text.SimpleDateFormat)dateFormat).applyPattern(pattern);
    }
        
    /**
     * Apply the given localized pattern string to this date format.
     * @stable ICU 2.0
     */
    public void applyLocalizedPattern(String pattern) {
        ((java.text.SimpleDateFormat)dateFormat).applyLocalizedPattern(pattern);
    }
        
    /**
     * Gets the date/time formatting data.
     * @return a copy of the date-time formatting data associated
     * with this date-time formatter.
     * @stable ICU 2.0
     */
    public DateFormatSymbols getDateFormatSymbols() {
        return new DateFormatSymbols(((java.text.SimpleDateFormat)dateFormat).getDateFormatSymbols());
    }
        
    /**
     * Allows you to set the date/time formatting data.
     * @param newFormatSymbols the new symbols
     * @stable ICU 2.0
     */
    public void setDateFormatSymbols(DateFormatSymbols newFormatSymbols) {
        ((java.text.SimpleDateFormat)dateFormat).setDateFormatSymbols(newFormatSymbols.dfs);
    }
        
    /**
     * Overrides Cloneable
     * @stable ICU 2.0
     */
    public Object clone() {
        return new SimpleDateFormat((java.text.SimpleDateFormat)dateFormat.clone());
    }
        
    /**
     * For clone to use
     * @param sdf
     */
    private SimpleDateFormat(java.text.SimpleDateFormat sdf) {
        super(sdf);
    }
        
    // hashCode, equals, clone use DateFormat implementation.
}

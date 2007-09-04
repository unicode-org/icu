//##header J2SE15
/*
 *******************************************************************************
 * Copyright (C) 1996-2007, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import com.ibm.icu.impl.CalendarData;
import com.ibm.icu.impl.DateNumberFormat;
import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.impl.ZoneMeta;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
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
 * V        time zone (location)    (Text)              United States (Los Angeles)
 * g*       Julian day              (Number)            2451334
 * A*       milliseconds in day     (Number)            69540000
 * Q*       quarter in year         (Text & Number)     Q1 & 01
 * c*       stand alone day of week (Text & Number)     Tuesday & 2
 * L*       stand alone month       (Text & Number)     July & 07
 * q*       stand alone quarter     (Text & Number)     Q1 & 01
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

    // the official serial version ID which says cryptically
    // which version we're compatible with
    private static final long serialVersionUID = 4774881970558875024L;

    // the internal serial version which says which version was written
    // - 0 (default) for version up to JDK 1.1.3
    // - 1 for version from JDK 1.1.4, which includes a new field
    static final int currentSerialVersion = 1;

    /**
     * The version of the serialized data on the stream.  Possible values:
     * <ul>
     * <li><b>0</b> or not present on stream: JDK 1.1.3.  This version
     * has no <code>defaultCenturyStart</code> on stream.
     * <li><b>1</b> JDK 1.1.4 or later.  This version adds
     * <code>defaultCenturyStart</code>.
     * </ul>
     * When streaming out this class, the most recent format
     * and the highest allowable <code>serialVersionOnStream</code>
     * is written.
     * @serial
     */
    private int serialVersionOnStream = currentSerialVersion;

    /**
     * The pattern string of this formatter.  This is always a non-localized
     * pattern.  May not be null.  See class documentation for details.
     * @serial
     */
    private String pattern;

    /**
     * The symbols used by this formatter for week names, month names,
     * etc.  May not be null.
     * @serial
     * @see DateFormatSymbols
     */
    private DateFormatSymbols formatData;

    private transient ULocale locale;

    /**
     * We map dates with two-digit years into the century starting at
     * <code>defaultCenturyStart</code>, which may be any date.  May
     * not be null.
     * @serial
     * @since JDK1.1.4
     */
    private Date defaultCenturyStart;

    private transient int defaultCenturyStartYear;

    // defaultCenturyBase is set when an instance is created
    // and may be used for calculating defaultCenturyStart when needed.
    private transient long defaultCenturyBase;

    private transient TimeZone parsedTimeZone;

    private static final int millisPerHour = 60 * 60 * 1000;
    private static final int millisPerMinute = 60 * 1000;

    // For time zones that have no names, use strings GMT+minutes and
    // GMT-minutes. For instance, in France the time zone is GMT+60.
    private static final String GMT_PLUS = "GMT+";
    private static final String GMT_MINUS = "GMT-";
    private static final String GMT = "GMT";

    // This prefix is designed to NEVER MATCH real text, in order to
    // suppress the parsing of negative numbers.  Adjust as needed (if
    // this becomes valid Unicode).
    private static final String SUPPRESS_NEGATIVE_PREFIX = "\uAB00";

    /**
     * If true, this object supports fast formatting using the
     * subFormat variant that takes a StringBuffer.
     */
    private transient boolean useFastFormat;

    /**
     * Construct a SimpleDateFormat using the default pattern for the default
     * locale.  <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     *
     * @see DateFormat
     * @stable ICU 2.0
     */
    public SimpleDateFormat() {
        this(getDefaultPattern(), null, null, null, null, true);
    }

    /**
     * Construct a SimpleDateFormat using the given pattern in the default
     * locale.  <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     * @stable ICU 2.0
     */
    public SimpleDateFormat(String pattern)
    {
        this(pattern, null, null, null, null, true);
    }

    /**
     * Construct a SimpleDateFormat using the given pattern and locale.
     * <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     * @stable ICU 2.0
     */
    public SimpleDateFormat(String pattern, Locale loc)
    {
        this(pattern, null, null, null, ULocale.forLocale(loc), true);
    }

    /**
     * Construct a SimpleDateFormat using the given pattern and locale.
     * <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     * @stable ICU 3.8
     */
    public SimpleDateFormat(String pattern, ULocale loc)
    {
        this(pattern, null, null, null, loc, true);
    }

    /**
     * Construct a SimpleDateFormat using the given pattern and
     * locale-specific symbol data.
     * Warning: uses default locale for digits!
     * @stable ICU 2.0
     */
    public SimpleDateFormat(String pattern, DateFormatSymbols formatData)
    {
        this(pattern, (DateFormatSymbols)formatData.clone(), null, null, null, true);
    }

    /**
     * @internal ICU 3.2
     * @deprecated This API is ICU internal only.
     */
    public SimpleDateFormat(String pattern, DateFormatSymbols formatData, ULocale loc)
    {
        this(pattern, (DateFormatSymbols)formatData.clone(), null, null, loc, true);
    }

    /**
     * Package-private constructor that allows a subclass to specify
     * whether it supports fast formatting.
     *
     * TODO make this API public.
     */
    SimpleDateFormat(String pattern, DateFormatSymbols formatData, Calendar calendar, ULocale locale,
                     boolean useFastFormat) {
        this(pattern, (DateFormatSymbols)formatData.clone(), (Calendar)calendar.clone(), null, locale, useFastFormat);
    }

    /*
     * The constructor called from all other SimpleDateFormat constructors
     */
    private SimpleDateFormat(String pattern, DateFormatSymbols formatData, Calendar calendar,
            NumberFormat numberFormat, ULocale locale, boolean useFastFormat) {
        this.pattern = pattern;
        this.formatData = formatData;
        this.calendar = calendar;
        this.numberFormat = numberFormat;
        this.locale = locale; // time zone formatting
        this.useFastFormat = useFastFormat;
        initialize();
    }

    /**
     * Create an instance of SimpleDateForamt for the given format configuration
     * @param formatConfig the format configuration
     * @return A SimpleDateFormat instance
     * @internal ICU 3.8
     * @deprecated This API is for internal ICU use only
     */
    public static SimpleDateFormat getInstance(Calendar.FormatConfiguration formatConfig) {
        return new SimpleDateFormat(formatConfig.getPatternString(),
                    formatConfig.getDateFormatSymbols(),
                    formatConfig.getCalendar(),
                    null,
                    formatConfig.getLocale(),
                    true);
    }

    /*
     * Initialized fields
     */
    private void initialize() {
        if (locale == null) {
            locale = ULocale.getDefault();
        }
        if (formatData == null) {
            formatData = new DateFormatSymbols(locale);
        }
        if (calendar == null) {
            calendar = Calendar.getInstance(locale);
        }
        if (numberFormat == null) {
            // Use a NumberFormat optimized for date formatting
            numberFormat = new DateNumberFormat(locale);
        }
        // Note: deferring calendar calculation until when we really need it.
        // Instead, we just record time of construction for backward compatibility.
        defaultCenturyBase = System.currentTimeMillis();

        initLocalZeroPaddingNumberFormat();
    }

    // privates for the default pattern
    private static ULocale cachedDefaultLocale = null;
    private static String cachedDefaultPattern = null;
    private static final String FALLBACKPATTERN = "yy/MM/dd HH:mm";

    /*
     * Returns the default date and time pattern (SHORT) for the default locale.
     * This method is only used by the default SimpleDateFormat constructor.
     */
    private static synchronized String getDefaultPattern() {
        ULocale defaultLocale = ULocale.getDefault();
        if (!defaultLocale.equals(cachedDefaultLocale)) {
            cachedDefaultLocale = defaultLocale;
            Calendar cal = Calendar.getInstance(cachedDefaultLocale);
            try {
                CalendarData calData = new CalendarData(cachedDefaultLocale, cal.getType());
                String[] dateTimePatterns = calData.getStringArray("DateTimePatterns");
                cachedDefaultPattern = MessageFormat.format(dateTimePatterns[8],
                        new Object[] {dateTimePatterns[SHORT], dateTimePatterns[SHORT + 4]});
            } catch (MissingResourceException e) {
                cachedDefaultPattern = FALLBACKPATTERN;
            }
        }
        return cachedDefaultPattern;
    }

    /* Define one-century window into which to disambiguate dates using
     * two-digit years.
     */
    private void parseAmbiguousDatesAsAfter(Date startDate) {
        defaultCenturyStart = startDate;
        calendar.setTime(startDate);
        defaultCenturyStartYear = calendar.get(Calendar.YEAR);
    }

    /* Initialize defaultCenturyStart and defaultCenturyStartYear by base time.
     * The default start time is 80 years before the creation time of this object.
     */
    private void initializeDefaultCenturyStart(long baseTime) {
        defaultCenturyBase = baseTime;
        // clone to avoid messing up date stored in calendar object
        // when this method is called while parsing
        Calendar tmpCal = (Calendar)calendar.clone();
        tmpCal.setTimeInMillis(baseTime);
        tmpCal.add(Calendar.YEAR, -80);
        defaultCenturyStart = tmpCal.getTime();
        defaultCenturyStartYear = tmpCal.get(Calendar.YEAR);
    }

    /* Gets the default century start date for this object */
    private Date getDefaultCenturyStart() {
        if (defaultCenturyStart == null) {
            // not yet initialized
            initializeDefaultCenturyStart(defaultCenturyBase);
        }
        return defaultCenturyStart;
    }

    /* Gets the default century start year for this object */
    private int getDefaultCenturyStartYear() {
        if (defaultCenturyStart == null) {
            // not yet initialized
            initializeDefaultCenturyStart(defaultCenturyBase);
        }
        return defaultCenturyStartYear;
    }

    /**
     * Sets the 100-year period 2-digit years will be interpreted as being in
     * to begin on the date the user specifies.
     * @param startDate During parsing, two digit years will be placed in the range
     * <code>startDate</code> to <code>startDate + 100 years</code>.
     * @stable ICU 2.0
     */
    public void set2DigitYearStart(Date startDate) {
        parseAmbiguousDatesAsAfter(startDate);
    }

    /**
     * Returns the beginning date of the 100-year period 2-digit years are interpreted
     * as being within.
     * @return the start of the 100-year period into which two digit years are
     * parsed
     * @stable ICU 2.0
     */
    public Date get2DigitYearStart() {
        return getDefaultCenturyStart();
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
    public StringBuffer format(Calendar cal, StringBuffer toAppendTo,
                               FieldPosition pos) {
        return format(cal, toAppendTo, pos, null);
    }

    // The actual method to format date. If List attributes is not null,
    // then attribute information will be recorded.
    private StringBuffer format(Calendar cal, StringBuffer toAppendTo,
            FieldPosition pos, List attributes) {
        // Initialize
        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        // Careful: For best performance, minimize the number of calls
        // to StringBuffer.append() by consolidating appends when
        // possible.

        Object[] items = getPatternItems();
        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof String) {
                toAppendTo.append((String)items[i]);
            } else {
                PatternItem item = (PatternItem)items[i];
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
                int start = 0;
                if (attributes != null) {
                    // Save the current length
                    start = toAppendTo.length();
                }
//#endif
                if (useFastFormat) {
                    subFormat(toAppendTo, item.type, item.length, toAppendTo.length(), pos, cal);
                } else {
                    toAppendTo.append(subFormat(item.type, item.length, toAppendTo.length(), pos, formatData, cal));
                }
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
                if (attributes != null) {
                    // Check the sub format length
                    int end = toAppendTo.length();
                    if (end - start > 0) {
                        // Append the attribute to the list
                        DateFormat.Field attr = patternCharToDateFormatField(item.type);
                        FieldPosition fp = new FieldPosition(attr);
                        fp.setBeginIndex(start);
                        fp.setEndIndex(end);
                        attributes.add(fp);
                    }
                }
//#endif
            }
        }
        return toAppendTo;
        
    }

    // Map pattern character to index
    private static final int PATTERN_CHAR_BASE = 0x40;
    private static final int[] PATTERN_CHAR_TO_INDEX =
    {
    //       A   B   C   D   E   F   G   H   I   J   K   L   M   N   O
        -1, 22, -1, -1, 10,  9, 11,  0,  5, -1, -1, 16, 26,  2, -1, -1,
    //   P   Q   R   S   T   U   V   W   X   Y   Z
        -1, 27, -1,  8, -1, -1, 29, 13, -1, 18, 23, -1, -1, -1, -1, -1,
    //       a   b   c   d   e   f   g   h   i   j   k   l   m   n   o
        -1, 14, -1, 25,  3, 19, -1, 21, 15, -1, -1,  4, -1,  6, -1, -1,
    //   p   q   r   s   t   u   v   w   x   y   z
        -1, 28, -1,  7, -1, 20, 24, 12, -1,  1, 17, -1, -1, -1, -1, -1
    };
    
    // Map pattern character index to Calendar field number
    private static final int[] PATTERN_INDEX_TO_CALENDAR_FIELD =
    {
        /*GyM*/ Calendar.ERA, Calendar.YEAR, Calendar.MONTH,
        /*dkH*/ Calendar.DATE, Calendar.HOUR_OF_DAY, Calendar.HOUR_OF_DAY,
        /*msS*/ Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND,
        /*EDF*/ Calendar.DAY_OF_WEEK, Calendar.DAY_OF_YEAR, Calendar.DAY_OF_WEEK_IN_MONTH,
        /*wWa*/ Calendar.WEEK_OF_YEAR, Calendar.WEEK_OF_MONTH, Calendar.AM_PM,
        /*hKz*/ Calendar.HOUR, Calendar.HOUR, Calendar.ZONE_OFFSET,
        /*Yeu*/ Calendar.YEAR_WOY, Calendar.DOW_LOCAL, Calendar.EXTENDED_YEAR,
        /*gAZ*/ Calendar.JULIAN_DAY, Calendar.MILLISECONDS_IN_DAY, Calendar.ZONE_OFFSET,
        /*v*/   Calendar.ZONE_OFFSET,
        /*c*/   Calendar.DAY_OF_WEEK,
        /*L*/   Calendar.MONTH,
        /*Qq*/  Calendar.MONTH, Calendar.MONTH,
        /*V*/   Calendar.ZONE_OFFSET,
    };

    // Map pattern character index to DateFormat field number
    private static final int[] PATTERN_INDEX_TO_DATE_FORMAT_FIELD = {
        /*GyM*/ DateFormat.ERA_FIELD, DateFormat.YEAR_FIELD, DateFormat.MONTH_FIELD,
        /*dkH*/ DateFormat.DATE_FIELD, DateFormat.HOUR_OF_DAY1_FIELD, DateFormat.HOUR_OF_DAY0_FIELD,
        /*msS*/ DateFormat.MINUTE_FIELD, DateFormat.SECOND_FIELD, DateFormat.FRACTIONAL_SECOND_FIELD,
        /*EDF*/ DateFormat.DAY_OF_WEEK_FIELD, DateFormat.DAY_OF_YEAR_FIELD, DateFormat.DAY_OF_WEEK_IN_MONTH_FIELD,
        /*wWa*/ DateFormat.WEEK_OF_YEAR_FIELD, DateFormat.WEEK_OF_MONTH_FIELD, DateFormat.AM_PM_FIELD,
        /*hKz*/ DateFormat.HOUR1_FIELD, DateFormat.HOUR0_FIELD, DateFormat.TIMEZONE_FIELD,
        /*Yeu*/ DateFormat.YEAR_WOY_FIELD, DateFormat.DOW_LOCAL_FIELD, DateFormat.EXTENDED_YEAR_FIELD,
        /*gAZ*/ DateFormat.JULIAN_DAY_FIELD, DateFormat.MILLISECONDS_IN_DAY_FIELD, DateFormat.TIMEZONE_RFC_FIELD,
        /*v*/   DateFormat.TIMEZONE_GENERIC_FIELD, 
        /*c*/   DateFormat.STANDALONE_DAY_FIELD,
        /*L*/   DateFormat.STANDALONE_MONTH_FIELD,
        /*Qq*/  DateFormat.QUARTER_FIELD, DateFormat.STANDALONE_QUARTER_FIELD,
        /*V*/   DateFormat.TIMEZONE_SPECIAL_FIELD, 
    };

//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
    // Map pattern character index to DateFormat.Field
    private static final DateFormat.Field[] PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE = {
        /*GyM*/ DateFormat.Field.ERA, DateFormat.Field.YEAR, DateFormat.Field.MONTH,
        /*dkH*/ DateFormat.Field.DAY_OF_MONTH, DateFormat.Field.HOUR_OF_DAY1, DateFormat.Field.HOUR_OF_DAY0,
        /*msS*/ DateFormat.Field.MINUTE, DateFormat.Field.SECOND, DateFormat.Field.MILLISECOND,
        /*EDF*/ DateFormat.Field.DAY_OF_WEEK, DateFormat.Field.DAY_OF_YEAR, DateFormat.Field.DAY_OF_WEEK_IN_MONTH,
        /*wWa*/ DateFormat.Field.WEEK_OF_YEAR, DateFormat.Field.WEEK_OF_MONTH, DateFormat.Field.AM_PM,
        /*hKz*/ DateFormat.Field.HOUR1, DateFormat.Field.HOUR0, DateFormat.Field.TIME_ZONE,
        /*Yeu*/ DateFormat.Field.YEAR_WOY, DateFormat.Field.DOW_LOCAL, DateFormat.Field.EXTENDED_YEAR,
        /*gAZ*/ DateFormat.Field.JULIAN_DAY, DateFormat.Field.MILLISECONDS_IN_DAY, DateFormat.Field.TIME_ZONE,
        /*v*/   DateFormat.Field.TIME_ZONE,
        /*c*/   DateFormat.Field.DAY_OF_WEEK,
        /*L*/   DateFormat.Field.MONTH,
        /*Qq*/  DateFormat.Field.QUARTER, DateFormat.Field.QUARTER,
        /*V*/   DateFormat.Field.TIME_ZONE,
    };

    /**
     * Return a DateFormat.Field constant associated with the specified format pattern
     * character.
     * 
     * @param ch The pattern character
     * @return DateFormat.Field associated with the pattern character
     * 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    protected DateFormat.Field patternCharToDateFormatField(char ch) {
        int patternCharIndex = -1;
        if ('A' <= ch && ch <= 'z') {
            patternCharIndex = PATTERN_CHAR_TO_INDEX[(int)ch - PATTERN_CHAR_BASE];
        }
        if (patternCharIndex != -1) {
            return PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE[patternCharIndex];
        }
        return null;
    }
//#endif

    /**
     * Format a single field, given its pattern character.  Subclasses may
     * override this method in order to modify or add formatting
     * capabilities.
     * @param ch the pattern character
     * @param count the number of times ch is repeated in the pattern
     * @param beginOffset the offset of the output string at the start of
     * this field; used to set pos when appropriate
     * @param pos receives the position of a field, when appropriate
     * @param formatData the symbols for this formatter
     * @stable ICU 2.0
     */
    protected String subFormat(char ch, int count, int beginOffset,
                               FieldPosition pos, DateFormatSymbols formatData,
                               Calendar cal)
        throws IllegalArgumentException
    {
        // Note: formatData is ignored
        StringBuffer buf = new StringBuffer();
        subFormat(buf, ch, count, beginOffset, pos, cal);
        return buf.toString();
    }

    /**
     * Format a single field; useFastFormat variant.  Reuses a
     * StringBuffer for results instead of creating a String on the
     * heap for each call.
     *
     * NOTE We don't really need the beginOffset parameter, EXCEPT for
     * the need to support the slow subFormat variant (above) which
     * has to pass it in to us.
     *
     * TODO make this API public
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected void subFormat(StringBuffer buf,
                             char ch, int count, int beginOffset,
                             FieldPosition pos,
                             Calendar cal) {
        final int maxIntCount = Integer.MAX_VALUE;
        final int bufstart = buf.length();

        // final int patternCharIndex = DateFormatSymbols.patternChars.indexOf(ch);
        int patternCharIndex = -1;
        if ('A' <= ch && ch <= 'z') {
            patternCharIndex = PATTERN_CHAR_TO_INDEX[(int)ch - PATTERN_CHAR_BASE];
        }

        if (patternCharIndex == -1) {
            throw new IllegalArgumentException("Illegal pattern character " +
                                               "'" + ch + "' in \"" +
                                               new String(pattern) + '"');
        }

        final int field = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
        int value = cal.get(field);

        switch (patternCharIndex) {
        case 0: // 'G' - ERA
            if (count == 5) {
                buf.append(formatData.narrowEras[value]);
            } else if (count == 4)
               buf.append(formatData.eraNames[value]);
            else
               buf.append(formatData.eras[value]);
            break;
        case 1: // 'y' - YEAR
            /* According to the specification, if the number of pattern letters ('y') is 2,
             * the year is truncated to 2 digits; otherwise it is interpreted as a number.
             * But the original code process 'y', 'yy', 'yyy' in the same way. and process
             * patterns with 4 or more than 4 'y' characters in the same way.
             * So I change the codes to meet the specification. [Richard/GCl]
             */
            if (count == 2)
                zeroPaddingNumber(buf, value, 2, 2); // clip 1996 to 96
            else //count = 1 or count > 2
                zeroPaddingNumber(buf, value, count, maxIntCount);
            break;
        case 2: // 'M' - MONTH
            if (count == 5) 
                buf.append(formatData.narrowMonths[value]);
            else if (count == 4)
                buf.append(formatData.months[value]);
            else if (count == 3)
                buf.append(formatData.shortMonths[value]);
            else
                zeroPaddingNumber(buf, value+1, count, maxIntCount);
            break;
        case 4: // 'k' - HOUR_OF_DAY (1..24)
            if (value == 0)
                zeroPaddingNumber(buf,
                                  cal.getMaximum(Calendar.HOUR_OF_DAY)+1,
                                  count, maxIntCount);
            else
                zeroPaddingNumber(buf, value, count, maxIntCount);
            break;
        case 8: // 'S' - FRACTIONAL_SECOND
            // Fractional seconds left-justify
            {
                numberFormat.setMinimumIntegerDigits(Math.min(3, count));
                numberFormat.setMaximumIntegerDigits(maxIntCount);
                if (count == 1) {
                    value = (value + 50) / 100;
                } else if (count == 2) {
                    value = (value + 5) / 10;
                }
                FieldPosition p = new FieldPosition(-1);
                numberFormat.format((long) value, buf, p);
                if (count > 3) {
                    numberFormat.setMinimumIntegerDigits(count - 3);
                    numberFormat.format(0L, buf, p);
                }
            }
            break;
        case 9: // 'E' - DAY_OF_WEEK
            if (count == 5) {
                buf.append(formatData.narrowWeekdays[value]);
            } else if (count == 4)
                buf.append(formatData.weekdays[value]);
            else // count <= 3, use abbreviated form if exists
                buf.append(formatData.shortWeekdays[value]);
            break;
        case 14: // 'a' - AM_PM
            buf.append(formatData.ampms[value]);
            break;
        case 15: // 'h' - HOUR (1..12)
            if (value == 0)
                zeroPaddingNumber(buf,
                                  cal.getLeastMaximum(Calendar.HOUR)+1,
                                  count, maxIntCount);
            else
                zeroPaddingNumber(buf, value, count, maxIntCount);
            break;
        case 17: // 'z' - ZONE_OFFSET
        case 24: // 'v' - TIMEZONE_GENERIC 
        case 29: // 'V' - TIMEZONE_SPECIAL
            {

                String zid;
                DateFormatSymbols.MetazoneInfo mz=null;
                String res=null;
                zid = ZoneMeta.getCanonicalID(cal.getTimeZone().getID());
                boolean isGeneric = patternCharIndex == 24;
                if (zid != null) {
                    if (patternCharIndex == TIMEZONE_GENERIC_FIELD) {
                        if(count < 4){
                            res = formatData.getZoneString(zid, DateFormatSymbols.TIMEZONE_SHORT_GENERIC);
                            if ( !formatData.isCommonlyUsed(zid)) {
                               res = null;
                            }
                            if ( res == null ) {
                               mz = formatData.getMetazoneInfo(zid, DateFormatSymbols.TIMEZONE_SHORT_GENERIC,cal);
                               if ( mz == null || !formatData.isCommonlyUsed(mz.mzid)) {
                                   res = null;
                               }
                               else {
                                   res = mz.value;
                               }
                            }
                        }else{
                            res = formatData.getZoneString(zid, DateFormatSymbols.TIMEZONE_LONG_GENERIC);
                            if ( res == null ) {
                               mz = formatData.getMetazoneInfo(zid, DateFormatSymbols.TIMEZONE_LONG_GENERIC,cal);
                               if ( mz != null ) {
                                   res = mz.value;
                               }
                            }
                        }
                    } else if (patternCharIndex == TIMEZONE_SPECIAL_FIELD) {
                        if (count == 4){ // VVVV format - always get the fallback string.
                            res = formatData.getZoneString(zid, DateFormatSymbols.TIMEZONE_EXEMPLAR_CITY);
                        
                            if (res == null) {
                                res = ZoneMeta.displayFallback(zid, null, locale);
                            }
                        }
                        else if (count == 1){ // V format - ignore commonlyUsed
                            if (cal.get(Calendar.DST_OFFSET) != 0) {
                                res = formatData.getZoneString(zid, DateFormatSymbols.TIMEZONE_SHORT_DAYLIGHT);
                                if ( res == null ) {
                                    mz = formatData.getMetazoneInfo(zid, DateFormatSymbols.TIMEZONE_SHORT_DAYLIGHT,cal);
                                    if ( mz != null ) {
                                        res = mz.value;
                                    }
                                }
                            }else{
                                res = formatData.getZoneString(zid, DateFormatSymbols.TIMEZONE_SHORT_STANDARD);
                                if ( res == null ) {
                                    mz = formatData.getMetazoneInfo(zid, DateFormatSymbols.TIMEZONE_SHORT_STANDARD,cal);
                                    if ( mz != null ) {
                                        res = mz.value;
                                    }
                                }
                            }
                        }
                    } else {
                        if (cal.get(Calendar.DST_OFFSET) != 0) {
                            if(count < 4){
                                res = formatData.getZoneString(zid, DateFormatSymbols.TIMEZONE_SHORT_DAYLIGHT);
                                if ( !formatData.isCommonlyUsed(zid)) {
                                   res = null;
                                }
                                if ( res == null ) {
                                    mz = formatData.getMetazoneInfo(zid, DateFormatSymbols.TIMEZONE_SHORT_DAYLIGHT,cal);
                                    if ( mz == null || !formatData.isCommonlyUsed(mz.mzid)) {
                                        res = null;
                                    }
                                    else {
                                        res = mz.value;
                                    }
                                }
                            }else{
                                res = formatData.getZoneString(zid, DateFormatSymbols.TIMEZONE_LONG_DAYLIGHT);
                                if ( res == null ) {
                                    mz = formatData.getMetazoneInfo(zid, DateFormatSymbols.TIMEZONE_LONG_DAYLIGHT,cal);
                                    if ( mz != null ) {
                                        res = mz.value;
                                    }
                                }
                            }
                        }else{
                            if(count < 4){
                                res = formatData.getZoneString(zid, DateFormatSymbols.TIMEZONE_SHORT_STANDARD);
                                if ( !formatData.isCommonlyUsed(zid)) {
                                   res = null;
                                }
                                if ( res == null ) {
                                    mz = formatData.getMetazoneInfo(zid, DateFormatSymbols.TIMEZONE_SHORT_STANDARD,cal);
                                    if ( mz == null || !formatData.isCommonlyUsed(mz.mzid)) {
                                        res = null;
                                    }
                                    else {
                                        res = mz.value;
                                    }
                                }
                            }else{
                                res = formatData.getZoneString(zid, DateFormatSymbols.TIMEZONE_LONG_STANDARD);
                                if ( res == null ) {
                                    mz = formatData.getMetazoneInfo(zid, DateFormatSymbols.TIMEZONE_LONG_STANDARD,cal);
                                    if ( mz != null ) {
                                        res = mz.value;
                                    }
                                }
                            }
                        }
                    }
                }

                if (res == null || res.length() == 0) {
                    // note, tr35 does not describe the special case for 'no country' 
                    // implemented below, this is from discussion with Mark
                    if (zid == null || !isGeneric || ZoneMeta.getCanonicalCountry(zid) == null) {
                         long offset = cal.get(Calendar.ZONE_OFFSET) +
                            cal.get(Calendar.DST_OFFSET);
                        res = ZoneMeta.displayGMT(offset, locale);
                    } else { 
                        /*
                        String city = formatData.getZoneString(zid, DateFormatSymbols.TIMEZONE_EXEMPLAR_CITY);
                        res = ZoneMeta.displayFallback(zid, city, locale);
                        */
                        res = formatData.getZoneString(zid, DateFormatSymbols.TIMEZONE_EXEMPLAR_CITY);
                        
                        if (res == null) {
                            res = ZoneMeta.displayFallback(zid, null, locale);
                        }
                    }
                }
                
                if(res.length()==0){
                    appendGMT(buf, cal);
                }else{
                    buf.append(res);
                }
            } break;
        case 23: // 'Z' - TIMEZONE_RFC
            {
                if (count < 4) {
                    // 'short' (standard Java) form, must use ASCII digits
                    long val= (cal.get(Calendar.ZONE_OFFSET) +
                           cal.get(Calendar.DST_OFFSET)) / millisPerMinute;
                    char sign = '+';
                    if (val < 0) {
                        val = -val;
                        sign = '-';
                    }
                    val = (val / 60) * 100 + (val % 60); // minutes => KKmm
                    buf.append(sign);

                    // Always use ASCII numbers
                    int num = (int)(val % 10000);
                    int denom = 1000;
                    while (denom >= 1) {
                        char digit = (char)((num / denom) + '0');
                        buf.append(digit);
                        num = num % denom;
                        denom /= 10;
                    }
                } else {
                    // long form, localized GMT pattern
                    // not in 3.4 locale data, need to add, so use same default as for general time zone names
                    long val = cal.get(Calendar.ZONE_OFFSET) +
                        cal.get(Calendar.DST_OFFSET);
                    buf.append(ZoneMeta.displayGMT(val, locale));
                }
            }
            break;
        case 25: // 'c' - STANDALONE DAY
            if (count == 5) 
                buf.append(formatData.standaloneNarrowWeekdays[value]);
            else if (count == 4)
                buf.append(formatData.standaloneWeekdays[value]);
            else if (count == 3)
                buf.append(formatData.standaloneShortWeekdays[value]);
            else
                zeroPaddingNumber(buf, value, 1, maxIntCount);
            break;
        case 26: // 'L' - STANDALONE MONTH
            if (count == 5) 
                buf.append(formatData.standaloneNarrowMonths[value]);
            else if (count == 4)
                buf.append(formatData.standaloneMonths[value]);
            else if (count == 3)
                buf.append(formatData.standaloneShortMonths[value]);
            else
                zeroPaddingNumber(buf, value+1, count, maxIntCount);
            break;
        case 27: // 'Q' - QUARTER
            if (count >= 4)
                buf.append(formatData.quarters[value/3]);
            else if (count == 3)
                buf.append(formatData.shortQuarters[value/3]);
            else
                zeroPaddingNumber(buf, (value/3)+1, count, maxIntCount);
            break;
        case 28: // 'q' - STANDALONE QUARTER
            if (count >= 4)
                buf.append(formatData.standaloneQuarters[value/3]);
            else if (count == 3)
                buf.append(formatData.standaloneShortQuarters[value/3]);
            else
                zeroPaddingNumber(buf, (value/3)+1, count, maxIntCount);
            break;
        default:
            // case 3: // 'd' - DATE
            // case 5: // 'H' - HOUR_OF_DAY (0..23)
            // case 6: // 'm' - MINUTE
            // case 7: // 's' - SECOND
            // case 10: // 'D' - DAY_OF_YEAR
            // case 11: // 'F' - DAY_OF_WEEK_IN_MONTH
            // case 12: // 'w' - WEEK_OF_YEAR
            // case 13: // 'W' - WEEK_OF_MONTH
            // case 16: // 'K' - HOUR (0..11)
            // case 18: // 'Y' - YEAR_WOY
            // case 19: // 'e' - DOW_LOCAL
            // case 20: // 'u' - EXTENDED_YEAR
            // case 21: // 'g' - JULIAN_DAY
            // case 22: // 'A' - MILLISECONDS_IN_DAY

            zeroPaddingNumber(buf, value, count, maxIntCount);
            break;
        } // switch (patternCharIndex)

        // Set the FieldPosition (for the first occurence only)
        if (pos.getBeginIndex() == pos.getEndIndex() &&
            pos.getField() == PATTERN_INDEX_TO_DATE_FORMAT_FIELD[patternCharIndex]) {
            pos.setBeginIndex(beginOffset);
            pos.setEndIndex(beginOffset + buf.length() - bufstart);
        }
    }

    /*
     * PatternItem store parsed date/time field pattern information.
     */
    private static class PatternItem {
        final char type;
        final int length;
        final boolean isNumeric;

        PatternItem(char type, int length) {
            this.type = type;
            this.length = length;
            isNumeric = isNumeric(type, length);
        }
    }

    private static ICUCache PARSED_PATTERN_CACHE = new SimpleCache();
    private transient Object[] patternItems;

    /*
     * Returns parsed pattern items.  Each item is either String or
     * PatternItem.
     */
    private Object[] getPatternItems() {
        if (patternItems != null) {
            return patternItems;
        }

        patternItems = (Object[])PARSED_PATTERN_CACHE.get(pattern);
        if (patternItems != null) {
            return patternItems;
        }

        boolean isPrevQuote = false;
        boolean inQuote = false;
        StringBuffer text = new StringBuffer();
        char itemType = 0;  // 0 for string literal, otherwise date/time pattern character
        int itemLength = 1;

        List items = new ArrayList();

        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == '\'') {
                if (isPrevQuote) {
                    text.append('\'');
                    isPrevQuote = false;
                } else {
                    isPrevQuote = true;
                    if (itemType != 0) {
                        items.add(new PatternItem(itemType, itemLength));
                        itemType = 0;
                    }
                }
                inQuote = !inQuote;
            } else {
                isPrevQuote = false;
                if (inQuote) {
                    text.append(ch);
                } else {
                    if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                        // a date/time pattern character
                        if (ch == itemType) {
                            itemLength++;
                        } else {
                            if (itemType == 0) {
                                if (text.length() > 0) {
                                    items.add(text.toString());
                                    text.setLength(0);
                                }
                            } else {
                                items.add(new PatternItem(itemType, itemLength));
                            }
                            itemType = ch;
                            itemLength = 1;
                        }
                    } else {
                        // a string literal
                        if (itemType != 0) {
                            items.add(new PatternItem(itemType, itemLength));
                            itemType = 0;
                        }
                        text.append(ch);
                    }
                }
            }
        }
        // handle last item
        if (itemType == 0) {
            if (text.length() > 0) {
                items.add(text.toString());
                text.setLength(0);
            }
        } else {
            items.add(new PatternItem(itemType, itemLength));
        }

        patternItems = new Object[items.size()];
        items.toArray(patternItems);

        PARSED_PATTERN_CACHE.put(pattern, patternItems);
        
        return patternItems;
    }

    private void appendGMT(StringBuffer buf, Calendar cal){
        int value = cal.get(Calendar.ZONE_OFFSET) +
        cal.get(Calendar.DST_OFFSET);

        if (value < 0) {
            buf.append(GMT_MINUS);
            value = -value; // suppress the '-' sign for text display.
        }else{
            buf.append(GMT_PLUS);
        }

        zeroPaddingNumber(buf, (int)(value/millisPerHour), 2, 2);
        buf.append((char)0x003A) /*':'*/;
        zeroPaddingNumber(buf, (int)((value%millisPerHour)/millisPerMinute), 2, 2);
    }
    /*
     * Internal method. Returns null if the value of an array is empty, or if the
     * index is out of bounds
     */
/*    private String getZoneArrayValue(String[] zs, int ix) {
        if (ix >= 0 && ix < zs.length) {
            String result = zs[ix];
            if (result != null && result.length() != 0) {
                return result;
            }
        }
        return null;
    }*/

    /**
     * Internal high-speed method.  Reuses a StringBuffer for results
     * instead of creating a String on the heap for each call.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected void zeroPaddingNumber(StringBuffer buf, int value,
                                     int minDigits, int maxDigits) {
        if (useLocalZeroPaddingNumberFormat) {
            fastZeroPaddingNumber(buf, value, minDigits, maxDigits);
        } else {
            numberFormat.setMinimumIntegerDigits(minDigits);
            numberFormat.setMaximumIntegerDigits(maxDigits);
            numberFormat.format(value, buf, new FieldPosition(-1));
        }
    }

    /**
     * Overrides superclass method
     * @stable ICU 2.0
     */
    public void setNumberFormat(NumberFormat newNumberFormat) {
        // Override this method to update local zero padding number formatter
        super.setNumberFormat(newNumberFormat);
        initLocalZeroPaddingNumberFormat();
    }

    private void initLocalZeroPaddingNumberFormat() {
        if (numberFormat instanceof DecimalFormat) {
            zeroDigit = ((DecimalFormat)numberFormat).getDecimalFormatSymbols().getZeroDigit();
            useLocalZeroPaddingNumberFormat = true;
        } else if (numberFormat instanceof DateNumberFormat) {
            zeroDigit = ((DateNumberFormat)numberFormat).getZeroDigit();
            useLocalZeroPaddingNumberFormat = true;
        } else {
            useLocalZeroPaddingNumberFormat = false;
        }

        if (useLocalZeroPaddingNumberFormat) {
            decimalBuf = new char[10];  // sufficient for int numbers
        }
    }

    // If true, use local version of zero padding number format
    private transient boolean useLocalZeroPaddingNumberFormat;
    private transient char zeroDigit;
    private transient char[] decimalBuf;

    /*
     * Lightweight zero padding integer number format function.
     * 
     * Note: This implementation is almost equivalent to format method in DateNumberFormat.
     * In the method zeroPaddingNumber above should be able to use the one in DateNumberFormat,
     * but, it does not help IBM J9's JIT to optimize the performance much.  In simple repeative
     * date format test case, having local implementation is ~10% faster than using one in
     * DateNumberFormat on IBM J9 VM.  On Sun Hotspot VM, I do not see such difference.
     * 
     * -Yoshito
     */
    private void fastZeroPaddingNumber(StringBuffer buf, int value, int minDigits, int maxDigits) {
        int limit = decimalBuf.length < maxDigits ? decimalBuf.length : maxDigits;
        int index = limit - 1;
        while (true) {
            decimalBuf[index] = (char)((value % 10) + zeroDigit);
            value /= 10;
            if (index == 0 || value == 0) {
                break;
            }
            index--;
        }
        int padding = minDigits - (limit - index);
        for (; padding > 0; padding--) {
            decimalBuf[--index] = zeroDigit;
        }
        int length = limit - index;
        buf.append(decimalBuf, index, length);        
    }

    /**
     * Formats a number with the specified minimum and maximum number of digits.
     * @stable ICU 2.0
     */
    protected String zeroPaddingNumber(long value, int minDigits, int maxDigits)
    {
        numberFormat.setMinimumIntegerDigits(minDigits);
        numberFormat.setMaximumIntegerDigits(maxDigits);
        return numberFormat.format(value);
    }

    /**
     * Format characters that indicate numeric fields.  The character
     * at index 0 is treated specially.
     */
    private static final String NUMERIC_FORMAT_CHARS = "MyudhHmsSDFwWkK";

    /**
     * Return true if the given format character, occuring count
     * times, represents a numeric field.
     */
    private static final boolean isNumeric(char formatChar, int count) {
        int i = NUMERIC_FORMAT_CHARS.indexOf(formatChar);
        return (i > 0 || (i == 0 && count < 3));
    }

    /**
     * Overrides DateFormat
     * @see DateFormat
     * @stable ICU 2.0
     */
    public void parse(String text, Calendar cal, ParsePosition parsePos)
    {
        int pos = parsePos.getIndex();
        int start = pos;
        boolean[] ambiguousYear = { false };

        // hack, clear parsedTimeZone
        parsedTimeZone = null;

        // item index for the first numeric field within a countiguous numeric run
        int numericFieldStart = -1;
        // item length for the first numeric field within a countiguous numeric run
        int numericFieldLength = 0;
        // start index of numeric text run in the input text
        int numericStartPos = 0;

        Object[] items = getPatternItems();
        int i = 0;
        while (i < items.length) {
            if (items[i] instanceof PatternItem) {
                // Handle pattern field
                PatternItem field = (PatternItem)items[i];
                if (field.isNumeric) {
                    // Handle fields within a run of abutting numeric fields.  Take
                    // the pattern "HHmmss" as an example. We will try to parse
                    // 2/2/2 characters of the input text, then if that fails,
                    // 1/2/2.  We only adjust the width of the leftmost field; the
                    // others remain fixed.  This allows "123456" => 12:34:56, but
                    // "12345" => 1:23:45.  Likewise, for the pattern "yyyyMMdd" we
                    // try 4/2/2, 3/2/2, 2/2/2, and finally 1/2/2.
                    if (numericFieldStart == -1) {
                        // check if this field is followed by abutting another numeric field
                        if ((i + 1) < items.length 
                                && (items[i + 1] instanceof PatternItem)
                                && ((PatternItem)items[i + 1]).isNumeric) {
                            // record the first numeric field within a numeric text run
                            numericFieldStart = i;
                            numericFieldLength = field.length;
                            numericStartPos = pos; 
                        }
                    }
                }
                if (numericFieldStart != -1) {
                    // Handle a numeric field within abutting numeric fields
                    int len = field.length;
                    if (numericFieldStart == i) {
                        len = numericFieldLength;
                    }

                    // Parse a numeric field
                    pos = subParse(text, pos, field.type, len,
                            true, false, ambiguousYear, cal);

                    if (pos < 0) {
                        // If the parse fails anywhere in the numeric run, back up to the
                        // start of the run and use shorter pattern length for the first
                        // numeric field.
                        --numericFieldLength;
                        if (numericFieldLength == 0) {
                            // can not make shorter any more
                            parsePos.setIndex(start);
                            parsePos.setErrorIndex(pos);
                            return;
                        }
                        i = numericFieldStart;
                        pos = numericStartPos;
                        continue;
                    }

                } else {
                    // Handle a non-numeric field or a non-abutting numeric field
                    numericFieldStart = -1;

                    int s = pos;
                    pos = subParse(text, pos, field.type, field.length,
                            false, true, ambiguousYear, cal);
                    if (pos < 0) {
                        parsePos.setIndex(start);
                        parsePos.setErrorIndex(s);
                        return;
                    }
                }
            } else {
                // Handle literal pattern text literal
                numericFieldStart = -1;

                String patl = (String)items[i];
                int plen = patl.length();
                int tlen = text.length();
                int idx = 0;
                while (idx < plen && pos < tlen) {
                    char pch = patl.charAt(idx);
                    char ich = text.charAt(pos);
                    if (UCharacterProperty.isRuleWhiteSpace(pch) && UCharacterProperty.isRuleWhiteSpace(ich)) {
                        // White space characters found in both patten and input.
                        // Skip contiguous white spaces.
                        while ((idx + 1) < plen &&
                                UCharacterProperty.isRuleWhiteSpace(patl.charAt(idx + 1))) {
                             ++idx;
                        }
                        while ((pos + 1) < tlen &&
                                UCharacterProperty.isRuleWhiteSpace(text.charAt(pos + 1))) {
                             ++pos;
                        }
                    } else if (pch != ich) {
                        break;
                    }
                    ++idx;
                    ++pos;
                }
                if (idx != plen) {
                    // Set the position of mismatch
                    parsePos.setIndex(start);
                    parsePos.setErrorIndex(pos);
                    return;
                }
            }
            ++i;
        }

        // At this point the fields of Calendar have been set.  Calendar
        // will fill in default values for missing fields when the time
        // is computed.

        parsePos.setIndex(pos);

        // This part is a problem:  When we call parsedDate.after, we compute the time.
        // Take the date April 3 2004 at 2:30 am.  When this is first set up, the year
        // will be wrong if we're parsing a 2-digit year pattern.  It will be 1904.
        // April 3 1904 is a Sunday (unlike 2004) so it is the DST onset day.  2:30 am
        // is therefore an "impossible" time, since the time goes from 1:59 to 3:00 am
        // on that day.  It is therefore parsed out to fields as 3:30 am.  Then we
        // add 100 years, and get April 3 2004 at 3:30 am.  Note that April 3 2004 is
        // a Saturday, so it can have a 2:30 am -- and it should. [LIU]
        /*
          Date parsedDate = cal.getTime();
          if( ambiguousYear[0] && !parsedDate.after(getDefaultCenturyStart()) ) {
          cal.add(Calendar.YEAR, 100);
          parsedDate = cal.getTime();
          }
        */
        // Because of the above condition, save off the fields in case we need to readjust.
        // The procedure we use here is not particularly efficient, but there is no other
        // way to do this given the API restrictions present in Calendar.  We minimize
        // inefficiency by only performing this computation when it might apply, that is,
        // when the two-digit year is equal to the start year, and thus might fall at the
        // front or the back of the default century.  This only works because we adjust
        // the year correctly to start with in other cases -- see subParse().
        try {
            if (ambiguousYear[0] || parsedTimeZone != null) {
                // We need a copy of the fields, and we need to avoid triggering a call to
                // complete(), which will recalculate the fields.  Since we can't access
                // the fields[] array in Calendar, we clone the entire object.  This will
                // stop working if Calendar.clone() is ever rewritten to call complete().
                Calendar copy = (Calendar)cal.clone();
                if (ambiguousYear[0]) { // the two-digit year == the default start year
                    Date parsedDate = copy.getTime();
                    if (parsedDate.before(getDefaultCenturyStart())) {
                        // We can't use add here because that does a complete() first.
                        cal.set(Calendar.YEAR, getDefaultCenturyStartYear() + 100);
                    }
                }

                if (parsedTimeZone != null) {
                    TimeZone tz = parsedTimeZone;

                    // the calendar represents the parse as gmt time
                    // we need to turn this into local time, so we add the raw offset
                    // then we ask the timezone to handle this local time
                    int[] offsets = new int[2];
                    tz.getOffset(copy.getTimeInMillis()+tz.getRawOffset(), true, offsets);

                    cal.set(Calendar.ZONE_OFFSET, offsets[0]);
                    cal.set(Calendar.DST_OFFSET, offsets[1]);
                    cal.setTimeZone(tz);
                }
            }
        }
        // An IllegalArgumentException will be thrown by Calendar.getTime()
        // if any fields are out of range, e.g., MONTH == 17.
        catch (IllegalArgumentException e) {
            parsePos.setErrorIndex(pos);
            parsePos.setIndex(start);
        }
    }

    /**
     * Attempt to match the text at a given position against an array of
     * strings.  Since multiple strings in the array may match (for
     * example, if the array contains "a", "ab", and "abc", all will match
     * the input string "abcd") the longest match is returned.  As a side
     * effect, the given field of <code>cal</code> is set to the index
     * of the best match, if there is one.
     * @param text the time text being parsed.
     * @param start where to start parsing.
     * @param field the date field being parsed.
     * @param data the string array to parsed.
     * @return the new start position if matching succeeded; a negative
     * number indicating matching failure, otherwise.  As a side effect,
     * sets the <code>cal</code> field <code>field</code> to the index
     * of the best match, if matching succeeded.
     * @stable ICU 2.0
     */
    protected int matchString(String text, int start, int field, String[] data, Calendar cal)
    {
        int i = 0;
        int count = data.length;

        if (field == Calendar.DAY_OF_WEEK) i = 1;

        // There may be multiple strings in the data[] array which begin with
        // the same prefix (e.g., Cerven and Cervenec (June and July) in Czech).
        // We keep track of the longest match, and return that.  Note that this
        // unfortunately requires us to test all array elements.
        int bestMatchLength = 0, bestMatch = -1;
        for (; i<count; ++i)
            {
                int length = data[i].length();
                // Always compare if we have no match yet; otherwise only compare
                // against potentially better matches (longer strings).
                if (length > bestMatchLength &&
                    text.regionMatches(true, start, data[i], 0, length))
                    {
                        bestMatch = i;
                        bestMatchLength = length;
                    }
            }
        if (bestMatch >= 0)
            {
                cal.set(field, bestMatch);
                return start + bestMatchLength;
            }
        return -start;
    }

    /**
     * Attempt to match the text at a given position against an array of quarter
     * strings.  Since multiple strings in the array may match (for
     * example, if the array contains "a", "ab", and "abc", all will match
     * the input string "abcd") the longest match is returned.  As a side
     * effect, the given field of <code>cal</code> is set to the index
     * of the best match, if there is one.
     * @param text the time text being parsed.
     * @param start where to start parsing.
     * @param field the date field being parsed.
     * @param data the string array to parsed.
     * @return the new start position if matching succeeded; a negative
     * number indicating matching failure, otherwise.  As a side effect,
     * sets the <code>cal</code> field <code>field</code> to the index
     * of the best match, if matching succeeded.
     * @stable ICU 2.0
     */
    protected int matchQuarterString(String text, int start, int field, String[] data, Calendar cal)
    {
        int i = 0;
        int count = data.length;

        // There may be multiple strings in the data[] array which begin with
        // the same prefix (e.g., Cerven and Cervenec (June and July) in Czech).
        // We keep track of the longest match, and return that.  Note that this
        // unfortunately requires us to test all array elements.
        int bestMatchLength = 0, bestMatch = -1;
        for (; i<count; ++i) {
            int length = data[i].length();
            // Always compare if we have no match yet; otherwise only compare
            // against potentially better matches (longer strings).
            if (length > bestMatchLength &&
                text.regionMatches(true, start, data[i], 0, length)) {
                bestMatch = i;
                bestMatchLength = length;
            }
        }
        
        if (bestMatch >= 0) {
            cal.set(field, bestMatch * 3);
            return start + bestMatchLength;
        }
        
        return -start;
    }
    
    /**
     * find time zone 'text' matched zoneStrings and set cal
     */
    private int subParseZoneString(String text, int start, Calendar cal) {
        // At this point, check for named time zones by looking through
        // the locale data from the DateFormatZoneData strings.
        // Want to be able to parse both short and long forms.

        // optimize for calendar's current time zone
        TimeZone tz = null;
        String zid = null, value = null;
        int type = -1;

        DateFormatSymbols.ZoneItem item = formatData.findZoneIDTypeValue(text, start);
        if (item != null) {
            zid = item.zid;
            value = item.value;
            type = item.type;
        }

        if (zid != null) {
            tz = TimeZone.getTimeZone(zid);
        }
        
        if (tz != null) { // Matched any ?
            // always set zone offset, needed to get correct hour in wall time
            // when checking daylight savings
            cal.set(Calendar.ZONE_OFFSET, tz.getRawOffset());
            if (type == DateFormatSymbols.TIMEZONE_SHORT_STANDARD
                    || type == DateFormatSymbols.TIMEZONE_LONG_STANDARD) {
                // standard time
                cal.set(Calendar.DST_OFFSET, 0);
                tz = null;
            } else if (type == DateFormatSymbols.TIMEZONE_SHORT_DAYLIGHT
                    || type == DateFormatSymbols.TIMEZONE_LONG_DAYLIGHT) {
                // daylight time
                // use the correct DST SAVINGS for the zone.
                // cal.set(UCAL_DST_OFFSET, tz->getDSTSavings());
                cal.set(Calendar.DST_OFFSET, millisPerHour);
                tz = null;
            } else {
                // either standard or daylight
                // need to finish getting the date, then compute dst offset as
                // appropriate
                parsedTimeZone = tz;
            }
            if(value != null) {
                return start + value.length();
            }
        }
        // complete failure
        return 0;
    }

    /**
     * Protected method that converts one field of the input string into a
     * numeric field value in <code>cal</code>.  Returns -start (for
     * ParsePosition) if failed.  Subclasses may override this method to
     * modify or add parsing capabilities.
     * @param text the time text to be parsed.
     * @param start where to start parsing.
     * @param ch the pattern character for the date field text to be parsed.
     * @param count the count of a pattern character.
     * @param obeyCount if true, then the next field directly abuts this one,
     * and we should use the count to know when to stop parsing.
     * @param ambiguousYear return parameter; upon return, if ambiguousYear[0]
     * is true, then a two-digit year was parsed and may need to be readjusted.
     * @return the new start position if matching succeeded; a negative
     * number indicating matching failure, otherwise.  As a side effect,
     * set the appropriate field of <code>cal</code> with the parsed
     * value.
     * @stable ICU 2.0
     */
    protected int subParse(String text, int start, char ch, int count,
                           boolean obeyCount, boolean allowNegative,
                           boolean[] ambiguousYear, Calendar cal)
    {
        Number number = null;
        int value = 0;
        int i;
        ParsePosition pos = new ParsePosition(0);
        //int patternCharIndex = DateFormatSymbols.patternChars.indexOf(ch);c
        int patternCharIndex = -1;
        if ('A' <= ch && ch <= 'z') {
            patternCharIndex = PATTERN_CHAR_TO_INDEX[(int)ch - PATTERN_CHAR_BASE];
        }

        if (patternCharIndex == -1) {
            return -start;
        }

        int field = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];

        // If there are any spaces here, skip over them.  If we hit the end
        // of the string, then fail.
        for (;;) {
            if (start >= text.length()) {
                return -start;
            }
            int c = UTF16.charAt(text, start);
            if (!UCharacter.isUWhiteSpace(c)) {
                break;
            }
            start += UTF16.getCharCount(c);
        }
        pos.setIndex(start);

        // We handle a few special cases here where we need to parse
        // a number value.  We handle further, more generic cases below.  We need
        // to handle some of them here because some fields require extra processing on
        // the parsed value.
        if (patternCharIndex == 4 /*HOUR_OF_DAY1_FIELD*/ ||
            patternCharIndex == 15 /*HOUR1_FIELD*/ ||
            (patternCharIndex == 2 /*MONTH_FIELD*/ && count <= 2) ||
            patternCharIndex == 1 ||
            patternCharIndex == 8)
            {
                // It would be good to unify this with the obeyCount logic below,
                // but that's going to be difficult.
                if (obeyCount)
                    {
                        if ((start+count) > text.length()) return -start;
                        number = parseInt(text.substring(0, start+count), pos, allowNegative);
                    }
                else number = parseInt(text, pos, allowNegative);
                if (number == null)
                    return -start;
                value = number.intValue();
            }

        switch (patternCharIndex)
            {
            case 0: // 'G' - ERA
                if (count == 4) {
                    return matchString(text, start, Calendar.ERA, formatData.eraNames, cal);
                } else {
                    return matchString(text, start, Calendar.ERA, formatData.eras, cal);
                }
            case 1: // 'y' - YEAR
                // If there are 3 or more YEAR pattern characters, this indicates
                // that the year value is to be treated literally, without any
                // two-digit year adjustments (e.g., from "01" to 2001).  Otherwise
                // we made adjustments to place the 2-digit year in the proper
                // century, for parsed strings from "00" to "99".  Any other string
                // is treated literally:  "2250", "-1", "1", "002".
                /* 'yy' is the only special case, 'y' is interpreted as number. [Richard/GCL]*/
                if (count == 2 && (pos.getIndex() - start) == 2
                    && Character.isDigit(text.charAt(start))
                    && Character.isDigit(text.charAt(start+1)))
                    {
                        // Assume for example that the defaultCenturyStart is 6/18/1903.
                        // This means that two-digit years will be forced into the range
                        // 6/18/1903 to 6/17/2003.  As a result, years 00, 01, and 02
                        // correspond to 2000, 2001, and 2002.  Years 04, 05, etc. correspond
                        // to 1904, 1905, etc.  If the year is 03, then it is 2003 if the
                        // other fields specify a date before 6/18, or 1903 if they specify a
                        // date afterwards.  As a result, 03 is an ambiguous year.  All other
                        // two-digit years are unambiguous.
                        int ambiguousTwoDigitYear = getDefaultCenturyStartYear() % 100;
                        ambiguousYear[0] = value == ambiguousTwoDigitYear;
                        value += (getDefaultCenturyStartYear()/100)*100 +
                            (value < ambiguousTwoDigitYear ? 100 : 0);
                    }
                cal.set(Calendar.YEAR, value);
                return pos.getIndex();
            case 2: // 'M' - MONTH
                if (count <= 2) // i.e., M or MM.
                    {
                        // Don't want to parse the month if it is a string
                        // while pattern uses numeric style: M or MM.
                        // [We computed 'value' above.]
                        cal.set(Calendar.MONTH, value - 1);
                        return pos.getIndex();
                    }
                else
                    {
                        // count >= 3 // i.e., MMM or MMMM
                        // Want to be able to parse both short and long forms.
                        // Try count == 4 first:
                        int newStart = matchString(text, start, Calendar.MONTH,
                                                   formatData.months, cal);
                        if (newStart > 0) {
                            return newStart;
                        } else { // count == 4 failed, now try count == 3
                            return matchString(text, start, Calendar.MONTH,
                                               formatData.shortMonths, cal);
                        }
                    }
            case 26: // 'L' - STAND_ALONE_MONTH
                if (count <= 2) // i.e., M or MM.
                    {
                        // Don't want to parse the month if it is a string
                        // while pattern uses numeric style: M or MM.
                        // [We computed 'value' above.]
                        cal.set(Calendar.MONTH, value - 1);
                        return pos.getIndex();
                    }
                else
                    {
                        // count >= 3 // i.e., MMM or MMMM
                        // Want to be able to parse both short and long forms.
                        // Try count == 4 first:
                        int newStart = matchString(text, start, Calendar.MONTH,
                                                   formatData.standaloneMonths, cal);
                        if (newStart > 0) {
                            return newStart;
                        } else { // count == 4 failed, now try count == 3
                            return matchString(text, start, Calendar.MONTH,
                                               formatData.standaloneShortMonths, cal);
                        }
                    }
            case 4: // 'k' - HOUR_OF_DAY (1..24)
                // [We computed 'value' above.]
                if (value == cal.getMaximum(Calendar.HOUR_OF_DAY)+1) value = 0;
                cal.set(Calendar.HOUR_OF_DAY, value);
                return pos.getIndex();
            case 8: // 'S' - FRACTIONAL_SECOND
                // Fractional seconds left-justify
                i = pos.getIndex() - start;
                if (i < 3) {
                    while (i < 3) {
                        value *= 10;
                        i++;
                    }
                } else {
                    int a = 1;
                    while (i > 3) {
                        a *= 10;
                        i--;
                    }
                    value = (value + (a>>1)) / a;
                }
                cal.set(Calendar.MILLISECOND, value);
                return pos.getIndex();
            case 9: { // 'E' - DAY_OF_WEEK
                // Want to be able to parse both short and long forms.
                // Try count == 4 (EEEE) first:
                int newStart = matchString(text, start, Calendar.DAY_OF_WEEK,
                                           formatData.weekdays, cal);
                if (newStart > 0) {
                    return newStart;
                } else { // EEEE failed, now try EEE
                    return matchString(text, start, Calendar.DAY_OF_WEEK,
                                       formatData.shortWeekdays, cal);
                }
            }
            case 25: { // 'c' - STAND_ALONE_DAY_OF_WEEK
                // Want to be able to parse both short and long forms.
                // Try count == 4 (cccc) first:
                int newStart = matchString(text, start, Calendar.DAY_OF_WEEK,
                                           formatData.standaloneWeekdays, cal);
                if (newStart > 0) {
                    return newStart;
                } else { // cccc failed, now try ccc
                    return matchString(text, start, Calendar.DAY_OF_WEEK,
                                       formatData.standaloneShortWeekdays, cal);
                }
            }
            case 14: // 'a' - AM_PM
                return matchString(text, start, Calendar.AM_PM, formatData.ampms, cal);
            case 15: // 'h' - HOUR (1..12)
                // [We computed 'value' above.]
                if (value == cal.getLeastMaximum(Calendar.HOUR)+1) value = 0;
                cal.set(Calendar.HOUR, value);
                return pos.getIndex();
            case 17: // 'z' - ZONE_OFFSET
            case 23: // 'Z' - TIMEZONE_RFC
            case 24: // 'v' - TIMEZONE_GENERIC
                // First try to parse generic forms such as GMT-07:00. Do this first
                // in case localized DateFormatZoneData contains the string "GMT"
                // for a zone; in that case, we don't want to match the first three
                // characters of GMT+/-HH:MM etc.
                {
                    int sign = 0;
                    int offset;

                    // For time zones that have no known names, look for strings
                    // of the form:
                    //    GMT[+-]hours:minutes or
                    //    GMT[+-]hhmm or
                    //    GMT.
                    if ((text.length() - start) >= GMT.length() &&
                        text.regionMatches(true, start, GMT, 0, GMT.length()))
                        {
                            cal.set(Calendar.DST_OFFSET, 0);

                            pos.setIndex(start + GMT.length());

                            try { // try-catch for "GMT" only time zone string
                                switch (text.charAt(pos.getIndex())) {
                                case '+':
                                    sign = 1;
                                    break;
                                case '-':
                                    sign = -1;
                                    break;
                                }
                            } catch(StringIndexOutOfBoundsException e) {
                            }
                            if (sign == 0) {
                                cal.set(Calendar.ZONE_OFFSET, 0 );
                                return pos.getIndex();
                            }

                            // Look for hours:minutes or hhmm.
                            pos.setIndex(pos.getIndex() + 1);
                            int st = pos.getIndex();
                            Number tzNumber = numberFormat.parse(text, pos);
                            if( tzNumber == null) {
                                return -start;
                            }
                            if( pos.getIndex() < text.length() &&
                                text.charAt(pos.getIndex()) == ':' ) {

                                // This is the hours:minutes case
                                offset = tzNumber.intValue() * 60;
                                pos.setIndex(pos.getIndex() + 1);
                                tzNumber = numberFormat.parse(text, pos);
                                if( tzNumber == null) {
                                    return -start;
                                }
                                offset += tzNumber.intValue();
                            }
                            else {
                                // This is the hhmm case.
                                offset = tzNumber.intValue();
                                // Assume "-23".."+23" refers to hours.
                                if( offset < 24 && (pos.getIndex() - st) <= 2)
                                    offset *= 60;
                                else
                                    // todo: this looks questionable, should have more error checking
                                    offset = offset % 100 + offset / 100 * 60;
                            }

                            // Fall through for final processing below of 'offset' and 'sign'.
                        }
                    else {
                        // At this point, check for named time zones by looking through
                        // the locale data from the DateFormatZoneData strings.
                        // Want to be able to parse both short and long forms.
                        i = subParseZoneString(text, start, cal);
                        if (i != 0)
                            return i;

                        // As a last resort, look for numeric timezones of the form
                        // [+-]hhmm as specified by RFC 822.  This code is actually
                        // a little more permissive than RFC 822.  It will try to do
                        // its best with numbers that aren't strictly 4 digits long.
                        DecimalFormat fmt = new DecimalFormat("+####;-####");
                        fmt.setParseIntegerOnly(true);
                        Number tzNumber = fmt.parse( text, pos );
                        if( tzNumber == null) {
                            return -start;   // Wasn't actually a number.
                        }
                        offset = tzNumber.intValue();
                        sign = 1;
                        if( offset < 0 ) {
                            sign = -1;
                            offset = -offset;
                        }
                        // Assume "-23".."+23" refers to hours. Length includes sign.
                        if( offset < 24 && (pos.getIndex() - start) <= 3)
                            offset = offset * 60;
                        else
                            offset = offset % 100 + offset / 100 * 60;

                        // Fall through for final processing below of 'offset' and 'sign'.
                    }

                    // Do the final processing for both of the above cases.  We only
                    // arrive here if the form GMT+/-... or an RFC 822 form was seen.

                    // assert (sign != 0) : sign; // enable when guaranteed JDK >= 1.4
                    offset *= millisPerMinute * sign;

                    if (cal.getTimeZone().useDaylightTime())
                        {
                            cal.set(Calendar.DST_OFFSET, millisPerHour);
                            offset -= millisPerHour;
                        }
                    cal.set(Calendar.ZONE_OFFSET, offset);

                    return pos.getIndex();
                }
                
            case 27: // 'Q' - QUARTER
                if (count <= 2) // i.e., Q or QQ.
                {
                    // Don't want to parse the quarter if it is a string
                    // while pattern uses numeric style: Q or QQ.
                    // [We computed 'value' above.]
                    cal.set(Calendar.MONTH, (value - 1) * 3);
                    return pos.getIndex();
                }
            else
                {
                    // count >= 3 // i.e., QQQ or QQQQ
                    // Want to be able to parse both short and long forms.
                    // Try count == 4 first:
                    int newStart = matchQuarterString(text, start, Calendar.MONTH,
                                               formatData.quarters, cal);
                    if (newStart > 0) {
                        return newStart;
                    } else { // count == 4 failed, now try count == 3
                        return matchQuarterString(text, start, Calendar.MONTH,
                                           formatData.shortQuarters, cal);
                    }
                }
                
            case 28: // 'q' - STANDALONE QUARTER
                if (count <= 2) // i.e., q or qq.
                {
                    // Don't want to parse the quarter if it is a string
                    // while pattern uses numeric style: q or qq.
                    // [We computed 'value' above.]
                    cal.set(Calendar.MONTH, (value - 1) * 3);
                    return pos.getIndex();
                }
            else
                {
                    // count >= 3 // i.e., qqq or qqqq
                    // Want to be able to parse both short and long forms.
                    // Try count == 4 first:
                    int newStart = matchQuarterString(text, start, Calendar.MONTH,
                                               formatData.standaloneQuarters, cal);
                    if (newStart > 0) {
                        return newStart;
                    } else { // count == 4 failed, now try count == 3
                        return matchQuarterString(text, start, Calendar.MONTH,
                                           formatData.standaloneShortQuarters, cal);
                    }
                }

            default:
                // case 3: // 'd' - DATE
                // case 5: // 'H' - HOUR_OF_DAY (0..23)
                // case 6: // 'm' - MINUTE
                // case 7: // 's' - SECOND
                // case 10: // 'D' - DAY_OF_YEAR
                // case 11: // 'F' - DAY_OF_WEEK_IN_MONTH
                // case 12: // 'w' - WEEK_OF_YEAR
                // case 13: // 'W' - WEEK_OF_MONTH
                // case 16: // 'K' - HOUR (0..11)
                // case 18: // 'Y' - YEAR_WOY
                // case 19: // 'e' - DOW_LOCAL
                // case 20: // 'u' - EXTENDED_YEAR
                // case 21: // 'g' - JULIAN_DAY
                // case 22: // 'A' - MILLISECONDS_IN_DAY

                // Handle "generic" fields
                if (obeyCount)
                    {
                        if ((start+count) > text.length()) return -start;
                        number = parseInt(text.substring(0, start+count), pos, allowNegative);
                    }
                else number = parseInt(text, pos, allowNegative);
                if (number != null) {
                    cal.set(field, number.intValue());
                    return pos.getIndex();
                }
                return -start;
            }
    }

    /**
     * Parse an integer using fNumberFormat.  This method is semantically
     * const, but actually may modify fNumberFormat.
     */
    private Number parseInt(String text,
                            ParsePosition pos,
                            boolean allowNegative) {
        Number number;        
        if (allowNegative) {
            number = numberFormat.parse(text, pos);
        } else {
            // Invalidate negative numbers
            if (numberFormat instanceof DecimalFormat) {
                String oldPrefix = ((DecimalFormat)numberFormat).getNegativePrefix();
                ((DecimalFormat)numberFormat).setNegativePrefix(SUPPRESS_NEGATIVE_PREFIX);
                number = numberFormat.parse(text, pos);
                ((DecimalFormat)numberFormat).setNegativePrefix(oldPrefix);
            } else {
                boolean dateNumberFormat = (numberFormat instanceof DateNumberFormat);
                if (dateNumberFormat) {
                    ((DateNumberFormat)numberFormat).setParsePositiveOnly(true);                    
                }
                number = numberFormat.parse(text, pos);                
                if (dateNumberFormat) {
                    ((DateNumberFormat)numberFormat).setParsePositiveOnly(false);                    
                }
            }
        }
        return number;
    }

    /**
     * Translate a pattern, mapping each character in the from string to the
     * corresponding character in the to string.
     */
    private String translatePattern(String pat, String from, String to) {
        StringBuffer result = new StringBuffer();
        boolean inQuote = false;
        for (int i = 0; i < pat.length(); ++i) {
            char c = pat.charAt(i);
            if (inQuote) {
                if (c == '\'')
                    inQuote = false;
            }
            else {
                if (c == '\'')
                    inQuote = true;
                else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                    int ci = from.indexOf(c);
                    if (ci != -1) {
                        c = to.charAt(ci);
                    }
                    // do not worry on translatepattern if the character is not listed
                    // we do the validity check elsewhere
                }
            }
            result.append(c);
        }
        if (inQuote)
            throw new IllegalArgumentException("Unfinished quote in pattern");
        return result.toString();
    }

    /**
     * Return a pattern string describing this date format.
     * @stable ICU 2.0
     */
    public String toPattern() {
        return pattern;
    }

    /**
     * Return a localized pattern string describing this date format.
     * @stable ICU 2.0
     */
    public String toLocalizedPattern() {
        return translatePattern(pattern,
                                DateFormatSymbols.patternChars,
                                formatData.localPatternChars);
    }

    /**
     * Apply the given unlocalized pattern string to this date format.
     * @stable ICU 2.0
     */
    public void applyPattern(String pat)
    {
        this.pattern = pat;
        setLocale(null, null);
        // reset parsed pattern items
        patternItems = null;
    }

    /**
     * Apply the given localized pattern string to this date format.
     * @stable ICU 2.0
     */
    public void applyLocalizedPattern(String pat) {
        this.pattern = translatePattern(pat,
                                        formatData.localPatternChars,
                                        DateFormatSymbols.patternChars);
        setLocale(null, null);
    }

    /**
     * Gets the date/time formatting data.
     * @return a copy of the date-time formatting data associated
     * with this date-time formatter.
     * @stable ICU 2.0
     */
    public DateFormatSymbols getDateFormatSymbols()
    {
        return (DateFormatSymbols)formatData.clone();
    }

    /**
     * Allows you to set the date/time formatting data.
     * @param newFormatSymbols the new symbols
     * @stable ICU 2.0
     */
    public void setDateFormatSymbols(DateFormatSymbols newFormatSymbols)
    {
        this.formatData = (DateFormatSymbols)newFormatSymbols.clone();
    }

    /**
     * Method for subclasses to access the DateFormatSymbols.
     * @stable ICU 2.0
     */
    protected DateFormatSymbols getSymbols() {
        return formatData;
    }

    /**
     * Overrides Cloneable
     * @stable ICU 2.0
     */
    public Object clone() {
        SimpleDateFormat other = (SimpleDateFormat) super.clone();
        other.formatData = (DateFormatSymbols) formatData.clone();
        return other;
    }

    /**
     * Override hashCode.
     * Generates the hash code for the SimpleDateFormat object
     * @stable ICU 2.0
     */
    public int hashCode()
    {
        return pattern.hashCode();
        // just enough fields for a reasonable distribution
    }

    /**
     * Override equals.
     * @stable ICU 2.0
     */
    public boolean equals(Object obj)
    {
        if (!super.equals(obj)) return false; // super does class check
        SimpleDateFormat that = (SimpleDateFormat) obj;
        return (pattern.equals(that.pattern)
                && formatData.equals(that.formatData));
    }

    /**
     * Override writeObject.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException{
        if (defaultCenturyStart == null) {
            // if defaultCenturyStart is not yet initialized,
            // calculate and set value before serialization.
            initializeDefaultCenturyStart(defaultCenturyBase);
        }
        stream.defaultWriteObject();
    }
    
    /**
     * Override readObject.
     */
    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        ///CLOVER:OFF
        // don't have old serial data to test with
        if (serialVersionOnStream < 1) {
            // didn't have defaultCenturyStart field
            defaultCenturyBase = System.currentTimeMillis();
        }
        ///CLOVER:ON
        else {
            // fill in dependent transient field
            parseAmbiguousDatesAsAfter(defaultCenturyStart);
        }
        serialVersionOnStream = currentSerialVersion;
        locale = getLocale(ULocale.VALID_LOCALE);

        initLocalZeroPaddingNumberFormat();
    }

//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
    /**
     * Format the object to an attributed string, and return the corresponding iterator
     * Overrides superclass method.
     * 
     * @param obj The object to format
     * @return <code>AttributedCharacterIterator</code> describing the formatted value.
     * 
     * @stable ICU 3.8
     */
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        Calendar cal = calendar;
        if (obj instanceof Calendar) {
            cal = (Calendar)obj;
        } else if (obj instanceof Date) {
            calendar.setTime((Date)obj);
        } else if (obj instanceof Number) {
            calendar.setTimeInMillis(((Number)obj).longValue());
        } else { 
            throw new IllegalArgumentException("Cannot format given Object as a Date");
        }
        StringBuffer toAppendTo = new StringBuffer();
        FieldPosition pos = new FieldPosition(0);
        List attributes = new LinkedList();
        format(cal, toAppendTo, pos, attributes);

        AttributedString as = new AttributedString(toAppendTo.toString());
        
        // add DateFormat field attributes to the AttributedString
        for (int i = 0; i < attributes.size(); i++) {
            FieldPosition fp = (FieldPosition) attributes.get(i);
            Format.Field attribute = fp.getFieldAttribute();
            as.addAttribute(attribute, attribute, fp.getBeginIndex(), fp.getEndIndex());
        }
        // return the CharacterIterator from AttributedString
        return as.getIterator();
    }
//#endif
}

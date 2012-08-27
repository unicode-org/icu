/*
 *******************************************************************************
 * Copyright (C) 1996-2012, International Business Machines Corporation and    *
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
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import com.ibm.icu.impl.CalendarData;
import com.ibm.icu.impl.DateNumberFormat;
import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.PatternProps;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.TimeZoneFormat.Style;
import com.ibm.icu.text.TimeZoneFormat.TimeType;
import com.ibm.icu.util.BasicTimeZone;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneTransition;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Category;


/**
 * {@icuenhanced java.text.SimpleDateFormat}.{@icu _usage_}
 *
 * <p><code>SimpleDateFormat</code> is a concrete class for formatting and
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
 * U*       cyclic year name        (Text,NumFallback)  ren-chen (29)
 * M        month in year           (Text & Number)     July & 07
 * d        day in month            (Number)            10
 * h        hour in am/pm (1~12)    (Number)            12
 * H        hour in day (0~23)      (Number)            0
 * m        minute in hour          (Number)            30
 * s        second in minute        (Number)            55
 * S        fractional second       (Number)            978
 * E        day of week             (Text)              Tuesday
 * e*       day of week (local 1~7) (Text & Number)     Tuesday & 2
 * D        day in year             (Number)            189
 * F        day of week in month    (Number)            2 (2nd Wed in July)
 * w        week in year            (Number)            27
 * W        week in month           (Number)            2
 * a        am/pm marker            (Text)              PM
 * k        hour in day (1~24)      (Number)            24
 * K        hour in am/pm (0~11)    (Number)            0
 * z        time zone               (Text)              PST
 * zzzz     time zone               (Text)              Pacific Standard Time
 * Z        time zone (RFC 822)     (Number)            -0800
 * ZZZZ     time zone (GMT offset)  (Text & Number)     GMT-08:00
 * ZZZZZ    time zone (ISO 8601)    (Text & Number)     -08:00 & Z (UTC)
 * v        time zone (generic)     (Text)              PT
 * vvvv     time zone (generic)     (Text)              Pacific Time
 * V        time zone (abreviation) (Text)              PST
 * VVVV     time zone (location)    (Text)              United States Time (Los Angeles)
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
 * <strong>(Text,NumFallback)</strong>: Behaves like Text if there is supporting
 * data, like Number otherwise.
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
 * <p>When parsing a date string using the abbreviated year pattern ("yy"),
 * SimpleDateFormat must interpret the abbreviated year
 * relative to some century.  It does this by adjusting dates to be
 * within 80 years before and 20 years after the time the SimpleDateFormat
 * instance is created. For example, using a pattern of "MM/dd/yy" and a
 * SimpleDateFormat instance created on Jan 1, 1997,  the string
 * "01/11/12" would be interpreted as Jan 11, 2012 while the string "05/04/64"
 * would be interpreted as May 4, 1964.
 * During parsing, only strings consisting of exactly two digits, as defined by
 * {@link com.ibm.icu.lang.UCharacter#isDigit(int)}, will be parsed into the default
 * century.
 * Any other numeric string, such as a one digit string, a three or more digit
 * string, or a two digit string that isn't all digits (for example, "-1"), is
 * interpreted literally.  So "01/02/3" or "01/02/003" are parsed, using the
 * same pattern, as Jan 2, 3 AD.  Likewise, "01/02/-3" is parsed as Jan 2, 4 BC.
 *
 * <p>If the year pattern does not have exactly two 'y' characters, the year is
 * interpreted literally, regardless of the number of digits.  So using the
 * pattern "MM/dd/yyyy", "01/11/12" parses to Jan 11, 12 A.D.
 *
 * <p>When numeric fields abut one another directly, with no intervening delimiter
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
 * <p>For time zones that have no names, use strings GMT+hours:minutes or
 * GMT-hours:minutes.
 *
 * <p>The calendar defines what is the first day of the week, the first week
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

    static boolean DelayedHebrewMonthCheck = false;

    /*
     * From calendar field to its level.
     * Used to order calendar field.
     * For example, calendar fields can be defined in the following order:
     * year >  month > date > am-pm > hour >  minute
     * YEAR --> 10, MONTH -->20, DATE --> 30;
     * AM_PM -->40, HOUR --> 50, MINUTE -->60
     */
    private static final int[] CALENDAR_FIELD_TO_LEVEL =
    {
        /*GyM*/ 0, 10, 20,
        /*wW*/ 20, 30,
        /*dDEF*/ 30, 20, 30, 30,
        /*ahHm*/ 40, 50, 50, 60,
        /*sS..*/ 70, 80,
        /*z?Y*/ 0, 0, 10,
        /*eug*/ 30, 10, 0,
        /*A*/ 40
    };



    /*
     * From calendar field letter to its level.
     * Used to order calendar field.
     * For example, calendar fields can be defined in the following order:
     * year >  month > date > am-pm > hour >  minute
     * 'y' --> 10, 'M' -->20, 'd' --> 30; 'a' -->40, 'h' --> 50, 'm' -->60
     */
    private static final int[] PATTERN_CHAR_TO_LEVEL =
    {
    //       A   B   C   D    E   F   G    H   I   J   K   L    M   N   O
        -1, 40, -1, -1, 20,  30, 30,  0,  50, -1, -1, 50, 20,  20, -1, -1,
    //   P   Q   R    S   T   U  V   W   X   Y  Z
        -1, 20, -1,  80, -1, 10, 0, 30, -1, 10, 0, -1, -1, -1, -1, -1,
    //       a   b   c   d    e   f  g   h   i   j    k   l    m   n   o
        -1, 40, -1, 30,  30, 30, -1, 0, 50, -1, -1,  50, -1,  60, -1, -1,
    //   p   q   r    s   t   u  v   w   x    y  z
        -1, 20, -1,  70, -1, 10, 0, 20, -1,  10, 0, -1, -1, -1, -1, -1
    };

    // When calendar uses hebr numbering (i.e. he@calendar=hebrew),
    // offset the years within the current millenium down to 1-999
    private static final int HEBREW_CAL_CUR_MILLENIUM_START_YEAR = 5000;
    private static final int HEBREW_CAL_CUR_MILLENIUM_END_YEAR = 6000;

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
     * The override string of this formatter.  Used to override the
     * numbering system for one or more fields.
     * @serial
     */
    private String override;

    /**
     * The hash map used for number format overrides.
     * @serial
     */
    private HashMap<String, NumberFormat> numberFormatters;

    /**
     * The hash map used for number format overrides.
     * @serial
     */
    private HashMap<Character, String> overrideMap;

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

    // We need to preserve time zone type when parsing specific
    // time zone text (xxx Standard Time vs xxx Daylight Time)
    private transient TimeType tztype = TimeType.UNKNOWN;

    private static final int millisPerHour = 60 * 60 * 1000;

    // When possessing ISO format, the ERA may be ommitted is the
    // year specifier is a negative number.
    private static final int ISOSpecialEra = -32000;
    
    // This prefix is designed to NEVER MATCH real text, in order to
    // suppress the parsing of negative numbers.  Adjust as needed (if
    // this becomes valid Unicode).
    private static final String SUPPRESS_NEGATIVE_PREFIX = "\uAB00";

    /**
     * If true, this object supports fast formatting using the
     * subFormat variant that takes a StringBuffer.
     */
    private transient boolean useFastFormat;

    /*
     *  The time zone sub-formatter, introduced in ICU 4.8
     */
    private volatile TimeZoneFormat tzFormat;

    /*
     *  Default capitalization context, introduced in ICU 49
     */
    private ContextValue defaultCapitalizationContext;

    /**
     * Date format context types
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public enum ContextType {
        /**
         * Type (key) for specifying the capitalization context for which a date
         * is to be formatted (possible values are in ContextValue).
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        CAPITALIZATION
    }
    
    /**
     * Values for date format context types
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public enum ContextValue {
        /** Values for any ContextType (key) */
        /**
         * Value for any ContextType (such as CAPITALIZATION) if the
         * relevant context to be used in formatting a date is unknown (this is the
         * default value for any ContextType when no value has been
         * explicitly specified for that ContextType).
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        UNKNOWN,
        /** Values for context type (key) CAPITALIZATION */
        /**
         * CAPITALIZATION value if a date (or date symbol) is to be formatted
         * with capitalization appropriate for the middle of a sentence.
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE,
        /**
         * CAPITALIZATION value if a date (or date symbol) is to be formatted
         * with capitalization appropriate for the beginning of a sentence.
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE,
        /**
         * CAPITALIZATION value if a date (or date symbol) is to be formatted
         * with capitalization appropriate for a user-interface list or menu item.
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        CAPITALIZATION_FOR_UI_LIST_OR_MENU,
        /**
         * CAPITALIZATION value if a date (or date symbol) is to be formatted
         * with capitalization appropriate for stand-alone usage such as an
         * isolated name on a calendar page.
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        CAPITALIZATION_FOR_STANDALONE
    }

    /**
     * Constructs a SimpleDateFormat using the default pattern for the default <code>FORMAT</code>
     * locale.  <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     *
     * @see DateFormat
     * @see Category#FORMAT
     * @stable ICU 2.0
     */
    public SimpleDateFormat() {
        this(getDefaultPattern(), null, null, null, null, true, null);
    }

    /**
     * Constructs a SimpleDateFormat using the given pattern in the default <code>FORMAT</code>
     * locale.  <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     * @see Category#FORMAT
     * @stable ICU 2.0
     */
    public SimpleDateFormat(String pattern)
    {
        this(pattern, null, null, null, null, true, null);
    }

    /**
     * Constructs a SimpleDateFormat using the given pattern and locale.
     * <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     * @stable ICU 2.0
     */
    public SimpleDateFormat(String pattern, Locale loc)
    {
        this(pattern, null, null, null, ULocale.forLocale(loc), true, null);
    }

    /**
     * Constructs a SimpleDateFormat using the given pattern and locale.
     * <b>Note:</b> Not all locales support SimpleDateFormat; for full
     * generality, use the factory methods in the DateFormat class.
     * @stable ICU 3.2
     */
    public SimpleDateFormat(String pattern, ULocale loc)
    {
        this(pattern, null, null, null, loc, true, null);
    }

    /**
     * Constructs a SimpleDateFormat using the given pattern , override and locale.
     * @param pattern The pattern to be used
     * @param override The override string.  A numbering system override string can take one of the following forms:
     *     1). If just a numbering system name is specified, it applies to all numeric fields in the date format pattern.
     *     2). To specify an alternate numbering system on a field by field basis, use the field letters from the pattern
     *         followed by an = sign, followed by the numbering system name.  For example, to specify that just the year
     *         be formatted using Hebrew digits, use the override "y=hebr".  Multiple overrides can be specified in a single
     *         string by separating them with a semi-colon. For example, the override string "m=thai;y=deva" would format using
     *         Thai digits for the month and Devanagari digits for the year.
     * @param loc The locale to be used
     * @stable ICU 4.2
     */
    public SimpleDateFormat(String pattern, String override, ULocale loc)
    {
        this(pattern, null, null, null, loc, false,override);
    }

    /**
     * Constructs a SimpleDateFormat using the given pattern and
     * locale-specific symbol data.
     * Warning: uses default <code>FORMAT</code> locale for digits!
     * @stable ICU 2.0
     */
    public SimpleDateFormat(String pattern, DateFormatSymbols formatData)
    {
        this(pattern, (DateFormatSymbols)formatData.clone(), null, null, null, true, null);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public SimpleDateFormat(String pattern, DateFormatSymbols formatData, ULocale loc)
    {
        this(pattern, (DateFormatSymbols)formatData.clone(), null, null, loc, true,null);
    }

    /**
     * Package-private constructor that allows a subclass to specify
     * whether it supports fast formatting.
     *
     * TODO make this API public.
     */
    SimpleDateFormat(String pattern, DateFormatSymbols formatData, Calendar calendar, ULocale locale,
                     boolean useFastFormat, String override) {
        this(pattern, (DateFormatSymbols)formatData.clone(), (Calendar)calendar.clone(), null, locale, useFastFormat,override);
    }

    /*
     * The constructor called from all other SimpleDateFormat constructors
     */
    private SimpleDateFormat(String pattern, DateFormatSymbols formatData, Calendar calendar,
            NumberFormat numberFormat, ULocale locale, boolean useFastFormat,String override) {
        this.pattern = pattern;
        this.formatData = formatData;
        this.calendar = calendar;
        this.numberFormat = numberFormat;
        this.locale = locale; // time zone formatting
        this.useFastFormat = useFastFormat;
        this.override = override;
        initialize();
    }

    /**
     * Creates an instance of SimpleDateFormat for the given format configuration
     * @param formatConfig the format configuration
     * @return A SimpleDateFormat instance
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static SimpleDateFormat getInstance(Calendar.FormatConfiguration formatConfig) {

        String ostr = formatConfig.getOverrideString();
        boolean useFast = ( ostr != null && ostr.length() > 0 );

        return new SimpleDateFormat(formatConfig.getPatternString(),
                    formatConfig.getDateFormatSymbols(),
                    formatConfig.getCalendar(),
                    null,
                    formatConfig.getLocale(),
                    useFast,
                    formatConfig.getOverrideString());
    }

    /*
     * Initialized fields
     */
    private void initialize() {
        if (locale == null) {
            locale = ULocale.getDefault(Category.FORMAT);
        }
        if (formatData == null) {
            formatData = new DateFormatSymbols(locale);
        }
        if (calendar == null) {
            calendar = Calendar.getInstance(locale);
        }
        if (numberFormat == null) {
            NumberingSystem ns = NumberingSystem.getInstance(locale);
            if (ns.isAlgorithmic()) {
                numberFormat = NumberFormat.getInstance(locale);
            } else {
                String digitString = ns.getDescription();
                String nsName = ns.getName();
                // Use a NumberFormat optimized for date formatting
                numberFormat = new DateNumberFormat(locale, digitString, nsName);
            }
        }
        // Note: deferring calendar calculation until when we really need it.
        // Instead, we just record time of construction for backward compatibility.
        defaultCenturyBase = System.currentTimeMillis();

        setLocale(calendar.getLocale(ULocale.VALID_LOCALE ), calendar.getLocale(ULocale.ACTUAL_LOCALE));
        initLocalZeroPaddingNumberFormat();

        if (override != null) {
           initNumberFormatters(locale);
        }
        
        defaultCapitalizationContext = ContextValue.UNKNOWN;

    }

    /**
     * Private method lazily instantiate the TimeZoneFormat field
     * @param bForceUpdate when true, check if tzFormat is synchronized with
     * the current numberFormat and update its digits if necessary. When false,
     * this check is skipped.
     */
    private synchronized void initializeTimeZoneFormat(boolean bForceUpdate) {
        if (bForceUpdate || tzFormat == null) {
            tzFormat = TimeZoneFormat.getInstance(locale);

            String digits = null;
            if (numberFormat instanceof DecimalFormat) {
                DecimalFormatSymbols decsym = ((DecimalFormat) numberFormat).getDecimalFormatSymbols();
                digits = new String(decsym.getDigits());
            } else if (numberFormat instanceof DateNumberFormat) {
                digits = new String(((DateNumberFormat)numberFormat).getDigits());
            }

            if (digits != null) {
                if (!tzFormat.getGMTOffsetDigits().equals(digits)) {
                    if (tzFormat.isFrozen()) {
                        tzFormat = tzFormat.cloneAsThawed();
                    }
                    tzFormat.setGMTOffsetDigits(digits);
                }
            }
        }
    }

    /**
     * Private method, returns non-null TimeZoneFormat.
     * @return the TimeZoneFormat used by this formatter.
     */
    private TimeZoneFormat tzFormat() {
        if (tzFormat == null) {
            initializeTimeZoneFormat(false);
        }
        return tzFormat;
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
        ULocale defaultLocale = ULocale.getDefault(Category.FORMAT);
        if (!defaultLocale.equals(cachedDefaultLocale)) {
            cachedDefaultLocale = defaultLocale;
            Calendar cal = Calendar.getInstance(cachedDefaultLocale);
            try {
                CalendarData calData = new CalendarData(cachedDefaultLocale, cal.getType());
                String[] dateTimePatterns = calData.getDateTimePatterns();
                int glueIndex = 8;
                if (dateTimePatterns.length >= 13)
                {
                    glueIndex += (SHORT + 1);
                }
                cachedDefaultPattern = MessageFormat.format(dateTimePatterns[glueIndex],
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
     * Formats a date or time, which is the standard millis
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
        return format(cal, null, toAppendTo, pos);
    }

    /**
     * Formats a date or time, which is the standard millis
     * since January 1, 1970, 00:00:00 GMT.
     * <p>Example: using the US locale:
     * "yyyy.MM.dd G 'at' HH:mm:ss zzz" ->> 1996.07.10 AD at 15:08:56 PDT
     * @param cal the calendar whose date-time value is to be formatted into a date-time string
     * @param contextValues a list of DateFormatContextTypes (e.g. CAPITALIZATION) and
     * corresponding DateFormatContextValues (e.g. CAPITALIZATION_FOR_MIDDLE_OF_SENTENCE)
     * which should override the formatter's default values just for this call (does not change
     * the default values). May be null, in which case the default values are used.
     * @param toAppendTo where the new date-time text is to be appended
     * @param pos the formatting position. On input: an alignment field,
     * if desired. On output: the offsets of the alignment field.
     * @return the formatted date-time string.
     * @see DateFormat
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public StringBuffer format(Calendar cal,
                               Map<ContextType,ContextValue> contextValues,
                               StringBuffer toAppendTo, FieldPosition pos) {
        ContextValue capitalizationContext = defaultCapitalizationContext;
        if (contextValues != null ) {
            ContextValue newCapContextValue = contextValues.get(ContextType.CAPITALIZATION);
            if (newCapContextValue != null) {
                capitalizationContext = newCapContextValue;
            }
        }
        TimeZone backupTZ = null;
        if (cal != calendar && !cal.getType().equals(calendar.getType())) {
            // Different calendar type
            // We use the time and time zone from the input calendar, but
            // do not use the input calendar for field calculation.
            calendar.setTimeInMillis(cal.getTimeInMillis());
            backupTZ = calendar.getTimeZone();
            calendar.setTimeZone(cal.getTimeZone());
            cal = calendar;
        }
        StringBuffer result = format(cal, capitalizationContext, toAppendTo, pos, null);
        if (backupTZ != null) {
            // Restore the original time zone
            calendar.setTimeZone(backupTZ);
        }
        return result;
    }

    // The actual method to format date. If List attributes is not null,
    // then attribute information will be recorded.
    private StringBuffer format(Calendar cal, ContextValue capitalizationContext,
            StringBuffer toAppendTo, FieldPosition pos, List<FieldPosition> attributes) {
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
                int start = 0;
                if (attributes != null) {
                    // Save the current length
                    start = toAppendTo.length();
                }
                if (useFastFormat) {
                    subFormat(toAppendTo, item.type, item.length, toAppendTo.length(),
                              i, capitalizationContext, pos, cal);
                } else {
                    toAppendTo.append(subFormat(item.type, item.length, toAppendTo.length(),
                                                i, capitalizationContext, pos, cal));
                }
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
        -1, 27, -1,  8, -1, 30, 29, 13, -1, 18, 23, -1, -1, -1, -1, -1,
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
        /*c*/   Calendar.DOW_LOCAL,
        /*L*/   Calendar.MONTH,
        /*Qq*/  Calendar.MONTH, Calendar.MONTH,
        /*V*/   Calendar.ZONE_OFFSET,
        /*U*/   Calendar.YEAR,
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
        /*U*/   DateFormat.YEAR_NAME_FIELD,
    };

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
        /*U*/   DateFormat.Field.YEAR,
    };

    /**
     * Returns a DateFormat.Field constant associated with the specified format pattern
     * character.
     *
     * @param ch The pattern character
     * @return DateFormat.Field associated with the pattern character
     *
     * @stable ICU 3.8
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

    /**
     * Formats a single field, given its pattern character.  Subclasses may
     * override this method in order to modify or add formatting
     * capabilities.
     * @param ch the pattern character
     * @param count the number of times ch is repeated in the pattern
     * @param beginOffset the offset of the output string at the start of
     * this field; used to set pos when appropriate
     * @param pos receives the position of a field, when appropriate
     * @param fmtData the symbols for this formatter
     * @stable ICU 2.0
     */
    protected String subFormat(char ch, int count, int beginOffset,
                               FieldPosition pos, DateFormatSymbols fmtData,
                               Calendar cal)
        throws IllegalArgumentException
    {
        // Note: formatData is ignored
        return subFormat(ch, count, beginOffset, 0, ContextValue.UNKNOWN, pos, cal);
    }

     /**
     * Formats a single field. This is the version called internally; it
     * adds fieldNum and capitalizationContext parameters.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected String subFormat(char ch, int count, int beginOffset,
                               int fieldNum, ContextValue capitalizationContext,
                               FieldPosition pos,
                               Calendar cal)
    {
        StringBuffer buf = new StringBuffer();
        subFormat(buf, ch, count, beginOffset, fieldNum, capitalizationContext, pos, cal);
        return buf.toString();
    }

   /**
     * Formats a single field; useFastFormat variant.  Reuses a
     * StringBuffer for results instead of creating a String on the
     * heap for each call.
     *
     * NOTE We don't really need the beginOffset parameter, EXCEPT for
     * the need to support the slow subFormat variant (above) which
     * has to pass it in to us.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @SuppressWarnings("fallthrough")
    protected void subFormat(StringBuffer buf,
                             char ch, int count, int beginOffset,
                             int fieldNum, ContextValue capitalizationContext,
                             FieldPosition pos,
                             Calendar cal) {

        final int maxIntCount = Integer.MAX_VALUE;
        final int bufstart = buf.length();
        TimeZone tz = cal.getTimeZone();
        long date = cal.getTimeInMillis();
        String result = null;
        
        // final int patternCharIndex = DateFormatSymbols.patternChars.indexOf(ch);
        int patternCharIndex = -1;
        if ('A' <= ch && ch <= 'z') {
            patternCharIndex = PATTERN_CHAR_TO_INDEX[(int)ch - PATTERN_CHAR_BASE];
        }

        if (patternCharIndex == -1) {
            if (ch == 'l') { // (SMALL LETTER L) deprecated placeholder for leap month marker, ignore
                return;
            } else {
                throw new IllegalArgumentException("Illegal pattern character " +
                                                   "'" + ch + "' in \"" +
                                                   pattern + '"');
            }
        }

        final int field = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
        int value = cal.get(field);

        NumberFormat currentNumberFormat = getNumberFormat(ch);
        DateFormatSymbols.CapitalizationContextUsage capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.OTHER;

        switch (patternCharIndex) {
        case 0: // 'G' - ERA
            if ( cal.getType().equals("chinese")) {
                // moved from ChineseDateFormat
                zeroPaddingNumber(currentNumberFormat, buf, value, 1, 9);
            } else {
                if (count == 5) {
                    safeAppend(formatData.narrowEras, value, buf);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.ERA_NARROW;
                } else if (count == 4) {
                    safeAppend(formatData.eraNames, value, buf);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.ERA_WIDE;
                } else {
                    safeAppend(formatData.eras, value, buf);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.ERA_ABBREV;
                }
            }
            break;
        case 30: // 'U' - YEAR_NAME_FIELD
            if (formatData.shortYearNames != null && value <= formatData.shortYearNames.length) {
                safeAppend(formatData.shortYearNames, value-1, buf);
                break;
            }
            // else fall through to numeric year handling, do not break here 
        case 1: // 'y' - YEAR
        case 18: // 'Y' - YEAR_WOY
            if ( override != null && (override.compareTo("hebr") == 0 || override.indexOf("y=hebr") >= 0) &&
                    value > HEBREW_CAL_CUR_MILLENIUM_START_YEAR && value < HEBREW_CAL_CUR_MILLENIUM_END_YEAR ) {
                value -= HEBREW_CAL_CUR_MILLENIUM_START_YEAR;
            }
            /* According to the specification, if the number of pattern letters ('y') is 2,
             * the year is truncated to 2 digits; otherwise it is interpreted as a number.
             * But the original code process 'y', 'yy', 'yyy' in the same way. and process
             * patterns with 4 or more than 4 'y' characters in the same way.
             * So I change the codes to meet the specification. [Richard/GCl]
             */
            if (count == 2) {
                zeroPaddingNumber(currentNumberFormat,buf, value, 2, 2); // clip 1996 to 96
            } else { //count = 1 or count > 2
                zeroPaddingNumber(currentNumberFormat,buf, value, count, maxIntCount);
            }
            break;
        case 2: // 'M' - MONTH
        case 26: // 'L' - STANDALONE MONTH
            if ( cal.getType().equals("hebrew")) {
                boolean isLeap = HebrewCalendar.isLeapYear(cal.get(Calendar.YEAR));
                if (isLeap && value == 6 && count >= 3 ) {
                    value = 13; // Show alternate form for Adar II in leap years in Hebrew calendar.
                }
                if (!isLeap && value >= 6 && count < 3 ) {
                    value--; // Adjust the month number down 1 in Hebrew non-leap years, i.e. Adar is 6, not 7.
                }
            }
            int isLeapMonth = (formatData.leapMonthPatterns != null && formatData.leapMonthPatterns.length >= DateFormatSymbols.DT_MONTH_PATTERN_COUNT)?
                     cal.get(Calendar.IS_LEAP_MONTH): 0;
            // should consolidate the next section by using arrays of pointers & counts for the right symbols...
            if (count == 5) {
                if (patternCharIndex == 2) {
                    safeAppendWithMonthPattern(formatData.narrowMonths, value, buf, (isLeapMonth!=0)? formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_FORMAT_NARROW]: null);
                } else {
                    safeAppendWithMonthPattern(formatData.standaloneNarrowMonths, value, buf, (isLeapMonth!=0)? formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_STANDALONE_NARROW]: null);
                }
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.MONTH_NARROW;
            } else if (count == 4) {
                if (patternCharIndex == 2) {
                    safeAppendWithMonthPattern(formatData.months, value, buf, (isLeapMonth!=0)? formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_FORMAT_WIDE]: null);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.MONTH_FORMAT;
                } else {
                    safeAppendWithMonthPattern(formatData.standaloneMonths, value, buf, (isLeapMonth!=0)? formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_STANDALONE_WIDE]: null);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.MONTH_STANDALONE;
                }
            } else if (count == 3) {
                if (patternCharIndex == 2) {
                    safeAppendWithMonthPattern(formatData.shortMonths, value, buf, (isLeapMonth!=0)? formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_FORMAT_ABBREV]: null);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.MONTH_FORMAT;
                } else {
                    safeAppendWithMonthPattern(formatData.standaloneShortMonths, value, buf, (isLeapMonth!=0)? formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_STANDALONE_ABBREV]: null);
                    capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.MONTH_STANDALONE;
                }
            } else {
                StringBuffer monthNumber = new StringBuffer();
                zeroPaddingNumber(currentNumberFormat, monthNumber, value+1, count, maxIntCount);
                String[] monthNumberStrings = new String[1];
                monthNumberStrings[0] = monthNumber.toString();
                safeAppendWithMonthPattern(monthNumberStrings, 0, buf, (isLeapMonth!=0)? formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_NUMERIC]: null);
            }
            break;
        case 4: // 'k' - HOUR_OF_DAY (1..24)
            if (value == 0) {
                zeroPaddingNumber(currentNumberFormat,buf,
                                  cal.getMaximum(Calendar.HOUR_OF_DAY)+1,
                                  count, maxIntCount);
            } else {
                zeroPaddingNumber(currentNumberFormat,buf, value, count, maxIntCount);
            }
            break;
        case 8: // 'S' - FRACTIONAL_SECOND
            // Fractional seconds left-justify
            {
                numberFormat.setMinimumIntegerDigits(Math.min(3, count));
                numberFormat.setMaximumIntegerDigits(maxIntCount);
                if (count == 1) {
                    value /= 100;
                } else if (count == 2) {
                    value /= 10;
                }
                FieldPosition p = new FieldPosition(-1);
                numberFormat.format((long) value, buf, p);
                if (count > 3) {
                    numberFormat.setMinimumIntegerDigits(count - 3);
                    numberFormat.format(0L, buf, p);
                }
            }
            break;
        case 19: // 'e' - DOW_LOCAL (use DOW_LOCAL for numeric, DAY_OF_WEEK for format names)
            if (count < 3) {
                zeroPaddingNumber(currentNumberFormat,buf, value, count, maxIntCount);
                break;
            }
            // For alpha day-of-week, we don't want DOW_LOCAL,
            // we need the standard DAY_OF_WEEK.
            value = cal.get(Calendar.DAY_OF_WEEK);
            // fall through, do not break here
        case 9: // 'E' - DAY_OF_WEEK
            if (count == 5) {
                safeAppend(formatData.narrowWeekdays, value, buf);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_NARROW;
            } else if (count == 4) {
                safeAppend(formatData.weekdays, value, buf);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_FORMAT;
            } else {// count <= 3, use abbreviated form if exists
                safeAppend(formatData.shortWeekdays, value, buf);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_FORMAT;
            }
            break;
        case 14: // 'a' - AM_PM
            safeAppend(formatData.ampms, value, buf);
            break;
        case 15: // 'h' - HOUR (1..12)
            if (value == 0) {
                zeroPaddingNumber(currentNumberFormat,buf,
                                  cal.getLeastMaximum(Calendar.HOUR)+1,
                                  count, maxIntCount);
            } else {
                zeroPaddingNumber(currentNumberFormat,buf, value, count, maxIntCount);
            }
            break;
        case 17: // 'z' - ZONE_OFFSET
            if (count < 4) {
                // "z", "zz", "zzz"
                result = tzFormat().format(Style.SPECIFIC_SHORT, tz, date);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.METAZONE_SHORT;
            } else {
                result = tzFormat().format(Style.SPECIFIC_LONG, tz, date);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.METAZONE_LONG;
            }
            buf.append(result);
            break;
        case 23: // 'Z' - TIMEZONE_RFC
        {
            if (count < 4) {
                // RFC822 format
                result = tzFormat().format(Style.RFC822, tz, date);
            } else if (count == 5) {
                // ISO 8601 extended format
                result = tzFormat().format(Style.ISO8601, tz, date);
            } else {
                // long form, localized GMT pattern
                result = tzFormat().format(Style.LOCALIZED_GMT, tz, date);
            }
                buf.append(result);
            break;
            }
        case 24: // 'v' - TIMEZONE_GENERIC
            if (count == 1) {
                // "v"
                result = tzFormat().format(Style.GENERIC_SHORT, tz, date);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.METAZONE_SHORT;
            } else if (count == 4) {
                // "vvvv"
                result = tzFormat().format(Style.GENERIC_LONG, tz, date);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.METAZONE_LONG;
           }
            buf.append(result);
            break;

        case 25: // 'c' - STANDALONE DAY (use DOW_LOCAL for numeric, DAY_OF_WEEK for standalone)
            if (count < 3) {
                zeroPaddingNumber(currentNumberFormat,buf, value, 1, maxIntCount);
                break;
            }
            // For alpha day-of-week, we don't want DOW_LOCAL,
            // we need the standard DAY_OF_WEEK.
            value = cal.get(Calendar.DAY_OF_WEEK);
            if (count == 5) {
                safeAppend(formatData.standaloneNarrowWeekdays, value, buf);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_NARROW;
            } else if (count == 4) {
                safeAppend(formatData.standaloneWeekdays, value, buf);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_STANDALONE;
            } else { // count == 3
                safeAppend(formatData.standaloneShortWeekdays, value, buf);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.DAY_STANDALONE;
            }
            break;
        case 27: // 'Q' - QUARTER
            if (count >= 4) {
                safeAppend(formatData.quarters, value/3, buf);
            } else if (count == 3) {
                safeAppend(formatData.shortQuarters, value/3, buf);
            } else {
                zeroPaddingNumber(currentNumberFormat,buf, (value/3)+1, count, maxIntCount);
            }
            break;
        case 28: // 'q' - STANDALONE QUARTER
            if (count >= 4) {
                safeAppend(formatData.standaloneQuarters, value/3, buf);
            } else if (count == 3) {
                safeAppend(formatData.standaloneShortQuarters, value/3, buf);
            } else {
                zeroPaddingNumber(currentNumberFormat,buf, (value/3)+1, count, maxIntCount);
            }
            break;
        case 29: // 'V' - TIMEZONE_SPECIAL
            if (count == 1) {
                // "V"
                result = tzFormat().format(Style.SPECIFIC_SHORT, tz, date);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.METAZONE_SHORT;
            } else if (count == 4) {
                // "VVVV"
                result = tzFormat().format(Style.GENERIC_LOCATION, tz, date);
                capContextUsageType = DateFormatSymbols.CapitalizationContextUsage.ZONE_LONG;
            }
            buf.append(result);
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
            // case 20: // 'u' - EXTENDED_YEAR
            // case 21: // 'g' - JULIAN_DAY
            // case 22: // 'A' - MILLISECONDS_IN_DAY

            zeroPaddingNumber(currentNumberFormat,buf, value, count, maxIntCount);
            break;
        } // switch (patternCharIndex)

        if (fieldNum == 0) {
            boolean titlecase = false;
            if (capitalizationContext != null) {
                switch (capitalizationContext) {
                    case CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE:
                        titlecase = true;
                        break;
                    case CAPITALIZATION_FOR_UI_LIST_OR_MENU:
                    case CAPITALIZATION_FOR_STANDALONE:
                        if (formatData.capitalization != null) {
                             boolean[] transforms = formatData.capitalization.get(capContextUsageType);
                            titlecase = (capitalizationContext==ContextValue.CAPITALIZATION_FOR_UI_LIST_OR_MENU)?
                                        transforms[0]: transforms[1];
                        }
                        break;
                    default:
                       break;
                }
            }
            if (titlecase) {
                String firstField = buf.substring(bufstart); // bufstart or beginOffset, should be the same
                String firstFieldTitleCase = UCharacter.toTitleCase(locale, firstField, null,
                                                     UCharacter.TITLECASE_NO_LOWERCASE | UCharacter.TITLECASE_NO_BREAK_ADJUSTMENT);
                buf.replace(bufstart, buf.length(), firstFieldTitleCase);
            }
        }

        // Set the FieldPosition (for the first occurrence only)
        if (pos.getBeginIndex() == pos.getEndIndex()) {
            if (pos.getField() == PATTERN_INDEX_TO_DATE_FORMAT_FIELD[patternCharIndex]) {
                pos.setBeginIndex(beginOffset);
                pos.setEndIndex(beginOffset + buf.length() - bufstart);
            } else if (pos.getFieldAttribute() ==
                       PATTERN_INDEX_TO_DATE_FORMAT_ATTRIBUTE[patternCharIndex]) {
                pos.setBeginIndex(beginOffset);
                pos.setEndIndex(beginOffset + buf.length() - bufstart);
            }
        }
    }

    private static void safeAppend(String[] array, int value, StringBuffer appendTo) {
        if (array != null && value >= 0 && value < array.length) {
            appendTo.append(array[value]);
        }
    }

    private static void safeAppendWithMonthPattern(String[] array, int value, StringBuffer appendTo, String monthPattern) {
        if (array != null && value >= 0 && value < array.length) {
            if (monthPattern == null) {
                appendTo.append(array[value]);
            } else {
                appendTo.append(MessageFormat.format(monthPattern, array[value]));
            }
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

    private static ICUCache<String, Object[]> PARSED_PATTERN_CACHE =
        new SimpleCache<String, Object[]>();
    private transient Object[] patternItems;

    /*
     * Returns parsed pattern items.  Each item is either String or
     * PatternItem.
     */
    private Object[] getPatternItems() {
        if (patternItems != null) {
            return patternItems;
        }

        patternItems = PARSED_PATTERN_CACHE.get(pattern);
        if (patternItems != null) {
            return patternItems;
        }

        boolean isPrevQuote = false;
        boolean inQuote = false;
        StringBuilder text = new StringBuilder();
        char itemType = 0;  // 0 for string literal, otherwise date/time pattern character
        int itemLength = 1;

        List<Object> items = new ArrayList<Object>();

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

        patternItems = items.toArray(new Object[items.size()]);

        PARSED_PATTERN_CACHE.put(pattern, patternItems);

        return patternItems;
    }

    /**
     * Internal high-speed method.  Reuses a StringBuffer for results
     * instead of creating a String on the heap for each call.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected void zeroPaddingNumber(NumberFormat nf,StringBuffer buf, int value,
                                     int minDigits, int maxDigits) {
        // Note: Indian calendar uses negative value for a calendar
        // field. fastZeroPaddingNumber cannot handle negative numbers.
        // BTW, it looks like a design bug in the Indian calendar...
        if (useLocalZeroPaddingNumberFormat && value >= 0) {
            fastZeroPaddingNumber(buf, value, minDigits, maxDigits);
        } else {
            nf.setMinimumIntegerDigits(minDigits);
            nf.setMaximumIntegerDigits(maxDigits);
            nf.format(value, buf, new FieldPosition(-1));
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
        initializeTimeZoneFormat(true);
    }

    private void initLocalZeroPaddingNumberFormat() {
        if (numberFormat instanceof DecimalFormat) {
            decDigits = ((DecimalFormat)numberFormat).getDecimalFormatSymbols().getDigits();
            useLocalZeroPaddingNumberFormat = true;
        } else if (numberFormat instanceof DateNumberFormat) {
            decDigits = ((DateNumberFormat)numberFormat).getDigits();
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
    private transient char[] decDigits;
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
            decimalBuf[index] = decDigits[(value % 10)];
            value /= 10;
            if (index == 0 || value == 0) {
                break;
            }
            index--;
        }
        int padding = minDigits - (limit - index);
        while (padding > 0 && index > 0) {
            decimalBuf[--index] = decDigits[0];
            padding--;
        }
        while (padding > 0) {
            // when pattern width is longer than decimalBuf, need extra
            // leading zeros - ticke#7595
            buf.append(decDigits[0]);
            padding--;
        }
        buf.append(decimalBuf, index, limit - index);
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
    private static final String NUMERIC_FORMAT_CHARS = "MYyudehHmsSDFwWkK";

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
        TimeZone backupTZ = null;
        Calendar resultCal = null;
        if (cal != calendar && !cal.getType().equals(calendar.getType())) {
            // Different calendar type
            // We use the time/zone from the input calendar, but
            // do not use the input calendar for field calculation.
            calendar.setTimeInMillis(cal.getTimeInMillis());
            backupTZ = calendar.getTimeZone();
            calendar.setTimeZone(cal.getTimeZone());
            resultCal = cal;
            cal = calendar;
        }

        int pos = parsePos.getIndex();
        int start = pos;

        // Reset tztype
        tztype = TimeType.UNKNOWN;
        boolean[] ambiguousYear = { false };

        // item index for the first numeric field within a contiguous numeric run
        int numericFieldStart = -1;
        // item length for the first numeric field within a contiguous numeric run
        int numericFieldLength = 0;
        // start index of numeric text run in the input text
        int numericStartPos = 0;
        
        MessageFormat numericLeapMonthFormatter = null;
        if (formatData.leapMonthPatterns != null && formatData.leapMonthPatterns.length >= DateFormatSymbols.DT_MONTH_PATTERN_COUNT) {
            numericLeapMonthFormatter = new MessageFormat(formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_NUMERIC], locale);
        }

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
                            true, false, ambiguousYear, cal, numericLeapMonthFormatter);

                    if (pos < 0) {
                        // If the parse fails anywhere in the numeric run, back up to the
                        // start of the run and use shorter pattern length for the first
                        // numeric field.
                        --numericFieldLength;
                        if (numericFieldLength == 0) {
                            // can not make shorter any more
                            parsePos.setIndex(start);
                            parsePos.setErrorIndex(pos);
                            if (backupTZ != null) {
                                calendar.setTimeZone(backupTZ);
                            }
                            return;
                        }
                        i = numericFieldStart;
                        pos = numericStartPos;
                        continue;
                    }

                } else if (field.type != 'l') { // (SMALL LETTER L) obsolete pattern char just gets ignored
                    // Handle a non-numeric field or a non-abutting numeric field
                    numericFieldStart = -1;

                    int s = pos;
                    pos = subParse(text, pos, field.type, field.length,
                            false, true, ambiguousYear, cal, numericLeapMonthFormatter);
                    
                    if (pos < 0) {
                        if (pos == ISOSpecialEra) {
                            // era not present, in special cases allow this to continue
                            pos = s;

                            if (i+1 < items.length) { 
                                
                                // get next item in pattern
                                String patl = (String)items[i+1];
                                int plen = patl.length();
                                int idx=0;
                                
                                // White space characters found in patten.
                                // Skip contiguous white spaces.
                                while (idx < plen) {

                                    char pch = patl.charAt(idx);
                                    if (PatternProps.isWhiteSpace(pch))
                                        idx++;
                                    else
                                        break;
                                }
                                
                                // if next item in pattern is all whitespace, skip it
                                if (idx == plen) {
                                    i++;
                                }

                            }
                        } else {
                            parsePos.setIndex(start);
                            parsePos.setErrorIndex(s);
                            if (backupTZ != null) {
                                calendar.setTimeZone(backupTZ);
                            }
                            return;
                        }                              
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
                    if (PatternProps.isWhiteSpace(pch)
                        && PatternProps.isWhiteSpace(ich)) {
                        // White space characters found in both patten and input.
                        // Skip contiguous white spaces.
                        while ((idx + 1) < plen &&
                                PatternProps.isWhiteSpace(patl.charAt(idx + 1))) {
                             ++idx;
                        }
                        while ((pos + 1) < tlen &&
                                PatternProps.isWhiteSpace(text.charAt(pos + 1))) {
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
                    if (backupTZ != null) {
                        calendar.setTimeZone(backupTZ);
                    }
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
            if (ambiguousYear[0] || tztype != TimeType.UNKNOWN) {
                // We need a copy of the fields, and we need to avoid triggering a call to
                // complete(), which will recalculate the fields.  Since we can't access
                // the fields[] array in Calendar, we clone the entire object.  This will
                // stop working if Calendar.clone() is ever rewritten to call complete().
                Calendar copy;
                if (ambiguousYear[0]) { // the two-digit year == the default start year
                    copy = (Calendar)cal.clone();
                    Date parsedDate = copy.getTime();
                    if (parsedDate.before(getDefaultCenturyStart())) {
                        // We can't use add here because that does a complete() first.
                        cal.set(Calendar.YEAR, getDefaultCenturyStartYear() + 100);
                    }
                }
                if (tztype != TimeType.UNKNOWN) {
                    copy = (Calendar)cal.clone();
                    TimeZone tz = copy.getTimeZone();
                    BasicTimeZone btz = null;
                    if (tz instanceof BasicTimeZone) {
                        btz = (BasicTimeZone)tz;
                    }

                    // Get local millis
                    copy.set(Calendar.ZONE_OFFSET, 0);
                    copy.set(Calendar.DST_OFFSET, 0);
                    long localMillis = copy.getTimeInMillis();

                    // Make sure parsed time zone type (Standard or Daylight)
                    // matches the rule used by the parsed time zone.
                    int[] offsets = new int[2];
                    if (btz != null) {
                        if (tztype == TimeType.STANDARD) {
                            btz.getOffsetFromLocal(localMillis,
                                    BasicTimeZone.LOCAL_STD, BasicTimeZone.LOCAL_STD, offsets);
                        } else {
                            btz.getOffsetFromLocal(localMillis,
                                    BasicTimeZone.LOCAL_DST, BasicTimeZone.LOCAL_DST, offsets);
                        }
                    } else {
                        // No good way to resolve ambiguous time at transition,
                        // but following code work in most case.
                        tz.getOffset(localMillis, true, offsets);

                        if (tztype == TimeType.STANDARD && offsets[1] != 0
                            || tztype == TimeType.DAYLIGHT && offsets[1] == 0) {
                            // Roll back one day and try it again.
                            // Note: This code assumes 1. timezone transition only happens
                            // once within 24 hours at max
                            // 2. the difference of local offsets at the transition is
                            // less than 24 hours.
                            tz.getOffset(localMillis - (24*60*60*1000), true, offsets);
                        }
                    }

                    // Now, compare the results with parsed type, either standard or
                    // daylight saving time
                    int resolvedSavings = offsets[1];
                    if (tztype == TimeType.STANDARD) {
                        if (offsets[1] != 0) {
                            // Override DST_OFFSET = 0 in the result calendar
                            resolvedSavings = 0;
                        }
                    } else { // tztype == TZTYPE_DST
                        if (offsets[1] == 0) {
                            if (btz != null) {
                                long time = localMillis + offsets[0];
                                // We use the nearest daylight saving time rule.
                                TimeZoneTransition beforeTrs, afterTrs;
                                long beforeT = time, afterT = time;
                                int beforeSav = 0, afterSav = 0;

                                // Search for DST rule before or on the time
                                while (true) {
                                    beforeTrs = btz.getPreviousTransition(beforeT, true);
                                    if (beforeTrs == null) {
                                        break;
                                    }
                                    beforeT = beforeTrs.getTime() - 1;
                                    beforeSav = beforeTrs.getFrom().getDSTSavings();
                                    if (beforeSav != 0) {
                                        break;
                                    }
                                }

                                // Search for DST rule after the time
                                while (true) {
                                    afterTrs = btz.getNextTransition(afterT, false);
                                    if (afterTrs == null) {
                                        break;
                                    }
                                    afterT = afterTrs.getTime();
                                    afterSav = afterTrs.getTo().getDSTSavings();
                                    if (afterSav != 0) {
                                        break;
                                    }
                                }

                                if (beforeTrs != null && afterTrs != null) {
                                    if (time - beforeT > afterT - time) {
                                        resolvedSavings = afterSav;
                                    } else {
                                        resolvedSavings = beforeSav;
                                    }
                                } else if (beforeTrs != null && beforeSav != 0) {
                                    resolvedSavings = beforeSav;
                                } else if (afterTrs != null && afterSav != 0) {
                                    resolvedSavings = afterSav;
                                } else {
                                    resolvedSavings = btz.getDSTSavings();
                                }
                            } else {
                                resolvedSavings = tz.getDSTSavings();
                            }
                            if (resolvedSavings == 0) {
                                // Final fallback
                                resolvedSavings = millisPerHour;
                            }
                        }
                    }
                    cal.set(Calendar.ZONE_OFFSET, offsets[0]);
                    cal.set(Calendar.DST_OFFSET, resolvedSavings);
                }
            }
        }
        // An IllegalArgumentException will be thrown by Calendar.getTime()
        // if any fields are out of range, e.g., MONTH == 17.
        catch (IllegalArgumentException e) {
            parsePos.setErrorIndex(pos);
            parsePos.setIndex(start);
            if (backupTZ != null) {
                calendar.setTimeZone(backupTZ);
            }
            return;
        }
        // Set the parsed result if local calendar is used
        // instead of the input calendar
        if (resultCal != null) {
            resultCal.setTimeZone(cal.getTimeZone());
            resultCal.setTimeInMillis(cal.getTimeInMillis());
        }
        // Restore the original time zone if required
        if (backupTZ != null) {
            calendar.setTimeZone(backupTZ);
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
     * @param cal
     * @return the new start position if matching succeeded; a negative
     * number indicating matching failure, otherwise.  As a side effect,
     * sets the <code>cal</code> field <code>field</code> to the index
     * of the best match, if matching succeeded.
     * @stable ICU 2.0
     */
    protected int matchString(String text, int start, int field, String[] data, Calendar cal)
    {
        return matchString(text, start, field, data, null, cal);
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
     * @param monthPattern leap month pattern, or null if none.
     * @param cal
     * @return the new start position if matching succeeded; a negative
     * number indicating matching failure, otherwise.  As a side effect,
     * sets the <code>cal</code> field <code>field</code> to the index
     * of the best match, if matching succeeded.
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    protected int matchString(String text, int start, int field, String[] data, String monthPattern, Calendar cal)
    {
        int i = 0;
        int count = data.length;

        if (field == Calendar.DAY_OF_WEEK) i = 1;

        // There may be multiple strings in the data[] array which begin with
        // the same prefix (e.g., Cerven and Cervenec (June and July) in Czech).
        // We keep track of the longest match, and return that.  Note that this
        // unfortunately requires us to test all array elements.
        int bestMatchLength = 0, bestMatch = -1;
        int isLeapMonth = 0;

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
                        isLeapMonth = 0;
                    }
                if (monthPattern != null) {
                    String leapMonthName = MessageFormat.format(monthPattern, data[i]);
                    length = leapMonthName.length();
                    if (length > bestMatchLength &&
                        text.regionMatches(true, start, leapMonthName, 0, length))
                        {
                            bestMatch = i;
                            bestMatchLength = length;
                            isLeapMonth = 1;
                        }
                 }
            }
        if (bestMatch >= 0)
            {
                if (field == Calendar.YEAR) {
                    bestMatch++; // only get here for cyclic year names, which match 1-based years 1-60
                }
                cal.set(field, bestMatch);
                if (monthPattern != null) {
                    cal.set(Calendar.IS_LEAP_MONTH, isLeapMonth);
                }
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
     * @param cal
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
        return subParse(text, start, ch, count, obeyCount, allowNegative, ambiguousYear, cal, null);
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
     * @param cal
     * @param numericLeapMonthFormatter if non-null, used to parse numeric leap months. 
     * @return the new start position if matching succeeded; a negative
     * number indicating matching failure, otherwise.  As a side effect,
     * set the appropriate field of <code>cal</code> with the parsed
     * value.
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    protected int subParse(String text, int start, char ch, int count,
                           boolean obeyCount, boolean allowNegative,
                           boolean[] ambiguousYear, Calendar cal, MessageFormat numericLeapMonthFormatter)
    {
        Number number = null;
        NumberFormat currentNumberFormat = null;
        int value = 0;
        int i;
        ParsePosition pos = new ParsePosition(0);
        boolean lenient = isLenient();

        //int patternCharIndex = DateFormatSymbols.patternChars.indexOf(ch);c
        int patternCharIndex = -1;
        if ('A' <= ch && ch <= 'z') {
            patternCharIndex = PATTERN_CHAR_TO_INDEX[(int)ch - PATTERN_CHAR_BASE];
        }

        if (patternCharIndex == -1) {
            return -start;
        }

        currentNumberFormat = getNumberFormat(ch);

        int field = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
        
        if (numericLeapMonthFormatter != null) {
            numericLeapMonthFormatter.setFormatByArgumentIndex(0, currentNumberFormat);
        }

        // If there are any spaces here, skip over them.  If we hit the end
        // of the string, then fail.
        for (;;) {
            if (start >= text.length()) {
                return -start;
            }
            int c = UTF16.charAt(text, start);
            if (!UCharacter.isUWhiteSpace(c) || !PatternProps.isWhiteSpace(c)) {
                break;
            }
            start += UTF16.getCharCount(c);
        }
        pos.setIndex(start);

        // We handle a few special cases here where we need to parse
        // a number value.  We handle further, more generic cases below.  We need
        // to handle some of them here because some fields require extra processing on
        // the parsed value.
        if (patternCharIndex == 4 /*'k' HOUR_OF_DAY1_FIELD*/ ||
            patternCharIndex == 15 /*'h' HOUR1_FIELD*/ ||
            (patternCharIndex == 2 /*'M' MONTH_FIELD*/ && count <= 2) ||
            (patternCharIndex == 26 /*'L' STAND_ALONE_MONTH*/ && count <= 2) ||
            patternCharIndex == 1 /*'y' YEAR */ || patternCharIndex == 18 /*'Y' YEAR_WOY */ ||
            patternCharIndex == 30 /*'U' YEAR_NAME_FIELD, falls back to numeric */ ||
            (patternCharIndex == 0 /*'G' ERA */ && cal.getType().equals("chinese")) ||
            patternCharIndex == 8 /*'S' FRACTIONAL_SECOND */ )
            {
                // It would be good to unify this with the obeyCount logic below,
                // but that's going to be difficult.
                
                boolean parsedNumericLeapMonth = false;
                if (numericLeapMonthFormatter != null && (patternCharIndex == 2 || patternCharIndex == 26)) {
                    // First see if we can parse month number with leap month pattern
                    Object[] args = numericLeapMonthFormatter.parse(text, pos);
                    if (args != null && pos.getIndex() > start && (args[0] instanceof Number)) {
                        parsedNumericLeapMonth = true;
                        number = (Number)args[0];
                        cal.set(Calendar.IS_LEAP_MONTH, 1);
                    } else {
                        pos.setIndex(start);
                        cal.set(Calendar.IS_LEAP_MONTH, 0);
                   }
                }
                
                if (!parsedNumericLeapMonth) {
                    if (obeyCount) {
                        if ((start+count) > text.length()) {
                            return -start;
                        }
                        number = parseInt(text, count, pos, allowNegative,currentNumberFormat);
                    } else {
                        number = parseInt(text, pos, allowNegative,currentNumberFormat);
                    }
                    if (number == null && patternCharIndex != 30) {
                        return -start;
                    }
                }

                if (number != null) {
                    value = number.intValue();
                }
            }

        switch (patternCharIndex)
            {
            case 0: // 'G' - ERA
                if ( cal.getType().equals("chinese") ) {
                    // Numeric era handling moved from ChineseDateFormat,
                    // If we didn't have a number, already returned -start above
                    cal.set(Calendar.ERA, value);
                    return pos.getIndex();
                }
                int ps = 0;
                if (count == 5) {
                    ps = matchString(text, start, Calendar.ERA, formatData.narrowEras, null, cal);
                } else if (count == 4) {
                    ps = matchString(text, start, Calendar.ERA, formatData.eraNames, null, cal);
                } else {
                    ps = matchString(text, start, Calendar.ERA, formatData.eras, null, cal);
                }

                // check return position, if it equals -start, then matchString error
                // special case the return code so we don't necessarily fail out until we 
                // verify no year information also
                if (ps == -start)
                    ps = ISOSpecialEra;

                return ps;  
                
            case 1: // 'y' - YEAR
            case 18: // 'Y' - YEAR_WOY
                // If there are 3 or more YEAR pattern characters, this indicates
                // that the year value is to be treated literally, without any
                // two-digit year adjustments (e.g., from "01" to 2001).  Otherwise
                // we made adjustments to place the 2-digit year in the proper
                // century, for parsed strings from "00" to "99".  Any other string
                // is treated literally:  "2250", "-1", "1", "002".
                /* 'yy' is the only special case, 'y' is interpreted as number. [Richard/GCL]*/
                /* Skip this for Chinese calendar, moved from ChineseDateFormat */
                if ( override != null && (override.compareTo("hebr") == 0 || override.indexOf("y=hebr") >= 0) && value < 1000 ) {
                    value += HEBREW_CAL_CUR_MILLENIUM_START_YEAR;
                } else if (count == 2 && (pos.getIndex() - start) == 2 && !cal.getType().equals("chinese")
                    && UCharacter.isDigit(text.charAt(start))
                    && UCharacter.isDigit(text.charAt(start+1)))
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
                cal.set(field, value);

                // Delayed checking for adjustment of Hebrew month numbers in non-leap years.
                if (DelayedHebrewMonthCheck) {
                    if (!HebrewCalendar.isLeapYear(value)) {
                        cal.add(Calendar.MONTH,1);
                    }
                    DelayedHebrewMonthCheck = false;
                }
                return pos.getIndex();
            case 30: // 'U' - YEAR_NAME_FIELD
                if (formatData.shortYearNames != null) {
                    int newStart = matchString(text, start, Calendar.YEAR, formatData.shortYearNames, null, cal);
                    if (newStart > 0) {
                        return newStart;
                    }
                }
                if ( number != null && (lenient || formatData.shortYearNames == null || value > formatData.shortYearNames.length) ) {
                    cal.set(Calendar.YEAR, value);
                    return pos.getIndex();
                }
                return -start;
            case 2: // 'M' - MONTH
            case 26: // 'L' - STAND_ALONE_MONTH
                if (count <= 2) { // i.e., M/MM, L/LL
                    // Don't want to parse the month if it is a string
                    // while pattern uses numeric style: M/MM, L/LL.
                    // [We computed 'value' above.]
                    cal.set(Calendar.MONTH, value - 1);
                    // When parsing month numbers from the Hebrew Calendar, we might need
                    // to adjust the month depending on whether or not it was a leap year.
                    // We may or may not yet know what year it is, so might have to delay
                    // checking until the year is parsed.
                    if (cal.getType().equals("hebrew") && value >= 6) {
                        if (cal.isSet(Calendar.YEAR)) {
                            if (!HebrewCalendar.isLeapYear(cal.get(Calendar.YEAR))) {
                                cal.set(Calendar.MONTH, value);
                            }
                        } else {
                            DelayedHebrewMonthCheck = true;
                        }
                    }
                    return pos.getIndex();
                } else {
                    // count >= 3 // i.e., MMM/MMMM or LLL/LLLL
                    // Want to be able to parse both short and long forms.
                    boolean haveMonthPat = (formatData.leapMonthPatterns != null && formatData.leapMonthPatterns.length >= DateFormatSymbols.DT_MONTH_PATTERN_COUNT);
                    // Try count == 4 first:
                    int newStart = (patternCharIndex == 2)?
                            matchString(text, start, Calendar.MONTH, formatData.months,
                                    (haveMonthPat)? formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_FORMAT_WIDE]: null, cal):
                            matchString(text, start, Calendar.MONTH, formatData.standaloneMonths,
                                    (haveMonthPat)? formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_STANDALONE_WIDE]: null, cal);
                    if (newStart > 0) {
                        return newStart;
                    } else { // count == 4 failed, now try count == 3
                        return (patternCharIndex == 2)?
                                matchString(text, start, Calendar.MONTH, formatData.shortMonths,
                                        (haveMonthPat)? formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_FORMAT_ABBREV]: null, cal):
                                matchString(text, start, Calendar.MONTH, formatData.standaloneShortMonths,
                                        (haveMonthPat)? formatData.leapMonthPatterns[DateFormatSymbols.DT_LEAP_MONTH_PATTERN_STANDALONE_ABBREV]: null, cal);
                    }
                }
            case 4: // 'k' - HOUR_OF_DAY (1..24)
                // [We computed 'value' above.]
                if (value == cal.getMaximum(Calendar.HOUR_OF_DAY)+1) {
                    value = 0;
                }
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
                                           formatData.weekdays, null, cal);
                if (newStart > 0) {
                    return newStart;
                } else { // EEEE failed, now try EEE
                    return matchString(text, start, Calendar.DAY_OF_WEEK,
                                       formatData.shortWeekdays, null, cal);
                }
            }
            case 25: { // 'c' - STAND_ALONE_DAY_OF_WEEK
                // Want to be able to parse both short and long forms.
                // Try count == 4 (cccc) first:
                int newStart = matchString(text, start, Calendar.DAY_OF_WEEK,
                                           formatData.standaloneWeekdays, null, cal);
                if (newStart > 0) {
                    return newStart;
                } else { // cccc failed, now try ccc
                    return matchString(text, start, Calendar.DAY_OF_WEEK,
                                       formatData.standaloneShortWeekdays, null, cal);
                }
            }
            case 14: // 'a' - AM_PM
                return matchString(text, start, Calendar.AM_PM, formatData.ampms, null, cal);
            case 15: // 'h' - HOUR (1..12)
                // [We computed 'value' above.]
                if (value == cal.getLeastMaximum(Calendar.HOUR)+1) {
                    value = 0;
                }
                cal.set(Calendar.HOUR, value);
                return pos.getIndex();
            case 17: // 'z' - ZONE_OFFSET
            {
                Output<TimeType> tzTimeType = new Output<TimeType>();
                Style style = (count < 4) ? Style.SPECIFIC_SHORT : Style.SPECIFIC_LONG;
                TimeZone tz = tzFormat().parse(style, text, pos, tzTimeType);
                if (tz != null) {
                    tztype = tzTimeType.value;
                    cal.setTimeZone(tz);
                    return pos.getIndex();
                }
                return -start;
                    }
            case 23: // 'Z' - TIMEZONE_RFC
            {
                Output<TimeType> tzTimeType = new Output<TimeType>();
                Style style = (count < 4) ? Style.RFC822 : ((count == 5) ? Style.ISO8601 : Style.LOCALIZED_GMT);
                TimeZone tz = tzFormat().parse(style, text, pos, tzTimeType);
                if (tz != null) {
                    tztype = tzTimeType.value;
                    cal.setTimeZone(tz);
                    return pos.getIndex();
                    }
                return -start;
                }
            case 24: // 'v' - TIMEZONE_GENERIC
            {
                Output<TimeType> tzTimeType = new Output<TimeType>();
                // Note: 'v' only supports count 1 and 4
                Style style = (count < 4) ? Style.GENERIC_SHORT : Style.GENERIC_LONG;
                TimeZone tz = tzFormat().parse(style, text, pos, tzTimeType);
                if (tz != null) {
                    tztype = tzTimeType.value;
                    cal.setTimeZone(tz);
                    return pos.getIndex();
                }
                return -start;
            }
            case 29: // 'V' - TIMEZONE_SPECIAL
            {
                Output<TimeType> tzTimeType = new Output<TimeType>();
                // Note: 'v' only supports count 1 and 4
                Style style = (count < 4) ? Style.SPECIFIC_SHORT : Style.GENERIC_LOCATION;
                TimeZone tz = tzFormat().parse(style, text, pos, tzTimeType);
                if (tz != null) {
                    tztype = tzTimeType.value;
                    cal.setTimeZone(tz);
                    return pos.getIndex();
                }
                return -start;
            }
            case 27: // 'Q' - QUARTER
                if (count <= 2) { // i.e., Q or QQ.
                    // Don't want to parse the quarter if it is a string
                    // while pattern uses numeric style: Q or QQ.
                    // [We computed 'value' above.]
                    cal.set(Calendar.MONTH, (value - 1) * 3);
                    return pos.getIndex();
                } else {
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
                if (count <= 2) { // i.e., q or qq.
                    // Don't want to parse the quarter if it is a string
                    // while pattern uses numeric style: q or qq.
                    // [We computed 'value' above.]
                    cal.set(Calendar.MONTH, (value - 1) * 3);
                    return pos.getIndex();
                } else {
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
                // case 19: // 'e' - DOW_LOCAL
                // case 20: // 'u' - EXTENDED_YEAR
                // case 21: // 'g' - JULIAN_DAY
                // case 22: // 'A' - MILLISECONDS_IN_DAY

                // Handle "generic" fields
                if (obeyCount) {
                    if ((start+count) > text.length()) return -start;
                    number = parseInt(text, count, pos, allowNegative,currentNumberFormat);
                } else {
                    number = parseInt(text, pos, allowNegative,currentNumberFormat);
                }
                if (number != null) {
                    cal.set(field, number.intValue());
                    return pos.getIndex();
                }
                return -start;
            }
    }

    /**
     * Parse an integer using numberFormat.  This method is semantically
     * const, but actually may modify fNumberFormat.
     */
    private Number parseInt(String text,
                            ParsePosition pos,
                            boolean allowNegative,
                            NumberFormat fmt) {
        return parseInt(text, -1, pos, allowNegative, fmt);
    }

    /**
     * Parse an integer using numberFormat up to maxDigits.
     */
    private Number parseInt(String text,
                            int maxDigits,
                            ParsePosition pos,
                            boolean allowNegative,
                            NumberFormat fmt) {
        Number number;
        int oldPos = pos.getIndex();
        if (allowNegative) {
            number = fmt.parse(text, pos);
        } else {
            // Invalidate negative numbers
            if (fmt instanceof DecimalFormat) {
                String oldPrefix = ((DecimalFormat)fmt).getNegativePrefix();
                ((DecimalFormat)fmt).setNegativePrefix(SUPPRESS_NEGATIVE_PREFIX);
                number = fmt.parse(text, pos);
                ((DecimalFormat)fmt).setNegativePrefix(oldPrefix);
            } else {
                boolean dateNumberFormat = (fmt instanceof DateNumberFormat);
                if (dateNumberFormat) {
                    ((DateNumberFormat)fmt).setParsePositiveOnly(true);
                }
                number = fmt.parse(text, pos);
                if (dateNumberFormat) {
                    ((DateNumberFormat)fmt).setParsePositiveOnly(false);
                }
            }
        }
        if (maxDigits > 0) {
            // adjust the result to fit into
            // the maxDigits and move the position back
            int nDigits = pos.getIndex() - oldPos;
            if (nDigits > maxDigits) {
                double val = number.doubleValue();
                nDigits -= maxDigits;
                while (nDigits > 0) {
                    val /= 10;
                    nDigits--;
                }
                pos.setIndex(oldPos + maxDigits);
                number = Integer.valueOf((int)val);
            }
        }
        return number;
    }


    /**
     * Translate a pattern, mapping each character in the from string to the
     * corresponding character in the to string.
     */
    private String translatePattern(String pat, String from, String to) {
        StringBuilder result = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < pat.length(); ++i) {
            char c = pat.charAt(i);
            if (inQuote) {
                if (c == '\'')
                    inQuote = false;
            } else {
                if (c == '\'') {
                    inQuote = true;
                } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
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
        if (inQuote) {
            throw new IllegalArgumentException("Unfinished quote in pattern");
        }
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
     * {@icu} Gets the time zone formatter which this date/time
     * formatter uses to format and parse a time zone.
     * 
     * @return the time zone formatter which this date/time
     * formatter uses.
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZoneFormat getTimeZoneFormat() {
        return tzFormat().freeze();
    }

    /**
     * {@icu} Allows you to set the time zone formatter.
     * 
     * @param tzfmt the new time zone formatter
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public void setTimeZoneFormat(TimeZoneFormat tzfmt) {
        if (tzfmt.isFrozen()) {
            // If frozen, use it as is.
            tzFormat = tzfmt;
        } else {
            // If not frozen, clone and freeze.
            tzFormat = tzfmt.cloneAsThawed().freeze();
        }
    }

    /**
     * {@icu} Set the formatter's default value for a particular context type,
     * such as CAPITALIZATION. 
     * 
     * @param type The context type for which the default value should be set. 
     * @param value The default value to set for the specified context type. 
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public void setDefaultContext(ContextType type, ContextValue value) {
        if (type == ContextType.CAPITALIZATION && value != null) {
            defaultCapitalizationContext = value;
        }
    }

    /**
     * {@icu} Get the formatter's default value for a particular context type, 
     * such as CAPITALIZATION. 
     * 
     * @param type The context type for which the default value should be obtained. 
     * @return The current default value for the specified context type.
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public ContextValue getDefaultContext(ContextType type) {
        return (type == ContextType.CAPITALIZATION && defaultCapitalizationContext != null)?
                defaultCapitalizationContext: ContextValue.UNKNOWN;
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
        initializeTimeZoneFormat(false);
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
        if (locale == null) {
            // ICU4J 3.6 or older versions did not have UFormat locales
            // in the serialized data. This is just for preventing the
            // worst case scenario...
            locale = ULocale.getDefault(Category.FORMAT);
        }

        initLocalZeroPaddingNumberFormat();
    }

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
        List<FieldPosition> attributes = new ArrayList<FieldPosition>();
        format(cal, defaultCapitalizationContext, toAppendTo, pos, attributes);

        AttributedString as = new AttributedString(toAppendTo.toString());

        // add DateFormat field attributes to the AttributedString
        for (int i = 0; i < attributes.size(); i++) {
            FieldPosition fp = attributes.get(i);
            Format.Field attribute = fp.getFieldAttribute();
            as.addAttribute(attribute, attribute, fp.getBeginIndex(), fp.getEndIndex());
        }
        // return the CharacterIterator from AttributedString
        return as.getIterator();
    }

    /**
     * Get the locale of this simple date formatter.
     * It is package accessible. also used in DateIntervalFormat.
     *
     * @return   locale in this simple date formatter
     */
    ULocale getLocale()
    {
        return locale;
    }



    /**
     * Check whether the 'field' is smaller than all the fields covered in
     * pattern, return true if it is.
     * The sequence of calendar field,
     * from large to small is: ERA, YEAR, MONTH, DATE, AM_PM, HOUR, MINUTE,...
     * @param field    the calendar field need to check against
     * @return         true if the 'field' is smaller than all the fields
     *                 covered in pattern. false otherwise.
     */

    boolean isFieldUnitIgnored(int field) {
        return isFieldUnitIgnored(pattern, field);
    }


    /*
     * Check whether the 'field' is smaller than all the fields covered in
     * pattern, return true if it is.
     * The sequence of calendar field,
     * from large to small is: ERA, YEAR, MONTH, DATE, AM_PM, HOUR, MINUTE,...
     * @param pattern  the pattern to check against
     * @param field    the calendar field need to check against
     * @return         true if the 'field' is smaller than all the fields
     *                 covered in pattern. false otherwise.
     */
    static boolean isFieldUnitIgnored(String pattern, int field) {
        int fieldLevel = CALENDAR_FIELD_TO_LEVEL[field];
        int level;
        char ch;
        boolean inQuote = false;
        char prevCh = 0;
        int count = 0;

        for (int i = 0; i < pattern.length(); ++i) {
            ch = pattern.charAt(i);
            if (ch != prevCh && count > 0) {
                level = PATTERN_CHAR_TO_LEVEL[prevCh - PATTERN_CHAR_BASE];
                if ( fieldLevel <= level ) {
                    return false;
                }
                count = 0;
            }
            if (ch == '\'') {
                if ((i+1) < pattern.length() && pattern.charAt(i+1) == '\'') {
                    ++i;
                } else {
                    inQuote = ! inQuote;
                }
            } else if ( ! inQuote && ((ch >= 0x0061 /*'a'*/ && ch <= 0x007A /*'z'*/)
                        || (ch >= 0x0041 /*'A'*/ && ch <= 0x005A /*'Z'*/))) {
                prevCh = ch;
                ++count;
            }
        }
        if (count > 0) {
            // last item
            level = PATTERN_CHAR_TO_LEVEL[prevCh - PATTERN_CHAR_BASE];
            if ( fieldLevel <= level ) {
                return false;
            }
        }
        return true;
    }


    /**
     * Format date interval by algorithm.
     * It is supposed to be used only by CLDR survey tool.
     *
     * @param fromCalendar      calendar set to the from date in date interval
     *                          to be formatted into date interval stirng
     * @param toCalendar        calendar set to the to date in date interval
     *                          to be formatted into date interval stirng
     * @param appendTo          Output parameter to receive result.
     *                          Result is appended to existing contents.
     * @param pos               On input: an alignment field, if desired.
     *                          On output: the offsets of the alignment field.
     * @exception IllegalArgumentException when there is non-recognized
     *                                     pattern letter
     * @return                  Reference to 'appendTo' parameter.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public final StringBuffer intervalFormatByAlgorithm(Calendar fromCalendar,
                                                        Calendar toCalendar,
                                                        StringBuffer appendTo,
                                                        FieldPosition pos)
                              throws IllegalArgumentException
    {
        // not support different calendar types and time zones
        if ( !fromCalendar.isEquivalentTo(toCalendar) ) {
            throw new IllegalArgumentException("can not format on two different calendars");
        }

        Object[] items = getPatternItems();
        int diffBegin = -1;
        int diffEnd = -1;

        /* look for different formatting string range */
        // look for start of difference
        try {
            for (int i = 0; i < items.length; i++) {
                if ( diffCalFieldValue(fromCalendar, toCalendar, items, i) ) {
                    diffBegin = i;
                    break;
                }
            }

            if ( diffBegin == -1 ) {
                // no difference, single date format
                return format(fromCalendar, appendTo, pos);
            }

            // look for end of difference
            for (int i = items.length-1; i >= diffBegin; i--) {
                if ( diffCalFieldValue(fromCalendar, toCalendar, items, i) ) {
                    diffEnd = i;
                    break;
                }
            }
        } catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException(e.toString());
        }

        // full range is different
        if ( diffBegin == 0 && diffEnd == items.length-1 ) {
            format(fromCalendar, appendTo, pos);
            appendTo.append(" \u2013 "); // default separator
            format(toCalendar, appendTo, pos);
            return appendTo;
        }


        /* search for largest calendar field within the different range */
        int highestLevel = 1000;
        for (int i = diffBegin; i <= diffEnd; i++) {
            if ( items[i] instanceof String) {
                continue;
            }
            PatternItem item = (PatternItem)items[i];
            char ch = item.type;
            int patternCharIndex = -1;
            if ('A' <= ch && ch <= 'z') {
                patternCharIndex = PATTERN_CHAR_TO_LEVEL[(int)ch - PATTERN_CHAR_BASE];
            }

            if (patternCharIndex == -1) {
                throw new IllegalArgumentException("Illegal pattern character " +
                                                   "'" + ch + "' in \"" +
                                                   pattern + '"');
            }

            if ( patternCharIndex < highestLevel ) {
                highestLevel = patternCharIndex;
            }
        }

        /* re-calculate diff range, including those calendar field which
           is in lower level than the largest calendar field covered
           in diff range calculated. */
        try {
            for (int i = 0; i < diffBegin; i++) {
                if ( lowerLevel(items, i, highestLevel) ) {
                    diffBegin = i;
                    break;
                }
            }


            for (int i = items.length-1; i > diffEnd; i--) {
                if ( lowerLevel(items, i, highestLevel) ) {
                    diffEnd = i;
                    break;
                }
            }
        } catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException(e.toString());
        }


        // full range is different
        if ( diffBegin == 0 && diffEnd == items.length-1 ) {
            format(fromCalendar, appendTo, pos);
            appendTo.append(" \u2013 "); // default separator
            format(toCalendar, appendTo, pos);
            return appendTo;
        }


        // formatting
        // Initialize
        pos.setBeginIndex(0);
        pos.setEndIndex(0);

        // formatting date 1
        for (int i = 0; i <= diffEnd; i++) {
            if (items[i] instanceof String) {
                appendTo.append((String)items[i]);
            } else {
                PatternItem item = (PatternItem)items[i];
                if (useFastFormat) {
                    subFormat(appendTo, item.type, item.length, appendTo.length(),
                              i, defaultCapitalizationContext, pos, fromCalendar);
                } else {
                    appendTo.append(subFormat(item.type, item.length, appendTo.length(),
                                              i, defaultCapitalizationContext, pos, fromCalendar));
                }
            }
        }

        appendTo.append(" \u2013 "); // default separator

        // formatting date 2
        for (int i = diffBegin; i < items.length; i++) {
            if (items[i] instanceof String) {
                appendTo.append((String)items[i]);
            } else {
                PatternItem item = (PatternItem)items[i];
                if (useFastFormat) {
                    subFormat(appendTo, item.type, item.length, appendTo.length(),
                              i, defaultCapitalizationContext, pos, toCalendar);
                } else {
                    appendTo.append(subFormat(item.type, item.length, appendTo.length(),
                                              i, defaultCapitalizationContext, pos, toCalendar));
                }
            }
        }
        return appendTo;
    }


    /**
     * check whether the i-th item in 2 calendar is in different value.
     *
     * It is supposed to be used only by CLDR survey tool.
     * It is used by intervalFormatByAlgorithm().
     *
     * @param fromCalendar   one calendar
     * @param toCalendar     the other calendar
     * @param items          pattern items
     * @param i              the i-th item in pattern items
     * @exception IllegalArgumentException when there is non-recognized
     *                                     pattern letter
     * @return               true is i-th item in 2 calendar is in different
     *                       value, false otherwise.
     */
    private boolean diffCalFieldValue(Calendar fromCalendar,
                                      Calendar toCalendar,
                                      Object[] items,
                                      int i) throws IllegalArgumentException {
        if ( items[i] instanceof String) {
            return false;
        }
        PatternItem item = (PatternItem)items[i];
        char ch = item.type;
        int patternCharIndex = -1;
        if ('A' <= ch && ch <= 'z') {
            patternCharIndex = PATTERN_CHAR_TO_INDEX[(int)ch - PATTERN_CHAR_BASE];
        }

        if (patternCharIndex == -1) {
            throw new IllegalArgumentException("Illegal pattern character " +
                                               "'" + ch + "' in \"" +
                                               pattern + '"');
        }

        final int field = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
        int value = fromCalendar.get(field);
        int value_2 = toCalendar.get(field);
        if ( value != value_2 ) {
            return true;
        }
        return false;
    }


    /**
     * check whether the i-th item's level is lower than the input 'level'
     *
     * It is supposed to be used only by CLDR survey tool.
     * It is used by intervalFormatByAlgorithm().
     *
     * @param items  the pattern items
     * @param i      the i-th item in pattern items
     * @param level  the level with which the i-th pattern item compared to
     * @exception IllegalArgumentException when there is non-recognized
     *                                     pattern letter
     * @return       true if i-th pattern item is lower than 'level',
     *               false otherwise
     */
    private boolean lowerLevel(Object[] items, int i, int level)
                    throws IllegalArgumentException {
        if ( items[i] instanceof String) {
            return false;
        }
        PatternItem item = (PatternItem)items[i];
        char ch = item.type;
        int patternCharIndex = -1;
        if ('A' <= ch && ch <= 'z') {
            patternCharIndex = PATTERN_CHAR_TO_LEVEL[(int)ch - PATTERN_CHAR_BASE];
        }

        if (patternCharIndex == -1) {
            throw new IllegalArgumentException("Illegal pattern character " +
                                               "'" + ch + "' in \"" +
                                               pattern + '"');
        }

        if ( patternCharIndex >= level ) {
            return true;
        }
        return false;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected NumberFormat getNumberFormat(char ch) {

       Character ovrField;
       ovrField = Character.valueOf(ch);
       if (overrideMap != null && overrideMap.containsKey(ovrField)) {
           String nsName = overrideMap.get(ovrField).toString();
           NumberFormat nf = numberFormatters.get(nsName);
           return nf;
       } else {
           return numberFormat;
       }
    }

    private void initNumberFormatters(ULocale loc) {

       numberFormatters = new HashMap<String, NumberFormat>();
       overrideMap = new HashMap<Character, String>();
       processOverrideString(loc,override);

    }

    private void processOverrideString(ULocale loc, String str) {

        if ( str == null || str.length() == 0 )
            return;

        int start = 0;
        int end;
        String nsName;
        Character ovrField;
        boolean moreToProcess = true;
        boolean fullOverride;

        while (moreToProcess) {
            int delimiterPosition = str.indexOf(";",start);
            if (delimiterPosition == -1) {
                moreToProcess = false;
                end = str.length();
            } else {
                end = delimiterPosition;
            }

            String currentString = str.substring(start,end);
            int equalSignPosition = currentString.indexOf("=");
            if (equalSignPosition == -1) { // Simple override string such as "hebrew"
               nsName = currentString;
               fullOverride = true;
            } else { // Field specific override string such as "y=hebrew"
               nsName = currentString.substring(equalSignPosition+1);
               ovrField = Character.valueOf(currentString.charAt(0));
               overrideMap.put(ovrField,nsName);
               fullOverride = false;
            }

            ULocale ovrLoc = new ULocale(loc.getBaseName()+"@numbers="+nsName);
            NumberFormat nf = NumberFormat.createInstance(ovrLoc,NumberFormat.NUMBERSTYLE);
            nf.setGroupingUsed(false);
            
            if (fullOverride) {
                setNumberFormat(nf);
            } else {
                // Since one or more of the override number formatters might be complex,
                // we can't rely on the fast numfmt where we have a partial field override.
                useLocalZeroPaddingNumberFormat = false;
            }

            if (!numberFormatters.containsKey(nsName)) {
                  numberFormatters.put(nsName,nf);
            }

            start = delimiterPosition + 1;
        }
    }
}

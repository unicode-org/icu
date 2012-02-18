/*
 *******************************************************************************
 * Copyright (C) 2011-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.MissingResourceException;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SoftCache;
import com.ibm.icu.impl.TimeZoneGenericNames;
import com.ibm.icu.impl.TimeZoneGenericNames.GenericMatchInfo;
import com.ibm.icu.impl.TimeZoneGenericNames.GenericNameType;
import com.ibm.icu.impl.TimeZoneNamesImpl;
import com.ibm.icu.impl.ZoneMeta;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.TimeZoneNames.MatchInfo;
import com.ibm.icu.text.TimeZoneNames.NameType;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * <code>TimeZoneFormat</code> supports time zone display name formatting and parsing.
 * An instance of TimeZoneFormat works as a subformatter of {@link SimpleDateFormat},
 * but you can also directly get a new instance of <code>TimeZoneFormat</code> and
 * formatting/parsing time zone display names.
 * <p>
 * ICU implements the time zone display names defined by <a href="http://www.unicode.org/reports/tr35/">UTS#35
 * Unicode Locale Data Markup Language (LDML)</a>. {@link TimeZoneNames} represents the
 * time zone display name data model and this class implements the algorithm for actual
 * formatting and parsing.
 * 
 * @see SimpleDateFormat
 * @see TimeZoneNames
 * @draft ICU 49
 * @provisional This API might change or be removed in a future release.
 */
public class TimeZoneFormat extends UFormat implements Freezable<TimeZoneFormat>, Serializable {

    private static final long serialVersionUID = 2281246852693575022L;

    /**
     * Time zone display format style enum used by format/parse APIs in <code>TimeZoneFormat</code>.
     * 
     * @see TimeZoneFormat#format(Style, TimeZone, long)
     * @see TimeZoneFormat#format(Style, TimeZone, long, Output)
     * @see TimeZoneFormat#parse(Style, String, ParsePosition, Output)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public enum Style {
        /**
         * Generic location format, such as "United States Time (New York)", "Italy Time"
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        GENERIC_LOCATION (0x0001),
        /**
         * Generic long non-location format, such as "Eastern Time".
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        GENERIC_LONG (0x0002),
        /**
         * Generic short non-location format, such as "ET".
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        GENERIC_SHORT (0x0004),
        /**
         * Specific long format, such as "Eastern Standard Time".
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        SPECIFIC_LONG (0x0008),
        /**
         * Specific short format, such as "EST", "PDT".
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        SPECIFIC_SHORT (0x0010),
        /**
         * RFC822 format, such as "-0500"
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        RFC822 (0x0020),
        /**
         * Localized GMT offset format, such as "GMT-05:00", "UTC+0100"
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        LOCALIZED_GMT (0x0040),
        /**
         * ISO 8601 format (extended), such as "-05:00", "Z"(UTC)
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        ISO8601 (0x0080);

        final int flag;
    
        private Style(int flag) {
            this.flag = flag;
        }
    }

    /**
     * Offset pattern type enum.
     * 
     * @see TimeZoneFormat#getGMTOffsetPattern(GMTOffsetPatternType)
     * @see TimeZoneFormat#setGMTOffsetPattern(GMTOffsetPatternType, String)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public enum GMTOffsetPatternType {
        /**
         * Positive offset with hour and minute fields
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        POSITIVE_HM ("+HH:mm", "Hm", true),
        /**
         * Positive offset with hour, minute and second fields
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        POSITIVE_HMS ("+HH:mm:ss", "Hms", true),
        /**
         * Negative offset with hour and minute fields
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        NEGATIVE_HM ("-HH:mm", "Hm", false),
        /**
         * Negative offset with hour, minute and second fields
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        NEGATIVE_HMS ("-HH:mm:ss", "Hms", false);

        private String _defaultPattern;
        private String _required;
        private boolean _isPositive;

        private GMTOffsetPatternType(String defaultPattern, String required, boolean isPositive) {
            _defaultPattern = defaultPattern;
            _required = required;
            _isPositive = isPositive;
        }

        private String defaultPattern() {
            return _defaultPattern;
        }

        private String required() {
            return _required;
        }

        private boolean isPositive() {
            return _isPositive;
        }
    }

    /**
     * Time type enum used for receiving time type (standard time, daylight time or unknown)
     * in <code>TimeZoneFormat</code> APIs.
     * 
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public enum TimeType {
        /**
         * Unknown
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        UNKNOWN,
        /**
         * Standard time
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        STANDARD,
        /**
         * Daylight saving time
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        DAYLIGHT;
    }

    /**
     * Parse option enum, used for specifying optional parse behavior.
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public enum ParseOption {
        /**
         * When a time zone display name is not found within a set of display names
         * used for the specified style, look for the name from display names used
         * by other styles.
         * @draft ICU 49
         * @provisional This API might change or be removed in a future release.
         */
        ALL_STYLES;
    }    

    /*
     * fields to be serialized
     */
    private ULocale _locale;
    private TimeZoneNames _tznames;
    private String _gmtPattern;
    private String[] _gmtOffsetPatterns;
    private String[] _gmtOffsetDigits;
    private String _gmtZeroFormat;
    private boolean _parseAllStyles;

    /*
     * Transient fields
     */
    private transient volatile TimeZoneGenericNames _gnames;

    private transient String[] _gmtPatternTokens;
    private transient Object[][] _gmtOffsetPatternItems;

    private transient String _region;

    private transient boolean _frozen;


    /*
     * Static final fields
     */
    private static final String TZID_GMT = "Etc/GMT"; // canonical tzid for GMT

    private static final String[] ALT_GMT_STRINGS = {"GMT", "UTC", "UT"};

    private static final String DEFAULT_GMT_PATTERN = "GMT{0}";
    private static final String DEFAULT_GMT_ZERO = "GMT";
    private static final String[] DEFAULT_GMT_DIGITS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final char DEFAULT_GMT_OFFSET_SEP = ':';
    private static final String ASCII_DIGITS = "0123456789";
    private static final String ISO8601_UTC = "Z";

    // Order of GMT offset pattern parsing, *_HMS must be evaluated first
    // because *_HM is most likely a substring of *_HMS 
    private static final GMTOffsetPatternType[] PARSE_GMT_OFFSET_TYPES = {
        GMTOffsetPatternType.POSITIVE_HMS, GMTOffsetPatternType.NEGATIVE_HMS,
        GMTOffsetPatternType.POSITIVE_HM, GMTOffsetPatternType.NEGATIVE_HM,
    };

    // Maximum values for GMT offset fields
    private static final int MAX_OFFSET_HOUR = 23;
    private static final int MAX_OFFSET_MINUTE = 59;
    private static final int MAX_OFFSET_SECOND = 59;

    private static final int MILLIS_PER_HOUR = 60 * 60 * 1000;
    private static final int MILLIS_PER_MINUTE = 60 * 1000;
    private static final int MILLIS_PER_SECOND = 1000;

    private static final int UNKNOWN_OFFSET = Integer.MAX_VALUE;

    private static TimeZoneFormatCache _tzfCache = new TimeZoneFormatCache();

    // The filter used for searching all specific names
    private static final EnumSet<NameType> ALL_SPECIFIC_NAME_TYPES = EnumSet.of(
        NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT,
        NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT
    );

    // The filter used for searching all generic names
    private static final EnumSet<GenericNameType> ALL_GENERIC_NAME_TYPES = EnumSet.of(
        GenericNameType.LOCATION, GenericNameType.LONG, GenericNameType.SHORT
    );

    /**
     * The protected constructor for subclassing.
     * @param locale the locale
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    protected TimeZoneFormat(ULocale locale) {
        _locale = locale;
        _tznames = TimeZoneNames.getInstance(locale);
        // TimeZoneGenericNames _gnames will be instantiated lazily

        String gmtPattern = null;
        String hourFormats = null;
        _gmtZeroFormat = DEFAULT_GMT_ZERO;

        try {
            ICUResourceBundle bundle = (ICUResourceBundle) ICUResourceBundle.getBundleInstance(
                    ICUResourceBundle.ICU_ZONE_BASE_NAME, locale);
            try {
                gmtPattern = bundle.getStringWithFallback("zoneStrings/gmtFormat");
            } catch (MissingResourceException e) {
                // fall through
            }
            try {
                hourFormats = bundle.getStringWithFallback("zoneStrings/hourFormat");
            } catch (MissingResourceException e) {
                // fall through
            }
            try {
                _gmtZeroFormat = bundle.getStringWithFallback("zoneStrings/gmtZeroFormat");
            } catch (MissingResourceException e) {
                // fall through
            }
        } catch (MissingResourceException e) {
            // fall through
        }

        if (gmtPattern == null) {
            gmtPattern = DEFAULT_GMT_PATTERN;
        }
        initGMTPattern(gmtPattern);

        String[] gmtOffsetPatterns = new String[GMTOffsetPatternType.values().length];
        if (hourFormats != null) {
            String[] hourPatterns = hourFormats.split(";", 2);
            gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HM.ordinal()] = hourPatterns[0];
            gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HMS.ordinal()] = expandOffsetPattern(hourPatterns[0]);
            gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HM.ordinal()] = hourPatterns[1];
            gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HMS.ordinal()] = expandOffsetPattern(hourPatterns[1]);
        } else {
            for (GMTOffsetPatternType patType : GMTOffsetPatternType.values()) {
                gmtOffsetPatterns[patType.ordinal()] = patType.defaultPattern();
            }
        }
        initGMTOffsetPatterns(gmtOffsetPatterns);

        _gmtOffsetDigits = DEFAULT_GMT_DIGITS;
        NumberingSystem ns = NumberingSystem.getInstance(locale);
        if (!ns.isAlgorithmic()) {
            // we do not support algorithmic numbering system for GMT offset for now
            _gmtOffsetDigits = toCodePoints(ns.getDescription());
        }
    }

    /**
     * Returns a frozen instance of <code>TimeZoneFormat</code> for the given locale.
     * <p><b>Note</b>: The instance returned by this method is frozen. If you want to
     * customize a TimeZoneFormat, you must use {@link #cloneAsThawed()} to get a
     * thawed copy first.
     * 
     * @param locale the locale.
     * @return a frozen instance of <code>TimeZoneFormat</code> for the given locale.
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public static TimeZoneFormat getInstance(ULocale locale) {
        if (locale == null) {
            throw new NullPointerException("locale is null");
        }
        return _tzfCache.getInstance(locale, locale);
    }

    /**
     * Returns the time zone display name data used by this instance.
     * 
     * @return the time zone display name data.
     * @see #setTimeZoneNames(TimeZoneNames)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZoneNames getTimeZoneNames() {
        return _tznames;
    }

    /**
     * Private method returning the instance of TimeZoneGenericNames
     * used by this object. The instance of TimeZoneGenericNames might
     * not be available until the first use (lazy instantiation) because
     * it is only required for handling generic names (that are not used
     * by DateFormat's default patterns) and it requires relatively heavy
     * one time initialization.
     * @return the instance of TimeZoneGenericNames used by this object.
     */
    private TimeZoneGenericNames getTimeZoneGenericNames() {
        if (_gnames == null) { // _gnames is volatile
            synchronized(this) {
                if (_gnames == null) {
                    _gnames = TimeZoneGenericNames.getInstance(_locale);
                }
            }
        }
        return _gnames;
    }

    /**
     * Sets the time zone display name data to this instance.
     * 
     * @param tznames the time zone display name data.
     * @return this object.
     * @throws UnsupportedOperationException when this object is frozen.
     * @see #getTimeZoneNames()
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZoneFormat setTimeZoneNames(TimeZoneNames tznames) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
       _tznames = tznames;
       // TimeZoneGenericNames must be changed to utilize the new TimeZoneNames instance.
       _gnames = new TimeZoneGenericNames(_locale, _tznames);
       return this;
    }

    /**
     * Returns the localized GMT format pattern.
     * 
     * @return the localized GMT format pattern.
     * @see #setGMTPattern(String)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public String getGMTPattern() {
        return _gmtPattern;
    }

    /**
     * Sets the localized GMT format pattern. The pattern must contain
     * a single argument {0}, for example "GMT {0}".
     * 
     * @param pattern the localized GMT format pattern string
     * @return this object.
     * @throws IllegalArgumentException when the pattern string does not contain "{0}"
     * @throws UnsupportedOperationException when this object is frozen.
     * @see #getGMTPattern()
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZoneFormat setGMTPattern(String pattern) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        initGMTPattern(pattern);
        return this;
    }

    /**
     * Returns the offset pattern used for localized GMT format.
     * 
     * @param type the offset pattern enum
     * @see #setGMTOffsetPattern(GMTOffsetPatternType, String)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public String getGMTOffsetPattern(GMTOffsetPatternType type) {
        return _gmtOffsetPatterns[type.ordinal()];
    }

    /**
     * Sets the offset pattern for the given offset type.
     * 
     * @param type the offset pattern.
     * @param pattern the pattern string.
     * @return this object.
     * @throws IllegalArgumentException when the pattern string does not have required time field letters.
     * @throws UnsupportedOperationException when this object is frozen.
     * @see #getGMTOffsetPattern(GMTOffsetPatternType)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZoneFormat setGMTOffsetPattern(GMTOffsetPatternType type, String pattern) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        if (pattern == null) {
            throw new NullPointerException("Null GMT offset pattern");
        }

        Object[] parsedItems = parseOffsetPattern(pattern, type.required());

        _gmtOffsetPatterns[type.ordinal()] = pattern;
        _gmtOffsetPatternItems[type.ordinal()] = parsedItems;

        return this;
    }

    /**
     * Returns the decimal digit characters used for localized GMT format in a single string
     * containing from 0 to 9 in the ascending order.
     * 
     * @return the decimal digits for localized GMT format.
     * @see #setGMTOffsetDigits(String)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public String getGMTOffsetDigits() {
        StringBuilder buf = new StringBuilder(_gmtOffsetDigits.length);
        for (String digit : _gmtOffsetDigits) {
            buf.append(digit);
        }
        return buf.toString();
    }

    /**
     * Sets the decimal digit characters used for localized GMT format.
     * 
     * @param digits a string contains the decimal digit characters from 0 to 9 n the ascending order.
     * @return this object.
     * @throws IllegalArgumentException when the string did not contain ten characters.
     * @throws UnsupportedOperationException when this object is frozen.
     * @see #getGMTOffsetDigits()
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZoneFormat setGMTOffsetDigits(String digits) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        if (digits == null) {
            throw new NullPointerException("Null GMT offset digits");
        }
        String[] digitArray = toCodePoints(digits);
        if (digitArray.length != 10) {
            throw new IllegalArgumentException("Length of digits must be 10");
        }
        _gmtOffsetDigits = digitArray;
        return this;
    }

    /**
     * Returns the localized GMT format string for GMT(UTC) itself (GMT offset is 0).
     * 
     * @return the localized GMT string string for GMT(UTC) itself.
     * @see #setGMTZeroFormat(String)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public String getGMTZeroFormat() {
        return _gmtZeroFormat;
    }

    /**
     * Sets the localized GMT format string for GMT(UTC) itself (GMT offset is 0).
     * 
     * @param gmtZeroFormat the localized GMT format string for GMT(UTC).
     * @return this object.
     * @throws UnsupportedOperationException when this object is frozen.
     * @see #getGMTZeroFormat()
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZoneFormat setGMTZeroFormat(String gmtZeroFormat) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        if (gmtZeroFormat == null) {
            throw new NullPointerException("Null GMT zero format");
        }
        if (gmtZeroFormat.length() == 0) {
            throw new IllegalArgumentException("Empty GMT zero format");
        }
        _gmtZeroFormat = gmtZeroFormat;
        return this;
    }

    /**
     * Sets the default parse options.
     * <p>
     * <b>Note:</b> By default, an instance of <code>TimeZoneFormat></code>
     * created by {#link {@link #getInstance(ULocale)} has no parse options set.
     * 
     * @param options the default parse options.
     * @return this object.
     * @see ParseOption
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZoneFormat setDefaultParseOptions(EnumSet<ParseOption> options) {
        // Currently, only ALL_STYLES is supported
        _parseAllStyles = options.contains(ParseOption.ALL_STYLES);
        return this;
    }

    /**
     * Returns the default parse options used by this <code>TimeZoneFormat</code> instance.
     * @return the default parse options.
     * @see ParseOption
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public EnumSet<ParseOption> getDefaultParseOptions() {
        if (_parseAllStyles) {
            return EnumSet.of(ParseOption.ALL_STYLES);
        }
        return EnumSet.noneOf(ParseOption.class);
    }

    /**
     * Returns the RFC822 style time zone string for the given offset.
     * For example, "-0800".
     * 
     * @param offset the offset from GMT(UTC) in milliseconds.
     * @return the RFC822 style GMT(UTC) offset format.
     * @see #parseOffsetRFC822(String, ParsePosition)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public final String formatOffsetRFC822(int offset) {
        // Note: OffsetFields.HMS as maxFields is an ICU extension. RFC822 specification
        // defines exactly 4 digits for the offset field in HHss format.
        return formatOffsetWithASCIIDigits(offset, null, OffsetFields.HM, OffsetFields.HMS);
    }

    /**
     * Returns the ISO 8601 style (extended format) time zone string for the given offset.
     * For example, "-08:00" and "Z"
     * 
     * @param offset the offset from GMT(UTC) in milliseconds.
     * @return the ISO 8601 style GMT(UTC) offset format.
     * @see #parseOffsetISO8601(String, ParsePosition)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public final String formatOffsetISO8601(int offset) {
        if (offset == 0) {
            return ISO8601_UTC;
        }
        // Note: OffsetFields.HMS as maxFields is an ICU extension. ISO 8601 specification does
        // not support second field.
        return formatOffsetWithASCIIDigits(offset, ':', OffsetFields.HM, OffsetFields.HMS);
    }

    /**
     * Returns the localized GMT(UTC) offset format for the given offset.
     * The localized GMT offset is defined by;
     * <ul>
     * <li>GMT format pattern (e.g. "GMT {0}" - see {@link #getGMTPattern()})
     * <li>Offset time pattern (e.g. "+HH:mm" - see {@link #getGMTOffsetPattern(GMTOffsetPatternType)})
     * <li>Offset digits (e.g. "0123456789" - see {@link #getGMTOffsetDigits()})
     * <li>GMT zero format (e.g. "GMT" - see {@link #getGMTZeroFormat()})
     * </ul>
     * @param offset the offset from GMT(UTC) in milliseconds.
     * @return the localized GMT format string
     * @see #parseOffsetLocalizedGMT(String, ParsePosition)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public String formatOffsetLocalizedGMT(int offset) {
        if (offset == 0) {
            return _gmtZeroFormat;
        }

        StringBuilder buf = new StringBuilder();
        boolean positive = true;
        if (offset < 0) {
            offset = -offset;
            positive = false;
        }

        int offsetH = offset / MILLIS_PER_HOUR;
        offset = offset % MILLIS_PER_HOUR;
        int offsetM = offset / MILLIS_PER_MINUTE;
        offset = offset % MILLIS_PER_MINUTE;
        int offsetS = offset / MILLIS_PER_SECOND;

        if (offsetH > MAX_OFFSET_HOUR || offsetM > MAX_OFFSET_MINUTE || offsetS > MAX_OFFSET_SECOND) {
            throw new IllegalArgumentException("Offset out of range :" + offset);
        }

        Object[] offsetPatternItems;
        if (positive) {
            offsetPatternItems = (offsetS == 0) ?
                    _gmtOffsetPatternItems[GMTOffsetPatternType.POSITIVE_HM.ordinal()] :
                    _gmtOffsetPatternItems[GMTOffsetPatternType.POSITIVE_HMS.ordinal()];
        } else {
            offsetPatternItems = (offsetS == 0) ?
                    _gmtOffsetPatternItems[GMTOffsetPatternType.NEGATIVE_HM.ordinal()] :
                    _gmtOffsetPatternItems[GMTOffsetPatternType.NEGATIVE_HMS.ordinal()];
        }

        // Building the GMT format string
        buf.append(_gmtPatternTokens[0]);

        for (Object item : offsetPatternItems) {
            if (item instanceof String) {
                // pattern literal
                buf.append((String)item);
            } else if (item instanceof GMTOffsetField) {
                // Hour/minute/second field
                GMTOffsetField field = (GMTOffsetField)item;
                switch (field.getType()) {
                case 'H':
                    appendOffsetDigits(buf, offsetH, field.getWidth());
                    break;
                case 'm':
                    appendOffsetDigits(buf, offsetM, field.getWidth());
                    break;
                case 's':
                    appendOffsetDigits(buf, offsetS, field.getWidth());
                    break;
                }
            }
        }
        buf.append(_gmtPatternTokens[1]);
        return buf.toString();
    }

    /**
     * Returns the display name of the time zone at the given date for
     * the style.
     * 
     * <p><b>Note</b>: A style may have fallback styles defined. For example,
     * when <code>GENERIC_LONG</code> is requested, but there is no display name
     * data available for <code>GENERIC_LONG</code> style, the implementation
     * may use <code>GENERIC_LOCATION</code> or <code>LOCALIZED_GMT</code>.
     * See UTS#35 UNICODE LOCALE DATA MARKUP LANGUAGE (LDML)
     * <a href="http://www.unicode.org/reports/tr35/#Time_Zone_Fallback">Appendix J: Time Zone Display Name</a>
     * for the details.
     * 
     * @param style the style enum (e.g. <code>GENERIC_LONG</code>, <code>LOCALIZED_GMT</code>...)
     * @param tz the time zone.
     * @param date the date.
     * @return the display name of the time zone.
     * @see Style
     * @see #format(Style, TimeZone, long, Output)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public final String format(Style style, TimeZone tz, long date) {
        return format(style, tz, date, null);
    }

    /**
     * Returns the display name of the time zone at the given date for
     * the style. This method takes an extra argument <code>Output&lt;TimeType&gt; timeType</code>
     * in addition to the argument list of {@link #format(Style, TimeZone, long)}.
     * The argument is used for receiving the time type (standard time
     * or daylight saving time, or unknown) actually used for the display name.
     * 
     * @param style the style enum (e.g. <code>GENERIC_LONG</code>, <code>LOCALIZED_GMT</code>...)
     * @param tz the time zone.
     * @param date the date.
     * @param timeType the output argument for receiving the time type (standard/daylight/unknown)
     * used for the display name, or specify null if the information is not necessary.
     * @return the display name of the time zone.
     * @see Style
     * @see #format(Style, TimeZone, long)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public String format(Style style, TimeZone tz, long date, Output<TimeType> timeType) {
        String result = null;

        if (timeType != null) {
            timeType.value = TimeType.UNKNOWN;
        }

        switch (style) {
        case GENERIC_LOCATION:
            result = getTimeZoneGenericNames().getGenericLocationName(ZoneMeta.getCanonicalCLDRID(tz));
            break;
        case GENERIC_LONG:
            result = getTimeZoneGenericNames().getDisplayName(tz, GenericNameType.LONG, date);
            break;
        case GENERIC_SHORT:
            result = getTimeZoneGenericNames().getDisplayName(tz, GenericNameType.SHORT, date);
            break;
        case SPECIFIC_LONG:
            result = formatSpecific(tz, NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT, date, timeType);
            break;
        case SPECIFIC_SHORT:
            result = formatSpecific(tz, NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT, date, timeType);
            break;
        case RFC822:
        case ISO8601:
        case LOCALIZED_GMT:
            // will be handled below
            break;
        }

        if (result == null) {
            int[] offsets = {0, 0};
            tz.getOffset(date, false, offsets);
            switch (style) {
            case RFC822:
                result = formatOffsetRFC822(offsets[0] + offsets[1]);
                break;
            case ISO8601:
                result = formatOffsetISO8601(offsets[0] + offsets[1]);
                break;
            default: // Other than RFC822/ISO8601, including fallback from SPECIFIC_XXX/GENERIC_XXX
                result = formatOffsetLocalizedGMT(offsets[0] + offsets[1]);
                break;
            }
            // time type
            if (timeType != null) {
                timeType.value = (offsets[1] != 0) ? TimeType.DAYLIGHT : TimeType.STANDARD;
            }
        }

        assert(result != null);

        return result;
    }

    /**
     * Returns offset from GMT(UTC) in milliseconds for the given RFC822
     * style time zone string. When the given string is not an RFC822 time zone
     * string, this method sets the current position as the error index
     * to <code>ParsePosition pos</code> and returns 0.
     * 
     * @param text the text contains RFC822 style time zone string (e.g. "-0800")
     * at the position.
     * @param pos the position.
     * @return the offset from GMT(UTC) in milliseconds for the given RFC822 style
     * time zone string.
     * @see #formatOffsetRFC822(int)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public final int parseOffsetRFC822(String text, ParsePosition pos) {
        int start = pos.getIndex();
        if (start >= text.length()) {
            pos.setErrorIndex(start);
            return 0;
        }

        int sign;
        char signChar = text.charAt(start);
        if (signChar == '+') {
            sign = 1;
        } else if (signChar == '-') {
            sign = -1;
        } else {
            // Not an RFC822 offset string
            pos.setErrorIndex(start);
            return 0;
        }

        // Parse digits
        pos.setIndex(start + 1);
        int offset = parseContiguousAsciiDigitOffset(text, pos, OffsetFields.H, OffsetFields.HMS, false);

        if (pos.getErrorIndex() != -1) {
            pos.setIndex(start);    // reset
            pos.setErrorIndex(start);
            return 0;
        }

        return sign * offset;
    }

    /**
     * Returns offset from GMT(UTC) in milliseconds for the given ISO 8601 style
     * (extended format) time zone string. When the given string is not an ISO 8601 time
     * zone string, this method sets the current position as the error index
     * to <code>ParsePosition pos</code> and returns 0.
     * 
     * @param text the text contains ISO 8601 style time zone string (e.g. "-08:00", "Z")
     * at the position.
     * @param pos the position.
     * @return the offset from GMT(UTC) in milliseconds for the given ISO 8601 style
     * time zone string.
     * @see #formatOffsetISO8601(int)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public final int parseOffsetISO8601(String text, ParsePosition pos) {
        return parseOffsetISO8601(text, pos, false, null);
    }

    /**
     * Returns offset from GMT(UTC) in milliseconds for the given localized GMT
     * offset format string. When the given string cannot be parsed, this method
     * sets the current position as the error index to <code>ParsePosition pos</code>
     * and returns 0.
     * 
     * @param text the text contains a localized GMT offset string at the position.
     * @param pos the position.
     * @return the offset from GMT(UTC) in milliseconds for the given localized GMT
     * offset format string.
     * @see #formatOffsetLocalizedGMT(int)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public int parseOffsetLocalizedGMT(String text, ParsePosition pos) {
        return parseOffsetLocalizedGMT(text, pos, null);
    }

    /**
     * Returns a <code>TimeZone</code> by parsing the time zone string according to
     * the specified parse position, the style and the parse options.
     * <p>
     * <b>Note:</b>When the input text does not match the specified style, this method
     * evaluate the input using the following order and return the longest match.
     * <ol>
     *   <li>ISO 8601 style time zone format</li>
     *   <li>RFC822 style time zone format</li>
     *   <li>Localized GMT offset format</li>
     *   <li>Time zone display names available for the given <code>style</code> argument</li>
     *   <li>When {@link ParseOption#ALL_STYLES} is enabled in the parse options, all time zone
     *   display names other than the <code>style</code></li>
     * </ol>
     * @param text the text contains a time zone string at the position.
     * @param style the format style
     * @param pos the position.
     * @param options the parse options to be used, or null to use the default parse options.
     * @param timeType The output argument for receiving the time type (standard/daylight/unknown),
     * or specify null if the information is not necessary.
     * @return A <code>TimeZone</code>, or null if the input could not be parsed.
     * @see Style
     * @see ParseOption
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZone parseX(Style style, String text, ParsePosition pos, EnumSet<ParseOption> options, Output<TimeType> timeType) {
        boolean parseAllStyles;
        if (options == null) {
            parseAllStyles = _parseAllStyles;
        } else {
            parseAllStyles = options.contains(ParseOption.ALL_STYLES);
        }

        if (timeType != null) {
            timeType.value = TimeType.UNKNOWN;
        }

        int startIdx = pos.getIndex();
        ParsePosition tmpPos = new ParsePosition(startIdx);

        // try RFC822
        int offset = parseOffsetRFC822(text, tmpPos);
        if (tmpPos.getErrorIndex() < 0) {
            pos.setIndex(tmpPos.getIndex());
            return getTimeZoneForOffset(offset);
        }

        // try Localized GMT
        int gmtZeroLen = 0;
        tmpPos.setErrorIndex(-1);
        tmpPos.setIndex(pos.getIndex());
        Output<Boolean> hasDigitOffset = new Output<Boolean>(false);
        offset = parseOffsetLocalizedGMT(text, tmpPos, hasDigitOffset);
        if (tmpPos.getErrorIndex() < 0) {
            if (hasDigitOffset.value || style == Style.LOCALIZED_GMT || style == Style.RFC822 || tmpPos.getIndex() == text.length()) {
                // When GMT zero format was detected, we won't try other styles if;
                //   1) LOCALIZED_GMT or RFC822 was requested.
                //   2) The input text was fully consumed.
                //
                // Note: Localized GMT format with offset numbers (such as "GMT+03:00") won't collide with other type of names
                // practically. But GMT zero formats (localized one + global ones - "GMT", "UTC", "UT") could - for example,
                // if a locale has a time zone name like "Utah Time", it should not be detected as GMT ("UT" matches the first
                // 2 letters).
                pos.setIndex(tmpPos.getIndex());
                return getTimeZoneForOffset(offset);
            } else {
                // Preserve the length of GMT zero format.
                // If no better matches are found later, GMT should be returned.
                gmtZeroLen = tmpPos.getIndex() - startIdx;
            }
        }

        if (!parseAllStyles && (style == Style.RFC822 || style == Style.LOCALIZED_GMT)) {
            pos.setErrorIndex(pos.getErrorIndex());
            return null;
        }

        // Find the best match within names which are possibly produced by the style
        if (style == Style.SPECIFIC_LONG || style == Style.SPECIFIC_SHORT) {
            // Specific styles
            EnumSet<NameType> nameTypes = null;
            switch (style) {
            case SPECIFIC_LONG:
                nameTypes = EnumSet.of(NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT);
                break;
            case SPECIFIC_SHORT:
                nameTypes = EnumSet.of(NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT);
                break;
            }
            Collection<MatchInfo> specificMatches = _tznames.find(text, startIdx, nameTypes);
            if (specificMatches != null) {
                int matchLen = 0;
                MatchInfo bestSpecific = null;
                for (MatchInfo match : specificMatches) {
                    if (bestSpecific == null || match.matchLength() > matchLen) {
                        bestSpecific = match;
                        matchLen = match.matchLength();
                    }
                }
                if (bestSpecific != null) {
                    if (timeType != null) {
                        timeType.value = getTimeType(bestSpecific.nameType());
                    }
                    pos.setIndex(startIdx + bestSpecific.matchLength());
                    return TimeZone.getTimeZone(getTimeZoneID(bestSpecific.tzID(), bestSpecific.mzID()));
                }
            }
        } else {
            // Generic styles
            assert(style == Style.GENERIC_LOCATION || style == Style.GENERIC_LONG || style == Style.GENERIC_SHORT);
            EnumSet<GenericNameType> genericNameTypes = null;
            switch (style) {
            case GENERIC_LOCATION:
                genericNameTypes = EnumSet.of(GenericNameType.LOCATION);
                break;
            case GENERIC_LONG:
                genericNameTypes = EnumSet.of(GenericNameType.LONG, GenericNameType.LOCATION);
                break;
            case GENERIC_SHORT:
                genericNameTypes = EnumSet.of(GenericNameType.SHORT, GenericNameType.LOCATION);
                break;
            }
            GenericMatchInfo bestGeneric = getTimeZoneGenericNames().findBestMatch(text, startIdx, genericNameTypes);
            if (bestGeneric != null) {
                if (timeType != null) {
                    timeType.value = bestGeneric.timeType();
                }
                pos.setIndex(startIdx + bestGeneric.matchLength());
                return TimeZone.getTimeZone(bestGeneric.tzID());
            }
        }

        // If GMT zero format was detected at the beginning, but there was no better match found
        // in names available for the given style, then GMT is returned here.
        // This should be done before evaluating other names even parseAllStyles is true, because
        // all styles (except RFC822 and LOCALIZED_GMT itself) use LOCALIZED_GMT as the final
        // fallback.
        if (gmtZeroLen > 0) {
            pos.setIndex(startIdx + gmtZeroLen);
            return getTimeZoneForOffset(0);
        }

        // If no match was found above, check if parseAllStyle is enabled.
        // If so, find the longest match in all possible names.

        // For example, when style is GENERIC_LONG, "EST" (SPECIFIC_SHORT) is never
        // used for America/New_York. With parseAllStyles true, this code parses "EST"
        // as America/New_York.
        if (parseAllStyles) {
            int maxMatchLength = text.length() - startIdx;

            // Try specific names first
            Collection<MatchInfo> specificMatches = _tznames.find(text, startIdx, ALL_SPECIFIC_NAME_TYPES);
            MatchInfo bestSpecific = null;
            if (specificMatches != null) {
                int matchLen = 0;
                for (MatchInfo match : specificMatches) {
                    if (bestSpecific == null || match.matchLength() > matchLen) {
                        bestSpecific = match;
                        matchLen = match.matchLength();
                    }
                }
                if (bestSpecific != null && bestSpecific.matchLength() == maxMatchLength) {
                    // complete match
                    if (timeType != null) {
                        timeType.value = getTimeType(bestSpecific.nameType());
                    }
                    pos.setIndex(startIdx + bestSpecific.matchLength());
                    return TimeZone.getTimeZone(getTimeZoneID(bestSpecific.tzID(), bestSpecific.mzID()));
                }
            }

            // Then generic names
            GenericMatchInfo bestGeneric = getTimeZoneGenericNames().findBestMatch(text, startIdx, ALL_GENERIC_NAME_TYPES);

            if (bestSpecific != null || bestGeneric != null) {
                if (bestGeneric == null ||
                        (bestSpecific != null && bestSpecific.matchLength() > bestGeneric.matchLength())) {
                    // the best specific match
                    if (timeType != null) {
                        timeType.value = getTimeType(bestSpecific.nameType());
                    }
                    pos.setIndex(startIdx + bestSpecific.matchLength());
                    return TimeZone.getTimeZone(getTimeZoneID(bestSpecific.tzID(), bestSpecific.mzID()));
                } else if (bestGeneric != null){
                    // the best generic match
                    if (timeType != null) {
                        timeType.value = bestGeneric.timeType();
                    }
                    pos.setIndex(startIdx + bestGeneric.matchLength());
                    return TimeZone.getTimeZone(bestGeneric.tzID());
                }
            }
        }

        pos.setErrorIndex(startIdx);
        return null;
    }

    public TimeZone parse(Style style, String text, ParsePosition pos, EnumSet<ParseOption> options, Output<TimeType> timeType) {
        if (timeType == null) {
            timeType = new Output<TimeType>(TimeType.UNKNOWN);
        } else {
            timeType.value = TimeType.UNKNOWN;
        }

        int startIdx = pos.getIndex();
        int maxPos = text.length();
        int offset;

        boolean fallbackLocalizedGMT = false;
        if (style == Style.SPECIFIC_LONG || style == Style.SPECIFIC_SHORT
                || style == Style.GENERIC_LONG || style == Style.GENERIC_SHORT || style == Style.GENERIC_LOCATION) {
            // above styles may use localized gmt format as fallback
            fallbackLocalizedGMT = true;
        }

        int evaluated = 0;  // bit flags representing already evaluated styles
        ParsePosition tmpPos = new ParsePosition(startIdx);

        int parsedOffset = UNKNOWN_OFFSET;  // stores successfully parsed offset for later use
        int parsedPos = -1;                 // stores successfully parsed offset position for later use

        // Try localized GMT format first if necessary
        if (fallbackLocalizedGMT) {
            Output<Boolean> hasDigitOffset = new Output<Boolean>(false);
            offset = parseOffsetLocalizedGMT(text, tmpPos, hasDigitOffset);
            if (tmpPos.getErrorIndex() == -1) {
                // Even when the input text was successfully parsed as a localized GMT format text,
                // we may still need to evaluate the specified style if -
                //   1) GMT zero format was used, and
                //   2) The input text was not completely processed
                if (tmpPos.getIndex() == maxPos || hasDigitOffset.value) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset);
                }
                parsedOffset = offset;
                parsedPos = tmpPos.getIndex();
            }
            evaluated |= Style.LOCALIZED_GMT.flag;

            tmpPos.setIndex(startIdx);
            tmpPos.setErrorIndex(-1);
        }

        // Try the specified style
        switch (style) {
            case RFC822:
            {
                offset = parseOffsetRFC822(text, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset);
                }
                break;
            }
            case LOCALIZED_GMT:
            {
                offset = parseOffsetLocalizedGMT(text, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset);
                }
                break;
            }
            case ISO8601:
            {
                offset = parseOffsetISO8601(text, tmpPos);
                if (tmpPos.getErrorIndex() == -1) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset);
                }
                // Note: ISO 8601 parser also support basic format (without ':'),
                // which is same with RFC 822 format.
                evaluated |= Style.RFC822.flag;
                break;
            }
            case SPECIFIC_LONG:
            case SPECIFIC_SHORT:
            {
                // Specific styles
                EnumSet<NameType> nameTypes = null;
                switch (style) {
                case SPECIFIC_LONG:
                    nameTypes = EnumSet.of(NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT);
                    break;
                case SPECIFIC_SHORT:
                    nameTypes = EnumSet.of(NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT);
                    break;
                }
                Collection<MatchInfo> specificMatches = _tznames.find(text, startIdx, nameTypes);
                if (specificMatches != null) {
                    MatchInfo specificMatch = null;
                    for (MatchInfo match : specificMatches) {
                        if (startIdx + match.matchLength() > parsedPos) {
                            specificMatch = match;
                            parsedPos = startIdx + match.matchLength();
                        }
                    }
                    if (specificMatch != null) {
                        timeType.value = getTimeType(specificMatch.nameType());
                        pos.setIndex(parsedPos);
                        return TimeZone.getTimeZone(getTimeZoneID(specificMatch.tzID(), specificMatch.mzID()));
                    }
                }
                break;
            }
            case GENERIC_LONG:
            case GENERIC_SHORT:
            case GENERIC_LOCATION:
            {
                EnumSet<GenericNameType> genericNameTypes = null;
                switch (style) {
                case GENERIC_LOCATION:
                    genericNameTypes = EnumSet.of(GenericNameType.LOCATION);
                    break;
                case GENERIC_LONG:
                    genericNameTypes = EnumSet.of(GenericNameType.LONG, GenericNameType.LOCATION);
                    break;
                case GENERIC_SHORT:
                    genericNameTypes = EnumSet.of(GenericNameType.SHORT, GenericNameType.LOCATION);
                    break;
                }
                GenericMatchInfo bestGeneric = getTimeZoneGenericNames().findBestMatch(text, startIdx, genericNameTypes);
                if (bestGeneric != null && (startIdx + bestGeneric.matchLength() > parsedPos)) {
                    timeType.value = bestGeneric.timeType();
                    pos.setIndex(startIdx + bestGeneric.matchLength());
                    return TimeZone.getTimeZone(bestGeneric.tzID());
                }
                break;
            }
        }
        evaluated |= style.flag;

        if (parsedPos > startIdx) {
            // When the specified style is one of SPECIFIC_XXX or GENERIC_XXX, we tried to parse the input
            // as localized GMT format earlier. If parsedOffset is positive, it means it was successfully
            // parsed as localized GMT format, but offset digits were not detected (more specifically, GMT
            // zero format). Then, it tried to find a match within the set of display names, but could not
            // find a match. At this point, we can safely assume the input text contains the localized
            // GMT format.
            assert parsedOffset != UNKNOWN_OFFSET;
            pos.setIndex(parsedPos);
            return getTimeZoneForOffset(parsedOffset);
        }


        // Failed to parse the input text as the time zone format in the specified style.
        // Check the longest match among other styles below.
        assert parsedPos < 0;
        assert parsedOffset == UNKNOWN_OFFSET;
        tmpPos.setIndex(startIdx);
        tmpPos.setErrorIndex(-1);

        // ISO 8601
        if ((evaluated & Style.ISO8601.flag) == 0) {
            Output<Boolean> hasDigitOffset = new Output<Boolean>(false);
            offset = parseOffsetISO8601(text, tmpPos, false, hasDigitOffset);
            if (tmpPos.getErrorIndex() == -1) {
                if (tmpPos.getIndex() == maxPos || hasDigitOffset.value) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset);
                }
                // Note: When ISO 8601 format contains offset digits, it should not
                // collide with other formats (except RFC 822, which is compatible with
                // ISO 8601 basic format). However, ISO 8601 UTC format "Z" (single letter)
                // may collide with other names. In this case, we need to evaluate other
                // names.
                parsedOffset = offset;
                parsedPos = tmpPos.getIndex();
                assert parsedPos == startIdx + 1;   // only when "Z" is used
            }
            tmpPos.setIndex(startIdx);
            tmpPos.setErrorIndex(-1);
        }

        // RFC 822
        // Note: ISO 8601 parser supports RFC 822 format. So we do not need to parse
        // it as RFC 822 here. This might be changed in future when we support
        // strict format option for ISO 8601 or RFC 822. 
//        if ((evaluated & Style.RFC822.flag) == 0) {
//            offset = parseOffsetRFC822(text, tmpPos);
//            if (tmpPos.getErrorIndex() == -1) {
//                pos.setIndex(tmpPos.getIndex());
//                return getTimeZoneForOffset(offset);
//            }
//            tmpPos.setIndex(startIdx);
//            tmpPos.setErrorIndex(-1);
//        }

        // Localized GMT format
        if ((evaluated & Style.LOCALIZED_GMT.flag) == 0) {
            Output<Boolean> hasDigitOffset = new Output<Boolean>(false);
            offset = parseOffsetLocalizedGMT(text, tmpPos, hasDigitOffset);
            if (tmpPos.getErrorIndex() == -1) {
                if (tmpPos.getIndex() == maxPos || hasDigitOffset.value) {
                    pos.setIndex(tmpPos.getIndex());
                    return getTimeZoneForOffset(offset);
                }
                // Evaluate other names - see the comment earlier in this method.
                parsedOffset = offset;
                parsedPos = tmpPos.getIndex();
            }
        }

        // When ParseOption.ALL_STYLES is available, we also try to look all possible display names.
        // For example, when style is GENERIC_LONG, "EST" (SPECIFIC_SHORT) is never
        // used for America/New_York. With parseAllStyles true, this code parses "EST"
        // as America/New_York.

        // Note: Adding all possible names into the trie used by the implementation is quite heavy operation,
        // which we want to avoid normally (note that we cache the trie, so this is applicable to the
        // first time only as long as the cache does not expire).

        boolean parseAllStyles = (options == null) ?
                getDefaultParseOptions().contains(ParseOption.ALL_STYLES)
                : options.contains(ParseOption.ALL_STYLES);

        if (parseAllStyles) {
            // Try all specific names first
            Collection<MatchInfo> specificMatches = _tznames.find(text, startIdx, ALL_SPECIFIC_NAME_TYPES);
            MatchInfo specificMatch = null;
            if (specificMatches != null) {
                for (MatchInfo match : specificMatches) {
                    if (startIdx + match.matchLength() > parsedPos) {
                        specificMatch = match;
                        parsedPos = startIdx + match.matchLength();
                    }
                }
            }

            GenericMatchInfo genericMatch = null;
            if (parsedPos < maxPos) {
                // Try generic names
                genericMatch = getTimeZoneGenericNames().findBestMatch(text, startIdx, ALL_GENERIC_NAME_TYPES);
            }

            // Pick up better match
            if (genericMatch != null && (startIdx + genericMatch.matchLength() > parsedPos)) {
                // use this one
                parsedPos = startIdx + genericMatch.matchLength();
                timeType.value = genericMatch.timeType();
                pos.setIndex(parsedPos);
                return TimeZone.getTimeZone(genericMatch.tzID());
            } else if (specificMatch != null){
                timeType.value = getTimeType(specificMatch.nameType());
                pos.setIndex(parsedPos);
                return TimeZone.getTimeZone(getTimeZoneID(specificMatch.tzID(), specificMatch.mzID()));
            }
        }

        if (parsedPos > startIdx) {
            // Parsed successfully as one of 'offset' format
            assert parsedOffset != UNKNOWN_OFFSET;
            pos.setIndex(parsedPos);
            return getTimeZoneForOffset(parsedOffset);
        }

        pos.setErrorIndex(startIdx);
        return null;
    }

    /**
     * Returns a <code>TimeZone</code> by parsing the time zone string according to
     * the parse position, the style and the default parse options.
     * <p>
     * <b>Note</b>: This method is equivalent to {@link #parse(Style, String, ParsePosition, EnumSet, Output)
     * parse(style, text, pos, null, timeType)}.
     * 
     * @param text the text contains a time zone string at the position.
     * @param style the format style
     * @param pos the position.
     * @param timeType The output argument for receiving the time type (standard/daylight/unknown),
     * or specify null if the information is not necessary.
     * @return A <code>TimeZone</code>, or null if the input could not be parsed.
     * @see Style
     * @see #parse(Style, String, ParsePosition, EnumSet, Output)
     * @see #format(Style, TimeZone, long, Output)
     * @see #setDefaultParseOptions(EnumSet)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZone parse(Style style, String text, ParsePosition pos, Output<TimeType> timeType) {
        return parse(style, text, pos, null, timeType);
    }

    /**
     * Returns a <code>TimeZone</code> by parsing the time zone string according to
     * the given parse position.
     * <p>
     * <b>Note</b>: This method is equivalent to {@link #parse(Style, String, ParsePosition, EnumSet, Output)
     * parse(Style.GENERIC_LOCATION, text, pos, EnumSet.of(ParseOption.ALL_STYLES), timeType)}.
     * 
     * @param text the text contains a time zone string at the position.
     * @param pos the position.
     * @return A <code>TimeZone</code>, or null if the input could not be parsed.
     * @see #parse(Style, String, ParsePosition, EnumSet, Output)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public final TimeZone parse(String text, ParsePosition pos) {
        return parse(Style.GENERIC_LOCATION, text, pos, EnumSet.of(ParseOption.ALL_STYLES), null);
    }

    /**
     * Returns a <code>TimeZone</code> for the given text.
     * <p>
     * <b>Note</b>: The behavior of this method is equivalent to {@link #parse(String, ParsePosition)}.
     * @param text the time zone string
     * @return A <code>TimeZone</code>.
     * @throws ParseException when the input could not be parsed as a time zone string.
     * @see #parse(String, ParsePosition)
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public final TimeZone parse(String text) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        TimeZone tz = parse(text, pos);
        if (pos.getErrorIndex() >= 0) {
            throw new ParseException("Unparseable time zone: \"" + text + "\"" , 0);
        }
        assert(tz != null);
        return tz;
    }

    /**
     * {@inheritDoc}
     * 
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        TimeZone tz = null;
        long date = System.currentTimeMillis();

        if (obj instanceof TimeZone) {
            tz = (TimeZone)obj;
        } else if (obj instanceof Calendar) {
            tz = ((Calendar)obj).getTimeZone();
            date = ((Calendar)obj).getTimeInMillis();
        } else {
            throw new IllegalArgumentException("Cannot format given Object (" +
                    obj.getClass().getName() + ") as a time zone");
        }
        assert(tz != null);
        String result = formatOffsetLocalizedGMT(tz.getOffset(date));
        toAppendTo.append(result);

        if (pos.getFieldAttribute() == DateFormat.Field.TIME_ZONE
                || pos.getField() == DateFormat.TIMEZONE_FIELD) {
            pos.setBeginIndex(0);
            pos.setEndIndex(result.length());
        }
        return toAppendTo;
    }

    /**
     * {@inheritDoc}
     * 
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        StringBuffer toAppendTo = new StringBuffer();
        FieldPosition pos = new FieldPosition(0);
        toAppendTo = format(obj, toAppendTo, pos);

        // supporting only DateFormat.Field.TIME_ZONE
        AttributedString as = new AttributedString(toAppendTo.toString());
        as.addAttribute(DateFormat.Field.TIME_ZONE, DateFormat.Field.TIME_ZONE);

        return as.getIterator();
    }

    /**
     * {@inheritDoc}
     * 
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    @Override
    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }


    /**
     * Private method returning the time zone's specific format string.
     * 
     * @param tz the time zone
     * @param stdType the name type used for standard time
     * @param dstType the name type used for daylight time
     * @param date the date
     * @param timeType when null, actual time type is set
     * @return the time zone's specific format name string
     */
    private String formatSpecific(TimeZone tz, NameType stdType, NameType dstType, long date, Output<TimeType> timeType) {
        assert(stdType == NameType.LONG_STANDARD || stdType == NameType.SHORT_STANDARD);
        assert(dstType == NameType.LONG_DAYLIGHT || dstType == NameType.SHORT_DAYLIGHT);

        boolean isDaylight = tz.inDaylightTime(new Date(date));
        String name = isDaylight?
                getTimeZoneNames().getDisplayName(ZoneMeta.getCanonicalCLDRID(tz), dstType, date) :
                getTimeZoneNames().getDisplayName(ZoneMeta.getCanonicalCLDRID(tz), stdType, date);

        if (name != null && timeType != null) {
            timeType.value = isDaylight ? TimeType.DAYLIGHT : TimeType.STANDARD;
        }
        return name;
    }

    /**
     * Private method returns a time zone ID. If tzID is not null, the value of tzID is returned.
     * If tzID is null, then this method look up a time zone ID for the current region. This is a
     * small helper method used by the parse implementation method
     * 
     * @param tzID
     *            the time zone ID or null
     * @param mzID
     *            the meta zone ID or null
     * @return A time zone ID
     * @throws IllegalArgumentException
     *             when both tzID and mzID are null
     */
    private String getTimeZoneID(String tzID, String mzID) {
        String id = tzID;
        if (id == null) {
            assert (mzID != null);
            id = _tznames.getReferenceZoneID(mzID, getTargetRegion());
            if (id == null) {
                throw new IllegalArgumentException("Invalid mzID: " + mzID);
            }
        }
        return id;
    }

    /**
     * Private method returning the target region. The target regions is determined by
     * the locale of this instance. When a generic name is coming from
     * a meta zone, this region is used for checking if the time zone
     * is a reference zone of the meta zone.
     * 
     * @return the target region
     */
    private synchronized String getTargetRegion() {
        if (_region == null) {
            _region = _locale.getCountry();
            if (_region.length() == 0) {
                ULocale tmp = ULocale.addLikelySubtags(_locale);
                _region = tmp.getCountry();
                if (_region.length() == 0) {
                    _region = "001";
                }
            }
        }
        return _region;
    }

    /**
     * Returns the time type for the given name type
     * @param nameType the name type
     * @return the time type (unknown/standard/daylight)
     */
    private TimeType getTimeType(NameType nameType) {
        switch (nameType) {
        case LONG_STANDARD:
        case SHORT_STANDARD:
            return TimeType.STANDARD;

        case LONG_DAYLIGHT:
        case SHORT_DAYLIGHT:
            return TimeType.DAYLIGHT;
        }
        return TimeType.UNKNOWN;
    }

    /**
     * Parses the localized GMT pattern string and initialize
     * localized gmt pattern fields including {{@link #_gmtPatternTokens}.
     * This method must be also called at deserialization time.
     * 
     * @param gmtPattern the localized GMT pattern string such as "GMT {0}"
     * @throws IllegalArgumentException when the pattern string does not contain "{0}"
     */
    private void initGMTPattern(String gmtPattern) {
        // This implementation not perfect, but sufficient practically.
        int idx = gmtPattern.indexOf("{0}");
        if (idx < 0) {
            throw new IllegalArgumentException("Bad localized GMT pattern: " + gmtPattern);
        }
        _gmtPattern = gmtPattern;
        _gmtPatternTokens = new String[2];
        _gmtPatternTokens[0] = unquote(gmtPattern.substring(0, idx));
        _gmtPatternTokens[1] = unquote(gmtPattern.substring(idx + 3));
    }

    /**
     * Unquotes the message format style pattern
     * 
     * @param s the pattern
     * @return the unquoted pattern string
     */
    private static String unquote(String s) {
        if (s.indexOf('\'') < 0) {
            return s;
        }
        boolean isPrevQuote = false;
        boolean inQuote = false;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                if (isPrevQuote) {
                    buf.append(c);
                    isPrevQuote = false;
                } else {
                    isPrevQuote = true;
                }
                inQuote = !inQuote;
            } else {
                isPrevQuote = false;
                buf.append(c);
            }
        }
        return buf.toString();
    }

    /**
     * Initialize localized GMT format offset hour/min/sec patterns.
     * This method parses patterns into optimized run-time format.
     * This method must be called at deserialization time.
     * 
     * @param gmtOffsetPatterns patterns, String[4]
     * @throws IllegalArgumentException when patterns are not valid
     */
    private void initGMTOffsetPatterns(String[] gmtOffsetPatterns) {
        int size = GMTOffsetPatternType.values().length;
        if (gmtOffsetPatterns.length < size) {
            throw new IllegalArgumentException("Insufficient number of elements in gmtOffsetPatterns");
        }
        Object[][] gmtOffsetPatternItems = new Object[size][];
        for (GMTOffsetPatternType t : GMTOffsetPatternType.values()) {
            int idx = t.ordinal();
            // Note: parseOffsetPattern will validate the given pattern and throws
            // IllegalArgumentException when pattern is not valid
            Object[] parsedItems = parseOffsetPattern(gmtOffsetPatterns[idx], t.required());
            gmtOffsetPatternItems[idx] = parsedItems;
        }

        _gmtOffsetPatterns = new String[size];
        System.arraycopy(gmtOffsetPatterns, 0, _gmtOffsetPatterns, 0, size);
        _gmtOffsetPatternItems = gmtOffsetPatternItems;
    }

    /**
     * Used for representing localized GMT time fields in the parsed pattern object.
     * @see TimeZoneFormat#parseOffsetPattern(String, String)
     */
    private static class GMTOffsetField {
        final char _type;
        final int _width;

        GMTOffsetField(char type, int width) {
            _type = type;
            _width = width;
        }

        char getType() {
            return _type;
        }

        int getWidth() {
            return _width;
        }

        static boolean isValid(char type, int width) {
            switch (type) {
            case 'H':
                return (width == 1 || width == 2);
            case 'm':
            case 's':
                return (width == 2);
            }
            return false;
        }
    }

    /**
     * Parse the GMT offset pattern into runtime optimized format
     * 
     * @param pattern the offset pattern string
     * @param letters the required pattern letters such as "Hm"
     * @return An array of Object. Each array entry is either String (representing
     * pattern literal) or GMTOffsetField (hour/min/sec field)
     */
    private static Object[] parseOffsetPattern(String pattern, String letters) {
        boolean isPrevQuote = false;
        boolean inQuote = false;
        StringBuilder text = new StringBuilder();
        char itemType = 0;  // 0 for string literal, otherwise time pattern character
        int itemLength = 1;
        boolean invalidPattern = false;

        List<Object> items = new ArrayList<Object>();
        BitSet checkBits = new BitSet(letters.length());

        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == '\'') {
                if (isPrevQuote) {
                    text.append('\'');
                    isPrevQuote = false;
                } else {
                    isPrevQuote = true;
                    if (itemType != 0) {
                        if (GMTOffsetField.isValid(itemType, itemLength)) {
                            items.add(new GMTOffsetField(itemType, itemLength));
                        } else {
                            invalidPattern = true;
                            break;
                        }
                        itemType = 0;
                    }
                }
                inQuote = !inQuote;
            } else {
                isPrevQuote = false;
                if (inQuote) {
                    text.append(ch);
                } else {
                    int patFieldIdx = letters.indexOf(ch);
                    if (patFieldIdx >= 0) {
                        // an offset time pattern character
                        if (ch == itemType) {
                            itemLength++;
                        } else {
                            if (itemType == 0) {
                                if (text.length() > 0) {
                                    items.add(text.toString());
                                    text.setLength(0);
                                }
                            } else {
                                if (GMTOffsetField.isValid(itemType, itemLength)) {
                                    items.add(new GMTOffsetField(itemType, itemLength));
                                } else {
                                    invalidPattern = true;
                                    break;
                                }
                            }
                            itemType = ch;
                            itemLength = 1;
                        }
                        checkBits.set(patFieldIdx);
                    } else {
                        // a string literal
                        if (itemType != 0) {
                            if (GMTOffsetField.isValid(itemType, itemLength)) {
                                items.add(new GMTOffsetField(itemType, itemLength));
                            } else {
                                invalidPattern = true;
                                break;
                            }
                            itemType = 0;
                        }
                        text.append(ch);
                    }
                }
            }
        }
        // handle last item
        if (!invalidPattern) {
            if (itemType == 0) {
                if (text.length() > 0) {
                    items.add(text.toString());
                    text.setLength(0);
                }
            } else {
                if (GMTOffsetField.isValid(itemType, itemLength)) {
                    items.add(new GMTOffsetField(itemType, itemLength));
                } else {
                    invalidPattern = true;
                }
            }
        }

        if (invalidPattern || checkBits.cardinality() != letters.length()) {
            throw new IllegalStateException("Bad localized GMT offset pattern: " + pattern);
        }

        return items.toArray(new Object[items.size()]);
    }
    /**
     * Appends second field to the offset pattern with hour/minute
     * 
     * @param offsetHM the offset pattern including hour and minute fields
     * @return the offset pattern including hour, minute and second fields
     */
    //TODO This code will be obsoleted once we add hour-minute-second pattern data in CLDR
    private static String expandOffsetPattern(String offsetHM) {
        int idx_mm = offsetHM.indexOf("mm");
        if (idx_mm < 0) {
            // we cannot do anything with this...
            return offsetHM + ":ss";
        }
        String sep = ":";
        int idx_H = offsetHM.substring(0, idx_mm).lastIndexOf("H");
        if (idx_H >= 0) {
            sep = offsetHM.substring(idx_H + 1, idx_mm);
        }
        return offsetHM.substring(0, idx_mm + 2) + sep + "ss" + offsetHM.substring(idx_mm + 2);
    }

    /**
     * Appends localized digits to the buffer.
     * <p>
     * Note: This code assumes that the input number is 0 - 59
     * 
     * @param buf the target buffer
     * @param n the integer number
     * @param minDigits the minimum digits width
     */
    private void appendOffsetDigits(StringBuilder buf, int n, int minDigits) {
        assert(n >= 0 && n < 60);
        int numDigits = n >= 10 ? 2 : 1;
        for (int i = 0; i < minDigits - numDigits; i++) {
            buf.append(_gmtOffsetDigits[0]);
        }
        if (numDigits == 2) {
            buf.append(_gmtOffsetDigits[n / 10]);
        }
        buf.append(_gmtOffsetDigits[n % 10]);
    }

    /**
     * Creates an instance of TimeZone for the given offset
     * @param offset the offset
     * @return A TimeZone with the given offset
     */
    private TimeZone getTimeZoneForOffset(int offset) {
        if (offset == 0) {
            // when offset is 0, we should use "Etc/GMT"
            return TimeZone.getTimeZone(TZID_GMT);
        }
        return ZoneMeta.getCustomTimeZone(offset);
    }

    /**
     * Returns offset from GMT(UTC) in milliseconds for the given localized GMT
     * offset format string. When the given string cannot be parsed, this method
     * sets the current position as the error index to <code>ParsePosition pos</code>
     * and returns 0.
     * 
     * @param text the text contains a localized GMT offset string at the position.
     * @param pos the position.
     * @param hasDigitOffset receiving if the parsed zone string contains offset digits.
     * @return the offset from GMT(UTC) in milliseconds for the given localized GMT
     * offset format string.
     */
    private int parseOffsetLocalizedGMT(String text, ParsePosition pos, Output<Boolean> hasDigitOffset) {
        int start = pos.getIndex();
        int idx = start;
        boolean parsed = false;
        int offset = 0;

        if (hasDigitOffset != null) {
            hasDigitOffset.value = false;
        }

        do {
            // Prefix part
            int len = _gmtPatternTokens[0].length();
            if (len > 0 && !text.regionMatches(true, idx, _gmtPatternTokens[0], 0, len)) {
                // prefix match failed
                break;
            }
            idx += len;

            // Offset part
            int[] tmpOffset = new int[1];
            int offsetLen = parseGMTOffset(text, idx, false, tmpOffset);
            if (offsetLen == 0) {
                // offset field match failed
                break;
            }
            offset = tmpOffset[0];
            idx += offsetLen;

            // Suffix part
            len = _gmtPatternTokens[1].length();
            if (len > 0 && !text.regionMatches(true, idx, _gmtPatternTokens[1], 0, len)) {
                // no suffix match
                break;
            }
            idx += len;
            parsed = true;

        } while (false);

        if (parsed) {
            if (hasDigitOffset != null) {
                hasDigitOffset.value = true;
            }            pos.setIndex(idx);
            return offset;
        }

        // Try the default patterns
        int[] parsedLength = {0};
        offset = parseDefaultGMT(text, start, parsedLength);
        if (parsedLength[0] > 0) {
            if (hasDigitOffset != null) {
                hasDigitOffset.value = true;
            }            pos.setIndex(start + parsedLength[0]);
            return offset;
        }

        // Check if this is a GMT zero format
        if (text.regionMatches(true, start, _gmtZeroFormat, 0, _gmtZeroFormat.length())) {
            pos.setIndex(start + _gmtZeroFormat.length());
            return 0;
        }

        // Check if this is a default GMT zero format
        for (String defGMTZero : ALT_GMT_STRINGS) {
            if (text.regionMatches(true, start, defGMTZero, 0, defGMTZero.length())) {
                pos.setIndex(start + defGMTZero.length());
                return 0;
            }
        }

        // Nothing matched
        pos.setErrorIndex(start);
        return 0;
    }

    /**
     * Parses localized GMT string into offset.
     * 
     * @param text the input text
     * @param start the start index
     * @param minimumHourWidth the minimum hour width, 1 or 2.
     * @param offset the result offset set to offset[0]
     * @return parsed length
     */
    private int parseGMTOffset(String text, int start, boolean minimumHourWidth, int[] offset) {
        int parsedLen = 0;
        int[] tmpParsedLen = new int[1];
        offset[0] = 0;
        boolean sawVarHourAndAbuttingField = false;

        for (GMTOffsetPatternType gmtPatType : PARSE_GMT_OFFSET_TYPES) {
            int offsetH = 0, offsetM = 0, offsetS = 0;
            int idx = start;
            Object[] items = _gmtOffsetPatternItems[gmtPatType.ordinal()];
            boolean failed = false;
            for (int i = 0; i < items.length; i++) {
                if (items[i] instanceof String) {
                    String patStr = (String)items[i];
                    int len = patStr.length();
                    if (!text.regionMatches(true, idx, patStr, 0, len)) {
                        failed = true;
                        break;
                    }
                    idx += len;
                } else {
                    assert(items[i] instanceof GMTOffsetField);
                    GMTOffsetField field = (GMTOffsetField)items[i];
                    char fieldType = field.getType();
                    if (fieldType == 'H') {
                        int minDigits = 1;
                        int maxDigits = minimumHourWidth ? 1 : 2;
                        if (!minimumHourWidth && !sawVarHourAndAbuttingField) {
                            if (i + 1 < items.length && (items[i] instanceof GMTOffsetField)) {
                                sawVarHourAndAbuttingField = true;
                            }
                        }
                        offsetH = parseOffsetDigits(text, idx, minDigits, maxDigits, 0, MAX_OFFSET_HOUR, tmpParsedLen);
                    } else if (fieldType == 'm') {
                        offsetM = parseOffsetDigits(text, idx, 2, 2, 0, MAX_OFFSET_MINUTE, tmpParsedLen);
                    } else if (fieldType == 's') {
                        offsetS = parseOffsetDigits(text, idx, 2, 2, 0, MAX_OFFSET_SECOND, tmpParsedLen);
                    }

                    if (tmpParsedLen[0] == 0) {
                        failed = true;
                        break;
                    }
                    idx += tmpParsedLen[0];
                }
            }
            if (!failed) {
                int sign = gmtPatType.isPositive() ? 1 : -1;
                offset[0] = ((((offsetH * 60) + offsetM) * 60) + offsetS) * 1000 * sign;
                parsedLen = idx - start;
                break;
            }
        }

        if (parsedLen == 0 && sawVarHourAndAbuttingField && !minimumHourWidth) {
            // When hour field is variable width and another non-literal pattern
            // field follows, the parse loop above might eat up the digit from
            // the abutting field. For example, with pattern "-Hmm" and input "-100",
            // the hour is parsed as -10 and fails to parse minute field.
            //
            // If this is the case, try parsing the text one more time with the arg
            // minimumHourWidth = true
            //
            // Note: This fallback is not applicable when quitAtHourField is true, because
            // the option is designed for supporting the case like "GMT+5". In this case,
            // we should get better result for parsing hour digits as much as possible.

            return parseGMTOffset(text, start, true, offset);
        }

        return parsedLen;
    }

    private int parseDefaultGMT(String text, int start, int[] parsedLength) {
        int idx = start;;
        int offset = 0;
        int parsed = 0;
        do {
            // check global default GMT alternatives
            int gmtLen = 0;
            for (String gmt : ALT_GMT_STRINGS) {
                int len = gmt.length();
                if (text.regionMatches(true, idx, gmt, 0, len)) {
                    gmtLen = len;
                    break;
                }
            }
            if (gmtLen == 0) {
                break;
            }
            idx += gmtLen;

            // offset needs a sign char and a digit at minimum
            if (idx + 1 >= text.length()) {
                break;
            }

            // parse sign
            int sign = 1;
            char c = text.charAt(idx);
            if (c == '+') {
                sign = 1;
            } else if (c == '-') {
                sign = -1;
            } else {
                break;
            }
            idx++;

            // offset part
            // try the default pattern with the separator first
            int[] lenWithSep = {0};
            int offsetWithSep = parseDefaultOffsetFields(text, idx, DEFAULT_GMT_OFFSET_SEP, lenWithSep);
            if (lenWithSep[0] == text.length() - idx) {
                // maximum match
                offset = offsetWithSep * sign;
                idx += lenWithSep[0];
            } else {
                // try abutting field pattern
                int[] lenAbut = {0};
                int offsetAbut = parseAbuttingOffsetFields(text, idx, lenAbut);

                if (lenWithSep[0] > lenAbut[0]) {
                    offset = offsetWithSep * sign;
                    idx += lenWithSep[0];
                } else {
                    offset = offsetAbut * sign;
                    idx += lenAbut[0];
                }
            }
            parsed = idx - start;
        } while (false);

        parsedLength[0] = parsed;
        return offset;
    }

    private int parseDefaultOffsetFields(String text, int start, char separator, int[] parsedLength) {
        int max = text.length();
        int idx = start;
        int[] len = {0};
        int hour = 0, min = 0, sec = 0;

        do {
            hour = parseOffsetDigits(text, idx, 1, 2, 0, MAX_OFFSET_HOUR, len);
            if (len[0] == 0) {
                break;
            }
            idx += len[0];

            if (idx + 1 < max && text.charAt(idx) == separator) {
                min = parseOffsetDigits(text, idx + 1, 2, 2, 0, MAX_OFFSET_MINUTE, len);
                if (len[0] == 0) {
                    break;
                }
                idx += (1 + len[0]);

                if (idx + 1 < max && text.charAt(idx) == separator) {
                    sec = parseOffsetDigits(text, idx + 1, 2, 2, 0, MAX_OFFSET_SECOND, len);
                    if (len[0] == 0) {
                        break;
                    }
                    idx += (1 + len[0]);
                }
            }
        } while (false);

        if (idx == start) {
            parsedLength[0] = 0;
            return 0;
        }

        parsedLength[0] = idx - start;
        return hour * MILLIS_PER_HOUR + min * MILLIS_PER_MINUTE + sec * MILLIS_PER_SECOND;
    }

    private int parseAbuttingOffsetFields(String text, int start, int[] parsedLength) {
        final int MAXDIGITS = 6;
        int[] digits = new int[MAXDIGITS];
        int[] parsed = new int[MAXDIGITS];  // accumulative offsets

        // Parse digits into int[]
        int idx = start;
        int[] len = {0};
        int numDigits = 0;
        for (int i = 0; i < MAXDIGITS; i++) {
            digits[i] = parseSingleDigit(text, idx, len);
            if (digits[i] < 0) {
                break;
            }
            idx += len[0];
            parsed[i] = idx - start;
            numDigits++;
        }

        if (numDigits == 0) {
            parsedLength[0] = 0;
            return 0;
        }

        int offset = 0;
        while (numDigits > 0) {
            int hour = 0;
            int min = 0;
            int sec = 0;

            assert(numDigits > 0 && numDigits <= 6);
            switch (numDigits) {
            case 1: // H
                hour = digits[0];
                break;
            case 2: // HH
                hour = digits[0] * 10 + digits[1];
                break;
            case 3: // Hmm
                hour = digits[0];
                min = digits[1] * 10 + digits[2];
                break;
            case 4: // HHmm
                hour = digits[0] * 10 + digits[1];
                min = digits[2] * 10 + digits[3];
                break;
            case 5: // Hmmss
                hour = digits[0];
                min = digits[1] * 10 + digits[2];
                sec = digits[3] * 10 + digits[4];
                break;
            case 6: // HHmmss
                hour = digits[0] * 10 + digits[1];
                min = digits[2] * 10 + digits[3];
                sec = digits[4] * 10 + digits[5];
                break;
            }
            if (hour <= MAX_OFFSET_HOUR && min <= MAX_OFFSET_MINUTE && sec <= MAX_OFFSET_SECOND) {
                // found a valid combination
                offset = hour * MILLIS_PER_HOUR + min * MILLIS_PER_MINUTE + sec * MILLIS_PER_SECOND;
                parsedLength[0] = parsed[numDigits - 1];
                break;
            }
            numDigits--;
        }
        return offset;
    }

    /**
     * Read an offset field number. This method will stop parsing when
     * 1) number of digits reaches <code>maxDigits</code>
     * 2) just before already parsed number exceeds <code>maxVal</code>
     * 
     * @param text the text
     * @param start the start offset
     * @param minDigits the minimum number of required digits
     * @param maxDigits the maximum number of digits
     * @param minVal the minimum value
     * @param maxVal the maximum value
     * @param parsedLength the actual parsed length is set to parsedLength[0], must not be null.
     * @return the integer value parsed
     */
    private int parseOffsetDigits(String text, int start, int minDigits, int maxDigits,
            int minVal, int maxVal, int[] parsedLength) {

        parsedLength[0] = 0;

        int decVal = 0;
        int numDigits = 0;
        int idx = start;
        int[] digitLen = {0};
        while (idx < text.length() && numDigits < maxDigits) {
            int digit = parseSingleDigit(text, idx, digitLen);
            if (digit < 0) {
                break;
            }
            int tmpVal = decVal * 10 + digit;
            if (tmpVal > maxVal) {
                break;
            }
            decVal = tmpVal;
            numDigits++;
            idx += digitLen[0];
        }

        // Note: maxVal is checked in the while loop
        if (numDigits < minDigits || decVal < minVal) {
            decVal = -1;
            numDigits = 0;
        } else {
            parsedLength[0] = idx - start;
        }


        return decVal;
    }

    private int parseSingleDigit(String text, int offset, int[] len) {
        int digit = -1;
        len[0] = 0;
        if (offset < text.length()) {
            int cp = Character.codePointAt(text, offset);

            // First, try digits configured for this instance
            for (int i = 0; i < _gmtOffsetDigits.length; i++) {
                if (cp == _gmtOffsetDigits[i].codePointAt(0)) {
                    digit = i;
                    break;
                }
            }
            // If failed, check if this is a Unicode digit
            if (digit < 0) {
                digit = UCharacter.digit(cp);
            }

            if (digit >= 0) {
                len[0] = Character.charCount(cp);
            }
        }
        return digit;
    }

    /**
     * Break input String into String[]. Each array element represents
     * a code point. This method is used for parsing localized digit
     * characters and support characters in Unicode supplemental planes.
     * 
     * @param str the string
     * @return the array of code points in String[]
     */
    private static String[] toCodePoints(String str) {
        int len = str.codePointCount(0, str.length());
        String[] codePoints = new String[len];

        for (int i = 0, offset = 0; i < len; i++) {
            int code = str.codePointAt(offset);
            int codeLen = Character.charCount(code);
            codePoints[i] = str.substring(offset, offset + codeLen);
            offset += codeLen;
        }
        return codePoints;
    }


    /**
     * Returns offset from GMT(UTC) in milliseconds for the given ISO 8601 style
     * (extended format) time zone string. When the given string is not an ISO 8601 time
     * zone string, this method sets the current position as the error index
     * to <code>ParsePosition pos</code> and returns 0.
     * 
     * @param text the text contains ISO 8601 style time zone string (e.g. "-08:00", "Z")
     * at the position.
     * @param pos the position.
     * @param extendedOnly <code>true</code> if parsing the text as ISO 8601 extended offset format (e.g. "-08:00"),
     *                     or <code>false</code> to evaluate the text as basic format.
     * @param hasDigitOffset receiving if the parsed zone string contains offset digits.
     * @return the offset from GMT(UTC) in milliseconds for the given ISO 8601 style
     * time zone string.
     */
    private int parseOffsetISO8601(String text, ParsePosition pos, boolean extendedOnly, Output<Boolean> hasDigitOffset) {
        if (hasDigitOffset != null) {
            hasDigitOffset.value = false;
        }
        int start = pos.getIndex();
        if (start >= text.length()) {
            pos.setErrorIndex(start);
            return 0;
        }

        char firstChar = text.charAt(start);
        if (Character.toUpperCase(firstChar) == ISO8601_UTC.charAt(0)) {
            // "Z" - indicates UTC
            pos.setIndex(start + 1);
            return 0;
        }

        int sign;
        if (firstChar == '+') {
            sign = 1;
        } else if (firstChar == '-') {
            sign = -1;
        } else {
            // Not an ISO 8601 offset string
            pos.setErrorIndex(start);
            return 0;
        }
        ParsePosition posOffset = new ParsePosition(start + 1);
        int offset = parseAsciiDigitOffsetWithSeparators(text, posOffset, ':', OffsetFields.H, OffsetFields.HMS, false);
        if (posOffset.getErrorIndex() == -1 && !extendedOnly) {
            // If the text is successfully parsed as extended format with the options above, it can be also parsed
            // as basic format. For example, "0230" can be parsed as offset 2:00 (only first digits are valid for
            // extended format), but it can be parsed as offset 2:30 with basic format. We use longer result.
            ParsePosition posBasic = new ParsePosition(start + 1);
            int tmpOffset = parseContiguousAsciiDigitOffset(text, posBasic, OffsetFields.H, OffsetFields.HMS, false);
            if (posBasic.getErrorIndex() == -1 && posBasic.getIndex() > posOffset.getIndex()) {
                offset = tmpOffset;
                posOffset.setIndex(posBasic.getIndex());
            }
        }

        if (posOffset.getErrorIndex() != -1) {
            pos.setErrorIndex(start);
            return 0;
        }

        pos.setIndex(posOffset.getIndex());
        if (hasDigitOffset != null) {
            hasDigitOffset.value = true;
        }
        return sign * offset;
    }

    /**
     * Numeric offset field combinations
     */
    private enum OffsetFields {
        H, HM, HMS
    }

    /**
     * Format offset using ASCII digits
     * @param offset The offset
     * @param sep The field separator character or null if not required
     * @param minFields The minimum fields
     * @param maxFields The maximum fields
     * @return The offset string
     */
    private static String formatOffsetWithASCIIDigits(int offset, Character sep, OffsetFields minFields, OffsetFields maxFields) {
        assert maxFields.ordinal() >= minFields.ordinal();

        StringBuilder buf = new StringBuilder();
        char sign = '+';
        if (offset < 0) {
            sign = '-';
            offset = -offset;
        }
        buf.append(sign);

        int[] fields = new int[3];
        fields[0] = offset / MILLIS_PER_HOUR;
        offset = offset % MILLIS_PER_HOUR;
        fields[1] = offset / MILLIS_PER_MINUTE;
        offset = offset % MILLIS_PER_MINUTE;
        fields[2] = offset / MILLIS_PER_SECOND;

        assert(fields[0] >= 0 && fields[0] < 100);
        assert(fields[1] >= 0 && fields[1] < 60);
        assert(fields[2] >= 0 && fields[2] < 60);

        int lastIdx = maxFields.ordinal();
        while (lastIdx > minFields.ordinal()) {
            if (fields[lastIdx] != 0) {
                break;
            }
            lastIdx--;
        }

        for (int idx = 0; idx <= lastIdx; idx++) {
            if (sep != null && idx != 0) {
                buf.append(sep);
            }
            if (fields[idx] < 10) {
                buf.append('0');
            }
            buf.append(fields[idx]);
        }
        return buf.toString();
    }

    /**
     * Parse offset represented by contiguous ASCII digits
     * <p>
     * Note: This method expects the input position is already at the start of
     * ASCII digits and does not parse sign (+/-).
     * 
     * @param text The text contains a sequence of ASCII digits
     * @param pos The parse position
     * @param minFields The minimum Fields to be parsed
     * @param maxFields The maximum Fields to be parsed
     * @param fixedHourWitdh true if hour field must be width of 2
     * @return Parsed offset, 0 or positive number.
     */
    private int parseContiguousAsciiDigitOffset(String text, ParsePosition pos,
            OffsetFields minFields, OffsetFields maxFields, boolean fixedHourWitdh) {
        int start = pos.getIndex();

        int minDigits = 2 * (minFields.ordinal() + 1) - (fixedHourWitdh ? 0 : 1);
        int maxDigits = 2 * (maxFields.ordinal() + 1);

        int[] digits = new int[maxDigits];
        int numDigits = 0;
        int idx = start;
        while (numDigits < digits.length && idx < text.length()) {
            int digit = ASCII_DIGITS.indexOf(text.charAt(idx));
            if (digit < 0) {
                break;
            }
            digits[numDigits] = digit;
            numDigits++;
            idx++;
        }

        if (fixedHourWitdh && (numDigits % 2 != 0)) {
            // Fixed digits, so the number of digits must be even number. Truncating.
            numDigits--;
        }

        if (numDigits < minDigits) {
            pos.setErrorIndex(start);
            return 0;
        }

        int hour = 0, min = 0, sec = 0;
        boolean bParsed = false;
        while (numDigits >= minDigits) {
            switch (numDigits) {
            case 1: //H
                hour = digits[0];
                break;
            case 2: //HH
                hour = digits[0] * 10 + digits[1];
                break;
            case 3: //Hmm
                hour = digits[0];
                min = digits[1] * 10 + digits[2];
                break;
            case 4: //HHmm
                hour = digits[0] * 10 + digits[1];
                min = digits[2] * 10 + digits[3];
                break;
            case 5: //Hmmss
                hour = digits[0];
                min = digits[1] * 10 + digits[2];
                sec = digits[3] * 10 + digits[4];
                break;
            case 6: //HHmmss
                hour = digits[0] * 10 + digits[1];
                min = digits[2] * 10 + digits[3];
                sec = digits[4] * 10 + digits[5];
                break;
            }

            if (hour <= MAX_OFFSET_HOUR && min <= MAX_OFFSET_MINUTE && sec <= MAX_OFFSET_SECOND) {
                // Successfully parsed
                bParsed = true;
                break;
            }

            // Truncating
            numDigits -= (fixedHourWitdh ? 2 : 1);
            hour = min = sec = 0;
        }

        if (!bParsed) {
            pos.setErrorIndex(start);
            return 0;
        }
        pos.setIndex(start + numDigits);
        return ((((hour * 60) + min) * 60) + sec) * 1000;
    }

    /**
     * Parse offset represented by ASCII digits and separators.
     * <p>
     * Note: This method expects the input position is already at the start of
     * ASCII digits and does not parse sign (+/-).
     * 
     * @param text The text
     * @param pos The parse position
     * @param sep The separator character
     * @param minFields The minimum Fields to be parsed
     * @param maxFields The maximum Fields to be parsed
     * @param fixedHourWitdh true if hour field must be width of 2
     * @return Parsed offset, 0 or positive number.
     */
    private int parseAsciiDigitOffsetWithSeparators(String text, ParsePosition pos,
            char sep, OffsetFields minFields, OffsetFields maxFields, boolean fixedHourWidth) {
        int start = pos.getIndex();
        int[] fieldVal = {0, 0, 0};
        int[] fieldLen = {0, -1, -1};
        for (int idx = start, fieldIdx = 0; idx < text.length() && fieldIdx <= maxFields.ordinal(); idx++) {
            char c = text.charAt(idx);
            if (c == sep) {
                if (fieldLen[fieldIdx] < 0) {
                    // next field - expected
                    fieldLen[fieldIdx] = 0;
                } else if (fieldIdx == 0 && !fixedHourWidth) {
                    // 1 digit hour, move to next field
                    fieldIdx++;
                    fieldLen[fieldIdx] = 0;
                } else {
                    // otherwise, premature field
                    break;
                }
                continue;
            }
            int digit = ASCII_DIGITS.indexOf(c);
            if (digit < 0) {
                // not a digit
                break;
            }
            fieldVal[fieldIdx] = fieldVal[fieldIdx] * 10 + digit;
            fieldLen[fieldIdx]++;
            if (fieldLen[fieldIdx] >= 2) {
                // parsed 2 digits, move to next field
                fieldIdx++;
            }
        }

        int offset = 0;
        int parsedLen = 0;
        OffsetFields parsedFields = null;
        do {
            // hour
            if (fieldLen[0] == 0 || (fieldLen[0] == 1 && fixedHourWidth)) {
                break;
            }
            if (fieldVal[0] > MAX_OFFSET_HOUR) {
                if (fixedHourWidth) {
                    break;
                }
                offset = (fieldVal[0] / 10) * MILLIS_PER_HOUR;
                parsedFields = OffsetFields.H;
                parsedLen = 1;
                break;
            }
            offset = fieldVal[0] * MILLIS_PER_HOUR;
            parsedLen = fieldLen[0];
            parsedFields = OffsetFields.H;

            // minute
            if (fieldLen[1] != 2 || fieldVal[1] > MAX_OFFSET_MINUTE) {
                break;
            }
            offset += fieldVal[1] * MILLIS_PER_MINUTE;
            parsedLen += (1 + fieldLen[1]);
            parsedFields = OffsetFields.HM;

            // second
            if (fieldLen[2] != 2 || fieldVal[2] > MAX_OFFSET_SECOND) {
                break;
            }
            offset += fieldVal[2] * MILLIS_PER_SECOND;
            parsedLen += (1 + fieldLen[2]);
            parsedFields = OffsetFields.HMS;
        } while (false);

        if (parsedFields == null || parsedFields.ordinal() < minFields.ordinal()) {
            pos.setErrorIndex(start);
            return 0;
        }

        pos.setIndex(start + parsedLen);
        return offset;
    }


    /**
     * Implements <code>TimeZoneFormat</code> object cache
     */
    private static class TimeZoneFormatCache extends SoftCache<ULocale, TimeZoneFormat, ULocale> {

        /* (non-Javadoc)
         * @see com.ibm.icu.impl.CacheBase#createInstance(java.lang.Object, java.lang.Object)
         */
        @Override
        protected TimeZoneFormat createInstance(ULocale key, ULocale data) {
            TimeZoneFormat fmt = new TimeZoneFormat(data);
            fmt.freeze();
            return fmt;
        }
    }

    // ----------------------------------
    // Serialization stuff
    //-----------------------------------

    /**
     * @serialField _locale ULocale The locale of this TimeZoneFormat object.
     * @serialField _tznames TimeZoneNames The time zone name data.
     * @serialField _gmtPattern String The pattern string for localized GMT format.
     * @serialField _gmtOffsetPatterns Stirng[] The array of GMT offset patterns used by localized GMT format
     *              (positive hour-min, positive hour-min-sec, negative hour-min, negative hour-min-sec).
     * @serialField _gmtOffsetDigits String[] The array of decimal digits used by localized GMT format
     *              (the size of array is 10).
     * @serialField _gmtZeroFormat String The localized GMT string used for GMT(UTC).
     * @serialField _parseAllStyles boolean <code>true</code> if this TimeZoneFormat object is configure
     *              for parsing all available names.
     */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("_locale", ULocale.class),
        new ObjectStreamField("_tznames", TimeZoneNames.class),
        new ObjectStreamField("_gmtPattern", String.class),
        new ObjectStreamField("_gmtOffsetPatterns", String[].class),
        new ObjectStreamField("_gmtOffsetDigits", String[].class),
        new ObjectStreamField("_gmtZeroFormat", String.class),
        new ObjectStreamField("_parseAllStyles", boolean.class),
    };

    /**
     * 
     * @param oos the object output stream
     * @throws IOException
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        ObjectOutputStream.PutField fields = oos.putFields();

        fields.put("_locale", _locale);
        fields.put("_tznames", _tznames);
        fields.put("_gmtPattern", _gmtPattern);
        fields.put("_gmtOffsetPatterns", _gmtOffsetPatterns);
        fields.put("_gmtOffsetDigits", _gmtOffsetDigits);
        fields.put("_gmtZeroFormat", _gmtZeroFormat);
        fields.put("_parseAllStyles", _parseAllStyles);

        oos.writeFields();
    }

    /**
     * 
     * @param ois the object input stream
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ObjectInputStream.GetField fields = ois.readFields();

        _locale = (ULocale)fields.get("_locale", null);
        if (_locale == null) {
            throw new InvalidObjectException("Missing field: locale");
        }

        _tznames = (TimeZoneNames)fields.get("_tznames", null);
        if (_tznames == null) {
            throw new InvalidObjectException("Missing field: tznames");
        }

        _gmtPattern = (String)fields.get("_gmtPattern", null);
        if (_gmtPattern == null) {
            throw new InvalidObjectException("Missing field: gmtPattern");
        }

        _gmtOffsetPatterns = (String[])fields.get("_gmtOffsetPatterns", null);
        if (_gmtOffsetPatterns == null) {
            throw new InvalidObjectException("Missing field: gmtOffsetPatterns");
        } else if (_gmtOffsetPatterns.length < 4) {
            throw new InvalidObjectException("Incompatible field: gmtOffsetPatterns");
        }

        _gmtOffsetDigits = (String[])fields.get("_gmtOffsetDigits", null);
        if (_gmtOffsetDigits == null) {
            throw new InvalidObjectException("Missing field: gmtOffsetDigits");
        } else if (_gmtOffsetDigits.length != 10) {
            throw new InvalidObjectException("Incompatible field: gmtOffsetDigits");
        }

        _gmtZeroFormat = (String)fields.get("_gmtZeroFormat", null);
        if (_gmtZeroFormat == null) {
            throw new InvalidObjectException("Missing field: gmtZeroFormat");
        }

        _parseAllStyles = fields.get("_parseAllStyles", false);
        if (fields.defaulted("_parseAllStyles")) {
            throw new InvalidObjectException("Missing field: parseAllStyles");
        }

        // Optimization for TimeZoneNames
        //
        // Note:
        //
        // com.ibm.icu.impl.TimeZoneNamesImpl is a read-only object initialized
        // by locale only. But it loads time zone names from resource bundles and
        // builds trie for parsing. We want to keep TimeZoneNamesImpl as singleton
        // per locale. We cannot do this for custom TimeZoneNames provided by user.
        //
        // com.ibm.icu.impl.TimeZoneGenericNames is a runtime generated object
        // initialized by ULocale and TimeZoneNames. Like TimeZoneNamesImpl, it
        // also composes time zone names and trie for parsing. We also want to keep
        // TimeZoneGenericNames as siongleton per locale. If TimeZoneNames is
        // actually a TimeZoneNamesImpl, we can reuse cached TimeZoneGenericNames
        // instance.
        if (_tznames instanceof TimeZoneNamesImpl) {
            _tznames = TimeZoneNames.getInstance(_locale);
            _gnames = null; // will be created by _locale later when necessary
        } else {
            // Custom TimeZoneNames implementation is used. We need to create
            // a new instance of TimeZoneGenericNames here.
            _gnames = new TimeZoneGenericNames(_locale, _tznames);
        }

        // Transient fields requiring initialization
        initGMTPattern(_gmtPattern);
        initGMTOffsetPatterns(_gmtOffsetPatterns);

    }

    // ----------------------------------
    // Freezable stuff
    //-----------------------------------

    /**
     * {@inheritDoc}
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public boolean isFrozen() {
        return _frozen;
    }

    /**
     * {@inheritDoc}
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZoneFormat freeze() {
        _frozen = true;
        return this;
    }

    /**
     * {@inheritDoc}
     * @draft ICU 49
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZoneFormat cloneAsThawed() {
        TimeZoneFormat copy = (TimeZoneFormat)super.clone();
        copy._frozen = false;
        return copy;
    }
}


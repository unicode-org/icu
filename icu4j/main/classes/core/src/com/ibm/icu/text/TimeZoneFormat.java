/*
 *******************************************************************************
 * Copyright (C) 2011, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.IOException;
import java.io.ObjectInputStream;
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
 * @internal ICU 4.8 technology preview
 * @deprecated This API might change or be removed in a future release.
 */
public class TimeZoneFormat extends UFormat implements Freezable<TimeZoneFormat>, Serializable {

    private static final long serialVersionUID = 2281246852693575022L;

    /**
     * Time zone display format style enum used by format/parse APIs in <code>TimeZoneFormat</code>.
     * 
     * @see TimeZoneFormat#format(Style, TimeZone, long)
     * @see TimeZoneFormat#format(Style, TimeZone, long, Output)
     * @see TimeZoneFormat#parse(Style, String, ParsePosition, Output)
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public enum Style {
        /**
         * Generic location format, such as "United States Time (New York)", "Italy Time"
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        GENERIC_LOCATION,
        /**
         * Generic long non-location format, such as "Eastern Time".
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        GENERIC_LONG,
        /**
         * Generic short non-location format, such as "ET".
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        GENERIC_SHORT,
        /**
         * Specific long format, such as "Eastern Standard Time".
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        SPECIFIC_LONG,
        /**
         * Specific short format, such as "EST", "PDT".
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        SPECIFIC_SHORT,
        /**
         * RFC822 format, such as "-0500"
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        RFC822,
        /**
         * Localized GMT offset format, such as "GMT-05:00", "UTC+0100"
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        LOCALIZED_GMT,
        /**
         * Specific short format, such as "EST", "PDT".
         * <p><b>Note</b>: This is a variant of {@link #SPECIFIC_SHORT}, but
         * excluding short abbreviations not commonly recognized by people
         * for the locale.
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        SPECIFIC_SHORT_COMMONLY_USED;
    }

    /**
     * Offset pattern type enum.
     * 
     * @see TimeZoneFormat#getGMTOffsetPattern(GMTOffsetPatternType)
     * @see TimeZoneFormat#setGMTOffsetPattern(GMTOffsetPatternType, String)
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public enum GMTOffsetPatternType {
        /**
         * Positive offset with hour and minute fields
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        POSITIVE_HM ("+HH:mm", "Hm", true),
        /**
         * Positive offset with hour, minute and second fields
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        POSITIVE_HMS ("+HH:mm:ss", "Hms", true),
        /**
         * Negative offset with hour and minute fields
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        NEGATIVE_HM ("-HH:mm", "Hm", false),
        /**
         * Negative offset with hour, minute and second fields
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public enum TimeType {
        /**
         * Unknown
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        UNKNOWN,
        /**
         * Standard time
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        STANDARD,
        /**
         * Daylight saving time
         * @internal ICU 4.8 technology preview
         * @deprecated This API might change or be removed in a future release.
         */
        DAYLIGHT;
    }

    /*
     * Serialized fields
     */
    private ULocale _locale;
    private TimeZoneNames _tznames;
    private volatile TimeZoneGenericNames _gnames;
    private String _gmtPattern;
    private String[] _gmtOffsetPatterns;
    private String[] _gmtOffsetDigits;
    private String _gmtZeroFormat;
    private boolean _parseAllStyles;

    /*
     * Transient fields
     */
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
    private static final String RFC822_DIGITS = "0123456789";

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

    private static TimeZoneFormatCache _tzfCache = new TimeZoneFormatCache();

    // The filter used for searching all specific names
    private static final EnumSet<NameType> ALL_SPECIFIC_NAME_TYPES = EnumSet.of(
        NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT,
        NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT,
        NameType.SHORT_STANDARD_COMMONLY_USED, NameType.SHORT_DAYLIGHT_COMMONLY_USED
    );

    // The filter used for searching all generic names
    private static final EnumSet<GenericNameType> ALL_GENERIC_NAME_TYPES = EnumSet.of(
        GenericNameType.LOCATION, GenericNameType.LONG, GenericNameType.SHORT
    );

    /**
     * The protected constructor for subclassing.
     * @param locale the locale
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @return the offset pattern enum.
     * @see #setGMTOffsetPattern(GMTOffsetPatternType, String)
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public String getGMTOffsetPattern(GMTOffsetPatternType type) {
        return _gmtOffsetPatterns[type.ordinal()];
    }

    /**
     * Sets the offset pattern for the given offset type.
     * 
     * @param type the offset pettern.
     * @param pattern the pattern string.
     * @return this object.
     * @throws IllegalArgumentException when the pattern string does not have required time field letters.
     * @throws UnsupportedOperationException when this object is frozen.
     * @see #getGMTOffsetPattern(GMTOffsetPatternType)
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public String getGMTZeroFormat() {
        return _gmtZeroFormat;
    }

    /**
     * Returns the localized GMT format string for GMT(UTC) itself (GMT offset is 0).
     * 
     * @param gmtZeroFormat the localized GMT format string for GMT(UTC).
     * @return this object.
     * @throws UnsupportedOperationException when this object is frozen.
     * @see #getGMTZeroFormat()
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * Returns <code>true</code> when this <code>TimeZoneFormat</code> is configured for parsing
     * display names including names that are only used by other styles by
     * {@link #parse(Style, String, ParsePosition, Output)}.
     * <p><b>Note</b>: An instance created by {@link #getInstance(ULocale)} is configured NOT
     * parsing all styles (<code>false</code>).
     * 
     * @return <code>true</code> when this instance is configure for parsing all available names.
     * @see #setParseAllStyles(boolean)
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public boolean isParseAllStyles() {
        return _parseAllStyles;
    }

    /**
     * Sets if {@link #parse(Style, String, ParsePosition, Output)} to parse display
     * names including names that are only used by other styles.
     * 
     * @param parseAllStyles <code>true</code> to parse all available names.
     * @return this object.
     * @throws UnsupportedOperationException when this object is frozen.
     * @see #isParseAllStyles()
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public TimeZoneFormat setParseAllStyles(boolean parseAllStyles) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        _parseAllStyles = parseAllStyles;
        return this;
    }

    /**
     * Returns the RFC822 style time zone string for the given offset.
     * For example, "-0800".
     * 
     * @param offset the offset for GMT(UTC) in milliseconds.
     * @return the RFC822 style GMT(UTC) offset format.
     * @see #parseOffsetRFC822(String, ParsePosition)
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public final String formatOffsetRFC822(int offset) {
        StringBuilder buf = new StringBuilder();
        char sign = '+';
        if (offset < 0) {
            sign = '-';
            offset = -offset;
        }
        buf.append(sign);

        int offsetH = offset / MILLIS_PER_HOUR;
        offset = offset % MILLIS_PER_HOUR;
        int offsetM = offset / MILLIS_PER_MINUTE;
        offset = offset % MILLIS_PER_MINUTE;
        int offsetS = offset / MILLIS_PER_SECOND;

        assert(offsetH >= 0 && offsetH < 100);
        assert(offsetM >= 0 && offsetM < 60);
        assert(offsetS >= 0 && offsetS < 60);

        int num = 0, denom = 0;
        if (offsetS == 0) {
            offset = offsetH * 100 + offsetM; // HHmm
            num = offset % 10000;
            denom = 1000;
        } else {
            offset = offsetH * 10000 + offsetM * 100 + offsetS; //HHmmss
            num = offset % 1000000;
            denom = 100000;
        }
        while (denom >= 1) {
            char digit = (char)((num / denom) + '0');
            buf.append(digit);
            num = num % denom;
            denom /= 10;
        }
        return buf.toString();
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
        case SPECIFIC_SHORT_COMMONLY_USED:
            result = formatSpecific(tz, NameType.SHORT_STANDARD_COMMONLY_USED, NameType.SHORT_DAYLIGHT_COMMONLY_USED, date, timeType);
            break;
        case RFC822:
        case LOCALIZED_GMT:
            // will be handled below
            break;
        }

        if (result == null) {
            int[] offsets = {0, 0};
            tz.getOffset(date, false, offsets);
            if (style == Style.RFC822) {
                // RFC822 was requested
                result = formatOffsetRFC822(offsets[0] + offsets[1]);
            } else {
                // LOCALIZED_GMT was requested, or fallback for other types
                result = formatOffsetLocalizedGMT(offsets[0] + offsets[1]);
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public final int parseOffsetRFC822(String text, ParsePosition pos) {
        int start = pos.getIndex();

        if (start + 2 >= text.length()) {
            // minimum 2 characters
            pos.setErrorIndex(start);
            return 0;
        }

        int len = 0;

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
        len++;

        // Parse digits
        // Possible format (excluding sign char) are:
        // HHmmss
        // HmmSS
        // HHmm
        // Hmm
        // HH
        // H
        int idx = start + 1;
        int numDigits = 0;
        int[] digits = new int[6];
        while (numDigits < digits.length && idx < text.length()) {
            int digit = RFC822_DIGITS.indexOf(text.charAt(idx));
            if (digit < 0) {
                break;
            }
            digits[numDigits] = digit;
            numDigits++;
            idx++;
        }

        if (numDigits == 0) {
            // Not an RFC822 offset string
            pos.setErrorIndex(start);
            return 0;
        }

        int hour = 0, min = 0, sec = 0;
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

        if (hour > MAX_OFFSET_HOUR || min > MAX_OFFSET_MINUTE || sec > MAX_OFFSET_SECOND) {
            // Invalid value range
            pos.setErrorIndex(start);
            return 0;
        }

        pos.setIndex(1 + numDigits);
        return ((((hour * 60) + min) * 60) + sec) * 1000 * sign;
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public int parseOffsetLocalizedGMT(String text, ParsePosition pos) {
        return parseOffsetLocalizedGMT(text, pos, null);
    }

    /**
     * Returns a <code>TimeZone</code> by parsing the time zone string according to
     * the given parse position.
     * 
     * <p><b>Note</b>: By default, this method supports 1) RFC822 style time zone format,
     * 2) Localized GMT offset format and 3) all display names that are used for the
     * given <code>style</code>. If you want to parse all display names including names that are
     * only used for styles other than the specified <code>style</code>, then you should
     * set true to {@link #setParseAllStyles(boolean)}.
     * 
     * @param text the text contains a time zone string at the position.
     * @param style the format style
     * @param pos the position.
     * @param timeType The output argument for receiving the time type (standard/daylight/unknown),
     * or specify null if the information is not necessary.
     * @return A <code>TimeZone</code>, or null if the input could not be parsed.
     * @see Style
     * @see #format(Style, TimeZone, long, Output)
     * @see #setParseAllStyles(boolean)
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public TimeZone parse(Style style, String text, ParsePosition pos, Output<TimeType> timeType) {
        return parse(style, text, pos, _parseAllStyles, timeType);
    }

    /**
     * Returns a <code>TimeZone</code> by parsing the time zone string according to
     * the given parse position.
     * 
     * <p><b>Note</b>: This method is equivalent to <code>parse(Style.GENERIC_LOCATION,
     * text, pos, null)</code> with {@link #setParseAllStyles(boolean) setParseAllStyles(true)}.
     * 
     * @param text the text contains a time zone string at the position.
     * @param pos the position.
     * @return A <code>TimeZone</code>, or null if the input could not be parsed.
     * @see #parse(Style, String, ParsePosition, Output)
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public final TimeZone parse(String text, ParsePosition pos) {
        return parse(Style.GENERIC_LOCATION, text, pos, true, null);
    }

    /**
     * Returns a <code>TimeZone</code> for the given text.
     * @param text the time zone string
     * @return A <code>TimeZone</code>.
     * @throws ParseException when the input could not be parsed as a time zone string.
     * @see #parse(String, ParsePosition)
     * @see #parse(Style, String, ParsePosition, Output)
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
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
        assert(stdType == NameType.LONG_STANDARD || stdType == NameType.SHORT_STANDARD || stdType == NameType.SHORT_STANDARD_COMMONLY_USED);
        assert(dstType == NameType.LONG_DAYLIGHT || dstType == NameType.SHORT_DAYLIGHT || dstType == NameType.SHORT_DAYLIGHT_COMMONLY_USED);

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
     * Private method implementing the parse logic
     * 
     * @param style the preferred style.
     * @param text the input text.
     * @param pos the parse position.
     * @param parseAllStyles true if parse other names when a match is not found within names
     * used by the preferred style.
     * @param timeType receiving parsed time type (unknown/standard/daylight). If not necessary, specify null.
     * @return the result time zone
     */
    private TimeZone parse(Style style, String text, ParsePosition pos, boolean parseAllStyles, Output<TimeType> timeType) {
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
        boolean[] isGMTZero = {false};
        offset = parseOffsetLocalizedGMT(text, tmpPos, isGMTZero);
        if (tmpPos.getErrorIndex() < 0) {
            if (!isGMTZero[0] || style == Style.LOCALIZED_GMT || style == Style.RFC822 || tmpPos.getIndex() == text.length()) {
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
        if (style == Style.SPECIFIC_LONG || style == Style.SPECIFIC_SHORT || style == Style.SPECIFIC_SHORT_COMMONLY_USED) {
            // Specific styles
            EnumSet<NameType> nameTypes = null;
            switch (style) {
            case SPECIFIC_LONG:
                nameTypes = EnumSet.of(NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT);
                break;
            case SPECIFIC_SHORT:
                nameTypes = EnumSet.of(NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT);
                break;
            case SPECIFIC_SHORT_COMMONLY_USED:
                nameTypes = EnumSet.of(NameType.SHORT_STANDARD_COMMONLY_USED, NameType.SHORT_DAYLIGHT_COMMONLY_USED);
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
        case SHORT_STANDARD_COMMONLY_USED:
            return TimeType.STANDARD;

        case LONG_DAYLIGHT:
        case SHORT_DAYLIGHT:
        case SHORT_DAYLIGHT_COMMONLY_USED:
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
     * @param isGMTZero receiving if the GMT zero format was detected. Note that
     * the string with offset digits is not a GMT zero format. For example, when "GMT+00:00"
     * is found, this method won't set true to isGMTZero[0].
     * @return the offset from GMT(UTC) in milliseconds for the given localized GMT
     * offset format string.
     */
    private int parseOffsetLocalizedGMT(String text, ParsePosition pos, boolean[] isGMTZero) {
        int start = pos.getIndex();
        int idx = start;
        boolean parsed = false;
        int offset = 0;

        if (isGMTZero != null && isGMTZero.length > 0) {
            isGMTZero[0] = false;
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
            pos.setIndex(idx);
            return offset;
        }

        // Try the default patterns
        int[] parsedLength = {0};
        offset = parseDefaultGMT(text, start, parsedLength);
        if (parsedLength[0] > 0) {
            pos.setIndex(start + parsedLength[0]);
            return offset;
        }

        // Check if this is a GMT zero format
        if (text.regionMatches(true, start, _gmtZeroFormat, 0, _gmtZeroFormat.length())) {
            pos.setIndex(start + _gmtZeroFormat.length());
            if (isGMTZero != null && isGMTZero.length > 0) {
                isGMTZero[0] = true;
            }
            return 0;
        }

        // Check if this is a default GMT zero format
        for (String defGMTZero : ALT_GMT_STRINGS) {
            if (text.regionMatches(true, start, defGMTZero, 0, defGMTZero.length())) {
                pos.setIndex(start + defGMTZero.length());
                if (isGMTZero != null && isGMTZero.length > 0) {
                    isGMTZero[0] = true;
                }
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
     * Custom readObject for initializing transient fields.
     * 
     * @param ois the object input stream
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();

        initGMTPattern(_gmtPattern);
        initGMTOffsetPatterns(_gmtOffsetPatterns);
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

    /**
     * {@inheritDoc}
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public boolean isFrozen() {
        return _frozen;
    }

    /**
     * {@inheritDoc}
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public TimeZoneFormat freeze() {
        _frozen = true;
        return this;
    }

    /**
     * {@inheritDoc}
     * @internal ICU 4.8 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    public TimeZoneFormat cloneAsThawed() {
        TimeZoneFormat copy = (TimeZoneFormat)super.clone();
        copy._frozen = false;
        return copy;
    }
}


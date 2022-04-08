// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*   Copyright (C) 2008-2016, International Business Machines
*   Corporation and others.  All Rights Reserved.
*/

package com.ibm.icu.text;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ibm.icu.impl.FormattedValueFieldPositionIteratorImpl;
import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.impl.SimpleFormatterImpl;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DateIntervalInfo.PatternInfo;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.DateInterval;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Category;
import com.ibm.icu.util.UResourceBundle;


/**
 * DateIntervalFormat is a class for formatting and parsing date
 * intervals in a language-independent manner.
 * Only formatting is supported. Parsing is not supported.
 *
 * <P>
 * Date interval means from one date to another date,
 * for example, from "Jan 11, 2008" to "Jan 18, 2008".
 * We introduced class DateInterval to represent it.
 * DateInterval is a pair of UDate, which is
 * the standard milliseconds since 24:00 GMT, Jan 1, 1970.
 *
 * <P>
 * DateIntervalFormat formats a DateInterval into
 * text as compactly as possible.
 * For example, the date interval format from "Jan 11, 2008" to "Jan 18,. 2008"
 * is "Jan 11-18, 2008" for English.
 * And it parses text into DateInterval,
 * although initially, parsing is not supported.
 *
 * <P>
 * There is no structural information in date time patterns.
 * For any punctuations and string literals inside a date time pattern,
 * we do not know whether it is just a separator, or a prefix, or a suffix.
 * Without such information, so, it is difficult to generate a sub-pattern
 * (or super-pattern) by algorithm.
 * So, formatting a DateInterval is pattern-driven. It is very
 * similar to formatting in SimpleDateFormat.
 * We introduce class DateIntervalInfo to save date interval
 * patterns, similar to date time pattern in SimpleDateFormat.
 *
 * <P>
 * Logically, the interval patterns are mappings
 * from (skeleton, the_largest_different_calendar_field)
 * to (date_interval_pattern).
 *
 * <P>
 * A skeleton
 * <ol>
 * <li>
 * only keeps the field pattern letter and ignores all other parts
 * in a pattern, such as space, punctuations, and string literals.
 * <li>
 * hides the order of fields.
 * <li>
 * might hide a field's pattern letter length.
 *
 * For those non-digit calendar fields, the pattern letter length is
 * important, such as MMM, MMMM, and MMMMM; EEE and EEEE,
 * and the field's pattern letter length is honored.
 *
 * For the digit calendar fields,  such as M or MM, d or dd, yy or yyyy,
 * the field pattern length is ignored and the best match, which is defined
 * in date time patterns, will be returned without honor the field pattern
 * letter length in skeleton.
 * </ol>
 *
 * <P>
 * The calendar fields we support for interval formatting are:
 * year, month, date, day-of-week, am-pm, hour, hour-of-day, minute, and
 * second (though we do not currently have specific intervalFormat data for
 * skeletons with seconds).
 * Those calendar fields can be defined in the following order:
 * year &gt; month &gt; date &gt; hour (in day) &gt; minute &gt; second
 *
 * The largest different calendar fields between 2 calendars is the
 * first different calendar field in above order.
 *
 * For example: the largest different calendar fields between "Jan 10, 2007"
 * and "Feb 20, 2008" is year.
 *
 * <P>
 * For other calendar fields, the compact interval formatting is not
 * supported. And the interval format will be fall back to fall-back
 * patterns, which is mostly "{date0} - {date1}".
 *
 * <P>
 * There is a set of pre-defined static skeleton strings in DateFormat,
 * There are pre-defined interval patterns for those pre-defined skeletons
 * in locales' resource files.
 * For example, for a skeleton YEAR_ABBR_MONTH_DAY, which is  "yMMMd",
 * in  en_US, if the largest different calendar field between date1 and date2
 * is "year", the date interval pattern  is "MMM d, yyyy - MMM d, yyyy",
 * such as "Jan 10, 2007 - Jan 10, 2008".
 * If the largest different calendar field between date1 and date2 is "month",
 * the date interval pattern is "MMM d - MMM d, yyyy",
 * such as "Jan 10 - Feb 10, 2007".
 * If the largest different calendar field between date1 and date2 is "day",
 * the date interval pattern is ""MMM d-d, yyyy", such as "Jan 10-20, 2007".
 *
 * For date skeleton, the interval patterns when year, or month, or date is
 * different are defined in resource files.
 * For time skeleton, the interval patterns when am/pm, or hour, or minute is
 * different are defined in resource files.
 *
 * <P>
 * If a skeleton is not found in a locale's DateIntervalInfo, which means
 * the interval patterns for the skeleton is not defined in resource file,
 * the interval pattern will falls back to the interval "fallback" pattern
 * defined in resource file.
 * If the interval "fallback" pattern is not defined, the default fall-back
 * is "{date0} - {data1}".
 *
 * <P>
 * For the combination of date and time,
 * The rule to genearte interval patterns are:
 * <ol>
 * <li>
 *    when the year, month, or day differs, falls back to fall-back
 *    interval pattern, which mostly is the concatenate the two original
 *    expressions with a separator between,
 *    For example, interval pattern from "Jan 10, 2007 10:10 am"
 *    to "Jan 11, 2007 10:10am" is
 *    "Jan 10, 2007 10:10 am - Jan 11, 2007 10:10am"
 * <li>
 *    otherwise, present the date followed by the range expression
 *    for the time.
 *    For example, interval pattern from "Jan 10, 2007 10:10 am"
 *    to "Jan 10, 2007 11:10am" is "Jan 10, 2007 10:10 am - 11:10am"
 * </ol>
 *
 *
 * <P>
 * If two dates are the same, the interval pattern is the single date pattern.
 * For example, interval pattern from "Jan 10, 2007" to "Jan 10, 2007" is
 * "Jan 10, 2007".
 *
 * Or if the presenting fields between 2 dates have the exact same values,
 * the interval pattern is the  single date pattern.
 * For example, if user only requests year and month,
 * the interval pattern from "Jan 10, 2007" to "Jan 20, 2007" is "Jan 2007".
 *
 * <P>
 * DateIntervalFormat needs the following information for correct
 * formatting: time zone, calendar type, pattern, date format symbols,
 * and date interval patterns.
 * It can be instantiated in several ways:
 * <ol>
 * <li>
 *    create an instance using default or given locale plus given skeleton.
 *    Users are encouraged to created date interval formatter this way and
 *    to use the pre-defined skeleton macros, such as
 *    YEAR_NUM_MONTH, which consists the calendar fields and
 *    the format style.
 * </li>
 * <li>
 *    create an instance using default or given locale plus given skeleton
 *    plus a given DateIntervalInfo.
 *    This factory method is for powerful users who want to provide their own
 *    interval patterns.
 *    Locale provides the timezone, calendar, and format symbols information.
 *    Local plus skeleton provides full pattern information.
 *    DateIntervalInfo provides the date interval patterns.
 * </li>
 * </ol>
 *
 * <P>
 * For the calendar field pattern letter, such as G, y, M, d, a, h, H, m, s etc.
 * DateIntervalFormat uses the same syntax as that of
 * DateTime format.
 *
 * <P>
 * Code Sample: general usage
 * <pre>
 *
 *   // the date interval object which the DateIntervalFormat formats on
 *   // and parses into
 *   DateInterval dtInterval = new DateInterval(1000*3600*24L, 1000*3600*24*2L);
 *   DateIntervalFormat dtIntervalFmt = DateIntervalFormat.getInstance(
 *                   YEAR_MONTH_DAY, Locale("en", "GB", ""));
 *   StringBuffer str = new StringBuffer("");
 *   FieldPosition pos = new FieldPosition(0);
 *   // formatting
 *   dtIntervalFmt.format(dtInterval, dateIntervalString, pos);
 *
 * </pre>
 *
 * <P>
 * Code Sample: for powerful users who wants to use their own interval pattern
 * <pre>
 *
 *     import com.ibm.icu.text.DateIntervalInfo;
 *     import com.ibm.icu.text.DateIntervalFormat;
 *     ....................
 *
 *     // Get DateIntervalFormat instance using default locale
 *     DateIntervalFormat dtitvfmt = DateIntervalFormat.getInstance(YEAR_MONTH_DAY);
 *
 *     // Create an empty DateIntervalInfo object, which does not have any interval patterns inside.
 *     dtitvinf = new DateIntervalInfo();
 *
 *     // a series of set interval patterns.
 *     // Only ERA, YEAR, MONTH, DATE,  DAY_OF_MONTH, DAY_OF_WEEK, AM_PM,  HOUR, HOUR_OF_DAY,
 *     MINUTE, SECOND and MILLISECOND are supported.
 *     dtitvinf.setIntervalPattern("yMMMd", Calendar.YEAR, "'y ~ y'");
 *     dtitvinf.setIntervalPattern("yMMMd", Calendar.MONTH, "yyyy 'diff' MMM d - MMM d");
 *     dtitvinf.setIntervalPattern("yMMMd", Calendar.DATE, "yyyy MMM d ~ d");
 *     dtitvinf.setIntervalPattern("yMMMd", Calendar.HOUR_OF_DAY, "yyyy MMM d HH:mm ~ HH:mm");
 *
 *     // Set fallback interval pattern. Fallback pattern is used when interval pattern is not found.
 *     // If the fall-back pattern is not set,  falls back to {date0} - {date1} if interval pattern is not found.
 *     dtitvinf.setFallbackIntervalPattern("{0} - {1}");
 *
 *     // Set above DateIntervalInfo object as the interval patterns of date interval formatter
 *     dtitvfmt.setDateIntervalInfo(dtitvinf);
 *
 *     // Prepare to format
 *     pos = new FieldPosition(0);
 *     str = new StringBuffer("");
 *
 *     // The 2 calendars should be equivalent, otherwise,  IllegalArgumentException will be thrown by format()
 *     Calendar fromCalendar = (Calendar) dtfmt.getCalendar().clone();
 *     Calendar toCalendar = (Calendar) dtfmt.getCalendar().clone();
 *     fromCalendar.setTimeInMillis(....);
 *     toCalendar.setTimeInMillis(...);
 *
 *     //Formatting given 2 calendars
 *     dtitvfmt.format(fromCalendar, toCalendar, str, pos);
 *
 *
 * </pre>
 * <h3>Synchronization</h3>
 *
 * The format methods of DateIntervalFormat may be used concurrently from multiple threads.
 * Functions that alter the state of a DateIntervalFormat object (setters)
 * may not be used concurrently with any other functions.
 *
 * @stable ICU 4.0
 */

public class DateIntervalFormat extends UFormat {

    /**
     * An immutable class containing the result of a date interval formatting operation.
     *
     * Instances of this class are immutable and thread-safe.
     *
     * Not intended for public subclassing.
     *
     * @stable ICU 64
     */
    public static final class FormattedDateInterval implements FormattedValue {
        private final String string;
        private final List<FieldPosition> attributes;

        FormattedDateInterval(CharSequence cs, List<FieldPosition> attributes) {
            this.string = cs.toString();
            this.attributes = Collections.unmodifiableList(attributes);
        }

        /**
         * {@inheritDoc}
         * @stable ICU 64
         */
        @Override
        public String toString() {
            return string;
        }

        /**
         * {@inheritDoc}
         * @stable ICU 64
         */
        @Override
        public int length() {
            return string.length();
        }

        /**
         * {@inheritDoc}
         * @stable ICU 64
         */
        @Override
        public char charAt(int index) {
            return string.charAt(index);
        }

        /**
         * {@inheritDoc}
         * @stable ICU 64
         */
        @Override
        public CharSequence subSequence(int start, int end) {
            return string.subSequence(start, end);
        }

        /**
         * {@inheritDoc}
         * @stable ICU 64
         */
        @Override
        public <A extends Appendable> A appendTo(A appendable) {
            return Utility.appendTo(string, appendable);
        }

        /**
         * {@inheritDoc}
         * @stable ICU 64
         */
        @Override
        public boolean nextPosition(ConstrainedFieldPosition cfpos) {
            return FormattedValueFieldPositionIteratorImpl.nextPosition(attributes, cfpos);
        }

        /**
         * {@inheritDoc}
         * @stable ICU 64
         */
        @Override
        public AttributedCharacterIterator toCharacterIterator() {
            return FormattedValueFieldPositionIteratorImpl.toCharacterIterator(string, attributes);
        }
    }

    /**
     * Class for span fields in FormattedDateInterval.
     *
     * @stable ICU 64
     */
    public static final class SpanField extends UFormat.SpanField {
        private static final long serialVersionUID = -6330879259553618133L;

        /**
         * The concrete field used for spans in FormattedDateInterval.
         *
         * Instances of DATE_INTERVAL_SPAN should have an associated value. If
         * 0, the date fields within the span are for the "from" date; if 1,
         * the date fields within the span are for the "to" date.
         *
         * @stable ICU 64
         */
        public static final SpanField DATE_INTERVAL_SPAN = new SpanField("date-interval-span");

        private SpanField(String name) {
            super(name);
        }

        /**
         * serialization method resolve instances to the constant
         * DateIntervalFormat.SpanField values
         * @internal
         * @deprecated This API is ICU internal only.
         */
        @Deprecated
        @Override
        protected Object readResolve() throws InvalidObjectException {
            if (this.getName().equals(DATE_INTERVAL_SPAN.getName()))
                return DATE_INTERVAL_SPAN;

            throw new InvalidObjectException("An invalid object.");
        }
    }

    private static final long serialVersionUID = 1;

    /**
     * Used to save the information for a skeleton's best match skeleton.
     * It is package accessible since it is used in DateIntervalInfo too.
     */
    static final class BestMatchInfo {
        // the best match skeleton
        final String bestMatchSkeleton;
        // 0 means the best matched skeleton is the same as input skeleton
        // 1 means the fields are the same, but field width are different
        // 2 means the only difference between fields are v/z,
        // -1 means there are other fields difference
        final int    bestMatchDistanceInfo;
        BestMatchInfo(String bestSkeleton, int difference) {
            bestMatchSkeleton = bestSkeleton;
            bestMatchDistanceInfo = difference;
        }
    }


    /*
     * Used to save the information on a skeleton and its best match.
     */
    private static final class SkeletonAndItsBestMatch {
        final String skeleton;
        final String bestMatchSkeleton;
        SkeletonAndItsBestMatch(String skeleton, String bestMatch) {
            this.skeleton = skeleton;
            bestMatchSkeleton = bestMatch;
        }
    }

    /** Used to output information during formatting. */
    private static final class FormatOutput {
        int firstIndex = -1;

        public void register(int i) {
            if (firstIndex == -1) {
                firstIndex = i;
            }
        }
    }


    // Cache for the locale interval pattern
    private static ICUCache<String, Map<String, PatternInfo>> LOCAL_PATTERN_CACHE =
        new SimpleCache<>();

    /*
     * The interval patterns for this locale.
     */
    private DateIntervalInfo     fInfo;

    /*
     * The DateFormat object used to format single pattern.
     * Because fDateFormat is modified during format operations, all
     * access to it from logically const, thread safe functions must be synchronized.
     */
    private SimpleDateFormat     fDateFormat;

    /*
     * The 2 calendars with the from and to date.
     * could re-use the calendar in fDateFormat,
     * but keeping 2 calendars make it clear and clean.
     * Because these Calendars are modified during format operations, all
     * access to them from logically const, thread safe functions must be synchronized.
     */
    private Calendar fFromCalendar;
    private Calendar fToCalendar;

    /*
     * Following are transient interval information
     * relevant (locale) to this formatter.
     */
    private String fSkeleton = null;

    /*
     * Needed for efficient deserialization. If set, it means we can use the
     * cache to initialize fIntervalPatterns.
     */
    private boolean isDateIntervalInfoDefault;

    /**
     *  Interval patterns for this instance's locale.
     */
    private transient Map<String, PatternInfo> fIntervalPatterns = null;

    /*
     * Patterns for fallback formatting.
     */
    private String fDatePattern = null;
    private String fTimePattern = null;
    private String fDateTimeFormat = null;

    /*
     * Capitalization context, new in ICU 68
     */
    private DisplayContext fCapitalizationSetting = DisplayContext.CAPITALIZATION_NONE;

    /*
     * default constructor; private because we don't want anyone to use
     */
    @SuppressWarnings("unused")
    private DateIntervalFormat() {
    }

    /**
     * Construct a DateIntervalFormat from DateFormat,
     * a DateIntervalInfo, and skeleton.
     * DateFormat provides the timezone, calendar,
     * full pattern, and date format symbols information.
     * It should be a SimpleDateFormat object which
     * has a pattern in it.
     * the DateIntervalInfo provides the interval patterns.
     *
     * @param skeleton  the skeleton of the date formatter
     * @param dtItvInfo  the DateIntervalInfo object to be adopted.
     * @param simpleDateFormat will be used for formatting
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public DateIntervalFormat(String skeleton, DateIntervalInfo dtItvInfo,
                               SimpleDateFormat simpleDateFormat)
    {
        fDateFormat = simpleDateFormat;
        // freeze date interval info
        dtItvInfo.freeze();
        fSkeleton = skeleton;
        fInfo = dtItvInfo;
        isDateIntervalInfoDefault = false;
        fFromCalendar = (Calendar) fDateFormat.getCalendar().clone();
        fToCalendar = (Calendar) fDateFormat.getCalendar().clone();
        initializePattern(null);
    }

    private DateIntervalFormat(String skeleton, ULocale locale,
            SimpleDateFormat simpleDateFormat)
    {
        fDateFormat = simpleDateFormat;
        fSkeleton = skeleton;
        fInfo = new DateIntervalInfo(locale).freeze();
        isDateIntervalInfoDefault = true;
        fFromCalendar = (Calendar) fDateFormat.getCalendar().clone();
        fToCalendar = (Calendar) fDateFormat.getCalendar().clone();
        initializePattern(LOCAL_PATTERN_CACHE);
}


    /**
     * Construct a DateIntervalFormat from skeleton and  the default <code>FORMAT</code> locale.
     *
     * This is a convenient override of
     * getInstance(String skeleton, ULocale locale)
     * with the value of locale as default <code>FORMAT</code> locale.
     *
     * @param skeleton  the skeleton on which interval format based.
     * @return          a date time interval formatter.
     * @see Category#FORMAT
     * @stable ICU 4.0
     */
    public static final DateIntervalFormat
        getInstance(String skeleton)

    {
        return getInstance(skeleton, ULocale.getDefault(Category.FORMAT));
    }


    /**
     * Construct a DateIntervalFormat from skeleton and a given locale.
     *
     * This is a convenient override of
     * getInstance(String skeleton, ULocale locale)
     *
     * <p>Example code:{@.jcite com.ibm.icu.samples.text.dateintervalformat.DateIntervalFormatSample:---dtitvfmtPreDefinedExample}
     * @param skeleton  the skeleton on which interval format based.
     * @param locale    the given locale
     * @return          a date time interval formatter.
     * @stable ICU 4.0
     */
    public static final DateIntervalFormat
        getInstance(String skeleton, Locale locale)
    {
        return getInstance(skeleton, ULocale.forLocale(locale));
    }


    /**
     * Construct a DateIntervalFormat from skeleton and a given locale.
     * <P>
     * In this factory method,
     * the date interval pattern information is load from resource files.
     * Users are encouraged to created date interval formatter this way and
     * to use the pre-defined skeleton macros.
     *
     * <P>
     * There are pre-defined skeletons in DateFormat,
     * such as MONTH_DAY, YEAR_MONTH_WEEKDAY_DAY etc.
     *
     * Those skeletons have pre-defined interval patterns in resource files.
     * Users are encouraged to use them.
     * For example:
     * DateIntervalFormat.getInstance(DateFormat.MONTH_DAY, false, loc);
     *
     * The given Locale provides the interval patterns.
     * For example, for en_GB, if skeleton is YEAR_ABBR_MONTH_WEEKDAY_DAY,
     * which is "yMMMEEEd",
     * the interval patterns defined in resource file to above skeleton are:
     * "EEE, d MMM, yyyy - EEE, d MMM, yyyy" for year differs,
     * "EEE, d MMM - EEE, d MMM, yyyy" for month differs,
     * "EEE, d - EEE, d MMM, yyyy" for day differs,
     * @param skeleton  the skeleton on which interval format based.
     * @param locale    the given locale
     * @return          a date time interval formatter.
     * @stable ICU 4.0
     */
    public static final DateIntervalFormat
        getInstance(String skeleton, ULocale locale)
    {
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);
        return new DateIntervalFormat(skeleton, locale, new SimpleDateFormat(generator.getBestPattern(skeleton), locale));
    }



    /**
     * Construct a DateIntervalFormat from skeleton
     *  DateIntervalInfo, and the default <code>FORMAT</code> locale.
     *
     * This is a convenient override of
     * getInstance(String skeleton, ULocale locale, DateIntervalInfo dtitvinf)
     * with the locale value as default <code>FORMAT</code> locale.
     *
     * @param skeleton  the skeleton on which interval format based.
     * @param dtitvinf  the DateIntervalInfo object to be adopted.
     * @return          a date time interval formatter.
     * @see Category#FORMAT
     * @stable ICU 4.0
     */
    public static final DateIntervalFormat getInstance(String skeleton,
                                                   DateIntervalInfo dtitvinf)
    {
        return getInstance(skeleton, ULocale.getDefault(Category.FORMAT), dtitvinf);
    }



    /**
     * Construct a DateIntervalFormat from skeleton
     * a DateIntervalInfo, and the given locale.
     *
     * This is a convenient override of
     * getInstance(String skeleton, ULocale locale, DateIntervalInfo dtitvinf)
     *
     * <p>Example code:{@.jcite com.ibm.icu.samples.text.dateintervalformat.DateIntervalFormatSample:---dtitvfmtCustomizedExample}
     * @param skeleton  the skeleton on which interval format based.
     * @param locale    the given locale
     * @param dtitvinf  the DateIntervalInfo object to be adopted.
     * @return          a date time interval formatter.
     * @stable ICU 4.0
     */
    public static final DateIntervalFormat getInstance(String skeleton,
                                                 Locale locale,
                                                 DateIntervalInfo dtitvinf)
    {
        return getInstance(skeleton, ULocale.forLocale(locale), dtitvinf);
    }



    /**
     * Construct a DateIntervalFormat from skeleton
     * a DateIntervalInfo, and the given locale.
     *
     * <P>
     * In this factory method, user provides its own date interval pattern
     * information, instead of using those pre-defined data in resource file.
     * This factory method is for powerful users who want to provide their own
     * interval patterns.
     *
     * <P>
     * There are pre-defined skeleton in DateFormat,
     * such as MONTH_DAY, YEAR_MONTH_WEEKDAY_DAY etc.
     *
     * Those skeletons have pre-defined interval patterns in resource files.
     * Users are encouraged to use them.
     * For example:
     * DateIntervalFormat.getInstance(DateFormat.MONTH_DAY, false, loc,itvinf);
     *
     * the DateIntervalInfo provides the interval patterns.
     *
     * User are encouraged to set default interval pattern in DateIntervalInfo
     * as well, if they want to set other interval patterns ( instead of
     * reading the interval patterns from resource files).
     * When the corresponding interval pattern for a largest calendar different
     * field is not found ( if user not set it ), interval format fallback to
     * the default interval pattern.
     * If user does not provide default interval pattern, it fallback to
     * "{date0} - {date1}"
     *
     * @param skeleton  the skeleton on which interval format based.
     * @param locale    the given locale
     * @param dtitvinf  the DateIntervalInfo object to be adopted.
     * @return          a date time interval formatter.
     * @stable ICU 4.0
     */
    public static final DateIntervalFormat getInstance(String skeleton,
                                                 ULocale locale,
                                                 DateIntervalInfo dtitvinf)
    {
        // clone. If it is frozen, clone returns itself, otherwise, clone
        // returns a copy.
        dtitvinf = (DateIntervalInfo)dtitvinf.clone();
        DateTimePatternGenerator generator = DateTimePatternGenerator.getInstance(locale);
        return new DateIntervalFormat(skeleton, dtitvinf, new SimpleDateFormat(generator.getBestPattern(skeleton), locale));
    }


    /**
     * Clone this Format object polymorphically.
     * @return    A copy of the object.
     * @stable ICU 4.0
     */
    @Override
    public synchronized Object clone()
    {
        DateIntervalFormat other = (DateIntervalFormat) super.clone();
        other.fDateFormat = (SimpleDateFormat) fDateFormat.clone();
        other.fInfo = (DateIntervalInfo) fInfo.clone();
        other.fFromCalendar = (Calendar) fFromCalendar.clone();
        other.fToCalendar = (Calendar) fToCalendar.clone();
        other.fDatePattern = fDatePattern;
        other.fTimePattern = fTimePattern;
        other.fDateTimeFormat = fDateTimeFormat;
        other.fCapitalizationSetting = fCapitalizationSetting;
        return other;
    }


    /**
     * Format an object to produce a string. This method handles Formattable
     * objects with a DateInterval type.
     * If a the Formattable object type is not a DateInterval,
     * IllegalArgumentException is thrown.
     *
     * @param obj               The object to format.
     *                          Must be a DateInterval.
     * @param appendTo          Output parameter to receive result.
     *                          Result is appended to existing contents.
     * @param fieldPosition     On input: an alignment field, if desired.
     *                          On output: the offsets of the alignment field.
     *                          There may be multiple instances of a given field type
     *                          in an interval format; in this case the fieldPosition
     *                          offsets refer to the first instance.
     * @return                  Reference to 'appendTo' parameter.
     * @throws    IllegalArgumentException  if the formatted object is not
     *                                      DateInterval object
     * @stable ICU 4.0
     */
    @Override
    public final StringBuffer
        format(Object obj, StringBuffer appendTo, FieldPosition fieldPosition)
    {
        if ( obj instanceof DateInterval ) {
            return format( (DateInterval)obj, appendTo, fieldPosition);
        }
        else {
            throw new IllegalArgumentException("Cannot format given Object (" + obj.getClass().getName() + ") as a DateInterval");
        }
    }

    /**
     * Format a DateInterval to produce a string.
     *
     * @param dtInterval        DateInterval to be formatted.
     * @param appendTo          Output parameter to receive result.
     *                          Result is appended to existing contents.
     * @param fieldPosition     On input: an alignment field, if desired.
     *                          On output: the offsets of the alignment field.
     *                          There may be multiple instances of a given field type
     *                          in an interval format; in this case the fieldPosition
     *                          offsets refer to the first instance.
     * @return                  Reference to 'appendTo' parameter.
     * @stable ICU 4.0
     */
    public final StringBuffer format(DateInterval dtInterval,
                                     StringBuffer appendTo,
                                     FieldPosition fieldPosition) {
        return formatIntervalImpl(dtInterval, appendTo, fieldPosition, null, null);
    }

    /**
     * Format a DateInterval to produce a FormattedDateInterval.
     *
     * The FormattedDateInterval exposes field information about the formatted string.
     *
     * @param dtInterval        DateInterval to be formatted.
     * @return                  A FormattedDateInterval containing the format result.
     * @stable ICU 64
     */
    public FormattedDateInterval formatToValue(DateInterval dtInterval) {
        StringBuffer sb = new StringBuffer();
        FieldPosition ignore = new FieldPosition(0);
        FormatOutput output = new FormatOutput();
        List<FieldPosition> attributes = new ArrayList<>();
        formatIntervalImpl(dtInterval, sb, ignore, output, attributes);
        if (output.firstIndex != -1) {
            FormattedValueFieldPositionIteratorImpl.addOverlapSpans(
                    attributes, SpanField.DATE_INTERVAL_SPAN, output.firstIndex);
            FormattedValueFieldPositionIteratorImpl.sort(attributes);
        }
        return new FormattedDateInterval(sb, attributes);
    }

    private synchronized StringBuffer formatIntervalImpl(
            DateInterval dtInterval,
            StringBuffer appendTo,
            FieldPosition pos,
            FormatOutput output,
            List<FieldPosition> attributes) {
        fFromCalendar.setTimeInMillis(dtInterval.getFromDate());
        fToCalendar.setTimeInMillis(dtInterval.getToDate());
        return formatImpl(fFromCalendar, fToCalendar, appendTo, pos, output, attributes);
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public String getPatterns(Calendar fromCalendar,
            Calendar toCalendar,
            Output<String> part2) {
        // First, find the largest different calendar field.
        int field;
        if ( fromCalendar.get(Calendar.ERA) != toCalendar.get(Calendar.ERA) ) {
            field = Calendar.ERA;
        } else if ( fromCalendar.get(Calendar.YEAR) !=
                    toCalendar.get(Calendar.YEAR) ) {
            field = Calendar.YEAR;
        } else if ( fromCalendar.get(Calendar.MONTH) !=
                    toCalendar.get(Calendar.MONTH) ) {
            field = Calendar.MONTH;
        } else if ( fromCalendar.get(Calendar.DATE) !=
                    toCalendar.get(Calendar.DATE) ) {
            field = Calendar.DATE;
        } else if ( fromCalendar.get(Calendar.AM_PM) !=
                    toCalendar.get(Calendar.AM_PM) ) {
            field = Calendar.AM_PM;
        } else if ( fromCalendar.get(Calendar.HOUR) !=
                    toCalendar.get(Calendar.HOUR) ) {
            field = Calendar.HOUR;
        } else if ( fromCalendar.get(Calendar.MINUTE) !=
                    toCalendar.get(Calendar.MINUTE) ) {
            field = Calendar.MINUTE;
        } else if ( fromCalendar.get(Calendar.SECOND) !=
                    toCalendar.get(Calendar.SECOND) ) {
            field = Calendar.SECOND;
        } else if ( fromCalendar.get(Calendar.MILLISECOND) !=
                    toCalendar.get(Calendar.MILLISECOND) ) {
            field = Calendar.MILLISECOND;
        } else {
            return null;
        }
        PatternInfo intervalPattern = fIntervalPatterns.get(
                DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field]);
        part2.value = intervalPattern.getSecondPart();
        return intervalPattern.getFirstPart();
    }

    /**
     * Format 2 Calendars to produce a string.
     *
     * @param fromCalendar      calendar set to the from date in date interval
     *                          to be formatted into date interval string
     * @param toCalendar        calendar set to the to date in date interval
     *                          to be formatted into date interval string
     * @param appendTo          Output parameter to receive result.
     *                          Result is appended to existing contents.
     * @param pos               On input: an alignment field, if desired.
     *                          On output: the offsets of the alignment field.
     *                          There may be multiple instances of a given field type
     *                          in an interval format; in this case the fieldPosition
     *                          offsets refer to the first instance.
     * @return                  Reference to 'appendTo' parameter.
     * @throws    IllegalArgumentException  if the two calendars are not equivalent.
     * @stable ICU 4.0
     */
    public final StringBuffer format(Calendar fromCalendar,
            Calendar toCalendar,
            StringBuffer appendTo,
            FieldPosition pos) {
        return formatImpl(fromCalendar, toCalendar, appendTo, pos, null, null);
    }

    /**
     * Format 2 Calendars to produce a FormattedDateInterval.
     *
     * The FormattedDateInterval exposes field information about the formatted string.
     *
     * @param fromCalendar      calendar set to the from date in date interval
     *                          to be formatted into date interval string
     * @param toCalendar        calendar set to the to date in date interval
     *                          to be formatted into date interval string
     * @return                  A FormattedDateInterval containing the format result.
     * @stable ICU 64
     */
    public FormattedDateInterval formatToValue(Calendar fromCalendar, Calendar toCalendar) {
        StringBuffer sb = new StringBuffer();
        FieldPosition ignore = new FieldPosition(0);
        FormatOutput output = new FormatOutput();
        List<FieldPosition> attributes = new ArrayList<>();
        formatImpl(fromCalendar, toCalendar, sb, ignore, output, attributes);
        if (output.firstIndex != -1) {
            FormattedValueFieldPositionIteratorImpl.addOverlapSpans(
                    attributes, SpanField.DATE_INTERVAL_SPAN, output.firstIndex);
            FormattedValueFieldPositionIteratorImpl.sort(attributes);
        }
        return new FormattedDateInterval(sb, attributes);
    }

    private synchronized StringBuffer formatImpl(Calendar fromCalendar,
                                     Calendar toCalendar,
                                     StringBuffer appendTo,
                                     FieldPosition pos,
                                     FormatOutput output,
                                     List<FieldPosition> attributes)
    {
        // not support different calendar types and time zones
        if ( !fromCalendar.isEquivalentTo(toCalendar) ) {
            throw new IllegalArgumentException("can not format on two different calendars");
        }

        // Set up fDateFormat to handle the first or only part of the interval
        // (override later for any second part).
        fDateFormat.setContext(fCapitalizationSetting);

        // First, find the largest different calendar field.
        int field = -1; //init with an invalid value.

        if ( fromCalendar.get(Calendar.ERA) != toCalendar.get(Calendar.ERA) ) {
            field = Calendar.ERA;
        } else if ( fromCalendar.get(Calendar.YEAR) !=
                    toCalendar.get(Calendar.YEAR) ) {
            field = Calendar.YEAR;
        } else if ( fromCalendar.get(Calendar.MONTH) !=
                    toCalendar.get(Calendar.MONTH) ) {
            field = Calendar.MONTH;
        } else if ( fromCalendar.get(Calendar.DATE) !=
                    toCalendar.get(Calendar.DATE) ) {
            field = Calendar.DATE;
        } else if ( fromCalendar.get(Calendar.AM_PM) !=
                    toCalendar.get(Calendar.AM_PM) ) {
            field = Calendar.AM_PM;
        } else if ( fromCalendar.get(Calendar.HOUR) !=
                    toCalendar.get(Calendar.HOUR) ) {
            field = Calendar.HOUR;
        } else if ( fromCalendar.get(Calendar.MINUTE) !=
                    toCalendar.get(Calendar.MINUTE) ) {
            field = Calendar.MINUTE;
        } else if ( fromCalendar.get(Calendar.SECOND) !=
                    toCalendar.get(Calendar.SECOND) ) {
            field = Calendar.SECOND;
        } else if ( fromCalendar.get(Calendar.MILLISECOND) !=
                    toCalendar.get(Calendar.MILLISECOND) ) {
            field = Calendar.MILLISECOND;
        } else {
            /* ignore the millisecond etc. small fields' difference.
             * use single date when all the above are the same.
             */
            return fDateFormat.format(fromCalendar, appendTo, pos, attributes);
        }
        boolean fromToOnSameDay = (field==Calendar.AM_PM || field==Calendar.HOUR || field==Calendar.MINUTE || field==Calendar.SECOND || field==Calendar.MILLISECOND);

        // get interval pattern
        PatternInfo intervalPattern = fIntervalPatterns.get(
              DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field]);

        if ( intervalPattern == null ) {
            if ( fDateFormat.isFieldUnitIgnored(field) ) {
                /* the largest different calendar field is small than
                 * the smallest calendar field in pattern,
                 * return single date format.
                 */
                return fDateFormat.format(fromCalendar, appendTo, pos, attributes);
            }

            return fallbackFormat(fromCalendar, toCalendar, fromToOnSameDay, appendTo, pos,
                    output, attributes);
        }

        // If the first part in interval pattern is empty,
        // the 2nd part of it saves the full-pattern used in fall-back.
        // For a 'real' interval pattern, the first part will never be empty.
        if ( intervalPattern.getFirstPart() == null ) {
            // fall back
            return fallbackFormat(fromCalendar, toCalendar, fromToOnSameDay, appendTo, pos,
                    output, attributes, intervalPattern.getSecondPart());
        }
        Calendar firstCal;
        Calendar secondCal;
        if ( intervalPattern.firstDateInPtnIsLaterDate() ) {
            if (output != null) {
                output.register(1);
            }
            firstCal = toCalendar;
            secondCal = fromCalendar;
        } else {
            if (output != null) {
                output.register(0);
            }
            firstCal = fromCalendar;
            secondCal = toCalendar;
        }
        // break the interval pattern into 2 parts
        // first part should not be empty,
        String originalPattern = fDateFormat.toPattern();
        fDateFormat.applyPattern(intervalPattern.getFirstPart());
        fDateFormat.format(firstCal, appendTo, pos, attributes);
        // Only accept the first instance of the field
        if (pos.getEndIndex() > 0) {
            pos = new FieldPosition(0);
        }
        if ( intervalPattern.getSecondPart() != null ) {
            fDateFormat.applyPattern(intervalPattern.getSecondPart());
            // No capitalization for second part of interval
            fDateFormat.setContext(DisplayContext.CAPITALIZATION_NONE);
            fDateFormat.format(secondCal, appendTo, pos, attributes);
        }
        fDateFormat.applyPattern(originalPattern);
        return appendTo;
    }

    /** Like fallbackFormat, but specifically for ranges. */
    private final void fallbackFormatRange(Calendar fromCalendar,
            Calendar toCalendar,
            StringBuffer appendTo,
            StringBuilder patternSB,
            FieldPosition pos,
            FormatOutput output,
            List<FieldPosition> attributes) {
        String compiledPattern = SimpleFormatterImpl.compileToStringMinMaxArguments(
                fInfo.getFallbackIntervalPattern(), patternSB, 2, 2);
        long state = 0;
        while (true) {
            state = SimpleFormatterImpl.IterInternal.step(state, compiledPattern, appendTo);
            if (state == SimpleFormatterImpl.IterInternal.DONE) {
                break;
            }
            if (SimpleFormatterImpl.IterInternal.getArgIndex(state) == 0) {
                if (output != null) {
                    output.register(0);
                }
                fDateFormat.format(fromCalendar, appendTo, pos, attributes);
            } else {
                if (output != null) {
                    output.register(1);
                }
                fDateFormat.format(toCalendar, appendTo, pos, attributes);
            }
            // Only accept the first instance of the field
            if (pos.getEndIndex() > 0) {
                pos = new FieldPosition(0);
            }
            // No capitalization for second portion
            fDateFormat.setContext(DisplayContext.CAPITALIZATION_NONE);
        }
    }

    /*
     * Format 2 Calendars to using fall-back interval pattern
     *
     * The full pattern used in this fall-back format is the
     * full pattern of the date formatter.
     *
     * @param fromCalendar      calendar set to the from date in date interval
     *                          to be formatted into date interval string
     * @param toCalendar        calendar set to the to date in date interval
     *                          to be formatted into date interval string
     * @param appendTo          Output parameter to receive result.
     *                          Result is appended to existing contents.
     * @param pos               On input: an alignment field, if desired.
     *                          On output: the offsets of the alignment field.
     * @return                  Reference to 'appendTo' parameter.
     */
    private final StringBuffer fallbackFormat(Calendar fromCalendar,
                                              Calendar toCalendar,
                                              boolean fromToOnSameDay,
                                              StringBuffer appendTo,
                                              FieldPosition pos,
                                              FormatOutput output,
                                              List<FieldPosition> attributes)  {
        StringBuilder patternSB = new StringBuilder();
        boolean formatDatePlusTimeRange = (fromToOnSameDay && fDatePattern != null && fTimePattern != null);
        if (formatDatePlusTimeRange) {
            String compiledPattern = SimpleFormatterImpl.compileToStringMinMaxArguments(
                    fDateTimeFormat, patternSB, 2, 2);

            String fullPattern; // for saving the pattern in fDateFormat
            fullPattern = fDateFormat.toPattern(); // save current pattern, restore later

            // {0} is time range
            // {1} is single date portion
            long state = 0;
            while (true) {
                state = SimpleFormatterImpl.IterInternal.step(state, compiledPattern, appendTo);
                if (state == SimpleFormatterImpl.IterInternal.DONE) {
                    break;
                }
                if (SimpleFormatterImpl.IterInternal.getArgIndex(state) == 0) {
                    fDateFormat.applyPattern(fTimePattern);
                    fallbackFormatRange(fromCalendar, toCalendar, appendTo, patternSB, pos, output, attributes);
                } else {
                    fDateFormat.applyPattern(fDatePattern);
                    fDateFormat.format(fromCalendar, appendTo, pos, attributes);
                }
                // Only accept the first instance of the field
                if (pos.getEndIndex() > 0) {
                    pos = new FieldPosition(0);
                }
                // No capitalization for second portion
                fDateFormat.setContext(DisplayContext.CAPITALIZATION_NONE);
            }

            // restore full pattern
            fDateFormat.applyPattern(fullPattern);
        } else {
            fallbackFormatRange(fromCalendar, toCalendar, appendTo, patternSB, pos, output, attributes);
        }
        return appendTo;
    }


    /*
     * Format 2 Calendars to using fall-back interval pattern
     *
     * This fall-back pattern is generated on a given full pattern,
     * not the full pattern of the date formatter.
     *
     * @param fromCalendar      calendar set to the from date in date interval
     *                          to be formatted into date interval string
     * @param toCalendar        calendar set to the to date in date interval
     *                          to be formatted into date interval string
     * @param appendTo          Output parameter to receive result.
     *                          Result is appended to existing contents.
     * @param pos               On input: an alignment field, if desired.
     *                          On output: the offsets of the alignment field.
     * @param fullPattern       the full pattern need to apply to date formatter
     * @return                  Reference to 'appendTo' parameter.
     */
    private final StringBuffer fallbackFormat(Calendar fromCalendar,
                                              Calendar toCalendar,
                                              boolean fromToOnSameDay,
                                              StringBuffer appendTo,
                                              FieldPosition pos,
                                              FormatOutput output,
                                              List<FieldPosition> attributes,
                                              String fullPattern)  {
            String originalPattern = fDateFormat.toPattern();
            fDateFormat.applyPattern(fullPattern);
            fallbackFormat(fromCalendar, toCalendar, fromToOnSameDay, appendTo, pos, output, attributes);
            fDateFormat.applyPattern(originalPattern);
            return appendTo;
    }


    /**
     * Date interval parsing is not supported.
     * <P>
     * This method should handle parsing of
     * date time interval strings into Formattable objects with
     * DateInterval type, which is a pair of UDate.
     * <P>
     * Before calling, set parse_pos.index to the offset you want to start
     * parsing at in the source. After calling, parse_pos.index is the end of
     * the text you parsed. If error occurs, index is unchanged.
     * <P>
     * When parsing, leading whitespace is discarded (with a successful parse),
     * while trailing whitespace is left as is.
     * <P>
     * See Format.parseObject() for more.
     *
     * @param source    The string to be parsed into an object.
     * @param parse_pos The position to start parsing at. Since no parsing
     *                  is supported, upon return this param is unchanged.
     * @return          A newly created Formattable* object, or NULL
     *                  on failure.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    @Deprecated
    public Object parseObject(String source, ParsePosition parse_pos)
    {
        throw new UnsupportedOperationException("parsing is not supported");
    }


    /**
     * Gets the date time interval patterns.
     * @return a copy of the date time interval patterns associated with
     * this date interval formatter.
     * @stable ICU 4.0
     */
    public DateIntervalInfo getDateIntervalInfo()
    {
        return (DateIntervalInfo)fInfo.clone();
    }


    /**
     * Set the date time interval patterns.
     * @param newItvPattern   the given interval patterns to copy.
     * @stable ICU 4.0
     */
    public void setDateIntervalInfo(DateIntervalInfo newItvPattern)
    {
        // clone it. If it is frozen, the clone returns itself.
        // Otherwise, clone returns a copy
        fInfo = (DateIntervalInfo)newItvPattern.clone();
        this.isDateIntervalInfoDefault = false;
        fInfo.freeze(); // freeze it
        if ( fDateFormat != null ) {
            initializePattern(null);
        }
    }

    /**
     * Get the TimeZone
     * @return A copy of the TimeZone associated with this date interval formatter.
     * @stable ICU 53
     */
    public TimeZone getTimeZone()
    {
        if ( fDateFormat != null ) {
            // Here we clone, like other getters here, but unlike
            // DateFormat.getTimeZone() and Calendar.getTimeZone()
            // which return the TimeZone from the Calendar's zone variable
            return (TimeZone)(fDateFormat.getTimeZone().clone());
        }
        // If fDateFormat is null (unexpected), return default timezone.
        return TimeZone.getDefault();
    }


    /**
     * Set the TimeZone for the calendar used by this DateIntervalFormat object.
     * @param zone The new TimeZone, will be cloned for use by this DateIntervalFormat.
     * @stable ICU 53
     */
    public void setTimeZone(TimeZone zone)
    {
        // zone is cloned once for all three usages below:
        TimeZone zoneToSet = (TimeZone)zone.clone();
        if (fDateFormat != null) {
            fDateFormat.setTimeZone(zoneToSet);
        }
        // fDateFormat has the primary calendar for the DateIntervalFormat;
        // fFromCalendar and fToCalendar are internal work clones of that calendar.
        if (fFromCalendar != null) {
            fFromCalendar.setTimeZone(zoneToSet);
        }
        if (fToCalendar != null) {
            fToCalendar.setTimeZone(zoneToSet);
        }
    }

    /**
     * {@icu} Set a particular DisplayContext value in the formatter,
     * such as CAPITALIZATION_FOR_STANDALONE. This causes the formatted
     * result to be capitalized appropriately for the context in which
     * it is intended to be used, considering both the locale and the
     * type of field at the beginning of the formatted result.
     *
     * @param context The DisplayContext value to set.
     * @stable ICU 68
     */
    public void setContext(DisplayContext context)
    {
        if (context.type() == DisplayContext.Type.CAPITALIZATION) {
            fCapitalizationSetting = context;
        }
    }

    /**
     * {@icu} Get the formatter's DisplayContext value for the specified DisplayContext.Type,
     * such as CAPITALIZATION.
     *
     * @param type the DisplayContext.Type whose value to return
     * @return the current DisplayContext setting for the specified type
     * @stable ICU 68
     */
    public DisplayContext getContext(DisplayContext.Type type)
    {
        return (type == DisplayContext.Type.CAPITALIZATION && fCapitalizationSetting != null)?
                fCapitalizationSetting: DisplayContext.CAPITALIZATION_NONE;
    }

    /**
     * Gets the date formatter
     * @return a copy of the date formatter associated with
     * this date interval formatter.
     * @stable ICU 4.0
     */
    public synchronized DateFormat getDateFormat()
    {
        return (DateFormat)fDateFormat.clone();
    }


    /*
     *  Below are for generating interval patterns locale to the formatter
     */

    /*
     * Initialize interval patterns locale to this formatter.
     */
    private void initializePattern(ICUCache<String, Map<String, PatternInfo>> cache) {
        String fullPattern = fDateFormat.toPattern();
        ULocale locale = fDateFormat.getLocale();
        String key = null;
        Map<String, PatternInfo> patterns = null;
        if (cache != null) {
            if ( fSkeleton != null ) {
                key = locale.toString() + "+" + fullPattern + "+" + fSkeleton;
            } else {
                key = locale.toString() + "+" + fullPattern;
            }
            patterns = cache.get(key);
        }
        if (patterns == null) {
            Map<String, PatternInfo> intervalPatterns = initializeIntervalPattern(fullPattern, locale);
            patterns = Collections.unmodifiableMap(intervalPatterns);
            if (cache != null) {
                cache.put(key, patterns);
            }
        }
        fIntervalPatterns = patterns;
    }



    /*
     * Initialize interval patterns locale to this formatter
     *
     * This code is a bit complicated since
     * 1. the interval patterns saved in resource bundle files are interval
     *    patterns based on date or time only.
     *    It does not have interval patterns based on both date and time.
     *    Interval patterns on both date and time are algorithm generated.
     *
     *    For example, it has interval patterns on skeleton "dMy" and "hm",
     *    but it does not have interval patterns on skeleton "dMyhm".
     *
     *    The rule to generate interval patterns for both date and time skeleton are
     *    1) when the year, month, or day differs, concatenate the two original
     *    expressions with a separator between,
     *    For example, interval pattern from "Jan 10, 2007 10:10 am"
     *    to "Jan 11, 2007 10:10am" is
     *    "Jan 10, 2007 10:10 am - Jan 11, 2007 10:10am"
     *
     *    2) otherwise, present the date followed by the range expression
     *    for the time.
     *    For example, interval pattern from "Jan 10, 2007 10:10 am"
     *    to "Jan 10, 2007 11:10am" is
     *    "Jan 10, 2007 10:10 am - 11:10am"
     *
     * 2. even a pattern does not request a certain calendar field,
     *    the interval pattern needs to include such field if such fields are
     *    different between 2 dates.
     *    For example, a pattern/skeleton is "hm", but the interval pattern
     *    includes year, month, and date when year, month, and date differs.
     *
     *
     * @param fullPattern  formatter's full pattern
     * @param locale       the given locale.
     * @return             interval patterns' hash map
     */
    private Map<String, PatternInfo> initializeIntervalPattern(String fullPattern, ULocale locale) {
        DateTimePatternGenerator dtpng = DateTimePatternGenerator.getInstance(locale);
        if ( fSkeleton == null ) {
            // fSkeleton is already set by getDateIntervalInstance()
            // or by getInstance(String skeleton, .... )
            fSkeleton = dtpng.getSkeleton(fullPattern);
        }
        String skeleton = normalizeHourMetacharacters(fSkeleton, locale);

        HashMap<String, PatternInfo> intervalPatterns = new HashMap<>();

        /* Check whether the skeleton is a combination of date and time.
         * For the complication reason 1 explained above.
         */
        StringBuilder date = new StringBuilder(skeleton.length());
        StringBuilder normalizedDate = new StringBuilder(skeleton.length());
        StringBuilder time = new StringBuilder(skeleton.length());
        StringBuilder normalizedTime = new StringBuilder(skeleton.length());

        /* the difference between time skeleton and normalizedTimeSkeleton are:
         * 1. (Formerly, normalized time skeleton folded 'H' to 'h'; no longer true)
         * 2. 'a' is omitted in normalized time skeleton.
         * 3. there is only one appearance for 'h', 'm','v', 'z' in normalized
         *    time skeleton
         *
         * The difference between date skeleton and normalizedDateSkeleton are:
         * 1. both 'y' and 'd' appear only once in normalizeDateSkeleton
         * 2. 'E' and 'EE' are normalized into 'EEE'
         * 3. 'MM' is normalized into 'M'
         */
        getDateTimeSkeleton(skeleton, date, normalizedDate,
                            time, normalizedTime);

        String dateSkeleton = date.toString();
        String timeSkeleton = time.toString();
        String normalizedDateSkeleton = normalizedDate.toString();
        String normalizedTimeSkeleton = normalizedTime.toString();

        // move this up here since we need it for fallbacks
        if (time.length() != 0 && date.length() != 0) {
            // Need the Date/Time pattern for concatenating the date with
            // the time interval.
            // The date/time pattern ( such as {0} {1} ) is saved in
            // calendar, that is why need to get the CalendarData here.
            fDateTimeFormat = getConcatenationPattern(locale);
        }

        boolean found = genSeparateDateTimePtn(normalizedDateSkeleton,
                                               normalizedTimeSkeleton,
                                               intervalPatterns, dtpng);

        // for skeletons with seconds, found is false and we enter this block
        if ( found == false ) {
            // use fallback
            // TODO: if user asks "m", but "d" differ
            //StringBuffer skeleton = new StringBuffer(skeleton);
            if ( time.length() != 0 ) {
                //genFallbackForNotFound(Calendar.MINUTE, skeleton);
                //genFallbackForNotFound(Calendar.HOUR, skeleton);
                //genFallbackForNotFound(Calendar.AM_PM, skeleton);
                if ( date.length() == 0 ) {
                    // prefix with yMd
                    timeSkeleton = DateFormat.YEAR_NUM_MONTH_DAY + timeSkeleton;
                    String pattern =dtpng.getBestPattern(timeSkeleton);
                    // for fall back interval patterns,
                    // the first part of the pattern is empty,
                    // the second part of the pattern is the full-pattern
                    // should be used in fall-back.
                    PatternInfo ptn = new PatternInfo(null, pattern,
                                                     fInfo.getDefaultOrder());
                    intervalPatterns.put(DateIntervalInfo.
                        CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.DATE], ptn);
                    // share interval pattern
                    intervalPatterns.put(DateIntervalInfo.
                        CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.MONTH], ptn);
                    // share interval pattern
                    intervalPatterns.put(DateIntervalInfo.
                        CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.YEAR], ptn);

                    pattern =dtpng.getBestPattern(timeSkeleton + "G");
                    ptn = new PatternInfo(null, pattern, fInfo.getDefaultOrder());
                    // share interval pattern
                    intervalPatterns.put(DateIntervalInfo.
                        CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.ERA], ptn);
                } else {
                    //genFallbackForNotFound(Calendar.DATE, skeleton);
                    //genFallbackForNotFound(Calendar.MONTH, skeleton);
                    //genFallbackForNotFound(Calendar.YEAR, skeleton);
                }
            } else {
                    //genFallbackForNotFound(Calendar.DATE, skeleton);
                    //genFallbackForNotFound(Calendar.MONTH, skeleton);
                    //genFallbackForNotFound(Calendar.YEAR, skeleton);
            }
            return intervalPatterns;
        } // end of skeleton not found
        // interval patterns for skeleton are found in resource
        if ( time.length() == 0 ) {
            // done
        } else if ( date.length() == 0 ) {
            // need to set up patterns for y/M/d differ
            /* result from following looks confusing.
             * for example: 10 10:10 - 11 10:10, it is not
             * clear that the first 10 is the 10th day
            time.insert(0, 'd');
            genFallbackPattern(Calendar.DATE, time);
            time.insert(0, 'M');
            genFallbackPattern(Calendar.MONTH, time);
            time.insert(0, 'y');
            genFallbackPattern(Calendar.YEAR, time);
            */
            // prefix with yMd
            timeSkeleton = DateFormat.YEAR_NUM_MONTH_DAY + timeSkeleton;
            String pattern =dtpng.getBestPattern(timeSkeleton);
            // for fall back interval patterns,
            // the first part of the pattern is empty,
            // the second part of the pattern is the full-pattern
            // should be used in fall-back.
            PatternInfo ptn = new PatternInfo(
                                    null, pattern, fInfo.getDefaultOrder());
            intervalPatterns.put(DateIntervalInfo.
                CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.DATE], ptn);
            intervalPatterns.put(DateIntervalInfo.
                CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.MONTH], ptn);
            intervalPatterns.put(DateIntervalInfo.
                CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.YEAR], ptn);

            pattern =dtpng.getBestPattern(timeSkeleton + "G");
            ptn = new PatternInfo(null, pattern, fInfo.getDefaultOrder());
            intervalPatterns.put(DateIntervalInfo.
                CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.ERA], ptn);
        } else {
            /* if both present,
             * 1) when the year, month, or day differs,
             * concatenate the two original expressions with a separator between,
             * 2) otherwise, present the date followed by the
             * range expression for the time.
             */
            /*
             * 1) when the era, year, month, or day differs,
             * concatenate the two original expressions with a separator between,
             */
            // if field exists, use fall back
            if ( !fieldExistsInSkeleton(Calendar.DATE, dateSkeleton) ) {
                // prefix skeleton with 'd'
                skeleton = DateIntervalInfo.
                    CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.DATE] + skeleton;
                genFallbackPattern(Calendar.DATE, skeleton, intervalPatterns, dtpng);
            }
            if ( !fieldExistsInSkeleton(Calendar.MONTH, dateSkeleton) ) {
                // then prefix skeleton with 'M'
                skeleton = DateIntervalInfo.
                    CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.MONTH] + skeleton;
                genFallbackPattern(Calendar.MONTH, skeleton, intervalPatterns, dtpng);
            }
            if ( !fieldExistsInSkeleton(Calendar.YEAR, dateSkeleton) ) {
                // then prefix skeleton with 'y'
                skeleton = DateIntervalInfo.
                    CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.YEAR] + skeleton;
                genFallbackPattern(Calendar.YEAR, skeleton, intervalPatterns, dtpng);
            }
            if ( !fieldExistsInSkeleton(Calendar.ERA, dateSkeleton) ) {
                // then prefix skeleton with 'G'
                skeleton = DateIntervalInfo.
                    CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.ERA] + skeleton;
                genFallbackPattern(Calendar.ERA, skeleton, intervalPatterns, dtpng);
            }

            /*
             * 2) otherwise, present the date followed by the
             * range expression for the time.
             */
            if (fDateTimeFormat == null) {
                fDateTimeFormat = "{1} {0}";
            }
            String datePattern =dtpng.getBestPattern(dateSkeleton);
            concatSingleDate2TimeInterval(fDateTimeFormat, datePattern, Calendar.AM_PM, intervalPatterns);
            concatSingleDate2TimeInterval(fDateTimeFormat, datePattern, Calendar.HOUR, intervalPatterns);
            concatSingleDate2TimeInterval(fDateTimeFormat, datePattern, Calendar.MINUTE, intervalPatterns);
        }

        return intervalPatterns;
    }

    /**
     * Retrieves the concatenation DateTime pattern from the resource bundle.
     * @param locale Locale to retrieve.
     * @return Concatenation DateTime pattern.
     */
    private String getConcatenationPattern(ULocale locale) {
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, locale);
        ICUResourceBundle dtPatternsRb = rb.getWithFallback("calendar/gregorian/DateTimePatterns");
        ICUResourceBundle concatenationPatternRb = (ICUResourceBundle) dtPatternsRb.get(8);
        if (concatenationPatternRb.getType() == UResourceBundle.STRING) {
            return concatenationPatternRb.getString();
        } else {
            return concatenationPatternRb.getString(0);
        }
    }

    /*
     * Generate fall back interval pattern given a calendar field,
     * a skeleton, and a date time pattern generator
     * @param field      the largest different calendar field
     * @param skeleton   a skeleton
     * @param dtpng      date time pattern generator
     * @param intervalPatterns interval patterns
     */
    private void genFallbackPattern(int field, String skeleton,
                                    Map<String, PatternInfo> intervalPatterns,
                                    DateTimePatternGenerator dtpng) {
        String pattern = dtpng.getBestPattern(skeleton);
        // for fall back interval patterns,
        // the first part of the pattern is empty,
        // the second part of the pattern is the full-pattern
        // should be used in fall-back.
        PatternInfo ptn = new PatternInfo(
                                    null, pattern, fInfo.getDefaultOrder());
        intervalPatterns.put(
            DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], ptn);
    }



    /*
    private void genFallbackForNotFound(String field, StringBuffer skeleton) {
        if ( SimpleDateFormat.isFieldUnitIgnored(skeleton.toString(), field) ) {
            // single date
            DateIntervalInfo.PatternInfo ptnInfo =
                new DateIntervalInfo.PatternInfo(null, fDateFormat.toPattern(),
                                                 fInfo.getDefaultOrder());
            fIntervalPatterns.put(field, ptnInfo);
            return;
        } else if ( skeleton.indexOf(field) == -1 ) {
            skeleton.insert(0,field);
            genFallbackPattern(field, skeleton, dtpng);
        }
    }
    */

    private String normalizeHourMetacharacters(String skeleton, ULocale locale) {
        StringBuilder result = new StringBuilder(skeleton);

        char hourMetachar = '\0';
        char dayPeriodChar = '\0';
        int hourFieldStart = 0;
        int hourFieldLength = 0;
        int dayPeriodStart = 0;
        int dayPeriodLength = 0;
        for (int i = 0; i < result.length(); i++) {
            char c = result.charAt(i);
            if (c == 'j' || c == 'J' || c == 'C' || c == 'h' || c == 'H' || c == 'k' || c == 'K') {
                if (hourMetachar == '\0') {
                    hourMetachar = c;
                    hourFieldStart = i;
                }
                ++hourFieldLength;
            } else if (c == 'a' || c == 'b' || c == 'B') {
                if (dayPeriodChar == '\0') {
                    dayPeriodChar = c;
                    dayPeriodStart = i;
                }
                ++dayPeriodLength;
            } else {
                if (hourMetachar != '\0' && dayPeriodChar != '\0') {
                    break;
                }
            }
        }

        if (hourMetachar != '\0') {
            char hourChar = 'H';

            DateTimePatternGenerator dtptng = DateTimePatternGenerator.getInstance(locale);
            String convertedPattern = dtptng.getBestPattern(String.valueOf(hourMetachar));

            // strip literal text from the pattern (so literal characters don't get mistaken for pattern
            // characters-- such as the 'h' in 'Uhr' in German)
            int firstQuotePos;
            while ((firstQuotePos = convertedPattern.indexOf('\'')) != -1) {
                int secondQuotePos = convertedPattern.indexOf('\'', firstQuotePos + 1);
                if (secondQuotePos == -1) {
                    secondQuotePos = firstQuotePos;
                }
                convertedPattern = convertedPattern.substring(0, firstQuotePos) + convertedPattern.substring(secondQuotePos + 1);
            }

            if (convertedPattern.indexOf('h') != -1) {
                hourChar = 'h';
            } else if (convertedPattern.indexOf('K') != -1) {
                hourChar = 'K';
            } else if (convertedPattern.indexOf('k') != -1) {
                hourChar = 'k';
            }

            if (convertedPattern.indexOf('b') != -1) {
                dayPeriodChar = 'b';
            } else if (convertedPattern.indexOf('B') != -1) {
                dayPeriodChar = 'B';
            } else if (dayPeriodChar == '\0') {
                dayPeriodChar = 'a';
            }

            StringBuilder hourAndDayPeriod = new StringBuilder();
            hourAndDayPeriod.append(hourChar);
            if (hourChar != 'H' && hourChar != 'k') {
                int newDayPeriodLength = 0;
                if (dayPeriodLength >= 5 || hourFieldLength >= 5) {
                    newDayPeriodLength = 5;
                } else if (dayPeriodLength >= 3 || hourFieldLength >= 3) {
                    newDayPeriodLength = 3;
                } else {
                    newDayPeriodLength = 1;
                }
                for (int i = 0; i < newDayPeriodLength; i++) {
                    hourAndDayPeriod.append(dayPeriodChar);
                }
            }
            result.replace(hourFieldStart, hourFieldStart + hourFieldLength, hourAndDayPeriod.toString());
            if (dayPeriodStart > hourFieldStart) {
                dayPeriodStart += hourAndDayPeriod.length() - hourFieldLength;
            }
            result.delete(dayPeriodStart, dayPeriodStart + dayPeriodLength);
        }
        return result.toString();
    }

    /*
     * get separated date and time skeleton from a combined skeleton.
     *
     * The difference between date skeleton and normalizedDateSkeleton are:
     * 1. both 'y' and 'd' are appeared only once in normalizeDateSkeleton
     * 2. 'E' and 'EE' are normalized into 'EEE'
     * 3. 'MM' is normalized into 'M'
     *
     ** the difference between time skeleton and normalizedTimeSkeleton are:
     * 1. both 'H' and 'h' are normalized as 'h' in normalized time skeleton,
     * 2. 'a' is omitted in normalized time skeleton.
     * 3. there is only one appearance for 'h', 'm','v', 'z' in normalized time
     *    skeleton
     *
     *
     *  @param skeleton               given combined skeleton.
     *  @param date                   Output parameter for date only skeleton.
     *  @param normalizedDate         Output parameter for normalized date only
     *
     *  @param time                   Output parameter for time only skeleton.
     *  @param normalizedTime         Output parameter for normalized time only
     *                                skeleton.
     */
    private static void getDateTimeSkeleton(String skeleton,
                                            StringBuilder dateSkeleton,
                                            StringBuilder normalizedDateSkeleton,
                                            StringBuilder timeSkeleton,
                                            StringBuilder normalizedTimeSkeleton)
    {
        // dateSkeleton follows the sequence of y*M*E*d*
        // timeSkeleton follows the sequence of hm*[v|z]?
        int i;
        int ECount = 0;
        int dCount = 0;
        int MCount = 0;
        int yCount = 0;
        int mCount = 0;
        int vCount = 0;
        int zCount = 0;
        char hourChar = '\0';

        for (i = 0; i < skeleton.length(); ++i) {
            char ch = skeleton.charAt(i);
            switch ( ch ) {
              case 'E':
                dateSkeleton.append(ch);
                ++ECount;
                break;
              case 'd':
                dateSkeleton.append(ch);
                ++dCount;
                break;
              case 'M':
                dateSkeleton.append(ch);
                ++MCount;
                break;
              case 'y':
                dateSkeleton.append(ch);
                ++yCount;
                break;
              case 'G':
              case 'Y':
              case 'u':
              case 'Q':
              case 'q':
              case 'L':
              case 'l':
              case 'W':
              case 'w':
              case 'D':
              case 'F':
              case 'g':
              case 'e':
              case 'c':
              case 'U':
              case 'r':
                normalizedDateSkeleton.append(ch);
                dateSkeleton.append(ch);
                break;
              case 'h':
              case 'H':
              case 'k':
              case 'K':
                timeSkeleton.append(ch);
                if (hourChar == '\0') {
                    hourChar = ch;
                }
                break;
              case 'm':
                timeSkeleton.append(ch);
                ++mCount;
                break;
              case 'z':
                ++zCount;
                timeSkeleton.append(ch);
                break;
              case 'v':
                ++vCount;
                timeSkeleton.append(ch);
                break;
              case 'a':
              case 'V':
              case 'Z':
              case 'j':
              case 's':
              case 'S':
              case 'A':
              case 'b':
              case 'B':
                timeSkeleton.append(ch);
                normalizedTimeSkeleton.append(ch);
                break;
            }
        }

        /* generate normalized form for date*/
        if ( yCount != 0 ) {
            for (i = 0; i < yCount; i++) {
                normalizedDateSkeleton.append('y');
            }
        }
        if ( MCount != 0 ) {
            if ( MCount < 3 ) {
                normalizedDateSkeleton.append('M');
            } else {
                for ( i = 0; i < MCount && i < 5; ++i ) {
                     normalizedDateSkeleton.append('M');
                }
            }
        }
        if ( ECount != 0 ) {
            if ( ECount <= 3 ) {
                normalizedDateSkeleton.append('E');
            } else {
                for ( i = 0; i < ECount && i < 5; ++i ) {
                     normalizedDateSkeleton.append('E');
                }
            }
        }
        if ( dCount != 0 ) {
            normalizedDateSkeleton.append('d');
        }

        /* generate normalized form for time */
        if ( hourChar != '\0' ) {
            normalizedTimeSkeleton.append(hourChar);
        }
        if ( mCount != 0 ) {
            normalizedTimeSkeleton.append('m');
        }
        if ( zCount != 0 ) {
            normalizedTimeSkeleton.append('z');
        }
        if ( vCount != 0 ) {
            normalizedTimeSkeleton.append('v');
        }
    }



    /*
     * Generate date or time interval pattern from resource.
     *
     * It needs to handle the following:
     * 1. need to adjust field width.
     *    For example, the interval patterns saved in DateIntervalInfo
     *    includes "dMMMy", but not "dMMMMy".
     *    Need to get interval patterns for dMMMMy from dMMMy.
     *    Another example, the interval patterns saved in DateIntervalInfo
     *    includes "hmv", but not "hmz".
     *    Need to get interval patterns for "hmz' from 'hmv'
     *
     * 2. there might be no pattern for 'y' differ for skeleton "Md",
     *    in order to get interval patterns for 'y' differ,
     *    need to look for it from skeleton 'yMd'
     *
     * @param dateSkeleton   normalized date skeleton
     * @param timeSkeleton   normalized time skeleton
     * @param intervalPatterns interval patterns
     * @return whether there is interval patterns for the skeleton.
     *         true if there is, false otherwise
     */
    private boolean genSeparateDateTimePtn(String dateSkeleton,
                                           String timeSkeleton,
                                           Map<String, PatternInfo> intervalPatterns,
                                           DateTimePatternGenerator dtpng)
    {
        String skeleton;
        // if both date and time skeleton present,
        // the final interval pattern might include time interval patterns
        // ( when, am_pm, hour, minute, second differ ),
        // but not date interval patterns ( when year, month, day differ ).
        // For year/month/day differ, it falls back to fall-back pattern.
        if ( timeSkeleton.length() != 0  ) {
            skeleton = timeSkeleton;
        } else {
            skeleton = dateSkeleton;
        }

        /* interval patterns for skeleton "dMMMy" (but not "dMMMMy")
         * are defined in resource,
         * interval patterns for skeleton "dMMMMy" are calculated by
         * 1. get the best match skeleton for "dMMMMy", which is "dMMMy"
         * 2. get the interval patterns for "dMMMy",
         * 3. extend "MMM" to "MMMM" in above interval patterns for "dMMMMy"
         * getBestSkeleton() is step 1.
         */
        // best skeleton, and the difference information
        BestMatchInfo retValue = fInfo.getBestSkeleton(skeleton);
        String bestSkeleton = retValue.bestMatchSkeleton;
        int differenceInfo =  retValue.bestMatchDistanceInfo;

        // Set patterns for fallback use, need to do this
        // before returning if differenceInfo == -1
        if (dateSkeleton.length() != 0  ) {
            fDatePattern = dtpng.getBestPattern(dateSkeleton);
        }
        if (timeSkeleton.length() != 0  ) {
            fTimePattern = dtpng.getBestPattern(timeSkeleton);
        }

        // difference:
        // 0 means the best matched skeleton is the same as input skeleton
        // 1 means the fields are the same, but field width are different
        // 2 means the only difference between fields are v/z,
        // -1 means there are other fields difference
        // (this will happen, for instance, if the supplied skeleton has seconds,
        //  but no skeletons in the intervalFormats data do)
        if ( differenceInfo == -1 ) {
            // skeleton has different fields, not only  v/z difference
            return false;
        }

        if ( timeSkeleton.length() == 0 ) {
            // only has date skeleton
            genIntervalPattern(Calendar.DATE, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
            SkeletonAndItsBestMatch skeletons = genIntervalPattern(
                                                  Calendar.MONTH, skeleton,
                                                  bestSkeleton, differenceInfo,
                                                  intervalPatterns);
            if ( skeletons != null ) {
                bestSkeleton = skeletons.skeleton;
                skeleton = skeletons.bestMatchSkeleton;
            }
            genIntervalPattern(Calendar.YEAR, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
            genIntervalPattern(Calendar.ERA, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
        } else {
            genIntervalPattern(Calendar.MINUTE, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
            genIntervalPattern(Calendar.HOUR, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
            genIntervalPattern(Calendar.AM_PM, skeleton, bestSkeleton, differenceInfo, intervalPatterns);
        }
        return true;

    }



    /*
     * Generate interval pattern from existing resource
     *
     * It not only save the interval patterns,
     * but also return the skeleton and its best match skeleton.
     *
     * @param field           largest different calendar field
     * @param skeleton        skeleton
     * @param bestSkeleton    the best match skeleton which has interval pattern
     *                        defined in resource
     * @param differenceInfo  the difference between skeleton and best skeleton
     *         0 means the best matched skeleton is the same as input skeleton
     *         1 means the fields are the same, but field width are different
     *         2 means the only difference between fields are v/z,
     *        -1 means there are other fields difference
     *
     * @param intervalPatterns interval patterns
     *
     * @return  an extended skeleton or extended best skeleton if applicable.
     *          null otherwise.
     */
    private SkeletonAndItsBestMatch genIntervalPattern(
                   int field, String skeleton, String bestSkeleton,
                   int differenceInfo, Map<String, PatternInfo> intervalPatterns) {
        SkeletonAndItsBestMatch retValue = null;
        PatternInfo pattern = fInfo.getIntervalPattern(
                                           bestSkeleton, field);
        if ( pattern == null ) {
            // single date
            if ( SimpleDateFormat.isFieldUnitIgnored(bestSkeleton, field) ) {
                PatternInfo ptnInfo =
                    new PatternInfo(fDateFormat.toPattern(),
                                                     null,
                                                     fInfo.getDefaultOrder());
                intervalPatterns.put(DateIntervalInfo.
                    CALENDAR_FIELD_TO_PATTERN_LETTER[field], ptnInfo);
                return null;
            }

            // for 24 hour system, interval patterns in resource file
            // might not include pattern when am_pm differ,
            // which should be the same as hour differ.
            // add it here for simplicity
            if ( field == Calendar.AM_PM ) {
                 pattern = fInfo.getIntervalPattern(bestSkeleton,
                                                         Calendar.HOUR);
                 if ( pattern != null ) {
                    boolean suppressDayPeriodField = fSkeleton.indexOf('J') != -1;
                    String part1 = adjustFieldWidth(skeleton, bestSkeleton,
                                       pattern.getFirstPart(), differenceInfo, suppressDayPeriodField);
                    String part2 = adjustFieldWidth(skeleton, bestSkeleton,
                                       pattern.getSecondPart(), differenceInfo, suppressDayPeriodField);
                    pattern =  new PatternInfo(part1, part2,
                                               pattern.firstDateInPtnIsLaterDate());

                      // share
                      intervalPatterns.put(DateIntervalInfo.
                          CALENDAR_FIELD_TO_PATTERN_LETTER[field],
                          pattern);
                 }
                 return null;
            }
            // else, looking for pattern when 'y' differ for 'dMMMM' skeleton,
            // first, get best match pattern "MMMd",
            // since there is no pattern for 'y' differs for skeleton 'MMMd',
            // need to look for it from skeleton 'yMMMd',
            // if found, adjust field width in interval pattern from
            // "MMM" to "MMMM".
            String fieldLetter =
                DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field];
            bestSkeleton = fieldLetter + bestSkeleton;
            skeleton = fieldLetter + skeleton;
            // for example, looking for patterns when 'y' differ for
            // skeleton "MMMM".
            pattern = fInfo.getIntervalPattern(bestSkeleton, field);
            if ( pattern == null && differenceInfo == 0 ) {
                // if there is no skeleton "yMMMM" defined,
                // look for the best match skeleton, for example: "yMMM"
                BestMatchInfo tmpRetValue = fInfo.getBestSkeleton(skeleton);
                String tmpBestSkeleton = tmpRetValue.bestMatchSkeleton;
                differenceInfo =  tmpRetValue.bestMatchDistanceInfo;
                if ( tmpBestSkeleton.length() != 0 && differenceInfo != -1 ) {
                    pattern = fInfo.getIntervalPattern(tmpBestSkeleton, field);
                    bestSkeleton = tmpBestSkeleton;
                }
            }
            if ( pattern != null ) {
                retValue = new SkeletonAndItsBestMatch(skeleton, bestSkeleton);
            }
        }
        if ( pattern != null ) {
            if ( differenceInfo != 0 ) {
                boolean suppressDayPeriodField = fSkeleton.indexOf('J') != -1;
                String part1 = adjustFieldWidth(skeleton, bestSkeleton,
                                   pattern.getFirstPart(), differenceInfo, suppressDayPeriodField);
                String part2 = adjustFieldWidth(skeleton, bestSkeleton,
                                   pattern.getSecondPart(), differenceInfo, suppressDayPeriodField);
                pattern =  new PatternInfo(part1, part2,
                                           pattern.firstDateInPtnIsLaterDate());
            } else {
                // pattern is immutable, no need to clone;
                // pattern = (PatternInfo)pattern.clone();
            }
            intervalPatterns.put(
              DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], pattern);
        }
        return retValue;
    }

    /*
     * Adjust field width in best match interval pattern to match
     * the field width in input skeleton.
     *
     * TODO (xji) make a general solution
     * The adjusting rule can be:
     * 1. always adjust
     * 2. never adjust
     * 3. default adjust, which means adjust according to the following rules
     * 3.1 always adjust string, such as MMM and MMMM
     * 3.2 never adjust between string and numeric, such as MM and MMM
     * 3.3 always adjust year
     * 3.4 do not adjust 'd', 'h', or 'm' if h presents
     * 3.5 do not adjust 'M' if it is numeric(?)
     *
     * Since date interval format is well-formed format,
     * date and time skeletons are normalized previously,
     * till this stage, the adjust here is only "adjust strings, such as MMM
     * and MMMM, EEE and EEEE.
     *
     * @param inputSkeleton            the input skeleton
     * @param bestMatchSkeleton        the best match skeleton
     * @param bestMatchIntervalpattern the best match interval pattern
     * @param differenceInfo           the difference between 2 skeletons
     *                                 1 means only field width differs
     *                                 2 means v/z exchange
     * @param suppressDayPeriodField   if true, remove the day period field from the result
     * @return the adjusted interval pattern
     */
    private static String adjustFieldWidth(String inputSkeleton,
                                    String bestMatchSkeleton,
                                    String bestMatchIntervalPattern,
                                    int differenceInfo,
                                    boolean suppressDayPeriodField ) {

        if ( bestMatchIntervalPattern == null ) {
            return null; // the 2nd part could be null
        }
        int[] inputSkeletonFieldWidth = new int[58];
        int[] bestMatchSkeletonFieldWidth = new int[58];

        /* initialize as following
        {
        //       A   B   C   D   E   F   G   H   I   J   K   L   M   N   O
            0, 0, 0, 0, 0,  0, 0,  0,  0, 0, 0, 0, 0,  0, 0, 0,
        //   P   Q   R   S   T   U   V   W   X   Y   Z
            0, 0, 0, 0, 0,  0, 0,  0,  0, 0, 0, 0, 0,  0, 0, 0,
        //       a   b   c   d   e   f   g   h   i   j   k   l   m   n   o
            0, 0, 0, 0, 0,  0, 0,  0,  0, 0, 0, 0, 0,  0, 0, 0,
        //   p   q   r   s   t   u   v   w   x   y   z
            0, 0, 0, 0, 0,  0, 0,  0,  0, 0, 0, 0, 0,  0, 0, 0,
        };
        */


        int PATTERN_CHAR_BASE = 0x41;

        DateIntervalInfo.parseSkeleton(inputSkeleton, inputSkeletonFieldWidth);
        DateIntervalInfo.parseSkeleton(bestMatchSkeleton, bestMatchSkeletonFieldWidth);
        if (suppressDayPeriodField) {
            if (bestMatchIntervalPattern.indexOf(" a") != -1) {
                bestMatchIntervalPattern = findReplaceInPattern(bestMatchIntervalPattern, " a", "");
            } else if (bestMatchIntervalPattern.indexOf("a ") != -1) {
                bestMatchIntervalPattern = findReplaceInPattern(bestMatchIntervalPattern, "a ", "");
            }
            bestMatchIntervalPattern = findReplaceInPattern(bestMatchIntervalPattern, "a", "");
        }
        if ( differenceInfo == 2 ) {
            if (inputSkeleton.indexOf('z') != -1) {
                bestMatchIntervalPattern = findReplaceInPattern(bestMatchIntervalPattern, "v", "z");
            }
            if (inputSkeleton.indexOf('K') != -1) {
                bestMatchIntervalPattern = findReplaceInPattern(bestMatchIntervalPattern, "h", "K");
            }
            if (inputSkeleton.indexOf('k') != -1) {
                bestMatchIntervalPattern = findReplaceInPattern(bestMatchIntervalPattern, "H", "k");
            }
            if (inputSkeleton.indexOf('b') != -1) {
                bestMatchIntervalPattern = findReplaceInPattern(bestMatchIntervalPattern, "a", "b");
            }
        }
        if (bestMatchIntervalPattern.indexOf('a') != -1 && bestMatchSkeletonFieldWidth['a' - PATTERN_CHAR_BASE] == 0) {
            bestMatchSkeletonFieldWidth['a' - PATTERN_CHAR_BASE] = 1;
        }
        if (bestMatchIntervalPattern.indexOf('b') != -1 && bestMatchSkeletonFieldWidth['b' - PATTERN_CHAR_BASE] == 0) {
            bestMatchSkeletonFieldWidth['b' - PATTERN_CHAR_BASE] = 1;
        }

        StringBuilder adjustedPtn = new StringBuilder(bestMatchIntervalPattern);


        boolean inQuote = false;
        char prevCh = 0;
        int count = 0;

        // loop through the pattern string character by character
        int adjustedPtnLength = adjustedPtn.length();
        for (int i = 0; i < adjustedPtnLength; ++i) {
            char ch = adjustedPtn.charAt(i);
            if (ch != prevCh && count > 0) {
                // check the repeativeness of pattern letter
                char skeletonChar = prevCh;
                if ( skeletonChar == 'L' ) {
                    // for skeleton "M+", the pattern is "...L..."
                    skeletonChar = 'M';
                }
                int fieldCount = bestMatchSkeletonFieldWidth[skeletonChar - PATTERN_CHAR_BASE];
                int inputFieldCount = inputSkeletonFieldWidth[skeletonChar - PATTERN_CHAR_BASE];
                if ( fieldCount == count && inputFieldCount > fieldCount ) {
                    count = inputFieldCount - fieldCount;
                    for ( int j = 0; j < count; ++j ) {
                        adjustedPtn.insert(i, prevCh);
                    }
                    i += count;
                    adjustedPtnLength += count;
                }
                count = 0;
            }
            if (ch == '\'') {
                // Consecutive single quotes are a single quote literal,
                // either outside of quotes or between quotes
                if ((i+1) < adjustedPtn.length() && adjustedPtn.charAt(i+1) == '\'') {
                    ++i;
                } else {
                    inQuote = ! inQuote;
                }
            }
            else if ( ! inQuote && ((ch >= 0x0061 /*'a'*/ && ch <= 0x007A /*'z'*/)
                        || (ch >= 0x0041 /*'A'*/ && ch <= 0x005A /*'Z'*/))) {
                // ch is a date-time pattern character
                prevCh = ch;
                ++count;
            }
        }
        if ( count > 0 ) {
            // last item
            // check the repeativeness of pattern letter
            char skeletonChar = prevCh;
            if ( skeletonChar == 'L' ) {
                // for skeleton "M+", the pattern is "...L..."
                skeletonChar = 'M';
            }
            int fieldCount = bestMatchSkeletonFieldWidth[skeletonChar - PATTERN_CHAR_BASE];
            int inputFieldCount = inputSkeletonFieldWidth[skeletonChar - PATTERN_CHAR_BASE];
            if ( fieldCount == count && inputFieldCount > fieldCount ) {
                count = inputFieldCount - fieldCount;
                for ( int j = 0; j < count; ++j ) {
                    adjustedPtn.append(prevCh);
                }
            }
        }
        return adjustedPtn.toString();
    }

    /**
     * Does the same thing as String.replace(), except that it won't perform the
     * substitution inside quoted literal text.
     * @param targetString The string to perform the find-replace operation on.
     * @param strToReplace The string to search for and replace in the target string.
     * @param strToReplaceWith The string to substitute in wherever `stringToReplace` was found.
     */
    private static String findReplaceInPattern(String targetString,
                                               String strToReplace,
                                               String strToReplaceWith) {
        int firstQuoteIndex = targetString.indexOf("\'");
        if (firstQuoteIndex < 0) {
            return targetString.replace(strToReplace, strToReplaceWith);
        } else {
            StringBuilder result = new StringBuilder();
            String source = targetString;

            while (firstQuoteIndex >= 0) {
                int secondQuoteIndex = source.indexOf("\'", firstQuoteIndex + 1);
                if (secondQuoteIndex < 0) {
                    secondQuoteIndex = source.length() - 1;
                }

                String unquotedText = source.substring(0, firstQuoteIndex);
                String quotedText = source.substring(firstQuoteIndex, secondQuoteIndex + 1);

                result.append(unquotedText.replace(strToReplace, strToReplaceWith));
                result.append(quotedText);

                source = source.substring(secondQuoteIndex + 1);
                firstQuoteIndex = source.indexOf("\'");
            }
            result.append(source.replace(strToReplace, strToReplaceWith));
            return result.toString();
        }
  }


    /*
     * Concat a single date pattern with a time interval pattern,
     * set it into the intervalPatterns, while field is time field.
     * This is used to handle time interval patterns on skeleton with
     * both time and date. Present the date followed by
     * the range expression for the time.
     * @param dtfmt                  date and time format
     * @param datePattern            date pattern
     * @param field                  time calendar field: AM_PM, HOUR, MINUTE
     * @param intervalPatterns       interval patterns
     */
    private void concatSingleDate2TimeInterval(String dtfmt,
                                               String datePattern,
                                               int field,
                                               Map<String, PatternInfo> intervalPatterns)
    {

        PatternInfo  timeItvPtnInfo =
            intervalPatterns.get(DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field]);
        if ( timeItvPtnInfo != null ) {
            String timeIntervalPattern = timeItvPtnInfo.getFirstPart() +
                                         timeItvPtnInfo.getSecondPart();
            String pattern = SimpleFormatterImpl.formatRawPattern(
                    dtfmt, 2, 2, timeIntervalPattern, datePattern);
            timeItvPtnInfo = DateIntervalInfo.genPatternInfo(pattern,
                                timeItvPtnInfo.firstDateInPtnIsLaterDate());
            intervalPatterns.put(
              DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field], timeItvPtnInfo);
        }
        // else: fall back
        // it should not happen if the interval format defined is valid
    }


    /*
     * check whether a calendar field present in a skeleton.
     * @param field      calendar field need to check
     * @param skeleton   given skeleton on which to check the calendar field
     * @return           true if field present in a skeleton.
     */
    private static boolean fieldExistsInSkeleton(int field, String skeleton)
    {
        String fieldChar = DateIntervalInfo.CALENDAR_FIELD_TO_PATTERN_LETTER[field];
        return ( (skeleton.indexOf(fieldChar) == -1) ? false : true ) ;
    }


    /*
     * readObject.
     */
    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        initializePattern(isDateIntervalInfoDefault ? LOCAL_PATTERN_CACHE : null);
        // if deserialized from a release that didn't have fCapitalizationSetting, set it to default
        if (fCapitalizationSetting == null) {
            fCapitalizationSetting = DisplayContext.CAPITALIZATION_NONE;
        }
    }

    /**
     * Get the internal patterns for the skeleton
     * @internal CLDR
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public Map<String, PatternInfo> getRawPatterns() {
        // this is unmodifiable, so ok to return directly
        return fIntervalPatterns;
    }
}

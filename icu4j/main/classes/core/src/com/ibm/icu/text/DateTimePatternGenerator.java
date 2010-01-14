/*
 ********************************************************************************
 * Copyright (C) 2006-2010, Google, International Business Machines Corporation *
 * and others. All Rights Reserved.                                             *
 ********************************************************************************
 */
package com.ibm.icu.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.PatternTokenizer;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * This class provides flexible generation of date format patterns, like
 * "yy-MM-dd". The user can build up the generator by adding successive
 * patterns. Once that is done, a query can be made using a "skeleton", which is
 * a pattern which just includes the desired fields and lengths. The generator
 * will return the "best fit" pattern corresponding to that skeleton.
 * <p>
 * The main method people will use is getBestPattern(String skeleton), since
 * normally this class is pre-built with data from a particular locale. However,
 * generators can be built directly from other data as well.
 * <pre>
 * // some simple use cases
 * Date sampleDate = new Date(99, 9, 13, 23, 58, 59);
 * ULocale locale = ULocale.GERMANY;
 * TimeZone zone = TimeZone.getTimeZone(&quot;Europe/Paris&quot;);
 * 
 * // make from locale
 * 
 * DateTimePatternGenerator gen = DateTimePatternGenerator.getInstance(locale);
 * SimpleDateFormat format = new SimpleDateFormat(gen.getBestPattern(&quot;MMMddHmm&quot;),
 *     locale);
 * format.setTimeZone(zone);
 * assertEquals(&quot;simple format: MMMddHmm&quot;, 
 *     &quot;8:58 14. Okt&quot;,
 *     format.format(sampleDate));
 * // (a generator can be built from scratch, but that is not a typical use case)
 * 
 * // modify the generator by adding patterns
 * DateTimePatternGenerator.PatternInfo returnInfo = new DateTimePatternGenerator.PatternInfo();
 * gen.add(&quot;d'. von' MMMM&quot;, true, returnInfo);
 * // the returnInfo is mostly useful for debugging problem cases
 * format.applyPattern(gen.getBestPattern(&quot;MMMMddHmm&quot;));
 * assertEquals(&quot;modified format: MMMddHmm&quot;,
 *     &quot;8:58 14. von Oktober&quot;,
 *     format.format(sampleDate));
 * 
 * // get a pattern and modify it
 * format = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.FULL,
 *     DateFormat.FULL, locale);
 * format.setTimeZone(zone);
 * String pattern = format.toPattern();
 * assertEquals(&quot;full-date&quot;,
 *     &quot;Donnerstag, 14. Oktober 1999 8:58 Uhr GMT+02:00&quot;,
 *     format.format(sampleDate));
 * 
 * // modify it to change the zone.
 * String newPattern = gen.replaceFieldTypes(pattern, &quot;vvvv&quot;);
 * format.applyPattern(newPattern);
 * assertEquals(&quot;full-date, modified zone&quot;,
 *     &quot;Donnerstag, 14. Oktober 1999 8:58 Uhr Frankreich&quot;,
 *     format.format(sampleDate));
 * </pre>
 * @stable ICU 3.6
 */
public class DateTimePatternGenerator implements Freezable<DateTimePatternGenerator>, Cloneable {
    // debugging flags
    //static boolean SHOW_DISTANCE = false;
    // TODO add hack to fix months for CJK, as per bug ticket 1099

    /**
     * Create empty generator, to be constructed with add(...) etc.
     * @stable ICU 3.6
     */
    public static DateTimePatternGenerator getEmptyInstance() {
        return new DateTimePatternGenerator();
    }

    /**
     * Only for use by subclasses
     * @stable ICU 3.6
     */
    protected DateTimePatternGenerator() {
    }

    /**
     * Construct a flexible generator according to data for the default locale.
     * @stable ICU 3.6
     */
    public static DateTimePatternGenerator getInstance() {
        return getInstance(ULocale.getDefault());
    }

    /**
     * Construct a flexible generator according to data for a given locale.
     * @param uLocale The locale to pass.
     * @stable ICU 3.6
     */
    public static DateTimePatternGenerator getInstance(ULocale uLocale) {
        return getFrozenInstance(uLocale).cloneAsThawed();
    }

    /**
     * Construct a frozen instance of DateTimePatternGenerator for a
     * given locale.  This method returns a cached frozen instance of
     * DateTimePatternGenerator, so less expensive than the regular
     * factory method.
     * @param uLocale The locale to pass.
     * @return A frozen DateTimePatternGenerator.
     * @internal
     */
    public static DateTimePatternGenerator getFrozenInstance(ULocale uLocale) {
        String localeKey = uLocale.toString();
        DateTimePatternGenerator result = DTPNG_CACHE.get(localeKey);
        if (result != null) {
            return result;
        }
        result = new DateTimePatternGenerator();
        String lang = uLocale.getLanguage();
        PatternInfo returnInfo = new PatternInfo();
        String shortTimePattern = null;
        // first load with the ICU patterns
        for (int i = DateFormat.FULL; i <= DateFormat.SHORT; ++i) {
            SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateInstance(i, uLocale);
            result.addPattern(df.toPattern(), false, returnInfo);
            df = (SimpleDateFormat) DateFormat.getTimeInstance(i, uLocale);
            result.addPattern(df.toPattern(), false, returnInfo);
            if (i == DateFormat.SHORT) {
                // keep this pattern to populate other time field
                // combination patterns by hackTimes later in this method.
                shortTimePattern = df.toPattern();

                // use hour style in SHORT time pattern as the default
                // hour style for the locale
                FormatParser fp = new FormatParser();
                fp.set(shortTimePattern);
                List<Object> items = fp.getItems();
                for (int idx = 0; idx < items.size(); idx++) {
                    Object item = items.get(idx);
                    if (item instanceof VariableField) {
                        VariableField fld = (VariableField)item;
                        if (fld.getType() == HOUR) {
                            result.defaultHourFormatChar = fld.toString().charAt(0);
                            break;
                        }
                    }
                }
            }
        }

        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, uLocale);
        ULocale parentLocale = rb.getULocale(); // for later
        // Get the correct calendar type
        String calendarTypeToUse = uLocale.getKeywordValue("calendar");
        if ( calendarTypeToUse == null ) {
            String[] preferredCalendarTypes = Calendar.getKeywordValuesForLocale("calendar", uLocale, true);
            calendarTypeToUse = preferredCalendarTypes[0]; // the most preferred calendar
        }
        if ( calendarTypeToUse == null ) {
            calendarTypeToUse = "gregorian"; // fallback
        }
        // Get data for that calendar
        rb = rb.getWithFallback("calendar");
        ICUResourceBundle calTypeBundle = rb.getWithFallback(calendarTypeToUse);
        // CLDR item formats


        // (hmm, do we need aliases in root for all non-gregorian calendars?)
        try {
            ICUResourceBundle itemBundle = calTypeBundle.getWithFallback("appendItems");
            for (int i=0; i<itemBundle.getSize(); ++i) {
                ICUResourceBundle formatBundle = (ICUResourceBundle)itemBundle.get(i);
                String formatName = itemBundle.get(i).getKey();
                String value = formatBundle.getString();
                result.setAppendItemFormat(getAppendFormatNumber(formatName), value);
            }
        }catch(Exception e) {
        }

        // CLDR item names (hmm, do we need aliases in root for all non-gregorian calendars?)
        try {
            ICUResourceBundle itemBundle = calTypeBundle.getWithFallback("fields");
            ICUResourceBundle fieldBundle, dnBundle;
            for (int i=0; i<TYPE_LIMIT; ++i) {
                if ( isCLDRFieldName(i) ) {
                    fieldBundle = itemBundle.getWithFallback(CLDR_FIELD_NAME[i]);
                    dnBundle = fieldBundle.getWithFallback("dn");
                    String value = dnBundle.getString();
                    //System.out.println("Field name:"+value);
                    result.setAppendItemName(i, value);
                }
            }
        }catch(Exception e) {
        }

        // set the AvailableFormat in CLDR
        try {
            ICUResourceBundle formatBundle =  calTypeBundle.getWithFallback("availableFormats");
            //System.out.println("available format from current locale:"+uLocale.getName());
            for (int i=0; i<formatBundle.getSize(); ++i) { 
                String formatKey = formatBundle.get(i).getKey();
                String formatValue = formatBundle.get(i).getString();
                //System.out.println(" availableFormat:"+formatValue);
                result.setAvailableFormat(formatKey);
                // Add pattern with its associated skeleton. Override any duplicate derived from std patterns,
                // but not a previous availableFormats entry:
                result.addPatternWithSkeleton(formatValue, formatKey, false, returnInfo);
            } 
        }catch(Exception e) {
        }

        // ULocale parentLocale=uLocale; // now set up above with aliases resolved etc.
        while ( (parentLocale=parentLocale.getFallback()) != null) {
            ICUResourceBundle prb = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, parentLocale);
            prb = prb.getWithFallback("calendar");
            ICUResourceBundle pCalTypeBundle = prb.getWithFallback(calendarTypeToUse);
            try {
                ICUResourceBundle formatBundle =  pCalTypeBundle.getWithFallback("availableFormats");
                //System.out.println("available format from parent locale:"+parentLocale.getName());
                for (int i=0; i<formatBundle.getSize(); ++i) { 
                    String formatKey = formatBundle.get(i).getKey();
                    String formatValue = formatBundle.get(i).getString();
                    //System.out.println(" availableFormat:"+formatValue);
                    if (!result.isAvailableFormatSet(formatKey)) {
                        result.setAvailableFormat(formatKey);
                        // Add pattern with its associated skeleton. Override any duplicate derived from std patterns,
                        // but not a previous availableFormats entry:
                        result.addPatternWithSkeleton(formatValue, formatKey, false, returnInfo);
                        //System.out.println(" availableFormat:"+formatValue);
                    }
                } 
            }catch(Exception e) {
            }
        }

        // assume it is always big endian (ok for CLDR right now)
        // some languages didn't add mm:ss or HH:mm, so put in a hack to compute that from the short time.
        if (shortTimePattern != null) {
            hackTimes(result, returnInfo, shortTimePattern);
        }

        result.setDateTimeFormat(Calendar.getDateTimePattern(Calendar.getInstance(uLocale), uLocale, DateFormat.MEDIUM));

        // decimal point for seconds
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(uLocale);
        result.setDecimal(String.valueOf(dfs.getDecimalSeparator()));

        // freeze and cache
        result.freeze();
        DTPNG_CACHE.put(localeKey, result);
        return result;
    }

    private static void hackTimes(DateTimePatternGenerator result, PatternInfo returnInfo, String hackPattern) {
        result.fp.set(hackPattern);
        StringBuilder mmss = new StringBuilder();
        // to get mm:ss, we strip all but mm literal ss
        boolean gotMm = false;
        for (int i = 0; i < result.fp.items.size(); ++i) {
            Object item = result.fp.items.get(i);
            if (item instanceof String) {
                if (gotMm) {
                    mmss.append(result.fp.quoteLiteral(item.toString()));
                }
            } else {
                char ch = item.toString().charAt(0);
                if (ch == 'm') {
                    gotMm = true;
                    mmss.append(item);
                } else if (ch == 's') {
                    if (!gotMm) {
                        break; // failed
                    }
                    mmss.append(item);
                    result.addPattern(mmss.toString(), false, returnInfo);
                    break;
                } else if (gotMm || ch == 'z' || ch == 'Z' || ch == 'v' || ch == 'V') {
                    break; // failed
                }
            }
        }
        // to get hh:mm, we strip (literal ss) and (literal S)
        // the easiest way to do this is to mark the stuff we want to nuke, then remove it in a second pass.
        BitSet variables = new BitSet();
        BitSet nuke = new BitSet();
        for (int i = 0; i < result.fp.items.size(); ++i) {
            Object item = result.fp.items.get(i);
            if (item instanceof VariableField) {
                variables.set(i);
                char ch = item.toString().charAt(0);
                if (ch == 's' || ch == 'S') {
                    nuke.set(i);
                    for (int j = i-1; j >= 0; ++j) {
                        if (variables.get(j)) break;
                        nuke.set(i);
                    }
                }
            }
        }
        String hhmm = getFilteredPattern(result.fp, nuke);
        result.addPattern(hhmm, false, returnInfo);
    }

    private static String getFilteredPattern(FormatParser fp, BitSet nuke) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < fp.items.size(); ++i) {
            if (nuke.get(i)) continue;
            Object item = fp.items.get(i);
            if (item instanceof String) {
                result.append(fp.quoteLiteral(item.toString()));
            } else {
                result.append(item.toString());
            }
        }
        return result.toString();
    }

    /*private static int getAppendNameNumber(String string) {
        for (int i = 0; i < CLDR_FIELD_NAME.length; ++i) {
            if (CLDR_FIELD_NAME[i].equals(string)) return i;
        }
        return -1;
    }*/

    private static int getAppendFormatNumber(String string) {
        for (int i = 0; i < CLDR_FIELD_APPEND.length; ++i) {
            if (CLDR_FIELD_APPEND[i].equals(string)) return i;
        }
        return -1;

    }

    private static boolean isCLDRFieldName(int index) {
        if ((index<0) && (index>=TYPE_LIMIT)) {
            return false;
        }
        if (CLDR_FIELD_NAME[index].charAt(0) == '*') {
            return false;
        }
        else {
            return true;
        }
    }

    /**
     * Return the best pattern matching the input skeleton. It is guaranteed to
     * have all of the fields in the skeleton.
     * 
     * @param skeleton The skeleton is a pattern containing only the variable fields.
     *            For example, "MMMdd" and "mmhh" are skeletons.
     * @return Best pattern matching the input skeleton.
     * @stable ICU 3.6
     */
    public String getBestPattern(String skeleton) {
        return getBestPattern(skeleton, null, MATCH_NO_OPTIONS);
    }

    /**
     * Return the best pattern matching the input skeleton. It is guaranteed to
     * have all of the fields in the skeleton.
     * 
     * @param skeleton The skeleton is a pattern containing only the variable fields.
     *            For example, "MMMdd" and "mmhh" are skeletons.
     * @param options MATCH_xxx options for forcing the length of specified fields in
     *            the returned pattern to match those in the skeleton (when this would
     *            not happen otherwise). For default behavior, use MATCH_NO_OPTIONS.
     * @return Best pattern matching the input skeleton (and options).
     * @draft ICU 4.4
     */
    public String getBestPattern(String skeleton, int options) {
        return getBestPattern(skeleton, null, options);
    }

    /*
     * getBestPattern which takes optional skip matcher
     */
    private String getBestPattern(String skeleton, DateTimeMatcher skipMatcher, int options) {
        //if (!isComplete) complete();
        // if skeleton contains meta hour field 'j', then
        // replace it with the default hour format char
        skeleton = skeleton.replaceAll("j", String.valueOf(defaultHourFormatChar));

        String datePattern, timePattern;
        synchronized(this) {
            current.set(skeleton, fp);
            PatternWithMatcher bestWithMatcher = getBestRaw(current, -1, _distanceInfo, skipMatcher);
            if (_distanceInfo.missingFieldMask == 0 && _distanceInfo.extraFieldMask == 0) {
                // we have a good item. Adjust the field types
                return adjustFieldTypes(bestWithMatcher, current, false, options);
            }
            int neededFields = current.getFieldMask();

            // otherwise break up by date and time.
            datePattern = getBestAppending(current, neededFields & DATE_MASK, _distanceInfo, skipMatcher, options);
            timePattern = getBestAppending(current, neededFields & TIME_MASK, _distanceInfo, skipMatcher, options);
        }

        if (datePattern == null) return timePattern == null ? "" : timePattern;
        if (timePattern == null) return datePattern;
        return MessageFormat.format(getDateTimeFormat(), new Object[]{timePattern, datePattern});
    }

    /**
     * PatternInfo supplies output parameters for add(...). It is used because
     * Java doesn't have real output parameters. It is treated like a struct (eg
     * Point), so all fields are public.
     * 
     * @stable ICU 3.6
     */
    public static final class PatternInfo { // struct for return information
        /**
         * @stable ICU 3.6
         */
        public static final int OK = 0;

        /**
         * @stable ICU 3.6
         */
        public static final int BASE_CONFLICT = 1;

        /**
         * @stable ICU 3.6
         */
        public static final int CONFLICT = 2;

        /**
         * @stable ICU 3.6
         */
        public int status;

        /**
         * @stable ICU 3.6
         */
        public String conflictingPattern;

        /**
         * Simple constructor, since this is treated like a struct.
         * @stable ICU 3.6
         */
        public PatternInfo() {
        }
    }

    /**
     * Adds a pattern to the generator. If the pattern has the same skeleton as
     * an existing pattern, and the override parameter is set, then the previous
     * value is overridden. Otherwise, the previous value is retained. In either
     * case, the conflicting information is returned in PatternInfo.
     * <p>
     * Note that single-field patterns (like "MMM") are automatically added, and
     * don't need to be added explicitly!
     * 
     * @param pattern Pattern to add.
     * @param override When existing values are to be overridden use true, otherwise
     *            use false.
     * @param returnInfo Returned information.
     * @stable ICU 3.6
     */
    public DateTimePatternGenerator addPattern(String pattern, boolean override, PatternInfo returnInfo) {
        return addPatternWithSkeleton(pattern, null, override, returnInfo);
    }

    /*
     * addPatternWithSkeleton:
     * If skeletonToUse is specified, then an availableFormats entry is being added. In this case:
     * 1. We pass that skeleton to DateTimeMatcher().set instead of having it derive a skeleton from the pattern.
     * 2. If the new entry's skeleton or basePattern does match an existing entry but that entry also had a skeleton specified
     * (i.e. it was also from availableFormats), then the new entry does not override it regardless of the value of the override
     * parameter. This prevents later availableFormats entries from a parent locale overriding earlier ones from the actual
     * specified locale. However, availableFormats entries *should* override entries with matching skeleton whose skeleton was
     * derived (i.e. entries derived from the standard date/time patters for the specified locale).
     * 3. When adding the pattern (skeleton2pattern.put, basePattern_pattern.put), we set a field to indicate that the added
     * entry had a specified skeleton.
     */
    private DateTimePatternGenerator addPatternWithSkeleton(String pattern, String skeletonToUse, boolean override, PatternInfo returnInfo) {
        checkFrozen();
        DateTimeMatcher matcher;
        if (skeletonToUse == null) {
            matcher = new DateTimeMatcher().set(pattern, fp);
        } else {
            matcher = new DateTimeMatcher().set(skeletonToUse, fp);
        }
        String basePattern = matcher.getBasePattern();
        PatternWithSkeletonFlag previousPatternWithSameBase = basePattern_pattern.get(basePattern);
        if (previousPatternWithSameBase != null) {
            returnInfo.status = PatternInfo.BASE_CONFLICT;
            returnInfo.conflictingPattern = previousPatternWithSameBase.pattern;
            if (!override || (skeletonToUse != null && previousPatternWithSameBase.skeletonWasSpecified)) return this;
        }
        PatternWithSkeletonFlag previousValue = skeleton2pattern.get(matcher);
        if (previousValue != null) {
            returnInfo.status = PatternInfo.CONFLICT;
            returnInfo.conflictingPattern = previousValue.pattern;
            if (!override || (skeletonToUse != null && previousValue.skeletonWasSpecified)) return this;
        }
        returnInfo.status = PatternInfo.OK;
        returnInfo.conflictingPattern = "";
        PatternWithSkeletonFlag patWithSkelFlag = new PatternWithSkeletonFlag(pattern,skeletonToUse != null);
        skeleton2pattern.put(matcher, patWithSkelFlag);
        basePattern_pattern.put(basePattern, patWithSkelFlag);
        return this;
    }

    /**
     * Utility to return a unique skeleton from a given pattern. For example,
     * both "MMM-dd" and "dd/MMM" produce the skeleton "MMMdd".
     * 
     * @param pattern Input pattern, such as "dd/MMM"
     * @return skeleton, such as "MMMdd"
     * @stable ICU 3.6
     */
    public String getSkeleton(String pattern) {
        synchronized (this) { // synchronized since a getter must be thread-safe
            current.set(pattern, fp);
            return current.toString();
        }
    }

    /**
     * Utility to return a unique base skeleton from a given pattern. This is
     * the same as the skeleton, except that differences in length are minimized
     * so as to only preserve the difference between string and numeric form. So
     * for example, both "MMM-dd" and "d/MMM" produce the skeleton "MMMd"
     * (notice the single d).
     * 
     * @param pattern Input pattern, such as "dd/MMM"
     * @return skeleton, such as "MMMdd"
     * @stable ICU 3.6
     */
    public String getBaseSkeleton(String pattern) {
        synchronized (this) { // synchronized since a getter must be thread-safe
            current.set(pattern, fp);
            return current.getBasePattern();
        }
    }

    /**
     * Return a list of all the skeletons (in canonical form) from this class,
     * and the patterns that they map to.
     * 
     * @param result an output Map in which to place the mapping from skeleton to
     *            pattern. If you want to see the internal order being used,
     *            supply a LinkedHashMap. If the input value is null, then a
     *            LinkedHashMap is allocated.
     *            <p>
     *            <i>Issue: an alternate API would be to just return a list of
     *            the skeletons, and then have a separate routine to get from
     *            skeleton to pattern.</i>
     * @return the input Map containing the values.
     * @stable ICU 3.6
     */
    public Map<String, String> getSkeletons(Map<String, String> result) {
        if (result == null) {
            result = new LinkedHashMap<String, String>();
        }
        for (DateTimeMatcher item : skeleton2pattern.keySet()) {
            PatternWithSkeletonFlag patternWithSkelFlag = skeleton2pattern.get(item);
            String pattern = patternWithSkelFlag.pattern;
            if (CANONICAL_SET.contains(pattern)) {
                continue;
            }
            result.put(item.toString(), pattern);
        }
        return result;
    }

    /**
     * Return a list of all the base skeletons (in canonical form) from this class
     * @stable ICU 3.6
     */
    public Set<String> getBaseSkeletons(Set<String> result) {
        if (result == null) {
            result = new HashSet<String>();
        }
        result.addAll(basePattern_pattern.keySet());
        return result;
    }

    /**
     * Adjusts the field types (width and subtype) of a pattern to match what is
     * in a skeleton. That is, if you supply a pattern like "d-M H:m", and a
     * skeleton of "MMMMddhhmm", then the input pattern is adjusted to be
     * "dd-MMMM hh:mm". This is used internally to get the best match for the
     * input skeleton, but can also be used externally.
     * 
     * @param pattern input pattern
     * @param skeleton For the pattern to match to.
     * @return pattern adjusted to match the skeleton fields widths and subtypes.
     * @stable ICU 3.6
     */
    public String replaceFieldTypes(String pattern, String skeleton) {
        return replaceFieldTypes(pattern, skeleton, MATCH_NO_OPTIONS);
    }

    /**
     * Adjusts the field types (width and subtype) of a pattern to match what is
     * in a skeleton. That is, if you supply a pattern like "d-M H:m", and a
     * skeleton of "MMMMddhhmm", then the input pattern is adjusted to be
     * "dd-MMMM hh:mm". This is used internally to get the best match for the
     * input skeleton, but can also be used externally.
     * 
     * @param pattern input pattern
     * @param skeleton For the pattern to match to.
     * @param options MATCH_xxx options for forcing the length of specified fields in
     *            the returned pattern to match those in the skeleton (when this would
     *            not happen otherwise). For default behavior, use MATCH_NO_OPTIONS.
     * @return pattern adjusted to match the skeleton fields widths and subtypes.
     * @draft ICU 4.4
     */
    public String replaceFieldTypes(String pattern, String skeleton, int options) {
        synchronized (this) { // synchronized since a getter must be thread-safe
            PatternWithMatcher patternNoMatcher = new PatternWithMatcher(pattern, null);
            return adjustFieldTypes(patternNoMatcher, current.set(skeleton, fp), false, options);
        }
    }

    /**
     * The date time format is a message format pattern used to compose date and
     * time patterns. The default value is "{1} {0}", where {1} will be replaced
     * by the date pattern and {0} will be replaced by the time pattern.
     * <p>
     * This is used when the input skeleton contains both date and time fields,
     * but there is not a close match among the added patterns. For example,
     * suppose that this object was created by adding "dd-MMM" and "hh:mm", and
     * its datetimeFormat is the default "{1} {0}". Then if the input skeleton
     * is "MMMdhmm", there is not an exact match, so the input skeleton is
     * broken up into two components "MMMd" and "hmm". There are close matches
     * for those two skeletons, so the result is put together with this pattern,
     * resulting in "d-MMM h:mm".
     * 
     * @param dateTimeFormat message format pattern, where {1} will be replaced by the date
     *            pattern and {0} will be replaced by the time pattern.
     * @stable ICU 3.6
     */
    public void setDateTimeFormat(String dateTimeFormat) {
        checkFrozen();
        this.dateTimeFormat = dateTimeFormat;
    }

    /**
     * Getter corresponding to setDateTimeFormat.
     * 
     * @return pattern
     * @stable ICU 3.6
     */
    public String getDateTimeFormat() {
        return dateTimeFormat;
    }

    /**
     * The decimal value is used in formatting fractions of seconds. If the
     * skeleton contains fractional seconds, then this is used with the
     * fractional seconds. For example, suppose that the input pattern is
     * "hhmmssSSSS", and the best matching pattern internally is "H:mm:ss", and
     * the decimal string is ",". Then the resulting pattern is modified to be
     * "H:mm:ss,SSSS"
     * 
     * @param decimal The decimal to set to.
     * @stable ICU 3.6
     */
    public void setDecimal(String decimal) {
        checkFrozen();
        this.decimal = decimal;
    }

    /**
     * Getter corresponding to setDecimal.
     * @return string corresponding to the decimal point
     * @stable ICU 3.6
     */
    public String getDecimal() {
        return decimal;
    }

    /**
     * Redundant patterns are those which if removed, make no difference in the
     * resulting getBestPattern values. This method returns a list of them, to
     * help check the consistency of the patterns used to build this generator.
     * 
     * @param output stores the redundant patterns that are removed. To get these
     *            in internal order, supply a LinkedHashSet. If null, a
     *            collection is allocated.
     * @return the collection with added elements.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public Collection<String> getRedundants(Collection<String> output) {
        synchronized (this) { // synchronized since a getter must be thread-safe
            if (output == null) {
                output = new LinkedHashSet<String>();
            }
            for (DateTimeMatcher cur : skeleton2pattern.keySet()) {
                PatternWithSkeletonFlag patternWithSkelFlag = skeleton2pattern.get(cur);
                String pattern = patternWithSkelFlag.pattern;
                if (CANONICAL_SET.contains(pattern)) {
                    continue;
                }
                String trial = getBestPattern(cur.toString(), cur, MATCH_NO_OPTIONS);
                if (trial.equals(pattern)) {
                    output.add(pattern);
                }
            }
            ///CLOVER:OFF
            //The following would never be called since the parameter is false
            //Eclipse stated the following is "dead code"
            /*if (false) { // ordered
                DateTimePatternGenerator results = new DateTimePatternGenerator();
                PatternInfo pinfo = new PatternInfo();
                for (DateTimeMatcher cur : skeleton2pattern.keySet()) {
                    String pattern = skeleton2pattern.get(cur);
                    if (CANONICAL_SET.contains(pattern)) {
                        continue;
                    }
                    //skipMatcher = current;
                    String trial = results.getBestPattern(cur.toString());
                    if (trial.equals(pattern)) {
                        output.add(pattern);
                    } else {
                        results.addPattern(pattern, false, pinfo);
                    }
                }
            }*/
            ///CLOVER:ON
            return output;
        }
    }

    // Field numbers, used for AppendItem functions

    /**
     * @stable ICU 3.6
     */
    static final public int ERA = 0;

    /**
     * @stable ICU 3.6
     */
    static final public int YEAR = 1; 

    /**
     * @stable ICU 3.6
     */
    static final public int QUARTER = 2; 

    /**
     * @stable ICU 3.6
     */
    static final public int MONTH = 3;

    /**
     * @stable ICU 3.6
     */
    static final public int WEEK_OF_YEAR = 4; 

    /**
     * @stable ICU 3.6
     */
    static final public int WEEK_OF_MONTH = 5; 

    /**
     * @stable ICU 3.6
     */
    static final public int WEEKDAY = 6; 

    /**
     * @stable ICU 3.6
     */
    static final public int DAY = 7;

    /**
     * @stable ICU 3.6
     */
    static final public int DAY_OF_YEAR = 8; 

    /**
     * @stable ICU 3.6
     */
    static final public int DAY_OF_WEEK_IN_MONTH = 9; 

    /**
     * @stable ICU 3.6
     */
    static final public int DAYPERIOD = 10;

    /**
     * @stable ICU 3.6
     */
    static final public int HOUR = 11; 

    /**
     * @stable ICU 3.6
     */
    static final public int MINUTE = 12; 

    /**
     * @stable ICU 3.6
     */
    static final public int SECOND = 13; 

    /**
     * @stable ICU 3.6
     */
    static final public int FRACTIONAL_SECOND = 14;

    /**
     * @stable ICU 3.6
     */
    static final public int ZONE = 15; 

    /**
     * @stable ICU 3.6
     */
    static final public int TYPE_LIMIT = 16;

    // Option masks for getBestPattern, replaceFieldTypes (individual masks may be ORed together)

    /**
     * @draft ICU 4.4
     */
    public static final int MATCH_NO_OPTIONS = 0;

    /**
     * @draft ICU 4.4
     */
    public static final int MATCH_HOUR_FIELD_LENGTH = 1 << HOUR;

    /**
     * @draft ICU 4.4
     */
    public static final int MATCH_ALL_FIELDS_LENGTH = (1 << TYPE_LIMIT) - 1;

    /**
     * An AppendItem format is a pattern used to append a field if there is no
     * good match. For example, suppose that the input skeleton is "GyyyyMMMd",
     * and there is no matching pattern internally, but there is a pattern
     * matching "yyyyMMMd", say "d-MM-yyyy". Then that pattern is used, plus the
     * G. The way these two are conjoined is by using the AppendItemFormat for G
     * (era). So if that value is, say "{0}, {1}" then the final resulting
     * pattern is "d-MM-yyyy, G".
     * <p>
     * There are actually three available variables: {0} is the pattern so far,
     * {1} is the element we are adding, and {2} is the name of the element.
     * <p>
     * This reflects the way that the CLDR data is organized.
     * 
     * @param field such as ERA
     * @param value pattern, such as "{0}, {1}"
     * @stable ICU 3.6
     */
    public void setAppendItemFormat(int field, String value) {
        checkFrozen();
        appendItemFormats[field] = value;
    }

    /**
     * Getter corresponding to setAppendItemFormats. Values below 0 or at or
     * above TYPE_LIMIT are illegal arguments.
     * 
     * @param field The index to retrieve the append item formats.
     * @return append pattern for field
     * @stable ICU 3.6
     */
    public String getAppendItemFormat(int field) {
        return appendItemFormats[field];
    }

    /**
     * Sets the names of fields, eg "era" in English for ERA. These are only
     * used if the corresponding AppendItemFormat is used, and if it contains a
     * {2} variable.
     * <p>
     * This reflects the way that the CLDR data is organized.
     * 
     * @param field Index of the append item names.
     * @param value The value to set the item to.
     * @stable ICU 3.6
     */
    public void setAppendItemName(int field, String value) {
        checkFrozen();
        appendItemNames[field] = value;
    }

    /**
     * Getter corresponding to setAppendItemNames. Values below 0 or at or above
     * TYPE_LIMIT are illegal arguments.
     * 
     * @param field The index to get the append item name.
     * @return name for field
     * @stable ICU 3.6
     */
    public String getAppendItemName(int field) {
        return appendItemNames[field];
    }

    /**
     * Determines whether a skeleton contains a single field
     * 
     * @param skeleton The skeleton to determine if it contains a single field.
     * @return true or not
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static boolean isSingleField(String skeleton) {
        char first = skeleton.charAt(0);
        for (int i = 1; i < skeleton.length(); ++i) {
            if (skeleton.charAt(i) != first) return false;
        }
        return true;
    }

    /**
     * Add key to HashSet cldrAvailableFormatKeys.
     * 
     * @param key of the availableFormats in CLDR
     * @stable ICU 3.6
     */
    private void setAvailableFormat(String key) {
        checkFrozen();
        cldrAvailableFormatKeys.add(key);
    }

    /**
     * This function checks the corresponding slot of CLDR_AVAIL_FORMAT_KEY[]
     * has been added to DateTimePatternGenerator.
     * The function is to avoid the duplicate availableFomats added to
     * the pattern map from parent locales.
     * 
     * @param key of the availableFormatMask in CLDR
     * @return TRUE if the corresponding slot of CLDR_AVAIL_FORMAT_KEY[]
     * has been added to DateTimePatternGenerator.
     * @stable ICU 3.6
     */
    private boolean isAvailableFormatSet(String key) {
        return cldrAvailableFormatKeys.contains(key);
    }

    /**
     * Boilerplate for Freezable
     * @stable ICU 3.6
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Boilerplate for Freezable
     * @stable ICU 3.6
     */
    public DateTimePatternGenerator freeze() {
        frozen = true;
        return this;
    }

    /**
     * Boilerplate for Freezable
     * @stable ICU 3.6
     */
    public DateTimePatternGenerator cloneAsThawed() {
        DateTimePatternGenerator result = (DateTimePatternGenerator) (this.clone());
        frozen = false;
        return result;
    }

    /**
     * Boilerplate
     * @stable ICU 3.6
     */
    @SuppressWarnings("unchecked")
    public Object clone() {
        try {
            DateTimePatternGenerator result = (DateTimePatternGenerator) (super.clone());
            result.skeleton2pattern = (TreeMap<DateTimeMatcher, PatternWithSkeletonFlag>) skeleton2pattern.clone();
            result.basePattern_pattern = (TreeMap<String, PatternWithSkeletonFlag>) basePattern_pattern.clone();
            result.appendItemFormats = appendItemFormats.clone();
            result.appendItemNames = appendItemNames.clone();
            result.current = new DateTimeMatcher();
            result.fp = new FormatParser();
            result._distanceInfo = new DistanceInfo();

            result.frozen = false;
            return result;
        } catch (CloneNotSupportedException e) {
            ///CLOVER:OFF
            throw new IllegalArgumentException("Internal Error");
            ///CLOVER:ON
        }
    }

    /**
     * Utility class for FormatParser. Immutable class that is only used to mark
     * the difference between a variable field and a literal string. Each
     * variable field must consist of 1 to n variable characters, representing
     * date format fields. For example, "VVVV" is valid while "V4" is not, nor
     * is "44".
     * 
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static class VariableField {
        private final String string;
        private final int canonicalIndex;

        /**
         * Create a variable field: equivalent to VariableField(string,false);
         * @param string The string for the variable field.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        public VariableField(String string) {
            this(string, false);
        }
        /**
         * Create a variable field
         * @param string The string for the variable field
         * @param strict If true, then only allows exactly those lengths specified by CLDR for variables. For example, "hh:mm aa" would throw an exception.
         * @throws IllegalArgumentException if the variable field is not valid.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        public VariableField(String string, boolean strict) {
            canonicalIndex = DateTimePatternGenerator.getCanonicalIndex(string, strict);
            if (canonicalIndex < 0) {
                throw new IllegalArgumentException("Illegal datetime field:\t"
                        + string);
            }
            this.string = string;
        }

        /**
         * Get the main type of this variable. These types are ERA, QUARTER,
         * MONTH, DAY, WEEK_OF_YEAR, WEEK_OF_MONTH, WEEKDAY, DAY, DAYPERIOD
         * (am/pm), HOUR, MINUTE, SECOND,FRACTIONAL_SECOND, ZONE. 
         * @return main type.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        public int getType() {
            return types[canonicalIndex][1];
        }

        /**
         * Protected method.
         */
        protected boolean isNumeric() {
            return types[canonicalIndex][2] > 0;
        }

        /**
         * Private method.
         */
        private int getCanonicalIndex() {
            return canonicalIndex;
        }

        /**
         * Get the string represented by this variable.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        public String toString() {
            return string;
        }
    }

    /**
     * This class provides mechanisms for parsing a SimpleDateFormat pattern
     * or generating a new pattern, while handling the quoting. It represents
     * the result of the parse as a list of items, where each item is either a
     * literal string or a variable field. When parsing It can be used to find
     * out which variable fields are in a date format, and in what order, such
     * as for presentation in a UI as separate text entry fields. It can also be
     * used to construct new SimpleDateFormats.
     * <p>Example:
     * <pre>
    public boolean containsZone(String pattern) {
        for (Iterator it = formatParser.set(pattern).getItems().iterator(); it.hasNext();) {
            Object item = it.next();
            if (item instanceof VariableField) {
                VariableField variableField = (VariableField) item;
                if (variableField.getType() == DateTimePatternGenerator.ZONE) {
                    return true;
                }
            }
        }
        return false;
    }
     *  </pre>
     * @internal
     * @deprecated This API is ICU internal only.
     */
    static public class FormatParser {
        private transient PatternTokenizer tokenizer = new PatternTokenizer()
        .setSyntaxCharacters(new UnicodeSet("[a-zA-Z]"))
        .setExtraQuotingCharacters(new UnicodeSet("[[[:script=Latn:][:script=Cyrl:]]&[[:L:][:M:]]]"))
        //.setEscapeCharacters(new UnicodeSet("[^\\u0020-\\u007E]")) // WARNING: DateFormat doesn't accept \\uXXXX
        .setUsingQuote(true);
        private List<Object> items = new ArrayList<Object>();

        /**
         * Construct an empty date format parser, to which strings and variables can be added with set(...).
         * @internal
         * @deprecated This API is ICU internal only.
         */
        public FormatParser() {
        }

        /**
         * Parses the string into a list of items.
         * @param string The string to parse.
         * @return this, for chaining
         * @internal
         * @deprecated This API is ICU internal only.
         */
        final public FormatParser set(String string) {
            return set(string, false);
        }

        /**
         * Parses the string into a list of items, taking into account all of the quoting that may be going on.
         * @param string  The string to parse.
         * @param strict If true, then only allows exactly those lengths specified by CLDR for variables. For example, "hh:mm aa" would throw an exception.
         * @return this, for chaining
         * @internal
         * @deprecated This API is ICU internal only.
         */
        public FormatParser set(String string, boolean strict) {
            items.clear();
            if (string.length() == 0) return this;
            tokenizer.setPattern(string);
            StringBuffer buffer = new StringBuffer();
            StringBuffer variable = new StringBuffer();
            while (true) {
                buffer.setLength(0);
                int status = tokenizer.next(buffer);
                if (status == PatternTokenizer.DONE) break;
                if (status == PatternTokenizer.SYNTAX) {
                    if (variable.length() != 0 && buffer.charAt(0) != variable.charAt(0)) {
                        addVariable(variable, false);
                    }
                    variable.append(buffer);
                } else {
                    addVariable(variable, false);
                    items.add(buffer.toString());
                }
            }
            addVariable(variable, false);
            return this;
        }

        private void addVariable(StringBuffer variable, boolean strict) {
            if (variable.length() != 0) {
                items.add(new VariableField(variable.toString(), strict));
                variable.setLength(0);
            }
        }

        //        /** Private method. Return a collection of fields. These will be a mixture of literal Strings and VariableFields. Any "a" variable field is removed.
        //         * @param output List to append the items to. If null, is allocated as an ArrayList.
        //         * @return list
        //         */
        //        private List getVariableFields(List output) {
        //            if (output == null) output = new ArrayList();
        //            main:
        //                for (Iterator it = items.iterator(); it.hasNext();) {
        //                    Object item = it.next();
        //                    if (item instanceof VariableField) {
        //                        String s = item.toString();
        //                        switch(s.charAt(0)) {
        //                        //case 'Q': continue main; // HACK
        //                        case 'a': continue main; // remove
        //                        }
        //                        output.add(item);
        //                    }
        //                }
        //            //System.out.println(output);
        //            return output;
        //        }

        //        /**
        //         * Produce a string which concatenates all the variables. That is, it is the logically the same as the input with all literals removed.
        //         * @return a string which is a concatenation of all the variable fields
        //         * @internal
        //         * @deprecated This API is ICU internal only.
        //         */
        //        public String getVariableFieldString() {
        //            List list = getVariableFields(null);
        //            StringBuffer result = new StringBuffer();
        //            for (Iterator it = list.iterator(); it.hasNext();) {
        //                String item = it.next().toString();
        //                result.append(item);
        //            }
        //            return result.toString();
        //        }

        /**
         * Returns modifiable list which is a mixture of Strings and VariableFields, in the order found during parsing. The strings represent literals, and have all quoting removed. Thus the string "dd 'de' MM" will parse into three items:
         * <pre>
         * VariableField: dd
         * String: " de "
         * VariableField: MM
         * </pre>
         * The list is modifiable, so you can add any strings or variables to it, or remove any items.
         * @return modifiable list of items.
         * @internal
         * @deprecated This API is ICU internal only.
         */
        public List<Object> getItems() {
            return items;
        }

        /** Provide display form of formatted input. Each literal string is quoted if necessary.. That is, if the input was "hh':'mm", the result would be "hh:mm", since the ":" doesn't need quoting. See quoteLiteral().
         * @return printable output string
         * @internal
         * @deprecated This API is ICU internal only.
         */
        public String toString() {
            return toString(0, items.size());
        }

        /**
         * Provide display form of a segment of the parsed input. Each literal string is minimally quoted. That is, if the input was "hh':'mm", the result would be "hh:mm", since the ":" doesn't need quoting. See quoteLiteral().
         * @param start item to start from
         * @param limit last item +1
         * @return printable output string
         * @internal
         * @deprecated This API is ICU internal only.
         */
        public String toString(int start, int limit) {
            StringBuffer result = new StringBuffer();
            for (int i = start; i < limit; ++i) {
                Object item = items.get(i);
                if (item instanceof String) {
                    String itemString = (String) item;
                    result.append(tokenizer.quoteLiteral(itemString));
                } else {
                    result.append(items.get(i).toString());
                }
            }
            return result.toString();
        }

        /**
         * Returns true if it has a mixture of date and time variable fields: that is, at least one date variable and at least one time variable.
         * @return true or false
         * @internal
         * @deprecated This API is ICU internal only.
         */
        public boolean hasDateAndTimeFields() {
            int foundMask = 0;
            for (Object item : items) {
                if (item instanceof VariableField) {
                    int type = ((VariableField)item).getType();
                    foundMask |= 1 << type;    
                }
            }
            boolean isDate = (foundMask & DATE_MASK) != 0;
            boolean isTime = (foundMask & TIME_MASK) != 0;
            return isDate && isTime;
        }

        //        /**
        //         * Internal routine
        //         * @param value
        //         * @param result
        //         * @return list
        //         * @internal
        //         * @deprecated This API is ICU internal only.
        //         */
        //        public List getAutoPatterns(String value, List result) {
        //            if (result == null) result = new ArrayList();
        //            int fieldCount = 0;
        //            int minField = Integer.MAX_VALUE;
        //            int maxField = Integer.MIN_VALUE;
        //            for (Iterator it = items.iterator(); it.hasNext();) {
        //                Object item = it.next();
        //                if (item instanceof VariableField) {
        //                    try {
        //                        int type = ((VariableField)item).getType();
        //                        if (minField > type) minField = type;
        //                        if (maxField < type) maxField = type;
        //                        if (type == ZONE || type == DAYPERIOD || type == WEEKDAY) return result; // skip anything with zones                    
        //                        fieldCount++;
        //                    } catch (Exception e) {
        //                        return result; // if there are any funny fields, return
        //                    }
        //                }
        //            }
        //            if (fieldCount < 3) return result; // skip
        //            // trim from start
        //            // trim first field IF there are no letters around it
        //            // and it is either the min or the max field
        //            // first field is either 0 or 1
        //            for (int i = 0; i < items.size(); ++i) {
        //                Object item = items.get(i);
        //                if (item instanceof VariableField) {
        //                    int type = ((VariableField)item).getType();
        //                    if (type != minField && type != maxField) break;
        //                    
        //                    if (i > 0) {
        //                        Object previousItem = items.get(0);
        //                        if (alpha.containsSome(previousItem.toString())) break;
        //                    }
        //                    int start = i+1;
        //                    if (start < items.size()) {
        //                        Object nextItem = items.get(start);
        //                        if (nextItem instanceof String) {
        //                            if (alpha.containsSome(nextItem.toString())) break;
        //                            start++; // otherwise skip over string
        //                        }
        //                    }
        //                    result.add(toString(start, items.size()));
        //                    break;
        //                }
        //            }
        //            // now trim from end
        //            for (int i = items.size()-1; i >= 0; --i) {
        //                Object item = items.get(i);
        //                if (item instanceof VariableField) {
        //                    int type = ((VariableField)item).getType();
        //                    if (type != minField && type != maxField) break;
        //                    if (i < items.size() - 1) {
        //                        Object previousItem = items.get(items.size() - 1);
        //                        if (alpha.containsSome(previousItem.toString())) break;
        //                    }
        //                    int end = i-1;
        //                    if (end > 0) {
        //                        Object nextItem = items.get(end);
        //                        if (nextItem instanceof String) {
        //                            if (alpha.containsSome(nextItem.toString())) break;
        //                            end--; // otherwise skip over string
        //                        }
        //                    }
        //                    result.add(toString(0, end+1));
        //                    break;
        //                }
        //            }
        //
        //            return result;
        //        }

        //        private static UnicodeSet alpha = new UnicodeSet("[:alphabetic:]");

        //        private int getType(Object item) {
        //            String s = item.toString();
        //            int canonicalIndex = getCanonicalIndex(s);
        //            if (canonicalIndex < 0) {
        //                throw new IllegalArgumentException("Illegal field:\t"
        //                        + s);
        //            }
        //            int type = types[canonicalIndex][1];
        //            return type;
        //        }

        /**
         *  Each literal string is quoted as needed. That is, the ' quote marks will only be added if needed. The exact pattern of quoting is not guaranteed, thus " de la " could be quoted as " 'de la' " or as " 'de' 'la' ".
         * @param string The string to check.
         * @return string with quoted literals
         * @internal
         * @deprecated This API is ICU internal only.
         */
        public Object quoteLiteral(String string) {
            return tokenizer.quoteLiteral(string);
        }

    }

    /**
    * (moved from CLDR)
    * @internal
    */
    public boolean skeletonsAreSimilar(String id, String skeleton) {
        if (id.equals(skeleton)) {
            return true; // fast path
        }
        List<Object> parser1 = fp.set(id).getItems();
        List<Object> parser2 = fp.set(skeleton).getItems();
        if (parser1.size() != parser2.size()) {
            return false;
        }
        for (int i = 0; i < parser1.size(); ++i) {
            int index1 = getCanonicalIndex(parser1.get(i).toString(), false);
            int index2 = getCanonicalIndex(parser2.get(i).toString(), false);
            if (types[index1][1] != types[index2][1]) {
                return false;
            }
        }
        return true;
    }

    // ========= PRIVATES ============

    private static class PatternWithMatcher {
        public String pattern;
        public DateTimeMatcher matcherWithSkeleton;
        // Simple constructor
        public PatternWithMatcher(String pat, DateTimeMatcher matcher) {
            pattern = pat;
            matcherWithSkeleton = matcher;
        }
    }
    private static class PatternWithSkeletonFlag {
        public String pattern;
        public boolean skeletonWasSpecified;
        // Simple constructor
        public PatternWithSkeletonFlag(String pat, boolean skelSpecified) {
            pattern = pat;
            skeletonWasSpecified = skelSpecified;
        }
    }
    private TreeMap<DateTimeMatcher, PatternWithSkeletonFlag> skeleton2pattern = new TreeMap<DateTimeMatcher, PatternWithSkeletonFlag>(); // items are in priority order
    private TreeMap<String, PatternWithSkeletonFlag> basePattern_pattern = new TreeMap<String, PatternWithSkeletonFlag>(); // items are in priority order
    private String decimal = "?";
    private String dateTimeFormat = "{1} {0}";
    private String[] appendItemFormats = new String[TYPE_LIMIT];
    private String[] appendItemNames = new String[TYPE_LIMIT];
    {
        for (int i = 0; i < TYPE_LIMIT; ++i) {
            appendItemFormats[i] = "{0} \u251C{2}: {1}\u2524";
            appendItemNames[i] = "F" + i;
        }
    }
    private char defaultHourFormatChar = 'H';
    //private boolean chineseMonthHack = false;
    //private boolean isComplete = false;
    private boolean frozen = false;

    private transient DateTimeMatcher current = new DateTimeMatcher();
    private transient FormatParser fp = new FormatParser();
    private transient DistanceInfo _distanceInfo = new DistanceInfo();

    private static final int FRACTIONAL_MASK = 1<<FRACTIONAL_SECOND;
    private static final int SECOND_AND_FRACTIONAL_MASK = (1<<SECOND) | (1<<FRACTIONAL_SECOND);

    // Cache for DateTimePatternGenerator
    private static ICUCache<String, DateTimePatternGenerator> DTPNG_CACHE = new SimpleCache<String, DateTimePatternGenerator>();

    private void checkFrozen() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
    }

    /**
     * We only get called here if we failed to find an exact skeleton. We have broken it into date + time, and look for the pieces.
     * If we fail to find a complete skeleton, we compose in a loop until we have all the fields.
     */
    private String getBestAppending(DateTimeMatcher source, int missingFields, DistanceInfo distInfo, DateTimeMatcher skipMatcher, int options) {
        String resultPattern = null;
        if (missingFields != 0) {
            PatternWithMatcher resultPatternWithMatcher = getBestRaw(source, missingFields, distInfo, skipMatcher);
            resultPattern = adjustFieldTypes(resultPatternWithMatcher, source, false, options);

            while (distInfo.missingFieldMask != 0) { // precondition: EVERY single field must work!

                // special hack for SSS. If we are missing SSS, and we had ss but found it, replace the s field according to the 
                // number separator
                if ((distInfo.missingFieldMask & SECOND_AND_FRACTIONAL_MASK) == FRACTIONAL_MASK
                        && (missingFields & SECOND_AND_FRACTIONAL_MASK) == SECOND_AND_FRACTIONAL_MASK) {
                    resultPatternWithMatcher.pattern = resultPattern;
                    resultPattern = adjustFieldTypes(resultPatternWithMatcher, source, true, options);
                    distInfo.missingFieldMask &= ~FRACTIONAL_MASK; // remove bit
                    continue;
                }

                int startingMask = distInfo.missingFieldMask;
                PatternWithMatcher tempWithMatcher = getBestRaw(source, distInfo.missingFieldMask, distInfo, skipMatcher);
                String temp = adjustFieldTypes(tempWithMatcher, source, false, options);
                int foundMask = startingMask & ~distInfo.missingFieldMask;
                int topField = getTopBitNumber(foundMask);
                resultPattern = MessageFormat.format(getAppendFormat(topField), new Object[]{resultPattern, temp, getAppendName(topField)});
            }
        }
        return resultPattern;
    }

    private String getAppendName(int foundMask) {
        return "'" + appendItemNames[foundMask] + "'";
    }
    private String getAppendFormat(int foundMask) {
        return appendItemFormats[foundMask];
    }

    //    /**
    //     * @param current2
    //     * @return
    //     */
    //    private String adjustSeconds(DateTimeMatcher current2) {
    //        // TODO Auto-generated method stub
    //        return null;
    //    }

    /**
     * @param foundMask
     * @return
     */
    private int getTopBitNumber(int foundMask) {
        int i = 0;
        while (foundMask != 0) {
            foundMask >>>= 1;
    ++i;
        }
        return i-1;
    }

    /**
     * 
     */
    private void complete() {
        PatternInfo patternInfo = new PatternInfo();
        // make sure that every valid field occurs once, with a "default" length
        for (int i = 0; i < CANONICAL_ITEMS.length; ++i) {
            //char c = (char)types[i][0];
            addPattern(String.valueOf(CANONICAL_ITEMS[i]), false, patternInfo);
        }
        //isComplete = true;
    }
    {
        complete();
    }

    /**
     * 
     */
    private PatternWithMatcher getBestRaw(DateTimeMatcher source, int includeMask, DistanceInfo missingFields, DateTimeMatcher skipMatcher) {
        //      if (SHOW_DISTANCE) System.out.println("Searching for: " + source.pattern 
        //      + ", mask: " + showMask(includeMask));
        int bestDistance = Integer.MAX_VALUE;
        PatternWithMatcher bestPatternWithMatcher = new PatternWithMatcher("", null);
        DistanceInfo tempInfo = new DistanceInfo();
        for (DateTimeMatcher trial : skeleton2pattern.keySet()) {
            if (trial.equals(skipMatcher)) {
                continue;
            }
            int distance = source.getDistance(trial, includeMask, tempInfo);
            //          if (SHOW_DISTANCE) System.out.println("\tDistance: " + trial.pattern + ":\t" 
            //          + distance + ",\tmissing fields: " + tempInfo);
            if (distance < bestDistance) {
                bestDistance = distance;
                PatternWithSkeletonFlag patternWithSkelFlag = skeleton2pattern.get(trial);
                bestPatternWithMatcher.pattern = patternWithSkelFlag.pattern;
                // If the best raw match had a specified skeleton then return it too.
                // This can be passed through to adjustFieldTypes to help it do a better job.
                if (patternWithSkelFlag.skeletonWasSpecified) {
                    bestPatternWithMatcher.matcherWithSkeleton = trial;
                } else {
                    bestPatternWithMatcher.matcherWithSkeleton = null;
                }
                missingFields.setTo(tempInfo);
                if (distance == 0) {
                    break;
                }
            }
        }
        return bestPatternWithMatcher;
    }

    /**
     * @param fixFractionalSeconds TODO
     * 
     */
    private String adjustFieldTypes(PatternWithMatcher patternWithMatcher, DateTimeMatcher inputRequest, boolean fixFractionalSeconds, int options) {
        fp.set(patternWithMatcher.pattern);
        StringBuffer newPattern = new StringBuffer();
        for (Object item : fp.getItems()) {
            if (item instanceof String) {
                newPattern.append(fp.quoteLiteral((String)item));
            } else {
                final VariableField variableField = (VariableField) item;
                String field = variableField.toString();
                //                int canonicalIndex = getCanonicalIndex(field, true);
                //                if (canonicalIndex < 0) {
                //                    continue; // don't adjust
                //                }
                //                int type = types[canonicalIndex][1];
                int type = variableField.getType();

                if (fixFractionalSeconds && type == SECOND) {
                    String newField = inputRequest.original[FRACTIONAL_SECOND];
                    field = field + decimal + newField;
                } else if (inputRequest.type[type] != 0) {
                    // Here:
                    // - "reqField" is the field from the originally requested skeleton, with length
                    // "reqFieldLen".
                    // - "field" is the field from the found pattern.
                    //
                    // The adjusted field should consist of characters from the originally requested
                    // skeleton, except in the case of HOUR or MONTH, in which case it should consist of
                    // characters from the found pattern.
                    //
                    // The length of the adjusted field (adjFieldLen) should match that in the originally
                    // requested skeleton, except that in the following cases the length of the adjusted field
                    // should match that in the found pattern (i.e. the length of this pattern field should
                    // not be adjusted):
                    // 1. type is HOUR and the corresponding bit in options is not set (ticket #7180).
                    //    Note, we may want to implement a similar change for other numeric fields (MM, dd,
                    //    etc.) so the default behavior is to get locale preference for field length, but
                    //    options bits can be used to override this.
                    // 2. There is a specified skeleton for the found pattern and one of the following is true:
                    //    a) The length of the field in the skeleton (skelFieldLen) is equal to reqFieldLen.
                    //    b) The pattern field is numeric and the skeleton field is not, or vice versa.
                    //
                    // Old behavior was:
                    // normally we just replace the field. However HOUR is special; we only change the length
                    
                    String reqField = inputRequest.original[type];
                    int reqFieldLen = reqField.length();
                    int adjFieldLen = reqFieldLen;
                    DateTimeMatcher matcherWithSkeleton = patternWithMatcher.matcherWithSkeleton;
                    if (type == HOUR && (options & MATCH_HOUR_FIELD_LENGTH)==0) {
                        adjFieldLen = field.length();
                    } else if (matcherWithSkeleton != null) {
                        String skelField = matcherWithSkeleton.origStringForField(type);
                        int skelFieldLen = skelField.length();
                        boolean patFieldIsNumeric = variableField.isNumeric();
                        boolean skelFieldIsNumeric = matcherWithSkeleton.fieldIsNumeric(type);
                        if (skelFieldLen == reqFieldLen || (patFieldIsNumeric && !skelFieldIsNumeric) || (skelFieldIsNumeric && !patFieldIsNumeric)) {
                            // don't adjust the field length in the found pattern
                            adjFieldLen = field.length();
                        }
                    }
                    char c = (type != HOUR && type != MONTH)? reqField.charAt(0): field.charAt(0);
                    field = "";
                    for (int i = adjFieldLen; i > 0; --i) field += c;
                }
                newPattern.append(field);
            }
        }
        //if (SHOW_DISTANCE) System.out.println("\tRaw: " + pattern);
        return newPattern.toString();
    }

    //  public static String repeat(String s, int count) {
    //  StringBuffer result = new StringBuffer();
    //  for (int i = 0; i < count; ++i) {
    //  result.append(s);
    //  }
    //  return result.toString();
    //  }

    /**
     * internal routine
     * @param pattern The pattern that is passed.
     * @return field value
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public String getFields(String pattern) {
        fp.set(pattern);
        StringBuffer newPattern = new StringBuffer();
        for (Object item : fp.getItems()) {
            if (item instanceof String) {
                newPattern.append(fp.quoteLiteral((String)item));
            } else {
                newPattern.append("{" + getName(item.toString()) + "}");
            }
        }
        return newPattern.toString();
    }

    private static String showMask(int mask) {
        String result = "";
        for (int i = 0; i < TYPE_LIMIT; ++i) {
            if ((mask & (1<<i)) == 0) continue;
            if (result.length() != 0) result += " | ";
            result += FIELD_NAME[i] + " ";
        }
        return result;
    }

    static private String[] CLDR_FIELD_APPEND = {
        "Era", "Year", "Quarter", "Month", "Week", "*", "Day-Of-Week", 
        "Day", "*", "*", "*", 
        "Hour", "Minute", "Second", "*", "Timezone"
    };

    static private String[] CLDR_FIELD_NAME = {
        "era", "year", "*", "month", "week", "*", "weekday", 
        "day", "*", "*", "dayperiod", 
        "hour", "minute", "second", "*", "zone"
    };

    static private String[] FIELD_NAME = {
        "Era", "Year", "Quarter", "Month", "Week_in_Year", "Week_in_Month", "Weekday", 
        "Day", "Day_Of_Year", "Day_of_Week_in_Month", "Dayperiod", 
        "Hour", "Minute", "Second", "Fractional_Second", "Zone"
    };


    static private String[] CANONICAL_ITEMS = {
        "G", "y", "Q", "M", "w", "W", "e", 
        "d", "D", "F", 
        "H", "m", "s", "S", "v"
    };

    static private Set<String> CANONICAL_SET = new HashSet<String>(Arrays.asList(CANONICAL_ITEMS));
    private Set<String> cldrAvailableFormatKeys = new HashSet<String>(20);

    static final private int 
    DATE_MASK = (1<<DAYPERIOD) - 1,
    TIME_MASK = (1<<TYPE_LIMIT) - 1 - DATE_MASK;

    static final private int // numbers are chosen to express 'distance'
    DELTA = 0x10,
    NUMERIC = 0x100,
    NONE = 0,
    NARROW = -0x101,
    SHORT = -0x102,
    LONG = -0x103,
    EXTRA_FIELD =   0x10000,
    MISSING_FIELD = 0x1000;


    static private String getName(String s) {
        int i = getCanonicalIndex(s, true);
        String name = FIELD_NAME[types[i][1]];
        int subtype = types[i][2];
        boolean string = subtype < 0;
        if (string) subtype = -subtype;
        if (subtype < 0) name += ":S";
        else name += ":N";
        return name;
    }

    /**
     * Get the canonical index, or return -1 if illegal.
     * @param s
     * @param strict TODO
     * @return
     */
    static private int getCanonicalIndex(String s, boolean strict) {
        int len = s.length();
        if (len == 0) {
            return -1;
        }
        int ch = s.charAt(0);
        //      verify that all are the same character
        for (int i = 1; i < len; ++i) {
            if (s.charAt(i) != ch) {
                return -1; 
            }
        }
        int bestRow = -1;
        for (int i = 0; i < types.length; ++i) {
            int[] row = types[i];
            if (row[0] != ch) continue;
            bestRow = i;
            if (row[3] > len) continue;
            if (row[row.length-1] < len) continue;
            return i;
        }
        return strict ? -1 : bestRow;
    }

    static private int[][] types = {
        // the order here makes a difference only when searching for single field.
        // format is:
        // pattern character, main type, weight, min length, weight
        {'G', ERA, SHORT, 1, 3},
        {'G', ERA, LONG, 4},

        {'y', YEAR, NUMERIC, 1, 20},
        {'Y', YEAR, NUMERIC + DELTA, 1, 20},
        {'u', YEAR, NUMERIC + 2*DELTA, 1, 20},

        {'Q', QUARTER, NUMERIC, 1, 2},
        {'Q', QUARTER, SHORT, 3},
        {'Q', QUARTER, LONG, 4},

        {'q', QUARTER, NUMERIC + DELTA, 1, 2},
        {'q', QUARTER, SHORT + DELTA, 3},
        {'q', QUARTER, LONG + DELTA, 4},

        {'M', MONTH, NUMERIC, 1, 2},
        {'M', MONTH, SHORT, 3},
        {'M', MONTH, LONG, 4},
        {'M', MONTH, NARROW, 5},
        {'L', MONTH, NUMERIC + DELTA, 1, 2},
        {'L', MONTH, SHORT - DELTA, 3},
        {'L', MONTH, LONG - DELTA, 4},
        {'L', MONTH, NARROW - DELTA, 5},

        {'w', WEEK_OF_YEAR, NUMERIC, 1, 2},
        {'W', WEEK_OF_MONTH, NUMERIC + DELTA, 1},

        {'e', WEEKDAY, NUMERIC + DELTA, 1, 2},
        {'e', WEEKDAY, SHORT - DELTA, 3},
        {'e', WEEKDAY, LONG - DELTA, 4},
        {'e', WEEKDAY, NARROW - DELTA, 5},
        {'E', WEEKDAY, SHORT, 1, 3},
        {'E', WEEKDAY, LONG, 4},
        {'E', WEEKDAY, NARROW, 5},
        {'c', WEEKDAY, NUMERIC + 2*DELTA, 1, 2},
        {'c', WEEKDAY, SHORT - 2*DELTA, 3},
        {'c', WEEKDAY, LONG - 2*DELTA, 4},
        {'c', WEEKDAY, NARROW - 2*DELTA, 5},

        {'d', DAY, NUMERIC, 1, 2},
        {'D', DAY_OF_YEAR, NUMERIC + DELTA, 1, 3},
        {'F', DAY_OF_WEEK_IN_MONTH, NUMERIC + 2*DELTA, 1},
        {'g', DAY, NUMERIC + 3*DELTA, 1, 20}, // really internal use, so we don't care

        {'a', DAYPERIOD, SHORT, 1},

        {'H', HOUR, NUMERIC + 10*DELTA, 1, 2}, // 24 hour
        {'k', HOUR, NUMERIC + 11*DELTA, 1, 2},
        {'h', HOUR, NUMERIC, 1, 2}, // 12 hour
        {'K', HOUR, NUMERIC + DELTA, 1, 2},

        {'m', MINUTE, NUMERIC, 1, 2},

        {'s', SECOND, NUMERIC, 1, 2},
        {'S', FRACTIONAL_SECOND, NUMERIC + DELTA, 1, 1000},
        {'A', SECOND, NUMERIC + 2*DELTA, 1, 1000},

        {'v', ZONE, SHORT - 2*DELTA, 1},
        {'v', ZONE, LONG - 2*DELTA, 4},
        {'z', ZONE, SHORT, 1, 3},
        {'z', ZONE, LONG, 4},
        {'Z', ZONE, SHORT - DELTA, 1, 3},
        {'Z', ZONE, LONG - DELTA, 4},
        {'V', ZONE, SHORT - DELTA, 1, 3},
        {'V', ZONE, LONG - DELTA, 4},
    };

    private static class DateTimeMatcher implements Comparable<DateTimeMatcher> {
        //private String pattern = null;
        private int[] type = new int[TYPE_LIMIT];
        private String[] original = new String[TYPE_LIMIT];
        private String[] baseOriginal = new String[TYPE_LIMIT];

        // just for testing; fix to make multi-threaded later
        // private static FormatParser fp = new FormatParser();

        public String origStringForField(int field) {
            return original[field];
        }

        public boolean fieldIsNumeric(int field) {
            return type[field] > 0;
        }

        public String toString() {
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < TYPE_LIMIT; ++i) {
                if (original[i].length() != 0) result.append(original[i]);
            }
            return result.toString();
        }

        String getBasePattern() {
            StringBuffer result = new StringBuffer();
            for (int i = 0; i < TYPE_LIMIT; ++i) {
                if (baseOriginal[i].length() != 0) result.append(baseOriginal[i]);
            }
            return result.toString();
        }

        DateTimeMatcher set(String pattern, FormatParser fp) {
            for (int i = 0; i < TYPE_LIMIT; ++i) {
                type[i] = NONE;
                original[i] = "";
                baseOriginal[i] = "";
            }
            fp.set(pattern);
            for (Object obj : fp.getItems()) {
                if (!(obj instanceof VariableField)) {
                    continue;
                }
                VariableField item = (VariableField)obj;
                String field = item.toString();
                if (field.charAt(0) == 'a') continue; // skip day period, special case
                int canonicalIndex = item.getCanonicalIndex();
                //                if (canonicalIndex < 0) {
                //                    throw new IllegalArgumentException("Illegal field:\t"
                //                            + field + "\t in " + pattern);
                //                }
                int[] row = types[canonicalIndex];
                int typeValue = row[1];
                if (original[typeValue].length() != 0) {
                    throw new IllegalArgumentException("Conflicting fields:\t"
                            + original[typeValue] + ", " + field + "\t in " + pattern);
                }
                original[typeValue] = field;
                char repeatChar = (char)row[0];
                int repeatCount = row[3];
                if (repeatCount > 3) repeatCount = 3; // hack to discard differences
                if ("GEzvQ".indexOf(repeatChar) >= 0) repeatCount = 1;
                baseOriginal[typeValue] = Utility.repeat(String.valueOf(repeatChar),repeatCount);
                int subTypeValue = row[2];
                if (subTypeValue > 0) subTypeValue += field.length();
                type[typeValue] = subTypeValue;
            }
            return this;
        }

        /**
         * 
         */
        int getFieldMask() {
            int result = 0;
            for (int i = 0; i < type.length; ++i) {
                if (type[i] != 0) result |= (1<<i);
            }
            return result;
        }

        /**
         * 
         */
        @SuppressWarnings("unused")
        void extractFrom(DateTimeMatcher source, int fieldMask) {
            for (int i = 0; i < type.length; ++i) {
                if ((fieldMask & (1<<i)) != 0) {
                    type[i] = source.type[i];
                    original[i] = source.original[i];
                } else {
                    type[i] = NONE;
                    original[i] = "";
                }
            }
        }

        int getDistance(DateTimeMatcher other, int includeMask, DistanceInfo distanceInfo) {
            int result = 0;
            distanceInfo.clear();
            for (int i = 0; i < type.length; ++i) {
                int myType = (includeMask & (1<<i)) == 0 ? 0 : type[i];
                int otherType = other.type[i];
                if (myType == otherType) continue; // identical (maybe both zero) add 0
                if (myType == 0) { // and other is not
                    result += EXTRA_FIELD;
                    distanceInfo.addExtra(i);
                } else if (otherType == 0) { // and mine is not
                    result += MISSING_FIELD;
                    distanceInfo.addMissing(i);
                } else {
                    result += Math.abs(myType - otherType); // square of mismatch
                }
            }
            return result;
        }

        public int compareTo(DateTimeMatcher that) {
            for (int i = 0; i < original.length; ++i) {
                int comp = original[i].compareTo(that.original[i]);
                if (comp != 0) return -comp;
            }
            return 0;
        }

        public boolean equals(Object other) {
            if (other == null) return false;
            DateTimeMatcher that = (DateTimeMatcher) other;
            for (int i = 0; i < original.length; ++i) {
                if (!original[i].equals(that.original[i])) return false;
            }
            return true;
        }       
        public int hashCode() {
            int result = 0;
            for (int i = 0; i < original.length; ++i) {
                result ^= original[i].hashCode();
            }
            return result;
        }
    }

    private static class DistanceInfo {
        int missingFieldMask;
        int extraFieldMask;
        void clear() {
            missingFieldMask = extraFieldMask = 0;
        }
        /**
         * 
         */
        void setTo(DistanceInfo other) {
            missingFieldMask = other.missingFieldMask;
            extraFieldMask = other.extraFieldMask;
        }
        void addMissing(int field) {
            missingFieldMask |= (1<<field);
        }
        void addExtra(int field) {
            extraFieldMask |= (1<<field);
        }
        public String toString() {
            return "missingFieldMask: " + DateTimePatternGenerator.showMask(missingFieldMask)
            + ", extraFieldMask: " + DateTimePatternGenerator.showMask(extraFieldMask);
        }
    }
}
//eof

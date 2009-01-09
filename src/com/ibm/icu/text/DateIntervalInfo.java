/*
*   Copyright (C) 2008, International Business Machines
*   Corporation and others.  All Rights Reserved.
*/

package com.ibm.icu.text;

import java.io.Serializable;

import java.util.MissingResourceException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ICUCache;
import com.ibm.icu.impl.SimpleCache;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.UResourceBundle;


/**
 * DateIntervalInfo is a public class for encapsulating localizable
 * date time interval patterns. It is used by DateIntervalFormat.
 *
 * <P>
 * For most users, ordinary use of DateIntervalFormat does not need to create
 * DateIntervalInfo object directly.
 * DateIntervalFormat will take care of it when creating a date interval
 * formatter when user pass in skeleton and locale.
 *
 * <P>
 * For power users, who want to create their own date interval patterns,
 * or want to re-set date interval patterns, they could do so by
 * directly creating DateIntervalInfo and manupulating it.
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
 * year, month, date, day-of-week, am-pm, hour, hour-of-day, and minute.
 * Those calendar fields can be defined in the following order:
 * year >  month > date > am-pm > hour >  minute 
 *  
 * The largest different calendar fields between 2 calendars is the
 * first different calendar field in above order.
 *
 * For example: the largest different calendar fields between "Jan 10, 2007" 
 * and "Feb 20, 2008" is year.
 *   
 * <P>
 * There is a set of pre-defined static skeleton strings.
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
 *
 * <P>
 * There are 2 dates in interval pattern. For most locales, the first date
 * in an interval pattern is the earlier date. There might be a locale in which
 * the first date in an interval pattern is the later date.
 * We use fallback format for the default order for the locale.
 * For example, if the fallback format is "{0} - {1}", it means
 * the first date in the interval pattern for this locale is earlier date.
 * If the fallback format is "{1} - {0}", it means the first date is the 
 * later date.
 * For a particular interval pattern, the default order can be overriden
 * by prefixing "latestFirst:" or "earliestFirst:" to the interval pattern.
 * For example, if the fallback format is "{0}-{1}",
 * but for skeleton "yMMMd", the interval pattern when day is different is 
 * "latestFirst:d-d MMM yy", it means by default, the first date in interval
 * pattern is the earlier date. But for skeleton "yMMMd", when day is different,
 * the first date in "d-d MMM yy" is the later date.
 * 
 * <P>
 * The recommended way to create a DateIntervalFormat object is to pass in 
 * the locale. 
 * By using a Locale parameter, the DateIntervalFormat object is 
 * initialized with the pre-defined interval patterns for a given or 
 * default locale.
 * <P>
 * Users can also create DateIntervalFormat object 
 * by supplying their own interval patterns.
 * It provides flexibility for power usage.
 *
 * <P>
 * After a DateIntervalInfo object is created, clients may modify
 * the interval patterns using setIntervalPattern function as so desired.
 * Currently, users can only set interval patterns when the following 
 * calendar fields are different: ERA, YEAR, MONTH, DATE,  DAY_OF_MONTH, 
 * DAY_OF_WEEK, AM_PM,  HOUR, HOUR_OF_DAY, and MINUTE.
 * Interval patterns when other calendar fields are different is not supported.
 * <P>
 * DateIntervalInfo objects are cloneable. 
 * When clients obtain a DateIntervalInfo object, 
 * they can feel free to modify it as necessary.
 * <P>
 * DateIntervalInfo are not expected to be subclassed. 
 * Data for a calendar is loaded out of resource bundles. 
 * To ICU 4.0, date interval patterns are only supported in Gregorian calendar.
 * 
 * @draft ICU 4.0
 * @provisional This API might change or be removed in a future release.
**/

public class DateIntervalInfo implements Cloneable, Freezable, Serializable {
    /* Save the interval pattern information.
     * Interval pattern consists of 2 single date patterns and the separator.
     * For example, interval pattern "MMM d - MMM d, yyyy" consists
     * a single date pattern "MMM d", another single date pattern "MMM d, yyyy",
     * and a separator "-".
     * Also, the first date appears in an interval pattern could be 
     * the earlier date or the later date.
     * And such information is saved in the interval pattern as well.
     */
    static final int currentSerialVersion = 1;

    /**
     * PatternInfo class saves the first and second part of interval pattern,
     * and whether the interval pattern is earlier date first.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public static final class PatternInfo implements Cloneable, Serializable {
        static final int currentSerialVersion = 1;
        private static final long serialVersionUID = 1;
        private final String fIntervalPatternFirstPart;
        private final String fIntervalPatternSecondPart;
        /*
         * Whether the first date in interval pattern is later date or not.
         * Fallback format set the default ordering.
         * And for a particular interval pattern, the order can be 
         * overriden by prefixing the interval pattern with "latestFirst:" or 
         * "earliestFirst:"
         * For example, given 2 date, Jan 10, 2007 to Feb 10, 2007.
         * if the fallback format is "{0} - {1}", 
         * and the pattern is "d MMM - d MMM yyyy", the interval format is
         * "10 Jan - 10 Feb, 2007".
         * If the pattern is "latestFirst:d MMM - d MMM yyyy", 
         * the interval format is "10 Feb - 10 Jan, 2007"
         */
        private final boolean fFirstDateInPtnIsLaterDate;

        /**
         * constructor
         * @draft ICU 4.0
         * @provisional This API might change or be removed in a future release.
         */
        public PatternInfo(String firstPart, String secondPart,
                           boolean firstDateInPtnIsLaterDate) {
            fIntervalPatternFirstPart = firstPart;
            fIntervalPatternSecondPart = secondPart;
            fFirstDateInPtnIsLaterDate = firstDateInPtnIsLaterDate;
        }

        /**
         * accessor
         * @draft ICU 4.0
         * @provisional This API might change or be removed in a future release.
         */
        public String getFirstPart() {
            return fIntervalPatternFirstPart;
        }

        /**
         * accessor
         * @draft ICU 4.0
         * @provisional This API might change or be removed in a future release.
         */
        public String getSecondPart() {
            return fIntervalPatternSecondPart;
        }

        /**
         * accessor
         * @draft ICU 4.0
         * @provisional This API might change or be removed in a future release.
         */
        public boolean firstDateInPtnIsLaterDate() {
            return fFirstDateInPtnIsLaterDate;
        }

        /**
         * Override equals
         * @draft ICU 4.0
         * @provisional This API might change or be removed in a future release.
         */
        public boolean equals(Object a) {
            if ( a instanceof PatternInfo ) {
                PatternInfo patternInfo = (PatternInfo)a;
                return Utility.objectEquals(fIntervalPatternFirstPart, patternInfo.fIntervalPatternFirstPart) && 
                       Utility.objectEquals(fIntervalPatternSecondPart, fIntervalPatternSecondPart) &&
                       fFirstDateInPtnIsLaterDate == patternInfo.fFirstDateInPtnIsLaterDate;
            }
            return false;
        }

        /**
         * Override hashcode
         * @draft ICU 4.0
         * @provisional This API might change or be removed in a future release.
         */
        public int hashCode() {
            int hash = fIntervalPatternFirstPart != null ? fIntervalPatternFirstPart.hashCode() : 0;
            if (fIntervalPatternSecondPart != null) {
                hash ^= fIntervalPatternSecondPart.hashCode();
            }
            if (fFirstDateInPtnIsLaterDate) {
                hash ^= -1;
            }
            return hash;
        }
    }

    // Following is package protected since 
    // it is shared with DateIntervalFormat.
    static final String[] CALENDAR_FIELD_TO_PATTERN_LETTER = 
    {
        "G", "y", "M",
        "w", "W", "d", 
        "D", "E", "F",
        "a", "h", "H",
        "m",
    };


    private static final long serialVersionUID = 1;
    private static final int MINIMUM_SUPPORTED_CALENDAR_FIELD = 
                                                          Calendar.MINUTE;
    //private static boolean DEBUG = true;

    private static String FALLBACK_STRING = "fallback";
    private static String LATEST_FIRST_PREFIX = "latestFirst:";
    private static String EARLIEST_FIRST_PREFIX = "earliestFirst:";

    // DateIntervalInfo cache
    private final static ICUCache DIICACHE = new SimpleCache();

    // default interval pattern on the skeleton, {0} - {1}
    private String fFallbackIntervalPattern;
    // default order
    private boolean fFirstDateInPtnIsLaterDate = false;

    // HashMap<String, HashMap<String, PatternInfo> >
    // HashMap( skeleton, HashMap(largest_different_field, pattern) )
    private HashMap fIntervalPatterns = null;

    private transient boolean frozen = false;


    /**
     * Create empty instance.
     * It does not initialize any interval patterns except
     * that it initialize default fall-back pattern as "{0} - {1}",
     * which can be reset by setFallbackIntervalPattern().
     *
     * It should be followed by setFallbackIntervalPattern() and 
     * setIntervalPattern(), 
     * and is recommended to be used only for power users who
     * wants to create their own interval patterns and use them to create
     * date interval formatter.
     * @internal ICU 4.0
     * @deprecated This API is ICU internal only.
     */
    public DateIntervalInfo() 
    {
        fIntervalPatterns = new HashMap();
        fFallbackIntervalPattern = "{0} \u2013 {1}";
    }


    /** 
     * Construct DateIntervalInfo for the given locale,
     * @param locale  the interval patterns are loaded from the Gregorian 
     *                calendar data in this locale.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public DateIntervalInfo(ULocale locale) 
    {
        initializeData(locale);
    }


    /*
     * Initialize the DateIntervalInfo from locale
     * @param locale   the given locale.
     */
    private void initializeData(ULocale locale)
    {
        String key = locale.toString();
        DateIntervalInfo dii = (DateIntervalInfo) DIICACHE.get(key);
        if ( dii == null ) {
            // initialize data from scratch
            setup(locale);
            // TODO: should put a clone in cache?
            // or put itself in cache?
            // DIICACHE.put(key, this);
            dii = (DateIntervalInfo)this.clone();
            DIICACHE.put(key, dii);
        } else {
            initializeData(dii);
        }
    }

 

    /*
     * Initialize DateIntervalInfo from another instance
     * @param dii  an DateIntervalInfo instance
     */
    private void initializeData(DateIntervalInfo dii) {
        fFallbackIntervalPattern = dii.fFallbackIntervalPattern;
        fFirstDateInPtnIsLaterDate = dii.fFirstDateInPtnIsLaterDate;
        fIntervalPatterns = dii.fIntervalPatterns;
    }


    /*
     * Initialize DateIntervalInfo from calendar data
     * @param calData  calendar data
     */
    private void setup(ULocale locale) {
        int DEFAULT_HASH_SIZE = 19;
        fIntervalPatterns = new HashMap(DEFAULT_HASH_SIZE);
        // initialize to guard if there is no interval date format defined in 
        // resource files
        fFallbackIntervalPattern = "{0} \u2013 {1}";
        HashSet skeletonSet = new HashSet();
        try {
            // loop through all locales to get all available skeletons'
            // interval format
            ULocale parentLocale = locale;
            // TODO: how to check for root
            //while ( !parentLocale.equals(ULocale.ROOT) ) {
            while (parentLocale != null && !parentLocale.equals(ULocale.ROOT)) {
                String name = parentLocale.getName();
                if ( name.length() == 0 ) {
                    break;
                }

                ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.
                  getBundleInstance(ICUResourceBundle.ICU_BASE_NAME,locale);
                rb = rb.getWithFallback("calendar");
                ICUResourceBundle gregorianBundle = rb.getWithFallback(
                                                              "gregorian");
                ICUResourceBundle itvDtPtnResource =gregorianBundle.
                                      getWithFallback("intervalFormats");
                // look for fallback first, since it establishes the default order
                String fallback = itvDtPtnResource.getStringWithFallback(
                                                          FALLBACK_STRING);
                setFallbackIntervalPattern(fallback);
                int size = itvDtPtnResource.getSize();
                for ( int index = 0; index < size; ++index ) {
                    String skeleton = itvDtPtnResource.get(index).getKey();
                    if ( skeletonSet.contains(skeleton) ) {
                        continue;
                    }
                    skeletonSet.add(skeleton);
                    if ( skeleton.compareTo(FALLBACK_STRING) == 0 ) {
                        continue;
                    }
                    ICUResourceBundle intervalPatterns =
                        itvDtPtnResource.getWithFallback(skeleton);
                    int ptnNum = intervalPatterns.getSize();
                    for ( int ptnIndex = 0; ptnIndex < ptnNum; ++ptnIndex) {
                        String key = intervalPatterns.get(ptnIndex).getKey();
                        String pattern = intervalPatterns.get(ptnIndex).getString();
    
                        int calendarField = Calendar.MILLISECONDS_IN_DAY + 1;
                        if ( key.compareTo(CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.YEAR]) == 0 ) {
                            calendarField = Calendar.YEAR;    
                        } else if ( key.compareTo(CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.MONTH]) == 0 ) {
                            calendarField = Calendar.MONTH;
                        } else if ( key.compareTo(CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.DATE]) == 0 ) {
                            calendarField = Calendar.DATE;
                        } else if ( key.compareTo(CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.AM_PM]) == 0 ) {
                            calendarField = Calendar.AM_PM;    
                        } else if ( key.compareTo(CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.HOUR]) == 0 ) {
                            calendarField = Calendar.HOUR;    
                        } else if ( key.compareTo(CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.MINUTE]) == 0 ) {
                            calendarField = Calendar.MINUTE;    
                        }
             
                        if ( calendarField != Calendar.MILLISECONDS_IN_DAY + 1 ) {
                            setIntervalPatternInternally(skeleton, key, pattern);
                        }
                    }
                }
                parentLocale = parentLocale.getFallback();
            }
        } catch ( MissingResourceException e) {
            // ok, will fallback to {data0} - {date1}
        }
    }


    /*
     * Split interval patterns into 2 part.
     * @param intervalPattern  interval pattern
     * @return the index in interval pattern which split the pattern into 2 part
     */
    private static int splitPatternInto2Part(String intervalPattern) {
        boolean inQuote = false;
        char prevCh = 0;
        int count = 0;
    
        /* repeatedPattern used to record whether a pattern has already seen.
           It is a pattern applies to first calendar if it is first time seen,
           otherwise, it is a pattern applies to the second calendar
         */
        int[] patternRepeated = new int[58];

        int PATTERN_CHAR_BASE = 0x41;
        
        /* loop through the pattern string character by character looking for
         * the first repeated pattern letter, which breaks the interval pattern
         * into 2 parts. 
         */
        int i;
        boolean foundRepetition = false;
        for (i = 0; i < intervalPattern.length(); ++i) {
            char ch = intervalPattern.charAt(i);
            
            if (ch != prevCh && count > 0) {
                // check the repeativeness of pattern letter
                int repeated = patternRepeated[(int)(prevCh - PATTERN_CHAR_BASE)];
                if ( repeated == 0 ) {
                    patternRepeated[prevCh - PATTERN_CHAR_BASE] = 1;
                } else {
                    foundRepetition = true;
                    break;
                }
                count = 0;
            }
            if (ch == '\'') {
                // Consecutive single quotes are a single quote literal,
                // either outside of quotes or between quotes
                if ((i+1) < intervalPattern.length() && 
                    intervalPattern.charAt(i+1) == '\'') {
                    ++i;
                } else {
                    inQuote = ! inQuote;
                }
            } 
            else if (!inQuote && ((ch >= 0x0061 /*'a'*/ && ch <= 0x007A /*'z'*/)
                        || (ch >= 0x0041 /*'A'*/ && ch <= 0x005A /*'Z'*/))) {
                // ch is a date-time pattern character 
                prevCh = ch;
                ++count;
            }
        }
        // check last pattern char, distinguish
        // "dd MM" ( no repetition ), 
        // "d-d"(last char repeated ), and 
        // "d-d MM" ( repetition found )
        if ( count > 0 && foundRepetition == false ) {
            if ( patternRepeated[(int)(prevCh - PATTERN_CHAR_BASE)] == 0 ) {
                count = 0;
            }
        }
        return (i - count);
    }


    /** 
     * Provides a way for client to build interval patterns.
     * User could construct DateIntervalInfo by providing 
     * a list of skeletons and their patterns.
     * <P>
     * For example:
     * <pre>
     * DateIntervalInfo dIntervalInfo = new DateIntervalInfo();
     * dIntervalInfo.setIntervalPattern("yMd", Calendar.YEAR, "'from' yyyy-M-d 'to' yyyy-M-d"); 
     * dIntervalInfo.setIntervalPattern("yMMMd", Calendar.MONTH, "'from' yyyy MMM d 'to' MMM d");
     * dIntervalInfo.setIntervalPattern("yMMMd", Calendar.DAY, "yyyy MMM d-d");
     * dIntervalInfo.setFallbackIntervalPattern("{0} ~ {1}");
     * </pre>
     *
     * Restriction: 
     * Currently, users can only set interval patterns when the following 
     * calendar fields are different: ERA, YEAR, MONTH, DATE,  DAY_OF_MONTH, 
     * DAY_OF_WEEK, AM_PM,  HOUR, HOUR_OF_DAY, and MINUTE.
     * Interval patterns when other calendar fields are different are 
     * not supported.
     *
     * @param skeleton         the skeleton on which interval pattern based
     * @param lrgDiffCalUnit   the largest different calendar unit.
     * @param intervalPattern  the interval pattern on the largest different
     *                         calendar unit.
     *                         For example, if lrgDiffCalUnit is 
     *                         "year", the interval pattern for en_US when year
     *                         is different could be "'from' yyyy 'to' yyyy".
     * @throws IllegalArgumentException  if setting interval pattern on 
     *                            a calendar field that is smaller
     *                            than the MINIMUM_SUPPORTED_CALENDAR_FIELD 
     * @throws UnsupportedOperationException  if the object is frozen
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public void setIntervalPattern(String skeleton, 
                                   int lrgDiffCalUnit, 
                                   String intervalPattern)
    {
        if ( frozen ) {
            throw new UnsupportedOperationException("no modification is allowed after DII is frozen");
        }
        if ( lrgDiffCalUnit > MINIMUM_SUPPORTED_CALENDAR_FIELD ) {
            throw new IllegalArgumentException("calendar field is larger than MINIMUM_SUPPORTED_CALENDAR_FIELD");
        }

        PatternInfo ptnInfo = setIntervalPatternInternally(skeleton,
                          CALENDAR_FIELD_TO_PATTERN_LETTER[lrgDiffCalUnit], 
                          intervalPattern);
        if ( lrgDiffCalUnit == Calendar.HOUR_OF_DAY ) {
            setIntervalPattern(skeleton, 
                               CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.AM_PM],
                               ptnInfo);
            setIntervalPattern(skeleton, 
                               CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.HOUR],
                               ptnInfo);
        } else if ( lrgDiffCalUnit == Calendar.DAY_OF_MONTH ||
                    lrgDiffCalUnit == Calendar.DAY_OF_WEEK ) {
            setIntervalPattern(skeleton, 
                               CALENDAR_FIELD_TO_PATTERN_LETTER[Calendar.DATE],
                               ptnInfo);
        }
    }


    /* Set Interval pattern.
     *
     * It generates the interval pattern info, 
     * afer which, not only sets the interval pattern info into the hash map,
     * but also returns the interval pattern info to the caller
     * so that caller can re-use it.
     *
     * @param skeleton         skeleton on which the interval pattern based
     * @param lrgDiffCalUnit   the largest different calendar unit.
     * @param intervalPattern  the interval pattern on the largest different
     *                         calendar unit.
     * @return the interval pattern pattern information
     */
    private PatternInfo setIntervalPatternInternally(String skeleton,
                                                String lrgDiffCalUnit,
                                                String intervalPattern) {
        HashMap patternsOfOneSkeleton = (HashMap)fIntervalPatterns.get(skeleton);
        boolean emptyHash = false;
        if ( patternsOfOneSkeleton == null ) {
            patternsOfOneSkeleton = new HashMap();
            emptyHash = true;
        }
        boolean order = fFirstDateInPtnIsLaterDate;
        // check for "latestFirst:" or "earliestFirst:" prefix
        if ( intervalPattern.startsWith(LATEST_FIRST_PREFIX) ) {
            order = true;
            int prefixLength = LATEST_FIRST_PREFIX.length();
            intervalPattern = intervalPattern.substring(prefixLength, intervalPattern.length());
        } else if ( intervalPattern.startsWith(EARLIEST_FIRST_PREFIX) ) {
            order = false;
            int earliestFirstLength = EARLIEST_FIRST_PREFIX.length();
            intervalPattern = intervalPattern.substring(earliestFirstLength, intervalPattern.length());
        }
        PatternInfo itvPtnInfo = genPatternInfo(intervalPattern, order);
        
        patternsOfOneSkeleton.put(lrgDiffCalUnit, itvPtnInfo);
        if ( emptyHash == true ) {
            fIntervalPatterns.put(skeleton, patternsOfOneSkeleton);
        }

        return itvPtnInfo;
    }


    /* Set Interval pattern.
     *
     * @param skeleton         skeleton on which the interval pattern based
     * @param lrgDiffCalUnit   the largest different calendar unit.
     * @param ptnInfo          interval pattern infomration 
     */
    private void setIntervalPattern(String skeleton,
                                    String lrgDiffCalUnit,
                                    PatternInfo ptnInfo) {
        HashMap patternsOfOneSkeleton = (HashMap)fIntervalPatterns.get(skeleton);
        patternsOfOneSkeleton.put(lrgDiffCalUnit, ptnInfo);
    }


    /**
     * Break interval patterns as 2 part and save them into pattern info.
     * @param intervalPattern  interval pattern
     * @param laterDateFirst   whether the first date in intervalPattern
     *                         is earlier date or later date
     * @return                 pattern info object
     * @internal ICU 4.0
     * @deprecated This API is ICU internal only.
     */
    static PatternInfo genPatternInfo(String intervalPattern, 
                                      boolean laterDateFirst) {
        int splitPoint = splitPatternInto2Part(intervalPattern);
        
        String firstPart = intervalPattern.substring(0, splitPoint);
        String secondPart = null;
        if ( splitPoint < intervalPattern.length() ) {
            secondPart = intervalPattern.substring(splitPoint, intervalPattern.length());
        }

        return new PatternInfo(firstPart, secondPart, laterDateFirst);
    }


    /**
     * Get the interval pattern given the largest different calendar field.
     * @param skeleton   the skeleton
     * @param field      the largest different calendar field
     * @return interval pattern  return null if interval pattern is not found.
     * @throws IllegalArgumentException  if getting interval pattern on 
     *                            a calendar field that is smaller
     *                            than the MINIMUM_SUPPORTED_CALENDAR_FIELD 
     * @draft ICU 4.0 
     * @provisional This API might change or be removed in a future release.
     */
    public PatternInfo getIntervalPattern(String skeleton, int field) 
    {
        if ( field > MINIMUM_SUPPORTED_CALENDAR_FIELD ) {
            throw new IllegalArgumentException("no support for field less than MINUTE");
        }
        HashMap patternsOfOneSkeleton = (HashMap) fIntervalPatterns.get(skeleton);
        if ( patternsOfOneSkeleton != null ) {
            PatternInfo intervalPattern = (PatternInfo) patternsOfOneSkeleton.
                get(CALENDAR_FIELD_TO_PATTERN_LETTER[field]);
            if ( intervalPattern != null ) {
                return intervalPattern;
            }
        }
        return null;
    }



    /**
     * Get the fallback interval pattern.
     * @return fallback interval pattern
     * @draft ICU 4.0 
     * @provisional This API might change or be removed in a future release.
     */
    public String getFallbackIntervalPattern()
    {
        return fFallbackIntervalPattern;
    }


    /**
     * Re-set the fallback interval pattern.
     *
     * In construction, default fallback pattern is set as "{0} - {1}".
     * And constructor taking locale as parameter will set the
     * fallback pattern as what defined in the locale resource file.
     *
     * This method provides a way for user to replace the fallback pattern.
     *
     * @param fallbackPattern                 fall-back interval pattern.
     * @throws UnsupportedOperationException  if the object is frozen
     * @throws IllegalArgumentException       if there is no pattern {0} or 
     *                                        pattern {1} in fallbakckPattern
     *                   
     * @draft ICU 4.0 
     * @provisional This API might change or be removed in a future release.
     */
    public void setFallbackIntervalPattern(String fallbackPattern)
    {
        if ( frozen ) {
            throw new UnsupportedOperationException("no modification is allowed after DII is frozen");
        }
        int firstPatternIndex = fallbackPattern.indexOf("{0}");
        int secondPatternIndex = fallbackPattern.indexOf("{1}");
        if ( firstPatternIndex == -1 || secondPatternIndex == -1 ) {
            throw new IllegalArgumentException("no pattern {0} or pattern {1} in fallbackPattern");
        }
        if ( firstPatternIndex > secondPatternIndex ) {
            fFirstDateInPtnIsLaterDate = true;
        }
        fFallbackIntervalPattern = fallbackPattern;
    }


    /**
     * Get default order -- whether the first date in pattern is later date
     *                      or not.
     *
     * return default date ordering in interval pattern. TRUE if the first date 
     *        in pattern is later date, FALSE otherwise.
     * @draft ICU 4.0 
     * @provisional This API might change or be removed in a future release.
     */
    public boolean getDefaultOrder()
    {
        return fFirstDateInPtnIsLaterDate;
    }


    /**
     * Boilerplate. Clone this object.
     * @return     a copy of the object
     * @draft ICU4.0
     * @provisional This API might change or be removed in a future release.
     */
    public Object clone() 
    {
        if ( frozen ) {
            return this;
        }
        return cloneUnfrozenDII();
    }


    /*
     * Clone an unfrozen DateIntervalInfo object.
     * @return     a copy of the object
     */
    private Object cloneUnfrozenDII() //throws IllegalStateException
    {
        try {
            DateIntervalInfo other = (DateIntervalInfo) super.clone();
            other.fFallbackIntervalPattern=fFallbackIntervalPattern;
            other.fFirstDateInPtnIsLaterDate = fFirstDateInPtnIsLaterDate;
            other.fIntervalPatterns = new HashMap();
            Iterator iter = fIntervalPatterns.keySet().iterator();
            while ( iter.hasNext() ) {
                String skeleton = (String) iter.next();
                HashMap patternsOfOneSkeleton = (HashMap)fIntervalPatterns.get(skeleton);
                HashMap oneSetPtn = new HashMap();
                Iterator patternIter = patternsOfOneSkeleton.keySet().iterator();
                while ( patternIter.hasNext() ) {
                    String calField = (String) patternIter.next();
                    PatternInfo value = (PatternInfo) patternsOfOneSkeleton.get(calField);
                    oneSetPtn.put(calField, value);
                }
                other.fIntervalPatterns.put(skeleton, oneSetPtn);    
            }
            other.frozen = false;
            return other;
        } catch ( CloneNotSupportedException e ) {
            throw new  IllegalStateException("clone is not supported");
        }
    }

    
    /**
     * Boilerplate for Freezable
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    /**
     * Boilerplate for Freezable
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public Object freeze() {
        frozen = true;
        return this;
    }
    
    /**
     * Boilerplate for Freezable
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public Object cloneAsThawed() {
        DateIntervalInfo result = (DateIntervalInfo) (this.cloneUnfrozenDII());
        return result;
    }


    /**
     * Parse skeleton, save each field's width.
     * It is used for looking for best match skeleton,
     * and adjust pattern field width.
     * @param skeleton            skeleton to be parsed
     * @param skeletonFieldWidth  parsed skeleton field width
     * @internal ICU 4.0
     * @deprecated This API is ICU internal only.
     */
    static void parseSkeleton(String skeleton, int[] skeletonFieldWidth) {
        int PATTERN_CHAR_BASE = 0x41;
        for ( int i = 0; i < skeleton.length(); ++i ) {
            ++skeletonFieldWidth[(int)(skeleton.charAt(i) - PATTERN_CHAR_BASE)];
        }
    }



    /*
     * Check whether one field width is numeric while the other is string.
     *
     * TODO (xji): make it general
     *
     * @param fieldWidth          one field width
     * @param anotherFieldWidth   another field width
     * @param patternLetter       pattern letter char
     * @return true if one field width is numeric and the other is string,
     *         false otherwise.
     */
    private static boolean stringNumeric(int fieldWidth,
                                         int anotherFieldWidth,
                                         char patternLetter) {
        if ( patternLetter == 'M' ) {
            if ( fieldWidth <= 2 && anotherFieldWidth > 2 ||
                 fieldWidth > 2 && anotherFieldWidth <= 2 ) {
                return true;
            }
        }        
        return false;
    }


    /*
     * given an input skeleton, get the best match skeleton 
     * which has pre-defined interval pattern in resource file.
     *
     * TODO (xji): set field weight or
     *             isolate the funtionality in DateTimePatternGenerator
     * @param  inputSkeleton        input skeleton
     * @return 0, if there is exact match for input skeleton
     *         1, if there is only field width difference between 
     *            the best match and the input skeleton
     *         2, the only field difference is 'v' and 'z'
     *        -1, if there is calendar field difference between
     *            the best match and the input skeleton
     */
    DateIntervalFormat.BestMatchInfo getBestSkeleton(String inputSkeleton) {
        String bestSkeleton = inputSkeleton;
        int[] inputSkeletonFieldWidth = new int[58];
        int[] skeletonFieldWidth = new int[58];

        final int DIFFERENT_FIELD = 0x1000;
        final int STRING_NUMERIC_DIFFERENCE = 0x100;
        final int BASE = 0x41;

        // TODO: this is a hack for 'v' and 'z'
        // resource bundle only have time skeletons ending with 'v',
        // but not for time skeletons ending with 'z'.
        boolean replaceZWithV = false;
        if ( inputSkeleton.indexOf('z') != -1 ) {
            inputSkeleton = inputSkeleton.replace('z', 'v');
            replaceZWithV = true;
        }

        parseSkeleton(inputSkeleton, inputSkeletonFieldWidth);
        int bestDistance = Integer.MAX_VALUE;
        // 0 means exact the same skeletons;
        // 1 means having the same field, but with different length,
        // 2 means only z/v differs
        // -1 means having different field.
        int bestFieldDifference = 0;
        Iterator iter = fIntervalPatterns.keySet().iterator();
        while ( iter.hasNext() ) {
            String skeleton = (String)iter.next();
            // clear skeleton field width
            for ( int i = 0; i < skeletonFieldWidth.length; ++i ) {
                skeletonFieldWidth[i] = 0;    
            }
            parseSkeleton(skeleton, skeletonFieldWidth);
            // calculate distance
            int distance = 0;
            int fieldDifference = 1;
            for ( int i = 0; i < inputSkeletonFieldWidth.length; ++i ) {
                int inputFieldWidth = inputSkeletonFieldWidth[i];
                int fieldWidth = skeletonFieldWidth[i];
                if ( inputFieldWidth == fieldWidth ) {
                    continue;
                }
                if ( inputFieldWidth == 0 ) {
                    fieldDifference = -1;
                    distance += DIFFERENT_FIELD;
                } else if ( fieldWidth == 0 ) {
                    fieldDifference = -1;
                    distance += DIFFERENT_FIELD;
                } else if (stringNumeric(inputFieldWidth, fieldWidth, 
                                         (char)(i+BASE) ) ) {
                    distance += STRING_NUMERIC_DIFFERENCE;
                } else {
                    distance += Math.abs(inputFieldWidth - fieldWidth);
                }
            }
            if ( distance < bestDistance ) {
                bestSkeleton = skeleton;
                bestDistance = distance;
                bestFieldDifference = fieldDifference;
            }
            if ( distance == 0 ) {
                bestFieldDifference = 0;
                break;
            }
        }
        if ( replaceZWithV && bestFieldDifference != -1 ) {
            bestFieldDifference = 2;
        }
        return new DateIntervalFormat.BestMatchInfo(bestSkeleton, bestFieldDifference);
    }

    /**
     * Override equals
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public boolean equals(Object a) {
        if ( a instanceof DateIntervalInfo ) {
            DateIntervalInfo dtInfo = (DateIntervalInfo)a;
            return fIntervalPatterns.equals(dtInfo.fIntervalPatterns);
        }
        return false;
    }

    /**
     * Override hashcode
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public int hashCode() {
        return fIntervalPatterns.hashCode();
    }
    
}// end class DateIntervalInfo

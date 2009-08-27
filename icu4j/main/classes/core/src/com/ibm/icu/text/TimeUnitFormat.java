/*
 **************************************************************************
 * Copyright (C) 2008-2009, Google, International Business Machines
 * Corporationand others. All Rights Reserved.
 **************************************************************************
 */
package com.ibm.icu.text;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.util.TimeUnit;
import com.ibm.icu.util.TimeUnitAmount;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;


/**
 * Format or parse a TimeUnitAmount, using plural rules for the units where available.
 *
 * <P>
 * Code Sample: 
 * <pre>
 *   // create a time unit instance.
 *   // only SECOND, MINUTE, HOUR, DAY, WEEK, MONTH, and YEAR are supported
 *   TimeUnit timeUnit = TimeUnit.SECOND;
 *   // create time unit amount instance - a combination of Number and time unit
 *   TimeUnitAmount source = new TimeUnitAmount(2, timeUnit);
 *   // create time unit format instance
 *   TimeUnitFormat format = new TimeUnitFormat();
 *   // set the locale of time unit format
 *   format.setLocale(new ULocale("en"));
 *   // format a time unit amount
 *   String formatted = format.format(source);
 *   System.out.println(formatted);
 *   try {
 *       // parse a string into time unit amount
 *       TimeUnitAmount result = (TimeUnitAmount) format.parseObject(formatted);
 *       // result should equal to source 
 *   } catch (ParseException e) {
 *   }
 * </pre>
 *
 * <P>
 * @see TimeUnitAmount
 * @see TimeUnitFormat
 * @author markdavis
 * @stable ICU 4.0
 */
public class TimeUnitFormat extends MeasureFormat {

    /**
     * Constant for full name style format. 
     * For example, the full name for "hour" in English is "hour" or "hours".
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static final int FULL_NAME = 0;
    /**
     * Constant for abbreviated name style format. 
     * For example, the abbreviated name for "hour" in English is "hr" or "hrs".
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public static final int ABBREVIATED_NAME = 1;

    private static final int TOTAL_STYLES = 2;

    private static final long serialVersionUID = -3707773153184971529L;
  
    private static final String DEFAULT_PATTERN_FOR_SECOND = "{0} s";
    private static final String DEFAULT_PATTERN_FOR_MINUTE = "{0} min";
    private static final String DEFAULT_PATTERN_FOR_HOUR = "{0} h";
    private static final String DEFAULT_PATTERN_FOR_DAY = "{0} d";
    private static final String DEFAULT_PATTERN_FOR_WEEK = "{0} w";
    private static final String DEFAULT_PATTERN_FOR_MONTH = "{0} m";
    private static final String DEFAULT_PATTERN_FOR_YEAR = "{0} y";

    private NumberFormat format;
    private ULocale locale;
    private transient Map<TimeUnit, Map<String, Object[]>> timeUnitToCountToPatterns;
    private transient PluralRules pluralRules;
    private transient boolean isReady;
    private int style;

    /**
     * Create empty format using full name style, for example, "hours". 
     * Use setLocale and/or setFormat to modify.
     * @stable ICU 4.0
     */
    public TimeUnitFormat() {
        isReady = false;
        style = FULL_NAME;

    }

    /**
     * Create TimeUnitFormat given a ULocale, and using full name style.
     * @param locale   locale of this time unit formatter.
     * @stable ICU 4.0
     */
    public TimeUnitFormat(ULocale locale) {
        this(locale, FULL_NAME);
    }

    /**
     * Create TimeUnitFormat given a Locale, and using full name style.
     * @param locale   locale of this time unit formatter.
     * @stable ICU 4.0
     */
    public TimeUnitFormat(Locale locale) {
        this(locale, FULL_NAME);
    }

    /**
     * Create TimeUnitFormat given a ULocale and a formatting style: full or
     * abbreviated.
     * @param locale   locale of this time unit formatter.
     * @param style    format style, either FULL_NAME or ABBREVIATED_NAME style.
     * @throws IllegalArgumentException if the style is not FULL_NAME or
     *                                  ABBREVIATED_NAME style.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat(ULocale locale, int style) {
        if (style < FULL_NAME || style >= TOTAL_STYLES) {
            throw new IllegalArgumentException("style should be either FULL_NAME or ABBREVIATED_NAME style");
        }
        this.style = style;
        this.locale = locale;
        isReady = false;
    }

    /**
     * Create TimeUnitFormat given a Locale and a formatting style: full or
     * abbreviated.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat(Locale locale, int style) {
        this(ULocale.forLocale(locale),  style);
    }

    /**
     * Set the locale used for formatting or parsing.
     * @param locale   locale of this time unit formatter.
     * @return this, for chaining.
     * @stable ICU 4.0
     */
    public TimeUnitFormat setLocale(ULocale locale) {
        if ( locale != this.locale ) {
            this.locale = locale;
            isReady = false;
        }
        return this;
    }
    
    /**
     * Set the locale used for formatting or parsing.
     * @param locale   locale of this time unit formatter.
     * @return this, for chaining.
     * @stable ICU 4.0
     */
    public TimeUnitFormat setLocale(Locale locale) {
        return setLocale(ULocale.forLocale(locale));
    }
    
    /**
     * Set the format used for formatting or parsing. If null or not available, use the getNumberInstance(locale).
     * @param format   the number formatter.
     * @return this, for chaining.
     * @stable ICU 4.0
     */
    public TimeUnitFormat setNumberFormat(NumberFormat format) {
        if (format == this.format) {
            return this;
        }
        if ( format == null ) {
            if ( locale == null ) {
                isReady = false;
                return this;
            } else {
                this.format = NumberFormat.getNumberInstance(locale);
            }
        } else {
            this.format = format;
        }
        // reset the number formatter in the timeUnitToCountToPatterns map
        if (isReady == false) {
            return this;
        }
        for (TimeUnit timeUnit : timeUnitToCountToPatterns.keySet()) {
            Map<String, Object[]> countToPattern = timeUnitToCountToPatterns.get(timeUnit);
            for (String count : countToPattern.keySet()) {
                Object[] pair = countToPattern.get(count);
                MessageFormat pattern = (MessageFormat)pair[FULL_NAME];
                pattern.setFormatByArgumentIndex(0, format);
                pattern = (MessageFormat)pair[ABBREVIATED_NAME];
                pattern.setFormatByArgumentIndex(0, format);
            }
        }
        return this;
    }


    /**
     * Format a TimeUnitAmount.
     * @see java.text.Format#format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)
     * @stable ICU 4.0
     */
    public StringBuffer format(Object obj, StringBuffer toAppendTo,
            FieldPosition pos) {
        if ( !(obj instanceof TimeUnitAmount) ) {
            throw new IllegalArgumentException("can not format non TimeUnitAmount object");
        }
        if (!isReady) {
            setup();
        }
        TimeUnitAmount amount = (TimeUnitAmount) obj;
        Map<String, Object[]> countToPattern = timeUnitToCountToPatterns.get(amount.getTimeUnit());
        double number = amount.getNumber().doubleValue();
        String count = pluralRules.select(number);
        MessageFormat pattern = (MessageFormat)(countToPattern.get(count))[style];
        return pattern.format(new Object[]{amount.getNumber()}, toAppendTo, pos);
    }


    /**
     * Parse a TimeUnitAmount.
     * @see java.text.Format#parseObject(java.lang.String, java.text.ParsePosition)
     * @stable ICU 4.0
     */
    public Object parseObject(String source, ParsePosition pos) {
        if (!isReady) {
            setup();
        }
        Number resultNumber = null;
        TimeUnit resultTimeUnit = null;
        int oldPos = pos.getIndex();
        int newPos = -1;
        int longestParseDistance = 0;
        String countOfLongestMatch = null;
        // we don't worry too much about speed on parsing, but this can be optimized later if needed.
        // Parse by iterating through all available patterns
        // and looking for the longest match.
        for (TimeUnit timeUnit : timeUnitToCountToPatterns.keySet()) {
            Map<String, Object[]> countToPattern = timeUnitToCountToPatterns.get(timeUnit);
            for (String count : countToPattern.keySet()) {
              for (int styl = FULL_NAME; styl < TOTAL_STYLES; ++styl) {
                MessageFormat pattern = (MessageFormat)(countToPattern.get(count))[styl];
                pos.setErrorIndex(-1);
                pos.setIndex(oldPos);
                // see if we can parse
                Object parsed = pattern.parseObject(source, pos);
                if ( pos.getErrorIndex() != -1 || pos.getIndex() == oldPos ) {
                    // nothing parsed
                    continue;
                }
                Number temp = null;
                if ( ((Object[])parsed).length != 0 ) {
                    // pattern with Number as beginning,
                    // such as "{0} d".
                    // check to make sure that the timeUnit is consistent
                    temp = (Number)((Object[])parsed)[0];
                    String select = pluralRules.select(temp.doubleValue());
                    if (!count.equals(select)) {
                        continue;
                    }
                }
                int parseDistance = pos.getIndex() - oldPos;
                if ( parseDistance > longestParseDistance ) {
                    resultNumber = temp;
                    resultTimeUnit = timeUnit;
                    newPos = pos.getIndex();
                    longestParseDistance = parseDistance;
                    countOfLongestMatch = count;
                }
            }
          }
        }
        /* After find the longest match, parse the number.
         * Result number could be null for the pattern without number pattern.
         * such as unit pattern in Arabic.
         * When result number is null, use plural rule to set the number.
         */
        if (resultNumber == null && longestParseDistance != 0) {
            // set the number using plurrual count
            if ( countOfLongestMatch.equals("zero") ) {
                resultNumber = new Integer(0);
            } else if ( countOfLongestMatch.equals("one") ) {
                resultNumber = new Integer(1);
            } else if ( countOfLongestMatch.equals("two") ) {
                resultNumber = new Integer(2);
            } else {
                // should not happen.
                // TODO: how to handle?
                resultNumber = new Integer(3);
            }
        }
        if (longestParseDistance == 0) {
            pos.setIndex(oldPos);
            pos.setErrorIndex(0);
            return null;
        } else {
            pos.setIndex(newPos);
            pos.setErrorIndex(-1);
            return new TimeUnitAmount(resultNumber, resultTimeUnit);
        }
    }
    
    
    /*
     * Initialize locale, number formatter, plural rules, and
     * time units patterns.
     * Initially, we are storing all of these as MessageFormats.
     * I think it might actually be simpler to make them Decimal Formats later.
     */
    private void setup() {
        if (locale == null) {
            if (format != null) {
                locale = format.getLocale(null);
            } else {
                locale = ULocale.getDefault();
            }
        }
        if (format == null) {
            format = NumberFormat.getNumberInstance(locale);
        }
        pluralRules = PluralRules.forLocale(locale);
        timeUnitToCountToPatterns = new HashMap<TimeUnit, Map<String, Object[]>>();

        setup("units", timeUnitToCountToPatterns, FULL_NAME);
        setup("unitsShort", timeUnitToCountToPatterns, ABBREVIATED_NAME);
        isReady = true;
    }


    private void setup(String resourceKey, Map<TimeUnit, Map<String, Object[]>> timeUnitToCountToPatterns,
                       int style) {
        // fill timeUnitToCountToPatterns from resource file
        try {
            ICUResourceBundle resource = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
            ICUResourceBundle unitsRes = resource.getWithFallback(resourceKey);
            int size = unitsRes.getSize();
            for ( int index = 0; index < size; ++index) {
                String timeUnitName = unitsRes.get(index).getKey();
                TimeUnit timeUnit = null;
                if ( timeUnitName.equals("year") ) {
                    timeUnit = TimeUnit.YEAR;
                } else if ( timeUnitName.equals("month") ) {
                    timeUnit = TimeUnit.MONTH;
                } else if ( timeUnitName.equals("day") ) {
                    timeUnit = TimeUnit.DAY;
                } else if ( timeUnitName.equals("hour") ) {
                    timeUnit = TimeUnit.HOUR;
                } else if ( timeUnitName.equals("minute") ) {
                    timeUnit = TimeUnit.MINUTE;
                } else if ( timeUnitName.equals("second") ) {
                    timeUnit = TimeUnit.SECOND;
                } else if ( timeUnitName.equals("week") ) {
                    timeUnit = TimeUnit.WEEK;
                } else {
                    continue;
                }
                ICUResourceBundle oneUnitRes = unitsRes.getWithFallback(timeUnitName);
                int count = oneUnitRes.getSize();
                Map<String, Object[]> countToPatterns = timeUnitToCountToPatterns.get(timeUnit);
                if (countToPatterns ==  null) {
                    countToPatterns = new TreeMap<String, Object[]>();
                    timeUnitToCountToPatterns.put(timeUnit, countToPatterns);
                } 
                for ( int pluralIndex = 0; pluralIndex < count; ++pluralIndex) {
                    String pluralCount = oneUnitRes.get(pluralIndex).getKey();
                    String pattern = oneUnitRes.get(pluralIndex).getString();
                    final MessageFormat messageFormat = new MessageFormat(pattern, locale);
                    if (format != null) {
                        messageFormat.setFormatByArgumentIndex(0, format);
                    }
                    // save both full name and abbreviated name in one table
                    // is good space-wise, but it degrades performance, 
                    // since it needs to check whether the needed space 
                    // is already allocated or not.
                    Object[] pair = countToPatterns.get(pluralCount);
                    if (pair == null) {
                        pair = new Object[2];
                        countToPatterns.put(pluralCount, pair);
                    } 
                    pair[style] = messageFormat;
                }
            }
        } catch ( MissingResourceException e ) {
        }

        // there should be patterns for each plural rule in each time unit.
        // For each time unit, 
        //     for each plural rule, following is unit pattern fall-back rule:
        //         ( for example: "one" hour )
        //         look for its unit pattern in its locale tree.
        //         if pattern is not found in its own locale, such as de_DE,
        //         look for the pattern in its parent, such as de,
        //         keep looking till found or till root.
        //         if the pattern is not found in root either,
        //         fallback to plural count "other",
        //         look for the pattern of "other" in the locale tree:
        //         "de_DE" to "de" to "root".
        //         If not found, fall back to value of 
        //         static variable DEFAULT_PATTERN_FOR_xxx, such as "{0} h". 
        //
        // Following is consistency check to create pattern for each
        // plural rule in each time unit using above fall-back rule.
        //
        final TimeUnit[] timeUnits = TimeUnit.values();
        Set<String> keywords = pluralRules.getKeywords();
        for ( int i = 0; i < timeUnits.length; ++i ) {
            // for each time unit, 
            // get all the patterns for each plural rule in this locale.
            final TimeUnit timeUnit = timeUnits[i];
            Map<String, Object[]> countToPatterns = timeUnitToCountToPatterns.get(timeUnit);
            if (countToPatterns == null) {
                countToPatterns = new TreeMap<String, Object[]>();
                timeUnitToCountToPatterns.put(timeUnit, countToPatterns);
            }
            for (String pluralCount : keywords) {
                if ( countToPatterns.get(pluralCount) == null ||
                     countToPatterns.get(pluralCount)[style] == null ) {
                    // look through parents
                    searchInTree(resourceKey, style, timeUnit, pluralCount, pluralCount, countToPatterns);
                }
            }
        }
    }



    // srcPluralCount is the original plural count on which the pattern is
    // searched for.
    // searchPluralCount is the fallback plural count.
    // For example, to search for pattern for ""one" hour",
    // "one" is the srcPluralCount,
    // if the pattern is not found even in root, fallback to 
    // using patterns of plural count "other", 
    // then, "other" is the searchPluralCount.
    private void searchInTree(String resourceKey, int styl,
                              TimeUnit timeUnit, String srcPluralCount,
                              String searchPluralCount, Map<String, Object[]> countToPatterns) {
        ULocale parentLocale=locale;
        String srcTimeUnitName = timeUnit.toString();
        while ( parentLocale != null ) {
            try {
                // look for pattern for srcPluralCount in locale tree
                ICUResourceBundle unitsRes = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, parentLocale);
                unitsRes = unitsRes.getWithFallback(resourceKey);
                ICUResourceBundle oneUnitRes = unitsRes.getWithFallback(srcTimeUnitName);
                String pattern = oneUnitRes.getStringWithFallback(searchPluralCount);
                final MessageFormat messageFormat = new MessageFormat(pattern, locale);
                if (format != null) {
                    messageFormat.setFormatByArgumentIndex(0, format);
                }
                Object[] pair = countToPatterns.get(srcPluralCount);
                if (pair == null) {
                    pair = new Object[2];
                    countToPatterns.put(srcPluralCount, pair);
                }
                pair[styl] = messageFormat;
                return;
            } catch ( MissingResourceException e ) {
            }
            parentLocale=parentLocale.getFallback();
        }
        // if not found the pattern for this plural count at all,
        // fall-back to plural count "other"
        if ( searchPluralCount.equals("other") ) {
            // set default fall back the same as the resource in root
            MessageFormat messageFormat = null;
            if ( timeUnit == TimeUnit.SECOND ) {
                messageFormat = new MessageFormat(DEFAULT_PATTERN_FOR_SECOND, locale);
            } else if ( timeUnit == TimeUnit.MINUTE ) {
                messageFormat = new MessageFormat(DEFAULT_PATTERN_FOR_MINUTE, locale);
            } else if ( timeUnit == TimeUnit.HOUR ) {
                messageFormat = new MessageFormat(DEFAULT_PATTERN_FOR_HOUR, locale);
            } else if ( timeUnit == TimeUnit.WEEK ) {
                messageFormat = new MessageFormat(DEFAULT_PATTERN_FOR_WEEK, locale);
            } else if ( timeUnit == TimeUnit.DAY ) {
                messageFormat = new MessageFormat(DEFAULT_PATTERN_FOR_DAY, locale);
            } else if ( timeUnit == TimeUnit.MONTH ) {
                messageFormat = new MessageFormat(DEFAULT_PATTERN_FOR_MONTH, locale);
            } else if ( timeUnit == TimeUnit.YEAR ) {
                messageFormat = new MessageFormat(DEFAULT_PATTERN_FOR_YEAR, locale);
            }
            if (format != null && messageFormat != null) {
                messageFormat.setFormatByArgumentIndex(0, format);
            }
            Object[] pair = countToPatterns.get(srcPluralCount);
            if (pair == null) {
                pair = new Object[2];
                countToPatterns.put(srcPluralCount, pair);
            }
            pair[styl] = messageFormat;
        } else {
            // fall back to rule "other", and search in parents
            searchInTree(resourceKey, styl, timeUnit, srcPluralCount, "other", countToPatterns);
        }
    }
}

/*
 *******************************************************************************
 * Copyright (C) 2008, Google, International Business Machines Corporation and *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;
import java.util.Set;

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
 * @draft ICU 4.0
 * @provisional This API might change or be removed in a future release.
 */
public class TimeUnitFormat extends MeasureFormat {

    private static final long serialVersionUID = -3707773153184971529L;
  
    private static final String DEFAULT_PATTERN_FOR_SECOND = "{0} s";
    private static final String DEFAULT_PATTERN_FOR_MINUTE = "{0} min";
    private static final String DEFAULT_PATTERN_FOR_HOUR = "{0} h";
    private static final String DEFAULT_PATTERN_FOR_WEEK = "{0} w";
    private static final String DEFAULT_PATTERN_FOR_DAY = "{0} d";
    private static final String DEFAULT_PATTERN_FOR_MONTH = "{0} m";
    private static final String DEFAULT_PATTERN_FOR_YEAR = "{0} y";

    private NumberFormat format;
    private ULocale locale;
    private transient Map timeUnitToCountToPatterns;
    private transient PluralRules pluralRules;
    private transient boolean isReady;

    /**
     * Create empty format. Use setLocale and/or setFormat to modify.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat() {}

    /**
     * Create TimeUnitFormat given a ULocale.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat(ULocale locale) {
        this.locale = locale;
        isReady = false;
    }

    /**
     * Create TimeUnitFormat given a Locale.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat(Locale locale) {
        this.locale = ULocale.forLocale(locale);
        isReady = false;
    }

    /**
     * Set the locale used for formatting or parsing.
     * @return this, for chaining.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat setLocale(ULocale locale) {
        this.locale = locale;
        isReady = false;
        return this;
    }
    
    /**
     * Set the locale used for formatting or parsing.
     * @return this, for chaining.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat setLocale(Locale locale) {
        this.locale = ULocale.forLocale(locale);
        isReady = false;
        return this;
    }
    
    /**
     * Set the format used for formatting or parsing. If null or not available, use the getNumberInstance(locale).
     * @return this, for chaining.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
     */
    public TimeUnitFormat setNumberFormat(NumberFormat format) {
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
        for (Iterator it = timeUnitToCountToPatterns.keySet().iterator(); 
             it.hasNext();) {
            TimeUnit timeUnit = (TimeUnit) it.next();
            Map countToPattern = (Map) timeUnitToCountToPatterns.get(timeUnit);
            for (Iterator it2 = countToPattern.keySet().iterator(); it2.hasNext();) {
                String count = (String) it2.next();
                MessageFormat pattern = (MessageFormat) countToPattern.get(count);
                pattern.setFormatByArgumentIndex(0, format);
            }
        }
        return this;
    }


    /**
     * Format a TimeUnitAmount.
     * @see java.text.Format#format(java.lang.Object, java.lang.StringBuffer, java.text.FieldPosition)
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
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
        Map countToPattern = (Map) timeUnitToCountToPatterns.get(amount.getTimeUnit());
        double number = amount.getNumber().doubleValue();
        String count = pluralRules.select(number);
        MessageFormat pattern = (MessageFormat) countToPattern.get(count);
        return pattern.format(new Object[]{amount.getNumber()}, toAppendTo, pos);
    }


    /**
     * Parse a TimeUnitAmount.
     * @draft ICU 4.0
     * @provisional This API might change or be removed in a future release.
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
        for (Iterator it = timeUnitToCountToPatterns.keySet().iterator(); it.hasNext();) {
            TimeUnit timeUnit = (TimeUnit) it.next();
            Map countToPattern = (Map) timeUnitToCountToPatterns.get(timeUnit);
            for (Iterator it2 = countToPattern.keySet().iterator(); it2.hasNext();) {
                String count = (String) it2.next();
                MessageFormat pattern = (MessageFormat) countToPattern.get(count);
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
        pos.setIndex(newPos);
        pos.setErrorIndex(-1);
        return new TimeUnitAmount(resultNumber, resultTimeUnit);
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
        timeUnitToCountToPatterns = new HashMap();

        // fill timeUnitToCountToPatterns from resource file
        try {
            ICUResourceBundle resource = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
            ICUResourceBundle unitsRes = resource.getWithFallback("units");
            int size = unitsRes.getSize();
            for ( int index = 0; index < size; ++index) {
                String timeUnitName = unitsRes.get(index).getKey();
                ICUResourceBundle oneUnitRes = unitsRes.getWithFallback(timeUnitName);
                int count = oneUnitRes.getSize();
                Map countToPatterns = new TreeMap();
                for ( int pluralIndex = 0; pluralIndex < count; ++pluralIndex) {
                    String pluralCount = oneUnitRes.get(pluralIndex).getKey();
                    String pattern = oneUnitRes.get(pluralIndex).getString();
                    final MessageFormat messageFormat = new MessageFormat(pattern, locale);
                    if (format != null) {
                        messageFormat.setFormatByArgumentIndex(0, format);
                    }
                    countToPatterns.put(pluralCount, messageFormat);
                }
                if ( timeUnitName.equals("year") ) {
                    timeUnitToCountToPatterns.put(TimeUnit.YEAR, countToPatterns);
                } else if ( timeUnitName.equals("month") ) {
                    timeUnitToCountToPatterns.put(TimeUnit.MONTH, countToPatterns);
                } else if ( timeUnitName.equals("day") ) {
                    timeUnitToCountToPatterns.put(TimeUnit.DAY, countToPatterns);
                } else if ( timeUnitName.equals("hour") ) {
                    timeUnitToCountToPatterns.put(TimeUnit.HOUR, countToPatterns);
                } else if ( timeUnitName.equals("minute") ) {
                    timeUnitToCountToPatterns.put(TimeUnit.MINUTE, countToPatterns);
                } else if ( timeUnitName.equals("second") ) {
                    timeUnitToCountToPatterns.put(TimeUnit.SECOND, countToPatterns);
                } else if ( timeUnitName.equals("week") ) {
                    timeUnitToCountToPatterns.put(TimeUnit.WEEK, countToPatterns);
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
        Set keywords = pluralRules.getKeywords();
        for ( int i = 0; i < timeUnits.length; ++i ) {
            // for each time unit, 
            // get all the patterns for each plural rule in this locale.
            final TimeUnit timeUnit = timeUnits[i];
            Map countToPatterns = (Map) timeUnitToCountToPatterns.get(timeUnit);
            boolean previousEmpty = false;
            if ( countToPatterns == null ) {
                countToPatterns = new TreeMap();
                previousEmpty = true;
            }
            for (Iterator it = keywords.iterator(); it.hasNext();) {
                String pluralCount = (String) it.next();
                if ( !countToPatterns.containsKey(pluralCount) ) {
                    // look through parents
                    searchInTree(timeUnit, pluralCount, pluralCount, countToPatterns);
                }
            }
            if ( previousEmpty ) {
                timeUnitToCountToPatterns.put(timeUnit, countToPatterns);
            }
        }
        isReady = true;
    }



    // srcPluralCount is the original plural count on which the pattern is
    // searched for.
    // searchPluralCount is the fallback plural count.
    // For example, to search for pattern for ""one" hour",
    // "one" is the srcPluralCount,
    // if the pattern is not found even in root, fallback to 
    // using patterns of plural count "other", 
    // then, "other" is the searchPluralCount.
    private void searchInTree(TimeUnit timeUnit, String srcPluralCount,
                              String searchPluralCount, Map countToPatterns) {
        ULocale parentLocale=locale;
        String srcTimeUnitName = timeUnit.toString();
        boolean found = false;
        try {
            // look for pattern for srcPluralCount in locale tree
            while ( parentLocale != null ) {
                ICUResourceBundle unitsRes = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, parentLocale);
                unitsRes = unitsRes.getWithFallback("units");
                int size = unitsRes.getSize();
                for ( int index = 0; index < size; ++index) {
                    String timeUnitName = unitsRes.get(index).getKey();
                    if ( !timeUnitName.equalsIgnoreCase(srcTimeUnitName) ) {
                        continue;
                    }
                    ICUResourceBundle oneUnitRes = unitsRes.getWithFallback(timeUnitName);
                    int count = oneUnitRes.getSize();
                    for ( int pluralIndex = 0; pluralIndex < count;
                          ++pluralIndex ) {
                        String pluralCount = oneUnitRes.get(pluralIndex).getKey();
                        if ( !pluralCount.equals(searchPluralCount) ) {
                            continue;
                        }
                        String pattern = oneUnitRes.get(pluralIndex).getString();
                        final MessageFormat messageFormat = new MessageFormat(pattern, locale);
                        if (format != null) {
                            messageFormat.setFormatByArgumentIndex(0, format);
                        }
                        countToPatterns.put(srcPluralCount, messageFormat);
                        found = true;
                        break;
                    }
                    // found the right timeUnit, break;
                    break;
                }
                if ( found ) {
                    break;
                }
                parentLocale=parentLocale.getFallback();
            }
        } catch ( MissingResourceException e ) {
        }
        if ( found ) {
            return;
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
            if (format != null && messageFormat != null ) {
                messageFormat.setFormatByArgumentIndex(0, format);
            }
            countToPatterns.put(srcPluralCount, messageFormat);
        } else {
            // fall back to rule "other", and search in parents
            searchInTree(timeUnit, srcPluralCount, "other", countToPatterns);
        }
    }
}

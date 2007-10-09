//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 2006-2007, Google, International Business Machines Corporation *
 * and others. All Rights Reserved.                                            *
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.CalendarData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.PatternTokenizer;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
 * @draft ICU 3.6
 * @provisional This API might change or be removed in a future release.
 */
public class DateTimePatternGenerator implements Freezable, Cloneable {
    // debugging flags
    //static boolean SHOW_DISTANCE = false;
    // TODO add hack to fix months for CJK, as per bug ticket 1099
    
    /**
     * Create empty generator, to be constructed with add(...) etc.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public static DateTimePatternGenerator getEmptyInstance() {
        return new DateTimePatternGenerator();
    }
    
    /**
     * Only for use by subclasses
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    protected DateTimePatternGenerator() {         
    }
    
    /**
     * Construct a flexible generator according to data for a given locale.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public static DateTimePatternGenerator getInstance() {
        return getInstance(ULocale.getDefault());
    }
    
    /**
     * Construct a flexible generator according to data for a given locale.
     * @param uLocale
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public static DateTimePatternGenerator getInstance(ULocale uLocale) {
        DateTimePatternGenerator result = new DateTimePatternGenerator();
        String lang = uLocale.getLanguage();
        if (lang.equals("zh") || lang.equals("ko") || lang.equals("ja")) {
          result.chineseMonthHack = true;
        }
        PatternInfo returnInfo = new PatternInfo();
        String hackPattern = null;
        // first load with the ICU patterns
        for (int i = DateFormat.FULL; i <= DateFormat.SHORT; ++i) {
            SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateInstance(i, uLocale);
            result.addPattern(df.toPattern(), false, returnInfo);
            df = (SimpleDateFormat) DateFormat.getTimeInstance(i, uLocale);
            result.addPattern(df.toPattern(), false, returnInfo);
            // HACK for hh:ss
            if (i == DateFormat.MEDIUM) {
                hackPattern = df.toPattern();
            }
        }

        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, uLocale);
        rb = rb.getWithFallback("calendar");
        ICUResourceBundle gregorianBundle = rb.getWithFallback("gregorian");
        // CLDR item formats
        
        ICUResourceBundle itemBundle = gregorianBundle.getWithFallback("appendItems");
        for (int i=0; i<itemBundle.getSize(); ++i) {
            ICUResourceBundle formatBundle = (ICUResourceBundle)itemBundle.get(i);
            String formatName = itemBundle.get(i).getKey();
            String value = formatBundle.getString();
            result.setAppendItemFormat(getAppendFormatNumber(formatName), value);
        }
        
        // CLDR item names
        itemBundle = gregorianBundle.getWithFallback("fields");
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
          
        // set the AvailableFormat in CLDR
        try {
           ICUResourceBundle formatBundle =  gregorianBundle.getWithFallback("availableFormats");
           //System.out.println("available format from current locale:"+uLocale.getName());
           for (int i=0; i<formatBundle.getSize(); ++i) { 
               String formatKey = formatBundle.get(i).getKey();
               String formatValue = formatBundle.get(i).getString();
               //System.out.println(" availableFormat:"+formatValue);
               result.setAvailableFormat(formatKey);
               result.addPattern(formatValue, false, returnInfo);
           } 
        }catch(Exception e) {
        }
       
        ULocale parentLocale=uLocale;
        while ( (parentLocale=parentLocale.getFallback()) != null) {
            ICUResourceBundle prb = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, parentLocale);
            prb = prb.getWithFallback("calendar");
            ICUResourceBundle pGregorianBundle = prb.getWithFallback("gregorian");
            try {
                ICUResourceBundle formatBundle =  pGregorianBundle.getWithFallback("availableFormats");
                //System.out.println("available format from parent locale:"+parentLocale.getName());
                for (int i=0; i<formatBundle.getSize(); ++i) { 
                    String formatKey = formatBundle.get(i).getKey();
                    String formatValue = formatBundle.get(i).getString();
                    //System.out.println(" availableFormat:"+formatValue);
                    if (!result.isAvailableFormatSet(formatKey)) {
                        result.setAvailableFormat(formatKey);
                        result.addPattern(formatValue, false, returnInfo);
                        //System.out.println(" availableFormat:"+formatValue);
                    }
                } 
              
             }catch(Exception e) {
             }
             
        }
        
        // assume it is always big endian (ok for CLDR right now)
        // some languages didn't add mm:ss or HH:mm, so put in a hack to compute that from the short time.
        if (hackPattern != null) {
            hackTimes(result, returnInfo, hackPattern);
        }
        
        // set the datetime pattern. This is ugly code -- there should be a public interface for this
        Calendar cal = Calendar.getInstance(uLocale);
        CalendarData calData = new CalendarData(uLocale, cal.getType());
        String[] patterns = calData.get("DateTimePatterns").getStringArray();
        result.setDateTimeFormat(patterns[8]);
        
        // decimal point for seconds
        DecimalFormatSymbols dfs = new DecimalFormatSymbols(uLocale);
        result.setDecimal(String.valueOf(dfs.getDecimalSeparator()));
        return result;
    }
    
    private static void hackTimes(DateTimePatternGenerator result, PatternInfo returnInfo, String hackPattern) {
        result.fp.set(hackPattern);
        String mmss = new String();
        // to get mm:ss, we strip all but mm literal ss
        boolean gotMm = false;
        for (int i = 0; i < result.fp.items.size(); ++i) {
            Object item = result.fp.items.get(i);
            if (item instanceof String) {
                if (gotMm) {
                    mmss += result.fp.quoteLiteral(item.toString());
                }
            } else {
                char ch = item.toString().charAt(0);
                if (ch == 'm') {
                    gotMm = true;
                    mmss += item;
                } else if (ch == 's') {
                    if (!gotMm) {
                        break; // failed
                    }
                    mmss += item;
                    result.addPattern(mmss, false, returnInfo);
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
        String result = new String();
        for (int i = 0; i < fp.items.size(); ++i) {
            if (nuke.get(i)) continue;
            Object item = fp.items.get(i);
            if (item instanceof String) {
                result += fp.quoteLiteral(item.toString());
            } else {
                result += item.toString();
            }
        }
        return result;
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
     * @param skeleton
     *            The skeleton is a pattern containing only the variable fields.
     *            For example, "MMMdd" and "mmhh" are skeletons.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public String getBestPattern(String skeleton) {
      if (chineseMonthHack) {
        skeleton = skeleton.replaceAll("MMM+", "MM");
      }
        //if (!isComplete) complete();
        current.set(skeleton, fp);
        String best = getBestRaw(current, -1, _distanceInfo);
        if (_distanceInfo.missingFieldMask == 0 && _distanceInfo.extraFieldMask == 0) {
            // we have a good item. Adjust the field types
            return adjustFieldTypes(best, current, false);
        }
        int neededFields = current.getFieldMask();
        // otherwise break up by date and time.
        String datePattern = getBestAppending(neededFields & DATE_MASK);
        String timePattern = getBestAppending(neededFields & TIME_MASK);
        
        if (datePattern == null) return timePattern == null ? "" : timePattern;
        if (timePattern == null) return datePattern;
        return MessageFormat.format(getDateTimeFormat(), new Object[]{datePattern, timePattern});
    }
    
    /**
     * PatternInfo supplies output parameters for add(...). It is used because
     * Java doesn't have real output parameters. It is treated like a struct (eg
     * Point), so all fields are public.
     * 
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public static final class PatternInfo { // struct for return information
        /**
         * @draft ICU 3.6
         * @provisional This API might change or be removed in a future release.
         */
        public static final int OK = 0;
        
        /**
         * @draft ICU 3.6
         * @provisional This API might change or be removed in a future release.
         */
        public static final int BASE_CONFLICT = 1;
        
        /**
         * @draft ICU 3.6
         * @provisional This API might change or be removed in a future release.
         */
        public static final int CONFLICT = 2;
        
        /**
         * @draft ICU 3.6
         * @provisional This API might change or be removed in a future release.
         */
        public int status;
        
        /**
         * @draft ICU 3.6
         * @provisional This API might change or be removed in a future release.
         */
        public String conflictingPattern;
        
        /**
         * Simple constructor, since this is treated like a struct.
         * @draft ICU 3.6
         * @provisional This API might change or be removed in a future release.
         */
        public PatternInfo() {
        }
    }
    
    /**
     * Adds a pattern to the generator. If the pattern has the same skeleton as
     * an existing pattern, and the override parameter is set, then the previous
     * value is overriden. Otherwise, the previous value is retained. In either
     * case, the conflicting information is returned in PatternInfo.
     * <p>
     * Note that single-field patterns (like "MMM") are automatically added, and
     * don't need to be added explicitly!
     * 
     * @param override
     *            when existing values are to be overridden use true, otherwise
     *            use false.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public DateTimePatternGenerator addPattern(String pattern, boolean override, PatternInfo returnInfo) {
        checkFrozen();
        DateTimeMatcher matcher = new DateTimeMatcher().set(pattern, fp);
        String basePattern = matcher.getBasePattern();
        String previousPatternWithSameBase = (String)basePattern_pattern.get(basePattern);
        if (previousPatternWithSameBase != null) {
            returnInfo.status = PatternInfo.BASE_CONFLICT;
            returnInfo.conflictingPattern = previousPatternWithSameBase;
            if (!override) return this;
        }
        String previousValue = (String)skeleton2pattern.get(matcher);
        if (previousValue != null) {
            returnInfo.status = PatternInfo.CONFLICT;
            returnInfo.conflictingPattern = previousValue;
            if (!override) return this;
        }
        returnInfo.status = PatternInfo.OK;
        returnInfo.conflictingPattern = "";
        skeleton2pattern.put(matcher, pattern);
        basePattern_pattern.put(basePattern, pattern);
        return this;
    }
    
    /**
     * Utility to return a unique skeleton from a given pattern. For example,
     * both "MMM-dd" and "dd/MMM" produce the skeleton "MMMdd".
     * 
     * @param pattern
     *            Input pattern, such as "dd/MMM"
     * @return skeleton, such as "MMMdd"
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
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
     * @param pattern
     *            Input pattern, such as "dd/MMM"
     * @return skeleton, such as "MMMdd"
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
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
     * @param result
     *            an output Map in which to place the mapping from skeleton to
     *            pattern. If you want to see the internal order being used,
     *            supply a LinkedHashMap. If the input value is null, then a
     *            LinkedHashMap is allocated.
     *            <p>
     *            <i>Issue: an alternate API would be to just return a list of
     *            the skeletons, and then have a separate routine to get from
     *            skeleton to pattern.</i>
     * @return the input Map containing the values.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public Map getSkeletons(Map result) {
        if (result == null) result = new LinkedHashMap();
        for (Iterator it = skeleton2pattern.keySet().iterator(); it.hasNext();) {
            DateTimeMatcher item = (DateTimeMatcher) it.next();
            String pattern = (String) skeleton2pattern.get(item);
            if (CANONICAL_SET.contains(pattern)) continue;
            result.put(item.toString(), pattern);
        }
        return result;
    }
    
    /**
     * Return a list of all the base skeletons (in canonical form) from this class
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public Set getBaseSkeletons(Set result) {
        if (result == null) result = new HashSet();
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
     * @param pattern
     *            input pattern
     * @param skeleton
     * @return pattern adjusted to match the skeleton fields widths and
     *         subtypes.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public String replaceFieldTypes(String pattern, String skeleton) {
        synchronized (this) { // synchronized since a getter must be thread-safe
            return adjustFieldTypes(pattern, current.set(skeleton, fp), false);
        }
    }
    
    /**
     * The date time format is a message format pattern used to compose date and
     * time patterns. The default value is "{0} {1}", where {0} will be replaced
     * by the date pattern and {1} will be replaced by the time pattern.
     * <p>
     * This is used when the input skeleton contains both date and time fields,
     * but there is not a close match among the added patterns. For example,
     * suppose that this object was created by adding "dd-MMM" and "hh:mm", and
     * its datetimeFormat is the default "{0} {1}". Then if the input skeleton
     * is "MMMdhmm", there is not an exact match, so the input skeleton is
     * broken up into two components "MMMd" and "hmm". There are close matches
     * for those two skeletons, so the result is put together with this pattern,
     * resulting in "d-MMM h:mm".
     * 
     * @param dateTimeFormat
     *            message format pattern, here {0} will be replaced by the date
     *            pattern and {1} will be replaced by the time pattern.
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public void setDateTimeFormat(String dateTimeFormat) {
        checkFrozen();
        this.dateTimeFormat = dateTimeFormat;
    }
    
    /**
     * Getter corresponding to setDateTimeFormat.
     * 
     * @return pattern
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
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
     * @param decimal
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public void setDecimal(String decimal) {
        checkFrozen();
        this.decimal = decimal;
    }
    
    /**
     * Getter corresponding to setDecimal.
     * @return string corresponding to the decimal point
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public String getDecimal() {
        return decimal;
    }
    
    /**
     * Redundant patterns are those which if removed, make no difference in the
     * resulting getBestPattern values. This method returns a list of them, to
     * help check the consistency of the patterns used to build this generator.
     * 
     * @param output
     *            stores the redundant patterns that are removed. To get these
     *            in internal order, supply a LinkedHashSet. If null, a
     *            collection is allocated.
     * @return the collection with added elements.
     * @deprecated
     * @internal
     */
    public Collection getRedundants(Collection output) {
        synchronized (this) { // synchronized since a getter must be thread-safe
            if (output == null) output = new LinkedHashSet();
            for (Iterator it = skeleton2pattern.keySet().iterator(); it.hasNext();) {
                DateTimeMatcher current = (DateTimeMatcher) it.next();
                String pattern = (String) skeleton2pattern.get(current);
                if (CANONICAL_SET.contains(pattern)) continue;
                skipMatcher = current;
                String trial = getBestPattern(current.toString());
                if (trial.equals(pattern)) {
                    output.add(pattern);
                }
            }
            if (false) { // ordered
                DateTimePatternGenerator results = new DateTimePatternGenerator();
                PatternInfo pinfo = new PatternInfo();
                for (Iterator it = skeleton2pattern.keySet().iterator(); it.hasNext();) {
                    DateTimeMatcher current = (DateTimeMatcher) it.next();
                    String pattern = (String) skeleton2pattern.get(current);
                    if (CANONICAL_SET.contains(pattern)) continue;
                    //skipMatcher = current;
                    String trial = results.getBestPattern(current.toString());
                    if (trial.equals(pattern)) {
                        output.add(pattern);
                    } else {
                        results.addPattern(pattern, false, pinfo);
                    }
                }
            }
            return output;
        }
    }
    
    // Field numbers, used for AppendItem functions
    
    /** 
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int ERA = 0;
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int YEAR = 1; 
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int QUARTER = 2; 
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int MONTH = 3;
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int WEEK_OF_YEAR = 4; 
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int WEEK_OF_MONTH = 5; 
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int WEEKDAY = 6; 
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int DAY = 7;
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int DAY_OF_YEAR = 8; 
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int DAY_OF_WEEK_IN_MONTH = 9; 
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int DAYPERIOD = 10;
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int HOUR = 11; 
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int MINUTE = 12; 
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int SECOND = 13; 
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int FRACTIONAL_SECOND = 14;
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int ZONE = 15; 
    
    /**
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    static final public int TYPE_LIMIT = 16;
    
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
     * @param field
     *            such as ERA
     * @param value
     *            pattern, such as "{0}, {1}"
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public void setAppendItemFormat(int field, String value) {
        checkFrozen();
        appendItemFormats[field] = value;
    }
    
    /**
     * Getter corresponding to setAppendItemFormats. Values below 0 or at or
     * above TYPE_LIMIT are illegal arguments.
     * 
     * @param field
     * @return append pattern for field
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
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
     * @param field
     * @param value
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public void setAppendItemName(int field, String value) {
        checkFrozen();
        appendItemNames[field] = value;
    }
    
    /**
     * Getter corresponding to setAppendItemNames. Values below 0 or at or above
     * TYPE_LIMIT are illegal arguments.
     * 
     * @param field
     * @return name for field
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public String getAppendItemName(int field) {
        return appendItemNames[field];
    }
    
    /**
     * Determines whether a skeleton contains a single field
     * 
     * @param skeleton
     * @return true or not
     * @deprecated
     * @internal
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
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    private boolean isAvailableFormatSet(String key) {
        return cldrAvailableFormatKeys.contains(key);
    }

    /**
     * Boilerplate for Freezable
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    /**
     * Boilerplate for Freezable
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public Object freeze() {
        frozen = true;
        return this;
    }
    
    /**
     * Boilerplate for Freezable
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public Object cloneAsThawed() {
        DateTimePatternGenerator result = (DateTimePatternGenerator) (this.clone());
        frozen = false;
        return result;
    }
    
    /**
     * Boilerplate
     * @draft ICU 3.6
     * @provisional This API might change or be removed in a future release.
     */
    public Object clone() {
        try {
            DateTimePatternGenerator result = (DateTimePatternGenerator) (super.clone());
            result.skeleton2pattern = (TreeMap) skeleton2pattern.clone();
            result.basePattern_pattern = (TreeMap) basePattern_pattern.clone();
            result.appendItemFormats = (String[]) appendItemFormats.clone();
            result.appendItemNames = (String[]) appendItemNames.clone();
            result.current = new DateTimeMatcher();
            result.fp = new FormatParser();
            result._distanceInfo = new DistanceInfo();
            
            result.frozen = false;
            return result;
        } catch (CloneNotSupportedException e) {
            throw new IllegalArgumentException("Internal Error");
        }
    }
    
    /**
     * Utility class for FormatParser. Immutable class.
     * @deprecated
     * @internal
     */
    public static class VariableField {
        private String string;
        /**
         * Create a variable field
         * @param string
         * @deprecated
         * @internal
         */
        public VariableField(String string) {
            this.string = string;
        }
        /**
         * Get the internal results
         * @deprecated
         * @internal
         */
        public String toString() {
            return string;
        }
    }
    
    /**
     * Class providing date formatting
     * @deprecated
     * @internal
     */
    static public class FormatParser {
        private transient PatternTokenizer tokenizer = new PatternTokenizer()
        .setSyntaxCharacters(new UnicodeSet("[a-zA-Z]"))
        .setExtraQuotingCharacters(new UnicodeSet("[[[:script=Latn:][:script=Cyrl:]]&[[:L:][:M:]]]"))
        //.setEscapeCharacters(new UnicodeSet("[^\\u0020-\\u007E]")) // WARNING: DateFormat doesn't accept \\uXXXX
        .setUsingQuote(true);
        private List items = new ArrayList();
        
        /**
         * Set the string to parse
         * @param string
         * @return this, for chaining
         * @deprecated
         * @internal
         */
        public FormatParser set(String string) {
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
                        addVariable(variable);
                    }
                    variable.append(buffer);
                } else {
                    addVariable(variable);
                    items.add(buffer.toString());
                }
            }
            addVariable(variable);
            return this;
        }
        
        private void addVariable(StringBuffer variable) {
            if (variable.length() != 0) {
                items.add(new VariableField(variable.toString()));
                variable.setLength(0);
            }
        }
        
        /** Return a collection of fields. These will be a mixture of Strings and VariableFields. Any "a" variable field is removed.
         * @param output List to append the items to. If null, is allocated as an ArrayList.
         * @return list
         */
        private List getVariableFields(List output) {
            if (output == null) output = new ArrayList();
            main:
                for (Iterator it = items.iterator(); it.hasNext();) {
                    Object item = it.next();
                    if (item instanceof VariableField) {
                        String s = item.toString();
                        switch(s.charAt(0)) {
                        //case 'Q': continue main; // HACK
                        case 'a': continue main; // remove
                        }
                        output.add(s);
                    }
                }
            //System.out.println(output);
            return output;
        }
        
        /**
         * @return a string which is a concatenation of all the variable fields
         * @deprecated
         * @internal
         */
        public String getVariableFieldString() {
            List list = getVariableFields(null);
            StringBuffer result = new StringBuffer();
            for (Iterator it = list.iterator(); it.hasNext();) {
                String item = (String) it.next();
                result.append(item);
            }
            return result.toString();
        }
        
        /**
         * Returns modifiable list which is a mixture of Strings and VariableFields, in the order found during parsing.
         * @return modifiable list of items.
         * @deprecated
         * @internal
         */
        public List getItems() {
            return items;
        }
        
        /** Provide display form of formatted input
         * @return printable output string
         * @deprecated
         * @internal
         */
        public String toString() {
            return toString(0, items.size());
        }
        
        /**
         * Provide display form of formatted input
         * @param start item to start from
         * @param limit last item +1
         * @return printable output string
         * @deprecated
         * @internal
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
         * Internal method <p>
         * Returns true if it has a mixture of date and time fields
         * @return true or false
         * @deprecated
         * @internal
         */
        public boolean hasDateAndTimeFields() {
            int foundMask = 0;
            for (Iterator it = items.iterator(); it.hasNext();) {
                Object item = it.next();
                if (item instanceof VariableField) {
                    int type = getType(item);
                    foundMask |= 1 << type;    
                }
            }
            boolean isDate = (foundMask & DATE_MASK) != 0;
            boolean isTime = (foundMask & TIME_MASK) != 0;
            return isDate && isTime;
        }
        
        /**
         * Internal routine
         * @param value
         * @param result
         * @return list
         * @deprecated
         * @internal
         */
        public List getAutoPatterns(String value, List result) {
            if (result == null) result = new ArrayList();
            int fieldCount = 0;
            int minField = Integer.MAX_VALUE;
            int maxField = Integer.MIN_VALUE;
            for (Iterator it = items.iterator(); it.hasNext();) {
                Object item = it.next();
                if (item instanceof VariableField) {
                    try {
                        int type = getType(item);
                        if (minField > type) minField = type;
                        if (maxField < type) maxField = type;
                        if (type == ZONE || type == DAYPERIOD || type == WEEKDAY) return result; // skip anything with zones                    
                        fieldCount++;
                    } catch (Exception e) {
                        return result; // if there are any funny fields, return
                    }
                }
            }
            if (fieldCount < 3) return result; // skip
            // trim from start
            // trim first field IF there are no letters around it
            // and it is either the min or the max field
            // first field is either 0 or 1
            for (int i = 0; i < items.size(); ++i) {
                Object item = items.get(i);
                if (item instanceof VariableField) {
                    int type = getType(item);
                    if (type != minField && type != maxField) break;
                    
                    if (i > 0) {
                        Object previousItem = items.get(0);
                        if (alpha.containsSome(previousItem.toString())) break;
                    }
                    int start = i+1;
                    if (start < items.size()) {
                        Object nextItem = items.get(start);
                        if (nextItem instanceof String) {
                            if (alpha.containsSome(nextItem.toString())) break;
                            start++; // otherwise skip over string
                        }
                    }
                    result.add(toString(start, items.size()));
                    break;
                }
            }
            // now trim from end
            for (int i = items.size()-1; i >= 0; --i) {
                Object item = items.get(i);
                if (item instanceof VariableField) {
                    int type = getType(item);
                    if (type != minField && type != maxField) break;
                    if (i < items.size() - 1) {
                        Object previousItem = items.get(items.size() - 1);
                        if (alpha.containsSome(previousItem.toString())) break;
                    }
                    int end = i-1;
                    if (end > 0) {
                        Object nextItem = items.get(end);
                        if (nextItem instanceof String) {
                            if (alpha.containsSome(nextItem.toString())) break;
                            end--; // otherwise skip over string
                        }
                    }
                    result.add(toString(0, end+1));
                    break;
                }
            }
            
            return result;
        }
        
        private static UnicodeSet alpha = new UnicodeSet("[:alphabetic:]");
        
        private int getType(Object item) {
            String s = item.toString();
            int canonicalIndex = getCanonicalIndex(s);
            if (canonicalIndex < 0) {
                throw new IllegalArgumentException("Illegal field:\t"
                        + s);
            }
            int type = types[canonicalIndex][1];
            return type;
        }
        
        /**
         *  produce a quoted literal
         * @param string
         * @return string with quoted literals
         * @deprecated
         * @internal
         */
        public Object quoteLiteral(String string) {
            return tokenizer.quoteLiteral(string);
        }
        
        /**
         * Simple constructor, since this is treated like a struct.
         * @deprecated
         * @internal
         */
        public FormatParser() {
            super();
            // TODO Auto-generated constructor stub
        }
    }
    // ========= PRIVATES ============
    
    private TreeMap skeleton2pattern = new TreeMap(); // items are in priority order
    private TreeMap basePattern_pattern = new TreeMap(); // items are in priority order
    private String decimal = "?";
    private String dateTimeFormat = "{0} {1}";
    private String[] appendItemFormats = new String[TYPE_LIMIT];
    private String[] appendItemNames = new String[TYPE_LIMIT];
    {
        for (int i = 0; i < TYPE_LIMIT; ++i) {
            appendItemFormats[i] = "{0} \u251C{2}: {1}\u2524";
            appendItemNames[i] = "F" + i;
        }
    }
    
    private transient DateTimeMatcher current = new DateTimeMatcher();
    private transient FormatParser fp = new FormatParser();
    private transient DistanceInfo _distanceInfo = new DistanceInfo();
    private transient boolean isComplete = false;
    private transient DateTimeMatcher skipMatcher = null; // only used temporarily, for internal purposes
    private transient boolean frozen = false;
    
    private transient boolean chineseMonthHack = false;
    
    private static final int FRACTIONAL_MASK = 1<<FRACTIONAL_SECOND;
    private static final int SECOND_AND_FRACTIONAL_MASK = (1<<SECOND) | (1<<FRACTIONAL_SECOND);
    
    private void checkFrozen() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
    }
    
    /**
     * We only get called here if we failed to find an exact skeleton. We have broken it into date + time, and look for the pieces.
     * If we fail to find a complete skeleton, we compose in a loop until we have all the fields.
     */
    private String getBestAppending(int missingFields) {
        String resultPattern = null;
        if (missingFields != 0) {
            resultPattern = getBestRaw(current, missingFields, _distanceInfo);
            resultPattern = adjustFieldTypes(resultPattern, current, false);
            
            while (_distanceInfo.missingFieldMask != 0) { // precondition: EVERY single field must work!
                
                // special hack for SSS. If we are missing SSS, and we had ss but found it, replace the s field according to the 
                // number separator
                if ((_distanceInfo.missingFieldMask & SECOND_AND_FRACTIONAL_MASK) == FRACTIONAL_MASK
                        && (missingFields & SECOND_AND_FRACTIONAL_MASK) == SECOND_AND_FRACTIONAL_MASK) {
                    resultPattern = adjustFieldTypes(resultPattern, current, true);
                    _distanceInfo.missingFieldMask &= ~FRACTIONAL_MASK; // remove bit
                    continue;
                }
                
                int startingMask = _distanceInfo.missingFieldMask;
                String temp = getBestRaw(current, _distanceInfo.missingFieldMask, _distanceInfo);
                temp = adjustFieldTypes(temp, current, false);
                int foundMask = startingMask & ~_distanceInfo.missingFieldMask;
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
        isComplete = true;
    }
    {
        complete();
    }
    
    /**
     * 
     */
    private String getBestRaw(DateTimeMatcher source, int includeMask, DistanceInfo missingFields) {
//      if (SHOW_DISTANCE) System.out.println("Searching for: " + source.pattern 
//      + ", mask: " + showMask(includeMask));
        int bestDistance = Integer.MAX_VALUE;
        String bestPattern = "";
        DistanceInfo tempInfo = new DistanceInfo();
        for (Iterator it = skeleton2pattern.keySet().iterator(); it.hasNext();) {
            DateTimeMatcher trial = (DateTimeMatcher) it.next();
            if (trial.equals(skipMatcher)) continue;
            int distance = source.getDistance(trial, includeMask, tempInfo);
//          if (SHOW_DISTANCE) System.out.println("\tDistance: " + trial.pattern + ":\t" 
//          + distance + ",\tmissing fields: " + tempInfo);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPattern = (String) skeleton2pattern.get(trial);
                missingFields.setTo(tempInfo);
                if (distance == 0) break;
            }
        }
        return bestPattern;
    }
    
    /**
     * @param fixFractionalSeconds TODO
     * 
     */
    private String adjustFieldTypes(String pattern, DateTimeMatcher inputRequest, boolean fixFractionalSeconds) {
        fp.set(pattern);
        StringBuffer newPattern = new StringBuffer();
        for (Iterator it = fp.getItems().iterator(); it.hasNext();) {
            Object item = it.next();
            if (item instanceof String) {
                newPattern.append(fp.quoteLiteral((String)item));
            } else {
                String field = ((VariableField) item).string;
                int canonicalIndex = getCanonicalIndex(field);
                if (canonicalIndex < 0) {
                    continue; // don't adjust
                }
                int type = types[canonicalIndex][1];
                if (fixFractionalSeconds && type == SECOND) {
                    String newField = inputRequest.original[FRACTIONAL_SECOND];
                    field = field + decimal + newField;
                } else if (inputRequest.type[type] != 0) {
                    String newField = inputRequest.original[type];
                    // normally we just replace the field. However HOUR is special; we only change the length
                    if (type != HOUR) {
                        field = newField;
                    } else if (field.length() != newField.length()){
                        char c = field.charAt(0);
                        field = "";
                        for (int i = newField.length(); i > 0; --i) field += c;
                    }
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
     * @param pattern
     * @return field value
     * @deprecated
     * @internal
     */
    public String getFields(String pattern) {
        fp.set(pattern);
        StringBuffer newPattern = new StringBuffer();
        for (Iterator it = fp.getItems().iterator(); it.hasNext();) {
            Object item = it.next();
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
    
    static private Set CANONICAL_SET = new HashSet(Arrays.asList(CANONICAL_ITEMS));
    private Set cldrAvailableFormatKeys = new HashSet(20);
    
    static final private int 
    DATE_MASK = (1<<DAYPERIOD) - 1,
    TIME_MASK = (1<<TYPE_LIMIT) - 1 - DATE_MASK;
    
    static final private int // numbers are chosen to express 'distance'
    DELTA = 0x10,
    NUMERIC = 0x100,
    NONE = 0,
    NARROW = -0x100,
    SHORT = -0x101,
    LONG = -0x102,
    EXTRA_FIELD =   0x10000,
    MISSING_FIELD = 0x1000;
    
    
    static private String getName(String s) {
        int i = getCanonicalIndex(s);
        String name = FIELD_NAME[types[i][1]];
        int subtype = types[i][2];
        boolean string = subtype < 0;
        if (string) subtype = -subtype;
        if (subtype < 0) name += ":S";
        else name += ":N";
        return name;
    }
    
    static private int getCanonicalIndex(String s) {
        int len = s.length();
        int ch = s.charAt(0);
        for (int i = 0; i < types.length; ++i) {
            int[] row = types[i];
            if (row[0] != ch) continue;
            if (row[3] > len) continue;
            if (row[row.length-1] < len) continue;
            return i;
        }
        return -1;
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
    };
    
    private static class DateTimeMatcher implements Comparable {
        //private String pattern = null;
        private int[] type = new int[TYPE_LIMIT];
        private String[] original = new String[TYPE_LIMIT];
        private String[] baseOriginal = new String[TYPE_LIMIT];
        
        // just for testing; fix to make multi-threaded later
        // private static FormatParser fp = new FormatParser();
        
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
            for (Iterator it = fp.getVariableFields(new ArrayList()).iterator(); it.hasNext();) {
                String field = (String) it.next();
                if (field.charAt(0) == 'a') continue; // skip day period, special cass
                int canonicalIndex = getCanonicalIndex(field);
                if (canonicalIndex < 0) {
                    throw new IllegalArgumentException("Illegal field:\t"
                            + field + "\t in " + pattern);
                }
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
                type[typeValue] = (byte) subTypeValue;
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
        
        public int compareTo(Object o) {
            DateTimeMatcher that = (DateTimeMatcher) o;
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
//#endif
//eof

/*
 *******************************************************************************
 * Copyright (C) 2013, Google Inc, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.util.FormatWidth;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Category;
import com.ibm.icu.util.UResourceBundle;

/**
 * Mutable class for formatting GeneralMeasures, or sequences of them.
 * @author markdavis
 * @internal
 * @deprecated This API is ICU internal only.
 */
public class GeneralMeasureFormat extends MeasureFormat {

    // Cache the data for units so we don't have to look it up each time.
    // For each format, we'll store a pointer into the EnumMap for quick access.
    // TODO use the data to allow parsing.
    static final transient Map<ULocale,ParseData> localeToParseData = new HashMap<ULocale,ParseData>();
    static final transient Map<ULocale,Map<MeasureUnit, EnumMap<FormatWidth, Map<String, PatternData>>>> localeToUnitToStyleToCountToFormat 
    = new HashMap<ULocale,Map<MeasureUnit, EnumMap<FormatWidth, Map<String, PatternData>>>>();
    static final transient Index<MeasureUnit> index = new Index<MeasureUnit>();

    static final class PatternData {
        final String prefix;
        final String suffix;
        public PatternData(String pattern) {
            int pos = pattern.indexOf("{0}");
            if (pos < 0) {
                prefix = pattern;
                suffix = null;
            } else {
                prefix = pattern.substring(0,pos);
                suffix = pattern.substring(pos+3);
            }
        }
        public String toString() {
            return prefix + "; " + suffix;
        }

    }
    private final ULocale locale;
    private final FormatWidth length;
    private final NumberFormat numberFormat;

    private final transient PluralRules rules;
    private final transient Map<MeasureUnit, EnumMap<FormatWidth, Map<String, PatternData>>> unitToStyleToCountToFormat; // invariant once built
    private transient ParseData parseData; // set as needed


    private static final long serialVersionUID = 7922671801770278517L;

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    protected GeneralMeasureFormat(ULocale locale, FormatWidth style, 
            Map<MeasureUnit, EnumMap<FormatWidth, Map<String, PatternData>>> unitToStyleToCountToFormat,
            NumberFormat numberFormat) {
        this.locale = locale;
        this.length = style;
        this.unitToStyleToCountToFormat = unitToStyleToCountToFormat;
        rules = PluralRules.forLocale(locale);
        this.numberFormat = numberFormat;
    }


    /**
     * Create a format from the locale and length
     * @param locale   locale of this time unit formatter.
     * @param length the desired length
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static GeneralMeasureFormat getInstance(ULocale locale, FormatWidth length) {
        return getInstance(locale, length, NumberFormat.getInstance(locale));
    }

    /**
     * Create a format from the locale and length
     * @param locale   locale of this time unit formatter.
     * @param length the desired length
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static GeneralMeasureFormat getInstance(ULocale locale, FormatWidth length,
            NumberFormat decimalFormat) {
        synchronized (localeToUnitToStyleToCountToFormat) {
            Map<MeasureUnit, EnumMap<FormatWidth, Map<String, PatternData>>> unitToStyleToCountToFormat 
            = localeToUnitToStyleToCountToFormat.get(locale);
            if (unitToStyleToCountToFormat == null) {
                unitToStyleToCountToFormat = cacheLocaleData(locale);
            }
            //            System.out.println(styleToCountToFormat);            
            return new GeneralMeasureFormat(locale, length, unitToStyleToCountToFormat, decimalFormat);
        }
    }

    /**
     * Return a formatter for CurrencyAmount objects in the given
     * locale.
     * @param locale desired locale
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static MeasureFormat getCurrencyFormat(ULocale locale) {
        return new CurrencyFormat(locale);
    }

    /**
     * Return a formatter for CurrencyAmount objects in the default
     * <code>FORMAT</code> locale.
     * @return a formatter object
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static MeasureFormat getCurrencyFormat() {
        return getCurrencyFormat(ULocale.getDefault(Category.FORMAT));
    }

    /**
     * @return the locale of the format.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public ULocale getLocale() {
        return locale;
    }

    /**
     * @return the desired length for the format
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public FormatWidth getLength() {
        return length;
    }

    private static Map<MeasureUnit, EnumMap<FormatWidth, Map<String, PatternData>>> cacheLocaleData(ULocale locale) {
        PluralRules rules = PluralRules.forLocale(locale);
        Set<String> keywords = rules.getKeywords();
        Map<MeasureUnit, EnumMap<FormatWidth, Map<String, PatternData>>> unitToStyleToCountToFormat;
        localeToUnitToStyleToCountToFormat.put(locale, unitToStyleToCountToFormat 
                = new HashMap<MeasureUnit, EnumMap<FormatWidth, Map<String, PatternData>>>());
        for (MeasureUnit unit : MeasureUnit.getAvailable()) {
            EnumMap<FormatWidth, Map<String, PatternData>> styleToCountToFormat = unitToStyleToCountToFormat.get(unit);
            if (styleToCountToFormat == null) {
                unitToStyleToCountToFormat.put(unit, styleToCountToFormat = new EnumMap<FormatWidth, Map<String, PatternData>>(FormatWidth.class));
            }
            ICUResourceBundle resource = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, locale);
            for (FormatWidth styleItem : FormatWidth.values()) {
                try {
                    ICUResourceBundle unitTypeRes = resource.getWithFallback(styleItem.resourceKey);
                    ICUResourceBundle unitsRes = unitTypeRes.getWithFallback(unit.getType());
                    ICUResourceBundle oneUnitRes = unitsRes.getWithFallback(unit.getCode());
                    Map<String, PatternData> countToFormat = styleToCountToFormat.get(styleItem);
                    if (countToFormat == null) {
                        styleToCountToFormat.put(styleItem, countToFormat = new HashMap<String, PatternData>());
                    }
                    for (String keyword : keywords) {
                        UResourceBundle countBundle;
                        try {
                            countBundle = oneUnitRes.get(keyword);
                        } catch (MissingResourceException e) {
                            continue;
                        }
                        String pattern = countBundle.getString();
                        //                        System.out.println(styleItem.resourceKey + "/" 
                        //                                + unit.getType() + "/" 
                        //                                + unit.getCode() + "/" 
                        //                                + keyword + "=" + pattern);
                        PatternData format = new PatternData(pattern);
                        countToFormat.put(keyword, format);
                        //                        System.out.println(styleToCountToFormat);
                    }
                    // fill in 'other' for any missing values
                    PatternData other = countToFormat.get("other");
                    for (String keyword : keywords) {
                        if (!countToFormat.containsKey(keyword)) {
                            countToFormat.put(keyword, other);
                        }
                    }
                } catch (MissingResourceException e) {
                    continue;
                }
            }
            // now fill in the holes
            fillin:
                if (styleToCountToFormat.size() != FormatWidth.values().length) {
                    Map<String, PatternData> fallback = styleToCountToFormat.get(FormatWidth.SHORT);
                    if (fallback == null) {
                        fallback = styleToCountToFormat.get(FormatWidth.WIDE);
                    }
                    if (fallback == null) {
                        break fillin; // TODO use root
                    }
                    for (FormatWidth styleItem : FormatWidth.values()) {
                        Map<String, PatternData> countToFormat = styleToCountToFormat.get(styleItem);
                        if (countToFormat == null) {
                            styleToCountToFormat.put(styleItem, countToFormat = new HashMap<String, PatternData>());
                            for (Entry<String, PatternData> entry : fallback.entrySet()) {
                                countToFormat.put(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
        }
        return unitToStyleToCountToFormat;
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @SuppressWarnings("unchecked")
    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (obj instanceof Collection) {
            Collection<Measure> coll = (Collection<Measure>) obj;
            return format(toAppendTo, pos, coll.toArray(new Measure[coll.size()]));
        } else if (obj instanceof Measure[]) {
            return format(toAppendTo, pos, (Measure[]) obj);
        } else {
            return format((Measure) obj, toAppendTo, pos);
        }
    }

    /**
     * Format a general measure (type-safe).
     * @param measure the measure to format
     * @param toAppendTo as in {@link #format(Object, StringBuffer, FieldPosition)}
     * @param pos as in {@link #format(Object, StringBuffer, FieldPosition)}
     * @return passed-in buffer with appended text.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public StringBuffer format(Measure measure, StringBuffer toAppendTo, FieldPosition pos) {
        Number n = measure.getNumber();
        MeasureUnit unit = measure.getUnit();        
        UFieldPosition fpos = new UFieldPosition(pos.getFieldAttribute(), pos.getField());
        StringBuffer formattedNumber = numberFormat.format(n, new StringBuffer(), fpos);
        String keyword = rules.select(new PluralRules.FixedDecimal(n.doubleValue(), fpos.getCountVisibleFractionDigits(), fpos.getFractionDigits()));

        Map<FormatWidth, Map<String, PatternData>> styleToCountToFormat = unitToStyleToCountToFormat.get(unit);
        Map<String, PatternData> countToFormat = styleToCountToFormat.get(length);
        PatternData messagePatternData = countToFormat.get(keyword);

        toAppendTo.append(messagePatternData.prefix);
        if (messagePatternData.suffix != null) { // there is a number (may not happen with, say, Arabic dual)
            // Fix field position
            pos.setBeginIndex(fpos.getBeginIndex() + messagePatternData.prefix.length());
            pos.setEndIndex(fpos.getEndIndex() + messagePatternData.prefix.length());
            toAppendTo.append(formattedNumber);
            toAppendTo.append(messagePatternData.suffix);
        }
        return toAppendTo;
    }


    /**
     * Format a sequence of measures.
     * @param toAppendto as in {@link #format(Object, StringBuffer, FieldPosition)}
     * @param pos as in {@link #format(Object, StringBuffer, FieldPosition)}
     * @param measures a sequence of one or more measures.
     * @return passed-in buffer with appended text.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public StringBuffer format(StringBuffer toAppendto, FieldPosition pos, Measure... measures) {
        StringBuffer[] results = new StringBuffer[measures.length];
        for (int i = 0; i < measures.length; ++i) {
            results[i] = format(measures[i], new StringBuffer(), pos);
        }
        ListFormatter listFormatter = ListFormatter.getInstance(locale, 
                length == FormatWidth.WIDE ? ListFormatter.Style.DURATION : ListFormatter.Style.DURATION_SHORT);
        return toAppendto.append(listFormatter.format((Object[]) results));
    }

    /**
     * Format a sequence of measures.
     * @param measures a sequence of one or more measures.
     * @return passed-in buffer with appended text.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public String format(Measure... measures) {
        StringBuffer result = format(new StringBuffer(), new FieldPosition(0), measures);
        return result.toString();
    }

    static final class ParseData {
        transient Map<String,BitSet> prefixMap;
        transient Map<String,BitSet> suffixMap;
        transient BitSet nullSuffix;

        ParseData(ULocale locale, Map<MeasureUnit, EnumMap<FormatWidth, Map<String, PatternData>>> unitToStyleToCountToFormat) {
            prefixMap = new TreeMap<String,BitSet>(LONGEST_FIRST);
            suffixMap = new TreeMap<String,BitSet>(LONGEST_FIRST);
            nullSuffix = new BitSet();
            for (Entry<MeasureUnit, EnumMap<FormatWidth, Map<String, PatternData>>> entry3 : unitToStyleToCountToFormat.entrySet()) {
                MeasureUnit unit = entry3.getKey();
                int unitIndex = index.addItem(unit);
                for (Entry<FormatWidth, Map<String, PatternData>> entry : entry3.getValue().entrySet()) {
                    //Style style = entry.getKey();
                    for (Entry<String, PatternData> entry2 : entry.getValue().entrySet()) {
                        //String keyword = entry2.getKey();
                        PatternData data = entry2.getValue();
                        setBits(prefixMap, data.prefix, unitIndex);
                        if (data.suffix == null) {
                            nullSuffix.set(unitIndex);
                        } else {
                            setBits(suffixMap, data.suffix, unitIndex);
                        }
                    }
                }
            }
        }
        private void setBits(Map<String, BitSet> map, String string, int unitIndex) {
            BitSet bs = map.get(string);
            if (bs == null) {
                map.put(string, bs = new BitSet());
            }
            bs.set(unitIndex);
        }
        public static synchronized ParseData of(ULocale locale,
                Map<MeasureUnit, EnumMap<FormatWidth, Map<String, PatternData>>> unitToStyleToCountToFormat) {
            ParseData result = localeToParseData.get(locale);
            if (result == null) {
                localeToParseData.put(locale, result = new ParseData(locale, unitToStyleToCountToFormat));
                //                System.out.println("Prefix:\t" + result.prefixMap.size());
                //                System.out.println("Suffix:\t" + result.suffixMap.size());
            }
            return result;
        }

        private Measure parse(NumberFormat numberFormat, String toParse, ParsePosition parsePosition) {
            // TODO optimize this as necessary
            // In particular, if we've already matched a suffix and number, store that.
            // If the same suffix turns up we can jump
            int startIndex = parsePosition.getIndex();
            Number bestNumber = null;
            int bestUnit = -1;
            int longestMatch = -1;
            int furthestError = -1;
            for (Entry<String, BitSet> prefixEntry : prefixMap.entrySet()) {
                String prefix = prefixEntry.getKey();
                BitSet prefixSet = prefixEntry.getValue();
                for (Entry<String, BitSet> suffixEntry : suffixMap.entrySet()) {
                    String suffix = suffixEntry.getKey();
                    BitSet suffixSet = suffixEntry.getValue();
                    parsePosition.setIndex(startIndex);
                    if (looseMatches(prefix, toParse, parsePosition)) {
                        //                    if (nullSuffix.intersects(prefixSet))
                        ////                        // can only happen with singular rule
                        ////                        if (longestMatch < parsePosition.getIndex()) {
                        ////                            longestMatch = parsePosition.getIndex();
                        ////                            Collection<Double> samples = rules.getSamples(keyword);
                        ////                            bestNumber = samples.iterator().next();
                        ////                            bestUnit = unit;
                        ////                        }
                        //                    }
                        Number number = numberFormat.parse(toParse, parsePosition);
                        if (parsePosition.getErrorIndex() >= 0) {
                            if (furthestError < parsePosition.getErrorIndex()) {
                                furthestError = parsePosition.getErrorIndex();
                            }
                            continue;
                        }
                        if (looseMatches(suffix, toParse, parsePosition) && prefixSet.intersects(suffixSet)) {
                            if (longestMatch < parsePosition.getIndex()) {
                                longestMatch = parsePosition.getIndex();
                                bestNumber = number;
                                bestUnit = getFirst(prefixSet, suffixSet);
                            }
                        } else if (furthestError < parsePosition.getErrorIndex()) {
                            furthestError = parsePosition.getErrorIndex();
                        } 
                    } else if (furthestError < parsePosition.getErrorIndex()) {
                        furthestError = parsePosition.getErrorIndex();
                    } 

                }
            }
            if (longestMatch >= 0) {
                parsePosition.setIndex(longestMatch);
                return new Measure(bestNumber, index.getUnit(bestUnit));
            }
            parsePosition.setErrorIndex(furthestError);
            return null;
        }
    }

    static class Index<T> {
        List<T> intToItem = new ArrayList<T>();
        Map<T,Integer> itemToInt = new HashMap<T,Integer>();

        int getIndex(T item) {
            return itemToInt.get(item);
        }
        T getUnit(int index) {
            return intToItem.get(index);
        }
        int addItem(T item) {
            Integer index = itemToInt.get(item);
            if (index != null) {
                return index;
            }
            int size = intToItem.size();
            itemToInt.put(item, size);
            intToItem.add(item);
            return size;
        }
    }
    
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    public Measure parseObject(String toParse, ParsePosition parsePosition) {
        if (parseData == null) {
            parseData = ParseData.of(locale, unitToStyleToCountToFormat);
        }
        //        int index = parsePosition.getIndex();
        //        int errorIndex = parsePosition.getIndex();
        Measure result = parseData.parse(numberFormat, toParse, parsePosition);
        //        if (result == null) {
        //            parsePosition.setIndex(index);
        //            parsePosition.setErrorIndex(errorIndex);
        //            result = compatCurrencyFormat.parseCurrency(toParse, parsePosition);
        //        }
        return result;
    }


    /*
     * @param prefixSet
     * @param suffixSet
     * @return
     */
    private static int getFirst(BitSet prefixSet, BitSet suffixSet) {
        for (int i = prefixSet.nextSetBit(0); i >= 0; i = prefixSet.nextSetBit(i+1)) {
            if (suffixSet.get(i)) {
                return i;
            }
        }
        return 0;
    }

    /*
     * @param suffix
     * @param arg0
     * @param arg1
     * @return
     */
    // TODO make this lenient
    private static boolean looseMatches(String suffix, String arg0, ParsePosition arg1) {
        boolean matches = suffix.regionMatches(0, arg0, arg1.getIndex(), suffix.length());
        if (matches) {
            arg1.setErrorIndex(-1);
            arg1.setIndex(arg1.getIndex() + suffix.length());
        } else {
            arg1.setErrorIndex(arg1.getIndex());
        }
        return matches;
    }

    static final Comparator<String> LONGEST_FIRST = new Comparator<String>() {
        public int compare(String as, String bs) {
            if (as.length() > bs.length()) {
                return -1;
            }
            if (as.length() < bs.length()) {
                return 1;
            }
            return as.compareTo(bs);
        }
    };

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != GeneralMeasureFormat.class) {
            return false;
          }
        GeneralMeasureFormat other = (GeneralMeasureFormat) obj;
        return locale.equals(other.locale) 
                && length == other.length
                && numberFormat.equals(other.numberFormat);
    }
    
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return (locale.hashCode() * 37 + length.hashCode()) * 37 + numberFormat.hashCode();
    }
    
    private Object writeReplace() throws ObjectStreamException {
        return new GeneralMeasureProxy(locale, length, numberFormat);
    }

    static class GeneralMeasureProxy implements Externalizable {
        private static final long serialVersionUID = -6033308329886716770L;

        private ULocale locale;
        private FormatWidth length;
        private NumberFormat numberFormat;

        public GeneralMeasureProxy(ULocale locale, FormatWidth length, NumberFormat numberFormat) {
            this.locale = locale;
            this.length = length;
            this.numberFormat = numberFormat;
        }

        // Must have public constructor, to enable Externalizable
        public GeneralMeasureProxy() {
        }

        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeByte(0); // version
            out.writeObject(locale);
            out.writeObject(length);
            out.writeObject(numberFormat);
            out.writeShort(0); // allow for more data.
        }

        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            /* byte version = */ in.readByte(); // version
            locale = (ULocale) in.readObject();
            length = (FormatWidth) in.readObject();
            numberFormat = (NumberFormat) in.readObject();
            // allow for more data from future version
            int extra = in.readShort();
            if (extra > 0) {
                byte[] extraBytes = new byte[extra];
                in.read(extraBytes, 0, extra);
            }
        }

        private Object readResolve() throws ObjectStreamException {
            return GeneralMeasureFormat.getInstance(locale, length, numberFormat);
        }
    }
}
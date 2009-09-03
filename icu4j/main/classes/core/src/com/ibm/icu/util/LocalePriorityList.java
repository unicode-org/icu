/*
 ****************************************************************************************
 * Copyright (C) 2009, Google, Inc.; International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                         *
 ****************************************************************************************
 */

package com.ibm.icu.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.util.ULocale;

/**
 * Provides an immutable list of languages (locales) in priority order.
 * The string format is based on the Accept-Language format 
 * {@link "http://www.ietf.org/rfc/rfc2616.txt"}, such as 
 * "af, en, fr;q=0.9". Syntactically it is slightly
 * more lenient, in allowing extra whitespace between elements, extra commas,
 * and more than 3 decimals (on input), and pins between 0 and 1.
 * <p>In theory, Accept-Language indicates the relative 'quality' of each item,
 * but in practice, all of the browsers just take an ordered list, like 
 * "en, fr, de", and synthesize arbitrary quality values that put these in the
 * right order, like: "en, fr;q=0.7, de;q=0.3". The quality values in these de facto
 * semantics thus have <b>nothing</b> to do with the relative qualities of the
 * original. Accept-Language also doesn't
 * specify the interpretation of multiple instances, eg what "en, fr, en;q=.5"
 * means.
 * <p>There are various ways to build a LanguagePriorityList, such
 * as using the following equivalent patterns:
 * 
 * <pre>
 * list = LanguagePriorityList.add(&quot;af, en, fr;q=0.9&quot;).build();
 * 
 * list2 = LanguagePriorityList
 *  .add(ULocale.forString(&quot;af&quot;))
 *  .add(ULocale.ENGLISH)
 *  .add(ULocale.FRENCH, 0.9d)
 *  .build();
 * </pre>
 * When the list is built, the internal values are sorted in descending order by
 * weight, and then by input order. That is, if two languages have the same weight, the first one in the original order
 * comes first. If exactly the same language tag appears multiple times,
 * the last one wins. 
 * 
 * There are two options when building. If preserveWeights are on, then "de;q=0.3, ja;q=0.3, en, fr;q=0.7, de " would result in the following:
 * <pre> en;q=1.0
 * de;q=1.0
 * fr;q=0.7
 * ja;q=0.3</pre>
 * If it is off (the default), then all weights are reset to 1.0 after reordering. 
 * This is to match the effect of the Accept-Language semantics as used in browsers, and results in the following:
 *  * <pre> en;q=1.0
 * de;q=1.0
 * fr;q=1.0
 * ja;q=1.0</pre>
 * @author markdavis@google.com
 */
public class LocalePriorityList implements Iterable<ULocale> {
    private static final double D0 = 0.0d;
    private static final Double D1 = 1.0d;

    private static final Pattern languageSplitter = Pattern.compile("\\s*,\\s*");
    private static final Pattern weightSplitter = Pattern
    .compile("\\s*(\\S*)\\s*;\\s*q\\s*=\\s*(\\S*)");
    private final Map<ULocale, Double> languagesAndWeights;

    /**
     * Add a language code to the list being built, with weight 1.0.
     * 
     * @param languageCode locale/language to be added
     * @return internal builder, for chaining
     */
    public static LanguagePriorityListBuilder add(ULocale languageCode) {
        return new LanguagePriorityListBuilder().add(languageCode);
    }

    /**
     * Add a language code to the list being built, with specified weight.
     * 
     * @param languageCode locale/language to be added
     * @param weight value from 0.0 to 1.0
     * @return internal builder, for chaining
     */
    public static LanguagePriorityListBuilder add(ULocale languageCode, final double weight) {
        return new LanguagePriorityListBuilder().add(languageCode, weight);
    }

    /**
     * Add a language priority list.
     * 
     * @param languagePriorityList list to add all the members of
     * @return internal builder, for chaining
     */
    public static LanguagePriorityListBuilder add(LocalePriorityList languagePriorityList) {
        return new LanguagePriorityListBuilder().add(languagePriorityList);
    }

    /**
     * Add language codes to the list being built, using a string in rfc2616
     * (lenient) format, where each language is a valid {@link ULocale}.
     * 
     * @param acceptLanguageString String in rfc2616 format (but leniently parsed)
     * @return internal builder, for chaining
     */
    public static LanguagePriorityListBuilder add(String acceptLanguageString) {
        return new LanguagePriorityListBuilder().add(acceptLanguageString);
    }

    /**
     * Return the weight for a given language, or null if there is none. Note that
     * the weights may be adjusted from those used to build the list.
     * 
     * @param language to get weight of
     * @return weight
     */
    public Double getWeight(ULocale language) {
        return languagesAndWeights.get(language);
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        for (final ULocale language : languagesAndWeights.keySet()) {
            if (result.length() != 0) {
                result.append(", ");
            }
            result.append(language);
            double weight = languagesAndWeights.get(language);
            if (weight != D1) {
                result.append(";q=").append(weight);
            }
        }
        return result.toString();
    }

    public Iterator<ULocale> iterator() {
        return languagesAndWeights.keySet().iterator();
    }

    @Override
    public boolean equals(final Object o) {
        try {
            final LocalePriorityList that = (LocalePriorityList) o;
            return languagesAndWeights.equals(that.languagesAndWeights);
        } catch (final RuntimeException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return languagesAndWeights.hashCode();
    }

    // ==================== Privates ====================


    private LocalePriorityList(final Map<ULocale, Double> languageToWeight) {
        this.languagesAndWeights = languageToWeight;
    }

    /**
     * Internal class used for building LanguagePriorityLists
     */
    static class LanguagePriorityListBuilder {
        /**
         * These store the input languages and weights, in chronological order,
         * where later additions override previous ones.
         */
        private final Map<ULocale, Double> languageToWeight 
        = new LinkedHashMap<ULocale, Double>();

        public LocalePriorityList build() {
            return build(false);
        }

        public LocalePriorityList build(boolean preserveWeights) {
            // Walk through the input list, collecting the items with the same weights.
            final Map<Double, Set<ULocale>> doubleCheck = new TreeMap<Double, Set<ULocale>>(
                    myDescendingDouble);
            for (final ULocale lang : languageToWeight.keySet()) {
                Double weight = languageToWeight.get(lang);
                Set<ULocale> s = doubleCheck.get(weight);
                if (s == null) {
                    doubleCheck.put(weight, s = new LinkedHashSet<ULocale>());
                }
                s.add(lang);
            }
            // We now have a bunch of items sorted by weight, then chronologically.
            // We can now create a list in the right order
            final Map<ULocale, Double> temp = new LinkedHashMap<ULocale, Double>();
            for (final Double weight : doubleCheck.keySet()) {
                for (final ULocale lang : doubleCheck.get(weight)) {
                    temp.put(lang, preserveWeights ? weight : D1);
                }
            }
            return new LocalePriorityList(Collections.unmodifiableMap(temp));
        }

        public LanguagePriorityListBuilder add(
                final LocalePriorityList languagePriorityList) {
            for (final ULocale language : languagePriorityList.languagesAndWeights
                    .keySet()) {
                add(language, languagePriorityList.languagesAndWeights.get(language));
            }
            return this;
        }

        /**
         * Adds a new language code, with weight = 1.0.
         * 
         * @param languageCode to add with weight 1.0
         * @return this, for chaining
         */
        public LanguagePriorityListBuilder add(final ULocale languageCode) {
            return add(languageCode, D1);
        }

        /**
         * Adds language codes, with each having weight = 1.0.
         * 
         * @param languageCodes List of language codes.
         * @return this, for chaining.
         */
        public LanguagePriorityListBuilder add(ULocale... languageCodes) {
            for (final ULocale languageCode : languageCodes) {
                add(languageCode, D1);
            }
            return this;
        }

        /**
         * Adds a new supported languageCode, with specified weight. Overrides any
         * previous weight for the language.
         * 
         * @param languageCode language/locale to add
         * @param weight value between 0.0 and 1.1
         * @return this, for chaining.
         */
        public LanguagePriorityListBuilder add(final ULocale languageCode,
                double weight) {
            if (languageToWeight.containsKey(languageCode)) {
                languageToWeight.remove(languageCode);
            }
            if (weight <= D0) {
                return this; // skip zeros
            } else if (weight > D1) {
                weight = D1;
            }
            languageToWeight.put(languageCode, weight);
            return this;
        }

        /**
         * Adds rfc2616 list.
         * 
         * @param acceptLanguageList in rfc2616 format
         * @return this, for chaining.
         */
        public LanguagePriorityListBuilder add(final String acceptLanguageList) {
            final String[] items = languageSplitter.split(acceptLanguageList.trim());
            final Matcher itemMatcher = weightSplitter.matcher("");
            for (final String item : items) {
                if (itemMatcher.reset(item).matches()) {
                    final ULocale language = new ULocale(itemMatcher.group(1));
                    final double weight = Double.parseDouble(itemMatcher.group(2));
                    if (!(weight >= D0 && weight <= D1)) { // do ! for NaN
                        throw new IllegalArgumentException("Illegal weight, must be 0..1: "
                                + weight);
                    }
                    add(language, weight);
                } else if (item.length() != 0) {
                    add(new ULocale(item));
                }
            }
            return this;
        }
    }

    private static Comparator<Double> myDescendingDouble = new Comparator<Double>() {
        public int compare(Double o1, Double o2) {
            return -o1.compareTo(o2);
        }
    };
}

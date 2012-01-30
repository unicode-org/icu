/*
 *******************************************************************************
 * Copyright (C) 2008-2012, Google Inc, International Business Machines Corporation
 * and others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.AlphabeticIndex.Bucket;
import com.ibm.icu.text.AlphabeticIndex.Bucket.LabelType;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;

/**
 * AlphabeticIndex supports the creation of a UI index appropriate for a given language. It can support either direct
 * use, or use with a client that doesn't support localized collation. The following is an example of what an index
 * might look like in a UI:
 * 
 * <pre>
 *  <b>... A B C D E F G H I J K L M N O P Q R S T U V W X Y Z  ...</b>
 *  
 *  <b>A</b>
 *     Addison
 *     Albertson
 *     Azensky
 *  <b>B</b>
 *     Baecker
 *  ...
 * </pre>
 * 
 * The class can generate a list of labels for use as a UI "index", that is, a list of clickable characters (or
 * character sequences) that allow the user to see a segment (bucket) of a larger "target" list. That is, each label
 * corresponds to a bucket in the target list, where everything in the bucket is greater than or equal to the character
 * (according to the locale's collation). Strings can be added to the index; they will be in sorted order in the right
 * bucket.</p>
 * <p>
 * The class also supports having buckets for strings before the first (underflow), after the last (overflow), and
 * between scripts (inflow). For example, if the index is constructed with labels for Russian and English, Greek
 * characters would fall into an inflow bucket between the other two scripts.</p>
 * 
 * <p><em>Note:</em> If you expect to have a lot of ASCII or Latin characters as well as characters from the user's language, then it is a good idea to call addLabels(ULocale.English).</p>
 * 
 * <h2>Direct Use</h2>
 * <p>The following shows an example of building an index directly.
 *  The "show..." methods below are just to illustrate usage.
 * 
 * <pre>
 * // Create a simple index where the values for the strings are Integers, and add the strings
 * 
 * AlphabeticIndex<Integer> index = new AlphabeticIndex<Integer>(desiredLocale).addLabels(additionalLocale);
 * int counter = 0;
 * for (String item : test) {
 *     index.addRecord(item, counter++); 
 * }
 * ...
 * // Show index at top. We could skip or gray out empty buckets
 * 
 * for (AlphabeticIndex.Bucket<Integer> bucket : index) {
 *     if (showAll || bucket.size() != 0) {
 *         showLabelAtTop(UI, bucket.getLabel());
 *     }
 * }
 *  ...
 * // Show the buckets with their contents, skipping empty buckets
 * 
 * for (AlphabeticIndex.Bucket<Integer> bucket : index) {
 *     if (bucket.size() != 0) {
 *         showLabelInList(UI, bucket.getLabel());
 *         for (AlphabeticIndex.Record<Integer> item : bucket) {
 *             showIndexedItem(UI, item.getName(), item.getData());
 *         }
 * </pre>
 * 
 * The caller can build different UIs using this class. For example, an index character could be omitted or grayed-out
 * if its bucket is empty. Small buckets could also be combined based on size, such as:
 * 
 * <pre>
 * <b>... A-F G-N O-Z ...</b>
 * </pre>
 * 
 * <h2>Client Support</h2>
 * <p>
 * Callers can also use the AlphabeticIndex to support sorting on a client that doesn't support collation.
 * <ul>
 * <li>getLabels() can be used to get a list of the labels, such as "...", "A", "B",..., and send that list to the client.
 * </li>
 * <li>When the client has a new name, it sends that name to the server. The server needs to call the following methods,
 * and communicate the bucketIndex and collationKey back to the client.
 * 
 * <pre>
 * int bucketIndex = alphabeticIndex.getBucketIndex(name);
 * RawCollationKey collationKey = collator.getRawCollationKey(name, null);
 * </pre>
 * 
 * <li>The client would put the name (and associated information) into its bucket for bucketIndex. The collationKey is a
 * sequence of bytes that can be compared with a binary compare, and produce the right localized result.</li>
 * </ul>
 * 
 * <p>
 * <b>Notes:</b>
 * <ul>
 * <li>Additional collation parameters can be passed in as part of the locale name. For example, German plus numeric
 * sorting would be "de@kn-true".
 * 
 * @author markdavis
 * @stable ICU 4.8
 */
public final class AlphabeticIndex<V> implements Iterable<Bucket<V>> {

    /**
     * Internals
     */
    static final boolean HACK_CODED_FIRSTS = true;

    private static UnicodeSet UNIHAN = new UnicodeSet("[:script=Hani:]").freeze();

    static final String BASE = "\uFDD0";
    // these are generated. Later, get from CLDR data.

    static final UnicodeSet PINYIN_LABELS = new UnicodeSet("[A-Z{\uFDD0A}{\uFDD0B}{\uFDD0C}{\uFDD0D}{\uFDD0E}{\uFDD0F}{\uFDD0G}{\uFDD0H}{\uFDD0I}{\uFDD0J}{\uFDD0K}{\uFDD0L}{\uFDD0M}{\uFDD0N}{\uFDD0O}{\uFDD0P}{\uFDD0Q}{\uFDD0R}{\uFDD0S}{\uFDD0T}{\uFDD0U}{\uFDD0V}{\uFDD0W}{\uFDD0X}{\uFDD0Y}{\uFDD0Z}]").freeze();
    static final UnicodeSet STROKE_LABELS = new UnicodeSet("[{\uFDD0\u2801}{\uFDD0\u2802}{\uFDD0\u2803}{\uFDD0\u2804}{\uFDD0\u2805}{\uFDD0\u2806}{\uFDD0\u2807}{\uFDD0\u2808}{\uFDD0\u2809}{\uFDD0\u280A}{\uFDD0\u280B}{\uFDD0\u280C}{\uFDD0\u280D}{\uFDD0\u280E}{\uFDD0\u280F}{\uFDD0\u2810}{\uFDD0\u2811}{\uFDD0\u2812}{\uFDD0\u2813}{\uFDD0\u2814}{\uFDD0\u2815}{\uFDD0\u2816}{\uFDD0\u2817}{\uFDD0\u2818}{\uFDD0\u2819}{\uFDD0\u281A}{\uFDD0\u281B}{\uFDD0\u281C}{\uFDD0\u281D}{\uFDD0\u281E}{\uFDD0\u281F}{\uFDD0\u2820}{\uFDD0\u2821}{\uFDD0\u2822}{\uFDD0\u2823}{\uFDD0\u2824}{\uFDD0\u2825}{\uFDD0\u2826}{\uFDD0\u2827}{\uFDD0\u2828}{\uFDD0\u2829}{\uFDD0\u282A}{\uFDD0\u282B}{\uFDD0\u282C}{\uFDD0\u282E}{\uFDD0\u2830}{\uFDD0\u2834}{\uFDD0\u2840}]").freeze();
    static final UnicodeSet RADICAL_LABELS = new UnicodeSet("[{\uFDD0\u2E80}{\uFDD0\u2E81}{\uFDD0\u2E84}{\uFDD0\u2E85}{\uFDD0\u2E86}{\uFDD0\u2E87}{\uFDD0\u2E88}{\uFDD0\u2E8A}{\uFDD0\u2E8B}{\uFDD0\u2E8C}{\uFDD0\u2E91}{\uFDD0\u2E92}{\uFDD0\u2E93}{\uFDD0\u2E95}{\uFDD0\u2E97}{\uFDD0\u2E98}{\uFDD0\u2E99}{\uFDD0\u2E9B}{\uFDD0\u2E9D}{\uFDD0\u2E9E}{\uFDD0\u2E9F}{\uFDD0\u2EA0}{\uFDD0\u2EA2}{\uFDD0\u2EA3}{\uFDD0\u2EA4}{\uFDD0\u2EA7}{\uFDD0\u2EA8}{\uFDD0\u2EA9}{\uFDD0\u2EAA}{\uFDD0\u2EAB}{\uFDD0\u2EAC}{\uFDD0\u2EAE}{\uFDD0\u2EAF}{\uFDD0\u2EB0}{\uFDD0\u2EB4}{\uFDD0\u2EB8}{\uFDD0\u2EB9}{\uFDD0\u2EBB}{\uFDD0\u2EBC}{\uFDD0\u2EBD}{\uFDD0\u2EC0}{\uFDD0\u2EC1}{\uFDD0\u2EC2}{\uFDD0\u2EC3}{\uFDD0\u2EC5}{\uFDD0\u2EC6}{\uFDD0\u2EC8}{\uFDD0\u2EC9}{\uFDD0\u2ECA}{\uFDD0\u2ECB}{\uFDD0\u2ECF}{\uFDD0\u2ED0}{\uFDD0\u2ED1}{\uFDD0\u2ED3}{\uFDD0\u2ED4}{\uFDD0\u2ED6}{\uFDD0\u2ED7}{\uFDD0\u2ED8}{\uFDD0\u2ED9}{\uFDD0\u2EDA}{\uFDD0\u2EDB}{\uFDD0\u2EDC}{\uFDD0\u2EDD}{\uFDD0\u2EE0}{\uFDD0\u2EE1}{\uFDD0\u2EE2}{\uFDD0\u2EE3}{\uFDD0\u2EE4}{\uFDD0\u2EE5}{\uFDD0\u2EE6}{\uFDD0\u2EE7}{\uFDD0\u2EE8}{\uFDD0\u2EEA}{\uFDD0\u2EEB}{\uFDD0\u2EED}{\uFDD0\u2EEE}{\uFDD0\u2EEF}{\uFDD0\u2EF0}{\uFDD0\u2EF2}{\uFDD0\u2EF3}{\uFDD0\u2F00}{\uFDD0\u2F01}{\uFDD0\u2F02}{\uFDD0\u2F03}{\uFDD0\u2F05}{\uFDD0\u2F06}{\uFDD0\u2F07}{\uFDD0\u2F09}{\uFDD0\u2F0A}{\uFDD0\u2F0B}{\uFDD0\u2F0D}{\uFDD0\u2F0E}{\uFDD0\u2F10}{\uFDD0\u2F12}{\uFDD0\u2F13}{\uFDD0\u2F14}{\uFDD0\u2F15}{\uFDD0\u2F16}{\uFDD0\u2F17}{\uFDD0\u2F1B}{\uFDD0\u2F1D}{\uFDD0\u2F1E}{\uFDD0\u2F1F}{\uFDD0\u2F20}{\uFDD0\u2F21}{\uFDD0\u2F22}{\uFDD0\u2F23}{\uFDD0\u2F24}{\uFDD0\u2F25}{\uFDD0\u2F26}{\uFDD0\u2F27}{\uFDD0\u2F28}{\uFDD0\u2F2B}{\uFDD0\u2F2C}{\uFDD0\u2F2D}{\uFDD0\u2F2E}{\uFDD0\u2F2F}{\uFDD0\u2F31}{\uFDD0\u2F32}{\uFDD0\u2F34}{\uFDD0\u2F35}{\uFDD0\u2F36}{\uFDD0\u2F37}{\uFDD0\u2F38}{\uFDD0\u2F3A}{\uFDD0\u2F3B}{\uFDD0\u2F3D}{\uFDD0\u2F3E}{\uFDD0\u2F40}{\uFDD0\u2F42}{\uFDD0\u2F43}{\uFDD0\u2F44}{\uFDD0\u2F45}{\uFDD0\u2F46}{\uFDD0\u2F48}{\uFDD0\u2F4A}{\uFDD0\u2F4B}{\uFDD0\u2F4C}{\uFDD0\u2F4E}{\uFDD0\u2F50}{\uFDD0\u2F51}{\uFDD0\u2F53}{\uFDD0\u2F57}{\uFDD0\u2F58}{\uFDD0\u2F59}{\uFDD0\u2F5A}{\uFDD0\u2F5B}{\uFDD0\u2F5E}{\uFDD0\u2F60}{\uFDD0\u2F61}{\uFDD0\u2F62}{\uFDD0\u2F63}{\uFDD0\u2F64}{\uFDD0\u2F65}{\uFDD0\u2F67}{\uFDD0\u2F68}{\uFDD0\u2F69}{\uFDD0\u2F6A}{\uFDD0\u2F6B}{\uFDD0\u2F6D}{\uFDD0\u2F6E}{\uFDD0\u2F6F}{\uFDD0\u2F71}{\uFDD0\u2F72}{\uFDD0\u2F73}{\uFDD0\u2F74}{\uFDD0\u2F76}{\uFDD0\u2F78}{\uFDD0\u2F7B}{\uFDD0\u2F7D}{\uFDD0\u2F7E}{\uFDD0\u2F7F}{\uFDD0\u2F82}{\uFDD0\u2F83}{\uFDD0\u2F84}{\uFDD0\u2F86}{\uFDD0\u2F87}{\uFDD0\u2F88}{\uFDD0\u2F89}{\uFDD0\u2F8A}{\uFDD0\u2F8D}{\uFDD0\u2F8E}{\uFDD0\u2F8F}{\uFDD0\u2F92}{\uFDD0\u2F94}{\uFDD0\u2F95}{\uFDD0\u2F96}{\uFDD0\u2F97}{\uFDD0\u2F98}{\uFDD0\u2F99}{\uFDD0\u2F9A}{\uFDD0\u2F9B}{\uFDD0\u2F9D}{\uFDD0\u2F9E}{\uFDD0\u2F9F}{\uFDD0\u2FA0}{\uFDD0\u2FA1}{\uFDD0\u2FA3}{\uFDD0\u2FA4}{\uFDD0\u2FA5}{\uFDD0\u2FA6}{\uFDD0\u2FA8}{\uFDD0\u2FAA}{\uFDD0\u2FAB}{\uFDD0\u2FAE}{\uFDD0\u2FAF}{\uFDD0\u2FB0}{\uFDD0\u2FB1}{\uFDD0\u2FB2}{\uFDD0\u2FB3}{\uFDD0\u2FB4}{\uFDD0\u2FB5}{\uFDD0\u2FB6}{\uFDD0\u2FB9}{\uFDD0\u2FBA}{\uFDD0\u2FBC}{\uFDD0\u2FBD}{\uFDD0\u2FBE}{\uFDD0\u2FBF}{\uFDD0\u2FC0}{\uFDD0\u2FC2}{\uFDD0\u2FC3}{\uFDD0\u2FC4}{\uFDD0\u2FC5}{\uFDD0\u2FC6}{\uFDD0\u2FC7}{\uFDD0\u2FC8}{\uFDD0\u2FC9}{\uFDD0\u2FCA}{\uFDD0\u2FCB}{\uFDD0\u2FCC}{\uFDD0\u2FCD}{\uFDD0\u2FCE}{\uFDD0\u2FCF}{\uFDD0\u2FD0}{\uFDD0\u2FD1}{\uFDD0\u2FD5}]").freeze();
    static final List<String> PROBES = Arrays.asList("\u4E00", "\uFDD0A", "\uFDD0\u2801", "\uFDD0\u2E80");
    static final int PINYIN_PROBE_INDEX = 1;
    static final UnicodeSet[] MATCHING = {null, PINYIN_LABELS, STROKE_LABELS, RADICAL_LABELS};

    private static final char CGJ = '\u034F';
    private static final UnicodeSet ALPHABETIC = new UnicodeSet("[[:alphabetic:]-[:mark:]]").add(BASE).freeze();
    private static final UnicodeSet HANGUL = new UnicodeSet(
    "[\uAC00 \uB098 \uB2E4 \uB77C \uB9C8 \uBC14  \uC0AC  \uC544 \uC790  \uCC28 \uCE74 \uD0C0 \uD30C \uD558]").freeze();
    private static final UnicodeSet ETHIOPIC = new UnicodeSet("[[:Block=Ethiopic:]&[:Script=Ethiopic:]]").freeze();
    private static final UnicodeSet CORE_LATIN = new UnicodeSet("[a-z]").freeze();

    private final RuleBasedCollator collatorOriginal;
    private final RuleBasedCollator collatorPrimaryOnly;
    private RuleBasedCollator collatorExternal;

    // for testing
    private final LinkedHashMap<String, Set<String>> alreadyIn = new LinkedHashMap<String, Set<String>>();
    private final List<String> noDistinctSorting = new ArrayList<String>();
    private final List<String> notAlphabetic = new ArrayList<String>();

    // We accumulate these as we build up the input parameters

    private final UnicodeSet initialLabels = new UnicodeSet();
    private final Collection<Record<V>> inputList = new ArrayList<Record<V>>();

    // Lazy evaluated: null means that we have not built yet.

    private BucketList buckets;

    private String overflowLabel = "\u2026";
    private String underflowLabel = "\u2026";
    private String inflowLabel = "\u2026";
    private boolean hasPinyin;

    /**
     * Create the index object.
     * 
     * @param locale
     *            The locale for the index.
     * @stable ICU 4.8
     */
    public AlphabeticIndex(ULocale locale) {
        this(locale, null, null);
    }

    /**
     * Create the index object.
     * 
     * @param locale
     *            The locale for the index.
     * @stable ICU 4.8
     */
    public AlphabeticIndex(Locale locale) {
        this(ULocale.forLocale(locale));
    }

    //    /**
    //     * @internal
    //     * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
    //     */
    //    public enum LangType { 
    //        /**
    //         * @internal
    //         * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
    //         */
    //        NORMAL, 
    //        /**
    //         * @internal
    //         * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
    //         */
    //        SIMPLIFIED,
    //        /**
    //         * @internal
    //         * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
    //         */
    //        TRADITIONAL;
    //        /**
    //         * @internal
    //         * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
    //         */
    //        public static LangType fromLocale(ULocale locale) {
    //            String lang = locale.getLanguage();
    //            if (lang.equals("zh")) {
    //                if ("Hant".equals(locale.getScript()) || "TW".equals(locale.getCountry())) {
    //                    return TRADITIONAL;
    //                }
    //                return SIMPLIFIED;
    //            }
    //            return NORMAL;
    //        }
    //    }

    /**
     * @internal
     * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
     */
    public AlphabeticIndex(ULocale locale, RuleBasedCollator collator, UnicodeSet exemplarChars) {
        //        langType = LangType.fromLocale(locale);
        //        // HACK because we have to know the type of the collation for Chinese
        //        if (langType != LangType.NORMAL) {
        //            locale = locale.setKeywordValue("collation", langType == LangType.TRADITIONAL ? "stroke" : "pinyin");
        //        }
        hasPinyin = false;
        collatorOriginal = collator != null ? collator : (RuleBasedCollator) Collator.getInstance(locale);
        try {
            collatorPrimaryOnly = (RuleBasedCollator) (collatorOriginal.clone());
        } catch (Exception e) {
            // should never happen
            throw new IllegalStateException("Collator cannot be cloned", e);
        }
        collatorPrimaryOnly.setStrength(Collator.PRIMARY);
        if (exemplarChars == null) {
            exemplarChars = getIndexExemplars(locale);
        }
        addLabels(exemplarChars);
    }

    /**
     * Add more index characters (aside from what are in the locale)
     * @param additions additional characters to add to the index, such as A-Z.
     * @return this, for chaining
     * @stable ICU 4.8
     */
    public AlphabeticIndex<V> addLabels(UnicodeSet additions) {
        initialLabels.addAll(additions);
        buckets = null;
        return this;
    }

    /**
     * Add more index characters (aside from what are in the locale)
     * @param additions additional characters to add to the index, such as those in Swedish.
     * @return this, for chaining
     * @stable ICU 4.8
     */
    public AlphabeticIndex<V> addLabels(ULocale... additions) {
        for (ULocale addition : additions) {
            initialLabels.addAll(getIndexExemplars(addition));
        }
        buckets = null;
        return this;
    }

    /**
     * Add more index characters (aside from what are in the locale)
     * @param additions additional characters to add to the index, such as those in Swedish.
     * @return this, for chaining
     * @stable ICU 4.8
     */
    public AlphabeticIndex<V> addLabels(Locale... additions) {
        for (Locale addition : additions) {
            initialLabels.addAll(getIndexExemplars(ULocale.forLocale(addition)));
        }
        buckets = null;
        return this;
    }

    /**
     * Set the overflow label
     * @param overflowLabel see class description
     * @return this, for chaining
     * @stable ICU 4.8
     */
    public AlphabeticIndex<V> setOverflowLabel(String overflowLabel) {
        this.overflowLabel = overflowLabel;
        return this;
    }

    /**
     * Get the default label used in the IndexCharacters' locale for underflow, eg the last item in: X Y Z ...
     * 
     * @return underflow label
     * @stable ICU 4.8
     */
    public String getUnderflowLabel() {
        return underflowLabel; // TODO get localized version
    }


    /**
     * Set the underflowLabel label
     * @param underflowLabel see class description
     * @return this, for chaining
     * @stable ICU 4.8
     */
    public AlphabeticIndex<V> setUnderflowLabel(String underflowLabel) {
        this.underflowLabel = underflowLabel;
        return this;
    }

    /**
     * Get the default label used in the IndexCharacters' locale for overflow, eg the first item in: ... A B C
     * 
     * @return overflow label
     * @stable ICU 4.8
     */
    public String getOverflowLabel() {
        return overflowLabel; // TODO get localized version
    }


    /**
     * Set the inflowLabel label
     * @param inflowLabel see class description
     * @return this, for chaining
     * @stable ICU 4.8
     */
    public AlphabeticIndex<V> setInflowLabel(String inflowLabel) {
        this.inflowLabel = inflowLabel;
        return this;
    }

    /**
     * Get the default label used for abbreviated buckets <i>between</i> other labels. For example, consider the labels
     * for Latin and Greek are used: X Y Z ... &#x0391; &#x0392; &#x0393;.
     * 
     * @return inflow label
     * @stable ICU 4.8
     */
    public String getInflowLabel() {
        return inflowLabel; // TODO get localized version
    }


    /**
     * Get the limit on the number of labels in the index. The number of buckets can be slightly larger: see getBucketCount().
     * 
     * @return maxLabelCount maximum number of labels.
     * @stable ICU 4.8
     */
    public int getMaxLabelCount() {
        return maxLabelCount;
    }

    /**
     * Set a limit on the number of labels in the index. The number of buckets can be slightly larger: see
     * getBucketCount().
     * 
     * @return maxLabelCount label Set the maximum number of labels. Currently, if the number is exceeded, then every
     *         nth item is removed to bring the count down. A more sophisticated mechanism may be available in the
     *         future.
     * @stable ICU 4.8
     */
    public AlphabeticIndex<V> setMaxLabelCount(int maxLabelCount) {
        this.maxLabelCount = maxLabelCount;
        return this;
    }

    /**
     * Determine the best labels to use. This is based on the exemplars, but we also process to make sure that they are unique,
     * and sort differently, and that the overall list is small enough.
     * @return 
     */
    private ArrayList<String> initLabels() {
        UnicodeSet exemplars = new UnicodeSet(initialLabels);

        // First sort them, with an "best" ordering among items that are the same according
        // to the collator.
        // Re the warning: the JDK inexplicably didn't make Collators be Comparator<String>!
        @SuppressWarnings("unchecked")
        Set<String> preferenceSorting = new TreeSet<String>(new MultiComparator<Object>(collatorPrimaryOnly, PREFERENCE_COMPARATOR));
        exemplars.addAllTo(preferenceSorting);

        TreeSet<String> indexCharacterSet = new TreeSet<String>(collatorPrimaryOnly);

        // We nw make a sorted array of elements
        // Some of the input may, however, be redundant.
        // That is, we might have c, ch, d, where "ch" sorts just like "c", "h"
        // So we make a pass through, filtering out those cases.

        for (String item : preferenceSorting) {
            if (indexCharacterSet.contains(item)) {
                for (String itemAlreadyIn : indexCharacterSet) {
                    if (collatorPrimaryOnly.compare(item, itemAlreadyIn) == 0) {
                        Set<String> targets = alreadyIn.get(itemAlreadyIn);
                        if (targets == null) {
                            alreadyIn.put(itemAlreadyIn, targets = new LinkedHashSet<String>());
                        }
                        targets.add(item);
                        break;
                    }
                }
            } else if (UTF16.countCodePoint(item) > 1 && collatorPrimaryOnly.compare(item, separated(item)) == 0) {
                noDistinctSorting.add(item);
            } else if (!ALPHABETIC.containsSome(item)) {
                notAlphabetic.add(item);
            } else {
                indexCharacterSet.add(item);
            }
        }

        // if the result is still too large, cut down to maxCount elements, by removing every nth element

        final int size = indexCharacterSet.size() - 1;
        if (size > maxLabelCount) {
            int count = 0;
            int old = -1;
            for (Iterator<String> it = indexCharacterSet.iterator(); it.hasNext();) {
                ++count;
                it.next();
                final int bump = count * maxLabelCount / size;
                if (bump == old) {
                    it.remove();
                } else {
                    old = bump;
                }
            }
        }

        return new ArrayList<String>(indexCharacterSet);
    }

    /**
     * This method is called to get the index exemplars. Normally these come from the locale directly,
     * but if they aren't available, we have to synthesize them.
     * @param locale
     * @return
     */
    private UnicodeSet getIndexExemplars(ULocale locale) {
        UnicodeSet exemplars;

        exemplars = LocaleData.getExemplarSet(locale, 0, LocaleData.ES_INDEX);
        if (exemplars != null) {
            // HACK      
            final String language = locale.getLanguage();
            if (language.equals("zh") || language.equals("ja") || language.equals("ko")) {
                // find out which one we are using
                TreeSet<String> probeSet = new TreeSet<String>(collatorOriginal);

                //                UnicodeSet tailored = collatorOriginal.getTailoredSet();
                //                tailored.addAllTo(probeSet);
                //                System.out.println(probeSet);
                //                probeSet.clear();

                probeSet.addAll(PROBES);
                String first = probeSet.iterator().next();
                int location = PROBES.indexOf(first);
                if (location > 0) {
                    if (location == PINYIN_PROBE_INDEX) {
                        hasPinyin = true;
                    }
                    exemplars.clear().addAll(MATCHING[location]);
                }
            }
            //            LangType langType2 = LangType.fromLocale(locale);
            //            if (langType2 == LangType.TRADITIONAL) {
            //                Collator collator = Collator.getInstance(locale);
            //                if (collator.getTailoredSet().contains(probeCharInLongStroke)) {
            //                    exemplars = HACK_LONG_TRAD_EXEMPLARS;
            //                } else {
            //                    exemplars = HACK_SHORT_TRAD_EXEMPLARS;
            //                }
            //                return exemplars;
            //            }
            return exemplars;
        }

        // Synthesize the index exemplars

        exemplars = LocaleData.getExemplarSet(locale, 0, LocaleData.ES_STANDARD);

        // get the exemplars, and handle special cases

        exemplars = exemplars.cloneAsThawed();
        // question: should we add auxiliary exemplars?
        if (exemplars.containsSome(CORE_LATIN) || exemplars.size() == 0) {
            exemplars.addAll(CORE_LATIN);
        }
        if (exemplars.containsSome(HANGUL)) {
            // cut down to small list
            exemplars.removeAll(new UnicodeSet("[:block=hangul_syllables:]")).addAll(HANGUL);
        }
        if (exemplars.containsSome(ETHIOPIC)) {
            // cut down to small list
            // make use of the fact that Ethiopic is allocated in 8's, where
            // the base is 0 mod 8.
            for (UnicodeSetIterator it = new UnicodeSetIterator(ETHIOPIC); it.next();) {
                if ((it.codepoint & 0x7) != 0) {
                    exemplars.remove(it.codepoint);
                }
            }
        }

        UnicodeSet uppercased = new UnicodeSet();
        for (String item : exemplars) {
            uppercased.add(UCharacter.toUpperCase(locale, item));
        }

        return uppercased;
    }

    /**
     * Return the string with interspersed CGJs. Input must have more than 2 codepoints.
     * <p>This is used to test whether contractions sort differently from their components.
     */
    private String separated(String item) {
        StringBuilder result = new StringBuilder();
        // add a CGJ except within surrogates
        char last = item.charAt(0);
        result.append(last);
        for (int i = 1; i < item.length(); ++i) {
            char ch = item.charAt(i);
            if (!UCharacter.isHighSurrogate(last) || !UCharacter.isLowSurrogate(ch)) {
                result.append(CGJ);
            }
            result.append(ch);
            last = ch;
        }
        return result.toString();
    }

    /**
     * Get the labels.
     * 
     * @return A collection listing the labels, after processing.
     * @stable ICU 4.8
     */
    public List<String> getBucketLabels() {
        if (buckets == null) {
            initBuckets();
        }
        ArrayList<String> result = new ArrayList<String>();
        for (Bucket<V> bucket : buckets) {
            result.add(bucket.getLabel());
        }
        return result;
    }

    /**
     * Get a clone of the collator used internally. Note that for performance reasons, the clone is only done once, and
     * then stored. The next time it is accessed, the same instance is returned.
     * <p>
     * <b><i>Don't use this method across threads if you are changing the settings on the collator, at least not without
     * synchronizing.</i></b>
     * 
     * @return a clone of the collator used internally
     * @stable ICU 4.8
     */
    public RuleBasedCollator getCollator() {
        if (collatorExternal == null) {
            try {
                collatorExternal = (RuleBasedCollator) (collatorOriginal.clone());
            } catch (Exception e) {
                // should never happen
                throw new IllegalStateException("Collator cannot be cloned", e);
            }
        }
        return collatorExternal;
    }

    /**
     * Add a record (name and data) to the index. The name will be used to sort the items into buckets, and to sort
     * within the bucket. Two records may have the same name. When they do, the sort order is according to the order added:
     * the first added comes first.
     * 
     * @param name
     *            Name, such as a name
     * @param data
     *            Data, such as an address or link
     * @return this, for chaining
     * @stable ICU 4.8
     */
    public AlphabeticIndex<V> addRecord(CharSequence name, V data) {
        // TODO instead of invalidating, just add to unprocessed list.
        buckets = null; // invalidate old bucketlist
        inputList.add(new Record<V>(name, data, inputList.size()));
        return this;
    }

    /**
     * Get the bucket number for the given name. This routine permits callers to implement their own bucket handling
     * mechanisms, including client-server handling. For example, when a new name is created on the client, it can ask
     * the server for the bucket for that name, and the sortkey (using getCollator). Once the client has that
     * information, it can put the name into the right bucket, and sort it within that bucket, without having access to
     * the index or collator.
     * <p>
     * Note that the bucket number (and sort key) are only valid for the settings of the current AlphabeticIndex; if
     * those are changed, then the bucket number and sort key must be regenerated.
     * 
     * @param name
     *            Name, such as a name
     * @return this, for chaining
     * @stable ICU 4.8
     */
    public int getBucketIndex(CharSequence name) {
        if (buckets == null) {
            initBuckets();
        }
        //        if (langType == LangType.SIMPLIFIED) {
        //            String hackPrefix = hackName(name, collatorPrimaryOnly);
        //            if (hackPrefix != null) {
        //                name = hackPrefix + name;
        //            }
        //        }
        return rawGetBucketIndex(name);
    }

    private int rawGetBucketIndex(CharSequence name) {
        // TODO use a binary search
        int result = 0;
        Bucket<V> lastBucket = null;
        Bucket<V> bucket = null;
        for (Iterator<Bucket<V>> it = buckets.fullIterator(); it.hasNext();) {
            bucket = it.next();
            if (bucket.lowerBoundary == null) { // last bucket
                bucket = lastBucket; // back up the bucket
                --result;
                break;
            }
            int bucketLower2name = collatorPrimaryOnly.compare(bucket.lowerBoundary, name);
            if (bucketLower2name > 0) { // the first boundary is always "", and so -1 will never be returned
                bucket = lastBucket; // back up the bucket
                --result;
                break;
            } else if (bucketLower2name == 0) {
                break;
            }
            result++;
            lastBucket = bucket;
        }
        // we will always have at least one bucket
        // see if we need to remap
        if (buckets.rebucket != null) {
            Bucket<V> temp = buckets.rebucket.get(bucket);
            if (temp != null) {
                bucket = temp;
            }
            result = 0;
            for (Bucket<V> bucket2 : buckets) {
                if (bucket2 == bucket) {
                    break;
                }
                ++result;
            }
        }
        return result;
    }

    /**
     * Clear the index.
     * 
     * @return this, for chaining
     * @stable ICU 4.8
     */
    public AlphabeticIndex<V> clearRecords() {
        buckets = null;
        inputList.clear();
        return this;
    }

    /**
     * Return the number of buckets in the index. This will be the same as the number of labels, plus buckets for the underflow, overflow, and inflow(s).
     * 
     * @return number of buckets
     * @stable ICU 4.8
     */
    public int getBucketCount() {
        if (buckets == null) {
            initBuckets();
        }
        return buckets.bucketList.size();
    }

    /**
     * Return the number of records in the index: that is, the total number of distinct <name,data> pairs added with addRecord(...), over all the buckets.
     * 
     * @return total number of records in buckets
     * @stable ICU 4.8
     */
    public int getRecordCount() {
        return inputList.size();
    }

    /**
     * Return an iterator over the buckets.
     * 
     * @return iterator over buckets.
     * @stable ICU 4.8
     */
    public Iterator<Bucket<V>> iterator() {
        if (buckets == null) {
            initBuckets();
        }
        return buckets.iterator();
    }

    /**
     * Convenience routine to bucket a list of input strings according to the index.<br>
     * Warning: if a UI suppresses buckets that are empty, this may result in the special buckets (underflow, overflow,
     * inflow) being adjacent. In that case, the application may want to combine them.
     * 
     * @param inputList
     *            List of strings to be sorted and bucketed according to the labels.
     * @stable ICU 4.8
     */
    private void initBuckets() {
        buckets = new BucketList();

        // Make a collator for records. Do this so that the Records can be static classes, and not know about the collators.
        // TODO make this a member of the class.
        Comparator<Record<V>> fullComparator = new Comparator<Record<V>>() {
            public int compare(Record<V> o1, Record<V> o2) {
                int result = collatorOriginal.compare(o1.name, o2.name);
                if (result != 0) {
                    return result;
                }
                return o1.counter - o2.counter;
            }
        };

        //        // If we have Pinyin, then we have a special hack to bucket items with ASCII.
        //        if (hasPinyin) {
        //            Map<String,Bucket<V>> rebucketMap = new HashMap<String, Bucket<V>>();
        //            for (Record<V> name : inputList) {
        //                String key = hackName(name.name, collatorOriginal);
        //                if (key == null) continue;
        //                Bucket<V> bucket = rebucketMap.get(key);
        //                if (bucket == null) {
        //                    int index = rawGetBucketIndex(key);
        //                    bucket = buckets.bucketList.get(index);
        //                }
        //                rebucketMap.put(key, bucket);
        //                name.rebucket = bucket;
        //            }
        //        }

        // Set up a sorted list of the input
        TreeSet<Record<V>> sortedInput = new TreeSet<Record<V>>(fullComparator);
        sortedInput.addAll(inputList);

        // Now, we traverse all of the input, which is now sorted.
        // If the item doesn't go in the current bucket, we find the next bucket that contains it.
        // This makes the process order n*log(n), since we just sort the list and then do a linear process.
        // However, if the user adds item at a time and then gets the buckets, this isn't efficient, so
        // we need to improve it for that case.

        Iterator<Bucket<V>> bucketIterator = buckets.fullIterator();
        Bucket<V> currentBucket = bucketIterator.next();
        Bucket<V> nextBucket = bucketIterator.next();
        String upperBoundary = nextBucket.lowerBoundary; // there is always at least one bucket, so this is safe
        boolean atEnd = false;
        for (Record<V> s : sortedInput) {
//            // special hack for pinyin
//            if (s.rebucket != null) {
//                s.rebucket.records.add(s);
//                continue;
//            }
            // if the current bucket isn't the right one, find the one that is
            // We have a special flag for the last bucket so that we don't look any further
            while (!atEnd && collatorPrimaryOnly.compare(s.name, upperBoundary) >= 0) {
                currentBucket = nextBucket;
                // now reset the boundary that we compare against
                if (bucketIterator.hasNext()) {
                    nextBucket = bucketIterator.next();
                    upperBoundary = nextBucket.lowerBoundary;
                    if (upperBoundary == null) {
                        atEnd = true;
                    }
                } else {
                    atEnd = true;
                }
            }
            // now put the record into the bucket.
            buckets.addTo(s, currentBucket);
        }
    }

    /**
     * Get the Unicode character (or tailored string) that defines an overflow bucket; that is anything greater than or
     * equal to that string should go in that bucket, instead of with the last character. Normally that is the first
     * character of the script after lowerLimit. Thus in X Y Z ... <i>Devanagari-ka</i>, the overflow character for Z
     * would be the <i>Greek-alpha</i>.
     * 
     * @param lowerLimit
     *            The character below the overflow (or inflow) bucket
     * @return string that defines top of the overflow buck for lowerLimit, or null if there is none
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public String getOverflowComparisonString(String lowerLimit) {
        // TODO Use collator method instead of this hack
        for (String s : HACK_FIRST_CHARS_IN_SCRIPTS) {
            if (collatorPrimaryOnly.compare(s, lowerLimit) > 0) {
                return s;
            }
        }
        return null;
    }

    /**
     * Return a list of the first character in each script, in collation order. Only exposed for testing.
     * 
     * @return list of first characters in each script
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public List<String> getFirstScriptCharacters() {
        return HACK_FIRST_CHARS_IN_SCRIPTS;
    }

    /**
     * As the index is built, strings may be discarded from the exemplars. This contains some of the discards, and is
     * intended for debugging.
     * 
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public Map<String, Set<String>> getAlreadyIn() {
        return alreadyIn;
    }

    /**
     * As the index is built, strings may be discarded from the exemplars. This contains some of the discards, and is
     * intended for debugging.
     * 
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public List<String> getNoDistinctSorting() {
        return noDistinctSorting;
    }

    /**
     * As the index is built, strings may be discarded from the exemplars. This contains some of the discards, and is
     * intended for debugging.
     * 
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public List<String> getNotAlphabetic() {
        return notAlphabetic;
    }

    private static UnicodeSet getScriptSet(String codePoint) {
        if (codePoint.startsWith(BASE)) {
            return new UnicodeSet(UNIHAN);
        }
        return new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, UScript.getScript(codePoint.codePointAt(0)));
    }

    private static final UnicodeSet IGNORE_SCRIPTS = new UnicodeSet(
    "[[:sc=Common:][:sc=inherited:][:script=Unknown:][:script=braille:]]").freeze();

    private static final PreferenceComparator PREFERENCE_COMPARATOR = new PreferenceComparator();
    private int maxLabelCount = 99;

    /**
     * Comparator that returns "better" strings first, where shorter NFKD is better, and otherwise NFKD binary order is
     * better, and otherwise binary order is better.
     */
    private static class PreferenceComparator implements Comparator<Object> {
        static final Comparator<String> binary = new UTF16.StringComparator(true, false, 0);

        public int compare(Object o1, Object o2) {
            return compare((String) o1, (String) o2);
        }

        public int compare(String s1, String s2) {
            if (s1 == s2) {
                return 0;
            }
            String n1 = Normalizer.decompose(s1, true);
            String n2 = Normalizer.decompose(s2, true);
            int result = n1.length() - n2.length();
            if (result != 0) {
                return result;
            }
            result = binary.compare(n1, n2);
            if (result != 0) {
                return result;
            }
            return binary.compare(s1, s2);
        }
    }

    /**
     * A record to be sorted into buckets with getIndexBucketCharacters.
     * 
     * @stable ICU 4.8
     */
    public static class Record<V> {
        //private Bucket<V> rebucket = null; // special hack for Pinyin
        private CharSequence name;
        private V data;
        private int counter;

        private Record(CharSequence name, V data, int counter) {
            this.name = name;
            this.data = data;
            this.counter = counter;
        }

        /**
         * Get the name
         * 
         * @return the name
         * @stable ICU 4.8
         */
        public CharSequence getName() {
            return name;
        }

        /**
         * Get the data
         * 
         * @return the data
         * @stable ICU 4.8
         */
        public V getData() {
            return data;
        }

        /**
         * Standard toString()
         * @stable ICU 4.8
         */
        public String toString() {
            return name + "=" + data 
            //+ (rebucket == null ? "" : "{" + rebucket.label + "}")
            ;
        }
    }

    /**
     * A "bucket", containing records sorted under an index string by getIndexBucketCharacters. Is created by the
     * addBucket method in BucketList. A typical implementation will provide methods getLabel(), getSpecial(), and
     * getValues().<br>
     * See com.ibm.icu.dev.test.collator.IndexCharactersTest for an example.
     * 
     * @param <V>
     *            Data type
     * @stable ICU 4.8
     */
    public static class Bucket<V> implements Iterable<Record<V>> {
        private final String label;
        private final String lowerBoundary;
        private final LabelType labelType;
        private final List<Record<V>> records = new ArrayList<Record<V>>();

        /**
         * Type of the label
         * 
         * @stable ICU 4.8
         */
        public enum LabelType {
            NORMAL, UNDERFLOW, INFLOW, OVERFLOW
        }

        /**
         * Set up the bucket.
         * 
         * @param label
         *            label for the bucket
         * @param labelType
         *            is an underflow, overflow, or inflow bucket
         * @stable ICU 4.8
         */
        private Bucket(String label, String lowerBoundary, LabelType labelType) {
            //            String hackLabel = HACK_TRADITIONAL.get(label);
            //            if (hackLabel != null) {
            //                label = hackLabel;
            //            }
            this.label = label;
            this.lowerBoundary = lowerBoundary;
            this.labelType = labelType;
        }

        String getLowerBoundary() {
            return lowerBoundary;
        }

        /**
         * Get the label
         * 
         * @return label for the bucket
         * @stable ICU 4.8
         */
        public String getLabel() {
            return label;
        }

        /**
         * Is a normal, underflow, overflow, or inflow bucket
         * 
         * @return is an underflow, overflow, or inflow bucket
         * @stable ICU 4.8
         */
        public LabelType getLabelType() {
            return labelType;
        }

        /**
         * Get the number of records in the bucket.
         * 
         * @return number of records in bucket
         * @stable ICU 4.8
         */
        public int size() {
            return records.size();
        }

        /**
         * Iterator over the records in the bucket
         * @stable ICU 4.8
         */
        public Iterator<Record<V>> iterator() {
            return records.iterator();
        }

        /**
         * Standard toString()
         * @stable ICU 4.8
         */
        @Override
        public String toString() {
            return "{" +
            "labelType=" + labelType
            + ", " +
            "lowerBoundary=" + lowerBoundary
            + ", " +
            "label=" + label
            + "}"
            ;
        }
    }

    private class BucketList implements Iterable<Bucket<V>> {
        private final ArrayList<Bucket<V>> bucketList = new ArrayList<Bucket<V>>();
        private final HashMap<Bucket<V>,Bucket<V>> rebucket;
        private final List<Bucket<V>> immutableVisibleList;

        private BucketList() {
            // initialize indexCharacters;
            List<String> indexCharacters = initLabels();

            // underflow bucket
            bucketList.add(new Bucket<V>(getUnderflowLabel(), "", Bucket.LabelType.UNDERFLOW));

            // fix up the list, adding underflow, additions, overflow
            // insert infix labels as needed, using \uFFFF.
            String last = indexCharacters.get(0);
            bucketList.add(new Bucket<V>(fixLabel(last), last, Bucket.LabelType.NORMAL));
            UnicodeSet lastSet = getScriptSet(last).removeAll(IGNORE_SCRIPTS);

            for (int i = 1; i < indexCharacters.size(); ++i) {
                String current = indexCharacters.get(i);
                UnicodeSet set = getScriptSet(current).removeAll(IGNORE_SCRIPTS);
                if (lastSet.containsNone(set)) {
                    // check for adjacent
                    String overflowComparisonString = getOverflowComparisonString(last);
                    if (collatorPrimaryOnly.compare(overflowComparisonString, current) < 0) {
                        bucketList.add(new Bucket<V>(getInflowLabel(), overflowComparisonString,
                                Bucket.LabelType.INFLOW));
                        //i++;
                        lastSet = set;
                    }
                }
                bucketList.add(new Bucket<V>(fixLabel(current), current, Bucket.LabelType.NORMAL));
                last = current;
                lastSet = set;
            }
            // overflow bucket
            String limitString = getOverflowComparisonString(last);
            bucketList.add(new Bucket<V>(getOverflowLabel(), limitString, Bucket.LabelType.OVERFLOW)); // final

            // add some redirects for Pinyin

            ArrayList<Bucket<V>> publicBucketList;
            if (hasPinyin) {
                rebucket = new HashMap<Bucket<V>,Bucket<V>>();
                publicBucketList = new ArrayList<Bucket<V>>();
                HashMap<String,Bucket<V>> rebucketLabel = new HashMap<String,Bucket<V>>();
                Bucket<V> flowBefore = null; // special handling for flow bucket before pinyin
                boolean flowRedirect = false;
                boolean havePinyin = false;

                for (Bucket<V> bucket : bucketList) {
                    String label = bucket.getLabel();
                    String lowerBound = bucket.getLowerBoundary();
                    if (lowerBound != null && lowerBound.startsWith(BASE)) { // pinyin
                        rebucket.put(bucket, rebucketLabel.get(label));
                        havePinyin = true;
                    } else { // not pinyin
                        if (bucket.labelType != LabelType.NORMAL) { // special handling for flows
                            if (flowRedirect == false) {
                                if (havePinyin) {
                                    // do a redirect from the last before  pinyin to the first before;
                                    // we do it this way so that the buckets are joined, and any between stuff goes to the end
                                    // eg a b c alpha chinese gorp
                                    // we want to show as ... a b c ... with the alpha and gorp both in the final bucket.
                                    rebucket.put(flowBefore, bucket);
                                    publicBucketList.remove(flowBefore);
                                    flowRedirect = true;
                                } else {
                                    flowBefore = bucket;
                                }
                            }
                        } else { // is NORMAL
                            rebucketLabel.put(label, bucket);
                        }
                        publicBucketList.add(bucket);
                    }
                }
            } else {
                rebucket = null;
                publicBucketList = bucketList;
            }
            immutableVisibleList = Collections.unmodifiableList(publicBucketList);
        }

        /**
         * @param s
         * @param currentBucket
         */
        private void addTo(Record<V> s, Bucket<V> currentBucket) {
            if (rebucket != null) {
                Bucket<V> newBucket = rebucket.get(currentBucket);
                if (newBucket != null) {
                    currentBucket = newBucket;
                }
            }
            currentBucket.records.add(s);
        }

        /**
         * @param current
         * @return
         */
        private String fixLabel(String current) {
            if (!current.startsWith(BASE)) {
                return current;
            }
            int rest = current.charAt(1);
            if (0x2800 < rest && rest <= 0x28FF) { // stroke count
                return (rest-0x2800) + "\u5283"; // HACK
            }
            return current.substring(1);
        }

        /**
         * Private iterator over all the buckets, visible and invisible
         */
        private Iterator<Bucket<V>> fullIterator() {
            return bucketList.iterator();
        }

        /**
         * Iterator over just the visible buckets.
         */
        public Iterator<Bucket<V>> iterator() {
            return immutableVisibleList.iterator(); // use immutable list to prevent remove().
        }
    }

    /*
     * HACKS
     */

    //    /**
    //     * Only gets called for simplified Chinese. Uses further hack to distinguish long from short pinyin table.
    //     */
    //    private String hackName(CharSequence name, RuleBasedCollator comparator) {
    //        if (!UNIHAN.contains(Character.codePointAt(name, 0))) {
    //            return null;
    //        }
    //        synchronized (PINYIN_LOWER_BOUNDS_LONG) {
    //            if (PINYIN_LOWER_BOUNDS == null) {
    //                if (comparator.getTailoredSet().contains(probeCharInLong)) {
    //                    PINYIN_LOWER_BOUNDS = PINYIN_LOWER_BOUNDS_LONG;
    //                    HACK_PINYIN_LOOKUP = HACK_PINYIN_LOOKUP_LONG;
    //                } else {
    //                    PINYIN_LOWER_BOUNDS = PINYIN_LOWER_BOUNDS_SHORT;
    //                    HACK_PINYIN_LOOKUP = HACK_PINYIN_LOOKUP_SHORT;
    //                }
    //            }
    //        }
    //        int index = Arrays.binarySearch(HACK_PINYIN_LOOKUP, name, comparator);
    //        if (index < 0) {
    //            index = -index - 2;
    //        }
    //        return PINYIN_LOWER_BOUNDS.substring(index, index + 1);
    //    }
    //
    //    private static String PINYIN_LOWER_BOUNDS;
    //
    //    private static String[] HACK_PINYIN_LOOKUP;
    //
    //
    //    /**
    //     * HACKS
    //     * Generated with org.unicode.draft.GenerateUnihanCollator.
    //     */
    //
    //    private static final int probeCharInLong = 0x28EAD;
    //    private static final int probeCharInLongStroke = 0x2A6A5;
    //
    //    private static final String PINYIN_LOWER_BOUNDS_LONG = "\u0101bcd\u0113fghjkl\u1E3F\u0144\u014Dpqrstwxyz";
    //
    //    private static final String[] HACK_PINYIN_LOOKUP_LONG = {
    //        "", // A
    //        "\u516B", // b : \u516B [b\u0101]
    //        "\uD863\uDEAD", // c : \U00028EAD [c\u0101]
    //        "\uD844\uDE51", // d : \U00021251 [d\u0101]
    //        "\u59B8", // e : \u59B8 [\u0113]
    //        "\u53D1", // f : \u53D1 [f\u0101]
    //        "\uD844\uDE45", // g : \U00021245 [g\u0101]
    //        "\u54C8", // h : \u54C8 [h\u0101]
    //        "\u4E0C", // j : \u4E0C [j\u012B]
    //        "\u5494", // k : \u5494 [k\u0101]
    //        "\u3547", // l : \u3547 [l\u0101]
    //        "\u5452", // m : \u5452 [\u1E3F]
    //        "\u5514", // n : \u5514 [\u0144]
    //        "\u5594", // o : \u5594 [\u014D]
    //        "\uD84F\uDC7A", // p : \U00023C7A [p\u0101]
    //        "\u4E03", // q : \u4E03 [q\u012B]
    //        "\u513F", // r : \u513F [r]
    //        "\u4EE8", // s : \u4EE8 [s\u0101]
    //        "\u4ED6", // t : \u4ED6 [t\u0101]
    //        "\u7A75", // w : \u7A75 [w\u0101]
    //        "\u5915", // x : \u5915 [x\u012B]
    //        "\u4E2B", // y : \u4E2B [y\u0101]
    //        "\u5E00", // z : \u5E00 [z\u0101]
    //    };
    //
    //    private static String PINYIN_LOWER_BOUNDS_SHORT = "\u0101bcd\u0113fghjkl\u1E3F\u0144\u014Dpqrstwxyz";
    //
    //    private static String[] HACK_PINYIN_LOOKUP_SHORT = {
    //        "", // A
    //        "\u516B", // b : \u516B [b\u0101]
    //        "\u5693", // c : \u5693 [c\u0101]
    //        "\u5491", // d : \u5491 [d\u0101]
    //        "\u59B8", // e : \u59B8 [\u0113]
    //        "\u53D1", // f : \u53D1 [f\u0101]
    //        "\u65EE", // g : \u65EE [g\u0101]
    //        "\u54C8", // h : \u54C8 [h\u0101]
    //        "\u4E0C", // j : \u4E0C [j\u012B]
    //        "\u5494", // k : \u5494 [k\u0101]
    //        "\u3547", // l : \u3547 [l\u0101]
    //        "\u5452", // m : \u5452 [\u1E3F]
    //        "\u5514", // n : \u5514 [\u0144]
    //        "\u5594", // o : \u5594 [\u014D]
    //        "\u5991", // p : \u5991 [p\u0101]
    //        "\u4E03", // q : \u4E03 [q\u012B]
    //        "\u513F", // r : \u513F [r]
    //        "\u4EE8", // s : \u4EE8 [s\u0101]
    //        "\u4ED6", // t : \u4ED6 [t\u0101]
    //        "\u7A75", // w : \u7A75 [w\u0101]
    //        "\u5915", // x : \u5915 [x\u012B]
    //        "\u4E2B", // y : \u4E2B [y\u0101]
    //        "\u5E00", // z : \u5E00 [z\u0101]
    //    };
    //    
    //    private static final Map<String,String> HACK_TRADITIONAL;
    //    static {
    //        Map<String,String> temp = new HashMap<String,String>();
    //        temp.put("\u4E00", "1\u5283"); 
    //        temp.put("\u4E01", "2\u5283"); 
    //        temp.put("\u4E07", "3\u5283"); 
    //        temp.put("\u4E0D", "4\u5283"); 
    //        temp.put("\u4E17", "5\u5283"); 
    //        temp.put("\u3401", "6\u5283"); 
    //        temp.put("\u4E23", "7\u5283"); 
    //        temp.put("\u4E26", "8\u5283"); 
    //        temp.put("\u4E34", "9\u5283"); 
    //        temp.put("\uD840\uDC35", "9\u5283"); 
    //        temp.put("\uD840\uDC3E", "10\u5283"); 
    //        temp.put("\uD840\uDC3D", "10\u5283"); 
    //        temp.put("\u3422", "11\u5283"); 
    //        temp.put("\uD840\uDC41", "11\u5283"); 
    //        temp.put("\uD840\uDC46", "12\u5283"); 
    //        temp.put("\u4E82", "13\u5283"); 
    //        temp.put("\uD840\uDC4C", "13\u5283"); 
    //        temp.put("\uD840\uDC4E", "14\u5283"); 
    //        temp.put("\u3493", "15\u5283"); 
    //        temp.put("\uD840\uDC53", "15\u5283"); 
    //        temp.put("\u4EB8", "16\u5283"); 
    //        temp.put("\uD840\uDC55", "16\u5283"); 
    //        temp.put("\u511F", "17\u5283"); 
    //        temp.put("\uD840\uDC56", "17\u5283"); 
    //        temp.put("\u512D", "18\u5283"); 
    //        temp.put("\uD840\uDC5F", "18\u5283"); 
    //        temp.put("\u3426", "19\u5283"); 
    //        temp.put("\uD840\uDC7A", "19\u5283"); 
    //        temp.put("\u34A5", "20\u5283"); 
    //        temp.put("\uD840\uDC60", "20\u5283"); 
    //        temp.put("\u34A7", "21\u5283"); 
    //        temp.put("\uD840\uDD9E", "21\u5283"); 
    //        temp.put("\u4EB9", "22\u5283"); 
    //        temp.put("\uD840\uDC7B", "22\u5283"); 
    //        temp.put("\u513D", "23\u5283"); 
    //        temp.put("\uD840\uDCC8", "23\u5283"); 
    //        temp.put("\u513E", "24\u5283"); 
    //        temp.put("\uD840\uDD9F", "24\u5283"); 
    //        temp.put("\u56D4", "25\u5283"); 
    //        temp.put("\uD842\uDCCA", "25\u5283"); 
    //        temp.put("\u3536", "26\u5283"); 
    //        temp.put("\u34AA", "26\u5283"); 
    //        temp.put("\u7065", "27\u5283"); 
    //        temp.put("\uD842\uDE0B", "27\u5283"); 
    //        temp.put("\u56D6", "28\u5283"); 
    //        temp.put("\uD840\uDDA0", "28\u5283"); 
    //        temp.put("\u7E9E", "29\u5283"); 
    //        temp.put("\uD840\uDDA1", "29\u5283"); 
    //        temp.put("\u53B5", "30\u5283"); 
    //        temp.put("\uD842\uDD6C", "30\u5283"); 
    //        temp.put("\u7069", "31\u5283"); 
    //        temp.put("\uD844\uDD9F", "31\u5283"); 
    //        temp.put("\u706A", "32\u5283"); 
    //        temp.put("\uD842\uDED1", "32\u5283"); 
    //        temp.put("\uD846\uDD3B", "33\u5283"); 
    //        temp.put("\uD842\uDE0C", "33\u5283"); 
    //        temp.put("\uD842\uDCCB", "34\u5283"); 
    //        temp.put("\u9F7E", "35\u5283"); 
    //        temp.put("\uD84C\uDF5C", "35\u5283"); 
    //        temp.put("\u9F49", "36\u5283"); 
    //        temp.put("\uD845\uDD19", "36\u5283"); 
    //        temp.put("\uD86B\uDE9A", "37\u5283"); 
    //        temp.put("\uD861\uDC04", "38\u5283"); 
    //        temp.put("\u9750", "39\u5283"); 
    //        temp.put("\uD845\uDD1A", "39\u5283"); 
    //        temp.put("\uD864\uDDD3", "40\u5283"); 
    //        temp.put("\uD869\uDCCA", "41\u5283"); 
    //        temp.put("\uD85A\uDDC4", "42\u5283"); 
    //        temp.put("\uD85C\uDD98", "43\u5283"); 
    //        temp.put("\uD85E\uDCB1", "44\u5283"); 
    //        temp.put("\uD865\uDE63", "46\u5283"); 
    //        temp.put("\u9F98", "48\u5283"); 
    //        temp.put("\uD85A\uDDC5", "48\u5283"); 
    //        temp.put("\u4A3B", "52\u5283"); 
    //        temp.put("\uD841\uDD3B", "64\u5283");
    //        HACK_TRADITIONAL = Collections.unmodifiableMap(temp);
    //    }

    /**
     * HACKS
     */
    private static final List<String> HACK_FIRST_CHARS_IN_SCRIPTS = 
        Arrays.asList(new String[] { 
                "a", "\u03B1", "\u2C81", "\u0430", "\u2C30", "\u10D0", "\u0561", "\u05D0", "\uD802\uDD00", "\u0800", "\u0621",
                "\u0710",  // Syriac
                "\u0840",  // Mandaic
                "\u0780", "\u07CA", "\u2D30", "\u1200", "\u0950", "\u0985", "\u0A74", "\u0AD0", "\u0B05", "\u0BD0", 
                "\u0C05", "\u0C85", "\u0D05", "\u0D85",
                "\uAAF2",  // Meetei Mayek
                "\uA800", "\uA882", "\uD804\uDC83",
                UCharacter.toString(0x111C4),  // Sharada
                UCharacter.toString(0x11680),  // Takri
                "\u1B83",  // Sundanese
                "\uD804\uDC05",  // Brahmi (U+11005)
                "\uD802\uDE00", "\u0E01",
                "\u0EDE",  // Lao
                "\uAA80", "\u0F40", "\u1C00", "\uA840", "\u1900", "\u1700", "\u1720", "\u1740", "\u1760", 
                "\u1A00",  // Buginese
                "\u1BC0",  // Batak
                "\uA930", "\uA90A", "\u1000",
                UCharacter.toString(0x11103),  // Chakma
                "\u1780", "\u1950", "\u1980", "\u1A20", "\uAA00", "\u1B05", "\uA984", "\u1880", "\u1C5A", "\u13A0", "\u1401", "\u1681", "\u16A0", "\uD803\uDC00", "\uA500", "\uA6A0", "\u1100", 
                "\u3041", "\u30A1", "\u3105", "\uA000", "\uA4F8",
                UCharacter.toString(0x16F00),  // Miao
                "\uD800\uDE80", "\uD800\uDEA0", "\uD802\uDD20", "\uD800\uDF00", "\uD800\uDF30", "\uD801\uDC28", "\uD801\uDC50", "\uD801\uDC80",
                UCharacter.toString(0x110D0),  // Sora Sompeng
                "\uD800\uDC00", "\uD802\uDC00", "\uD802\uDE60", "\uD802\uDF00", "\uD802\uDC40", 
                "\uD802\uDF40", "\uD802\uDF60", "\uD800\uDF80", "\uD800\uDFA0", "\uD808\uDC00", "\uD80C\uDC00",
                UCharacter.toString(0x109A0),  // Meroitic Cursive
                UCharacter.toString(0x10980),  // Meroitic Hieroglyphs
                "\u4E00" 
        });

    //    private static final UnicodeSet HACK_SHORT_TRAD_EXEMPLARS = new UnicodeSet(
    //            "[\u3401 \u3422 \u3426 \u3493 \u34A5 \u34A7 \u3536 \u4E00 \u4E01 \u4E07 \u4E0D \u4E17 \u4E23 \u4E26 \u4E34 \u4E82 \u4EB8 \u4EB9 \u511F \u512D \u513D" +
    //    		" \u513E \u53B5 \u56D4 \u56D6 \u7065 \u7069 \u706A \u7E9E \u9750 \u9F49 \u9F7E \u9F98 \\U0002003E \\U00020046 \\U0002004E \\U0002193B]").freeze();
    //    private static final UnicodeSet HACK_LONG_TRAD_EXEMPLARS = new UnicodeSet(
    //            "[\u3401\u34AA\u4A3B\u4E00\u4E01\u4E07\u4E0D\u4E17\u4E23\u4E26" +
    //    		"\\U00020035\\U0002003D\\U00020041\\U00020046\\U0002004C\\U0002004E\\U00020053\\U00020055\\U00020056\\U0002005F\\U00020060\\U0002007A\\U0002007B\\U000200C8" +
    //    		"\\U0002019E-\\U000201A1\\U0002053B\\U000208CA\\U000208CB\\U0002096C\\U00020A0B\\U00020A0C\\U00020AD1\\U0002119F\\U00021519\\U0002151A\\U0002335C\\U000269C4" +
    //    		"\\U000269C5\\U00027198\\U000278B1\\U00028404\\U000291D3\\U00029663\\U0002A4CA\\U0002AE9A]").freeze();
    /**
     * Only for testing...
     * @internal
     * @deprecated only for internal testing
     */
    public static List<String> getFirstCharactersInScripts() {
        return HACK_FIRST_CHARS_IN_SCRIPTS;
    }
}

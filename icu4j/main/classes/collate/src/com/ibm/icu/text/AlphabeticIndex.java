/*
 *******************************************************************************
 * Copyright (C) 2008-2013, Google Inc, International Business Machines Corporation
 * and others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
 * AlphabeticIndex&lt;Integer&gt; index = new AlphabeticIndex&lt;Integer&gt;(desiredLocale).addLabels(additionalLocale);
 * int counter = 0;
 * for (String item : test) {
 *     index.addRecord(item, counter++); 
 * }
 * ...
 * // Show index at top. We could skip or gray out empty buckets
 * 
 * for (AlphabeticIndex.Bucket&lt;Integer&gt; bucket : index) {
 *     if (showAll || bucket.size() != 0) {
 *         showLabelAtTop(UI, bucket.getLabel());
 *     }
 * }
 *  ...
 * // Show the buckets with their contents, skipping empty buckets
 * 
 * for (AlphabeticIndex.Bucket&lt;Integer&gt; bucket : index) {
 *     if (bucket.size() != 0) {
 *         showLabelInList(UI, bucket.getLabel());
 *         for (AlphabeticIndex.Record&lt;Integer&gt; item : bucket) {
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
 * <p>Callers can also use the {@link AlphabeticIndex.ImmutableIndex}, or the AlphabeticIndex itself,
 * to support sorting on a client that doesn't support AlphabeticIndex functionality.
 *
 * <p>The ImmutableIndex is both immutable and thread-safe.
 * The corresponding AlphabeticIndex methods are not thread-safe because
 * they "lazily" build the index buckets.
 * <ul>
 * <li>ImmutableIndex.getBucket(index) provides random access to all
 *     buckets and their labels and label types.
 * <li>AlphabeticIndex.getBucketLabels() or the bucket iterator on either class
 *     can be used to get a list of the labels,
 *     such as "...", "A", "B",..., and send that list to the client.
 * <li>When the client has a new name, it sends that name to the server.
 * The server needs to call the following methods,
 * and communicate the bucketIndex and collationKey back to the client.
 * 
 * <pre>
 * int bucketIndex = index.getBucketIndex(name);
 * String label = immutableIndex.getBucket(bucketIndex).getLabel();  // optional
 * RawCollationKey collationKey = collator.getRawCollationKey(name, null);
 * </pre>
 * 
 * <li>The client would put the name (and associated information) into its bucket for bucketIndex. The collationKey is a
 * sequence of bytes that can be compared with a binary compare, and produce the right localized result.</li>
 * </ul>
 *
 * @author Mark Davis
 * @stable ICU 4.8
 */
public final class AlphabeticIndex<V> implements Iterable<Bucket<V>> {
    /**
     * Prefix string for Chinese index buckets.
     * See http://unicode.org/repos/cldr/trunk/specs/ldml/tr35-collation.html#Collation_Indexes
     */
    private static final String BASE = "\uFDD0";

    // these are generated. Later, get from CLDR data.
    private static final UnicodeSet PINYIN_LABELS = new UnicodeSet("[A-Z{\uFDD0A}{\uFDD0B}{\uFDD0C}{\uFDD0D}{\uFDD0E}{\uFDD0F}{\uFDD0G}{\uFDD0H}{\uFDD0I}{\uFDD0J}{\uFDD0K}{\uFDD0L}{\uFDD0M}{\uFDD0N}{\uFDD0O}{\uFDD0P}{\uFDD0Q}{\uFDD0R}{\uFDD0S}{\uFDD0T}{\uFDD0U}{\uFDD0V}{\uFDD0W}{\uFDD0X}{\uFDD0Y}{\uFDD0Z}]").freeze();
    private static final UnicodeSet STROKE_LABELS = new UnicodeSet("[{\uFDD0\u2801}{\uFDD0\u2802}{\uFDD0\u2803}{\uFDD0\u2804}{\uFDD0\u2805}{\uFDD0\u2806}{\uFDD0\u2807}{\uFDD0\u2808}{\uFDD0\u2809}{\uFDD0\u280A}{\uFDD0\u280B}{\uFDD0\u280C}{\uFDD0\u280D}{\uFDD0\u280E}{\uFDD0\u280F}{\uFDD0\u2810}{\uFDD0\u2811}{\uFDD0\u2812}{\uFDD0\u2813}{\uFDD0\u2814}{\uFDD0\u2815}{\uFDD0\u2816}{\uFDD0\u2817}{\uFDD0\u2818}{\uFDD0\u2819}{\uFDD0\u281A}{\uFDD0\u281B}{\uFDD0\u281C}{\uFDD0\u281D}{\uFDD0\u281E}{\uFDD0\u281F}{\uFDD0\u2820}{\uFDD0\u2821}{\uFDD0\u2822}{\uFDD0\u2823}{\uFDD0\u2824}{\uFDD0\u2825}{\uFDD0\u2826}{\uFDD0\u2827}{\uFDD0\u2828}{\uFDD0\u2829}{\uFDD0\u282A}{\uFDD0\u282B}{\uFDD0\u282C}{\uFDD0\u282E}{\uFDD0\u2830}{\uFDD0\u2834}{\uFDD0\u2840}]").freeze();
    private static final UnicodeSet RADICAL_LABELS = new UnicodeSet("[{\uFDD0\u2E80}{\uFDD0\u2E81}{\uFDD0\u2E84}{\uFDD0\u2E85}{\uFDD0\u2E86}{\uFDD0\u2E87}{\uFDD0\u2E88}{\uFDD0\u2E8A}{\uFDD0\u2E8B}{\uFDD0\u2E8C}{\uFDD0\u2E91}{\uFDD0\u2E92}{\uFDD0\u2E93}{\uFDD0\u2E95}{\uFDD0\u2E97}{\uFDD0\u2E98}{\uFDD0\u2E99}{\uFDD0\u2E9B}{\uFDD0\u2E9D}{\uFDD0\u2E9E}{\uFDD0\u2E9F}{\uFDD0\u2EA0}{\uFDD0\u2EA2}{\uFDD0\u2EA3}{\uFDD0\u2EA4}{\uFDD0\u2EA7}{\uFDD0\u2EA8}{\uFDD0\u2EA9}{\uFDD0\u2EAA}{\uFDD0\u2EAB}{\uFDD0\u2EAC}{\uFDD0\u2EAE}{\uFDD0\u2EAF}{\uFDD0\u2EB0}{\uFDD0\u2EB4}{\uFDD0\u2EB8}{\uFDD0\u2EB9}{\uFDD0\u2EBB}{\uFDD0\u2EBC}{\uFDD0\u2EBD}{\uFDD0\u2EC0}{\uFDD0\u2EC1}{\uFDD0\u2EC2}{\uFDD0\u2EC3}{\uFDD0\u2EC5}{\uFDD0\u2EC6}{\uFDD0\u2EC8}{\uFDD0\u2EC9}{\uFDD0\u2ECA}{\uFDD0\u2ECB}{\uFDD0\u2ECF}{\uFDD0\u2ED0}{\uFDD0\u2ED1}{\uFDD0\u2ED3}{\uFDD0\u2ED4}{\uFDD0\u2ED6}{\uFDD0\u2ED7}{\uFDD0\u2ED8}{\uFDD0\u2ED9}{\uFDD0\u2EDA}{\uFDD0\u2EDB}{\uFDD0\u2EDC}{\uFDD0\u2EDD}{\uFDD0\u2EE0}{\uFDD0\u2EE1}{\uFDD0\u2EE2}{\uFDD0\u2EE3}{\uFDD0\u2EE4}{\uFDD0\u2EE5}{\uFDD0\u2EE6}{\uFDD0\u2EE7}{\uFDD0\u2EE8}{\uFDD0\u2EEA}{\uFDD0\u2EEB}{\uFDD0\u2EED}{\uFDD0\u2EEE}{\uFDD0\u2EEF}{\uFDD0\u2EF0}{\uFDD0\u2EF2}{\uFDD0\u2EF3}{\uFDD0\u2F00}{\uFDD0\u2F01}{\uFDD0\u2F02}{\uFDD0\u2F03}{\uFDD0\u2F05}{\uFDD0\u2F06}{\uFDD0\u2F07}{\uFDD0\u2F09}{\uFDD0\u2F0A}{\uFDD0\u2F0B}{\uFDD0\u2F0D}{\uFDD0\u2F0E}{\uFDD0\u2F10}{\uFDD0\u2F12}{\uFDD0\u2F13}{\uFDD0\u2F14}{\uFDD0\u2F15}{\uFDD0\u2F16}{\uFDD0\u2F17}{\uFDD0\u2F1B}{\uFDD0\u2F1D}{\uFDD0\u2F1E}{\uFDD0\u2F1F}{\uFDD0\u2F20}{\uFDD0\u2F21}{\uFDD0\u2F22}{\uFDD0\u2F23}{\uFDD0\u2F24}{\uFDD0\u2F25}{\uFDD0\u2F26}{\uFDD0\u2F27}{\uFDD0\u2F28}{\uFDD0\u2F2B}{\uFDD0\u2F2C}{\uFDD0\u2F2D}{\uFDD0\u2F2E}{\uFDD0\u2F2F}{\uFDD0\u2F31}{\uFDD0\u2F32}{\uFDD0\u2F34}{\uFDD0\u2F35}{\uFDD0\u2F36}{\uFDD0\u2F37}{\uFDD0\u2F38}{\uFDD0\u2F3A}{\uFDD0\u2F3B}{\uFDD0\u2F3D}{\uFDD0\u2F3E}{\uFDD0\u2F40}{\uFDD0\u2F42}{\uFDD0\u2F43}{\uFDD0\u2F44}{\uFDD0\u2F45}{\uFDD0\u2F46}{\uFDD0\u2F48}{\uFDD0\u2F4A}{\uFDD0\u2F4B}{\uFDD0\u2F4C}{\uFDD0\u2F4E}{\uFDD0\u2F50}{\uFDD0\u2F51}{\uFDD0\u2F53}{\uFDD0\u2F57}{\uFDD0\u2F58}{\uFDD0\u2F59}{\uFDD0\u2F5A}{\uFDD0\u2F5B}{\uFDD0\u2F5E}{\uFDD0\u2F60}{\uFDD0\u2F61}{\uFDD0\u2F62}{\uFDD0\u2F63}{\uFDD0\u2F64}{\uFDD0\u2F65}{\uFDD0\u2F67}{\uFDD0\u2F68}{\uFDD0\u2F69}{\uFDD0\u2F6A}{\uFDD0\u2F6B}{\uFDD0\u2F6D}{\uFDD0\u2F6E}{\uFDD0\u2F6F}{\uFDD0\u2F71}{\uFDD0\u2F72}{\uFDD0\u2F73}{\uFDD0\u2F74}{\uFDD0\u2F76}{\uFDD0\u2F78}{\uFDD0\u2F7B}{\uFDD0\u2F7D}{\uFDD0\u2F7E}{\uFDD0\u2F7F}{\uFDD0\u2F82}{\uFDD0\u2F83}{\uFDD0\u2F84}{\uFDD0\u2F86}{\uFDD0\u2F87}{\uFDD0\u2F88}{\uFDD0\u2F89}{\uFDD0\u2F8A}{\uFDD0\u2F8D}{\uFDD0\u2F8E}{\uFDD0\u2F8F}{\uFDD0\u2F92}{\uFDD0\u2F94}{\uFDD0\u2F95}{\uFDD0\u2F96}{\uFDD0\u2F97}{\uFDD0\u2F98}{\uFDD0\u2F99}{\uFDD0\u2F9A}{\uFDD0\u2F9B}{\uFDD0\u2F9D}{\uFDD0\u2F9E}{\uFDD0\u2F9F}{\uFDD0\u2FA0}{\uFDD0\u2FA1}{\uFDD0\u2FA3}{\uFDD0\u2FA4}{\uFDD0\u2FA5}{\uFDD0\u2FA6}{\uFDD0\u2FA8}{\uFDD0\u2FAA}{\uFDD0\u2FAB}{\uFDD0\u2FAE}{\uFDD0\u2FAF}{\uFDD0\u2FB0}{\uFDD0\u2FB1}{\uFDD0\u2FB2}{\uFDD0\u2FB3}{\uFDD0\u2FB4}{\uFDD0\u2FB5}{\uFDD0\u2FB6}{\uFDD0\u2FB9}{\uFDD0\u2FBA}{\uFDD0\u2FBC}{\uFDD0\u2FBD}{\uFDD0\u2FBE}{\uFDD0\u2FBF}{\uFDD0\u2FC0}{\uFDD0\u2FC2}{\uFDD0\u2FC3}{\uFDD0\u2FC4}{\uFDD0\u2FC5}{\uFDD0\u2FC6}{\uFDD0\u2FC7}{\uFDD0\u2FC8}{\uFDD0\u2FC9}{\uFDD0\u2FCA}{\uFDD0\u2FCB}{\uFDD0\u2FCC}{\uFDD0\u2FCD}{\uFDD0\u2FCE}{\uFDD0\u2FCF}{\uFDD0\u2FD0}{\uFDD0\u2FD1}{\uFDD0\u2FD5}]").freeze();
    private static final List<String> PROBES = Arrays.asList("\u4E00", "\uFDD0A", "\uFDD0\u2801", "\uFDD0\u2E80");
    private static final UnicodeSet[] MATCHING = {null, PINYIN_LABELS, STROKE_LABELS, RADICAL_LABELS};

    private static final char CGJ = '\u034F';
    private static final UnicodeSet ALPHABETIC = new UnicodeSet("[[:alphabetic:]-[:mark:]]").add(BASE).freeze();
    private static final UnicodeSet HANGUL = new UnicodeSet(
    "[\uAC00 \uB098 \uB2E4 \uB77C \uB9C8 \uBC14  \uC0AC  \uC544 \uC790  \uCC28 \uCE74 \uD0C0 \uD30C \uD558]").freeze();
    private static final UnicodeSet ETHIOPIC = new UnicodeSet("[[:Block=Ethiopic:]&[:Script=Ethiopic:]]").freeze();
    private static final UnicodeSet CORE_LATIN = new UnicodeSet("[a-z]").freeze();

    private final RuleBasedCollator collatorOriginal;
    private final RuleBasedCollator collatorPrimaryOnly;
    private RuleBasedCollator collatorExternal;

    private final List<String> firstCharsInScripts;

    // for testing
    private LinkedHashMap<String, Set<String>> alreadyIn;
    private List<String> noDistinctSorting;
    private List<String> notAlphabetic;

    // We accumulate these as we build up the input parameters
    private final UnicodeSet initialLabels = new UnicodeSet();
    private Collection<Record<V>> inputList;

    // Lazy evaluated: null means that we have not built yet.
    private BucketList<V> buckets;

    private String overflowLabel = "\u2026";
    private String underflowLabel = "\u2026";
    private String inflowLabel = "\u2026";

    /**
     * Immutable, thread-safe version of {@link AlphabeticIndex}.
     * This class provides thread-safe methods for bucketing,
     * and random access to buckets and their properties,
     * but does not offer adding records to the index.
     *
     * @param <V> The Record value type is unused. It can be omitted for this class
     * if it was omitted for the AlphabeticIndex that built it.
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    public static final class ImmutableIndex<V> implements Iterable<Bucket<V>> {
        private final BucketList<V> buckets;
        private final Collator collatorPrimaryOnly;

        private ImmutableIndex(BucketList<V> bucketList, Collator collatorPrimaryOnly) {
            this.buckets = bucketList;
            this.collatorPrimaryOnly = collatorPrimaryOnly;
        }

        /**
         * Returns the number of index buckets and labels, including underflow/inflow/overflow.
         *
         * @return the number of index buckets
         * @draft ICU 51
         * @provisional This API might change or be removed in a future release.
         */
        public int getBucketCount() {
            return buckets.getBucketCount();
        }

        /**
         * Finds the index bucket for the given name and returns the number of that bucket.
         * Use {@link #getBucket(int)} to get the bucket's properties.
         *
         * @param name the string to be sorted into an index bucket
         * @return the bucket number for the name
         * @draft ICU 51
         * @provisional This API might change or be removed in a future release.
         */
        public int getBucketIndex(CharSequence name) {
            return buckets.getBucketIndex(name, collatorPrimaryOnly);
        }

        /**
         * Returns the index-th bucket. Returns null if the index is out of range.
         *
         * @param index bucket number
         * @return the index-th bucket
         * @draft ICU 51
         * @provisional This API might change or be removed in a future release.
         */
        public Bucket<V> getBucket(int index) {
            if (0 <= index && index < buckets.getBucketCount()) {
                return buckets.immutableVisibleList.get(index);
            } else {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         * @draft ICU 51
         * @provisional This API might change or be removed in a future release.
         */
        public Iterator<Bucket<V>> iterator() {
            return buckets.iterator();
        }
    }

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
        collatorOriginal = collator != null ? collator : (RuleBasedCollator) Collator.getInstance(locale);
        try {
            collatorPrimaryOnly = (RuleBasedCollator) (collatorOriginal.clone());
        } catch (Exception e) {
            // should never happen
            throw new IllegalStateException("Collator cannot be cloned", e);
        }
        collatorPrimaryOnly.setStrength(Collator.PRIMARY);
        collatorPrimaryOnly.freeze();
        firstCharsInScripts = new ArrayList<String>(HACK_FIRST_CHARS_IN_SCRIPTS);
        Collections.sort(firstCharsInScripts, collatorPrimaryOnly);
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
     * @param maxLabelCount Set the maximum number of labels. Currently, if the number is exceeded, then every
     *         nth item is removed to bring the count down. A more sophisticated mechanism may be available in the
     *         future.
     * @return this, for chaining
     * @stable ICU 4.8
     */
    public AlphabeticIndex<V> setMaxLabelCount(int maxLabelCount) {
        this.maxLabelCount = maxLabelCount;
        return this;
    }

    /**
     * Determine the best labels to use. This is based on the exemplars, but we also process to make sure that they are unique,
     * and sort differently, and that the overall list is small enough.
     */
    private ArrayList<String> initLabels() {
        UnicodeSet exemplars = new UnicodeSet(initialLabels);

        // First sort them, with a "best" ordering among items that are the same according
        // to the collator.
        // Re the warning: the JDK inexplicably didn't make Collators be Comparator<String>!
        @SuppressWarnings("unchecked")
        Set<String> preferenceSorting = new TreeSet<String>(new MultiComparator<Object>(collatorPrimaryOnly, PREFERENCE_COMPARATOR));
        exemplars.addAllTo(preferenceSorting);

        TreeSet<String> indexCharacterSet = new TreeSet<String>(collatorPrimaryOnly);

        String overflowBoundary = firstCharsInScripts.get(firstCharsInScripts.size() - 1);

        // We now make a sorted array of elements
        // Some of the input may, however, be redundant.
        // That is, we might have c, ch, d, where "ch" sorts just like "c", "h"
        // So we make a pass through, filtering out those cases.

        for (String item : preferenceSorting) {
            boolean checkDistinct;
            if (UTF16.hasMoreCodePointsThan(item, 1) &&
                    item.charAt(item.length() - 1) == '*' &&
                    item.charAt(item.length() - 2) != '*') {
                // Use a label if it is marked with one trailing star,
                // even if the label string sorts the same when all contractions are suppressed.
                item = item.substring(0, item.length() - 1);
                checkDistinct = false;
            } else {
                checkDistinct = true;
            }
            if (indexCharacterSet.contains(item)) {
                if (alreadyIn == null) {
                    alreadyIn = new LinkedHashMap<String, Set<String>>();
                }
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
            } else if (checkDistinct && UTF16.hasMoreCodePointsThan(item, 1) &&
                    collatorPrimaryOnly.compare(item, separated(item)) == 0) {
                if (noDistinctSorting == null) {
                    noDistinctSorting = new ArrayList<String>();
                }
                noDistinctSorting.add(item);
            } else if (!ALPHABETIC.containsSome(item)) {
                if (notAlphabetic == null) {
                    notAlphabetic = new ArrayList<String>();
                }
                notAlphabetic.add(item);
            } else if (collatorPrimaryOnly.compare(item, "") == 0) {
                // Ignore primary-ignorable index characters.
            } else if (collatorPrimaryOnly.compare(item, overflowBoundary) >= 0) {
                // Ignore index characters that will land in the overflow bucket.
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

    private static String fixLabel(String current) {
        if (!current.startsWith(BASE)) {
            return current;
        }
        int rest = current.charAt(BASE.length());
        if (0x2800 < rest && rest <= 0x28FF) { // stroke count
            return (rest-0x2800) + "\u5283";
        }
        return current.substring(BASE.length());
    }

    /**
     * This method is called to get the index exemplars. Normally these come from the locale directly,
     * but if they aren't available, we have to synthesize them.
     * @param locale
     */
    private UnicodeSet getIndexExemplars(ULocale locale) {
        UnicodeSet exemplars;

        exemplars = LocaleData.getExemplarSet(locale, 0, LocaleData.ES_INDEX);
        if (exemplars != null) {
            final String language = locale.getLanguage();
            if (language.equals("zh") || language.equals("ja") || language.equals("ko")) {
                // TODO: HACK      
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
                    exemplars.clear().addAll(MATCHING[location]);
                }
            }
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
     * Builds an immutable, thread-safe version of this instance, without data records.
     *
     * @return an immutable index instance
     * @draft ICU 51
     * @provisional This API might change or be removed in a future release.
     */
    public ImmutableIndex<V> buildImmutableIndex() {
        // The current AlphabeticIndex Java code never modifies the bucket list once built.
        // If it contains no records, we can use it.
        // addRecord() sets buckets=null rather than inserting the new record into it.
        BucketList<V> immutableBucketList;
        if (inputList != null && !inputList.isEmpty()) {
            // We need a bucket list with no records.
            immutableBucketList = createBucketList();
        } else {
            if (buckets == null) {
                buckets = createBucketList();
            }
            immutableBucketList = buckets;
        }
        return new ImmutableIndex<V>(immutableBucketList, collatorPrimaryOnly);
    }

    /**
     * Get the labels.
     * 
     * @return The list of bucket labels, after processing.
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
        if (inputList == null) {
            inputList = new ArrayList<Record<V>>();
        }
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
     * @return the bucket index for the name
     * @stable ICU 4.8
     */
    public int getBucketIndex(CharSequence name) {
        if (buckets == null) {
            initBuckets();
        }
        return buckets.getBucketIndex(name, collatorPrimaryOnly);
    }

    /**
     * Clear the index.
     * 
     * @return this, for chaining
     * @stable ICU 4.8
     */
    public AlphabeticIndex<V> clearRecords() {
        buckets = null;
        if (inputList != null) {
            inputList.clear();
        }
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
        return buckets.getBucketCount();
    }

    /**
     * Return the number of records in the index: that is, the total number of distinct <name,data> pairs added with addRecord(...), over all the buckets.
     * 
     * @return total number of records in buckets
     * @stable ICU 4.8
     */
    public int getRecordCount() {
        return inputList != null ? inputList.size() : 0;
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
     * Works on the inputList:
     *            List of strings to be sorted and bucketed according to the labels.
     */
    private void initBuckets() {
        buckets = createBucketList();
        if (inputList == null || inputList.isEmpty()) {
            return;
        }

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

        // Set up a sorted list of the input
        TreeSet<Record<V>> sortedInput = new TreeSet<Record<V>>(fullComparator);
        sortedInput.addAll(inputList);

        // Now, we traverse all of the input, which is now sorted.
        // If the item doesn't go in the current bucket, we find the next bucket that contains it.
        // This makes the process order n*log(n), since we just sort the list and then do a linear process.
        // However, if the user adds an item at a time and then gets the buckets, this isn't efficient, so
        // we need to improve it for that case.

        Iterator<Bucket<V>> bucketIterator = buckets.fullIterator();
        Bucket<V> currentBucket = bucketIterator.next();
        Bucket<V> nextBucket = bucketIterator.next();
        String upperBoundary = nextBucket.lowerBoundary; // there is always at least one bucket, so this is safe
        boolean atEnd = false;
        for (Record<V> s : sortedInput) {
            // if the current bucket isn't the right one, find the one that is
            // We have a special flag for the last bucket so that we don't look any further
            while (!atEnd && collatorPrimaryOnly.compare(s.name, upperBoundary) >= 0) {
                currentBucket = nextBucket;
                // now reset the boundary that we compare against
                if (bucketIterator.hasNext()) {
                    nextBucket = bucketIterator.next();
                    upperBoundary = nextBucket.lowerBoundary;
                } else {
                    atEnd = true;
                }
            }
            // now put the record into the bucket.
            Bucket<V> bucket = currentBucket;
            if (bucket.displayBucket != null) {
                bucket = bucket.displayBucket;
            }
            if (bucket.records == null) {
                bucket.records = new ArrayList<Record<V>>();
            }
            bucket.records.add(s);
        }
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
            return name + "=" + data;
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
        private Bucket<V> displayBucket;
        private int displayIndex;
        private List<Record<V>> records;

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
            this.label = label;
            this.lowerBoundary = lowerBoundary;
            this.labelType = labelType;
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
            return records == null ? 0 : records.size();
        }

        /**
         * Iterator over the records in the bucket
         * @stable ICU 4.8
         */
        public Iterator<Record<V>> iterator() {
            if (records == null) {
                return Collections.<Record<V>>emptyList().iterator();
            }
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

    private BucketList<V> createBucketList() {
        CollationElementIterator cei = collatorPrimaryOnly.getCollationElementIterator("");
        boolean hasInvisibleBuckets = false;

        // initialize indexCharacters;
        List<String> indexCharacters = initLabels();

        // Helper arrays for Chinese Pinyin collation.
        @SuppressWarnings("unchecked")
        Bucket<V>[] asciiBuckets = new Bucket[26];
        @SuppressWarnings("unchecked")
        Bucket<V>[] pinyinBuckets = new Bucket[26];
        boolean hasPinyin = false;

        ArrayList<Bucket<V>> bucketList = new ArrayList<Bucket<V>>();

        // underflow bucket
        bucketList.add(new Bucket<V>(getUnderflowLabel(), "", LabelType.UNDERFLOW));

        // fix up the list, adding underflow, additions, overflow
        // Insert inflow labels as needed.
        int prevScript = UScript.INVALID_CODE;
        int scriptIndex = -1;
        String scriptUpperBoundary = "";
        for (String current : indexCharacters) {
            int indexCharVsScriptUpper = collatorPrimaryOnly.compare(current, scriptUpperBoundary);
            // TODO start of hack: Remove this hack and the "script" variable
            // when we have a reliable Han-script first primary string.
            // Until then, we use U+4E00 as the first Han character,
            // but it is usually tailored in CJK collations.
            // When we see a Chinese index boundary string or detect that we do not really
            // cross a script boundary
            // (because the Korean tailoring interleaves Han characters with Hangul syllables)
            // we look for the next script boundary string.
            int script;
            if (current.startsWith(BASE) && !current.equals(BASE)) {
                script = UScript.HAN;
            } else {
                int c;
                for (int i = 0;; i += Character.charCount(c)) {
                    if (i == current.length()) {
                        script = prevScript;
                        break;
                    }
                    c = current.codePointAt(i);
                    int sc = UScript.getScript(c);
                    if (sc != UScript.UNKNOWN && sc != UScript.INHERITED) {
                        script = sc;
                        break;
                    }
                }
            }
            if (indexCharVsScriptUpper > 0 && scriptUpperBoundary.equals("\u4E00")) {
                if (script == UScript.HAN || script == prevScript) {
                    do {
                        scriptUpperBoundary = firstCharsInScripts.get(++scriptIndex);
                    } while (collatorPrimaryOnly.compare(current, scriptUpperBoundary) >= 0);
                    indexCharVsScriptUpper = -1;
                }
            }
            // TODO end of hack
            if (indexCharVsScriptUpper >= 0) {
                // We crossed the script boundary into a new script.
                if (indexCharVsScriptUpper > 0 && bucketList.size() > 1) {
                    // We are skipping one or more scripts.
                    bucketList.add(new Bucket<V>(getInflowLabel(), scriptUpperBoundary,
                            LabelType.INFLOW));
                }
                do {
                    scriptUpperBoundary = firstCharsInScripts.get(++scriptIndex);
                } while (collatorPrimaryOnly.compare(current, scriptUpperBoundary) >= 0);
            }
            Bucket<V> bucket = new Bucket<V>(fixLabel(current), current, LabelType.NORMAL);
            bucketList.add(bucket);
            char c;
            if (current.length() == 1 && 'A' <= (c = current.charAt(0)) && c <= 'Z') {
                asciiBuckets[c - 'A'] = bucket;
            } else if (current.length() == BASE.length() + 1 && current.startsWith(BASE) &&
                    'A' <= (c = current.charAt(BASE.length())) && c <= 'Z') {
                pinyinBuckets[c - 'A'] = bucket;
                hasPinyin = true;
            } else if (hasMultiplePrimaryWeights(cei, current) && !current.endsWith("\uffff")) {
                // "Sch" etc.
                for (int i = bucketList.size() - 2;; --i) {
                    Bucket<V> singleBucket = bucketList.get(i);
                    if (singleBucket.labelType != LabelType.NORMAL) {
                        // There is no single-character bucket since the last
                        // underflow or inflow label.
                        break;
                    }
                    if (singleBucket.displayBucket == null &&
                            !hasMultiplePrimaryWeights(cei, singleBucket.lowerBoundary)) {
                        // Add an invisible bucket that redirects strings greater than the expansion
                        // to the previous single-character bucket.
                        // For example, after ... Q R S Sch we add Sch\uFFFF->S
                        // and after ... Q R S Sch Sch\uFFFF St we add St\uFFFF->S.
                        bucket = new Bucket<V>("", current + "\uFFFF", LabelType.NORMAL);
                        bucket.displayBucket = singleBucket;
                        bucketList.add(bucket);
                        hasInvisibleBuckets = true;
                        break;
                    }
                }
            }
            prevScript = script;
        }
        if (bucketList.size() == 1) {
            // No real labels, show only the underflow label.
            return new BucketList<V>(bucketList, bucketList);
        }
        // overflow bucket
        bucketList.add(new Bucket<V>(getOverflowLabel(), scriptUpperBoundary, Bucket.LabelType.OVERFLOW)); // final

        if (hasPinyin) {
            // Redirect Pinyin buckets.
            Bucket<V> asciiBucket = null;
            for (int i = 0; i < 26; ++i) {
                if (asciiBuckets[i] != null) {
                    asciiBucket = asciiBuckets[i];
                }
                if (pinyinBuckets[i] != null && asciiBucket != null) {
                    pinyinBuckets[i].displayBucket = asciiBucket;
                    hasInvisibleBuckets = true;
                }
            }
        }

        if (!hasInvisibleBuckets) {
            return new BucketList<V>(bucketList, bucketList);
        }
        // Merge inflow buckets that are visually adjacent.
        // Iterate backwards: Merge inflow into overflow rather than the other way around.
        int i = bucketList.size() - 1;
        Bucket<V> nextBucket = bucketList.get(i);
        while (--i > 0) {
            Bucket<V> bucket = bucketList.get(i);
            if (bucket.displayBucket != null) {
                continue;  // skip invisible buckets
            }
            if (bucket.labelType == LabelType.INFLOW) {
                if (nextBucket.labelType != LabelType.NORMAL) {
                    bucket.displayBucket = nextBucket;
                    continue;
                }
            }
            nextBucket = bucket;
        }

        ArrayList<Bucket<V>> publicBucketList = new ArrayList<Bucket<V>>();
        for (Bucket<V> bucket : bucketList) {
            if (bucket.displayBucket == null) {
                publicBucketList.add(bucket);
            }
        }
        return new BucketList<V>(bucketList, publicBucketList);
    }

    private static class BucketList<V> implements Iterable<Bucket<V>> {
        private final ArrayList<Bucket<V>> bucketList;
        private final List<Bucket<V>> immutableVisibleList;

        private BucketList(ArrayList<Bucket<V>> bucketList, ArrayList<Bucket<V>> publicBucketList) {
            this.bucketList = bucketList;

            int displayIndex = 0;
            for (Bucket<V> bucket : publicBucketList) {
                bucket.displayIndex = displayIndex++;
            }
            immutableVisibleList = Collections.unmodifiableList(publicBucketList);
        }

        private int getBucketCount() {
            return immutableVisibleList.size();
        }

        private int getBucketIndex(CharSequence name, Collator collatorPrimaryOnly) {
            // binary search
            int start = 0;
            int limit = bucketList.size();
            while ((start + 1) < limit) {
                int i = (start + limit) / 2;
                Bucket<V> bucket = bucketList.get(i);
                int nameVsBucket = collatorPrimaryOnly.compare(name, bucket.lowerBoundary);
                if (nameVsBucket < 0) {
                    limit = i;
                } else {
                    start = i;
                }
            }
            Bucket<V> bucket = bucketList.get(start);
            if (bucket.displayBucket != null) {
                bucket = bucket.displayBucket;
            }
            return bucket.displayIndex;
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

    private static boolean hasMultiplePrimaryWeights(CollationElementIterator cei, String s) {
        cei.setText(s);
        boolean seenPrimary = false;
        for (;;) {
            int ce32 = cei.next();
            if (ce32 == CollationElementIterator.NULLORDER) {
                break;
            }
            int p = CollationElementIterator.primaryOrder(ce32);
            if (p != 0 && (ce32 & 0xc0) != 0xc0) {
                // not primary ignorable, and not a continuation CE
                if (seenPrimary) {
                    return true;
                }
                seenPrimary = true;
            }
        }
        return false;
    }

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
                "\u4E00",
                // TODO: The overflow bucket's lowerBoundary string should be the
                // first item after the last reordering group in the collator's script order.
                // This should normally be the first Unicode code point
                // that is unassigned (U+0378 in Unicode 6.3) and untailored.
                // However, at least up to ICU 51 the Hani reordering group includes
                // unassigned code points,
                // and there is no stable string for the start of the trailing-weights range.
                // The only known string that sorts "high" is U+FFFF.
                // When ICU separates Hani vs. unassigned reordering groups, we need to fix this,
                // and fix relevant test code.
                // Ideally, FractionalUCA.txt will have a "script first primary"
                // for unassigned code points.
                "\uFFFF"
        });

    /**
     * Return a list of the first character in each script. Only exposed for testing.
     *
     * @return list of first characters in each script
     * @internal
     * @deprecated This API is ICU internal, only for testing.
     */
    public static Collection<String> getFirstCharactersInScripts() {
        return HACK_FIRST_CHARS_IN_SCRIPTS;
    }
}

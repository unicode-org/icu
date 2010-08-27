/*
 *******************************************************************************
 * Copyright (C) 2008-2010, Google Inc, International Business Machines Corporation
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.impl.MultiComparator;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.Normalizer2.Mode;
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.text.AlphabeticIndex.Bucket;

/**
 * A class that supports the creation of a UI index appropriate for a given language, such as:
 * 
 * <pre>
 *  <b>… A B C D E F G H I J K L M N O P Q R S T U V W X Y Z Æ Ø Å …</b>
 *  
 *  <b>A</b>
 *     Addison
 *     Albertson
 *     Azensky
 *  <b>B</b>
 *     Bäcker
 *  ...
 * </pre>
 * 
 * The class can generate a list of labels for use as a UI "index", that is, a list of clickable characters (or
 * character sequences) that allow the user to see a segment (bucket) of a larger "target" list. That is, each label
 * corresponds to a bucket in the target list, where everything in the bucket is greater than or equal to the character
 * (according to the locale's collation). Strings can be added to the index; they will be in sorted order in the right
 * bucket.
 * <p>
 * The class also supports having buckets for strings before the first (underflow), after the last (overflow), and
 * between scripts (inflow). For example, if the index is constructed with labels for Russian and English, Greek
 * characters would fall into an inflow bucket between the other two scripts.
 * <p>
 * <i>Example</i>
 * <p>
 * The "show..." methods below are just to illustrate usage.
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
 *             showIndexedItem(UI, item.getName(), item.getValue());
 *         }
 * </pre>
 * 
 * The caller can build different UIs using this class. For example, an index character could be omitted or grayed-out
 * if its bucket is empty. Small buckets could also be combined based on size, such as:
 * 
 * <pre>
 * <b>… A-F G-N O-Z …</b>
 * </pre>
 * 
 * <p>
 * <b>Notes:</b>
 * <ul>
 * <li>Additional collation parameters can be passed in as part of the locale name. For example, German plus numeric
 * sorting would be "de@kn-true".
 * 
 * @author markdavis
 * @draft ICU 4.6
 * @provisional This API might change or be removed in a future release.
 */
public final class AlphabeticIndex<V> implements Iterable<Bucket<V>> {

    /**
     * Internals
     */
    static final boolean HACK_CODED_FIRSTS = true;

    private static UnicodeSet UNIHAN = new UnicodeSet("[:script=Hani:]");

    private static final char CGJ = '\u034F';
    private static final UnicodeSet ALPHABETIC = new UnicodeSet("[[:alphabetic:]-[:mark:]]");
    private static final UnicodeSet HANGUL = new UnicodeSet(
    "[\uAC00 \uB098 \uB2E4 \uB77C \uB9C8 \uBC14  \uC0AC  \uC544 \uC790  \uCC28 \uCE74 \uD0C0 \uD30C \uD558]");
    private static final UnicodeSet ETHIOPIC = new UnicodeSet("[[:Block=Ethiopic:]&[:Script=Ethiopic:]]");
    private static final UnicodeSet CORE_LATIN = new UnicodeSet("[a-z]");

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

    private List<String> indexCharacters;
    private BucketList buckets;

    private String overflowLabel = "\u2026";
    private String underflowLabel = "\u2026";
    private String inflowLabel = "\u2026";
    private LangType langType;

    /**
     * Create the index object.
     * 
     * @param locale
     *            The locale for the index.
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public AlphabeticIndex(ULocale locale) {
        this(locale, null, getIndexExemplars(locale));
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
     */
    public enum LangType { 
        /**
         * @internal
         * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
         */
        NORMAL, 
        /**
         * @internal
         * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
         */
        SIMPLIFIED,
        /**
         * @internal
         * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
         */
        TRADITIONAL;
        /**
         * @internal
         * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
         */
        public static LangType fromLocale(ULocale locale) {
            String lang = locale.getLanguage();
            if (lang.equals("zh")) {
                if ("Hant".equals(locale.getScript()) || "TW".equals(locale.getCountry())) {
                    return TRADITIONAL;
                }
                return SIMPLIFIED;
            }
            return NORMAL;
        }
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only, for testing purposes and use with CLDR.
     */
    public AlphabeticIndex(ULocale locale, RuleBasedCollator collator, UnicodeSet exemplarChars) {
        langType = LangType.fromLocale(locale);
        // HACK because we have to know the type of the collation for Chinese
        if (langType != LangType.NORMAL) {
            locale = locale.setKeywordValue("collation", langType == LangType.TRADITIONAL ? "stroke" : "pinyin");
        }
        collatorOriginal = collator != null ? (RuleBasedCollator) collator : (RuleBasedCollator) Collator.getInstance(locale);
        try {
            collatorPrimaryOnly = (RuleBasedCollator) (collatorOriginal.clone());
        } catch (CloneNotSupportedException e) {
            // should never happen
            throw new IllegalArgumentException("Collator cannot be cloned", e);
        }
        collatorPrimaryOnly.setStrength(Collator.PRIMARY);
        addLabels(exemplarChars);
    }

    /**
     * Add more index characters (aside from what are in the locale)
     * @param additions additional characters to add to the index, such as A-Z.
     * @return this, for chaining
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public AlphabeticIndex<V> addLabels(UnicodeSet additions) {
        initialLabels.addAll(additions);
        indexCharacters = null;
        return this;
    }

    /**
     * Add more index characters (aside from what are in the locale)
     * @param additions additional characters to add to the index, such as those in Swedish.
     * @return this, for chaining
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public AlphabeticIndex<V> addLabels(ULocale... additions) {
        for (ULocale addition : additions) {
            initialLabels.addAll(getIndexExemplars(addition));
        }
        indexCharacters = null;
        return this;
    }

    /**
     * Set the overflow label
     * @param overflowLabel see class description
     * @return this, for chaining
     */
    public AlphabeticIndex<V> setOverflowLabel(String overflowLabel) {
        this.overflowLabel = overflowLabel;
        return this;
    }

    /**
     * Get the default label used in the IndexCharacters' locale for underflow, eg the last item in: X Y Z …
     * 
     * @return underflow label
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public String getUnderflowLabel() {
        return underflowLabel; // TODO get localized version
    }


    /**
     * Set the underflowLabel label
     * @param underflowLabel see class description
     * @return this, for chaining
     */
    public AlphabeticIndex<V> setUnderflowLabel(String underflowLabel) {
        this.underflowLabel = underflowLabel;
        return this;
    }

    /**
     * Get the default label used in the IndexCharacters' locale for overflow, eg the first item in: … A B C
     * 
     * @return overflow label
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public String getOverflowLabel() {
        return overflowLabel; // TODO get localized version
    }


    /**
     * Set the inflowLabel label
     * @param inflowLabel see class description
     * @return this, for chaining
     */
    public AlphabeticIndex<V> setInflowLabel(String inflowLabel) {
        this.inflowLabel = inflowLabel;
        return this;
    }

    /**
     * Get the default label used for abbreviated buckets <i>between</i> other labels. For example, consider the labels
     * for Latin and Greek are used: X Y Z … &#x0391; &#x0392; &#x0393;.
     * 
     * @return inflow label
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public String getInflowLabel() {
        return inflowLabel; // TODO get localized version
    }


    /**
     * Get the limit on the number of labels in the index. The number of buckets can be slightly larger: see getBucketCount().
     * 
     * @return maxLabelCount maximum number of labels.
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public AlphabeticIndex<V> setMaxLabelCount(int maxLabelCount) {
        this.maxLabelCount = maxLabelCount;
        return this;
    }

    private void initLabels() {
        UnicodeSet exemplars = new UnicodeSet(initialLabels);

        // First sort them, with an "best" ordering among items that are the same according
        // to the collator.
        // Re the warning: the JDK inexplicably didn't make Collators be Comparator<String>!
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

        indexCharacters = Collections.unmodifiableList(new ArrayList<String>(indexCharacterSet));
    }

    private static UnicodeSet getIndexExemplars(ULocale locale) {
        UnicodeSet exemplars = LocaleData.getExemplarSet(locale, 0, LocaleData.ES_INDEX);

        if (exemplars != null) {
            return exemplars;
        }

        // Synthesize the index exemplars

        exemplars = LocaleData.getExemplarSet(locale, 0, LocaleData.ES_STANDARD);

        // get the exemplars, and handle special cases

        exemplars = exemplars.cloneAsThawed();
        // question: should we add auxiliary exemplars?
        if (exemplars.containsSome(CORE_LATIN)) {
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

    /*
     * Return the string with interspersed CGJs. Input must have more than 2 codepoints.
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
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public List<String> getLabels() {
        if (indexCharacters == null) {
            initLabels();
        }
        if (buckets == null) {
            buckets = getIndexBuckets();
        }
        ArrayList<String> result = new ArrayList<String>();
        for (Bucket<V> bucket : this) {
            result.add(bucket.getLabel());
        }
        return result;
    }

    /**
     * Get a clone of the collator used internally
     * 
     * @return a clone of the collator used internally
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public RuleBasedCollator getCollator() {
        if (collatorExternal == null) {
            try {
                collatorExternal = (RuleBasedCollator) (collatorOriginal.clone());
            } catch (CloneNotSupportedException e) {
                // should never happen
                throw new IllegalArgumentException("Collator cannot be cloned", e);
            }
        }
        return collatorExternal;
    }

    /**
     * Add a record (name and info) to the index. The name will be used to sort the items into buckets, and to sort
     * within the bucket. Two records may have the same name. When they do, the sort order is according to the order added:
     * the first added comes first.
     * 
     * @param name
     *            Name, such as a name
     * @param info
     *            Info, such as an address or link
     * @return this, for chaining
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public AlphabeticIndex<V> addRecord(CharSequence name, V info) {
        buckets = null; // invalidate old bucketlist
        inputList.add(new Record<V>(name, info, inputList.size()));
        return this;
    }

    /**
     * Clear the index.
     * 
     * @return this, for chaining
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
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
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public int getBucketCount() {
        if (buckets == null) {
            buckets = getIndexBuckets();
        }
        return buckets.bucketList.size();
    }

    /**
     * Return the number of records in the index: that is, the total number of distinct <name,info> pairs added with addRecord(...), over all the buckets.
     * 
     * @return total number of records in buckets
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public int getRecordCount() {
        return inputList.size();
    }

    /**
     * Return an iterator over the buckets.
     * 
     * @return iterator over buckets.
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public Iterator<Bucket<V>> iterator() {
        if (buckets == null) {
            buckets = getIndexBuckets();
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
     * @return List of buckets, where each bucket has a label (typically an index character) and the strings in order in
     *         that bucket.
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    private BucketList getIndexBuckets() {
        BucketList output = new BucketList();

        // Set up an array of sorted intput name/info pairs
        Comparator<Record<V>> fullComparator = new Comparator<Record<V>>() {
            public int compare(Record<V> o1, Record<V> o2) {
                CharSequence name1 = o1.substitute;
                CharSequence name2 = o2.substitute;
                if (name1 == null) {
                    name1 = o1.getName();
                }
                if (name2 == null) {
                    name2 = o2.getName();
                }
                int result = collatorOriginal.compare(name1, name2);
                if (result != 0) {
                    return result;
                }
                if (o1.substitute != null || o2.substitute != null) {
                    result = collatorOriginal.compare(o1.getName(), o2.getName());
                    if (result != 0) {
                        return result;
                    }
                }
                return o1.counter - o2.counter;
            }
        };
        if (langType == LangType.SIMPLIFIED) {
            for (Record<V> name : inputList) {
                name.substitute = hackName(name.name, collatorOriginal);
            }
        }
        TreeSet<Record<V>> sortedInput = new TreeSet<Record<V>>(fullComparator);
        sortedInput.addAll(inputList);

        Iterator<Bucket<V>> bucketIterator = output.iterator();
        Bucket<V> currentBucket = bucketIterator.next();
        Bucket<V> nextBucket = bucketIterator.next();
        String upperBoundary = nextBucket.lowerBoundary; // there is always at least one
        boolean atEnd = false;
        for (Record<V> s : sortedInput) {
            while (!atEnd && s.isGreater(collatorOriginal, upperBoundary)) {
                currentBucket = nextBucket;
                // now reset nextChar
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
            currentBucket.records.add(s);
        }
        return output;
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
        return new UnicodeSet().applyIntPropertyValue(UProperty.SCRIPT, UScript.getScript(codePoint.codePointAt(0)));
    }

    private static final UnicodeSet IGNORE_SCRIPTS = new UnicodeSet(
    "[[:sc=Common:][:sc=inherited:][:script=Unknown:][:script=braille:]]").freeze();
    private static final UnicodeSet TO_TRY = new UnicodeSet("[:^nfcqc=no:]").removeAll(IGNORE_SCRIPTS).freeze();

//    /**
//     * Returns a list of all the "First" characters of scripts, according to the collation, and sorted according to the
//     * collation.
//     * 
//     * @param ruleBasedCollator
//     *            TODO
//     * @param comparator
//     * @param lowerLimit
//     * @param testScript
//     * 
//     * @return
//     */
//
//    private static List<String> firstStringsInScript(RuleBasedCollator ruleBasedCollator) {
//        String[] results = new String[UScript.CODE_LIMIT];
//        for (String current : TO_TRY) {
//            if (ruleBasedCollator.compare(current, "a") < 0) { // TODO fix; we only want "real" script characters, not
//                // symbols.
//                continue;
//            }
//            int script = UScript.getScript(current.codePointAt(0));
//            if (results[script] == null) {
//                results[script] = current;
//            } else if (ruleBasedCollator.compare(current, results[script]) < 0) {
//                results[script] = current;
//            }
//        }
//
//        try {
//            UnicodeSet extras = new UnicodeSet();
//            UnicodeSet expansions = new UnicodeSet();
//            ruleBasedCollator.getContractionsAndExpansions(extras, expansions, true);
//            extras.addAll(expansions).removeAll(TO_TRY);
//            if (extras.size() != 0) {
//                Normalizer2 normalizer = Normalizer2.getInstance(null, "nfkc", Mode.COMPOSE);
//                for (String current : extras) {
//                    if (!TO_TRY.containsAll(current))
//                        continue;
//                    if (!normalizer.isNormalized(current) || ruleBasedCollator.compare(current, "a") < 0) {
//                        continue;
//                    }
//                    int script = UScript.getScript(current.codePointAt(0));
//                    if (results[script] == null) {
//                        results[script] = current;
//                    } else if (ruleBasedCollator.compare(current, results[script]) < 0) {
//                        results[script] = current;
//                    }
//                }
//            }
//        } catch (Exception e) {
//        } // why have a checked exception???
//
//        TreeSet<String> sorted = new TreeSet<String>(ruleBasedCollator);
//        for (int i = 0; i < results.length; ++i) {
//            if (results[i] != null) {
//                sorted.add(results[i]);
//            }
//        }
//        if (true) {
//            for (String s : sorted) {
//                System.out.println("\"" + s + "\",");
//            }
//        }
//
//        List<String> result = Collections.unmodifiableList(new ArrayList<String>(sorted));
//        return result;
//    }

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
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public static class Record<V> {
        private CharSequence substitute;
        private CharSequence name;
        private V info;
        private int counter;

        private Record(CharSequence name, V info, int counter) {
            this.name = name;
            this.info = info;
            this.counter = counter;
        }

        /**
         * @param upperBoundary
         * @return
         */
        private boolean isGreater(Comparator comparator, String upperBoundary) {
            return comparator.compare(substitute == null ? name : substitute, upperBoundary) >= 0;
        }

        /**
         * Get the name
         * 
         * @return the name
         * @draft ICU 4.6
         * @provisional This API might change or be removed in a future release.
         */
        public CharSequence getName() {
            return name;
        }

        /**
         * Get the info
         * 
         * @return the info
         * @draft ICU 4.6
         * @provisional This API might change or be removed in a future release.
         */
        public V getInfo() {
            return info;
        }

        @Override
        public String toString() {
            return name + "=" + info;
        }
    }

    /**
     * A "bucket", containing records sorted under an index string by getIndexBucketCharacters. Is created by the
     * addBucket method in BucketList. A typical implementation will provide methods getLabel(), getSpecial(), and
     * getValues().<br>
     * See com.ibm.icu.dev.test.collator.IndexCharactersTest for an example.
     * 
     * @param <V>
     *            Info type
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public static class Bucket<V> implements Iterable<Record<V>> {
        private final String label;
        private final String lowerBoundary;
        private final LabelType labelType;
        private final List<Record<V>> records = new ArrayList<Record<V>>();

        /**
         * Type of the label
         * 
         * @draft ICU 4.6
         * @provisional This API might change or be removed in a future release.
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
         * @draft ICU 4.6
         * @provisional This API might change or be removed in a future release.
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
         * @draft ICU 4.6
         * @provisional This API might change or be removed in a future release.
         */
        public String getLabel() {
            return label;
        }

        /**
         * Is a normal, underflow, overflow, or inflow bucket
         * 
         * @return is an underflow, overflow, or inflow bucket
         * @draft ICU 4.6
         * @provisional This API might change or be removed in a future release.
         */
        public LabelType getLabelType() {
            return labelType;
        }

        /**
         * Get the number of records in the bucket.
         * 
         * @return number of records in bucket
         * @draft ICU 4.6
         * @provisional This API might change or be removed in a future release.
         */
        public int size() {
            return records.size();
        }

        /**
         * Iterator over the records in the bucket
         */
        public Iterator<Record<V>> iterator() {
            return records.iterator();
        }

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
        private ArrayList<Bucket<V>> bucketList = new ArrayList<Bucket<V>>();

        BucketList() {
            // initialize indexCharacters;
            initLabels();

            bucketList.add(new Bucket<V>(getUnderflowLabel(), "", Bucket.LabelType.UNDERFLOW));

            // fix up the list, adding underflow, additions, overflow
            // insert infix labels as needed, using \uFFFF.
            String last = indexCharacters.get(0);
            bucketList.add(new Bucket<V>(last, last, Bucket.LabelType.NORMAL));
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
                        i++;
                        lastSet = set;
                    }
                }
                bucketList.add(new Bucket<V>(current, current, Bucket.LabelType.NORMAL));
                last = current;
                lastSet = set;
            }
            String limitString = getOverflowComparisonString(last);
            bucketList.add(new Bucket<V>(getOverflowLabel(), limitString, Bucket.LabelType.OVERFLOW)); // final,
            // overflow
            // bucket
        }

        public Iterator<Bucket<V>> iterator() {
            return bucketList.iterator();
        }
    }

    /**
     * HACKS
     */
    private static CharSequence hackName(CharSequence name, Comparator comparator) {
        if (!UNIHAN.contains(Character.codePointAt(name, 0))) {
            return null;
        }
        int index = Arrays.binarySearch(HACK_PINYIN_LOOKUP, name, comparator);
        if (index < 0) {
            index = -index - 2;
        }
        //if (true) return index + "";
        return "ābcdēfghjklmnōpqrstwxyz".substring(index, index + 1);
    }

    /**
     * HACKS
     */
    private static String[] HACK_PINYIN_LOOKUP = {
        "", // A 
        "\u516B", // B 
        "\u5693", // C 
        "\u5491", // D 
        "\u59B8", // E 
        "\u53D1", // F 
        "\u7324", // G 
        "\u598E", // H 
        "\u4E0C", // J 
        "\u5494", // K 
        "\u5783", // L 
        "\u5638", // M 
        "\u62FF", // N 
        "\u5662", // O 
        "\u5991", // P 
        "\u4E03", // Q 
        "\u5465", // R 
        "\u4EE8", // S 
        "\u4ED6", // T 
        "\u5C72", // W 
        "\u5915", // X 
        "\u4E2B", // Y 
        "\u5E00", // Z 
    };

    /**
     * HACKS
     */
    private static final List<String> HACK_FIRST_CHARS_IN_SCRIPTS = 
        Arrays.asList(new String[] { 
                "a", "\u03B1", "\u2C81", "\u0430", "\u2C30", "\u10D0", "\u0561", "\u05D0", "\uD802\uDD00", "\u0800", "\u0621", "\u0710", "\u0780", "\u07CA", "\u2D30", "\u1200", "\u0950", "\u0985", "\u0A74", "\u0AD0", "\u0B05", "\u0BD0", 
                "\u0C05", "\u0C85", "\u0D05", "\u0D85", "\uABC0", "\uA800", "\uA882", "\uD804\uDC83", "\u1B83", "\uD802\uDE00", "\u0E01", "\u0E81", "\uAA80", "\u0F40", "\u1C00", "\uA840", "\u1900", "\u1700", "\u1720", "\u1740", "\u1760", 
                "\u1A00", "\uA930", "\uA90A", "\u1000", "\u1780", "\u1950", "\u1980", "\u1A20", "\uAA00", "\u1B05", "\uA984", "\u1880", "\u1C5A", "\u13A0", "\u1401", "\u1681", "\u16A0", "\uD803\uDC00", "\uA500", "\uA6A0", "\u1100", 
                "\u3041", "\u30A1", "\u3105", "\uA000", "\uA4F8", "\uD800\uDE80", "\uD800\uDEA0", "\uD802\uDD20", "\uD800\uDF00", "\uD800\uDF30", "\uD801\uDC28", "\uD801\uDC50", "\uD801\uDC80", "\uD800\uDC00", "\uD802\uDC00", "\uD802\uDE60", "\uD802\uDF00", "\uD802\uDC40", 
                "\uD802\uDF40", "\uD802\uDF60", "\uD800\uDF80", "\uD800\uDFA0", "\uD808\uDC00", "\uD80C\uDC00", "\u4E00" 
        });
}

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
 *             showIndexedItem(UI, item.getKey(), item.getValue());
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

    private static final char CGJ = '\u034F';
    private static final UnicodeSet ALPHABETIC = new UnicodeSet("[[:alphabetic:]-[:mark:]]");
    private static final UnicodeSet HANGUL = new UnicodeSet(
    "[\uAC00 \uB098 \uB2E4 \uB77C \uB9C8 \uBC14  \uC0AC  \uC544 \uC790  \uCC28 \uCE74 \uD0C0 \uD30C \uD558]");
    private static final UnicodeSet ETHIOPIC = new UnicodeSet("[[:Block=Ethiopic:]&[:Script=Ethiopic:]]");
    private static final UnicodeSet CORE_LATIN = new UnicodeSet("[a-z]");

    private final RuleBasedCollator comparator;
    private final List<String> firstScriptCharacters;

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
        comparator = collator != null ? (RuleBasedCollator) collator : (RuleBasedCollator) Collator.getInstance(locale);
        comparator.setStrength(Collator.PRIMARY);
        firstScriptCharacters = FIRST_CHARS_IN_SCRIPTS;
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
        Set<String> preferenceSorting = new TreeSet<String>(new MultiComparator<Object>(comparator, PREFERENCE_COMPARATOR));
        exemplars.addAllTo(preferenceSorting);

        TreeSet<String> indexCharacterSet = new TreeSet<String>(comparator);

        // We nw make a sorted array of elements
        // Some of the input may, however, be redundant.
        // That is, we might have c, ch, d, where "ch" sorts just like "c", "h"
        // So we make a pass through, filtering out those cases.

        for (String item : preferenceSorting) {
            if (indexCharacterSet.contains(item)) {
                for (String itemAlreadyIn : indexCharacterSet) {
                    if (comparator.compare(item, itemAlreadyIn) == 0) {
                        Set<String> targets = alreadyIn.get(itemAlreadyIn);
                        if (targets == null) {
                            alreadyIn.put(itemAlreadyIn, targets = new LinkedHashSet<String>());
                        }
                        targets.add(item);
                        break;
                    }
                }
            } else if (UTF16.countCodePoint(item) > 1 && comparator.compare(item, separated(item)) == 0) {
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
        // firstStringsInScript(comparator);

        buckets = new BucketList();
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
        return indexCharacters;
    }

    /**
     * Get a clone of the collator used internally
     * 
     * @return a clone of the collator used internally
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public RuleBasedCollator getCollator() {
        try {
            return (RuleBasedCollator) (comparator.clone());
        } catch (CloneNotSupportedException e) {
            // should never happen
            throw new IllegalArgumentException("Collator cannot be cloned", e);
        }
    }

    /**
     * Add a record (key and value) to the index. The key will be used to sort the items into buckets, and to sort
     * within the bucket. Two records may have the same key. When they do, the sort order is according to the order added:
     * the first added comes first.
     * 
     * @param key
     *            Key, such as a name
     * @param value
     *            Value, such as an address or link
     * @return this, for chaining
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public AlphabeticIndex<V> addRecord(CharSequence key, V value) {
        buckets = null; // invalidate old bucketlist
        inputList.add(new Record<V>(key, value, inputList.size()));
        return this;
    }

    private static UnicodeSet UNIHAN = new UnicodeSet("[:script=Hani:]");

    /**
     * @param key
     * @return
     */
    public static CharSequence hackKey(CharSequence key, Comparator comparator) {
        if (!UNIHAN.contains(Character.codePointAt(key, 0))) {
            return null;
        }
        int index = Arrays.binarySearch(PINYIN_LOOKUP, key, comparator);
        if (index < 0) {
            index = -index - 2;
        }
        //if (true) return index + "";
        return "ābcdēfghjklmnōpqrstwxyz".substring(index, index + 1);
    }

    private static String[] PINYIN_LOOKUP = {
        //        "呵", // a
        //        "㭭", // b
        //        "䃰", // c
        //        "㙮", // d
        //        "䋪", // e
        //        "发", // f
        //        "旮", // g
        //        "哈", // h
        //        "㚻", // i = j
        //        "㚻", // j
        //        "䘔", // k
        //        "㕇", // l
        //        "呒", // m
        //        "唔", // n
        //        "喔", // o
        //        "䔤", // p
        //        "㠌", // q
        //        "儿", // r
        //        "仨", // s
        //        "㯚", // t
        //        "䨟", // u = w
        //        "䨟", // v = w
        //        "䨟", // w
        //        "㓾", // x
        //        "㝞", // y
        //        "㞉", // z
        "",     //A
        "八",    //B
        "嚓",    //C
        "咑",    //D
        "妸",    //E
        "发",    //F
        "猤",    //G
        "妎",    //H
        "丌",    //J
        "咔",    //K
        "垃",    //L
        "嘸",    //M
        "拿",    //N
        "噢",    //O
        "妑",    //P
        "七",    //Q
        "呥",    //R
        "仨",    //S
        "他",    //T
        "屲",    //W
        "夕",    //X
        "丫",    //Y
        "帀",    //Z
    };

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
     * Return the number of records in the index: that is, the number of distinct <key,value> pairs added with addRecord(...)
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

        // Set up an array of sorted intput key/value pairs
        comparator.setStrength(Collator.TERTIARY);
        Comparator<Record<V>> fullComparator = new Comparator<Record<V>>() {
            public int compare(Record<V> o1, Record<V> o2) {
                CharSequence key1 = o1.substitute;
                CharSequence key2 = o2.substitute;
                if (key1 == null) {
                    key1 = o1.getKey();
                }
                if (key2 == null) {
                    key2 = o2.getKey();
                }
                int result = comparator.compare(key1, key2);
                if (result != 0) {
                    return result;
                }
                if (o1.substitute != null || o2.substitute != null) {
                    result = comparator.compare(o1.getKey(), o2.getKey());
                    if (result != 0) {
                        return result;
                    }
                }
                return o1.counter - o2.counter;
            }
        };
        if (langType == LangType.SIMPLIFIED) {
            for (Record<V> key : inputList) {
                key.substitute = hackKey(key.key, comparator);
            }
        }
        TreeSet<Record<V>> sortedInput = new TreeSet<Record<V>>(fullComparator);
        sortedInput.addAll(inputList);
        comparator.setStrength(Collator.PRIMARY); // used for bucketing

        Iterator<Bucket<V>> bucketIterator = output.iterator();
        Bucket<V> currentBucket = bucketIterator.next();
        Bucket<V> nextBucket = bucketIterator.next();
        String upperBoundary = nextBucket.lowerBoundary; // there is always at least one
        boolean atEnd = false;
        for (Record<V> s : sortedInput) {
            while (!atEnd && s.isGreater(comparator, upperBoundary)) {
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
            currentBucket.values.add(s);
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
        for (String s : firstScriptCharacters) {
            if (comparator.compare(s, lowerLimit) > 0) {
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
        return firstScriptCharacters;
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

    private static final List<String> FIRST_CHARS_IN_SCRIPTS = 
        HACK_CODED_FIRSTS ? Arrays.asList(new String[] { "a",
                "α", "ⲁ", "а", "ⰰ", "ა", "ա", "א", "𐤀", "ࠀ", "ء", "ܐ", "ހ", "ߊ", "ⴰ", "ሀ", "ॐ", "অ", "ੴ", "ૐ", "ଅ", "ௐ",
                "అ", "ಅ", "അ", "අ", "ꯀ", "ꠀ", "ꢂ", "𑂃", "ᮃ", "𐨀", "ก", "ກ", "ꪀ", "ཀ", "ᰀ", "ꡀ", "ᤀ", "ᜀ", "ᜠ", "ᝀ", "ᝠ",
                "ᨀ", "ꤰ", "ꤊ", "က", "ក", "ᥐ", "ᦀ", "ᨠ", "ꨀ", "ᬅ", "ꦄ", "ᢀ", "ᱚ", "Ꭰ", "ᐁ", "ᚁ", "ᚠ", "𐰀", "ꔀ", "ꚠ", "ᄀ",
                "ぁ", "ァ", "ㄅ", "ꀀ", "ꓸ", "𐊀", "𐊠", "𐤠", "𐌀", "𐌰", "𐐨", "𐑐", "𐒀", "𐀀", "𐠀", "𐩠", "𐬀", "𐡀",
                "𐭀", "𐭠", "𐎀", "𐎠", "𒀀", "𓀀", "一"})
                : firstStringsInScript((RuleBasedCollator) Collator
                        .getInstance(ULocale.ROOT));

        /**
         * Returns a list of all the "First" characters of scripts, according to the collation, and sorted according to the
         * collation.
         * 
         * @param ruleBasedCollator
         *            TODO
         * @param comparator
         * @param lowerLimit
         * @param testScript
         * 
         * @return
         */

        private static List<String> firstStringsInScript(RuleBasedCollator ruleBasedCollator) {
            String[] results = new String[UScript.CODE_LIMIT];
            for (String current : TO_TRY) {
                if (ruleBasedCollator.compare(current, "a") < 0) { // TODO fix; we only want "real" script characters, not
                    // symbols.
                    continue;
                }
                int script = UScript.getScript(current.codePointAt(0));
                if (results[script] == null) {
                    results[script] = current;
                } else if (ruleBasedCollator.compare(current, results[script]) < 0) {
                    results[script] = current;
                }
            }

            try {
                UnicodeSet extras = new UnicodeSet();
                UnicodeSet expansions = new UnicodeSet();
                ruleBasedCollator.getContractionsAndExpansions(extras, expansions, true);
                extras.addAll(expansions).removeAll(TO_TRY);
                if (extras.size() != 0) {
                    Normalizer2 normalizer = Normalizer2.getInstance(null, "nfkc", Mode.COMPOSE);
                    for (String current : extras) {
                        if (!TO_TRY.containsAll(current))
                            continue;
                        if (!normalizer.isNormalized(current) || ruleBasedCollator.compare(current, "a") < 0) {
                            continue;
                        }
                        int script = UScript.getScript(current.codePointAt(0));
                        if (results[script] == null) {
                            results[script] = current;
                        } else if (ruleBasedCollator.compare(current, results[script]) < 0) {
                            results[script] = current;
                        }
                    }
                }
            } catch (Exception e) {
            } // why have a checked exception???

            TreeSet<String> sorted = new TreeSet<String>(ruleBasedCollator);
            for (int i = 0; i < results.length; ++i) {
                if (results[i] != null) {
                    sorted.add(results[i]);
                }
            }
            if (true) {
                for (String s : sorted) {
                    System.out.println("\"" + s + "\",");
                }
            }

            List<String> result = Collections.unmodifiableList(new ArrayList<String>(sorted));
            return result;
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
         * @draft ICU 4.6
         * @provisional This API might change or be removed in a future release.
         */
        public static class Record<V> {
            private CharSequence substitute;
            private CharSequence key;
            private V value;
            private int counter;

            private Record(CharSequence key, V value, int counter) {
                this.key = key;
                this.value = value;
                this.counter = counter;
                this.substitute = substitute;
            }

            /**
             * @param upperBoundary
             * @return
             */
            public boolean isGreater(Comparator comparator, String upperBoundary) {
                return comparator.compare(substitute == null ? key : substitute, upperBoundary) >= 0;
            }

            /**
             * Get the key
             * 
             * @return the key
             * @draft ICU 4.6
             * @provisional This API might change or be removed in a future release.
             */
            public CharSequence getKey() {
                return key;
            }

            /**
             * Get the value
             * 
             * @return the value
             * @draft ICU 4.6
             * @provisional This API might change or be removed in a future release.
             */
            public V getValue() {
                return value;
            }

            @Override
            public String toString() {
                return key + "=" + value;
            }
        }

        /**
         * A "bucket", containing records sorted under an index string by getIndexBucketCharacters. Is created by the
         * addBucket method in BucketList. A typical implementation will provide methods getLabel(), getSpecial(), and
         * getValues().<br>
         * See com.ibm.icu.dev.test.collator.IndexCharactersTest for an example.
         * 
         * @param <V>
         *            Value type
         * @draft ICU 4.6
         * @provisional This API might change or be removed in a future release.
         */
        public static class Bucket<V> implements Iterable<Record<V>> {
            private final String label;
            private final String lowerBoundary;
            private final LabelType labelType;
            private final List<Record<V>> values = new ArrayList<Record<V>>();

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
                return values.size();
            }

            /**
             * Iterator over the records in the bucket
             */
            public Iterator<Record<V>> iterator() {
                return values.iterator();
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
                getLabels();

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
                        if (comparator.compare(overflowComparisonString, current) < 0) {
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
}

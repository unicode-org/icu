/*
 *******************************************************************************
 * Copyright (C) 2008-2010, Google Inc, International Business Machines Corporation
 * and others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.ArrayList;
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
import com.ibm.icu.text.Index.Bucket;

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
 * (according to the locale's collation). Strings can be added to the index; they will be in sorted order in the right bucket.
 * <p>
 * The class also supports having buckets for strings before the first (underflow), after the last (overflow), and between
 * scripts (inflow). For example, if the index is constructed with labels for Russian and English, Greek characters
 * would fall into an inflow bucket between the other two scripts.
 * <p>
 * <i>Example</i>
 * <p>
 * The "show..." methods below are just to illustrate usage.
 * 
 * <pre>
 * // Create a simple index where the values for the strings are Integers, and add the strings
 * 
 * Index<Integer> index = new Index<Integer>(desiredLocale, additionalLocale);
 * int counter = 0;
 * for (String item : test) {
 *     index.add(item, counter++); 
 * }
 * ...
 * // Show index at top. We could skip or gray out empty buckets
 * 
 * for (Index.Bucket<Integer> bucket : index) {
 *     if (showAll || bucket.size() != 0) {
 *         showLabelAtTopInUI(buffer, bucket.getLabel());
 *     }
 * }
 *  ...
 * // Show the buckets with their contents, skipping empty buckets
 * 
 * for (Index.Bucket<Integer> bucket : index) {
 *     if (bucket.size() != 0) {
 *         showLabelInUIList(buffer, bucket.getLabel());
 *         for (Index.Record<Integer> item : bucket) {
 *             showIndexedItemInUI(buffer, item.getKey(), item.getValue());
 *         }
 * </pre>
 * 
 * The caller can build different UIs using this class. For example, an index character could be omitted or grayed-out
 * if its bucket is empty. Small buckets could also be combined based on size, such as:
 * 
 * <pre>
 *  … A-F G-N O-Z …
 * </pre>
 * 
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>For Chinese, the results are not yet optimal, and it is probably best best not to use these index characters. The
 * class can still be used to get the correct sorting order, but the index characters should be suppressed.</li>
 * <li>Additional collation parameters can be passed in as part of the locale name. For example, German plus numeric
 * sorting would be "de@kn-true".
 * <li>In the initial version, a limit of 100 items is placed on these lists. This may change or become configureable in
 * the future. When the limit is reached, then every nth value is removed to bring the list down below the limit.</li>
 * </ul>
 * 
 * @author markdavis
 * @draft ICU 4.2
 * @provisional This API might change or be removed in a future release.
 */
public class Index<V extends Comparable<V>> implements Iterable<Bucket<V>> {

    private static final char CGJ = '\u034F';
    private static final UnicodeSet ALPHABETIC = new UnicodeSet("[[:alphabetic:]-[:mark:]]");
    private static final UnicodeSet HANGUL = new UnicodeSet(
    "[\uAC00 \uB098 \uB2E4 \uB77C \uB9C8 \uBC14  \uC0AC  \uC544 \uC790  \uCC28 \uCE74 \uD0C0 \uD30C \uD558]");
    private static final UnicodeSet ETHIOPIC = new UnicodeSet("[[:Block=Ethiopic:]&[:Script=Ethiopic:]]");
    private static final UnicodeSet CORE_LATIN = new UnicodeSet("[a-z]");

    private final RuleBasedCollator comparator;
    private final List<String> indexCharacters;
    private final LinkedHashMap<String, Set<String>> alreadyIn = new LinkedHashMap<String, Set<String>>();
    private final List<String> noDistinctSorting = new ArrayList<String>();
    private final List<String> notAlphabetic = new ArrayList<String>();
    private final List<String> firstScriptCharacters;    
    private Collection<Record<V>> inputList = new ArrayList<Record<V>>();
    private BucketList buckets; 

    /**
     * Create the index object.
     * 
     * @param locale
     *            The locale for the index.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public Index(ULocale locale) {
        this(locale, (RuleBasedCollator) Collator.getInstance(locale), null, null);
    }

    /**
     * Create the index object.
     * 
     * @param locale
     *            The locale to be passed.
     * @param additions
     *            Additional characters to be added, eg A-Z for non-Latin locales.
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public Index(ULocale locale, UnicodeSet additions) {
        this(locale, (RuleBasedCollator) Collator.getInstance(locale), null, additions);
    }

    /**
     * Create the index object.
     * 
     * @param locale
     *            The locale for the index.
     * @param additionalLocales
     *            Additional characters to be added based on the index characters for those locales.
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public Index(ULocale locale, ULocale... additionalLocales) {
        this(locale, (RuleBasedCollator) Collator.getInstance(locale), null, getIndexExemplars(additionalLocales));
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only, for testing purposes.
     */
    public Index(ULocale locale, RuleBasedCollator collator, UnicodeSet exemplarChars, UnicodeSet additions) {
        comparator = (RuleBasedCollator) collator;
        comparator.setStrength(Collator.PRIMARY);

        boolean[] explicitIndexChars = { true };
        UnicodeSet exemplars = exemplarChars != null ? exemplarChars : getIndexExemplars(locale, explicitIndexChars);

        if (additions != null) {
            exemplars.addAll(additions);
        }

        // first sort them, with an "best" ordering among items that are the same according
        // to the collator
        Set<String> preferenceSorting = new TreeSet<String>(new MultiComparator<Object>(comparator,
                PREFERENCE_COMPARATOR));
        exemplars.addAllTo(preferenceSorting);

        TreeSet<String> indexCharacterSet = new TreeSet<String>(comparator);

        // We nw make a sorted array of elements, uppercased
        // Some of the input may, however, be redundant.
        // That is, we might have c, ch, d, where "ch" sorts just like "c", "h"
        // So we make a pass through, filtering out those cases.

        for (String item : preferenceSorting) {
            if (!explicitIndexChars[0]) {
                item = UCharacter.toUpperCase(locale, item);
            }
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
        if (size > maxCount) {
            int count = 0;
            int old = -1;
            for (Iterator<String> it = indexCharacterSet.iterator(); it.hasNext();) {
                ++count;
                it.next();
                final int bump = count * maxCount / size;
                if (bump == old) {
                    it.remove();
                } else {
                    old = bump;
                }
            }
        }
        indexCharacters = Collections.unmodifiableList(new ArrayList<String>(indexCharacterSet));
        firstScriptCharacters = FIRST_CHARS_IN_SCRIPTS; // TODO, use collation method when fast enough.
        // firstStringsInScript(comparator);

        buckets = new BucketList(indexCharacters);
    }

    private static UnicodeSet getIndexExemplars(ULocale locale, boolean[] explicitIndexChars) {
        UnicodeSet exemplars = LocaleData.getExemplarSet(locale, 0, LocaleData.ES_INDEX);

        if (exemplars != null) {
            explicitIndexChars[0] = true;
            return exemplars;
        }
        explicitIndexChars[0] = false;

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
        return exemplars;
    }

    private static UnicodeSet getIndexExemplars(ULocale... additionalLocales) {
        UnicodeSet additions = new UnicodeSet();
        boolean[] explicitIndexChars = { true };
        for (ULocale other : additionalLocales) {
            additions.addAll(getIndexExemplars(other, explicitIndexChars));
        }
        return additions;
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
     * @return A collection including the labels
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public List<String> getLabels() {
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
     * Get the default label used for abbreviated buckets <i>between</i> other labels. For example, consider
     * the labels for Latin and Greek are used: X Y Z … &#x0391; &#x0392; &#x0393;.
     * 
     * @return inflow label
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public String getInflowLabel() {
        return "\u2026"; // TODO get localized version
    }

    /**
     * Get the default label used in the IndexCharacters' locale for overflow, eg the first item in: … A B C
     * 
     * @return overflow label
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public String getOverflowLabel() {
        return "\u2026"; // TODO get localized version
    }

    /**
     * Get the default label used in the IndexCharacters' locale for underflow, eg the last item in: X Y Z …
     * 
     * @return underflow label
     * @draft ICU 4.6
     * @provisional This API might change or be removed in a future release.
     */
    public String getUnderflowLabel() {
        return "\u2026"; // TODO get localized version
    }

    public Index<V> add(CharSequence key, V value) {
        buckets = null; // invalidate old bucketlist
        inputList.add(new Record<V>(key, value));
        return this;
    }

    public int size() {
        if (buckets == null) {
            buckets = getIndexBuckets();
        }
        return buckets.bucketList.size();
    }

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
        BucketList output = new BucketList(indexCharacters);

        // Set up an array of sorted intput key/value pairs
        comparator.setStrength(Collator.TERTIARY);
        Comparator<Record<V>> fullComparator = new Comparator<Record<V>>() {
            public int compare(Record<V> o1, Record<V> o2) {
                CharSequence key1 = o1.getKey();
                CharSequence key2 = o2.getKey();
                int result = comparator.compare(key1, key2);
                if (result != 0)
                    return result;
                return o1.getValue().compareTo(o2.getValue());
            }
        };
        TreeSet<Record<V>> sortedInput = new TreeSet<Record<V>>(fullComparator);
        sortedInput.addAll(inputList);
        comparator.setStrength(Collator.PRIMARY); // used for bucketing

        Iterator<Bucket<V>> bucketIterator = output.iterator();
        Bucket<V> currentBucket = bucketIterator.next();
        Bucket<V> nextBucket = bucketIterator.next();
        String upperBoundary = nextBucket.lowerBoundary; // there is always at least one
        boolean atEnd = false;
        for (Record<V> s : sortedInput) {
            while (!atEnd && comparator.compare(s.getKey(), upperBoundary) >= 0) {
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
     * As the index is built, items may be discarded from the exemplars. This contains some of the discards, and is
     * intended for debugging.
     * 
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public Map<String, Set<String>> getAlreadyIn() {
        return alreadyIn;
    }

    /**
     * As the index is built, items may be discarded from the exemplars. This contains some of the discards, and is
     * intended for debugging.
     * 
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public List<String> getNoDistinctSorting() {
        return noDistinctSorting;
    }

    /**
     * As the index is built, items may be discarded from the exemplars. This contains some of the discards, and is
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

    private static final List<String> FIRST_CHARS_IN_SCRIPTS = firstStringsInScript((RuleBasedCollator) Collator
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
        return Collections.unmodifiableList(new ArrayList<String>(sorted));
    }

    private static final PreferenceComparator PREFERENCE_COMPARATOR = new PreferenceComparator();
    private int maxCount = 99;

    /**
     * Comparator that returns "better" items first, where shorter NFKD is better, and otherwise NFKD binary order is
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
        private CharSequence key;
        private V value;

        private Record(CharSequence key, V value) {
            this.key = key;
            this.value = value;
        }

        public CharSequence getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

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
     * @param <V> Value type
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
         */
        public enum LabelType {NORMAL, UNDERFLOW, INFLOW, OVERFLOW}

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
         * Is an underflow, overflow, or inflow bucket
         * 
         * @return is an underflow, overflow, or inflow bucket
         */
        public LabelType getLabelType() {
            return labelType;
        }

        public int size() {
            return values.size();
        }

        public Iterator<Record<V>> iterator() {
            return values.iterator();
        }
    }

    private class BucketList implements Iterable<Bucket<V>> {
        private ArrayList<Bucket<V>> bucketList = new ArrayList<Bucket<V>>();

        BucketList(List<String> indexChars) {
            bucketList.add(new Bucket<V>(getUnderflowLabel(), "", Bucket.LabelType.UNDERFLOW));

            // fix up the list, adding underflow, additions, overflow
            // insert infix labels as needed, using \uFFFF.
            String last = indexChars.get(0);
            bucketList.add(new Bucket<V>(last, last, Bucket.LabelType.NORMAL));
            UnicodeSet lastSet = getScriptSet(last).removeAll(IGNORE_SCRIPTS);
            for (int i = 1; i < indexCharacters.size(); ++i) {
                String current = indexCharacters.get(i);
                UnicodeSet set = getScriptSet(current).removeAll(IGNORE_SCRIPTS);
                if (lastSet.containsNone(set)) {
                    // check for adjacent
                    String overflowComparisonString = getOverflowComparisonString(last);
                    if (comparator.compare(overflowComparisonString, current) < 0) {
                        bucketList.add(new Bucket<V>(getInflowLabel(), overflowComparisonString, Bucket.LabelType.INFLOW));
                        i++;
                        lastSet = set;
                    }
                }
                bucketList.add(new Bucket<V>(current, current, Bucket.LabelType.NORMAL));
                last = current;
                lastSet = set;
            }
            String limitString = getOverflowComparisonString(last);
            bucketList.add(new Bucket<V>(getOverflowLabel(), limitString, Bucket.LabelType.OVERFLOW)); // final, overflow bucket
        }

        public Iterator<Bucket<V>> iterator() {
            return bucketList.iterator();
        }
    }
}

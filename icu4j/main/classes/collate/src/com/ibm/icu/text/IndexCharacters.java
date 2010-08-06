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

/**
 * A set of characters for use as a UI "index", that is, a list of clickable characters (or character sequences) that
 * allow the user to see a segment (bucket) of a larger "target" list. That is, each character corresponds to a bucket in the
 * target list, where everything in the bucket is greater than or equal to the character (according to the locale's
 * collation). This class produces the index list, and also has a convenience method for producing the sorted buckets.
 * <p>
 * A list would be presented as something like
 * <pre>
 *  A B C D E F G H I J K L M N O P Q R S T U V W X Y Z
 * </pre>
 * 
 * In the UI, an index character could be omitted if its bucket is empty. For example, if there is nothing in the bucket
 * for Q, then Q could be omitted.
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>Although we say "character" above, the index character could be a sequence, like "CH".</li>
 * <li>There could be items in a target list that are less than the first or (much) greater than the last; examples
 * include words from other scripts. The UI could have some symbol
 * for those categories. There are suggested symbols supplied via methods, and also for a bucket that is "between" scripts.</li>
 * <li>For languages without widely accepted sorting methods (eg Chinese/Japanese) the results may appear arbitrary, and
 * it may be best not to use these methods.</li>
 * <li>Additional collation parameters can be passed in as part of the locale name. For example, German plus numeric sorting would be "de@kn-true"
 * <li>In the initial version, an arbitrary limit of 100 items is placed on these lists.</li>
 * </ul>
 * 
 * @author markdavis
 * @draft ICU 4.2
 * @provisional This API might change or be removed in a future release.
 */
public class IndexCharacters {

    private static final char OVERFLOW_MARKER = '\uFFFF';
    private static final char INFLOW_MARKER = '\uFFFD';
    private static final char CGJ = '\u034F';
    private static final UnicodeSet ALPHABETIC = new UnicodeSet("[[:alphabetic:]-[:mark:]]");
    private static final UnicodeSet HANGUL = new UnicodeSet(
    "[\uAC00 \uB098 \uB2E4 \uB77C \uB9C8 \uBC14  \uC0AC  \uC544 \uC790  \uCC28 \uCE74 \uD0C0 \uD30C \uD558]");
    private static final UnicodeSet ETHIOPIC = new UnicodeSet("[[:Block=Ethiopic:]&[:Script=Ethiopic:]]");
    private static final UnicodeSet CORE_LATIN = new UnicodeSet("[a-z]");

    private final ULocale locale;
    private final RuleBasedCollator comparator;
    private final List<String> indexCharacters;
    private final LinkedHashMap<String, Set<String>> alreadyIn = new LinkedHashMap<String, Set<String>>();
    private final List<String> noDistinctSorting = new ArrayList<String>();
    private final List<String> notAlphabetic = new ArrayList<String>();
    private final List<String> firstScriptCharacters;

    /**
     * Create the index object.
     * 
     * @param locale
     *            The locale to be passed.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public IndexCharacters(ULocale locale) {
        this(locale, (RuleBasedCollator) Collator.getInstance(locale), null, null);
    }

    /**
     * Create the index object.
     * 
     * @param locale
     *            The locale to be passed.
     * @param additions
     *            Additional characters to be added, eg A-Z for non-Latin locales.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public IndexCharacters(ULocale locale, UnicodeSet additions) {
        this(locale, (RuleBasedCollator) Collator.getInstance(locale), null, additions);
    }

    /**
     * Create the index object.
     * 
     * @param locale
     *            The locale to be passed.
     * @param additions
     *            Additional characters to be added, eg A-Z for non-Latin locales.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public IndexCharacters(ULocale locale, ULocale... additionalLocales) {
        this(locale, (RuleBasedCollator) Collator.getInstance(locale), null, getIndexExemplars(additionalLocales));
    }

    /**
     * @internal
     * @param exemplarChars TODO
     * @deprecated This API is ICU internal only, for testing purposes.
     */
    @SuppressWarnings("unchecked")
    public IndexCharacters(ULocale locale, RuleBasedCollator collator, UnicodeSet exemplarChars, UnicodeSet additions) {
        this.locale = locale;
        comparator = (RuleBasedCollator) collator;
        comparator.setStrength(Collator.PRIMARY);


        boolean[] explicitIndexChars = {true};
        UnicodeSet exemplars = exemplarChars != null ? exemplarChars : getIndexExemplars(locale, explicitIndexChars);

        if (additions != null) {
            exemplars.addAll(additions);
        }

        // first sort them, with an "best" ordering among items that are the same according
        // to the collator
        Set<String> preferenceSorting = new TreeSet<String>(new MultiComparator<Object>(comparator, PREFERENCE_COMPARATOR));
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
        indexCharacters = Collections.unmodifiableList(new ArrayList(indexCharacterSet));
        firstScriptCharacters = FIRST_CHARS_IN_SCRIPTS; // TODO, use collation method when fast enough. firstStringsInScript(comparator);
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
        boolean[] explicitIndexChars = {true};
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
     * Get the index characters.
     * 
     * @return A collection including the index characters
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public List<String> getIndexCharacters() {
        return indexCharacters;
    }

    /**
     * Get the locale
     * 
     * @return The locale.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public ULocale getLocale() {
        return locale;
    }

    /**
     * Get a clone of the collator used internally
     * 
     * @return a clone of the collator used internally
     * @draft ICU 4.4
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
     * Get the default label used for abbreviated buckets <i>between</i> other index characters. For example, consider
     * the index characters for Latin and Greek are used: X Y Z … &#x0391; &#x0392; &#x0393;.
     * 
     * @return inflow label
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public String getInflowLabel() {
        return "\u2026"; // TODO get localized version
    }

    /**
     * Get the default label used in the IndexCharacters' locale for overflow, eg the first item in: … A B C
     * 
     * @return overflow label
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public String getOverflowLabel() {
        return "\u2026"; // TODO get localized version
    }

    /**
     * Get the default label used in the IndexCharacters' locale for underflow, eg the last item in: X Y Z …
     * 
     * @return underflow label
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public String getUnderflowLabel() {
        return "\u2026"; // TODO get localized version
    }

    /**
     * Associates a label with a set of values V in the bucket for that label. Used for the return value from
     * getIndexBucketCharacters.
     * 
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static class Bucket {
        private String label;
        boolean isSpecial;
        private List<String> values = new ArrayList<String>();

        /**
         * Set up the bucket.
         * 
         * @param label label for the bucket
         * @param special is an underflow, overflow, or inflow bucket
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        public Bucket(String label, boolean special) {
            this.label = label;
            isSpecial = special;
        }

        /**
         * Get the label
         * 
         * @return label for the bucket
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        public String getLabel() {
            return label;
        }
        
        /**
         * Is an underflow, overflow, or inflow bucket
         * @return is an underflow, overflow, or inflow bucket
         */
        public boolean isSpecial() {
            return isSpecial;
        }

        /**
         * Add a value to a bucket.
         * 
         * @param value value to add to the bucket
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        public void add(String value) {
            getValues().add(value);
        }

        /**
         * Get the values.
         * 
         * @return values in the bucket, in order
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        public List<String> getValues() {
            return values;
        }
    }

    /**
     * Convenience routine to bucket a list of input strings according to the index.<br>
     * Warning: if a UI suppresses buckets that are empty,
     * this may result in the special buckets (underflow, overflow, inflow) being adjacent.
     * In that case, the application may want to combine them.
     * 
     * @param inputList
     *            List of strings to be sorted and bucketed according to the index characters.
     * @return List of buckets, where each bucket has a label (typically an index character) and the strings in order in
     *         that bucket.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public <T extends CharSequence> List<Bucket> getIndexBuckets(Collection<T> inputList) {
        // fix up the list, adding underflow, additions, overflow
        List<String> characters = new ArrayList<String>(indexCharacters);

        // insert infix labels as needed, using \uFFFF.
        String last = characters.get(0);
        UnicodeSet lastSet = getScriptSet(last).removeAll(IGNORE_SCRIPTS);
        for (int i = 1; i < characters.size(); ++i) {
            String current = characters.get(i);
            UnicodeSet set = getScriptSet(current).removeAll(IGNORE_SCRIPTS);
            if (lastSet.containsNone(set)) {
                // check for adjacent
                String overflowComparisonString = getOverflowComparisonString(last);
                if (comparator.compare(overflowComparisonString, current) < 0) {
                    characters.add(i, INFLOW_MARKER + overflowComparisonString);
                    i++;
                    lastSet = set;
                }
            }
            last = current;
            lastSet = set;
        }

        String beforeMarker = getUnderflowLabel();
        String afterMarker = getOverflowLabel();
        String inMarker = getInflowLabel();
        String limitString = getOverflowComparisonString(characters.get(characters.size() - 1));
        characters.add(OVERFLOW_MARKER + limitString); // final, overflow bucket

        // Set up an array of sorted elements
        String[] sortedInput = inputList.toArray(new String[inputList.size()]);
        comparator.setStrength(Collator.TERTIARY);
        Arrays.sort(sortedInput, 0, sortedInput.length, comparator);
        comparator.setStrength(Collator.PRIMARY); // used for bucketing
        List<Bucket> buckets = new ArrayList<Bucket>(); // Can't use Map,
        // because keys might
        // not be unique

        Bucket currentBucket;
        buckets.add(currentBucket = new Bucket(beforeMarker, true));
        Iterator<String> characterIterator = characters.iterator();
        String nextChar = characterIterator.next(); // there is always at least one
        String nextLabel = nextChar;
        // one
        boolean atEnd = false;
        boolean nextCharIsSpecial = false;
        for (String s : sortedInput) {
            while (!atEnd && comparator.compare(s, nextChar) >= 0) {
                buckets.add(currentBucket = new Bucket(nextLabel, nextCharIsSpecial));
                nextCharIsSpecial = false;
                // now reset nextChar
                if (characterIterator.hasNext()) {
                    nextLabel = nextChar = characterIterator.next();
                    switch (nextChar.charAt(0)) {
                    case INFLOW_MARKER:
                        nextChar = nextChar.substring(1); // the rest of the string is the comparison value
                        nextLabel = inMarker;
                        nextCharIsSpecial = true;
                        break;
                    case OVERFLOW_MARKER:
                        nextChar = nextChar.substring(1); // the rest of the string is the comparison value
                        nextLabel = afterMarker;
                        nextCharIsSpecial = true;
                        break;
                    }
                } else {
                    atEnd = true;
                }
            }
            currentBucket.add(s);
        }
        return buckets;
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
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
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
     * @return
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

    private static final UnicodeSet IGNORE_SCRIPTS = new UnicodeSet("[[:sc=Common:][:sc=inherited:][:script=Unknown:][:script=braille:]]").freeze();
    private static final UnicodeSet TO_TRY = new UnicodeSet("[:^nfcqc=no:]").removeAll(IGNORE_SCRIPTS).freeze();

    private static final List<String> FIRST_CHARS_IN_SCRIPTS = firstStringsInScript((RuleBasedCollator) Collator.getInstance(ULocale.ROOT));

    /**
     * Returns a list of all the "First" characters of scripts, according to the collation, and sorted according to the
     * collation.
     * @param ruleBasedCollator TODO
     * @param comparator
     * @param lowerLimit
     * @param testScript
     * 
     * @return
     */

    private static List<String> firstStringsInScript(RuleBasedCollator ruleBasedCollator) {
        String[] results = new String[UScript.CODE_LIMIT];
        for (String current : TO_TRY) {
            if (ruleBasedCollator.compare(current, "a") < 0) { // TODO fix; we only want "real" script characters, not symbols.
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
                    if (!TO_TRY.containsAll(current)) continue;
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
        } catch (Exception e) {} // why have a checked exception???

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
}

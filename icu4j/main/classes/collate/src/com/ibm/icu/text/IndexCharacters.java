/*
 *******************************************************************************
 * Copyright (C) 2008-2009, Google Inc, International Business Machines Corporation
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
import com.ibm.icu.util.LocaleData;
import com.ibm.icu.util.ULocale;

/**
 * A set of characters for use as a UI "index", that is, a
 * list of clickable characters (or character sequences) that allow the user to
 * see a segment of a larger "target" list. That is, each character corresponds
 * to a bucket in the target list, where everything in the bucket is greater
 * than or equal to the character (according to the locale's collation). The
 * intention is to have two main functions; one that produces an index list that
 * is relatively static, and the other is a list that produces roughly
 * equally-sized buckets. Only the first is currently provided.
 * <p>
 * The static list would be presented as something like
 * 
 * <pre>
 *  A B C D E F G H I J K L M N O P Q R S T U V W X Y Z
 * </pre>
 * 
 * In the UI, an index character could be omitted if its bucket is empty. For
 * example, if there is nothing in the bucket for Q, then Q could be omitted.
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>Although we say "character" above, the index character could be a
 * sequence, like "CH".</li>
 * <li>There could be items in a target list that are less than the first or
 * (much) greater than the last; examples include words from other scripts. The
 * UI could bucket them with the first or last respectively, or have some symbol
 * for those categories.</li>
 * <li>The use of the list requires that the target list be sorted according to
 * the locale that is used to create that list.</li>
 * <li>For languages without widely accepted sorting methods (eg Chinese/Japanese)
 * the results may appear arbitrary, and it may be best not to use these methods.</li>
 * <li>In the initial version, an arbitrary limit of 100 is placed on these lists.</li>
 * </ul>
 * 
 * @author markdavis
 * @draft ICU 4.2
 * @provisional This API might change or be removed in a future release.
 */
//TODO(markdavis) return an additional character that is the "least greater" character than
//the last character.
public class IndexCharacters {
    public static final char CGJ = '\u034F';
    private static final UnicodeSet ALPHABETIC = new UnicodeSet("[[:alphabetic:]-[:mark:]]");
    private static final UnicodeSet HANGUL = new UnicodeSet("[\uAC00 \uB098 \uB2E4 \uB77C \uB9C8 \uBC14  \uC0AC  \uC544 \uC790  \uCC28 \uCE74 \uD0C0 \uD30C \uD558]");
    private static final UnicodeSet ETHIOPIC = new UnicodeSet("[[:Block=Ethiopic:]&[:Script=Ethiopic:]]");
    private static final UnicodeSet CORE_LATIN = new UnicodeSet("[a-z]");

    private ULocale locale;
    private Collator comparator;
    private Set<String> indexCharacters;
    private LinkedHashMap<String, Set<String>> alreadyIn = new LinkedHashMap<String, Set<String>>();
    private List<String> noDistinctSorting = new ArrayList<String>();
    private List<String> notAlphabetic = new ArrayList<String>();

    /**
     * Create the index object.
     * @param locale The locale to be passed.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    @SuppressWarnings("unchecked")
    public IndexCharacters(ULocale locale) {
        this.locale = locale;
        comparator = Collator.getInstance(locale);
        comparator.setStrength(Collator.PRIMARY);

        // get the exemplars, and handle special cases

        UnicodeSet exemplars = LocaleData.getExemplarSet(locale, LocaleData.ES_STANDARD);
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

        // first sort them, with an "best" ordering among items that are the same according
        // to the collator
        Comparator<Object>[] comparators = (Comparator<Object>[])new Comparator[2];
        comparators[0] = comparator;
        comparators[1] = new PreferenceComparator(Collator.getInstance(locale));

        Set<String> preferenceSorting = new TreeSet<String>(new MultiComparator<Object>(comparators));
        for (UnicodeSetIterator it = new UnicodeSetIterator(exemplars); it.next();) {
            preferenceSorting.add(it.getString());
        }

        indexCharacters = new TreeSet<String>(comparator);

        // We nw make a sorted array of elements, uppercased
        // Some of the input may, however, be redundant.
        // That is, we might have c, ch, d, where "ch" sorts just like "c", "h"
        // So we make a pass through, filtering out those cases.

        for (String item : preferenceSorting) {
            item = UCharacter.toUpperCase(locale, item);
            if (indexCharacters.contains(item)) {
                for (String itemAlreadyIn : indexCharacters) {
                    if (comparator.compare(item, itemAlreadyIn) == 0) {
                        Set<String> targets = alreadyIn.get(itemAlreadyIn);
                        if (targets == null) {
                            alreadyIn.put(itemAlreadyIn, targets = new LinkedHashSet<String>());
                        }
                        targets.add(item);
                        break;
                    }
                }
            } else if (UTF16.countCodePoint(item) > 1 && comparator.compare(item, separated(item)) == 0){
                noDistinctSorting.add(item);
            } else if (!ALPHABETIC.containsSome(item)) {
                notAlphabetic.add(item);
            } else {
                indexCharacters.add(item);
            }
        }

        // if the result is still too large, cut down to 100 elements

        final int size = indexCharacters.size() - 1;
        if (size > 99) {
            int count = 0;
            int old = -1;
            for (Iterator<String> it = indexCharacters.iterator(); it.hasNext();) {
                ++ count;
                it.next();
                final int bump = count * 99 / size;
                if (bump == old) {
                    it.remove();
                } else {
                    old = bump;
                }   
            }
        }
        indexCharacters = Collections.unmodifiableSet(indexCharacters);
    }

    /*
     * Return the string with interspersed CGJs. Input must have more than 2 codepoints.
     */
    private String separated(String item) {
        StringBuffer result = new StringBuffer();
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
     * @return A collection including the index characters
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public Collection<String> getIndexCharacters() {
        return indexCharacters;
    }

    /**
     * Get the locale
     * @return The locale.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public ULocale getLocale() {
        return locale;
    }

    /**
     * As the index is built, items may be discarded from the exemplars.
     * This contains some of the discards, and is intended for debugging.
     * @internal
     */
    public Map<String, Set<String>> getAlreadyIn() {
        return alreadyIn;
    }

    /**
     * As the index is built, items may be discarded from the exemplars.
     * This contains some of the discards, and is intended for debugging.
     * @internal
     */
    public List<String> getNoDistinctSorting() {
        return noDistinctSorting;
    }

    /**
     * As the index is built, items may be discarded from the exemplars.
     * This contains some of the discards, and is intended for debugging.
     * @internal
     */
    public List<String> getNotAlphabetic() {
        return notAlphabetic;
    }

    /*
     * Comparator that returns "better" items first, where shorter NFKD is better,
     * and otherwise NFKD binary order is better, and otherwise binary order is better.
     */
    private static class PreferenceComparator implements Comparator<Object> {
        static final Comparator<String> binary = new UTF16.StringComparator(true,false,0);
        final Collator collator;

        public PreferenceComparator(Collator collator) {
            this.collator = collator;
        }

        public int compare(Object o1, Object o2) {
            return compare((String)o1, (String)o2);
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
            result = collator.compare(n1, n2);
            if (result != 0) {
                return result;
            }
            return binary.compare(s1, s2);
        }
    }
}

/*
 *******************************************************************************
 * Copyright (C) 2008, Google Inc, International Business Machines Corporation
 * and others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
 * </ul>
 * 
 * @author markdavis
 * @draft
 */
// TODO(markdavis) return an additional character that is the "least greater" character than
// the last character.
public class IndexCharacters {
    public static final char CGJ = '\u034F';
    private static final UnicodeSet ALPHABETIC = new UnicodeSet("[:alphabetic:]");
    private static final UnicodeSet HANGUL = new UnicodeSet("[\uAC00 \uB098 \uB2E4 \uB77C \uB9C8 \uBC14  \uC0AC  \uC544 \uC790  \uCC28 \uCE74 \uD0C0 \uD30C \uD558]");
    private static final UnicodeSet ETHIOPIC = new UnicodeSet("[[:Block=Ethiopic:]&[:Script=Ethiopic:]]");
    private static final UnicodeSet CORE_LATIN = new UnicodeSet("[a-zA-Z]");

    private ULocale locale;
    private Collator comparator;
    private Set indexCharacters;
    private List alreadyIn = new ArrayList();
    private List noDistinctSorting = new ArrayList();
    private List notAlphabetic = new ArrayList();

    /**
     * Create the index object.
     * @param locale
     * @draft
     */
    public IndexCharacters(ULocale locale) {
        this.locale = locale;
        comparator = Collator.getInstance(locale);
        comparator.setStrength(Collator.PRIMARY);
        UnicodeSet exemplars = LocaleData.getExemplarSet(locale, LocaleData.ES_STANDARD);
        indexCharacters = new TreeSet(comparator);
        // special cases
        if (exemplars.containsSome(CORE_LATIN)) {
            exemplars.addAll(CORE_LATIN);
        }
        if (exemplars.containsSome(HANGUL)) {
            // cut down to small list
            exemplars.removeAll(new UnicodeSet("[:block=hangul_syllables:]")).addAll(HANGUL);
        }
        if (exemplars.containsSome(ETHIOPIC)) {
            // cut down to small list
            for (UnicodeSetIterator it = new UnicodeSetIterator(ETHIOPIC); it.next();) {
                if ((it.codepoint & 0x7) != 0) {
                    exemplars.remove(it.codepoint);
                }
            }
        }

        // We make a sorted array of elements, uppercased
        // Some of the input may, however, be redundant.
        // That is, we might have c, ch, d, where "ch" sorts just like "c", "h"
        // So we make a pass through, filtering out those cases.
        for (UnicodeSetIterator it = new UnicodeSetIterator(exemplars); it.next();) {
            String item = it.getString();
            item = UCharacter.toUpperCase(locale, item);
            if (indexCharacters.contains(item)) {
                alreadyIn.add(item);
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
            for (Iterator it = indexCharacters.iterator(); it.hasNext();) {
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

    /**
     * Return the string with interspersed CGJs. Input must have more than 2 codepoints.
     * @param item
     * @return
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
     * @return
     * @draft
     */
    public Collection getIndexCharacters() {
        return indexCharacters;
    }

    /**
     * Get the locale
     * @return
     * @draft
     */
    public ULocale getLocale() {
        return locale;
    }

    /**
     * As the index is built, items may be discarded from the exemplars.
     * This contains some of the discards, and is intended for debugging.
     * @return
     * @draft
     */
    public List getAlreadyIn() {
        return alreadyIn;
    }

    /**
     * As the index is built, items may be discarded from the exemplars.
     * This contains some of the discards, and is intended for debugging.
     * @return
     * @draft
     */
    public List getNoDistinctSorting() {
        return noDistinctSorting;
    }

    /**
     * As the index is built, items may be discarded from the exemplars.
     * This contains some of the discards, and is intended for debugging.
     * @return
     * @draft
     */
    public List getNotAlphabetic() {
        return notAlphabetic;
    }
}

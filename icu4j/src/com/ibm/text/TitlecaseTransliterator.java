/*
 * Copyright (C) 1996-2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/TitlecaseTransliterator.java,v $ 
 * $Date: 2001/11/21 21:23:28 $ 
 * $Revision: 1.7 $
 */
package com.ibm.text;
import java.util.*;

/**
 * A transliterator that converts all letters (as defined by
 * <code>UCharacter.isLetter()</code>) to lower case, except for those
 * letters preceded by non-letters.  The latter are converted to title
 * case using <code>UCharacter.toTitleCase()</code>.
 * @author Alan Liu
 */
class TitlecaseTransliterator extends Transliterator {

    static final String _ID = "Any-Title";

    /**
     * The set of characters we skip.  These are neither cased nor
     * non-cased, to us; we copy them verbatim.
     */
    static final UnicodeSet SKIP = new UnicodeSet("[\u00AD \u2019 \\' [:Mn:] [:Me:] [:Cf:] [:Lm:]]");

    /**
     * The set of characters that cause the next non-SKIP character
     * to be lowercased.
     */
    static final UnicodeSet CASED = new UnicodeSet("[[:Lu:] [:Ll:] [:Lt:]]");

    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return new TitlecaseTransliterator();
            }
        });

        registerSpecialInverse("Title", "Lower", false);
    }

    /**
     * Constructs a transliterator.
     */
    public TitlecaseTransliterator() {
        this(null);
    }

    /**
     * Constructs a transliterator.
     */
    public TitlecaseTransliterator(UnicodeFilter f) {
        super(_ID, f);
        // Need to look back 2 characters in the case of "can't"
        setMaximumContextLength(2);
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean incremental) {

        // Our mode; we are either converting letter toTitle or
        // toLower.
        boolean doTitle = true;

        // Determine if there is a preceding context of CASED SKIP*,
        // in which case we want to start in toLower mode.  If the
        // prior context is anything else (including empty) then start
        // in toTitle mode.
        int start = offsets.start;
        while (start > offsets.contextStart) {
            char c = text.charAt(--start);
            if (SKIP.contains(c)) {
                continue;
            }
            doTitle = !CASED.contains(c);
            break;
        }

        // Convert things after a CASED character toLower; things
        // after a non-CASED, non-SKIP character toTitle.  SKIP
        // characters are copied directly and do not change the mode.
        for (start=offsets.start; start<offsets.limit; ++start) {
            char c = text.charAt(start);
            if (SKIP.contains(c)) {
                continue;
            }
            char d = (char) (doTitle ? UCharacter.toTitleCase(c)
                                     : UCharacter.toLowerCase(c));
            if (c != d) {
                text.replace(start, start+1, String.valueOf(d));
            }
            doTitle = !CASED.contains(c);
        }

        offsets.start = start;
    }
}

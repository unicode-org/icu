/*
 * Copyright (C) 1996-2004, International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.util.ULocale;

/**
 * A transliterator that converts all letters (as defined by
 * <code>UCharacter.isLetter()</code>) to lower case, except for those
 * letters preceded by non-letters.  The latter are converted to title
 * case using <code>UCharacter.toTitleCase()</code>.
 * @author Alan Liu
 */
class TitlecaseTransliterator extends Transliterator {

    static final String _ID = "Any-Title";
    private ULocale loc;

    /**
     * The set of characters we skip.  These are neither cased nor
     * non-cased, to us; we copy them verbatim.
     */
    static UnicodeSet SKIP = null;

    /**
     * The set of characters that cause the next non-SKIP character
     * to be lowercased.
     */
    static UnicodeSet CASED = null;

    /**
     * Initialize static variables.  We defer intilization because it
     * is slow (typically over 1000 ms).
     */
    private static final void initStatics() {
        SKIP = new UnicodeSet("[\u00AD \u2019 \\' [:Mn:] [:Me:] [:Cf:] [:Lm:] [:Sk:]]");
        CASED = new UnicodeSet("[[:Lu:] [:Ll:] [:Lt:]]");
    }

    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return new TitlecaseTransliterator(ULocale.US);
            }
        });

        registerSpecialInverse("Title", "Lower", false);
    }

   /**
     * Constructs a transliterator.
     */
    public TitlecaseTransliterator(ULocale loc) {
        super(_ID, null);
        this.loc = loc;
        // Need to look back 2 characters in the case of "can't"
        setMaximumContextLength(2);
    }
     
    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean incremental) {
        if (SKIP == null) {
            initStatics();
        }

        // Our mode; we are either converting letter toTitle or
        // toLower.
        boolean doTitle = true;

        // Determine if there is a preceding context of CASED SKIP*,
        // in which case we want to start in toLower mode.  If the
        // prior context is anything else (including empty) then start
        // in toTitle mode.
        int c;
        for (int start = offsets.start - 1; start >= offsets.contextStart; start -= UTF16.getCharCount(c)) {
            c = text.char32At(start);
            if (SKIP.contains(c)) {
                continue;
            }
            doTitle = !CASED.contains(c);
            break;
        }

        // Convert things after a CASED character toLower; things
        // after a non-CASED, non-SKIP character toTitle.  SKIP
        // characters are copied directly and do not change the mode.

        int textPos = offsets.start;
        if (textPos >= offsets.limit) return;

        // get string for context
        // TODO: add convenience method to do this, since we do it all over
        
        UCharacterIterator original = UCharacterIterator.getInstance(text);
        
        // Walk through original string
        // If there is a case change, modify corresponding position in replaceable
        
        int limit = offsets.limit;
        int cp;
        int oldLen;
        int newLen;
        
        while (textPos < limit) {
            original.setIndex(textPos);
            cp = original.currentCodePoint();
            oldLen = UTF16.getCharCount(cp);
            
            if (!SKIP.contains(cp)) {
                if (doTitle) {
                    newLen = m_charppty_.toUpperOrTitleCase(loc, cp, original, false, buffer);
                } else {
                    newLen = m_charppty_.toLowerCase(loc, cp, original, buffer);
                }
                doTitle = !CASED.contains(cp);
                if (newLen >= 0) {
                    text.replace(textPos, textPos + oldLen, buffer, 0, newLen);
                    if (newLen != oldLen) {
                        textPos += newLen;
                        offsets.limit += newLen - oldLen;
                        offsets.contextLimit += newLen - oldLen;
                        continue;
                    }
                }
            }
            textPos += oldLen;
        }
        offsets.start = offsets.limit;
    }
    
    private char buffer[] = new char[UCharacterProperty.MAX_CASE_MAP_SIZE];
    /**
     * Character property database
     */
    private static final UCharacterProperty m_charppty_ = 
                                            UCharacterProperty.getInstance();
}

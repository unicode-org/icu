/*
 * Copyright (C) 1996-2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/TitlecaseTransliterator.java,v $ 
 * $Date: 2001/11/25 23:12:22 $ 
 * $Revision: 1.8 $
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
    private Locale loc;

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
                return new TitlecaseTransliterator(Locale.US);
            }
        });

        registerSpecialInverse("Title", "Lower", false);
    }

   /**
     * Constructs a transliterator.
     */
    public TitlecaseTransliterator(Locale loc) {
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

        int textPos = offsets.start;
        if (textPos >= offsets.limit) return;

        // get string for context
        // TODO: add convenience method to do this, since we do it all over
        
        char[] strBuffer = new char[offsets.contextLimit - offsets.contextStart]; // get whole context
        text.getChars(offsets.contextStart, offsets.contextLimit, strBuffer, 0);
        String original = new String(strBuffer);
        
        // Walk through original string
        // If there is a case change, modify corresponding position in replaceable
        
        int i = textPos - offsets.contextStart;
        int limit = offsets.limit - offsets.contextStart;
        int cp;
        int oldLen;
        int newLen;
        
        for (; i < limit; i += oldLen) {
            cp = UTF16.charAt(original, i);
            oldLen = UTF16.getCharCount(cp);
            
            if (!SKIP.contains(cp)) {
                if (doTitle) {
                    newLen = UCharacter.toTitleCase(loc, original, i, buffer);
                } else {
                    newLen = UCharacter.toLowerCase(loc, original, i, buffer);
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
    
    private char buffer[] = new char[UCharacter.getMaxCaseExpansion()];
}

/*
 * Copyright (C) 1996-2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/TitlecaseTransliterator.java,v $ 
 * $Date: 2001/07/02 19:24:26 $ 
 * $Revision: 1.1 $
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
public class TitlecaseTransliterator extends Transliterator {

    static final String _ID = "Any-Title";

    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            public Transliterator getInstance() {
                return new TitlecaseTransliterator();
            }
        });
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

        // The way a filter is supposed to work isn't precisely
        // specified by Transliterator yet.  We interpret the filter
        // as masking characters completely -- they do not get
        // modified, and they are also _invisible for the purposes of
        // context_.  We are a little inconsistent about this -- we
        // don't filter characters in the range contextStart..start-1
        // (the left context).
        
        // NOTE: This method contains some special case code to handle
        // apostrophes between alpha characters.  We want to have
        // "can't" => "Can't" (not "Can'T").  This may be incorrect
        // for some locales, e.g., "l'arbre" => "L'Arbre" (?).
        // TODO: Revisit this.

        // Determine if there is a preceding letter character in the
        // left context (if there is any left context).
        boolean wasLastCharALetter = false;
        if (offsets.start > offsets.contextStart) {
            char c = text.charAt(offsets.start - 1);
            // Handle the case "Can'|t", where the | marks the context
            // boundary.  We only handle a single apostrophe.
            if (c == '\'' && (offsets.start-2) >= offsets.contextStart) {
                c = text.charAt(offsets.start - 2);
            }
            wasLastCharALetter = UCharacter.isLetter(c);            
        }

        // The buffer used to batch up changes to be made
        StringBuffer buffer = new StringBuffer();
        int bufStart = 0;
        int bufLimit = -1;

        int start;
        for (start = offsets.start; start < offsets.limit; ++start) {
            // For each character, if the preceding character was a
            // non-letter, and this character is a letter, then apply
            // the titlecase transformation.  Otherwise apply the
            // lowercase transformation.
            char c = filteredCharAt(text, start);
            if (UCharacter.isLetter(c)) {
                int newChar;
                if (wasLastCharALetter) {
                    newChar = UCharacter.toLowerCase(c);
                } else {
                    newChar = UCharacter.toTitleCase(c);
                }
                if (c != newChar) {
                    // This is the simple way of doing this:
                    //text.replace(start, start+1,
                    //             String.valueOf((char) newChar));

                    // Instead, we do something more complicated that
                    // minimizes the number of calls to
                    // Replaceable.replace().  We batch up the changes
                    // we want to make in a StringBuffer, recording
                    // our position and dumping the buffer out when a
                    // non-contiguous change arrives.
                    if (bufLimit == start) {
                        ++bufLimit;
                        // Fall through and append newChar below
                    } else {
                        if (buffer.length() > 0) {
                            text.replace(bufStart, bufLimit, buffer.toString());
                            buffer.setLength(0);
                        }
                        bufStart = start;
                        bufLimit = start+1;
                        // Fall through and append newChar below
                    }
                    buffer.append((char) newChar);
                }
                wasLastCharALetter = true;
            } else if (c == '\'' && wasLastCharALetter) {
                // Ignore a single embedded apostrophe, so that "can't" =>
                // "Can't", not "Can'T".
            } else {
                wasLastCharALetter = false;
            }
        }
        // assert(start == offsets.limit);
        offsets.start = start;

        if (buffer.length() > 0) {
            text.replace(bufStart, bufLimit, buffer.toString());
        }
    }
}

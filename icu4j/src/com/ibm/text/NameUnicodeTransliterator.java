/*
 * Copyright (C) 1996-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/NameUnicodeTransliterator.java,v $ 
 * $Date: 2001/07/02 20:55:29 $ 
 * $Revision: 1.1 $
 */
package com.ibm.text;
import java.util.*;

/**
 * A transliterator that performs name to character mapping.
 * @author Alan Liu
 */
public class NameUnicodeTransliterator extends Transliterator {

    char openDelimiter;
    char closeDelimiter;

    static final String _ID = "Name-Any";

    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            public Transliterator getInstance() {
                return new NameUnicodeTransliterator(null);
            }
        });
    }

    /**
     * Constructs a transliterator.
     */
    public NameUnicodeTransliterator(char openDelimiter, char closeDelimiter,
                                     UnicodeFilter filter) {
        super(_ID, filter);
        this.openDelimiter = openDelimiter;
        this.closeDelimiter = closeDelimiter;
    }

    /**
     * Constructs a transliterator with the default delimiters '{' and
     * '}'.
     */
    public NameUnicodeTransliterator(UnicodeFilter filter) {
        this('{', '}', filter);
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean isIncremental) {
        // Longest name as of 3.0.0 is 83
        final int LONGEST_NAME = 83;
        
        // Accomodate the longest possible name plus padding
        char[] buf = new char[LONGEST_NAME + 8]; 

        // The only characters used in names are (as of Unicode 3.0.0):
        //  -0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ
        // (first character is a space).

        int cursor = offsets.start;
        int limit = offsets.limit;

        // Modes:
        // 0 - looking for open delimiter
        // 1 - after open delimiter
        int mode = 0;
        int ibuf = 0;
        int openPos = offsets.start; // position of openDelimiter

        for (; cursor < limit; ++cursor) {
            char c = filteredCharAt(text, cursor);

            switch (mode) {
            case 0: // looking for open delimiter
                if (c == openDelimiter) {
                    openPos = cursor;
                    mode = 1;
                    ibuf = 0;
                }
                break;

            case 1: // after open delimiter
                // Look for [-a-zA-Z0-9].  If \w+ is found, convert it
                // to a single space.  If closeDelimiter is found, exit
                // the loop.  If any other character is found, exit the
                // loop.  If the limit is found, exit the loop.
                if (UCharacter.isWhitespace(c)) {
                    // Ignore leading whitespace
                    if (ibuf != 0 && buf[ibuf-1] != (char)0x0020) {
                        buf[ibuf++] = (char)0x0020 /* */;
                        // If we go a bit past the longest possible name then abort
                        if (ibuf == (LONGEST_NAME + 4)) {
                            mode = 0;
                        }
                    }
                    continue;
                }

                if (c == closeDelimiter) {
                    // Delete trailing space, if any
                    if (ibuf > 0 && buf[ibuf-1] == (char)0x0020) {
                        --ibuf;
                    }
                    int ch = UCharacter.getCharFromName(new String(buf, 0, ibuf));
                    if (ch != -1) {
                        // Lookup succeeded
                        text.replace(openPos, cursor+1, String.valueOf((char) ch));

                        // Adjust indices for the change in the length of
                        // the string.  Do not assume that str.length() ==
                        // 1, in case of surrogates.
                        int delta = cursor + 1 - openPos - 1/*str.length()*/;
                        cursor -= delta;
                        limit -= delta;
                        // assert(cursor == openPos + str.length());
                    }
                    // If the lookup failed, we leave things as-is and
                    // still switch to mode 0 and continue.
                    mode = 0;
                    continue;
                }

                if (c >= (char)0x0061 && c <= (char)0x007A) {
                    c -= 0x0020; // [a-z] => [A-Z]
                }

                // Check if c =~ [-A-Z0-9]
                if (c == (char)0x002D ||
                    (c >= (char)0x0041 && c <= (char)0x005A) ||
                    (c >= (char)0x0030 && c <= (char)0x0039)) {
                    buf[ibuf++] = (char) c;
                    // If we go a bit past the longest possible name then abort
                    if (ibuf == (LONGEST_NAME + 4)) {
                        mode = 0;
                    }
                }

                // Invalid character
                else {
                    --cursor; // Backup and reprocess this character
                    mode = 0;
                }

                break;
            }
        }

        offsets.contextLimit += limit - offsets.limit;
        offsets.limit = limit;
        // In incremental mode, only advance the cursor up to the last
        // open delimiter, if we are in mode 1.
        offsets.start = (mode == 1 && isIncremental) ? openPos : cursor;
    }
}

/*
 * Copyright (C) 1996-2010, International Business Machines Corporation and
 * others. All Rights Reserved.
 */
package com.ibm.icu.text;
import com.ibm.icu.lang.*;

/**
 * A transliterator that performs character to name mapping.
 * It generates the Perl syntax \N{name}.
 * @author Alan Liu
 */
class UnicodeNameTransliterator extends Transliterator {

    static final String _ID = "Any-Name";

    static final String OPEN_DELIM = "\\N{";
    static final char CLOSE_DELIM = '}';
    static final int OPEN_DELIM_LEN = 3;

    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return new UnicodeNameTransliterator(null);
            }
        });
    }

    /**
     * Constructs a transliterator.
     */
    public UnicodeNameTransliterator(UnicodeFilter filter) {
        super(_ID, filter);
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean isIncremental) {
        int cursor = offsets.start;
        int limit = offsets.limit;
        
        StringBuilder str = new StringBuilder();
        str.append(OPEN_DELIM);
        int len;
        String name;
        
        while (cursor < limit) {
            int c = text.char32At(cursor);
            if ((name=UCharacter.getExtendedName(c)) != null) {
                
                str.setLength(OPEN_DELIM_LEN);
                str.append(name).append(CLOSE_DELIM);

                int clen = UTF16.getCharCount(c);
                text.replace(cursor, cursor+clen, str.toString());
                len = str.length();
                cursor += len; // advance cursor by 1 and adjust for new text
                limit += len-clen; // change in length
            } else {
                ++cursor;
            }
        }

        offsets.contextLimit += limit - offsets.limit;
        offsets.limit = limit;
        offsets.start = cursor;
    }
}

/*
 * Copyright (C) 1996-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/UnicodeNameTransliterator.java,v $ 
 * $Date: 2001/07/02 20:55:29 $ 
 * $Revision: 1.1 $
 */
package com.ibm.text;
import java.util.*;

/**
 * A transliterator that performs character to name mapping.
 * @author Alan Liu
 */
public class UnicodeNameTransliterator extends Transliterator {

    char openDelimiter;
    char closeDelimiter;

    static final String _ID = "Any-Name";

    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            public Transliterator getInstance() {
                return new UnicodeNameTransliterator(null);
            }
        });
    }

    /**
     * Constructs a transliterator.
     */
    public UnicodeNameTransliterator(char openDelimiter, char closeDelimiter,
                                     UnicodeFilter filter) {
        super(_ID, filter);
        this.openDelimiter = openDelimiter;
        this.closeDelimiter = closeDelimiter;
    }

    /**
     * Constructs a transliterator with the default delimiters '{' and
     * '}'.
     */
    public UnicodeNameTransliterator(UnicodeFilter filter) {
        this('{', '}', filter);
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean isIncremental) {
        int cursor = offsets.start;
        int limit = offsets.limit;
        
        UnicodeFilter filt = getFilter();
        StringBuffer str = new StringBuffer();
        str.append(openDelimiter);
        int len;
        String name;
        
        while (cursor < limit) {
            char c = text.charAt(cursor);
            if ((filt == null || filt.contains(c)) &&
                (name=UCharacter.getName(c)) != null) {
                
                str.setLength(1);
                str.append(name).append(closeDelimiter);
                
                text.replace(cursor, cursor+1, str.toString());
                len = str.length();
                cursor += len; // advance cursor by 1 and adjust for new text
                limit += len-1; // change in length is (len - 1)
            } else {
                ++cursor;
            }
        }

        offsets.contextLimit += limit - offsets.limit;
        offsets.limit = limit;
        offsets.start = cursor;
    }
}

/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/RemoveTransliterator.java,v $ 
 * $Date: 2001/04/04 18:06:53 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.text;
import java.util.*;

/**
 * A transliterator that removes characters.  This is useful in conjunction
 * with a filter.
 */
public class RemoveTransliterator extends Transliterator {

    /**
     * Package accessible ID for this transliterator.
     */
    static String _ID = "Remove";

    /**
     * Constructs a transliterator.
     */
    public RemoveTransliterator() {
        super(_ID, null);
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean incremental) {
        // Find runs of unfiltered characters and replace them with the
        // empty string.  This loop has been optimized to what is probably
        // an unnecessary degree.
        String empty = "";
        int start = offsets.start;
        for (;;) {
            // Find first unfiltered character, if any
            while (start < offsets.limit &&
                   filteredCharAt(text, start) == '\uFFFE') {
                ++start;
            }
            if (start >= offsets.limit) {
                break;
            }
            
            // assert(start < offsets.limit &&
            //        filteredCharAt(text, start) != 0xFFFE);
            
            // Find last unfiltered character
            int limit = start+1; // sic: +1
            while (limit < offsets.limit &&
                   filteredCharAt(text, limit) != '\uFFFE') {
                ++limit;
            }
            
            // assert(start < limit);
            
            // Remove characters
            text.replace(start, limit, empty);
            limit -= start; // limit <= deleted length
            offsets.contextLimit -= limit;
            offsets.limit -= limit;
        }
        offsets.start = start;
    }
}

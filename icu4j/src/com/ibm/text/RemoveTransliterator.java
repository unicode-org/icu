/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/RemoveTransliterator.java,v $ 
 * $Date: 2001/11/17 20:45:35 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.text;
import java.util.*;

/**
 * A transliterator that removes characters.  This is useful in conjunction
 * with a filter.
 */
class RemoveTransliterator extends Transliterator {

    /**
     * Package accessible ID for this transliterator.
     */
    static String _ID = "Any-Remove";

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
                                       Position index, boolean incremental) {
        // Our caller (filteredTransliterate) has already narrowed us
        // to an unfiltered run.  Delete it.
        text.replace(index.start, index.limit, "");
        int len = index.limit - index.start;
        index.contextLimit -= len;
        index.limit -= len;
    }
}

/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;


/**
 * A transliterator that removes characters.  This is useful in conjunction
 * with a filter.
 */
class RemoveTransliterator extends Transliterator {

    /**
     * ID for this transliterator.
     */
    private static String _ID = "Any-Remove";

    /**
     * System registration hook.
     */
    static void register() {
        Transliterator.registerFactory(_ID, new Transliterator.Factory() {
            public Transliterator getInstance(String ID) {
                return new RemoveTransliterator();
            }
        });
        Transliterator.registerSpecialInverse("Remove", "Null", false);
    }

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

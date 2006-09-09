/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

/**
 * A transliterator that leaves text unchanged.
 * @internal
 */
class NullTransliterator extends Transliterator {
    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 2000. All rights reserved.";

    /**
     * Package accessible IDs for this transliterator.
     */
    static String SHORT_ID = "Null";
    static String _ID      = "Any-Null";

    /**
     * Constructs a transliterator.
     * @internal
     */
    public NullTransliterator() {
        super(_ID, null);
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @internal
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean incremental) {
        offsets.start = offsets.limit;
    }
}

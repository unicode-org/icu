// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

/**
 * A transliterator that leaves text unchanged.
 */
class NullTransliterator extends Transliterator {
    /**
     * Package accessible IDs for this transliterator.
     */
    static final String SHORT_ID = "Null";
    static final String _ID      = "Any-Null";

    /**
     * Constructs a transliterator.
     */
    public NullTransliterator() {
        super(_ID, null);
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    @Override
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean incremental) {
        offsets.start = offsets.limit;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.Transliterator#addSourceTargetSet(boolean, com.ibm.icu.text.UnicodeSet, com.ibm.icu.text.UnicodeSet)
     */
    @Override
    public void addSourceTargetSet(UnicodeSet inputFilter, UnicodeSet sourceSet, UnicodeSet targetSet) {
        // do nothing
    }
}

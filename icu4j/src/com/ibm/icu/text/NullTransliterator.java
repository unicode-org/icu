/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/NullTransliterator.java,v $ 
 * $Date: 2003/06/03 18:49:34 $ 
 * $Revision: 1.13 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;

/**
 * A transliterator that leaves text unchanged.
 * @deprecated ICU 2.4 This class to become private after 2003-12-01. Use the Transliterator factory methods.
 */
public class NullTransliterator extends Transliterator {
    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 2000. All rights reserved.";

    /**
     * Package accessible IDs for this transliterator.
     */
    static String SHORT_ID = "Null";
    static String _ID      = "Any-Null";

    /**
     * Constructs a transliterator.
     * @deprecated ICU 2.4 This class to become private after 2003-12-01. Use the Transliterator factory methods.
     */
    public NullTransliterator() {
        super(_ID, null);
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @deprecated ICU 2.4 This class to become private after 2003-12-01. Use the Transliterator factory methods.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean incremental) {
        offsets.start = offsets.limit;
    }
}

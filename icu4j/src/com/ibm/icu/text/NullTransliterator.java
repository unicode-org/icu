/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/NullTransliterator.java,v $ 
 * $Date: 2002/02/16 03:06:11 $ 
 * $Revision: 1.10 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;
import java.util.*;

/**
 * A transliterator that leaves text unchanged.
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
     */
    public NullTransliterator() {
        super(_ID, null);
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
                                       Position offsets, boolean incremental) {
        offsets.start = offsets.limit;
    }
}

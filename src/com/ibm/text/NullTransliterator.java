/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/NullTransliterator.java,v $ 
 * $Date: 2000/06/28 20:49:54 $ 
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
package com.ibm.text;
import java.util.*;

/**
 * A transliterator that leaves text unchanged.
 */
public class NullTransliterator extends Transliterator {
    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 2000. All rights reserved.";

    /**
     * Package accessible ID for this transliterator.
     */
    static String _ID = "Null";

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

/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/LowerUpperTransliterator.java,v $ 
 * $Date: 2001/05/23 19:43:26 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.text;
import java.util.*;

/**
 * A transliterator that performs locale-sensitive toUpper()
 * case mapping.
 */
public class LowerUpperTransliterator extends TransformTransliterator {

    /**
     * Package accessible ID.
     */
    static final String _ID = "Lower-Upper";

    private Locale loc;

    /**
     * Constructs a transliterator.
     */
    public LowerUpperTransliterator(Locale loc, UnicodeFilter f) {
        super(_ID, f);
        this.loc = loc;
    }

    /**
     * Constructs a transliterator in the default locale.
     */
    public LowerUpperTransliterator() {
        this(Locale.getDefault(), null);
    }

    protected boolean hasTransform(int c) {
        return c != UCharacter.toUpperCase(c);
    }

    protected String transform(String s) {
        return UCharacter.toUpperCase(loc, s);
    }
}

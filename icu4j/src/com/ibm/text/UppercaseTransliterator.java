/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/UppercaseTransliterator.java,v $ 
 * $Date: 2001/06/29 22:35:41 $ 
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
public class UppercaseTransliterator extends TransformTransliterator {

    /**
     * Package accessible ID.
     */
    static final String _ID = "Any-Upper";

    private Locale loc;

    /**
     * Constructs a transliterator.
     */
    public UppercaseTransliterator(Locale loc, UnicodeFilter f) {
        super(_ID, f);
        this.loc = loc;
    }

    /**
     * Constructs a transliterator in the default locale.
     */
    public UppercaseTransliterator() {
        this(Locale.getDefault(), null);
    }

    protected boolean hasTransform(int c) {
        return c != UCharacter.toUpperCase(c);
    }

    protected String transform(String s) {
        return UCharacter.toUpperCase(loc, s);
    }
}

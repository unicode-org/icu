/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/UpperLowerTransliterator.java,v $ 
 * $Date: 2001/05/23 19:43:26 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.text;
import java.util.*;

/**
 * A transliterator that performs locale-sensitive toLower()
 * case mapping.
 */
public class UpperLowerTransliterator extends TransformTransliterator {

    /**
     * Package accessible ID.
     */
    static final String _ID = "Upper-Lower";

    private Locale loc;

    /**
     * Constructs a transliterator.
     */
    public UpperLowerTransliterator(Locale loc, UnicodeFilter f) {
        super(_ID, f);
        this.loc = loc;
    }

    /**
     * Constructs a transliterator in the default locale.
     */
    public UpperLowerTransliterator() {
        this(Locale.getDefault(), null);
    }

    protected boolean hasTransform(int c) {
        return c != UCharacter.toLowerCase(c);
    }

    protected String transform(String s) {
        return UCharacter.toLowerCase(loc, s);
    }
}

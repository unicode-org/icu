/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/SymbolTable.java,v $ 
 * $Date: 2000/03/10 04:07:24 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

/**
 * An interface that maps strings to objects.
 */
public interface SymbolTable {

    /**
     * Lookup the object associated with this string and return it.
     * Return <tt>null</tt> if no such name exists.
     */
    Object lookup(String s);
}

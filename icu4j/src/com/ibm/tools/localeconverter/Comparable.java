/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/tools/localeconverter/Attic/Comparable.java,v $ 
 * $Date: 2002/01/31 01:21:24 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.tools.localeconverter;

public interface Comparable {
    /**
        returns 0 if objects are equal, -1 if a is less than b, 1 otherwise.
    */
    public int compareTo(Object b);
}
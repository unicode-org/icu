/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/localeconverter/Comparable.java,v $ 
 * $Date: 2002/02/16 03:05:26 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.tool.localeconverter;

public interface Comparable {
    /**
        returns 0 if objects are equal, -1 if a is less than b, 1 otherwise.
    */
    public int compareTo(Object b);
}
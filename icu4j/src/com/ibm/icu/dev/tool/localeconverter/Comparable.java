/*
 *******************************************************************************
 * Copyright (C) 2002-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/localeconverter/Comparable.java,v $ 
 * $Date: 2003/12/20 03:06:58 $ 
 * $Revision: 1.3 $
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
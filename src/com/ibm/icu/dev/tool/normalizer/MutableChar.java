/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/normalizer/Attic/MutableChar.java,v $ 
 * $Date: 2002/02/16 03:05:33 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
// MutableChar.java

package com.ibm.icu.dev.tool.normalizer;

import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
import java.lang.Comparable;
import java.io.Serializable;

class MutableChar implements Cloneable, Comparable, Serializable {

    public char value;

    public MutableChar(char newValue) {
        value = newValue;
    }
    public MutableChar set(char newValue) {
        value = newValue;
        return this;
    }
    public boolean equals(Object other) {
        return value == ((MutableChar)other).value;
    }
    public int hashCode() {
        return value;
    }
    public String toString() {
        return String.valueOf(value);
    }
    public int compareTo(Object b) {
        char ch = ((MutableChar)b).value;
        return value == ch ? 0 : value < ch ? -1 : 1;
    }
}


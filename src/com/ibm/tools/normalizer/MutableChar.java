/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/tools/normalizer/Attic/MutableChar.java,v $ 
 * $Date: 2000/03/10 04:17:56 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */
// MutableChar.java

package com.ibm.tools.normalizer;

import com.ibm.text.*;
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


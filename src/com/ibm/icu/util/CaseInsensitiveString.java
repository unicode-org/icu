/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/CaseInsensitiveString.java,v $
 * $Date: 2002/08/13 23:43:27 $
 * $Revision: 1.3 $
 *
 *******************************************************************************
 */
package com.ibm.icu.util;

import com.ibm.icu.lang.UCharacter;

/**
 * A string used as a key in java.util.Hashtable and other
 * collections.  It retains case information, but its equals() and
 * hashCode() methods ignore case.
 */
public class CaseInsensitiveString {
    
    private String string;

    private int hash = 0;

    public CaseInsensitiveString(String s) {
        string = s;
    }

    public String getString() {
        return string;
    }

    public boolean equals(Object o) {
        try {
            return string.equalsIgnoreCase(((CaseInsensitiveString)o).string);
        } catch (ClassCastException e) {
            try {
                return string.equalsIgnoreCase((String)o);
            } catch (ClassCastException e2) {
                return false;
            }
        }
    }

    public int hashCode() {
        if (hash == 0) {
            hash = UCharacter.foldCase(string, true).hashCode();
        }
        return hash;
    }
}

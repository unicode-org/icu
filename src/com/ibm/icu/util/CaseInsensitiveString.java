/**
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import com.ibm.icu.lang.UCharacter;

/**
 * A string used as a key in java.util.Hashtable and other
 * collections.  It retains case information, but its equals() and
 * hashCode() methods ignore case.
 * @stable ICU 2.0
 */
public class CaseInsensitiveString {
    
    private String string;

    private int hash = 0;
    /**
     * Constructs an CaseInsentiveString object from the given string
     * @param s The string to construct this object from 
     * @stable ICU 2.0
     */
    public CaseInsensitiveString(String s) {
        string = s;
    }
    /**
     * returns the underlying string 
     * @return String
     * @stable ICU 2.0
     */
    public String getString() {
        return string;
    }
    /**
     * Compare the object with this 
     * @param o Object to compare this object with 
     * @stable ICU 2.0
     */
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
    
    /**
     * Returns the hashCode of this object
     * @return int hashcode
     * @stable ICU 2.0
     */
    public int hashCode() {
        if (hash == 0) {
            hash = UCharacter.foldCase(string, true).hashCode();
        }
        return hash;
    }
}

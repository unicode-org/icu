package com.ibm.util;
import com.ibm.text.UCharacter;

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

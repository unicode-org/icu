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


package com.ibm.text.UCD;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.text.utility.*;
import java.util.*;

// Enumerated properties will be IntCodePointProperty.
// The string values they return will be the property value names.
// Binary properties are Enumerated properties. They return 0 or 1

abstract public class CodePointProperty {
    // styles for names and string values
    static final byte SHORT = 0, DEFAULT = 1, LONG = 2, NORMAL_LIMIT = 3;
    
    // gets the property name
    abstract public String getName(byte style);
    
    // value may also be numeric, etc, but this returns string equivalent.
    abstract public String getValue(int codePoint, byte style);
    
    // returns true if the code point has the value
    // works with any style that getValue takes
    abstract public boolean hasValue(int codePoint, String value);
    
    // returns the set of all code points with that value.
    // same effect as using hasValue one by one, but faster internal implementation
    abstract public UnicodeSet getSet(String value);
    
    // returns a list of all possible values
    // logically the same as looping from 0..10FFFF with getValue and getStyleLimit,
    // and throwing out duplicates, but much faster.
    static Iterator getAllValues(byte style) {
        return null;
    }
    
    // gets top value style available for this property
    public byte getStyleLimit(byte style) {
        return NORMAL_LIMIT;
    }
    
    // returns true if the value is known to be uniform over a type.
    // this is used for various optimizations, especially for Cn & Co
    public boolean isUniformOverCategory(byte generalCategory) {
        return false;
    }
    
    // subclasses
    
    static abstract public class IntCodePointProperty extends CodePointProperty {
        abstract int getNumericValue(int codePoint);
        abstract int getMaxValue();
        abstract int getMinValue();
        static Iterator getAllNumericValues() {
            return null;
        }
    }
        
    static abstract public class DoubleCodePointProperty extends CodePointProperty {
        abstract double getNumericValue(int codePoint);
        abstract double getMaxValue();
        abstract double getMinValue();
        static Iterator getAllNumericValues() {
            return null;
        }
    }
    
    // registration and lookup
    
    // register a new property
    static void register(CodePointProperty newProp) {
        //...
    }
    
    // finds a registered property by name
    static CodePointProperty getInstance(String name) {
        return null;
    }
    
    // returns a list of all registered properties
    static Iterator getAllRegistered() {
        return null;
    }
    
    // UnicodeSet would use these internally to handle properties. That is, when
    // it encountered ... [:name=value:] ...
    // it would do:
    //        CodePointProperty x = getInstance(name);
    //        if (x != null) doError(name, value);
    //        UnicodeSet s = x.getSet(value);
    // and then use s.
    
    // open issue: we could have a property like: contains("dot")
    // in that case, we would register "contains" as the 'base' name,
    // but allow lookup with string parameters ("dot")
    // Maybe just adding:
    
    public boolean hasParameters() {
        return false;
    }
    public void setParameters(String parameters) {}
    public String getParameters() {
        return null;
    }
    
    // that way we could have [[:letter:]&[:contains(dot):]]
    
}
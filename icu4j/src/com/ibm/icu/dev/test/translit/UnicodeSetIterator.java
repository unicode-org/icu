package com.ibm.test.translit;
import com.ibm.text.*;
import com.ibm.test.*;
import com.ibm.util.Utility;
//import java.text.*;
import java.util.*;
import java.io.*;

public final class UnicodeSetIterator {
    public UnicodeSet set;
    int endRange = 0;
    int range = 0;
    int endElement;
    int element;
        
    public UnicodeSetIterator(UnicodeSet set) {
        reset(set);
    }
        
    public UnicodeSetIterator() {
        reset(new UnicodeSet());
    }
        
    /* returns -1 when done */
        
    public int next() {
        if (element < endElement) {
            return ++element;
        }
        if (range >= endRange) return -1;
        ++range;
        endElement = set.getRangeEnd(range);
        element = set.getRangeStart(range);
        return element;
    }
        
    public void reset(UnicodeSet set) {
        this.set = set;
        endRange = set.getRangeCount() - 1;
        reset();
    }
        
    public void reset() {
        range = 0;
        endElement = 0;
        element = 0;            
        if (endRange >= 0) {
            element = set.getRangeStart(range);
            endElement = set.getRangeEnd(range);
        }
    }
        
    // tests whether a string is in a set.
    // should be in UnicodeSet
    public static boolean containsSome(UnicodeSet set, String s) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(i)) {
            cp = UTF16.charAt(s, i);
            if (set.contains(cp)) return true;
        }
        return false;
    }
        
    // tests whether a string is in a set.
    // should be in UnicodeSet
    public static boolean containsAll(UnicodeSet set, String s) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(i)) {
            cp = UTF16.charAt(s, i);
            if (!set.contains(cp)) return false;
        }
        return true;
    }
}

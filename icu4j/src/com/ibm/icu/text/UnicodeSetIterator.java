/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/UnicodeSetIterator.java,v $ 
 * $Date: 2002/02/01 02:05:35 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

import com.ibm.util.Utility;
//import java.text.*;
import java.util.*;
import java.io.*;

/**
 * Class that allows simple iteration over a UnicodeSet.
 * @author M. Davis
 * @draft
 */
public final class UnicodeSetIterator {

    /**
     *@set set to iterate over
     */
    public UnicodeSetIterator(UnicodeSet set) {
        reset(set);
    }
        
    /**
     *@return next character in the set. Returns -1 when done!
     */
    public int next() {
        if (abbreviated) {
            if (element >= startElement + 50 && element <= endElement - 50) {
                element = endElement - 50;
            }
        }
        if (element < endElement) {
            return ++element;
        }
        if (range >= endRange) return -1;
        ++range;
        endElement = set.getRangeEnd(range);
        startElement = set.getRangeStart(range);
        element = set.getRangeStart(range);
        return element;
    }
        
    /**
     *@param set the set to iterate over. This allows reuse of the iterator.
     */
    public void reset(UnicodeSet set) {
        this.set = set;
        endRange = set.getRangeCount() - 1;
        resetInternal();
    }
        
    /**
     * Resets to the start, to allow the iteration to start over again.
     */
    public void reset() {
        endRange = set.getRangeCount() - 1;
        resetInternal();
    }
    
    /**
     * TODO: Move to UnicodeSet!
     *@param s the string to test
     *@return true if and only if no character from s are in the set.
     */
    public static boolean containsNone(UnicodeSet set, String s) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(i)) {
            cp = UTF16.charAt(s, i);
            if (set.contains(cp)) return false;
        }
        return true;
    }
        
    /**
     * TODO: Move to UnicodeSet!
     *@param s the string to test
     *@return true if and only if all characters from s are in the set.
     */
    public static boolean containsAll(UnicodeSet set, String s) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(i)) {
            cp = UTF16.charAt(s, i);
            if (!set.contains(cp)) return false;
        }
        return true;
    }
    
    // ======================= PRIVATES ===========================
    
    private UnicodeSet set;
    private int endRange = 0;
    private int range = 0;
    private int startElement = 0;
    private int endElement;
    private int element;
    private boolean abbreviated = false;
        
    private void resetInternal() {
        range = 0;
        endElement = 0;
        element = 0;            
        if (endRange >= 0) {
            element = set.getRangeStart(range);
            endElement = set.getRangeEnd(range);
            startElement = set.getRangeStart(range);
        }
    }
}

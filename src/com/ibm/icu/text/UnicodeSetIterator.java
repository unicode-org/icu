/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/UnicodeSetIterator.java,v $ 
 * $Date: 2002/03/20 05:11:17 $ 
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.Utility;
//import java.text.*;
import java.util.*;
import java.io.*;

/**
 * Class that allows simple iteration over a UnicodeSet.
 * @author M. Davis
 * @draft
 * @internal -- this API is not for general use, and may change at any time.
 */
public final class UnicodeSetIterator {
	
	public static int IS_STRING = -1;
	
	// for results of iteration
	
	public int codepoint;
	public int codepointEnd;
	public String string;

    /**
     * Create an iterator
     * @param set set to iterate over
     */
    public UnicodeSetIterator(UnicodeSet set) {
        reset(set);
    }
        
    /**
     * Create an iterator. Convenience for when the contents are to be set later.
     */
    public UnicodeSetIterator() {
        reset(new UnicodeSet());
    }
        
    /**
     * Returns the next element in the set.
     * @return true if there was another element in the set.
     * if so, if codepoint == IS_STRING, the value is a string in the string field
     * else the value is a single code point in the codepoint field.
     * <br>You are guaranteed that the codepoints are in sorted order, and the strings are in sorted order,
     * and that all code points are returned before any strings are returned.
     * <br>Note also that the codepointEnd is undefined after calling this method.
     */
    public boolean next() {
        if (nextElement <= endElement) {
        	codepoint = codepointEnd = nextElement++;
            return true;
        }
        if (range < endRange) {
        	++range;
        	nextElement = startElement = set.getRangeStart(range);
        	endElement = set.getRangeEnd(range);
        	if (abbreviated && (endElement > startElement + 50)) {
            	endElement = startElement + 50;
        	}
        	codepoint = codepointEnd = nextElement++;
        	return true;
        }
        
        // stringIterator == null iff there are no string elements remaining
        
        if (stringIterator == null) return false;
        codepoint = IS_STRING; // signal that value is actually a string
        string = (String)stringIterator.next();
        if (!stringIterator.hasNext()) stringIterator = null;
        return true;
    }
        
    /**
     * @return true if there was another element in the set.
     * if so, if codepoint == IS_STRING, the value is a string in the string field
     * else the value is a range of codepoints in the <codepoint, codepointEnd> fields.
     * <br>Note that the codepoints are in sorted order, and the strings are in sorted order,
     * and that all code points are returned before any strings are returned.
     * <br>You are guaranteed that the ranges are in sorted order, and the strings are in sorted order,
     * and that all ranges are returned before any strings are returned.
     * <br>You are also guaranteed that ranges are disjoint and non-contiguous.
     * <br>Note also that the codepointEnd is undefined after calling this method.
     */
    public boolean nextRange() {
        if (nextElement <= endElement) {
        	codepointEnd = endElement;
        	codepoint = nextElement;
        	nextElement = endElement+1;
            return true;
        }
        if (range < endRange) {
        	++range;
        	nextElement = startElement = set.getRangeStart(range);
        	endElement = set.getRangeEnd(range);
        	if (abbreviated && (endElement > startElement + 50)) {
            	endElement = startElement + 50;
        	}
        	codepointEnd = endElement;
        	codepoint = nextElement;
        	nextElement = endElement+1;
        	return true;
        }
        
        // stringIterator == null iff there are no string elements remaining
        
        if (stringIterator == null) return false;
        codepoint = IS_STRING; // signal that value is actually a string
        string = (String)stringIterator.next();
        if (!stringIterator.hasNext()) stringIterator = null;
        return true;
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
     * Causes the interation to only to part of long ranges
     * @internal -- used only for testing
     */
    public void setAbbreviated(boolean abbr) {
        abbreviated = abbr;
    }
    
    /**
     * Causes the interation to only to part of long ranges
     * @internal -- used only for testing
     */
    public boolean getAbbreviated() {
        return abbreviated;
    }
    
    // ======================= PRIVATES ===========================
    
    private UnicodeSet set;
    private int endRange = 0;
    private int range = 0;
    private int startElement = 0;
    private int endElement;
    private int nextElement;
    private boolean abbreviated = false;
    private Iterator stringIterator = null;
    
    /**
     * Invariant: stringIterator is null when there are no (more) strings remaining
     */
        
    private void resetInternal() {
        range = 0;
        endElement = -1;
        nextElement = 0;            
        if (endRange >= 0) {
            nextElement = startElement = set.getRangeStart(range);
            endElement = set.getRangeEnd(range);
            if (abbreviated && (endElement > startElement + 50)) {
            	endElement = startElement + 50;
        	}
        }
        stringIterator = null;
        if (set.strings != null) {
        	stringIterator = set.strings.iterator();
        	if (!stringIterator.hasNext()) stringIterator = null;
        }
    }
}

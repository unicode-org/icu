/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/UnicodeSetIterator.java,v $ 
 * $Date: 2002/03/13 19:52:34 $ 
 * $Revision: 1.4 $
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
 */
public final class UnicodeSetIterator {
	
	public static int IS_STRING = -1;
	
	// for results of iteration
	
	public int codepoint;
	public String string;

    /**
     *@set set to iterate over
     */
    public UnicodeSetIterator(UnicodeSet set) {
        reset(set);
    }
        
    /**
     *convenience
     */
    public UnicodeSetIterator() {
        reset(new UnicodeSet());
    }
        
    /**
     *@return true if there was another element in the set.
     *if so, if codepoint == IS_STRING, the value is a string in the string field
     *else the value is a single code point in the codepoint field.
     */
    public boolean next() {
        if (nextElement <= endElement) {
        	codepoint = nextElement++;
            return true;
        }
        if (range < endRange) {
        	++range;
        	nextElement = startElement = set.getRangeStart(range);
        	endElement = set.getRangeEnd(range);
        	if (abbreviated && (endElement > startElement + 50)) {
            	endElement = startElement + 50;
        	}
        	codepoint = nextElement++;
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
     * @internal
     */
    public void setAbbreviated(boolean abbr) {
        abbreviated = abbr;
    }
    
    /**
     * Causes the interation to only to part of long ranges
     * @internal
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

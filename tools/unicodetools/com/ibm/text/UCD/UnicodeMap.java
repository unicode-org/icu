/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/Attic/UnicodeMap.java,v $
* $Date: 2003/04/01 02:53:07 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;

import com.ibm.text.utility.*;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

/**
 * Class that maps from codepoints to an index, and optionally a label.
 */
public class UnicodeMap {
    UnicodeSet[] sets = new UnicodeSet[50];
    String[] labels = new String[50];
    int count = 0;
    
    public int add(String label, UnicodeSet set) {
        return add(label, set, false, true);
    }
    
    /**
     * Add set
     *@param removeOld true: remove any collisions from sets already in the map
     * if false, remove any collisions from this set
     *@param signal: print a warning when collisions occur
     */
    public int add(String label, UnicodeSet set, boolean removeOld, boolean signal) {
        // remove from any preceding!!
        for (int i = 0; i < count; ++i) {
            if (!set.containsSome(sets[i])) continue;
            if (signal) showOverlap(label, set, i);
            if (removeOld) {
                sets[i] = sets[i].removeAll(set);
            } else {
                set = set.removeAll(sets[i]);
            }
        }
        sets[count] = set;
        labels[count++] = label;
        return (short)(count - 1);
    }
    
    public void showOverlap(String label, UnicodeSet set, int i) {
        UnicodeSet delta = new UnicodeSet(set).retainAll(sets[i]);
        System.out.println("Warning! Overlap with " + label + " and " + labels[i]
            + ": " + delta);
    }
    
    public int getIndex(int codepoint) {
        for (int i = count - 1; i >= 0; --i) {
            if (sets[i].contains(codepoint)) return i;
        }
        return -1;
    }
    
    public int getIndexFromLabel(String label) {
        for (int i = count - 1; i >= 0; --i) {
            if (labels[i].equalsIgnoreCase(label)) return i;
        }
        return -1;
    }

    public String getLabel(int codepoint) {
        return getLabelFromIndex(getIndex(codepoint));
    }

    public String getLabelFromIndex(int index) {
        if (index < 0 || index >= count) return null;
        return labels[index];
    }

    public UnicodeSet getSetFromIndex(int index) {
        if (index < 0 || index >= count) return null;
        return new UnicodeSet(sets[index]); // protect from changes
    }
    
    public int size() {
        return count;
    }
}

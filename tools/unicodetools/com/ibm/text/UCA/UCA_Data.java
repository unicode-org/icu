/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCA/UCA_Data.java,v $ 
* $Date: 2002/07/14 22:07:00 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCA;

import java.util.*;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.PrintWriter;
import java.io.FileReader;
import java.text.MessageFormat;
import java.io.IOException;
import com.ibm.text.UCD.Normalizer;
import com.ibm.text.UCD.UCD;
import com.ibm.text.utility.*;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

public class UCA_Data implements UCA_Types {
    static final boolean DEBUG = true;
    static final boolean DEBUG_SHOW_ADD = false;
    
    private Normalizer toD;
    private UCD ucd;
    
    public UCA_Data(Normalizer toD, UCD ucd) {
        this.toD = toD;
        this.ucd = ucd;
    }
    
    /**
     * The collation element data is stored a couple of different structures.
     * First is collationElements, which generally contains the 32-bit CE corresponding
     * to the data. It is directly indexed by character code.<br>
     * For brevity in the implementation, we just use a flat array.
     * A real implementation would use a multi-stage table, as described in TUS Section 5.
     * table of simple collation elements, indexed by char.<br>
     * Exceptional cases: expanding, contracting, unsupported are handled as described below.
     */
    private int[] collationElements = new int[65536];
    
    /**
     * Although a single character can expand into multiple CEs, we don't want to burden
     * the normal case with the storage. So, they get a special value in the collationElements
     * array. This value has a distinct primary weight, followed by an index into a separate
     * table called expandingTable. All of the CEs in that table, up to a TERMINATOR value
     * will be used for the expansion. The implementation is as a stack; this just makes it
     * easy to generate.
     */
    private IntStack expandingTable = new IntStack(3600); // initial number is from compKeys
        
    /**
     * For now, this is just a simple mapping of strings to collation elements.
     * The implementation depends on the contracting characters being "completed",
     * so that it can be efficiently determined when to stop looking.
     */
    private Map contractingTable = new TreeMap();
    
    {
        // clear some tables
        for (int i = 0; i < collationElements.length; ++i) {
            collationElements[i] = UNSUPPORTED_FLAG;
        }
        // preload with parts
        for (char i = 0xD800; i < 0xDC00; ++i) {
            collationElements[i] = CONTRACTING;
            addToContractingTable(String.valueOf(i), UNSUPPORTED_FLAG);
        }
        checkConsistency();
    }
    
    /**
     * Return the type of the CE
     */
    public byte getCEType(int ch) {
        if (ch > 0xFFFF) ch = UTF16.getLeadSurrogate(ch); // first if expands
        
        int ce = collationElements[ch];
        if (ce == UNSUPPORTED_FLAG) {
            
            // Special check for Han, Hangul
            if (ucd.isHangulSyllable(ch)) return HANGUL_CE;
            
            if (ucd.isCJK_BASE(ch)) return CJK_CE;
            if (ucd.isCJK_AB(ch)) return CJK_AB_CE;
                        
            // special check for unsupported surrogate pair, 20 1/8 bits
            //if (0xD800 <= ch && ch <= 0xDFFF) {
            //    return SURROGATE_CE;
            //}
            return UNSUPPORTED_CE;
        }
        if (ce == CONTRACTING) return CONTRACTING_CE;
        if ((ce & EXPANDING_MASK) == EXPANDING_MASK) return EXPANDING_CE;
        return NORMAL_CE;
    }
    
    public void add(String source, IntStack ces) {
        add(new StringBuffer(source), ces);
    }
        
    public void add(StringBuffer source, IntStack ces) {
        
        if (DEBUG_SHOW_ADD) {
            System.out.println("Adding: " + ucd.getCodeAndName(source.toString()) + CEList.toString(ces));
        }
        if (source.length() < 1 || ces.length() < 1) {
            throw new IllegalArgumentException("String or CEs too short");
        }
        
        int ce;
        if (ces.length() == 1) {
            ce = ces.get(0);
        } else {
            ce = EXPANDING_MASK | expandingTable.getTop();
            expandingTable.append(ces);
            expandingTable.append(TERMINATOR);
        }
        
        // assign CE(s) to char(s)
        char value = source.charAt(0);
        //if (value == 0x10000) System.out.print("DEBUG2: " + source);
            	        
        if (source.length() > 1) {
            addToContractingTable(source, ce);
            if (collationElements[value] == UNSUPPORTED_FLAG) {
                collationElements[value] = CONTRACTING; // mark special
            } else if (collationElements[value] != CONTRACTING) {
                // move old value to contracting table!
                //contractingTable.put(String.valueOf(value), new Integer(collationElements[value]));
                addToContractingTable(String.valueOf(value), collationElements[value]);
                collationElements[value] = CONTRACTING; // signal we must look up in table
            }
        } else if (collationElements[value] == CONTRACTING) {
            // must add old value to contracting table!
            addToContractingTable(source, ce);
            //contractingTable.put(source, new Integer(ce));
        } else {
            collationElements[source.charAt(0)] = ce; // normal
        }
        //if (DEBUG) checkConsistency();
    }
    
    boolean isCompletelyIgnoreable(int cp) {
        int ce = collationElements[cp < UTF16.SUPPLEMENTARY_MIN_VALUE ? cp : UTF16.getLeadSurrogate(cp)];
        if (ce == 0) return true;
        if (ce != CONTRACTING) return false;
        Object newValue = contractingTable.get(UTF16.valueOf(cp));       
        if (newValue == null) return false;
        return ((Integer)newValue).intValue() == 0;
    }
    
    // returns new pos, fills in result.
    public int get(char ch, StringBuffer decompositionBuffer, int index, IntStack result) {
        int ce = collationElements[ch];
        
        if (ce == CONTRACTING) {
            // Contracting is probably the most interesting (read "tricky") part
            // of the algorithm.
            // First get longest substring that is in the contracting table.
            // For simplicity, we use a hash table for contracting.
            // There are much better optimizations, 
            // but they take a more complicated build algorithm than we want to show here.
            // NOTE: We are guaranteed that the first code unit is in the contracting table because
            // of the build process.
            String probe = String.valueOf(ch);
            Object value = contractingTable.get(probe);
            if (value == null) throw new IllegalArgumentException("Missing value for " + Utility.hex(ch));
            
            // complete the first character, if part of supplementary
            if (UTF16.isLeadSurrogate(ch) && index < decompositionBuffer.length()) {
                char ch2 = decompositionBuffer.charAt(index);
                String newProbe = probe + ch2;
                Object newValue = contractingTable.get(newProbe);
                if (newValue != null) {
                    probe = newProbe;
                    value = newValue;
                    index++;
                }
            }           
            
            // We loop, trying to add successive CODE UNITS to the longest substring.
            int cp2;
            while (index < decompositionBuffer.length()) {
                //char ch2 = decompositionBuffer.charAt(index);
                cp2 = UTF16.charAt(decompositionBuffer, index);
                int increment = UTF16.getCharCount(cp2);
                
                // CHECK if last char was completely ignorable
                if (isCompletelyIgnoreable(cp2)) {
                    index += increment; // just skip char don't set probe, value
                    continue;
                }
                
                // see whether the current string plus the next char are in
                // the contracting table.
                String newProbe = probe + UTF16.valueOf(cp2);
                Object newValue = contractingTable.get(newProbe);
                if (newValue == null) break;    // stop if not in table.
                
                // We succeeded--so update our new values, and set index
                // and quaternary to indicate that we swallowed another character.
                probe = newProbe;
                value = newValue;
                index += increment;
            }
            
            // Now, see if we can add any combining marks
            short lastCan = 0;
            int increment;
            for (int i = index; i < decompositionBuffer.length(); i += increment) {
                // We only take certain characters. They have to be accents,
                // and they have to not be blocked.
                // Unlike above, if we don't find a match (and it was an accent!)
                // then we don't stop, we continue looping.
                cp2 = UTF16.charAt(decompositionBuffer, i);
                increment = UTF16.getCharCount(cp2);
                short can = toD.getCanonicalClass(cp2);
                if (can == 0) break;            // stop with any zero (non-accent)
                if (can == lastCan) continue;   // blocked if same class as last
                lastCan = can;                  // remember for next time
                
                // CHECK if last char was completely ignorable. If so, skip it.
                if (isCompletelyIgnoreable(cp2)) {
                    continue;
                }
                
                // Now see if we can successfully add it onto our string
                // and find it in the contracting table.
                String newProbe = probe + UTF16.valueOf(cp2);
                Object newValue = contractingTable.get(newProbe);
                if (newValue == null) continue;

                // We succeeded--so update our new values, remove the char, and update
                // quaternary to indicate that we swallowed another character.
                probe = newProbe;
                value = newValue;
                decompositionBuffer.setCharAt(i,'\u0000');  // zero char
                if (increment == 2) {
                    // WARNING: we had a supplementary character. zero BOTH parts
                    decompositionBuffer.setCharAt(i+1,'\u0000');  // zero char
                }
            }
            
            // we are all done, and can extract the CE from the last value set.
            ce = ((Integer)value).intValue();
            
        }
        
        // if the CE is not expanding) we are done.
        if ((ce & EXPANDING_MASK) != EXPANDING_MASK) {
            result.push(ce);
        } else {
            // expanding, so copy list of items onto stack
            int ii = ce & EXCEPTION_INDEX_MASK; // get index
            // copy onto stack from index until reach TERMINATOR
            while (true) {
                ce = expandingTable.get(ii++);
                if (ce == TERMINATOR) break;
                result.push(ce);
            }
        }
        return index;
    }
    
    private void addToContractingTable(Object s, int ce) {
        if (s == null) {
            throw new IllegalArgumentException("String can't be null");
        }
        contractingTable.put(s.toString(), new Integer(ce));
    }
        
    void checkConsistency() {
                // at this point, we have to guarantee that the contractingTable is CLOSED
        // e.g. if a substring of length n is in the table, then the first n-1 characters
        // are also!!
        
        // First check consistency. the CE for a value is CONTRACTING if and only if there is a contraction starting
        // with that value.
        
        UnicodeSet ceSet = new UnicodeSet();
        for (int i = 0; i < collationElements.length; ++i) {
            if (collationElements[i] == CONTRACTING) ceSet.add(i);
        }
        UnicodeSet ceSet2 = new UnicodeSet();
        Iterator enum = contractingTable.keySet().iterator();
        while (enum.hasNext()) {
            String sequence = (String)enum.next();
            ceSet2.add(sequence.charAt(0));
        }
        
        if (!ceSet.equals(ceSet2)) {
            System.out.println("In both: " + new UnicodeSet(ceSet).retainAll(ceSet2).toPattern(true));
            System.out.println("CONTRACTING but not in table: " + new UnicodeSet(ceSet).removeAll(ceSet2).toPattern(true));
            System.out.println("In table but not CONTRACTING: " + new UnicodeSet(ceSet2).removeAll(ceSet).toPattern(true));
            throw new IllegalArgumentException("Inconsistent data");
        }
        
/*
0FB2 0F71 ; [.124E.0020.0002.0FB2][.125F.0020.0002.0F71] # TIBETAN SUBJOINED LETTER RA + TIBETAN VOWEL SIGN AA
0FB3 0F71 ; [.1250.0020.0002.0FB3][.125F.0020.0002.0F71] # TIBETAN SUBJOINED LETTER LA + TIBETAN VOWEL SIGN AA
        int[] temp1 = int[20];
        int[] temp2 = int[20];
        int[] temp3 = int[20];
        getCEs("\u0fb2", true, temp1);
        getCEs("\u0fb3", true, temp2);
        getCEs("\u0f71", true, temp3);
        add("\u0FB2\u0F71", concat(temp1, temp3));
*/
        
    }
    
    Iterator getContractions() {
        return contractingTable.keySet().iterator();
    }
    
    int getContractionCount() {
        return contractingTable.size();
    }
    
    boolean contractionTableContains(String s) {
        return contractingTable.get(s) != null;
    }
    
}
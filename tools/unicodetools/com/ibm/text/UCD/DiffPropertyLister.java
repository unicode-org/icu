/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/DiffPropertyLister.java,v $
* $Date: 2002/06/22 01:21:09 $
* $Revision: 1.7 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import com.ibm.icu.text.UnicodeSet;
import java.io.*;

class DiffPropertyLister extends PropertyLister {
    private UCD oldUCD;
    private UnicodeSet set = new UnicodeSet();
    private static final int NOPROPERTY = -1;

    public DiffPropertyLister(String oldUCDName, String newUCDName, PrintWriter output, int property) {
        this.output = output;
        this.ucdData = UCD.make(newUCDName);
        if (property != NOPROPERTY) newProp = DerivedProperty.make(property, ucdData);
        
        if (oldUCDName != null) {
        	this.oldUCD = UCD.make(oldUCDName);
        	if (property != NOPROPERTY) oldProp = DerivedProperty.make(property, oldUCD);
        }
        breakByCategory = property != NOPROPERTY;
        useKenName = false;
    }
    
    public DiffPropertyLister(String oldUCDName, String newUCDName, PrintWriter output) {
    	this(oldUCDName, newUCDName, output, NOPROPERTY);
    }
    
    public UnicodeSet getSet() {
        return set;
    }

    public String valueName(int cp) {
        return major_minor_only(ucdData.getVersion());
    }

    /*
    public String optionalName(int cp) {
        if ((propMask & 0xFF00) == DECOMPOSITION_TYPE) {
            return Utility.hex(ucdData.getDecompositionMapping(cp));
        } else {
            return "";
        }
    }
    */

	UnicodeProperty newProp = null;
	UnicodeProperty oldProp = null;
	String value = "";
	
    public String optionalComment(int cp) {
    	String normal = super.optionalComment(cp);
        return oldUCD.getModCatID_fromIndex(
        	oldUCD.getModCat(cp, breakByCategory ? CASED_LETTER_MASK : 0))
        	+ "/" + normal;
    }

	

    public byte status(int cp) {
    	if (newProp == null) {
        	if (ucdData.isAllocated(cp) && (oldUCD == null || !oldUCD.isAllocated(cp))) {
    	        set.add(cp);
        	    return INCLUDE;
        	}
        	else {
        	    return EXCLUDE;
        	}
    	}
    	
    	// just look at property differences among allocated characters
    	
    	if (!ucdData.isAllocated(cp)) return EXCLUDE;    	
    	if (!oldUCD.isAllocated(cp)) return EXCLUDE;   
    	
    	String val = newProp.getValue(cp);
    	String oldVal = oldProp.getValue(cp);
    	if (!oldVal.equals(val)) {
    	    set.add(cp);
    	    return INCLUDE;
    	}
    	return EXCLUDE;

        /*if (cp == 0xFFFF) {
            System.out.println("# " + Utility.hex(cp));
        }
        */
    }
    
    public String headerString() {
        String result;
        if (oldUCD != null) {
            result = "# Differences between " 
                + major_minor_only(ucdData.getVersion()) 
                + " and " 
                + major_minor_only(oldUCD.getVersion());
        } else {
            result = "# Designated as of " 
                + major_minor_only(ucdData.getVersion())
                + " [excluding removed Hangul Syllables]";
        }
        //System.out.println("hs: " + result);
        return result;
    }
    
    /*
    public int print() {
        String status;
        if (oldUCD != null) {
            status = "# Differences between " + ucdData.getVersion() + " and " + oldUCD.getVersion();
        } else {
            status = "# Allocated as of " + ucdData.getVersion();
        }
        output.println();
        output.println();
        output.println(status);
        output.println();
        System.out.println(status);
        int count = super.print();
        output.println();
        if (oldUCD != null) {
            output.println("# Total " + count + " new code points allocated in " + ucdData.getVersion());
        } else {
            output.println("# Total " + count + " code points allocated in " + ucdData.getVersion());
        }

        output.println();
        return count;
    }
    */
    
    private String major_minor_only(String s) {
    	if (newProp != null) return s;
    	
        return s.substring(0, s.lastIndexOf('.'));
    }

}


/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/DerivedPropertyLister.java,v $
* $Date: 2002/03/20 00:21:43 $
* $Revision: 1.10 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;
import java.util.*;

import com.ibm.text.utility.*;

final class DerivedPropertyLister extends PropertyLister {
    static final boolean BRIDGE = false;

    static int enum = 0;

    //private int propMask;
    //private DerivedProperty dprop;
    private UnicodeProperty uprop;
    int width;
    boolean varies;

    public DerivedPropertyLister(UCD ucd, int propMask, PrintWriter output) {
        //this.propMask = propMask;
        this.output = output;
        this.ucdData = ucd;
        // this.dprop = new DerivedProperty(ucd);
        uprop = DerivedProperty.make(propMask, ucd);
        varies = uprop.getValueType() < BINARY;

        width = super.minPropertyWidth();
        switch (propMask) {
          case DerivedProperty.GenNFD: case DerivedProperty.GenNFC: case DerivedProperty.GenNFKD: case DerivedProperty.GenNFKC:
            alwaysBreaks = true;
            break;
          case DerivedProperty.FC_NFKC_Closure:
            alwaysBreaks = true;
            width = 21;
            break;
          case DerivedProperty.QuickNFC: case DerivedProperty.QuickNFKC:
            width = 11;
            break;
        }
    }
    
    public String headerString() {
        return uprop.getHeader();
    }

    public String valueName(int cp) {
    	return uprop.getListingValue(cp);
    }

    //public String optionalComment(int cp) {
    //    return super.optionalComment(cp) + " [" + ucdData.getCodeAndName(computedValue) + "]";
    //}


    public int minPropertyWidth() {
        return width;
    }


    /*
    public String optionalComment(int cp) {
        String id = ucdData.getCategoryID(cp);
        if (UCD.mainCategoryMask(ucdData.getCategory(cp)) == LETTER_MASK) return id.substring(0,1) + "*";
        return id;
    }
    */
    /*
    public String optionalName(int cp) {
        if ((propMask & 0xFF00) == DECOMPOSITION_TYPE) {
            return Utility.hex(ucdData.getDecompositionMapping(cp));
        } else {
            return "";
        }
    }
    */

    String last;

    public byte status(int cp) {
        if (!uprop.hasUnassigned() && !ucdData.isAssigned(cp)) return EXCLUDE;
        if (!varies) {
            return uprop.hasValue(cp) ? INCLUDE : EXCLUDE;
        }
        String prop = uprop.getValue(cp);
        if (prop.length() == 0) return EXCLUDE;
        if (prop.equals(last)) return INCLUDE;
        last = prop;
        return BREAK;
    }

    /*
    static Map computedValue = new HashMap();
    static String getComputedValue(int cp) {
        return (String) computedValue.get(new Integer(cp));
    }
    static void setComputedValue(int cp, String value) {
        computedValue.put(new Integer(cp), value);
    }
    static String lastValue = "";
    static String currentValue = "";

    StringBuffer foldBuffer = new StringBuffer();

    */
}


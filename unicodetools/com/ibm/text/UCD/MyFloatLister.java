/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/MyFloatLister.java,v $
* $Date: 2004/03/11 19:03:17 $
* $Revision: 1.6 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;
import java.text.NumberFormat;
import java.util.Locale;

class MyFloatLister extends PropertyLister {
    private double propMask;
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    
    public MyFloatLister(UCD ucd, double f, PrintWriter output) {
        this.propMask = f;
        this.output = output;
        this.ucdData = ucd;
        nf.setGroupingUsed(false);
        nf.setMaximumFractionDigits(8);
        nf.setMinimumFractionDigits(1);
    }

    public String valueName(int cp) {
        return nf.format(ucdData.getNumericValue(cp));
    }

    public String optionalName(int cp) {
        return ucdData.getNumericTypeID(cp);
    }

    public byte status(int cp) {
        //if ((cp & 0xFFF) == 0) System.out.println("# " + Utility.hex(cp));
        if (false && !ucdData.isRepresented(cp)) {
            if (ucdData.mapToRepresentative(cp, ucdData.getCompositeVersion()) != cp) return PropertyLister.CONTINUE;
            return PropertyLister.CONTINUE;
        }
        if (ucdData.getCategory(cp) == Cn) return PropertyLister.CONTINUE;
        return ucdData.getNumericValue(cp) == propMask ? INCLUDE : EXCLUDE;
    }
}


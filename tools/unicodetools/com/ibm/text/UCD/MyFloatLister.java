/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/MyFloatLister.java,v $
* $Date: 2001/08/31 00:30:17 $
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;

class MyFloatLister extends PropertyLister {
    private float propMask;

    public MyFloatLister(UCD ucd, float f, PrintStream output) {
        this.propMask = f;
        this.output = output;
        this.ucdData = ucd;
    }

    public String propertyName(int cp) {
        return ""+ucdData.getNumericValue(cp);
    }

    public String optionalName(int cp) {
        return ucdData.getNumericTypeID(cp);
    }

    public byte status(int cp) {
        //if ((cp & 0xFFF) == 0) System.out.println("# " + Utility.hex(cp));
        if (!ucdData.isRepresented(cp)) {
            if (ucdData.mapToRepresentative(cp, false) != cp) return PropertyLister.CONTINUE;
            return PropertyLister.CONTINUE;
        }
        if (ucdData.getCategory(cp) == Cn) return PropertyLister.CONTINUE;
        return ucdData.getNumericValue(cp) == propMask ? INCLUDE : EXCLUDE;
    }
}


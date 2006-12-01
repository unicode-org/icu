/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCA/AbbreviatedUnicodeSetIterator.java,v $ 
* $Date: 2004/02/06 18:32:04 $ 
* $Revision: 1.2 $
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
import com.ibm.text.UCD.UnifiedBinaryProperty;
import com.ibm.text.UCD.UCDProperty;

import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class AbbreviatedUnicodeSetIterator extends UnicodeSetIterator {

    private boolean abbreviated;
    private int perRange;

    public AbbreviatedUnicodeSetIterator() {
        super();
        abbreviated = false;
    }

    public void reset(UnicodeSet newSet) {
        reset(newSet, false);
    }

    public void reset(UnicodeSet newSet, boolean abb) {
        reset(newSet, abb, 100);
    }

    public void reset(UnicodeSet newSet, boolean abb, int density) {
        super.reset(newSet);
        abbreviated = abb;
        perRange = newSet.getRangeCount();
        if (perRange != 0) {
            perRange = density / perRange;
        }
    }

    protected void loadRange(int myRange) {
        super.loadRange(myRange);
        if (abbreviated && (endElement > nextElement + perRange)) {
            endElement = nextElement + perRange;
        }
    }
}

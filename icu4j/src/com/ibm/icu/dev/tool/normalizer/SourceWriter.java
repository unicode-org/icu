/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/normalizer/Attic/SourceWriter.java,v $ 
 * $Date: 2000/03/10 04:17:57 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */
package com.ibm.tools.normalizer;

import com.ibm.text.*;
import com.ibm.util.CompactCharArray;
import com.ibm.util.CompactByteArray;

//===========================================================================================
// Utilities for writing data out as compilable source code
//
public abstract class SourceWriter {
    abstract public void close();
    abstract public void write(String name, short value);
    abstract public void write(String name, int value);

    abstract public void write(String name, CompactCharArray array);
    abstract public void write(String name, CompactByteArray array);
    abstract public void write(String name, StringBuffer str);

    abstract public void write(String name, char[] array);
    abstract public void write(String name, int[] array);

    abstract public void writeHex(String name, char value);
    abstract public void writeHex(String name, int value);
}


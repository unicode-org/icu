/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/tools/normalizer/Attic/SourceWriter.java,v $ 
 * $Date: 2000/07/12 16:41:26 $ 
 * $Revision: 1.3 $
 *
 *****************************************************************************************
 */
package com.ibm.tools.normalizer;

import com.ibm.text.*;
import com.ibm.util.CompactCharArray;
import com.ibm.util.CompactByteArray;
import java.io.PrintWriter;
import java.util.Date;

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

    void writeHeader(PrintWriter out) {
        out.println("/*");
        out.println("************************************************************************");
        out.println("*   Copyright (c) 1997-2000, International Business Machines");
        out.println("*   Corporation and others.  All Rights Reserved.");
        out.println("************************************************************************");
        out.println("* > THIS FILE WAS MACHINE GENERATED <");
        out.println("* >       DO NOT EDIT BY HAND       <");
        out.println("* >      RUN TOOL TO REGENERATE     <");
        out.println("* Tool: " + Normalizer.class.getName());
        out.println("* Creation date: " + new Date());
        out.println("*/");
    }
}

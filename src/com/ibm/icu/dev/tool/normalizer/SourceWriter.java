/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/normalizer/Attic/SourceWriter.java,v $ 
 * $Date: 2002/02/16 03:05:34 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.tool.normalizer;

import com.ibm.icu.lang.*;
import com.ibm.icu.text.*;
import com.ibm.icu.util.CompactCharArray;
import com.ibm.icu.util.CompactByteArray;
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

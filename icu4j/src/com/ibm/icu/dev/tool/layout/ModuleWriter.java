/*
 *******************************************************************************
 * Copyright (C) 1998-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/ModuleWriter.java,v $
 * $Date: 2003/01/14 19:05:23 $
 * $Revision: 1.1 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ModuleWriter
{
    public ModuleWriter(ScriptData theScriptData)
    {
        scriptData = theScriptData;
        output = null;
    }

    public void openFile(String outputFileName)
    {
        try
        {
            output = new PrintStream(
                new FileOutputStream(outputFileName));
        }
        catch (IOException e)
        {
            System.out.println("? Could not open " + outputFileName + " for writing.");
            return;
        }

        System.out.println("\nWriting module " + outputFileName + "...");
    }

    public void writeHeader()
    {
        output.print(moduleHeader);
    }
    
    public void includeFile(String fileName)
    {
        output.print("#include \"");
        output.print(fileName);
        output.println("\"");
    };

    public void closeFile()
    {
        System.out.println("Done.");
        output.close();
    }

    protected ScriptData scriptData;
    protected PrintStream output;
    
    protected static final String moduleHeader = 
    "/*\n" +
    " * %W% %E%\n" +
    " *\n" +
    " * (C) Copyright IBM Corp. 1998 - 2003. All Rights Reserved.\n" +
    " *\n" +
    " * WARNING: THIS FILE IS MACHINE GENERATED. DO NOT HAND EDIT IT UNLESS\n" +
    " * YOU REALLY KNOW WHAT YOU'RE DOING.\n" +
    " *\n" +
    " */\n" +
    "\n";
}
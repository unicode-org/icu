/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * Created on Dec 3, 2003
 *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.layout;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ModuleWriter
{
    public ModuleWriter()
    {
        wroteDefine = false;
        output = null;
    }

    public void openFile(String outputFileName) {
        try
        {
            output = new PrintStream(
                new FileOutputStream(outputFileName));
        } catch (IOException e) {
            System.out.println("? Could not open " + outputFileName + " for writing.");
            return;
        }
    
        wroteDefine = false;
        System.out.println("Writing module " + outputFileName + "...");
    }

    public void writeHeader(String define, String[] includeFiles) {
        output.print(moduleHeader);
        
        if (define != null) {
            wroteDefine = true;
            output.print("#ifndef ");
            output.println(define);
            
            output.print("#define ");
            output.println(define);
            
            output.println();
        }
        
        if (includeFiles != null) {
            for (int i = 0; i < includeFiles.length; i += 1) {
                output.print("#include \"");
                output.print(includeFiles[i]);
                output.println("\"");
            }
            
            output.println();
        }
        
        output.print(moduleBegin);
    }

    public void writeTrailer() {
        output.print(moduleTrailer);
        
        if (wroteDefine) {
            output.println("#endif");
            
        }
    }

    public void closeFile() {
        System.out.println("Done.");
        output.close();
    }

    protected boolean wroteDefine;
    
    protected PrintStream output;

    protected static final String moduleHeader = "/*\n" +
            " *\n" +
            " * (C) Copyright IBM Corp. 1998-2004. All Rights Reserved.\n" +
            " *\n" +
            " * WARNING: THIS FILE IS MACHINE GENERATED. DO NOT HAND EDIT IT UNLESS\n" +
            " * YOU REALLY KNOW WHAT YOU'RE DOING.\n" +
            " */\n" +
            "\n";

    protected static final String moduleBegin = "U_NAMESPACE_BEGIN\n\n";

    protected static final String moduleTrailer = "U_NAMESPACE_END\n";

}

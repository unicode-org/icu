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



class GDEFWriter extends OpenTypeTableWriter
{
    ClassTable classTable;
    String scriptName;
    
    public GDEFWriter(String scriptName, ClassTable classTable)
    {
        super(1024);
        this.classTable = classTable;
        this.scriptName = scriptName;
    }
    
    public void writeTable(PrintStream output)
    {
        System.out.println("Writing " + scriptName + " GDEF table...");
        
        // 0x0001000 (fixed1) version number
        writeData(0x0001);
        writeData(0x0000);
        
        int classDefOffset = getOutputIndex();
        writeData(0); // glyphClassDefOffset (will fix later);
        writeData(0); // attachListOffset
        writeData(0); // ligCaretListOffset
        writeData(0); // markAttachClassDefOffset
        
        fixOffset(classDefOffset, 0);
        
        classTable.writeClassTable(this);

        output.print("const le_uint8 ");
        output.print(scriptName);
        output.println("Shaping::glyphDefinitionTable[] = {");
        
        dumpTable(output, 8);
        output.println("};\n");
    }
}

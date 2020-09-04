// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
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


public class GSUBWriter extends OpenTypeTableWriter
{
    private ScriptList scriptList;
    private FeatureList featureList;
    private LookupList lookupList;
    private String scriptName;
    
    public GSUBWriter(String theScriptName, ScriptList theScriptList, FeatureList theFeatureList,
                      LookupList theLookupList)
    {
        super(1024);
        
        scriptList  = theScriptList;
        featureList = theFeatureList;
        lookupList  = theLookupList;
        scriptName  = theScriptName;
    }
    
    public void writeTable(PrintStream output)
    {
        System.out.println("writing " + scriptName + " GSUB table...");
        
        // 0x00010000 (fixed1) version number
        writeData(0x0001);
        writeData(0x0000);
        
        int listOffset = getOutputIndex();
        
        writeData(0); // script list offset (fixed later)
        writeData(0); // feature list offset (fixed later)
        writeData(0); // lookup list offset (fixed later)
        
        fixOffset(listOffset++, 0);
        scriptList.writeScriptList(this);
        
        fixOffset(listOffset++, 0);
        featureList.writeFeaturetList(this);
        
        fixOffset(listOffset++, 0);
        lookupList.writeLookupList(this);

        output.print("const le_uint8 ");
        output.print(scriptName);
        output.println("Shaping::glyphSubstitutionTable[] = {");
        
        dumpTable(output, 8);
        output.println("};\n");
    }
}

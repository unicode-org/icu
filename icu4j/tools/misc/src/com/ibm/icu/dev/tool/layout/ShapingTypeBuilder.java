// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.tool.layout;

import java.io.PrintStream;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ShapingTypeBuilder extends OpenTypeTableWriter
{
    private ClassTable classTable;
    
    public ShapingTypeBuilder()
    {
        classTable = new ClassTable();
    }
    
    public void writeTable(PrintStream output)
    {
        classTable.writeClassTable(this);
        output.println("const le_uint8 ArabicShaping::shapingTypeTable[] = {");
        
        dumpTable(output, 8);
        output.println("};\n");
    }
    
    // TODO: The UnicodeSet is constrained to the BMP because the ClassTable data structure can
    // only handle 16-bit entries. This is probably OK as long as there aren't any joining scripts
    // outside of the BMP...
    public void buildShapingTypes(String filename)
    {
        UnicodeSet shapingTypes = new UnicodeSet("[[\\P{Joining_Type=Non_Joining}] & [\\u0000-\\uFFFF]]");
        int count = shapingTypes.size();
        
        System.out.println("There are " + count + " characters with a joining type.");
        
        for(int i = 0; i < count; i += 1) {
            int ch = shapingTypes.charAt(i);
            
            classTable.addMapping(ch, UCharacter.getIntPropertyValue(ch, UProperty.JOINING_TYPE));
        }
        
        LigatureModuleWriter writer = new LigatureModuleWriter();
        String[] includeFiles = {"LETypes.h", "ArabicShaping.h"};        
        
        writer.openFile(filename);
        writer.writeHeader(null, includeFiles);
        writer.writeTable(this);
        writer.writeTrailer();
        writer.closeFile();
    }
    
    public static void main(String[] args)
    {
        ShapingTypeBuilder stb = new ShapingTypeBuilder();
        
        stb.buildShapingTypes(args[0]);
    }
}

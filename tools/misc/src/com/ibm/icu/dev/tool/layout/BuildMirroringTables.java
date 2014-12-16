/*
 *******************************************************************************
 * Copyright (C) 1996-2005, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 */

package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

/**
 * @author emader
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BuildMirroringTables extends ModuleWriter
{
    public BuildMirroringTables()
    {
        super();
    }
    
    public void dump(String name, int[] array, int length, int valuesPerLine)
    {
        StringBuffer line = new StringBuffer("    ");
        
        output.println("const LEUnicode32 DefaultCharMapper::" + name + "[] = {");
        
        for (int i = 0; i < length; i += 1) {
            
            if (i > 0 && i % valuesPerLine == 0) {
                output.println(line.toString());
                line.setLength(4);
            }
    
            line.append("0x" + Utility.hex(array[i], 4));
            line.append(", ");
        }
    
        line.setLength(line.length() - 2);
    
        output.println(line.toString());
        output.println("};\n");
    }
    
    public void writeMirroredDataFile(String filename)
    {
        UnicodeSet mirrored = new UnicodeSet("[\\p{Bidi_Mirrored}]");
        int count = mirrored.size();
        int[] chars   = new int[count];
        int[] mirrors = new int[count];
        int total = 0;
        
        System.out.println("There are " + count + " mirrored characters.");
        
        for(int i = 0; i < count; i += 1) {
            int ch = mirrored.charAt(i);
            int m  = UCharacter.getMirror(ch);
            
            if (ch != m) {
                chars[total] = ch & 0xFFFF;
                mirrors[total++] = m & 0xFFFF;
            }
        }
        
        System.out.println("There are " + total + " characters with a different mirror.\n");
        
        openFile(filename);
        writeHeader(null, includeFiles);
        
        
        dump("mirroredChars", chars, total, 8);
        
        System.out.println();
        
        dump("srahCderorrim", mirrors, total, 8);
        
        output.println("const le_int32 DefaultCharMapper::mirroredCharsCount = " + total + ";\n");

        writeTrailer();
        closeFile();
    }
    
    private static String includeFiles[] = {"LETypes.h", "DefaultCharMapper.h"};
    
    public static void main(String[] args)
    {
        BuildMirroringTables bmt = new BuildMirroringTables();
        
        bmt.writeMirroredDataFile("MirroredCharData.cpp");
    }
}

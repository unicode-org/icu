/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import java.util.*;
import com.ibm.icu.impl.Utility;

public class ScriptRunModuleWriter extends ScriptModuleWriter
{
    public ScriptRunModuleWriter(ScriptData theScriptData)
    {
        super(theScriptData, null);
    }
    
    public void writeScriptRuns(String fileName)
    {
        int minScript   = scriptData.getMinValue();
        int maxScript   = scriptData.getMaxValue();
        int recordCount = scriptData.getRecordCount();
        
        openFile(fileName);
        writeHeader(null, includeFiles);
        output.println(preamble);
        
        for (int record = 0; record < recordCount; record += 1) {
            int script = scriptData.getRecord(record).scriptCode();
            
            output.print("    {0x");
            output.print(Utility.hex(scriptData.getRecord(record).startChar(), 6));
            output.print(", 0x");
            output.print(Utility.hex(scriptData.getRecord(record).endChar(), 6));
            output.print(", ");
            output.print(scriptData.getTag(script));
            output.print("ScriptCode}");
            output.print((record == recordCount - 1) ? " " : ",");
            output.print(" // ");
            output.println(scriptData.getName(script));
        }
        
        output.println(postamble);
        
        int power = 1 << Utility.highBit(recordCount);
        int extra = recordCount - power;
        
        output.print("le_int32 ScriptRun::scriptRecordsPower = 0x");
        output.print(Utility.hex(power, 4));
        output.println(";");
        
        
        output.print("le_int32 ScriptRun::scriptRecordsExtra = 0x");
        output.print(Utility.hex(extra, 4));
        output.println(";");

        Vector[] scriptRangeOffsets = new Vector[maxScript - minScript + 1];
        
        for (int script = minScript; script <= maxScript; script += 1) {
            scriptRangeOffsets[script - minScript] = new Vector();
        }
        
        for (int record = 0; record < recordCount; record += 1) {
            scriptRangeOffsets[scriptData.getRecord(record).scriptCode() - minScript].addElement(new Integer(record));
        }
        
        output.println();
        
        for (int script = minScript; script <= maxScript; script += 1) {
            Vector offsets = scriptRangeOffsets[script - minScript];
            
            output.print("le_int16 ");
            output.print(scriptData.getTag(script));
            output.println("ScriptRanges[] = {");
            output.print("    ");
            
            for (int offset = 0; offset < offsets.size(); offset += 1) {
                Integer i = (Integer) offsets.elementAt(offset);
                
                output.print(i.intValue());
                output.print(", ");
            }
            
            output.println("-1");
            output.println(postamble);
        }
        
        output.println("le_int16 *ScriptRun::scriptRangeOffsets[] = {");
        
        for (int script = minScript; script <= maxScript; script += 1) {
            output.print("    ");
            output.print(scriptData.getTag(script));
            output.print("ScriptRanges");
            output.print(script == maxScript? "  " : ", ");
            output.print("// ");
            output.println(scriptData.getName(script));
        }
        
        output.println(postamble);
        
        writeTrailer();
        closeFile();
    }
    
    private static final String[] includeFiles = {"LETypes.h", "LEScripts.h", "ScriptRun.h"};
    
    private static final String preamble = 
    "\n" +
    "ScriptRecord ScriptRun::scriptRecords[] = {";
    
    private static final String postamble =
    "};\n";
}
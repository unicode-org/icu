/*
 *******************************************************************************
 * Copyright (C) 1998-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/ScriptModuleWriter.java,v $
 * $Date: 2003/01/14 19:05:23 $
 * $Revision: 1.1 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

public class ScriptModuleWriter extends ModuleWriter
{
    public ScriptModuleWriter(ScriptData scriptData)
    {
        super(scriptData);
    }
    
    public void writeScriptHeader(String fileName)
    {
        int minScript = scriptData.getMinScript();
        int maxScript = scriptData.getMaxScript();
        
        openFile(fileName);
        writeHeader();
        output.println(preamble);
        
        for (int script = minScript; script <= maxScript; script += 1) {
            output.print("    ");
            output.print(scriptData.getScriptTag(script));
            output.print("ScriptCode = ");
            
            if (script < 10) {
                output.print(" ");
            }
            
            output.print(script);
            output.println(",");
        }
        
        output.println();
        output.print("    scriptCodeCount = ");
        output.println(maxScript - minScript + 1);
        
        output.println(postamble);
        closeFile();
    }
    
    private static final String preamble = 
    "#ifndef __LESCRIPTS_H\n" + 
    "#define __LESCRIPTS_H\n" +
    "\n" +
    "U_NAMESPACE_BEGIN\n" +
    "\n" +
    "/**\n" +
    " * Constants for Unicode script values, generated using\n" +
    " * ICU4J's <code>UScript</code> class.\n" +
    " *\n" +
    " * @draft ICU 2.4\n" +
    " */\n" +
    "enum ScriptCodes {";
    
    private static final String postamble =
    "};\n" +
    "\n" +
    "U_NAMESPACE_END\n" +
    "#endif";
}
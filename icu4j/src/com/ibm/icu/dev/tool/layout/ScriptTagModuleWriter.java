/*
 *******************************************************************************
 * Copyright (C) 1998-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/ScriptTagModuleWriter.java,v $
 * $Date: 2003/01/14 19:05:23 $
 * $Revision: 1.1 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.impl.Utility;

public class ScriptTagModuleWriter extends ModuleWriter
{
    
    private int scriptTag(String tag)
    {
        int result = 0;
        
        for (int i = 0; i < 4; i += 1) {
            result <<= 8;
            result += (int) (tag.charAt(i) & 0xFF);
        }
        
        return result;
    }
    
    public ScriptTagModuleWriter(ScriptData theScriptData)
    {
        super(theScriptData);
    }
    
    public void writeHeaderFile(String fileName)
    {
        int minScript = scriptData.getMinScript();
        int maxScript = scriptData.getMaxScript();
        
        openFile(fileName);
        writeHeader();
        output.println(hPreamble);
        
        for (int script = minScript; script <= maxScript; script += 1) {
            String tag = scriptData.getScriptTag(script);
            
            output.print("const LETag ");
            output.print(tag);
            output.print("ScriptTag = 0x");
            output.print(Utility.hex(scriptTag(tag), 8));
            output.print("; /* '");
            output.print(tag);
            output.print("' (");
            output.print(scriptData.getScriptName(script));
            output.println(") */");
        }
        
        output.println(hPostamble);
        closeFile();
    }
    
    public void writeCPPFile(String fileName)
    {
        int minScript = scriptData.getMinScript();
        int maxScript = scriptData.getMaxScript();
        
        openFile(fileName);
        writeHeader();
        output.println(cppPreamble);
        
        for (int script = minScript; script <= maxScript; script += 1) {
            String tag = scriptData.getScriptTag(script);
            
            output.print("    ");
            output.print(tag);
            output.print("ScriptTag");
            output.print((script == maxScript? " " : ","));
            output.print(" /* '");
            output.print(tag);
            output.print("' (");
            output.print(scriptData.getScriptName(script));
            output.println(") */");
        }
        
        output.println(cppPostamble);
        closeFile();
    }
    
    public void writeScriptTags(String fileName)
    {
        writeHeaderFile(fileName + ".h");
        writeCPPFile(fileName + ".cpp");
    }
    
    private static final String hPreamble = 
    "#ifndef __SCRIPTANDLANGUAGES_H\n" + 
    "#define __SCRIPTANDLANGUAGES_H\n" +
    "\n" +
    "/**\n" +
    " * \\file\n" +
    " * \\internal\n" +
    " */\n" +
    "\n" +
    "#include \"LETypes.h\"\n" +
    "#include \"LEScripts.h\"\n" +
    "\n" +
    "U_NAMESPACE_BEGIN\n";
    
    private static final String hPostamble =
    "\n" +
    "const LETag nullScriptTag = 0x00000000; /* ''     (NULL) */\n" +
    "\n" +
    "const LETag noLangSysTag  = 0x00000000; /* ''     (NONE) */\n" +
    "\n" +
    "U_NAMESPACE_END\n" +
    "#endif";
    
    private static final String cppPreamble = 
    "#include \"LETypes.h\"\n" +
    "#include \"ScriptAndLanguageTags.h\"\n" +
    "#include \"OpenTypeLayoutEngine.h\"\n" +
    "\n" +
    "U_NAMESPACE_BEGIN\n" +
    "\n" +
    "const LETag OpenTypeLayoutEngine::scriptTags[] = {";
    
    private static final String cppPostamble =
    "};\n" +
    "\n" +
    "U_NAMESPACE_END";
}
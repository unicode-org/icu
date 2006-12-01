/*
 *******************************************************************************
 * Copyright (C) 1998-2006, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

public class ScriptTagModuleWriter extends ScriptModuleWriter
{
    public ScriptTagModuleWriter(ScriptData theScriptData, LanguageData theLanguageData)
    {
        super(theScriptData, theLanguageData);
    }
    
    private void writeTagValueHeader(TagValueData data, String kind)
    {
        int min = data.getMinValue();
        int max = data.getMaxValue();

        for (int value = min; value <= max; value += 1) {
            output.print("const LETag ");
            output.print(data.getTagLabel(value));
            output.print(kind);
            output.print("Tag = ");
            output.print(data.makeTag(value));
            output.print("; /* '");
            output.print(data.getTag(value));
            output.print("' (");
            output.print(data.getName(value));
            output.println(") */");
        }
    }
    
    public void writeHeaderFile(String fileName)
    {
        openFile(fileName);
        writeHeader("__SCRIPTANDLANGUAGES_H", hIncludes, hPreamble);
        
        writeTagValueHeader(scriptData, "Script");
        
        output.println(hScriptPostamble);
        
        writeTagValueHeader(languageData, "Language");
        
        output.println(hPostamble);
        closeFile();
    }
    
    private void writeTagValueCPP(TagValueData data, String kind)
    {
        int min = data.getMinValue();
        int max = data.getMaxValue();
        
        for (int value = min; value <= max; value += 1) {
            output.print("    ");
            output.print(data.getTagLabel(value));
            output.print(kind);
            output.print("Tag");
            output.print((value == max? " " : ","));
            output.print(" /* '");
            output.print(data.getTag(value));
            output.print("' (");
            output.print(data.getName(value));
            output.println(") */");
        }
    }
    
    public void writeCPPFile(String fileName)
    {
        openFile(fileName);
        writeHeader(null, cppIncludes);
        output.println(cppPreamble);
        
        writeTagValueCPP(scriptData, "Script");
        
        output.println(cppScriptPostamble);
        
        writeTagValueCPP(languageData, "Language");
        
        output.println(cppPostamble);
        
        writeTrailer();
        closeFile();
    }
    
    public void writeScriptTags(String fileName)
    {
        writeHeaderFile(fileName + ".h");
        writeCPPFile(fileName + ".cpp");
    }

    private static final String[] hIncludes = {"LETypes.h"};

    private static final String hPreamble = 
    "/**\n" +
    " * \\file\n" +
    " * \\internal\n" +
    " */\n" +
    "\n";
    
    private static final String hScriptPostamble =
    "\n" +
    "const LETag nullScriptTag = 0x00000000; /* ''     (NULL) */\n" +
    "\n";
    
    private static final String hPostamble =
    "\n" +
    "\n" +
    "U_NAMESPACE_END\n" +
    "#endif";
    
    private static final String[] cppIncludes =
        {"LETypes.h", "ScriptAndLanguageTags.h", "OpenTypeLayoutEngine.h"};
    
    private static final String cppPreamble = 
    "const LETag OpenTypeLayoutEngine::scriptTags[] = {";
    
    private static final String cppScriptPostamble =
    "};\n" +
    "\n" +
    "const LETag OpenTypeLayoutEngine::languageTags[] = {";
    
    private static final String cppPostamble =
    "};\n";
}
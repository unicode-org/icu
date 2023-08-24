// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1998-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

import com.ibm.icu.lang.UScript;

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
            if(kind.equals("Script")) {
                switch(value) {
                case UScript.BENGALI:
                    output.println("const LETag bng2ScriptTag = 0x626E6732; /* 'bng2' (BENGALI v.2) (manually added) */");
                    break;
                case UScript.DEVANAGARI:
                    output.println("const LETag dev2ScriptTag = 0x64657632; /* 'dev2' (DEVANAGARI v.2) (manually added) */");
                    break;
                case UScript.GUJARATI:
                    output.println("const LETag gjr2ScriptTag = 0x676A7232; /* 'gjr2' (GUJARATI v.2) (manually added) */");
                    break;
                case UScript.GURMUKHI:
                    output.println("const LETag gur2ScriptTag = 0x67757232; /* 'gur2' (GURMUKHI v.2) (manually added) */");
                    break;
                case UScript.KANNADA:
                    output.println("const LETag knd2ScriptTag = 0x6B6E6432; /* 'knd2' (KANNADA v.2) (manually added) */");
                    break;
                case UScript.MALAYALAM:
                    output.println("const LETag mlm2ScriptTag = 0x6D6C6D32; /* 'mlm2' (MALAYALAM v.2) (manually added) */");
                    break;
                case UScript.ORIYA:
                    output.println("const LETag ory2ScriptTag = 0x6F727932; /* 'ory2' (ORIYA v.2) (manually added) */");
                    break;
                case UScript.TAMIL:
                    output.println("const LETag tml2ScriptTag = 0x746D6C32; /* 'tml2' (TAMIL v.2) (manually added) */");
                    break;
                case UScript.TELUGU:
                    output.println("const LETag tel2ScriptTag = 0x74656C32; /* 'tel2' (TELUGU v.2) (manually added) */");
                    break;
                default:
                    break;
                }
            }
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

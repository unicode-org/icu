/*
 *******************************************************************************
 * Copyright (C) 1998-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

public class ScriptIDModuleWriter extends ScriptModuleWriter
{
    public ScriptIDModuleWriter(ScriptData scriptData, LanguageData languageData)
    {
        super(scriptData, languageData);
    }

    public void writeScriptHeader(String fileName)
    {
        int minScript = scriptData.getMinValue();
        int maxScript = scriptData.getMaxValue();
        
        openFile(fileName);
        writeHeader("__LESCRIPTS_H", includeFiles);
        output.println(scriptPreamble);
        
        for (int script = minScript; script <= maxScript; script += 1) {
            output.print("    ");
            output.print(scriptData.getTag(script));
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
        
        writeTrailer();
        closeFile();
    }
    
    public void writeLanguageHeader(String fileName)
    {
        int minLanguage = languageData.getMinValue();
        int maxLanguage = languageData.getMaxValue();
        
        openFile(fileName);
        writeHeader("__LELANGUAGES_H", includeFiles);
        output.println(languagePreamble);
        
        for (int language = minLanguage; language <= maxLanguage; language += 1) {
            output.print("    ");
            output.print(languageData.getTagLabel(language).toLowerCase());
            output.print("LanguageCode = ");
            
            if (language < 10) {
                output.print(" ");
            }
            
            output.print(language);
            output.println(",");
        }
        
        output.println();
        output.print("    languageCodeCount = ");
        output.println(maxLanguage - minLanguage + 1);
        
        output.println(postamble);
        
        writeTrailer();
        closeFile();
    }
    
    private static final String[] includeFiles = {"LETypes.h"};
    
    private static final String scriptPreamble = 
    "/**\n" +
    " * Constants for Unicode script values, generated using\n" +
    " * ICU4J's <code>UScript</code> class.\n" +
    " *\n" +
    " * @draft ICU 2.6\n" +
    " */\n" +
    "\n" +
    "enum ScriptCodes {";
    
    private static final String languagePreamble = 
    "/**\n" +
    " * A provisional list of language codes. For now,\n" +
    " * this is just a list of languages which the LayoutEngine\n" +
    " * supports.\n" +
    " *\n" +
    " * @draft ICU 2.6\n" +
    " */\n" +
    "\n" +
    "enum LanguageCodes {";
    
    private static final String postamble =
    "};\n";
}
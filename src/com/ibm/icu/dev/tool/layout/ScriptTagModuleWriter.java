/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/ScriptTagModuleWriter.java,v $
 * $Date: 2003/06/03 18:49:32 $
 * $Revision: 1.6 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

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
    
    public ScriptTagModuleWriter(ScriptData theScriptData, LanguageData theLanguageData)
    {
        super(theScriptData, theLanguageData);
    }
    
    public void writeHeaderFile(String fileName)
    {
        int min = scriptData.getMinScript();
        int max = scriptData.getMaxScript();
        
        openFile(fileName);
        writeHeader();
        output.println(hPreamble);
        
        for (int script = min; script <= max; script += 1) {
            output.print("const LETag ");
            output.print(scriptData.getScriptTagLabel(script));
            output.print("ScriptTag = ");
            output.print(scriptData.makeScriptTag(script));
            output.print("; /* '");
            output.print(scriptData.getScriptTag(script));
            output.print("' (");
            output.print(scriptData.getScriptName(script));
            output.println(") */");
        }
        
        output.println(hScriptPostamble);
        
        min = languageData.getMinLanguage();
        max = languageData.getMaxLanguage();
        
        for (int language = min; language <= max; language += 1) {
            output.print("const LETag ");
            output.print(languageData.getLanguageTagLabel(language));
            output.print("LanguageTag = ");
            output.print(languageData.makeLanguageTag(language));
            output.print("; /* '");
            output.print(languageData.getLanguageTag(language));
            output.print("' (");
            output.print(languageData.getLanguageName(language));
            output.println(") */");
        }
        
        output.println(hPostamble);
        closeFile();
    }
    
    public void writeCPPFile(String fileName)
    {
        int min = scriptData.getMinScript();
        int max = scriptData.getMaxScript();
        
        openFile(fileName);
        writeHeader();
        output.println(cppPreamble);
        
        for (int script = min; script <= max; script += 1) {
            String tag = scriptData.getScriptTag(script);
            
            output.print("    ");
            output.print(tag);
            output.print("ScriptTag");
            output.print((script == max? " " : ","));
            output.print(" /* '");
            output.print(tag);
            output.print("' (");
            output.print(scriptData.getScriptName(script));
            output.println(") */");
        }
        
        output.println(cppScriptPostamble);
        
        min = languageData.getMinLanguage();
        max = languageData.getMaxLanguage();
        
        for (int language = min; language <= max; language += 1) {
            //String tag = languageData.getLanguageTag(language);
            
            output.print("    ");
            output.print(languageData.getLanguageTagLabel(language));
            output.print("LanguageTag");
            output.print((language == max? " " : ","));
            output.print(" /* '");
            output.print(languageData.getLanguageTag(language));
            output.print("' (");
            output.print(languageData.getLanguageName(language));
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
    "\n" +
    "U_NAMESPACE_BEGIN\n";
    
    private static final String hScriptPostamble =
    "\n" +
    "const LETag nullScriptTag = 0x00000000; /* ''     (NULL) */\n" +
    "\n";
    
    private static final String hPostamble =
    "\n" +
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
    
    private static final String cppScriptPostamble =
    "};\n" +
    "\n" +
    "const LETag OpenTypeLayoutEngine::languageTags[] = {";
    
    private static final String cppPostamble =
    "};\n" +
    "\n" +
    "U_NAMESPACE_END";
}
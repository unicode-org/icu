/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/ScriptModuleWriter.java,v $
 * $Date: 2003/06/03 18:49:31 $
 * $Revision: 1.3 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

public class ScriptModuleWriter extends ModuleWriter
{
    public ScriptModuleWriter(ScriptData scriptData, LanguageData languageData)
    {
        super(scriptData, languageData);
    }

	public void writeScriptHeader(String fileName)
	{
	    int minScript = scriptData.getMinScript();
	    int maxScript = scriptData.getMaxScript();
	    
	    openFile(fileName);
	    writeHeader();
	    output.println(scriptPreamble);
	    
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
    
    public void writeLanguageHeader(String fileName)
    {
        int minLanguage = languageData.getMinLanguage();
        int maxLanguage = languageData.getMaxLanguage();
        
        openFile(fileName);
        writeHeader();
        output.println(languagePreamble);
        
        for (int language = minLanguage; language <= maxLanguage; language += 1) {
            output.print("    ");
            output.print(languageData.getLanguageTagLabel(language).toLowerCase());
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
        closeFile();
    }
    
    private static final String scriptPreamble = 
    "#ifndef __LESCRIPTS_H\n" + 
    "#define __LESCRIPTS_H\n" +
    "\n" +
    "U_NAMESPACE_BEGIN\n" +
    "\n" +
    "/**\n" +
    " * Constants for Unicode script values, generated using\n" +
    " * ICU4J's <code>UScript</code> class.\n" +
    " *\n" +
    " * @draft ICU 2.6\n" +
    " */\n" +
    "\n" +
    "enum ScriptCodes {";
    
    private static final String languagePreamble = 
    "#ifndef __LELANGUAGES_H\n" + 
    "#define __LELANGUAGES_H\n" +
    "\n" +
    "U_NAMESPACE_BEGIN\n" +
    "\n" +
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
    "};\n" +
    "\n" +
    "U_NAMESPACE_END\n" +
    "#endif";
}
/*
 *******************************************************************************
 * Copyright (C) 1998-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/tool/layout/ScriptNameBuilder.java,v $
 * $Date: 2003/06/03 18:49:32 $
 * $Revision: 1.3 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.layout;

public class ScriptNameBuilder
{
    /*
     * This tool builds the script related header files and data tables needed by
     * the ICU LayoutEngine. By using the ICU4J interfaces to access the script
     * information, we can be sure that the data written by this tool is in synch
     * with ICU.
     */
    public static void main(String[] args)
    {
        ScriptData scriptData = new ScriptData();
        LanguageData languageData = new LanguageData();
        
        ScriptModuleWriter scriptModuleWriter = new ScriptModuleWriter(scriptData, languageData);
        
        scriptModuleWriter.writeScriptHeader("LEScripts.h");
        scriptModuleWriter.writeLanguageHeader("LELanguages.h");
        
        ScriptTagModuleWriter scriptTagModuleWriter = new ScriptTagModuleWriter(scriptData, languageData);
        
        scriptTagModuleWriter.writeScriptTags("ScriptAndLanguageTags");
        
        ScriptRunModuleWriter scriptRunModuleWriter = new ScriptRunModuleWriter(scriptData);
        
        scriptRunModuleWriter.writeScriptRuns("ScriptRunData.cpp");
    }
}